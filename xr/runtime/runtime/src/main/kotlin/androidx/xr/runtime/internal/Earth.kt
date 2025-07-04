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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion

/** Describes the Earth interface for Geospatial localization and tracking. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Earth {

    /**
     * Describes the state of the [Earth]. The State must be Running to use the Earth functionality.
     * If the [Earth] has entered an error state other than [ERROR_APP_PREEMPTED], Geospatial must
     * be re-enabled to use the Earth again.
     */
    public class State private constructor(private val value: Int) {
        public companion object {
            /**
             * The [Earth] is enabled and has not encountered an error. Functions to create anchors
             * or convert poses may still fail if the Earth is not tracking.
             */
            @JvmField public val RUNNING: State = State(1)

            /** The [Earth] is stopped. The Geospatial config must be enabled to use the Earth. */
            @JvmField public val STOPPED: State = State(0)

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
             *   to memory pressure). </ul>
             */
            @JvmField public val ERROR_NOT_AUTHORIZED: State = State(-2)

            /**
             * The application has exhausted the quota allotted to the given Google Cloud project.
             * The developer should
             * [request additional quota](https://cloud.google.com/docs/quota#requesting_higher_quota)
             * for the ARCore API for their project from the Google Cloud Console.
             */
            @JvmField public val ERROR_RESOURCES_EXHAUSTED: State = State(-3)

            /**
             * The APK is older than the current supported version. This error is only possible on
             * an ARCore runtime.
             */
            @JvmField public val ERROR_APK_VERSION_TOO_OLD: State = State(-4)

            /**
             * The app is no longer in full-space mode and has been disconnected from the Geospatial
             * Session. This is only possible on an OpenXR runtime.
             */
            @JvmField public val ERROR_APP_PREEMPTED: State = State(-5)
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

    /** The current state of the Earth. */
    public val state: State

    /** Converts the input [GeospatialPose] to a [Pose] in the same position. */
    public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose

    /** Converts the input [Pose] to a [GeospatialPose] in the same position. */
    public fun createGeospatialPoseFromPose(pose: Pose): GeospatialPoseResult

    /** Returns the [GeospatialPose] for the latest device pose. */
    public fun createGeospatialPoseFromDevicePose(): GeospatialPoseResult

    /**
     * Creates an anchor at the specified geospatial location and orientation relative to the Earth.
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
     */
    public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Surface,
    ): Anchor
}
