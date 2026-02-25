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

package androidx.xr.scenecore

import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.Plane
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent.Companion.createAnchorable
import androidx.xr.scenecore.MovableComponent.Companion.createSystemMovable
import androidx.xr.scenecore.runtime.MoveEventListener as RtMoveEventListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import kotlin.math.max
import kotlinx.coroutines.flow.StateFlow

/**
 * This [Component] can be attached to a single instance of an [Entity]. When attached, this
 * Component will enable the user to translate the Entity by pointing and dragging on it.
 *
 * Creating this Component with [MovableComponent.createCustomMovable] will create the Component but
 * not move the attached Entity. It requires an [EntityMoveListener] which will provide suggested
 * Poses from the system that an application can use to move the attached Entity. This should be
 * used if the application wants to add custom logic for the Entity's movement.
 * [MovableComponent.createSystemMovable] will create the Component and move the attached Entity
 * when the user drags it to a position recommended by the system.
 * [MovableComponent.createAnchorable] will create the Component, move the attached Entity when the
 * user drags it, and also potentially reparent the Entity to a new [AnchorEntity]. This will occur
 * if the user lets go of the Entity near a perception plane that matches the settings in the
 * provided [AnchorPlacement].
 *
 * This component cannot be attached to an [AnchorEntity] or to the [ActivitySpace]. Calling
 * [Entity.addComponent] to an Entity with these types will return false.
 */
