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

package androidx.compose.foundation.text2.input.internal.undo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.selection.FakeClipboardManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextField2UndoTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun canUndo_imeInsert() {
        val state = TextFieldState("Hello", TextRange(5))

        rule.setContent {
            BasicTextField2(state)
        }

        rule.onNode(hasSetTextAction()).performTextInput(", World")
        assertThat(state.text.toString()).isEqualTo("Hello, World")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("Hello")
        rule.onNode(hasSetTextAction()).assertTextEquals("Hello")
    }

    @Test
    fun canRedo_imeInsert() {
        val state = TextFieldState("Hello", TextRange(5))

        rule.setContent {
            BasicTextField2(state)
        }

        rule.onNode(hasSetTextAction()).performTextInput(", World")

        state.undoState.undo()
        rule.onNode(hasSetTextAction()).assertTextEquals("Hello")

        state.undoState.redo()
        rule.onNode(hasSetTextAction()).assertTextEquals("Hello, World")
    }

    @Test
    fun undoMerges_imeInserts() {
        val state = TextFieldState("Hello", TextRange(5))

        rule.setContent {
            BasicTextField2(state)
        }

        rule.onNode(hasSetTextAction()).typeText(", World")
        assertThat(state.text.toString()).isEqualTo("Hello, World")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("Hello")
        rule.onNode(hasSetTextAction()).assertTextEquals("Hello")
    }

    @Test
    fun undoMerges_imeInserts_onlyInForwardsDirection() {
        val state = TextFieldState("Hello", TextRange(5))

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            performTextInput(", World")
            performTextInputSelection(TextRange(5))
            performTextInput(" Compose")
        }
        assertThat(state.text.toString()).isEqualTo("Hello Compose, World")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("Hello, World")
        rule.onNode(hasSetTextAction()).assertTextEquals("Hello, World")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("Hello")
        rule.onNode(hasSetTextAction()).assertTextEquals("Hello")
    }

    @Test
    fun undoMerges_deletes() {
        val state = TextFieldState("Hello, World", TextRange(12))

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            requestFocus()
            performKeyInput {
                repeat(12) {
                    pressKey(Key.Backspace)
                }
            }
        }
        state.assertTextAndSelection("", TextRange.Zero)

        state.undoState.undo()

        state.assertTextAndSelection("Hello, World", TextRange(12))
    }

    @Test
    fun undoDoesNotMerge_deletes_inBothDirections() {
        val state = TextFieldState("Hello, World", TextRange(6))

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            requestFocus()
            performKeyInput {
                repeat(6) {
                    pressKey(Key.Backspace)
                }
                repeat(6) {
                    pressKey(Key.Delete)
                }
            }
        }
        state.assertTextAndSelection("", TextRange.Zero)

        state.undoState.undo()
        state.assertTextAndSelection(" World", TextRange(0))

        state.undoState.undo()
        state.assertTextAndSelection("Hello, World", TextRange(6))
    }

    @Test
    fun undo_revertsSelection() {
        val state = TextFieldState("Hello", TextRange(5))

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            performTextInputSelection(TextRange(0, 5))
            performTextInput("a")
        }
        assertThat(state.text.toString()).isEqualTo("a")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("Hello")
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun redo_revertsSelection() {
        val state = TextFieldState("Hello", TextRange(5))

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            performTextInputSelection(TextRange(2))
            performTextInput(" abc ")
        }
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(7))

        state.undoState.undo()

        assertThat(state.text.selectionInChars).isNotEqualTo(TextRange(7))

        state.undoState.redo()
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(7))
    }

    @Test
    fun variousEditOperations() {
        val state = TextFieldState()

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            typeText("abc def")
            performTextInputSelection(TextRange(4))
            typeText("123 ")
            performTextInputSelection(TextRange(0, 3))
            typeText("ghi")
            performTextClearance()
        }
        state.assertTextAndSelection("", TextRange.Zero)
        state.undoState.undo()
        state.assertTextAndSelection("ghi 123 def", TextRange(3))
        state.undoState.undo()
        state.assertTextAndSelection("g 123 def", TextRange(1))
        state.undoState.undo()
        state.assertTextAndSelection("abc 123 def", TextRange(0, 3))
        state.undoState.undo()
        state.assertTextAndSelection("abc def", TextRange(4))
        state.undoState.undo()
        state.assertTextAndSelection("", TextRange.Zero)
        assertThat(state.undoState.canUndo).isFalse()
    }

    @Test
    fun clearHistory_removesAllUndoAndRedo() {
        val state = TextFieldState()

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            typeText("abc def")
            performTextInputSelection(TextRange(4))
            typeText("123 ")
            performTextInputSelection(TextRange(0, 3))
            typeText("ghi")
            performTextClearance()
        }
        state.undoState.undo()
        state.undoState.undo()
        state.undoState.undo()

        assertThat(state.undoState.canUndo).isTrue()
        assertThat(state.undoState.canRedo).isTrue()

        state.undoState.clearHistory()

        assertThat(state.undoState.canUndo).isFalse()
        assertThat(state.undoState.canRedo).isFalse()
    }

    @Test
    fun paste_neverMerges() {
        val state = TextFieldState()
        val clipboardManager = FakeClipboardManager("ghi")

        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(state)
            }
        }

        with(rule.onNode(hasSetTextAction())) {
            typeText("abc def ")
            performSemanticsAction(SemanticsActions.PasteText)
            typeText(" jkl")
        }
        state.undoState.undo()

        state.assertTextAndSelection("abc def ghi", TextRange(11))

        state.undoState.undo()

        state.assertTextAndSelection("abc def ", TextRange(8))

        state.undoState.undo()

        state.assertTextAndSelection("", TextRange.Zero)
    }

    @Test
    fun cut_neverMerges() {
        val state = TextFieldState("abc def ghi", TextRange(11))

        rule.setContent {
            BasicTextField2(state)
        }

        with(rule.onNode(hasSetTextAction())) {
            requestFocus()
            repeat(4) {
                performKeyInput {
                    pressKey(Key.Backspace)
                }
            }
            performTextInputSelection(TextRange(4, 7))
            performSemanticsAction(SemanticsActions.CutText)
            repeat(4) {
                performKeyInput {
                    pressKey(Key.Backspace)
                }
            }
        }
        state.assertTextAndSelection("", TextRange.Zero)

        state.undoState.undo()

        state.assertTextAndSelection("abc ", TextRange(4))

        state.undoState.undo()

        state.assertTextAndSelection("abc def", TextRange(4, 7))

        state.undoState.undo()

        state.assertTextAndSelection("abc def ghi", TextRange(11))
    }

    private fun SemanticsNodeInteraction.typeText(text: String) {
        text.forEach { performTextInput(it.toString()) }
    }

    private fun TextFieldState.assertTextAndSelection(text: String, selection: TextRange) {
        assertThat(this.text.toString()).isEqualTo(text)
        assertThat(this.text.selectionInChars).isEqualTo(selection)
    }
}
