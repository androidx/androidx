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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.toDegrees
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.SpatialModeChangeListener
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.function.Consumer
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.Vec3
import com.android.extensions.xr.space.Bounds
import com.android.extensions.xr.space.SpatialState
import java.util.Collections
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Immutable snapshot of the scale and orientation compensation applied to an [ActivitySpaceImpl].
 *
 * Captures one transform observation so coordinate conversions do not see intermediate updates that
 * happen during an origin change.
 *
 * @property sceneParentScale positive uniform scale factor of the scene parent node.
 * @property rotation gravity-aligned orientation applied to the activity space root node.
 */
internal class OriginCompensation(val sceneParentScale: Float, val rotation: Quaternion) {
    /** Converts [sceneParentSpacePosition] from scene parent space into activity space. */
    fun toActivitySpace(sceneParentSpacePosition: Vector3): Vector3 =
        (rotation.inverse * sceneParentSpacePosition) * sceneParentScale

    /**
     * Converts a direction vector [sceneParentSpaceDirection] from scene parent space into a
     * unit-length activity space direction vector.
     */
    fun toActivitySpaceDirection(sceneParentSpaceDirection: Vector3): Vector3 =
        (rotation.inverse * sceneParentSpaceDirection).toNormalized()

    /**
     * Converts a surface normal [sceneParentSpaceNormal] from scene parent space into a unit-length
     * activity space normal vector.
     */
    fun toActivitySpaceNormal(sceneParentSpaceNormal: Vector3): Vector3 =
        (rotation.inverse * sceneParentSpaceNormal).toNormalized()

    /** Converts [activitySpacePosition] from activity space into scene parent space. */
    fun toSceneParentSpace(activitySpacePosition: Vector3): Vector3 =
        (rotation * activitySpacePosition) / sceneParentScale

    /**
     * Converts a direction vector [activitySpaceDirection] from activity space into a unit-length
     * scene parent direction vector.
     */
    fun toSceneParentSpaceDirection(activitySpaceDirection: Vector3): Vector3 =
        (rotation * activitySpaceDirection).toNormalized()

    companion object {
        val IDENTITY = OriginCompensation(1.0f, Quaternion.Identity)
    }
}

private const val MIN_SCENE_PARENT_SCALE = 1e-4f
private const val MAX_SCENE_PARENT_SCALE_RELATIVE_ERROR = 1e-3f

/**
 * Checks whether [matrix] can drive activity space compensation.
 *
 * Accepts finite TRS transforms whose scale is positive, above [MIN_SCENE_PARENT_SCALE], and
 * uniform within [MAX_SCENE_PARENT_SCALE_RELATIVE_ERROR].
 */
internal fun isValidSceneParentTransform(matrix: Matrix4): Boolean {
    val data = matrix.data
    for (i in 0 until 16) {
        if (!data[i].isFinite()) return false
    }
    if (!matrix.isTrs) return false

    val scale = matrix.scale
    val minScale = minOf(scale.x, scale.y, scale.z)
    val maxScale = maxOf(scale.x, scale.y, scale.z)

    if (minScale < MIN_SCENE_PARENT_SCALE) {
        return false
    }
    val relativeError = (maxScale - minScale) / maxScale
    return relativeError <= MAX_SCENE_PARENT_SCALE_RELATIVE_ERROR
}

/**
 * Extracts a gravity-aligned yaw rotation, avoiding gimbal lock near vertical poles (pitch = ±90°).
 *
 * Standard Euler yaw (`eulerAngles.y`) becomes unstable when looking nearly straight up or down
 * because the horizontal projection of the Forward (-Z) vector approaches zero. To maintain
 * stability:
 * - Normal pitch: Yaw is computed from the horizontal projection of Forward (-Z).
 * - Vertical poles: Yaw smoothly blends into being derived from the device's Up (+Y) vector, which
 *   remains well-behaved horizontally at extreme pitch angles.
 */
