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
import androidx.health.services.client.proto.DataProto.ExerciseGoalType.EXERCISE_GOAL_TYPE_UNKNOWN

/** Exercise goal types. */
public class ExerciseGoalType private constructor(public val id: Int, public val name: String) {

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseGoalType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun toProto(): DataProto.ExerciseGoalType =
        DataProto.ExerciseGoalType.forNumber(id) ?: EXERCISE_GOAL_TYPE_UNKNOWN

    public companion object {
        /** Goal type indicating this goal is for one event and should then be removed. */
        @JvmField public val ONE_TIME_GOAL: ExerciseGoalType = ExerciseGoalType(1, "ONE_TIME_GOAL")

        /**
         * Goal type indicating this goal is for a repeating event and should remain until the
         * calling app removes it.
         */
        @JvmField public val MILESTONE: ExerciseGoalType = ExerciseGoalType(2, "MILESTONE")

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val VALUES: List<ExerciseGoalType> = listOf(ONE_TIME_GOAL, MILESTONE)

        @JvmStatic
        public fun fromId(id: Int): ExerciseGoalType? = VALUES.firstOrNull { it.id == id }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        internal fun fromProto(proto: DataProto.ExerciseGoalType): ExerciseGoalType? =
            fromId(proto.number)
    }
}
