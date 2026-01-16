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

import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * A component that monitors the bounds of an entity and notifies registered listeners of changes.
 *
 * The bounds are represented as an axis-aligned bounding box (AABB). This component acts as a
 * bridge to the underlying runtime, receiving bounds update events. These events are triggered
 * whenever the entity's transformation changes, which can be caused by animations, direct pose
 * manipulation, or changes to its parent's transformation.
 *
 * **Note:** Currently, this component can only be attached to a [GltfModelEntity]. Attaching it to
 * other entity types will fail.
 *
 * To receive updates, register a `BiConsumer<Entity, BoundingBox>` listener using the
 * [addOnBoundsUpdateListener] method. The listener will be invoked on the provided [Executor],
 * which defaults to the main thread. Listeners should be unregistered using
 * [removeOnBoundsUpdateListener] when no longer needed to prevent resource leaks.
 *
 * Create instances of this component using the [BoundsComponent.create] factory method.
 *
 * @see GltfModelEntity
 * @see BoundingBox
 */
public class BoundsComponent
private constructor(
    private val sceneRuntime: SceneRuntime,
    private val initialListenerExecutor: Executor? = null,
    private val initialListener: BiConsumer<Entity, BoundingBox>? = null,
) : Component {

    internal val rtBoundsComponent by lazy { sceneRuntime.createBoundsComponent() }
    internal val boundsUpdateListenerMap =
        ConcurrentHashMap<BiConsumer<Entity, BoundingBox>, Consumer<BoundingBox>>()

    private var entity: Entity? = null

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null || entity !is GltfModelEntity) {
            return false
        }
        this.entity = entity
        val attached = (entity as BaseEntity<*>).rtEntity!!.addComponent(rtBoundsComponent)
        if (attached && initialListener != null) {
            if (initialListenerExecutor != null) {
                addOnBoundsUpdateListener(initialListenerExecutor, initialListener)
            } else {
                addOnBoundsUpdateListener(initialListener)
            }
        }

        return attached
    }

    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtBoundsComponent)
        this.entity = null
    }

    /**
     * Registers a listener to receive bounds updates.
     *
     * The listener's `accept(Entity, BoundingBox)` method will be invoked on the specified
     * [Executor] whenever the entity's bounds change. The updated bounds are provided as a
     * [BoundingBox].
     *
     * Each listener instance can only be registered once. Registering the same listener instance
     * multiple times will have no effect.
     *
     * @param listener The `BiConsumer` to be invoked with the entity and its updated bounds.
     * @param executor The executor on which the listener callbacks will be invoked.
     * @see removeOnBoundsUpdateListener
     */
    public fun addOnBoundsUpdateListener(
        executor: Executor,
        listener: BiConsumer<Entity, BoundingBox>,
    ) {
        // Prevent creating multiple rtListeners of one listener.
        if (boundsUpdateListenerMap.containsKey(listener)) {
            return
        }
        val rtListener =
            Consumer<BoundingBox> { boundingBox ->
                entity?.let { listener.accept(it, boundingBox) }
            }
        rtBoundsComponent.addOnBoundsUpdateListener(executor, rtListener)
        boundsUpdateListenerMap[listener] = rtListener
    }

    /**
     * Registers a listener that will be invoked on the main application thread for bounds updates.
     *
     * This is a convenience overload of [addOnBoundsUpdateListener] that defaults to using the main
     * thread executor, which is useful for performing UI updates in response to bounds changes.
     *
     * @param listener The `BiConsumer` to be invoked with bounds updates on the main thread.
     */
    public fun addOnBoundsUpdateListener(listener: BiConsumer<Entity, BoundingBox>) {
        addOnBoundsUpdateListener(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Unregisters a previously registered bounds update listener.
     *
     * The specified listener will no longer receive bounds updates. It is important to call this
     * method when the listener is no longer needed to prevent potential resource and memory leaks.
     *
     * If the listener was not previously registered, this method has no effect.
     *
     * @param listener The `BiConsumer` instance to unregister. This must be the same object
     *   instance that was passed to [addOnBoundsUpdateListener].
     */
    public fun removeOnBoundsUpdateListener(listener: BiConsumer<Entity, BoundingBox>) {
        val rtListener = boundsUpdateListenerMap.remove(listener)
        if (rtListener != null) {
            rtBoundsComponent.removeOnBoundsUpdateListener(rtListener)
        }
    }

    public companion object {
        internal fun create(
            sceneRuntime: SceneRuntime,
            executor: Executor? = null,
            listener: BiConsumer<Entity, BoundingBox>? = null,
        ): BoundsComponent = BoundsComponent(sceneRuntime, executor, listener)

        /**
         * Creates a [BoundsComponent].
         *
         * @param session The active [Session].
         * @return A new [BoundsComponent] instance.
         */
        @JvmStatic
        public fun create(session: Session): BoundsComponent {
            return create(session.sceneRuntime)
        }
    }
}
