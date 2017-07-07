/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.lifecycles.step4_solution;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

public class BoundLocationManager {
    public static void bindLocationListenerIn(LifecycleOwner lifecycleOwner,
                                              LocationListener listener, Context context) {
        new BoundLocationListener(lifecycleOwner, listener, context);
    }

    @SuppressWarnings("MissingPermission")
    static class BoundLocationListener implements LifecycleObserver {
        private final Context mContext;
        private LocationManager mLocationManager;
        private final LocationListener mListener;

        public BoundLocationListener(LifecycleOwner lifecycleOwner,
                                     LocationListener listener, Context context) {
            mContext = context;
            mListener = listener;
            lifecycleOwner.getLifecycle().addObserver(this);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        void addLocationListener() {
            // Note: Use the Fused Location Provider from Google Play Services instead.
            // https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi

            mLocationManager =
                    (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mListener);
            Log.d("BoundLocationMgr", "Listener added");

            // Force an update with the last location, if available.
            Location lastLocation = mLocationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                mListener.onLocationChanged(lastLocation);
            }
        }


        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void removeLocationListener() {
            if (mLocationManager == null) {
                return;
            }
            mLocationManager.removeUpdates(mListener);
            mLocationManager = null;
            Log.d("BoundLocationMgr", "Listener removed");
        }
    }
}
