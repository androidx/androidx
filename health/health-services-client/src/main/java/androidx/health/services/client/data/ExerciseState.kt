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

import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.DataProto

/** The state of an exercise. */
public class ExerciseState private constructor(public val id: Int, public val name: String) {

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

    /**
     * Returns true if this [ExerciseState] corresponds to one of the ending states and false
     * otherwise. This method returns false if the exercise has ended, to check whether it has ended
     * call [isEnded].
     */
    public val isEnding: Boolean
        get() = ENDING_STATES.contains(this)

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseState) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun toProto(): DataProto.ExerciseState =
        DataProto.ExerciseState.forNumber(id) ?: DataProto.ExerciseState.EXERCISE_STATE_UNKNOWN

    public companion object {
        /**
         * The exercise is being prepared, GPS and HeartRate sensors will be turned on if requested
         * in the [WarmUpConfig].
         */
        @JvmField
        public val PREPARING: ExerciseState = ExerciseState(15, "PREPARING")

        /**
         * The exercise is actively being started, but we don't yet have sensor stability or GPS
         * fix.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val USER_STARTING: ExerciseState = ExerciseState(1, "USER_STARTING")

        /**
         * The exercise is actively in-progress.
         *
         * Used in both of the manually started exercise and the automatic exercise detection. It's
         * also the state when the automatic exercise detection has detected an exercise and the
         * exercise is actively in-progress.
         */
        @JvmField
        public val ACTIVE: ExerciseState = ExerciseState(2, "ACTIVE")

        /**
         * The session is being paused by the user. Sensors are actively being flushed.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val USER_PAUSING: ExerciseState = ExerciseState(3, "USER_PAUSING")

        /**
         * The session has been paused by the user. Sensors have completed flushing.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val USER_PAUSED: ExerciseState = ExerciseState(4, "USER_PAUSED")

        /**
         * The session is being paused by auto-pause. Sensors are actively being flushed.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val AUTO_PAUSING: ExerciseState = ExerciseState(5, "AUTO_PAUSING")

        /**
         * The session has been automatically paused. Sensors have completed flushing.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val AUTO_PAUSED: ExerciseState = ExerciseState(6, "AUTO_PAUSED")

        /**
         * The session is being resumed by the user.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val USER_RESUMING: ExerciseState = ExerciseState(7, "USER_RESUMING")

        /**
         * The session is being automatically resumed.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val AUTO_RESUMING: ExerciseState = ExerciseState(8, "AUTO_RESUMING")

        /**
         * The exercise is being ended by the user. Sensors are actively being flushed.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val USER_ENDING: ExerciseState = ExerciseState(9, "USER_ENDING")

        /**
         * The exercise has been ended by the user. No new metrics will be exported and a final
         * summary should be provided to the client.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        internal val USER_ENDED: ExerciseState = ExerciseState(10, "USER_ENDED")

        /**
         * The exercise is being automatically ended due to a lack of exercise updates being
         * received by the user. Sensors are actively being flushed.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        public val AUTO_ENDING: ExerciseState = ExerciseState(11, "AUTO_ENDING")

        /**
         * The exercise has been automatically ended due to a lack of exercise updates being
         * received by the user. No new metrics will be exported and a final summary should be
         * provided to the client.
         *
         * Used only in the manually started exercise.
         */
        @JvmField
        internal val AUTO_ENDED: ExerciseState = ExerciseState(12, "AUTO_ENDED")

        /**
         * The exercise is being automatically ended due to lack of client's permissions to receive
         * data for the exercise.
         */
        @JvmField
        public val AUTO_ENDING_PERMISSION_LOST: ExerciseState =
            ExerciseState(16, "AUTO_ENDING_PERMISSION_LOST")

        /**
         * The exercise has been automatically ended due to lack of client's permissions to receive
         * data for the exercise.
         */
        @JvmField
        internal val AUTO_ENDED_PERMISSION_LOST: ExerciseState =
            ExerciseState(17, "AUTO_ENDED_PERMISSION_LOST")

        /**
         * The exercise is being ended because it has been superseded by a new exercise being
         * started by another client. Sensors are actively being flushed.
         *
         * Used in both of the manually started exercise and the automatic exercise detection.
         */
        @JvmField
        public val TERMINATING: ExerciseState = ExerciseState(13, "TERMINATING")

        /**
         * The exercise has been ended because it was superseded by a new exercise being started by
         * another client. No new metrics will be exported and a final summary should be provided to
         * the client.
         *
         * Used in both of the manually started exercise and the automatic exercise detection.
         */
        @JvmField
        internal val TERMINATED: ExerciseState = ExerciseState(14, "TERMINATED")

        /**
         * The exercise has been ended, with the reason specified by [ExerciseStateInfo.endReason].
         */
        @JvmField
        public val ENDED: ExerciseState = ExerciseState(18, "ENDED")

        private val RESUMING_STATES = setOf(USER_RESUMING, AUTO_RESUMING)
        private val PAUSED_STATES = setOf(USER_PAUSED, AUTO_PAUSED)
        private val ENDED_STATES =
            setOf(USER_ENDED, AUTO_ENDED, AUTO_ENDED_PERMISSION_LOST, TERMINATED, ENDED)
        private val ENDING_STATES =
            setOf(USER_ENDING, AUTO_ENDING, AUTO_ENDING_PERMISSION_LOST, TERMINATING)
        private val OTHER_STATES =
            setOf(PREPARING, USER_STARTING, USER_PAUSING, AUTO_PAUSING, ACTIVE)

        private val VALUES: Set<ExerciseState> =
            HashSet<ExerciseState>().apply {
                addAll(OTHER_STATES)
                addAll(RESUMING_STATES)
                addAll(PAUSED_STATES)
                addAll(ENDED_STATES)
                addAll(ENDING_STATES)
            }

        /**
         * Returns the [ExerciseState] object corresponding to [id] or `null` if none match.
         *
         * @param id the [ExerciseState.id] to match against
         */
        @JvmStatic
        public fun fromId(id: Int): ExerciseState? = VALUES.firstOrNull { it.id == id }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        public fun fromProto(proto: DataProto.ExerciseState): ExerciseState? = fromId(proto.number)
    }
}
