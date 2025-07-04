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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.AnchorNotAuthorizedException
import androidx.xr.runtime.internal.AnchorUnsupportedLocationException
import androidx.xr.runtime.internal.Earth
import androidx.xr.runtime.internal.GeospatialPoseNotTrackingException
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import com.google.ar.core.Anchor as ARCore1xAnchor
import com.google.ar.core.Anchor.RooftopAnchorState as ARCore1xRooftopAnchorState
import com.google.ar.core.Anchor.TerrainAnchorState as ARCore1xTerrainAnchorState
import com.google.ar.core.Earth as ARCore1xEarth
import com.google.ar.core.Future as ARCore1xFuture
import com.google.ar.core.ResolveAnchorOnRooftopFuture as ARCore1xRooftopAnchorFuture
import com.google.ar.core.ResolveAnchorOnTerrainFuture as ARCore1xTerrainAnchorFuture
import com.google.ar.core.Session
import com.google.ar.core.TrackingState as ARCore1xTrackingState
import com.google.ar.core.exceptions.NotTrackingException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Wraps the native [ARCore1xEarth] with the [Earth] interface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreEarth internal constructor(private val resources: XrResources) : Earth {

    /** Reference to the ARCore Java Earth object. */
    public var arCoreEarth: ARCore1xEarth? = null
        internal set

    public override var state: Earth.State = Earth.State.STOPPED
        private set

    override public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose {
        validateEarthTracking()

        try {
            val arCorePose =
                checkNotNull(arCoreEarth)
                    .getPose(
                        geospatialPose.latitude,
                        geospatialPose.longitude,
                        geospatialPose.altitude,
                        geospatialPose.eastUpSouthQuaternion.x,
                        geospatialPose.eastUpSouthQuaternion.y,
                        geospatialPose.eastUpSouthQuaternion.z,
                        geospatialPose.eastUpSouthQuaternion.w,
                    )
            return arCorePose.toRuntimePose()
        } catch (e: NotTrackingException) {
            // Since Jetpack updates are async, it's possible that the Earth becomes not tracking
            // even
            // after validation.
            throw GeospatialPoseNotTrackingException(e)
        }
    }

    override public fun createGeospatialPoseFromPose(pose: Pose): Earth.GeospatialPoseResult {
        validateEarthTracking()

        try {
            val arCoreGeospatialPose =
                checkNotNull(arCoreEarth).getGeospatialPose(pose.toARCorePose())

            return Earth.GeospatialPoseResult(
                arCoreGeospatialPose.toRuntimeGeospatialPose(),
                arCoreGeospatialPose.horizontalAccuracy,
                arCoreGeospatialPose.verticalAccuracy,
                arCoreGeospatialPose.orientationYawAccuracy,
            )
        } catch (e: NotTrackingException) {
            // Since Jetpack updates are async, it's possible that the Earth becomes not tracking
            // even
            // after validation.
            throw GeospatialPoseNotTrackingException(e)
        }
    }

    override public fun createGeospatialPoseFromDevicePose(): Earth.GeospatialPoseResult {
        validateEarthTracking()

        try {
            val arCoreGeospatialPose = checkNotNull(arCoreEarth).cameraGeospatialPose

            return Earth.GeospatialPoseResult(
                arCoreGeospatialPose.toRuntimeGeospatialPose(),
                arCoreGeospatialPose.horizontalAccuracy,
                arCoreGeospatialPose.verticalAccuracy,
                arCoreGeospatialPose.orientationYawAccuracy,
            )
        } catch (e: NotTrackingException) {
            // Since Jetpack updates are async, it's possible that the Earth becomes not tracking
            // even
            // after validation.
            throw GeospatialPoseNotTrackingException(e)
        }
    }

    override public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): RuntimeAnchor {
        validateEarthEnabled()

        val arCoreAnchor =
            checkNotNull(arCoreEarth)
                .createAnchor(
                    latitude,
                    longitude,
                    altitude,
                    eastUpSouthQuaternion.x,
                    eastUpSouthQuaternion.y,
                    eastUpSouthQuaternion.z,
                    eastUpSouthQuaternion.w,
                )
        return ArCoreAnchor(arCoreAnchor)
    }

    override public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Earth.Surface,
    ): RuntimeAnchor {
        validateEarthEnabled()

        return suspendCancellableCoroutine { continuation ->
            val future: ARCore1xFuture =
                when (surface) {
                    Earth.Surface.TERRAIN ->
                        checkNotNull(arCoreEarth)
                            .resolveAnchorOnTerrainAsync(
                                latitude,
                                longitude,
                                altitudeAboveSurface,
                                eastUpSouthQuaternion.x,
                                eastUpSouthQuaternion.y,
                                eastUpSouthQuaternion.z,
                                eastUpSouthQuaternion.w,
                                { anchor, terrainAnchorState ->
                                    resumeWithTerrainAnchorOrException(
                                        anchor,
                                        terrainAnchorState,
                                        continuation,
                                    )
                                },
                            )
                    Earth.Surface.ROOFTOP ->
                        checkNotNull(arCoreEarth)
                            .resolveAnchorOnRooftopAsync(
                                latitude,
                                longitude,
                                altitudeAboveSurface,
                                eastUpSouthQuaternion.x,
                                eastUpSouthQuaternion.y,
                                eastUpSouthQuaternion.z,
                                eastUpSouthQuaternion.w,
                                { anchor, rooftopAnchorState ->
                                    resumeWithRooftopAnchorOrException(
                                        anchor,
                                        rooftopAnchorState,
                                        continuation,
                                    )
                                },
                            )
                    else -> throw IllegalStateException("Unknown surface type.")
                }

            continuation.invokeOnCancellation {
                if (!future.cancel()) {
                    // The future was already completed, so it could not be cancelled. We need to
                    // clean up by
                    // deleting the anchor. Note that due to the "prompt cancellation guarantee", if
                    // the
                    // coroutine is cancelled, that means it will not be resumed with a result, so
                    // the user
                    // will never see the created anchor.
                    // Also note that ArCore1xFuture cancellation is thread-safe, so if we
                    // successfully
                    // cancel the future, the callback was not invoked and the anchor is already
                    // deleted.
                    when (future) {
                        is ARCore1xTerrainAnchorFuture -> {
                            future.resultAnchor?.detach()
                        }
                        is ARCore1xRooftopAnchorFuture -> {
                            future.resultAnchor?.detach()
                        }
                        else -> {
                            throw IllegalStateException("Unknown future type.")
                        }
                    }
                }
            }
        }
    }

    public fun update(session: Session) {
        this.arCoreEarth = session.earth

        when (arCoreEarth?.earthState) {
            null -> {
                state = Earth.State.STOPPED
            }
            ARCore1xEarth.EarthState.ENABLED -> {
                state = Earth.State.RUNNING
            }
            ARCore1xEarth.EarthState.ERROR_APK_VERSION_TOO_OLD -> {
                state = Earth.State.ERROR_APK_VERSION_TOO_OLD
            }
            ARCore1xEarth.EarthState.ERROR_GEOSPATIAL_MODE_DISABLED -> {
                state = Earth.State.STOPPED
                arCoreEarth = null
            }
            ARCore1xEarth.EarthState.ERROR_INTERNAL -> {
                state = Earth.State.ERROR_INTERNAL
            }
            ARCore1xEarth.EarthState.ERROR_NOT_AUTHORIZED -> {
                state = Earth.State.ERROR_NOT_AUTHORIZED
            }
            ARCore1xEarth.EarthState.ERROR_RESOURCE_EXHAUSTED -> {
                state = Earth.State.ERROR_RESOURCES_EXHAUSTED
            }
        }
    }

    /**
     * Validates that the Earth is tracking and available.
     *
     * @throws IllegalStateException if the Earth is not tracking or not available.
     */
    private fun validateEarthTracking() {
        validateEarthEnabled()
        if (checkNotNull(arCoreEarth).trackingState != ARCore1xTrackingState.TRACKING) {
            throw GeospatialPoseNotTrackingException()
        }
    }

    private fun validateEarthEnabled() {
        // TODO: b/408482647 - Without locking this doesn't guarantee that the state won't change
        // between the check and the call.
        check(state == Earth.State.RUNNING)
        check(checkNotNull(arCoreEarth).earthState == ARCore1xEarth.EarthState.ENABLED)
    }

    /**
     * Converts the ARCore1xAnchor to a RuntimeAnchor, or resumes the continuation with an exception
     * if the anchor could not be created.
     */
    private fun resumeWithTerrainAnchorOrException(
        anchor: ARCore1xAnchor,
        terrainAnchorState: ARCore1xTerrainAnchorState,
        continuation: Continuation<RuntimeAnchor>,
    ) {
        when (terrainAnchorState) {
            ARCore1xTerrainAnchorState.SUCCESS -> {
                continuation.resume(ArCoreAnchor(anchor))
            }
            ARCore1xTerrainAnchorState.NONE -> {
                continuation.resumeWithException(
                    IllegalStateException("Anchor creation failed: Unknown Error.")
                )
            }
            ARCore1xTerrainAnchorState.TASK_IN_PROGRESS -> {
                // TASK_IN_PROGRESS should not be possible when called from the Anchor callback.
                continuation.resumeWithException(
                    IllegalStateException("Callback resumed on incomplete Terrain Anchor Future.")
                )
            }
            ARCore1xTerrainAnchorState.ERROR_INTERNAL -> {
                continuation.resumeWithException(
                    IllegalStateException("Anchor creation failed: Unknown Error.")
                )
            }
            ARCore1xTerrainAnchorState.ERROR_NOT_AUTHORIZED -> {
                continuation.resumeWithException(AnchorNotAuthorizedException())
            }
            ARCore1xTerrainAnchorState.ERROR_UNSUPPORTED_LOCATION -> {
                continuation.resumeWithException(AnchorUnsupportedLocationException())
            }
        }
    }

    /**
     * Converts the ARCore1xAnchor to a RuntimeAnchor, or resumes the continuation with an exception
     * if the anchor could not be created.
     */
    private fun resumeWithRooftopAnchorOrException(
        anchor: ARCore1xAnchor,
        rooftopAnchorState: ARCore1xRooftopAnchorState,
        continuation: Continuation<RuntimeAnchor>,
    ) {
        when (rooftopAnchorState) {
            ARCore1xRooftopAnchorState.SUCCESS -> {
                continuation.resume(ArCoreAnchor(anchor))
            }
            ARCore1xRooftopAnchorState.NONE -> {
                continuation.resumeWithException(
                    IllegalStateException("Anchor creation failed: Unknown Error.")
                )
            }
            ARCore1xRooftopAnchorState.ERROR_INTERNAL -> {
                continuation.resumeWithException(
                    IllegalStateException("Anchor creation failed: Unknown Error.")
                )
            }
            ARCore1xRooftopAnchorState.ERROR_NOT_AUTHORIZED -> {
                continuation.resumeWithException(AnchorNotAuthorizedException())
            }
            ARCore1xRooftopAnchorState.ERROR_UNSUPPORTED_LOCATION -> {
                continuation.resumeWithException(AnchorUnsupportedLocationException())
            }
        }
    }
}
