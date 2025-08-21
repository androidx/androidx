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

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldKeyboardActionsTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    @Test
    fun textField_performsImeAction_viaSemantics() {
        var called = false
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { called = true },
            )
        }

        rule.onNode(hasSetTextAction()).performImeAction()

        assertThat(called).isTrue()
    }

    @Test
    fun textField_performsImeAction_viaInputConnection() {
        var called = false
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { called = true },
            )
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performEditorAction(EditorInfo.IME_ACTION_SEND)
            assertThat(called).isTrue()
        }
    }

    @Test
    fun textField_performsUnexpectedImeAction_fromInputConnection() {
        var called = false
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { called = true },
            )
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            assertThat(called).isTrue()
        }
    }

    @Test
    fun textField_performsDefaultBehavior_forFocusNext() {
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            Column {
                Box(Modifier.size(1.dp).focusable().testTag("box1"))
                BasicTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                Box(Modifier.size(1.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithTag("box2").assertIsFocused()
    }

    @Test
    fun textField_performsDefaultBehavior_forFocusPrevious() {
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            Column {
                Box(Modifier.size(1.dp).focusable().testTag("box1"))
                BasicTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Previous),
                )
                Box(Modifier.size(1.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box1").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithTag("box1").assertIsFocused()
    }

    @Test
    fun textField_performsDefaultBehavior_forDone() {
        val testKeyboardController = TestSoftwareKeyboardController(rule)
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides testKeyboardController
            ) {
                BasicTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()
        testKeyboardController.assertShown()
        rule.onNode(hasSetTextAction()).performImeAction()
        testKeyboardController.assertHidden()
    }

    @Test
    fun textField_canOverrideDefaultBehavior() {
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            Column {
                Box(Modifier.size(1.dp).focusable().testTag("box1"))
                BasicTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    onKeyboardAction = {
                        // don't call default action
                    },
                )
                Box(Modifier.size(1.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNode(hasSetTextAction()).assertIsFocused()
        rule.onNodeWithTag("box2").assertIsNotFocused()
    }

    @Test
    fun textField_canRequestDefaultBehavior() {
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            Column {
                Box(Modifier.size(1.dp).focusable().testTag("box1"))
                BasicTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    onKeyboardAction = { it() },
                )
                Box(Modifier.size(1.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithTag("box2").assertIsFocused()
    }

    @Test
    fun textField_changingKeyboardActions_usesNewKeyboardActions() {
        var lastCaller = 0
        val actions1 = KeyboardActionHandler { lastCaller = 1 }
        val actions2 = KeyboardActionHandler { lastCaller = 2 }
        var onKeyboardAction by mutableStateOf(actions1)
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = onKeyboardAction,
            )
        }

        rule.onNode(hasSetTextAction()).performImeAction()
        rule.runOnIdle { assertThat(lastCaller).isEqualTo(1) }

        onKeyboardAction = actions2

        // do not go through focus requests again
        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.OnImeAction)
        rule.runOnIdle { assertThat(lastCaller).isEqualTo(2) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textField_singleLinePressEnter_triggersKeyboardAction() {
        var called = false
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                onKeyboardAction = { called = true },
                lineLimits = SingleLine,
            )
        }

        with(rule.onNode(hasSetTextAction())) {
            performClick()
            performKeyInput { pressKey(Key.Enter) }
        }
        rule.runOnIdle { assertThat(called).isTrue() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textField_multiLinePressEnter_doesNotTriggerKeyboardAction() {
        var called = false
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                onKeyboardAction = { called = true },
                lineLimits = MultiLine(maxHeightInLines = 1),
            )
        }

        with(rule.onNode(hasSetTextAction())) {
            performClick()
            performKeyInput { pressKey(Key.Enter) }
        }
        rule.runOnIdle { assertThat(called).isFalse() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textField_singleLinePressEnter_triggersDefaultBehavior() {
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            Column {
                Box(Modifier.size(1.dp).focusable().testTag("box1"))
                BasicTextField(
                    state = state,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    lineLimits = SingleLine,
                )
                Box(Modifier.size(1.dp).focusable().testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        with(rule.onNode(hasSetTextAction())) {
            performClick()
            performKeyInput { pressKey(Key.Enter) }
        }
        rule.onNodeWithTag("box2").assertIsFocused()
    }

    @Test
    fun textField_ImeActionNone_isNotPassedToKeyboardActionHandler() {
        var called = false
        val state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
                onKeyboardAction = { called = true },
            )
        }

        rule.onNode(hasSetTextAction()).performImeAction()

        assertThat(called).isFalse()
    }

    @Test
    fun textField_ImeActionDefault_isNotPassedToKeyboardActionHandler() {
        var called = false
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                onKeyboardAction = { called = true },
            )
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performEditorAction(EditorInfo.IME_ACTION_NONE)
            performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
        }

        assertThat(called).isFalse()
    }
}
