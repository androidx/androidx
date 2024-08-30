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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import org.w3c.dom.events.KeyboardEvent

internal interface InputAwareInputService {
    fun getOffset(rect: Rect): Offset
    fun processKeyboardEvent(keyboardEvent: KeyboardEvent)
    fun isVirtualKeyboard(): Boolean
}

internal abstract class WebTextInputService : PlatformTextInputService, InputAwareInputService {
    private val webImeInputService = WebImeInputService(this)
    private val webKeyboardInputService = WebKeyboardInputService()

    private fun delegatedService(): PlatformTextInputService {
        return if (isVirtualKeyboard()) webImeInputService else webKeyboardInputService
    }

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        delegatedService().startInput(value, imeOptions, onEditCommand, onImeActionPerformed)
    }

    override fun stopInput() {
        delegatedService().stopInput()
    }

    override fun showSoftwareKeyboard() {
        delegatedService().showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        delegatedService().hideSoftwareKeyboard()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        delegatedService().updateState(oldValue, newValue)
    }

    override fun notifyFocusedRect(rect: Rect) {
        delegatedService().notifyFocusedRect(rect)
    }
}
