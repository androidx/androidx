/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.location.altitude;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.location.Location;

import androidx.arch.core.util.Function;
import androidx.core.location.LocationCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AltitudeConverterCompatTest {

    private Location mValidLocation;
    private Context mContext;

    private static void assertEquals(Location actual, Location expected) {
        assertThat(actual.getLatitude()).isEqualTo(expected.getLatitude());
        assertThat(actual.getLongitude()).isEqualTo(expected.getLongitude());
        assertEquals(actual, expected, Location::hasAltitude, Location::getAltitude);
        assertEquals(
                actual,
                expected,
                LocationCompat::hasVerticalAccuracy,
                LocationCompat::getVerticalAccuracyMeters);
        assertEquals(
                actual, expected, LocationCompat::hasMslAltitude,
                LocationCompat::getMslAltitudeMeters);
        assertEquals(
                actual,
                expected,
                LocationCompat::hasMslAltitudeAccuracy,
                LocationCompat::getMslAltitudeAccuracyMeters);
    }

    private static <T> void assertEquals(
            Location actual,
            Location expected,
            Function<Location, Boolean> has,
            Function<Location, T> get) {
        assertThat(has.apply(actual)).isEqualTo(has.apply(expected));
        if (has.apply(expected)) {
            assertThat(get.apply(actual)).isEqualTo(get.apply(expected));
        }
    }

    @Before
    public void setUp() {
        mValidLocation = new Location("");
        mValidLocation.setLatitude(-90);
        mValidLocation.setLongitude(180);
        mValidLocation.setAltitude(-1);
        LocationCompat.setVerticalAccuracyMeters(mValidLocation, 1);

        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testAddMslAltitude_validLocationThrows() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(mValidLocation)));
    }

    @Test
    public void testAddMslAltitude_invalidLatitudeThrows() {
        Location location = new Location(mValidLocation);

        location.setLatitude(Double.NaN);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));

        location.setLatitude(91);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));

        location.setLatitude(-91);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));
    }

    @Test
    public void testAddMslAltitude_invalidLongitudeThrows() {
        Location location = new Location(mValidLocation);

        location.setLongitude(Double.NaN);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));

        location.setLongitude(181);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));

        location.setLongitude(-181);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));
    }

    @Test
    public void testAddMslAltitude_invalidAltitudeThrows() {
        Location location = new Location(mValidLocation);

        location.removeAltitude();
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));

        location.setAltitude(Double.NaN);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));

        location.setAltitude(Double.POSITIVE_INFINITY);
        assertThrows(
                IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeAsync(mContext,
                        new Location(location)));
    }
}
