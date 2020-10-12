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
class TimeRange internal constructor(
    val startDateTimeMillis: Long,
    val endDateTimeMillis: Long
) {
    /** Returns whether the [TimeRange] contains a given point in time. */
    operator fun contains(dateTimeMillis: Long) =
        (dateTimeMillis >= startDateTimeMillis) and (dateTimeMillis <= endDateTimeMillis)

    companion object {
        /** The [TimeRange] that includes every point in time. */
        @JvmField
        val ALWAYS = TimeRange(0, Long.MAX_VALUE)

        /** Constructs a time range after a given point in time. */
        @JvmStatic
        fun after(startDateTimeMillis: Long) = TimeRange(startDateTimeMillis, Long.MAX_VALUE)

        /** Constructs a time range until a given point in time. */
        @JvmStatic
        fun before(endDateTimeMillis: Long) = TimeRange(0, endDateTimeMillis)

        /** Constructs a time range between two points in time, inclusive of the points
         * themselves.
         */
        @JvmStatic
        fun between(startDateTimeMillis: Long, endDateTimeMillis: Long) =
            TimeRange(startDateTimeMillis, endDateTimeMillis)
    }
}

/**
 * Expresses the reference point for a time difference.
 *
 * It defines one of [endDateTimeMillis] or [startDateTimeMillis] to express the corresponding
 * time differences relative before or after the givene point in time.
 */
class TimeReference internal constructor(
    val endDateTimeMillis: Long,
    val startDateTimeMillis: Long
) {
    fun hasStartDateTimeMillis() = startDateTimeMillis != NONE
    fun hasEndDateTimeMillis() = endDateTimeMillis != NONE

    companion object {
        private const val NONE = -1L

        /**
         * Creates a [TimeReference] for the time difference ending at the given [dateTimeMillis].
         */
        @JvmStatic
        fun ending(dateTimeMillis: Long) = TimeReference(dateTimeMillis, NONE)

        /**
         * Creates a [TimeReference] for the time difference starting at the given [dateTimeMillis].
         */
        @JvmStatic
        fun starting(dateTimeMillis: Long) = TimeReference(NONE, dateTimeMillis)
    }
}
