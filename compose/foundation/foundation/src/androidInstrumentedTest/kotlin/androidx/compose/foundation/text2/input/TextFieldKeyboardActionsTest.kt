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

package androidx.compose.foundation.text2.input

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text2.input.TextFieldLineLimits.SingleLine
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
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldKeyboardActionsTest : FocusedWindowTest {

    @get:Rule
    val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    @Test
    fun textField_performsImeAction_viaSemantics() {
        var called = false
        rule.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions {
                    called = true
                }
            )
        }

        rule.onNode(hasSetTextAction()).performImeAction()

        assertThat(called).isTrue()
    }

    @Test
    fun textField_performsImeAction_viaInputConnection() {
        var called = false
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions {
                    called = true
                }
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
        var calledFor: ImeAction? = null
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActionsAll {
                    calledFor = it
                }
            )
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            assertThat(calledFor).isEqualTo(ImeAction.Search)
        }
    }

    @Test
    fun textField_performsDefaultBehavior_forFocusNext() {
        rule.setTextFieldTestContent {
            Column {
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box1"))
                BasicTextField2(
                    state = TextFieldState(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithTag("box2").assertIsFocused()
    }

    @Test
    fun textField_performsDefaultBehavior_forFocusPrevious() {
        rule.setTextFieldTestContent {
            Column {
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box1"))
                BasicTextField2(
                    state = TextFieldState(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Previous)
                )
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box2"))
            }
        }

        rule.onNodeWithTag("box1").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithTag("box1").assertIsFocused()
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun textField_performsDefaultBehavior_forDone() {
        val testKeyboardController = TestSoftwareKeyboardController(rule)
        rule.setTextFieldTestContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides testKeyboardController
            ) {
                BasicTextField2(
                    state = TextFieldState(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
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
        rule.setTextFieldTestContent {
            Column {
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box1"))
                BasicTextField2(
                    state = TextFieldState(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActionsAll {
                        // don't call default action
                    }
                )
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNode(hasSetTextAction()).assertIsFocused()
        rule.onNodeWithTag("box2").assertIsNotFocused()
    }

    @Test
    fun textField_canRequestDefaultBehavior() {
        rule.setTextFieldTestContent {
            Column {
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box1"))
                BasicTextField2(
                    state = TextFieldState(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActionsAll {
                        defaultKeyboardAction(it)
                    }
                )
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithTag("box2").assertIsFocused()
    }

    @Test
    fun textField_performsGo_whenReceivedImeActionIsGo() {
        var called = false
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardActions = KeyboardActions(onGo = {
                    called = true
                })
            )
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performEditorAction(EditorInfo.IME_ACTION_GO)
            assertThat(called).isTrue()
        }
    }

    @Test
    fun textField_doesNotPerformGo_whenReceivedImeActionIsNotGo() {
        var called = false
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardActions = KeyboardActions(onGo = {
                    called = true
                })
            )
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        inputMethodInterceptor.withInputConnection {
            performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            assertThat(called).isFalse()
        }
    }

    @Test
    fun textField_changingKeyboardActions_usesNewKeyboardActions() {
        var lastCaller = 0
        val actions1 = KeyboardActionsAll { lastCaller = 1 }
        val actions2 = KeyboardActionsAll { lastCaller = 2 }
        var keyboardActions by mutableStateOf(actions1)
        rule.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = keyboardActions
            )
        }

        rule.onNode(hasSetTextAction()).performImeAction()
        rule.runOnIdle { assertThat(lastCaller).isEqualTo(1) }

        keyboardActions = actions2

        // do not go through focus requests again
        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.OnImeAction)
        rule.runOnIdle { assertThat(lastCaller).isEqualTo(2) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textField_singleLinePressEnter_triggersPassedImeAction() {
        var calledFor: ImeAction? = null
        rule.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActionsAll {
                    calledFor = it
                },
                lineLimits = SingleLine
            )
        }

        with(rule.onNode(hasSetTextAction())) {
            performClick()
            performKeyInput { pressKey(Key.Enter) }
        }
        rule.runOnIdle { assertThat(calledFor).isEqualTo(ImeAction.Go) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textField_multiLinePressEnter_doesNotTriggerPassedImeAction() {
        var calledFor: ImeAction? = null
        rule.setTextFieldTestContent {
            BasicTextField2(
                state = TextFieldState(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActionsAll {
                    calledFor = it
                },
                lineLimits = MultiLine(maxHeightInLines = 1)
            )
        }

        with(rule.onNode(hasSetTextAction())) {
            performClick()
            performKeyInput { pressKey(Key.Enter) }
        }
        rule.runOnIdle { assertThat(calledFor).isNull() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textField_singleLinePressEnter_triggersDefaultBehavior() {
        rule.setTextFieldTestContent {
            Column {
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box1"))
                BasicTextField2(
                    state = TextFieldState(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    lineLimits = SingleLine
                )
                Box(
                    Modifier
                        .size(1.dp)
                        .focusable()
                        .testTag("box2"))
            }
        }

        rule.onNodeWithTag("box2").assertIsNotFocused()
        with(rule.onNode(hasSetTextAction())) {
            performClick()
            performKeyInput { pressKey(Key.Enter) }
        }
        rule.onNodeWithTag("box2").assertIsFocused()
    }
}

private fun KeyboardActionsAll(
    onAny: KeyboardActionScope.(ImeAction) -> Unit
): KeyboardActions = KeyboardActions(
    onDone = { onAny(ImeAction.Done) },
    onGo = { onAny(ImeAction.Go) },
    onNext = { onAny(ImeAction.Next) },
    onPrevious = { onAny(ImeAction.Previous) },
    onSearch = { onAny(ImeAction.Search) },
    onSend = { onAny(ImeAction.Send) }
)
