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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.FakeTextToolbar
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

/** Tests for long click interactions on BasicTextField. */
@LargeTest
class TextFieldLongPressTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private val TAG = "BasicTextField"

    private val fontSize = 10.sp

    private val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    @Test
    fun emptyTextField_longPressDoesNotShowCursor() {
        rule.setTextFieldTestContent {
            BasicTextField(
                state = rememberTextFieldState(),
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longClick() }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun longPress_doesNotRequestsFocus_beforePointerIsReleased() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longPress(center) }

        rule.onNodeWithTag(TAG).assertIsNotFocused()
        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsNotDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsNotDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(4, 7))
    }

    @Test
    fun longPress_requestsFocus_afterPointerIsReleased() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longPress(center) }

        rule.onNodeWithTag(TAG).assertIsNotFocused()

        rule.onNodeWithTag(TAG).performTouchInput { up() }

        rule.onNodeWithTag(TAG).assertIsFocused()
    }

    @Test
    fun longPressOnEmptyRegion_showsCursorAtTheEnd() {
        val state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun longPressOnEmptyRegion_showsTextToolbar() {
        val state = TextFieldState("abc")
        var showMenuCalled = 0
        val textToolbar =
            FakeTextToolbar(onShowMenu = { _, _, _, _, _ -> showMenuCalled++ }, onHideMenu = {})
        val clipboardManager = FakeClipboardManager("hello")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(
                LocalTextToolbar provides textToolbar,
                LocalClipboardManager provides clipboardManager
            ) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(100.dp)
                )
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.runOnIdle { assertThat(showMenuCalled).isEqualTo(1) }
    }

    @Test
    fun longPressOnWord_selectsWord() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(4, 7))
    }

    @Test
    fun longPressOnWhitespace_doesNotSelectWhitespace() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 7.5f, fontSize.toPx() / 2))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isNotEqualTo(TextRange(7, 8))
        assertThat(state.selection.collapsed).isFalse()
    }

    @Test
    fun longPressOnScrolledTextField_selectsWord() {
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
                modifier = Modifier.testTag(TAG).width(30.dp)
            )
        }

        assertThat(scrollState.maxValue).isGreaterThan(0)
        scope.launch { scrollState.scrollTo(scrollState.maxValue) }

        rule.onNodeWithTag(TAG).performTouchInput { longClick(centerRight) }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(20, 23))
    }

    @Test
    fun longPressOnDecoratedTextField_selectsWord() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG),
                decorator = { Box(modifier = Modifier.padding(32.dp)) { it() } }
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(
                Offset(
                    x = 32.dp.toPx() + fontSize.toPx() * 5f,
                    y = 32.dp.toPx() + fontSize.toPx() / 2
                )
            )
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        assertThat(state.selection).isEqualTo(TextRange(4, 7))
    }

    @Test
    fun longPress_dragToRight_selectsCurrentAndNextWord_ltr() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2))
            moveBy(Offset(fontSize.toPx() * 3f, 0f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 11))
    }

    @Test
    fun longPress_dragToLeft_selectsCurrentAndPreviousWord_ltr() {
        val state = TextFieldState("abc def ghi")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2))
            moveBy(Offset(-fontSize.toPx() * 3f, 0f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 7))
    }

    @Test
    fun longPress_dragDown_selectsFromCurrentToTargetWord_ltr() {
        val state = TextFieldState("abc def\nabc def\nabc def")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2))
            moveBy(Offset(0f, fontSize.toPx()))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 15))
    }

    @Test
    fun longPress_dragUp_selectsFromCurrentToTargetWord_ltr() {
        val state = TextFieldState("abc def\nabc def\nabc def")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() * 3 / 2)) // second line, def
            moveBy(Offset(0f, -fontSize.toPx()))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 15))
    }

    @Test
    fun longPress_startingFromEndPadding_dragToLeft_selectsLastWord_ltr() {
        val state = TextFieldState("abc def")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(centerRight)
            moveTo(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 7))
    }

    @Test
    fun longPress_startingFromEndPadding_draggingUp_selectsFromLastWord_ltr() {
        val state = TextFieldState("abc def\nghi jkl\nmno pqr")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = TextStyle(),
                modifier = Modifier.testTag(TAG).width(200.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(bottomRight)
            repeat((bottomRight - topRight).y.roundToInt()) { moveBy(Offset(0f, -1f)) }
            up()
        }

        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(4, 23)) }
    }

    // region RTL

    @Test
    fun longPress_dragToRight_selectsCurrentAndPreviousWord_rtl() {
        val state = TextFieldState(rtlText3)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2))
            moveBy(Offset(fontSize.toPx() * 3f, 0f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 7))
    }

    @Test
    fun longPress_dragToLeft_selectsCurrentAndNextWord_rtl() {
        val state = TextFieldState(rtlText3)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2))
            moveBy(Offset(-fontSize.toPx() * 3f, 0f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 11))
    }

    @Test
    fun longPress_dragDown_selectsFromCurrentToTargetWord_rtl() {
        val state = TextFieldState("$rtlText2\n$rtlText2\n$rtlText2")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() / 2))
            moveBy(Offset(0f, fontSize.toPx()))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun longPress_dragUp_selectsFromCurrentToTargetWord_rtl() {
        val state = TextFieldState("$rtlText2\n$rtlText2\n$rtlText2")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx() * 5f, fontSize.toPx() * 3 / 2))
            moveBy(Offset(0f, -fontSize.toPx()))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 11))
    }

    @Test
    fun longPress_startingFromEndPadding_dragToRight_selectsLastWord_rtl() {
        val state = TextFieldState(rtlText2)
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(100.dp)
                )
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(centerLeft)
            moveTo(Offset(fontSize.toPx() * 5, fontSize.toPx() / 2f))
            up()
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 7))
    }

    @Test
    fun longPress_startingFromEndPadding_draggingUp_selectsFromLastWord_rtl() {
        val state = TextFieldState("$rtlText2\n$rtlText2\n$rtlText2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(),
                    modifier = Modifier.testTag(TAG).width(200.dp)
                )
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(bottomLeft)
            repeat((bottomLeft - topLeft).y.roundToInt()) { moveBy(Offset(0f, -1f)) }
            up()
        }

        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(4, 23)) }
    }

    @Test
    fun longPress_startDraggingToScrollRight_startHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(center) }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx(), fontSize.toPx() / 2))
            moveBy(Offset(fontSize.toPx() * 30, 0f))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }

    @Test
    fun longPress_startDraggingToScrollRightThenSlightlyBack_startHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(center) }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx(), fontSize.toPx() / 2))
            moveBy(Offset(fontSize.toPx() * 30, 0f))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()

        // slightly back a little bit so that selection seems to be collapsing but the acting
        // handle should remain the same
        rule.onNodeWithTag(TAG).performTouchInput {
            moveBy(Offset(-fontSize.toPx(), 0f))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }

    @Test
    fun longPress_startDraggingToScrollDown_startHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.MultiLine(1, 3),
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(center) }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx(), fontSize.toPx() / 2))
            moveBy(Offset(0f, fontSize.toPx() * 30))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }

    @Test
    fun longPress_startDraggingToScrollDownThenSlightlyBack_startHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.MultiLine(1, 3),
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { click(center) }

        rule.onNodeWithTag(TAG).performTouchInput {
            longPress(Offset(fontSize.toPx(), fontSize.toPx() / 2))
            moveBy(Offset(0f, fontSize.toPx() * 30))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()

        // slightly back a little bit so that selection seems to be collapsing but the acting
        // handle should remain the same
        rule.onNodeWithTag(TAG).performTouchInput {
            moveBy(Offset(0f, -fontSize.toPx()))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }

    @Test
    fun longPress_startDraggingToScrollLeft_endHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            // swipe to the absolute right by specifying a huge swipe delta
            swipeLeft(endX = -10000f)
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            longPress(Offset(right - 1f, centerY))
            moveBy(Offset(-fontSize.toPx() * 30, 0f))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
    }

    @Test
    fun longPress_startDraggingToScrollLeftThenSlightlyForward_endHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            // swipe to the absolute right by specifying a huge swipe delta
            swipeLeft(endX = -10000f)
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            longPress(Offset(right - 1f, centerY))
            moveBy(Offset(-fontSize.toPx() * 30, 0f))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()

        // slightly back a little bit so that selection seems to be collapsing but the acting
        // handle should remain the same
        rule.onNodeWithTag(TAG).performTouchInput {
            moveBy(Offset(fontSize.toPx(), 0f))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
    }

    @Test
    fun longPress_startDraggingToScrollUp_endHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.MultiLine(1, 3),
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            // swipe to the absolute bottom by specifying a huge swipe delta
            swipeUp(endY = -10000f)
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            longPress(Offset(centerX, bottom - 1f))
            moveBy(Offset(0f, -fontSize.toPx() * 30))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
    }

    @Test
    fun longPress_startDraggingToScrollUpThenSlightlyForward_endHandleDoesNotShow_ltr() {
        val state = TextFieldState("abc def ghi ".repeat(10))
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                lineLimits = TextFieldLineLimits.MultiLine(1, 3),
                modifier = Modifier.testTag(TAG).width(100.dp)
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            // swipe to the absolute bottom by specifying a huge swipe delta
            swipeUp(endY = -10000f)
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis * 2)
            longPress(Offset(centerX, bottom - 1f))
            moveBy(Offset(0f, -fontSize.toPx() * 30))
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()

        // slightly back a little bit so that selection seems to be collapsing but the acting
        // handle should remain the same
        rule.onNodeWithTag(TAG).performTouchInput {
            moveBy(Offset(0f, fontSize.toPx()))
            up()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
    }

    // endregion

    companion object {
        private const val rtlText2 = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5"
        private const val rtlText3 = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5 \u05D6\u05D7\u05D8"
    }
}
