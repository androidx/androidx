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

import android.view.InputDevice
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.SoftwareKeyboardController

internal actual fun createTextFieldKeyEventHandler(): TextFieldKeyEventHandler =
    AndroidTextFieldKeyEventHandler()

internal actual val KeyEvent.isFromSoftKeyboard: Boolean
    get() =
        (nativeKeyEvent.flags and android.view.KeyEvent.FLAG_SOFT_KEYBOARD) ==
            android.view.KeyEvent.FLAG_SOFT_KEYBOARD

internal class AndroidTextFieldKeyEventHandler : TextFieldKeyEventHandler() {

    // After fixing the Dpad navigation, we no longer need to intercept prekey events for the
    // Android platform. Therefore no `override fun onPreKeyEvent`

    override fun onKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        clipboardKeyCommandsHandler: ClipboardKeyCommandsHandler,
        keyboardController: SoftwareKeyboardController,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Boolean,
    ): Boolean {
        // Before handing off the key processing to the super class, we check whether the event is
        // coming from a hardware keyboard (virtual or not) to decide touch mode.
        if (
            event.type == KeyDown &&
                event.nativeKeyEvent.isFromSource(InputDevice.SOURCE_KEYBOARD) &&
                (!event.isFromSoftKeyboard || !event.isTypedEvent)
        ) {
            textFieldSelectionState.isInTouchMode = false
        }

        return super.onKeyEvent(
            event,
            textFieldState,
            textLayoutState,
            textFieldSelectionState,
            clipboardKeyCommandsHandler,
            keyboardController,
            editable,
            singleLine,
            onSubmit,
        )
    }
}

private fun KeyEvent.isKeyCode(keyCode: Int): Boolean = this.key.nativeKeyCode == keyCode
