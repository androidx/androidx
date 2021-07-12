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

import static androidx.core.location.LocationRequestCompat.PASSIVE_INTERVAL;
import static androidx.core.location.LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY;
import static androidx.core.location.LocationRequestCompat.QUALITY_HIGH_ACCURACY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.location.LocationRequest;
import android.os.SystemClock;

import androidx.core.util.Preconditions;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Range;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SmallTest
public class LocationRequestCompatTest {

    private static Method sGetProviderMethod;
    private static Method sGetIntervalMethod;
    private static Method sGetFastestIntervalMethod;
    private static Method sGetExpireAtMethod;
    private static Method sGetNumUpdatesMethod;
    private static Method sGetSmallestDisplacementMethod;

    @Test
    public void testBuilder() {
        LocationRequestCompat.Builder builder = new LocationRequestCompat.Builder(0);

        assertEquals(QUALITY_BALANCED_POWER_ACCURACY, builder.build().getQuality());
        assertEquals(0, builder.build().getIntervalMillis());
        assertEquals(0, builder.build().getMinUpdateIntervalMillis());
        assertEquals(Long.MAX_VALUE, builder.build().getDurationMillis());
        assertEquals(Integer.MAX_VALUE, builder.build().getMaxUpdates());
        assertEquals(0, builder.build().getMinUpdateDistanceMeters(), 0);
        assertEquals(0, builder.build().getMaxUpdateDelayMillis());

        builder.setQuality(QUALITY_HIGH_ACCURACY);
        assertEquals(QUALITY_HIGH_ACCURACY, builder.build().getQuality());

        builder.setIntervalMillis(1000);
        assertEquals(1000, builder.build().getIntervalMillis());

        builder.setMinUpdateIntervalMillis(1500);
        assertEquals(1000, builder.build().getMinUpdateIntervalMillis());

        builder.setMinUpdateIntervalMillis(500);
        assertEquals(500, builder.build().getMinUpdateIntervalMillis());

        builder.clearMinUpdateIntervalMillis();
        assertEquals(1000, builder.build().getMinUpdateIntervalMillis());

        builder.setDurationMillis(1);
        assertEquals(1, builder.build().getDurationMillis());

        builder.setMaxUpdates(1);
        assertEquals(1, builder.build().getMaxUpdates());

        builder.setMinUpdateDistanceMeters(10);
        assertEquals(10, builder.build().getMinUpdateDistanceMeters(), 0);

        builder.setMaxUpdateDelayMillis(10000);
        assertEquals(10000, builder.build().getMaxUpdateDelayMillis());

        builder.setIntervalMillis(PASSIVE_INTERVAL);
        builder.setMinUpdateIntervalMillis(0);
        assertEquals(PASSIVE_INTERVAL, builder.build().getIntervalMillis());
    }

