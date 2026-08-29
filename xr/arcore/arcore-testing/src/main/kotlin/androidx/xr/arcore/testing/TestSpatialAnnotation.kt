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

import androidx.xr.arcore.SpatialAnnotationId
import androidx.xr.arcore.SpatialAnnotationQuadAlignment
import androidx.xr.arcore.runtime.SpatialAnnotationId as RuntimeSpatialAnnotationId
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimeSpatialAnnotation
import androidx.xr.arcore.toRuntimeAlignment
import androidx.xr.arcore.toSpatialAnnotationQuadAlignment
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.SpatialAnnotationTrackingMode
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quad

/**
 * A simulated SpatialAnnotation in the test environment.
 *
 * Modifying the properties on this instance will automatically trigger the underlying perception
 * runtime to tick forward, allowing reactive UI tests to synchronously test spatial layout shifts.
 */
@ExperimentalSpatialAnnotationsApi
public class TestSpatialAnnotation(public val id: SpatialAnnotationId) : TestTrackable() {

    override val fakeRuntimeTrackable: FakeRuntimeSpatialAnnotation =
        FakeRuntimeSpatialAnnotation(id = RuntimeSpatialAnnotationId.fromString(id.toString()))

    override var isVisible: Boolean
        get() = fakeRuntimeTrackable.trackingState == TrackingState.TRACKING
        set(value) {
            fakeRuntimeTrackable.trackingState =
                if (value) TrackingState.TRACKING else TrackingState.PAUSED
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var alignment: SpatialAnnotationQuadAlignment?
        get() = fakeRuntimeTrackable.alignment?.toSpatialAnnotationQuadAlignment()
        set(value) {
            fakeRuntimeTrackable.alignment = value?.toRuntimeAlignment()
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var centerPose: Pose
        get() = fakeRuntimeTrackable.centerPose
        set(value) {
            fakeRuntimeTrackable.centerPose = value
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var quad: Quad?
        get() = fakeRuntimeTrackable.quad
        set(value) {
            fakeRuntimeTrackable.quad = value
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    override fun isTrackableConfigured(): Boolean =
        if (isAddedToTestRule)
            arCoreTestRule.runtime.config.getSpatialAnnotationTracking() !=
                SpatialAnnotationTrackingMode.DISABLED
        else false
}
