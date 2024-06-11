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

package androidx.compose.ui.events

import androidx.compose.ui.input.key.Key
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import org.w3c.dom.events.MouseEvent

internal external interface KeyboardEventInitExtended : KeyboardEventInit {
    var keyCode: Int?
}

private fun KeyboardEventInit.keyDownEvent() = KeyboardEvent("keydown", this)
private fun KeyboardEventInit.withKeyCode(keyCode: Int) = (this as KeyboardEventInitExtended).apply {
    this.keyCode = keyCode
}

internal fun keyDownEvent(
    key: String,
    code: String = "Key${key.uppercase()}",
    keyCode: Int = key.uppercase().first().code,
    ctrlKey: Boolean = false,
    metaKey: Boolean = false,
    altKey: Boolean = false,
    shiftKey: Boolean = false,
    cancelable: Boolean = true
): KeyboardEvent =
    KeyboardEventInit(
        key = key,
        code = code,
        ctrlKey = ctrlKey,
        metaKey = metaKey,
        altKey = altKey,
        shiftKey = shiftKey,
        cancelable = cancelable
    )
        .withKeyCode(keyCode)
        .keyDownEvent()

internal fun keyDownEventUnprevented(): KeyboardEvent =
    KeyboardEventInit(ctrlKey = true, cancelable = true, key = "Control")
        .withKeyCode(Key.CtrlLeft.keyCode.toInt())
        .keyDownEvent()

private fun DummyTouchEventInit(): TouchEventInit = js("({ changedTouches: [new Touch({identifier: 0, target: document})] })")

internal fun createTouchEvent(type: String): TouchEvent {
    return TouchEvent(type, DummyTouchEventInit())
}

internal fun createMouseEvent(type: String): MouseEvent {
    return MouseEvent(type)
}

