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

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import org.w3c.dom.clipboard.ClipboardEvent

internal interface CopyPasteTestSpec : TextFieldTestSpec {
    @Test
    fun pasteEvent() = runApplicationTest {
        val textFieldValue =  createApplicationWithHolder(initialText = "A ")
        textFieldValue.awaitAndAssertTextEquals("A ")

        sendToHtmlInput(
            clipboardEvent(type = "paste").also {
                it.clipboardData!!.setData("text/plain", "QWERTY")
            }
        )

        textFieldValue.awaitAndAssertTextEquals("A QWERTY")
    }

    @Test
    fun copyEvent() = runApplicationTest {
        createApplicationWithHolder("HELLO", TextRange(1, 5))
        awaitIdle()

        val copyEvent = clipboardEvent(type = "copy")
        sendToHtmlInput(copyEvent)
        awaitIdle()

        assertEquals("ELLO", copyEvent.clipboardData!!.getData("text/plain"))
    }

    @Test
    fun cutEvent() = runApplicationTest {
        val textFieldValue =  createApplicationWithHolder("HELLO", TextRange(1, 4))
        awaitIdle()

        val cutEvent = clipboardEvent(type = "cut")
        sendToHtmlInput(cutEvent)
        awaitIdle()

        assertEquals("ELL", cutEvent.clipboardData!!.getData("text/plain"))
        assertEquals("HO", textFieldValue.text)
    }
}

// The default API doesn't work correctly on FF :(, so we do it manually
private fun clipboardEvent(type: String): ClipboardEvent = js(""" 
        new ClipboardEvent(type, { 'clipboardData': new DataTransfer() })
    """)