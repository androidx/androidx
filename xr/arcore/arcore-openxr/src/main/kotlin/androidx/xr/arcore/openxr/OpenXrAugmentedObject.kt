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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.AugmentedObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose

/**
 * Wraps a native
 * [XrTrackableObjectANDROID](https://registry.khronos.org/OpenXR/specs/1.1/man/html/XrTrackableObjectANDROID.html)
 * with the [AugmentedObject] interface.
 *
 * @property objectId the ID of the object
 * @property timeSource the [OpenXrTimeSource] for the object
 * @property xrResources the [XrResources] for the object
 * @property category the [AugmentedObjectCategory] of the object
 * @property centerPose the [Pose] of the center of the object
 * @property extents the extents of the object
 * @property trackingState the [TrackingState] of the object
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrAugmentedObject
internal constructor(
    internal val objectId: Long,
    internal val timeSource: OpenXrTimeSource,
    internal val xrResources: XrResources,
) : AugmentedObject, Updatable {
    override var category: AugmentedObjectCategory = AugmentedObjectCategory.UNKNOWN
        private set

    override var centerPose: Pose = Pose()
        private set

    override var extents: FloatSize3d = FloatSize3d()
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override fun update(xrTime: Long) {
        val objState = nativeGetAugmentedObjectState(objectId, xrTime)
        if (objState == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        category = categoryFromNativeValue(objState.category)
        trackingState = objState.trackingState
        centerPose = objState.centerPose
        extents = objState.extents
    }

    private external fun nativeGetAugmentedObjectState(
        objectId: Long,
        timestampNs: Long,
    ): AugmentedObjectState?
}

internal fun categoryFromNativeValue(value: Long): AugmentedObjectCategory {
    return when (value) {
        1L -> AugmentedObjectCategory.KEYBOARD
        2L -> AugmentedObjectCategory.MOUSE
        3L -> AugmentedObjectCategory.LAPTOP
        else -> AugmentedObjectCategory.UNKNOWN
    }
}

internal fun nativeValueFromCategory(category: AugmentedObjectCategory): Long {
    return when (category) {
        AugmentedObjectCategory.KEYBOARD -> 1L
        AugmentedObjectCategory.MOUSE -> 2L
        AugmentedObjectCategory.LAPTOP -> 3L
        else -> 0L
    }
}
