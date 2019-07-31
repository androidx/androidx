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

import androidx.compose.Model

/**
 * Interface that defines behaviour of value that is dragged via [Draggable]
 */
interface DragValueController {
    /**
     * Current Float value of controlled value
     */
    val currentValue: Float

    /**
     * Perform drag to [target] on controlled value
     *
     * @param target the resulted position for controlled value
     */
    fun onDrag(target: Float)

    /**
     * Perform finishing activities when drag has ended with given velocity
     *
     * This is a good place to start fling or consume velocity in some other way
     * if you need to do so.
     *
     * Callback passed to this function *must* be called after controlled value reached it's final
     * destination, e.g. it might be an immediate drag end or the end of the fling.
     *
     * @param velocity the velocity value when drag has ended
     * @param onValueSettled callback to call after fling has ended and controlled value settled
     */
    fun onDragEnd(velocity: Float, onValueSettled: (Float) -> Unit)

    /**
     * Set bounds for controlled value
     *
     * @param min lower bound for dragging
     * @param max upper bound for dragging
     */
    fun setBounds(min: Float, max: Float)
}

@Model
private class FloatValueHolder(var inner: Float)

/**
 * Simple [DragValueController] that backs up single [Float] value with no fling support
 *
 * @param initialValue the initial value to set for controlled Float value
 */
class FloatDragValueController(initialValue: Float) : DragValueController {
    override val currentValue: Float
        get() = value.inner

    private val value = FloatValueHolder(initialValue)
    private var minBound = Float.MIN_VALUE
    private var maxBound = Float.MAX_VALUE

    override fun onDrag(target: Float) {
        this.value.inner = target.coerceIn(minBound, maxBound)
    }

    override fun onDragEnd(velocity: Float, onValueSettled: (Float) -> Unit) {
        onValueSettled(currentValue)
    }

    override fun setBounds(min: Float, max: Float) {
        val changed = minBound != min || maxBound != max
        minBound = min
        maxBound = max
        if (changed) value.inner = value.inner.coerceIn(minBound, maxBound)
    }
}