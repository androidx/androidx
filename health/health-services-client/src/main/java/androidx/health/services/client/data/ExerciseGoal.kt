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
import java.util.Objects

// TODO(yeabkal): as we support more types of goals, we may want to rename the class.
/** Defines a goal for an exercise. */
@Suppress("DataClassPrivateConstructor")
public data class ExerciseGoal
protected constructor(
    val exerciseGoalType: ExerciseGoalType,
    val dataTypeCondition: DataTypeCondition,
    // TODO(yeabkal): shall we rename to "getMilestonePeriod"? Currently "getPeriod" is used to be
    // flexible in case we support other kinds of goals. Recheck when design is fully locked.
    val period: Value? = null,
) : Parcelable {

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(exerciseGoalType.id)
        dest.writeParcelable(dataTypeCondition, flags)
        dest.writeParcelable(period, flags)
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

        return when (exerciseGoalType) {
            ExerciseGoalType.ONE_TIME_GOAL -> dataTypeCondition == other.dataTypeCondition
            // The threshold of a milestone is not included in the equality calculation to let apps
            // easily map back an achieved milestone to the milestone they requested for tracking.
            ExerciseGoalType.MILESTONE ->
                dataTypeCondition.dataType == other.dataTypeCondition.dataType &&
                    dataTypeCondition.comparisonType == other.dataTypeCondition.comparisonType &&
                    period == other.period
        }
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

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseGoal> =
            object : Parcelable.Creator<ExerciseGoal> {
                override fun createFromParcel(source: Parcel): ExerciseGoal? {
                    val exerciseGoalType = ExerciseGoalType.fromId(source.readInt()) ?: return null
                    val dataTypeCondition: DataTypeCondition =
                        source.readParcelable(DataTypeCondition::class.java.classLoader)
                            ?: return null
                    val period: Value? = source.readParcelable(Value::class.java.classLoader)

                    return ExerciseGoal(exerciseGoalType, dataTypeCondition, period)
                }

                override fun newArray(size: Int): Array<ExerciseGoal?> {
                    return arrayOfNulls(size)
                }
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
            val (dataType, oldThreshold, comparisonType) = goal.dataTypeCondition
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
