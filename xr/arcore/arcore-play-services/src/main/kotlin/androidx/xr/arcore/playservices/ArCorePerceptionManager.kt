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
import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityNetworkError
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorNotTrackingException
import androidx.xr.runtime.internal.ArDevice
import androidx.xr.runtime.internal.DepthMap
import androidx.xr.runtime.internal.Face
import androidx.xr.runtime.internal.Hand
import androidx.xr.runtime.internal.HitResult
import androidx.xr.runtime.internal.PerceptionManager
import androidx.xr.runtime.internal.Trackable
import androidx.xr.runtime.internal.ViewCamera
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import com.google.ar.core.Frame
import com.google.ar.core.Plane as ARCore1xPlane
import com.google.ar.core.Session
import com.google.ar.core.VpsAvailability as ARCore1xVpsAvailability
import com.google.ar.core.VpsAvailabilityFuture
import com.google.ar.core.exceptions.NotTrackingException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implementation of the perception capabilities of a runtime using ARCore.
 *
 * @property timeSource The time source to use for the perception manager.
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

    private var displayRotation = Surface.ROTATION_0
    private var displayWidth = 0
    private var displayHeight = 0
    internal var displayChanged: Boolean = false

    /** The latest [Frame] returned by the underlying [Session]. */
    @UnsupportedArCoreCompatApi public fun lastFrame(): Frame = _latestFrame

    internal fun lastFrame(value: Frame) {
        _latestFrame = value
    }

    /**
     * Creates an anchor in the scene.
     *
     * This method calls the [Session.createAnchor] method.
     *
     * @param pose The pose of the anchor.
     * @return The created anchor.
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
     * @param ray The ray to perform the hit test against.
     * @return The list of hit results.
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
     * Loads an anchor from the given native pointer.
     *
     * This method throws [NotImplementedError] because ARCore does not support native pointers.
     */
    override fun loadAnchorFromNativePointer(nativePointer: Long): Anchor {
        throw NotImplementedError("Native pointers are not supported by ARCore.")
    }

    /**
     * Unpersists an anchor with the given UUID.
     *
     * This method throws [NotImplementedError] because ARCore does not support anchor persistence.
     */
    override fun unpersistAnchor(uuid: UUID) {
        throw NotImplementedError("Anchor persistence is currently not supported by ARCore.")
    }

    /** Gets the VPS availability at the given location. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult {
        return suspendCancellableCoroutine { continuation ->
            val future: VpsAvailabilityFuture =
                session.checkVpsAvailabilityAsync(latitude, longitude) {
                    arCoreVpsAvailability: ARCore1xVpsAvailability? ->
                    val vpsResult =
                        when (arCoreVpsAvailability) {
                            ARCore1xVpsAvailability.AVAILABLE -> VpsAvailabilityAvailable()
                            ARCore1xVpsAvailability.ERROR_INTERNAL -> VpsAvailabilityErrorInternal()
                            ARCore1xVpsAvailability.ERROR_NETWORK_CONNECTION ->
                                VpsAvailabilityNetworkError()
                            ARCore1xVpsAvailability.ERROR_NOT_AUTHORIZED ->
                                VpsAvailabilityNotAuthorized()
                            ARCore1xVpsAvailability.ERROR_RESOURCE_EXHAUSTED ->
                                VpsAvailabilityResourceExhausted()
                            ARCore1xVpsAvailability.UNAVAILABLE -> VpsAvailabilityUnavailable()
                            else -> VpsAvailabilityErrorInternal()
                        }
                    continuation.resume(vpsResult)
                }

            continuation.invokeOnCancellation {
                // No cleanup is necessary, so we don't care if it is completed or not.
                val unused = future.cancel()
            }
        }
    }

    override val trackables: Collection<Trackable> = xrResources.trackables.values

    /**
     * Returns the left hand.
     *
     * ARCore does not support hand tracking, so this property is always null.
     */
    override val leftHand: Hand? = null

    /**
     * Returns the right hand.
     *
     * ARCore does not support hand tracking, so this property is always null.
     */
    override val rightHand: Hand? = null

    /**
     * Returns the face
     *
     * ARCore does not support face tracking, so this property is always null.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) override val userFace: Face? = null

    /** Returns the [Earth] instance. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val earth: ArCoreEarth = xrResources.earth

    /** Returns the [ArDevice] instance. */
    override val arDevice: ArDevice
        get() = throw NotImplementedError("Not implemented on mobile runtime.")

    /** Returns a list of [ViewCamera] objects. */
    override val viewCameras: List<ViewCamera>
        get() = throw NotImplementedError("Not implemented on mobile runtime.")

    /** Returns a list of [DepthMap] objects. */
    override val depthMaps: List<DepthMap>
        get() = throw NotImplementedError("Not implemented on mobile runtime")

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

        earth.update(session)
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
}
