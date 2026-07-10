/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * A {@link CarSpan} that represents a timer.
 *
 * <p>This span can be used within text to display a live-updating timer. The timer will count up
 * from the provided start time, which is based on the elapsed real time since device boot, as
 * provided by {@code SystemClock.elapsedRealtime()}.
 *
 * <p>For example, a {@link TimerSpan} can be used in a {@link CarText} to show how much time has
 * passed since a certain event. The exact rendering of the timer is dependent on the car screen.
 *
 * @see CarSpan
 */
@ExperimentalCarApi
@KeepFields
@CarProtocol
public final class TimerSpan extends CarSpan {
    /**
     * A threshold used to detect if the provided start time is likely from {@code
     * System.currentTimeMillis()} instead of {@code SystemClock.elapsedRealtime()}. Since {@code
     * elapsedRealtime()} starts from device boot, a value larger than a few years is highly
     * improbable and suggests an incorrect time source. 20 years is chosen as a safe upper bound
     * for any reasonable device uptime.
     */
    private static final long TWENTY_YEARS_IN_MS = TimeUnit.DAYS.toMillis(365L * 20);

    /** The start time for the timer in elapsed real time since device boot. */
    private final long mElapsedRealtimeMillis;

    private TimerSpan(long elapsedRealtimeMillis) {
        this.mElapsedRealtimeMillis = elapsedRealtimeMillis;
    }

    private TimerSpan() {
        mElapsedRealtimeMillis = 0L;
    }

    /**
     * Creates a {@link TimerSpan} from a start time in elapsed real time since device boot.
     *
     * <p>The timer will count up if the provided {@code elapsedRealtimeMillis} is in the past
     * relative to the current {@code SystemClock.elapsedRealtime()}, and will count down if it is
     * in the future.
     *
     * @param elapsedRealtimeMillis the start time in milliseconds using {@code SystemClock
     *     .elapsedRealtime()}.
     */
    @NonNull
    public static TimerSpan create(long elapsedRealtimeMillis) {
        if (elapsedRealtimeMillis > TWENTY_YEARS_IN_MS) {
            throw new IllegalArgumentException(
                    "The given start time ["
                            + elapsedRealtimeMillis
                            + "] cannot be larger than "
                            + TWENTY_YEARS_IN_MS);
        }
        return new TimerSpan(elapsedRealtimeMillis);
    }

    /** Returns the start time of the timer in milliseconds since device boot. */
    public long getElapsedRealtimeMillis() {
        return mElapsedRealtimeMillis;
    }

    @NonNull
    @Override
    public String toString() {
        return "[start: " + mElapsedRealtimeMillis + "]";
    }

    @Override
    public int hashCode() {
        return Long.hashCode(mElapsedRealtimeMillis);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimerSpan)) {
            return false;
        }
        return (mElapsedRealtimeMillis == ((TimerSpan) other).mElapsedRealtimeMillis);
    }
}
