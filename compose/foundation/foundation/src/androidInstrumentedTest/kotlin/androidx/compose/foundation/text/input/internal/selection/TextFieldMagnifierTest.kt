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

import android.view.DragEvent
import android.view.View
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.DragAndDropTestUtils.makeImageDragEvent
import androidx.compose.foundation.text.input.internal.DragAndDropTestUtils.makeTextDragEvent
import androidx.compose.foundation.text.selection.AbstractSelectionMagnifierTests
import androidx.compose.foundation.text.selection.assertMagnifierExists
import androidx.compose.foundation.text.selection.assertNoMagnifierExists
import androidx.compose.foundation.text.selection.assertThatOffset
import androidx.compose.foundation.text.selection.gestures.RtlChar
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.getMagnifierCenterOffset
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
internal class TextFieldMagnifierTest : AbstractSelectionMagnifierTests() {

    @Composable
    override fun TestContent(
        text: String,
        modifier: Modifier,
        style: TextStyle,
        onTextLayout: (TextLayoutResult) -> Unit,
        maxLines: Int
    ) {
        val state = remember { TextFieldState(text) }
        BasicTextField(
            state = state,
            modifier = modifier,
            textStyle = style,
            onTextLayout = { it()?.let(onTextLayout) }
        )
    }

    @Test
    fun magnifier_followsCursorHorizontally_whenDragged() {
        checkMagnifierFollowsHandleHorizontally(Handle.Cursor)
    }

    @Test
    fun magnifier_staysAtLineEnd_whenCursorDraggedPastStart() {
        checkMagnifierConstrainedToLineHorizontalBounds(
            Handle.Cursor,
            checkStart = true
        )
    }

    @Test
    fun magnifier_staysAtLineEnd_whenCursorDraggedPastEnd() {
        checkMagnifierConstrainedToLineHorizontalBounds(
            Handle.Cursor,
            checkStart = false
        )
    }

    @Test
    fun magnifier_hidden_whenCursorDraggedFarPastStartOfLine() {
        checkMagnifierHiddenWhenDraggedTooFar(Handle.Cursor, checkStart = true)
    }

    @Test
    fun magnifier_hidden_whenCursorDraggedFarPastEndOfLine() {
        checkMagnifierHiddenWhenDraggedTooFar(Handle.Cursor, checkStart = false)
    }

    @Test
    fun magnifier_staysAtVisibleRegion_whenCursorDraggedPastScrollThreshold_Ltr() {
        checkMagnifierStayAtEndWhenDraggedBeyondScroll(Handle.Cursor, LayoutDirection.Ltr)
    }

    @Test
    fun magnifier_staysAtVisibleRegion_whenCursorDraggedPastScrollThreshold_Rtl() {
        checkMagnifierStayAtEndWhenDraggedBeyondScroll(Handle.Cursor, LayoutDirection.Rtl)
    }

    @Test
    fun magnifier_staysAtVisibleRegion_whenSelectionStartDraggedPastScrollThreshold_Ltr() {
        checkMagnifierStayAtEndWhenDraggedBeyondScroll(Handle.SelectionStart, LayoutDirection.Ltr)
    }

    @Test
    fun magnifier_staysAtVisibleRegion_whenSelectionStartDraggedPastScrollThreshold_Rtl() {
        checkMagnifierStayAtEndWhenDraggedBeyondScroll(Handle.SelectionStart, LayoutDirection.Rtl)
    }

    @Test
    fun magnifier_staysAtVisibleRegion_whenSelectionEndDraggedPastScrollThreshold_Ltr() {
        checkMagnifierStayAtEndWhenDraggedBeyondScroll(Handle.SelectionEnd, LayoutDirection.Ltr)
    }

    @Test
    fun magnifier_staysAtVisibleRegion_whenSelectionEndDraggedPastScrollThreshold_Rtl() {
        checkMagnifierStayAtEndWhenDraggedBeyondScroll(Handle.SelectionEnd, LayoutDirection.Rtl)
    }

    @Test
    fun magnifier_shows_whenTextIsDraggingFromAnotherApp() {
        val view = setupDragAndDropContent()

        rule.runOnIdle {
            val startEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent = makeTextDragEvent(
                action = DragEvent.ACTION_DRAG_LOCATION,
                offset = Offset(40f, 10f)
            )

            view.dispatchDragEvent(startEvent)
            view.dispatchDragEvent(enterEvent)
            view.dispatchDragEvent(moveEvent)
        }

        Truth.assertThat(getMagnifierCenterOffset(rule)).isEqualTo(Offset(40f, 10f))
    }

