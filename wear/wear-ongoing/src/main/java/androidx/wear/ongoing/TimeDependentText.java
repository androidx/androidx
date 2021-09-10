/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.ongoing;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Represents the status or a part of the status of an ongoing activity.
 * Its content may change with time (for example, if the status contains a timer)
 */
public interface TimeDependentText {
    /**
     * Returns a textual representation of the ongoing activity status or a part of it
     * at the given time represented as milliseconds timestamp
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
    CharSequence getText(@NonNull Context context, long timeNowMillis);

    /**
     * Returns the timestamp of the next time when the display may be different from the one
     * at the specified time.
     *
     * @param fromTimeMillis current time, usually now as returned by
     *                       {@link android.os.SystemClock#elapsedRealtime()}. In most cases
     *                       {@code getText} and {@code getNextChangeTimeMillis} should be called
     *                       with the exact same timestamp, so changes are not missed.
     * @return the first point in time after {@code fromTimeMillis} when the displayed value of
     * this status may change. returns Long.MAX_VALUE if the display will never change.
     */
    long getNextChangeTimeMillis(long fromTimeMillis);
}
