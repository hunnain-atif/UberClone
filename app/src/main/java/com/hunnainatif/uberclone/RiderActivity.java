package com.hunnainatif.uberclone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Boolean requestActive = false;
    Boolean driverActive = false;
    Button uberButton;
    Handler handler = new Handler();
    TextView driverStatus;

    public void callUber(View view) {
        if (requestActive) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null && objects.size() > 0) {

                        for(ParseObject object : objects) {
                            object.deleteInBackground();
                        }
                        requestActive = false;
                        uberButton.setText("Call An Uber");
                    }
                }
            });

        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    request.put("location", parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                uberButton = (Button) findViewById(R.id.uberButton);
                                uberButton.setText("Cancel Uber");
                                requestActive = true;
                                checkDriverStatus();
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Could not find loaction, Please try again", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    public void logout(View view) {
        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);

    }

    public void checkDriverStatus() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if(objects.size() > 0) {
                        driverActive = true;
                        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                        userQuery.whereEqualTo("username", objects.get(0).getString("driverUsername"));
                        userQuery.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> objects, ParseException e) {
                                if(e == null) {
                                    if (objects.size() > 0) {

                                        ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");
                                        if(Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(RiderActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                            if(lastKnownLocation != null) {
                                                ParseGeoPoint riderLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                                Double distanceToRequest = driverLocation.distanceInKilometersTo(riderLocation);

                                                if(distanceToRequest < 1) {
                                                    driverStatus.setText("YOUR DRIVER IS HERE");

                                                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                                    query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                                    query.findInBackground(new FindCallback<ParseObject>() {
                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            if (e==null) {
                                                                for(ParseObject object: objects)
                                                                    object.deleteInBackground();
                                                            }
                                                        }
                                                    });

                                                    handler.postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            driverStatus.setText("");
                                                            uberButton.setVisibility(View.VISIBLE);
                                                            uberButton.setText("Call An Uber");
                                                            requestActive = false;
                                                            driverActive = false;
                                                        }
                                                    }, 7000);

                                                } else {
                                                    Double distanceRoundedOneDP = (double) Math.round(distanceToRequest * 10) / 10;
                                                    driverStatus.setText("Your driver is " + distanceRoundedOneDP + " kilometers away!");

                                                    LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                                    LatLng riderLocationLatLng = new LatLng(riderLocation.getLatitude(), riderLocation.getLongitude());

                                                    ArrayList<Marker> markers = new ArrayList<>();
                                                    mMap.clear();
                                                    markers.add(mMap.addMarker(new MarkerOptions().position(riderLocationLatLng).title("Your Location")));
                                                    markers.add(mMap.addMarker(new MarkerOptions()
                                                            .position(driverLocationLatLng).title("Rider Location")
                                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));

                                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                    for (Marker marker : markers) {
                                                        builder.include(marker.getPosition());
                                                    }

                                                    LatLngBounds bounds = builder.build();

                                                    int padding = 50;
                                                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);


                                                    // Add a marker in Sydney and move the camera
                                                    mMap.animateCamera(cu);

                                                    uberButton.setVisibility(View.INVISIBLE);

                                                    handler.postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            checkDriverStatus();
                                                        }
                                                    }, 5000);
                                                }
                                            }
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateMapUI(lastKnownLocation);
                }
            }
        }
    }

    public void updateMapUI(Location location) {

        if(driverActive == false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        uberButton = (Button) findViewById(R.id.uberButton);
        driverStatus = (TextView) findViewById(R.id.driverInfoTextView);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() > 0) {
                        requestActive = true;
                        uberButton.setText("Cancel Uber");
                        checkDriverStatus();
                    }
                }
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
               updateMapUI(location);

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
                    updateMapUI(lastKnownLocation);
                }
            }
        }


    }
}
