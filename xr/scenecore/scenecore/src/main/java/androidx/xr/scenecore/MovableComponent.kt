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

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * Allows users to interactively move the Entity. This component can be attached to a single
 * instance of any PanelEntity.
 *
 * NOTE: This Component is currently unsupported on GltfModelEntity.
 */
public class MovableComponent
private constructor(
    private val platformAdapter: JxrPlatformAdapter,
    private val entityManager: EntityManager,
    private val systemMovable: Boolean = true,
    private val scaleInZ: Boolean = true,
    private val anchorPlacement: Set<AnchorPlacement> = emptySet(),
    private val shouldDisposeParentAnchor: Boolean = true,
) : Component {
    private val rtMovableComponent by lazy {
        platformAdapter.createMovableComponent(
            systemMovable,
            scaleInZ,
            anchorPlacement.toRtAnchorPlacement(platformAdapter),
            shouldDisposeParentAnchor,
        )
    }
    private val moveListenersMap =
        ConcurrentHashMap<MoveListener, JxrPlatformAdapter.MoveEventListener>()

    private var entity: Entity? = null

    /**
     * The current size of the entity, in meters. The size of the entity determines the size of the
     * bounding box that is used to draw the draggable move affordances around the entity.
     */
    public var size: Dimensions = kDimensionsOneMeter
        set(value) {
            if (field != value) {
                field = value
                rtMovableComponent.setSize(value.toRtDimensions())
            }
        }

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            Log.e("MovableComponent", "Already attached to entity ${this.entity}")
            return false
        }
        this.entity = entity
        return (entity as BaseEntity<*>).rtEntity.addComponent(rtMovableComponent)
    }

    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity.removeComponent(rtMovableComponent)
        this.entity = null
    }

    /**
     * Adds a listener to the set of active listeners for the move events. The listener will be
     * invoked regardless of whether the entity is being moved by the system or the user.
     *
     * <p>The listener is invoked on the provided executor. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * @param executor The executor to run the listener on.
     * @param moveListener The move event listener to set.
     */
    public fun addMoveListener(executor: Executor, moveListener: MoveListener) {
        val rtMoveEventListener =
            JxrPlatformAdapter.MoveEventListener { rtMoveEvent ->
                run {
                    // TODO: b/369157703 - Mirror the callback hierarchy in the runtime API.
                    val moveEvent = rtMoveEvent.toMoveEvent(entityManager)
                    when (moveEvent.moveState) {
                        MoveEvent.MOVE_STATE_START ->
                            entity?.let {
                                moveListener.onMoveStart(
                                    it,
                                    moveEvent.initialInputRay,
                                    moveEvent.previousPose,
                                    moveEvent.previousScale,
                                    moveEvent.initialParent,
                                )
                            }
                        MoveEvent.MOVE_STATE_ONGOING ->
                            entity?.let {
                                moveListener.onMoveUpdate(
                                    it,
                                    moveEvent.currentInputRay,
                                    moveEvent.currentPose,
                                    moveEvent.currentScale,
                                )
                            }
                        MoveEvent.MOVE_STATE_END ->
                            entity?.let {
                                moveListener.onMoveEnd(
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
        moveListenersMap[moveListener] = rtMoveEventListener
    }

    /**
     * Adds a listener to the set of active listeners for the move events. The listener will be
     * invoked regardless of whether the entity is being moved by the system or the user.
     *
     * <p>The listener is invoked on the main thread.
     *
     * @param moveListener The move event listener to set.
     */
    public fun addMoveListener(moveListener: MoveListener) {
        addMoveListener(HandlerExecutor.mainThreadExecutor, moveListener)
    }

    /**
     * Removes a listener from the set of active listeners for the move events.
     *
     * @param moveListener The move event listener to remove.
     */
    public fun removeMoveListener(moveListener: MoveListener) {
        val rtMoveEventListener = moveListenersMap.remove(moveListener)
        if (rtMoveEventListener != null) {
            rtMovableComponent.removeMoveEventListener(rtMoveEventListener)
        }
    }

    public companion object {
        private val kDimensionsOneMeter = Dimensions(1f, 1f, 1f)

        /** Factory function for creating a MovableComponent. */
        internal fun create(
            platformAdapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            systemMovable: Boolean = true,
            scaleInZ: Boolean = true,
            anchorPlacement: Set<AnchorPlacement> = emptySet(),
            shouldDisposeParentAnchor: Boolean = true,
        ): MovableComponent {
            return MovableComponent(
                platformAdapter,
                entityManager,
                systemMovable,
                scaleInZ,
                anchorPlacement,
                shouldDisposeParentAnchor,
            )
        }

        /**
         * Public factory function for creating a MovableComponent. This component can be attached
         * to a single instance of any non-Anchor Entity.
         *
         * When attached, this Component will enable the user to translate the Entity by pointing
         * and dragging on it.
         *
         * @param session The [Session] instance.
         * @param systemMovable A [Boolean] which causes the system to automatically apply transform
         *   updates to the entity in response to user interaction.
         * @param scaleInZ A [Boolean] which tells the system to update the scale of the Entity as
         *   the user moves it closer and further away. This is mostly useful for Panel
         *   auto-rescaling with Distance
         * @param anchorPlacement A Set containing different [AnchorPlacement] for how to anchor the
         *   [Entity] movable component. If this is not empty the movement semantics will be
         *   slightly different from the system as it will add the ability to anchor to nearby
         *   planes.
         * @param shouldDisposeParentAnchor A [Boolean], which if set to true, when an entity is
         *   moved off of an [AnchorEntity] that was created by the underlying [MovableComponent],
         *   and the [AnchorEntity] has no other children, the AnchorEntity will be disposed, and
         *   the underlying Anchor will be detached.
         * @return [MovableComponent] instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            systemMovable: Boolean = true,
            scaleInZ: Boolean = true,
            anchorPlacement: Set<AnchorPlacement> = emptySet(),
            shouldDisposeParentAnchor: Boolean = true,
        ): MovableComponent =
            MovableComponent.create(
                platformAdapter = session.platformAdapter,
                entityManager = session.entityManager,
                systemMovable = systemMovable,
                scaleInZ = scaleInZ,
                anchorPlacement = anchorPlacement,
                shouldDisposeParentAnchor = shouldDisposeParentAnchor,
            )
    }
}
