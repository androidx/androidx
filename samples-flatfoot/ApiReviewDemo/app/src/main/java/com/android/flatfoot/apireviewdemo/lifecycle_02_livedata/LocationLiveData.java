package com.android.flatfoot.apireviewdemo.lifecycle_02_livedata;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.android.flatfoot.apireviewdemo.DemoApplication;
import com.android.support.lifecycle.LiveData;

@SuppressWarnings("MissingPermission")
public class LocationLiveData extends LiveData<Location> {

    private static LocationLiveData sInstance;

    public static LocationLiveData getInstance() {
        if (sInstance == null) {
            sInstance = new LocationLiveData();
        }
        return sInstance;
    }

    private LocationManager mLocationManager;

    private LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            setValue(location);
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

    private LocationLiveData() {
        mLocationManager = (LocationManager) DemoApplication.context().getSystemService(
                Context.LOCATION_SERVICE);
    }

    @Override
    protected void onActive() {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mListener);
    }

    @Override
    protected void onInactive() {
        mLocationManager.removeUpdates(mListener);
    }
}
