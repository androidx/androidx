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

import android.os.Build
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.View
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionHandleAnchor
import androidx.compose.foundation.text.selection.assertHandleAnchorMatches
import androidx.compose.foundation.text.selection.assertHandlePositionMatches
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

@LargeTest
class TextFieldSelectionHandlesTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val TAG = "BasicTextField"

    private val fontSize = 10.sp
    private val fontSizePx = with(rule.density) { fontSize.toPx() }

    @Test
    fun selectionHandles_doNotShow_whenFieldNotFocused() {
        state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        assertHandlesNotExist()
    }

    @Test
    fun selectionHandles_haveMinimumTouchSizeArea() =
        with(rule.density) {
            state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
            rule.setContent {
                BasicTextField(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG).width(100.dp),
                )
            }

            focusAndWait()

            var actualStartBottomRight = Offset.Zero
            var actualEndBottomRight = Offset.Zero
            rule.onNode(isSelectionHandle(Handle.SelectionStart)).performTouchInput {
                actualStartBottomRight = bottomRight
            }
            rule.onNode(isSelectionHandle(Handle.SelectionEnd)).performTouchInput {
                actualEndBottomRight = bottomRight
            }

            val expectedBottomRight = Offset(40.dp.toPx(), 40.dp.toPx())
            assertThat(actualStartBottomRight.x).isWithin(1f).of(expectedBottomRight.x)
            assertThat(actualStartBottomRight.y).isWithin(1f).of(expectedBottomRight.y)
            assertThat(actualEndBottomRight.x).isWithin(1f).of(expectedBottomRight.x)
            assertThat(actualEndBottomRight.y).isWithin(1f).of(expectedBottomRight.y)
        }

    @Test
    fun selectionHandles_appears_whenFieldGetsFocused() {
        state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        focusAndWait()
        assertHandlesDisplayed()
    }

    @Test
    fun selectionHandles_disappear_whenFieldLosesFocus() {
        state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
        val focusRequester = FocusRequester()
        rule.setTextFieldTestContent {
            Column {
                Box(Modifier.size(100.dp).focusRequester(focusRequester).focusable())
                BasicTextField(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG).width(100.dp),
                )
            }
        }

        focusAndWait()
        assertHandlesDisplayed()
        rule.runOnIdle { focusRequester.requestFocus() }
        assertHandlesNotExist()
    }

    @Test
    fun selectionHandles_appear_whenTextAlignedToEnd() {
        state = TextFieldState("hello", initialSelection = TextRange(0, 5))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle =
                    TextStyle(
                        fontSize = fontSize,
                        fontFamily = TEST_FONT_FAMILY,
                        textAlign = TextAlign.End,
                        letterSpacing = 1.2.sp,
                    ),
                modifier = Modifier.testTag(TAG).fillMaxWidth(),
            )
        }

        focusAndWait()
        assertHandlesDisplayed()
    }

    @Test
    fun textField_noSelectionHandles_whenWindowLosesFocus() {
        state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
        val focusWindow = mutableStateOf(true)
        val windowInfo =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focusWindow.value
            }

        rule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                BasicTextField(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG).width(100.dp),
                )
            }
        }

        // selection handles displayed
        focusAndWait()
        assertHandlesDisplayed()

        // window lost focus, make sure handles disappeared
        focusWindow.value = false
        rule.waitForIdle()

        assertHandlesNotExist()
    }

    @Test
    fun textField_redisplaysSelectionHandles_whenWindowRegainsFocus() {
        state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
        val focusWindow = mutableStateOf(true)
        val windowInfo =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focusWindow.value
            }

        rule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                BasicTextField(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG).width(100.dp),
                )
            }
        }

        // selection handles displayed
        focusAndWait()
        assertHandlesDisplayed()

        // window lost focus, make sure handles disappeared
        focusWindow.value = false
        rule.waitForIdle()

        assertHandlesNotExist()

        // regain window focus
        focusWindow.value = true
        rule.waitForIdle()

        assertHandlesDisplayed()
    }

    @Test
    fun textField_redisplaysSelectionHandles_whenTextFieldStateChanges() {
        val tfsState =
            mutableStateOf(TextFieldState("hello, world", initialSelection = TextRange(2, 5)))

        rule.setContent {
            BasicTextField(
                tfsState.value,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        // selection handles displayed
        focusAndWait()
        assertHandlesDisplayed()

        // change state to something without initial selection, assert that handles are disappeared
        tfsState.value = TextFieldState("hello", initialSelection = TextRange.Zero)
        rule.waitForIdle()

        assertHandlesNotExist()

        // create selection via touch, assert that handles are displayed
        focusAndWait()
        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSizePx * 2, fontSizePx / 2))
        }
        rule.waitForIdle()

        assertHandlesDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectionHandles_locatedAtTheRightPosition_ltr_ltr() {
        state = TextFieldState("hello, world", initialSelection = TextRange(2, 5))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        focusAndWait()

        with(rule.onNode(isSelectionHandle(Handle.SelectionStart))) {
            assertHandlePositionMatches((2 * fontSize.value).dp, fontSize.value.dp)
            assertHandleAnchorMatches(SelectionHandleAnchor.Left)
        }

        with(rule.onNode(isSelectionHandle(Handle.SelectionEnd))) {
            assertHandlePositionMatches((5 * fontSize.value).dp, fontSize.value.dp)
            assertHandleAnchorMatches(SelectionHandleAnchor.Right)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectionHandles_locatedAtTheRightPosition_ltr_rtl() {
        state = TextFieldState("abc \u05D0\u05D1\u05D2", initialSelection = TextRange(1, 6))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        focusAndWait()

        with(rule.onNode(isSelectionHandle(Handle.SelectionStart))) {
            assertHandlePositionMatches((1 * fontSize.value).dp, fontSize.value.dp)
            assertHandleAnchorMatches(SelectionHandleAnchor.Left)
        }

        with(rule.onNode(isSelectionHandle(Handle.SelectionEnd))) {
            assertHandlePositionMatches((5 * fontSize.value).dp, fontSize.value.dp)
            assertHandleAnchorMatches(SelectionHandleAnchor.Left)
        }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_horizontally() {
        // make it scrollable
        state = TextFieldState("hello ".repeat(10), initialSelection = TextRange(1, 2))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(TAG).performTouchInput { swipeLeft() }
        assertHandlesNotExist()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }

        rule.onNodeWithTag(TAG).performTouchInput { swipeRight() }
        assertHandlesDisplayed()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_vertically() {
        // make it scrollable
        state = TextFieldState("hello ".repeat(10), initialSelection = TextRange(1, 2))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 2),
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(TAG).performTouchInput { swipeUp() }
        assertHandlesNotExist()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }

        rule.onNodeWithTag(TAG).performTouchInput { swipeDown() }
        assertHandlesDisplayed()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_horizontally_inContainer() {
        // make it scrollable
        val containerTag = "container"
        state = TextFieldState("hello", initialSelection = TextRange(1, 2))
        rule.setTextFieldTestContent {
            Row(
                modifier =
                    Modifier.width(200.dp)
                        .horizontalScroll(rememberScrollState())
                        .testTag(containerTag)
            ) {
                BasicTextField(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG).width(100.dp),
                )
                Box(modifier = Modifier.height(12.dp).width(400.dp))
            }
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(containerTag).performTouchInput { swipeLeft() }
        assertHandlesNotExist()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }

        rule.onNodeWithTag(containerTag).performTouchInput { swipeRight() }
        assertHandlesDisplayed()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }
    }

    @Test
    fun selectionHandlesDisappear_whenScrolledOutOfView_vertically_inContainer() {
        // make it scrollable
        val containerTag = "container"
        state = TextFieldState("hello", initialSelection = TextRange(1, 2))
        rule.setTextFieldTestContent {
            Column(
                modifier =
                    Modifier.height(200.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag(containerTag)
            ) {
                BasicTextField(
                    state,
                    textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                    modifier = Modifier.testTag(TAG).height(100.dp),
                )
                Box(modifier = Modifier.width(12.dp).height(400.dp))
            }
        }

        focusAndWait()
        assertHandlesDisplayed()

        rule.onNodeWithTag(containerTag).performTouchInput { swipeUp() }
        assertHandlesNotExist()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }

        rule.onNodeWithTag(containerTag).performTouchInput { swipeDown() }
        assertHandlesDisplayed()
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(1, 2)) }
    }

    @Test
    fun dragStartSelectionHandle_toExtendSelection() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionStart, fontSizePx * 4)
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(0, 7)) }
    }

    @Test
    fun dragEndSelectionHandle_toExtendSelection() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionEnd, fontSizePx * 4)
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(4, 11)) }
    }

    @Test
    fun doubleClickOnWord_toSelectWord() {
        state = TextFieldState("abc def ghj")
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            doubleClick(Offset(fontSizePx * 5, fontSizePx / 2)) // middle word
        }
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(4, 7)) }
    }

    @Test
    fun doubleClickOnWhitespace_doesNotSelectWhitespace() {
        state = TextFieldState("abc def ghj")
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            // space between first and second words
            doubleClick(Offset(fontSizePx * 3.5f, fontSizePx / 2))
        }
        rule.runOnIdle {
            assertThat(state.selection).isNotEqualTo(TextRange(3, 4))
            assertThat(state.selection.collapsed).isFalse()
        }
    }

    @Test
    fun dragStartSelectionHandle_outOfBounds_horizontally() {
        state = TextFieldState("abc def ".repeat(10), initialSelection = TextRange(77, 80))
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setTextFieldTestContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                scrollState = scrollState,
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        rule.waitForIdle()
        scope.launch { scrollState.scrollTo(scrollState.maxValue) } // scroll to the most right
        focusAndWait() // selection handles show up

        repeat(80) { swipeToLeft(Handle.SelectionStart, fontSizePx) }
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(0, 80)) }
    }

    @Test
    fun dragStartSelectionHandle_outOfBounds_vertically() {
        state = TextFieldState("abc def ".repeat(10), initialSelection = TextRange(77, 80))
        val scrollState = ScrollState(0)
        lateinit var scope: CoroutineScope
        rule.setTextFieldTestContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                scrollState = scrollState,
                modifier = Modifier.testTag(TAG).width(50.dp),
            )
        }

        rule.waitForIdle()
        scope.launch { scrollState.scrollTo(scrollState.maxValue) } // scroll to the bottom
        focusAndWait() // selection handles show up

        swipeUp(Handle.SelectionStart, scrollState.maxValue.toFloat() * 2)
        // make sure that we also swipe to start on the first line
        swipeToLeft(Handle.SelectionStart, fontSizePx * 10)
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(0, 80)) }
    }

    @Test
    fun dragEndSelectionHandle_outOfBounds_horizontally() {
        state = TextFieldState("abc def ".repeat(10), initialSelection = TextRange(0, 3))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        rule.waitForIdle()
        focusAndWait() // selection handles show up

        repeat(80) { swipeToRight(Handle.SelectionEnd, fontSizePx) }
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(0, 80)) }
    }

    @Test
    fun dragEndSelectionHandle_outOfBounds_vertically() {
        state = TextFieldState("abc def ".repeat(10), initialSelection = TextRange(0, 3))
        lateinit var layoutResult: () -> TextLayoutResult?
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                onTextLayout = { layoutResult = it },
                modifier = Modifier.testTag(TAG).width(100.dp),
            )
        }

        rule.waitForIdle()
        focusAndWait() // selection handles show up

        @Suppress("NAME_SHADOWING")
        layoutResult()!!.let { layoutResult ->
            swipeDown(Handle.SelectionEnd, layoutResult.size.height.toFloat())
            swipeToRight(Handle.SelectionEnd, layoutResult.size.width.toFloat())
        }
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(0, 80)) }
    }

    @Test
    fun dragStartSelectionHandle_extendsByWord() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionStart, fontSizePx * 2) // only move by 2 characters
        rule.runOnIdle {
            // selection extends by a word
            assertThat(state.selection).isEqualTo(TextRange(0, 7))
        }
    }

    @Test
    fun dragEndSelectionHandle_extendsByWord() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionEnd, fontSizePx * 2) // only move by 2 characters
        rule.runOnIdle {
            // selection extends by a word
            assertThat(state.selection).isEqualTo(TextRange(4, 11))
        }
    }

    @Test
    fun dragStartSelectionHandle_shrinksByCharacter() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionStart, fontSizePx) // only move by a single character
        rule.runOnIdle {
            // selection shrinks by a character
            assertThat(state.selection).isEqualTo(TextRange(5, 7))
        }
    }

    @Test
    fun dragEndSelectionHandle_shrinksByCharacter() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionEnd, fontSizePx) // only move by a single character
        rule.runOnIdle {
            // selection shrinks by a character
            assertThat(state.selection).isEqualTo(TextRange(4, 6))
        }
    }

    @Test
    fun dragStartSelectionHandlePastEndHandle_reversesTheSelection() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToRight(Handle.SelectionStart, fontSizePx * 7)
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(11, 7)) }
    }

    @Test
    fun dragEndSelectionHandlePastStartHandle_canReverseSelection() {
        state = TextFieldState("abc def ghj", initialSelection = TextRange(4, 7))
        rule.setTextFieldTestContent {
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG).width(200.dp),
            )
        }

        focusAndWait()

        swipeToLeft(Handle.SelectionEnd, fontSizePx * 7)
        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(4, 0)) }
    }

    @Test
    fun selectionHandlesHide_whenHardwareKeyboardIsUsed_thenComesBackWithTouch() {
        state = TextFieldState("hello")
        lateinit var view: View
        rule.setTextFieldTestContent {
            view = LocalView.current
            BasicTextField(
                state,
                textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY),
                modifier = Modifier.testTag(TAG),
            )
        }

        focusAndWait()

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 2, fontSize.toPx() / 2))
        }

        assertHandlesDisplayed()

        // regular `performKeyInput` scope does not set source to InputDevice.SOURCE_KEYBOARD
        view.dispatchKeyEvent(
            KeyEvent(
                /* downTime = */ 0,
                /* eventTime = */ 0,
                /* action = */ ACTION_DOWN,
                /* code = */ KeyEvent.KEYCODE_A,
                /* repeat = */ 0,
                /* metaState = */ KeyEvent.META_CTRL_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                /* scancode= */ 0,
                /* flags= */ 0,
                /* source= */ InputDevice.SOURCE_KEYBOARD,
            )
        )

        assertHandlesNotExist()
        assertThat(state.selection).isEqualTo(TextRange(0, 5))

        rule.onNodeWithTag(TAG).performTouchInput {
            longClick(Offset(fontSize.toPx() * 2, fontSize.toPx() / 2))
        }

        assertHandlesDisplayed()
    }

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.RequestFocus)
    }

    private fun assertHandlesDisplayed(
        assertStartHandle: Boolean = true,
        assertEndHandle: Boolean = true,
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
        assertEndHandle: Boolean = true,
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
        orientation: Orientation = Orientation.Horizontal,
    ) {
        val handleNode = rule.onNode(isSelectionHandle(handle))

        handleNode.performTouchInput {
            if (orientation == Orientation.Horizontal) {
                if (toStart) {
                    swipeLeft(
                        startX = centerX,
                        endX = centerX - viewConfiguration.touchSlop - swipeDistance,
                        durationMillis = 1000,
                    )
                } else {
                    swipeRight(
                        startX = centerX,
                        endX = centerX + viewConfiguration.touchSlop + swipeDistance,
                        durationMillis = 1000,
                    )
                }
            } else {
                if (toStart) {
                    swipeUp(
                        startY = centerY,
                        endY = centerY - viewConfiguration.touchSlop - swipeDistance,
                        durationMillis = 1000,
                    )
                } else {
                    swipeDown(
                        startY = centerY,
                        endY = centerY + viewConfiguration.touchSlop + swipeDistance,
                        durationMillis = 1000,
                    )
                }
            }
        }
    }
}
