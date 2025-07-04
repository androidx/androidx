/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.Plane as RuntimePlane
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/**
 * Test-only implementation of [RuntimePlane]
 *
 * The properties of the [FakeRuntimePlane] can be set manually in order to simulate a runtime plane
 * in the environment.
 *
 * For example, for a [FakeRuntimePlane] with [Label.WALL], [Type.VERTICAL] and
 * [TrackingState.PAUSED]:
 * ```
 * val plane = FakeRuntimePlane(type = RuntimePlane.Type.VERTICAL,
 *                              label = RuntimePlane.Label.WALL,
 *                              trackingState = TrackingState.PAUSED)
 * ```
 *
 * And to modify the properties during the test:
 * ```
 * plane.apply {
 *     trackingState = TrackingState.TRACKING
 *     centerPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f))
 * }
 * ```
 */
public class FakeRuntimePlane(
    override val type: RuntimePlane.Type = RuntimePlane.Type.HORIZONTAL_UPWARD_FACING,
    override val label: RuntimePlane.Label = RuntimePlane.Label.FLOOR,
    override var trackingState: TrackingState = TrackingState.TRACKING,
    override var centerPose: Pose = Pose(),
    override var extents: Vector2 = Vector2.Zero,
    override var vertices: List<Vector2> = emptyList(),
    override var subsumedBy: RuntimePlane? = null,
    /** The anchors that are attached to this plane. */
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimePlane, AnchorHolder {

    /** Creates a new [FakeRuntimeAnchor] and adds it to the [anchors] property. */
    override fun createAnchor(pose: Pose): RuntimeAnchor {
        val anchor = FakeRuntimeAnchor(pose, this)
        anchors.add(anchor)
        return anchor
    }

    /** Removes the given [anchor] from the [anchors] property. */
    override fun detachAnchor(anchor: RuntimeAnchor) {
        anchors.remove(anchor)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY) override fun onAnchorPersisted(anchor: RuntimeAnchor) {}
}
