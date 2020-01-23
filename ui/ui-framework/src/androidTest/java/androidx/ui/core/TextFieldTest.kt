/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.state
import androidx.test.filters.SmallTest
import androidx.ui.core.input.FocusManager
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.InputState
import androidx.ui.input.TextInputService
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun textField_focusInSemantics() {
        val focusManager = mock<FocusManager>()
        val inputService = mock<TextInputService>()
        composeTestRule.setContent {
            val state = state { "" }
            Providers(
                FocusManagerAmbient provides focusManager,
                TextInputServiceAmbient provides inputService
            ) {
                TestTag(tag = "textField") {
                    TextField(
                        value = state.value,
                        onValueChange = { state.value = it }
                    )
                }
            }
        }

        findByTag("textField").doClick()

        composeTestRule.runOnIdleCompose {
            verify(focusManager, times(1)).requestFocus(any())
        }
    }

    @Composable
    private fun TextFieldApp() {
        val state = state { "" }
        TextField(
            value = state.value,
            onValueChange = {
                state.value = it
            }
        )
    }

    @Test
    fun textField_commitTexts() {
        val focusManager = mock<FocusManager>()
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        // Always give focus to the passed node.
        whenever(focusManager.requestFocus(any())).thenAnswer {
            (it.arguments[0] as FocusManager.FocusNode).onFocus()
        }
        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        composeTestRule.setContent {
            Providers(
                FocusManagerAmbient provides focusManager,
                TextInputServiceAmbient provides textInputService
            ) {
                TestTag(tag = "textField") {
                    TextFieldApp()
                }
            }
        }

        // Perform click to focus in.
        findByTag("textField")
            .doClick()

        var onEditCommandCallback: ((List<EditOperation>) -> Unit)? = null
        composeTestRule.runOnIdleCompose {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditOperation>) -> Unit>()
            verify(textInputService, times(1)).startInput(
                initModel = any(),
                keyboardType = any(),
                imeAction = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        arrayOf(
            listOf(CommitTextEditOp("1", 1)),
            listOf(CommitTextEditOp("a", 1)),
            listOf(CommitTextEditOp("2", 1)),
            listOf(CommitTextEditOp("b", 1)),
            listOf(CommitTextEditOp("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            composeTestRule.runOnIdleCompose { onEditCommandCallback!!.invoke(it) }
        }

        composeTestRule.runOnIdleCompose {
            val stateCaptor = argumentCaptor<InputState>()
            verify(textInputService, atLeastOnce())
                .onStateUpdated(eq(inputSessionToken), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "1a2b3".
            assertThat(stateCaptor.lastValue.text).isEqualTo("1a2b3")
        }
    }

    @Composable
    private fun OnlyDigitsApp() {
        val state = state { "" }
        TextField(
            value = state.value,
            onValueChange = {
                if (it.all { it.isDigit() }) {
                    state.value = it
                }
            }
        )
    }

    @Test
    fun textField_commitTexts_state_may_not_set() {
        val focusManager = mock<FocusManager>()
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        // Always give focus to the passed node.
        whenever(focusManager.requestFocus(any())).thenAnswer {
            (it.arguments[0] as FocusManager.FocusNode).onFocus()
        }
        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        composeTestRule.setContent {
            Providers(
                FocusManagerAmbient provides focusManager,
                TextInputServiceAmbient provides textInputService
            ) {
                TestTag(tag = "textField") {
                    OnlyDigitsApp()
                }
            }
        }

        // Perform click to focus in.
        findByTag("textField")
            .doClick()

        var onEditCommandCallback: ((List<EditOperation>) -> Unit)? = null
        composeTestRule.runOnIdleCompose {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditOperation>) -> Unit>()
            verify(textInputService, times(1)).startInput(
                initModel = any(),
                keyboardType = any(),
                imeAction = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        arrayOf(
            listOf(CommitTextEditOp("1", 1)),
            listOf(CommitTextEditOp("a", 1)),
            listOf(CommitTextEditOp("2", 1)),
            listOf(CommitTextEditOp("b", 1)),
            listOf(CommitTextEditOp("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            composeTestRule.runOnIdleCompose { onEditCommandCallback!!.invoke(it) }
        }

        composeTestRule.runOnIdleCompose {
            val stateCaptor = argumentCaptor<InputState>()
            verify(textInputService, atLeastOnce())
                .onStateUpdated(eq(inputSessionToken), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123" since
            // the rejects if the incoming model contains alphabets.
            assertThat(stateCaptor.lastValue.text).isEqualTo("123")
        }
    }
}
