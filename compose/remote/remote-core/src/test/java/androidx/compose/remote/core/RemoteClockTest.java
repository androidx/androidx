/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Common tests for RemoteClock.TimeSnapshot implementations.
 */
public abstract class RemoteClockTest {

    protected abstract RemoteClock getClock(ZoneId zoneId);

    @Test
    public void testSnapshot() {
        // 2024-05-20T15:30:45.123 Z (UTC)
        long millis = Instant.parse("2024-05-20T15:30:45.123Z").toEpochMilli();
        ZoneId zoneId = ZoneId.of("UTC");
        RemoteClock clock = getClock(zoneId);
        RemoteClock.TimeSnapshot snapshot = clock.snapshot(millis);

        assertEquals(millis, snapshot.getMillis());
        assertEquals(2024, snapshot.getYear());
        assertEquals(5, snapshot.getMonth());
        assertEquals(20, snapshot.getDayOfMonth());
        assertEquals(141, snapshot.getDayOfYear()); // May 20th in 2024 (leap year) is 141st day
        assertEquals(15, snapshot.getHour());
        assertEquals(30, snapshot.getMinute());
        assertEquals(45, snapshot.getSecond());
        assertEquals(123, snapshot.getMillisOfSecond());
        assertEquals(1, snapshot.getDayOfWeek()); // Monday
        assertEquals(0, snapshot.getOffsetSeconds());
    }

    @Test
    public void testSnapshotWithOffset() {
        // 2024-05-20T15:30:45.123+02:00
        long millis = ZonedDateTime.parse(
                "2024-05-20T15:30:45.123+02:00").toInstant().toEpochMilli();
        ZoneId zoneId = ZoneId.of("Europe/Paris"); // UTC+2 in summer
        RemoteClock clock = getClock(zoneId);
        RemoteClock.TimeSnapshot snapshot = clock.snapshot(millis);

        assertEquals(millis, snapshot.getMillis());
        assertEquals(2024, snapshot.getYear());
        assertEquals(5, snapshot.getMonth());
        assertEquals(20, snapshot.getDayOfMonth());
        assertEquals(15, snapshot.getHour());
        assertEquals(30, snapshot.getMinute());
        assertEquals(45, snapshot.getSecond());
        assertEquals(7200, snapshot.getOffsetSeconds()); // 2 hours in seconds
    }
}
