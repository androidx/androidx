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

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto

/** A condition which is considered met when a data type value passes a defined threshold. */
@Suppress("ParcelCreator")
public class DataTypeCondition(
    public val dataType: DataType,
    public val threshold: Value,
    public val comparisonType: ComparisonType,
) : ProtoParcelable<DataProto.DataTypeCondition>() {

    internal constructor(
        proto: DataProto.DataTypeCondition
    ) : this(
        DataType(proto.dataType),
        Value(proto.threshold),
        ComparisonType.fromProto(proto.comparisonType)
            ?: throw IllegalStateException("Invalid ComparisonType: ${proto.comparisonType}")
    )

    init {
        require(dataType.format == threshold.format) {
            "provided data type and threshold must have the same formats."
        }
    }

    /** @hide */
    override val proto: DataProto.DataTypeCondition by lazy {
        DataProto.DataTypeCondition.newBuilder()
            .setDataType(dataType.proto)
            .setThreshold(threshold.proto)
            .setComparisonType(comparisonType.toProto())
            .build()
    }

    override fun toString(): String =
        "DataTypeCondition(" +
            "dataType=$dataType, threshold=$threshold, comparisonType=$comparisonType)"

    /** Checks whether or not the condition is satisfied by a given [DataPoint]. */
    public fun isSatisfied(dataPoint: DataPoint): Boolean {
        require(dataType == dataPoint.dataType) {
            "attempted to evaluate data type condition with incorrect data type. Expected " +
                "${dataType.name} but was ${dataPoint.dataType.name}"
        }
        return isThresholdSatisfied(dataPoint.value)
    }

    /** Checks whether or not the value of the condition is satisfied by a given [Value]. */
    public fun isThresholdSatisfied(value: Value): Boolean {
        val comparison = Value.compare(value, threshold)
        return when (comparisonType) {
            ComparisonType.LESS_THAN -> comparison < 0
            ComparisonType.GREATER_THAN -> comparison > 0
            ComparisonType.LESS_THAN_OR_EQUAL -> comparison <= 0
            ComparisonType.GREATER_THAN_OR_EQUAL -> comparison >= 0
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DataTypeCondition> = newCreator {
            val proto = DataProto.DataTypeCondition.parseFrom(it)
            DataTypeCondition(proto)
        }
    }
}
