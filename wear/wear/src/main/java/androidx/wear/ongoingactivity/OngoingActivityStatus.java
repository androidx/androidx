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
package androidx.wear.ongoingactivity;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Base class to serialize / deserialize {@link OngoingActivityStatus} into / from a Notification
 *
 * The classes here use timestamps for updating the displayed representation of the status, in
 * cases when this is needed (chronometers), as returned by
 * {@link android.os.SystemClock#elapsedRealtime()}
 */
public abstract class OngoingActivityStatus {
    /**
     * Returns a textual representation of the ongoing activity status at the given time
     * represented as milliseconds timestamp
     *
     * For forward compatibility, the best way to display this is on a
     * {@link android.widget.TextView}
     *
     * @param context       may be used for internationalization. Only used while this method
     *                      executed.
     * @param timeNowMillis the timestamp of the time we want to display, usually now, as
     *                      returned by {@link android.os.SystemClock#elapsedRealtime()}.
     */
    @NonNull
    public abstract CharSequence getText(@NonNull Context context, long timeNowMillis);

    /**
     * Returns the timestamp of the next time when the display will be different from the current
     * one.
     *
     * @param fromTimeMillis current time, usually now as returned by
     *                       {@link android.os.SystemClock#elapsedRealtime()}. In most cases
     *                       {@code getText} and {@code getNextChangeTimeMillis} should be called
     *                       with the exact same timestamp, so changes are not missed.
     * @return the first point in time after {@code fromTimeMillis} when the displayed value of
     * this status will change. returns Long.MAX_VALUE if the display will never change.
     */
    public abstract long getNextChangeTimeMillis(long fromTimeMillis);

    /**
     * Serializes the information into the given {@link Bundle}.
     */
    abstract void extend(Bundle bundle);

    /**
     * Deserializes the information from the given {@link Bundle}.
     */
    @Nullable
    static OngoingActivityStatus create(Bundle bundle) {
        if (bundle.getBoolean(KEY_USE_CHRONOMETER, false)) {
            return new TimerOngoingActivityStatus(
                    bundle.getLong(KEY_TIME_ZERO),
                    bundle.getBoolean(KEY_COUNT_DOWN, false),
                    bundle.getLong(KEY_PAUSED_AT, LONG_DEFAULT),
                    bundle.getLong(KEY_TOTAL_DURATION, LONG_DEFAULT)
            );
        } else {
            String text = bundle.getString(KEY_STATUS);
            return text == null ? null : new TextOngoingActivityStatus(text);
        }
    }

    // keys to use inside the bundle when serializing/deserializing.
    static final String KEY_COUNT_DOWN = "countdown";
    static final String KEY_TIME_ZERO = "timeZero";
    static final String KEY_USE_CHRONOMETER = "useChronometer";
    static final String KEY_STATUS = "status";
    static final String KEY_TOTAL_DURATION = "totalDuration";
    static final String KEY_PAUSED_AT = "pausedAt";

    // Invalid value to use for paused_at and duration, as suggested by api guidelines 5.15
    static final long LONG_DEFAULT = -1L;
}



