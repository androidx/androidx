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

/** Component to enable resize semantics. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ResizableComponent : Component {
    /**
     * Sets the size of the entity.
     *
     * <p>The size of the entity is the size of the bounding box that contains the content of the
     * entity. The size of the content inside that bounding box is fully controlled by the
     * application.
     */
    public var size: Dimensions

    /**
     * Sets the minimum size constraint for the entity.
     *
     * <p>The minimum size constraint is used to set constraints on how small the user can resize
     * the bounding box of the entity up to. The size of the content inside that bounding box is
     * fully controlled by the application.
     */
    public var minimumSize: Dimensions

    /**
     * Sets the maximum size constraint for the entity.
     *
     * <p>The maximum size constraint is used to set constraints on how large the user can resize
     * the bounding box of the entity up to. The size of the content inside that bounding box is
     * fully controlled by the application.
     */
    public var maximumSize: Dimensions

    /**
     * Sets the aspect ratio of the entity during resizing.
     *
     * <p>The aspect ratio is determined by taking the panel's width over its height. A value of
     * 0.0f (or negative) means there are no preferences.
     *
     * <p>This method does not immediately resize the entity. The new aspect ratio will be applied
     * the next time the user resizes the entity through the reform UI. During this resize
     * operation, the entity's current area will be preserved.
     *
     * <p>If a different resizing behavior is desired, such as fixing the width and adjusting the
     * height, the client can manually resize the entity to the preferred dimensions before calling
     * this method. No automatic resizing will occur when using the reform UI then.
     */
    public var fixedAspectRatio: Float

    /**
     * Sets whether or not content (including content of all child nodes) is auto-hidden during
     * resizing. Defaults to true.
     */
    @get:Suppress("GetterSetterNames") public var autoHideContent: Boolean

    /**
     * Sets whether the size of the ResizableComponent is automatically updated to match during an
     * ongoing resize (to match the proposed size as resize events are received). Defaults to true.
     */
    @get:Suppress("GetterSetterNames") public var autoUpdateSize: Boolean

    /**
     * Sets whether to force showing the resize overlay even when this entity is not being resized.
     * Defaults to false.
     */
    @get:Suppress("GetterSetterNames") public var forceShowResizeOverlay: Boolean

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
    // TODO: b/361638845 - Mirror the Kotlin API for ResizeListener.
    @Suppress("ExecutorRegistration")
    public fun addResizeEventListener(executor: Executor, resizeEventListener: ResizeEventListener)

    /**
     * Removes the given listener from the set of listeners for the resize events.
     *
     * @param resizeEventListener The listener to be removed.
     */
    public fun removeResizeEventListener(resizeEventListener: ResizeEventListener)
}
