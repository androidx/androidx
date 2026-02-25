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
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateIllegalState
import androidx.xr.arcore.AnchorCreateNotAuthorized
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorCreateTrackingUnavailable
import androidx.xr.arcore.AnchorCreateUnsupportedLocation
import androidx.xr.arcore.AnchorCreateUnsupportedObject
import androidx.xr.arcore.AnchorLoadInvalidUuid
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.scene
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * @param session the [Session] to create the anchor in
 * @param pose the [Pose] of the anchor
 */
@Sampled
fun callCreateAnchor(session: Session, pose: Pose) {
    // We need to first translate the pose from activity space to perception space.
    val perceptionPose =
        session.scene.activitySpace.transformPoseTo(pose, session.scene.perceptionSpace)

    // Now we can try and create the anchor.
    // Anchor creation can fail for a variety of reasons, so we need to handle those various cases.
    when (val anchorResult = Anchor.create(session, perceptionPose)) {
        is AnchorCreateSuccess -> {
            // Our call succeeded, and we can now make use of our new anchor. In this example,
            // we are simply going to convert our pose back to activity space and then pass it to a
            // rendering function for rendering.
            val anchor = anchorResult.anchor
            yourCoroutineScope.launch {
                anchor.state.collect {
                    // Early out if tracking is lost on the anchor; we only want to render it when
                    // we have confidence it is in the correct position.
                    if (it.trackingState != TrackingState.TRACKING) return@collect

                    // Convert our anchor pose back into activity space.
                    val activityPose =
                        session.scene.perceptionSpace.transformPoseTo(
                            it.pose,
                            session.scene.activitySpace,
                        )

                    // Render the anchor.
                    yourAnchorRenderingFunction(activityPose)
                }
            }
        }
        is AnchorCreateIllegalState -> {
            // This result indicates the session was in an invalid state, which usually means that
            // the session has been paused or destroyed.
        }
        is AnchorCreateNotAuthorized -> {
            // This result indicates that the activity is lacking sufficient permissions to create
            // an anchor. It is recommended that applications both check for appropriate permissions
            // before attempting to create an Anchor as well as handling this result; it is possible
            // for permissions to be granted initially and then revoked at runtime, so even
            // applications that check for permission in advance can still potentially get this
            // result.
        }
        is AnchorCreateResourcesExhausted -> {
            // In order to conserve resources, runtimes impose a limit on how many anchors may be
            // in use concurrently. When that limit is reached, subsequent `Anchor.create()` will
            // fail with this result. Depending on your use case, you can either notify the user
            // that no more anchors can be created, or you can try and free up resources by removing
            // older anchors if they're no longer necessary.
        }
        is AnchorCreateTrackingUnavailable -> {
            // This result indicates that tracking is currently unavailable. Depending on the
            // situation, tracking may or may not return, so this result could just indicate a
            // temporary problem. Repeatedly getting this result for a prolonged period of time may
            // indicate a non-recoverable loss of tracking, which will likely severely impact
            // application functionality.
        }
        is AnchorCreateUnsupportedLocation -> {
            // This result indicates that the underlying runtime does not support creating anchors
            // in this particular location.
        }
        is AnchorLoadInvalidUuid -> {
            // This result only occurs when calling `Anchor.load()` to load a persistent anchor with
            // an invalid UUID.
        }
        is AnchorCreateUnsupportedObject -> {
            // This result occurs when calling `createAnchor()` with a `HitResult` against a
            // `Trackable` that doesn't implement `Anchorable`.
        }
    }
}

private fun yourAnchorRenderingFunction(pose: Pose) {}

private val yourCoroutineScope = MainScope()
