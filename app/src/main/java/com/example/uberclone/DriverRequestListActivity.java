package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

public class DriverRequestListActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ListView listView;
    private ArrayList<String> nearbyRequests,requestCarUserNames;
    private ArrayList<Double> passengerLatitudes, passengerLongitudes;
    private ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request_list);

        findViewById(R.id.getrequestsbtn).setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        listView = findViewById(R.id.requestlistview);
        nearbyRequests = new ArrayList<>();
        passengerLatitudes = new ArrayList<>();
        passengerLongitudes = new ArrayList<>();
        requestCarUserNames=new ArrayList<>();
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nearbyRequests);

        listView.setAdapter(adapter);

        nearbyRequests.clear();

        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeLocationListener();
        }

        listView.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logouticon) {
            ParseUser.logOutInBackground(new LogOutCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        startActivity(new Intent(DriverRequestListActivity.this, MainActivity.class));
                        finish();
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        if (Build.VERSION.SDK_INT < 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location driverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updatedriverListView(driverLocation);
        }
        else if(Build.VERSION.SDK_INT>=23){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            }
            else {
                Location driverLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updatedriverListView(driverLocation);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1000 && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                initializeLocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                Location driverLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updatedriverListView(driverLocation);
            }
        }
    }

    private void updatedriverListView(Location driverLocation){

        if(driverLocation!=null){
            saveDriverLocationToParse(driverLocation);
            ParseGeoPoint driverCurrentLocation=new ParseGeoPoint(driverLocation.getLatitude(),driverLocation.getLongitude());
            ParseQuery<ParseObject> requestCarQuery=ParseQuery.getQuery("RequestCar");
            requestCarQuery.whereNear("passengerLocation",driverCurrentLocation);
            requestCarQuery.whereDoesNotExist("driverOfMe");
            requestCarQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null){
                        if(objects.size()>0){
                            if(nearbyRequests.size()>0){
                                nearbyRequests.clear();
                            }
                            if (passengerLatitudes.size()>0){
                                passengerLatitudes.clear();
                            }
                            if (passengerLongitudes.size()>0){
                                passengerLongitudes.clear();
                            }
                            if(requestCarUserNames.size()>0){
                                requestCarUserNames.clear();
                            }
                            for(ParseObject nearRequest: objects){

                                ParseGeoPoint pLocation= (ParseGeoPoint) nearRequest.get("passengerLocation");
                                Double milesDistanceToPassenger=driverCurrentLocation.distanceInMilesTo(pLocation);

                                float roundedDistanceValue=Math.round(milesDistanceToPassenger*10)/10;
                                nearbyRequests.add("There are "+roundedDistanceValue+" miles to "+nearRequest.get("username"));

                                passengerLatitudes.add(pLocation.getLatitude());
                                passengerLongitudes.add(pLocation.getLongitude());

                                requestCarUserNames.add(nearRequest.get("username")+"");
                            }
                        }
                        else {
                            Toast.makeText(DriverRequestListActivity.this,"No passenger requests are available",Toast.LENGTH_SHORT).show();
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            Location cdLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (cdLocation!=null) {
                Intent intent = new Intent(this, ViewLocationsMapActivity.class);
                intent.putExtra("dLatitude", cdLocation.getLatitude());
                intent.putExtra("dLongitude", cdLocation.getLongitude());
                intent.putExtra("pLatitude", passengerLatitudes.get(position));
                intent.putExtra("pLongitude", passengerLongitudes.get(position));
                intent.putExtra("rUserName",requestCarUserNames.get(position));
                startActivity(intent);
            }
        }
    }

    private void initializeLocationListener(){
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (ActivityCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DriverRequestListActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                updatedriverListView(location);
            }
        };
    }

    private void saveDriverLocationToParse(Location location){
        ParseUser driver=ParseUser.getCurrentUser();

        ParseGeoPoint driverLocation=new ParseGeoPoint(location.getLatitude(),location.getLongitude());
        driver.put("driverLocation",driverLocation);
        driver.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e==null){
                    Toast.makeText(DriverRequestListActivity.this,"Location Saved",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}