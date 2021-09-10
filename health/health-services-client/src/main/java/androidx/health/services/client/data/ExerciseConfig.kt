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

import android.os.Bundle
import android.os.Parcelable
import androidx.health.services.client.proto.DataProto

/** Defines configuration for an exercise tracked using HealthServices. */
@Suppress("DataClassPrivateConstructor", "ParcelCreator")
public class ExerciseConfig
protected constructor(
    /**
     * [ExerciseType] the user is performing for this exercise.
     *
     * This information can be used to tune sensors, e.g. the calories estimate can take the MET
     * value into account.
     */
    public val exerciseType: ExerciseType,
    public val dataTypes: Set<DataType>,
    public val aggregateDataTypes: Set<DataType>,
    @get:JvmName("shouldEnableAutoPauseAndResume")
    public val shouldEnableAutoPauseAndResume: Boolean,
    @get:JvmName("shouldEnableGps") public val shouldEnableGps: Boolean,
    public val exerciseGoals: List<ExerciseGoal>,
    public val exerciseParams: Bundle,
) : ProtoParcelable<DataProto.ExerciseConfig>() {

    /** @hide */
    public constructor(
        proto: DataProto.ExerciseConfig
    ) : this(
        ExerciseType.fromProto(proto.exerciseType),
        proto.dataTypesList.map { DataType(it) }.toSet(),
        proto.aggregateDataTypesList.map { DataType(it) }.toSet(),
        proto.isAutoPauseAndResumeEnabled,
        proto.isGpsUsageEnabled,
        proto.exerciseGoalsList.map { ExerciseGoal(it) },
        BundlesUtil.fromProto(proto.exerciseParams)
    )

    init {
        require(!dataTypes.contains(DataType.LOCATION) || shouldEnableGps) {
            "If LOCATION data is being requested, setShouldEnableGps(true) must be configured in " +
                "the ExerciseConfig. "
        }
    }

    /** Builder for [ExerciseConfig] instances. */
    public class Builder {
        private var exerciseType: ExerciseType? = null
        private var dataTypes: Set<DataType> = emptySet()
        private var aggregateDataTypes: Set<DataType> = emptySet()
        private var shouldEnableAutoPauseAndResume: Boolean = false
        private var shouldEnableGps: Boolean = false
        private var exerciseGoals: List<ExerciseGoal> = emptyList()
        private var exerciseParams: Bundle = Bundle.EMPTY

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
         * Sets the requested [DataType] s that should be tracked during this exercise. If not
         * explicitly called, a default set of [DataType] will be chosen based on the [ExerciseType]
         * .
         */
        public fun setDataTypes(dataTypes: Set<DataType>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets the requested [DataType]s that should be tracked as aggregates (i.e. total steps or
         * average heart rate) during this exercise. If not explicitly called, a default set of
         * [DataType] will be chosen based on the [ExerciseType].
         */
        public fun setAggregateDataTypes(dataTypes: Set<DataType>): Builder {
            this.aggregateDataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets whether auto pause and auto resume should be enabled for this exercise. If not set,
         * auto-pause is disabled by default.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setShouldEnableAutoPauseAndResume(
            shouldEnableAutoPauseAndResume: Boolean
        ): Builder {
            this.shouldEnableAutoPauseAndResume = shouldEnableAutoPauseAndResume
            return this
        }

        /**
         * Sets whether GPS will be used for this exercise. If not set, it's disabled by default.
         *
         * <p>If {@link DataType#LOCATION} is among the data types requested for the exercise, GPS
         * usage MUST be enabled. Enabling GPS will improve data generation for types like distance
         * and speed.
         *
         * <p>If no data type is specified in the configuration, WHS provides all data types
         * supported for the exercise. In this case, if {@link DataType#LOCATION} is among the
         * supported data types for the exercise but GPS usage is disabled (i.e. {@code
         * shouldEnableGps} is {@code false}, then [ExerciseClient.startExercise] will fail.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setShouldEnableGps(shouldEnableGps: Boolean): Builder {
            this.shouldEnableGps = shouldEnableGps
            return this
        }

        /**
         * Sets [ExerciseGoal] s specified for this exercise.
         *
         * This is useful to have goals specified before the start of an exercise.
         */
        public fun setExerciseGoals(exerciseGoals: List<ExerciseGoal>): Builder {
            this.exerciseGoals = exerciseGoals.toList()
            return this
        }

        /**
         * Sets additional OEM specific parameters for the current exercise. Intended to be used by
         * OEMs or apps working closely with them.
         */
        public fun setExerciseParams(exerciseParams: Bundle): Builder {
            this.exerciseParams = exerciseParams
            return this
        }

        /** Returns the built `ExerciseConfig`. */
        public fun build(): ExerciseConfig {
            return ExerciseConfig(
                checkNotNull(exerciseType) { "No exercise type specified" },
                dataTypes,
                aggregateDataTypes,
                shouldEnableAutoPauseAndResume,
                shouldEnableGps,
                exerciseGoals,
                exerciseParams
            )
        }
    }

    /** @hide */
    override val proto: DataProto.ExerciseConfig by lazy {
        DataProto.ExerciseConfig.newBuilder()
            .setExerciseType(exerciseType.toProto())
            .addAllDataTypes(dataTypes.map { it.proto })
            .addAllAggregateDataTypes(aggregateDataTypes.map { it.proto })
            .setIsAutoPauseAndResumeEnabled(shouldEnableAutoPauseAndResume)
            .setIsGpsUsageEnabled(shouldEnableGps)
            .addAllExerciseGoals(exerciseGoals.map { it.proto })
            .setExerciseParams(BundlesUtil.toProto(exerciseParams))
            .build()
    }

    override fun toString(): String =
        "ExerciseConfig(" +
            "exerciseType=$exerciseType, " +
            "dataTypes=$dataTypes, " +
            "aggregateDataTypes=$aggregateDataTypes, " +
            "shouldEnableAutoPauseAndResume=$shouldEnableAutoPauseAndResume, " +
            "shouldEnableGps=$shouldEnableGps, " +
            "exerciseGoals=$exerciseGoals)"

    public companion object {
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseConfig> = newCreator { bytes ->
            val proto = DataProto.ExerciseConfig.parseFrom(bytes)
            ExerciseConfig(proto)
        }
    }
}
