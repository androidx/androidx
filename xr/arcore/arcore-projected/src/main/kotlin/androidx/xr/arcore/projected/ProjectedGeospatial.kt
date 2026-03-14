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

package androidx.xr.arcore.projected

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityNetworkError
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Currently unimplemented implementation of [Geospatial] on Projected.
 *
 * @property state the [Geospatial.State] of the geospatial instance
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ProjectedGeospatial internal constructor(private val xrResources: XrResources) :
    Geospatial {
    public override var state: Geospatial.State = Geospatial.State.NOT_RUNNING
        internal set

    private val service: IProjectedPerceptionService
        get() = xrResources.service

    private fun checkTrackingState() {
        if (
            xrResources.trackingState == TrackingState.STOPPED ||
                xrResources.geospatialTrackingState == TrackingState.STOPPED
        ) {
            throw GeospatialPoseNotTrackingException()
        }
    }

    override public fun createPoseFromGeospatialPose(geospatialPose: GeospatialPose): Pose {
        checkTrackingState()
        val projectedQuaternion =
            ProjectedQuarternion().apply {
                x = geospatialPose.eastUpSouthQuaternion.x
                y = geospatialPose.eastUpSouthQuaternion.y
                z = geospatialPose.eastUpSouthQuaternion.z
                w = geospatialPose.eastUpSouthQuaternion.w
            }
        val projectedEarthPose =
            ProjectedEarthPose().apply {
                latitude = geospatialPose.latitude
                longitude = geospatialPose.longitude
                altitude = geospatialPose.altitude
                eus = projectedQuaternion
            }
        val projectedPose = service.createPoseFromGeospatialPose(projectedEarthPose)
        // TODO: b/446185235 - maybe we need better error handling or in service?
        if (projectedPose == null) {
            return Pose()
        }
        return Pose(
            Vector3(projectedPose.vector.x, projectedPose.vector.y, projectedPose.vector.z),
            Quaternion(projectedPose.q.x, projectedPose.q.y, projectedPose.q.z, projectedPose.q.w),
        )
    }

    override public fun createGeospatialPoseFromPose(pose: Pose): Geospatial.GeospatialPoseResult {
        checkTrackingState()
        val projectedVector =
            ProjectedVector3().apply {
                x = pose.translation.x
                y = pose.translation.y
                z = pose.translation.z
            }
        val projectedQuaternion =
            ProjectedQuarternion().apply {
                x = pose.rotation.x
                y = pose.rotation.y
                z = pose.rotation.z
                w = pose.rotation.w
            }
        val projectedPose =
            ProjectedPose().apply {
                vector = projectedVector
                q = projectedQuaternion
            }
        val projectedEarthPose =
            try {
                // TODO: b/491554279 - remove android.os.RemoteException once the bug is fixed.
                service.createGeospatialPoseFromPose(projectedPose)
            } catch (e: android.os.RemoteException) {
                throw GeospatialPoseNotTrackingException()
            } catch (e: RuntimeException) {
                throw GeospatialPoseNotTrackingException()
            }
        // TODO: b/446185235 - maybe we need better error handling or in service?
        if (projectedEarthPose == null) {
            return Geospatial.GeospatialPoseResult(
                geospatialPose = GeospatialPose(0.0, 0.0, 0.0, Quaternion()),
                horizontalAccuracy = 0.0,
                verticalAccuracy = 0.0,
                orientationYawAccuracy = 0.0,
            )
        }
        val geospatialPose =
            GeospatialPose(
                latitude = projectedEarthPose.latitude,
                longitude = projectedEarthPose.longitude,
                altitude = projectedEarthPose.altitude,
                eastUpSouthQuaternion =
                    Quaternion(
                        projectedEarthPose.eus.x,
                        projectedEarthPose.eus.y,
                        projectedEarthPose.eus.z,
                        projectedEarthPose.eus.w,
                    ),
            )
        return Geospatial.GeospatialPoseResult(
            geospatialPose = geospatialPose,
            horizontalAccuracy = projectedEarthPose.locationAccuracyMeters,
            verticalAccuracy = projectedEarthPose.altitudeAccuracyMeters,
            orientationYawAccuracy = projectedEarthPose.orientationYawAccuracyDegrees,
        )
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
        surface: Geospatial.Surface,
    ): Anchor {
        throw NotImplementedError("Not implemented yet.")
    }

    override suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult = suspendCancellableCoroutine { continuation ->
        val callback =
            object : IVpsAvailabilityCallback.Stub() {
                override fun onVpsAvailabilityChanged(vpsState: Int) {
                    val vpsResult =
                        // vpsState is the enum VpsAvailability, see the code in
                        // third_party/arcore/java/com/google/ar/core/VpsAvailability.java
                        // and the onVpsAvailabilityChanged callback call in
                        // java/com/google/android/projection/core/modules/perception/PerceptionManagerService.java.
                        when (vpsState) {
                            // VpsAvailability.AVAILABLE
                            1 -> VpsAvailabilityAvailable()
                            // VpsAvailability.UNAVAILABLE
                            2 -> VpsAvailabilityUnavailable()
                            // VpsAvailability.ERROR_NETWORK_CONNECTION
                            -2 -> VpsAvailabilityNetworkError()
                            // VpsAvailability.ERROR_NOT_AUTHORIZED
                            -3 -> VpsAvailabilityNotAuthorized()
                            // VpsAvailability.ERROR_RESOURCE_EXHAUSTED
                            -4 -> VpsAvailabilityResourceExhausted()
                            // VpsAvailability.UNKNOWN or VpsAvailability.ERROR_INTERNAL
                            else -> VpsAvailabilityErrorInternal()
                        }
                    continuation.resume(vpsResult)
                }

                override fun getInterfaceVersion(): Int = VERSION
            }
        try {
            xrResources.service.checkVpsAvailability(latitude, longitude, callback)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
