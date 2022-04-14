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

import androidx.health.connect.client.metadata.DataOrigin
import java.lang.IllegalArgumentException
import java.time.Duration

/**
 * Contains results of requested [AggregateMetric].
 *
 * To retrieve aggregate metrics:
 * ```
 * val result = healthConnectClient.aggregate(
 *   metrics = setOf(Steps.TOTAL, Distance.TOTAL)
 * )
 * val totalSteps = result.getMetric(Steps.TOTAL)
 * val totalDistance = result.getMetric(Distance.TOTAL)
 * ```
 *
 * @see [androidx.health.connect.client.HealthConnectClient.aggregate]
 */
class AggregationResult
internal constructor(
    internal val longValues: Map<String, Long>,
    internal val doubleValues: Map<String, Double>,
    /** List of [DataOrigin]s that contributed to the aggregation result. */
    public val dataOrigins: List<DataOrigin>
) {

    /**
     * Checks whether the aggregation result contains a metric or not. If there is no relevant
     * record that contribute to requested metric, the metric will not be provided.
     *
     * @param metric an aggregate metric identifier.
     * @return whether given metric is set.
     */
    fun hasMetric(metric: AggregateMetric<*>): Boolean {
        return when (metric.type) {
            Long::class, Duration::class -> longValues.containsKey(metric.metricKey)
            Double::class -> doubleValues.containsKey(metric.metricKey)
            else -> false
        }
    }

    /**
     * Retrieves a metric with given metric identifier.
     *
     * If there are no relevant records contributing to the requested metric, the metric will not be
     * provided.
     *
     * @return the value of the metric, or null if not set.
     * @throws IllegalArgumentException for invalid argument with metric not defined within SDK.
     *
     * @see hasMetric
     */
    fun <T : Any> getMetric(metric: AggregateMetric<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return when (metric.type) {
            Long::class -> longValues[metric.metricKey] as? T
            Duration::class -> longValues[metric.metricKey]?.let { Duration.ofMillis(it) } as? T
            Double::class -> doubleValues[metric.metricKey] as? T
            else -> throw IllegalArgumentException("Unsupported metric type")
        }
    }
}
