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

import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.DepthMap
import androidx.xr.arcore.runtime.Eye
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Hand
import androidx.xr.arcore.runtime.HitResult
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.runtime.CameraFacingDirection
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.internal.UnsupportedDeviceException
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import com.google.ar.core.AugmentedFace as ARCore1xAugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Frame
import com.google.ar.core.Plane as ARCore1xPlane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.NotTrackingException
import java.util.UUID
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic

/**
 * Implementation of the perception capabilities of a runtime using ARCore.
 *
 * @property timeSource the time source to use for the perception manager
 * @property trackables the collection of [Trackable] objects
 * @property leftEye the left eye, or null if not available
 * @property rightEye the right eye, or null if not available
 * @property leftHand the left hand, or null if not available
 * @property rightHand the right hand, or null if not available
 * @property userFace the user's face, or null if not available
 * @property geospatial the [ArCoreEarth] instance
 * @property arDevice the [ArCoreDevice] instance
 * @property leftRenderViewpoint the left [RenderViewpoint], or null if not available
 * @property rightRenderViewpoint the right [RenderViewpoint], or null if not available
 * @property monoRenderViewpoint the mono [RenderViewpoint], or null if not available
 * @property leftDepthMap the left [DepthMap], or null if not available
 * @property rightDepthMap the right [DepthMap], or null if not available
 * @property monoDepthMap the mono [DepthMap], or null if not available
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCorePerceptionManager
internal constructor(private val timeSource: ArCoreTimeSource) : PerceptionManager {

    /* Synchronizes access to the [_latestFrame] */
    internal val frameLock = Any()
    internal lateinit var _latestFrame: Frame
    internal var lastFrameTimestampNs: Long = -1L
    internal lateinit var session: Session

    private val timeProvider: TimeSource.WithComparableMarks = Monotonic
    private var lastFrameTimeMark: ComparableTimeMark? = null

    internal fun timeSinceLastFrame(): Duration = lastFrameTimeMark?.elapsedNow() ?: Duration.ZERO

    private val xrResources: XrResources = XrResources()
    internal var depthEstimationMode = DepthEstimationMode.DISABLED

    private var displayRotation = Surface.ROTATION_0
    private var displayWidth = 0
    private var displayHeight = 0
    internal var displayChanged: Boolean = false

    /**
     * The latest [Frame] returned by the underlying [Session].
     *
     * @return the latest [Frame]
     * @sample androidx.xr.arcore.samples.getARCoreFrame
     */
    @UnsupportedArCoreCompatApi public fun lastFrame(): Frame = _latestFrame

    internal fun lastFrame(value: Frame) {
        _latestFrame = value
    }

    internal val usingFrontFacingCamera: Boolean
        get() =
            if (::session.isInitialized) {
                val arCoreCameraConfig: CameraConfig? = session.cameraConfig
                arCoreCameraConfig?.facingDirection == CameraConfig.FacingDirection.FRONT
            } else false

    /**
     * Creates an anchor in the scene.
     *
     * This method calls the [Session.createAnchor] method.
     *
     * @param pose the [Pose] of the anchor
     * @return the created [Anchor]
     */
    override fun createAnchor(pose: Pose): Anchor {
        try {
            val arCoreAnchor = session.createAnchor(pose.toARCorePose())
            val anchor = ArCoreAnchor(arCoreAnchor)
            return anchor
        } catch (e: NotTrackingException) {
            throw AnchorNotTrackingException(e)
        }
    }

    /**
     * Performs a hit test against the scene.
     *
     * This method calls the [Frame.hitTest] method.
     *
     * @param ray the [Ray] to perform the hit test against
     * @return the list of [HitResult] objects
     */
    override fun hitTest(ray: Ray): List<HitResult> {
        val origin = floatArrayOf(ray.origin.x, ray.origin.y, ray.origin.z)
        val direction = floatArrayOf(ray.direction.x, ray.direction.y, ray.direction.z)
        return _latestFrame
            .hitTest(origin, /* originOffset= */ 0, direction, /* directionOffset= */ 0)
            .filter { it.trackable in xrResources.trackables }
            .map {
                HitResult(
                    it.distance,
                    it.hitPose.toRuntimePose(),
                    xrResources.trackables[it.trackable]!!,
                )
            }
    }

    /**
     * Returns the UUIDs of all persisted anchors.
     *
     * This method throws [NotImplementedError] because ARCore does not support anchor persistence.
     */
    override fun getPersistedAnchorUuids(): List<UUID> {
        throw NotImplementedError("Anchor persistence is currently not supported by ARCore.")
    }

    /**
     * Loads an anchor from the given UUID.
     *
     * This method throws [NotImplementedError] because ARCore does not support anchor persistence.
     */
    override fun loadAnchor(uuid: UUID): Anchor {
        throw NotImplementedError("Anchor persistence is currently not supported by ARCore.")
    }

    /**
     * Unpersists an anchor with the given UUID.
     *
     * This method throws [NotImplementedError] because ARCore does not support anchor persistence.
     */
    override fun unpersistAnchor(uuid: UUID) {
        throw NotImplementedError("Anchor persistence is currently not supported by ARCore.")
    }

    override val trackables: Collection<Trackable> = xrResources.trackables.values

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) override val leftEye: Eye? = null

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) override val rightEye: Eye? = null

    override val leftHand: Hand? = null

    override val rightHand: Hand? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) override val userFace: Face? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val geospatial: ArCoreEarth = xrResources.geospatial

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val arDevice: ArCoreDevice = xrResources.arDevice

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val leftRenderViewpoint: RenderViewpoint? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val rightRenderViewpoint: RenderViewpoint? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val monoRenderViewpoint: RenderViewpoint? = null

    override val leftDepthMap: DepthMap? = null

    override val rightDepthMap: DepthMap? = null

    override val monoDepthMap: DepthMap?
        get() = xrResources.depthMap

    /**
     * Updates the perception manager.
     *
     * Sets the display geometry of the underlying [Session] if the display has changed. Grabs the
     * latest [Frame] from the underlying [Session], and if new, updates the internal state of the
     * perception manager.
     */
    internal fun update() {
        if (displayChanged) {
            session.setDisplayGeometry(displayRotation, displayWidth, displayHeight)
        }

        synchronized(frameLock) {
            _latestFrame = session.update()
            if (lastFrameTimestampNs == _latestFrame.timestamp) {
                return
            }
            lastFrameTimestampNs = _latestFrame.timestamp
        }
        lastFrameTimeMark = timeProvider.markNow()

        timeSource.update(lastFrameTimestampNs)

        val planes = _latestFrame.getUpdatedTrackables(ARCore1xPlane::class.java)
        planes.forEach { xrResources.addTrackable(it, ArCorePlane(it, xrResources)) }

        val augmentedFaces = session.getAllTrackables(ARCore1xAugmentedFace::class.java)
        // Don't retain any AugmentedFaces that the ArCore Session is no longer tracking
        xrResources.trackables
            .filter { it.value is ArCoreFace }
            .keys
            .forEach {
                if (!augmentedFaces.contains(it)) {
                    xrResources.removeTrackable(it)
                }
            }
        augmentedFaces.forEach { xrResources.addTrackable(it, ArCoreFace(it)) }

        arDevice.update(_latestFrame)

        if (depthEstimationMode != DepthEstimationMode.DISABLED) {
            xrResources.depthMap.update(_latestFrame)
        }

        geospatial.update(session)
    }

    /**
     * Clears any internal state of the perception manager.
     *
     * Currently, this method only clears the [xrResources] instance.
     */
    internal fun clear() {
        xrResources.clear()
    }

    public fun setDisplayRotation(rotation: Int, width: Int, height: Int) {
        if (rotation != displayRotation || width != displayWidth || height != displayHeight) {
            displayRotation = rotation
            displayWidth = width
            displayHeight = height
            displayChanged = true
        }
    }

    /**
     * Sets the Depth Estimation Mode for the Perception Manager and the [XrResources.depthMap] of
     * [xrResources]
     *
     * @param depthMode the desired [DepthEstimationMode]
     */
    public fun setDepthEstimationMode(depthMode: DepthEstimationMode) {
        depthEstimationMode = depthMode
        xrResources.depthMap.updateDepthEstimationMode(depthMode)
    }

    /**
     * Clears any lingering resources within [xrResources].
     *
     * @see ArCoreDepthMap.dispose
     */
    public fun dispose() {
        xrResources.depthMap.dispose()
    }

    internal fun setCameraFacingDirection(facingDirection: CameraFacingDirection) {
        val arCoreFacingDirection =
            when (facingDirection) {
                CameraFacingDirection.USER -> CameraConfig.FacingDirection.FRONT
                CameraFacingDirection.WORLD -> CameraConfig.FacingDirection.BACK
                else ->
                    throw IllegalArgumentException(
                        "Unsupported CameraFacingDirection ${facingDirection}."
                    )
            }
        val filter = CameraConfigFilter(session)
        filter.facingDirection = arCoreFacingDirection
        val supportedConfigs = session.getSupportedCameraConfigs(filter)
        if (supportedConfigs.isEmpty()) {
            throw UnsupportedDeviceException()
        }
        // Element 0 contains the best match
        session.cameraConfig = supportedConfigs[0]
    }
}
