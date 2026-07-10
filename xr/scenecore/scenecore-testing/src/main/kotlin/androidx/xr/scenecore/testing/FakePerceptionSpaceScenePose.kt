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
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.testing.internal.FakePerceptionSpaceScenePose as InternalFakePerceptionSpaceScenePose

/** A fake ScenePose representing a perception space. */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakePerceptionSpaceScenePose
internal constructor(fakeInternal: InternalFakePerceptionSpaceScenePose) :
    FakeScenePose(fakeInternal), PerceptionSpaceScenePose {

    public constructor(pose: Pose = Pose()) : this(InternalFakePerceptionSpaceScenePose(pose))

    internal val pose: Pose
        get() = (fakeInternal as InternalFakePerceptionSpaceScenePose).pose

    override var activitySpacePose: Pose
        get() = fakeInternal.activitySpacePose
        set(value) {
            fakeInternal.activitySpacePose = value
        }

    override var activitySpaceScale: Vector3
        get() = fakeInternal.activitySpaceScale
        set(value) {
            fakeInternal.activitySpaceScale = value
        }
}
