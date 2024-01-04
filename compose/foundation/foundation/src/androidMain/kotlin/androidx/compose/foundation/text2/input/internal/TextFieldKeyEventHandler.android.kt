/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import android.view.InputDevice.SOURCE_DPAD
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.foundation.text2.input.internal.selection.TextFieldSelectionState
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.SoftwareKeyboardController

internal actual fun createTextFieldKeyEventHandler(): TextFieldKeyEventHandler =
    AndroidTextFieldKeyEventHandler()

internal class AndroidTextFieldKeyEventHandler : TextFieldKeyEventHandler() {

    override fun onPreKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        focusManager: FocusManager,
        keyboardController: SoftwareKeyboardController
    ): Boolean {
        // do not proceed if common code has consumed the event
        if (
            super.onPreKeyEvent(
                event = event,
                textFieldState = textFieldState,
                textFieldSelectionState = textFieldSelectionState,
                focusManager = focusManager,
                keyboardController = keyboardController
            )
        ) return true

        val device = event.nativeKeyEvent.device
        return when {
            device == null -> false

            // Ignore key events from non-dpad sources
            !device.supportsSource(SOURCE_DPAD) -> false

            // Ignore key events from virtual keyboards
            device.isVirtual -> false

            // Ignore key release events
            event.type != KeyDown -> false

            event.isKeyCode(KEYCODE_DPAD_UP) -> focusManager.moveFocus(FocusDirection.Up)
            event.isKeyCode(KEYCODE_DPAD_DOWN) -> focusManager.moveFocus(FocusDirection.Down)
            event.isKeyCode(KEYCODE_DPAD_LEFT) -> focusManager.moveFocus(FocusDirection.Left)
            event.isKeyCode(KEYCODE_DPAD_RIGHT) -> focusManager.moveFocus(FocusDirection.Right)
            event.isKeyCode(KEYCODE_DPAD_CENTER) -> {
                // Enable keyboard on center key press
                keyboardController.show()
                true
            }
            else -> false
        }
    }
}

private fun KeyEvent.isKeyCode(keyCode: Int): Boolean =
    this.key.nativeKeyCode == keyCode
