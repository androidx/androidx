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

package androidx.pdf.util.persistence;

import androidx.annotation.RestrictTo;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Interface of SystemClock; real instances should just delegate the calls to the static methods,
 * while test instances return values set manually; see {@link android.os.SystemClock}. In addition,
 * this interface also has instance methods for {@link System#currentTimeMillis} and {@link
 * System#nanoTime}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Clock {
    /**
     * Returns the current system time in milliseconds since January 1, 1970 00:00:00 UTC. This
     * method shouldn't be used for measuring timeouts or other elapsed time measurements, as
     * changing the system time can affect the results.
     *
     * @return the local system time in milliseconds.
     */
    @CheckReturnValue
    long currentTimeMillis();

    /**
     * Returns the current value of the running Java Virtual Machine's high-resolution time source,
     * in nanoseconds.
     *
     * @return the current value of the running Java Virtual Machine's high-resolution time source,
     * in nanoseconds
     * @see System#nanoTime()
     */
    long nanoTime();

    /**
     * Returns the number of milliseconds that the current thread has been running. Does not advance
     * while the thread's execution is suspended.
     *
     * @return milliseconds running in the current thread.
     */
    long currentThreadTimeMillis();

    /**
     * Returns milliseconds since boot, including time spent in sleep.
     *
     * @return elapsed milliseconds since boot.
     */
    long elapsedRealtime();

    /**
     * Returns nanoseconds since boot, including time spent in sleep.
     *
     * @return elapsed nanoseconds since boot.
     */
    long elapsedRealtimeNanos();

    /**
     * Returns milliseconds since boot, not counting time spent in deep sleep.
     *
     * @return milliseconds of non-sleep uptime since boot.
     */
    long uptimeMillis();
}
