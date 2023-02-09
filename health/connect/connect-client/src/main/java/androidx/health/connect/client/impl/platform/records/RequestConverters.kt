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
import android.health.connect.datatypes.Record as PlatformRecord
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.time.TimeSource
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

fun ReadRecordsRequest<out Record>.toPlatformRequest(
    timeSource: TimeSource
): ReadRecordsRequestUsingFilters<out PlatformRecord> {
    return ReadRecordsRequestUsingFilters.Builder(recordType.toPlatformRecordClass())
        .setTimeRangeFilter(timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .apply { dataOriginFilter.forEach { addDataOrigins(it.toPlatformDataOrigin()) } }
        .build()
}

fun TimeRangeFilter.toPlatformTimeRangeFilter(timeSource: TimeSource): TimeInstantRangeFilter {
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

@Suppress("UNCHECKED_CAST")
fun AggregateMetric<Any>.toAggregationType(): AggregationType<Any> {
    return ENERGY_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: LENGTH_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: LONG_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: MASS_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: POWER_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: VOLUME_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: MISMATCHING_UNITS_AGGREGATION_METRIC_TYPE_MAP[this]
            ?: throw IllegalArgumentException("Unsupported aggregation type $metricKey")
}
