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
@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.records

import android.health.connect.AggregateRecordsRequest
import android.health.connect.LocalTimeRangeFilter
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.TimeRangeFilter as PlatformTimeRangeFilter
import android.health.connect.changelog.ChangeLogTokenRequest
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.Record as PlatformRecord
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
import java.time.LocalDateTime
import java.time.ZoneOffset

fun ReadRecordsRequest<out Record>.toPlatformRequest(
    timeSource: TimeSource
): ReadRecordsRequestUsingFilters<out PlatformRecord> {
    return ReadRecordsRequestUsingFilters.Builder(recordType.toPlatformRecordClass())
        .setTimeRangeFilter(timeRangeFilter.toPlatformTimeRangeFilter(timeSource))
        .setAscending(ascendingOrder)
        .setPageSize(pageSize)
        .apply {
            dataOriginFilter.forEach { addDataOrigins(it.toPlatformDataOrigin()) }
            pageToken?.let { setPageToken(pageToken.toLong()) }
        }
        .build()
}

fun TimeRangeFilter.toPlatformTimeRangeFilter(timeSource: TimeSource): PlatformTimeRangeFilter {
    // TODO(b/272760519): Remove handling for nullable fields in the first two branches. Needed as
    // the values used in the underlining implementation cause long overflow
    return if (startTime != null || endTime != null) {
        TimeInstantRangeFilter.Builder()
            .setStartTime(startTime ?: Instant.EPOCH)
            .setEndTime(endTime ?: timeSource.now)
            .build()
    } else if (localStartTime != null || localEndTime != null) {
        LocalTimeRangeFilter.Builder()
            .setStartTime(localStartTime ?: LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.MIN))
            .setEndTime(localEndTime ?: LocalDateTime.ofInstant(timeSource.now, ZoneOffset.MAX))
            .build()
    } else {
        TimeInstantRangeFilter.Builder()
            .setStartTime(Instant.EPOCH)
            .setEndTime(timeSource.now)
            .build()
    }
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
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource)
        )
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
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource)
        )
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
            timeRangeFilter.toPlatformTimeRangeFilter(timeSource)
        )
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

@Suppress("UNCHECKED_CAST")
fun AggregateMetric<Any>.toAggregationType(): AggregationType<Any> {
    return DOUBLE_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: DURATION_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: ENERGY_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: LENGTH_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: LONG_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: GRAMS_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: POWER_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: VOLUME_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
            ?: throw IllegalArgumentException("Unsupported aggregation type $metricKey")
}
