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
import androidx.health.connect.client.impl.platform.div
import androidx.health.connect.client.impl.platform.duration
import androidx.health.connect.client.impl.platform.minus
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
import java.time.Instant
import kotlin.math.max
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Max buffer to account for overlapping records that have startTime < timeRangeFilter.startTime
val RECORD_START_TIME_BUFFER: Duration = Duration.ofDays(1)

internal suspend fun HealthConnectClient.aggregateFallback(request: AggregateRequest):
    AggregationResult {
    return request.fallbackMetrics
        .fold(emptyAggregationResult()) { currentAggregateResult, metric ->
            currentAggregateResult + aggregate(
                metric,
                request.timeRangeFilter,
                request.dataOriginFilter
            )
        }
}

private suspend fun <T : Any> HealthConnectClient.aggregate(
    metric: AggregateMetric<T>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
): AggregationResult {
    return when (metric) {
        NutritionRecord.TRANS_FAT_TOTAL -> aggregateNutritionTransFatTotal(
            timeRangeFilter,
            dataOriginFilter
        )

        BloodPressureRecord.DIASTOLIC_AVG -> TODO(reason = "b/326414908")
        BloodPressureRecord.DIASTOLIC_MAX -> TODO(reason = "b/326414908")
        BloodPressureRecord.DIASTOLIC_MIN -> TODO(reason = "b/326414908")
        BloodPressureRecord.SYSTOLIC_AVG -> TODO(reason = "b/326414908")
        BloodPressureRecord.SYSTOLIC_MAX -> TODO(reason = "b/326414908")
        BloodPressureRecord.SYSTOLIC_MIN -> TODO(reason = "b/326414908")
        CyclingPedalingCadenceRecord.RPM_AVG -> TODO(reason = "b/326414908")
        CyclingPedalingCadenceRecord.RPM_MAX -> TODO(reason = "b/326414908")
        CyclingPedalingCadenceRecord.RPM_MIN -> TODO(reason = "b/326414908")
        SpeedRecord.SPEED_AVG -> TODO(reason = "b/326414908")
        SpeedRecord.SPEED_MAX -> TODO(reason = "b/326414908")
        SpeedRecord.SPEED_MIN -> TODO(reason = "b/326414908")
        StepsCadenceRecord.RATE_AVG -> TODO(reason = "b/326414908")
        StepsCadenceRecord.RATE_MAX -> TODO(reason = "b/326414908")
        StepsCadenceRecord.RATE_MIN -> TODO(reason = "b/326414908")
        else -> error("Invalid fallback aggregation type ${metric.metricKey}")
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
            val response = readRecords(
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
        startTimeOverlaps = timeRangeFilter.localEndTime == null ||
            startTime.isBefore(
                timeRangeFilter.localEndTime.toInstantWithDefaultZoneFallback(startZoneOffset)
            )
        endTimeOverlaps = timeRangeFilter.localStartTime == null ||
            endTime.isAfter(
                timeRangeFilter.localStartTime.toInstantWithDefaultZoneFallback(endZoneOffset)
            )
    } else {
        startTimeOverlaps = timeRangeFilter.endTime == null ||
            startTime.isBefore(timeRangeFilter.endTime)
        endTimeOverlaps = timeRangeFilter.startTime == null ||
            endTime.isAfter(timeRangeFilter.startTime)
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

internal fun sliceFactor(record: NutritionRecord, timeRangeFilter: TimeRangeFilter): Double {
    val startTime: Instant
    val endTime: Instant

    if (timeRangeFilter.useLocalTime()) {
        val requestStartTime =
            timeRangeFilter.localStartTime?.toInstantWithDefaultZoneFallback(record.startZoneOffset)
        val requestEndTime =
            timeRangeFilter.localEndTime?.toInstantWithDefaultZoneFallback(record.endZoneOffset)
        startTime = maxOf(record.startTime, requestStartTime ?: record.startTime)
        endTime = minOf(record.endTime, requestEndTime ?: record.endTime)
    } else {
        startTime = maxOf(record.startTime, timeRangeFilter.startTime ?: record.startTime)
        endTime = minOf(record.endTime, timeRangeFilter.endTime ?: record.endTime)
    }

    return max(0.0, (endTime - startTime) / record.duration)
}

internal fun emptyAggregationResult() =
    AggregationResult(longValues = mapOf(), doubleValues = mapOf(), dataOrigins = setOf())

internal data class AggregatedData<T>(
    var value: T,
    var dataOrigins: MutableSet<DataOrigin> = mutableSetOf()
)
