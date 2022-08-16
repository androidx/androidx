/*
 * Copyright (C) 2021 The Android Open Source Project
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

/** A condition which is considered met when a data type value passes a defined threshold. */
@Suppress("ParcelCreator")
public class DataTypeCondition<T : Number, D : DataType<T, out DataPoint<T>>>(
    /** [DataType] which this condition applies to. */
    public val dataType: D,

    /** The threshold at which point this condition should be met. */
    public val threshold: T,

    /** The comparison type to use when comparing the threshold against the current value. */
    public val comparisonType: ComparisonType,
) {

    internal val proto: DataProto.DataTypeCondition by lazy {
        DataProto.DataTypeCondition.newBuilder()
            .setDataType(dataType.proto)
            .setThreshold(dataType.toProtoFromValue(threshold))
            .setComparisonType(comparisonType.toProto())
            .build()
    }

    override fun toString(): String =
        "DataTypeCondition(" +
            "dataType=$dataType, threshold=$threshold, comparisonType=$comparisonType)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataTypeCondition<*, *>) return false
        if (dataType != other.dataType) return false
        if (threshold != other.threshold) return false
        if (comparisonType != other.comparisonType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dataType.hashCode()
        result = 31 * result + threshold.hashCode()
        result = 31 * result + comparisonType.hashCode()
        return result
    }

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun deltaFromProto(
            proto: DataProto.DataTypeCondition
        ): DataTypeCondition<out Number, out DeltaDataType<out Number, *>> {
            val dataType =
                DataType.deltaFromProto(proto.dataType) as DeltaDataType<Number, *>
            return DataTypeCondition(
                dataType,
                dataType.toValueFromProto(proto.threshold),
                ComparisonType.fromProto(proto.comparisonType)
            )
        }

        @Suppress("UNCHECKED_CAST")
        internal fun aggregateFromProto(
            proto: DataProto.DataTypeCondition
        ): DataTypeCondition<out Number, out AggregateDataType<out Number, *>> {
            val dataType =
                DataType.aggregateFromProto(proto.dataType) as AggregateDataType<Number, *>
            return DataTypeCondition(
                dataType,
                dataType.toValueFromProto(proto.threshold),
                ComparisonType.fromProto(proto.comparisonType)
            )
        }
    }
}
