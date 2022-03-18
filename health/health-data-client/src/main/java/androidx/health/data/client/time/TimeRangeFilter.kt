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

import androidx.annotation.RestrictTo
import java.time.Instant
import java.time.LocalDateTime

/**
 * Specification of time range for read and delete requests.
 *
 * The time range can be specified either in exact times, or zoneless local times.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class TimeRangeFilter
internal constructor(
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val localStartTime: LocalDateTime? = null,
    val localEndTime: LocalDateTime? = null,
) {
    companion object {
        @JvmSynthetic
        @JvmOverloads
        /**
         * Specifies the time range with [Instant] start and end time, inclusive - exclusive.
         *
         * E.g. User created [Record]s at 2pm(UTC+1), crossed a time zone and created new [Record]s
         * at 3pm(UTC). Filtering between 2pm(UTC) and 6pm(UTC) will include records at 3pm(UTC) but
         * not records at 2pm(UTC+1).
         *
         * Both interval endpoints are nullable where any null value means the interval is
         * open-ended in that direction.
         */
        fun exact(startTime: Instant? = null, endTime: Instant? = null): TimeRangeFilter =
            TimeRangeFilter(
                startTime = startTime,
                endTime = endTime,
                localStartTime = null,
                localEndTime = null
            )

        @JvmSynthetic
        @JvmOverloads
        /**
         * Specifies the time range with [LocalDateTime] start and end time, inclusive - exclusive,
         * without specifying any time zone.
         *
         * E.g. User created [Record]s at 2pm(UTC+1), crossed a time zone and created new [Record]s
         * at 3pm(UTC). Filtering between 2pm and 6pm will include records at both 2pm(UTC+1) and
         * 3pm(UTC).
         *
         * Both interval endpoints are nullable where any null value means the interval is
         * open-ended in that direction.
         */
        fun zoneless(
            localStartTime: LocalDateTime? = null,
            localEndTime: LocalDateTime? = null
        ): TimeRangeFilter =
            TimeRangeFilter(
                startTime = null,
                endTime = null,
                localStartTime = localStartTime,
                localEndTime = localEndTime
            )

        @JvmSynthetic
        /** Default [TimeRangeFilter] where all fields are null. */
        fun empty(): TimeRangeFilter =
            TimeRangeFilter(
                startTime = null,
                endTime = null,
                localStartTime = null,
                localEndTime = null
            )
    }

    internal fun isOpenEnded(): Boolean =
        (localStartTime == null || localEndTime == null) && (startTime == null || endTime == null)
}
