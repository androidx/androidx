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
import androidx.xr.arcore.Plane
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * @param session the [Session] to get the planes from
 * @param lifecycle the [Lifecycle] to scope the coroutine to
 */
@Sampled
fun getPlanes(session: Session, lifecycle: Lifecycle) {
    // Use a coroutine to listen to changes to the set of detected planes.
    yourCoroutineScope.launch {
        val activePlanes = mutableMapOf<Plane, Job>()

        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val supervisor = SupervisorJob()
            val supervisorScope = CoroutineScope(yourCoroutineScope.coroutineContext + supervisor)
            try {
                Plane.subscribe(session).collect { planes ->
                    // The list of detected planes has changed.
                    for (plane in planes) {

                        // If a plane doesn't exist in our set of active planes, set up a
                        // coroutine to respond to its state changes.
                        if (!activePlanes.contains(plane)) {
                            val job =
                                supervisorScope.launch {
                                    plane.state.collect {
                                        // if the plane is not currently reporting as tracked, then
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

                                        // This function is where you'll actually render the plane
                                        // to the display.
                                        renderFunction(pose, it.extents, it.vertices, it.label)
                                    }
                                }
                            activePlanes[plane] = job
                        }

                        // Likewise, if a plane exists in the `activePlanes` map, but not in our
                        // `planes` list, it needs to be removed, and its corresponding job
                        // canceled.
                        for (plane in activePlanes.keys.toList()) {
                            if (planes.none { it == plane }) {
                                activePlanes.remove(plane)?.cancel()
                            }
                        }
                    }
                }
            } finally {
                // cancel any coroutines still running.
                supervisor.cancel()
                activePlanes.clear()
            }
        }
    }
}

private fun renderFunction(
    pose: Pose,
    extents: FloatSize2d,
    vertices: List<Vector2>,
    label: Plane.Label,
) {}

private val yourCoroutineScope = MainScope()
