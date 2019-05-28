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

package androidx.car.cluster.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Tests for {@link Destination} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class DestinationTest {
    private static final String TEST_TITLE = "foo";
    private static final String TEST_ADDRESS = "bar";
    private static final Distance TEST_DISTANCE = new Distance(10, "10.0", Distance.Unit.METERS);
    private static final ZonedDateTime TEST_ETA = ZonedDateTime.of(2019, 1, 9, 10, 9, 20, 0,
            ZoneId.of("UTC"));
    private static final LatLng TEST_LOCATION = new LatLng(20.1, 30.2);
    private static final String TEST_FORMATTED_ETA = "1h 30min";

    /**
     * Tests that creating a {@link Destination} using a default constructor produces an instance
     * that complies with the {@link androidx.annotation.NonNull} annotations.
     */
    @Test
    public void nullability_defaultConstructor() {
        Destination destination = new Destination();
        assertEquals("", destination.getTitle());
        assertEquals("", destination.getAddress());
        assertNull(destination.getDistance());
        assertNull(destination.getEta());
        assertNull(destination.getLocation());
        assertEquals(Destination.Traffic.UNKNOWN, destination.getTraffic());
        assertEquals("", destination.getFormattedEta());
    }

    /**
     * Basic equality check
     */
    @Test
    public void equality_sameContentIsConsideredEqual() {
        Destination expected = createSampleDestination();
        assertEquals(expected, createSampleDestination());
        assertEquals(expected.hashCode(), createSampleDestination().hashCode());
    }

    /**
     * Basic inequality check
     */
    @Test
    public void equality_differentContentIsConsideredNotEqual() {
        assertNotEquals(createSampleDestination(), new Destination.Builder().build());
    }

    /**
     * Builder doesn't accept null title
     */
    @Test(expected = NullPointerException.class)
    public void builder_titleCantBeNull() {
        new Destination.Builder().setTitle(null).build();
    }

    /**
     * Builder doesn't accept null address
     */
    @Test(expected = NullPointerException.class)
    public void builder_addressCantBeNull() {
        new Destination.Builder().setAddress(null).build();
    }

    /**
     * Builder doesn't accept null delay
     */
    @Test(expected = NullPointerException.class)
    public void builder_delayCantBeNull() {
        new Destination.Builder().setTraffic(null).build();
    }

    /**
     * Builder doesn't accept null formatted eta
     */
    @Test(expected = NullPointerException.class)
    public void builder_formattedEtaCantBeNull() {
        new Destination.Builder().setFormattedEta(null).build();
    }

    /**
     * Tests that creating a {@link Destination} using the build without setting any data
     * produces an instance that complies with the {@link androidx.annotation.NonNull} annotations.
     */
    @Test
    public void builder_emptyMatchesNullAnnotations() {
        Destination destination = new Destination.Builder().build();
        assertEquals("", destination.getTitle());
        assertEquals("", destination.getAddress());
        assertNull(destination.getDistance());
        assertNull(destination.getEta());
        assertNull(destination.getLocation());
        assertEquals(Destination.Traffic.UNKNOWN, destination.getTraffic());
        assertEquals("", destination.getFormattedEta());
    }

    /**
     * Returns a sample {@link Destination} instance for testing.
     */
    public static Destination createSampleDestination() {
        return new Destination.Builder()
                .setAddress(TEST_ADDRESS)
                .setTitle(TEST_TITLE)
                .setTraffic(Destination.Traffic.LOW)
                .setDistance(TEST_DISTANCE)
                .setEta(TEST_ETA)
                .setLocation(TEST_LOCATION)
                .setFormattedEta(TEST_FORMATTED_ETA)
                .build();
    }
}
