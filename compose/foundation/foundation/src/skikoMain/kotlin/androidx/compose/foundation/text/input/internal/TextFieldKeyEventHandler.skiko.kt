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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.KeyModifiers
import androidx.compose.foundation.text.commonKeyMapping
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * Factory function to create a platform specific [TextFieldKeyEventHandler].
 */
// TODO https://youtrack.jetbrains.com/issue/COMPOSE-741/Implement-createTextFieldKeyEventHandler
internal fun createSkikoTextFieldKeyEventHandler() = object : TextFieldKeyEventHandler() {}

internal fun createIOSTextFieldKeyEventHandler() = object : TextFieldKeyEventHandler() {
    override fun onKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        clipboardKeyCommandsHandler: ClipboardKeyCommandsHandler,
        keyboardController: SoftwareKeyboardController,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Boolean
    ): Boolean {
        return when (commonKeyMapping(KeyModifiers.ShiftMeta).map(event)) {
            // iOS has its own Key Input handler for these keys:
            KeyCommand.LEFT_CHAR,
            KeyCommand.RIGHT_CHAR,
            KeyCommand.UP,
            KeyCommand.DOWN,
            KeyCommand.SELECT_LEFT_CHAR,
            KeyCommand.SELECT_RIGHT_CHAR,
            KeyCommand.SELECT_UP,
            KeyCommand.SELECT_DOWN -> false
            else -> {
                super.onKeyEvent(
                    event,
                    textFieldState,
                    textLayoutState,
                    textFieldSelectionState,
                    clipboardKeyCommandsHandler,
                    keyboardController,
                    editable,
                    singleLine,
                    onSubmit
                )
            }
        }
    }
}

// TODO https://youtrack.jetbrains.com/issue/CMP-10296/Implement-isFromHardwareSource
internal actual val KeyEvent.isFromHardwareSource: Boolean
    get() = true

// TODO https://youtrack.jetbrains.com/issue/COMPOSE-1361/Implement-isFromSoftKeyboard
internal actual val KeyEvent.isFromSoftKeyboard: Boolean
    get() = false
