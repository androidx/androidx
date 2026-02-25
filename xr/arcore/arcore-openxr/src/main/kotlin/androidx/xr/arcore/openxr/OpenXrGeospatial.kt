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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorNotAuthorizedException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.AnchorUnsupportedLocationException
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implementation of [Geospatial] on OpenXR.
 *
 * @property state the current [Geospatial.State]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrGeospatial
internal constructor(
    private val xrResources: XrResources,
    private val timeSource: OpenXrTimeSource,
) : Geospatial, Updatable {

    public override var state: Geospatial.State = Geospatial.State.NOT_RUNNING
        private set

    override public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose {
        val xrTime = timeSource.getXrTime(timeSource.markNow())
        val result = nativeLocatePoseFromGeospatialPose(xrTime, geospatialPose)
        // The native implementation returns null when not tracking.
        return result ?: throw GeospatialPoseNotTrackingException()
    }

    override public fun createGeospatialPoseFromPose(pose: Pose): Geospatial.GeospatialPoseResult {
        val xrTime = timeSource.getXrTime(timeSource.markNow())
        val result = nativeCreateGeospatialPoseFromPose(xrTime, pose)
        // The native implementation returns null when not tracking.
        return result ?: throw GeospatialPoseNotTrackingException()
    }

    override public fun createAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): Anchor {
        val xrTime = timeSource.getXrTime(timeSource.markNow())
        val anchorNativePointer =
            nativeCreateAnchor(latitude, longitude, altitude, eastUpSouthQuaternion, xrTime)
        checkNativeAnchorIsValid(anchorNativePointer)
        val anchor = OpenXrAnchor(anchorNativePointer, xrResources)
        xrResources.addUpdatable(anchor)
        return anchor
    }

    override public suspend fun createAnchorOnSurface(
        latitude: Double,
        longitude: Double,
        altitudeAboveSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        surface: Geospatial.Surface,
    ): Anchor {
        return suspendCancellableCoroutine { continuation ->
            nativeCreateSurfaceAnchorAsync(
                surfaceTypeToXrSurfaceAnchorType(surface),
                latitude,
                longitude,
                altitudeAboveSurface,
                eastUpSouthQuaternion,
            ) { result ->
                try {
                    checkNativeAnchorIsValid(result)
                    val anchor = OpenXrAnchor(result, xrResources)
                    xrResources.addUpdatable(anchor)
                    continuation.resume(anchor)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    override suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult {
        return suspendCancellableCoroutine { continuation ->
            nativeCheckVpsAvailabilityAsync(latitude, longitude) { result ->
                continuation.resume(result)
            }
        }
    }

    /**
     * Updates the entity retrieving its state at [xrTime].
     *
     * @param xrTime the number of nanoseconds since the start of the OpenXR epoch
     */
    override fun update(xrTime: Long) {
        state = nativeGetGeospatialState(xrTime) ?: Geospatial.State.NOT_RUNNING
    }

    private fun checkNativeAnchorIsValid(nativeAnchor: Long) {
        when (nativeAnchor) {
            -2L -> throw IllegalStateException("Failed to create anchor.") // kErrorRuntimeFailure
            -10L -> throw AnchorResourcesExhaustedException() // kErrorLimitReached
            -1000789002L -> AnchorNotAuthorizedException() // kErrorCloudAuthFailed
            -1000797000L ->
                AnchorUnsupportedLocationException() // kErrorSurfaceAnchorLocationUnsupported
        }
    }

    private fun surfaceTypeToXrSurfaceAnchorType(surface: Geospatial.Surface): Int {
        return when (surface) {
            Geospatial.Surface.TERRAIN -> 1 // XR_SURFACE_ANCHOR_TYPE_TERRAIN_ANDROID
            Geospatial.Surface.ROOFTOP -> 2 // XR_SURFACE_ANCHOR_TYPE_ROOFTOP_ANDROID
            else -> 1
        }
    }

    private external fun nativeGetGeospatialState(monotonicTimeNs: Long): Geospatial.State?

    private external fun nativeLocatePoseFromGeospatialPose(
        monotonicTimeNs: Long,
        geospatialPose: GeospatialPose,
    ): Pose?

    private external fun nativeCreateGeospatialPoseFromPose(
        monotonicTimeNs: Long,
        pose: Pose,
    ): Geospatial.GeospatialPoseResult?

    private external fun nativeCreateAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
        monotonicTimeNs: Long,
    ): Long

    private external fun nativeCheckVpsAvailabilityAsync(
        latitude: Double,
        longitude: Double,
        callback: (VpsAvailabilityResult) -> Unit,
    )

    private external fun nativeCreateSurfaceAnchorAsync(
        surfaceAnchorType: Int,
        latitude: Double,
        longitude: Double,
        altitudeRelativeToSurface: Double,
        eastUpSouthQuaternion: Quaternion,
        callback: (Long) -> Unit,
    )
}
