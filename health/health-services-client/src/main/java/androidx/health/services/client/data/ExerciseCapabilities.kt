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
import androidx.health.services.client.proto.DataProto.ExerciseCapabilities.TypeToCapabilitiesEntry

/**
 * A place holder class that represents the capabilities of the
 * [androidx.health.services.client.ExerciseClient] on the device.
 */
@Suppress("ParcelCreator")
public class ExerciseCapabilities(
    /**
     * Mapping for each supported [ExerciseType] to its [ExerciseTypeCapabilities] on this device.
     */
    public val typeToCapabilities: Map<ExerciseType, ExerciseTypeCapabilities>,
) : ProtoParcelable<DataProto.ExerciseCapabilities>() {

    internal constructor(
        proto: DataProto.ExerciseCapabilities
    ) : this(
        proto
            .typeToCapabilitiesList
            .map { entry ->
                ExerciseType.fromProto(entry.type) to ExerciseTypeCapabilities(entry.capabilities)
            }
            .toMap()
    )

    /** @hide */
    override val proto: DataProto.ExerciseCapabilities by lazy {
        DataProto.ExerciseCapabilities.newBuilder()
            .addAllTypeToCapabilities(
                typeToCapabilities
                    .map {
                        TypeToCapabilitiesEntry.newBuilder()
                            .setType(it.key.toProto())
                            .setCapabilities(it.value.proto)
                            .build()
                    }
                    .sortedBy { it.type.name } // Ensures equals() works correctly
            )
            .build()
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

    override fun toString(): String = "ExerciseCapabilities(typeToCapabilities=$typeToCapabilities)"

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseCapabilities> = newCreator { bytes ->
            val proto = DataProto.ExerciseCapabilities.parseFrom(bytes)
            ExerciseCapabilities(proto)
        }
    }
}
