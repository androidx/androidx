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

/** A place holder class that represents the capabilities of WHS client on the device. */
public data class Capabilities(
    /** Mapping for each supported [ExerciseType] to its [ExerciseCapabilities] on this device. */
    val exerciseTypeToExerciseCapabilities: Map<ExerciseType, ExerciseCapabilities>,
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        writeExerciseTypeToExerciseCapabilities(dest, flags)
    }

    /** Set of supported [ExerciseType] s on this device. */
    public val supportedExerciseTypes: Set<ExerciseType>
        get() = exerciseTypeToExerciseCapabilities.keys

    /**
     * Returns the supported [ExerciseCapabilities] for a requested [ExerciseType].
     *
     * @throws IllegalArgumentException if the supplied [exercise] isn't supported
     */
    public fun getExerciseCapabilities(exercise: ExerciseType): ExerciseCapabilities {
        return exerciseTypeToExerciseCapabilities[exercise]
            ?: throw IllegalArgumentException(
                String.format("%s exercise type is not supported", exercise)
            )
    }

    /** Returns the set of [ExerciseType] s that support auto pause and resume on this device. */
    public val autoPauseAndResumeEnabledExercises: Set<ExerciseType>
        get() {
            return exerciseTypeToExerciseCapabilities
                .entries
                .filter { it.value.supportsAutoPauseAndResume }
                .map { it.key }
                .toSet()
        }

    private fun writeExerciseTypeToExerciseCapabilities(dest: Parcel, flags: Int) {
        dest.writeInt(exerciseTypeToExerciseCapabilities.size)
        for ((key1, value) in exerciseTypeToExerciseCapabilities) {
            val key = key1.id
            dest.writeInt(key)
            dest.writeParcelable(value, flags)
        }
    }

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<Capabilities> =
            object : Parcelable.Creator<Capabilities> {
                override fun createFromParcel(parcel: Parcel): Capabilities {
                    val exerciseTypeToExerciseCapabilitiesFromParcel =
                        getExerciseToExerciseCapabilityMap(parcel)
                    return Capabilities(
                        exerciseTypeToExerciseCapabilitiesFromParcel,
                    )
                }

                override fun newArray(size: Int): Array<Capabilities?> = arrayOfNulls(size)
            }

        private fun readDataTypeSet(parcel: Parcel): Set<DataType> {
            return parcel.createTypedArray(DataType.CREATOR)!!.toSet()
        }

        private fun writeDataTypeSet(out: Parcel, flags: Int, dataTypes: Set<DataType>) {
            out.writeTypedArray(dataTypes.toTypedArray(), flags)
        }

        private fun getExerciseToExerciseCapabilityMap(
            parcel: Parcel
        ): Map<ExerciseType, ExerciseCapabilities> {
            val map = HashMap<ExerciseType, ExerciseCapabilities>()
            val mapSize = parcel.readInt()
            for (i in 0 until mapSize) {
                val key = fromId(parcel.readInt())
                val value =
                    parcel.readParcelable<Parcelable>(
                        ExerciseCapabilities::class.java.classLoader
                    ) as
                        ExerciseCapabilities
                map[key] = value
            }
            return map
        }
    }
}
