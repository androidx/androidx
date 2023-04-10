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

package androidx.wear.watchface.complications.data

import android.support.wearable.complications.ComplicationData as WireComplicationData
import java.time.Instant

/** A range of time, that may be unbounded on either side. */
public class TimeRange internal constructor(
    public val startDateTimeMillis: Instant,
    public val endDateTimeMillis: Instant
) {
    /** Returns whether the [TimeRange] contains a given point in time. */
    public operator fun contains(dateTimeMillis: Instant): Boolean =
        (dateTimeMillis >= startDateTimeMillis) and (dateTimeMillis <= endDateTimeMillis)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeRange

        if (startDateTimeMillis != other.startDateTimeMillis) return false
        if (endDateTimeMillis != other.endDateTimeMillis) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startDateTimeMillis.hashCode()
        result = 31 * result + endDateTimeMillis.hashCode()
        return result
    }

    override fun toString(): String {
        if (WireComplicationData.shouldRedact()) {
            return "TimeRange(REDACTED)"
        }
        return "TimeRange(startDateTimeMillis=$startDateTimeMillis, " +
            "endDateTimeMillis=$endDateTimeMillis)"
    }

    public companion object {
        /** The [TimeRange] that includes every point in time. */
        @JvmField
        public val ALWAYS: TimeRange = TimeRange(Instant.MIN, Instant.MAX)

        /** Constructs a time range after a given point in time. */
        @JvmStatic
        public fun after(startInstant: Instant): TimeRange =
            TimeRange(startInstant, Instant.MAX)

        /** Constructs a time range until a given point in time. */
        @JvmStatic
        public fun before(endInstant: Instant): TimeRange = TimeRange(Instant.MIN, endInstant)

        /** Constructs a time range between two points in time, inclusive of the points
         * themselves.
         */
        @JvmStatic
        public fun between(startInstant: Instant, endInstant: Instant): TimeRange =
            TimeRange(startInstant, endInstant)
    }
}

/** Defines a point in time the complication is counting down until. */
public class CountDownTimeReference(public val instant: Instant)

/** Defines a point in time the complication is counting up from. */
public class CountUpTimeReference(public val instant: Instant)
