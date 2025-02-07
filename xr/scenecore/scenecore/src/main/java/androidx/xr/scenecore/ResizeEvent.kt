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

import androidx.annotation.IntDef

/**
 * A high-level resize event which is sent in response to the User interacting with the Entity.
 *
 * @param resizeState The state of the resize event.
 * @param newSize The new proposed size of the Entity in meters.
 */
internal data class ResizeEvent(
    @ResizeState public val resizeState: Int,
    public val newSize: Dimensions,
) {
    public companion object {
        /** Constant for {@link resizeState}: The resize state is unknown. */
        public const val RESIZE_STATE_UNKNOWN: Int = 0
        /** Constant for {@link resizeState}: The user has started dragging the resize handles. */
        public const val RESIZE_STATE_START: Int = 1
        /** Constant for {@link resizeState}: The user is continuing to drag the resize handles. */
        public const val RESIZE_STATE_ONGOING: Int = 2
        /** Constant for {@link resizeState}: The user has stopped dragging the resize handles. */
        public const val RESIZE_STATE_END: Int = 3
    }

    @IntDef(
        value = [RESIZE_STATE_UNKNOWN, RESIZE_STATE_START, RESIZE_STATE_ONGOING, RESIZE_STATE_END]
    )
    public annotation class ResizeState
}

/**
 * Listener for resize actions. Callbacks are invoked as the user interacts with the resize
 * affordance.
 */
public interface ResizeListener {
    /**
     * Called when the user starts resizing the entity.
     *
     * @param entity The entity being resized.
     * @param originalSize The original size of the entity in meters at the start of the resize
     *   operation.
     */
    public fun onResizeStart(entity: Entity, originalSize: Dimensions) {}

    /**
     * Called continuously while the user is resizing the entity.
     *
     * @param entity The entity being resized.
     * @param newSize The new proposed size of the entity in meters.
     */
    public fun onResizeUpdate(entity: Entity, newSize: Dimensions) {}

    /**
     * Called when the user has finished resizing the entity, for example when the user concludes
     * the resize gesture.
     *
     * @param entity The entity being resized.
     * @param finalSize The final proposed size of the entity in meters.
     */
    public fun onResizeEnd(entity: Entity, finalSize: Dimensions) {}
}
