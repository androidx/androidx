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
import androidx.xr.runtime.internal.ResizableComponent
import androidx.xr.runtime.internal.ResizeEventListener
import java.util.concurrent.Executor

/** Fake implementation of [ResizableComponent] for testing. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeResizableComponent(
    override var size: Dimensions = Dimensions(2.0f, 2.0f, 2.0f),
    override var minimumSize: Dimensions = Dimensions(1.0f, 1.0f, 1.0f),
    override var maximumSize: Dimensions = Dimensions(2.0f, 2.0f, 2.0f),
    override var fixedAspectRatio: Float = 20.0f,
    @get:Suppress("GetterSetterNames") override var autoHideContent: Boolean = false,
    @get:Suppress("GetterSetterNames") override var autoUpdateSize: Boolean = false,
    @get:Suppress("GetterSetterNames") override var forceShowResizeOverlay: Boolean = false,
) : FakeComponent(), ResizableComponent {

    /**
     * For test purposes only.
     *
     * Represents the set of listeners that are invoked through the resize operation. In tests, you
     * can use this map to manually trigger the listener and verify that your code responds
     * correctly to resize operation.
     *
     * <p>Map of resize event listeners to their executors.
     */
    public val resizeEventListenersMap: MutableMap<ResizeEventListener, Executor> = mutableMapOf()

    /**
     * Adds the listener to the set of listeners that are invoked through the resize operation, such
     * as start, ongoing and end.
     *
     * <p>The listener is invoked on the provided executor. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * @param executor The executor to use for the listener callback.
     * @param resizeEventListener The listener to be invoked when a resize event occurs.
     */
    @Suppress("ExecutorRegistration")
    override fun addResizeEventListener(
        executor: Executor,
        resizeEventListener: ResizeEventListener,
    ) {
        resizeEventListenersMap.put(resizeEventListener, executor)
    }

    /**
     * Removes the given listener from the set of listeners for the resize events.
     *
     * @param resizeEventListener The listener to be removed.
     */
    override fun removeResizeEventListener(resizeEventListener: ResizeEventListener) {
        resizeEventListenersMap.remove(resizeEventListener)
    }
}
