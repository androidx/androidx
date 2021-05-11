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

/** Provides exercise specific capabilities data. */
public data class ExerciseTypeCapabilities(
    val supportedDataTypes: Set<DataType>,
    val supportedGoals: Map<DataType, Set<ComparisonType>>,
    val supportedMilestones: Map<DataType, Set<ComparisonType>>,
    val supportsAutoPauseAndResume: Boolean,
    val supportsLaps: Boolean,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(supportedDataTypes.size)
        dest.writeTypedArray(supportedDataTypes.toTypedArray(), flags)

        writeSupportedDataTypes(supportedGoals, dest, flags)
        writeSupportedDataTypes(supportedMilestones, dest, flags)

        dest.writeInt(if (supportsAutoPauseAndResume) 1 else 0)
        dest.writeInt(if (supportsLaps) 1 else 0)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseTypeCapabilities> =
            object : Parcelable.Creator<ExerciseTypeCapabilities> {
                override fun createFromParcel(source: Parcel): ExerciseTypeCapabilities? {
                    val supportedDataTypesArray = Array<DataType?>(source.readInt()) { null }
                    source.readTypedArray(supportedDataTypesArray, DataType.CREATOR)

                    val supportedGoals = readSupportedDataTypes(source) ?: return null
                    val supportedMilestones = readSupportedDataTypes(source) ?: return null
                    val supportsAutoPauseAndResume = source.readInt() == 1
                    val supportsLaps = source.readInt() == 1

                    return ExerciseTypeCapabilities(
                        supportedDataTypesArray.filterNotNull().toSet(),
                        supportedGoals,
                        supportedMilestones,
                        supportsAutoPauseAndResume,
                        supportsLaps
                    )
                }

                override fun newArray(size: Int): Array<ExerciseTypeCapabilities?> {
                    return arrayOfNulls(size)
                }
            }

        private fun writeSupportedDataTypes(
            supportedDataTypes: Map<DataType, Set<ComparisonType>>,
            dest: Parcel,
            flags: Int
        ) {
            dest.writeInt(supportedDataTypes.size)
            for ((dataType, comparisonTypeSet) in supportedDataTypes) {
                dest.writeParcelable(dataType, flags)
                dest.writeInt(comparisonTypeSet.size)
                dest.writeIntArray(comparisonTypeSet.map { it.id }.toIntArray())
            }
        }

        private fun readSupportedDataTypes(source: Parcel): Map<DataType, Set<ComparisonType>>? {
            val supportedDataTypes = HashMap<DataType, Set<ComparisonType>>()

            val numSupportedDataTypes = source.readInt()
            repeat(numSupportedDataTypes) {
                val dataType: DataType =
                    source.readParcelable(DataType::class.java.classLoader) ?: return null

                val comparisonTypeIntArray = IntArray(source.readInt())
                source.readIntArray(comparisonTypeIntArray)
                val comparisonTypeSet =
                    comparisonTypeIntArray.map { ComparisonType.fromId(it) }.filterNotNull().toSet()

                supportedDataTypes[dataType] = comparisonTypeSet
            }

            return supportedDataTypes
        }
    }
}
