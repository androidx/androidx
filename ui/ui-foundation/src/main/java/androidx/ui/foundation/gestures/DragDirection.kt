/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.gestures

import androidx.ui.core.Direction
import androidx.ui.geometry.Offset

/**
 * Draggable Direction specifies the direction in which you can drag an [draggable] or [scrollable].
 * It can be either [Horizontal] or [Vertical].
 */
sealed class DragDirection {

    // TODO: remove internals for children when b/137357249 is ready
    internal abstract val xProjection: (Float) -> Float
    internal abstract val yProjection: (Float) -> Float
    internal abstract val isDraggableInDirection: (
        direction: Direction,
        currentValue: Float
    ) -> Boolean

    internal open fun project(pos: Offset) = xProjection(pos.x) + yProjection(pos.y)

    /**
     * Horizontal direction of dragging in [draggable] or [scrollable].
     */
    object Horizontal : DragDirection() {
        internal override val xProjection: (Float) -> Float = { it }
        internal override val yProjection: (Float) -> Float = { 0f }
        internal override val isDraggableInDirection:
                    (direction: Direction, currentValue: Float) -> Boolean =
            { direction, _ ->
                when (direction) {
                    Direction.RIGHT -> true
                    Direction.LEFT -> true
                    else -> false
                }
            }
    }

    /**
     * Horizontal direction of dragging in [draggable] or [scrollable], but reversed.
     *
     * Reversed means that if you drag in the positive direction (to the right), you're going to
     * receive a negative delta in [draggable] or [scrollable].
     *
     * Typically used when [androidx.ui.core.LayoutDirection.Rtl] is on to support RTL in
     * draggable components.
     */
    // TODO(malkov) This should be named RtlAwareHorizontal and automatically handle reversal when
    // b/150774151 is fixed
    object ReversedHorizontal : DragDirection() {
        internal override val xProjection: (Float) -> Float = { -it }
        internal override val yProjection: (Float) -> Float = { 0f }
        internal override val isDraggableInDirection:
                    (direction: Direction, currentValue: Float) -> Boolean = { direction, _ ->
            when (direction) {
                Direction.RIGHT -> true
                Direction.LEFT -> true
                else -> false
            }
        }
    }

    /**
     * Vertical direction of dragging in [draggable] or [scrollable].
     */
    object Vertical : DragDirection() {
        internal override val xProjection: (Float) -> Float = { 0f }
        internal override val yProjection: (Float) -> Float = { it }
        internal override val isDraggableInDirection: (
            direction: Direction,
            currentValue: Float
        ) -> Boolean =
            { direction, _ ->
                when (direction) {
                    Direction.UP -> true
                    Direction.DOWN -> true
                    else -> false
                }
            }
    }
}