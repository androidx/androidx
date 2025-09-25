/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.input.specs

import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.TestInputState
import androidx.compose.ui.WebApplicationScope
import androidx.compose.ui.events.EventsSequence
import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event

internal interface TextFieldTestSpec : OnCanvasTests {
    fun currentHtmlInput() = getShadowRoot().querySelector("textarea") as HTMLTextAreaElement

    suspend fun createTestInputState(
        initialText: String = "",
        initialSelection: TextRange = TextRange(initialText.length)
    ): TestInputState


    suspend fun WebApplicationScope.createApplicationWithHolder(
        initialText: String = "",
        initialSelection: TextRange = TextRange(initialText.length)
    ): TestInputState {
        val focusRequester = FocusRequester()
        val textFieldStateHolder = createTestInputState(initialText, initialSelection)

        createComposeWindow {
            textFieldStateHolder.createBasicTextField(focusRequester)
        }

        focusRequester.requestFocus()
        waitForHtmlInput()

        return textFieldStateHolder
    }

    fun sendToHtmlInput(vararg events: Event) {
        dispatchEvents(currentHtmlInput(), *events)
    }

    fun EventsSequence.sendToHtmlInput() = sendToHtmlInput(*toList().toTypedArray())

    suspend fun WebApplicationScope.waitForHtmlInput(): HTMLTextAreaElement {
        while (true) {
            val element = getShadowRoot().querySelector("textarea")
            if (element is HTMLTextAreaElement) {
                return element
            }
            yield()
        }
        awaitIdle()
    }

    // sends keydown / input sequence of events like in Chrome in normal mode
    private fun standardKeyboardSequence(vararg keys: String): Array<Event> {
        return keys.flatMap {  key ->
            buildList {
                add(keyEvent(key))
                // we treat anything of size > 0 as a character that does not have type representation
                if (key.length == 1) {
                    add(beforeInput(inputType = "insertText", data = key))
                }
                add(keyEvent(key, type = "keyup"))
            }
        }.toTypedArray()
    }

    private fun standardKeyboardSequence(str: String): Array<Event> = standardKeyboardSequence(*str.toCharArray().map { it.toString() }.toTypedArray())
    fun sendStandardKeyboardSequence(str: String) = sendToHtmlInput(*standardKeyboardSequence(str))

}
