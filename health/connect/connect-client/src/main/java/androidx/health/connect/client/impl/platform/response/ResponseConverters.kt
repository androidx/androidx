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

package androidx.health.connect.client.impl.platform.response

import android.annotation.SuppressLint
import android.health.connect.AggregateRecordsGroupedByDurationResponse
import android.health.connect.AggregateRecordsGroupedByPeriodResponse
import android.health.connect.AggregateRecordsResponse
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.units.Energy as PlatformEnergy
import android.health.connect.datatypes.units.Volume as PlatformVolume
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
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
import androidx.health.connect.client.impl.platform.records.PlatformDataOrigin
import androidx.health.connect.client.impl.platform.records.PlatformLength
import androidx.health.connect.client.impl.platform.records.PlatformMass
import androidx.health.connect.client.impl.platform.records.PlatformPower
import androidx.health.connect.client.impl.platform.records.PlatformPressure
import androidx.health.connect.client.impl.platform.records.PlatformTemperatureDelta
import androidx.health.connect.client.impl.platform.records.PlatformVelocity
import androidx.health.connect.client.impl.platform.records.toSdkDataOrigin
import androidx.health.connect.client.impl.platform.request.toAggregationType
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val BUCKET_DATA_ORIGINS_EXTENSION_VERSION = 10

fun AggregateRecordsResponse<Any>.toSdkResponse(metrics: Set<AggregateMetric<Any>>) =
    buildAggregationResult(metrics, ::get, ::getDataOrigins)

fun AggregateRecordsGroupedByDurationResponse<Any>.toSdkResponse(
    metrics: Set<AggregateMetric<Any>>
): AggregationResultGroupedByDuration {
    val platformDataOriginsGetter: (AggregationType<Any>) -> Set<PlatformDataOrigin> =
        if (
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >=
                BUCKET_DATA_ORIGINS_EXTENSION_VERSION
        ) {
            ::getDataOrigins
        } else {
            { emptySet() }
        }
    return AggregationResultGroupedByDuration(
        buildAggregationResult(metrics, ::get, platformDataOriginsGetter),
        startTime,
        endTime,
        metrics.firstNotNullOfOrNull { getZoneOffset(it.toAggregationType()) }
            ?: ZoneOffset.systemDefault().rules.getOffset(startTime),
        shouldSkipValidation = true,
    )
}

fun AggregateRecordsGroupedByPeriodResponse<Any>.toSdkResponse(metrics: Set<AggregateMetric<Any>>) =
    toSdkResponse(metrics, startTime, endTime)

fun AggregateRecordsGroupedByPeriodResponse<Any>.toSdkResponse(
    metrics: Set<AggregateMetric<Any>>,
    bucketStartTime: LocalDateTime,
    bucketEndTime: LocalDateTime,
): AggregationResultGroupedByPeriod {
    val platformDataOriginsGetter: (AggregationType<Any>) -> Set<PlatformDataOrigin> =
        if (
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >=
                BUCKET_DATA_ORIGINS_EXTENSION_VERSION
        ) {
            ::getDataOrigins
        } else {
            { emptySet() }
        }
    return AggregationResultGroupedByPeriod(
        buildAggregationResult(metrics, ::get, platformDataOriginsGetter),
        bucketStartTime,
        bucketEndTime,
        shouldSkipValidation = true,
    )
}

@VisibleForTesting
internal fun buildAggregationResult(
    metrics: Set<AggregateMetric<Any>>,
    aggregationValueGetter: (AggregationType<Any>) -> Any?,
    platformDataOriginsGetter: (AggregationType<Any>) -> Set<PlatformDataOrigin>,
): AggregationResult {
    val metricValueMap = buildMap {
        metrics.forEach { metric ->
            aggregationValueGetter(metric.toAggregationType())?.also { this[metric] = it }
        }
    }
    return AggregationResult(
        getLongMetricValues(metricValueMap),
        getDoubleMetricValues(metricValueMap),
        metrics.flatMapTo(hashSetOf()) { metric ->
            platformDataOriginsGetter(metric.toAggregationType()).map { it.toSdkDataOrigin() }
        },
    )
}

@VisibleForTesting
internal fun getLongMetricValues(
    metricValueMap: Map<AggregateMetric<Any>, Any>
): Map<String, Long> {
    return buildMap {
        metricValueMap.forEach { (key, value) ->
            if (
                key in DURATION_AGGREGATION_METRIC_TYPE_MAP ||
                    key in LONG_AGGREGATION_METRIC_TYPE_MAP
            ) {
                this[key.metricKey] = value as Long
            }
        }
    }
}

@SuppressLint("NewApi") // Api 35 covered by sdk extension check
@VisibleForTesting
internal fun getDoubleMetricValues(
    metricValueMap: Map<AggregateMetric<Any>, Any>
): Map<String, Double> {
    return buildMap {
        metricValueMap.forEach { (key, value) ->
            when (key) {
                in DOUBLE_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = value as Double
                }
                in ENERGY_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] =
                        Energy.calories((value as PlatformEnergy).inCalories).inKilocalories
                }
                in GRAMS_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = (value as PlatformMass).inGrams
                }
                in LENGTH_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = (value as PlatformLength).inMeters
                }
                in KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = Mass.grams((value as PlatformMass).inGrams).inKilograms
                }
                in PRESSURE_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = (value as PlatformPressure).inMillimetersOfMercury
                }
                in POWER_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = (value as PlatformPower).inWatts
                }
                in TEMPERATURE_DELTA_METRIC_TYPE_MAP -> {
                    require(
                        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >=
                            13
                    )
                    this[key.metricKey] = (value as PlatformTemperatureDelta).inCelsius
                }
                in VELOCITY_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = (value as PlatformVelocity).inMetersPerSecond
                }
                in VOLUME_AGGREGATION_METRIC_TYPE_MAP -> {
                    this[key.metricKey] = (value as PlatformVolume).inLiters
                }
            }
        }
    }
}
