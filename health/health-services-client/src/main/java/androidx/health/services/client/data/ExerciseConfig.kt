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
import androidx.annotation.FloatRange
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.proto.DataProto

/**
 * Defines configuration for an exercise tracked using Health Services.
 *
 * @constructor Creates a new ExerciseConfig for an exercise tracked using Health Services
 *
 * @property exerciseType [ExerciseType] user is performing for this exercise
 * @property dataTypes [DataType] which will be tracked for this exercise
 * @property isAutoPauseAndResumeEnabled whether auto-pause/resume is enabled for this exercise
 * @property isGpsEnabled whether GPS is enabled for this exercise. Must be set to `true` when
 * [DataType.LOCATION] is present in [dataTypes].
 * @property exerciseGoals [ExerciseGoal]s for this exercise. [DataType]s in [ExerciseGoal]s must
 * also be tracked (i.e. contained in [dataTypes]) in some form. For example, an [ExerciseGoal] for
 * [DataType.STEPS_TOTAL] requires that [dataTypes] contains either or both of
 * [DataType.STEPS_TOTAL] / [DataType.STEPS].
 * @property exerciseParams [Bundle] bundle for specifying exercise presets, the values of an
 * on-going exercise which can be used to pre-populate a new exercise.
 * @property swimmingPoolLengthMeters length (in meters) of the swimming pool, or 0 if not relevant to
 * this exercise
 * @property exerciseTypeConfig [ExerciseTypeConfig] containing attributes which may be
 * modified after the exercise has started
 * @property batchingModeOverrides [BatchingMode] overrides for this exercise
 * @property exerciseEventTypes [ExerciseEventType]s which should be tracked for this exercise
 */
