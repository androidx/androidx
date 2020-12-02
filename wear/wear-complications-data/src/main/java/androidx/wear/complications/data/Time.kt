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

package androidx.wear.complications.data

/** A range of time, that may be unbounded on either side. */
public class TimeRange internal constructor(
    public val startDateTimeMillis: Long,
    public val endDateTimeMillis: Long
) {
    /** Returns whether the [TimeRange] contains a given point in time. */
    public operator fun contains(dateTimeMillis: Long): Boolean =
        (dateTimeMillis >= startDateTimeMillis) and (dateTimeMillis <= endDateTimeMillis)

    public companion object {
        /** The [TimeRange] that includes every point in time. */
        @JvmField
        public val ALWAYS: TimeRange = TimeRange(0, Long.MAX_VALUE)

        /** Constructs a time range after a given point in time. */
        @JvmStatic
        public fun after(startDateTimeMillis: Long): TimeRange =
            TimeRange(startDateTimeMillis, Long.MAX_VALUE)

        /** Constructs a time range until a given point in time. */
        @JvmStatic
        public fun before(endDateTimeMillis: Long): TimeRange = TimeRange(0, endDateTimeMillis)

        /** Constructs a time range between two points in time, inclusive of the points
         * themselves.
         */
        @JvmStatic
        public fun between(startDateTimeMillis: Long, endDateTimeMillis: Long): TimeRange =
            TimeRange(startDateTimeMillis, endDateTimeMillis)
    }
}

/**
 * Expresses a reference point or range for a time difference.
 *
 * It defines [endDateTimeMillis] and/or [startDateTimeMillis] to express the corresponding
 * time differences relative to before, between or after the given point(s) in time.
 */
public class TimeReference internal constructor(
    public val endDateTimeMillis: Long,
    public val startDateTimeMillis: Long
) {
    public fun hasStartDateTimeMillis(): Boolean = startDateTimeMillis != NONE
    public fun hasEndDateTimeMillis(): Boolean = endDateTimeMillis != NONE

    public companion object {
        private const val NONE = -1L

        /**
         * Creates a [TimeReference] for the time difference ending at the given [dateTimeMillis].
         */
        @JvmStatic
        public fun ending(dateTimeMillis: Long): TimeReference = TimeReference(dateTimeMillis, NONE)

        /**
         * Creates a [TimeReference] for the time difference starting at the given [dateTimeMillis].
         */
        @JvmStatic
        public fun starting(dateTimeMillis: Long): TimeReference =
            TimeReference(NONE, dateTimeMillis)

        /**
         * Creates a [TimeReference] for the time difference between [startDateTimeMillis] and [endDateTimeMillis].
         */
        @JvmStatic
        public fun between(startDateTimeMillis: Long, endDateTimeMillis: Long): TimeReference =
            TimeReference(endDateTimeMillis, startDateTimeMillis)
    }
}
