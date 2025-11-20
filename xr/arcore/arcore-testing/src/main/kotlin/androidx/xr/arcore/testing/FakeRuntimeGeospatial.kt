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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.Geospatial as RuntimeGeospatial
import androidx.xr.arcore.runtime.Geospatial.GeospatialPoseResult
import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion

/** Test-only implementation of [androidx.xr.arcore.runtime.Geospatial]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeRuntimeGeospatial(
    override var state: RuntimeGeospatial.State = RuntimeGeospatial.State.NOT_RUNNING
) : RuntimeGeospatial {

    /**
     * The next [androidx.xr.arcore.runtime.Geospatial.GeospatialPoseResult] that will be returned
     * by [createGeospatialPoseFromPose]. Is reset after the result is returned.
     */
    public var nextGeospatialPoseResult: GeospatialPoseResult? = null

    /**
     * The next Pose that will be returned by FakeRuntimeGeospatial by
     * [createPoseFromGeospatialPose]. Is reset after the result is returned.
     */
    public var nextPose: Pose? = null

    /**
     * The next Exception that will be thrown by FakeRuntimeGeospatial for any function. Is reset
     * after the exception is thrown.
     */
    public var nextException: Exception? = null

    /**
     * The next Anchor that will be returned by FakeRuntimeGeospatial by [createAnchor]. Is reset
     * after the result is returned.
     */
    public var nextAnchor: Anchor? = null

    /** The VpsAvailabilityResult to be returned by [checkVpsAvailability]. */
    public var nextVpsAvailabilityResult: VpsAvailabilityResult = VpsAvailabilityAvailable()

    /**
     * Returns the supplied Pose.
     *
     * @throws IllegalStateException if no Pose is set.
     */
    override public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose {
        maybeThrowException()

        val toReturn = checkNotNull(nextPose) { "No pose result set." }
        nextPose = null
        return toReturn
    }

    /**
     * Returns the supplied [androidx.xr.arcore.runtime.Geospatial.GeospatialPoseResult].
     *
     * @throws IllegalStateException if no
     *   [androidx.xr.arcore.runtime.Geospatial.GeospatialPoseResult] is set.
     */
    override public fun createGeospatialPoseFromPose(pose: Pose): GeospatialPoseResult {
        maybeThrowException()

        val toReturn = checkNotNull(nextGeospatialPoseResult) { "No geospatial pose result set." }
        nextGeospatialPoseResult = null
        return toReturn
    }

    /**
     * Returns the supplied anchor.
     *
     * @throws IllegalStateException if no anchor is set.
     */
    override public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): Anchor {
        maybeThrowException()

        val toReturn = checkNotNull(nextAnchor) { "No anchor set." }
        nextAnchor = null
        return toReturn
    }

    private fun maybeThrowException() {
        nextException?.let { toThrow ->
            nextException = null
            throw toThrow
        }
    }

    /**
     * Returns the supplied anchor.
     *
     * @throws IllegalStateException if no anchor is set.
     */
    override public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: RuntimeGeospatial.Surface,
    ): Anchor {
        maybeThrowException()

        val toReturn = checkNotNull(nextAnchor) { "No anchor set." }
        nextAnchor = null
        return toReturn
    }

    override public suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult {
        maybeThrowException()
        return nextVpsAvailabilityResult
    }
}
