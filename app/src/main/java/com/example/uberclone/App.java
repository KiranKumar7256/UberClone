package com.example.uberclone;

import android.app.Application;

import com.parse.Parse;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("e20iMrk1SbyGM1NKKDruF5n1A9JSMW07gk6uYZ4G")
                // if defined
                .clientKey("8jqKg6aGck8ApgK5wzha36JVX4gHVx1DUdilPyyd")
                .server("https://parseapi.back4app.com/")
                .build()
        );
    }
}
