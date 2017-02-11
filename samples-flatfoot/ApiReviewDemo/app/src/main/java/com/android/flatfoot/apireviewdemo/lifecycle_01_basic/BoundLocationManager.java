package com.android.flatfoot.apireviewdemo.lifecycle_01_basic;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;

import com.android.flatfoot.apireviewdemo.DemoApplication;
import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

public class BoundLocationManager {

    public static void bindLocationListenerIn(LifecycleProvider provider,
            LocationListener listener) {
        new BoundedLocationListener(provider, listener);
    }
}

@SuppressWarnings("MissingPermission")
class BoundedLocationListener implements LifecycleObserver {
    private LocationManager mLocationManager;
    private final LocationListener mListener;

    public BoundedLocationListener(LifecycleProvider provider, LocationListener listener) {
        provider.getLifecycle().addObserver(this);
        mListener = listener;
        if (provider.getLifecycle().getCurrentState() == Lifecycle.RESUMED) {
            start();
        }
    }

    @OnLifecycleEvent(Lifecycle.ON_RESUME)
    void start() {
        mLocationManager = (LocationManager) DemoApplication.context().getSystemService(
                Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mListener);
    }


    @OnLifecycleEvent(Lifecycle.ON_PAUSE)
    void stop() {
        mLocationManager.removeUpdates(mListener);
        mLocationManager = null;
    }
}
