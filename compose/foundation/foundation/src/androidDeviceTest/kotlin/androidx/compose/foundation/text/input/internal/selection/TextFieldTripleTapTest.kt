/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.PlatformSelectionBehaviorsRule
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.gestures.util.tripleTap
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for long click interactions on BasicTextField. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldTripleTapTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())
    @get:Rule val platformSelectionBehaviorsRule = PlatformSelectionBehaviorsRule()

    private val TAG = "BasicTextField"

    private val fontSize = 10.sp

    private val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    @Test
    fun emptyTextField_tripleTapDoesNotShowCursor() {
        rule.setTextFieldTestContent {
            BasicTextField(
                state = rememberTextFieldState(),
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { tripleTap() }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun tripleTap_firstPlacesTheCursor_thenSelectsWord_thenSelectsParagraph() {
        val state = TextFieldState("abc def ghi\nabc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(5))

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(4, 7))

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun tripleTap_emptySpace_selectsParagraph() {
        val state = TextFieldState("abc  def")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            tripleTap(Offset(fontSize.toPx() * 4, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(0, 8))
    }

    @Test
    fun tripleTap_scrolledTextField_selectsParagraph() {
        val state = TextFieldState("abc def ghi abc def ghi")
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setTextFieldTestContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                scrollState = scrollState,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(30.dp),
            )
        }

        assertThat(scrollState.maxValue).isGreaterThan(0)
        scope.launch { scrollState.scrollTo(scrollState.maxValue) }

        rule.onNodeWithTag(TAG).performTouchInput { tripleTap(centerRight) }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        // expected since the entire selection cannot fit
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsNotDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(0, 23))
    }

    @Test
    fun tripleTap_decoratedTextField_selectsParagraph() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
                decorator = { Box(modifier = Modifier.padding(32.dp)) { it() } },
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            tripleTap(
                Offset(
                    x = 32.dp.toPx() + fontSize.toPx() * 5f,
                    y = 32.dp.toPx() + fontSize.toPx() / 2,
                )
            )
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun tripleTapThen_dragDown_selectsFromCurrentToTargetParagraph_ltr() {
        val state = TextFieldState("abc def\nabc def\nabc def")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            tripleTap(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2), liftPointer = false)
            moveBy(Offset(0f, fontSize.toPx() * 3 / 2))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 15))
    }

    @Test
    fun tripleTapThen_dragUp_selectsFromCurrentToTargetWord_ltr() {
        val state = TextFieldState("abc def\nabc def\nabc def")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            tripleTap(
                Offset(fontSize.toPx() * 5f, fontSize.toPx() * 3 / 2),
                liftPointer = false,
            ) // second line, def
            moveBy(Offset(0f, -fontSize.toPx() * 2f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 15))
    }

    companion object {
        private const val rtlText2 = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5"
        private const val rtlText3 = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5 \u05D6\u05D7\u05D8"
    }
}
