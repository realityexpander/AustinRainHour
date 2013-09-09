package com.realityexpander.austinrainhour;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by realityexpander on 8/30/13.
 */
// http://developer.android.com/guide/topics/location/strategies.html

public class UserLocationManager implements LocationListener {

    private double oldLong, oldLat;
    private MainActivity mMainActivity;

    public UserLocationManager(MainActivity m) {
        mMainActivity = m;

        LocationManager locationManager = (LocationManager)mMainActivity.getSystemService(Context.LOCATION_SERVICE);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } catch (Exception e) {
            e.printStackTrace();
        }


        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownLocation == null)
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mMainActivity.setLocation(lastKnownLocation);

    }

    @Override
    public void onLocationChanged(Location loc) {

        Log.i("UserLocationManager=", loc.getLatitude() + "," + loc.getLongitude());

        // TODO Add time >15 min, do update
        // Spawn notification if inclement weather is within next hour
        if (loc.getLongitude() != oldLong || loc.getLatitude() != oldLat) {
            oldLong = loc.getLongitude();
            oldLat = loc.getLatitude();
            mMainActivity.setLocation(loc);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
}
