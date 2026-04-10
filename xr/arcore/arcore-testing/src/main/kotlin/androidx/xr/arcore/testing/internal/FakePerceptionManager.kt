/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.testing.internal

import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorInvalidUuidException
import androidx.xr.arcore.runtime.HitResult
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.util.UUID

internal class FakePerceptionManager() : PerceptionManager, AnchorHolder {

    private val fakeArDevice = FakeRuntimeArDevice()
    private val fakeLeftEye = FakeRuntimeEye()
    private val fakeRightEye = FakeRuntimeEye()
    private val fakeLeftHand = FakeRuntimeHand()
    private val fakeRightHand = FakeRuntimeHand()
    private val fakeLeftRenderViewpoint = FakeRuntimeRenderViewpoint()
    private val fakeRightRenderViewpoint = FakeRuntimeRenderViewpoint()
    private val fakeMonoRenderViewpoint = FakeRuntimeRenderViewpoint()
    private val fakeUserFace = FakeRuntimeFace()
    private val fakeGeospatial = FakeRuntimeGeospatial()
    private val fakeLeftDepthMap = FakeRuntimeDepthMap()
    private val fakeRightDepthMap = FakeRuntimeDepthMap()
    private val fakeMonoDepthMap = FakeRuntimeDepthMap()

    override val trackables: MutableList<Trackable> = mutableListOf()

    override val leftEye: androidx.xr.arcore.runtime.Eye
        get() = fakeLeftEye

    override val rightEye: androidx.xr.arcore.runtime.Eye
        get() = fakeRightEye

    override val leftHand: androidx.xr.arcore.runtime.Hand
        get() = fakeLeftHand

    override val rightHand: androidx.xr.arcore.runtime.Hand
        get() = fakeRightHand

    override val arDevice: androidx.xr.arcore.runtime.ArDevice
        get() = fakeArDevice

    override val leftRenderViewpoint: androidx.xr.arcore.runtime.RenderViewpoint
        get() = fakeLeftRenderViewpoint

    override val rightRenderViewpoint: androidx.xr.arcore.runtime.RenderViewpoint
        get() = fakeRightRenderViewpoint

    override val monoRenderViewpoint: androidx.xr.arcore.runtime.RenderViewpoint
        get() = fakeMonoRenderViewpoint

    override val userFace: androidx.xr.arcore.runtime.Face
        get() = fakeUserFace

    override val geospatial: androidx.xr.arcore.runtime.Geospatial
        get() = fakeGeospatial

    override val leftDepthMap: androidx.xr.arcore.runtime.DepthMap
        get() = fakeLeftDepthMap

    override val rightDepthMap: androidx.xr.arcore.runtime.DepthMap
        get() = fakeRightDepthMap

    override val monoDepthMap: androidx.xr.arcore.runtime.DepthMap
        get() = fakeMonoDepthMap

    internal val persistedAnchorUUIDs: MutableMap<UUID, Pose> = mutableMapOf()
    internal val anchors: MutableList<FakeRuntimeAnchor> = mutableListOf()
    internal var isCameraTracking: Boolean = true

    override fun createAnchor(pose: Pose): Anchor {
        // TODO: b/349862231 - Modify it once detach is implemented.
        val anchor =
            FakeRuntimeAnchor(pose, anchorHolder = this, isTrackingAvailable = isCameraTracking)
        anchors.add(anchor)
        return anchor
    }

    override fun hitTest(ray: Ray): List<HitResult> {
        val results: MutableList<HitResult> = mutableListOf()
        trackables.forEach {
            val centerPose =
                when (it) {
                    is FakeRuntimePlane -> it.centerPose
                    is FakeRuntimeAugmentedObject -> it.centerPose
                    is FakeRuntimeFace -> it.centerPose
                    else -> throw IllegalStateException("centerPose of $it is unknown")
                }
            val toTrackable = centerPose.translation - ray.origin
            if (toTrackable == Vector3.Zero) {
                results.add(HitResult(0f, centerPose, it))
            } else {
                val crossProduct = ray.direction.cross(toTrackable)
                val dotProduct = ray.direction.dot(toTrackable)
                if (crossProduct == Vector3.Zero && dotProduct >= 0.0f) {
                    results.add(HitResult(toTrackable.length, centerPose, it))
                }
            }
        }
        return results.toList()
    }

    override fun getPersistedAnchorUuids(): List<UUID> = persistedAnchorUUIDs.keys.toList()

    override fun loadAnchor(uuid: UUID): Anchor {
        if (!persistedAnchorUUIDs.contains(uuid)) {
            throw AnchorInvalidUuidException()
        }
        val anchor =
            FakeRuntimeAnchor(
                pose = persistedAnchorUUIDs[uuid]!!,
                isTrackingAvailable = isCameraTracking,
            )
        anchor.anchorHolder = this
        return anchor
    }

    override fun unpersistAnchor(uuid: UUID) {
        if (!persistedAnchorUUIDs.contains(uuid)) {
            throw AnchorInvalidUuidException()
        }
        persistedAnchorUUIDs.remove(uuid)
    }

    override fun onAnchorPersisted(anchor: Anchor) {
        require(anchor.uuid != null)
        persistedAnchorUUIDs[anchor.uuid!!] = anchor.pose
    }

    override fun detachAnchor(anchor: Anchor) {
        require(anchor is FakeRuntimeAnchor)
        anchor.trackingState = TrackingState.STOPPED
        anchors.remove(anchor)
        anchor.uuid?.let { persistedAnchorUUIDs.remove(it) }
    }

    @OptIn(PreviewSpatialApi::class)
    internal fun updateTrackingStates(config: Config) {
        fakeArDevice.trackingState =
            when (config.deviceTracking) {
                DeviceTrackingMode.SPATIAL_LAST_KNOWN -> TrackingState.TRACKING
                DeviceTrackingMode.INERTIAL_LAST_KNOWN -> TrackingState.TRACKING_DEGRADED
                else -> TrackingState.PAUSED
            }
        if (config.planeTracking == PlaneTrackingMode.DISABLED) {
            trackables.filterIsInstance<FakeRuntimePlane>().forEach {
                it.trackingState = TrackingState.STOPPED
            }
        }
        if (config.faceTracking != FaceTrackingMode.MESHES) {
            trackables.filterIsInstance<FakeRuntimeFace>().forEach {
                it.trackingState = TrackingState.STOPPED
            }
        }
        trackables
            .filterIsInstance<FakeRuntimeAugmentedObject>()
            .filter { !config.augmentedObjectCategories.contains(it.category) }
            .forEach { it.trackingState = TrackingState.STOPPED }

        if (config.faceTracking != FaceTrackingMode.BLEND_SHAPES) {
            fakeUserFace.trackingState = TrackingState.STOPPED
        }
        if (config.handTracking == HandTrackingMode.DISABLED) {
            fakeLeftHand.trackingState = TrackingState.STOPPED
            fakeRightHand.trackingState = TrackingState.STOPPED
        }
        if (config.eyeTracking == EyeTrackingMode.DISABLED) {
            fakeLeftEye.trackingState = TrackingState.STOPPED
            fakeRightEye.trackingState = TrackingState.STOPPED
        }
        if (config.geospatial == GeospatialMode.DISABLED) {
            fakeGeospatial.state = androidx.xr.arcore.runtime.Geospatial.State.NOT_RUNNING
        }
    }
}
