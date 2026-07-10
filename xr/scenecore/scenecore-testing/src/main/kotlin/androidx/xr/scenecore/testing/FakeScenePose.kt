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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.impl.BaseScenePose
import androidx.xr.scenecore.testing.internal.FakeScenePose as InternalFakeScenePose

/**
 * A test double for [androidx.xr.scenecore.runtime.ScenePose], designed for use in unit or
 * integration tests.
 *
 * This test double offers greater control compared to the real
 * [androidx.xr.scenecore.runtime.ScenePose] by allowing:
 * * Direct modification of most properties to simulate specific scenarios or states.
 * * Mocking of hit test results for predictable and verifiable interaction testing.
 *
 * @see androidx.xr.scenecore.runtime.ScenePose
 */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeScenePose
internal constructor(internal var fakeInternal: InternalFakeScenePose) : BaseScenePose() {

    public constructor() : this(InternalFakeScenePose())

    /** Returns the pose for this entity, relative to the activity space root. */
    override var activitySpacePose: Pose
        get() = fakeInternal.activitySpacePose
        set(value) {
            fakeInternal.activitySpacePose = value
        }

    /**
     * Returns the scale of this ScenePose. For base ScenePoses, the scale is (1,1,1). For entities
     * this returns the accumulated scale. This value includes the parent's scale, and is similar to
     * a ActivitySpace scale.
     *
     * @return Total [androidx.xr.runtime.math.Vector3] scale applied to self and children.
     */
    override val worldSpaceScale: Vector3
        get() = fakeInternal.worldSpaceScale

    /**
     * Returns the scale in the activity space. This is used by [transformPoseTo] in its
     * calculation.
     */
    override var activitySpaceScale: Vector3
        get() = fakeInternal.activitySpaceScale
        set(value) {
            fakeInternal.activitySpaceScale = value
        }

    /**
     * For test purposes only.
     *
     * The [androidx.xr.scenecore.runtime.HitTestResult] that will be returned by [hitTest]. This
     * can be modified in tests to simulate different hit test outcomes.
     */
    public var hitTestResult: HitTestResult
        get() = fakeInternal.hitTestResult
        set(value) {
            fakeInternal.hitTestResult = value
        }

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult = hitTestResult
}
