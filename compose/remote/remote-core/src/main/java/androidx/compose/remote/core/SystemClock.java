/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Implementation of RemoteClock using java.time.Clock for API 26+.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(26)
public class SystemClock implements RemoteClock {
    private final Clock mClock;

    @SuppressWarnings("JavaTimeDefaultTimeZone")
    public SystemClock() {
        this.mClock = Clock.systemDefaultZone();
    }

    public SystemClock(@NonNull Clock clock) {
        this.mClock = clock;
    }

    @Override
    public long millis() {
        return mClock.millis();
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @NonNull
    @Override
    public String getZoneId() {
        return mClock.getZone().getId();
    }

    @NonNull
    @Override
    public TimeSnapshot snapshot(@Nullable Long millis) {
        long at = millis != null ? millis : mClock.millis();

        return new JavaTimeSnapshot(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), mClock.getZone()));
    }

    static class JavaTimeSnapshot implements TimeSnapshot {
        private final ZonedDateTime mZdt;

        JavaTimeSnapshot(ZonedDateTime zdt) {
            mZdt = zdt;
        }

        @Override
        public long getMillis() {
            return mZdt.toInstant().toEpochMilli();
        }

        @Override
        public int getYear() {
            return mZdt.getYear();
        }

        @Override
        public int getMonth() {
            return mZdt.getMonthValue();
        }

        @Override
        public int getDayOfMonth() {
            return mZdt.getDayOfMonth();
        }

        @Override
        public int getDayOfYear() {
            return mZdt.getDayOfYear();
        }

        @Override
        public int getHour() {
            return mZdt.getHour();
        }

        @Override
        public int getMinute() {
            return mZdt.getMinute();
        }

        @Override
        public int getSecond() {
            return mZdt.getSecond();
        }

        @Override
        public int getMillisOfSecond() {
            return mZdt.getNano() / 1_000_000;
        }

        @Override
        public int getDayOfWeek() {
            return mZdt.getDayOfWeek().getValue();
        }

        @Override
        public int getOffsetSeconds() {
            return mZdt.getOffset().getTotalSeconds();
        }
    }
}
