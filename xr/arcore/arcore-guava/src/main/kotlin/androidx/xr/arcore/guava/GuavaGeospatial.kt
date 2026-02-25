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

@file:JvmName("GuavaGeospatial")

package androidx.xr.arcore.guava

import androidx.annotation.RestrictTo
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateIllegalState
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateResult
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.Geospatial.State
import androidx.xr.arcore.Geospatial.Surface
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.Quaternion
import com.google.common.util.concurrent.ListenableFuture

/**
 * Asynchronously creates a new [Anchor] at a specified horizontal position and altitude relative to
 * the horizontal position's surface (Terrain or Rooftop).
 *
 * The specified [altitudeAboveSurface] is interpreted to be relative to the given surface at the
 * specified latitude/longitude geospatial coordinates, rather than relative to the WGS84 ellipsoid.
 * Specifying an altitude of 0 will position the anchor directly on the surface whereas specifying a
 * positive altitude will position the anchor above the surface, against the direction of gravity.
 *
 * [Surface.TERRAIN] refers to the Earth's terrain (or floor) and [Surface.ROOFTOP] refers to the
 * top of a building at the given horizontal location. If there is no building at the given
 * location, then the rooftop surface is interpreted to be the terrain instead.
 *
 * You may resolve multiple anchors at a time, but a session cannot be tracking more than 100
 * surface anchors at time. Attempting to resolve more than 100 surface anchors will return an
 * [AnchorCreateResourcesExhausted] result.
 *
 * Creating a Terrain anchor requires an active Geospatial which is [State.RUNNING]. If it is not,
 * then this function returns an [AnchorCreateIllegalState] result. This call also requires a
 * working internet connection to communicate with the ARCore API on Google Cloud. ARCore will
 * continue to retry if it is unable to establish a connection to the ARCore service.
 *
 * A Terrain anchor's tracking state will be [TrackingState.PAUSED] if Geospatial is not actively
 * tracking. Its tracking state will permanently become [TrackingState.STOPPED] if
 * [androidx.xr.runtime.GeospatialMode] is disabled, or if another full-space app uses Geospatial.
 *
 * Latitude and longitude are defined by the
 * [WGS84 specification](https://en.wikipedia.org/wiki/World_Geodetic_System), and the altitude
 * value is defined by the elevation above the Earth's terrain (or floor) in meters.
 *
 * The rotation quaternion provided is with respect to an east-up-south coordinate frame. An
 * identity rotation will have the anchor oriented such that X+ points to the east, Y+ points up
 * away from the center of the earth, and Z+ points to the south.
 *
 * @param session the current [Session]
 * @param latitude the latitude of the anchor
 * @param longitude the longitude of the anchor
 * @param altitudeAboveSurface the altitude of the anchor above the given surface
 * @param eastUpSouthQuaternion the rotation [Quaternion] of the anchor
 * @param surface the [Surface] the anchor is attached to
 * @return a [ListenableFuture] that will complete with the [AnchorCreateResult]
 * @throws IllegalArgumentException if the latitude is outside the allowable range
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Geospatial.createAnchorOnSurfaceAsync(
    session: Session,
    latitude: Double,
    longitude: Double,
    altitudeAboveSurface: Double,
    eastUpSouthQuaternion: Quaternion,
    surface: Surface,
): ListenableFuture<AnchorCreateResult> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        createAnchorOnSurface(
            latitude,
            longitude,
            altitudeAboveSurface,
            eastUpSouthQuaternion,
            surface,
        )
    }

/**
 * Gets the availability of the Visual Positioning System (VPS) at a specified horizontal position.
 *
 * The availability of VPS in a given location helps to improve the quality of Geospatial
 * localization and tracking accuracy.
 *
 * This launches an asynchronous operation used to query the Google Cloud ARCore API. It may be
 * called without calling [Session.configure].
 *
 * Your app must be properly set up to communicate with the Google Cloud ARCore API in order to
 * obtain a result from this call, otherwise the result will be [VpsAvailabilityNotAuthorized].
 *
 * @param session the current [Session]
 * @param latitude the latitude in degrees
 * @param longitude the longitude in degrees
 * @return a [ListenableFuture] that will complete with the [VpsAvailabilityResult]
 */
public fun Geospatial.checkVpsAvailabilityAsync(
    session: Session,
    latitude: Double,
    longitude: Double,
): ListenableFuture<VpsAvailabilityResult> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        checkVpsAvailability(latitude, longitude)
    }