@Suppress("ParcelCreator")
class ExerciseConfig
@JvmOverloads
constructor(
    val exerciseType: ExerciseType,
    val dataTypes: Set<DataType<*, *>>,
    val isAutoPauseAndResumeEnabled: Boolean,
    val isGpsEnabled: Boolean,
    val exerciseGoals: List<ExerciseGoal<*>> = listOf(),
    val exerciseParams: Bundle = Bundle(),
    @FloatRange(from = 0.0) val swimmingPoolLengthMeters: Float = SWIMMING_POOL_LENGTH_UNSPECIFIED,
    val exerciseTypeConfig: ExerciseTypeConfig? = null,
    val batchingModeOverrides: Set<BatchingMode> = emptySet(),
    val exerciseEventTypes: Set<ExerciseEventType<*>> = emptySet(),
) {

    internal constructor(
        proto: DataProto.ExerciseConfig
    ) : this(
        ExerciseType.fromProto(proto.exerciseType),
        proto.dataTypesList.map { DataType.deltaFromProto(it) }.toMutableSet() +
            proto.aggregateDataTypesList.map { DataType.aggregateFromProto(it) },
        proto.isAutoPauseAndResumeEnabled,
        proto.isGpsUsageEnabled,
        proto.exerciseGoalsList.map { ExerciseGoal.fromProto(it) },
        BundlesUtil.fromProto(proto.exerciseParams),
        if (proto.hasSwimmingPoolLength()) {
            proto.swimmingPoolLength
        } else {
            SWIMMING_POOL_LENGTH_UNSPECIFIED
        },
        if (proto.hasExerciseTypeConfig()) {
            ExerciseTypeConfig.fromProto(proto.exerciseTypeConfig)
        } else null,
        proto.batchingModeOverridesList.map { BatchingMode(it) }.toSet(),
        proto.exerciseEventTypesList.map { ExerciseEventType.fromProto(it) }.toSet(),
    )

    init {
        require(!dataTypes.contains(DataType.LOCATION) || isGpsEnabled) {
            "If LOCATION data is being requested, setGpsEnabled(true) must be configured in the " +
                "ExerciseConfig. "
        }

        if (exerciseType == ExerciseType.SWIMMING_POOL) {
            require(swimmingPoolLengthMeters != 0.0f) {
                "If exercise type is SWIMMING_POOL, " +
                    "then swimming pool length must also be specified"
            }
        }
    }

    /** Builder for [ExerciseConfig] instances. */
    class Builder(
        /**
         * The active [ExerciseType] the user is performing for this exercise.
         *
         * Provide this parameter when tracking a workout to provide more accurate data. This
         * information can be used to tune sensors, e.g. the calories estimate can take the MET
         * value into account.
         */
        private val exerciseType: ExerciseType
    ) {
        private var dataTypes: Set<DataType<*, *>> = emptySet()
        private var isAutoPauseAndResumeEnabled: Boolean = false
        private var isGpsEnabled: Boolean = false
        private var exerciseGoals: List<ExerciseGoal<*>> = emptyList()
        private var exerciseParams: Bundle = Bundle.EMPTY
        private var swimmingPoolLength: Float = SWIMMING_POOL_LENGTH_UNSPECIFIED
        private var exerciseTypeConfig: ExerciseTypeConfig? = null
        private var batchingModeOverrides: Set<BatchingMode> = emptySet()
        private var exerciseEventTypes: Set<ExerciseEventType<*>> = emptySet()

        /**
         * Sets the requested [DataType]s that should be tracked during this exercise. If not
         * explicitly called, a default set of [DataType]s will be chosen based on the
         * [ExerciseType].
         *
         * @param dataTypes set of [DataType]s ([AggregateDataType] or [DeltaDataType]) to track
         * during this exercise
         */
        fun setDataTypes(dataTypes: Set<DataType<*, *>>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets whether auto pause and auto resume should be enabled for this exercise. If not set,
         * auto-pause is disabled by default.
         *
         * @param isAutoPauseAndResumeEnabled if true, exercise will automatically pause and resume
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setIsAutoPauseAndResumeEnabled(isAutoPauseAndResumeEnabled: Boolean): Builder {
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
        fun setIsGpsEnabled(isGpsEnabled: Boolean): Builder {
            this.isGpsEnabled = isGpsEnabled
            return this
        }

        /**
         * Sets [ExerciseGoal]s specified for this exercise.
         *
         * [DataType]s in [ExerciseGoal]s must also be tracked (i.e. provided to [setDataTypes]) in
         * some form. For example, an [ExerciseGoal] for [DataType.STEPS_TOTAL] requires that either
         * or both of [DataType.STEPS_TOTAL] / [DataType.STEPS] be passed into [setDataTypes].
         *
         * @param exerciseGoals the list of [ExerciseGoal]s to begin the exercise with
         */
        fun setExerciseGoals(exerciseGoals: List<ExerciseGoal<*>>): Builder {
            this.exerciseGoals = exerciseGoals
            return this
        }

        /**
         * Sets additional OEM specific parameters for the current exercise. Intended to be used by
         * OEMs or apps working closely with them.
         *
         * @param exerciseParams [Bundle] containing OEM specific parameters
         */
        fun setExerciseParams(exerciseParams: Bundle): Builder {
            this.exerciseParams = exerciseParams
            return this
        }

        /** Sets the swimming pool length (in m). */
        @Suppress("MissingGetterMatchingBuilder")
        fun setSwimmingPoolLengthMeters(swimmingPoolLength: Float): Builder {
            this.swimmingPoolLength = swimmingPoolLength
            return this
        }

        /**
         * Sets the [ExerciseTypeConfig] which are configurable attributes for the ongoing exercise.
         *
         * @param exerciseTypeConfig [ExerciseTypeConfig] specifying active exercise type
         * configurations
         */
        fun setExerciseTypeConfig(exerciseTypeConfig: ExerciseTypeConfig?): Builder {
            this.exerciseTypeConfig = exerciseTypeConfig
            return this
        }

        /**
         * Sets the [BatchingMode] overrides for the ongoing exercise.
         *
         * @param batchingModeOverrides [BatchingMode] overrides
         */
        fun setBatchingModeOverrides(batchingModeOverrides: Set<BatchingMode>): Builder {
            this.batchingModeOverrides = batchingModeOverrides
            return this
        }

        /**
         * Sets the [ExerciseEventType]s that should be tracked for this exercise.
         *
         * @param exerciseEventTypes the set of [ExerciseEventType]s to begin the exercise with
         */
        fun setExerciseEventTypes(exerciseEventTypes: Set<ExerciseEventType<*>>): Builder {
            this.exerciseEventTypes = exerciseEventTypes
            return this
        }

        /** Returns the built [ExerciseConfig]. */
        fun build(): ExerciseConfig {
            return ExerciseConfig(
                exerciseType,
                dataTypes,
                isAutoPauseAndResumeEnabled,
                isGpsEnabled,
                exerciseGoals,
                exerciseParams,
                swimmingPoolLength,
                exerciseTypeConfig,
                batchingModeOverrides,
                exerciseEventTypes,
            )
        }
    }

    override fun toString(): String =
        "ExerciseConfig(" +
            "exerciseType=$exerciseType, " +
            "dataTypes=$dataTypes, " +
            "isAutoPauseAndResumeEnabled=$isAutoPauseAndResumeEnabled, " +
            "isGpsEnabled=$isGpsEnabled, " +
            "exerciseGoals=$exerciseGoals, " +
            "swimmingPoolLengthMeters=$swimmingPoolLengthMeters, " +
            "exerciseTypeConfig=$exerciseTypeConfig)"

    internal fun toProto(): DataProto.ExerciseConfig {
        val builder = DataProto.ExerciseConfig.newBuilder()
            .setExerciseType(exerciseType.toProto())
            .addAllDataTypes(dataTypes.filter { !it.isAggregate }.map { it.proto })
            .addAllAggregateDataTypes(dataTypes.filter { it.isAggregate }.map { it.proto })
            .setIsAutoPauseAndResumeEnabled(isAutoPauseAndResumeEnabled)
            .setIsGpsUsageEnabled(isGpsEnabled)
            .addAllExerciseGoals(exerciseGoals.map { it.proto })
            .setExerciseParams(BundlesUtil.toProto(exerciseParams))
            .setSwimmingPoolLength(swimmingPoolLengthMeters)
            .addAllBatchingModeOverrides(batchingModeOverrides.map { it.toProto() })
            .addAllExerciseEventTypes(exerciseEventTypes.map { it.toProto() })
        if (exerciseTypeConfig != null) {
            builder.exerciseTypeConfig = exerciseTypeConfig.toProto()
        }
        return builder.build()
    }

    companion object {
        /**
         * Returns a fresh new [Builder].
         *
         * @param exerciseType the [ExerciseType] representing this exercise
         */
        @JvmStatic
        fun builder(exerciseType: ExerciseType): Builder = Builder(exerciseType)

        public const val SWIMMING_POOL_LENGTH_UNSPECIFIED = 0.0f
    }
}
