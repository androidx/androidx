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
import androidx.annotation.RestrictTo
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.proto.DataProto

/**
 * Defines configuration for an exercise tracked using Health Services.
 *
 * @constructor Creates a new ExerciseConfig for an exercise tracked using Health Services
 *
 * @property exerciseType [ExerciseType] user is performing for this exercise
 * @property dataTypes [DataType] which will be tracked for this exercise
 * @property aggregateDataTypes [DataType]s which should be tracked as aggregates for this exercise
 * @property isAutoPauseAndResumeEnabled whether auto-pause/ resume is enabled for this exercise
 * @property isGpsEnabled whether GPS is enabled for this exercise
 * @property exerciseGoals [ExerciseGoal]s for this exercise
 * @property exerciseParams [Bundle] additional OEM specific params for this exercise
 */
@Suppress("ParcelCreator")
public class ExerciseConfig
public constructor(
    public val exerciseType: ExerciseType,
    public val dataTypes: Set<DataType>,
    public val aggregateDataTypes: Set<DataType>,
    public val isAutoPauseAndResumeEnabled: Boolean,
    public val isGpsEnabled: Boolean,
    public val exerciseGoals: List<ExerciseGoal>,
    public val exerciseParams: Bundle,
) : ProtoParcelable<DataProto.ExerciseConfig>() {

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
        require(!dataTypes.contains(DataType.LOCATION) || isGpsEnabled) {
            "If LOCATION data is being requested, setGpsEnabled(true) must be configured in the " +
                "ExerciseConfig. "
        }
    }

    /** Builder for [ExerciseConfig] instances. */
    public class Builder {
        private var exerciseType: ExerciseType? = null
        private var dataTypes: Set<DataType> = emptySet()
        private var aggregateDataTypes: Set<DataType> = emptySet()
        private var isAutoPauseAndResumeEnabled: Boolean = false
        private var isGpsEnabled: Boolean = false
        private var exerciseGoals: List<ExerciseGoal> = emptyList()
        private var exerciseParams: Bundle = Bundle.EMPTY

        /**
         * Sets the active [ExerciseType] the user is performing for this exercise.
         *
         * Provide this parameter when tracking a workout to provide more accurate data. This
         * information can be used to tune sensors, e.g. the calories estimate can take the MET
         * value into account.
         *
         * @param exerciseType the [ExerciseType] representing this exercise
         */
        public fun setExerciseType(exerciseType: ExerciseType): Builder {
            require(exerciseType != ExerciseType.UNKNOWN) { "Must specify a valid exercise type." }
            this.exerciseType = exerciseType
            return this
        }

        /**
         * Sets the requested [DataType]s that should be tracked during this exercise. If not
         * explicitly called, a default set of [DataType] will be chosen based on the
         * [ExerciseType].
         *
         * @param dataTypes set of [DataType]s to track during this exercise
         */
        public fun setDataTypes(dataTypes: Set<DataType>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets the requested [DataType]s that should be tracked as aggregates (i.e. total steps or
         * average heart rate) during this exercise. If not explicitly called, a default set of
         * [DataType] will be chosen based on the [ExerciseType].
         *
         * @param dataTypes set of aggregate [DataType]s to track during this exercise
         */
        public fun setAggregateDataTypes(dataTypes: Set<DataType>): Builder {
            this.aggregateDataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets whether auto pause and auto resume should be enabled for this exercise. If not set,
         * auto-pause is disabled by default.
         *
         * @param isAutoPauseAndResumeEnabled if true, exercise will automatically pause and resume
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setIsAutoPauseAndResumeEnabled(isAutoPauseAndResumeEnabled: Boolean): Builder {
            this.isAutoPauseAndResumeEnabled = isAutoPauseAndResumeEnabled
            return this
        }

        /**
         * Sets whether GPS will be used for this exercise. If not set, it's disabled by default.
         *
         * If [DataType.LOCATION] is among the data types requested for the exercise, GPS usage
         * MUST be enabled. Enabling GPS will improve data generation for types like distance and
         * speed.
         *
         * If no data type is specified in the configuration, WHS provides all data types
         * supported for the exercise. In this case, if [DataType.LOCATION] is among the supported
         * data types for the exercise but GPS usage is disabled (i.e. [isGpsEnabled] is `false`,
         * then [ExerciseClient.startExerciseAsync] will fail.
         *
         * @param isGpsEnabled if true, GPS will be enabled for this exercise
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setIsGpsEnabled(isGpsEnabled: Boolean): Builder {
            this.isGpsEnabled = isGpsEnabled
            return this
        }

        /**
         * Sets [ExerciseGoal]s specified for this exercise.
         *
         * This is useful to have goals specified before the start of an exercise.
         *
         * @param exerciseGoals the list of [ExerciseGoal]s to begin the exercise with
         */
        public fun setExerciseGoals(exerciseGoals: List<ExerciseGoal>): Builder {
            this.exerciseGoals = exerciseGoals.toList()
            return this
        }

        /**
         * Sets additional OEM specific parameters for the current exercise. Intended to be used by
         * OEMs or apps working closely with them.
         *
         * @param exerciseParams [Bundle] containing OEM specific parameters
         */
        public fun setExerciseParams(exerciseParams: Bundle): Builder {
            this.exerciseParams = exerciseParams
            return this
        }

        /** Returns the built [ExerciseConfig]. */
        public fun build(): ExerciseConfig {
            return ExerciseConfig(
                checkNotNull(exerciseType) { "No exercise type specified" },
                dataTypes,
                aggregateDataTypes,
                isAutoPauseAndResumeEnabled,
                isGpsEnabled,
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
            .setIsAutoPauseAndResumeEnabled(isAutoPauseAndResumeEnabled)
            .setIsGpsUsageEnabled(isGpsEnabled)
            .addAllExerciseGoals(exerciseGoals.map { it.proto })
            .setExerciseParams(BundlesUtil.toProto(exerciseParams))
            .build()
    }

    override fun toString(): String =
        "ExerciseConfig(" +
            "exerciseType=$exerciseType, " +
            "dataTypes=$dataTypes, " +
            "aggregateDataTypes=$aggregateDataTypes, " +
            "isAutoPauseAndResumeEnabled=$isAutoPauseAndResumeEnabled, " +
            "isGpsEnabled=$isGpsEnabled, " +
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
