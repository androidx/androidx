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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.internal.LegacyPlatformTextInputServiceAdapter.LegacyPlatformTextInputNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import java.awt.Component
import java.awt.event.InputMethodEvent
import java.text.AttributedString
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LegacyPlatformTextInputServiceAdapterTest {

    @Test
    fun replaceInputMethodText_basic() = runTest {
        val processor = EditProcessor()
        val adapter = DesktopLegacyPlatformTextInputServiceAdapter()
        val inputService = TextInputService(adapter)
        var currentRequest: PlatformTextInputMethodRequest? = null

        adapter.registerModifier(object : LegacyPlatformTextInputNode {
            override val softwareKeyboardController: SoftwareKeyboardController?
                get() = null
            override val layoutCoordinates: LayoutCoordinates?
                get() = null

            override fun launchTextInputSession(
                block: suspend PlatformTextInputSession.() -> Nothing
            ): Job {
                val session = object : PlatformTextInputSession {
                    override suspend fun startInputMethod(
                        request: PlatformTextInputMethodRequest
                    ): Nothing {
                        currentRequest = request
                        try {
                            awaitCancellation()
                        } finally {
                            currentRequest = null
                        }
                    }
                }

                return backgroundScope.launch(Dispatchers.Unconfined) {
                    block(session)
                }
            }
        })

        val session = inputService.startInput(
            TextFieldValue(),
            ImeOptions.Default,
            processor::apply,
            {}
        )

        processor.reset(TextFieldValue("h"), session)

        val familyEmoji = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66"

        val request = assertIs<LegacyTextInputMethodRequest>(currentRequest)
        request.replaceInputMethodText(
            InputMethodEvent(
                object : Component() {},
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                AttributedString(familyEmoji).iterator,
                11,
                null,
                null
            )
        )

        val buffer = processor.toTextFieldValue()

        assertEquals("${familyEmoji}h", buffer.text)
        assertEquals(TextRange(11), buffer.selection)
    }

    @Test
    fun longPressWorkaroundTest() = runTest {
        assumeTrue(isMac)
        val processor = EditProcessor()

        val adapter = DesktopLegacyPlatformTextInputServiceAdapter()
        val inputService = TextInputService(adapter)
        var currentRequest: PlatformTextInputMethodRequest? = null

        adapter.registerModifier(object : LegacyPlatformTextInputNode {
            override val softwareKeyboardController: SoftwareKeyboardController?
                get() = null
            override val layoutCoordinates: LayoutCoordinates?
                get() = null

            override fun launchTextInputSession(
                block: suspend PlatformTextInputSession.() -> Nothing
            ): Job {
                val session = object : PlatformTextInputSession {
                    override suspend fun startInputMethod(
                        request: PlatformTextInputMethodRequest
                    ): Nothing {
                        currentRequest = request
                        try {
                            awaitCancellation()
                        } finally {
                            currentRequest = null
                        }
                    }
                }

                return backgroundScope.launch(Dispatchers.Unconfined) {
                    block(session)
                }
            }
        })

        val session = inputService.startInput(
            TextFieldValue(),
            ImeOptions.Default,
            processor::apply,
            {}
        )

        val request = assertIs<LegacyTextInputMethodRequest>(currentRequest)
        request.charKeyPressed = true
        processor.reset(TextFieldValue("a", selection = TextRange(1)), session)
        request.getSelectedText(null)
        request.charKeyPressed = false

        request.replaceInputMethodText(
            InputMethodEvent(
                object : Component() {},
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                AttributedString("ä").iterator,
                1,
                null,
                null
            )
        )

        val buffer = processor.toTextFieldValue()

        assertEquals("ä", buffer.text)
        assertEquals(TextRange(1), buffer.selection)
    }
}
