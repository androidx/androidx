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
import androidx.xr.arcore.AnchorCreateResult
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Plane
import androidx.xr.arcore.hitTest
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.scene

/**
 * @param session the [Session] to use for the hit test
 * @param hitOrigin the [Vector3] origin of the hit test ray
 * @param hitDirection the [Vector3] direction of the hit test ray
 */
@Sampled
fun callHitTest(session: Session, hitOrigin: Vector3, hitDirection: Vector3) {
    val arDevice = ArDevice.getInstance(session)

    // Assume hitOrigin and hitDirection are in Activity Space.
    // We need to transform the ray into Perception Space for hitTest.

    // Transform the origin point from Activity Space to Perception Space.
    val perceptionOrigin =
        session.scene.activitySpace
            .transformPoseTo(Pose(hitOrigin), session.scene.perceptionSpace)
            .translation

    // Transform the direction vector from Activity Space to Perception Space.
    val perceptionDirection =
        session.scene.activitySpace
            .transformPoseTo(Pose(hitDirection), session.scene.perceptionSpace)
            .translation
            .toNormalized() // Ensure the direction is normalized after transformation.

    val ray = Ray(perceptionOrigin, perceptionDirection)

    hitTest(session, ray)
        // Use a valid plane label as a test to filter out any unreliable hits.
        .firstOrNull { (it.trackable as? Plane?)?.state?.value?.label != Plane.Label.UNKNOWN }
        ?.let { hitResult ->
            // Do something with the hit result, like create an anchor representing where the ray
            // intersected the plane. The hitResult.hitPose is in Perception Space.
            val anchorResult = Anchor.create(session, hitResult.hitPose)

            // Anchor creation can fail, so it is important to properly handle the return value of
            // and `Anchor.create()` call. See `Anchor.create()` for more information.
            yourAnchorResultHandler(anchorResult)

            // If rendering is needed, transform hitResult.hitPose from Perception Space
            // to the desired Scene Space (e.g., Activity Space).
            val activitySpaceHitPose =
                session.scene.perceptionSpace.transformPoseTo(
                    hitResult.hitPose,
                    session.scene.activitySpace,
                )
            renderSomething(activitySpaceHitPose)
        }
}

private fun yourAnchorResultHandler(result: AnchorCreateResult) {}

private fun renderSomething(pose: Pose) {}
