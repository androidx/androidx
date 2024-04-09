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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.SdkSuppress
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
internal class BasicTextFieldImmIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField"
    private val imm = FakeInputMethodManager()

    @Before
    fun setUp() {
        immRule.setFactory { imm }
    }

    @Test
    fun becomesTextEditor_whenFocusGained() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state, Modifier.testTag(Tag))
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
        lateinit var focusManager: FocusManager
        inputMethodInterceptor.setTextFieldTestContent {
            focusManager = LocalFocusManager.current
            BasicTextField(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        rule.runOnIdle {
            focusManager.clearFocus()
        }
        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun doesNotStopBeingTextEditor_whenWindowFocusLost() {
        val state = TextFieldState()
        var windowFocus by mutableStateOf(true)
        inputMethodInterceptor.setContent {
            CompositionLocalProvider(LocalWindowInfo provides object : WindowInfo {
                override val isWindowFocused: Boolean get() = windowFocus
            }) {
                BasicTextField(state, Modifier.testTag(Tag))
            }
        }
        requestFocus(Tag)
        rule.runOnIdle {
            windowFocus = false
        }
        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun stopsBeingTextEditor_whenChangedToReadOnly() {
        val state = TextFieldState()
        var readOnly by mutableStateOf(false)
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state, Modifier.testTag(Tag), readOnly = readOnly)
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state, Modifier.testTag(Tag), enabled = enabled)
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state1, Modifier.testTag(Tag + 1))
            BasicTextField(state2, Modifier.testTag(Tag + 2))
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
        inputMethodInterceptor.setTextFieldTestContent {
            if (compose) {
                BasicTextField(state, Modifier.testTag(Tag))
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state, Modifier.testTag(Tag))
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = {
                    // Force the selection not to change.
                    val initialSelection = selection
                    append("world")
                    selection = initialSelection
                }
            )
        }
        requestFocus(Tag)
        inputMethodInterceptor.withInputConnection {
            // TODO move this before withInputConnection?
            imm.resetCalls()

            commitText("hello", 1)

            @Suppress("SpellCheckingInspection")
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
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = {
                    val initialSelection = selection
                    append("world")
                    selection = initialSelection
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

    @FlakyTest(bugId = 290927588)
    @Test
    fun immUpdated_whenFilterChangesSelection_fromInputConnection() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { selectAll() }
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
        inputMethodInterceptor.setContent {
            BasicTextField(state, Modifier.testTag(Tag))
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
        val state = TextFieldState("hello", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(state, Modifier.testTag(Tag))
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
        inputMethodInterceptor.setContent {
            BasicTextField(state, Modifier.testTag(Tag))
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
    fun immNotRestarted_whenEditsComeFromIME() {
        // We are not expecting the IME to restart itself when changes are coming from the IME.
        // Following edits are simulated as if they are coming from IME through InputConnection.
        // We expect calls to `updateSelection` but never `restartInput`
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setContent {
            BasicTextField(state, Modifier.testTag(Tag))
        }
        requestFocus(Tag)
        inputMethodInterceptor.withInputConnection {
            beginBatchEdit()
            commitText(" World", 1)
            endBatchEdit()
        }

        rule.runOnIdle {
            imm.expectCall("updateSelection(11, 11, -1, -1)")
            imm.expectNoMoreCalls()
            assertThat(state.text.toString()).isEqualTo("Hello World")
        }

        inputMethodInterceptor.withInputConnection {
            beginBatchEdit()
            // Remove the " World" that we added in the previous edit.
            deleteSurroundingText(6, 0)
            endBatchEdit()
        }

        rule.runOnIdle {
            imm.expectCall("updateSelection(5, 5, -1, -1)")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun immNotRestarted_whenKeyboardIsConfiguredAsPassword() {
        val state = TextFieldState()
        inputMethodInterceptor.setContent {
            BasicTextField(
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
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = object : InputTransformation {
                    override val keyboardOptions: KeyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Password)

                    override fun TextFieldBuffer.transformInput() {
                        append('A')
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

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun setHintLocales() {
        val state = TextFieldState()
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                keyboardOptions = KeyboardOptions(hintLocales = LocaleList("tr"))
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withEditorInfo {
            assertThat(hintLocales?.toLanguageTags()).isEqualTo("tr")
        }
    }

    private fun requestFocus(tag: String) =
        rule.onNodeWithTag(tag).requestFocus()
}

// sets the WindowInfo with isWindowFocused is true
internal fun InputMethodInterceptor.setTextFieldTestContent(
    content: @Composable () -> Unit
) {
    val windowInfo = object : WindowInfo {
        override val isWindowFocused = true
    }
    this.setContent {
        CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
            Row {
                // Extra focusable that takes initial focus when focus is cleared.
                Box(
                    Modifier
                        .size(10.dp)
                        .focusable())
                Box { content() }
            }
        }
    }
}
