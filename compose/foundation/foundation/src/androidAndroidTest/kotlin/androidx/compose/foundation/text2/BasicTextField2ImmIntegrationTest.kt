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

package androidx.compose.foundation.text2

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.ComposeInputMethodManager
import androidx.compose.foundation.text2.input.internal.setInputConnectionCreatedListenerForTests
import androidx.compose.foundation.text2.input.placeCursorAtEnd
import androidx.compose.foundation.text2.input.selectAll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalTestApi::class,
)
@SmallTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextField2ImmIntegrationTest {

    private lateinit var hostView: View

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    private val Tag = "BasicTextField2"
    private val imm = FakeInputMethodManager()

    @Before
    fun setUp() {
        immRule.setFactory { imm }
    }

    @Test
    fun becomesTextEditor_whenFocusGained() {
        val state = TextFieldState()
        rule.setContent {
            hostView = LocalView.current
            BasicTextField2(state, Modifier.testTag(Tag))
        }

        requestFocus(Tag)

        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isTrue()
            val connection = hostView.onCreateInputConnection(EditorInfo())
            assertThat(connection).isNotNull()
            connection.commitText("hello", 0)
            assertThat(state.text.toString()).isEqualTo("hello")
        }
    }

    @Test
    fun stopsBeingTextEditor_whenFocusLost() {
        val state = TextFieldState()
        var focusManager: FocusManager? = null
        rule.setContent {
            hostView = LocalView.current
            focusManager = LocalFocusManager.current
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            focusManager!!.clearFocus()
        }
        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isFalse()
            assertThat(hostView.onCreateInputConnection(EditorInfo())).isNull()
        }
    }

    @Test
    fun staysTextEditor_whenFocusTransferred() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        rule.setContent {
            hostView = LocalView.current
            BasicTextField2(state1, Modifier.testTag(Tag + 1))
            BasicTextField2(state2, Modifier.testTag(Tag + 2))
        }

        requestFocus(Tag + 1)
        requestFocus(Tag + 2)

        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isTrue()
            val connection = hostView.onCreateInputConnection(EditorInfo())
            assertThat(connection).isNotNull()
            connection.commitText("hello", 0)
            connection.endBatchEdit()
            assertThat(state2.text.toString()).isEqualTo("hello")
            assertThat(state1.text.toString()).isEmpty()
        }
    }

    @Test
    fun stopsBeingTextEditor_whenRemovedFromCompositionWhileFocused() {
        val state = TextFieldState()
        var compose by mutableStateOf(true)
        rule.setContent {
            hostView = LocalView.current
            if (compose) {
                BasicTextField2(state, Modifier.testTag(Tag))
            }
        }
        requestFocus(Tag)
        rule.runOnIdle {
            compose = false
        }

        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isFalse()
            assertThat(hostView.onCreateInputConnection(EditorInfo())).isNull()
        }
    }

    @Test
    fun inputRestarted_whenStateInstanceChanged() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        var state by mutableStateOf(state1)
        rule.setContent {
            hostView = LocalView.current
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)

        state = state2

        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isTrue()
            val connection = hostView.onCreateInputConnection(EditorInfo())
            assertThat(connection).isNotNull()
            connection.commitText("hello", 0)
            assertThat(state2.text.toString()).isEqualTo("hello")
            assertThat(state1.text.toString()).isEmpty()
        }
    }

    @Test
    fun immUpdated_whenFilterChangesText_fromInputConnection() {
        val state = TextFieldState()
        rule.setContent {
            hostView = LocalView.current
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                filter = { _, new ->
                    // Force the selection not to change.
                    val initialSelection = new.selectionInChars
                    new.append("world")
                    new.selectCharsIn(initialSelection)
                }
            )
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()
            assertThat(hostView.onCheckIsTextEditor()).isTrue()
            val connection = hostView.onCreateInputConnection(EditorInfo())
            assertThat(connection).isNotNull()

            connection.commitText("hello", 1)

            assertThat(state.text.toString()).isEqualTo("helloworld")
            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenFilterChangesText_fromKeyEvent() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                filter = { _, new ->
                    val initialSelection = new.selectionInChars
                    new.append("world")
                    new.selectCharsIn(initialSelection)
                }
            )
        }
        requestFocus(Tag)
        rule.runOnIdle { imm.resetCalls() }

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }

        rule.runOnIdle {
            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenFilterChangesSelection_fromInputConnection() {
        val state = TextFieldState()
        var inputConnection: InputConnection? = null
        setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                filter = { _, new -> new.selectAll() }
            )
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()
            inputConnection!!.setComposingText("hello", 1)
            imm.expectCall("updateSelection(0, 5, 0, 5)")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenEditChangesText() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()

            state.edit {
                append("hello")
                placeCursorBeforeCharAt(0)
            }

            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenEditChangesSelection() {
        val state = TextFieldState("hello", initialSelectionInChars = TextRange(0))
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()

            state.edit {
                placeCursorAtEnd()
            }

            imm.expectCall("updateSelection(5, 5, -1, -1)")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenEditChangesTextAndSelection() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()

            state.edit {
                append("hello")
                placeCursorAtEnd()
            }

            imm.expectCall("updateSelection(5, 5, -1, -1)")
            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    private fun requestFocus(tag: String) =
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)

    private class FakeInputMethodManager : ComposeInputMethodManager {
        private val calls = mutableListOf<String>()

        fun expectCall(description: String) {
            assertThat(calls.removeFirst()).isEqualTo(description)
        }

        fun expectNoMoreCalls() {
            assertThat(calls).isEmpty()
        }

        fun resetCalls() {
            calls.clear()
        }

        override fun restartInput() {
            calls += "restartInput"
        }

        override fun showSoftInput() {
            calls += "showSoftInput"
        }

        override fun hideSoftInput() {
            calls += "hideSoftInput"
        }

        override fun updateExtractedText(token: Int, extractedText: ExtractedText) {
            calls += "updateExtractedText"
        }

        override fun updateSelection(
            selectionStart: Int,
            selectionEnd: Int,
            compositionStart: Int,
            compositionEnd: Int
        ) {
            calls += "updateSelection($selectionStart, $selectionEnd, " +
                "$compositionStart, $compositionEnd)"
        }

        override fun sendKeyEvent(event: KeyEvent) {
            calls += "sendKeyEvent"
        }
    }
}
