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
import androidx.health.services.client.data.ExerciseType.Companion.fromId

/**
 * A place holder class that represents the capabilities of the
 * [androidx.health.services.client.ExerciseClient] on the device.
 */
public data class ExerciseCapabilities(
    /**
     * Mapping for each supported [ExerciseType] to its [ExerciseTypeCapabilities] on this device.
     */
    val typeToCapabilities: Map<ExerciseType, ExerciseTypeCapabilities>,
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        writeTypeToCapabilities(dest, flags)
    }

    /** Set of supported [ExerciseType] s on this device. */
    public val supportedExerciseTypes: Set<ExerciseType>
        get() = typeToCapabilities.keys

    /**
     * Returns the supported [ExerciseTypeCapabilities] for a requested [ExerciseType].
     *
     * @throws IllegalArgumentException if the [exercise] is not supported
     */
    public fun getExerciseTypeCapabilities(exercise: ExerciseType): ExerciseTypeCapabilities {
        return typeToCapabilities[exercise]
            ?: throw IllegalArgumentException(
                String.format("%s exercise type is not supported", exercise)
            )
    }

    /** Returns the set of [ExerciseType] s that support auto pause and resume on this device. */
    public val autoPauseAndResumeEnabledExercises: Set<ExerciseType>
        get() {
            return typeToCapabilities
                .entries
                .filter { it.value.supportsAutoPauseAndResume }
                .map { it.key }
                .toSet()
        }

    private fun writeTypeToCapabilities(dest: Parcel, flags: Int) {
        dest.writeInt(typeToCapabilities.size)
        for ((key1, value) in typeToCapabilities) {
            val key = key1.id
            dest.writeInt(key)
            dest.writeParcelable(value, flags)
        }
    }

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseCapabilities> =
            object : Parcelable.Creator<ExerciseCapabilities> {
                override fun createFromParcel(parcel: Parcel): ExerciseCapabilities {
                    val typeToCapabilitiesFromParcel = getTypeToCapabilityMap(parcel)
                    return ExerciseCapabilities(
                        typeToCapabilitiesFromParcel,
                    )
                }

                override fun newArray(size: Int): Array<ExerciseCapabilities?> = arrayOfNulls(size)
            }

        private fun readDataTypeSet(parcel: Parcel): Set<DataType> {
            return parcel.createTypedArray(DataType.CREATOR)!!.toSet()
        }

        private fun writeDataTypeSet(out: Parcel, flags: Int, dataTypes: Set<DataType>) {
            out.writeTypedArray(dataTypes.toTypedArray(), flags)
        }

        private fun getTypeToCapabilityMap(
            parcel: Parcel
        ): Map<ExerciseType, ExerciseTypeCapabilities> {
            val map = HashMap<ExerciseType, ExerciseTypeCapabilities>()
            val mapSize = parcel.readInt()
            for (i in 0 until mapSize) {
                val key = fromId(parcel.readInt())
                val value =
                    parcel.readParcelable<Parcelable>(
                        ExerciseTypeCapabilities::class.java.classLoader
                    ) as
                        ExerciseTypeCapabilities
                map[key] = value
            }
            return map
        }
    }
}
