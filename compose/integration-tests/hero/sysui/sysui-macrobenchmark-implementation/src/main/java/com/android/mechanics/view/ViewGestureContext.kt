/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.view

import android.content.Context
import android.view.ViewConfiguration
import androidx.compose.ui.util.fastForEach
import com.android.mechanics.spec.InputDirection
import kotlin.math.max
import kotlin.math.min

fun interface GestureContextUpdateListener {
    fun onGestureContextUpdated()
}

interface ViewGestureContext {
    val direction: InputDirection
    val dragOffset: Float

    fun addUpdateCallback(listener: GestureContextUpdateListener)

    fun removeUpdateCallback(listener: GestureContextUpdateListener)
}

/**
 * [ViewGestureContext] driven by a gesture distance.
 *
 * The direction is determined from the gesture input, where going further than
 * [directionChangeSlop] in the opposite direction toggles the direction.
 *
 * @param initialDragOffset The initial [dragOffset] of the [ViewGestureContext]
 * @param initialDirection The initial [direction] of the [ViewGestureContext]
 * @param directionChangeSlop the amount [dragOffset] must be moved in the opposite direction for
 *   the [direction] to flip.
 */
class DistanceGestureContext(
    initialDragOffset: Float,
    initialDirection: InputDirection,
    private val directionChangeSlop: Float,
) : ViewGestureContext {
    init {
        require(directionChangeSlop > 0) {
            "directionChangeSlop must be greater than 0, was $directionChangeSlop"
        }
    }

    companion object {
        @JvmStatic
        fun create(
            context: Context,
            initialDragOffset: Float = 0f,
            initialDirection: InputDirection = InputDirection.Max,
        ): DistanceGestureContext {
            val directionChangeSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
            return DistanceGestureContext(initialDragOffset, initialDirection, directionChangeSlop)
        }
    }

    private val callbacks = mutableListOf<GestureContextUpdateListener>()

    override var dragOffset: Float = initialDragOffset
        set(value) {
            if (field == value) return

            field = value
            direction =
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
            invokeCallbacks()
        }

    override var direction = initialDirection
        private set

    private var furthestDragOffset = initialDragOffset

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

        invokeCallbacks()
    }

    override fun addUpdateCallback(listener: GestureContextUpdateListener) {
        callbacks.add(listener)
    }

    override fun removeUpdateCallback(listener: GestureContextUpdateListener) {
        callbacks.remove(listener)
    }

    private fun invokeCallbacks() {
        callbacks.fastForEach { it.onGestureContextUpdated() }
    }
}
