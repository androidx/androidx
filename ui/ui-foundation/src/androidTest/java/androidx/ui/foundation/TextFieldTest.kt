/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.Providers
import androidx.compose.state
import androidx.test.filters.SmallTest
import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.TestTag
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.input.FocusManager
import androidx.ui.core.input.FocusNode
import androidx.ui.core.input.FocusTransitionObserver
import androidx.ui.core.onPositioned
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.EditorValue
import androidx.ui.input.TextInputService
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidthIn
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
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
            val state = state { TextFieldValue("") }
            Providers(
                FocusManagerAmbient provides focusManager,
                TextInputServiceAmbient provides inputService
            ) {
                TestTag(tag = "textField") {
                    TextField(
                        value = state.value,
                        modifier = Modifier.fillMaxSize(),
                        onValueChange = { state.value = it }
                    )
                }
            }
        }

        findByTag("textField").doClick()

        runOnIdleCompose {
            verify(focusManager, times(1)).requestFocus(any())
        }
    }

    @Composable
    private fun TextFieldApp() {
        val state = state { TextFieldValue("") }
        TextField(
            value = state.value,
            modifier = Modifier.fillMaxSize(),
            onValueChange = {
                state.value = it
            }
        )
    }

    /**
     * Fake class for giving input focus when requestFocus is called.
     */
    class FakeFocusManager : FocusManager {
        var observer: FocusTransitionObserver? = null

        override fun registerObserver(node: FocusNode, observer: FocusTransitionObserver) {
            this.observer = observer
        }

        override fun requestFocus(client: FocusNode) {
            observer?.invoke(null, client)
        }

        override fun blur(client: FocusNode) {}
    }

    @Test
    fun textField_commitTexts() {
        val focusManager = FakeFocusManager()
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

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
        runOnIdleCompose {
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
            runOnIdleCompose { onEditCommandCallback!!.invoke(it) }
        }

        runOnIdleCompose {
            val stateCaptor = argumentCaptor<EditorValue>()
            verify(textInputService, atLeastOnce())
                .onStateUpdated(eq(inputSessionToken), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "1a2b3".
            assertThat(stateCaptor.lastValue.text).isEqualTo("1a2b3")
        }
    }

    @Composable
    private fun OnlyDigitsApp() {
        val state = state { TextFieldValue("") }
        TextField(
            value = state.value,
            modifier = Modifier.fillMaxSize(),
            onValueChange = {
                if (it.text.all { it.isDigit() }) {
                    state.value = it
                }
            }
        )
    }

    @Test
    fun textField_commitTexts_state_may_not_set() {
        val focusManager = FakeFocusManager()
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

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
        runOnIdleCompose {
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
            runOnIdleCompose { onEditCommandCallback!!.invoke(it) }
        }

        runOnIdleCompose {
            val stateCaptor = argumentCaptor<EditorValue>()
            verify(textInputService, atLeastOnce())
                .onStateUpdated(eq(inputSessionToken), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123" since
            // the rejects if the incoming model contains alphabets.
            assertThat(stateCaptor.lastValue.text).isEqualTo("123")
        }
    }

    @Test
    fun textField_onTextLayoutCallback() {
        val focusManager = FakeFocusManager()
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        val onTextLayout: (TextLayoutResult) -> Unit = mock()
        composeTestRule.setContent {
            Providers(
                FocusManagerAmbient provides focusManager,
                TextInputServiceAmbient provides textInputService
            ) {
                TestTag(tag = "textField") {
                    val state = state { TextFieldValue("") }
                    TextField(
                        value = state.value,
                        modifier = Modifier.fillMaxSize(),
                        onValueChange = {
                            state.value = it
                        },
                        onTextLayout = onTextLayout
                    )
                }
            }
        }

        // Perform click to focus in.
        findByTag("textField")
            .doClick()

        var onEditCommandCallback: ((List<EditOperation>) -> Unit)? = null
        runOnIdleCompose {
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

        // Performs input events "1", "2", "3".
        arrayOf(
            listOf(CommitTextEditOp("1", 1)),
            listOf(CommitTextEditOp("2", 1)),
            listOf(CommitTextEditOp("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            runOnIdleCompose { onEditCommandCallback!!.invoke(it) }
        }

        runOnIdleCompose {
            val layoutCaptor = argumentCaptor<TextLayoutResult>()
            verify(onTextLayout, atLeastOnce()).invoke(layoutCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123"
            assertThat(layoutCaptor.lastValue.layoutInput.text.text).isEqualTo("123")
        }
    }

    @Test
    fun textField_occupiesAllAvailableSpace() {
        val parentSize = 300.dp
        var size: IntPx? = null
        composeTestRule.setContent {
            Box(Modifier.preferredSize(parentSize)) {
                TextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    modifier = Modifier.onPositioned {
                        size = it.size.width
                    }
                )
            }
        }

        with(composeTestRule.density) {
            assertThat(size).isEqualTo(parentSize.toIntPx())
        }
    }

    @Test
    fun textField_respectsMaxWidthSetByModifier() {
        val parentSize = 300.dp
        val textFieldWidth = 100.dp
        var size: IntPx? = null
        composeTestRule.setContent {
            Box(Modifier.preferredSize(parentSize)) {
                TextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    modifier = Modifier
                        .preferredWidthIn(maxWidth = textFieldWidth)
                        .onPositioned {
                            size = it.size.width
                        }
                )
            }
        }

        with(composeTestRule.density) {
            assertThat(size).isEqualTo(textFieldWidth.toIntPx())
        }
    }

    @Test
    fun textFieldInRow_fixedElementIsVisible() {
        val parentSize = 300.dp
        val boxSize = 50.dp
        var size: IntPx? = null
        composeTestRule.setContent {
            Box(Modifier.preferredSize(parentSize)) {
                Row {
                    TextField(
                        value = TextFieldValue(),
                        onValueChange = {},
                        modifier = Modifier
                            .weight(1f)
                            .onPositioned {
                                size = it.size.width
                            }
                    )
                    Box(Modifier.preferredSize(boxSize))
                }
            }
        }

        with(composeTestRule.density) {
            assertThat(size).isEqualTo(parentSize.toIntPx() - boxSize.toIntPx())
        }
    }

    @Test
    fun textFieldValue_saverRestoresState() {
        var state: MutableState<TextFieldValue>? = null

        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            state = savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }
        }

        runOnIdleCompose {
            state!!.value = TextFieldValue("test", TextRange(1, 2))

            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        runOnIdleCompose {
            assertThat(state!!.value).isEqualTo(TextFieldValue("test", TextRange(1, 2)))
        }
    }
}