    @Test
    fun magnifier_doesNotShow_ifDraggingItem_doesNotHaveText() {
        val view = setupDragAndDropContent()

        rule.runOnIdle {
            val startEvent = makeImageDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeImageDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent = makeImageDragEvent(
                DragEvent.ACTION_DRAG_LOCATION,
                offset = Offset(40f, 10f)
            )

            view.dispatchDragEvent(startEvent)
            view.dispatchDragEvent(enterEvent)
            view.dispatchDragEvent(moveEvent)
        }

        assertNoMagnifierExists(rule)
    }

    @Test
    fun magnifier_doesNotLinger_whenDraggingItemLeaves() {
        val view = setupDragAndDropContent()

        rule.runOnIdle {
            val startEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_STARTED)
            val enterEvent = makeTextDragEvent(DragEvent.ACTION_DRAG_ENTERED)
            val moveEvent = makeTextDragEvent(
                action = DragEvent.ACTION_DRAG_LOCATION,
                offset = Offset(40f, 10f)
            )

            view.dispatchDragEvent(startEvent)
            view.dispatchDragEvent(enterEvent)
            view.dispatchDragEvent(moveEvent)
        }

        assertMagnifierExists(rule)

        rule.runOnIdle {
            val moveEvent2 = makeTextDragEvent(
                action = DragEvent.ACTION_DRAG_LOCATION,
                offset = Offset(40f, 40f) // force it out of BTF2's hit box
            )
            view.dispatchDragEvent(moveEvent2)
        }

