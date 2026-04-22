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

package androidx.xr.arcore.testing

import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AugmentedImage as RuntimeImage
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose

/**
 * Test-only implementation of [RuntimeImage]
 *
 * The properties of the [FakeRuntimeAugmentedImage] can be set manually in order to simulate a
 * runtime image in the environment.
 *
 * For example, for a [FakeRuntimeAugmentedImage] with [TrackingState.PAUSED]:
 * ```
 * val image = FakeRuntimeImage(trackingState = TrackingState.PAUSED)
 * ```
 *
 * And to modify the properties during the test:
 * ```
 * augmentedImage.apply {
 *     trackingState = TrackingState.TRACKING
 *     centerPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f))
 * }
 * ```
 */
@SuppressWarnings("HiddenSuperclass")
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
public class FakeRuntimeAugmentedImage(
    override var trackingState: TrackingState = TrackingState.TRACKING,
    override var centerPose: Pose = Pose(),
    override var extents: FloatSize2d = FloatSize2d(),
    override var index: Int = 0,
    /** The anchors that are attached to this image. */
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimeImage {}
