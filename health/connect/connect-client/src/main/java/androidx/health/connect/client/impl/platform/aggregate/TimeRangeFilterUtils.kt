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

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun createTimeRange(timeRangeFilter: TimeRangeFilter): TimeRange<*> {
    if (timeRangeFilter.isBasedOnLocalTime()) {
        return createLocalTimeRange(timeRangeFilter)
    }
    return createInstantTimeRange(timeRangeFilter)
}

internal fun createInstantTimeRange(timeRangeFilter: TimeRangeFilter): InstantTimeRange {
    require(!timeRangeFilter.isBasedOnLocalTime()) {
        "TimeRangeFilter should be based on instant time"
    }
    val startTime = timeRangeFilter.startTime ?: Instant.EPOCH
    val endTime = timeRangeFilter.endTime ?: Instant.now()
    return InstantTimeRange(startTime, endTime)
}

internal fun createLocalTimeRange(timeRangeFilter: TimeRangeFilter): LocalTimeRange {
    require(timeRangeFilter.isBasedOnLocalTime()) {
        "TimeRangeFilter should be based on local time"
    }
    val startTime =
        timeRangeFilter.localStartTime ?: LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.MIN)
    val endTime =
        timeRangeFilter.localEndTime
            ?: LocalDateTime.ofInstant(Instant.now().plus(Duration.ofDays(1)), ZoneOffset.MAX)
    return LocalTimeRange(startTime, endTime)
}

internal fun TimeRangeFilter.withBufferedStart(): TimeRangeFilter {
    return TimeRangeFilter(
        startTime = startTime?.minus(RECORD_START_TIME_BUFFER),
        endTime = endTime,
        localStartTime = localStartTime?.minus(RECORD_START_TIME_BUFFER),
        localEndTime = localEndTime
    )
}

// Max buffer to account for overlapping records that have startTime < timeRangeFilter.startTime
private val RECORD_START_TIME_BUFFER: Duration = Duration.ofDays(1)
