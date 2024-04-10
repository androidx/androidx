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
import platform.UIKit.UIKeyModifierAlternate
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIKeyModifierControl
import platform.UIKit.UIKeyModifierShift
import platform.UIKit.UIPress
import platform.UIKit.UIPressPhase.UIPressPhaseBegan
import platform.UIKit.UIPressPhase.UIPressPhaseEnded

internal fun UIPress.toComposeEvent(): KeyEvent {
    // TODO: https://developer.apple.com/documentation/uikit/uipress/3526315-key
    //  can be potentially nil on TVOS, this will cause a crash
    val uiKey = requireNotNull(key) {
        "UIPress with null key is not supported"
    }
    return KeyEvent(
        nativeKeyEvent = InternalKeyEvent(
            key = Key(uiKey.keyCode),
            type = when (phase) {
                UIPressPhaseBegan -> KeyEventType.KeyDown
                UIPressPhaseEnded -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
            codePoint = uiKey.characters.firstOrNull()?.code ?: 0,
            modifiers = PointerKeyboardModifiers(
                isCtrlPressed = uiKey.modifierFlags and UIKeyModifierControl != 0L,
                isMetaPressed = uiKey.modifierFlags and UIKeyModifierCommand != 0L,
                isAltPressed = uiKey.modifierFlags and UIKeyModifierAlternate != 0L,
                isShiftPressed = uiKey.modifierFlags and UIKeyModifierShift != 0L,
            ),
            nativeEvent = this
        )
    )
}
