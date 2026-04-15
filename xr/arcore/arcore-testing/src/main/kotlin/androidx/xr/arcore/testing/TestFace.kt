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

/**
 * Represents a human face as observed by the runtime within a test environment. If [isValid] is
 * `false`, the [blendShapeValues] will be ignored and tracking will be [TrackingState.PAUSED].
 *
 * @property isValid whether the [blendShapeValues] are valid
 * @property blendShapeValues a list of normalized blend shape values of facial features
 * @property confidenceValues a list of normalized values describing the confidence of blend shape
 *   values in a specific region of the face
 */
public class TestFace public constructor() : TestTrackable() {

    internal constructor(testRule: ArCoreTestRule) : this() {
        isUserFace = true
        arCoreTestRule = testRule
    }

    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    override var isVisible: Boolean = true
        set(value) {
            field = value
            if (isConfiguredForMeshing()) {
                fakeRuntimeTrackable.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    private var isUserFace: Boolean = false

    public var isValid: Boolean = true
        set(value) {
            field = value
            if (isConfiguredForBlendShapes()) {
                fakeRuntimeTrackable.isValid = value
                fakeRuntimeTrackable.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    internal val fakeRuntimeTrackable: FakeRuntimeFace by lazy {
        if (isUserFace) {
            arCoreTestRule.runtime.perceptionManager.userFace as FakeRuntimeFace
        } else {
            FakeRuntimeFace(
                trackingState =
                    if (isVisible || isValid) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            )
        }
    }

    public var blendShapeValues: List<Float> = listOf()
        set(value) {
            field = value
            if (isConfiguredForBlendShapes() && value.isNotEmpty() && value.all { it in 0f..1f }) {
                fakeRuntimeTrackable.blendShapeValues = value.toFloatArray()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var confidenceValues: List<Float> = listOf()
        set(value) {
            field = value
            if (isConfiguredForBlendShapes() && value.isNotEmpty() && value.all { it in 0f..1f }) {
                fakeRuntimeTrackable.confidenceValues = value.toFloatArray()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    public var centerPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfiguredForMeshing()) {
                fakeRuntimeTrackable.centerPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    public var mesh: Mesh = Mesh(null, null, null, null)
        set(value) {
            field = value
            if (isConfiguredForMeshing()) {
                fakeRuntimeTrackable.mesh = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    public var noseTipPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfiguredForMeshing()) {
                fakeRuntimeTrackable.noseTipPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    public var foreheadLeftPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfiguredForMeshing()) {
                fakeRuntimeTrackable.foreheadLeftPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    // TODO b/452680433: Unrestrict when the ArCore Face meshing APIs are unrestricted
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    public var foreheadRightPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfiguredForMeshing()) {
                fakeRuntimeTrackable.foreheadRightPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    internal fun isConfiguredForMeshing() =
        if (isAddedToTestRule) arCoreTestRule.runtime.config.faceTracking == FaceTrackingMode.MESHES
        else false

    internal fun isConfiguredForBlendShapes() =
        if (isAddedToTestRule)
            arCoreTestRule.runtime.config.faceTracking == FaceTrackingMode.BLEND_SHAPES
        else false
}
