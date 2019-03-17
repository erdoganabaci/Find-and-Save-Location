package com.locationfinder.firebase.findsavedlocation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    String address;
    Double latituteDouble;
    Double longituteDouble;
    String latituteString;
    String longituteString;
    EditText mapText;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        myRef = firebaseDatabase.getReference();
        mapText = findViewById(R.id.map_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_place, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save_place) {
            UUID uuid = UUID.randomUUID();
            myRef.child("Locations").child(uuid.toString()).child("name").setValue(address);
            myRef.child("Locations").child(uuid.toString()).child("latitute").setValue(latituteString);
            myRef.child("Locations").child(uuid.toString()).child("longitute").setValue(longituteString);
            Toast.makeText(this, "Database Kayıt Edildi", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == android.R.id.home) {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }
        return super.onOptionsItemSelected(item);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        Intent intent = getIntent();
        String info = intent.getStringExtra("info"); //take info wheter new add place or not
        if (info.matches("new")) {

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    SharedPreferences sharedPreferences = MapsActivity.this.getSharedPreferences("com.locationfinder.aliburak.findsavedlocation_aliburak", MODE_PRIVATE);
                    boolean firstTimeCheck = sharedPreferences.getBoolean("noFirstTime", false);
                    if (firstTimeCheck == false) { //no need location update every time.
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        sharedPreferences.edit().putBoolean("noFirstTime", true).apply();
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            };

            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener); //take location when user change position.
                    mMap.setMyLocationEnabled(true);
                    mMap.clear();
                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation != null) {
                        LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                    }

                }
            } else { //Api 23's below no need take permission.
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener); //take location when user change position.
                mMap.setMyLocationEnabled(true);
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null) {
                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                }

            }

        } else {
            mMap.clear();
            int position = intent.getIntExtra("position", 0);
            LatLng locationFromMainActivity = new LatLng(MainActivity.locations.get(position).latitude, MainActivity.locations.get(position).longitude); //take mainactivity with static which index cliked.
            String placeNameFormMainActivity = MainActivity.names.get(position);
            mMap.addMarker(new MarkerOptions().title(placeNameFormMainActivity).position(locationFromMainActivity).icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_icon)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationFromMainActivity, 15));
            //Toast.makeText(getApplicationContext(),"Alınan Pozisyon :"+position,Toast.LENGTH_SHORT).show();


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            if (requestCode == 1) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    mMap.setMyLocationEnabled(true);
                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation != null) {
                        LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        address = "";
        latituteDouble = latLng.latitude;
        longituteDouble = latLng.longitude;
        latituteString = latituteDouble.toString();
        longituteString = longituteDouble.toString();
        if (!mapText.getText().toString().equals("")) {
            address += mapText.getText().toString();
        }
        try {
            if (mapText.getText().toString().equals("")) {

                List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addressList != null && addressList.size() > 0) {
                    if (addressList.get(0).getThoroughfare() != null) {
                        address += addressList.get(0).getThoroughfare();
                        if (addressList.get(0).getSubThoroughfare() != null) {
                            address += addressList.get(0).getSubThoroughfare();
                        }
                    }
                } else {
                    address = "New Place";
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        mMap.clear();
        mMap.addMarker(new MarkerOptions().title(address).position(latLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_icon)));
        Toast.makeText(this, "New Place Created", Toast.LENGTH_LONG).show();


    }
}