    @SdkSuppress(minSdkVersion = 19, maxSdkVersion = 30)
    @Test
    public void testConversion_19Plus() throws Exception {
        LocationRequestCompat.Builder builder = new LocationRequestCompat.Builder(0);

        assertEquals("test", getProvider(builder.build().toLocationRequest("test")));
        assertEquals(QUALITY_BALANCED_POWER_ACCURACY,
                builder.build().toLocationRequest("test").getQuality());
        assertEquals(0, getInterval(builder.build().toLocationRequest("test")));
        assertEquals(0, getFastestInterval(builder.build().toLocationRequest("test")));
        assertEquals(Long.MAX_VALUE, getExpireAt(builder.build().toLocationRequest()));
        assertEquals(Integer.MAX_VALUE, getNumUpdates(builder.build().toLocationRequest("test")));
        assertEquals(0, getSmallestDisplacement(builder.build().toLocationRequest("test")), 0);

        builder.setQuality(QUALITY_HIGH_ACCURACY);
        assertEquals(QUALITY_HIGH_ACCURACY, builder.build().toLocationRequest("test").getQuality());

        builder.setIntervalMillis(1000);
        assertEquals(1000, getInterval(builder.build().toLocationRequest("test")));

        builder.setMinUpdateIntervalMillis(1500);
        assertEquals(1000, getFastestInterval(builder.build().toLocationRequest("test")));

        builder.setMinUpdateIntervalMillis(500);
        assertEquals(500, getFastestInterval(builder.build().toLocationRequest("test")));

        builder.clearMinUpdateIntervalMillis();
        assertEquals(1000, getFastestInterval(builder.build().toLocationRequest("test")));

        builder.setDurationMillis(1);
        long time = SystemClock.elapsedRealtime();
        assertThat(getExpireAt(builder.build().toLocationRequest())).isIn(
                Range.closed(time - 1000, time));

        builder.setMaxUpdates(1);
        assertEquals(1, getNumUpdates(builder.build().toLocationRequest("test")));

        builder.setMinUpdateDistanceMeters(10);
        assertEquals(10, getSmallestDisplacement(builder.build().toLocationRequest("test")), 0);
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testConversion_31Plus() {
        LocationRequestCompat.Builder builder = new LocationRequestCompat.Builder(0);

        assertEquals(QUALITY_BALANCED_POWER_ACCURACY,
                builder.build().toLocationRequest().getQuality());
        assertEquals(0, builder.build().toLocationRequest().getIntervalMillis());
        assertEquals(0, builder.build().toLocationRequest().getMinUpdateIntervalMillis());
        assertEquals(Long.MAX_VALUE, builder.build().toLocationRequest().getDurationMillis());
        assertEquals(Integer.MAX_VALUE, builder.build().toLocationRequest().getMaxUpdates());
        assertEquals(0, builder.build().toLocationRequest().getMinUpdateDistanceMeters(), 0);
        assertEquals(0, builder.build().toLocationRequest().getMaxUpdateDelayMillis());

        builder.setQuality(QUALITY_HIGH_ACCURACY);
        assertEquals(QUALITY_HIGH_ACCURACY, builder.build().toLocationRequest().getQuality());

        builder.setIntervalMillis(1000);
        assertEquals(1000, builder.build().toLocationRequest().getIntervalMillis());

        builder.setMinUpdateIntervalMillis(1500);
        assertEquals(1000, builder.build().toLocationRequest().getMinUpdateIntervalMillis());

        builder.setMinUpdateIntervalMillis(500);
        assertEquals(500, builder.build().toLocationRequest().getMinUpdateIntervalMillis());

        builder.clearMinUpdateIntervalMillis();
        assertEquals(1000, builder.build().toLocationRequest().getMinUpdateIntervalMillis());

        builder.setDurationMillis(1);
        assertEquals(1, builder.build().toLocationRequest().getDurationMillis());

        builder.setMaxUpdates(1);
        assertEquals(1, builder.build().toLocationRequest().getMaxUpdates());

        builder.setMinUpdateDistanceMeters(10);
        assertEquals(10, builder.build().toLocationRequest().getMinUpdateDistanceMeters(), 0);

        builder.setMaxUpdateDelayMillis(10000);
        assertEquals(10000, builder.build().toLocationRequest().getMaxUpdateDelayMillis());

        builder.setMinUpdateIntervalMillis(1000);
        builder.setIntervalMillis(PASSIVE_INTERVAL);
        assertEquals(PASSIVE_INTERVAL,
                builder.build().toLocationRequest().getIntervalMillis());
    }

    private static String getProvider(LocationRequest request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (sGetProviderMethod == null) {
            sGetProviderMethod = LocationRequest.class.getDeclaredMethod("getProvider");
            sGetProviderMethod.setAccessible(true);
        }

        return (String) sGetProviderMethod.invoke(request);
    }

    private static long getInterval(LocationRequest request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (sGetIntervalMethod == null) {
            sGetIntervalMethod = LocationRequest.class.getDeclaredMethod("getInterval");
            sGetIntervalMethod.setAccessible(true);
        }

        return (Long) Preconditions.checkNotNull(sGetIntervalMethod.invoke(request));
    }

    private static long getFastestInterval(LocationRequest request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (sGetFastestIntervalMethod == null) {
            sGetFastestIntervalMethod = LocationRequest.class.getDeclaredMethod(
                    "getFastestInterval");
            sGetFastestIntervalMethod.setAccessible(true);
        }

        return (Long) Preconditions.checkNotNull(sGetFastestIntervalMethod.invoke(request));
    }

    private static long getExpireAt(LocationRequest request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (sGetExpireAtMethod == null) {
            sGetExpireAtMethod = LocationRequest.class.getDeclaredMethod("getExpireAt");
            sGetExpireAtMethod.setAccessible(true);
        }

        return (Long) Preconditions.checkNotNull(sGetExpireAtMethod.invoke(request));
    }

    private static int getNumUpdates(LocationRequest request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (sGetNumUpdatesMethod == null) {
            sGetNumUpdatesMethod = LocationRequest.class.getDeclaredMethod("getNumUpdates");
            sGetNumUpdatesMethod.setAccessible(true);
        }

        return (Integer) Preconditions.checkNotNull(sGetNumUpdatesMethod.invoke(request));
    }

    private static float getSmallestDisplacement(LocationRequest request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (sGetSmallestDisplacementMethod == null) {
            sGetSmallestDisplacementMethod = LocationRequest.class.getDeclaredMethod(
                    "getSmallestDisplacement");
            sGetSmallestDisplacementMethod.setAccessible(true);
        }

        return (Float) Preconditions.checkNotNull(sGetSmallestDisplacementMethod.invoke(request));
    }
}
