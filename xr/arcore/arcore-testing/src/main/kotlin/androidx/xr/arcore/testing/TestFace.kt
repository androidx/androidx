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

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimeFace
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.math.Pose

// TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TestFace public constructor() : TestTrackable() {

    override var isVisible: Boolean = true
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    internal val fakeRuntimeTrackable: FakeRuntimeFace =
        FakeRuntimeFace(
            trackingState =
                if (isVisible) {
                    TrackingState.TRACKING
                } else {
                    TrackingState.PAUSED
                }
        )

    public var centerPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.centerPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var mesh: Mesh = Mesh(null, null, null, null)
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.mesh = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var noseTipPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.noseTipPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var foreheadLeftPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.foreheadLeftPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var foreheadRightPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.foreheadRightPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    @SuppressWarnings("RestrictedApiAndroidX")
    internal fun isConfigured() =
        if (isAddedToTestRule) arCoreTestRule.runtime.config.faceTracking == FaceTrackingMode.MESHES
        else false
}
