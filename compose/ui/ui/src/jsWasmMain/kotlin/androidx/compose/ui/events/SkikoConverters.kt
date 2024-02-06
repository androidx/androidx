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

import org.jetbrains.skiko.SkikoInputEvent
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.SkikoMouseButtons
import org.jetbrains.skiko.SkikoPlatformPointerEvent
import org.jetbrains.skiko.SkikoPointer
import org.jetbrains.skiko.SkikoPointerDevice
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind
import org.jetbrains.skiko.currentNanoTime
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.asList

private fun KeyboardEvent.toSkikoKey(): Int {
    var key = this.keyCode
    val side = this.location
    if (side == KeyboardEvent.DOM_KEY_LOCATION_RIGHT) {
        if (
            key == SkikoKey.KEY_LEFT_CONTROL.platformKeyCode ||
            key == SkikoKey.KEY_LEFT_SHIFT.platformKeyCode ||
            key == SkikoKey.KEY_LEFT_META.platformKeyCode
        )
            key = key.or(0x80000000.toInt())
    }
    return key
}

private fun KeyboardEvent.toSkikoModifiers(): SkikoInputModifiers {
    var result = 0
    if (altKey) {
        result = result.or(SkikoInputModifiers.ALT.value)
    }
    if (shiftKey) {
        result = result.or(SkikoInputModifiers.SHIFT.value)
    }
    if (ctrlKey) {
        result = result.or(SkikoInputModifiers.CONTROL.value)
    }
    if (metaKey) {
        result = result.or(SkikoInputModifiers.META.value)
    }
    return SkikoInputModifiers(result)
}

private fun MouseEvent.toSkikoModifiers(): SkikoInputModifiers {
    var result = 0
    if (altKey) {
        result = result.or(SkikoInputModifiers.ALT.value)
    }
    if (shiftKey) {
        result = result.or(SkikoInputModifiers.SHIFT.value)
    }
    if (ctrlKey) {
        result = result.or(SkikoInputModifiers.CONTROL.value)
    }
    if (metaKey) {
        result = result.or(SkikoInputModifiers.META.value)
    }
    return SkikoInputModifiers(result)
}


internal fun KeyboardEvent.toSkikoEvent(
    kind: SkikoKeyboardEventKind
): SkikoKeyboardEvent {
    val skikoKey = toSkikoKey()
    return SkikoKeyboardEvent(
        SkikoKey.valueOf(skikoKey),
        toSkikoModifiers(),
        kind,
        timeStamp.toInt().toLong(),
        this
    )
}

internal val SPECIAL_KEYS = setOf(
    "Unidentified",
    "Alt",
    "AltGraph",
    "Backspace",
    "CapsLock",
    "Control",
    "Fn",
    "FnLock",
    "Hyper",
    "Meta",
    "NumLock",
    "ScrollLock",
    "Shift",
    "Super",
    "Symbol",
    "SymbolLock",
    "F1",
    "F2",
    "F3",
    "F4",
    "F5",
    "F6",
    "F7",
    "F8",
    "F9",
    "F10",
    "F11",
    "F12",
    "F13",
    "F14",
    "F15",
    "F16",
    "F17",
    "F18",
    "F19",
    "F20",
    "F21",
    "F22",
    "ArrowLeft",
    "ArrowUp",
    "ArrowRight",
    "ArrowDown",
    "Help",
    "Home",
    "Delete",
    "End",
    "PageUp",
    "PageDown",
    "Escape",
    "Clear",
    "Clear"
)

internal fun KeyboardEvent.toSkikoTypeEvent(): SkikoInputEvent? {
    return if (SPECIAL_KEYS.contains(key)) {
        null
    } else {
        val input = when (key) {
            "Enter" -> "\n"
            "Tab" -> "\t"
            else -> key
        }
        val key = SkikoKey.valueOf(keyCode)
        val modifiers = toSkikoModifiers()
        SkikoInputEvent(
            input,
            key,
            modifiers,
            SkikoKeyboardEventKind.TYPE,
            this
        )
    }
}

