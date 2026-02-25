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

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Implementation of RemoteClock for API 23+.
 *
 * Uses a {@link Calendar} to get the current time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CalendarSystemClock implements RemoteClock {
    private final Calendar mCalendar = Calendar.getInstance();
    private final TimeZone mTimeZone;

    public CalendarSystemClock() {
        this(TimeZone.getDefault());
    }

    public CalendarSystemClock(@NonNull TimeZone timeZone) {
        mTimeZone = timeZone;
        mCalendar.setTimeZone(timeZone);
    }

    @Override
    public long millis() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public @NonNull String getZoneId() {
        return mTimeZone.getID();
    }

    /**
     * The calendar is stateful and cached, so calls will fail if used concurrently.
     *
     * @param millis the time for the snapshot or now if null.
     * @return a time snapshot to query a time
     */
    @Override
    public @NonNull TimeSnapshot snapshot(@Nullable Long millis) {
        long at = millis != null ? millis : System.currentTimeMillis();

        mCalendar.setTimeInMillis(at);
        return new CalendarTimeSnapshot(at, mCalendar, mTimeZone);
    }

    static class CalendarTimeSnapshot implements TimeSnapshot {
        private final long mMillis;
        private final Calendar mCal;
        private final TimeZone mTz;

        CalendarTimeSnapshot(long millis, Calendar cal, TimeZone tz) {
            mMillis = millis;
            mCal = cal;
            mTz = tz;
        }

        @Override
        public long getMillis() {
            return mMillis;
        }

        @Override
        public int getYear() {
            return getCalendar().get(Calendar.YEAR);
        }

        private Calendar getCalendar() {
            if (mCal.getTimeInMillis() != mMillis) {
                throw new IllegalStateException("Calendar is out of sync");
            }
            return mCal;
        }

        @Override
        public int getMonth() {
            return getCalendar().get(Calendar.MONTH) + 1;
        }

        @Override
        public int getDayOfMonth() {
            return getCalendar().get(Calendar.DAY_OF_MONTH);
        }

        @Override
        public int getDayOfYear() {
            return getCalendar().get(Calendar.DAY_OF_YEAR);
        }

        @Override
        public int getHour() {
            return getCalendar().get(Calendar.HOUR_OF_DAY);
        }

        @Override
        public int getMinute() {
            return getCalendar().get(Calendar.MINUTE);
        }

        @Override
        public int getSecond() {
            return getCalendar().get(Calendar.SECOND);
        }

        @Override
        public int getMillisOfSecond() {
            return getCalendar().get(Calendar.MILLISECOND);
        }

        @Override
        public int getDayOfWeek() {
            int dow = getCalendar().get(Calendar.DAY_OF_WEEK);
            return (dow == Calendar.SUNDAY) ? 7 : (dow - 1);
        }

        @Override
        public int getOffsetSeconds() {
            return mTz.getOffset(mMillis) / 1000;
        }
    }
}
