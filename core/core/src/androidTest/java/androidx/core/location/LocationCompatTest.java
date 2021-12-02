/*
 * Copyright 2021 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import android.location.Location;
import android.os.Build;
import android.os.SystemClock;

import androidx.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public class LocationCompatTest {

    private static final long MAX_ELAPSED_REALTIME_OFFSET_MS = 10;

    @Test
    public void testGetElapsedRealtimeNanos() {
        long locationElapsedRealtimeNs;
        if (Build.VERSION.SDK_INT >= 17) {
            locationElapsedRealtimeNs = SystemClock.elapsedRealtimeNanos();
        } else {
            locationElapsedRealtimeNs = MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
        }

        Location location = new Location("");
        if (Build.VERSION.SDK_INT >= 17) {
            location.setElapsedRealtimeNanos(locationElapsedRealtimeNs);
        }
        location.setTime(System.currentTimeMillis());

        assertTrue(NANOSECONDS.toMillis(Math.abs(
                LocationCompat.getElapsedRealtimeNanos(location) - locationElapsedRealtimeNs))
                < MAX_ELAPSED_REALTIME_OFFSET_MS);
    }

    @Test
    public void testGetElapsedRealtimeMillis() {
        long locationElapsedRealtimeMs = SystemClock.elapsedRealtime();

        Location location = new Location("");
        if (Build.VERSION.SDK_INT >= 17) {
            location.setElapsedRealtimeNanos(MILLISECONDS.toNanos(locationElapsedRealtimeMs));
        }
        location.setTime(System.currentTimeMillis());

        assertTrue(Math.abs(
                LocationCompat.getElapsedRealtimeMillis(location) - locationElapsedRealtimeMs)
                < MAX_ELAPSED_REALTIME_OFFSET_MS);
    }

    @Test
    public void testVerticalAccuracy() {
        Location location = new Location("");
        assertFalse(LocationCompat.hasVerticalAccuracy(location));
        LocationCompat.setVerticalAccuracyMeters(location, 1f);
        assertTrue(LocationCompat.hasVerticalAccuracy(location));
        assertEquals(1f, LocationCompat.getVerticalAccuracyMeters(location), 0f);
    }

    @Test
    public void testSpeedAccuracy() {
        Location location = new Location("");
        assertFalse(LocationCompat.hasSpeedAccuracy(location));
        LocationCompat.setSpeedAccuracyMetersPerSecond(location, 1f);
        assertTrue(LocationCompat.hasSpeedAccuracy(location));
        assertEquals(1f, LocationCompat.getSpeedAccuracyMetersPerSecond(location), 0f);
    }

    @Test
    public void testBearingAccuracy() {
        Location location = new Location("");
        assertFalse(LocationCompat.hasBearingAccuracy(location));
        LocationCompat.setBearingAccuracyDegrees(location, 1f);
        assertTrue(LocationCompat.hasBearingAccuracy(location));
        assertEquals(1f, LocationCompat.getBearingAccuracyDegrees(location), 0f);
    }

    @Test
    public void testMock() {
        Location location = new Location("");
        assertFalse(LocationCompat.isMock(location));
        LocationCompat.setMock(location, true);
        assertTrue(LocationCompat.isMock(location));
        LocationCompat.setMock(location, false);
        assertFalse(LocationCompat.isMock(location));
    }
}
