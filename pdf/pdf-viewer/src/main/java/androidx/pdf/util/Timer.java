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

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Simple timer for profiling methods.
 */
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
}
