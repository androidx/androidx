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
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.AndroidTextInputAdapter
import androidx.compose.foundation.text2.input.internal.ComposeInputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextField2ImmIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    private val Tag = "BasicTextField2"

    private val imm = FakeInputMethodManager()
    private val state = TextFieldState()

    @Before
    fun setUp() {
        immRule.setFactory { imm }
    }

    @Test
    fun keyboardVisibility_whenFocusGained() {
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }

        rule.runOnIdle {
            imm.expectNoMoreCalls()
        }

        requestFocus(Tag)

        rule.runOnIdle {
            imm.expectCall("restartInput")
            imm.expectCall("showSoftInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun keyboardVisibility_whenFocusLost() {
        var focusManager: FocusManager? = null
        rule.setContent {
            focusManager = LocalFocusManager.current
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()

            focusManager!!.clearFocus()
        }

        rule.runOnIdle {
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun keyboardVisibility_whenFocusTransferred() {
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag + 1))
            BasicTextField2(state, Modifier.testTag(Tag + 2))
        }
        requestFocus(Tag + 1)
        rule.runOnIdle { imm.resetCalls() }

        requestFocus(Tag + 2)

        rule.runOnIdle {
            imm.expectCall("restartInput")
            imm.expectCall("showSoftInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun keyboardVisibility_whenRemovedFromCompositionWhileFocused() {
        var compose by mutableStateOf(true)
        rule.setContent {
            if (compose) {
                BasicTextField2(state, Modifier.testTag(Tag))
            }
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()

            compose = false
        }

        rule.runOnIdle {
            imm.expectCall("hideSoftInput")
            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun inputRestarted_whenStateInstanceChanged() {
        var state by mutableStateOf(state)
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle { imm.resetCalls() }

        state = TextFieldState()

        rule.runOnIdle {
            imm.expectCall("restartInput")
            imm.expectCall("showSoftInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenFilterChangesText_fromInputConnection() {
        val state = TextFieldState(filter = { _, new -> new.copy(text = new.text + "world") })
        var inputConnection: InputConnection? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()
            inputConnection!!.setComposingText("hello", 1)
            imm.expectCall("updateSelection(5, 5, -1, -1)")
            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immUpdated_whenFilterChangesText_fromKeyEvent() {
        val state = TextFieldState(filter = { _, new -> new.copy(text = new.text + "world") })
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
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
        val state = TextFieldState(filter = { _, new ->
            new.copy(selection = TextRange(0, new.text.length))
        })
        var inputConnection: InputConnection? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            imm.resetCalls()
            inputConnection!!.setComposingText("hello", 1)
            imm.expectCall("updateSelection(0, 5, 0, 5)")
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
