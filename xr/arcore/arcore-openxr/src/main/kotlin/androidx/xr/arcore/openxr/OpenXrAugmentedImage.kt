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

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.AugmentedImage
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose

/**
 * Wraps a native
 * [XrTrackableImageANDROID](https://registry.khronos.org/OpenXR/specs/1.1/man/html/XrTrackableImageANDROID.html)
 * with the [AugmentedImage] interface.
 *
 * @property augmentedImageId the ID of the image
 * @property centerPose the [Pose] of the center of the image
 * @property extents the extents of the image
 * @property trackingState the [TrackingState] of the image
 */
internal class OpenXrAugmentedImage internal constructor(internal val augmentedImageId: Long) :
    AugmentedImage, Updatable {
    override var centerPose: Pose = Pose()
        private set

    override var extents: FloatSize2d = FloatSize2d()
        private set

    override var index: Int = 0
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override fun update(xrTime: Long) {
        val augmentedImageState = nativeGetAugmentedImageState(augmentedImageId, xrTime)
        if (augmentedImageState == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        trackingState = augmentedImageState.trackingState
        centerPose = augmentedImageState.centerPose
        extents = augmentedImageState.extents
        index = augmentedImageState.index
    }

    private external fun nativeGetAugmentedImageState(
        augmentedImageId: Long,
        timestampNs: Long,
    ): AugmentedImageState?
}
