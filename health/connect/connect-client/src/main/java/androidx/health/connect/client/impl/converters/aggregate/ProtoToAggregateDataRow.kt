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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.impl.converters.aggregate

import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.platform.client.proto.DataProto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// ZoneOffset.ofTotalSeconds() has been banned but safe here for serialization.
@SuppressWarnings("GoodTime")
fun DataProto.AggregateDataRow.toAggregateDataRowGroupByDuration():
    AggregationResultGroupedByDuration {
    require(hasStartTimeEpochMs()) { "start time must be set" }
    require(hasEndTimeEpochMs()) { "end time must be set" }

    return AggregationResultGroupedByDuration(
        result = retrieveAggregateDataRow(),
        startTime = Instant.ofEpochMilli(startTimeEpochMs),
        endTime = Instant.ofEpochMilli(endTimeEpochMs),
        zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds)
    )
}

fun DataProto.AggregateDataRow.toAggregateDataRowGroupByPeriod(): AggregationResultGroupedByPeriod {
    require(hasStartLocalDateTime()) { "start time must be set" }
    require(hasEndLocalDateTime()) { "end time must be set" }

    return AggregationResultGroupedByPeriod(
        result = retrieveAggregateDataRow(),
        startTime = LocalDateTime.parse(startLocalDateTime),
        endTime = LocalDateTime.parse(endLocalDateTime),
    )
}

fun DataProto.AggregateDataRow.retrieveAggregateDataRow() =
    AggregationResult(
        longValues = longValuesMap,
        doubleValues = doubleValuesMap,
        dataOrigins = dataOriginsList.mapTo(HashSet()) { DataOrigin(it.applicationId) }
    )
