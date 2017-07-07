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

package com.android.flatfoot.apireviewdemo.lifecycle_02_livedata;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.android.flatfoot.apireviewdemo.DemoApplication;
import com.android.flatfoot.apireviewdemo.internal.SimpleLocationListener;

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

    private SimpleLocationListener mListener = new SimpleLocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            setValue(location);
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
