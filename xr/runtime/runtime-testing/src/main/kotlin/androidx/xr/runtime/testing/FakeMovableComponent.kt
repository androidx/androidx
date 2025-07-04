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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.MovableComponent
import androidx.xr.runtime.internal.MoveEventListener
import java.util.concurrent.Executor

/** Test-only implementation of [MovableComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeMovableComponent : FakeComponent(), MovableComponent {

    /**
     * Sets the scale with distance mode.
     *
     * @param scaleWithDistanceMode The scale with distance mode to set
     */
    override var scaleWithDistanceMode: Int = MovableComponent.ScaleWithDistanceMode.DEFAULT

    /** Sets the size of the interaction highlight extent. */
    override var size: Dimensions = Dimensions(2.0f, 1.0f, 0.0f)

    /**
     * For test purposes only.
     *
     * A map of move event listeners to their executors.
     */
    public val moveEventListenersMap: MutableMap<MoveEventListener, Executor> = mutableMapOf()

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
        moveEventListenersMap.put(moveEventListener, executor)
    }

    /**
     * Removes the listener from the set of active listeners for the move events.
     *
     * @param moveEventListener the move event listener to remove
     */
    override fun removeMoveEventListener(moveEventListener: MoveEventListener) {
        moveEventListenersMap.remove(moveEventListener)
    }
}
