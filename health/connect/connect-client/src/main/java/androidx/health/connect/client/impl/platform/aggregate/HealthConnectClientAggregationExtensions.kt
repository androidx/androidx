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

@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.aggregate

import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.impl.converters.datatype.RECORDS_CLASS_NAME_MAP
import androidx.health.connect.client.impl.platform.toInstantWithDefaultZoneFallback
import androidx.health.connect.client.impl.platform.useLocalTime
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Max buffer to account for overlapping records that have startTime < timeRangeFilter.startTime
val RECORD_START_TIME_BUFFER: Duration = Duration.ofDays(1)

private val AGGREGATION_FALLBACK_RECORD_TYPES =
    setOf(
        BloodPressureRecord::class,
        CyclingPedalingCadenceRecord::class,
        NutritionRecord::class,
        SpeedRecord::class,
        StepsCadenceRecord::class
    )

internal suspend fun HealthConnectClient.aggregateFallback(
    request: AggregateRequest
): AggregationResult {
    var aggregationResult = emptyAggregationResult()

    for (recordType in AGGREGATION_FALLBACK_RECORD_TYPES) {
        aggregationResult +=
            aggregate(
                recordType,
                request.fallbackMetrics,
                request.timeRangeFilter,
                request.dataOriginFilter
            )
    }

    return aggregationResult
}

private suspend fun <T : Record> HealthConnectClient.aggregate(
    recordType: KClass<T>,
    metrics: Set<AggregateMetric<*>>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
): AggregationResult {
    val dataTypeName = RECORDS_CLASS_NAME_MAP[recordType]
    val recordTypeMetrics = metrics.filter { it.dataTypeName == dataTypeName }.toSet()

    if (recordTypeMetrics.isEmpty()) {
        return emptyAggregationResult()
    }

    return when (recordType) {
        BloodPressureRecord::class ->
            aggregateBloodPressure(recordTypeMetrics, timeRangeFilter, dataOriginFilter)
        CyclingPedalingCadenceRecord::class ->
            aggregateCyclingPedalingCadence(recordTypeMetrics, timeRangeFilter, dataOriginFilter)
        NutritionRecord::class -> aggregateNutritionTransFatTotal(timeRangeFilter, dataOriginFilter)
        SpeedRecord::class -> aggregateSpeed(recordTypeMetrics, timeRangeFilter, dataOriginFilter)
        StepsCadenceRecord::class ->
            aggregateStepsCadence(recordTypeMetrics, timeRangeFilter, dataOriginFilter)
        else -> error("Invalid record type for aggregation fallback: $recordType")
    }
}

/** Reads all existing records that satisfy [timeRangeFilter] and [dataOriginFilter]. */
suspend fun <T : Record> HealthConnectClient.readRecordsFlow(
    recordType: KClass<T>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
): Flow<List<T>> {
    return flow {
        var pageToken: String? = null
        do {
            val response =
                readRecords(
                    ReadRecordsRequest(
                        recordType = recordType,
                        timeRangeFilter = timeRangeFilter,
                        dataOriginFilter = dataOriginFilter,
                        pageToken = pageToken
                    )
                )
            emit(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)
    }
}

internal fun IntervalRecord.overlaps(timeRangeFilter: TimeRangeFilter): Boolean {
    val startTimeOverlaps: Boolean
    val endTimeOverlaps: Boolean
    if (timeRangeFilter.useLocalTime()) {
        startTimeOverlaps =
            timeRangeFilter.localEndTime == null ||
                startTime.isBefore(
                    timeRangeFilter.localEndTime.toInstantWithDefaultZoneFallback(startZoneOffset)
                )
        endTimeOverlaps =
            timeRangeFilter.localStartTime == null ||
                endTime.isAfter(
                    timeRangeFilter.localStartTime.toInstantWithDefaultZoneFallback(endZoneOffset)
                )
    } else {
        startTimeOverlaps =
            timeRangeFilter.endTime == null || startTime.isBefore(timeRangeFilter.endTime)
        endTimeOverlaps =
            timeRangeFilter.startTime == null || endTime.isAfter(timeRangeFilter.startTime)
    }
    return startTimeOverlaps && endTimeOverlaps
}

internal fun TimeRangeFilter.withBufferedStart(): TimeRangeFilter {
    return TimeRangeFilter(
        startTime = startTime?.minus(RECORD_START_TIME_BUFFER),
        endTime = endTime,
        localStartTime = localStartTime?.minus(RECORD_START_TIME_BUFFER),
        localEndTime = localEndTime
    )
}

internal fun emptyAggregationResult() =
    AggregationResult(longValues = mapOf(), doubleValues = mapOf(), dataOrigins = setOf())

internal data class AvgData(var count: Int = 0, var total: Double = 0.0) {
    operator fun plusAssign(value: Double) {
        count++
        total += value
    }

    fun average() = total / count
}
