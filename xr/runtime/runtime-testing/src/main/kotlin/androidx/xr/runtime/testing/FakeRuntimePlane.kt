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

import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.Plane as RuntimePlane
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/** Test-only implementation of [Plane] */
public class FakeRuntimePlane(
    override val type: RuntimePlane.Type = RuntimePlane.Type.HorizontalUpwardFacing,
    override val label: RuntimePlane.Label = RuntimePlane.Label.Floor,
    override var trackingState: TrackingState = TrackingState.Tracking,
    override var centerPose: Pose = Pose(),
    override var extents: Vector2 = Vector2.Zero,
    override var vertices: List<Vector2> = emptyList(),
    override var subsumedBy: RuntimePlane? = null,
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimePlane, AnchorHolder {

    override fun createAnchor(pose: Pose): RuntimeAnchor {
        val anchor = FakeRuntimeAnchor(pose, this)
        anchors.add(anchor)
        return anchor
    }

    override fun detachAnchor(anchor: RuntimeAnchor) {
        anchors.remove(anchor)
    }

    override fun persistAnchor(anchor: RuntimeAnchor) {}
}
