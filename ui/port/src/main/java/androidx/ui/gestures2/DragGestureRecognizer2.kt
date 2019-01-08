/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures2

import androidx.ui.engine.geometry.Offset

enum class Direction {
    LEFT, UP, RIGHT, DOWN
}

interface DragGestureRecognizerCallback {
    fun onDragStart() {}
    fun onDragStop() {}
    fun onDragCancelled() {}
    fun canDrag(direction: Direction) = false
    fun drag(dx: Float, dy: Float) = Offset(dx, dy)
}

class DragGestureRecognizer2(
    private val slop: Int,
    private val consumePass: PointerEventPass = PointerEventPass.POST_UP,
    private val callback: DragGestureRecognizerCallback
) : GestureRecognizer2 {

    var dxDraggedUnderSlop = 0.0
    var dyDraggedUnderSlop = 0.0
    var passedSlop = false
    var pointerCount = 0

    override fun handleEvent(event: PointerEvent2, pass: PointerEventPass): PointerEvent2 {
        var pointerEvent = event

        if (pass == PointerEventPass.INITIAL_DOWN && pointerEvent.changedToDown(true)) {
            pointerCount++
        }

        if (pass == consumePass && pointerCount >= 1) {
            if ((pointerCount == 1 && event.changedToUp(true)) || event.current.cancelled) {
                if (passedSlop && !event.current.cancelled) {
                    callback.onDragStop()
                }
                dxDraggedUnderSlop = 0.0
                dyDraggedUnderSlop = 0.0
                passedSlop = false
            } else if (event.current.down) {
                val dx = event.positionChange().dx / pointerCount
                val dy = event.positionChange().dy / pointerCount
                val directionX = if (dx < 0) Direction.LEFT else Direction.RIGHT
                val directionY = if (dy < 0) Direction.UP else Direction.DOWN
                val canDragX = callback.canDrag(directionX)
                val canDragY = callback.canDrag(directionY)

                if (!passedSlop) {
                    dxDraggedUnderSlop += dx
                    dyDraggedUnderSlop += dy

                    val passedSlopX = canDragX && Math.abs(dxDraggedUnderSlop) > slop
                    val passedSlopY = canDragY && Math.abs(dyDraggedUnderSlop) > slop

                    if (passedSlopX || passedSlopY) {
                        passedSlop = true
                        callback.onDragStart()
                        pointerEvent = dragAndConsume(event, dx, dy)
                    } else {
                        if (!canDragX &&
                            ((directionX == Direction.LEFT && dxDraggedUnderSlop < 0) ||
                                    (directionX == Direction.RIGHT && dxDraggedUnderSlop > 0))
                        ) {
                            dxDraggedUnderSlop = 0.0
                        }
                        if (!canDragY &&
                            ((directionY == Direction.LEFT && dyDraggedUnderSlop < 0) ||
                                    (directionY == Direction.DOWN && dyDraggedUnderSlop > 0))
                        ) {
                            dyDraggedUnderSlop = 0.0
                        }
                    }
                } else {
                    pointerEvent = dragAndConsume(event, dx, dy)
                }
            }
        }

        if (pass == PointerEventPass.POST_DOWN && pointerEvent.changedToUp(true)) {
            pointerCount--
        }

        return pointerEvent
    }

    private fun dragAndConsume(
        pointerEvent2: PointerEvent2,
        dx: Float,
        dy: Float
    ): PointerEvent2 {
        val (consumedDx, consumedDY) = callback.drag(dx, dy)
        return pointerEvent2.consumePositionChange(consumedDx, consumedDY)
    }

    private fun clampToZero(clampToZero: Boolean, value: Float) =
        if (clampToZero) 0.0 else value

    private fun clampToAbsLimit(limit: Float, value: Float): Float {
        val absLimit = Math.abs(limit)
        val absValue = Math.abs(value)

        if (absLimit >= absValue) {
            return value
        } else {
            return if (value < 0) absLimit * -1 else absLimit
        }
    }
}