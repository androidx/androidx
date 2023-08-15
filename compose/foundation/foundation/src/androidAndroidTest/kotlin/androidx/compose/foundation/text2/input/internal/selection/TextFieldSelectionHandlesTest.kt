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

package androidx.compose.foundation.text2.input.internal.selection

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.SelectionHandleAnchor
import androidx.compose.foundation.text.selection.assertHandleAnchorMatches
import androidx.compose.foundation.text.selection.assertHandlePositionMatches
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
class TextFieldSelectionHandlesTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val TAG = "BasicTextField2"

    private val fontSize = 10.sp
    private val fontSizePx = with(rule.density) { fontSize.toPx() }

    @Test
    fun selectionHandles_doNotShow_whenFieldNotFocused() {
        state = TextFieldState("hello, world", initialSelectionInChars = TextRange(2, 5))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        assertHandlesNotExist()
    }

    @Test
    fun selectionHandles_appears_whenFieldGetsFocused() {
        state = TextFieldState("hello, world", initialSelectionInChars = TextRange(2, 5))
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
        assertHandlesDisplayed()
    }

    @Test
    fun selectionHandles_disappear_whenFieldLosesFocus() {
        state = TextFieldState("hello, world", initialSelectionInChars = TextRange(2, 5))
        val focusRequester = FocusRequester()
        rule.setContent {
            Column {
                Box(
                    Modifier
                        .size(100.dp)
                        .focusRequester(focusRequester)
                        .focusable())
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(100.dp)
                )
            }
        }

        focusAndWait()
        assertHandlesDisplayed()
        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        assertHandlesNotExist()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectionHandles_locatedAtTheRightPosition_ltr_ltr() {
        state = TextFieldState("hello, world", initialSelectionInChars = TextRange(2, 5))
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

        with(rule.onNode(isSelectionHandle(Handle.SelectionStart))) {
            assertHandlePositionMatches(
                (2 * fontSize.value).dp,
                fontSize.value.dp
            )
            assertHandleAnchorMatches(SelectionHandleAnchor.Left)
        }

        with(rule.onNode(isSelectionHandle(Handle.SelectionEnd))) {
            assertHandlePositionMatches(
                (5 * fontSize.value).dp,
                fontSize.value.dp
            )
            assertHandleAnchorMatches(SelectionHandleAnchor.Right)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectionHandles_locatedAtTheRightPosition_ltr_rtl() {
        state = TextFieldState("abc \u05D0\u05D1\u05D2", initialSelectionInChars = TextRange(1, 6))
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

        with(rule.onNode(isSelectionHandle(Handle.SelectionStart))) {
            assertHandlePositionMatches(
                (1 * fontSize.value).dp,
                fontSize.value.dp
            )
            assertHandleAnchorMatches(SelectionHandleAnchor.Left)
        }

        with(rule.onNode(isSelectionHandle(Handle.SelectionEnd))) {
            assertHandlePositionMatches(
                (5 * fontSize.value).dp,
                fontSize.value.dp
            )
            assertHandleAnchorMatches(SelectionHandleAnchor.Left)
        }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_horizontally() {
        // make it scrollable
        state = TextFieldState("hello ".repeat(10), initialSelectionInChars = TextRange(1, 2))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(TAG).performTouchInput { swipeLeft() }
        assertHandlesNotExist()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }

        rule.onNodeWithTag(TAG).performTouchInput { swipeRight() }
        assertHandlesDisplayed()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_vertically() {
        // make it scrollable
        state = TextFieldState("hello ".repeat(10), initialSelectionInChars = TextRange(1, 2))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 2),
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(TAG).performTouchInput {
            swipeUp()
        }
        assertHandlesNotExist()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            swipeDown()
        }
        assertHandlesDisplayed()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_horizontally_inContainer() {
        // make it scrollable
        val containerTag = "container"
        state = TextFieldState("hello", initialSelectionInChars = TextRange(1, 2))
        rule.setContent {
            Row(modifier = Modifier
                .width(200.dp)
                .horizontalScroll(rememberScrollState())
                .testTag(containerTag)
            ) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .width(100.dp)
                )
                Box(modifier = Modifier
                    .height(12.dp)
                    .width(400.dp))
            }
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(containerTag).performTouchInput {
            swipeLeft()
        }
        assertHandlesNotExist()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }

        rule.onNodeWithTag(containerTag).performTouchInput {
            swipeRight()
        }
        assertHandlesDisplayed()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_vertically_inContainer() {
        // make it scrollable
        val containerTag = "container"
        state = TextFieldState("hello", initialSelectionInChars = TextRange(1, 2))
        rule.setContent {
            Column(modifier = Modifier
                .height(200.dp)
                .verticalScroll(rememberScrollState())
                .testTag(containerTag)
            ) {
                BasicTextField2(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier
                        .testTag(TAG)
                        .height(100.dp)
                )
                Box(modifier = Modifier
                    .width(12.dp)
                    .height(400.dp))
            }
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(containerTag).performTouchInput {
            swipeUp()
        }
        assertHandlesNotExist()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }

        rule.onNodeWithTag(containerTag).performTouchInput {
            swipeDown()
        }
        assertHandlesDisplayed()
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 2))
        }
    }

    @Test
    fun dragStartSelectionHandle_toExtendSelection() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionStart, fontSizePx * 4)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 7))
        }
    }

    @Test
    fun dragEndSelectionHandle_toExtendSelection() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionEnd, fontSizePx * 4)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 11))
        }
    }

    @Test
    fun doubleClickOnWord_toSelectWord() {
        state = TextFieldState("abc def ghj")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            doubleClick(Offset(fontSizePx * 5, fontSizePx / 2)) // middle word
        }
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 7))
        }
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun doubleClickOnWhitespace_doesNotSelectWhitespace() {
        state = TextFieldState("abc def ghj")
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            // space between first and second words
            doubleClick(Offset(fontSizePx * 3.5f, fontSizePx / 2))
        }
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isNotEqualTo(TextRange(3, 4))
            assertThat(state.text.selectionInChars.collapsed).isFalse()
        }
    }

    @Test
    fun dragStartSelectionHandle_outOfBounds_horizontally() {
        state = TextFieldState("abc def ".repeat(10), initialSelectionInChars = TextRange(77, 80))
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        rule.waitForIdle()
        scope.launch { scrollState.scrollTo(scrollState.maxValue) } // scroll to the most right
        focusAndWait() // selection handles show up

        repeat(80) {
            swipeToLeft(Handle.SelectionStart, fontSizePx)
        }
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 80))
        }
    }

    @Test
    fun dragStartSelectionHandle_outOfBounds_vertically() {
        state = TextFieldState("abc def ".repeat(10), initialSelectionInChars = TextRange(77, 80))
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                scrollState = scrollState,
                modifier = Modifier
                    .testTag(TAG)
                    .width(50.dp)
            )
        }

        rule.waitForIdle()
        scope.launch { scrollState.scrollTo(scrollState.maxValue) } // scroll to the bottom
        focusAndWait() // selection handles show up

        swipeUp(Handle.SelectionStart, scrollState.maxValue.toFloat() * 2)
        // make sure that we also swipe to start on the first line
        swipeToLeft(Handle.SelectionStart, fontSizePx * 10)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 80))
        }
    }

    @Test
    fun dragEndSelectionHandle_outOfBounds_horizontally() {
        state = TextFieldState("abc def ".repeat(10), initialSelectionInChars = TextRange(0, 3))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        rule.waitForIdle()
        focusAndWait() // selection handles show up

        repeat(80) {
            swipeToRight(Handle.SelectionEnd, fontSizePx)
        }
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 80))
        }
    }

    @Test
    fun dragEndSelectionHandle_outOfBounds_vertically() {
        state = TextFieldState("abc def ".repeat(10), initialSelectionInChars = TextRange(0, 3))
        lateinit var layoutResult: () -> TextLayoutResult?
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .testTag(TAG)
                    .width(100.dp)
            )
        }

        rule.waitForIdle()
        focusAndWait() // selection handles show up

        @Suppress("NAME_SHADOWING")
        layoutResult()!!.let { layoutResult ->
            swipeDown(Handle.SelectionEnd, layoutResult.size.height.toFloat())
            swipeToRight(Handle.SelectionEnd, layoutResult.size.width.toFloat())
        }
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 80))
        }
    }

    @Test
    fun dragStartSelectionHandle_extendsByWord() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionStart, fontSizePx * 2) // only move by 2 characters
        rule.runOnIdle {
            // selection extends by a word
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 7))
        }
    }

    @Test
    fun dragEndSelectionHandle_extendsByWord() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionEnd, fontSizePx * 2) // only move by 2 characters
        rule.runOnIdle {
            // selection extends by a word
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 11))
        }
    }

    @Test
    fun dragStartSelectionHandle_shrinksByCharacter() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionStart, fontSizePx) // only move by a single character
        rule.runOnIdle {
            // selection shrinks by a character
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(5, 7))
        }
    }

    @Test
    fun dragEndSelectionHandle_shrinksByCharacter() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionEnd, fontSizePx) // only move by a single character
        rule.runOnIdle {
            // selection shrinks by a character
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 6))
        }
    }

    @Test
    fun dragStartSelectionHandlePastEndHandle_reversesTheSelection() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionStart, fontSizePx * 7)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(11, 7))
        }
    }

    @Test
    fun dragEndSelectionHandlePastStartHandle_canReverseSelection() {
        state = TextFieldState("abc def ghj", initialSelectionInChars = TextRange(4, 7))
        rule.setContent {
            BasicTextField2(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier
                    .testTag(TAG)
                    .width(200.dp)
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionEnd, fontSizePx * 7)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(4, 0))
        }
    }

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.RequestFocus)
    }

    private fun assertHandlesDisplayed(
        assertStartHandle: Boolean = true,
        assertEndHandle: Boolean = true
    ) {
        if (assertStartHandle) {
            rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        }
        if (assertEndHandle) {
            rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
        }
    }

    private fun assertHandlesNotExist(
        assertStartHandle: Boolean = true,
        assertEndHandle: Boolean = true
    ) {
        if (assertStartHandle) {
            rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        }
        if (assertEndHandle) {
            rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
        }
    }

    private fun swipeUp(handle: Handle, swipeDistance: Float) =
        performHandleDrag(handle, true, swipeDistance, Orientation.Vertical)

    private fun swipeDown(handle: Handle, swipeDistance: Float) =
        performHandleDrag(handle, false, swipeDistance, Orientation.Vertical)

    private fun swipeToLeft(handle: Handle, swipeDistance: Float) =
        performHandleDrag(handle, true, swipeDistance)

    private fun swipeToRight(handle: Handle, swipeDistance: Float) =
        performHandleDrag(handle, false, swipeDistance)

    private fun performHandleDrag(
        handle: Handle,
        toStart: Boolean,
        swipeDistance: Float = 1f,
        orientation: Orientation = Orientation.Horizontal
    ) {
        val handleNode = rule.onNode(isSelectionHandle(handle))

        handleNode.performTouchInput {
            if (orientation == Orientation.Horizontal) {
                if (toStart) {
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
            } else {
                if (toStart) {
                    swipeUp(
                        startY = centerY,
                        endY = centerY - viewConfiguration.touchSlop - swipeDistance,
                        durationMillis = 1000
                    )
                } else {
                    swipeDown(
                        startY = centerY,
                        endY = centerY + viewConfiguration.touchSlop + swipeDistance,
                        durationMillis = 1000
                    )
                }
            }
        }
    }
}
