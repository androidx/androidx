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

package androidx.compose.foundation.text2.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.assertHandlePositionMatches
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
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
class TextFieldCursorHandleTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val TAG = "BasicTextField2"

    private val fontSize = 10.sp

    private val fontSizePx = with(rule.density) { fontSize.toPx() }

    @Test
    fun cursorHandle_showsAtCorrectLocation_ltr() = with(rule.density) {
        state = TextFieldState("hello")
        rule.setContent {
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
            (2 * fontSize.value + 1).dp, // cursorWidth / 2
            fontSize.value.dp
        )
    }

    @Test
    fun tapTextField_cursorHandleFiltered() = with(rule.density) {
        state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                filter = { _, valueWithChanges ->
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
    fun cursorHandle_showsAtCorrectLocation_outOfTextBoundsTouch_ltr() = with(rule.density) {
        state = TextFieldState("hello")
        rule.setContent {
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

        with(rule.density) {
            rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
                5 * fontSize.toDp() + 1.dp, // cursorWidth / 2
                fontSize.toDp()
            )
        }
    }

    @Test
    fun cursorHandle_showsAtCorrectLocation_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2\u05D3\u05D4")
        rule.setContent {
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
            (2 * fontSize.value + 1).dp, // cursorWidth / 2
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_showsAtCorrectLocation_outOfTextBoundsTouch_rtl() = with(rule.density) {
        state = TextFieldState("hello")
        rule.setContent {
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
            (5 * fontSize.value + 1).dp, // cursorWidth / 2
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_notVisibleOnEmptyField() {
        state = TextFieldState()
        rule.setContent {
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
        rule.setContent {
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
        rule.setContent {
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
        rule.setContent {
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
        rule.setContent {
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
            (2 * fontSize.value + 1).dp, // cursorWidth / 2
            fontSize.value.dp
        )
    }

    @Test
    fun cursorHandle_coercesAtBoundaries_ltr() = with(rule.density) {
        state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSize.toDp() * 5)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick() // cursor handle appears
        state.edit { selectCharsIn(TextRange(0)) } // move cursor to the start

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            1.dp, // cursorWidth / 2
            fontSize.toDp()
        )

        state.edit { selectCharsIn(TextRange(5)) } // move cursor to the end

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            5 * fontSize.toDp() - 1.dp, // cursorWidth / 2
            fontSize.toDp()
        )
    }

    @Test
    fun cursorHandle_coercesAtBoundaries_rtl() = with(rule.density) {
        state = TextFieldState("\u05D0\u05D1\u05D2\u05D3\u05D4")
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(fontSize.toDp() * 5)
                )
            }
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performClick() // cursor handle appears
        state.edit { selectCharsIn(TextRange(0)) } // move cursor to the start

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            5 * fontSize.toDp() - 1.dp, // cursorWidth / 2
            fontSize.toDp()
        )

        state.edit { selectCharsIn(TextRange(5)) } // move cursor to the end

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertHandlePositionMatches(
            1.dp, // cursorWidth / 2
            fontSize.toDp()
        )
    }

    @Test
    fun cursorHandle_disappearsOnVerticalScroll() {
        state = TextFieldState("hello hello hello hello")
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 1),
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 5)
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
    fun cursorHandle_disappearsOnHorizontalScroll() = with(rule.density) {
        state = TextFieldState("hello hello hello hello")
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(fontSize.toDp() * 10)
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
        state = TextFieldState("hello hello hello hello")
        val scrollState = ScrollState(0)
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 1),
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 5)
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
        state = TextFieldState("hello hello hello hello")
        val scrollState = ScrollState(0)
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                // scrollable but still only show maximum one line in its viewport
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 5)
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
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                filter = { _, valueWithChanges ->
                    valueWithChanges.selectCharsIn(TextRange.Zero)
                },
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 10)
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
        state = TextFieldState("abc")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 10)
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
        state = TextFieldState("abc")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(topRight - Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToLeft(fontSizePx)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
    }

    @Test
    fun moveCursorHandleToRight_ltr_outOfBounds() {
        state = TextFieldState("abc")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 5)
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
        state = TextFieldState("abc")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 5)
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
        state = TextFieldState("abcd abcd abcd abcd abcd")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .testTag(TAG)
                    .width(with(rule.density) { fontSize.toDp() } * 10)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput { click(Offset(1f, 1f)) }
        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

        swipeToRight(getTextFieldWidth() * 3)
        rule.waitForIdle()

        assertThat(state.text.selectionInChars).isEqualTo(TextRange(state.text.length))
    }

    // endregion

    // region rtl drag tests
    @Test
    fun moveCursorHandleToRight_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(with(rule.density) { fontSize.toDp() } * 10)
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
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(with(rule.density) { fontSize.toDp() } * 10)
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
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(with(rule.density) { fontSize.toDp() } * 5)
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
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(with(rule.density) { fontSize.toDp() } * 5)
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
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier
                        .testTag(TAG)
                        .width(with(rule.density) { fontSize.toDp() } * 10)
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

    // endregion

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).requestFocus()
    }

    private fun swipeToLeft(swipeDistance: Float) =
        performHandleDrag(Handle.Cursor, true, swipeDistance)

    private fun swipeToRight(swipeDistance: Float) =
        performHandleDrag(Handle.Cursor, false, swipeDistance)

    private fun performHandleDrag(handle: Handle, toLeft: Boolean, swipeDistance: Float = 1f) {
        val handleNode = rule.onNode(isSelectionHandle(handle))

        handleNode.performTouchInput {
            if (toLeft) {
                swipeLeft(
                    startX = centerX,
                    endX = centerX - viewConfiguration.touchSlop - swipeDistance,
                    durationMillis = 1000
                )
            } else {
                swipeRight(
                    startX = centerX,
                    endX = centerX + viewConfiguration.touchSlop + swipeDistance,
                    durationMillis = 1000
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