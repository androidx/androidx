/*
 * Copyright 2020 The Android Open Source Project
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

import static android.provider.Settings.Secure.LOCATION_MODE;
import static android.provider.Settings.Secure.LOCATION_MODE_OFF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.os.CancellationSignal;
import androidx.core.os.ExecutorCompat;
import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link androidx.core.location.LocationManagerCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LocationManagerCompatTest {

    private Context mContext;
    private LocationManager mLocationManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    @Test
    public void testIsLocationEnabled() {
        boolean isLocationEnabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLocationEnabled = mLocationManager.isLocationEnabled();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            isLocationEnabled = Settings.Secure.getInt(mContext.getContentResolver(), LOCATION_MODE,
                    LOCATION_MODE_OFF) != LOCATION_MODE_OFF;
        } else {
            isLocationEnabled = !TextUtils.isEmpty(
                    Settings.Secure.getString(mContext.getContentResolver(),
                            Settings.Secure.LOCATION_PROVIDERS_ALLOWED));
        }

        assertEquals(isLocationEnabled, LocationManagerCompat.isLocationEnabled(mLocationManager));
    }

    @Test
    public void testGetCurrentLocation() {
        // can't do much to test this except check it doesn't crash
        CancellationSignal cs = new CancellationSignal();
        LocationManagerCompat.getCurrentLocation(mLocationManager,
                LocationManager.PASSIVE_PROVIDER, cs,
                ExecutorCompat.create(new Handler(Looper.getMainLooper())),
                new Consumer<Location>() {
                    @Override
                    public void accept(Location location) {}
                });
        cs.cancel();
    }

    @Test
    public void testGetGnssHardwareModelName() {
        // can't do much to test this except check it doesn't crash
        LocationManagerCompat.getGnssHardwareModelName(mLocationManager);
    }

    @Test
    public void testGetGnssYearOfHardware() {
        // can't do much to test this except check it doesn't crash
        assertTrue(LocationManagerCompat.getGnssYearOfHardware(mLocationManager) >= 0);
    }
}
