/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.mechanics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalViewConfiguration
import com.android.mechanics.spec.InputDirection
import kotlin.math.max
import kotlin.math.min

/**
 * Remembers [DistanceGestureContext] with the given initial distance / direction.
 *
 * Providing update [initDistance] or [initialDirection] will not re-create the
 * [DistanceGestureContext].
 *
 * The `directionChangeSlop` is derived from `ViewConfiguration.touchSlop` and kept current without
 * re-creating, should it ever change.
 */
@Composable
fun rememberDistanceGestureContext(
    initDistance: Float = 0f,
    initialDirection: InputDirection = InputDirection.Max,
): DistanceGestureContext {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    return remember { DistanceGestureContext(initDistance, initialDirection, touchSlop) }
        .also { it.directionChangeSlop = touchSlop }
}

/**
 * Gesture-specific context to augment [MotionValue.currentInput].
 *
 * This context helps to capture the user's intent, and should be provided to [MotionValue]s that
 * respond to a user gesture.
 */
@Stable
interface GestureContext {

    /**
     * The intrinsic direction of the [MotionValue.currentInput].
     *
     * This property determines which of the [DirectionalMotionSpec] from the [MotionSpec] is used,
     * and also prevents flip-flopping of the output value on tiny input-changes around a
     * breakpoint.
     *
     * If the [MotionValue.currentInput] is driven - directly or indirectly - by a user gesture,
     * this property should only change direction after the gesture travelled a significant distance
     * in the opposite direction.
     *
     * @see DistanceGestureContext for a default implementation.
     */
    val direction: InputDirection

    /**
     * The gesture distance of the current gesture, in pixels.
     *
     * Used solely for the [GestureDragDelta] [Guarantee]. Can be hard-coded to a static value if
     * this type of [Guarantee] is not used.
     */
    val dragOffset: Float
}

/**
 * [GestureContext] with a mutable [dragOffset].
 *
 * The implementation class defines whether the [direction] is updated accordingly.
 */
interface MutableDragOffsetGestureContext : GestureContext {
    /** The gesture distance of the current gesture, in pixels. */
    override var dragOffset: Float
}

/** [GestureContext] implementation for manually set values. */
class ProvidedGestureContext(dragOffset: Float, direction: InputDirection) :
    MutableDragOffsetGestureContext {
    override var direction by mutableStateOf(direction)
    override var dragOffset by mutableFloatStateOf(dragOffset)
}

/**
 * [GestureContext] driven by a gesture distance.
 *
 * The direction is determined from the gesture input, where going further than
 * [directionChangeSlop] in the opposite direction toggles the direction.
 *
 * @param initialDragOffset The initial [dragOffset] of the [GestureContext]
 * @param initialDirection The initial [direction] of the [GestureContext]
 * @param directionChangeSlop the amount [dragOffset] must be moved in the opposite direction for
 *   the [direction] to flip.
 */
class DistanceGestureContext(
    initialDragOffset: Float,
    initialDirection: InputDirection,
    directionChangeSlop: Float,
) : MutableDragOffsetGestureContext {
    init {
        require(directionChangeSlop > 0) {
            "directionChangeSlop must be greater than 0, was $directionChangeSlop"
        }
    }

    override var direction by mutableStateOf(initialDirection)
        private set

    private var furthestDragOffset by mutableFloatStateOf(initialDragOffset)

    private var _dragOffset by mutableFloatStateOf(initialDragOffset)

    override var dragOffset: Float
        get() = _dragOffset
        /**
         * Updates the [dragOffset].
         *
         * This flips the [direction], if the [value] is further than [directionChangeSlop] away
         * from the furthest recorded value regarding to the current [direction].
         */
        set(value) {
            _dragOffset = value
            this.direction =
                when (direction) {
                    InputDirection.Max -> {
                        if (furthestDragOffset - value > directionChangeSlop) {
                            furthestDragOffset = value
                            InputDirection.Min
                        } else {
                            furthestDragOffset = max(value, furthestDragOffset)
                            InputDirection.Max
                        }
                    }

                    InputDirection.Min -> {
                        if (value - furthestDragOffset > directionChangeSlop) {
                            furthestDragOffset = value
                            InputDirection.Max
                        } else {
                            furthestDragOffset = min(value, furthestDragOffset)
                            InputDirection.Min
                        }
                    }
                }
        }

    private var _directionChangeSlop by mutableFloatStateOf(directionChangeSlop)

    var directionChangeSlop: Float
        get() = _directionChangeSlop

        /**
         * This flips the [direction], if the current [direction] is further than the new
         * directionChangeSlop [value] away from the furthest recorded value regarding to the
         * current [direction].
         */
        set(value) {
            require(value > 0) { "directionChangeSlop must be greater than 0, was $value" }

            _directionChangeSlop = value

            when (direction) {
                InputDirection.Max -> {
                    if (furthestDragOffset - dragOffset > directionChangeSlop) {
                        furthestDragOffset = dragOffset
                        direction = InputDirection.Min
                    }
                }
                InputDirection.Min -> {
                    if (dragOffset - furthestDragOffset > directionChangeSlop) {
                        furthestDragOffset = value
                        direction = InputDirection.Max
                    }
                }
            }
        }

    /**
     * Sets [dragOffset] and [direction] to the specified values.
     *
     * This also resets memoized [furthestDragOffset], which is used to determine the direction
     * change.
     */
    fun reset(dragOffset: Float, direction: InputDirection) {
        this.dragOffset = dragOffset
        this.direction = direction
        this.furthestDragOffset = dragOffset
    }
}
