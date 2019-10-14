/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.location;

import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;

/**
 * Helper for accessing features in {@link LocationManager}.
 */
public final class LocationManagerCompat {

    /**
     * Returns the current enabled/disabled state of location.
     *
     * @return true if location is enabled and false if location is disabled.
     */
    public static boolean isLocationEnabled(@NonNull LocationManager locationManager) {
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            // NOTE: for KitKat and above, it's preferable to use the proper API at the time to get
            // the location mode, Secure.getInt(context, LOCATION_MODE, LOCATION_MODE_OFF). however,
            // this requires a context we don't have directly (we could either ask the client to
            // pass one in, or use reflection to get it from the location manager), and since KitKat
            // and above remained backwards compatible, we can fallback to pre-kitkat behavior.

            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }

    private LocationManagerCompat() {}
}