internal fun gravityAlignedYawRotation(rotation: Quaternion): Quaternion {
    val polarBlendThreshold = 0.15f // ~8.6 degrees from vertical poles
    val exactPoleEpsilon = 1e-4f

    val forward = rotation * Vector3.Forward
    val r = kotlin.math.hypot(forward.x, forward.z)

    // In OpenXR/SceneCore, -Z is forward. We measure angle around Y relative to negative Z axis.
    val angleFwd = kotlin.math.atan2(-forward.x, -forward.z)

    val yawRad =
        if (r >= polarBlendThreshold) {
            // Standard pitch: Horizontal forward vector magnitude is large enough for stable yaw
            angleFwd
        } else {
            // Compute Up vector transforms when approaching vertical poles (>81.4 deg pitch)
            val up = rotation * Vector3.Up
            val upHorizontal = kotlin.math.hypot(up.x, up.z)
            if (upHorizontal < exactPoleEpsilon) {
                angleFwd
            } else {
                // When pointing straight up (forward.y > 0), the top (+Y) of the device tilts
                // backwards across the pole, pointing opposite to facing direction. Invert sign to
                // align with forward yaw.
                val upSign = if (forward.y > 0f) -1f else 1f
                val angleUp = kotlin.math.atan2(-up.x * upSign, -up.z * upSign)

                if (r < exactPoleEpsilon) {
                    // Vertical poles: Forward vector is vertical; rely entirely on the horizontal
                    // up vector
                    angleUp
                } else {
                    // Transition zone: Smoothly blend forward and up yaw angles to prevent heading
                    // jumps
                    val u = r / polarBlendThreshold
                    val smoothstep = 3f * u * u - 2f * u * u * u // Hermite curve (3u^2 - 2u^3)

                    val pi = kotlin.math.PI.toFloat()
                    val twoPi = 2f * pi
                    var delta = angleFwd - angleUp

                    // Wrap angle difference into [-pi, pi) to ensure shortest-arc interpolation
                    delta = ((delta + pi) % twoPi + twoPi) % twoPi - pi

                    // At exact 180-degree (antipodal) roll differences, float noise can flip the
                    // sign between -pi and +pi. Snap to +pi within epsilon to guarantee
                    // deterministic interpolation direction.
                    if (kotlin.math.abs(kotlin.math.abs(delta) - pi) < exactPoleEpsilon) {
                        delta = pi
                    }
                    angleUp + smoothstep * delta
                }
            }
        }

    return Quaternion.fromEulerAngles(0f, toDegrees(yawRad), 0f)
}

/**
 * Implementation of SceneCore's ActivitySpace.
 *
 * <p>This Entity represents the origin of the Scene, and is positioned by the system.
 */
