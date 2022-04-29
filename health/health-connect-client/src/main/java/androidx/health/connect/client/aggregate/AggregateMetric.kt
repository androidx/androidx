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

import java.time.Duration
import kotlin.reflect.KClass

/**
 * Identifier to supported metrics for aggregation.
 *
 * @see AggregationResult.hasMetric
 * @see AggregationResult.getMetric
 */
class AggregateMetric<T : Any>
internal constructor(
    /**
     * Return type of aggregation, such as `Long`, `Double`, `Duration` etc. Internal to SDK only.
     */
    internal val type: KClass<T>,
    /**
     * Data type name of the aggregation, such as `Nutrition`, `Speed` etc. Internal to SDK only.
     */
    internal val dataTypeName: String,
    /** Type of aggregation, such as `total`, `avg` etc. Internal to SDK only. */
    internal val aggregationType: AggregationType,
    /**
     * Field name of the aggregation metric, such as `vitaminC`, `speed` etc. Internal to SDK only.
     */
    internal val aggregationField: String?
) {
    /** Supported aggregation types. Internal for SDK use only. */
    internal enum class AggregationType(
        /** Serialization string for the aggregation type. */
        val aggregationTypeString: String
    ) {
        DURATION("duration"),
        AVERAGE("avg"),
        MINIMUM("min"),
        MAXIMUM("max"),
        TOTAL("total"),
        COUNT("count"),
    }

    internal companion object {
        /**
         * Creates a metric with type [Duration] that sum up duration of intervals. Internal for SDK
         * use only.
         */
        internal fun durationMetric(dataTypeName: String): AggregateMetric<Duration> {
            return AggregateMetric(
                Duration::class,
                dataTypeName,
                aggregationType = AggregationType.DURATION,
                aggregationField = null
            )
        }

        /** Creates a metric with type [Double]. Internal for SDK use only. */
        internal fun doubleMetric(
            dataTypeName: String,
            aggregationType: AggregationType,
            fieldName: String
        ): AggregateMetric<Double> {
            return AggregateMetric(Double::class, dataTypeName, aggregationType, fieldName)
        }

        /** Creates a metric with type [Long]. Internal for SDK use only. */
        internal fun longMetric(
            dataTypeName: String,
            aggregationType: AggregationType,
            fieldName: String
        ): AggregateMetric<Long> {
            return AggregateMetric(Long::class, dataTypeName, aggregationType, fieldName)
        }

        /**
         * Creates a [AggregateMetric] returning sample or record counts. Internal for SDK use only.
         */
        internal fun countMetric(dataTypeName: String): AggregateMetric<Long> {
            return AggregateMetric(
                Long::class,
                dataTypeName,
                AggregationType.COUNT,
                aggregationField = null
            )
        }
    }

    internal val metricKey: String
        get() {
            val aggregationTypeString = aggregationType.aggregationTypeString
            return if (aggregationField == null) {
                "${dataTypeName}_$aggregationTypeString"
            } else {
                "${dataTypeName}_${aggregationField}_$aggregationTypeString"
            }
        }
}
