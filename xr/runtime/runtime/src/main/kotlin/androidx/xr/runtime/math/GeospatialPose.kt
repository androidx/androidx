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

package androidx.xr.runtime.math

import androidx.annotation.RestrictTo

/**
 * Describes a specific location, elevation, and orientation relative to Earth. It is comprised of:
 * - Latitude and longitude, specified in degrees, with positive values being north of the equator
 *   and east of the prime meridian as defined by the
 *   [WGS84 specification](https://en.wikipedia.org/wiki/World_Geodetic_System).
 * - Altitude is specified in meters above the WGS84 ellipsoid, which is roughly equivalent to
 *   meters above sea level.
 * - Orientation approximates the direction the user is facing in the EUS coordinate system. The EUS
 *   coordinate system has X+ pointing east, Y+ pointing up, and Z+ pointing south.
 *
 * @property latitude The latitude of the GeospatialPose in degrees.
 * @property longitude The longitude of the GeospatialPose in degrees.
 * @property altitude The altitude of the GeospatialPose in meters.
 * @property eastUpSouthQuaternion The orientation of the GeospatialPose in the EUS coordinate
 *   system.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class GeospatialPose
constructor(
    public val latitude: Double = 0.0,
    public val longitude: Double = 0.0,
    public val altitude: Double = 0.0,
    public val eastUpSouthQuaternion: Quaternion = Quaternion(),
) {
    /** Returns true if this GeospatialPose is equal to the [other]. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeospatialPose) return false

        return this.latitude == other.latitude &&
            this.longitude == other.longitude &&
            this.altitude == other.altitude &&
            this.eastUpSouthQuaternion == other.eastUpSouthQuaternion
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + altitude.hashCode()
        result = 31 * result + eastUpSouthQuaternion.hashCode()
        return result
    }

    override fun toString(): String =
        "GeospatialPose{\n\tLatitude=$latitude\n\tLongitude=$longitude\n\tAltitude=$altitude\n\tEastUpSouthQuaternion=$eastUpSouthQuaternion\n}"
}
