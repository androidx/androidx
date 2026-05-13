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

package androidx.xr.scenecore.spatial.core

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.TrackableComponent
import androidx.xr.scenecore.runtime.impl.OpenXrScenePose
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

internal class TrackableComponentImpl(
    private val activitySpaceImpl: ActivitySpaceImpl,
    private val lifecycleOwner: LifecycleOwner,
    private val trackable: Trackable<Trackable.State>,
    private val poseExtractor: ((Any?) -> Pose?),
) : TrackableComponent {
    private var entity: Entity? = null
    private var collectionJob: Job? = null

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }
        this.entity = entity

        collectionJob?.cancel()
        collectionJob =
            lifecycleOwner.lifecycleScope.launch {
                trackable.state
                    .mapNotNull { state -> poseExtractor(state) }
                    .collect { newPose -> updatePerceptionPose(newPose) }
            }
        return true
    }

    override fun onDetach(entity: Entity) {
        collectionJob?.cancel()
        collectionJob = null
        this.entity = null
    }

    private fun updatePerceptionPose(pose: Pose) {
        val activitySpacePose = OpenXrScenePose(activitySpaceImpl, pose).activitySpacePose
        entity?.setPose(activitySpacePose, Space.ACTIVITY)
    }
}
