/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.openxr.testing

import androidx.xr.scenecore.testing.FakeScheduledExecutorService

/**
 * Lets a scene committer that runs on this executor commit the scene changes made so far: runs the
 * scheduled tasks one at a time, each followed by the tasks posted meanwhile, until no task is
 * scheduled, or 100 times.
 */
internal fun FakeScheduledExecutorService.commitSceneChanges() {
    repeat(100) {
        val hasRunTask = simulateSleepExecutingAtMostOneTask()
        runAll()
        if (!hasRunTask) return
    }
}
