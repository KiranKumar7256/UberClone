package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PassengerActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Button requestcarbtn, beepbtn;
    private boolean isUberCancelled = true;
    private boolean isCarReady=false;

    private Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        requestcarbtn = findViewById(R.id.requestcarbtn);
        requestcarbtn.setOnClickListener(this);

        beepbtn = findViewById(R.id.beepbtn);
        t=new Timer();

        beepbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDriverUpdates();
            }
        });

        ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
        carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (objects.size() > 0 && e == null) {
                    isUberCancelled = false;
                    requestcarbtn.setText("Cancel your Uber Order");
                }
            }
        });

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        startActivity(new Intent(PassengerActivity.this, MainActivity.class));
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateCameraPassengerLocation(location);
            }
        };

        if (Build.VERSION.SDK_INT < 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateCameraPassengerLocation(currentPassengerLocation);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateCameraPassengerLocation(currentPassengerLocation);
            }
        }
    }

    private void updateCameraPassengerLocation(Location pLocation) {
        if (isCarReady == false) {
            LatLng passengerLocation = new LatLng(pLocation.getLatitude(), pLocation.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLocation, 16.0f));
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("You are Here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    @Override
    public void onClick(View v) {
        if (isUberCancelled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location passengerCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (passengerCurrentLocation != null) {
                    ParseObject requestCar = new ParseObject("RequestCar");
                    requestCar.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint userLocation = new ParseGeoPoint(passengerCurrentLocation.getLatitude(), passengerCurrentLocation.getLongitude());
                    requestCar.put("passengerLocation", userLocation);

                    requestCar.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(PassengerActivity.this, "A Car request is sent", Toast.LENGTH_SHORT).show();
                                requestcarbtn.setText("Cancel your Uber Order");
                                isUberCancelled = false;
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "unknown error: something went wrong", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
            carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> requestList, ParseException e) {
                    if (requestList.size() > 0 && e == null) {
                        isUberCancelled = true;
                        requestcarbtn.setText("Request a Car");

                        for (ParseObject uberRequest : requestList) {
                            uberRequest.deleteInBackground(new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Toast.makeText(PassengerActivity.this, "Car request Cancellled", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void getDriverUpdates() {

        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ParseQuery<ParseObject> uberRequestQuery = ParseQuery.getQuery("RequestCar");
                uberRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                uberRequestQuery.whereEqualTo("requestAccepted", true);
                uberRequestQuery.whereExists("driverOfMe");

                uberRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (objects.size() > 0 && e == null) {
                            isCarReady=true;
                            for (ParseObject requestObject : objects) {
                                ParseQuery<ParseUser> driverQuery = ParseUser.getQuery();
                                driverQuery.whereEqualTo("username", requestObject.get("driverOfMe"));
                                driverQuery.findInBackground(new FindCallback<ParseUser>() {
                                    @Override
                                    public void done(List<ParseUser> drivers, ParseException e) {
                                        if (drivers.size() > 0 && e == null) {
                                            for (ParseUser parseUser : drivers) {
                                                ParseGeoPoint driverOfRequestLocation = parseUser.getParseGeoPoint("driverLocation");
                                                if (ActivityCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                    return;
                                                }
                                                Location passengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                                ParseGeoPoint pLocationAsParseGeoPoint = new ParseGeoPoint(passengerLocation.getLatitude(), passengerLocation.getLongitude());

                                                double milesDistance = driverOfRequestLocation.distanceInMilesTo(pLocationAsParseGeoPoint);

                                                if (milesDistance < 0.3) {
                                                    requestObject.deleteInBackground(new DeleteCallback() {
                                                        @Override
                                                        public void done(ParseException e) {
                                                            if (e == null) {
                                                                Toast.makeText(PassengerActivity.this, "Hurray! your Uber is arrived", Toast.LENGTH_LONG).show();
                                                                isCarReady = false;
                                                                isUberCancelled = true;
                                                                requestcarbtn.setText("You can order a new Car Now");
                                                            }
                                                        }
                                                    });
                                                } else {

                                                    float roundedDiatance = Math.round(milesDistance * 10) / 10;
                                                    Toast.makeText(PassengerActivity.this, requestObject.getString("driverOfMe") + " is " + roundedDiatance + " miles away from you. Please Wait!", Toast.LENGTH_SHORT).show();

                                                    LatLng dLocation = new LatLng(driverOfRequestLocation.getLatitude(), driverOfRequestLocation.getLongitude());

                                                    LatLng pLocation = new LatLng(passengerLocation.getLatitude(), passengerLocation.getLongitude());

                                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                    Marker driverMarker = mMap.addMarker(new MarkerOptions().position(dLocation).title("Driver Locaion"));
                                                    Marker passengerMarker = mMap.addMarker(new MarkerOptions().position(pLocation).title("Passenger Location"));

                                                    ArrayList<Marker> myMarkers = new ArrayList<>();
                                                    myMarkers.add(driverMarker);
                                                    myMarkers.add(passengerMarker);

                                                    for (Marker marker : myMarkers) {
                                                        builder.include(marker.getPosition());
                                                    }

                                                    LatLngBounds bounds = builder.build();

                                                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 10);
                                                    mMap.animateCamera(cameraUpdate);
                                                }
                                            }
                                        }
                                    }
                                });

                            }
                        }
                    }
                });
            }
        },0,3000);


    }
}