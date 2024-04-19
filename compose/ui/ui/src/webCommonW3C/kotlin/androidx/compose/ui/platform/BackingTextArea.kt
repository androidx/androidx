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
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteAllCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent

/**
* The purpose of this entity is to isolate synchronization between a TextFieldValue
* and the DOM HTMLTextAreaElement we are actually listening events on in order to show
* the virtual keyboard.
*/
internal class BackingTextArea(
    private val imeOptions: ImeOptions,
    private val onEditCommand: (List<EditCommand>) -> Unit,
    private val onImeActionPerformed: (ImeAction) -> Unit
) {
    private val textArea: HTMLTextAreaElement = createHtmlInput()

    private fun createHtmlInput(): HTMLTextAreaElement {
        val htmlInput = document.createElement("textarea") as HTMLTextAreaElement

        htmlInput.setAttribute("autocorrect", "off")
        htmlInput.setAttribute("autocomplete", "off")
        htmlInput.setAttribute("autocapitalize", "off")
        htmlInput.setAttribute("spellcheck", "false")

        val inputMode = when (imeOptions.keyboardType) {
            KeyboardType.Text -> "text"
            KeyboardType.Ascii -> "text"
            KeyboardType.Number -> "number"
            KeyboardType.Phone -> "tel"
            KeyboardType.Uri -> "url"
            KeyboardType.Email -> "email"
            KeyboardType.Password -> "password"
            KeyboardType.NumberPassword -> "number"
            KeyboardType.Decimal -> "decimal"
            else -> "text"
        }

        val enterKeyHint = when (imeOptions.imeAction) {
            ImeAction.Default -> "enter"
            ImeAction.None -> "enter"
            ImeAction.Done -> "done"
            ImeAction.Go -> "go"
            ImeAction.Next -> "next"
            ImeAction.Previous -> "previous"
            ImeAction.Search -> "search"
            ImeAction.Send -> "send"
            else -> "enter"
        }

        htmlInput.setAttribute("inputmode", inputMode)
        htmlInput.setAttribute("enterkeyhint", enterKeyHint)

        htmlInput.style.apply {
            setProperty("position", "absolute")
            setProperty("user-select", "none")
            setProperty("forced-color-adjust", "none")
            setProperty("white-space", "pre-wrap")
            setProperty("align-content", "center")
            setProperty("top", "0")
            setProperty("left", "0")
            setProperty("padding", "0")
            setProperty("opacity", "0")
            setProperty("color", "transparent")
            setProperty("background", "transparent")
            setProperty("caret-color", "transparent")
            setProperty("outline", "none")
            setProperty("border", "none")
            setProperty("resize", "none")
            setProperty("text-shadow", "none")
        }

        htmlInput.addEventListener("input", {
            val text = htmlInput.value
            val cursorPosition = htmlInput.selectionEnd
            sendImeValueToCompose(onEditCommand, text, cursorPosition)
        })

        htmlInput.addEventListener("contextmenu", { evt ->
            evt.preventDefault()
            evt.stopPropagation()
        })

        // this done by analogy with KeyCommand.NEW_LINE processing in TextFieldKeyInput
        if (imeOptions.singleLine) {
            htmlInput.addEventListener("keydown", { evt ->
                evt.preventDefault()
                evt as KeyboardEvent
                if (evt.key == "Enter" && evt.type == "keydown") {
                    onImeActionPerformed(imeOptions.imeAction)
                }
            })
        }

        return htmlInput
    }

    fun register() {
        document.body?.appendChild(textArea)
    }

    private fun sendImeValueToCompose(
        onEditCommand: (List<EditCommand>) -> Unit,
        text: String,
        newCursorPosition: Int? = null
    ) {
        val value = if (text == "\n") {
            ""
        } else {
            text
        }

        if (newCursorPosition != null) {
            onEditCommand(
                listOf(
                    DeleteAllCommand(),
                    CommitTextCommand(value, newCursorPosition),
                )
            )
        } else {
            onEditCommand(
                listOf(
                    CommitTextCommand(value, 1)
                )
            )
        }
    }

    fun focus() {
        textArea.focus()
    }

    fun blur() {
        textArea.blur()
    }

    fun updateHtmlInputPosition(offset: Offset) {
        textArea.style.left = "${offset.x}px"
        textArea.style.top = "${offset.y}px"

        focus()
    }

    fun updateState(textFieldValue: TextFieldValue) {
        textArea.value = textFieldValue.text
        textArea.setSelectionRange(textFieldValue.selection.start, textFieldValue.selection.end)
    }

    fun dispose() {
        textArea.remove()
    }
}