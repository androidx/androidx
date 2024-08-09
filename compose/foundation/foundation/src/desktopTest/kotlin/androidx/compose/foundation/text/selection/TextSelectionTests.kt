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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.KeyMapping
import androidx.compose.foundation.text.createPlatformDefaultKeyMapping
import androidx.compose.foundation.text.overriddenDefaultKeyMapping
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test


class TextSelectionTests {

    @get:Rule
    val rule = createComposeRule()

    @After
    fun restoreRealDesktopPlatform() {
        overriddenDefaultKeyMapping = null
    }

    private fun setPlatformDefaultKeyMapping(value: KeyMapping) {
        overriddenDefaultKeyMapping = value
    }

    private fun SemanticsNodeInteraction.waitAndCheck(check: () -> Unit): SemanticsNodeInteraction {
        rule.waitForIdle()
        check()
        return this
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.textFieldSemanticInteraction(
        initialValue: String = "",
        semanticNodeContext: SemanticsNodeInteraction.(state: MutableState<TextFieldValue>) -> SemanticsNodeInteraction
    ) {
        setPlatformDefaultKeyMapping(createPlatformDefaultKeyMapping(this@textFieldSemanticInteraction))
        val state = mutableStateOf(TextFieldValue(initialValue))

        rule.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        val textField = rule.onNodeWithTag("textField")
        textField.performMouseInput {
            click(Offset(0f, 0f))
        }
        rule.waitForIdle()
        textField.assertIsFocused()

        Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        semanticNodeContext.invoke(textField, state)
    }


    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectLineStart(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
                }
                .performKeyInput(keyboardInteraction)
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 7))
                }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectTextStart(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }.waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            performKeyInput(keyboardInteraction)
                .waitAndCheck { Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 0)) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectTextEnd(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
                }
                .performKeyInput(keyboardInteraction)
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 34))
                }
        }
    }
    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectLineEnd(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }.waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
                .performKeyInput(keyboardInteraction)
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 13))
                }
        }
    }

    @Test
    fun `Select till line start with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.selectLineStart {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
        }
    }

    @Test
    fun `Select till text start with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.selectTextStart {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
    }

    @Test
    fun `Select till line end with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.selectLineEnd {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
    }

    @Test
    fun `Select till text end with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.selectTextEnd {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
    }


    @Test
    fun `Select till line start with DesktopPlatform-MacOs`() {
        DesktopPlatform.MacOS.selectLineStart {
            keyDown(Key.ShiftLeft)
            keyDown(Key.MetaLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.ShiftLeft)
            keyUp(Key.MetaLeft)
        }
    }

    @Test
    fun `Select till text start with DesktopPlatform-MacOs`() {
        DesktopPlatform.MacOS.selectTextStart {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Home)
            keyUp(Key.ShiftLeft)
        }
    }

    @Test
    fun `Select till line end with DesktopPlatform-Macos`() {
        DesktopPlatform.MacOS.selectLineEnd {
            keyDown(Key.ShiftLeft)
            keyDown(Key.MetaLeft)
            pressKey(Key.DirectionRight)
            keyUp(Key.ShiftLeft)
            keyUp(Key.MetaLeft)
        }
    }

    @Test
    fun `Select till text end with DesktopPlatform-Macos`() {
        DesktopPlatform.MacOS.selectTextEnd {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.deleteAllFromKeyBoard(
        initialText: String, deleteAllInteraction: KeyInjectionScope.() -> Unit
    ) {
        textFieldSemanticInteraction(initialText) { state ->
            performKeyInput(deleteAllInteraction).waitAndCheck { Truth.assertThat(state.value.text).isEqualTo("") }
        }
    }


    @Test
    fun `Delete backwards on an empty line with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.deleteAllFromKeyBoard("") {
            keyDown(Key.CtrlLeft)
            keyDown(Key.Backspace)
        }
    }

    @Test
    fun `Delete backwards on an empty line with DesktopPlatform-Macos`() {
        DesktopPlatform.MacOS.deleteAllFromKeyBoard("") {
            keyDown(Key.MetaLeft)
            keyDown(Key.Delete)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectAllTest(selectAllInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("Select this text") { state ->
            performKeyInput(selectAllInteraction)
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 16))
                }
                .performKeyInput { keyDown(Key.Delete) }
                .waitAndCheck {
                    Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))
                    Truth.assertThat(state.value.text).isEqualTo("")
                }
        }
    }

    @Test
    fun `Select all with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.selectAllTest {
            keyDown(Key.CtrlLeft)
            pressKey(Key.A)
            keyUp(Key.CtrlLeft)
        }
    }

    @Test
    fun `Select all with DesktopPlatform-Macos`() {
        DesktopPlatform.MacOS.selectAllTest {
            keyDown(Key.MetaLeft)
            pressKey(Key.A)
            keyUp(Key.MetaLeft)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rightClickOnMacOsInSelectionContainerSelectsWord() {
        assumeTrue(DesktopPlatform.Current == DesktopPlatform.MacOS)

        val text = "word1 word2"
        var selection by mutableStateOf<Selection?>(null)
        lateinit var textLayout: TextLayoutResult
        rule.setContent {
            SelectionContainer(
                modifier = Modifier.fillMaxSize(),
                selection = selection,
                onSelectionChange = { selection = it }
            ) {
                BasicText(
                    text = text,
                    onTextLayout = { textLayout = it },
                    modifier = Modifier
                        .padding(top = 100.dp, start = 100.dp)  // Test with padding
                        .testTag("selectable")
                )
            }
        }


        rule.onNodeWithTag("selectable").apply {
            // Right-click first word
            performMouseInput {
                val firstCharBounds = textLayout.getBoundingBox(0)
                rightClick(firstCharBounds.center)
            }

            assertEquals(expected = "word1", actual = selection.selectedText(text))

            // Left-click to close the context menu
            performMouseInput {
                click(Offset.Zero)
            }

            // Right click second word
            performMouseInput {
                val firstCharInSecondWordBounds = textLayout.getBoundingBox(6)
                rightClick(firstCharInSecondWordBounds.center)
            }
            assertEquals(expected = "word2", actual = selection.selectedText(text))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rightClickOnMacOsInTextFieldSelectsWord() {
        assumeTrue(DesktopPlatform.Current == DesktopPlatform.MacOS)

        val text = "word1 word2"
        var textFieldValue by mutableStateOf(TextFieldValue(text))
        lateinit var textLayout: TextLayoutResult
        rule.setContent {
            CoreTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                onTextLayout = { textLayout = it },
                decorationBox = { innerTextField ->  // Test with decoration box
                    Box(
                        modifier = Modifier.padding(100.dp)
                    ) {
                        Box(Modifier.testTag("textfield")) {
                            innerTextField()
                        }
                    }
                }
            )
        }

        rule.onNodeWithTag("textfield", useUnmergedTree = true).apply {
            // Right-click first word
            performMouseInput {
                val firstCharBounds = textLayout.getBoundingBox(0)
                rightClick(firstCharBounds.center)
            }
            assertEquals(expected = "word1", actual = textFieldValue.selectedText(text))

            // Left-click to close the context menu
            performMouseInput {
                click(Offset.Zero)
            }

            // Right-click second word
            performMouseInput {
                val firstCharInSecondWordBounds = textLayout.getBoundingBox(6)
                rightClick(firstCharInSecondWordBounds.center)
            }
            assertEquals(expected = "word2", actual = textFieldValue.selectedText(text))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rightClickOnMacOsWithoutTextDoesNotCrash() {
        assumeTrue(DesktopPlatform.Current == DesktopPlatform.MacOS)

        var selection by mutableStateOf<Selection?>(null)
        rule.setContent {
            SelectionContainer(
                selection = selection,
                onSelectionChange = { selection = it }
            ) {
                Box(
                    modifier = Modifier
                        .testTag("selectable")
                        .fillMaxSize()
                )
            }
        }

        rule.onNodeWithTag("selectable").performMouseInput {
            rightClick(center)
        }
        assertNull(selection)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rightClickOnMacOsAtEmptySpaceDoesNotCrash() {
        assumeTrue(DesktopPlatform.Current == DesktopPlatform.MacOS)

        var selection by mutableStateOf<Selection?>(null)
        val text = "Hello, Compose"
        rule.setContent {
            SelectionContainer(
                selection = selection,
                onSelectionChange = { selection = it }
            ) {
                Box(
                    modifier = Modifier
                        .testTag("selectable")
                ) {
                    BasicText(
                        text = text,
                        modifier = Modifier
                            .padding(all = 100.dp)
                    )
                }
            }
        }

        // All corners and edges
        val positions = (-1..1).flatMap { x ->
            (-1 .. 1).map { y -> Pair(x, y) }
        }.toMutableSet().minus(Pair(0, 0))

        rule.onNodeWithTag("selectable").apply {
            for ((x, y) in positions) {
                performMouseInput {
                    click(Offset.Zero)  // Close the context menu
                    rightClick(
                        Offset(
                            x = width/2f + x * (width/2f - 10f),
                            y = height/2f + y * (height/2f - 10f)
                        )
                    )
                }
                assertTrue(selection.selectedText(text).isNullOrEmpty())
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rightClickOnMacOsInSelectionDoesNotCancelExistingSelection() {
        assumeTrue(DesktopPlatform.Current == DesktopPlatform.MacOS)

        val text = "word1 word2"
        var selection by mutableStateOf<Selection?>(null)
        lateinit var textLayout: TextLayoutResult
        rule.setContent {
            SelectionContainer(
                modifier = Modifier.fillMaxSize(),
                selection = selection,
                onSelectionChange = { selection = it }
            ) {
                BasicText(
                    text = text,
                    onTextLayout = { textLayout = it },
                    modifier = Modifier
                        .testTag("selectable")
                )
            }
        }


        rule.onNodeWithTag("selectable").apply {
            // Triple click-to select everything
            performMouseInput {
                tripleClick(center)
            }
            assertEquals(expected = text, actual = selection.selectedText(text))

            // Right-click first word; selection should not change
            performMouseInput {
                val firstCharBounds = textLayout.getBoundingBox(0)
                rightClick(firstCharBounds.center)
            }
            assertEquals(expected = text, actual = selection.selectedText(text))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rightClickOnMacOsInTextFieldSelectionDoesNotCancelExistingSelection() {
        assumeTrue(DesktopPlatform.Current == DesktopPlatform.MacOS)

        val text = "word1 word2"
        var textFieldValue by mutableStateOf(
            // Pre-select everything
            TextFieldValue(
                text = text,
                selection = TextRange(0, text.length),
            )
        )
        lateinit var textLayout: TextLayoutResult
        rule.setContent {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                onTextLayout = { textLayout = it },
                decorationBox = { innerTextField ->  // Test with decoration box
                    Box(
                        modifier = Modifier.padding(100.dp)
                    ) {
                        Box(Modifier.testTag("textfield")) {
                            innerTextField()
                        }
                    }
                }
            )
        }

        rule.onNodeWithTag("textfield", useUnmergedTree = true).apply {
            assertEquals(expected = text, actual = textFieldValue.selectedText(text))

            // Right-click first word; selection should not change
            performMouseInput {
                val firstCharBounds = textLayout.getBoundingBox(0)
                rightClick(firstCharBounds.center)
            }
            assertEquals(expected = text, actual = textFieldValue.selectedText(text))
        }
    }

}

private fun Selection?.selectedText(text: String): String? {
    if (this == null) return null
    return text.substring(startIndex = start.offset, endIndex = end.offset)
}

private fun TextFieldValue.selectedText(text: String): String {
    return text.substring(startIndex = selection.start, endIndex = selection.end)
}