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

@file:JvmName("AggregationTestUtils")

package androidx.health.connect.client.testing

import android.annotation.SuppressLint
import androidx.health.connect.client.ExperimentalHealthConnectApi
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.impl.platform.aggregate.KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Volume
import java.time.Duration

/** Creates an AggregationResult from a map of metrics. */
@JvmOverloads
@ExperimentalHealthConnectApi
public fun AggregationResult(
    dataOrigins: Set<DataOrigin> = emptySet(),
    metrics: Map<AggregateMetric<Any>, Any> = emptyMap()
): AggregationResult {
    val longValuesMap = mutableMapOf<String, Long>()
    val doubleValuesMap = mutableMapOf<String, Double>()
    for ((metric, value) in metrics) {
        val metricKey = metric.metricKey
        @SuppressLint("NewApi")
        when (value) {
            is Long -> longValuesMap[metricKey] = value
            is Double -> doubleValuesMap[metricKey] = value
            is Duration -> longValuesMap[metricKey] = value.toMillis()
            is Energy -> doubleValuesMap[metricKey] = value.inKilocalories
            is Length -> doubleValuesMap[metricKey] = value.inMeters
            // Using platform mappings in Jetpack, won't access any platform values
            is Mass ->
                doubleValuesMap[metricKey] =
                    if (KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP.containsKey(metric)) value.inKilograms
                    else value.inGrams
            is Power -> doubleValuesMap[metricKey] = value.inWatts
            is Volume -> doubleValuesMap[metricKey] = value.inLiters
            is Pressure -> doubleValuesMap[metricKey] = value.inMillimetersOfMercury
        }
    }
    return AggregationResult(longValuesMap, doubleValuesMap, dataOrigins)
}
