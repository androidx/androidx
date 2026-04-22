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

package androidx.xr.arcore.playservices

import androidx.xr.arcore.runtime.AugmentedImage
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import com.google.ar.core.AugmentedImage as ARCoreAugmentedImage

/** Wraps the [ARCoreAugmentedImage] with an implementation of the [AugmentedImage] interface. */
internal class ArCoreAugmentedImage
internal constructor(internal val _arCoreAugmentedImage: ARCoreAugmentedImage) :
    AugmentedImage, Trackable {

    /**
     * The pose of the augmented image's center.
     *
     * This property gets the pose from the underlying [ARCoreAugmentedImage] instance, and converts
     * it to a [Pose].
     *
     * @return The pose of the augmented image's center.
     */
    override val centerPose: Pose
        get() = _arCoreAugmentedImage.centerPose.toRuntimePose()

    /**
     * The extents of the augmented image.
     *
     * This property gets the extents from the underlying [ARCoreAugmentedImage] instance, and
     * converts it to a [FloatSize2d].
     *
     * @return The extents of the augmented image.
     */
    override val extents: FloatSize2d
        get() = FloatSize2d(_arCoreAugmentedImage.extentX, _arCoreAugmentedImage.extentZ)

    /**
     * The tracking state of the augmented image.
     *
     * This property gets the tracking state from the underlying [ARCoreAugmentedImage] instance,
     * and converts it to a [TrackingState].
     *
     * @return The tracking state of the augmented image.
     */
    override val trackingState: TrackingState
        get() = TrackingState.fromArCoreTrackingState(_arCoreAugmentedImage.trackingState)

    /**
     * The index of the augmented image in the originating image database.
     *
     * This property gets the zero-based position index of the image from its originating image
     * database. This index serves as the unique identifier for the image in the database.
     *
     * @return The index of the augmented image in the originating image database.
     */
    override val index: Int
        get() = _arCoreAugmentedImage.index
}
