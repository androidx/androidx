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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion

/**
 * Describes the interface for Geospatial localization and tracking.
 *
 * @property state the current [State] of Geospatial
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Geospatial {

    /**
     * Describes the state of Geospatial. The State must be [RUNNING] to use Geospatial
     * functionality. If Geospatial has entered an error state other than [ERROR_APP_PREEMPTED],
     * Geospatial must be disabled and re-enabled to use Geospatial again.
     */
    public class State private constructor(private val value: Int) {
        public companion object {
            /**
             * Geospatial is running and has not encountered an error. Functions to create anchors
             * or convert poses may still fail if Geospatial is not tracking.
             */
            @JvmField public val RUNNING: State = State(1)

            /**
             * Geospatial is not running. The Geospatial config must be enabled to use the
             * Geospatial APIs. After enablement, Geospatial will not immediately enter the RUNNING
             * state.
             */
            @JvmField public val NOT_RUNNING: State = State(0)

            /**
             * Earth localization has encountered an internal error. The app should not attempt to
             * recover from this error. Please see the Android logs for additional information.
             */
            @JvmField public val ERROR_INTERNAL: State = State(-1)

            /**
             * The authorization provided by the application is not valid.
             * - The associated Google Cloud project may not have enabled the ARCore API.
             * - When using API key authentication, this will happen if the API key in the manifest
             *   is invalid or unauthorized. It may also fail if the API key is restricted to a set
             *   of apps not including the current one.
             * - When using keyless authentication, this may happen when no OAuth client has been
             *   created, or when the signing key and package name combination does not match the
             *   values used in the Google Cloud project. It may also fail if Google Play Services
             *   isn't installed, is too old, or is malfunctioning for some reason (e.g. killed due
             *   to memory pressure).
             */
            @JvmField public val ERROR_NOT_AUTHORIZED: State = State(-2)

            /**
             * The application has hit the rate limit for created Geospatial Sessions. The developer
             * should
             * [request additional quota](https://cloud.google.com/docs/quota#requesting_higher_quota)
             * for the ARCore API for their project from the Google Cloud Console.
             *
             * Sessions are limited per-minute [link TBD] and enabling may succeed if retried. The
             * application can disable and re-enable Geospatial to try again.
             */
            @JvmField public val ERROR_RESOURCE_EXHAUSTED: State = State(-3)

            /**
             * The geospatial connection has been paused. The connection may resume, and does not
             * require action from the app. Tracked entities will enter the STOPPED state and must
             * be destroyed.
             */
            @JvmField public val PAUSED: State = State(2)
        }
    }

    /** The type of surface on which to create an anchor. */
    public class Surface private constructor(private val value: Int) {
        public companion object {
            /** The terrain surface. */
            @JvmField public val TERRAIN: Surface = Surface(0)
            /** The rooftop surface. */
            @JvmField public val ROOFTOP: Surface = Surface(1)
        }
    }

    /**
     * @property geospatialPose the [GeospatialPose] that was created
     * @property horizontalAccuracy the estimated horizontal accuracy in meters
     * @property verticalAccuracy the estimated altitude accuracy in meters
     * @property orientationYawAccuracy the estimated orientation yaw angle accuracy
     */
    public class GeospatialPoseResult(
        public val geospatialPose: GeospatialPose,
        public val horizontalAccuracy: Double,
        public val verticalAccuracy: Double,
        public val orientationYawAccuracy: Double,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GeospatialPoseResult) return false

            return this.geospatialPose == other.geospatialPose &&
                this.horizontalAccuracy == other.horizontalAccuracy &&
                this.verticalAccuracy == other.verticalAccuracy &&
                this.orientationYawAccuracy == other.orientationYawAccuracy
        }

        override fun hashCode(): Int {
            var result = geospatialPose.hashCode()
            result = 31 * result + horizontalAccuracy.hashCode()
            result = 31 * result + verticalAccuracy.hashCode()
            result = 31 * result + orientationYawAccuracy.hashCode()
            return result
        }
    }

    public val state: State

    /**
     * Converts the input [GeospatialPose] to a [Pose] in the same position.
     *
     * @param geospatialPose the [GeospatialPose] to convert
     * @return the converted [Pose]
     */
    public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose

    /**
     * Converts the input [Pose] to a [GeospatialPose] in the same position.
     *
     * @param pose the [Pose] to convert
     * @return the converted [GeospatialPoseResult]
     */
    public fun createGeospatialPoseFromPose(pose: Pose): GeospatialPoseResult

    /**
     * Creates an anchor at the specified geospatial location and orientation relative to
     * Geospatial.
     *
     * @param latitude the latitude of the anchor
     * @param longitude the longitude of the anchor
     * @param altitude the altitude of the anchor
     * @param eastUpSouthQuaternion the rotation of the anchor
     * @return the created [Anchor]
     */
    public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): Anchor

    /**
     * Creates an anchor at a specified geospatial location and altitude relative to the horizontal
     * position's surface (Terrain or Rooftop).
     *
     * @param latitude the latitude of the anchor
     * @param longitude the longitude of the anchor
     * @param altitudeAboveSurface the altitude of the anchor above the surface
     * @param eastUpSouthQuaternion the rotation of the anchor
     * @param surface the [Surface] to create the anchor on
     * @return the created [Anchor]
     */
    public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Surface,
    ): Anchor

    /**
     * Gets the availability of the Visual Positioning System (VPS) at a specified horizontal
     * position.
     *
     * The availability of VPS in a given location helps to improve the quality of Geospatial
     * localization and tracking accuracy.
     *
     * @param latitude the latitude to check
     * @param longitude the longitude to check
     * @return the [VpsAvailabilityResult]
     */
    public suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult
}
