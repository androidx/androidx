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

import android.os.Parcel
import android.os.Parcelable

/** A condition which is considered met when a data type value passes a defined threshold. */
public data class DataTypeCondition(
    val dataType: DataType,
    val threshold: Value,
    val comparisonType: ComparisonType,
) : Parcelable {
    init {
        require(dataType.format == threshold.format) {
            "provided data type must have sample time type."
        }
    }

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

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(dataType, flags)
        dest.writeParcelable(threshold, flags)
        dest.writeInt(comparisonType.id)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DataTypeCondition> =
            object : Parcelable.Creator<DataTypeCondition> {
                override fun createFromParcel(source: Parcel): DataTypeCondition? {
                    val dataType =
                        source.readParcelable<DataType>(DataType::class.java.classLoader)
                            ?: return null
                    val threshold =
                        source.readParcelable<Value>(Value::class.java.classLoader) ?: return null
                    val comparisonType = ComparisonType.fromId(source.readInt()) ?: return null
                    return DataTypeCondition(dataType, threshold, comparisonType)
                }

                override fun newArray(size: Int): Array<DataTypeCondition?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
