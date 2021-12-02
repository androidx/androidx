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

/** Defines configuration for an exercise tracked using HealthServices. */
@Suppress("DataClassPrivateConstructor", "ParcelCreator")
public class WarmUpConfig
protected constructor(
    /**
     * [ExerciseType] the user is performing for this exercise.
     *
     * This information can be used to tune sensors, e.g. the calories estimate can take the MET
     * value into account.
     */
    public val exerciseType: ExerciseType,
    public val dataTypes: Set<DataType>,
) : ProtoParcelable<DataProto.WarmUpConfig>() {

    internal constructor(
        proto: DataProto.WarmUpConfig
    ) : this(
        ExerciseType.fromProto(proto.exerciseType),
        proto.dataTypesList.map { DataType(it) }.toSet(),
    )

    init {
        require(dataTypes.isNotEmpty()) { "Must specify the desired data types." }
    }

    /** Builder for [WarmUpConfig] instances. */
    public class Builder {
        private var exerciseType: ExerciseType? = null
        private var dataTypes: Set<DataType>? = null

        /**
         * Sets the active [ExerciseType] the user is performing for this exercise.
         *
         * Provide this parameter when tracking a workout to provide more accurate data. This
         * information can be used to tune sensors, e.g. the calories estimate can take the MET
         * value into account.
         */
        public fun setExerciseType(exerciseType: ExerciseType): Builder {
            require(exerciseType != ExerciseType.UNKNOWN) { "Must specify a valid exercise type." }
            this.exerciseType = exerciseType
            return this
        }

        /**
         * Sets the requested [DataType]s that should be tracked during this exercise. If not
         * explicitly called, a default set of [DataType] will be chosen based on the [ExerciseType]
         * .
         */
        public fun setDataTypes(dataTypes: Set<DataType>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /** Returns the built `WarmUpConfig`. */
        public fun build(): WarmUpConfig {
            return WarmUpConfig(
                checkNotNull(exerciseType) { "No exercise type specified" },
                checkNotNull(dataTypes) { "No data types specified" },
            )
        }
    }

    /** @hide */
    override val proto: DataProto.WarmUpConfig by lazy {
        DataProto.WarmUpConfig.newBuilder()
            .setExerciseType(exerciseType.toProto())
            .addAllDataTypes(dataTypes.map { it.proto })
            .build()
    }

    override fun toString(): String =
        "WarmUpConfig(exerciseType=$exerciseType, dataTypes=$dataTypes)"

    public companion object {
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val CREATOR: Parcelable.Creator<WarmUpConfig> = newCreator { bytes ->
            val proto = DataProto.WarmUpConfig.parseFrom(bytes)
            WarmUpConfig(proto)
        }
    }
}
