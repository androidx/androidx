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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.AnchorNotAuthorizedException
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.AnchorUnsupportedLocationException
import androidx.xr.runtime.internal.Earth as RuntimeEarth
import androidx.xr.runtime.internal.GeospatialPoseNotTrackingException
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides localization ability in Earth-relative coordinates.
 *
 * To use the Earth object, configure the session with [Config.GeospatialMode.Enabled].
 *
 * Not all devices support [Config.GeospatialMode.Enabled], use [isGeospatialModeSupported] to check
 * if the current device and selected camera support enabling this mode.
 *
 * The Earth object should only be used when its [EarthState] is [EarthState.Running], and otherwise
 * should not be used. Use [Earth.state.earthState] to obtain the current [EarthState]. If the
 * [EarthState] does not become [EarthState.Running], then [Earth.State.earthErrorState] may contain
 * more information in a [EarthErrorState].
 *
 * Use [Earth.createGeospatialPoseFromDevicePose] to obtain the Earth-relative virtual camera pose
 * for the latest frame.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Earth
internal constructor(
    private val runtimeEarth: RuntimeEarth,
    private val xrResourcesManager: XrResourcesManager,
) : Updatable {
    public companion object {
        /**
         * Returns the Earth object for the given [Session].
         *
         * @param session the [Session] to get the [Earth] object from.
         */
        @JvmStatic
        public fun getInstance(session: Session): Earth {
            val perceptionStateExtender =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            return perceptionStateExtender.xrResourcesManager.earth
        }
    }

    /**
     * Describes the state of the [Earth]. The State must be [RUNNING] to use the Earth
     * functionality. If the [Earth] has entered an error state other than [ERROR_APP_PREEMPTED],
     * Geospatial must be re-enabled to use the Earth again.
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

    private val _state = MutableStateFlow(State.STOPPED)
    /** The current [State] of the [Earth]. */
    public val state: StateFlow<Earth.State> = _state.asStateFlow()

    /**
     * Converts the input geospatial location and orientation relative to the Earth to a [Pose] in
     * the same position.
     *
     * This method may return a [PoseNotTracking] result if the Earth is not currently tracking.
     *
     * Positions near the north pole or south pole is not supported. If the latitude is within 0.1
     * degrees of the north pole or south pole (90 degrees or -90 degrees), this function will throw
     * an [IllegalArgumentException].
     *
     * @param geospatialPose the [GeospatialPose] to be converted into a [Pose].
     * @throws [IllegalArgumentException] if the latitude is within 0.1 degrees of the north pole or
     *   south pole (90 degrees or -90 degrees).
     */
    public fun createPoseFromGeospatialPose(
        geospatialPose: GeospatialPose
    ): CreatePoseFromGeospatialPoseResult {
        return try {
            CreatePoseFromGeospatialPoseSuccess(
                runtimeEarth.createPoseFromGeospatialPose(geospatialPose)
            )
        } catch (e: GeospatialPoseNotTrackingException) {
            CreatePoseFromGeospatialPoseNotTracking()
        } catch (e: IllegalStateException) {
            CreatePoseFromGeospatialPoseIllegalState()
        }
    }

    /**
     * Converts the input [Pose] to a [GeospatialPose] in the same position as the original pose.
     *
     * This method may return a [GeospatialPoseNotTracking] result if the Earth is not currently
     * tracking.
     *
     * @param pose the [Pose] to be converted into a [GeospatialPose].
     */
    public fun createGeospatialPoseFromPose(pose: Pose): CreateGeospatialPoseFromPoseResult {
        return try {
            val runtimeResult = runtimeEarth.createGeospatialPoseFromPose(pose)
            CreateGeospatialPoseFromPoseSuccess(
                runtimeResult.geospatialPose,
                runtimeResult.horizontalAccuracy,
                runtimeResult.verticalAccuracy,
                runtimeResult.orientationYawAccuracy,
            )
        } catch (e: GeospatialPoseNotTrackingException) {
            CreateGeospatialPoseFromPoseNotTracking()
        } catch (e: IllegalStateException) {
            CreateGeospatialPoseFromPoseIllegalState()
        }
    }

    /**
     * Returns the [GeospatialPose] for the latest device pose, describing the geospatial
     * positioning of the device.
     *
     * The orientation of the obtained [GeospatialPose] approximates the direction the user is
     * facing in the EUS coordinate system. The EUS coordinate system has X+ pointing east, Y+
     * pointing up, and Z+ pointing south.
     *
     * Note: This method may return a [GeospatialPoseNotTracking] result if the Earth is not
     * currently tracking.
     */
    public fun createGeospatialPoseFromDevicePose(): CreateGeospatialPoseFromPoseResult {
        return try {
            val runtimeResult = runtimeEarth.createGeospatialPoseFromDevicePose()
            CreateGeospatialPoseFromPoseSuccess(
                runtimeResult.geospatialPose,
                runtimeResult.horizontalAccuracy,
                runtimeResult.verticalAccuracy,
                runtimeResult.orientationYawAccuracy,
            )
        } catch (e: GeospatialPoseNotTrackingException) {
            CreateGeospatialPoseFromPoseNotTracking()
        } catch (e: IllegalStateException) {
            CreateGeospatialPoseFromPoseIllegalState()
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
     * Creates a new [Anchor] at the specified geospatial location and orientation relative to the
     * Earth.
     *
     * Latitude and longitude are defined by the
     * [WGS84 specification](https://en.wikipedia.org/wiki/World_Geodetic_System), and the altitude
     * value is defined by the elevation above the WGS84 ellipsoid in meters. To create an anchor
     * using an altitude relative to the Earth's terrain instead of altitude above the WGS84
     * ellipsoid, use [Earth.createAnchorOnTerrain].
     *
     * The rotation quaternion provided is with respect to an east-up-south coordinate frame. An
     * identity rotation will have the anchor oriented such that X+ points to the east, Y+ points up
     * away from the center of the earth, and Z+ points to the south.
     *
     * The tracking state of an [Anchor] will permanently become [TrackingState.Stopped] if the
     * [Config.GeospatialMode] is disabled, or if another full-space app uses Geospatial.
     *
     * Creating anchors near the north pole or south pole is not supported. If the latitude is
     * within 0.1 degrees of the north pole or south pole (90 degrees or -90 degrees), this function
     * will throw [IllegalArgumentException].
     *
     * @param latitude the latitude of the anchor.
     * @param longitude the longitude of the anchor.
     * @param altitude the altitude of the anchor.
     * @param eastUpSouthQuaternion the rotation quaternion of the anchor.
     * @throws [IllegalArgumentException] if the latitude is outside the allowable range.
     */
    public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): AnchorCreateResult {
        return try {
            val runtimeAnchor =
                runtimeEarth.createAnchor(latitude, longitude, altitude, eastUpSouthQuaternion)
            val anchor = Anchor(runtimeAnchor, xrResourcesManager)
            xrResourcesManager.addUpdatable(anchor)
            AnchorCreateSuccess(anchor)
        } catch (e: AnchorResourcesExhaustedException) {
            AnchorCreateResourcesExhausted()
        } catch (e: IllegalStateException) {
            AnchorCreateIllegalState()
        }
    }

    /**
     * Asynchronously creates a new [Anchor] at a specified horizontal position and altitude
     * relative to the horizontal position's surface (Terrain or Rooftop).
     *
     * The specified [altitudeAboveSurface] is interpreted to be relative to the given surface at
     * the specified latitude/longitude geospatial coordinates, rather than relative to the WGS84
     * ellipsoid. Specifying an altitude of 0 will position the anchor directly on the surface
     * whereas specifying a positive altitude will position the anchor above the surface, against
     * the direction of gravity.
     *
     * [Surface.TERRAIN] refers to the Earth's terrain (or floor) and [Surface.ROOFTOP] refers to
     * the top of a building at the given horizontal location. If there is no building at the given
     * location, then the rooftop surface is interpreted to be the terrain instead.
     *
     * You may resolve multiple anchors at a time, but a session cannot be tracking more than 100
     * surface anchors at time. Attempting to resolve more than 100 surface anchors will return an
     * [AnchorCreateResourcesExhausted] result.
     *
     * Creating a Terrain anchor requires an active Earth which is [EarthState.Running]. If it is
     * not, then this function returns an [AnchorCreateIllegalState] result. This call also requires
     * a working internet connection to communicate with the ARCore API on Google Cloud. ARCore will
     * continue to retry if it is unable to establish a connection to the ARCore service.
     *
     * A Terrain anchor's tracking state will be [TrackingState.Paused] if the Earth is not actively
     * tracking. Its tracking state will permanently become [TrackingState.Stopped] if
     * [Config.GeospatialMode] is disabled, or if another full-space app uses Geospatial.
     *
     * Latitude and longitude are defined by the
     * [WGS84 specification](https://en.wikipedia.org/wiki/World_Geodetic_System), and the altitude
     * value is defined by the elevation above the Earth's terrain (or floor) in meters.
     *
     * The rotation quaternion provided is with respect to an east-up-south coordinate frame. An
     * identity rotation will have the anchor oriented such that X+ points to the east, Y+ points up
     * away from the center of the earth, and Z+ points to the south.
     *
     * @param latitude the latitude of the anchor.
     * @param longitude the longitude of the anchor.
     * @param altitudeAboveSurface The altitude of the anchor above the given surface.
     * @param eastUpSouthQuaternion the rotation quaternion of the anchor.
     * @throws IllegalArgumentException if the latitude is outside the allowable range.
     */
    public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Surface,
    ): AnchorCreateResult {
        return try {
            val runtimeAnchor =
                runtimeEarth.createAnchorOnSurface(
                    latitude,
                    longitude,
                    altitudeAboveSurface,
                    eastUpSouthQuaternion,
                    surfaceToRuntimeSurface(surface),
                )

            val anchor = Anchor(runtimeAnchor, xrResourcesManager)
            xrResourcesManager.addUpdatable(anchor)
            AnchorCreateSuccess(anchor)
        } catch (e: IllegalStateException) {
            AnchorCreateIllegalState()
        } catch (e: AnchorResourcesExhaustedException) {
            AnchorCreateResourcesExhausted()
        } catch (e: AnchorNotAuthorizedException) {
            AnchorCreateNotAuthorized()
        } catch (e: AnchorUnsupportedLocationException) {
            AnchorCreateUnsupportedLocation()
        }
    }

    override suspend fun update() {
        _state.emit(runtimeStateToState(runtimeEarth.state))
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Earth) return false
        return runtimeEarth == other.runtimeEarth
    }

    private fun runtimeStateToState(runtimeState: RuntimeEarth.State): State {
        return when (runtimeState) {
            RuntimeEarth.State.RUNNING -> State.RUNNING
            RuntimeEarth.State.STOPPED -> State.STOPPED
            RuntimeEarth.State.ERROR_INTERNAL -> State.ERROR_INTERNAL
            RuntimeEarth.State.ERROR_NOT_AUTHORIZED -> State.ERROR_NOT_AUTHORIZED
            RuntimeEarth.State.ERROR_RESOURCES_EXHAUSTED -> State.ERROR_RESOURCES_EXHAUSTED
            RuntimeEarth.State.ERROR_APK_VERSION_TOO_OLD -> State.ERROR_APK_VERSION_TOO_OLD
            RuntimeEarth.State.ERROR_APP_PREEMPTED -> State.ERROR_APP_PREEMPTED
            else -> throw IllegalStateException("Unknown EarthErrorState: $runtimeState")
        }
    }

    private fun surfaceToRuntimeSurface(surface: Surface): RuntimeEarth.Surface {
        return when (surface) {
            Surface.TERRAIN -> RuntimeEarth.Surface.TERRAIN
            Surface.ROOFTOP -> RuntimeEarth.Surface.ROOFTOP
            else -> throw IllegalStateException("Unknown Surface: $surface")
        }
    }

    public override fun hashCode(): Int = runtimeEarth.hashCode()
}
