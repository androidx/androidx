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
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/**
 * Provides pointer capture capabilities for a given entity.
 *
 * To enable pointer capture, the task must be in full space, and the entity must be visible.
 *
 * Only one PointerCaptureComponent can be attached to an entity at a given time. If a second one
 * tries to attach to an entity, it will fail.
 */
public class PointerCaptureComponent
private constructor(
    private val platformAdapter: JxrPlatformAdapter,
    private val entityManager: EntityManager,
    private val executor: Executor,
    private val stateListener: StateListener,
    private val inputEventListener: InputEventListener,
) : Component {

    private var attachedEntity: Entity? = null

    private val rtInputEventListener =
        JxrPlatformAdapter.InputEventListener { rtEvent ->
            inputEventListener.onInputEvent(rtEvent.toInputEvent(entityManager))
        }

    private val rtStateListener =
        JxrPlatformAdapter.PointerCaptureComponent.StateListener { pcState: Int ->
            when (pcState) {
                JxrPlatformAdapter.PointerCaptureComponent.POINTER_CAPTURE_STATE_PAUSED ->
                    stateListener.onStateChanged(POINTER_CAPTURE_STATE_PAUSED)
                JxrPlatformAdapter.PointerCaptureComponent.POINTER_CAPTURE_STATE_ACTIVE ->
                    stateListener.onStateChanged(POINTER_CAPTURE_STATE_ACTIVE)
                JxrPlatformAdapter.PointerCaptureComponent.POINTER_CAPTURE_STATE_STOPPED ->
                    stateListener.onStateChanged(POINTER_CAPTURE_STATE_STOPPED)
                else -> {
                    Log.e(TAG, "Unknown pointer capture state received: ${pcState}")
                    stateListener.onStateChanged(pcState)
                }
            }
        }

    private val rtComponent by lazy {
        platformAdapter.createPointerCaptureComponent(
            executor,
            rtStateListener,
            rtInputEventListener
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                POINTER_CAPTURE_STATE_PAUSED,
                POINTER_CAPTURE_STATE_ACTIVE,
                POINTER_CAPTURE_STATE_STOPPED
            ]
    )
    internal annotation class PointerCaptureState

    /** Listener for pointer capture state changes. */
    public interface StateListener {
        public fun onStateChanged(@PointerCaptureState newState: Int)
    }

    override fun onAttach(entity: Entity): Boolean {
        if (attachedEntity != null) {
            Log.e(TAG, "Already attached to entity ${attachedEntity}")
            return false
        }
        attachedEntity = entity

        return (entity as BaseEntity<*>).rtEntity.addComponent(rtComponent)
    }

    override fun onDetach(entity: Entity) {
        if (entity != attachedEntity) {
            Log.e(TAG, "Detaching from non-attached entity, ignoring")
            return
        }
        (entity as BaseEntity<*>).rtEntity.removeComponent(rtComponent)
        attachedEntity = null
    }

    public companion object {
        /** Pointer Capture is enabled for this component. */
        public const val POINTER_CAPTURE_STATE_PAUSED: Int = 0

        /** Pointer Capture is disabled for this component. */
        public const val POINTER_CAPTURE_STATE_ACTIVE: Int = 1

        /** Pointer Capture has been stopped for this component. */
        public const val POINTER_CAPTURE_STATE_STOPPED: Int = 2

        private const val TAG: String = "PointerCaptureComponent"

        /** Factory function for creating [PointerCaptureComponent] instances. */
        @JvmStatic
        public fun create(
            session: Session,
            executor: Executor,
            stateListener: StateListener,
            inputListener: InputEventListener,
        ): PointerCaptureComponent =
            PointerCaptureComponent(
                session.platformAdapter,
                session.entityManager,
                executor,
                stateListener,
                inputListener,
            )
    }
}
