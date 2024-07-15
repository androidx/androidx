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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * A single step within an [PlannedExerciseBlock] e.g. 8x 60kg barbell squats.
 *
 * @param exerciseType The type of exercise that this step involves.
 * @param exercisePhase The phase e.g. 'warmup' that this step belongs to.
 * @param description The description of this step.
 * @param completionGoal The goal that must be completed to finish this step.
 * @param performanceTargets Performance related targets that should be met during this step.
 */
class PlannedExerciseStep(
    @property:ExerciseSegment.Companion.ExerciseSegmentTypes val exerciseType: Int,
    @property:ExercisePhase val exercisePhase: Int,
    val completionGoal: ExerciseCompletionGoal,
    val performanceTargets: List<ExercisePerformanceTarget>,
    val description: String? = null,
) {
    companion object {
        /* Next Id: 6. */
        /** An unknown phase of exercise. */
        const val EXERCISE_PHASE_UNKNOWN = 0
        /** A warmup. */
        const val EXERCISE_PHASE_WARMUP = 1
        /** A rest. */
        const val EXERCISE_PHASE_REST = 2
        /** Active exercise. */
        const val EXERCISE_PHASE_ACTIVE = 3
        /** Cooldown exercise, typically at the end of a workout. */
        const val EXERCISE_PHASE_COOLDOWN = 4
        /** Lower intensity, active exercise. */
        const val EXERCISE_PHASE_RECOVERY = 5

        /** List of supported exercise phase types. */
        @Retention(AnnotationRetention.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            value =
                [
                    EXERCISE_PHASE_UNKNOWN,
                    EXERCISE_PHASE_WARMUP,
                    EXERCISE_PHASE_REST,
                    EXERCISE_PHASE_ACTIVE,
                    EXERCISE_PHASE_COOLDOWN,
                    EXERCISE_PHASE_RECOVERY,
                ]
        )
        annotation class ExercisePhase
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlannedExerciseStep) return false

        if (exerciseType != other.exerciseType) return false
        if (exercisePhase != other.exercisePhase) return false
        if (description != other.description) return false
        if (completionGoal != other.completionGoal) return false
        if (performanceTargets != other.performanceTargets) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exerciseType
        result = 31 * result + exercisePhase
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + completionGoal.hashCode()
        result = 31 * result + performanceTargets.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlannedExerciseStep(exerciseType=$exerciseType, exerciseCategory=$exercisePhase, description=$description, completionGoal=$completionGoal, performanceTargets=$performanceTargets)"
    }
}
