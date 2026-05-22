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

import android.util.Range
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.VpsAvailabilityAvailable
import androidx.xr.arcore.VpsAvailabilityErrorInternal
import androidx.xr.arcore.VpsAvailabilityNetworkError
import androidx.xr.arcore.VpsAvailabilityNotAuthorized
import androidx.xr.arcore.VpsAvailabilityResourceExhausted
import androidx.xr.arcore.VpsAvailabilityResult
import androidx.xr.arcore.VpsAvailabilityUnavailable
import androidx.xr.arcore.runtime.Geospatial.State as RuntimeGeospatialState
import androidx.xr.arcore.runtime.VpsAvailabilityResult as RuntimeVpsAvailabilityResult
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimeGeospatial
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion

/**
 * An object which allows for testing the overall condition of [Geospatial] operations in an ARCore
 * unit test environment.
 *
 * @property expectedPose the [Pose] that would be created by
 *   [Geospatial.createPoseFromGeospatialPose]
 * @property expectedGeospatialPose the [GeospatialPose] that would be created by
 *   [Geospatial.createGeospatialPoseFromPose]
 * @property expectedHorizontalAccuracy the horizontal accuracy of the value returned by
 *   [Geospatial.createGeospatialPoseFromPose]
 * @property expectedVerticalAccuracy the vertical accuracy of the value returned by
 *   [Geospatial.createGeospatialPoseFromPose]
 * @property expectedOrientationYawAccuracy the yaw accuracy of the value returned by
 *   [Geospatial.createGeospatialPoseFromPose]
 * @property expectedAnchorPose the [Pose] of the [androidx.xr.arcore.Anchor] that will be returned
 *   by [Geospatial.createAnchor] or [Geospatial.createAnchorOnSurface]
 * @property state the [Geospatial.GeospatialTrackingState] device's Geospatial communication
 * @property expectedVpsResult [VpsAvailabilityResult] that will be returned when checking for VPS
 *   availability if not null
 * @property allowedAnchorLatitudeRange the acceptable range of latitude values that [Geospatial]
 *   can use when creating Anchors, with a default of `-90..90` (inclusive)
 */
public class GeospatialTester internal constructor(private val arCoreTestRule: ArCoreTestRule) {
    private val fakeRuntimeGeospatial: FakeRuntimeGeospatial by lazy {
        arCoreTestRule.runtime.perceptionManager.geospatial as FakeRuntimeGeospatial
    }

    public var expectedPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var expectedGeospatialPose: GeospatialPose =
        GeospatialPose(0.0, 0.0, 0.0, Quaternion.Identity)
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedGeospatialPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var expectedHorizontalAccuracy: Double = 0.0
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedHorizontalAccuracy = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var expectedVerticalAccuracy: Double = 0.0
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedVerticalAccuracy = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var expectedOrientationYawAccuracy: Double = 0.0
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedOrientationYawAccuracy = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var expectedAnchorPose: Pose? = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedAnchorPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var state: Geospatial.GeospatialTrackingState =
        Geospatial.GeospatialTrackingState.NOT_RUNNING
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.state = value.toRuntimeType()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var expectedVpsResult: VpsAvailabilityResult = VpsAvailabilityAvailable()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.expectedVpsAvailabilityResult = value.toRuntimeType()
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var allowedAnchorLatitudeRange: Range<Double> = Range(-90.0, 90.0)
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeGeospatial.allowedAnchorLatitudeRange = value.lower..value.upper
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    private fun isConfigured() = arCoreTestRule.runtime.config.geospatial == GeospatialMode.SPATIAL
}

internal fun Geospatial.GeospatialTrackingState.toRuntimeType(): RuntimeGeospatialState =
    when (this) {
        Geospatial.GeospatialTrackingState.PAUSED -> RuntimeGeospatialState.PAUSED
        Geospatial.GeospatialTrackingState.RUNNING -> RuntimeGeospatialState.RUNNING
        Geospatial.GeospatialTrackingState.NOT_RUNNING -> RuntimeGeospatialState.NOT_RUNNING
        Geospatial.GeospatialTrackingState.ERROR_INTERNAL -> RuntimeGeospatialState.ERROR_INTERNAL
        Geospatial.GeospatialTrackingState.ERROR_NOT_AUTHORIZED ->
            RuntimeGeospatialState.ERROR_NOT_AUTHORIZED
        Geospatial.GeospatialTrackingState.ERROR_RESOURCE_EXHAUSTED ->
            RuntimeGeospatialState.ERROR_RESOURCE_EXHAUSTED
        else ->
            throw IllegalStateException(
                "failed to convert $this to ${RuntimeGeospatialState::class.qualifiedName}"
            )
    }

internal fun VpsAvailabilityResult.toRuntimeType(): RuntimeVpsAvailabilityResult =
    when (this) {
        is VpsAvailabilityAvailable -> androidx.xr.arcore.runtime.VpsAvailabilityAvailable()
        is VpsAvailabilityResourceExhausted ->
            androidx.xr.arcore.runtime.VpsAvailabilityResourceExhausted()
        is VpsAvailabilityErrorInternal -> androidx.xr.arcore.runtime.VpsAvailabilityErrorInternal()
        is VpsAvailabilityNetworkError -> androidx.xr.arcore.runtime.VpsAvailabilityNetworkError()
        is VpsAvailabilityNotAuthorized -> androidx.xr.arcore.runtime.VpsAvailabilityNotAuthorized()
        is VpsAvailabilityUnavailable -> androidx.xr.arcore.runtime.VpsAvailabilityUnavailable()
        else ->
            throw IllegalStateException(
                "failed to convert $this to ${RuntimeVpsAvailabilityResult::class.qualifiedName}"
            )
    }
