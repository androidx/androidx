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

import androidx.core.location.LocationCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AltitudeConverterCompatTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testAddMslAltitudeToLocation_expectedBehavior() throws IOException {
        // Interpolates in boundary region (bffffc).
        Location location = new Location("");
        location.setLatitude(-35.334815);
        location.setLongitude(-45);
        location.setAltitude(-1);
        LocationCompat.setVerticalAccuracyMeters(location, 1);
        // Loads data from raw assets.
        AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location);
        assertThat(LocationCompat.getMslAltitudeMeters(location)).isWithin(2).of(5.0622);
        assertThat(LocationCompat.getMslAltitudeAccuracyMeters(location)).isGreaterThan(1f);
        assertThat(LocationCompat.getMslAltitudeAccuracyMeters(location)).isLessThan(1.1f);

        // Again interpolates at same location to assert no loading from raw assets. Also checks
        // behavior w.r.t. invalid vertical accuracy.
        location = new Location("");
        location.setLatitude(-35.334815);
        location.setLongitude(-45);
        location.setAltitude(-1);
        LocationCompat.setVerticalAccuracyMeters(location, -1); // Invalid vertical accuracy
        // Results in same outcome.
        AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location);
        assertThat(LocationCompat.getMslAltitudeMeters(location)).isWithin(2).of(5.0622);
        assertThat(LocationCompat.hasMslAltitudeAccuracy(location)).isFalse();

        // Interpolates out of boundary region, e.g., Hawaii.
        location = new Location("");
        location.setLatitude(19.545519);
        location.setLongitude(-155.998774);
        location.setAltitude(-1);
        LocationCompat.setVerticalAccuracyMeters(location, 1);
        // Loads data from raw assets.
        AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location);
        assertThat(LocationCompat.getMslAltitudeMeters(location)).isWithin(2).of(-19.2359);
        assertThat(LocationCompat.getMslAltitudeAccuracyMeters(location)).isGreaterThan(1f);
        assertThat(LocationCompat.getMslAltitudeAccuracyMeters(location)).isLessThan(1.1f);

        // The following round out test coverage for boundary regions.

        location = new Location("");
        location.setLatitude(-35.229154);
        location.setLongitude(44.925335);
        location.setAltitude(-1);
        AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location);
        assertThat(LocationCompat.getMslAltitudeMeters(location)).isWithin(2).of(-34.1913);

        location = new Location("");
        location.setLatitude(-35.334815);
        location.setLongitude(45);
        location.setAltitude(-1);
        AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location);
        assertThat(LocationCompat.getMslAltitudeMeters(location)).isWithin(2).of(-34.2258);

        location = new Location("");
        location.setLatitude(35.229154);
        location.setLongitude(-44.925335);
        location.setAltitude(-1);
        AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location);
        assertThat(LocationCompat.getMslAltitudeMeters(location)).isWithin(2).of(-11.0691);
    }

    @Test
    public void testAddMslAltitudeToLocation_invalidLatitudeThrows() {
        Location location = new Location("");
        location.setLongitude(-44.962683);
        location.setAltitude(-1);

        location.setLatitude(Double.NaN);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));

        location.setLatitude(91);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));

        location.setLatitude(-91);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));
    }

    @Test
    public void testAddMslAltitudeToLocation_invalidLongitudeThrows() {
        Location location = new Location("");
        location.setLatitude(-35.246789);
        location.setAltitude(-1);

        location.setLongitude(Double.NaN);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));

        location.setLongitude(181);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));

        location.setLongitude(-181);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));
    }

    @Test
    public void testAddMslAltitudeToLocation_invalidAltitudeThrows() {
        Location location = new Location("");
        location.setLatitude(-35.246789);
        location.setLongitude(-44.962683);

        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));

        location.setAltitude(Double.NaN);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));

        location.setAltitude(Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class,
                () -> AltitudeConverterCompat.addMslAltitudeToLocation(mContext, location));
    }
}
