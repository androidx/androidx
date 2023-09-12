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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.input.InputTransformation
import androidx.compose.foundation.text2.input.TextFieldBuffer
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.placeCursorAtEnd
import androidx.compose.foundation.text2.input.selectAll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
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

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    @get:Rule
    val inputMethodInterceptor = InputMethodInterceptorRule(rule)

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
            BasicTextField2(state, Modifier.testTag(Tag))
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            commitText("hello", 0)
            assertThat(state.text.toString()).isEqualTo("hello")
        }
    }

    @Test
    fun stopsBeingTextEditor_whenFocusLost() {
        val state = TextFieldState()
        var focusManager: FocusManager? = null
        rule.setContent {
            focusManager = LocalFocusManager.current
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            focusManager!!.clearFocus()
        }
        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun stopsBeingTextEditor_whenChangedToReadOnly() {
        val state = TextFieldState()
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag), readOnly = readOnly)
        }
        requestFocus(Tag)
        inputMethodInterceptor.assertSessionActive()

        readOnly = true

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun stopsBeingTextEditor_whenChangedToDisabled() {
        val state = TextFieldState()
        var enabled by mutableStateOf(true)
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag), enabled = enabled)
        }
        requestFocus(Tag)
        inputMethodInterceptor.assertSessionActive()

        enabled = false

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun staysTextEditor_whenFocusTransferred() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        rule.setContent {
            BasicTextField2(state1, Modifier.testTag(Tag + 1))
            BasicTextField2(state2, Modifier.testTag(Tag + 2))
        }

        requestFocus(Tag + 1)
        requestFocus(Tag + 2)

        inputMethodInterceptor.withInputConnection {
            commitText("hello", 0)
            endBatchEdit()
            assertThat(state2.text.toString()).isEqualTo("hello")
            assertThat(state1.text.toString()).isEmpty()
        }
    }

    @Test
    fun stopsBeingTextEditor_whenRemovedFromCompositionWhileFocused() {
        val state = TextFieldState()
        var compose by mutableStateOf(true)
        rule.setContent {
            if (compose) {
                BasicTextField2(state, Modifier.testTag(Tag))
            }
        }
        requestFocus(Tag)
        rule.runOnIdle {
            compose = false
        }

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun inputRestarted_whenStateInstanceChanged() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        var state by mutableStateOf(state1)
        rule.setContent {
            BasicTextField2(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)

        state = state2

        inputMethodInterceptor.withInputConnection {
            commitText("hello", 0)
            assertThat(state2.text.toString()).isEqualTo("hello")
            assertThat(state1.text.toString()).isEmpty()
        }
    }

    @Test
    fun immUpdated_whenFilterChangesText_fromInputConnection() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { _, new ->
                    // Force the selection not to change.
                    val initialSelection = new.selectionInChars
                    new.append("world")
                    new.selectCharsIn(initialSelection)
                }
            )
        }
        requestFocus(Tag)
        inputMethodInterceptor.withInputConnection {
            // TODO move this before withInputConnection?
            imm.resetCalls()

            commitText("hello", 1)

            assertThat(state.text.toString()).isEqualTo("helloworld")
        }

        rule.runOnIdle {
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
                inputTransformation = { _, new ->
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
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { _, new -> new.selectAll() }
            )
        }
        requestFocus(Tag)
        inputMethodInterceptor.withInputConnection {
            imm.resetCalls()
            setComposingText("hello", 1)
        }

        rule.runOnIdle {
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
        }

        rule.runOnIdle {
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
        }

        rule.runOnIdle {
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
        }

        rule.runOnIdle {
            imm.expectCall("updateSelection(5, 5, -1, -1)")
            imm.expectCall("restartInput")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immNotRestarted_whenKeyboardIsConfiguredAsPassword() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
        requestFocus(Tag)
        rule.runOnIdle { imm.resetCalls() }

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.Backspace) }

        rule.runOnIdle {
            imm.expectCall("updateSelection(1, 1, -1, -1)")
            imm.expectCall("updateSelection(0, 0, -1, -1)")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immNotRestarted_whenKeyboardIsConfiguredAsPassword_fromTransformation() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = object : InputTransformation {
                    override val keyboardOptions: KeyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Password)

                    override fun transformInput(
                        originalValue: TextFieldCharSequence,
                        valueWithChanges: TextFieldBuffer
                    ) {
                        valueWithChanges.append('A')
                    }
                }
            )
        }
        requestFocus(Tag)
        rule.runOnIdle { imm.resetCalls() }

        // "" -key-> "A" -filter> "AA"
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }
        // "AA" -key-> "A" -filter> "AA"
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.Backspace) }

        rule.runOnIdle {
            imm.expectCall("updateSelection(2, 2, -1, -1)")
            imm.expectCall("updateSelection(2, 2, -1, -1)")
            imm.expectNoMoreCalls()
        }
    }

    private fun requestFocus(tag: String) =
        rule.onNodeWithTag(tag).requestFocus()
}
