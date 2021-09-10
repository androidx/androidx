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

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseGoalType.EXERCISE_GOAL_TYPE_MILESTONE
import androidx.health.services.client.proto.DataProto.ExerciseGoalType.EXERCISE_GOAL_TYPE_ONE_TIME
import androidx.health.services.client.proto.DataProto.ExerciseGoalType.EXERCISE_GOAL_TYPE_UNKNOWN

/** Exercise goal types. */
public enum class ExerciseGoalType(public val id: Int) {
    ONE_TIME_GOAL(1),
    MILESTONE(2);

    /** @hide */
    internal fun toProto(): DataProto.ExerciseGoalType =
        when (this) {
            ONE_TIME_GOAL -> EXERCISE_GOAL_TYPE_ONE_TIME
            MILESTONE -> EXERCISE_GOAL_TYPE_MILESTONE
        }

    public companion object {
        @JvmStatic
        public fun fromId(id: Int): ExerciseGoalType? = values().firstOrNull { it.id == id }

        /** @hide */
        @JvmStatic
        internal fun fromProto(proto: DataProto.ExerciseGoalType): ExerciseGoalType? =
            when (proto) {
                EXERCISE_GOAL_TYPE_ONE_TIME -> ONE_TIME_GOAL
                EXERCISE_GOAL_TYPE_MILESTONE -> MILESTONE
                EXERCISE_GOAL_TYPE_UNKNOWN -> null
            }
    }
}
