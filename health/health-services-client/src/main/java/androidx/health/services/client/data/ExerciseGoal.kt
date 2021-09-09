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
import java.util.Objects

// TODO(yeabkal): as we support more types of goals, we may want to rename the class.
/** Defines a goal for an exercise. */
@Suppress("DataClassPrivateConstructor", "ParcelCreator")
public class ExerciseGoal
private constructor(
    public val exerciseGoalType: ExerciseGoalType,
    public val dataTypeCondition: DataTypeCondition,
    // TODO(yeabkal): shall we rename to "getMilestonePeriod"? Currently "getPeriod" is used to be
    // flexible in case we support other kinds of goals. Recheck when design is fully locked.
    public val period: Value? = null,
) : ProtoParcelable<DataProto.ExerciseGoal>() {

    internal constructor(
        proto: DataProto.ExerciseGoal
    ) : this(
        ExerciseGoalType.fromProto(proto.exerciseGoalType)
            ?: throw IllegalStateException("${proto.exerciseGoalType} not found"),
        DataTypeCondition(proto.dataTypeCondition),
        if (proto.hasPeriod()) Value(proto.period) else null
    )

    /** @hide */
    override val proto: DataProto.ExerciseGoal by lazy {
        val builder =
            DataProto.ExerciseGoal.newBuilder()
                .setExerciseGoalType(exerciseGoalType.toProto())
                .setDataTypeCondition(dataTypeCondition.proto)
        if (period != null) {
            builder.period = period.proto
        }
        builder.build()
    }

    // TODO(yeabkal): try to unify equality logic across goal types.
    // TODO(b/186899729): We need a better way to match on achieved goals.
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is ExerciseGoal) {
            return false
        }

        if (this.exerciseGoalType != other.exerciseGoalType) {
            return false
        }

        return dataTypeCondition == other.dataTypeCondition && period == other.period
    }

    override fun hashCode(): Int {
        return if (exerciseGoalType == ExerciseGoalType.ONE_TIME_GOAL) {
            Objects.hash(exerciseGoalType, dataTypeCondition)
        } else {
            Objects.hash(
                exerciseGoalType,
                dataTypeCondition.dataType,
                dataTypeCondition.comparisonType,
                period
            )
        }
    }

    override fun toString(): String =
        "ExerciseGoal(" +
            "exerciseGoalType=$exerciseGoalType, " +
            "dataTypeCondition=$dataTypeCondition, " +
            "period=$period)"

    /**
     * Checks if [other] is a possible representation of this goal. For one-time goals, this simply
     * checks for equality. For milestones, this returns `true` if and only if:
     * - [other] uses the same [ComparisonType], [DataType], and [period] as this goal, and
     * - the difference between [other]'s threshold and the threshold of this goal is a multiple of
     * of their common period.
     *
     * @hide
     */
    public fun isEquivalentTo(other: ExerciseGoal): Boolean {
        if (this.exerciseGoalType != other.exerciseGoalType) {
            return false
        }

        return when (exerciseGoalType) {
            ExerciseGoalType.ONE_TIME_GOAL -> equals(other)
            ExerciseGoalType.MILESTONE ->
                this.dataTypeCondition.dataType == other.dataTypeCondition.dataType &&
                    this.dataTypeCondition.comparisonType ==
                    other.dataTypeCondition.comparisonType &&
                    this.period == other.period &&
                    Value.isZero(
                        Value.modulo(
                            Value.difference(
                                dataTypeCondition.threshold,
                                other.dataTypeCondition.threshold
                            ),
                            period!!
                        )
                    )
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseGoal> = newCreator {
            val proto = DataProto.ExerciseGoal.parseFrom(it)
            ExerciseGoal(proto)
        }

        /**
         * Creates an [ExerciseGoal] that is achieved once when the given [DataTypeCondition] is
         * satisfied.
         */
        @JvmStatic
        public fun createOneTimeGoal(condition: DataTypeCondition): ExerciseGoal {
            return ExerciseGoal(ExerciseGoalType.ONE_TIME_GOAL, condition)
        }

        /**
         * Creates an [ExerciseGoal] that is achieved multiple times with its threshold being
         * updated by a `period` value each time it is achieved. For instance, a milestone could be
         * one for every 2km. This goal will there be triggered at distances = 2km, 4km, 6km, ...
         */
        @JvmStatic
        public fun createMilestone(condition: DataTypeCondition, period: Value): ExerciseGoal {
            require(period.format == condition.threshold.format) {
                "The condition's threshold and the period should have the same types of values."
            }
            return ExerciseGoal(ExerciseGoalType.MILESTONE, condition, period)
        }

        /** Creates a new goal that is the same as a given goal but with a new threshold value. */
        @JvmStatic
        public fun createMilestoneGoalWithUpdatedThreshold(
            goal: ExerciseGoal,
            newThreshold: Value
        ): ExerciseGoal {
            require(ExerciseGoalType.MILESTONE == goal.exerciseGoalType) {
                "The goal to update should be of MILESTONE type."
            }
            require(goal.period != null) { "The milestone goal's period should not be null." }
            val dataType = goal.dataTypeCondition.dataType
            val oldThreshold = goal.dataTypeCondition.threshold
            val comparisonType = goal.dataTypeCondition.comparisonType
            require(oldThreshold.format == newThreshold.format) {
                "The old and new thresholds should have the same types of values."
            }
            return ExerciseGoal(
                ExerciseGoalType.MILESTONE,
                DataTypeCondition(dataType, newThreshold, comparisonType),
                goal.period
            )
        }
    }
}
