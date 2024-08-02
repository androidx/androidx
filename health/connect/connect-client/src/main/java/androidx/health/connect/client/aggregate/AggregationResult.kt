/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.aggregate

import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric.Converter
import androidx.health.connect.client.records.metadata.DataOrigin

/**
 * Contains results of requested [AggregateMetric].
 *
 * To retrieve aggregate metrics:
 * ```
 * val result = healthConnectClient.aggregate(
 *   metrics = setOf(Steps.COUNT_TOTAL, Distance.DISTANCE_TOTAL)
 * )
 * val totalSteps = result[Steps.COUNT_TOTAL]
 * val totalDistance = result[Distance.DISTANCE_TOTAL]
 * ```
 *
 * @see [androidx.health.connect.client.HealthConnectClient.aggregate]
 */
class AggregationResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val longValues: Map<String, Long>,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val doubleValues: Map<String, Double>,
    /** Set of [DataOrigin]s that contributed to the aggregation result. */
    public val dataOrigins: Set<DataOrigin>
) {

    /**
     * Checks whether the aggregation result contains a metric or not. If there is no relevant
     * record that contribute to requested metric, the metric will not be provided.
     *
     * @param metric an aggregate metric identifier.
     * @return whether given metric is set.
     */
    operator fun contains(metric: AggregateMetric<*>): Boolean =
        when (metric.converter) {
            is Converter.FromLong -> metric.metricKey in longValues
            is Converter.FromDouble -> metric.metricKey in doubleValues
        }

    /**
     * Retrieves a metric with given metric identifier.
     *
     * If there are no relevant records contributing to the requested metric, the metric will not be
     * provided.
     *
     * @return the value of the metric, or null if not set.
     * @see contains
     */
    operator fun <T : Any> get(metric: AggregateMetric<T>): T? =
        when (metric.converter) {
            is Converter.FromLong -> longValues[metric.metricKey]?.let(metric.converter)
            is Converter.FromDouble -> doubleValues[metric.metricKey]?.let(metric.converter)
        }
}
