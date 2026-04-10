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

package androidx.xr.arcore.testing.internal

import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorNotAuthorizedException
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.AnchorUnsupportedLocationException
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.arcore.runtime.Geospatial.GeospatialPoseResult
import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.arcore.runtime.VpsAvailabilityAvailable
import androidx.xr.arcore.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion

internal class FakeRuntimeGeospatial(
    override var state: Geospatial.State = Geospatial.State.NOT_RUNNING
) : Geospatial {

    var expectedPose: Pose = Pose()
    var expectedGeospatialPose: GeospatialPose = GeospatialPose(0.0, 0.0, 0.0, Quaternion.Identity)
    var expectedHorizontalAccuracy: Double = 0.0
    var expectedVerticalAccuracy: Double = 0.0
    var expectedOrientationYawAccuracy: Double = 0.0
    var expectedAnchorPose: Pose? = Pose()
    var expectedVpsAvailabilityResult: VpsAvailabilityResult = VpsAvailabilityAvailable()

    internal var allowedAnchorLatitudeRange = -90.0..90.0

    override fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose {
        if (state == Geospatial.State.RUNNING) {
            return expectedPose
        }
        throw GeospatialPoseNotTrackingException()
    }

    override fun createGeospatialPoseFromPose(pose: Pose): GeospatialPoseResult {
        if (state == Geospatial.State.RUNNING) {
            return GeospatialPoseResult(
                expectedGeospatialPose,
                expectedHorizontalAccuracy,
                expectedVerticalAccuracy,
                expectedOrientationYawAccuracy,
            )
        }
        throw GeospatialPoseNotTrackingException()
    }

    override fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): FakeRuntimeAnchor {
        require(latitude in allowedAnchorLatitudeRange)
        if (state == Geospatial.State.ERROR_NOT_AUTHORIZED) {
            throw AnchorNotAuthorizedException()
        } else if (state == Geospatial.State.ERROR_RESOURCE_EXHAUSTED) {
            throw AnchorResourcesExhaustedException()
        } else if (state != Geospatial.State.RUNNING) {
            throw AnchorNotTrackingException()
        }
        check(expectedAnchorPose != null)
        return FakeRuntimeAnchor(expectedAnchorPose!!)
    }

    override suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Geospatial.Surface,
    ): Anchor {
        require(latitude in allowedAnchorLatitudeRange)
        if (altitudeAboveSurface < 0) {
            throw AnchorUnsupportedLocationException(
                cause =
                    IllegalArgumentException(
                        "altitudeAboveSurface < 0: can't create anchor below surface"
                    )
            )
        }
        if (state == Geospatial.State.ERROR_NOT_AUTHORIZED) {
            throw AnchorNotAuthorizedException()
        } else if (state == Geospatial.State.ERROR_RESOURCE_EXHAUSTED) {
            throw AnchorResourcesExhaustedException()
        } else if (state != Geospatial.State.RUNNING) {
            throw AnchorNotTrackingException()
        }
        check(expectedAnchorPose != null)
        return FakeRuntimeAnchor(expectedAnchorPose!!)
    }

    override suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult {
        check(state == Geospatial.State.RUNNING)
        return expectedVpsAvailabilityResult
    }
}
