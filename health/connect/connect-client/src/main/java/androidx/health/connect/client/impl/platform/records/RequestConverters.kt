/*
 * Copyright 2022 The Android Open Source Project
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
@file:RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

package androidx.health.connect.client.impl.platform.records

import android.health.connect.AggregateRecordsRequest
import android.health.connect.ChangeLogTokenRequest
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.HeartRateRecord as PlatformHeartRateRecord
import android.health.connect.datatypes.NutritionRecord as PlatformNutritionRecord
import android.health.connect.datatypes.Record as PlatformRecord
import android.health.connect.datatypes.StepsRecord as PlatformStepsRecord
import android.health.connect.datatypes.WheelchairPushesRecord as PlatformWheelchairPushesRecord
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.time.TimeSource
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

@Suppress("UNCHECKED_CAST")
private val LONG_AGGREGATION_METRIC_TYPE_MAP: Map<AggregateMetric<Any>, AggregationType<Any>> =
    mapOf(
        StepsRecord.COUNT_TOTAL to PlatformStepsRecord.STEPS_COUNT_TOTAL as AggregationType<Any>,
        HeartRateRecord.BPM_MIN to PlatformHeartRateRecord.BPM_MIN as AggregationType<Any>,
        HeartRateRecord.BPM_MAX to PlatformHeartRateRecord.BPM_MAX as AggregationType<Any>,
        HeartRateRecord.BPM_AVG to PlatformHeartRateRecord.BPM_AVG as AggregationType<Any>,
        WheelchairPushesRecord.COUNT_TOTAL to
            PlatformWheelchairPushesRecord.WHEEL_CHAIR_PUSHES_COUNT_TOTAL as AggregationType<Any>)

@Suppress("UNCHECKED_CAST")
private val DOUBLE_AGGREGATION_METRIC_TYPE_MAP: Map<AggregateMetric<Any>, AggregationType<Any>> =
    mapOf(
        NutritionRecord.CAFFEINE_TOTAL to
            PlatformNutritionRecord.CAFFEINE_TOTAL as AggregationType<Any>,
        NutritionRecord.ENERGY_TOTAL to
            PlatformNutritionRecord.ENERGY_TOTAL as AggregationType<Any>)

fun ReadRecordsRequest<out Record>.toPlatformRequest(
    timeSource: TimeSource
): ReadRecordsRequestUsingFilters<out PlatformRecord> {
    return ReadRecordsRequestUsingFilters.Builder(recordType.toPlatformRecordClass())
        .setTimeRangeFilter(timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply { dataOriginFilter.forEach { addDataOrigins(it.toPlatformDataOrigin()) } }
        .build()
}

fun TimeRangeFilter.toPlatformTimeRangeFilter(
    timeSource: TimeSource
): TimeInstantRangeFilter {
    // TODO(b/262571990): pass nullable Instant start/end
    // TODO(b/262571990): pass nullable LocalDateTime start/end
    return TimeInstantRangeFilter.Builder()
        .setStartTime(startTime ?: Instant.EPOCH)
        .setEndTime(endTime ?: timeSource.now)
        .build()
}

fun ChangesTokenRequest.toPlatformRequest(): ChangeLogTokenRequest {
    return ChangeLogTokenRequest.Builder()
        .apply {
            dataOriginFilters.forEach { addDataOriginFilter(it.toPlatformDataOrigin()) }
            recordTypes.forEach { addRecordType(it.toPlatformRecordClass()) }
        }
        .build()
}

fun AggregateRequest.toPlatformRequest(timeSource: TimeSource): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateGroupByDurationRequest.toPlatformRequest(
    timeSource: TimeSource
): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateGroupByPeriodRequest.toPlatformRequest(
    timeSource: TimeSource
): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateMetric<Any>.toAggregationType(): AggregationType<Any> {
    return LONG_AGGREGATION_METRIC_TYPE_MAP[this]
        ?: DOUBLE_AGGREGATION_METRIC_TYPE_MAP[this]
            ?: throw IllegalArgumentException("Unsupported aggregation type $metricKey")
}

fun AggregateMetric<Any>.isLongAggregationType() =
    LONG_AGGREGATION_METRIC_TYPE_MAP.containsKey(this)

fun AggregateMetric<Any>.isDoubleAggregationType() =
    DOUBLE_AGGREGATION_METRIC_TYPE_MAP.containsKey(this)
