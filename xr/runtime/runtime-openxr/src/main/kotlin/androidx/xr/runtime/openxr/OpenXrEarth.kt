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

package androidx.xr.runtime.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.Earth
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion

/** Currently unimplemented implementation of [Earth] on OpenXR. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrEarth internal constructor(private val xrResources: XrResources) :
    Earth, Updatable {

    public override var state: Earth.State = Earth.State.STOPPED
        private set

    override public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose {
        throw NotImplementedError("Not implemented yet.")
    }

    override public fun createGeospatialPoseFromPose(pose: Pose): Earth.GeospatialPoseResult {
        throw NotImplementedError("Not implemented yet.")
    }

    override public fun createGeospatialPoseFromDevicePose(): Earth.GeospatialPoseResult {
        throw NotImplementedError("Not implemented yet.")
    }

    override public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): Anchor {
        throw NotImplementedError("Not implemented yet.")
    }

    override public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Earth.Surface,
    ): Anchor {
        throw NotImplementedError("Not implemented yet.")
    }

    override fun update(xrTime: Long) {}
}
