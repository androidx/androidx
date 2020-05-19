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

import android.os.Build
import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.Providers
import androidx.compose.state
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.TestTag
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.onPositioned
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState
import androidx.ui.core.focus.focusState
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.input.CommitTextEditOp
import androidx.ui.input.EditOperation
import androidx.ui.input.EditorValue
import androidx.ui.input.ImeAction
import androidx.ui.input.TextInputService
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.assert
import androidx.ui.test.assertPixels
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.hasImeAction
import androidx.ui.test.hasInputMethodsSupport
import androidx.ui.test.runOnIdleCompose
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class TextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val DefaultTextFieldWidth = 280.dp

    @Test
    fun textField_focusInSemantics() {
        val inputService = mock<TextInputService>()

        lateinit var focusModifier: FocusModifier
        composeTestRule.setContent {
            val state = state { TextFieldValue("") }
            Providers(
                TextInputServiceAmbient provides inputService
            ) {
                focusModifier = FocusModifier()
                TestTag(tag = "textField") {
                    TextField(
                        value = state.value,
                        modifier = Modifier.fillMaxSize() + focusModifier,
                        onValueChange = { state.value = it }
                    )
                }
            }
        }

        findByTag("textField").doClick()

        runOnIdleCompose {
            assertThat(focusModifier.focusState).isEqualTo(FocusState.Focused)
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

    @Test
    fun textField_commitTexts() {
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        composeTestRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                TestTag(tag = "textField") {
                    TextFieldApp()
                }
            }
        }

        findByTag("textField").doClick()

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
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        composeTestRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                TestTag(tag = "textField") {
                    OnlyDigitsApp()
                }
            }
        }

        findByTag("textField").doClick()

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
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        val onTextLayout: (TextLayoutResult) -> Unit = mock()
        composeTestRule.setContent {
            Providers(
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

        findByTag("textField").doClick()

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
    fun textField_hasDefaultWidth() {
        var size: IntPx? = null
        composeTestRule.setContent {
            Box {
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
            assertThat(size).isEqualTo(DefaultTextFieldWidth.toIntPx())
        }
    }

    @Test
    fun textField_respectsWidthSetByModifier() {
        val textFieldWidth = 100.dp
        var size: IntPx? = null
        composeTestRule.setContent {
            Box {
                TextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    modifier = Modifier
                        .preferredWidth(textFieldWidth)
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

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldNotFocused_cursorNotRendered() {
        composeTestRule.setContent {
            TestTag("textField") {
                TextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    textColor = Color.White,
                    modifier = Modifier.preferredSize(10.dp, 20.dp).drawBackground(Color.White),
                    cursorColor = Color.Blue
                )
            }
        }

        findByTag("textField")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() = with(composeTestRule.density) {
        val width = 10.dp
        val height = 20.dp
        val halfCursorWidth = 2.dp.toIntPx() / 2f
        val latch = CountDownLatch(1)
        composeTestRule.setContent {
            TestTag("textField") {
                TextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    textStyle = TextStyle(color = Color.White, background = Color.White),
                    modifier = Modifier.preferredSize(width, height).drawBackground(Color.White),
                    cursorColor = Color.Red,
                    onFocusChange = { focused ->
                        if (focused) latch.countDown()
                    }
                )
            }
        }
        findByTag("textField").doClick()
        assert(latch.await(1, TimeUnit.SECONDS))

        findByTag("textField")
            .captureToBitmap()
            .assertPixels(
                IntPxSize(width.toIntPx(), height.toIntPx())
            ) { position ->
                if (position.x >= halfCursorWidth - 1.ipx && position.x < halfCursorWidth + 1.ipx) {
                    // skip some pixels around cursor
                    null
                } else if (position.y < 5.ipx || position.y > height.toIntPx() - 5.ipx) {
                    // skip some pixels vertically
                    null
                } else if (position.x in 0.ipx..halfCursorWidth) {
                    // cursor
                    Color.Red
                } else {
                    // text field background
                    Color.White
                }
            }
    }

    @Test
    fun defaultSemantics() {
        composeTestRule.setContent {
            TestTag("textField") {
                TextField(
                    value = TextFieldValue(),
                    onValueChange = {}
                )
            }
        }

        findByTag("textField")
            .assert(hasInputMethodsSupport())
            .assert(hasImeAction(ImeAction.Unspecified))
    }

    @Test
    fun setImeAction_isReflectedInSemantics() {
        composeTestRule.setContent {
            TestTag("textField") {
                TextField(
                    value = TextFieldValue(),
                    imeAction = ImeAction.Search,
                    onValueChange = {}
                )
            }
        }

        findByTag("textField")
            .assert(hasImeAction(ImeAction.Search))
    }
}
