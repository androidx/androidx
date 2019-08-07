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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.PxPosition
import androidx.ui.foundation.animation.AnimatedFloatDragController
import androidx.ui.foundation.animation.AnchorsFlingConfig
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.px

/**
 * Component that provides high-level drag functionality reflected in one value
 *
 * The common usecase for this component is when you need to be able to drag/scroll something
 * on the screen and represent it as one value via [DragValueController].
 *
 * If you need to control the whole dragging flow, consider using [TouchSlopDragGestureDetector] instead.
 *
 * @sample androidx.ui.foundation.samples.DraggableSample
 *
 * By using [AnimatedFloatDragController] with [AnchorsFlingConfig] you can achieve behaviour
 * when value is gravitating to predefined set of points after drag has ended.
 *
 * @sample androidx.ui.foundation.samples.AnchoredDraggableSample
 *
 * @param dragDirection direction in which drag should be happening.
 * Either [DragDirection.Vertical] or [DragDirection.Horizontal]
 * @param minValue lower bound for draggable value in this component
 * @param maxValue upper bound for draggable value in this component
 * @param valueController controller to control value and how it will consume drag events,
 * such as drag, fling or change of dragging bounds. The default is [FloatDragValueController],
 * which provides simple move-as-much-as-user-drags login with no fling support.
 * @param callback callback to react to drag events
 */
@Composable
fun Draggable(
    dragDirection: DragDirection,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    valueController: DragValueController = +memo(minValue) { FloatDragValueController(minValue) },
    callback: DraggableCallback? = null,
    children: @Composable() (Float) -> Unit
) {
    fun current() = valueController.currentValue
    +memo(valueController, minValue, maxValue) {
        valueController.setBounds(minValue, maxValue)
    }
    TouchSlopDragGestureDetector(
        dragObserver = object : DragObserver {

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                callback?.notifyDrag()
                val projected = dragDirection.project(dragDistance)
                val newValue = (current() + projected).coerceIn(minValue, maxValue)
                val consumed = newValue - current()
                valueController.onDrag(newValue)
                val fractionConsumed = if (projected == 0f) 0f else consumed / projected
                return PxPosition(
                    dragDirection.xProjection(dragDistance.x).px * fractionConsumed,
                    dragDirection.yProjection(dragDistance.y).px * fractionConsumed
                )
            }

            override fun onStop(velocity: PxPosition) {
                val projected = dragDirection.project(velocity)
                valueController.onDragEnd(projected) {
                    callback?.notifyFinished(it)
                }
            }
        },
        canDrag = { direction ->
            dragDirection.isDraggableInDirection(direction, minValue, current(), maxValue)
        }
    ) {
        children(current())
    }
}

class DraggableCallback(
    private val onDragStarted: () -> Unit = {},
    private val onDragSettled: (Float) -> Unit = {}
) {
    private var startNotified: Boolean = false
    internal fun notifyDrag() {
        if (!startNotified) {
            startNotified = true
            onDragStarted()
        }
    }

    internal fun notifyFinished(final: Float) {
        startNotified = false
        onDragSettled(final)
    }
}