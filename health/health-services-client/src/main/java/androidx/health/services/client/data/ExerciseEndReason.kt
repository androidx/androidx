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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.proto.DataProto
import kotlin.annotation.AnnotationRetention.SOURCE

/** The reason why an exercise has been ended for [ExerciseState] used in [ExerciseStateInfo]. */
@Retention(SOURCE)
@IntDef(
    ExerciseEndReason.UNKNOWN,
    ExerciseEndReason.AUTO_END_PERMISSION_LOST,
    ExerciseEndReason.AUTO_END_PAUSE_EXPIRED,
    ExerciseEndReason.AUTO_END_MISSING_LISTENER,
    ExerciseEndReason.USER_END,
    ExerciseEndReason.AUTO_END_SUPERSEDED,
    ExerciseEndReason.AUTO_END_PREPARE_EXPIRED
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public annotation class ExerciseEndReason {

    public companion object {
        /** The exercise has been ended, but the end reason is not known or has not been set. */
        public const val UNKNOWN: Int = 0

        /**
         * The exercise has been automatically ended due to lack of client's permissions to receive
         * data for the exercise.
         */
        public const val AUTO_END_PERMISSION_LOST: Int = 1

        /** The exercise has been automatically ended due to being paused for too long. */
        public const val AUTO_END_PAUSE_EXPIRED: Int = 2

        /**
         * The exercise being automatically ended, due to a lack of exercise updates being received
         * by the user.
         */
        public const val AUTO_END_MISSING_LISTENER: Int = 3

        /** The exercise has been ended by a direct call to [ExerciseClient.endExerciseAsync]. */
        public const val USER_END: Int = 4

        /**
         * The exercise has been ended because it was superseded by a new exercise being started by
         * another client.
         */
        public const val AUTO_END_SUPERSEDED: Int = 5

        /** The exercise has been ended due to being in PREPARE state for too long. */
        public const val AUTO_END_PREPARE_EXPIRED: Int = 6

        internal fun @receiver:ExerciseEndReason Int.toProto(): DataProto.ExerciseEndReason =
            when (this) {
                UNKNOWN -> DataProto.ExerciseEndReason.EXERCISE_END_REASON_UNKNOWN
                AUTO_END_PERMISSION_LOST ->
                    DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_PERMISSION_LOST
                AUTO_END_PAUSE_EXPIRED ->
                    DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_PAUSE_EXPIRED
                AUTO_END_MISSING_LISTENER ->
                    DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_MISSING_LISTENER
                USER_END -> DataProto.ExerciseEndReason.EXERCISE_END_REASON_USER_END
                AUTO_END_SUPERSEDED ->
                    DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_SUPERSEDED
                AUTO_END_PREPARE_EXPIRED ->
                    DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_PREPARE_EXPIRED
                else -> DataProto.ExerciseEndReason.EXERCISE_END_REASON_UNKNOWN
            }

        @ExerciseEndReason
        internal fun fromProto(proto: DataProto.ExerciseEndReason): Int =
            when (proto) {
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_UNKNOWN -> UNKNOWN
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_PERMISSION_LOST ->
                    AUTO_END_PERMISSION_LOST
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_PAUSE_EXPIRED ->
                    AUTO_END_PAUSE_EXPIRED
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_MISSING_LISTENER ->
                    AUTO_END_MISSING_LISTENER
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_USER_END -> USER_END
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_SUPERSEDED ->
                    AUTO_END_SUPERSEDED
                DataProto.ExerciseEndReason.EXERCISE_END_REASON_AUTO_END_PREPARE_EXPIRED ->
                    AUTO_END_PREPARE_EXPIRED
            }
    }
}
