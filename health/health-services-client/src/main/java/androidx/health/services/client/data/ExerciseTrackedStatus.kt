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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseTrackedStatus.EXERCISE_TRACKED_STATUS_UNKNOWN

/** Status representing if an exercise is being tracked and which app owns the exercise. */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS,
    ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS,
    ExerciseTrackedStatus.NO_EXERCISE_IN_PROGRESS
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public annotation class ExerciseTrackedStatus {

    public companion object {
        /** Exercise Tracked Status is an unknown or unexpected value. */
        public const val UNKNOWN: Int = 0
        /** An app other than the calling one owns the active exercise in progress. */
        public const val OTHER_APP_IN_PROGRESS: Int = 1
        /** The current calling app owns the active exercise in progress. */
        public const val OWNED_EXERCISE_IN_PROGRESS: Int = 2
        /** There is not currently any exercise in progress owned by any app. */
        public const val NO_EXERCISE_IN_PROGRESS: Int = 3

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun @receiver:ExerciseTrackedStatus Int.toProto():
            DataProto.ExerciseTrackedStatus =
            DataProto.ExerciseTrackedStatus.forNumber(this) ?: EXERCISE_TRACKED_STATUS_UNKNOWN

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @ExerciseTrackedStatus
        @Suppress("WrongConstant")
        public fun fromProto(proto: DataProto.ExerciseTrackedStatus): Int = proto.number
    }
}
