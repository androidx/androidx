/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.interaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.waitForContextMenu
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIPasteboard

@OptIn(ExperimentalForeignApi::class)
class TextFieldHotkeyTest {

    @Test
    fun copySelectedText() = runTestsWithTextField(
        initialText = "Hello World",
        initialSelection = TextRange(0, 5),
        actions = {
            UIPasteboard.generalPasteboard().string = null
            keystroke('c', modifierFlags = UIKeyModifierCommand)
        },
        validate = {
            assertEquals("Hello", UIPasteboard.generalPasteboard().string)
        }
    )

    @Test
    fun cutSelectedText() = runTestsWithTextField(
        initialText = "Hello World",
        initialSelection = TextRange(0, 5),
        actions = {
            UIPasteboard.generalPasteboard().string = null
            keystroke('x', modifierFlags = UIKeyModifierCommand)
        },
        validate = { value ->
            assertEquals("Hello", UIPasteboard.generalPasteboard().string)
            assertEquals(" World", value.text)
        }
    )

    @Test
    fun selectAllText() = runTestsWithTextField(
        initialText = "Hello World",
        actions = {
            keystroke('a', modifierFlags = UIKeyModifierCommand)
        },
        validate = { value ->
            assertEquals(TextRange(0, "Hello World".length), value.selection)
        }
    )

    @Test
    fun pasteClipboardAtCursor() = runTestsWithTextField(
        initialText = "Hello ",
        initialSelection = TextRange(6),
        actions = {
            keystroke('v', modifierFlags = UIKeyModifierCommand)
        },
        validate = { value ->
            assertEquals("Hello Kotlin", value.text)
        }
    )

    @Test
    fun pasteClipboardReplacesSelection() = runTestsWithTextField(
        initialText = "Hello World",
        initialSelection = TextRange(6, 11),
        actions = {
            keystroke('v', modifierFlags = UIKeyModifierCommand)
        },
        validate = { value ->
            assertEquals("Hello Kotlin", value.text)
        }
    )

    @Test
    fun showEditMenuThenPasteWithHotkey() = runTestsWithTextField(
        initialText = "Hello-LongLongLongLongLongLong-text",
        actions = {
            // Select a word and bring up the edit menu via double-tap.
            findNodeWithTag("TextField").tap()
            delay(500)
            findNodeWithTag("TextField").doubleTap()
            waitForContextMenu()

            // Replace the selected word with clipboard content via the keyboard shortcut,
            // without touching any menu button.
            keystroke('v', modifierFlags = UIKeyModifierCommand)
        },
        validate = { value ->
            // Double-tap selects "LongLongLongLongLongLong" (the long middle word).
            assertEquals("Hello-Kotlin-text", value.text)
        }
    )

    private fun runTestsWithTextField(
        initialText: String,
        initialSelection: TextRange = TextRange.Zero,
        actions: UIKitInstrumentedTest.() -> Unit,
        validate: UIKitInstrumentedTest.(value: TextFieldValue) -> Unit,
    ) {
        println("BasicTextField:")
        runUIKitInstrumentedTest {
            val requester = FocusRequester()
            val valueState = mutableStateOf(TextFieldValue(initialText, initialSelection))
            UIPasteboard.generalPasteboard().string = "Kotlin"

            setContent {
                LaunchedEffect(Unit) { requester.requestFocus() }
                Column(modifier = Modifier.safeDrawingPadding().padding(30.dp)) {
                    BasicTextField(
                        value = valueState.value,
                        onValueChange = { valueState.value = it },
                        modifier = Modifier
                            .focusRequester(requester)
                            .testTag("TextField"),
                    )
                }
            }
            actions()

            validate(valueState.value)
        }

        println("BasicTextField 2:")
        runUIKitInstrumentedTest {
            val requester = FocusRequester()
            val state = TextFieldState(initialText, initialSelection)
            UIPasteboard.generalPasteboard().string = "Kotlin"

            setContent {
                LaunchedEffect(Unit) { requester.requestFocus() }
                Column(modifier = Modifier.safeDrawingPadding().padding(30.dp)) {
                    BasicTextField(
                        state = state,
                        modifier = Modifier
                            .focusRequester(requester)
                            .testTag("TextField"),
                    )
                }
            }
            actions()
            waitForIdle()
            validate(TextFieldValue(state.text.toString(), state.selection))
        }
    }
}
