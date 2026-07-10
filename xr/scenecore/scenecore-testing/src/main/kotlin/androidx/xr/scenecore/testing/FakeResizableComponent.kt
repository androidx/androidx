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
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.ResizableComponent
import androidx.xr.scenecore.runtime.ResizeEvent
import androidx.xr.scenecore.runtime.ResizeEventListener
import androidx.xr.scenecore.testing.internal.FakeResizableComponent as InternalFakeResizableComponent
import java.util.concurrent.Executor

/** Fake implementation of [androidx.xr.scenecore.runtime.ResizableComponent] for testing. */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeResizableComponent
internal constructor(internal val fakeInternal: InternalFakeResizableComponent) :
    FakeComponent(), ResizableComponent {
    public constructor(
        size: Dimensions = Dimensions(2.0f, 2.0f, 2.0f),
        minimumSize: Dimensions = Dimensions(1.0f, 1.0f, 1.0f),
        maximumSize: Dimensions = Dimensions(2.0f, 2.0f, 2.0f),
        isFixedAspectRatioEnabled: Boolean = false,
        autoHideContent: Boolean = false,
        autoUpdateSize: Boolean = false,
        forceShowResizeOverlay: Boolean = false,
    ) : this(
        InternalFakeResizableComponent(
            size,
            minimumSize,
            maximumSize,
            isFixedAspectRatioEnabled,
            autoHideContent,
            autoUpdateSize,
            forceShowResizeOverlay,
        )
    )

    override var size: Dimensions
        get() = fakeInternal.size
        set(value) {
            fakeInternal.size = value
        }

    override var minimumSize: Dimensions
        get() = fakeInternal.minimumSize
        set(value) {
            fakeInternal.minimumSize = value
        }

    override var maximumSize: Dimensions
        get() = fakeInternal.maximumSize
        set(value) {
            fakeInternal.maximumSize = value
        }

    override var isFixedAspectRatioEnabled: Boolean
        get() = fakeInternal.isFixedAspectRatioEnabled
        set(value) {
            fakeInternal.isFixedAspectRatioEnabled = value
        }

    @get:Suppress("GetterSetterNames")
    override var autoHideContent: Boolean
        get() = fakeInternal.autoHideContent
        set(value) {
            fakeInternal.autoHideContent = value
        }

    @get:Suppress("GetterSetterNames")
    override var autoUpdateSize: Boolean
        get() = fakeInternal.autoUpdateSize
        set(value) {
            fakeInternal.autoUpdateSize = value
        }

    @get:Suppress("GetterSetterNames")
    override var forceShowResizeOverlay: Boolean
        get() = fakeInternal.forceShowResizeOverlay
        set(value) {
            fakeInternal.forceShowResizeOverlay = value
        }

    /**
     * For test purposes only.
     *
     * Represents the set of listeners that are invoked through the resize operation. In tests, you
     * can use this map to manually trigger the listener and verify that your code responds
     * correctly to resize operation.
     *
     * <p>Map of resize event listeners to their executors.
     */
    public val resizeEventListenersMap: Map<ResizeEventListener, Executor>
        get() = fakeInternal.resizeEventListenersMap

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
        fakeInternal.addResizeEventListener(executor, resizeEventListener)
    }

    /**
     * Removes the given listener from the set of listeners for the resize events.
     *
     * @param resizeEventListener The listener to be removed.
     */
    override fun removeResizeEventListener(resizeEventListener: ResizeEventListener) {
        fakeInternal.removeResizeEventListener(resizeEventListener)
    }

    /**
     * Simulates a resize event from the runtime, notifying all registered listeners.
     *
     * This function is intended for testing purposes to allow manual triggering of the update
     * mechanism. It iterates through all currently registered listeners and invokes their
     * `onResizeEvent` method on their respective [Executor]s.
     *
     * @param event The new [InputEvent] to be sent in the simulated event.
     */
    public fun onResizeEvent(event: ResizeEvent) {
        fakeInternal.onResizeEvent(event)
    }
}
