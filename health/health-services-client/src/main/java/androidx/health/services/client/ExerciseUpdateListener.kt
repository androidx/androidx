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

package androidx.health.services.client

import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate

/** Listener that is called when the state of the current exercise is updated. */
// TODO(b/179756577): Add onExerciseEnd(ExerciseSummary) method.
public interface ExerciseUpdateListener {
    /** Called during an ACTIVE exercise or on any changes in [ExerciseState]. */
    public fun onExerciseUpdate(update: ExerciseUpdate)

    /** Called during an ACTIVE exercise once a lap has been marked. */
    public fun onLapSummary(lapSummary: ExerciseLapSummary)
}
