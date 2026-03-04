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
    entityManager: EntityManager,
    private val spatialStateProvider: Supplier<SpatialState>,
    executor: ScheduledExecutorService,
) : SystemSpaceEntityImpl(activity, taskNode, extensions, entityManager, executor), ActivitySpace {

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
        get() =
            _bounds.updateAndGet { oldBounds ->
                if (oldBounds == null) {
                    val bounds = spatialStateProvider.get().bounds
                    Dimensions(bounds.width, bounds.height, bounds.depth)
                } else {
                    oldBounds
                }
            }

    /** Returns the identity pose since this entity defines the origin of the activity space. */
    override val poseInActivitySpace: Pose
        get() = Pose()

    public val poseInPerceptionSpace: Pose
        get() {
            val perceptionSpaceScenePose =
                mEntityManager
                    .getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose::class.java)
                    .single()
            return transformPoseTo(Pose(), perceptionSpaceScenePose)
        }

    /** Returns the identity pose since we assume the activity space is the world space root. */
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
        get() =
            cachedRecommendedContentBox.updateAndGet { currentBox ->
                currentBox
                    ?: run {
                        val recommendedBox = mExtensions.recommendedContentBoxInFullSpace
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
            Space.ACTIVITY -> poseInActivitySpace
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

    internal var sceneParentScaleAbs: Vector3 = Vector3.One

    /**
     * Handles the updates to scene core root transform.
     * <pre>
     * Hierarchy:
     * OpenXR Unbounded Reference Space Origin
     * └── Scene Parent Node (Intermediate system-managed node)
     * └── Scene Root Node (ActivitySpace Node)
     *
     * Transform Flow:
     * 1. The system updates the transform of the 'Scene Parent Node' when in HOME_SPACE mode.
     * 2. The 'Scene Root Node' becomes a child of 'Scene Parent Node' and inherits its transform
     * when activity enters FULL_SPACE_MANAGED mode.
     * </pre>
     * <p>By inverting the inherited scale and roll and pitch rotations of the scene parent
     * transform, SceneCore effectively re-orients the ActivitySpace to be unscaled and
     * gravity-aligned like its grandparent OpenXR unbounded space, while preserving its yaw
     * rotation (i.e. facing user direction).
     *
     * <p>To maintain continuity when entering FSM, SceneCore provides the original rotation and
     * scale of the scene parent transform via the onSpatialModeChanged callback. This ensures FSM
     * continuity when spatial modes change.
     *
     * @param newTransform New scene parent transform relative to OpenXR unbounded reference space.
     */
    public fun handleOriginUpdate(newTransform: Matrix4) {
        openXrReferenceSpaceTransform.set(newTransform)
        sceneParentScaleAbs = Vector3.abs(newTransform.scale)
        val sceneParentScaleInv = sceneParentScaleAbs.inverse()
        // Get the unscaled rotation of the activity space.
        var activitySpaceRotation = newTransform.unscaled().rotation
        val yaw = activitySpaceRotation.eulerAngles.y
        val yawRotation = Quaternion.fromEulerAngles(0.0f, yaw, 0.0f)
        val gravityAlignedRotation = activitySpaceRotation.inverse * yawRotation
        mExtensions.createNodeTransaction().use { transaction ->
            transaction
                .setScale(
                    getNode(),
                    sceneParentScaleInv.x,
                    sceneParentScaleInv.y,
                    sceneParentScaleInv.z,
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
        // Update the rotation to be sent out in onSpatialModeChanged.
        // It needs to provide identity yaw rotation since we already preserved that part of
        // original rotation for the activity space origin.
        activitySpaceRotation = yawRotation.inverse * activitySpaceRotation

        // The translation is zero - since the activity space origin has been already translated by
        // system. SceneCore is relaying the same rotation and scale that activity space would have
        // inherited if it was in HOME_SPACE mode for continuity in FULL_SPACE_MANAGED mode.
        spatialModeChangeListener?.onSpatialModeChanged(
            Pose(Vector3.Zero, activitySpaceRotation),
            sceneParentScaleAbs,
        )
    }

    // TODO: b/469860602 - Remove this override once transform listener fix lands.
    override val worldSpaceScale: Vector3
        get() = Vector3.One

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
        val newDimensions =
            _bounds.updateAndGet { Dimensions(newBounds.width, newBounds.height, newBounds.depth) }
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
        val consumer =
            Consumer<com.android.extensions.xr.space.HitTestResult> { result ->
                if (continuation.isActive) {
                    continuation.resume(RuntimeUtils.getHitTestResult(result))
                }
            }

        try {
            mExtensions.hitTest(
                activity,
                Vec3(origin.x, origin.y, origin.z),
                Vec3(direction.x, direction.y, direction.z),
                RuntimeUtils.getHitTestFilter(hitTestFilter),
                mExecutor,
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
        val originInActivitySpace = scenePose.transformPoseTo(Pose(origin), this).translation

        // Get the Translation of the direction pose relative to the ActivitySpace.
        val directionPoseInActivitySpace = scenePose.transformPoseTo(Pose(direction), this)

        // Convert the direction pose to a direction vector relative to the ActivitySpace.
        val directionInActivitySpace =
            directionPoseInActivitySpace.compose(scenePose.activitySpacePose.inverse).translation

        // Perform the hit test then convert the result to be relative to the provided ScenePose.
        val result = hitTest(originInActivitySpace, directionInActivitySpace, hitTestFilter)

        // No need to do a conversion if the hit test result is not a hit.
        if (result.distance == Float.POSITIVE_INFINITY) {
            return result
        }

        // Update the hit position and surface normal to be relative to the
        // ScenePose.
        val updatedHitPosition =
            if (result.hitPosition == null) {
                null
            } else {
                transformPoseTo(Pose(result.hitPosition!!), scenePose).translation
            }
        val updatedSurfaceNormal =
            if (result.surfaceNormal == null) {
                null
            } else {
                transformPoseTo(Pose(Vector3(result.surfaceNormal!!)), scenePose)
                    .compose(this.transformPoseTo(Pose.Identity, scenePose).inverse)
                    .translation
            }
        return HitTestResult(
            updatedHitPosition,
            updatedSurfaceNormal,
            result.surfaceType,
            result.distance,
        )
    }
}
