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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)
@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.request

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
import androidx.health.connect.client.impl.platform.aggregate.DOUBLE_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.DURATION_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.ENERGY_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.GRAMS_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.LENGTH_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.LONG_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.POWER_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.PRESSURE_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.TEMPERATURE_DELTA_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.VELOCITY_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.VOLUME_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.impl.platform.aggregate.platformMetrics
import androidx.health.connect.client.impl.platform.records.toPlatformDataOrigin
import androidx.health.connect.client.impl.platform.records.toPlatformRecordClass
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

fun ReadRecordsRequest<out Record>.toPlatformRequest():
    ReadRecordsRequestUsingFilters<out PlatformRecord> {
    return ReadRecordsRequestUsingFilters.Builder(recordType.toPlatformRecordClass())
        .setTimeRangeFilter(timeRangeFilter.toPlatformTimeRangeFilter())
        .setPageSize(pageSize)
        .apply {
            dataOriginFilter.forEach { addDataOrigins(it.toPlatformDataOrigin()) }
            pageToken?.let { setPageToken(it.toLong()) }
            // Platform doesn't allow setting both pageToken and ascendingOrder together.
            if (pageToken == null) {
                setAscending(ascendingOrder)
            }
        }
        .build()
}

fun TimeRangeFilter.toPlatformTimeRangeFilter(): PlatformTimeRangeFilter {
    return if (startTime != null || endTime != null) {
        TimeInstantRangeFilter.Builder().setStartTime(startTime).setEndTime(endTime).build()
    } else if (localStartTime != null || localEndTime != null) {
        LocalTimeRangeFilter.Builder().setStartTime(localStartTime).setEndTime(localEndTime).build()
    } else {
        // Platform doesn't allow both startTime and endTime to be null
        TimeInstantRangeFilter.Builder().setStartTime(Instant.EPOCH).build()
    }
}

fun TimeRangeFilter.toPlatformLocalTimeRangeFilter(): LocalTimeRangeFilter {
    return when {
        localStartTime != null || localEndTime != null ->
            LocalTimeRangeFilter.Builder()
                .setStartTime(localStartTime)
                .setEndTime(localEndTime)
                .build()
        startTime != null || endTime != null ->
            LocalTimeRangeFilter.Builder()
                .setStartTime(startTime?.toLocalDateTime())
                .setEndTime(endTime?.toLocalDateTime())
                .build()
        else ->
            // Platform doesn't allow both startTime and endTime to be null
            LocalTimeRangeFilter.Builder().setStartTime(Instant.EPOCH.toLocalDateTime()).build()
    }
}

private fun Instant.toLocalDateTime() = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

fun ChangesTokenRequest.toPlatformRequest(): ChangeLogTokenRequest {
    return ChangeLogTokenRequest.Builder()
        .apply {
            dataOriginFilters.forEach { addDataOriginFilter(it.toPlatformDataOrigin()) }
            recordTypes.forEach { addRecordType(it.toPlatformRecordClass()) }
        }
        .build()
}

fun AggregateRequest.toPlatformRequest(): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(timeRangeFilter.toPlatformTimeRangeFilter())
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            platformMetrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateGroupByDurationRequest.toPlatformRequest(): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(timeRangeFilter.toPlatformTimeRangeFilter())
        .apply {
            dataOriginFilter.forEach { addDataOriginsFilter(it.toPlatformDataOrigin()) }
            metrics.forEach { addAggregationType(it.toAggregationType()) }
        }
        .build()
}

fun AggregateGroupByPeriodRequest.toPlatformRequest(): AggregateRecordsRequest<Any> {
    return AggregateRecordsRequest.Builder<Any>(timeRangeFilter.toPlatformLocalTimeRangeFilter())
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
        ?: GRAMS_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: LENGTH_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: LONG_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: POWER_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: PRESSURE_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: TEMPERATURE_DELTA_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: VELOCITY_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: VOLUME_AGGREGATION_METRIC_TYPE_MAP[this] as AggregationType<Any>?
        ?: throw IllegalArgumentException("Unsupported aggregation type $metricKey")
}
