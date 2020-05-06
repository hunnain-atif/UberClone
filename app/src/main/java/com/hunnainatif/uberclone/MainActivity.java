package com.hunnainatif.uberclone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity {

    public void getStarted(View view) {
        Switch userTypeSwitch = (Switch) findViewById(R.id.userTypeSwitch);
        Log.i("SwitchValue", String.valueOf(userTypeSwitch.isChecked()));
        String userType = "driver";

        if(!userTypeSwitch.isChecked()) {
            userType = "rider";
        }
        ParseUser.getCurrentUser().put("userType", userType);
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null ) {
                    changeActivity();
                }

            }
        });


    }

    public void changeActivity() {
        if(ParseUser.getCurrentUser().get("userType").equals("rider")) {
            Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getApplicationContext(), ViewRequestsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        if(ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e==null) {
                        Log.i("Login", "Anonymous login successful");
                    } else {
                        Log.i("Login", "Anonymous login failed");
                    }
                }
            });
        } else {
            if (ParseUser.getCurrentUser().get("userType") != null) {
                changeActivity();
            }
        }
    }
}
