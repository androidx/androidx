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

package androidx.xr.scenecore

import android.util.Log
import java.util.concurrent.Executor

/**
 * Provides access to raw input events for given Entity, so a client can implement their own
 * interaction logic.
 */
public class InteractableComponent
private constructor(
    private val runtime: JxrPlatformAdapter,
    private val entityManager: EntityManager,
    private val executor: Executor,
    private val inputEventListener: InputEventListener,
) : Component {
    private val rtInputEventListener =
        JxrPlatformAdapter.InputEventListener { rtEvent ->
            inputEventListener.onInputEvent(rtEvent.toInputEvent(entityManager))
        }
    private val rtInteractableComponent by lazy {
        runtime.createInteractableComponent(executor, rtInputEventListener)
    }
    private var entity: Entity? = null

    /**
     * Attaches this component to the given entity.
     *
     * @param entity The entity to attach this component to.
     * @return `true` if the component was successfully attached, `false` otherwise.
     */
    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            Log.e("InteractableComponent", "Already attached to entity ${this.entity}")
            return false
        }
        this.entity = entity
        return (entity as BaseEntity<*>).rtEntity.addComponent(rtInteractableComponent)
    }

    /**
     * Detaches this component from the given entity.
     *
     * @param entity The entity to detach this component from.
     */
    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity.removeComponent(rtInteractableComponent)
        this.entity = null
    }

    public companion object {
        /** Factory for Interactable component. */
        internal fun create(
            runtime: JxrPlatformAdapter,
            entityManager: EntityManager,
            executor: Executor,
            inputEventListener: InputEventListener,
        ): InteractableComponent {
            return InteractableComponent(runtime, entityManager, executor, inputEventListener)
        }

        /**
         * Public factory for creating an [InteractableComponent]. It enables access to raw input
         * events.
         *
         * @param session [Session] to create the [InteractableComponent] in.
         * @param executor Executor for invoking [InputEventListener].
         * @param inputEventListener [InputEventListener] that accepts [InputEvent]s.
         * @return [InteractableComponent] instance.
         */
        @JvmStatic
        @Suppress("ExecutorRegistration")
        public fun create(
            session: Session,
            executor: Executor,
            inputEventListener: InputEventListener,
        ): InteractableComponent =
            InteractableComponent.create(
                session.platformAdapter,
                session.entityManager,
                executor,
                inputEventListener,
            )
    }
}
