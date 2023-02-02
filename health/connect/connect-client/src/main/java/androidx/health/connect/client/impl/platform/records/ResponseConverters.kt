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
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.units.Energy as PlatformEnergy
import android.health.connect.datatypes.units.Mass
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.units.Energy
import java.time.ZoneOffset

private val DOUBLE_AGGREGATION_TYPE_CONVERTERS: Map<AggregationType<out Any>, (Any) -> Double> =
    mapOf(
        NutritionRecord.CAFFEINE_TOTAL to { (it as Mass).inKilograms },
        NutritionRecord.ENERGY_TOTAL to
            {
                Energy.joules((it as PlatformEnergy).inJoules).inKilocalories
            })

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

private fun getLongMetricValues(metricValueMap: Map<AggregateMetric<Any>, Any>): Map<String, Long> {
    return buildMap {
        metricValueMap
            .filterKeys { it.isLongAggregationType() }
            .forEach { this[it.key.metricKey] = it.value as Long }
    }
}

private fun getDoubleMetricValues(
    metricValueMap: Map<AggregateMetric<Any>, Any>
): Map<String, Double> {
    return buildMap {
        metricValueMap
            .filterKeys { it.isDoubleAggregationType() }
            .forEach {
                DOUBLE_AGGREGATION_TYPE_CONVERTERS[it.key.toAggregationType()]?.also { convert ->
                    this[it.key.metricKey] = convert(it.value)
                }
            }
    }
}
