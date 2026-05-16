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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.testing.internal.FakeActivitySpace as InternalFakeActivitySpace

/**
 * A test double for [androidx.xr.scenecore.runtime.ActivitySpace], designed for use in unit or
 * integration tests.
 */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeActivitySpace
internal constructor(private val internalActivitySpace: InternalFakeActivitySpace) :
    FakeSystemSpaceEntity(internalActivitySpace), ActivitySpace {

    public constructor() : this(InternalFakeActivitySpace())

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated(
        "unscaledGravityAlignedActivitySpace flag deprecated, scheduled for removal in future release."
    )
    public constructor(unscaledGravityAlignedActivitySpace: Boolean) : this()

    /** Returns the bounds of this ActivitySpace. */
    override val bounds: Dimensions
        get() = internalActivitySpace.bounds

    /** Returns the pose for this ActivitySpace, relative to the given space. */
    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return internalActivitySpace.getPose(relativeTo)
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        internalActivitySpace.setPose(pose, relativeTo)
    }

    /**
     * For test purposes only.
     *
     * The set of listeners to be called when the bounds of the primary Activity change.
     */
    public val onBoundsChangedListeners: Set<ActivitySpace.OnBoundsChangedListener>
        get() = internalActivitySpace.onBoundsChangedListeners

    /**
     * For test purposes only.
     *
     * Simulates a bounds change event, invoking all registered listeners with the new bounds. This
     * will also update the internal `bounds` property of this fake.
     *
     * @param bounds The new bounds to propagate to the listeners.
     */
    public fun onBoundsChanged(bounds: Dimensions) {
        internalActivitySpace.onBoundsChanged(bounds)
    }

    /**
     * Adds a listener to be called when the bounds of the primary Activity change. If the same
     * listener is added multiple times, it will only fire each event on time.
     *
     * @param listener The listener to register.
     */
    @Suppress("ExecutorRegistration")
    override fun addOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        internalActivitySpace.addOnBoundsChangedListener(listener)
    }

    /**
     * Removes a listener to be called when the bounds of the primary Activity change. If the given
     * listener was not added, this call does nothing.
     *
     * @param listener The listener to unregister.
     */
    override fun removeOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        internalActivitySpace.removeOnBoundsChangedListener(listener)
    }

    /**
     * The [androidx.xr.scenecore.runtime.HitTestResult] that will be returned by
     * [hitTestRelativeToActivityPose]. This can be modified in tests to simulate different hit test
     * outcomes.
     */
    public var activitySpaceHitTestResult: HitTestResult
        get() = internalActivitySpace.activitySpaceHitTestResult
        set(value) {
            internalActivitySpace.activitySpaceHitTestResult = value
        }

    override suspend fun hitTestRelativeToActivityPose(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
        scenePose: ScenePose,
    ): HitTestResult =
        internalActivitySpace.hitTestRelativeToActivityPose(
            origin,
            direction,
            hitTestFilter,
            scenePose,
        )

    override val recommendedContentBoxInFullSpace: BoundingBox
        get() = internalActivitySpace.recommendedContentBoxInFullSpace
}
