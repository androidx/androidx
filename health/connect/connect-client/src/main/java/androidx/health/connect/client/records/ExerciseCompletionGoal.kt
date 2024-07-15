/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.health.connect.client.records

import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import java.time.Duration

/** A goal which should be met to complete a [PlannedExerciseStep]. */
abstract class ExerciseCompletionGoal internal constructor() {
    /** An [ExerciseCompletionGoal] that requires covering a specified distance. */
    class DistanceGoal(
        val distance: Length,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DistanceGoal) return false

            return distance == other.distance
        }

        override fun hashCode(): Int {
            return distance.hashCode()
        }

        override fun toString(): String {
            return "DistanceGoal(distance=$distance)"
        }
    }

    /**
     * An [ExerciseCompletionGoal] that requires covering a specified distance. Additionally, the
     * step is not complete until the specified time has elapsed. Time remaining after the specified
     * distance has been completed should be spent resting. In the context of swimming, this is
     * sometimes referred to as 'interval training'.
     *
     * <p>For example, a swimming coach may specify '100m @ 1min40s'. This implies: complete 100m
     * and if you manage it in 1min30s, you will have 10s of rest prior to the next set.
     */
    class DistanceAndDurationGoal(
        val distance: Length,
        val duration: Duration,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DistanceAndDurationGoal) return false

            return distance == other.distance && duration == other.duration
        }

        override fun toString(): String {
            return "DistanceAndDurationGoal(distance=$distance, duration=$duration)"
        }

        override fun hashCode(): Int {
            var result = distance.hashCode()
            result = 31 * result + duration.hashCode()
            return result
        }
    }

    /** An [ExerciseCompletionGoal] that requires completing a specified number of steps. */
    class StepsGoal(
        val steps: Int,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StepsGoal) return false

            return steps == other.steps
        }

        override fun hashCode(): Int {
            return steps
        }

        override fun toString(): String {
            return "StepsGoal(steps=$steps)"
        }
    }

    /** An [ExerciseCompletionGoal] that requires a specified duration to elapse. */
    class DurationGoal(
        val duration: Duration,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DurationGoal) return false

            return duration == other.duration
        }

        override fun hashCode(): Int {
            return duration.hashCode()
        }

        override fun toString(): String {
            return "DurationGoal(duration=$duration)"
        }
    }

    /**
     * An [ExerciseCompletionGoal] that requires a specified number of repetitions to be completed.
     */
    class RepetitionsGoal(
        val repetitions: Duration,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RepetitionsGoal) return false

            return repetitions == other.repetitions
        }

        override fun hashCode(): Int {
            return repetitions.hashCode()
        }

        override fun toString(): String {
            return "RepetitionsGoal(repetitions=$repetitions)"
        }
    }

    /**
     * An [ExerciseCompletionGoal] that requires a specified number of total calories to be burned.
     */
    class TotalCaloriesBurnedGoal(
        val totalCalories: Energy,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TotalCaloriesBurnedGoal) return false

            return totalCalories == other.totalCalories
        }

        override fun hashCode(): Int {
            return totalCalories.hashCode()
        }

        override fun toString(): String {
            return "TotalCaloriesBurnedGoal(totalCalories=$totalCalories)"
        }
    }

    /**
     * An [ExerciseCompletionGoal] that requires a specified number of active calories to be burned.
     */
    class ActiveCaloriesBurnedGoal(
        val activeCalories: Energy,
    ) : ExerciseCompletionGoal() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ActiveCaloriesBurnedGoal) return false

            return activeCalories == other.activeCalories
        }

        override fun hashCode(): Int {
            return activeCalories.hashCode()
        }

        override fun toString(): String {
            return "ActiveCaloriesBurnedGoal(activeCalories=$activeCalories)"
        }
    }

    /** An [ExerciseCompletionGoal] that is unknown. */
    object UnknownGoal : ExerciseCompletionGoal() {
        override fun toString(): String {
            return "UnknownGoal()"
        }
    }

    /**
     * An [ExerciseCompletionGoal] that has no specific target metric. It is up to the user to
     * determine when the associated [PlannedExerciseStep] is complete, typically based upon some
     * instruction in the [PlannedExerciseStep.description] field.
     */
    object ManualCompletion : ExerciseCompletionGoal() {
        override fun toString(): String {
            return "ManualCompletion()"
        }
    }
}
