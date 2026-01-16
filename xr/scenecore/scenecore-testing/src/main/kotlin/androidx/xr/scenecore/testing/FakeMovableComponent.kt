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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.MoveEventListener
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MovableComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeMovableComponent : FakeComponent(), MovableComponent {

    /**
     * This property reflects the `systemMovable` parameter that was passed to the runtime's factory
     * method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to verify that
     * the component was created with the correct configuration.
     */
    public var systemMovable: Boolean = false
        internal set

    /**
     * This property reflects the `scaleInZ` parameter that was passed to the runtime's factory
     * method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to verify that
     * the component was created with the correct configuration.
     */
    public var scaleInZ: Boolean = false
        internal set

    /**
     * This property reflects the `userAnchorable` parameter that was passed to the runtime's
     * factory method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to
     * verify that the component was created with the correct configuration.
     */
    public var userAnchorable: Boolean = false
        internal set

    /**
     * Sets the scale with distance mode.
     *
     * @param scaleWithDistanceMode The scale with distance mode to set
     */
    override var scaleWithDistanceMode: Int = MovableComponent.ScaleWithDistanceMode.DEFAULT

    /** Sets the size of the interaction highlight extent. */
    override var size: Dimensions = Dimensions(2.0f, 1.0f, 0.0f)

    /** The default executor for the component */
    public var defaultExecutor: Executor = FakeScheduledExecutorService()

    /**
     * For test purposes only.
     *
     * A map of move event listeners to their executors.
     */
    internal val moveEventListenersMap: MutableMap<MoveEventListener, Executor> = mutableMapOf()

    /** The number of times setPlanePoseForMoveUpdatePose is called */
    public var setPlanePoseForMoveUpdatePoseCallCount: Long = 0

    /** The last plane pose set by setPlanePoseForMoveUpdatePose */
    public var lastPlanePose: Pose? = null

    /**
     * Adds the listener to the set of active listeners for the move events.
     *
     * <p>The listener is invoked on the default executor of the runtime.
     *
     * @param moveEventListener The move event listener to set.
     */
    override fun addMoveEventListener(moveEventListener: MoveEventListener) {
        moveEventListenersMap.put(moveEventListener, defaultExecutor)
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
     * @param event The new [InputEvent] to be sent in the simulated event.
     */
    public fun onMoveEvent(event: MoveEvent) {
        // Note that MovableComponent uses HandlerExecutor.mainThreadExecutor as the default
        // executor, which doesn't work in the fake runtime. So we trigger the listener callback
        // function directly instead of executor.execute { listener.onMoveEvent(event) }.
        moveEventListenersMap.forEach { entry -> entry.key.onMoveEvent(event) }
    }
}