        assertNoMagnifierExists(rule)
    }

    @Test
    fun magnifier_insideDecorationBox() {
        val tag = "BasicTextField"
        val state = TextFieldState(
            "aaaa",
            initialSelection = TextRange.Zero
        )

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BasicTextField(
                    state = state,
                    Modifier.testTag(tag),
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    decorator = {
                        Box(modifier = Modifier.padding(8.dp)) {
                            it()
                        }
                    }
                )
            }
        }

        rule.onNodeWithTag(tag).performTouchInput {
            click(topLeft)
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            down(center)
            movePastSlopBy(Offset(-0.1f, 0.1f))
        }

        Truth.assertThat(getMagnifierCenterOffset(rule)).isEqualTo(
            Offset(0f, 10f) + Offset(8f, 8f)
        )
    }

    @Test
    fun magnifier_insideDecorationBox_scrolledVertically() {
        val tag = "BasicTextField"
        val state = TextFieldState(
            "aaaa\naaaa\naaaa\n".repeat(5),
            initialSelection = TextRange.Zero
        )
        val scrollState = ScrollState(0)
        var coroutineScope: CoroutineScope? = null

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                coroutineScope = rememberCoroutineScope()
                BasicTextField(
                    state = state,
                    Modifier.testTag(tag),
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    lineLimits = TextFieldLineLimits.MultiLine(1, 2),
                    scrollState = scrollState,
                    decorator = {
                        Box(modifier = Modifier.padding(8.dp)) {
                            it()
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        coroutineScope?.launch {
            scrollState.scrollTo(scrollState.maxValue)
        }

        rule.onNodeWithTag(tag).performTouchInput {
            click(bottomLeft)
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            down(center)
            movePastSlopBy(Offset(0.1f, 0.1f))
        }

        Truth.assertThat(getMagnifierCenterOffset(rule)).isEqualTo(
            Offset(0f, 30f) + Offset(8f, 8f)
        )
    }

    @Test
    fun magnifier_insideDecorationBox_scrolledHorizontally() {
        val tag = "BasicTextField"
        val state = TextFieldState(
            "aaaa aaaa aaaa ".repeat(5),
            initialSelection = TextRange.Zero
        )
        val scrollState = ScrollState(0)
        var coroutineScope: CoroutineScope? = null

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                coroutineScope = rememberCoroutineScope()
                BasicTextField(
                    state = state,
                    Modifier.testTag(tag).width(100.dp),
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    scrollState = scrollState,
                    decorator = {
                        Box(modifier = Modifier.padding(8.dp)) {
                            it()
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        coroutineScope?.launch {
            scrollState.scrollTo(scrollState.maxValue)
        }

        rule.onNodeWithTag(tag).performTouchInput {
            click(centerRight)
        }

        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            down(center)
            movePastSlopBy(Offset(0.1f, 0.1f))
        }

        Truth.assertThat(getMagnifierCenterOffset(rule)).isEqualTo(
            // x: drag threshold, y: line center(2nd line in view) + x: padding, y: padding
            Offset(100f - 16f, 10f) + Offset(8f, 8f)
        )
    }

    // regression - When dragging to the final empty line, the magnifier appeared on the second
    // to last line instead of on the final line. It should appear on the final line.
    // This test is moved up from abstract test class `AbstractSelectionMagnifierTests` because
    // the long press behavior is different between the new BasicTextField and the legacy one.
    @Test
    fun textField_magnifier_centeredOnCorrectLine_whenLinesAreEmpty() {
        lateinit var textLayout: TextLayoutResult
        rule.setTextFieldTestContent {
            Content(
                text = "a\n\n",
                modifier = Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag),
                onTextLayout = { textLayout = it }
            )
        }

        rule.waitForIdle()

        val firstPressOffset = textLayout.getBoundingBox(0).centerLeft + Offset(1f, 0f)

        val placedOffset = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.topLeft

        fun assertMagnifierAt(expected: Offset) {
            rule.waitForIdle()
            val actual = getMagnifierCenterOffset(rule, requireSpecified = true) - placedOffset
            assertThatOffset(actual).equalsWithTolerance(expected)
        }

        // start selection at first character
        rule.onNodeWithTag(tag).performTouchInput {
            longPress(firstPressOffset)
        }
        assertMagnifierAt(firstPressOffset)

        fun getOffsetAtLine(line: Int): Offset = Offset(
            x = firstPressOffset.x,
            y = lerp(
                start = textLayout.getLineTop(lineIndex = line),
                stop = textLayout.getLineBottom(lineIndex = line),
                fraction = 0.5f
            )
        )

        val secondOffset = getOffsetAtLine(1)
        rule.onNodeWithTag(tag).performTouchInput {
            moveTo(secondOffset)
        }
        assertMagnifierAt(Offset(0f, secondOffset.y))

        val thirdOffset = getOffsetAtLine(2)
        rule.onNodeWithTag(tag).performTouchInput {
            moveTo(thirdOffset)
        }
        assertMagnifierAt(Offset(0f, thirdOffset.y))
    }

    // Regression - magnifier should be constrained to end of line in BiDi,
    // not the last offset which could be in middle of the line.
    // This test is moved up from abstract test class `AbstractSelectionMagnifierTests` because
    // the long press behavior is different between the new BasicTextField and the legacy one.
    @Test
    fun textField_magnifier_centeredToEndOfLine_whenBidiEndOffsetInMiddleOfLine() {
        val ltrWord = "hello"
        val rtlWord = RtlChar.repeat(5)

        lateinit var textLayout: TextLayoutResult
        rule.setTextFieldTestContent {
            Content(
                text = """
                    $rtlWord $ltrWord
                    $ltrWord $rtlWord
                    $rtlWord $ltrWord
                """.trimIndent().trim(),
                modifier = Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentHeight()
                    .testTag(tag),
                onTextLayout = { textLayout = it }
            )
        }

        val placedPosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot

        fun getCenterForLine(line: Int): Float {
            val top = textLayout.getLineTop(line)
            val bottom = textLayout.getLineBottom(line)
            return (bottom - top) / 2 + top
        }

        val farRightX = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.right - 1f

        rule.onNodeWithTag(tag).performTouchInput {
            longPress(Offset(farRightX, getCenterForLine(0)))
        }
        rule.waitForIdle()
        Truth.assertWithMessage("Magnifier should not be shown")
            .that(getMagnifierCenterOffset(rule).isUnspecified)
            .isTrue()

        val secondLineCenterY = getCenterForLine(1)
        val secondOffset = Offset(farRightX, secondLineCenterY)
        rule.onNodeWithTag(tag).performTouchInput {
            moveTo(secondOffset)
        }
        rule.waitForIdle()
        Truth.assertWithMessage("Magnifier should not be shown")
            .that(getMagnifierCenterOffset(rule).isUnspecified)
            .isTrue()

        val lineRightX = textLayout.getLineRight(1)
        val thirdOffset = Offset(lineRightX + 1f, secondLineCenterY)
        rule.onNodeWithTag(tag).performTouchInput {
            moveTo(thirdOffset)
        }
        rule.waitForIdle()
        val actual = getMagnifierCenterOffset(rule, requireSpecified = true) - placedPosition
        assertThatOffset(actual).equalsWithTolerance(Offset(lineRightX, secondLineCenterY))
    }

    @OptIn(ExperimentalTestApi::class)
    private fun checkMagnifierStayAtEndWhenDraggedBeyondScroll(
        handle: Handle,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        var screenSize = Size.Zero
        val dragDirection = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        val directionVector = Offset(1f, 0f) * dragDirection
        val fillerWord = if (layoutDirection == LayoutDirection.Ltr)
            "aaaa"
        else
            "\u05D0\u05D1\u05D2\u05D3"

        val tag = "BasicTextField"
        val state = TextFieldState(
            "$fillerWord $fillerWord $fillerWord ".repeat(10),
            initialSelection = TextRange.Zero
        )

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                BasicTextField(
                    state = state,
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { screenSize = it.toSize() }
                        .wrapContentSize()
                        .testTag(tag),
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY),
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
        }

        if (handle == Handle.Cursor) {
            rule.onNodeWithTag(tag).performClick()
        } else {
            rule.onNodeWithTag(tag).performTextInputSelection(TextRange(5, 9))
        }

        // Touch and move the handle to show the magnifier.
        rule.onNode(isSelectionHandle(handle)).performTouchInput {
            down(center)
            // If cursor, we have to drag the cursor to show the magnifier,
            // press alone will not suffice
            movePastSlopBy(directionVector)
        }

        val magnifierInitialPosition = getMagnifierCenterOffset(rule, requireSpecified = true)

        // Drag all the way past the end of the line.
        rule.onNode(isSelectionHandle(handle))
            .performTouchInput {
                val delta = Offset(
                    x = screenSize.width * directionVector.x,
                    y = screenSize.height * directionVector.y
                )
                moveBy(delta)
            }

        val x = if (layoutDirection == LayoutDirection.Ltr) screenSize.width else 0f
        Truth.assertThat(getMagnifierCenterOffset(rule)).isEqualTo(
            Offset(x, magnifierInitialPosition.y)
        )
    }

    /**
     * BasicTextField(state) has a different long press behavior compared to the legacy
     * `BasicTextField`. We need to override this helper function to make sure that we are testing
     * it correctly.
     */
    override fun checkMagnifierShowsDuringInitialLongPressDrag(
        expandForwards: Boolean,
        layoutDirection: LayoutDirection
    ) {
        val dragDistance = Offset(10f, 0f)
        val dragDirection = if (expandForwards) 1f else -1f
        val char = if (layoutDirection == LayoutDirection.Ltr) "a" else RtlChar
        val word = char.repeat(4)
        rule.setTextFieldTestContent {
            Content(
                text = "$word $word $word",
                modifier = Modifier
                    // Center the text to give the magnifier lots of room to move.
                    .fillMaxSize()
                    .wrapContentSize()
                    .testTag(tag)
            )
        }

        // Initiate selection.
        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset.Zero, delayMillis = viewConfiguration.longPressTimeoutMillis + 100)
            }

        // Magnifier should show after long-press starts.
        val magnifierInitialPosition = getMagnifierCenterOffset(rule, requireSpecified = true)

        // Drag horizontally - the magnifier should follow.
        rule.onNodeWithTag(tag)
            .performTouchInput {
                // Don't need to worry about touch slop for this test since the drag starts as soon
                // as the long click is detected.
                moveBy(dragDistance * dragDirection)
            }

        // make the assertion without sending an `up` event which would cause an input session
        // to start and keyboard to show up.
        Truth.assertThat(getMagnifierCenterOffset(rule))
            .isEqualTo(magnifierInitialPosition + (dragDistance * dragDirection))
    }

    private fun setupDragAndDropContent(): View {
        val state = TextFieldState(
            "aaaa",
            initialSelection = TextRange.Zero
        )
        var view: View? = null
        rule.setContent { // Do not use setTextFieldTestContent for DnD tests.
            view = LocalView.current
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1f),
                LocalWindowInfo provides object : WindowInfo {
                    override val isWindowFocused = false
                }
            ) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
        }

        return view!!
    }
}