@SuppressWarnings("HidingField") // super class AndroidXrEntity has mEntityManager
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/452961674): Review RestrictTo annotations in SceneCore.
public class ActivitySpaceImpl(
    taskNode: Node,
    activity: Activity,
    extensions: XrExtensions,
    sceneNodeRegistry: SceneNodeRegistry,
    private val spatialStateProvider: Supplier<SpatialState>,
    executor: ScheduledExecutorService,
    private val unscaledGravityAlignedActivitySpace: Boolean = true,
) :
    SystemSpaceEntityImpl(activity, taskNode, extensions, sceneNodeRegistry, executor),
    ActivitySpace {

    private val boundsListeners =
        Collections.synchronizedSet(HashSet<ActivitySpace.OnBoundsChangedListener>())
    private val _bounds = AtomicReference<Dimensions>()
    // Spatial mode change handler will be invoked on every update to activity space origin we
    // receive from the node transform listener.
    private var spatialModeChangeListener: SpatialModeChangeListener? = null
    private val cachedRecommendedContentBox = AtomicReference<BoundingBox>(null)

    // The bounds are kept in sync with the Extensions in the onBoundsChangedEvent callback. We
    // only invoke getSpatialState if they've never been set.
    override val bounds: Dimensions
        get() = _bounds.updateAndGet { oldBounds ->
            if (oldBounds == null) {
                val bounds = spatialStateProvider.get().bounds
                Dimensions(bounds.width, bounds.height, bounds.depth)
            } else {
                oldBounds
            }
        }

    public val poseInPerceptionSpace: Pose
        get() {
            val perceptionSpaceScenePose =
                sceneNodeRegistry
                    .getSystemSpaceScenePoseOfType(PerceptionSpaceScenePose::class.java)
                    .single()
            return transformPoseTo(Pose(), perceptionSpaceScenePose)
        }

    /** Returns the identity pose since this entity defines the origin of the activity space. */
    override val activitySpacePose: Pose
        get() = Pose()

    override val activitySpaceScale: Vector3
        get() = Vector3(1.0f, 1.0f, 1.0f)

    override var parent: Entity?
        get() = super.parent
        set(_) = throw UnsupportedOperationException("Cannot set 'parent' on an ActivitySpace.")

    /**
     * Return a recommended box for content to be placed in when in Full Space Mode.
     *
     * <p>The box is relative to the ActivitySpace's coordinate system. It is not scaled by the
     * ActivitySpace's transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     *
     * @return a [BoundingBox] sized to place content in.
     */
    override val recommendedContentBoxInFullSpace: BoundingBox
        get() = cachedRecommendedContentBox.updateAndGet { currentBox ->
            currentBox
                ?: run {
                    val recommendedBox = extensions.recommendedContentBoxInFullSpace
                    BoundingBox.fromMinMax(
                        Vector3(
                            recommendedBox.min.x,
                            recommendedBox.min.y,
                            recommendedBox.min.z,
                        ),
                        Vector3(
                            recommendedBox.max.x,
                            recommendedBox.max.y,
                            recommendedBox.max.z,
                        ),
                    )
                }
        }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "ActivitySpace is a root space and it does not have a parent."
                )
            Space.ACTIVITY -> activitySpacePose
            Space.REAL_WORLD -> poseInPerceptionSpace
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'scale' on an ActivitySpace.")
    }

    override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "ActivitySpace is a root space and it does not have a parent."
                )
            Space.ACTIVITY -> activitySpaceScale
            Space.REAL_WORLD -> worldSpaceScale
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'pose' on an ActivitySpace.")
    }

    @SuppressWarnings("ObjectToString")
    override fun dispose() {
        super.dispose()
    }

    internal val originCompensation = AtomicReference(OriginCompensation.IDENTITY)
    private val lastSceneParentTransform = AtomicReference<Matrix4?>(null)

    internal val sceneParentScaleAbs: Vector3
        get() {
            val scale = originCompensation.get().sceneParentScale
            return Vector3(scale, scale, scale)
        }

    /**
     * Handles the updates to scene core root transform.
     * <pre>
     * Hierarchy:
     * Platform Reference Space Origin (OpenXR Unbounded in XROS)
     * └── Scene Parent Node (Intermediate system-managed node)
     * └── Scene Root Node (ActivitySpace Node)
     *
     * Transform Flow:
     * 1. The system updates the transform of the 'Scene Parent Node' when in HOME_SPACE mode or
     * during a recenter operation.
     * 2. The 'Scene Root Node' becomes a child of 'Scene Parent Node' and inherits its transform
     * when activity enters FULL_SPACE_MANAGED mode.
     * </pre>
     * <p>By inverting the inherited scale and roll and pitch rotations of the scene parent
     * transform, SceneCore effectively re-orients the ActivitySpace to be unscaled and
     * gravity-aligned like its grandparent platform unbounded space (OpenXR Unbounded in XROS),
     * while preserving its yaw rotation (i.e. facing user direction).
     *
     * <p>To maintain continuity when entering FSM, SceneCore provides the original rotation and
     * scale of the scene parent transform via the onSpatialModeChanged callback. This ensures FSM
     * continuity when spatial modes change.
     *
     * @param newTransform New scene parent transform relative to platform reference space (OpenXR
     *   Unbounded in XROS).
     */
    public fun handleOriginUpdate(newTransform: Matrix4) {
        if (!isValidSceneParentTransform(newTransform)) {
            return
        }
        if (lastSceneParentTransform.get() == newTransform) {
            return
        }
        var activitySpaceRotation = Quaternion.Identity
        var sceneParentScale = Vector3.One
        if (unscaledGravityAlignedActivitySpace) {
            sceneParentScale = newTransform.scale
            val scaleFactor = sceneParentScale.x
            val scaleFactorInv = 1.0f / scaleFactor
            activitySpaceRotation = newTransform.rotation
            val yawRotation = gravityAlignedYawRotation(activitySpaceRotation)
            val gravityAlignedRotation = activitySpaceRotation.inverse * yawRotation
            val newOriginCompensation = OriginCompensation(scaleFactor, gravityAlignedRotation)
            extensions.createNodeTransaction().use { transaction ->
                transaction
                    .setScale(
                        getNode(),
                        scaleFactorInv,
                        scaleFactorInv,
                        scaleFactorInv,
                    )
                    .setOrientation(
                        getNode(),
                        gravityAlignedRotation.x,
                        gravityAlignedRotation.y,
                        gravityAlignedRotation.z,
                        gravityAlignedRotation.w,
                    )
                    .apply()
            }
            originCompensation.set(newOriginCompensation)
            // Update the rotation to be sent out in onSpatialModeChanged.
            // It needs to provide identity yaw rotation since we already preserved that part of
            // original rotation for the activity space origin.
            activitySpaceRotation = yawRotation.inverse * activitySpaceRotation
        }
        lastSceneParentTransform.set(newTransform)
        // The translation is zero - since the activity space origin has been already translated by
        // system. SceneCore is relaying the same rotation and scale that activity space would have
        // inherited if it was in HOME_SPACE mode for continuity in FULL_SPACE_MANAGED mode.
        spatialModeChangeListener?.onSpatialModeChanged(
            Pose(Vector3.Zero, activitySpaceRotation),
            sceneParentScale,
        )
    }

    // TODO: b/469860602 - Remove this override once transform listener fix lands.
    override val worldSpaceScale: Vector3
        get() = if (unscaledGravityAlignedActivitySpace) Vector3.One else super.worldSpaceScale

    override fun addOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        boundsListeners.add(listener)
    }

    override fun removeOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        boundsListeners.remove(listener)
    }

    /**
     * This method is called by the Runtime when the bounds of the Activity change. We dispatch the
     * event upwards to the JXRCoreSession via ActivitySpace.
     *
     * <p>Note that this call happens on the Activity's UI thread, so we should be careful not to
     * block it.
     */
    public fun onBoundsChanged(newBounds: Bounds) {
        val newDimensions = _bounds.updateAndGet {
            Dimensions(newBounds.width, newBounds.height, newBounds.depth)
        }
        synchronized(boundsListeners) {
            for (listener in boundsListeners) {
                listener.onBoundsChanged(newDimensions)
            }
        }
    }

    public fun setSpatialModeChangeListener(spatialModeChangeListener: SpatialModeChangeListener?) {
        this.spatialModeChangeListener = spatialModeChangeListener
    }

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult = suspendCancellableCoroutine { continuation ->
        val compensation = originCompensation.get()
        val originInSceneParent = compensation.toSceneParentSpace(origin)
        val directionInSceneParent = compensation.toSceneParentSpaceDirection(direction)

        val consumer =
            Consumer<com.android.extensions.xr.space.HitTestResult> { result ->
                if (continuation.isActive) {
                    continuation.resume(RuntimeUtils.getHitTestResult(result, compensation))
                }
            }

        try {
            extensions.hitTest(
                activity,
                Vec3(originInSceneParent.x, originInSceneParent.y, originInSceneParent.z),
                Vec3(directionInSceneParent.x, directionInSceneParent.y, directionInSceneParent.z),
                RuntimeUtils.getHitTestFilter(hitTestFilter),
                scheduledExecutor,
                consumer,
            )
        } catch (e: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    override suspend fun hitTestRelativeToActivityPose(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
        scenePose: ScenePose,
    ): HitTestResult {

        // Get the Translation of the origin relative to the ActivitySpace.
        val originInActivitySpace = scenePose.transformPositionTo(origin, this)

        // Convert the direction vector to be relative to the ActivitySpace.
        val directionInActivitySpace =
            scenePose.transformDirectionTo(direction, this).toNormalized()

        // Perform the hit test then convert the result to be relative to the provided ScenePose.
        val result = hitTest(originInActivitySpace, directionInActivitySpace, hitTestFilter)

        // No need to do a conversion if the hit test result is not a hit.
        if (result.distance == Float.POSITIVE_INFINITY) {
            return result
        }

        // Update the hit position and surface normal to be relative to the ScenePose.
        val updatedHitPosition = result.hitPosition?.let { this.transformPositionTo(it, scenePose) }
        val updatedSurfaceNormal =
            result.surfaceNormal?.let {
                this.transformDirectionTo(it.toNormalized(), scenePose).toNormalized()
            }
        return HitTestResult(
            updatedHitPosition,
            updatedSurfaceNormal,
            result.surfaceType,
            result.distance,
        )
    }
}
