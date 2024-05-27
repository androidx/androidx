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

private fun Key(keyCode: Long, location: Int) = Key(
    keyCode = if (location == KeyboardEvent.DOM_KEY_LOCATION_RIGHT) {
        when (keyCode) {
            Key.CtrlLeft.keyCode,
            Key.ShiftLeft.keyCode,
            Key.MetaLeft.keyCode -> keyCode or 0x80000000
            else -> keyCode
        }
    } else {
        keyCode
    }
)

private fun KeyboardEvent.toInputModifiers(): PointerKeyboardModifiers {
    return PointerKeyboardModifiers(
        isAltPressed = altKey,
        isShiftPressed = shiftKey,
        isCtrlPressed = ctrlKey,
        isMetaPressed = metaKey
    )
}

internal fun KeyboardEvent.toComposeEvent(): KeyEvent {
    val composeKey = Key(keyCode.toLong(), location)
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
