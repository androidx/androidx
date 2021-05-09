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

/** Enumerates the state of an exercise. */
public enum class ExerciseState(public val id: Int) {
    /**
     * The exercise is actively being started, but we don't yet have sensor stability or GPS fix.
     *
     * Used only in the manually started exercise.
     */
    USER_STARTING(1),

    /**
     * The exercise is actively in-progress.
     *
     * Used in both of the manually started exercise and the automatic exercise detection. It's also
     * the state when the automatic exercise detection has detected an exercise and the exercise is
     * actively in-progress.
     */
    ACTIVE(2),

    /**
     * The session is being paused by the user. Sensors are actively being flushed.
     *
     * Used only in the manually started exercise.
     */
    USER_PAUSING(3),

    /**
     * The session has been paused by the user. Sensors have completed flushing.
     *
     * Used only in the manually started exercise.
     */
    USER_PAUSED(4),

    /**
     * The session is being paused by auto-pause. Sensors are actively being flushed.
     *
     * Used only in the manually started exercise.
     */
    AUTO_PAUSING(5),

    /**
     * The session has been automatically paused. Sensors have completed flushing.
     *
     * Used only in the manually started exercise.
     */
    AUTO_PAUSED(6),

    /**
     * The session is being resumed by the user.
     *
     * Used only in the manually started exercise.
     */
    USER_RESUMING(7),

    /**
     * The session is being automatically resumed.
     *
     * Used only in the manually started exercise.
     */
    AUTO_RESUMING(8),

    /**
     * The exercise is being ended by the user. Sensors are actively being flushed.
     *
     * Used only in the manually started exercise.
     */
    USER_ENDING(9),

    /**
     * The exercise has been ended by the user. No new metrics will be exported and a final summary
     * should be provided to the client.
     *
     * Used only in the manually started exercise.
     */
    USER_ENDED(10),

    /**
     * The exercise is being automatically ended due to a lack of exercise updates being received by
     * the user. Sensors are actively being flushed.
     *
     * Used only in the manually started exercise.
     */
    AUTO_ENDING(11),

    /**
     * The exercise has been automatically ended due to a lack of exercise updates being received by
     * the user. No new metrics will be exported and a final summary should be provided to the
     * client.
     *
     * Used only in the manually started exercise.
     */
    AUTO_ENDED(12),

    /**
     * The exercise is being ended because it has been superseded by a new exercise being started by
     * another client. Sensors are actively being flushed.
     *
     * Used in both of the manually started exercise and the automatic exercise detection.
     */
    TERMINATING(13),

    /**
     * The exercise has been ended because it was superseded by a new exercise being started by
     * another client. No new metrics will be exported and a final summary should be provided to the
     * client.
     *
     * Used in both of the manually started exercise and the automatic exercise detection.
     */
    TERMINATED(14);

    /**
     * Returns true if this [ExerciseState] corresponds to one of the paused states and false
     * otherwise. This method returns false if the exercise has ended, to check whether it has ended
     * call [isEnded].
     */
    public val isPaused: Boolean
        get() = PAUSED_STATES.contains(this)

    /**
     * Returns true if this [ExerciseState] corresponds to one of the resuming states and false
     * otherwise. This method returns false if the exercise has ended, to check whether it has ended
     * call [isEnded].
     */
    public val isResuming: Boolean
        get() = RESUMING_STATES.contains(this)

    /**
     * Returns true if this [ExerciseState] corresponds to one of the ended states and false
     * otherwise. This method returns false if the exercise has been paused, to check whether it is
     * currently paused call [isPaused].
     */
    public val isEnded: Boolean
        get() = ENDED_STATES.contains(this)

    public companion object {
        private val RESUMING_STATES = setOf(USER_RESUMING, AUTO_RESUMING)
        private val PAUSED_STATES = setOf(USER_PAUSED, AUTO_PAUSED)
        private val ENDED_STATES = setOf(USER_ENDED, AUTO_ENDED, TERMINATED)

        @JvmStatic public fun fromId(id: Int): ExerciseState? = values().firstOrNull { it.id == id }
    }
}
