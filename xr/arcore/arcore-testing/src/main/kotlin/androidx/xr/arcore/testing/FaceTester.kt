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

import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimeFace
import androidx.xr.runtime.FaceTrackingMode

/**
 * An object which allows for testing a simulation of the user's face in an ARCore unit test
 * environment. If [isValid] is `false`, the [blendShapeValues] will be ignored and tracking will be
 * [TrackingState.PAUSED].
 *
 * @property isValid whether the [blendShapeValues] are valid
 * @property blendShapeValues a list of normalized blend shape values of facial features
 * @property confidenceValues a list of normalized values describing the confidence of blend shape
 *   values in a specific region of the face
 */
public class FaceTester
internal constructor(
    private val arCoreTestRule: ArCoreTestRule,
    private val fakeRuntimeFace: FakeRuntimeFace,
) {
    public var isValid: Boolean = true
        set(value) {
            field = value
            if (arCoreTestRule.runtime.config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
                fakeRuntimeFace.isValid = value
                fakeRuntimeFace.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var blendShapeValues: List<Float> = listOf()
        set(value) {
            field = value
            if (arCoreTestRule.runtime.config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
                if (value.isNotEmpty() && value.all { it in 0.0f..1.0f }) {
                    fakeRuntimeFace.blendShapeValues = value.toFloatArray()
                }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var confidenceValues: List<Float> = listOf()
        set(value) {
            field = value
            if (arCoreTestRule.runtime.config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
                if (value.isNotEmpty() && value.all { it in 0.0f..1.0f }) {
                    fakeRuntimeFace.confidenceValues = value.toFloatArray()
                }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }
}