public class MovableComponent
private constructor(
    private val session: Session,
    private val entityManager: EntityManager,
    private val systemMovable: Boolean = true,
    private val scaleInZ: Boolean = true,
    private val anchorPlacement: Set<AnchorPlacement> = emptySet(),
    private val disposeParentOnReAnchor: Boolean = true,
    private val initialListener: EntityMoveListener? = null,
    private val initialListenerExecutor: Executor? = null,
) : Component {

    private val sceneRuntime = session.sceneRuntime
    private val anchorable = !anchorPlacement.isEmpty()
    private var createdAnchorEntity: AnchorEntity? = null

    internal val rtMovableComponent by lazy {
        sceneRuntime.createMovableComponent(systemMovable, scaleInZ, anchorable)
    }
    private val moveListenersMap = ConcurrentHashMap<EntityMoveListener, Executor>()
    private val rtMoveEventListener: RtMoveEventListener = RtMoveEventListener { rtMoveEvent ->
        val moveEvent = rtMoveEvent.toMoveEvent(entityManager)
        val updatedReformEventInfo: UpdatedReformEventInfo? =
            if (anchorable) getUpdatedReformEventPoseAndParent(moveEvent) else null
        moveListenersMap.forEach { (entityMoveListener, executor) ->
            executor.execute {
                when (moveEvent.moveState) {
                    MoveEvent.MOVE_STATE_START ->
                        entity?.let {
                            entityMoveListener.onMoveStart(
                                it,
                                moveEvent.initialInputRay,
                                moveEvent.previousPose,
                                moveEvent.previousScale,
                                moveEvent.initialParent,
                            )
                        }

                    MoveEvent.MOVE_STATE_ONGOING ->
                        entity?.let {
                            entityMoveListener.onMoveUpdate(
                                it,
                                moveEvent.currentInputRay,
                                updatedReformEventInfo?.pose ?: moveEvent.currentPose,
                                updatedReformEventInfo?.scale ?: moveEvent.currentScale,
                            )
                        }

                    MoveEvent.MOVE_STATE_END ->
                        entity?.let {
                            entityMoveListener.onMoveEnd(
                                it,
                                moveEvent.currentInputRay,
                                updatedReformEventInfo?.pose ?: moveEvent.currentPose,
                                updatedReformEventInfo?.scale ?: moveEvent.currentScale,
                                updatedReformEventInfo?.parent ?: moveEvent.initialParent,
                            )
                        }
                }
            }
        }
    }

    private var entity: Entity? = null
    private lateinit var planesFlow: StateFlow<Collection<Plane>>

    private fun Collection<Plane>.filterByAnchorPlacement(
        anchorPlacement: Set<AnchorPlacement>
    ): Collection<Plane> =
        this.mapNotNull { it ->
            var outPlane: Plane? = null
            val planeData = it.state.value
            val planeOrientation = it.type.toSceneCoreOrientation()
            val planeSemantic = planeData.label.toSceneCoreSemanticType()
            for (anchorPlacementSpec in anchorPlacement) {
                if (
                    (anchorPlacementSpec.anchorablePlaneOrientations.contains(planeOrientation) ||
                        anchorPlacementSpec.anchorablePlaneOrientations.contains(
                            PlaneOrientation.ANY
                        )) &&
                        (anchorPlacementSpec.anchorablePlaneSemanticTypes.contains(planeSemantic) ||
                            anchorPlacementSpec.anchorablePlaneSemanticTypes.contains(
                                PlaneSemanticType.ANY
                            )) &&
                        planeData.trackingState == TrackingState.TRACKING
                ) {
                    outPlane = it
                    break
                }
            }
            outPlane
        }

    /**
     * The size of the move affordance in meters. This property determines the size of the bounding
     * box that is used to draw the draggable move affordances around the [Entity]. This property
     * can be modified if the move affordance needs to be larger or smaller than the Entity itself.
     */
    public var size: FloatSize3d = kDimensionsOneMeter
        set(value) {
            if (field != value) {
                field = value
                rtMovableComponent.size = value.toRtDimensions()
            }
        }

    override fun onAttach(entity: Entity): Boolean {
        if (entity is AnchorEntity || entity is ActivitySpace) {
            return false
        }
        if (this.entity != null) {
            return false
        }
        this.entity = entity
        val attached = (entity as BaseEntity<*>).rtEntity!!.addComponent(rtMovableComponent)
        if (attached) {
            if (anchorable) {
                planesFlow = Plane.subscribe(session)
            }
            rtMovableComponent.addMoveEventListener(rtMoveEventListener)
            if (initialListener != null) {
                if (initialListenerExecutor != null) {
                    addMoveListener(initialListenerExecutor, initialListener)
                } else {
                    addMoveListener(initialListener)
                }
            }
        }
        return attached
    }

    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtMovableComponent)
        this.entity = null
    }

    private data class UpdatedReformEventInfo(val pose: Pose, val parent: Entity?, val scale: Float)

    private fun getUpdatedReformEventPoseAndParent(moveEvent: MoveEvent): UpdatedReformEventInfo {
        val initialParent = moveEvent.initialParent
        val initialPose = moveEvent.currentPose
        val moveEventPoseInOxr =
            initialParent.transformPoseTo(moveEvent.currentPose, session.scene.perceptionSpace)
        val initialScale = moveEvent.currentScale
        var updatedPose: Pose = initialPose
        var updatedParent: Entity? = null
        // Ignore the initial scale from the move event, it is currently incorrect.
        var updatedScale: Float = entity?.getScale() ?: initialScale
        var anchorablePlanePose: Pose? = null
        var anchorablePlane: Plane? = null
        val planes: Collection<Plane> = planesFlow.value
        for (plane in planes.filterByAnchorPlacement(anchorPlacement)) {
            val planeData = plane.state.value
            var centerPoseToProposedPose = planeData.centerPose.inverse.compose(moveEventPoseInOxr)

            // The extents of the plane are in the X and Z directions so we can use
            // those to determine if the point is outside the plane. The absolute value
            // of the y-value of centerPoseToProposedPose is the projected distance from
            // the plane to the point.
            if (
                centerPoseToProposedPose.translation.x > -planeData.extents.width / 2.0f &&
                    centerPoseToProposedPose.translation.x < planeData.extents.width / 2.0f &&
                    centerPoseToProposedPose.translation.z > -planeData.extents.height / 2.0f &&
                    centerPoseToProposedPose.translation.z < planeData.extents.height / 2.0f &&
                    centerPoseToProposedPose.translation.y < MAX_PLANE_ANCHOR_DISTANCE
            ) {
                centerPoseToProposedPose =
                    Pose(
                        Vector3(
                            centerPoseToProposedPose.translation.x,
                            max(0f, centerPoseToProposedPose.translation.y),
                            centerPoseToProposedPose.translation.z,
                        ),
                        centerPoseToProposedPose.rotation,
                    )
                anchorablePlanePose = planeData.centerPose
                anchorablePlane = plane
                updatedPose = planeData.centerPose.compose(centerPoseToProposedPose)
                updatedPose =
                    session.scene.perceptionSpace.transformPoseTo(updatedPose, initialParent)
                break
            }
        }

        if (!systemMovable) {
            return UpdatedReformEventInfo(updatedPose, null, updatedScale)
        }

        if (anchorablePlanePose != null && anchorablePlane != null) {
            if (moveEvent.moveState == MoveEvent.MOVE_STATE_END) {
                val rotation =
                    when (entity) {
                        is PanelEntity ->
                            moveEventPoseInOxr.getForwardVectorToUpRotation(anchorablePlanePose)
                        is GltfModelEntity ->
                            moveEventPoseInOxr.getUpVectorToUpRotation(anchorablePlanePose)
                        else ->
                            throw IllegalArgumentException(
                                "Movable component can be applied to either a PanelEntity or GltfModelEntity"
                            )
                    }
                val rotatedPose = Pose(moveEventPoseInOxr.translation, rotation)
                var poseToAnchor: Pose = anchorablePlanePose.inverse.compose(rotatedPose)
                poseToAnchor =
                    Pose(
                        Vector3(poseToAnchor.translation.x, 0f, poseToAnchor.translation.z),
                        poseToAnchor.rotation,
                    )
                val anchorResult = anchorablePlane.createAnchor(Pose.Identity)
                if (anchorResult is AnchorCreateSuccess) {
                    updatedPose = poseToAnchor
                    updatedParent = AnchorEntity.create(session, anchorResult.anchor)
                    createdAnchorEntity = updatedParent
                }
            }
        } else {
            entity?.let { entity ->
                if (
                    entity.parent == createdAnchorEntity &&
                        moveEvent.moveState == MoveEvent.MOVE_STATE_END
                ) {
                    updatedParent = session.scene.activitySpace
                    updatedPose =
                        initialParent.transformPoseTo(
                            moveEvent.currentPose,
                            session.scene.activitySpace,
                        )
                }
            }
        }
        rtMovableComponent.setPlanePoseForMoveUpdatePose(anchorablePlanePose, moveEventPoseInOxr)

        // If the parent of the entity is changing, update its scale to reflect the ratio of the
        // scale of the initial parent to the scale of the updated parent. This preserves activity
        // space scaling when anchoring to an AnchorEntity, and removes it when anchoring back to
        // the activity space.
        if (updatedParent != null && updatedParent != initialParent) {
            entity?.let {
                updatedScale =
                    it.getScale() * initialParent.getScale(Space.REAL_WORLD) /
                        updatedParent.getScale(Space.REAL_WORLD)
            }
        }

        entity?.let { entity ->
            if (updatedParent != null && entity.parent != updatedParent) {
                val prevParent = entity.parent
                entity.parent = updatedParent
                if (
                    prevParent != null &&
                        prevParent == createdAnchorEntity &&
                        disposeParentOnReAnchor &&
                        prevParent.children.isEmpty()
                ) {
                    prevParent.dispose()
                    createdAnchorEntity = null
                }
            }
            entity.setPose(updatedPose)
            entity.setScale(updatedScale)
        }
        return UpdatedReformEventInfo(updatedPose, updatedParent, updatedScale)
    }

    /**
     * Adds a listener to the set of active listeners for the move events. The listener will be
     * invoked regardless of whether the [Entity] is being moved by the system or the user.
     *
     * The listener is invoked on the provided [Executor]. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * @param executor The executor to run the listener on.
     * @param entityMoveListener The move event listener to set.
     */
    public fun addMoveListener(executor: Executor, entityMoveListener: EntityMoveListener) {
        moveListenersMap[entityMoveListener] = executor
    }

    /**
     * Adds a listener to the set of active listeners for the move events. The listener will be
     * invoked regardless of whether the [Entity] is being moved by the system or the user.
     *
     * The listener is invoked on the main thread.
     *
     * @param entityMoveListener The move event listener to set.
     */
    public fun addMoveListener(entityMoveListener: EntityMoveListener) {
        addMoveListener(HandlerExecutor.mainThreadExecutor, entityMoveListener)
    }

    /**
     * Removes a listener from the set of active listeners for the move events.
     *
     * @param entityMoveListener The move event listener to remove.
     */
    public fun removeMoveListener(entityMoveListener: EntityMoveListener) {
        moveListenersMap.remove(entityMoveListener)
    }

    public companion object {
        private val kDimensionsOneMeter = FloatSize3d(1f, 1f, 1f)
        internal const val MAX_PLANE_ANCHOR_DISTANCE = 0.2f

        /** Factory function for creating a MovableComponent. */
        internal fun create(
            session: Session,
            entityManager: EntityManager,
            systemMovable: Boolean = true,
            scaleInZ: Boolean = true,
            anchorPlacement: Set<AnchorPlacement> = emptySet(),
            disposeParentOnReAnchor: Boolean = true,
            initialListener: EntityMoveListener? = null,
            initialListenerExecutor: Executor? = null,
        ): MovableComponent {
            return MovableComponent(
                session,
                entityManager,
                systemMovable,
                scaleInZ,
                anchorPlacement,
                disposeParentOnReAnchor,
                initialListener,
                initialListenerExecutor,
            )
        }

        /**
         * Public factory function for creating a MovableComponent.
         *
         * This [Component] can be attached to a single instance of an [Entity]. When attached, this
         * Component will enable the user to translate the Entity by pointing and dragging on it.
         *
         * When created with this function the MovableComponent will not move or rescale the Entity
         * but it could be done using the [EntityMoveListener.onMoveUpdate] callback.
         *
         * This component cannot be attached to an [AnchorEntity] or to the [ActivitySpace]. Calling
         * [Entity.addComponent] to an Entity with these types will return false.
         *
         * @param session The [Session] instance.
         * @param scaleInZ A Boolean which tells the system to update the scale of the Entity as the
         *   user moves it closer and further away. This is mostly useful for Panel auto-rescaling
         *   with distance.
         * @param executor The executor to run the listener on. If set to null, the listener will be
         *   invoked on the main thread.
         * @param entityMoveListener A move event listener for the event. The application should set
         *   the entity position and scale as desired using [Entity.setPose] and [Entity.setScale]
         *   in the [EntityMoveListener.onMoveUpdate] callback. To have the system do this movement
         *   use [createSystemMovable] or [createAnchorable].
         * @return MovableComponent instance.
         */
        @JvmStatic
        public fun createCustomMovable(
            session: Session,
            scaleInZ: Boolean,
            executor: Executor?,
            entityMoveListener: EntityMoveListener,
        ): MovableComponent =
            MovableComponent.create(
                session = session,
                entityManager = session.scene.entityManager,
                systemMovable = false,
                scaleInZ = scaleInZ,
                initialListener = entityMoveListener,
                initialListenerExecutor = executor,
            )

        /**
         * Public factory function for creating a MovableComponent.
         *
         * This [Component] can be attached to a single instance of an [Entity]. When attached, this
         * Component will enable the user to translate the Entity by pointing and dragging on it.
         *
         * When created with this function the MovableComponent will move and rescale the Entity.
         * [EntityMoveListener] can be attached to received callbacks when the Entity is being
         * moved.
         *
         * This component cannot be attached to an [AnchorEntity] or to the [ActivitySpace]. Calling
         * [Entity.addComponent] to an Entity with these types will return false.
         *
         * @param session The [Session] instance.
         * @param scaleInZ A Boolean which tells the system to update the scale of the Entity as the
         *   user moves it closer and further away. This is mostly useful for Panel auto-rescaling
         *   with distance.
         * @return MovableComponent instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun createSystemMovable(
            session: Session,
            scaleInZ: Boolean = true,
        ): MovableComponent =
            MovableComponent.create(
                session = session,
                entityManager = session.scene.entityManager,
                systemMovable = true,
                scaleInZ = scaleInZ,
            )

        /**
         * Public factory function for creating a MovableComponent.
         *
         * This [Component] can be attached to a single instance of an [Entity]. When attached, this
         * Component will enable the user to translate the Entity by pointing and dragging on it.
         *
         * When created with this function the MovableComponent will move and potentially Anchor the
         * Entity. When anchored a new [AnchorEntity] will be created and set as the parent of the
         * Entity. If the entity is moved off of a created [AnchorEntity] it will be reparented to
         * the [ActivitySpace]. An [EntityMoveListener] can be attached to receive callbacks when
         * the Entity is being moved and to see if it was reparented to an AnchorEntity.
         *
         * This component cannot be attached to an AnchorEntity or to the ActivitySpace. Calling
         * [Entity.addComponent] to an Entity with these types will return false.
         *
         * @param session The [Session] instance.
         * @param anchorPlacement A Set containing different [AnchorPlacement] for how to anchor the
         *   Entity with a MovableComponent. When empty this Entity will not be anchored.
         * @param disposeParentOnReAnchor A Boolean, which if set to true, when an Entity is moved
         *   off of an [AnchorEntity] that was created by the underlying MovableComponent, and the
         *   AnchorEntity has no other children, the AnchorEntity will be disposed, and the
         *   underlying Anchor will be detached.
         * @return MovableComponent instance.
         * @throws IllegalArgumentException if created with an Empty Set of for anchorPlacement
         */
        @JvmOverloads
        @JvmStatic
        public fun createAnchorable(
            session: Session,
            anchorPlacement: Set<AnchorPlacement> = setOf(AnchorPlacement.createForPlanes()),
            disposeParentOnReAnchor: Boolean = true,
        ): MovableComponent {
            require(anchorPlacement.isNotEmpty()) {
                "Cannot create a MovableComponent with createAnchorable and an empty set for anchorPlacement"
            }
            return MovableComponent.create(
                session = session,
                entityManager = session.scene.entityManager,
                systemMovable = true,
                scaleInZ = false,
                anchorPlacement = anchorPlacement,
                disposeParentOnReAnchor = disposeParentOnReAnchor,
            )
        }
    }
}
