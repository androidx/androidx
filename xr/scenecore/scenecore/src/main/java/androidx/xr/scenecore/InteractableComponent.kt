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

package androidx.xr.scenecore

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.InputEventListener as RtInputEventListener
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Provides access to raw [InputEvent]s for given [Entity], so a client can implement their own
 * interaction logic.
 */
public class InteractableComponent
private constructor(
    private val sceneRuntime: SceneRuntime,
    private val entityManager: EntityManager,
    private val executor: Executor,
    private val inputEventListener: Consumer<InputEvent>,
) : Component {
    private val rtInputEventListener = RtInputEventListener { rtEvent ->
        inputEventListener.accept(rtEvent.toInputEvent(entityManager))
    }
    private val rtInteractableComponent by lazy {
        sceneRuntime.createInteractableComponent(executor, rtInputEventListener)
    }
    private var entity: Entity? = null

    /**
     * Attaches this component to the given [Entity].
     *
     * @param entity The [Entity] to attach this component to.
     * @return `true` if the component was successfully attached, `false` otherwise.
     */
    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            Log.e("InteractableComponent", "Already attached to entity ${this.entity}")
            return false
        }
        this.entity = entity
        return (entity as BaseEntity<*>).rtEntity!!.addComponent(rtInteractableComponent)
    }

    /**
     * Detaches this component from the given [Entity].
     *
     * @param entity The [Entity] to detach this component from.
     */
    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtInteractableComponent)
        this.entity = null
    }

    public companion object {
        /** Factory for Interactable component. */
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityManager: EntityManager,
            executor: Executor,
            inputEventListener: Consumer<InputEvent>,
        ): InteractableComponent {
            return InteractableComponent(sceneRuntime, entityManager, executor, inputEventListener)
        }

        /**
         * Public factory for creating an InteractableComponent. It enables access to raw input
         * events.
         *
         * @param session [Session] to create the InteractableComponent in.
         * @param executor Executor for invoking the inputEventListener.
         * @param inputEventListener [Consumer] that accepts [InputEvent]s.
         */
        @JvmStatic
        public fun create(
            session: Session,
            executor: Executor,
            inputEventListener: Consumer<InputEvent>,
        ): InteractableComponent =
            create(session.sceneRuntime, session.scene.entityManager, executor, inputEventListener)

        /**
         * Public factory for creating an InteractableComponent. It enables access to raw input
         * events.
         *
         * @param session [Session] to create the InteractableComponent in.
         * @param inputEventListener [Consumer] that accepts [InputEvent]s. The listener callbacks
         *   will be invoked on the main thread.
         */
        @JvmStatic
        public fun create(
            session: Session,
            inputEventListener: Consumer<InputEvent>,
        ): InteractableComponent =
            create(session, ContextCompat.getMainExecutor(session.activity), inputEventListener)
    }
}
