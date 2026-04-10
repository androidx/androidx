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

package androidx.xr.arcore.testing.internal

import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.runtime.Plane.Label
import androidx.xr.arcore.runtime.Plane.Type
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

internal class FakeRuntimePlane(
    override var type: Type = Type.HORIZONTAL_UPWARD_FACING,
    override var label: Label = Label.FLOOR,
    override var trackingState: TrackingState = TrackingState.TRACKING,
    centerPose: Pose = Pose(),
    override var extents: FloatSize2d = FloatSize2d(),
    override var vertices: List<Vector2> = emptyList(),
    override var subsumedBy: Plane? = null,
) : Plane, AnchorHolder {

    val anchors: MutableCollection<FakeRuntimeAnchor> = mutableListOf()

    override var centerPose: Pose = centerPose
        // when this Trackable moves, its Anchors move with it.
        set(value) {
            field = value
            anchors.forEach { it.pose = value.compose(it.pose) }
        }

    override fun createAnchor(pose: Pose): Anchor {
        if (trackingState != TrackingState.TRACKING) {
            throw AnchorNotTrackingException()
        }
        val anchor = FakeRuntimeAnchor(centerPose.compose(pose), anchorHolder = this)
        anchors.add(anchor)
        return anchor
    }

    override fun detachAnchor(anchor: Anchor) {
        anchors.remove(anchor)
    }

    override fun onAnchorPersisted(anchor: Anchor) {}
}
