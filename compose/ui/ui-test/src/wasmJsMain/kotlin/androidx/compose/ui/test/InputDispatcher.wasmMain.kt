/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent

/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
internal actual fun keyEvent(
    key: Key, keyEventType: KeyEventType, modifiers: Int
): KeyEvent {
    val nativeCode = key.keyCode.toInt()

    return KeyEvent(
        NativeKeyEvent(
            key = key,
            kind = keyEventType,
            value =  if (key.isPrintable()) nativeCode.toChar().toString() else "Unknown",
            modifiers = modifiers,
            timestamp = 0
        )
    )
}

// Basically this is what we've tried to avoid in isTypedEvent implementation, but in tests it's totally fine
// Just add any non-printable key you need for tests here
private val NonPrintableKeys = setOf(
    Key.AltLeft,
    Key.MetaLeft,
    Key.ShiftLeft,
    Key.CtrlLeft,
    Key.Delete,
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.DirectionDown,
    Key.DirectionUp,
    Key.Home,
    Key.MoveEnd
)

private fun Key.isPrintable(): Boolean {
    return !NonPrintableKeys.contains(this)
}

internal actual fun Int.updatedKeyboardModifiers(key: Key, down: Boolean): Int = this