package com.android.flatfoot.apireviewdemo.lifecycle_01_basic;

import android.content.Context;
import android.location.LocationManager;

import com.android.flatfoot.apireviewdemo.DemoApplication;
import com.android.flatfoot.apireviewdemo.internal.SimpleLocationListener;
import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

@SuppressWarnings("MissingPermission")
public class LocationListener implements LifecycleObserver {

    public static void listenLocation(LifecycleProvider provider,
            SimpleLocationListener listener) {
        new LocationListener(provider, listener);
    }

    private android.location.LocationManager mLocationManager;
    private final SimpleLocationListener mListener;

    LocationListener(LifecycleProvider provider, SimpleLocationListener listener) {
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
        mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0,
                mListener);
    }


    @OnLifecycleEvent(Lifecycle.ON_PAUSE)
    void stop() {
        mLocationManager.removeUpdates(mListener);
        mLocationManager = null;
    }
}
