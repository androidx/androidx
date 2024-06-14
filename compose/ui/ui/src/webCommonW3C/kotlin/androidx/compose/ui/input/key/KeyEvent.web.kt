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

package androidx.compose.ui.input.key

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import org.w3c.dom.events.KeyboardEvent

private fun KeyboardEvent.toInputModifiers(): PointerKeyboardModifiers {
    return PointerKeyboardModifiers(
        isAltPressed = altKey,
        isShiftPressed = shiftKey,
        isCtrlPressed = ctrlKey,
        isMetaPressed = metaKey
    )
}

internal fun KeyboardEvent.toComposeEvent(): KeyEvent {
    val composeKey = toKey()
    return KeyEvent(
        nativeKeyEvent = InternalKeyEvent(
            key = composeKey,
            type = when (type) {
                "keydown" -> KeyEventType.KeyDown
                "keyup" -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
            codePoint = if (key.firstOrNull()?.toString() == key) key.codePointAt(0) else composeKey.keyCode.toInt(),
            modifiers = toInputModifiers(),
            nativeEvent = this
        )
    )
}


// TODO Remove once it's available in common stdlib https://youtrack.jetbrains.com/issue/KT-23251
internal typealias CodePoint = Int

/**
 * Converts a surrogate pair to a unicode code point.
 */
private fun Char.Companion.toCodePoint(high: Char, low: Char): CodePoint =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

/**
 * Returns the character (Unicode code point) at the specified index.
 */
internal fun String.codePointAt(index: Int): CodePoint {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < this.length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return high.code
}

private val codeMap = mapOf(
    "KeyA" to Key.A,
    "KeyB" to Key.B,
    "KeyC" to Key.C,
    "KeyD" to Key.D,
    "KeyE" to Key.E,
    "KeyF" to Key.F,
    "KeyG" to Key.G,
    "KeyH" to Key.H,
    "KeyI" to Key.I,
    "KeyJ" to Key.J,
    "KeyK" to Key.K,
    "KeyL" to Key.L,
    "KeyM" to Key.M,
    "KeyN" to Key.N,
    "KeyO" to Key.O,
    "KeyP" to Key.P,
    "KeyQ" to Key.Q,
    "KeyR" to Key.R,
    "KeyS" to Key.S,
    "KeyT" to Key.T,
    "KeyU" to Key.U,
    "KeyV" to Key.V,
    "KeyW" to Key.W,
    "KeyX" to Key.X,
    "KeyY" to Key.Y,
    "KeyZ" to Key.Z,

    "Digit0" to Key.Zero,
    "Digit1" to Key.One,
    "Digit2" to Key.Two,
    "Digit3" to Key.Three,
    "Digit4" to Key.Four,
    "Digit5" to Key.Five,
    "Digit6" to Key.Six,
    "Digit7" to Key.Seven,
    "Digit8" to Key.Eight,
    "Digit9" to Key.Nine,

    "Numpad0" to Key.NumPad0,
    "Numpad1" to Key.NumPad1,
    "Numpad2" to Key.NumPad2,
    "Numpad3" to Key.NumPad3,
    "Numpad4" to Key.NumPad4,
    "Numpad5" to Key.NumPad5,
    "Numpad6" to Key.NumPad6,
    "Numpad7" to Key.NumPad7,
    "Numpad8" to Key.NumPad8,
    "Numpad9" to Key.NumPad9,

    "NumpadDivide" to Key.NumPadDivide,
    "NumpadMultiply" to Key.NumPadMultiply,
    "NumpadSubtract" to Key.NumPadSubtract,
    "NumpadAdd" to Key.NumPadAdd,
    "NumpadEnter" to Key.NumPadEnter,
    "NumpadEqual" to Key.NumPadEquals,
    "NumpadDecimal" to Key.NumPadDot,

    "NumLock" to Key.NumLock,

    "Minus" to Key.Minus,
    "Equal" to Key.Equals,
    "Backspace" to Key.Backspace,
    "BracketLeft" to Key.LeftBracket,
    "BracketRight" to Key.RightBracket,
    "Backslash" to Key.Backslash,
    "Semicolon" to Key.Semicolon,
    "Enter" to Key.Enter,
    "Comma" to Key.Comma,
    "Period" to Key.Period,
    "Slash" to Key.Slash,

    "ArrowLeft" to Key.DirectionLeft,
    "ArrowUp" to Key.DirectionUp,
    "ArrowRight" to Key.DirectionRight,
    "ArrowDown" to Key.DirectionDown,

    "Home" to Key.MoveHome,
    "PageUp" to Key.PageUp,
    "PageDown" to Key.PageDown,
    "Delete" to Key.Delete,
    "End" to Key.MoveEnd,

    "Backquote" to Key.Grave,
    "Tab" to Key.Tab,
    "CapsLock" to Key.CapsLock,

    "ShiftLeft" to Key.ShiftLeft,
    "ControlLeft" to Key.CtrlLeft,
    "AltLeft" to Key.AltLeft,
    "MetaLeft" to Key.MetaLeft,

    "ShiftRight" to Key.ShiftRight,
    "ControlRight" to Key.CtrlRight,
    "AltRight" to Key.AltRight,
    "MetaRight" to Key.MetaRight,
    "Insert" to Key.Insert,

    "Escape" to Key.Escape,

    "F1" to Key.F1,
    "F2" to Key.F2,
    "F3" to Key.F3,
    "F4" to Key.F4,
    "F5" to Key.F5,
    "F6" to Key.F6,
    "F7" to Key.F7,
    "F8" to Key.F8,
    "F9" to Key.F9,
    "F10" to Key.F10,
    "F11" to Key.F11,
    "F12" to Key.F12,

    "Space" to Key.Spacebar,
)

private fun KeyboardEvent.toKey(): Key {
    // code is empty string if the actual keyboard event actually is generated by virtual keyboard
    val keyResolved = if (code.isEmpty()) {
        codeMap[key]
    } else {
        codeMap[code]
    }

    return keyResolved ?: Key.Unknown
}
