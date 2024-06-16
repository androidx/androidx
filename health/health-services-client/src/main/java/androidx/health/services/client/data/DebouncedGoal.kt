/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.health.services.client.proto.DataProto
import java.util.Objects

/**
 * Defines a debounced goal for an exercise. Debounced means, the goal will be triggered only after
 * the threshold has been crossed for a specified duration of time, e.g. initialDelay and
 * durationAtThreshold. Only applies to sample data types(e.g. heart rate, speed) and aggregate data
 * type with statistical data points(e.g. pace stats).
 */
class DebouncedGoal<T : Number>
private constructor(

    /**
     * The condition which specifies data type, threshold, comparison type and debounced params. The
     * condition must be met in order to trigger the goal.
     */
    val debouncedDataTypeCondition: DebouncedDataTypeCondition<T, *>,
) {

    internal val proto: DataProto.DebouncedGoal =
        DataProto.DebouncedGoal.newBuilder()
            .setDebouncedDataTypeCondition(debouncedDataTypeCondition.proto)
            .build()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is DebouncedGoal<*>) {
            return false
        }

        return debouncedDataTypeCondition == other.debouncedDataTypeCondition
    }

    override fun hashCode(): Int {
        return Objects.hash(debouncedDataTypeCondition)
    }

    override fun toString(): String =
        "DebouncedGoal(debouncedDataTypeCondition=$debouncedDataTypeCondition)"

    companion object {

        internal fun fromProto(proto: DataProto.DebouncedGoal): DebouncedGoal<Number> {
            val condition = DebouncedDataTypeCondition.fromProto(proto.debouncedDataTypeCondition)
            return DebouncedGoal(condition)
        }

        /**
         * Creates a [DebouncedGoal] that is achieved once when given [DebouncedDataTypeCondition]
         * is satisfied for the [DeltaDataType].
         *
         * @param condition the debounced data type condition for a sample data type, and whose
         *   value represents an instantaneous value, e.g. instantaneous heart rate
         * @return a debounced goal that is triggered when the condition is met
         */
        @JvmStatic
        fun <T : Number> createSampleDebouncedGoal(
            condition: DebouncedDataTypeCondition<T, DeltaDataType<T, SampleDataPoint<T>>>
        ): DebouncedGoal<T> {
            return DebouncedGoal(condition)
        }

        /**
         * Creates a [DebouncedGoal] that is achieved once when given [DebouncedDataTypeCondition]
         * is satisfied for the [AggregateDataType].
         *
         * @param condition the debounced data type condition for an aggregate data type, and whose
         *   value represents an average value, e.g. average heart rate
         * @return a debounced goal that is triggered when the condition is met
         */
        @JvmStatic
        fun <T : Number> createAggregateDebouncedGoal(
            condition: DebouncedDataTypeCondition<T, AggregateDataType<T, StatisticalDataPoint<T>>>
        ): DebouncedGoal<T> = DebouncedGoal(condition)
    }
}
