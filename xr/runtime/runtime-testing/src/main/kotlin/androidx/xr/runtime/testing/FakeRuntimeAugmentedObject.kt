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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.AugmentedObject as RuntimeObject
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Fake implementation of [RuntimeObject] for testing purposes.
 *
 * @property centerPose The pose of the center of the augmented object.
 * @property extents The extents of the augmented object.
 * @property category The category of the augmented object.
 * @property trackingState The tracking state of the augmented object.
 * @property anchors The anchors attached to this object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeRuntimeAugmentedObject(
    override var centerPose: Pose = Pose(),
    override var extents: Vector3 = Vector3.Zero,
    override var category: AugmentedObjectCategory = AugmentedObjectCategory.KEYBOARD,
    override var trackingState: TrackingState = TrackingState.TRACKING,
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimeObject, AnchorHolder {

    override fun createAnchor(pose: Pose): RuntimeAnchor {
        val anchor = FakeRuntimeAnchor(pose, this)
        anchors.add(anchor)
        return anchor
    }

    override fun detachAnchor(anchor: RuntimeAnchor) {
        anchors.remove(anchor)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY) override fun onAnchorPersisted(anchor: RuntimeAnchor) {}
}
