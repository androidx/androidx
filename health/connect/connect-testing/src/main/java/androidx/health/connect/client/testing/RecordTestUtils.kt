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

package androidx.health.connect.client.testing

import androidx.health.connect.client.impl.converters.records.endTime
import androidx.health.connect.client.impl.converters.records.startTime
import androidx.health.connect.client.impl.converters.records.time
import androidx.health.connect.client.impl.converters.records.toProto
import androidx.health.connect.client.impl.converters.records.zoneOffset
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.platform.client.proto.DataProto
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal fun Record.isWithin(filter: TimeRangeFilter, clock: Clock): Boolean {
    val proto: DataProto.DataPoint = toProto()
    val timeRangeFilter = filter.sanitize(clock)

    if (timeRangeFilter.isLocalBasedFilter()) {
        if (proto.hasInstantTimeMillis()) {
            val time =
                LocalDateTime.ofInstant(proto.time, proto.zoneOffset ?: ZoneId.systemDefault())
            return !time.isBefore(timeRangeFilter.localStartTime!!) &&
                timeRangeFilter.localEndTime!!.isAfter(time)
        }
        val startTime =
            LocalDateTime.ofInstant(proto.startTime, proto.zoneOffset ?: ZoneId.systemDefault())
        return !startTime.isBefore(timeRangeFilter.localStartTime!!) &&
            timeRangeFilter.localEndTime!!.isAfter(startTime)
    }

    if (proto.hasInstantTimeMillis()) {
        return proto.time >= timeRangeFilter.startTime!! && // Inclusive
            proto.time.isBefore(timeRangeFilter.endTime!!) // Exclusive
    }

    return proto.startTime >= timeRangeFilter.startTime!! && // Inclusive
        proto.endTime.isBefore(timeRangeFilter.endTime!!) // Exclusive
}

private fun TimeRangeFilter.sanitize(clock: Clock): TimeRangeFilter {
    if (isLocalBasedFilter()) {
        return TimeRangeFilter.between(
            startTime =
                localStartTime ?: LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()),
            endTime =
                localEndTime ?: LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault())
        )
    }
    return TimeRangeFilter.between(
        startTime = startTime ?: Instant.EPOCH,
        endTime = endTime ?: clock.instant()
    )
}

private fun TimeRangeFilter.isLocalBasedFilter(): Boolean {
    return localStartTime != null || localEndTime != null
}

/** Gets the package name from metadata */
internal val Record.packageName: String
    get() = this.metadata.dataOrigin.packageName
