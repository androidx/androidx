/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.flatfoot.apireviewdemo.lifecycle_01_basic;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.location.LocationManager;

import com.android.flatfoot.apireviewdemo.DemoApplication;
import com.android.flatfoot.apireviewdemo.internal.SimpleLocationListener;

@SuppressWarnings("MissingPermission")
class LocationListener implements LifecycleObserver {

    static void listenLocation(LifecycleOwner provider,
            SimpleLocationListener listener) {
        new LocationListener(provider, listener);
    }

    private android.location.LocationManager mLocationManager;
    private final SimpleLocationListener mListener;

    private LocationListener(LifecycleOwner provider, SimpleLocationListener listener) {
        provider.getLifecycle().addObserver(this);
        mListener = listener;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void start() {
        mLocationManager = (LocationManager) DemoApplication.context().getSystemService(
                Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0,
                mListener);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void stop() {
        mLocationManager.removeUpdates(mListener);
        mLocationManager = null;
    }
}
