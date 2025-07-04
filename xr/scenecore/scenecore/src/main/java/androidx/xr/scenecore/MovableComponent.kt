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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.MoveEventListener as RtMoveEventListener
import androidx.xr.runtime.math.FloatSize3d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * This [Component] can be attached to a single instance of an [Entity]. When attached, this
 * Component will enable the user to translate the Entity by pointing and dragging on it.
 *
 * This component cannot be attached to an [AnchorEntity] or to the [ActivitySpace]. Calling
 * [Entity.addComponent] to an Entity with these types will return false.
 *
 * NOTE: This Component is currently unsupported on [GltfModelEntity].
 */
public class MovableComponent
private constructor(
    private val platformAdapter: JxrPlatformAdapter,
    private val entityManager: EntityManager,
    private val systemMovable: Boolean = true,
    private val scaleInZ: Boolean = true,
    private val anchorPlacement: Set<AnchorPlacement> = emptySet(),
    private val disposeParentOnReAnchor: Boolean = true,
    private val initialListener: EntityMoveListener? = null,
    private val initialListenerExecutor: Executor? = null,
) : Component {
    private val rtMovableComponent by lazy {
        platformAdapter.createMovableComponent(
            systemMovable,
            scaleInZ,
            anchorPlacement.toRtAnchorPlacement(platformAdapter),
            disposeParentOnReAnchor,
        )
    }
    private val moveListenersMap = ConcurrentHashMap<EntityMoveListener, RtMoveEventListener>()

    private var entity: Entity? = null

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
        val attached = (entity as BaseEntity<*>).rtEntity.addComponent(rtMovableComponent)
        if (attached && initialListener != null) {
            if (initialListenerExecutor != null) {
                addMoveListener(initialListenerExecutor, initialListener)
            } else {
                addMoveListener(initialListener)
            }
        }
        return attached
    }

    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity.removeComponent(rtMovableComponent)
        this.entity = null
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
        val rtMoveEventListener = RtMoveEventListener { rtMoveEvent ->
            run {
                // TODO: b/369157703 - Mirror the callback hierarchy in the runtime API.
                val moveEvent = rtMoveEvent.toMoveEvent(entityManager)
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
                                moveEvent.currentPose,
                                moveEvent.currentScale,
                            )
                        }

                    MoveEvent.MOVE_STATE_END ->
                        entity?.let {
                            entityMoveListener.onMoveEnd(
                                it,
                                moveEvent.currentInputRay,
                                moveEvent.currentPose,
                                moveEvent.currentScale,
                                moveEvent.updatedParent,
                            )
                        }
                }
            }
        }
        rtMovableComponent.addMoveEventListener(executor, rtMoveEventListener)
        moveListenersMap[entityMoveListener] = rtMoveEventListener
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
        val rtMoveEventListener = moveListenersMap.remove(entityMoveListener)
        if (rtMoveEventListener != null) {
            rtMovableComponent.removeMoveEventListener(rtMoveEventListener)
        }
    }

    public companion object {
        private val kDimensionsOneMeter = FloatSize3d(1f, 1f, 1f)

        /** Factory function for creating a MovableComponent. */
        internal fun create(
            platformAdapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            systemMovable: Boolean = true,
            scaleInZ: Boolean = true,
            anchorPlacement: Set<AnchorPlacement> = emptySet(),
            disposeParentOnReAnchor: Boolean = true,
            initialListener: EntityMoveListener? = null,
            initialListenerExecutor: Executor? = null,
        ): MovableComponent {
            return MovableComponent(
                platformAdapter,
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
                platformAdapter = session.platformAdapter,
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
                platformAdapter = session.platformAdapter,
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
                platformAdapter = session.platformAdapter,
                entityManager = session.scene.entityManager,
                systemMovable = true,
                scaleInZ = false,
                anchorPlacement = anchorPlacement,
                disposeParentOnReAnchor = disposeParentOnReAnchor,
            )
        }
    }
}
