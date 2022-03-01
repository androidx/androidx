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
package androidx.health.data.client.aggregate

import java.time.Duration

/**
 * Represents an aggregation result row.
 *
 * See [HealthDataClient.aggregate]
 */
class AggregateDataRow
internal constructor(
    // TODO(b/219327548): Accommodate optional aggregate groupBy keys (time range) when we add
    // groupBy.
    internal val longValues: Map<String, Long>,
    internal val doubleValues: Map<String, Double>
) {

    /**
     * Checks whether the aggregation result contains a metric or not. If there is no relevant
     * record that contribute to requested metric, the metric will not be provided.
     *
     * @param metric an aggregate metric identifier
     * @return whether given metric is set
     */
    fun hasMetric(metric: AggregateMetric): Boolean {
        return when (metric) {
            is LongAggregateMetric, is DurationAggregateMetric ->
                longValues.containsKey(metric.metricKey)
            is DoubleAggregateMetric -> doubleValues.containsKey(metric.metricKey)
            else -> false
        }
    }

    /**
     * Retrieves a metric of type `Long` with a given metric identifier.
     *
     * @throws NullPointerException if given metric is not set
     */
    fun getMetric(metric: LongAggregateMetric): Long {
        return checkNotNull(longValues.get(metric.metricKey))
    }

    /**
     * Retrieves a metric of type `Double` with a given metric identifier.
     *
     * @throws NullPointerException if given metric is not set
     */
    fun getMetric(metric: DoubleAggregateMetric): Double {
        return checkNotNull(doubleValues.get(metric.metricKey))
    }

    /**
     * Retrieves a metric of type `Duration` with a given metric identifier.
     *
     * @throws NullPointerException if given metric is not set
     */
    @SuppressWarnings("NewApi") // Safe to use as supported API >= 27
    fun getMetric(metric: DurationAggregateMetric): Duration {
        return Duration.ofMillis(checkNotNull(longValues.get(metric.metricKey)))
    }
}
