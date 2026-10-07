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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/** Component to enable resize semantics. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * Whether the aspect ratio is maintained during resizing.
     *
     * If true the affordance will maintain its current aspect ratio being resized and all suggested
     * sizes will maintain the current aspect ratio. This defaults to false.
     */
    public var isFixedAspectRatioEnabled: Boolean

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
     * Valid integer constants for bounding 3D geometry resizing and scaling interaction modes.
     *
     * These settings dictate which user physical inputs are allowed to trigger scale adjustments on
     * the target object.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class GeometryGestureType {
        public companion object {
            /** Disable all scaling gestures. */
            public const val NONE: Int = 0

            /** Allow scaling via single-handed pinch or drag gestures. */
            public const val ONE_HANDED: Int = 1

            /** Allow scaling only via two-handed pinch-and-stretch gestures. */
            public const val TWO_HANDED: Int = 2

            /** Enable all standard scaling gestures. */
            public const val ALL: Int = 3
        }
    }

    /**
     * Sets the gesture interaction type for scaling geometries.
     *
     * NOTE: This property is only applicable when the component is attached to 3D spatial
     * geometries, such as [GltfEntity] or [MeshEntity]. Setting this value has no effect on 2D
     * panels ([PanelEntity] or [SurfaceEntity]), where resizing options are handled automatically
     * by the platform's default window borders.
     */
    public var geometryGestureType: Int

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
