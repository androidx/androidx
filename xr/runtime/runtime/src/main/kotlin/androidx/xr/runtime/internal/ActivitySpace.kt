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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.ActivityPose.HitTestFilterValue
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import com.google.common.util.concurrent.ListenableFuture

/**
 * Interface for a SceneCore activity space. There is one activity space and it is the ancestor for
 * all elements in the scene. The activity space does not have a parent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ActivitySpace : SystemSpaceEntity {
    /** Returns the bounds of this ActivitySpace. */
    public val bounds: Dimensions

    /**
     * Adds a listener to be called when the bounds of the primary Activity change. If the same
     * listener is added multiple times, it will only fire each event on time.
     *
     * @param listener The listener to register.
     */
    @Suppress("ExecutorRegistration")
    public fun addOnBoundsChangedListener(listener: OnBoundsChangedListener)

    /**
     * Removes a listener to be called when the bounds of the primary Activity change. If the given
     * listener was not added, this call does nothing.
     *
     * @param listener The listener to unregister.
     */
    @Suppress("ExecutorRegistration")
    public fun removeOnBoundsChangedListener(listener: OnBoundsChangedListener)

    /** Interface for a listener which receives changes to the bounds of the primary Activity. */
    public fun interface OnBoundsChangedListener {
        // Is called by the system when the bounds of the primary Activity change
        /**
         * Called by the system when the bounds of the primary Activity change.
         *
         * @param bounds The new bounds of the primary Activity in Meters
         */
        public fun onBoundsChanged(bounds: Dimensions)
    }

    /**
     * Creates a hit test at the from the specified origin in the specified direction into the
     * scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ActivityPose.
     * @param direction The direction for the hit test ray from the ActivityPose.
     * @param hitTestFilter The scenes that will be in range for the hit test.
     * @param activityPose The ActivityPose to hit test against.
     * @return a {@code ListenableFuture<HitTestResult>}. The HitResult describes if it hit
     *   something and where relative to the given ActivityPose. Listeners will be called on the
     *   main thread if Runnable::run is supplied.
     */
    @Suppress("AsyncSuffixFuture")
    public fun hitTestRelativeToActivityPose(
        origin: Vector3,
        direction: Vector3,
        @HitTestFilterValue hitTestFilter: Int,
        activityPose: ActivityPose,
    ): ListenableFuture<HitTestResult>

    /**
     * A recommended box for content to be placed in when in Full Space Mode.
     *
     * The box is relative to the ActivitySpace's coordinate system. It is not scaled by the
     * ActivitySpace's transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     */
    public val recommendedContentBoxInFullSpace: BoundingBox
}