private fun getSkikoButtonValue(button: Int): Int {
    return when (button) {
        0 -> SkikoMouseButtons.LEFT.value
        1 -> SkikoMouseButtons.MIDDLE.value
        2 -> SkikoMouseButtons.RIGHT.value
        3 -> SkikoMouseButtons.BUTTON_4.value
        4 -> SkikoMouseButtons.BUTTON_5.value
        else -> 0
    }
}

private var buttonsFlags = 0
private fun MouseEvent.toSkikoPressedMouseButtons(
    kind: SkikoPointerEventKind
): SkikoMouseButtons {
    // https://www.w3schools.com/jsref/event_button.asp
    val button = button.toInt()
    if (kind == SkikoPointerEventKind.DOWN) {
        buttonsFlags = buttonsFlags.or(getSkikoButtonValue(button))
        return SkikoMouseButtons(buttonsFlags)
    }
    buttonsFlags = buttonsFlags.xor(getSkikoButtonValue(button))
    return SkikoMouseButtons(buttonsFlags)
}

private fun toSkikoMouseButton(event: MouseEvent): SkikoMouseButtons {
    return SkikoMouseButtons(getSkikoButtonValue(event.button.toInt()))
}

internal fun MouseEvent.toSkikoEvent(
    kind: SkikoPointerEventKind
): SkikoPointerEvent {
    return SkikoPointerEvent(
        x = offsetX,
        y = offsetY,
        pressedButtons = toSkikoPressedMouseButtons(kind),
        button = toSkikoMouseButton(this),
        modifiers = this.toSkikoModifiers(),
        kind = kind,
        timestamp = timeStamp.toInt().toLong(),
        platform = this
    )
}

internal fun MouseEvent.toSkikoDragEvent(): SkikoPointerEvent {
    return SkikoPointerEvent(
        x = offsetX,
        y = offsetY,
        pressedButtons = SkikoMouseButtons(buttonsFlags),
        button = toSkikoMouseButton(this),
        modifiers = toSkikoModifiers(),
        kind = SkikoPointerEventKind.DRAG,
        timestamp = timeStamp.toInt().toLong(),
        platform = this
    )
}

internal fun WheelEvent.toSkikoScrollEvent(): SkikoPointerEvent {
    return SkikoPointerEvent(
        x = offsetX,
        y = offsetY,
        deltaX = deltaX,
        deltaY = deltaY,
        pressedButtons = SkikoMouseButtons(buttonsFlags),
        button = SkikoMouseButtons.NONE,
        modifiers = toSkikoModifiers(),
        kind = SkikoPointerEventKind.SCROLL,
        timestamp = timeStamp.toInt().toLong(),
        platform = this
    )
}

private abstract external class ExtendedTouchEvent: TouchEvent {
    val force: Double
}

internal fun TouchEvent.toSkikoEvent(
    kind: SkikoPointerEventKind,
    offsetX: Double,
    offsetY: Double
): SkikoPointerEvent {
    val pointers = changedTouches.asList().map { touch ->
        val x = touch.clientX.toDouble() - offsetX
        val y = touch.clientY.toDouble() - offsetY
        val force = touch.unsafeCast<ExtendedTouchEvent>().force

        SkikoPointer(
            x = x,
            y = y,
            pressed = when (kind) {
                SkikoPointerEventKind.DOWN, SkikoPointerEventKind.MOVE -> true
                else -> false
            },
            device = SkikoPointerDevice.TOUCH,
            id = touch.identifier.toLong(),
            pressure = force
        )
    }

    return SkikoPointerEvent(
        x = pointers.map { it.x }.average(),
        y = pointers.map { it.y }.average(),
        kind = kind,
        timestamp = (currentNanoTime() / 1E6).toLong(),
        pointers = pointers,
        platform = this.unsafeCast<SkikoPlatformPointerEvent>()
    )
}