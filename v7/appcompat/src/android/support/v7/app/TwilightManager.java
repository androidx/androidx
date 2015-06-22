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

package android.support.v7.app;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.Calendar;

/**
 * Class which managing whether we are in the night or not.
 */
class TwilightManager {

    private static final String TAG = "TwilightManager";

    private static final int SUNRISE = 6; // 6am
    private static final int SUNSET = 22; // 10pm

    private static final TwilightState sTwilightState = new TwilightState();

    private final Context mContext;
    private final LocationManager mLocationManager;

    TwilightManager(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Returns true we are currently in the 'night'.
     *
     * @return true if we are at night, false if the day.
     */
    boolean isNight() {
        final TwilightState state = sTwilightState;

        if (isStateValid(state)) {
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

    private Location getLastKnownLocation() {
        Location coarseLocation = null;
        Location fineLocation = null;

        int permission = PermissionChecker.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PermissionChecker.PERMISSION_GRANTED) {
            coarseLocation = getLastKnownLocationForProvider(LocationManager.NETWORK_PROVIDER);
        }

        permission = PermissionChecker.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission == PermissionChecker.PERMISSION_GRANTED) {
            fineLocation = getLastKnownLocationForProvider(LocationManager.GPS_PROVIDER);
        }

        if (coarseLocation != null && fineLocation != null) {
            // If we have both a fine and coarse location, use the latest
            if (fineLocation.getTime() > coarseLocation.getTime()) {
                return fineLocation;
            } else {
                return coarseLocation;
            }
        } else {
            // Else, return the non-null one (if there is one)
            return fineLocation != null ? fineLocation : coarseLocation;
        }
    }

    private Location getLastKnownLocationForProvider(String provider) {
        if (mLocationManager != null) {
            try {
                if (mLocationManager.isProviderEnabled(provider)) {
                    return mLocationManager.getLastKnownLocation(provider);
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to get last known location", e);
            }
        }
        return null;
    }

    private boolean isStateValid(TwilightState state) {
        return state != null && state.nextUpdate > System.currentTimeMillis();
    }

    private void updateState(@NonNull Location location) {
        final TwilightState state = sTwilightState;
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
    }
}
