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

package androidx.xr.scenecore.spatial.core

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GeometryAffordanceState
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.ReformAffordanceFlag
import androidx.xr.scenecore.runtime.RotatableComponent
import androidx.xr.scenecore.runtime.Space
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementation of [RotatableComponent].
 *
 * Enables the [ReformAffordanceFlag.ROTATABLE] geometry affordance on the attached [GltfEntity] or
 * [MeshEntity], which makes the rendering runtime interpret grab-and-drag gestures on the geometry
 * as a rotation. The runtime reports the resulting rotation through the entity's recommended
 * affordance rotation and this component applies it to the entity's pose.
 *
 * The recommended rotation is the entity's rotation in the rendering runtime's world space (the
 * platform reference space the entity's subspace transform is reported in), not in the entity's
 * parent space. The orientation of the parent space relative to that world space is not known here,
 * but it is constant for the duration of a gesture, so the rotation accumulated since the gesture
 * started is extracted in the entity's own frame (relative to the recommended rotation at gesture
 * start) and composed with the parent-space orientation the entity had at that time. This keeps the
 * component correct regardless of how the activity space is oriented in the world, robust to pose
 * changes made by the application between gestures, and to the component being detached and
 * re-attached. The rotation pivots around the center of the geometry's bounding box, matching the
 * pivot the rendering runtime uses.
 */
