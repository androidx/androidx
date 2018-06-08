/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.app;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.PermissionChecker;

import java.util.Calendar;

/**
 * Class which managing whether we are in the night or not.
 */
class TwilightManager {

    private static final String TAG = "TwilightManager";

    private static final int SUNRISE = 6; // 6am
    private static final int SUNSET = 22; // 10pm

    private static TwilightManager sInstance;

    static TwilightManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            context = context.getApplicationContext();
            sInstance = new TwilightManager(context,
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE));
        }
        return sInstance;
    }

    @VisibleForTesting
    static void setInstance(TwilightManager twilightManager) {
        sInstance = twilightManager;
    }

    private final Context mContext;
    private final LocationManager mLocationManager;

    private final TwilightState mTwilightState = new TwilightState();

    @VisibleForTesting
    TwilightManager(@NonNull Context context, @NonNull LocationManager locationManager) {
        mContext = context;
        mLocationManager = locationManager;
    }

    /**
     * Returns true we are currently in the 'night'.
     *
     * @return true if we are at night, false if the day.
     */
    boolean isNight() {
        final TwilightState state = mTwilightState;

        if (isStateValid()) {
            // If the current twilight state is still valid, use it
            return state.isNight;
        }

        // Else, we will try and grab the last known location
        final Location location = getLastKnownLocation();
        if (location != null) {
            updateState(location);
            return state.isNight;
        }

        Log.i(TAG, "Could not get last known location. This is probably because the app does not"
                + " have any location permissions. Falling back to hardcoded"
                + " sunrise/sunset values.");

        // If we don't have a location, we'll use our hardcoded sunrise/sunset values.
        // These aren't great, but it's better than nothing.
        Calendar calendar = Calendar.getInstance();
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour < SUNRISE || hour >= SUNSET;
    }

    @SuppressLint("MissingPermission") // permissions are checked for the needed call.
    private Location getLastKnownLocation() {
        Location coarseLoc = null;
        Location fineLoc = null;

        int permission = PermissionChecker.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission == PermissionChecker.PERMISSION_GRANTED) {
            coarseLoc = getLastKnownLocationForProvider(LocationManager.NETWORK_PROVIDER);
        }

        permission = PermissionChecker.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PermissionChecker.PERMISSION_GRANTED) {
            fineLoc = getLastKnownLocationForProvider(LocationManager.GPS_PROVIDER);
        }

        if (fineLoc != null && coarseLoc != null) {
            // If we have both a fine and coarse location, use the latest
            return fineLoc.getTime() > coarseLoc.getTime() ? fineLoc : coarseLoc;
        } else {
            // Else, return the non-null one (if there is one)
            return fineLoc != null ? fineLoc : coarseLoc;
        }
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    private Location getLastKnownLocationForProvider(String provider) {
        try {
            if (mLocationManager.isProviderEnabled(provider)) {
                return mLocationManager.getLastKnownLocation(provider);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to get last known location", e);
        }
        return null;
    }

    private boolean isStateValid() {
        return mTwilightState.nextUpdate > System.currentTimeMillis();
    }

    private void updateState(@NonNull Location location) {
        final TwilightState state = mTwilightState;
        final long now = System.currentTimeMillis();
        final TwilightCalculator calculator = TwilightCalculator.getInstance();

        // calculate yesterday's twilight
        calculator.calculateTwilight(now - DateUtils.DAY_IN_MILLIS,
                location.getLatitude(), location.getLongitude());
        final long yesterdaySunset = calculator.sunset;

        // calculate today's twilight
        calculator.calculateTwilight(now, location.getLatitude(), location.getLongitude());
        final boolean isNight = (calculator.state == TwilightCalculator.NIGHT);
        final long todaySunrise = calculator.sunrise;
        final long todaySunset = calculator.sunset;

        // calculate tomorrow's twilight
        calculator.calculateTwilight(now + DateUtils.DAY_IN_MILLIS,
                location.getLatitude(), location.getLongitude());
        final long tomorrowSunrise = calculator.sunrise;

        // Set next update
        long nextUpdate = 0;
        if (todaySunrise == -1 || todaySunset == -1) {
            // In the case the day or night never ends the update is scheduled 12 hours later.
            nextUpdate = now + 12 * DateUtils.HOUR_IN_MILLIS;
        } else {
            if (now > todaySunset) {
                nextUpdate += tomorrowSunrise;
            } else if (now > todaySunrise) {
                nextUpdate += todaySunset;
            } else {
                nextUpdate += todaySunrise;
            }
            // add some extra time to be on the safe side.
            nextUpdate += DateUtils.MINUTE_IN_MILLIS;
        }

        // Update the twilight state
        state.isNight = isNight;
        state.yesterdaySunset = yesterdaySunset;
        state.todaySunrise = todaySunrise;
        state.todaySunset = todaySunset;
        state.tomorrowSunrise = tomorrowSunrise;
        state.nextUpdate = nextUpdate;
    }

    /**
     * Describes whether it is day or night.
     */
    private static class TwilightState {
        boolean isNight;
        long yesterdaySunset;
        long todaySunrise;
        long todaySunset;
        long tomorrowSunrise;
        long nextUpdate;

        TwilightState() {
        }
    }
}
