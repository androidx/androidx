/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.time

import androidx.health.data.client.records.Record
import java.time.Instant
import java.time.LocalDateTime

/**
 * Specification of time range for read and delete requests.
 *
 * The time range can be specified in one of the following ways:
 * - use [between] for a closed-ended time range, inclusive-exclusive;
 * - use [before] for a open-ended start time range, end time is exclusive;
 * - use [after] for a open-ended end time range, start time is inclusive.
 *
 * Time can be specified in one of the two ways:
 * - use [Instant] for a specific point in time such as "2021-01-03 at 10:00 UTC+1";
 * - use [LocalDateTime] for a user experienced time concept such as "2021-01-03 at 10 o'clock",
 * without knowing which time zone the user was at that time.
 */
class TimeRangeFilter
internal constructor(
    internal val startTime: Instant? = null,
    internal val endTime: Instant? = null,
    internal val localStartTime: LocalDateTime? = null,
    internal val localEndTime: LocalDateTime? = null,
) {
    companion object {
        /**
         * Creates a [TimeRangeFilter] for a time range within the [Instant] time range [startTime,
         * endTime).
         *
         * If user created a [Record] at 2pm(UTC+1), crossed a time zone and created a new [Record]
         * at 3pm(UTC). Filtering between 2pm(UTC) and 6pm(UTC) will include the record at 3pm(UTC)
         * but not the record at 2pm(UTC+1), because 2pm(UTC+1) happened before 2pm(UTC).
         *
         * @param startTime start time of the filter.
         * @param endTime end time of the filter.
         * @return a [TimeRangeFilter] for filtering [Record]s.
         *
         * @see before for time range with open-ended [startTime].
         * @see after for time range with open-ended [endTime].
         */
        @JvmStatic
        fun between(startTime: Instant, endTime: Instant): TimeRangeFilter {
            require(startTime.isBefore(endTime)) { "end time needs be after start time" }
            return TimeRangeFilter(startTime = startTime, endTime = endTime)
        }

        /**
         * Creates a [TimeRangeFilter] for a time range within the [LocalDateTime] range [startTime,
         * endTime).
         *
         * @param startTime start time of the filter.
         * @param endTime end time of the filter.
         * @return a [TimeRangeFilter] for filtering [Record]s.
         *
         * @see before for time range with open-ended [startTime].
         * @see after for time range with open-ended [endTime].
         */
        @JvmStatic
        fun between(startTime: LocalDateTime, endTime: LocalDateTime): TimeRangeFilter {
            require(startTime.isBefore(endTime)) { "end time needs be after start time" }
            return TimeRangeFilter(
                startTime = null,
                endTime = null,
                localStartTime = startTime,
                localEndTime = endTime
            )
        }

        /**
         * Creates a [TimeRangeFilter] for a time range until the given [endTime].
         *
         * @param endTime end time of the filter.
         * @return a [TimeRangeFilter] for filtering [Record]s.
         *
         * @see between for closed-ended time range.
         * @see after for time range with open-ended [endTime]
         */
        @JvmStatic
        fun before(endTime: Instant) = TimeRangeFilter(startTime = null, endTime = endTime)

        /**
         * Creates a [TimeRangeFilter] for a time range until the given [endTime].
         *
         * @param endTime end time of the filter.
         * @return a [TimeRangeFilter] for filtering [Record]s.
         *
         * @see between for closed-ended time range.
         * @see after for time range with open-ended [endTime]
         */
        @JvmStatic
        fun before(endTime: LocalDateTime) =
            TimeRangeFilter(
                startTime = null,
                endTime = null,
                localStartTime = null,
                localEndTime = endTime
            )

        /**
         * Creates a [TimeRangeFilter] for a time range after the given [startTime].
         *
         * @param startTime start time of the filter.
         * @return a [TimeRangeFilter] for filtering [Record]s.
         *
         * @see between for closed-ended time range.
         * @see after for time range with open-ended [startTime]
         */
        @JvmStatic fun after(startTime: Instant) = TimeRangeFilter(startTime = startTime)

        /**
         * Creates a [TimeRangeFilter] for a time range after the given [startTime].
         *
         * @param startTime start time of the filter.
         * @return a [TimeRangeFilter] for filtering [Record]s.
         *
         * @see between for closed-ended time range.
         * @see after for time range with open-ended [startTime]
         */
        @JvmStatic
        fun after(startTime: LocalDateTime) =
            TimeRangeFilter(startTime = null, endTime = null, localStartTime = startTime)

        /**
         * Default [TimeRangeFilter] where neither start nor end time is specified, no [Record]s
         * will be filtered.
         */
        @JvmStatic internal fun none(): TimeRangeFilter = TimeRangeFilter()
    }

    internal fun isOpenEnded(): Boolean =
        (localStartTime == null || localEndTime == null) && (startTime == null || endTime == null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimeRangeFilter) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (localStartTime != other.localStartTime) return false
        if (localEndTime != other.localEndTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + (startTime?.hashCode() ?: 0)
        result = 31 * result + (endTime?.hashCode() ?: 0)
        result = 31 * result + (localStartTime?.hashCode() ?: 0)
        result = 31 * result + (localEndTime?.hashCode() ?: 0)
        return result
    }
}
