/*
 * Copyright 2021 The Android Open Source Project
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

import android.view.DragEvent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.AbstractSelectionMagnifierTests
import androidx.compose.foundation.text.selection.assertMagnifierExists
import androidx.compose.foundation.text.selection.assertNoMagnifierExists
import androidx.compose.foundation.text.selection.getMagnifierCenterOffset
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.DragAndDropTestUtils.makeImageDragEvent
import androidx.compose.foundation.text2.input.internal.DragAndDropTestUtils.makeTextDragEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
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
        BasicTextField2(
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
        val tag = "BasicTextField2"
        val state = TextFieldState(
            "aaaa",
            initialSelectionInChars = TextRange.Zero
        )

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BasicTextField2(
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
        val tag = "BasicTextField2"
        val state = TextFieldState(
            "aaaa\naaaa\naaaa\n".repeat(5),
            initialSelectionInChars = TextRange.Zero
        )
        val scrollState = ScrollState(0)
        var coroutineScope: CoroutineScope? = null

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                coroutineScope = rememberCoroutineScope()
                BasicTextField2(
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
        val tag = "BasicTextField2"
        val state = TextFieldState(
            "aaaa aaaa aaaa ".repeat(5),
            initialSelectionInChars = TextRange.Zero
        )
        val scrollState = ScrollState(0)
        var coroutineScope: CoroutineScope? = null

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                coroutineScope = rememberCoroutineScope()
                BasicTextField2(
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

    @OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
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

        val tag = "BasicTextField2"
        val state = TextFieldState(
            "$fillerWord $fillerWord $fillerWord ".repeat(10),
            initialSelectionInChars = TextRange.Zero
        )

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                BasicTextField2(
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

    private fun setupDragAndDropContent(): View {
        val state = TextFieldState(
            "aaaa",
            initialSelectionInChars = TextRange.Zero
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
                BasicTextField2(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
        }

        return view!!
    }
}
