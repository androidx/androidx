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

class TapGestureRecognizer2(
    val consumePass: PointerEventPass
) : GestureRecognizer2 {

    var onTapDown: (() -> Unit)? = null
    var onTapUp: (() -> Unit)? = null
    var onTap: (() -> Unit)? = null

    // Pointers being tracked independent of consumed
    private var pointerCount = 0
    // True if we've consumed a down for the first pointer that touched us
    private var state = State.IDLE

    private enum class State {
        IDLE, ACTIVE, CANCELLED
    }

    override fun handleEvent(event: PointerEvent2, pass: PointerEventPass): PointerEvent2 {
        var pointerEvent = event

        if (pass == PointerEventPass.INITIAL_DOWN && pointerEvent.changedToDown(true)) {
            pointerCount++
        }

        if (event.anyPositionChangeConsumed()) {
            state = State.CANCELLED
        }

        if (pass == consumePass && state != State.CANCELLED && pointerCount == 1) {
            if (pointerEvent.changedToDown()) {
                pointerEvent = pointerEvent.consumeDownChange()
                state = State.ACTIVE
                onTapDown?.invoke()
            } else if (pointerEvent.changedToUp()) {
                pointerEvent = pointerEvent.consumeDownChange()
                onTapUp?.invoke()
                onTap?.invoke()
            }
        }

        if (pass == PointerEventPass.POST_DOWN && pointerEvent.changedToUp(true)) {
            pointerCount--
            if (pointerCount == 0) {
                state = State.IDLE
            }
        }

        return pointerEvent
    }
}