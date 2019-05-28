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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Tests for {@link Time} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TimeTest {
    public static final ZonedDateTime TEST_ZONED_DATE_TIME = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(12345), ZoneId.of("EST"));
    public static final ZonedDateTime TEST_ZONED_DATE_TIME_2 = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(123456), ZoneId.of("PST"));

    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        Time expected = new Time(TEST_ZONED_DATE_TIME);

        assertEquals(expected, new Time(TEST_ZONED_DATE_TIME));
        assertEquals(expected.getTime(), TEST_ZONED_DATE_TIME);
        assertNotEquals(expected, new Time(TEST_ZONED_DATE_TIME_2));
        assertNotEquals(expected.getTime(), TEST_ZONED_DATE_TIME_2);

        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault()),
                new Time().getTime());

        assertEquals(expected.hashCode(), new Time(TEST_ZONED_DATE_TIME).hashCode());
        assertNotEquals(expected.hashCode(), new Time(TEST_ZONED_DATE_TIME_2).hashCode());
    }

    /**
     * Test null on {@link Time} constructor
     */
    @Test(expected = NullPointerException.class)
    public void nullability() {
        new Time(null);
    }
}
