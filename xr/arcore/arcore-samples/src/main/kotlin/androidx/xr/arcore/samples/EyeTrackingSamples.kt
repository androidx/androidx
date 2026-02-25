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
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Eye
import androidx.xr.arcore.HitResult
import androidx.xr.arcore.hitTest
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.scene
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/** @param session the [Session] to get the eye from */
@Sampled
fun getLeftEye(session: Session) {
    // We need the ArDevice object to properly transform the eye pose coordinate space into
    // something we can use.
    val arDevice = ArDevice.getInstance(session)

    // Obtain the left eye from the Session object.
    Eye.left(session)?.let {
        // The left eye is available; launch a coroutine
        // to respond to changes in the eye's state.
        yourCoroutineScope.launch {
            it.state.collect { eyeState ->
                // Early out if the eye is not reported as tracking
                if (eyeState.trackingState != TrackingState.TRACKING) return@collect

                // We are only interested in the eye pose if it is open, so early out if it is
                // closed as well.
                if (!eyeState.isOpen) return@collect

                // The pose we get from the eye is the actual pose of the eye. We want to turn that
                // into something we can use for interacting with other objects in the scene (for
                // example, a hit test). To do that, we need to convert the eye pose into the
                // correct coordinate space, and then use it to create a `Ray` we can pass to the
                // `hitTest()` function.
                //
                // Unlike most other objects, which are reported in the perception coordinate space,
                // the eye pose is reported in the device coordinate space. So we'll first need to
                // convert the device pose into the activity space (via the perception space), and
                // then we can use that to convert the eye pose in to the activity space.
                val transformedPose =
                    eyeState.pose.let { pose ->
                        val headPose =
                            session.scene.perceptionSpace.getScenePoseFromPerceptionPose(
                                arDevice.state.value.devicePose
                            )
                        headPose.transformPoseTo(pose, session.scene.activitySpace)
                    }

                val gazeRay = Ray(transformedPose.translation, transformedPose.backward)

                val hits = hitTest(session, gazeRay)
                yourHitTestHandler(hits)
            }
        }
    }
}

/** @param session the [Session] to get the eye from */
@Sampled
fun getRightEye(session: Session) {
    // We need the ArDevice object to properly transform the eye pose coordinate space into
    // something we can use.
    val arDevice = ArDevice.getInstance(session)

    // Obtain the right eye from the Session object.
    Eye.right(session)?.let {
        // The left eye is available; launch a coroutine
        // to respond to changes in the eye's state.
        yourCoroutineScope.launch {
            it.state.collect { eyeState ->
                // Early out if the eye is not reported as tracking
                if (eyeState.trackingState != TrackingState.TRACKING) return@collect

                // We are only interested in the eye pose if it is open, so early out if it is
                // closed as well.
                if (!eyeState.isOpen) return@collect

                // The pose we get from the eye is the actual pose of the eye. We want to turn that
                // into something we can use for interacting with other objects in the scene (for
                // example, a hit test). To do that, we need to convert the eye pose into the
                // correct coordinate space, and then use it to create a `Ray` we can pass to the
                // `hitTest()` function.
                //
                // Unlike most other objects, which are reported in the perception coordinate space,
                // the eye pose is reported in the device coordinate space. So we'll first need to
                // convert the device pose into the activity space (via the perception space), and
                // then we can use that to convert the eye pose in to the activity space.
                val transformedPose =
                    eyeState.pose.let { pose ->
                        val headPose =
                            session.scene.perceptionSpace.getScenePoseFromPerceptionPose(
                                arDevice.state.value.devicePose
                            )
                        headPose.transformPoseTo(pose, session.scene.activitySpace)
                    }

                val gazeRay = Ray(transformedPose.translation, transformedPose.backward)

                val hits = hitTest(session, gazeRay)
                yourHitTestHandler(hits)
            }
        }
    }
}

private fun yourHitTestHandler(hits: List<HitResult>) {}

private val yourCoroutineScope = MainScope()
