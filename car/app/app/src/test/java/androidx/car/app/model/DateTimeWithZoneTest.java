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

package androidx.car.app.model;

import static androidx.car.app.TestUtils.assertDateTimeWithZoneEquals;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Tests for {@link DateTimeWithZone}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class DateTimeWithZoneTest {
    @Test
    @SuppressWarnings("JdkObsolete")
    public void create() {
        GregorianCalendar calendar = new GregorianCalendar(2020, 4, 15, 2, 57, 0);
        Date date = calendar.getTime();
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        long timeSinceEpochMillis = date.getTime();
        long timeZoneOffsetSeconds = MILLISECONDS.toSeconds(
                timeZone.getOffset(timeSinceEpochMillis));
        String zoneShortName = "PST";

        DateTimeWithZone dateTimeWithZone =
                DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        zoneShortName);

        assertThat(dateTimeWithZone.getTimeSinceEpochMillis()).isEqualTo(timeSinceEpochMillis);
        assertThat(dateTimeWithZone.getZoneOffsetSeconds()).isEqualTo(timeZoneOffsetSeconds);
        assertThat(dateTimeWithZone.getZoneShortName()).isEqualTo(zoneShortName);
    }

    @Test
    public void create_argumentChecks() {
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");

        long timeSinceEpochMillis = 123;
        long timeZoneOffsetSeconds = MILLISECONDS.toSeconds(
                timeZone.getOffset(timeSinceEpochMillis));
        String zoneShortName = "PST";

        // Negative time.
        assertThrows(
                IllegalArgumentException.class,
                () -> DateTimeWithZone.create(-1, (int) timeZoneOffsetSeconds, zoneShortName));

        // Offset out of range.
        assertThrows(
                IllegalArgumentException.class,
                () -> DateTimeWithZone.create(timeSinceEpochMillis, 18 * 60 * 60 + 1,
                        zoneShortName));
        assertThrows(
                IllegalArgumentException.class,
                () -> DateTimeWithZone.create(timeSinceEpochMillis, -18 * 60 * 60 - 1,
                        zoneShortName));

        // Null short name.
        assertThrows(
                NullPointerException.class,
                () -> DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        null));

        // Empty short name.
        assertThrows(
                IllegalArgumentException.class,
                () -> DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        ""));
    }

    @Test
    @SuppressWarnings("JdkObsolete")
    public void create_withTimeZone() {
        GregorianCalendar calendar = new GregorianCalendar(2020, 4, 15, 2, 57, 0);
        Date date = calendar.getTime();
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        long timeSinceEpochMillis = date.getTime();

        DateTimeWithZone dateTimeWithZone = DateTimeWithZone.create(timeSinceEpochMillis, timeZone);

        long timeZoneOffsetSeconds = MILLISECONDS.toSeconds(
                timeZone.getOffset(timeSinceEpochMillis));
        String zoneShortName = "PST";

        assertThat(dateTimeWithZone.getZoneOffsetSeconds()).isEqualTo(timeZoneOffsetSeconds);
        assertThat(dateTimeWithZone.getTimeSinceEpochMillis()).isEqualTo(timeSinceEpochMillis);
        assertThat(dateTimeWithZone.getZoneShortName()).isEqualTo(zoneShortName);
    }

    @Test
    public void create_withTimeZone_argumentChecks() {
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");

        // Negative time.
        assertThrows(
                IllegalArgumentException.class,
                () -> DateTimeWithZone.create(-1, timeZone));

        // Null time zone.
        assertThrows(
                NullPointerException.class,
                () -> DateTimeWithZone.create(123, null));
    }

    @Test
    public void create_withZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-05-14T19:57:00-07:00[US/Pacific]");
        DateTimeWithZone dateTimeWithZone = DateTimeWithZone.create(zonedDateTime);

        assertDateTimeWithZoneEquals(zonedDateTime, dateTimeWithZone);
    }

    @Test
    public void create_withZonedDateTime_argumentChecks() {
        // Null date time.
        assertThrows(
                NullPointerException.class,
                () -> {
                    DateTimeWithZone.create(null);
                });
    }

    @Test
    public void equals() {
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        long timeSinceEpochMillis = System.currentTimeMillis();
        long timeZoneOffsetSeconds =
                Duration.ofMillis(timeZone.getOffset(timeSinceEpochMillis)).getSeconds();
        String zoneShortName = "PST";

        DateTimeWithZone dateTimeWithZone =
                DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        zoneShortName);

        assertThat(dateTimeWithZone)
                .isEqualTo(
                        DateTimeWithZone.create(
                                timeSinceEpochMillis, (int) timeZoneOffsetSeconds, zoneShortName));
    }

    @Test
    public void notEquals_differentTimeSinceEpoch() {
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        long timeSinceEpochMillis = System.currentTimeMillis();
        long timeZoneOffsetSeconds =
                Duration.ofMillis(timeZone.getOffset(timeSinceEpochMillis)).getSeconds();
        String zoneShortName = "PST";

        DateTimeWithZone dateTimeWithZone =
                DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        zoneShortName);

        assertThat(dateTimeWithZone)
                .isNotEqualTo(
                        DateTimeWithZone.create(
                                timeSinceEpochMillis + 1, (int) timeZoneOffsetSeconds,
                                zoneShortName));
    }

    @Test
    public void notEquals_differentTimeZoneOffsetSeconds() {
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        long timeSinceEpochMillis = System.currentTimeMillis();
        long timeZoneOffsetSeconds =
                Duration.ofMillis(timeZone.getOffset(timeSinceEpochMillis)).getSeconds();
        String zoneShortName = "PST";

        DateTimeWithZone dateTimeWithZone =
                DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        zoneShortName);

        assertThat(dateTimeWithZone)
                .isNotEqualTo(
                        DateTimeWithZone.create(
                                timeSinceEpochMillis, (int) timeZoneOffsetSeconds + 1,
                                zoneShortName));
    }

    @Test
    public void notEquals_differentTimeZone() {
        TimeZone timeZone = TimeZone.getTimeZone("US/Pacific");
        long timeSinceEpochMillis = System.currentTimeMillis();
        long timeZoneOffsetSeconds =
                Duration.ofMillis(timeZone.getOffset(timeSinceEpochMillis)).getSeconds();
        String zoneShortName = "PST";

        DateTimeWithZone dateTimeWithZone =
                DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                        zoneShortName);

        assertThat(dateTimeWithZone)
                .isNotEqualTo(
                        DateTimeWithZone.create(timeSinceEpochMillis, (int) timeZoneOffsetSeconds,
                                "UTC"));
    }
}
