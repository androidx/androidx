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

import android.os.SystemClock;

import androidx.annotation.RestrictTo;

/**
 * Implementation of {@link Clock} that delegates to the system clock.
 *
 * <p>This class is intended for use only in contexts where injection is impossible. Where possible,
 * prefer to simply inject a {@link Clock}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class SystemClockImpl implements Clock {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public long currentThreadTimeMillis() {
        return SystemClock.currentThreadTimeMillis();
    }

    @Override
    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    public long elapsedRealtimeNanos() {
        return ElapsedRealtimeNanosImpl.elapsedRealtimeNanos();
    }

    @Override
    public long uptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    // This companion object is required to work around AppReduce which prevents
    // inlining of all the methods above.
    //
    // This actually *reduces* the number of classes in an optimized build by allowing
    // Clock+SystemClockImpl to be removed.
    private static final class ElapsedRealtimeNanosImpl {

        static long elapsedRealtimeNanos() {
            return SystemClock.elapsedRealtimeNanos();
        }

    }
}

