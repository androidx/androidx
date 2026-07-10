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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.MovableComponent
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.MoveEventListener
import androidx.xr.scenecore.testing.internal.FakeMovableComponent as InternalFakeMovableComponent
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.MovableComponent] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeMovableComponent
internal constructor(internal var fakeInternal: InternalFakeMovableComponent) :
    FakeComponent(), MovableComponent {
    public constructor() : this(InternalFakeMovableComponent())

    /**
     * This property reflects the `systemMovable` parameter that was passed to the runtime's factory
     * method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to verify that
     * the component was created with the correct configuration.
     */
    public var systemMovable: Boolean
        get() = fakeInternal.systemMovable
        internal set(value) {
            fakeInternal.systemMovable = value
        }

    /**
     * This property reflects the `scaleInZ` parameter that was passed to the runtime's factory
     * method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to verify that
     * the component was created with the correct configuration.
     */
    public var scaleInZ: Boolean
        get() = fakeInternal.scaleInZ
        internal set(value) {
            fakeInternal.scaleInZ = value
        }

    /**
     * This property reflects the `userAnchorable` parameter that was passed to the runtime's
     * factory method [FakeSceneRuntime.createMovableComponent]. Tests can inspect this value to
     * verify that the component was created with the correct configuration.
     */
    public var userAnchorable: Boolean
        get() = fakeInternal.userAnchorable
        internal set(value) {
            fakeInternal.userAnchorable = value
        }

    /** Sets the scale with distance mode. */
    override var scaleWithDistanceMode: Int
        get() = fakeInternal.scaleWithDistanceMode
        set(value) {
            fakeInternal.scaleWithDistanceMode = value
        }

    /** Sets the size of the interaction highlight extent. */
    override var size: Dimensions
        get() = fakeInternal.size
        set(value) {
            fakeInternal.size = value
        }

    /** The default executor for the component */
    public var defaultExecutor: Executor
        get() = fakeInternal.defaultExecutor
        set(value) {
            fakeInternal.defaultExecutor = value
        }

    /**
     * For test purposes only.
     *
     * A map of move event listeners to their executors.
     */
    internal val moveEventListenersMap: MutableMap<MoveEventListener, Executor>
        get() = fakeInternal.moveEventListenersMap

    /** The number of times setPlanePoseForMoveUpdatePose is called */
    public var setPlanePoseForMoveUpdatePoseCallCount: Long
        get() = fakeInternal.setPlanePoseForMoveUpdatePoseCallCount
        set(value) {
            fakeInternal.setPlanePoseForMoveUpdatePoseCallCount = value
        }

    /** The last plane pose set by setPlanePoseForMoveUpdatePose */
    public var lastPlanePose: Pose?
        get() = fakeInternal.lastPlanePose
        set(value) {
            fakeInternal.lastPlanePose = value
        }

    /**
     * Adds the listener to the set of active listeners for the move events.
     *
     * <p>The listener is invoked on the default executor of the runtime.
     *
     * @param moveEventListener The move event listener to set.
     */
    override fun addMoveEventListener(moveEventListener: MoveEventListener) {
        fakeInternal.addMoveEventListener(moveEventListener)
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
        fakeInternal.addMoveEventListener(executor, moveEventListener)
    }

    /**
     * Removes the listener from the set of active listeners for the move events.
     *
     * @param moveEventListener the move event listener to remove
     */
    override fun removeMoveEventListener(moveEventListener: MoveEventListener) {
        fakeInternal.removeMoveEventListener(moveEventListener)
    }

    override fun setPlanePoseForMoveUpdatePose(planePose: Pose?, moveUpdatePose: Pose) {
        fakeInternal.setPlanePoseForMoveUpdatePose(planePose, moveUpdatePose)
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
    public fun onMoveEvent(event: MoveEvent) {
        fakeInternal.onMoveEvent(event)
    }
}
