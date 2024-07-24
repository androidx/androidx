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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@LargeTest
class TextFieldClickToMoveCursorTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val TAG = "BasicTextField"

    private val fontSize = 10.sp

    private val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    @Test
    fun emptyTextField_clinkOnCornersAndCenter() {
        // this test is more about detecting a possible crash
        state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(center) }
            assertThat(state.selection).isEqualTo(TextRange(0))
            performTouchInput { click(Offset(left + 1f, top + 1f)) } // topLeft
            assertThat(state.selection).isEqualTo(TextRange(0))
            performTouchInput { click(Offset(right - 1f, top + 1f)) } // topRight
            assertThat(state.selection).isEqualTo(TextRange(0))
            performTouchInput { click(Offset(left + 1f, bottom - 1f)) } // bottomLeft
            assertThat(state.selection).isEqualTo(TextRange(0))
            performTouchInput { click(Offset(right - 1f, bottom - 1f)) } // bottomRight
            assertThat(state.selection).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun clickOnText_ltr() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset((fontSize * 2).toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun clickOnText_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
                )
            }
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset(right - (fontSize * 2).toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun clickOnText_ltr_in_rtlLayout() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
                )
            }
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset(right - (fontSize * 2).toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(1))
    }

    @Test
    fun clickOnText_rtl_in_ltrLayout() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset((fontSize * 2).toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(1))
    }

    @Test
    fun clickOnEmptyRegion_ltr() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset((fontSize * 4).toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun clickOnEmptyRegion_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
                )
            }
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset(fontSize.toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun clickOnTextEdge_horizontalScrollable() {
        state = TextFieldState("abcabcabcabc")
        val scrollState = ScrollState(0)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier.testTag(TAG).width(57.dp).height(12.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(right - 1f, height / 2f)) }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(6))
            assertThat(scrollState.value).isGreaterThan(0)
        }
    }

    @Test
    fun clickOnTextEdge_verticalScrollable() {
        state = TextFieldState("abc abc abc abc")
        val scrollState = ScrollState(0)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 2),
                scrollState = scrollState,
                modifier = Modifier.testTag(TAG).width(50.dp).height(17.dp)
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset(fontSize.toPx(), bottom - 1f)) }
        }
        rule.waitForIdle()
        assertThat(state.selection).isEqualTo(TextRange(5))
        assertThat(scrollState.value).isGreaterThan(0)
    }

    @Test
    fun clickOnText_readOnly() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                readOnly = true,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp)
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset((fontSize * 2).toPx(), height / 2f)) }
        }
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun textFieldInsideDialog_tapsChangeCursorPosition() {
        state = TextFieldState("abc abc abc abc")
        val show = mutableStateOf(true)
        val focusRequester = FocusRequester()
        rule.setContent {
            Dialog(
                onDismissRequest = { show.value = false },
                content = {
                    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
                    BasicTextField(
                        state = state,
                        textStyle = defaultTextStyle,
                        modifier =
                            Modifier.fillMaxWidth().testTag(TAG).focusRequester(focusRequester),
                    )
                    LaunchedEffect(isWindowFocused) {
                        if (isWindowFocused) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performTouchInput { click(Offset(2 * fontSize.toPx(), centerY)) }
        }
        rule.waitForIdle()
        assertThat(state.selection).isEqualTo(TextRange(2))
    }
}
