/*
 * Copyright 2023 The Android Open Source Project
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

import android.health.connect.AggregateRecordsGroupedByDurationResponse
import android.health.connect.AggregateRecordsGroupedByPeriodResponse
import android.health.connect.AggregateRecordsResponse
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.units.Energy as PlatformEnergy
import android.health.connect.datatypes.units.Length as PlatformLength
import android.health.connect.datatypes.units.Mass as PlatformMass
import android.health.connect.datatypes.units.Power as PlatformPower
import android.health.connect.datatypes.units.Volume as PlatformVolume
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import java.time.ZoneOffset

fun AggregateRecordsResponse<Any>.toSdkResponse(metrics: Set<AggregateMetric<Any>>) =
    buildAggregationResult(metrics, ::get)

fun AggregateRecordsGroupedByDurationResponse<Any>.toSdkResponse(
    metrics: Set<AggregateMetric<Any>>
) =
    AggregationResultGroupedByDuration(
        buildAggregationResult(metrics, ::get),
        startTime,
        endTime,
        getZoneOffset(metrics.first().toAggregationType())
            ?: ZoneOffset.systemDefault().rules.getOffset(startTime))

fun AggregateRecordsGroupedByPeriodResponse<Any>.toSdkResponse(metrics: Set<AggregateMetric<Any>>) =
    AggregationResultGroupedByPeriod(buildAggregationResult(metrics, ::get), startTime, endTime)

private fun buildAggregationResult(
    metrics: Set<AggregateMetric<Any>>,
    aggregationValueGetter: (AggregationType<Any>) -> Any?
): AggregationResult {
    val metricValueMap = buildMap {
        metrics.forEach { metric ->
            aggregationValueGetter(metric.toAggregationType())?.also { this[metric] = it }
        }
    }
    return AggregationResult(
        getLongMetricValues(metricValueMap), getDoubleMetricValues(metricValueMap), setOf())
}

@VisibleForTesting
internal fun getLongMetricValues(
    metricValueMap: Map<AggregateMetric<Any>, Any>
): Map<String, Long> {
    return buildMap {
        metricValueMap.forEach {
            LONG_AGGREGATION_METRIC_TYPE_MAP[it.key]?.also { _ ->
                this[it.key.metricKey] = it.value as Long
            }
        }
    }
}

@VisibleForTesting
internal fun getDoubleMetricValues(
    metricValueMap: Map<AggregateMetric<Any>, Any>
): Map<String, Double> {
    return buildMap {
        metricValueMap.forEach {
            ENERGY_AGGREGATION_METRIC_TYPE_MAP[it.key]?.also { _ ->
                this[it.key.metricKey] =
                    Energy.joules((it.value as PlatformEnergy).inJoules).inKilocalories
            }
                ?: LENGTH_AGGREGATION_METRIC_TYPE_MAP[it.key]?.also { _ ->
                    this[it.key.metricKey] = (it.value as PlatformLength).inMeters
                }
                    ?: MASS_AGGREGATION_METRIC_TYPE_MAP[it.key]?.also { _ ->
                    this[it.key.metricKey] =
                        Mass.kilograms((it.value as PlatformMass).inKilograms).inGrams
                }
                    ?: POWER_AGGREGATION_METRIC_TYPE_MAP[it.key]?.also { _ ->
                    this[it.key.metricKey] = (it.value as PlatformPower).inWatts
                }
                    ?: VOLUME_AGGREGATION_METRIC_TYPE_MAP[it.key]?.also { _ ->
                    this[it.key.metricKey] =
                        Volume.milliliters((it.value as PlatformVolume).inMilliliters).inLiters
                }
                    ?: it.key
                    .takeIf { aggregateMetric ->
                        aggregateMetric == FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL
                    }
                    ?.also { _ -> this[it.key.metricKey] = (it.value as Long).toDouble() }
        }
    }
}
