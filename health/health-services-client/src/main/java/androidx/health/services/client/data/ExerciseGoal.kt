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

/** Defines a goal for an exercise. */
class ExerciseGoal<T : Number>
internal constructor(
    /**
     * The type of this exercise goal ([ExerciseGoalType.ONE_TIME_GOAL] or
     * [ExerciseGoalType.MILESTONE].)
     */
    val exerciseGoalType: ExerciseGoalType,
    val dataTypeCondition: DataTypeCondition<T, AggregateDataType<T, *>>,
    val period: T? = null,
) : ProtoParcelable<DataProto.ExerciseGoal>() {

    /** @hide */
    override val proto: DataProto.ExerciseGoal = getDataProtoExerciseGoalProto()

    private fun getDataProtoExerciseGoalProto(): DataProto.ExerciseGoal {
        val builder =
            DataProto.ExerciseGoal.newBuilder().setExerciseGoalType(exerciseGoalType.toProto())
                .setDataTypeCondition(dataTypeCondition.proto)
        if (period != null) {
            builder.period = dataTypeCondition.dataType.toProtoFromValue(period)
        }
        return builder.build()
    }

    // TODO(yeabkal): try to unify equality logic across goal types.
    // TODO(b/186899729): We need a better way to match on achieved goals.
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is ExerciseGoal<*>) {
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

    override fun toString(): String = "ExerciseGoal(" +
        "exerciseGoalType=$exerciseGoalType, " +
        "dataTypeCondition=$dataTypeCondition, " +
        "period=$period" +
        ")"

    companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseGoal<*>> = newCreator {
            val proto = DataProto.ExerciseGoal.parseFrom(it)
            fromProto(proto)
        }

        @Suppress("UNCHECKED_CAST")
        internal fun fromProto(proto: DataProto.ExerciseGoal): ExerciseGoal<Number> {
            val condition = DataTypeCondition.aggregateFromProto(proto.dataTypeCondition)
                as DataTypeCondition<Number, AggregateDataType<Number, *>>
            return ExerciseGoal(
                ExerciseGoalType.fromProto(proto.exerciseGoalType)
                    ?: throw IllegalStateException("${proto.exerciseGoalType} not found"),
                condition,
                if (proto.hasPeriod()) condition.dataType.toValueFromProto(proto.period) else null
            )
        }

        /**
         * Creates an [ExerciseGoal] that is achieved once when the given [DataTypeCondition] is
         * satisfied.
         */
        @JvmStatic
        fun <T : Number> createOneTimeGoal(
            condition: DataTypeCondition<T, AggregateDataType<T, *>>
        ): ExerciseGoal<T> {
            return ExerciseGoal(ExerciseGoalType.ONE_TIME_GOAL, condition)
        }

        /**
         * Creates an [ExerciseGoal] that is achieved multiple times with its threshold being
         * updated by a `period` value each time it is achieved. For instance, a milestone could be
         * one for every 2km. This goal will there be triggered at distances = 2km, 4km, 6km, ...
         */
        @JvmStatic
        fun <T : Number> createMilestone(
            condition: DataTypeCondition<T, AggregateDataType<T, *>>,
            period: T
        ): ExerciseGoal<T> = ExerciseGoal(ExerciseGoalType.MILESTONE, condition, period)

        /** Creates a new goal that is the same as a given goal but with a new threshold value. */
        @JvmStatic
        fun <T : Number> createMilestoneGoalWithUpdatedThreshold(
            goal: ExerciseGoal<T>,
            newThreshold: T
        ): ExerciseGoal<T> {
            require(ExerciseGoalType.MILESTONE == goal.exerciseGoalType) {
                "The goal to update should be of MILESTONE type."
            }
            require(goal.period != null) { "The milestone goal's period should not be null." }
            val dataType = goal.dataTypeCondition.dataType
            val comparisonType = goal.dataTypeCondition.comparisonType
            return ExerciseGoal(
                ExerciseGoalType.MILESTONE,
                DataTypeCondition(dataType, newThreshold, comparisonType),
                goal.period
            )
        }
    }
}