internal class RotatableComponentImpl(private val runtimeExecutor: ScheduledExecutorService) :
    RotatableComponent {
    @Volatile private var entity: Entity? = null

    /**
     * Offset from the entity origin to the rotation pivot, in the entity's local (unscaled) space.
     * Captured when the component is attached.
     */
    private var pivotOffsetLocal = Vector3.Zero

    /** Parent and parent-space orientation of the entity when the component was attached. */
    private var attachParent: Entity? = null
    private var attachRotation = Quaternion.Identity

    /** Activity-space orientation of the entity when the component was attached, if known. */
    private var attachActivityRotation: Quaternion? = null

    /**
     * Gesture state. Only accessed on [runtimeExecutor], which dispatches [inputEventListener] and
     * [reset].
     */
    private var isRotating = false
    private var gestureStartPose = Pose.Identity
    private var gestureStartRecommendedRotation = Quaternion.Identity

    /** Set by [reset] during an interaction; cleared once the rendering runtime is idle again. */
    private var ignoreRotationUntilIdle = false

    /**
     * Applies the runtime's recommended rotation while the entity's geometry affordance reports a
     * rotation gesture.
     */
    private val inputEventListener = InputEventListener { _ ->
        val geometry = entity ?: return@InputEventListener
        val currentState = geometry.geometryAffordanceState
        val recommendedRotation = geometry.recommendedGeometryRotation

        if (!currentState.isInteracting) {
            // While idle, the rendering runtime re-syncs its rotation baseline with the entity.
            ignoreRotationUntilIdle = false
        }
        if (currentState == GeometryAffordanceState.ROTATION && !ignoreRotationUntilIdle) {
            if (!isRotating) {
                isRotating = true
                startGesture(geometry, recommendedRotation)
            }
            applyRecommendedRotation(geometry, recommendedRotation)
        } else if (isRotating) {
            // Gesture ended: the entity keeps the orientation it had when released. The
            // rendering runtime is configured to keep its orientation on release as well, and
            // it re-synchronizes its baseline with the entity's actual world rotation while no
            // gesture is in progress, so nothing else needs to be applied here.
            isRotating = false
        }
    }

    private fun startGesture(geometry: Entity, recommendedRotation: Quaternion) {
        gestureStartPose = geometry.getPose(Space.PARENT)
        gestureStartRecommendedRotation = recommendedRotation
    }

    /**
     * The pivot offset in the parent's scale. The pivot offset is authored in the geometry's local
     * space, so it is scaled before being combined with the parent-space pose.
     */
    private fun scaledPivotOffset(geometry: Entity): Vector3 {
        val scale = geometry.getScale(Space.PARENT)
        return Vector3(
            pivotOffsetLocal.x * scale.x,
            pivotOffsetLocal.y * scale.y,
            pivotOffsetLocal.z * scale.z,
        )
    }

    private fun applyRecommendedRotation(geometry: Entity, recommendedRotation: Quaternion) {
        // Both the recommended rotation and the one captured at gesture start are expressed in
        // the runtime's world space, so their quotient is the rotation accumulated since the
        // gesture started. Taking it on the right of the start rotation expresses that delta in
        // the entity's own frame at gesture start, which can be composed with the parent-space
        // rotation directly: the result equals the world-space delta expressed in the parent
        // space, without needing to know how the parent space is oriented in the world.
        val localRotationDelta = gestureStartRecommendedRotation.inverse * recommendedRotation
        val newRotation = gestureStartPose.rotation * localRotationDelta
        setRotationAroundPivot(geometry, gestureStartPose, scaledPivotOffset(geometry), newRotation)
    }

    /**
     * Sets the entity's rotation to [newRotation] while keeping the pivot (the point at
     * [pivotOffset] from the origin of [fromPose]) stationary in the parent space.
     */
    private fun setRotationAroundPivot(
        geometry: Entity,
        fromPose: Pose,
        pivotOffset: Vector3,
        newRotation: Quaternion,
    ) {
        val pivotInParent = fromPose.translation + fromPose.rotation * pivotOffset
        val newTranslation = pivotInParent - newRotation * pivotOffset
        geometry.setPose(Pose(newTranslation, newRotation), Space.PARENT)
    }

    override fun reset() {
        val geometry = entity ?: return
        // Serialize with the gesture handling, which runs on the same executor.
        runtimeExecutor.execute {
            if (entity !== geometry) return@execute
            // The rendering runtime keeps its pre-reset rotation baseline until it is idle again,
            // so a gesture in progress must not rotate the entity until then.
            if (geometry.geometryAffordanceState.isInteracting) {
                ignoreRotationUntilIdle = true
            }
            isRotating = false
            val currentParent = geometry.parent
            val targetRotation =
                if (currentParent === attachParent) {
                    attachRotation
                } else {
                    // Convert through the activity space, which doesn't depend on the original
                    // parent still being connected.
                    val parentRotation = currentParent?.activitySpaceRotationOrNull()
                    val savedRotation = attachActivityRotation
                    if (parentRotation != null && savedRotation != null) {
                        parentRotation.inverse * savedRotation
                    } else {
                        attachRotation
                    }
                }
            setRotationAroundPivot(
                geometry,
                geometry.getPose(Space.PARENT),
                scaledPivotOffset(geometry),
                targetRotation,
            )
        }
    }

    internal val isAttached: Boolean
        get() = entity != null

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }
        if (!entity.isRotatableGeometry) {
            return false
        }
        // ROTATABLE is one bit in the entity's affordance mask, so a second component would share
        // it, and detaching either one would disable rotation for the other.
        if (entity.getComponentsOfType(RotatableComponent::class.java).isNotEmpty()) {
            return false
        }
        this.entity = entity
        pivotOffsetLocal = entity.geometryBoundingBoxCenter
        attachParent = entity.parent
        attachRotation = entity.getPose(Space.PARENT).rotation
        attachActivityRotation = entity.activitySpaceRotationOrNull()
        // The rotation gesture starts by grabbing the geometry, which requires it to be
        // hit-testable. glTF models are instanced without a collider by default.
        enableGeometryCollider(entity)
        entity.setGeometryReformAffordanceEnabled(enabled = true, ReformAffordanceFlag.ROTATABLE)
        entity.addInputEventListener(runtimeExecutor, inputEventListener)
        return true
    }

    override fun onDetach(entity: Entity) {
        attachParent = null
        this.entity = null
        if (entity.isRotatableGeometry) {
            entity.setGeometryReformAffordanceEnabled(
                enabled = false,
                ReformAffordanceFlag.ROTATABLE,
            )
            entity.removeInputEventListener(inputEventListener)
            disableGeometryColliderIfUnused(entity, excludingComponent = this)
        }
        // Clear the gesture state on the executor, after any input events already queued for it.
        runtimeExecutor.execute { isRotating = false }
    }

    // The two supported geometry entity types expose the same affordance surface but do not share
    // an interface; the helpers below dispatch on the concrete type.

    private val Entity.isRotatableGeometry: Boolean
        get() = this is GltfEntity || this is MeshEntity

    private val Entity.geometryBoundingBoxCenter: Vector3
        get() =
            when (this) {
                is GltfEntity -> gltfModelBoundingBox.center
                is MeshEntity -> meshBoundingBox.center
                else -> throw IllegalArgumentException("Unsupported entity: $this")
            }

    private val Entity.geometryAffordanceState: GeometryAffordanceState
        get() =
            when (this) {
                is GltfEntity -> affordanceState
                is MeshEntity -> affordanceState
                else -> throw IllegalArgumentException("Unsupported entity: $this")
            }

    private val Entity.recommendedGeometryRotation: Quaternion
        get() =
            when (this) {
                is GltfEntity -> recommendedAffordanceRotation
                is MeshEntity -> recommendedAffordanceRotation
                else -> throw IllegalArgumentException("Unsupported entity: $this")
            }

    private fun Entity.setGeometryReformAffordanceEnabled(
        enabled: Boolean,
        flag: ReformAffordanceFlag,
    ) {
        when (this) {
            is GltfEntity -> setReformAffordanceEnabled(enabled, flag)
            is MeshEntity -> setReformAffordanceEnabled(enabled, flag)
            else -> throw IllegalArgumentException("Unsupported entity: $this")
        }
    }

    private val GeometryAffordanceState.isInteracting: Boolean
        get() =
            this == GeometryAffordanceState.TRANSLATION ||
                this == GeometryAffordanceState.ROTATION ||
                this == GeometryAffordanceState.ONE_HANDED_SCALE ||
                this == GeometryAffordanceState.TWO_HANDED_SCALE

    private fun Entity.activitySpaceRotationOrNull(): Quaternion? =
        try {
            activitySpacePose.rotation
        } catch (e: IllegalStateException) {
            null // Not connected to the activity space.
        }
}
