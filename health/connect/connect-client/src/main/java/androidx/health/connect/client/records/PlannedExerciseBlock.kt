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
/** Represents a series of [PlannedExerciseStep]s. Part of a [PlannedExerciseSessionRecord]. */
class PlannedExerciseBlock(
    val repetitions: Int,
    val steps: List<PlannedExerciseStep>,
    val description: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlannedExerciseBlock) return false

        if (repetitions != other.repetitions) return false
        if (description != other.description) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = repetitions
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + steps.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlannedExerciseBlock(repetitions=$repetitions, description=$description, steps=$steps)"
    }
}
