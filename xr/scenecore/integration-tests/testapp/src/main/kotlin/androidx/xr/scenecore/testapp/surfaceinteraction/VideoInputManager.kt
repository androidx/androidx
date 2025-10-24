/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testapp.surfaceinteraction

import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.InputEvent

class VideoInputManager() {
    interface InputHandler {
        fun onClick(
            pointerType: InputEvent.Pointer,
            origin: Vector3,
            direction: Vector3,
            count: Int,
        ) {}

        fun onDragStart(
            pointerType: InputEvent.Pointer,
            downOrigin: Vector3,
            downDirection: Vector3,
        ) {}

        fun onDragMove(pointerType: InputEvent.Pointer, origin: Vector3, direction: Vector3) {}

        fun onDragEnd(pointerType: InputEvent.Pointer, origin: Vector3, direction: Vector3) {}

        fun onDragCanceled(pointerType: InputEvent.Pointer) {}

        fun canDrag(): Boolean {
            return false
        }
    }

    private class PointerState(
        var downTime: Long = 0,
        var downOrigin: Vector3 = Vector3(),
        var downDirection: Vector3 = Vector3(),
        var upTime: Long = 0,
        var clickCount: Int = 0,
        var isDragging: Boolean = false,
    )

    var handler: InputHandler? = null
    var singleClickEnabled = true
    var doubleClickEnabled = true
    var dragEnabled = true

    val clickInterval = 200L
    val dragStartLen = 0.008f

    private val pointersStateMap = mutableMapOf<InputEvent.Pointer, PointerState>()

    val canDrag
        get() = handler?.canDrag() ?: false

    fun update(inputEvent: InputEvent) {
        if (handler == null) return
        val hdl = handler!!

        val state = pointersStateMap.getOrPut(inputEvent.pointerType) { PointerState() }
        when (inputEvent.action) {
            InputEvent.Action.DOWN -> {
                state.downTime = inputEvent.timestamp
                state.downOrigin = inputEvent.origin
                state.downDirection = inputEvent.direction
                if (inputEvent.timestamp - state.upTime > clickInterval) {
                    state.clickCount = 0
                }
            }
            InputEvent.Action.MOVE -> {
                if (!state.isDragging) {
                    if (canDrag && dragEnabled) {
                        // detect if need to start drag
                        val currDistPos = inputEvent.origin + inputEvent.direction
                        val downDistPos = state.downOrigin + state.downDirection
                        val dragStartLenSqr = dragStartLen * dragStartLen
                        if (
                            (currDistPos - downDistPos).lengthSquared > dragStartLenSqr ||
                                (inputEvent.origin - state.downOrigin).lengthSquared >
                                    dragStartLenSqr
                        ) {
                            state.isDragging = true
                            hdl.onDragStart(
                                inputEvent.pointerType,
                                state.downOrigin,
                                state.downDirection,
                            )
                            hdl.onDragMove(
                                inputEvent.pointerType,
                                inputEvent.origin,
                                inputEvent.direction,
                            )
                        }
                    }
                } else {
                    hdl.onDragMove(inputEvent.pointerType, inputEvent.origin, inputEvent.direction)
                }
            }
            InputEvent.Action.UP -> {
                state.upTime = inputEvent.timestamp
                if (!state.isDragging) {
                    if (inputEvent.timestamp - state.downTime < clickInterval) {
                        state.clickCount++
                        if (
                            when (state.clickCount) {
                                1 -> singleClickEnabled
                                2 -> doubleClickEnabled
                                else -> true
                            }
                        ) {
                            hdl.onClick(
                                inputEvent.pointerType,
                                inputEvent.origin,
                                inputEvent.direction,
                                state.clickCount,
                            )
                        }
                    }
                } else {
                    hdl.onDragEnd(inputEvent.pointerType, inputEvent.origin, inputEvent.direction)
                    state.isDragging = false
                }
            }
            InputEvent.Action.CANCEL -> {
                if (state.isDragging) {
                    hdl.onDragCanceled(inputEvent.pointerType)
                    state.isDragging = false
                }
                pointersStateMap.remove(inputEvent.pointerType)
            }
        }
    }
}
