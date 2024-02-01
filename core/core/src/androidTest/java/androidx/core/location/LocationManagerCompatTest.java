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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.os.ExecutorCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link androidx.core.location.LocationManagerCompat}.
 */
@SmallTest
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
        if (VERSION.SDK_INT >= 28) {
            isLocationEnabled = mLocationManager.isLocationEnabled();
        } else {
            isLocationEnabled = Settings.Secure.getInt(mContext.getContentResolver(), LOCATION_MODE,
                    LOCATION_MODE_OFF) != LOCATION_MODE_OFF;
        }

        assertEquals(isLocationEnabled, LocationManagerCompat.isLocationEnabled(mLocationManager));
    }

    @Test
    public void testHasProvider() {
        for (String provider : mLocationManager.getAllProviders()) {
            boolean hasProvider;
            if (VERSION.SDK_INT >= 31) {
                hasProvider = mLocationManager.hasProvider(provider);
            } else {
                hasProvider = mLocationManager.getProvider(provider) != null;
            }

            assertEquals(hasProvider, LocationManagerCompat.hasProvider(mLocationManager,
                    provider));
        }
    }

    @Test
    public void testGetCurrentLocation() {
        // can't do much to test this except check it doesn't crash
        CancellationSignal cs = new CancellationSignal();
        LocationManagerCompat.getCurrentLocation(mLocationManager,
                LocationManager.PASSIVE_PROVIDER, cs,
                ExecutorCompat.create(new Handler(Looper.getMainLooper())),
                location -> {});
        cs.cancel();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetCurrentLocation_compat() {
        // can't do much to test this except check it doesn't crash
        androidx.core.os.CancellationSignal cs = new androidx.core.os.CancellationSignal();
        LocationManagerCompat.getCurrentLocation(mLocationManager,
                LocationManager.PASSIVE_PROVIDER, cs,
                ExecutorCompat.create(new Handler(Looper.getMainLooper())),
                location -> {});
        cs.cancel();
    }

    @Test
    public void testRequestLocationUpdates_Executor() {
        // can't do much to test this except check it doesn't crash
        LocationRequestCompat request = new LocationRequestCompat.Builder(0).build();
        LocationListenerCompat listener1 = location -> {};
        LocationListenerCompat listener2 = location -> {};
        for (String provider : mLocationManager.getAllProviders()) {
            LocationManagerCompat.requestLocationUpdates(mLocationManager, provider, request,
                    directExecutor(), listener1);
            LocationManagerCompat.requestLocationUpdates(mLocationManager, provider, request,
                    directExecutor(), listener2);
            LocationManagerCompat.requestLocationUpdates(mLocationManager, provider, request,
                    directExecutor(), listener1);
        }
        LocationManagerCompat.removeUpdates(mLocationManager, listener1);
        LocationManagerCompat.removeUpdates(mLocationManager, listener2);
    }

    @Test
    public void testRequestLocationUpdates_Looper() {
        // can't do much to test this except check it doesn't crash
        LocationRequestCompat request = new LocationRequestCompat.Builder(0).build();
        LocationListenerCompat listener1 = location -> {};
        LocationListenerCompat listener2 = location -> {};
        for (String provider : mLocationManager.getAllProviders()) {
            LocationManagerCompat.requestLocationUpdates(mLocationManager, provider, request,
                    listener1, Looper.getMainLooper());
            LocationManagerCompat.requestLocationUpdates(mLocationManager, provider, request,
                    listener2, Looper.getMainLooper());
            LocationManagerCompat.requestLocationUpdates(mLocationManager, provider, request,
                    listener1, Looper.getMainLooper());
        }
        LocationManagerCompat.removeUpdates(mLocationManager, listener1);
        LocationManagerCompat.removeUpdates(mLocationManager, listener2);
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

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testRegisterGnssMeasurementsCallback_handler() {
        if (VERSION.SDK_INT == VERSION_CODES.Q) {
            // Q is very flaky
            return;
        }

        GnssMeasurementsEvent.Callback callback = new GnssMeasurementsEvent.Callback() {};

        // can't do much to test this except check it doesn't crash
        assertTrue(LocationManagerCompat.registerGnssMeasurementsCallback(mLocationManager,
                callback, new Handler(Looper.getMainLooper())));
        try {
            assertTrue(LocationManagerCompat.registerGnssMeasurementsCallback(mLocationManager,
                    Runnable::run,
                    callback));
            assertTrue(LocationManagerCompat.registerGnssMeasurementsCallback(mLocationManager,
                    callback, new Handler(Looper.getMainLooper())));
        } finally {
            LocationManagerCompat.unregisterGnssMeasurementsCallback(mLocationManager, callback);
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void testRegisterGnssMeasurementsCallback_executor() {
        if (VERSION.SDK_INT == VERSION_CODES.Q) {
            // Q is very flaky
            return;
        }

        GnssMeasurementsEvent.Callback callback = new GnssMeasurementsEvent.Callback() {};

        // can't do much to test this except check it doesn't crash
        assertTrue(LocationManagerCompat.registerGnssMeasurementsCallback(mLocationManager,
                Runnable::run,
                callback));
        try {
            assertTrue(LocationManagerCompat.registerGnssMeasurementsCallback(mLocationManager,
                    callback, new Handler(Looper.getMainLooper())));
            assertTrue(LocationManagerCompat.registerGnssMeasurementsCallback(mLocationManager,
                    Runnable::run,
                    callback));
        } finally {
            LocationManagerCompat.unregisterGnssMeasurementsCallback(mLocationManager, callback);
        }
    }
}
