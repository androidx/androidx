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

import static androidx.compose.remote.core.RemoteContext.ID_CALENDAR_MONTH;
import static androidx.compose.remote.core.RemoteContext.ID_CONTINUOUS_SEC;
import static androidx.compose.remote.core.RemoteContext.ID_DAY_OF_MONTH;
import static androidx.compose.remote.core.RemoteContext.ID_DAY_OF_YEAR;
import static androidx.compose.remote.core.RemoteContext.ID_OFFSET_TO_UTC;
import static androidx.compose.remote.core.RemoteContext.ID_TIME_IN_HR;
import static androidx.compose.remote.core.RemoteContext.ID_TIME_IN_MIN;
import static androidx.compose.remote.core.RemoteContext.ID_TIME_IN_SEC;
import static androidx.compose.remote.core.RemoteContext.ID_WEEK_DAY;
import static androidx.compose.remote.core.RemoteContext.ID_YEAR;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** RemoteCompose concept of the current time. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteClock {
    RemoteClock SYSTEM = new CalendarSystemClock();

    /**
     * Return System.currentTimeMillis() or something with similar properties.
     */
    long millis();

    /**
     * Return System.nanoTime, or something with similar properties such as not being affected by
     * time synchronization such as NTP.
     */
    long nanoTime();

    /**
     * Return the current time zone ID.
     */
    @NonNull String getZoneId();

    /**
     * Returns a snapshot of the given time, defaults to now.
     */
    @NonNull TimeSnapshot snapshot(@Nullable Long millis);

    /**
     * An immutable snapshot of time, typically used for consistent calculations across a single
     * frame/document update.
     *
     * @see RemoteContext#ID_TIME_IN_MIN
     * @see RemoteContext#ID_YEAR
     */
    interface TimeSnapshot {
        /**
         * Returns the number of milliseconds since the Unix epoch.
         * Used for {@link RemoteContext#ID_EPOCH_SECOND}.
         */
        long getMillis();

        /**
         * Returns the year.
         * Used for {@link RemoteContext#ID_YEAR}.
         */
        int getYear();

        /**
         * Returns the month (1-12).
         * Used for {@link RemoteContext#ID_CALENDAR_MONTH}.
         */
        int getMonth();

        /**
         * Returns the day of the month (1-31).
         * Used for {@link RemoteContext#ID_DAY_OF_MONTH}.
         */
        int getDayOfMonth();

        /**
         * Returns the day of the year (1-366).
         * Used for {@link RemoteContext#ID_DAY_OF_YEAR}.
         */
        int getDayOfYear();

        /**
         * Returns the hour of the day (0-23).
         * Used for {@link RemoteContext#ID_TIME_IN_HR} and contributes to
         * {@link RemoteContext#ID_TIME_IN_MIN}.
         */
        int getHour();

        /**
         * Returns the minute of the hour (0-59).
         * Contributes to {@link RemoteContext#ID_TIME_IN_MIN},
         * {@link RemoteContext#ID_TIME_IN_SEC},
         * and {@link RemoteContext#ID_CONTINUOUS_SEC}.
         */
        int getMinute();

        /**
         * Returns the second of the minute (0-59).
         * Contributes to {@link RemoteContext#ID_TIME_IN_SEC} and
         * {@link RemoteContext#ID_CONTINUOUS_SEC}.
         */
        int getSecond();

        /**
         * Returns the millisecond of the second (0-999).
         * Contributes to {@link RemoteContext#ID_CONTINUOUS_SEC}.
         */
        int getMillisOfSecond();

        /**
         * Returns the day of the week (1 Monday to 7 Sunday).
         * Used for {@link RemoteContext#ID_WEEK_DAY}.
         */
        int getDayOfWeek();

        /**
         * Returns the offset from UTC in seconds.
         * Used for {@link RemoteContext#ID_OFFSET_TO_UTC}.
         */
        int getOffsetSeconds();

        /**
         * Returns the seconds within the hour down to millis precision.
         */
        default float getContinuousSeconds() {
            return getMinute() * 60 + getSecond() + getMillisOfSecond() * 1E-3f;
        }

        /**
         * Returns the number of seconds since the Unix epoch.
         */
        default int getEpochSeconds() {
            return (int) (getMillis() / 1000L);
        }

        /**
         * Returns the second within the hour.
         */
        default float getTimeInSec() {
            return getMinute() * 60 + getSecond();
        }

        /**
         * Returns the minute within the day.
         */
        default float getTimeInMin() {
            return getHour() * 60 + getMinute();
        }

        /**
         * Get a time value by id.
         */
        default float getTime(float value) {
            int id = Utils.idFromNan(value);
            switch (id) {
                case ID_OFFSET_TO_UTC:
                    return getOffsetSeconds();
                case ID_CONTINUOUS_SEC:
                    return this.getContinuousSeconds();
                case ID_TIME_IN_SEC:
                    return getTimeInSec();
                case ID_TIME_IN_MIN:
                    return getTimeInMin();
                case ID_TIME_IN_HR:
                    return getHour();
                case ID_CALENDAR_MONTH:
                    return getMonth();
                case ID_DAY_OF_MONTH:
                    return getDayOfMonth();
                case ID_WEEK_DAY:
                    return getDayOfWeek();
                case ID_DAY_OF_YEAR:
                    return getDayOfYear();
                case ID_YEAR:
                    return getYear();
            }
            return value;
        }
    }
}
