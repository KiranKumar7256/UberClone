package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    enum STATE{
        SIGNUP,LOGIN;
    }

    private STATE state=STATE.SIGNUP;

    private EditText edtusername,edtpassword,edtpassengerdriver;
    Button signuploginbtn,onetimeloginbtn;
    RadioButton rbpassenger,rbdriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ParseUser.getCurrentUser() !=null){
        //    ParseUser.logOut();
            transitionToPassengerActivity();
            transitionToDriverRequestActivity();
            finish();
        }

        edtusername= findViewById(R.id.edtusername);
        edtpassword= findViewById(R.id.edtpassword);
        edtpassengerdriver= findViewById(R.id.edtpassengerdriver);
        signuploginbtn= findViewById(R.id.signuploginbtn);
        onetimeloginbtn= findViewById(R.id.onetimeloginbtn);
        rbpassenger= findViewById(R.id.rbpassenger);
        rbdriver= findViewById(R.id.rbdriver);

        signuploginbtn.setOnClickListener(this);
        onetimeloginbtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(state==STATE.SIGNUP){
            item.setTitle("SIGNUP");
            signuploginbtn.setText("LOGIN");
            state=STATE.LOGIN;
        }
        else if(state==STATE.LOGIN){
            item.setTitle("LOGIN");
            signuploginbtn.setText("SIGNUP");
            state=STATE.SIGNUP;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.signuploginbtn:
                if(state==STATE.SIGNUP){
                    if(rbpassenger.isChecked()==false && rbdriver.isChecked()==false){
                        Toast.makeText(this,"Are you a Driver or a Passenger",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ParseUser appUser= new ParseUser();
                    appUser.setUsername(edtusername.getText().toString());
                    appUser.setPassword(edtpassword.getText().toString());
                    if(rbpassenger.isChecked()){
                        appUser.put("as","Passenger");
                    }
                    else if(rbdriver.isChecked()){
                        appUser.put("as","Driver");
                    }
                    appUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e==null){
                                Toast.makeText(MainActivity.this,"Signup Successful !",Toast.LENGTH_SHORT).show();
                                transitionToPassengerActivity();
                                transitionToDriverRequestActivity();
                            }
                            else {
                                Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else if(state==STATE.LOGIN){
                    ParseUser.logInInBackground(edtusername.getText().toString(), edtpassword.getText().toString(), new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if(user!=null && e==null){
                                Toast.makeText(MainActivity.this," Login Successful !",Toast.LENGTH_SHORT).show();
                                transitionToPassengerActivity();
                                transitionToDriverRequestActivity();
                            }
                            else {
                                Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                break;
            case R.id.onetimeloginbtn:
                if(edtpassengerdriver.getText().toString().equals("Driver") || edtpassengerdriver.getText().toString().equals("Passenger")){
                    if(ParseUser.getCurrentUser()==null){
                        ParseAnonymousUtils.logIn(new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {
                                if(user!=null && e==null){
                                    Toast.makeText(MainActivity.this," Anonymous User logged in !",Toast.LENGTH_SHORT).show();
                                    user.put("as",edtpassengerdriver.getText().toString());
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if(e==null){
                                                transitionToPassengerActivity();
                                                transitionToDriverRequestActivity();
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
                break;
        }
    }

    private void transitionToPassengerActivity(){
        if(ParseUser.getCurrentUser()!=null){
            if(ParseUser.getCurrentUser().get("as").equals("Passenger")){
                startActivity(new Intent(MainActivity.this,PassengerActivity.class));
                finish();
            }
        }
    }

    private void transitionToDriverRequestActivity(){

        if(ParseUser.getCurrentUser()!=null){
            if(ParseUser.getCurrentUser().get("as").equals("Driver")){
                startActivity(new Intent(MainActivity.this,DriverRequestListActivity.class));
                finish();
            }
        }
    }
}