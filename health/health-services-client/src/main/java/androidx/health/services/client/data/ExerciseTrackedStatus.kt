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

/** Status representing if an exercise is being tracked and which app owns the exercise. */
public enum class ExerciseTrackedStatus(public val id: Int) {
    OTHER_APP_IN_PROGRESS(1),
    OWNED_EXERCISE_IN_PROGRESS(2),
    NO_EXERCISE_IN_PROGRESS(3);

    public companion object {
        @JvmStatic
        public fun fromId(id: Int): ExerciseTrackedStatus? = values().firstOrNull { it.id == id }
    }
}
