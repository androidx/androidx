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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.assertHandlePositionMatches
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
class TextFieldCursorHandleTest : FocusedWindowTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val TAG = "BasicTextField2"

    private val fontSize = 10.sp

    private val fontSizePx = with(rule.density) { fontSize.toPx() }

    private val fontSizeDp = with(rule.density) { fontSize.toDp() }

    private val cursorWidth = DefaultCursorThickness

    @Test
    fun cursorHandle_showsAtCorrectLocation_ltr() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 2, fontSize.toPx() / 2))
        }

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            (2 * fontSize.value).dp + cursorWidth / 2,
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_hasMinimumTouchSizeArea() = with(rule.density) {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.width(100.dp).testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click() }

        var actualBottomRight = Offset.Zero
        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            actualBottomRight = bottomRight
        }

        val expectedBottomRight = Offset(40.dp.toPx(), 40.dp.toPx())
        assertThat(actualBottomRight.x).isWithin(1f).of(expectedBottomRight.x)
        assertThat(actualBottomRight.y).isWithin(1f).of(expectedBottomRight.y)
    }

    @Test
    fun tapTextField_cursorHandleFiltered() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                inputTransformation = { _, valueWithChanges ->
                    valueWithChanges.selectCharsIn(TextRange(4))
                },
                modifier = Modifier.testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 2, fontSize.toPx() / 2))
        }

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(4))
    }

    @Test
    fun cursorHandle_showsAtCorrectLocation_outOfTextBoundsTouch_ltr() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 8, fontSize.toPx() / 2))
        }

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            5 * fontSizeDp + cursorWidth / 2,
            fontSizeDp
        )
    }

    @Test
    fun cursorHandle_showsAtCorrectLocation_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2\u05D3\u05D4")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSize.value.dp * 5)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 2, fontSize.toPx() / 2))
        }

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(3))

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            (2 * fontSize.value).dp + cursorWidth / 2,
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_showsAtCorrectLocation_outOfTextBoundsTouch_rtl() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            click(Offset(fontSize.toPx() * 8, fontSize.toPx() / 2))
        }

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            (5 * fontSize.value).dp + cursorWidth / 2,
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_notVisibleOnEmptyField() {
        state = TextFieldState()
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_doesNotShow_whenTextFieldIsReadOnly() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG),
                readOnly = true
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_disappears_whenTextIsEdited() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        rule.onNodeWithTag(TAG).performTextInput("m")
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_disappears_whenTextStateChanges() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        state.setTextAndPlaceCursorAtEnd("hello2")
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_doesNotDisappear_whenSelectionChanges() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        state.edit { placeCursorBeforeCharAt(2) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            (2 * fontSize.value).dp + cursorWidth / 2,
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_disappears_whenWindowLosesFocus() {
        state = TextFieldState("hello")
        val focusWindow = mutableStateOf(true)
        val windowInfo = object : WindowInfo {
            override val isWindowFocused: Boolean
                get() = focusWindow.value
        }
        rule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        focusWindow.value = false
        rule.waitForIdle()

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_coercesAtBoundaries_ltr() {
        state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    // Make this TextField guaranteed to be wider than the text content
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick() // cursor handle appears
        rule.runOnIdle {
            state.edit { selectCharsIn(TextRange(0)) } // move cursor to the start of text
        }

        val characterSize = fontSizeDp // width and height is the same.
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            cursorWidth / 2,
            characterSize
        )

        rule.runOnIdle {
            state.edit { selectCharsIn(TextRange(5)) } // move cursor to the end of text
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            // Move 5 characters to right (5 * character), finally account for the center of
            // cursor (cursorWidth / 2).
            5 * characterSize + cursorWidth / 2,
            fontSizeDp
        )
    }

    @Test
    fun cursorHandle_coercesAtBoundaries_rtl() = with(rule.density) {
        state = TextFieldState("\u05D0\u05D1\u05D2\u05D3\u05D4")
        var width = 0
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        // Make this TextField guaranteed to be wider than the text content
                        .width(fontSizeDp * 10)
                        .onSizeChanged { width = it.width }
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick() // cursor handle appears

        rule.runOnIdle {
            state.edit { selectCharsIn(TextRange(0)) } // move cursor to the start of text
        }

        val characterSize = fontSizeDp // width and height is the same.
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            width.toDp() - cursorWidth / 2, // Should align to the right
            characterSize
        )

        rule.runOnIdle {
            state.edit { selectCharsIn(TextRange(5)) } // move cursor to the end of text
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            // Start from right (width), move 5 characters to left (5 * character), finally account
            // for the center of cursor (cursorWidth / 2).
            width.toDp() - 5 * characterSize - cursorWidth / 2,
            fontSizeDp
        )
    }

    @Test
    fun cursorHandle_disappearsOnVerticalScroll() {
        state = TextFieldState("hello hello hello hello", initialSelectionInChars = TextRange.Zero)
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setTextFieldTestContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 1),
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 5)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        scope.runBlockingOnIdle {
            scrollState.scrollTo(scrollState.maxValue)
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_disappearsOnHorizontalScroll() {
        state = TextFieldState("hello hello hello hello", initialSelectionInChars = TextRange.Zero)
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setTextFieldTestContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        scope.runBlockingOnIdle {
            scrollState.scrollTo(scrollState.maxValue)
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()
    }

    @Test
    fun cursorHandle_reappearsOnVerticalScroll() {
        state = TextFieldState("hello hello hello hello", initialSelectionInChars = TextRange.Zero)
        val scrollState = ScrollState(0)
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 1),
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 5)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        rule.onNodeWithTag(TAG).performTouchInput {
            swipeUp(endY = -bottom)
        }
        rule.waitForIdle()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()

        rule.onNodeWithTag(TAG).performTouchInput {
            swipeDown(endY = 2 * bottom)
        }
        rule.waitForIdle()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
    }

    @Test
    fun cursorHandle_reappearsOnHorizontalScroll() {
        state = TextFieldState("hello hello hello hello", initialSelectionInChars = TextRange.Zero)
        val scrollState = ScrollState(0)
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 5)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        rule.onNodeWithTag(TAG).performTouchInput { swipeLeft(endX = -right) }
        rule.waitForIdle()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertDoesNotExist()

        rule.onNodeWithTag(TAG).performTouchInput { swipeRight(endX = 2 * right) }
        rule.waitForIdle()
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
    }

    @Test
    fun cursorHandleDrag_getsFiltered() {
        state = TextFieldState("abc abc")
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                inputTransformation = { _, valueWithChanges ->
                    valueWithChanges.selectCharsIn(TextRange.Zero)
                },
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) } // click most left
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(fontSizePx * 5)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange.Zero)
    }

    // region ltr drag tests
    @Test
    fun moveCursorHandleToRight_ltr() {
        state = TextFieldState("abc", initialSelectionInChars = TextRange.Zero)
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) } // click most left
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(fontSizePx)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
    }

    @Test
    fun moveCursorHandleToLeft_ltr() {
        state = TextFieldState("abc", initialSelectionInChars = TextRange.Zero)
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight - Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(fontSizePx)

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
        }
    }

    @Test
    fun moveCursorHandleToRight_ltr_outOfBounds() {
        state = TextFieldState("abc", initialSelectionInChars = TextRange.Zero)
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 5)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(getTextFieldWidth() * 2)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(3))
    }

    @Test
    fun moveCursorHandleToLeft_ltr_outOfBounds() {
        state = TextFieldState("abc", initialSelectionInChars = TextRange(3))
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 5)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight - Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(getTextFieldWidth() * 2)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange.Zero)
    }

    @Test
    fun moveCursorHandleToRight_ltr_outOfBounds_scrollable_continuesDrag() {
        state = TextFieldState(
            initialText = "abcd abcd abcd abcd abcd",
            initialSelectionInChars = TextRange.Zero
        )
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(getTextFieldWidth() * 3)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(state.text.length))
    }

    @Test
    fun moveCursorHandleToRight_ltr_outOfBounds_scrollable() {
        state = TextFieldState(
            initialText = "abcd abcd abcd abcd abcd",
            initialSelectionInChars = TextRange.Zero
        )
        rule.setTextFieldTestContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSizeDp * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(fontSizePx * 12, durationMillis = 1)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(12))
        }

        swipeToRight(fontSizePx * 2, durationMillis = 1)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(14))
        }
    }

    // endregion

    // region rtl drag tests
    @Test
    fun moveCursorHandleToRight_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSizeDp * 10)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) } // click most left
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(fontSizePx)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
    }

    @Test
    fun moveCursorHandleToLeft_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSizeDp * 10)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight - Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(fontSizePx)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
    }

    @Test
    fun moveCursorHandleToRight_rtl_outOfBounds() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSizeDp * 5)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(getTextFieldWidth() * 2)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange.Zero)
    }

    @Test
    fun moveCursorHandleToLeft_rtl_outOfBounds() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSizeDp * 5)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight - Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(getTextFieldWidth() * 2)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(state.text.length))
    }

    @Test
    fun moveCursorHandleToLeft_rtl_outOfBounds_scrollable_continuesDrag() {
        state = TextFieldState(
            "\u05D0\u05D1\u05D2\u05D3 " +
                "\u05D0\u05D1\u05D2\u05D3 " +
                "\u05D0\u05D1\u05D2\u05D3 " +
                "\u05D0\u05D1\u05D2\u05D3"
        )
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSizeDp * 10)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight - Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(getTextFieldWidth() * 3)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(state.text.length))
    }

    @Test
    fun moveCursorHandleToLeft_rtl_outOfBounds_scrollable() {
        val scrollState = ScrollState(0)
        state = TextFieldState(
            initialText = "\u05D0\u05D1\u05D2\u05D3 " +
                "\u05D0\u05D1\u05D2\u05D3 " +
                "\u05D0\u05D1\u05D2\u05D3 " +
                "\u05D0\u05D1\u05D2\u05D3",
            initialSelectionInChars = TextRange.Zero
        )
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    scrollState = scrollState,
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSizeDp * 10f)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(fontSizePx * 12, durationMillis = 1)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(12))
        }

        swipeToLeft(fontSizePx * 2, durationMillis = 1)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(14))
        }
    }

    // endregion

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).requestFocus()
    }

    private fun swipeToLeft(swipeDistance: Float, durationMillis: Long = 1000) =
        performHandleDrag(Handle.Cursor, true, swipeDistance, durationMillis)

    private fun swipeToRight(swipeDistance: Float, durationMillis: Long = 1000) =
        performHandleDrag(Handle.Cursor, false, swipeDistance, durationMillis)

    private fun performHandleDrag(
        handle: Handle,
        toLeft: Boolean,
        swipeDistance: Float = 1f,
        durationMillis: Long = 1000
    ) {
        val handleNode = rule.onNode(isSelectionHandle(handle))

        handleNode.performTouchInput {
            if (toLeft) {
                swipeLeft(
                    startX = centerX,
                    endX = centerX - viewConfiguration.touchSlop - swipeDistance,
                    durationMillis = durationMillis
                )
            } else {
                swipeRight(
                    startX = centerX,
                    endX = centerX + viewConfiguration.touchSlop + swipeDistance,
                    durationMillis = durationMillis
                )
            }
        }
    }

    private fun getTextFieldWidth() = rule.onNodeWithTag(TAG)
        .fetchSemanticsNode()
        .boundsInRoot.width

    private fun CoroutineScope.runBlockingOnIdle(block: suspend CoroutineScope.() -> Unit) {
        val job = rule.runOnIdle {
            launch(block = block)
        }
        runBlocking { job.join() }
    }
}
