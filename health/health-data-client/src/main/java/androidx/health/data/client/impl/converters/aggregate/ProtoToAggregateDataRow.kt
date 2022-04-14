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
package androidx.health.data.client.impl.converters.aggregate

import androidx.health.data.client.aggregate.AggregateDataRow
import androidx.health.data.client.aggregate.AggregateDataRowGroupByDuration
import androidx.health.data.client.aggregate.AggregateDataRowGroupByPeriod
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.platform.client.proto.DataProto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// ZoneOffset.ofTotalSeconds() has been banned but safe here for serialization.
@SuppressWarnings("GoodTime")
fun DataProto.AggregateDataRow.toAggregateDataRowGroupByDuration():
    AggregateDataRowGroupByDuration {
    require(hasStartTimeEpochMs()) { "start time must be set" }
    require(hasEndTimeEpochMs()) { "end time must be set" }

    return AggregateDataRowGroupByDuration(
        data = retrieveAggregateDataRow(),
        startTime = Instant.ofEpochMilli(startTimeEpochMs),
        endTime = Instant.ofEpochMilli(endTimeEpochMs),
        zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds)
    )
}

fun DataProto.AggregateDataRow.toAggregateDataRowGroupByPeriod(): AggregateDataRowGroupByPeriod {
    require(hasStartLocalDateTime()) { "start time must be set" }
    require(hasEndLocalDateTime()) { "end time must be set" }

    return AggregateDataRowGroupByPeriod(
        data = retrieveAggregateDataRow(),
        startTime = LocalDateTime.parse(startLocalDateTime),
        endTime = LocalDateTime.parse(endLocalDateTime),
    )
}

fun DataProto.AggregateDataRow.retrieveAggregateDataRow() =
    AggregateDataRow(
        longValues = longValuesMap,
        doubleValues = doubleValuesMap,
        dataOrigins = dataOriginsList.map { DataOrigin(it.applicationId) }
    )
