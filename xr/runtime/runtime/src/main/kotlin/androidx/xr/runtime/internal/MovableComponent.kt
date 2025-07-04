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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/** Component to enable a high level user movement affordance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface MovableComponent : Component {

    /**
     * Sets the scale with distance mode.
     *
     * @param scaleWithDistanceMode The scale with distance mode to set
     */
    @ScaleWithDistanceMode public var scaleWithDistanceMode: Int

    /** Sets the size of the interaction highlight extent. */
    public var size: Dimensions

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
    public fun addMoveEventListener(executor: Executor, moveEventListener: MoveEventListener)

    /**
     * Removes the listener from the set of active listeners for the move events.
     *
     * @param moveEventListener the move event listener to remove
     */
    public fun removeMoveEventListener(moveEventListener: MoveEventListener)

    /**
     * Modes for scaling the entity as the user moves it closer and further away. *
     *
     * <p>DEFAULT: The panel scales in the same way as home space mode.
     *
     * <p>DMM: The panel scales in a way that the user-perceived panel size never changes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class ScaleWithDistanceMode {
        public companion object {
            public const val DEFAULT: Int = 3
            public const val DMM: Int = 2
        }
    }
}
