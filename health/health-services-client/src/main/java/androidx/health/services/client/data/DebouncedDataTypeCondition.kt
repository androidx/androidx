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
import androidx.health.services.client.proto.DataProto.DebouncedDataTypeCondition.DataTypeCase
import java.util.Objects

/**
 * A condition which is considered met when a data type value passes a defined threshold for a
 * specified duration.
 */
public class DebouncedDataTypeCondition<T : Number, D : DataType<T, out DataPoint<T>>>
internal constructor(

    /** [DataType] which this condition applies to. */
    val dataType: D,

    /** The threshold at which point this condition should be met. */
    val threshold: T,

    /** The comparison type to use when comparing the threshold against the current value. */
    val comparisonType: ComparisonType,

    /**
     * The amount of time (in seconds) that must pass before the goal can trigger. Applicable only
     * for sample data types.
     *
     * Example 1: For a DebouncedDataTypeCondition(HeartRate, threshold=100.00,
     * GREATER_THAN_OR_EQUAL, initialDelaySec=60, durationAtThresholdSec=10). If user HeartRate
     * stays above 100.00bpm from t=0s, then the condition will be met on t=60s, since this is when
     * the value has exceeded threshold for consecutively 10 seconds and the 60 seconds of
     * initialDelay has expired.
     *
     * Example 2: For a DebouncedDataTypeCondition(HeartRate, threshold=100.00,
     * GREATER_THAN_OR_EQUAL, initialDelaySec=5, durationAtThresholdSec=10). If user HeartRate stays
     * above 100.00bpm from t=0s, then the condition will be met on t=10s, since this is when the
     * value has exceeded threshold for consecutively 10 seconds and the 5 seconds of initialDelay
     * has expired.
     *
     * The default value is 0, which means trigger whenever the goal has reached threshold, or has
     * reached threshold for a specified durationAtThreshold.
     */
    val initialDelaySeconds: Int = 0,

    /**
     * The amount of time (in seconds) the threshold must be crossed uninterruptedly for this goal
     * to trigger. Applicable only for sample data types.
     *
     * Each time the value moves off threshold will reset durationAtThresholdSec timer. For example:
     * For a DebouncedDataTypeCondition(HeartRate, threshold=100.00, GREATER_THAN_OR_EQUAL,
     * initialDelaySec=60, durationAtThresholdSec=10). If user HeartRate fluctuates around 100.00bpm
     * in the following pattern,
     * 1. from t=0s to t=56s: HeartRate=100
     * 2. at t=57s: HeartRate=99.99
     * 3. from t=58s: HeartRate=100.00. Then the condition will be met on t=68s, since the
     *    durationAtThreshold timer has reset at t=57s and expired at t=68s, and the initialDelay
     *    timer has expired at t=60s.
     *
     * The default value is 0, which means once reached threshold, trigger immediately (if
     * initialDelay has expired).
     */
    val durationAtThresholdSeconds: Int = 0,
) {

    internal val proto: DataProto.DebouncedDataTypeCondition =
        DataProto.DebouncedDataTypeCondition.newBuilder()
            .setThreshold(dataType.toProtoFromValue(threshold))
            .setComparisonType(comparisonType.toProto())
            .setInitialDelaySeconds(initialDelaySeconds)
            .setDurationAtThresholdSeconds(durationAtThresholdSeconds)
            .apply {
                when (dataType.isAggregate) {
                    true -> setAggregate(dataType.proto)
                    false -> setDelta(dataType.proto)
                }
            }
            .build()

    override fun toString(): String =
        "DebouncedDataTypeCondition(" +
            "dataType=$dataType, " +
            "threshold=$threshold, " +
            "comparisonType=$comparisonType," +
            "initialDelaySeconds=$initialDelaySeconds, " +
            "durationAtThresholdSeconds=$durationAtThresholdSeconds" +
            ")"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DebouncedDataTypeCondition<*, *>) return false
        if (dataType != other.dataType) return false
        if (threshold != other.threshold) return false
        if (comparisonType != other.comparisonType) return false
        if (initialDelaySeconds != other.initialDelaySeconds) return false
        if (durationAtThresholdSeconds != other.durationAtThresholdSeconds) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            dataType,
            threshold,
            comparisonType,
            initialDelaySeconds,
            durationAtThresholdSeconds
        )
    }

    companion object {

        /**
         * Creates a [DebouncedDataTypeCondition] for a sample data type, whose value represents an
         * instantaneous value, e.g. instantaneous heart rate, instantaneous speed.
         *
         * @param dataType a delta data type that is associated with [SampleDataPoint]s, and whose
         *   value represents an instantaneous value
         * @param threshold the threshold for the value of this data type to cross in order to
         *   satisfy the condition
         * @param comparisonType the way that determines how to compare the value of the data type
         *   with the threshold in the condition, e.g. greater than, less than or equal
         * @param initialDelaySeconds the amount of time (in seconds) that must pass before the goal
         *   can trigger. Must be greater or equal to zero
         * @param durationAtThresholdSeconds the amount of time (in seconds) the threshold must be
         *   crossed uninterruptedly for this goal to trigger. Must be greater or equal to zero
         */
        @JvmStatic
        fun <
            T : Number,
            D : DeltaDataType<T, out SampleDataPoint<T>>
        > createDebouncedDataTypeCondition(
            dataType: D,
            threshold: T,
            comparisonType: ComparisonType,
            initialDelaySeconds: Int,
            durationAtThresholdSeconds: Int
        ): DebouncedDataTypeCondition<T, D> =
            DebouncedDataTypeCondition(
                dataType,
                threshold,
                comparisonType,
                initialDelaySeconds,
                durationAtThresholdSeconds
            )

        /**
         * Creates a [DebouncedDataTypeCondition] for an aggregate data type, whose value represents
         * an average value, e.g. average heart rate over the tracking period, average speed over
         * the tracking period.
         *
         * @param dataType an aggregate data type that is associated with [StatisticalDataPoint]s,
         *   and whose value represents an average value over the tracking period
         * @param threshold the threshold for the value of this data type to cross in order to
         *   satisfy the condition
         * @param comparisonType the way that determines how to compare the value of the data type
         *   with the threshold in the condition, e.g. greater than, less than or equal
         * @param initialDelaySeconds the amount of time (in seconds) that must pass before the goal
         *   can trigger. Must be greater or equal to zero
         * @param durationAtThresholdSeconds the amount of time (in seconds) the threshold must be
         *   crossed uninterruptedly for this goal to trigger. Must be greater or equal to zero
         */
        @JvmStatic
        fun <
            T : Number,
            D : AggregateDataType<T, out StatisticalDataPoint<T>>
        > createDebouncedDataTypeCondition(
            dataType: D,
            threshold: T,
            comparisonType: ComparisonType,
            initialDelaySeconds: Int,
            durationAtThresholdSeconds: Int
        ): DebouncedDataTypeCondition<T, D> =
            DebouncedDataTypeCondition(
                dataType,
                threshold,
                comparisonType,
                initialDelaySeconds,
                durationAtThresholdSeconds
            )

        @Suppress("UNCHECKED_CAST")
        internal fun fromProto(
            proto: DataProto.DebouncedDataTypeCondition
        ): DebouncedDataTypeCondition<Number, DataType<Number, out DataPoint<Number>>> {
            val dataType =
                when (proto.dataTypeCase) {
                    DataTypeCase.DELTA ->
                        DataType.deltaFromProto(proto.delta)
                            as DeltaDataType<Number, out SampleDataPoint<Number>>
                    DataTypeCase.AGGREGATE ->
                        DataType.aggregateFromProto(proto.aggregate)
                            as AggregateDataType<Number, out StatisticalDataPoint<Number>>
                    else -> throw IllegalStateException("DataType not set on $proto")
                }

            return DebouncedDataTypeCondition(
                dataType,
                dataType.toValueFromProto(proto.threshold),
                ComparisonType.fromProto(proto.comparisonType),
                proto.initialDelaySeconds,
                proto.durationAtThresholdSeconds,
            )
        }
    }
}
