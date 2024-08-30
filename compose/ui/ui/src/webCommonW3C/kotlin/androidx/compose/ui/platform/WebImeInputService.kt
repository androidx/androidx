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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue

internal class WebImeInputService(parentInputService: InputAwareInputService) : PlatformTextInputService, InputAwareInputService by parentInputService {

    private var backingTextArea: BackingTextArea? = null
        set(value) {
            field?.dispose()
            field = value
        }

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        backingTextArea =
            BackingTextArea(
                imeOptions = imeOptions,
                onEditCommand = onEditCommand,
                onImeActionPerformed = onImeActionPerformed,
                processKeyboardEvent = this@WebImeInputService::processKeyboardEvent
            )
        backingTextArea?.register()

        showSoftwareKeyboard()
    }

    override fun stopInput() {
        backingTextArea?.dispose()
    }

    override fun showSoftwareKeyboard() {
        backingTextArea?.focus()
    }

    override fun hideSoftwareKeyboard() {
        backingTextArea?.blur()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        backingTextArea?.updateState(newValue)
    }

    override fun notifyFocusedRect(rect: Rect) {
        super.notifyFocusedRect(rect)
        backingTextArea?.updateHtmlInputPosition(getOffset(rect))
    }

}