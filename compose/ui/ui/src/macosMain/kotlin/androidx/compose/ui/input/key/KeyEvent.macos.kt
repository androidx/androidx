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
import platform.AppKit.NSEvent
import platform.AppKit.NSEventModifierFlagCommand
import platform.AppKit.NSEventModifierFlagControl
import platform.AppKit.NSEventModifierFlagOption
import platform.AppKit.NSEventModifierFlagShift
import platform.AppKit.NSKeyDown
import platform.AppKit.NSKeyUp

internal fun NSEvent.toComposeEvent(): KeyEvent {
    return KeyEvent(
        nativeKeyEvent = InternalKeyEvent(
            key = Key(keyCode.toLong()),
            type = when (type) {
                NSKeyDown -> KeyEventType.KeyDown
                NSKeyUp -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
            codePoint = characters?.firstOrNull()?.code ?: 0, // TODO: Support multichar CodePoint
            modifiers = toInputModifiers(),
            nativeEvent = this
        )
    )
}

private fun NSEvent.toInputModifiers() = PointerKeyboardModifiers(
    isAltPressed = modifierFlags and NSEventModifierFlagOption != 0UL,
    isShiftPressed = modifierFlags and NSEventModifierFlagShift != 0UL,
    isCtrlPressed = modifierFlags and NSEventModifierFlagControl != 0UL,
    isMetaPressed = modifierFlags and NSEventModifierFlagCommand != 0UL
)
