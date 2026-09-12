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

import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorInvalidUuidException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.AnchorRuntimeFailureException
import androidx.xr.arcore.runtime.ConversationState
import androidx.xr.arcore.runtime.Depth
import androidx.xr.arcore.runtime.Eye
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Hand
import androidx.xr.arcore.runtime.HitResult
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationImageFormat
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quad
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
 * @property leftDepth the left [Depth], or null if not available
 * @property rightDepth the right [Depth], or null if not available
 * @property monoDepth the mono [Depth], or null if not available
 */
internal class OpenXrPerceptionManager(private val timeSource: OpenXrTimeSource) :
    PerceptionManager {

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

    override val leftDepth: Depth?
        get() = xrResources.leftDepth

    override val rightDepth: Depth?
        get() = xrResources.rightDepth

    // Mono depth map is not supported in OpenXR.
    override val monoDepth: Depth? = null

    // Conversation scene signal is not supported in OpenXR.
    override val conversationSceneSignal: ConversationState? = null

    internal var depthEstimationMode = DepthEstimationMode.DISABLED

    internal var eyeTrackingMode = EyeTrackingMode.DISABLED
    private var lastUpdateXrTime: Long = 0L

    /**
     * Updates the perception manager.
     *
     * @param xrTime the number of nanoseconds since the start of the OpenXR epoch
     */
    internal fun update(xrTime: Long) {
        for (updatable in xrResources.updatables) {
            updatable.update(xrTime)
        }

        // View Cameras data are fetch within one JNI call, so they are updated separately.
        // TODO(b/421191332): Add the View Camera config and apply it for poseInUnboundedSpace.
        updateRenderViewpoints(xrTime, false)

        if (depthEstimationMode != DepthEstimationMode.DISABLED) {
            val depthMapBuffers = nativeGetDepthImagesDataBuffers(xrTime)
            xrResources.leftDepth.update(depthMapBuffers)
            xrResources.rightDepth.update(depthMapBuffers)
        }

        if (eyeTrackingMode != EyeTrackingMode.DISABLED) {
            updateEyes(xrTime)
        }

        lastUpdateXrTime = xrTime
    }

    override val imageDatabaseMaxLoadedImageCount: Int
        get() = nativeGetImageDatabaseMaxLoadedImageCount()

    override val isPhysicalSizeEstimationSupported: Boolean
        get() = nativeIsPhysicalSizeEstimationSupported()

    override val isQrCodeSizeEstimationSupported: Boolean
        get() = nativeIsQrCodeSizeEstimationSupported()

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
            // TODO(b/508726641) - Restore to a check that planeTypeInt is non-negative once
            // xrGetTrackablePlaneANDROID issue is resolved.
            if (planeTypeInt < 0) {
                continue
            }

            val trackable =
                OpenXrPlane(plane, Plane.Type.fromOpenXrType(planeTypeInt), timeSource, xrResources)
            xrResources.addTrackable(plane, trackable)
            xrResources.addUpdatable(trackable as Updatable)
        }
    }

    internal fun updateAugmentedImages(xrTime: Long) {
        val augmentedImages = nativeGetAugmentedImages()
        // Add new images to the list of trackables.
        for (augmentedImage in augmentedImages) {
            if (xrResources.trackablesMap.containsKey(augmentedImage)) continue

            val trackable = OpenXrAugmentedImage(augmentedImage)
            xrResources.addTrackable(augmentedImage, trackable)
            xrResources.addUpdatable(trackable as Updatable)
        }

        // Remove images that are no longer tracked.
        for ((key, value) in xrResources.trackablesMap.toMap()) {
            if (value is OpenXrAugmentedImage && !augmentedImages.contains(key)) {
                xrResources.removeUpdatable(value as Updatable)
                xrResources.removeTrackable(key)
            }
        }
    }

    internal fun updateQrCode(xrTime: Long) {
        val qrCodes = nativeGetQrCodes()
        // Add new QR codes to the list of trackables.
        for (qrCode in qrCodes) {
            if (xrResources.trackablesMap.containsKey(qrCode)) continue

            val trackable = OpenXrQrCode(qrCode)
            xrResources.addTrackable(qrCode, trackable)
            xrResources.addUpdatable(trackable as Updatable)
        }

        // Remove QR codes that are no longer tracked.
        for ((key, value) in xrResources.trackablesMap.toMap()) {
            if (value is OpenXrQrCode && !qrCodes.contains(key)) {
                xrResources.removeUpdatable(value as Updatable)
                xrResources.removeTrackable(key)
            }
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

    internal fun updateSpatialAnnotations(xrTime: Long) {
        if (xrResources.annotationConfigs.isEmpty()) return

        for ((id, config) in xrResources.annotationConfigs.entries) {
            var trackable = xrResources.trackablesMap[config.handle] as? OpenXrSpatialAnnotation
            if (trackable == null) {
                trackable = OpenXrSpatialAnnotation(config.handle, id, config.alignment)
                xrResources.addTrackable(config.handle, trackable)
                xrResources.addUpdatable(trackable as Updatable)
            }
        }
    }

    override fun startSpatialAnnotationTracking(
        imageBuffer: ByteBuffer,
        imageSize: IntSize2d,
        rowStride: Int,
        format: SpatialAnnotationImageFormat,
        alignment: SpatialAnnotationQuadAlignment,
        quads: Map<SpatialAnnotationId, Quad>,
        timestampNanos: Long,
    ) {
        val keys = quads.keys.toList()
        val quadsExtents =
            quads.values
                .flatMap { quad ->
                    val uL = quad.upperLeft
                    val uR = quad.upperRight
                    val lR = quad.lowerRight
                    val lL = quad.lowerLeft
                    listOf(uL.x, uL.y, uR.x, uR.y, lR.x, lR.y, lL.x, lL.y)
                }
                .toFloatArray()

        // TODO(b/559357621): Add RGBA support in the native code.
        // TODO(b/560289000): Prove imageBuffer.isDirect.
        try {
            // TODO(b/560289167): Investigate coroutines teardown issue.
            nativeStartSpatialAnnotationTracking(
                imageBuffer,
                imageSize.width,
                imageSize.height,
                rowStride,
                format.value,
                alignment.value,
                quadsExtents,
                timestampNanos,
            ) { handles ->
                if (handles?.size == keys.size) {
                    keys.forEachIndexed { index, key ->
                        xrResources.addAnnotationHandle(key, handles[index], alignment)
                    }
                }
            }
        } catch (_: UnsatisfiedLinkError) {
            // Native method is not linked in JVM host unit tests.
        }
    }

    @ExperimentalSpatialAnnotationsApi
    override fun stopSpatialAnnotationTracking(ids: List<SpatialAnnotationId>) {
        // If no IDs are provided, default to stopping all active spatial annotations (used when
        // stopping tracking for all annotations, disabling tracking mode, or during teardown).
        // TODO(b/560286118): Change TrackingState to STOPPED for all annotations when stopping all
        // annotations.
        val targetIds = ids.ifEmpty { xrResources.annotationConfigs.keys.toList() }
        val handlesToStop = targetIds.mapNotNull { xrResources.removeAnnotationHandle(it) }
        handlesToStop
            .mapNotNull { xrResources.removeTrackable(it) as? Updatable }
            .forEach(xrResources::removeUpdatable)
        if (handlesToStop.isNotEmpty()) {
            try {
                nativeStopSpatialAnnotationTracking(handlesToStop.toLongArray())
            } catch (_: UnsatisfiedLinkError) {
                // Native method is not linked in JVM host unit tests.
            }
        }
    }

    @OptIn(ExperimentalSpatialAnnotationsApi::class)
    internal fun clear() {
        stopSpatialAnnotationTracking(emptyList())
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
            -2L -> throw AnchorRuntimeFailureException() // kErrorRuntimeFailure
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

    private external fun nativeGetAugmentedImages(): LongArray

    private external fun nativeGetImageDatabaseMaxLoadedImageCount(): Int

    private external fun nativeIsPhysicalSizeEstimationSupported(): Boolean

    private external fun nativeGetQrCodes(): LongArray

    private external fun nativeIsQrCodeSizeEstimationSupported(): Boolean

    private external fun nativeStartSpatialAnnotationTracking(
        imageBuffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        format: Int,
        alignment: Int,
        quadsExtents: FloatArray,
        timestampNanos: Long,
        callback: (LongArray?) -> Unit,
    )

    private external fun nativeStopSpatialAnnotationTracking(handles: LongArray)
}
