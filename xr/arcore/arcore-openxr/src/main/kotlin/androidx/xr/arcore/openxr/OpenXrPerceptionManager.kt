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
import androidx.xr.arcore.runtime.AnchorInvalidUuidException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.DepthMap
import androidx.xr.arcore.runtime.Eye
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Hand
import androidx.xr.arcore.runtime.HitResult
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.UUID

/**
 * Implementation of the perception capabilities of a runtime using OpenXR.
 *
 * @property xrResources the [XrResources] for this manager
 * @property trackables the collection of [Trackable] objects
 * @property leftEye the left [Eye], or null if not available
 * @property rightEye the right [Eye], or null if not available
 * @property leftHand the left [Hand], or null if not available
 * @property rightHand the right [Hand], or null if not available
 * @property arDevice the [OpenXrDevice] instance
 * @property leftRenderViewpoint the left [RenderViewpoint], or null if not available
 * @property rightRenderViewpoint the right [RenderViewpoint], or null if not available
 * @property monoRenderViewpoint the mono [RenderViewpoint], or null if not available
 * @property userFace the user's [Face], or null if not available
 * @property geospatial the [OpenXrGeospatial] instance
 * @property leftDepthMap the left [DepthMap], or null if not available
 * @property rightDepthMap the right [DepthMap], or null if not available
 * @property monoDepthMap the mono [DepthMap], or null if not available
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrPerceptionManager
internal constructor(private val timeSource: OpenXrTimeSource) : PerceptionManager {

    override fun createAnchor(pose: Pose): Anchor {
        val nativeAnchor = nativeCreateAnchor(pose, lastUpdateXrTime)
        checkNativeAnchorIsValid(nativeAnchor)
        val anchor = OpenXrAnchor(nativeAnchor, xrResources)
        anchor.update(lastUpdateXrTime)
        xrResources.addUpdatable(anchor as Updatable)
        return anchor
    }

    // TODO: b/345315434 - Implement this method correctly once we have the ability to conduct
    // hit tests in the native OpenXrManager.
    override fun hitTest(ray: Ray): List<HitResult> {
        val hitData =
            nativeHitTest(
                maxResults = 5,
                ray.origin.x,
                ray.origin.y,
                ray.origin.z,
                ray.direction.x,
                ray.direction.y,
                ray.direction.z,
                lastUpdateXrTime,
            )
        return Arrays.asList(*hitData).toList().map { toHitResult(it, ray.origin) }
    }

    override fun getPersistedAnchorUuids(): List<UUID> {
        val anchorUuids = nativeGetPersistedAnchorUuids()
        return Arrays.asList(*anchorUuids)
            .toList()
            .map { OpenXrAnchor.UUIDFromByteArray(it) }
            .filterNotNull()
    }

    override fun loadAnchor(uuid: UUID): Anchor {
        val nativeAnchor = nativeLoadAnchor(uuid)
        when (nativeAnchor) {
            -2L -> throw AnchorInvalidUuidException()
            -10L -> throw AnchorResourcesExhaustedException()
        }
        val anchor = OpenXrAnchor(nativeAnchor, xrResources, loadedUuid = uuid)
        anchor.update(lastUpdateXrTime)
        xrResources.addUpdatable(anchor as Updatable)
        return anchor
    }

    override fun unpersistAnchor(uuid: UUID) {
        check(nativeUnpersistAnchor(uuid)) { "Failed to unpersist anchor." }
    }

    internal val xrResources = XrResources(timeSource)
    override val trackables: Collection<Trackable> = xrResources.trackablesMap.values

    override val leftEye: Eye
        get() = xrResources.leftEye

    override val rightEye: Eye
        get() = xrResources.rightEye

    override val leftHand: Hand
        get() = xrResources.leftHand

    override val rightHand: Hand
        get() = xrResources.rightHand

    override val arDevice: OpenXrDevice
        get() = xrResources.arDevice

    override val leftRenderViewpoint: RenderViewpoint?
        get() = xrResources.leftRenderViewpoint

    override val rightRenderViewpoint: RenderViewpoint?
        get() = xrResources.rightRenderViewpoint

    // Mono render viewpoint is not supported in OpenXR.
    override val monoRenderViewpoint: RenderViewpoint? = null

    override val userFace: Face?
        get() = xrResources.userFace

    override val geospatial: OpenXrGeospatial = xrResources.geospatial

    override val leftDepthMap: DepthMap?
        get() = xrResources.leftDepthMap

    override val rightDepthMap: DepthMap?
        get() = xrResources.rightDepthMap

    // Mono depth map is not supported in OpenXR.
    override val monoDepthMap: DepthMap? = null

    internal var depthEstimationMode = DepthEstimationMode.DISABLED

    internal var eyeTrackingMode = EyeTrackingMode.DISABLED

    private var lastUpdateXrTime: Long = 0L

    /**
     * Updates the perception manager.
     *
     * @param xrTime the number of nanoseconds since the start of the OpenXR epoch
     */
    public fun update(xrTime: Long) {
        for (updatable in xrResources.updatables) {
            updatable.update(xrTime)
        }

        // View Cameras data are fetch within one JNI call, so they are updated separately.
        // TODO(b/421191332): Add the View Camera config and apply it for poseInUnboundedSpace.
        updateRenderViewpoints(xrTime, false)

        if (depthEstimationMode != DepthEstimationMode.DISABLED) {
            val depthMapBuffers = nativeGetDepthImagesDataBuffers(xrTime)
            xrResources.leftDepthMap.update(depthMapBuffers)
            xrResources.rightDepthMap.update(depthMapBuffers)
        }

        if (eyeTrackingMode != EyeTrackingMode.DISABLED) {
            updateEyes(xrTime)
        }

        lastUpdateXrTime = xrTime
    }

    internal fun updateAugmentedObjects(xrTime: Long) {
        val objects = nativeGetAugmentedObjects(xrTime)
        // Add new objects to the list of trackables.
        for (obj in objects) {
            if (xrResources.trackablesMap.containsKey(obj)) continue

            val trackable = OpenXrAugmentedObject(obj, timeSource, xrResources)
            xrResources.addTrackable(obj, trackable)
            xrResources.addUpdatable(trackable as Updatable)
        }
    }

    internal fun updateEyes(xrTime: Long) {
        val eyesInfo = nativeGetEyesInfo(xrTime)
        if (eyesInfo.trackingState.hasLeft) {
            xrResources.leftEye.update(eyesInfo.eyes[0])
        }
        if (eyesInfo.trackingState.hasRight) {
            xrResources.rightEye.update(eyesInfo.eyes[1])
        }
    }

    internal fun updatePlanes(xrTime: Long) {
        val planes = nativeGetPlanes()
        // Add new planes to the list of trackables.
        for (plane in planes) {
            if (xrResources.trackablesMap.containsKey(plane)) continue

            val planeTypeInt = nativeGetPlaneType(plane, xrTime)
            check(planeTypeInt >= 0) { "Failed to get plane type." }

            val trackable =
                OpenXrPlane(plane, Plane.Type.fromOpenXrType(planeTypeInt), timeSource, xrResources)
            xrResources.addTrackable(plane, trackable)
            xrResources.addUpdatable(trackable as Updatable)
        }
    }

    internal fun updateRenderViewpoints(xrTime: Long, poseInUnboundedSpace: Boolean) {
        val viewCameraStates = nativeGetViewCameras(poseInUnboundedSpace, xrTime)
        if (viewCameraStates != null) {
            check(viewCameraStates.size == 2)
            xrResources.leftRenderViewpoint.update(viewCameraStates[0])
            xrResources.rightRenderViewpoint.update(viewCameraStates[1])
        }
    }

    internal fun clear() {
        xrResources.clear()
    }

    private fun toHitResult(hitData: HitData, origin: Vector3): HitResult {
        val trackable =
            xrResources.trackablesMap[hitData.id]
                ?: throw IllegalStateException("Trackable not found.")

        return HitResult(
            distance = (hitData.pose.translation - origin).length,
            hitPose = hitData.pose,
            trackable = trackable,
        )
    }

    private fun checkNativeAnchorIsValid(nativeAnchor: Long) {
        when (nativeAnchor) {
            -2L -> throw IllegalStateException("Failed to create anchor.") // kErrorRuntimeFailure
            -10L -> throw AnchorResourcesExhaustedException() // kErrorLimitReached
        }
    }

    private external fun nativeCreateAnchor(pose: Pose, timestampNs: Long): Long

    private external fun nativeGetAugmentedObjects(timestampNs: Long): LongArray

    private external fun nativeGetEyesInfo(xrTime: Long): EyesInfo

    private external fun nativeGetPlanes(): LongArray

    private external fun nativeGetPlaneType(planeId: Long, timestampNs: Long): Int

    private external fun nativeHitTest(
        maxResults: Int,
        originX: Float,
        originY: Float,
        originZ: Float,
        directionX: Float,
        directionY: Float,
        directionZ: Float,
        timestampNs: Long,
    ): Array<HitData>

    private external fun nativeGetPersistedAnchorUuids(): Array<ByteArray>

    private external fun nativeLoadAnchor(uuid: UUID): Long

    private external fun nativeUnpersistAnchor(uuid: UUID): Boolean

    private external fun nativeGetViewCameras(
        isHeadTrackingEnabled: Boolean,
        timestampNs: Long,
    ): Array<ViewCameraState>?

    private external fun nativeGetDepthImagesDataBuffers(timestampNs: Long): Array<ByteBuffer>
}
