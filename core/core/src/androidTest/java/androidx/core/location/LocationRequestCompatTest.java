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

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public class LocationRequestCompatTest {

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
}
