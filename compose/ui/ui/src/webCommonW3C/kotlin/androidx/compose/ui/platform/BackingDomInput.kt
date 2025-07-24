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

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

internal interface ComposeCommandCommunicator {
    fun sendEditCommand(commands: List<EditCommand>)
    fun sendEditCommand(command: EditCommand) = sendEditCommand(listOf(command))

    fun sendKeyboardEvent(keyboardEvent: KeyEvent)
}

private fun setBackingInputBox(container: HTMLElement, left: Float, top: Float, width: Float, height: Float) { js("""
    container.style.setProperty("--compose-internal-web-backing-input-left", left);
    container.style.setProperty("--compose-internal-web-backing-input-top", top);
    container.style.setProperty("--compose-internal-web-backing-input-width", width);
    container.style.setProperty("--compose-internal-web-backing-input-height", height)
""") }

/**
 * The purpose of this entity is to isolate synchronization between a TextFieldValue
 * and the DOM HTMLTextAreaElement we are actually listening events on in order to show
 * the virtual keyboard.
 */
internal class BackingDomInput(
    val inputContainer: HTMLElement,
    imeOptions: ImeOptions,
    composeCommunicator : ComposeCommandCommunicator,
) {
    private val inputStrategy = DomInputStrategy(
        imeOptions,
        composeCommunicator
    )

    internal val backingElement = inputStrategy.htmlInput

    fun register() {
        setBackingInputBox(container = inputContainer, 0f, 0f, 0f, 0f)
        inputContainer.appendChild(backingElement)
    }

    fun focus() {
        // we focus twice to be sure that ios and non-ios browser both manage to focus
        // see https://youtrack.jetbrains.com/issue/CMP-8013
        // and https://youtrack.jetbrains.com/issue/CMP-7836/
        backingElement.focus()
        window.requestAnimationFrame {
            if (document.activeElement != backingElement) {
                backingElement.focus()
            }
        }
    }

    fun blur() {
        backingElement.blur()
    }

    fun updateHtmlInputBox(left: Float, top: Float, width: Float, height: Float) {
        // we coerce the width to at least 1px, to mitigate the typing lags in mobile Safari:
        // https://youtrack.jetbrains.com/issue/CMP-8373/Web-Mobile.-TextField.-Typing-input-delay
        // Apparently when the width is 0px and the selection index is large enough (big text length),
        // the lags become more noticeable.
        // I assume it has something to do with text layout. Read more in the comments of the linked issue.
        setBackingInputBox(container = inputContainer, left, top, width.coerceAtLeast(1f), height)
    }

    fun updateState(textFieldValue: TextFieldValue) {
        inputStrategy.updateState(textFieldValue)
        focus()
    }

    fun dispose() {
        backingElement.remove()
    }
}