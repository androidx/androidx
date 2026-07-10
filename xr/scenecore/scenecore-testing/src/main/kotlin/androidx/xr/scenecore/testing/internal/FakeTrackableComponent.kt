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

package androidx.xr.scenecore.testing.internal

import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.TrackableComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class FakeTrackableComponent(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    private val poseFlow: Flow<Pose>? = null,
) : FakeComponent(), TrackableComponent {

    /** For test purpose only. Inspecting the pose updated by [poseFlow]. */
    internal var perceptionPose: Pose? = null
        private set

    private var collectionJob: Job? = null

    override fun onAttach(entity: Entity): Boolean {
        if (poseFlow != null) {
            collectionJob =
                coroutineScope.launch { poseFlow.collect { newPose -> perceptionPose = newPose } }
        }

        return super.onAttach(entity)
    }

    override fun onDetach(entity: Entity) {
        collectionJob?.cancel()

        super.onDetach(entity)
    }
}
