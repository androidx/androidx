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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.w3c.dom.HTMLElement

internal interface InputAwareInputService {
    fun processKeyboardEvent(keyboardEvent: KeyEvent): Boolean

    /**
     * @param rect is the rect in Compose coordinates
     *
     * @return a DpRect appropriate for positioning and laying out the HTML backing input
     */
    fun getNewGeometryForBackingInput(rect: Rect): DpRect
}

internal abstract class WebTextInputService : PlatformTextInputService, InputAwareInputService {

    private var backingDomInput: BackingDomInput? = null
        set(value) {
            field?.dispose()
            field = value
        }

    /**
     * This container will host the actual hidden HTML input element.
     */
    abstract val backingDomInputContainer: HTMLElement

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        backingDomInput =
            BackingDomInput(
                imeOptions = imeOptions,
                composeCommunicator = object : ComposeCommandCommunicator {
                    override fun sendKeyboardEvent(keyboardEvent: KeyEvent): Boolean {
                        return this@WebTextInputService.processKeyboardEvent(keyboardEvent)
                    }

                    override fun sendEditCommand(commands: List<EditCommand>) {
                        onEditCommand(commands)
                    }
                },
                inputContainer = backingDomInputContainer,
            )
        backingDomInput?.register()

        showSoftwareKeyboard()
    }

    fun getBackingInput(): HTMLElement? {
        return backingDomInput?.backingElement?.takeIf { it.isConnected }
    }

    override fun stopInput() {
        backingDomInput?.dispose()
    }

    override fun showSoftwareKeyboard() {
        backingDomInput?.focus()
    }

    override fun hideSoftwareKeyboard() {
        backingDomInput?.blur()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        backingDomInput?.updateState(newValue)
    }

    override fun notifyFocusedRect(rect: Rect) {
        val newRect = getNewGeometryForBackingInput(rect)
        backingDomInput?.updateHtmlInputBox(newRect.left.value, newRect.top.value, newRect.width.value, newRect.height.value)
    }
}