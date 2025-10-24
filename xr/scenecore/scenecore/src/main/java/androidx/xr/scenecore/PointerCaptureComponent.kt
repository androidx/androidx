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

import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.InputEventListener as RtInputEventListener
import androidx.xr.scenecore.runtime.PointerCaptureComponent as RtPointerCaptureComponent
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Provides pointer capture capabilities for a given [Entity].
 *
 * To enable pointer capture, the task must be in full space, the entity must be visible, and the
 * [PointerCaptureComponent] must be attached to the entity.
 *
 * Only one PointerCaptureComponent can be attached to an entity at a given time. If a second one
 * tries to attach to an entity, it will fail.
 */
public class PointerCaptureComponent
private constructor(
    private val sceneRuntime: SceneRuntime,
    private val entityManager: EntityManager,
    private val executor: Executor,
    private val stateListener: Consumer<PointerCaptureState>,
    private val inputEventListener: Consumer<InputEvent>,
) : Component {

    /** Defines the possible states of a [PointerCaptureComponent]. */
    public class PointerCaptureState private constructor(private val name: String) {

        public companion object {
            /**
             * Pointer Capture is temporarily disabled for this component. The component can resume
             * capture from this state.
             */
            @JvmField public val PAUSED: PointerCaptureState = PointerCaptureState("PAUSED")

            /** Pointer Capture is enabled for this component. */
            @JvmField public val ACTIVE: PointerCaptureState = PointerCaptureState("STOPPED")

            /**
             * Pointer Capture has been stopped for this component and no more callbacks will get
             * triggered. The component will not recover from this state. This can occur if the
             * underlying system replaces this pointer capture request by another one.
             */
            @JvmField public val STOPPED: PointerCaptureState = PointerCaptureState("STOPPED")
        }

        override fun toString(): String = name
    }

    private var attachedEntity: Entity? = null

    private val rtInputEventListener = RtInputEventListener { rtEvent ->
        inputEventListener.accept(rtEvent.toInputEvent(entityManager))
    }

    private val rtStateListener =
        RtPointerCaptureComponent.StateListener { pcState: Int ->
            when (pcState) {
                RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED ->
                    stateListener.accept(PointerCaptureState.PAUSED)
                RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE ->
                    stateListener.accept(PointerCaptureState.ACTIVE)
                RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED ->
                    stateListener.accept(PointerCaptureState.STOPPED)
                else -> {
                    // Unreachable
                }
            }
        }

    private val rtComponent by lazy {
        sceneRuntime.createPointerCaptureComponent(executor, rtStateListener, rtInputEventListener)
    }

    override fun onAttach(entity: Entity): Boolean {
        if (attachedEntity != null) {
            return false
        }
        attachedEntity = entity

        return (entity as BaseEntity<*>).rtEntity!!.addComponent(rtComponent)
    }

    override fun onDetach(entity: Entity) {
        if (entity != attachedEntity) {
            return
        }
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtComponent)
        attachedEntity = null
    }

    public companion object {
        /**
         * Creates a new instance of [PointerCaptureComponent].
         *
         * @param session The active [Session] for the scene.
         * @param executor The [Executor] on which the listener callbacks will be invoked.
         * @param stateListener A [Consumer] to receive updates when the pointer capture state
         *   changes (e.g., from active to paused).
         * @param inputListener A [Consumer] to receive all [InputEvent]s while pointer capture is
         *   active for the attached entity.
         * @return A new instance of [PointerCaptureComponent].
         */
        @JvmStatic
        public fun create(
            session: Session,
            executor: Executor,
            stateListener: Consumer<PointerCaptureState>,
            inputListener: Consumer<InputEvent>,
        ): PointerCaptureComponent =
            PointerCaptureComponent(
                session.sceneRuntime,
                session.scene.entityManager,
                executor,
                stateListener,
                inputListener,
            )
    }
}
