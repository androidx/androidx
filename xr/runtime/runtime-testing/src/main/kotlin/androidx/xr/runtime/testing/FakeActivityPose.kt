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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.ActivityPose
import androidx.xr.runtime.internal.HitTestResult
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture

/**
 * A test double for [ActivityPose], designed for use in unit or integration tests.
 *
 * This test double offers greater control compared to the real [ActivityPose] by allowing:
 * * Direct modification of most properties to simulate specific scenarios or states.
 * * Mocking of hit test results for predictable and verifiable interaction testing.
 *
 * @see ActivityPose
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeActivityPose : ActivityPose {
    /** Returns the pose for this entity, relative to the activity space root. */
    override val activitySpacePose: Pose = Pose.Identity

    /**
     * Returns the scale of this ActivityPose. For base ActivityPoses, the scale is (1,1,1). For
     * entities this returns the accumulated scale. This value includes the parent's scale, and is
     * similar to a ActivitySpace scale.
     *
     * @return Total [Vector3] scale applied to self and children.
     */
    override val worldSpaceScale: Vector3 = Vector3.One

    /**
     * Returns the scale in the activity space. This is used by [transformPoseTo] in its
     * calculation.
     */
    override val activitySpaceScale: Vector3 = Vector3.One

    /**
     * Returns a pose relative to this entity transformed into a pose relative to the destination.
     *
     * @param pose A pose in this entity's local coordinate space.
     * @param destination The entity which the returned pose will be relative to.
     * @return The pose relative to the destination entity.
     */
    override fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose {
        return pose
    }

    /**
     * For test purposes only.
     *
     * The [HitTestResult] that will be returned by [hitTest]. This can be modified in tests to
     * simulate different hit test outcomes.
     */
    public var hitTestResult: HitTestResult =
        HitTestResult(
            null,
            null,
            HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN,
            0f,
        )

    @Suppress("AsyncSuffixFuture")
    override fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ActivityPose.HitTestFilterValue hitTestFilter: Int,
    ): ListenableFuture<HitTestResult> = immediateFuture(hitTestResult)
}
