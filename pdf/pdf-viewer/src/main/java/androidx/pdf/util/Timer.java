/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.annotation.SuppressLint;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.FormatMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple timer for profiling methods.
 */
@SuppressLint("BanConcurrentHashMap")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Timer {

    private final long mStartTimeMs;

    /** Start a new timer right now. */
    @NonNull
    public static Timer start() {
        return new Timer();
    }

    private Timer() {
        mStartTimeMs = getElapsedTimeMs();
    }

    /** Return the time (in milliseconds) elapsed since this timer was started. */
    public long time() {
        return getElapsedTimeMs() - mStartTimeMs;
    }

    /** Returns the number of milliseconds elapsed since some fixed past time. */
    private static long getElapsedTimeMs() {
        return SystemClock.elapsedRealtime();
    }

    /** A logger that times every event it receives and outputs all events and their timestamp. */
    public static class LogBuilder {

        /** Using a StringBuffer because we need this to be thread-safe. */
        private final StringBuilder mLog = new StringBuilder();

        private final Timer mTimer = Timer.start();

        {
            track("Created");
        }

        /**
         *
         */
        @NonNull
        @CanIgnoreReturnValue
        public LogBuilder track(@NonNull String event) {
            mLog.append(event).append(":").append(mTimer.time()).append("; ");
            return this;
        }

        /**
         *
         */
        @NonNull
        @CanIgnoreReturnValue
        @FormatMethod
        public LogBuilder trackFmt(@NonNull String eventFmt, @NonNull Object... args) {
            mLog.append(String.format(eventFmt, args)).append(":").append(mTimer.time()).append(
                    "; ");
            return this;
        }

        /**
         *
         */
        @NonNull
        @Override
        public String toString() {
            return mLog.toString();
        }
    }

    /** Keeps a concurrent collection of loggers and allows them to be easily retrieved. */
    public static class Loggers {
        private final Map<String, LogBuilder> mNameToTimer = new ConcurrentHashMap<>();

        /**
         *
         */
        public void start(@NonNull String key) {
            mNameToTimer.put(key, new LogBuilder());
        }

        /**
         *
         */
        public void track(@NonNull String key, @NonNull String event) {
            LogBuilder logger = mNameToTimer.get(key);
            if (logger != null) {
                logger.track(event);
            }
        }

        /**
         *
         */
        @NonNull
        public String stop(@NonNull String key) {
            LogBuilder logger = mNameToTimer.remove(key);
            if (logger != null) {
                return logger.toString();
            } else {
                return "No logger for key " + key;
            }
        }
    }
}
