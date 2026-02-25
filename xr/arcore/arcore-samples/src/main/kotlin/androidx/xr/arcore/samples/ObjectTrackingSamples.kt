/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.samples

import androidx.annotation.Sampled
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.arcore.AugmentedObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * @param session the [Session] to get the augmented objects from
 * @param lifecycle the [Lifecycle] to scope the coroutine to
 */
@Sampled
fun getAugmentedObjects(session: Session, lifecycle: Lifecycle) {
    // Use a coroutine to listen to changes to the set of detected objects.
    yourCoroutineScope.launch {
        val activeObjects = mutableMapOf<AugmentedObject, Job>()

        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val supervisor = SupervisorJob()
            val supervisorScope = CoroutineScope(yourCoroutineScope.coroutineContext + supervisor)
            try {
                AugmentedObject.subscribe(session).collect { augmentedObjects ->
                    // The list of detected objects has changed.
                    for (obj in augmentedObjects) {

                        // If an object doesn't exist in our set of active objects, set up a
                        // coroutine to respond to its state changes.
                        if (!activeObjects.contains(obj)) {
                            val job =
                                supervisorScope.launch {
                                    obj.state.collect {
                                        // if the object is not currently reporting as tracked, then
                                        // we don't want to render it.
                                        if (it.trackingState != TrackingState.TRACKING)
                                            return@collect

                                        // Transform the pose from its original coordinate space to
                                        // one suitable for rendering to the display.
                                        val pose =
                                            it.centerPose.let { p ->
                                                session.scene.perceptionSpace.transformPoseTo(
                                                    p,
                                                    session.scene.activitySpace,
                                                )
                                            }

                                        // This function is where you'll actually render the object
                                        // to the display.
                                        renderFunction(pose, it.extents, it.category)
                                    }
                                }
                            activeObjects[obj] = job
                        }

                        // Likewise, if a object exists in the `activeObjects` map, but not in our
                        // `augmentedObjects` list, it needs to be removed, and its corresponding
                        // job canceled.
                        for (obj in activeObjects.keys.toList()) {
                            if (augmentedObjects.none { it == obj }) {
                                activeObjects.remove(obj)?.cancel()
                            }
                        }
                    }
                }
            } finally {
                // cancel any coroutines still running.
                supervisor.cancel()
                activeObjects.clear()
            }
        }
    }
}

private fun renderFunction(pose: Pose, extents: FloatSize3d, category: AugmentedObjectCategory) {}

private val yourCoroutineScope = MainScope()
