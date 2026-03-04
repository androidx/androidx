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
import androidx.xr.arcore.Geospatial.State.Companion.RUNNING
import androidx.xr.arcore.runtime.AnchorNotAuthorizedException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.AnchorUnsupportedLocationException
import androidx.xr.arcore.runtime.Geospatial as RuntimeGeospatial
import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides localization ability in Earth-relative coordinates.
 *
 * To use the Geospatial object, configure the session with
 * [androidx.xr.runtime.GeospatialMode.VPS_AND_GPS].
 *
 * Not all devices support [androidx.xr.runtime.GeospatialMode.VPS_AND_GPS], use
 * [androidx.xr.runtime.Config.ConfigMode.isSupported] to check if the current device supports
 * enabling this mode.
 *
 * The Geospatial object should only be used when its [State] is [State.RUNNING], and otherwise
 * should not be used. Use [Geospatial.state] to obtain the current [State].
 *
 * @property state the current [State] of [Geospatial]
 */
public class Geospatial
internal constructor(
    private val runtimeGeospatial: RuntimeGeospatial,
    private val xrResourcesManager: XrResourcesManager,
) : Updatable {
    public companion object {
        /**
         * Returns the Geospatial object for the given [Session].
         *
         * @param session the [Session] to get the [Geospatial] object from
         */
        @JvmStatic
        public fun getInstance(session: Session): Geospatial {
            val perceptionStateExtender =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            return perceptionStateExtender.xrResourcesManager.geospatial
        }
    }

    /**
     * Describes the state of Geospatial. The State must be [RUNNING] to use Geospatial
     * functionality. If Geospatial has entered an error state other than [PAUSED], Geospatial must
     * be disabled and re-enabled to use Geospatial again.
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
             * Sessions are limited per-minute and enabling may succeed if retried. The application
             * can disable and re-enable Geospatial to try again.
             */
            @JvmField public val ERROR_RESOURCE_EXHAUSTED: State = State(-3)

            /**
             * The Geospatial connection has been paused. The connection may resume, and does not
             * require action from the app. Tracked entities will enter the STOPPED state and must
             * be destroyed.
             */
            @JvmField public val PAUSED: State = State(2)
        }

        override fun toString(): String {
            return when (this) {
                RUNNING -> "RUNNING"
                NOT_RUNNING -> "NOT_RUNNING"
                ERROR_INTERNAL -> "ERROR_INTERNAL"
                ERROR_NOT_AUTHORIZED -> "ERROR_NOT_AUTHORIZED"
                ERROR_RESOURCE_EXHAUSTED -> "ERROR_RESOURCE_EXHAUSTED"
                PAUSED -> "PAUSED"
                else -> "Unknown"
            }
        }
    }

    private val _state = MutableStateFlow(State.NOT_RUNNING)

    public val state: StateFlow<Geospatial.State> = _state.asStateFlow()

    /**
     * Gets the availability of the Visual Positioning System (VPS) at a specified horizontal
     * position.
     *
     * The availability of VPS in a given location helps to improve the quality of Geospatial
     * localization and tracking accuracy.
     *
     * This launches an asynchronous operation used to query the Google Cloud ARCore API. It may be
     * called without calling [Session.configure].
     *
     * Your app must be properly set up to communicate with the Google Cloud ARCore API in order to
     * obtain a result from this call, otherwise the result will be
     * [androidx.xr.runtime.VpsAvailabilityNotAuthorized].
     *
     * @param latitude the latitude in degrees
     * @param longitude the longitude in degrees
     * @return the result of the VPS availability check
     */
    public suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult {
        return runtimeGeospatial.checkVpsAvailability(latitude, longitude)
    }

    /**
     * Converts the input geospatial location and orientation relative to the Earth to a [Pose] in
     * the same position.
     *
     * This method may return a [CreatePoseFromGeospatialPoseNotTracking] result if Geospatial is
     * not currently tracking.
     *
     * Positions near the north pole or south pole is not supported. If the latitude is within 0.1
     * degrees of the north pole or south pole (90 degrees or -90 degrees), this function will throw
     * an [IllegalArgumentException].
     *
     * @param geospatialPose the [GeospatialPose] to be converted into a [Pose]
     * @return a [CreatePoseFromGeospatialPoseResult] with the result of the conversion
     * @throws [IllegalArgumentException] if the latitude is within 0.1 degrees of the north pole or
     *   south pole (90 degrees or -90 degrees)
     */
    public fun createPoseFromGeospatialPose(
        geospatialPose: GeospatialPose
    ): CreatePoseFromGeospatialPoseResult {
        checkGeospatialModeEnabled()
        return try {
            CreatePoseFromGeospatialPoseSuccess(
                runtimeGeospatial.createPoseFromGeospatialPose(geospatialPose)
            )
        } catch (e: GeospatialPoseNotTrackingException) {
            CreatePoseFromGeospatialPoseNotTracking()
        }
    }

    /**
     * Converts the input [Pose] to a [GeospatialPose] in the same position as the original pose.
     *
     * This method may return a [GeospatialPoseNotTrackingException] result if Geospatial is not
     * currently tracking.
     *
     * @param pose the [Pose] to be converted into a [GeospatialPose]
     * @return a [CreateGeospatialPoseFromPoseResult] with the result of the conversion
     */
    public fun createGeospatialPoseFromPose(pose: Pose): CreateGeospatialPoseFromPoseResult {
        checkGeospatialModeEnabled()
        return try {
            val runtimeResult = runtimeGeospatial.createGeospatialPoseFromPose(pose)
            CreateGeospatialPoseFromPoseSuccess(
                runtimeResult.geospatialPose,
                runtimeResult.horizontalAccuracy,
                runtimeResult.verticalAccuracy,
                runtimeResult.orientationYawAccuracy,
            )
        } catch (e: GeospatialPoseNotTrackingException) {
            CreateGeospatialPoseFromPoseNotTracking()
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
     * ellipsoid, use [Geospatial.createAnchorOnTerrain].
     *
     * The rotation quaternion provided is with respect to an east-up-south coordinate frame. An
     * identity rotation will have the anchor oriented such that X+ points to the east, Y+ points up
     * away from the center of the earth, and Z+ points to the south.
     *
     * The tracking state of an [Anchor] will permanently become
     * [androidx.xr.runtime.TrackingState.STOPPED] if the [androidx.xr.runtime.GeospatialMode] is
     * disabled, or if another full-space app uses Geospatial.
     *
     * Creating anchors near the north pole or south pole is not supported. If the latitude is
     * within 0.1 degrees of the north pole or south pole (90 degrees or -90 degrees), this function
     * will throw [IllegalArgumentException].
     *
     * @param latitude the latitude of the anchor
     * @param longitude the longitude of the anchor
     * @param altitude the altitude of the anchor
     * @param eastUpSouthQuaternion the rotation quaternion of the anchor
     * @return an [AnchorCreateResult] with the result of the anchor creation
     * @throws [IllegalArgumentException] if the latitude is outside the allowable range
     */
    public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): AnchorCreateResult {
        checkGeospatialModeEnabled()
        return try {
            val runtimeAnchor =
                runtimeGeospatial.createAnchor(latitude, longitude, altitude, eastUpSouthQuaternion)
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
     * A Terrain anchor's tracking state will be [androidx.xr.runtime.TrackingState.PAUSED] if the
     * Earth is not actively tracking. Its tracking state will permanently become
     * [androidx.xr.runtime.TrackingState.STOPPED] if [androidx.xr.runtime.GeospatialMode] is
     * disabled, or if another full-space app uses Geospatial.
     *
     * Latitude and longitude are defined by the
     * [WGS84 specification](https://en.wikipedia.org/wiki/World_Geodetic_System), and the altitude
     * value is defined by the elevation above the Earth's terrain (or floor) in meters.
     *
     * The rotation quaternion provided is with respect to an east-up-south coordinate frame. An
     * identity rotation will have the anchor oriented such that X+ points to the east, Y+ points up
     * away from the center of the earth, and Z+ points to the south.
     *
     * @param latitude the latitude of the anchor
     * @param longitude the longitude of the anchor
     * @param altitudeAboveSurface the altitude of the anchor above the given surface
     * @param eastUpSouthQuaternion the rotation quaternion of the anchor
     * @param surface the [Surface] on which to create the anchor
     * @return an [AnchorCreateResult] with the result of the anchor creation
     * @throws IllegalArgumentException if the latitude is outside the allowable range
     */
    public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Surface,
    ): AnchorCreateResult {
        checkGeospatialModeEnabled()
        return try {
            val runtimeAnchor =
                runtimeGeospatial.createAnchorOnSurface(
                    latitude,
                    longitude,
                    altitudeAboveSurface,
                    eastUpSouthQuaternion,
                    surfaceToRuntimeSurface(surface),
                )

            val anchor = Anchor(runtimeAnchor, xrResourcesManager)
            xrResourcesManager.addUpdatable(anchor)
            AnchorCreateSuccess(anchor)
        } catch (e: AnchorResourcesExhaustedException) {
            AnchorCreateResourcesExhausted()
        } catch (e: AnchorNotAuthorizedException) {
            AnchorCreateNotAuthorized()
        } catch (e: AnchorUnsupportedLocationException) {
            AnchorCreateUnsupportedLocation()
        } catch (e: IllegalStateException) {
            AnchorCreateIllegalState()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override suspend fun update() {
        _state.emit(runtimeStateToState(runtimeGeospatial.state))
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Geospatial) return false
        return runtimeGeospatial == other.runtimeGeospatial
    }

    private fun checkGeospatialModeEnabled() {
        check(xrResourcesManager.lifecycleManager.config.geospatial == GeospatialMode.VPS_AND_GPS) {
            "To use this function, Config.GeospatialMode must be set to VPS_AND_GPS."
        }
    }

    private fun runtimeStateToState(runtimeState: RuntimeGeospatial.State): State {
        return when (runtimeState) {
            RuntimeGeospatial.State.RUNNING -> State.RUNNING
            RuntimeGeospatial.State.NOT_RUNNING -> State.NOT_RUNNING
            RuntimeGeospatial.State.ERROR_INTERNAL -> State.ERROR_INTERNAL
            RuntimeGeospatial.State.ERROR_NOT_AUTHORIZED -> State.ERROR_NOT_AUTHORIZED
            RuntimeGeospatial.State.ERROR_RESOURCE_EXHAUSTED -> State.ERROR_RESOURCE_EXHAUSTED
            RuntimeGeospatial.State.PAUSED -> State.PAUSED
            else -> throw IllegalStateException("Unknown State: $runtimeState")
        }
    }

    private fun surfaceToRuntimeSurface(surface: Surface): RuntimeGeospatial.Surface {
        return when (surface) {
            Surface.TERRAIN -> RuntimeGeospatial.Surface.TERRAIN
            Surface.ROOFTOP -> RuntimeGeospatial.Surface.ROOFTOP
            else -> throw IllegalStateException("Unknown Surface: $surface")
        }
    }

    public override fun hashCode(): Int = runtimeGeospatial.hashCode()
}
