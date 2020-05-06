package com.hunnainatif.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {
    ListView requestListView;
    ArrayList<String> requests = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    LocationManager locationManager;
    LocationListener locationListener;
    ArrayList<Double> riderLatitudes = new ArrayList<>();
    ArrayList<Double> riderLongitudes = new ArrayList<>();
    ArrayList<String> usernames = new ArrayList<>();

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);
        setTitle("Nearby Requests");

       requestListView = (ListView) findViewById(R.id.requestListView);
       arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
       requestListView.setAdapter(arrayAdapter);
       
       requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               if(ContextCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 23) {
                   Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                   if (riderLatitudes.size() > position && usernames.size() > position && riderLongitudes.size() > position && lastKnownLocation != null) {
                       Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                       intent.putExtra("username", usernames.get(position));
                       intent.putExtra("riderLatitude", riderLatitudes.get(position));
                       intent.putExtra("riderLongitude", riderLongitudes.get(position));
                       intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                       intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
                       startActivity(intent);

                   }
               }
               
           }
       });




        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if(Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(lastKnownLocation != null) {
                    updateListView(lastKnownLocation);
                }
            }
        }


    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastKnownLocation);
                }
            }
        }
    }

    public void updateListView(Location location) {
        if(location != null) {
            requests.clear();

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            query.whereNear("location", geoPointLocation);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(7);

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            requests.clear();
                            riderLatitudes.clear();
                            riderLongitudes.clear();
                            usernames.clear();
                            for(ParseObject object: objects) {
                                ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");
                                Double distanceToRequest = geoPointLocation.distanceInKilometersTo(requestLocation);
                                Double distanceRoundedOneDP = (double) Math.round(distanceToRequest * 10 ) / 10;
                                requests.add(distanceRoundedOneDP.toString() + " Km");
                                riderLatitudes.add(requestLocation.getLatitude());
                                riderLongitudes.add(requestLocation.getLongitude());
                                usernames.add(object.getString("username"));
                            }


                        } else {
                            requests.add("No Riders Nearby");
                        }
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

    }
}
