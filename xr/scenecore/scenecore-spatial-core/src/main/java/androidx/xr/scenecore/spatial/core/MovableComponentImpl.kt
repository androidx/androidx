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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore.spatial.core

import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.MoveEventListener
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getPose
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getVector3
import com.android.extensions.xr.function.Consumer
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.ReformOptions
import com.android.extensions.xr.node.Vec3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Implementation of MovableComponent. */
internal class MovableComponentImpl(
    private val systemMovable: Boolean,
    private val scaleInZ: Boolean,
    private val userAnchorable: Boolean,
    private val activitySpaceImpl: ActivitySpaceImpl,
    private val entityShadowRenderer: EntityShadowRenderer?,
    private val runtimeExecutor: ScheduledExecutorService,
) : MovableComponent {
    private val moveEventListenersMap = ConcurrentHashMap<MoveEventListener, Executor>()
    private var entity: Entity? = null
    private var initialParent: Entity? = null
    private var lastPose = Pose()
    private var lastScale = Vector3(1f, 1f, 1f)
    private var initialRay: Ray? = null
    private var isMoving = false
    private var isSizeExplicit = false // True if size is explicitly set by the user.
    override var size: Dimensions = Dimensions(1f, 1f, 1f)
        set(value) {
            isSizeExplicit = true
            if (value == field) {
                return
            }
            field = value
            if ((entity == null) or (entity is GltfEntity) or (entity is MeshEntity)) {
                return
            }
            val reformOptions = (entity as AndroidXrEntity).getReformOptions()
            reformOptions.currentSize = Vec3(value.width, value.height, value.depth)
            (entity as AndroidXrEntity).updateReformOptions()
        }

    @MovableComponent.ScaleWithDistanceMode
    override var scaleWithDistanceMode = MovableComponent.ScaleWithDistanceMode.DEFAULT
        set(value) {
            field = value
            if (entity == null) {
                return
            }
            val reformOptions = (entity as AndroidXrEntity).getReformOptions()
            reformOptions.scaleWithDistanceMode = translateScaleWithDistanceMode(value)
            (entity as AndroidXrEntity).updateReformOptions()
        }

    private var hitPointToOriginDistance = 0f
    private var grabPointToCenterOffset = Vector3.Zero
    private val inputEventListener = InputEventListener { inputEvent: InputEvent ->
        moveEventListenersMap.forEach { (listener: MoveEventListener, executor: Executor) ->
            executor.execute {
                val moveEvent = getMoveEventFromInputEvent(inputEvent)
                // ignoring other events that are not UP, DOWN and END
                moveEvent?.let { listener.onMoveEvent(it) }
            }
        }
    }

    // Visible for testing.
    internal var reformEventConsumer = Consumer { reformEvent: ReformEvent ->
        if (reformEvent.type != ReformEvent.REFORM_TYPE_MOVE) {
            return@Consumer
        }
        if (reformEvent.state == ReformEvent.REFORM_STATE_START) {
            val entity = entity
            initialParent =
                if (entity != null && entity.parent != null) entity.parent else activitySpaceImpl
            isMoving = true
        } else if (reformEvent.state == ReformEvent.REFORM_STATE_END) {
            isMoving = false
            entityShadowRenderer?.hideShadow()
        }

        val newPose = getPose(reformEvent.proposedPosition, reformEvent.proposedOrientation)
        val newScale = if (scaleInZ) getVector3(reformEvent.proposedScale) else lastScale

        moveEventListenersMap.forEach { (listener: MoveEventListener, listenerExecutor: Executor) ->
            listenerExecutor.execute {
                listener.onMoveEvent(getMoveEventFromReformEvent(reformEvent, newPose, newScale))
            }
        }
        lastPose = newPose
        lastScale = newScale
    }

    private fun getMoveEventFromReformEvent(
        reformEvent: ReformEvent,
        newPose: Pose,
        newScale: Vector3,
    ): MoveEvent {
        return MoveEvent(
            reformEvent.state,
            Ray(
                getVector3(reformEvent.initialRayOrigin),
                getVector3(reformEvent.initialRayDirection),
            ),
            Ray(
                getVector3(reformEvent.currentRayOrigin),
                getVector3(reformEvent.currentRayDirection),
            ),
            lastPose,
            newPose,
            lastScale,
            newScale,
            initialParent!!,
            null,
            null,
        )
    }

    private fun getMoveEventFromInputEvent(inputEvent: InputEvent): MoveEvent? {
        val targetEntity: Entity = entity ?: return null
        val parent: Entity = targetEntity.parent ?: activitySpaceImpl
        val inputOriginInParentSpace: Vector3 =
            activitySpaceImpl.transformPositionTo(inputEvent.origin, parent)
        val inputDirectionInParentSpace: Vector3 =
            activitySpaceImpl.transformDirectionTo(inputEvent.direction, parent)
        val currentRayInParentSpace: Ray =
            Ray(inputOriginInParentSpace, inputDirectionInParentSpace)
        val currentEntityPoseInParentSpace: Pose = targetEntity.getPose()
        val currentEntityScaleInParentSpace: Vector3 = targetEntity.getScale()
        var moveState = -1

        when (inputEvent.action) {
            InputEvent.Action.DOWN -> {

                val hitPosition = inputEvent.hitInfoList.firstOrNull()?.hitPosition ?: return null

                moveState = MoveEvent.MOVE_STATE_START
                initialRay = currentRayInParentSpace
                initialParent = parent
                isMoving = true
                val hitPositionInParentSpace =
                    activitySpaceImpl.transformPositionTo(hitPosition, parent)
                hitPointToOriginDistance =
                    hitPositionInParentSpace.minus(currentRayInParentSpace.origin).length
                grabPointToCenterOffset =
                    currentEntityPoseInParentSpace.translation.minus(hitPositionInParentSpace)
            }
            InputEvent.Action.MOVE -> {
                if (!isMoving) return null
                moveState = MoveEvent.MOVE_STATE_ONGOING
            }
            InputEvent.Action.UP -> {
                if (!isMoving) return null
                moveState = MoveEvent.MOVE_STATE_END
                isMoving = false
                if (targetEntity is GltfEntity || targetEntity is MeshEntity)
                    entityShadowRenderer?.hideShadow()
            }
            else -> return null
        }

        val grabPoint: Vector3 =
            inputOriginInParentSpace.plus(
                inputDirectionInParentSpace.toNormalized().times(hitPointToOriginDistance)
            )

        val proposedTranslationInParentSpace: Vector3 = grabPoint.plus(grabPointToCenterOffset)
        val proposedPoseInParentSpace: Pose =
            Pose(proposedTranslationInParentSpace, currentEntityPoseInParentSpace.rotation)

        val moveEvent =
            MoveEvent(
                moveState,
                initialRay ?: currentRayInParentSpace,
                currentRayInParentSpace,
                if (moveState == MoveEvent.MOVE_STATE_START) proposedPoseInParentSpace
                else lastPose,
                proposedPoseInParentSpace,
                if (moveState == MoveEvent.MOVE_STATE_START) currentEntityScaleInParentSpace
                else lastScale,
                currentEntityScaleInParentSpace,
                initialParent ?: parent,
                null,
                null,
            )

        lastPose = proposedPoseInParentSpace
        lastScale = currentEntityScaleInParentSpace
        return moveEvent
    }

    private fun updateEntityReformOptionsForMove() {
        val reformOptions = (entity as AndroidXrEntity).getReformOptions()
        var reformFlags = ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT
        reformFlags =
            if (systemMovable && !userAnchorable)
                reformFlags or ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
            else reformFlags
        reformFlags =
            if (scaleInZ) reformFlags or ReformOptions.FLAG_SCALE_WITH_DISTANCE else reformFlags
        reformOptions.flags = reformFlags
        reformOptions
            .setEnabledReform(reformOptions.enabledReform or ReformOptions.ALLOW_MOVE)
            .scaleWithDistanceMode = translateScaleWithDistanceMode(scaleWithDistanceMode)
        reformOptions.currentSize = Vec3(size.width, size.height, size.depth)
        (entity as AndroidXrEntity).updateReformOptions()
    }

    private fun updateReformsForPanelEntity(): Boolean {
        updateEntityReformOptionsForMove()
        // Update the size to match panel entity's current size if user hasn't explicitly set it.
        if (!isSizeExplicit) size = (entity as PanelEntity).size
        return true
    }

    private fun updateReformsForGltfEntity(): Boolean {
        (entity as GltfEntity).setReformAffordanceEnabled(
            enabled = true,
            // Ensure system movable is not handled in api-bindings.
            // TODO(b/495927805): Remove system movable flag from the SVXR forked component.
            systemMovable = false,
        )
        (entity as AndroidXrEntity).addInputEventListener(runtimeExecutor, inputEventListener)
        return true
    }

    private fun updateReformsForMeshEntity(): Boolean {
        (entity as MeshEntity).setReformAffordanceEnabled(enabled = true, systemMovable = false)
        (entity as AndroidXrEntity).addInputEventListener(runtimeExecutor, inputEventListener)
        return true
    }

    private fun updateReformsForSurfaceEntity(): Boolean {
        updateEntityReformOptionsForMove()
        // Update the size to match surface entity's current size if user hasn't explicitly set it.
        if (!isSizeExplicit) size = (entity as SurfaceEntity).shape.dimensions
        return true
    }

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }
        this.entity = entity
        lastPose = entity.getPose(Space.PARENT)
        lastScale = entity.getScale(Space.PARENT)

        val success =
            when (entity) {
                is PanelEntity -> updateReformsForPanelEntity()
                is GltfEntity -> updateReformsForGltfEntity()
                is MeshEntity -> updateReformsForMeshEntity()
                is SurfaceEntity -> updateReformsForSurfaceEntity()
                else -> {
                    updateEntityReformOptionsForMove()
                    true
                }
            }

        if (success && entity !is GltfEntity && entity !is MeshEntity) {
            (entity as AndroidXrEntity).addReformEventConsumer(reformEventConsumer, runtimeExecutor)
        }
        if (userAnchorable) entityShadowRenderer?.enableShadow()
        return success
    }

    private fun cleanReformOptions() {
        val reformOptions = (entity as AndroidXrEntity).getReformOptions()
        reformOptions.enabledReform = reformOptions.enabledReform and ReformOptions.ALLOW_MOVE.inv()
        // Clear any flags that were set by this component.
        var reformFlags = reformOptions.flags
        reformFlags =
            if (systemMovable) reformFlags and ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT.inv()
            else reformFlags
        reformFlags =
            if (scaleInZ) reformFlags and ReformOptions.FLAG_SCALE_WITH_DISTANCE.inv()
            else reformFlags
        reformOptions.flags = reformFlags
        (entity as AndroidXrEntity).updateReformOptions()
        (entity as AndroidXrEntity).removeReformEventConsumer(reformEventConsumer)
    }

    override fun onDetach(entity: Entity) {
        when (entity) {
            is GltfEntity -> {
                entity.setReformAffordanceEnabled(
                    enabled = false,
                    systemMovable = systemMovable && !userAnchorable,
                )
                entity.removeInputEventListener(inputEventListener)
            }
            is MeshEntity -> {
                entity.setReformAffordanceEnabled(
                    enabled = false,
                    systemMovable = systemMovable && !userAnchorable,
                )
                entity.removeInputEventListener(inputEventListener)
            }

            is PanelEntity,
            is SurfaceEntity -> cleanReformOptions()
            else -> {}
        }
        this.entity = null
        if (userAnchorable) entityShadowRenderer?.disableShadow()
    }

    override fun addMoveEventListener(moveEventListener: MoveEventListener) {
        moveEventListenersMap[moveEventListener] = runtimeExecutor
    }

    override fun addMoveEventListener(executor: Executor, moveEventListener: MoveEventListener) {
        moveEventListenersMap[moveEventListener] = executor
    }

    override fun removeMoveEventListener(moveEventListener: MoveEventListener) {
        moveEventListenersMap.remove(moveEventListener)
    }

    private fun tryRenderPlaneShadow(proposedPose: Pose, planePose: Pose) {
        if (!shouldRenderPlaneShadow()) {
            return
        }
        var shadowDim: FloatSize2d = FloatSize2d(0f, 0f)
        when (entity) {
            is BasePanelEntity -> {
                shadowDim = calculateSizesForBasePanelEntity(entity as BasePanelEntity)
            }
            is GltfEntity -> {
                shadowDim = calculateSizesForGltfEntity(entity as GltfEntity)
            }
            is MeshEntity -> {
                shadowDim = calculateSizesForMeshEntity(entity as MeshEntity)
            }
        }

        entityShadowRenderer?.updateShadow(proposedPose, planePose, shadowDim = shadowDim)
    }

    private fun shouldRenderPlaneShadow(): Boolean {
        return (entity is BasePanelEntity || entity is GltfEntity || entity is MeshEntity) &&
            userAnchorable &&
            isMoving
    }

    override fun setPlanePoseForMoveUpdatePose(planePose: Pose?, moveUpdatePose: Pose) {
        if (planePose == null) {
            entityShadowRenderer?.hideShadow()
        } else {
            tryRenderPlaneShadow(moveUpdatePose, planePose)
        }
    }

    private fun calculateSizesForBasePanelEntity(panelEntity: BasePanelEntity): FloatSize2d {
        // Scale the panel shadow to the size of the PanelEntity in the activity space.
        val entityScale: Vector3 = panelEntity.worldSpaceScale
        val sizeX: Float =
            (panelEntity.size.width * entityScale.x / activitySpaceImpl.worldSpaceScale.x)
        val sizeZ: Float =
            (panelEntity.size.height * entityScale.z / activitySpaceImpl.worldSpaceScale.x)
        return FloatSize2d(sizeX, sizeZ)
    }

    private fun calculateSizesForMeshEntity(meshEntity: MeshEntity): FloatSize2d {
        val (entityScale, meshBounds) =
            runBlocking(Dispatchers.Main) {
                meshEntity.worldSpaceScale to meshEntity.meshBoundingBox
            }

        val width: Float =
            meshBounds.halfExtents.width.times(HALF_EXTENTS_MULTIPLIER) * entityScale.x /
                activitySpaceImpl.worldSpaceScale.x
        val depth: Float =
            meshBounds.halfExtents.depth.times(HALF_EXTENTS_MULTIPLIER) * entityScale.z /
                activitySpaceImpl.worldSpaceScale.x

        return FloatSize2d(width, depth)
    }

    private fun calculateSizesForGltfEntity(gltfEntity: GltfEntity): FloatSize2d {
        val (entityScale, gltfBounds) =
        // TODO(b/486738199) Check if this can be replaced with suspend
        runBlocking(Dispatchers.Main) {
                gltfEntity.worldSpaceScale to gltfEntity.gltfModelBoundingBox
            }

        // Determine the rendered width and depth of the glTF entity in the activity space
        // to scale the gLTF shadow accordingly
        val width: Float =
            gltfBounds.halfExtents.width.times(HALF_EXTENTS_MULTIPLIER) * entityScale.x /
                activitySpaceImpl.worldSpaceScale.x
        val depth: Float =
            gltfBounds.halfExtents.depth.times(HALF_EXTENTS_MULTIPLIER) * entityScale.z /
                activitySpaceImpl.worldSpaceScale.x

        return FloatSize2d(width, depth)
    }

    companion object {
        // Multiplier to convert half extents to full width/depth.
        private const val HALF_EXTENTS_MULTIPLIER: Float = 2.0f

        private fun translateScaleWithDistanceMode(
            @MovableComponent.ScaleWithDistanceMode scale: Int
        ): Int {
            if (scale == MovableComponent.ScaleWithDistanceMode.DMM) {
                return ReformOptions.SCALE_WITH_DISTANCE_MODE_DMM
            }
            return ReformOptions.SCALE_WITH_DISTANCE_MODE_DEFAULT
        }
    }
}
