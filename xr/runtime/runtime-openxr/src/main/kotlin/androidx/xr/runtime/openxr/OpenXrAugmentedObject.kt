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

package androidx.xr.runtime.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.AugmentedObject as RuntimeObject
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrAugmentedObject
internal constructor(
    internal val objectId: Long,
    internal val timeSource: OpenXrTimeSource,
    internal val xrResources: XrResources,
) : RuntimeObject, Updatable {
    override var category: AugmentedObjectCategory = AugmentedObjectCategory.UNKNOWN
        private set

    override var centerPose: Pose = Pose()
        private set

    override var extents: Vector3 = Vector3.Zero
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override fun createAnchor(pose: Pose): Anchor {
        val xrTime = timeSource.getXrTime(timeSource.markNow())
        val anchorNativePointer = nativeCreateAnchorForObject(objectId, pose, xrTime)
        checkNativeAnchorIsValid(anchorNativePointer)
        val anchor: Anchor = OpenXrAnchor(anchorNativePointer, xrResources)
        xrResources.addUpdatable(anchor as Updatable)
        return anchor
    }

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

    private fun checkNativeAnchorIsValid(nativeAnchor: Long) {
        when (nativeAnchor) {
            -2L -> throw IllegalStateException("Failed to create anchor.") // kErrorRuntimeFailure
            -10L -> throw AnchorResourcesExhaustedException() // kErrorLimitReached
        }
    }

    private external fun nativeGetAugmentedObjectState(
        objectId: Long,
        timestampNs: Long,
    ): AugmentedObjectState?

    private external fun nativeCreateAnchorForObject(
        objectId: Long,
        pose: Pose,
        timestampNs: Long,
    ): Long
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
