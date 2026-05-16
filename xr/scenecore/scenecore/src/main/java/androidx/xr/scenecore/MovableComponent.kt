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

@file:Suppress("BanConcurrentHashMap", "OPT_IN_USAGE")

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AugmentedObject
import androidx.xr.arcore.Eye
import androidx.xr.arcore.Hand
import androidx.xr.arcore.HandJointType
import androidx.xr.arcore.Plane
import androidx.xr.arcore.Trackable
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent.Companion.createAnchorable
import androidx.xr.scenecore.MovableComponent.Companion.createSystemMovable
import androidx.xr.scenecore.runtime.HandlerExecutor
import androidx.xr.scenecore.runtime.MovableComponent as RtMovableComponent
import androidx.xr.scenecore.runtime.MoveEventListener as RtMoveEventListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Function
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
    entityRegistry: EntityRegistry,
    private val systemMovable: Boolean = true,
    private val scaleInZ: Boolean = true,
    private val anchorPlacement: Set<AnchorPlacement> = emptySet(),
    private val disposeParentOnReAnchor: Boolean = true,
    private val initialListener: EntityMoveListener? = null,
    private val initialListenerExecutor: Executor? = null,
    private val trackable: Trackable<Trackable.State>? = null,
    private val poseExtractor: ((Any?) -> Pose?)? = null,
) : Component() {

    private val sceneRuntime = session.sceneRuntime
    private val anchorable = !anchorPlacement.isEmpty()
    private var createdAnchorEntity: AnchorEntity? = null

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val rtMovableComponent: RtMovableComponent by lazy {
        sceneRuntime.createMovableComponent(systemMovable, scaleInZ, anchorable)
    }
    internal val rtTrackableComponent by lazy {
        sceneRuntime.createTrackableComponent(
            session.lifecycleOwner,
            requireNotNull(trackable),
            requireNotNull(poseExtractor),
        )
    }

    private val moveListenersMap = ConcurrentHashMap<EntityMoveListener, Executor>()

    private val rtMoveEventListener: RtMoveEventListener = RtMoveEventListener { rtMoveEvent ->
        val moveEvent = rtMoveEvent.toMoveEvent(entityRegistry)
        var updatedReformEventInfo: UpdatedReformEventInfo? = null
        if (anchorable) {
            updatedReformEventInfo = getUpdatedReformEventPoseAndParent(moveEvent)
        } else if (systemMovable && (entity is GltfModelEntity || entity is MeshEntity)) {
            entity?.apply {
                // TODO(b/495925250): Add SceneCore unit tests for movable glTFs
                setPose(moveEvent.currentPose)
                setScale(moveEvent.currentScale)
            }
        }
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
        this.mapNotNull {
            var outPlane: Plane? = null
            val planeData = it.state.value
            val planeOrientation = it.type.toSceneCoreOrientation()
            val planeSemantic = planeData.label.toSceneCoreSemanticType()
            for (anchorPlacementSpec in anchorPlacement) {
                if (
                    anchorPlacementSpec.anchorablePlaneOrientations.contains(planeOrientation) &&
                        anchorPlacementSpec.anchorablePlaneSemanticTypes.contains(planeSemantic) &&
                        planeData.trackingState == TrackingState.TRACKING
                ) {
                    outPlane = it
                    break
                }
            }
            outPlane
        }

    /**
     * The size of the move affordance in local space virtual meters. This property determines the
     * size of the bounding box that is used to draw the draggable move affordances around the
     * [Entity]. This property can be modified if the move affordance needs to be larger or smaller
     * than the Entity itself.
     *
     * When attaching this component to an entity, the apps may update this value to appropriate new
     * value, such as the size of the entity this component is being added to. If apps don't set
     * this value, the component will try to use the entity's dimensions as value for this property
     * where applicable, or default to (1 x 1 x 1).
     */
    public var size: FloatSize3d
        get() = rtMovableComponent.size.toFloatSize3d()
        set(value) {
            rtMovableComponent.size = value.toRtDimensions()
        }

    override fun onAttach(entity: Entity): Boolean {
        if (entity is AnchorEntity || entity is ActivitySpace) {
            return false
        }
        if (this.entity != null) {
            return false
        }

        // Adds TrackableComponent instead of MovableComponent.
        if (trackable != null && poseExtractor != null) {
            val attached = entity.rtEntity.addComponent(rtTrackableComponent)
            if (attached) {
                this.entity = entity
            }
            return attached
        }

        val attached = entity.rtEntity.addComponent(rtMovableComponent)
        if (attached) {
            this.entity = entity
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
        // Removes TrackableComponent instead of MovableComponent if trackable is non-null.
        if (trackable != null && poseExtractor != null) {
            entity.rtEntity.removeComponent(rtTrackableComponent)
        } else {
            rtMovableComponent.removeMoveEventListener(rtMoveEventListener)
            entity.rtEntity.removeComponent(rtMovableComponent)
        }

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
                        is GltfModelEntity,
                        is MeshEntity ->
                            moveEventPoseInOxr.getUpVectorToUpRotation(anchorablePlanePose)
                        else ->
                            throw IllegalArgumentException(
                                "Movable component can be applied to either a PanelEntity, GltfModelEntity, or MeshEntity"
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
                @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
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
                    prevParent?.disposeInternal()
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
        internal const val MAX_PLANE_ANCHOR_DISTANCE = 0.2f

        /** Factory function for creating a MovableComponent. */
        internal fun create(
            session: Session,
            entityRegistry: EntityRegistry,
            systemMovable: Boolean = true,
            scaleInZ: Boolean = true,
            anchorPlacement: Set<AnchorPlacement> = emptySet(),
            disposeParentOnReAnchor: Boolean = true,
            initialListener: EntityMoveListener? = null,
            initialListenerExecutor: Executor? = null,
            trackable: Trackable<Trackable.State>? = null,
            poseExtractor: ((Any?) -> Pose?)? = null,
        ): MovableComponent {
            return MovableComponent(
                session,
                entityRegistry,
                systemMovable,
                scaleInZ,
                anchorPlacement,
                disposeParentOnReAnchor,
                initialListener,
                initialListenerExecutor,
                trackable,
                poseExtractor,
            )
        }

        /**
         * Public factory function for creating a MovableComponent.
         *
         * This [Component] can be attached to a single instance of an [Entity]. When attached, this
         * Component will enable the user to translate the Entity by pointing and dragging on it.
         *
         * When created with this function the MovableComponent will not move or rescale the Entity,
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
            create(
                session = session,
                entityRegistry = session.scene.entityRegistry,
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
            create(
                session = session,
                entityRegistry = session.scene.entityRegistry,
                systemMovable = true,
                scaleInZ = scaleInZ,
            )

        /**
         * Creates a [MovableComponent] that allows an entity's pose to be driven by an external
         * ARCore [Trackable].
         *
         * This factory is designed for scenarios where an entity needs to continuously track a pose
         * provided by an ARCore data source, such as the user's hand joint from [Hand].
         *
         * Once this component is created and attached to an entity, it will automatically start
         * collecting pose updates. The collection operation is internally managed and tied to the
         * component's attachment lifecycle. The provided `poseExtractor` function is invoked on the
         * main thread during the frame update loop whenever the underlying [Trackable] emits a new
         * state. This invocation begins when the component is attached to an [Entity] and stops
         * when it is detached.
         *
         * If the `poseExtractor` returns `null` (e.g., if a valid pose cannot be extracted from the
         * current state), the system silently does nothing and the entity's pose remains unchanged.
         * Note that any exceptions thrown by the `poseExtractor` are not caught by the runtime and
         * will propagate, potentially crashing the application.
         *
         * The default implementation of `poseExtractor` extracts a pose from the following states:
         * [AugmentedObject.State] (using `centerPose`), [Eye.State] (using `pose`), [Plane.State]
         * (using `centerPose`), and [Hand.State] (using the pose of the [HandJointType.PALM]
         * joint). For any other state types, the default implementation returns null, resulting in
         * no pose updates. You must provide a custom `poseExtractor` for unlisted types.
         *
         * @param T The type of the state emitted by the source [Trackable].
         * @param session The active [Session] instance.
         * @param trackable A [Trackable] that provides a continuous stream of state updates.
         * @param poseExtractor A [Function] that extracts a nullable [Pose] from the given source
         *   state `T`.
         * @return A new instance of [MovableComponent] configured for automatic, perception-driven
         *   movement.
         */
        @JvmOverloads
        @JvmStatic
        public fun <T : Trackable.State> createTrackingMovable(
            session: Session,
            trackable: Trackable<T>,
            poseExtractor: Function<T, Pose?> = Function { state ->
                when (state) {
                    is AugmentedObject.State -> {
                        state.centerPose
                    }
                    is Eye.State -> {
                        state.pose
                    }
                    is Hand.State -> {
                        state.handJoints[HandJointType.PALM]
                    }
                    is Plane.State -> {
                        state.centerPose
                    }
                    else -> {
                        null
                    }
                }
            },
        ): MovableComponent =
            create(
                session = session,
                entityRegistry = session.scene.entityRegistry,
                trackable = trackable,
                // Suppressing UNCHECKED_CAST due to runtime type erasure.
                // This cast is safe because the ARCore Trackable interface contract strictly
                // guarantees that the `trackable.state` Flow will only emit objects of type `T`.
                poseExtractor = { state ->
                    @Suppress("UNCHECKED_CAST") poseExtractor.apply(state as T)
                },
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
         * This functionality requires [Session] to be called with
         * [androidx.xr.runtime.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL]. This configuration
         * requires that the `SCENE_UNDERSTANDING_COARSE` Android permission is granted. If not
         * granted, the `anchorable` functionality will be disabled, and the element will behave as
         * if the anchoring functionality was not applied.
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
            return create(
                session = session,
                entityRegistry = session.scene.entityRegistry,
                systemMovable = true,
                scaleInZ = false,
                anchorPlacement = anchorPlacement,
                disposeParentOnReAnchor = disposeParentOnReAnchor,
            )
        }
    }
}
