/*
 * Copyright (C) 2022 The Android Open Source Project
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

/** Wrapper class for accessing [ExerciseState] and [ExerciseEndReason]. */
public class ExerciseStateInfo(
    exerciseState: ExerciseState,
    @ExerciseEndReason exerciseEndReason: Int
) {

    /**
     * The [ExerciseEndReason] if [state] is [ExerciseState.ENDED], otherwise
     * [ExerciseEndReason.UNKNOWN].
     */
    @ExerciseEndReason public val endReason: Int

    /**
     * The [ExerciseState]. If set to [ExerciseState.ENDED], the [endReason] property will contain
     * why it was ended.
     */
    public val state: ExerciseState

    init {
        // If passed exerciseEndReason is UNKNOWN, use the value indicated by ExerciseState.

        if (exerciseEndReason != ExerciseEndReason.UNKNOWN) {
            endReason = exerciseEndReason
            state = exerciseState
        } else {
            endReason = getEndReasonFromState(exerciseState)
            // Mark exerciseState as ExerciseState.ENDED or ENDING if we set exerciseEndReason from
            // exerciseState.
            state =
                if (endReason != ExerciseEndReason.UNKNOWN) {
                    if (exerciseState.isEnded) {
                        ExerciseState.ENDED
                    } else {
                        ExerciseState.ENDING
                    }
                } else {
                    exerciseState
                }
        }
    }

    override fun toString(): String = "ExerciseStateInfo(state=$state, endReason=$endReason)"

    override fun equals(other: Any?): Boolean {
        if (other !is ExerciseStateInfo) return false
        return (endReason == other.endReason && state == other.state)
    }

    override fun hashCode(): Int = 31 * endReason + state.hashCode()

    public companion object {

        /** Gets the [ExerciseEndReason] from the current [ExerciseState]. */
        @ExerciseEndReason
        internal fun getEndReasonFromState(exerciseState: ExerciseState): Int =
            when (exerciseState) {
                // Follows ordering of ExerciseState definition in ExerciseState.kt
                // ENDING states.
                ExerciseState.USER_ENDING -> ExerciseEndReason.USER_END
                ExerciseState.AUTO_ENDING -> ExerciseEndReason.AUTO_END_MISSING_LISTENER
                ExerciseState.AUTO_ENDING_PERMISSION_LOST ->
                    ExerciseEndReason.AUTO_END_PERMISSION_LOST
                ExerciseState.TERMINATING -> ExerciseEndReason.AUTO_END_SUPERSEDED
                ExerciseState.ENDING -> ExerciseEndReason.AUTO_END_PERMISSION_LOST

                // ENDED states.
                ExerciseState.USER_ENDED -> ExerciseEndReason.USER_END
                ExerciseState.AUTO_ENDED -> ExerciseEndReason.AUTO_END_MISSING_LISTENER
                ExerciseState.AUTO_ENDED_PERMISSION_LOST ->
                    ExerciseEndReason.AUTO_END_PERMISSION_LOST
                ExerciseState.TERMINATED -> ExerciseEndReason.AUTO_END_SUPERSEDED
                ExerciseState.ENDED -> ExerciseEndReason.AUTO_END_PERMISSION_LOST
                else -> ExerciseEndReason.UNKNOWN
            }
    }
}
