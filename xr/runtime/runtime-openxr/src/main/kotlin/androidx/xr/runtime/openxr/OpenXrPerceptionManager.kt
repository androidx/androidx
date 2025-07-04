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

package androidx.xr.runtime.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorInvalidUuidException
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.DepthMap
import androidx.xr.runtime.internal.Face
import androidx.xr.runtime.internal.Hand
import androidx.xr.runtime.internal.HitResult
import androidx.xr.runtime.internal.PerceptionManager
import androidx.xr.runtime.internal.Plane
import androidx.xr.runtime.internal.Trackable
import androidx.xr.runtime.internal.ViewCamera
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.UUID

/** Implementation of the perception capabilities of a runtime using OpenXR. */
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

    override fun loadAnchorFromNativePointer(nativePointer: Long): Anchor {
        val anchor = OpenXrAnchor(nativePointer, xrResources)
        anchor.update(lastUpdateXrTime)
        xrResources.addUpdatable(anchor as Updatable)
        return anchor
    }

    override fun unpersistAnchor(uuid: UUID) {
        check(nativeUnpersistAnchor(uuid)) { "Failed to unpersist anchor." }
    }

    internal val xrResources = XrResources()
    override val trackables: Collection<Trackable> = xrResources.trackablesMap.values
    override val leftHand: Hand
        get() = xrResources.leftHand

    override val rightHand: Hand
        get() = xrResources.rightHand

    override val arDevice: OpenXrDevice
        get() = xrResources.arDevice

    override val viewCameras: List<ViewCamera>
        get() = xrResources.viewCameras

    override val userFace: Face?
        get() = xrResources.userFace

    override val earth: OpenXrEarth = xrResources.earth

    override val depthMaps: List<DepthMap> =
        listOf(xrResources.leftDepthMap, xrResources.rightDepthMap)
    internal var depthEstimationMode = Config.DepthEstimationMode.DISABLED

    private var lastUpdateXrTime: Long = 0L

    /**
     * Updates the perception manager.
     *
     * @param xrTime the number of nanoseconds since the start of the OpenXR epoch.
     */
    public fun update(xrTime: Long) {
        for (updatable in xrResources.updatables) {
            updatable.update(xrTime)
        }

        // View Cameras data are fetch within one JNI call, so they are updated separately.
        // TODO(b/421191332): Add the View Camera config and apply it for poseInUnboundedSpace.
        updateViewCameras(xrTime, false)

        if (depthEstimationMode != Config.DepthEstimationMode.DISABLED) {
            val depthMapBuffers = nativeGetDepthImagesDataBuffers(xrTime)
            xrResources.leftDepthMap.update(depthMapBuffers)
            xrResources.rightDepthMap.update(depthMapBuffers)
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

    internal fun updateViewCameras(xrTime: Long, poseInUnboundedSpace: Boolean) {
        val viewCameraStates = nativeGetViewCameras(poseInUnboundedSpace, xrTime)
        if (viewCameraStates != null) {
            check(viewCameraStates.size == 2)
            xrResources.viewCameras[0].update(viewCameraStates[0])
            xrResources.viewCameras[1].update(viewCameraStates[1])
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
