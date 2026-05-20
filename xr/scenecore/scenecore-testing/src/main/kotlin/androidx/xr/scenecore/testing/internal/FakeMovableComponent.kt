/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testing.internal

import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.MoveEventListener
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MovableComponent] */
internal class FakeMovableComponent : FakeComponent(), MovableComponent {

    /**
     * This property reflects the `systemMovable` parameter that was passed to the runtime's factory
     * method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to verify that
     * the component was created with the correct configuration.
     */
    var systemMovable: Boolean = false
        internal set

    /**
     * This property reflects the `scaleInZ` parameter that was passed to the runtime's factory
     * method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to verify that
     * the component was created with the correct configuration.
     */
    var scaleInZ: Boolean = false
        internal set

    /**
     * This property reflects the `userAnchorable` parameter that was passed to the runtime's
     * factory method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to
     * verify that the component was created with the correct configuration.
     */
    var userAnchorable: Boolean = false
        internal set

    override var scaleWithDistanceMode: Int = MovableComponent.ScaleWithDistanceMode.DEFAULT

    /** Sets the size of the interaction highlight extent. */
    override var size: Dimensions = Dimensions(2.0f, 1.0f, 0.0f)

    /** The default executor for the component */
    var defaultExecutor: Executor = FakeScheduledExecutorService()

    /**
     * For test purposes only.
     *
     * A map of move event listeners to their executors.
     */
    val moveEventListenersMap: MutableMap<MoveEventListener, Executor> = mutableMapOf()

    /** The number of times setPlanePoseForMoveUpdatePose is called */
    var setPlanePoseForMoveUpdatePoseCallCount: Long = 0

    /** The last plane pose set by setPlanePoseForMoveUpdatePose */
    var lastPlanePose: Pose? = null

    /**
     * Adds the listener to the set of active listeners for the move events.
     *
     * <p>The listener is invoked on the default executor of the runtime.
     *
     * @param moveEventListener The move event listener to set.
     */
    override fun addMoveEventListener(moveEventListener: MoveEventListener) {
        moveEventListenersMap[moveEventListener] = defaultExecutor
    }

    /**
     * Adds the listener to the set of active listeners for the move events.
     *
     * <p>The listener is invoked on the provided executor. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * @param executor The executor to run the listener on.
     * @param moveEventListener The move event listener to set.
     */
    @Suppress("ExecutorRegistration")
    override fun addMoveEventListener(executor: Executor, moveEventListener: MoveEventListener) {
        moveEventListenersMap[moveEventListener] = executor
    }

    /**
     * Removes the listener from the set of active listeners for the move events.
     *
     * @param moveEventListener the move event listener to remove
     */
    override fun removeMoveEventListener(moveEventListener: MoveEventListener) {
        moveEventListenersMap.remove(moveEventListener)
    }

    override fun setPlanePoseForMoveUpdatePose(planePose: Pose?, moveUpdatePose: Pose) {
        setPlanePoseForMoveUpdatePoseCallCount++
        lastPlanePose = planePose
    }

    /**
     * Simulates a move event from the runtime, notifying all registered listeners.
     *
     * This function is intended for testing purposes to allow manual triggering of the update
     * mechanism. It iterates through all currently registered listeners and invokes their
     * `onMoveEvent` method.
     *
     * @param event The new [MoveEvent] to be sent in the simulated event.
     */
    fun onMoveEvent(event: MoveEvent) {
        moveEventListenersMap.forEach { (listener, executor) ->
            executor.execute { listener.onMoveEvent(event) }
            // If the executor is our fake service, we manually trigger all tasks to ensure
            // the listener callback is executed immediately and deterministically within the
            // calling thread of the test.
            if (executor is FakeScheduledExecutorService) {
                executor.runAll()
            }
        }
    }
}
