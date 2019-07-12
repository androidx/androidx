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
import androidx.ui.core.Px
import androidx.ui.core.PxPosition

/**
 * Draggable Direction specifies the direction in which you can drag an [AnimatedDraggable].
 * It can be either [Horizontal] or [Vertical].
 */
sealed class DragDirection {

    // TODO: remove internals for children when b/137357249 is ready
    internal abstract val xProjection: (Px) -> Float
    internal abstract val yProjection: (Px) -> Float
    internal abstract val isDraggableInDirection: (
        direction: Direction,
        minValue: Float,
        currentValue: Float,
        maxValue: Float
    ) -> Boolean

    internal open fun project(pos: PxPosition) = xProjection(pos.x) + yProjection(pos.y)

    /**
     * Horizontal direction of dragging in [AnimatedDraggable].
     */
    object Horizontal : DragDirection() {
        internal override val xProjection: (Px) -> Float = { it.value }
        internal override val yProjection: (Px) -> Float = { 0f }
        internal override val isDraggableInDirection: (
            direction: Direction,
            minValue: Float,
            currentValue: Float,
            maxValue: Float
        ) -> Boolean =
            { direction, minValue, currentValue, maxValue ->
                when (direction) {
                    Direction.RIGHT -> currentValue <= maxValue
                    Direction.LEFT -> currentValue >= minValue
                    else -> false
                }
            }
    }

    /**
     * Vertical direction of dragging in [AnimatedDraggable].
     */
    object Vertical : DragDirection() {
        internal override val xProjection: (Px) -> Float = { 0f }
        internal override val yProjection: (Px) -> Float = { it.value }
        internal override val isDraggableInDirection: (
            direction: Direction,
            minValue: Float,
            currentValue: Float,
            maxValue: Float
        ) -> Boolean =
            { direction, minValue, currentValue, maxValue ->
                when (direction) {
                    Direction.UP -> currentValue <= maxValue
                    Direction.DOWN -> currentValue >= minValue
                    else -> false
                }
            }
    }
}