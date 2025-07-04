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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.test.assertThatIntRect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.sign
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@Suppress("DEPRECATION")
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class SelectionContainerTest : AbstractSelectionContainerTest() {

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun press_to_cancel() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        createSelectionContainer()
        val position = 50f
        rule.onSelectionContainer().performTouchInput {
            longClick(Offset(x = position, y = position))
        }
        rule.runOnIdle { assertThat(selection.value).isNotNull() }

        // Act.
        rule.onSelectionContainer().performTouchInput { click(Offset(x = position, y = position)) }

        // Assert.
        rule.runOnIdle {
            assertThat(selection.value).isNull()
            verify(hapticFeedback, times(2))
                .performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun tapToCancelDoesNotBlockUp() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        createSelectionContainer()
        val position = 50f
        rule.onSelectionContainer().performTouchInput {
            longClick(Offset(x = position, y = position))
        }

        log.entries.clear()

        // Act.
        rule.onSelectionContainer().performTouchInput { click(Offset(x = position, y = position)) }

        // Assert.
        rule.runOnIdle {
            // We are interested in looking at the final up event.
            assertThat(log.entries.last().pass).isEqualTo(PointerEventPass.Final)
            assertThat(log.entries.last().changes).hasSize(1)
            assertThat(log.entries.last().changes[0].changedToUp()).isTrue()
        }
    }

    @Test
    fun long_press_select_a_word() {
        with(rule.density) {
            // Setup.
            // Long Press "m" in "Demo", and "Demo" should be selected.
            createSelectionContainer()
            val characterSize = fontSize.toPx()
            val expectedLeftX = fontSize.toDp().times(textContent.indexOf('D'))
            val expectedLeftY = fontSize.toDp()
            val expectedRightX = fontSize.toDp().times(textContent.indexOf('o') + 1)
            val expectedRightY = fontSize.toDp()

            // Act.
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            rule.mainClock.advanceTimeByFrame()
            // Assert. Should select "Demo".
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
            verify(hapticFeedback, times(1))
                .performHapticFeedback(HapticFeedbackType.TextHandleMove)

            // Check the position of the anchors of the selection handles. We don't need to compare
            // to the absolute position since the semantics report selection relative to the
            // container composable, not the screen.
            rule
                .onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedLeftX, expectedLeftY)
            rule
                .onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedRightX, expectedRightY)
        }
    }

    @Test
    fun long_press_select_a_word_rtl_layout() {
        with(rule.density) {
            // Setup.
            // Long Press "m" in "Demo", and "Demo" should be selected.
            createSelectionContainer(isRtl = true)
            val characterSize = fontSize.toPx()
            val expectedLeftX = rule.rootWidth() - fontSize.toDp().times(textContent.length)
            val expectedLeftY = fontSize.toDp()
            val expectedRightX = rule.rootWidth() - fontSize.toDp().times(" Demo Text".length)
            val expectedRightY = fontSize.toDp()

            // Act.
            rule.onSelectionContainer().performTouchInput {
                longClick(
                    Offset(
                        rule.rootWidth().toSp().toPx() - ("xt Demo Text").length * characterSize,
                        0.5f * characterSize,
                    )
                )
            }

            rule.mainClock.advanceTimeByFrame()

            // Assert. Should select "Demo".
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('T'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('t') + 1)
            verify(hapticFeedback, times(1))
                .performHapticFeedback(HapticFeedbackType.TextHandleMove)

            // Check the position of the anchors of the selection handles. We don't need to compare
            // to the absolute position since the semantics report selection relative to the
            // container composable, not the screen.
            rule
                .onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedLeftX, expectedLeftY)
            rule
                .onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedRightX, expectedRightY)
        }
    }

    @Test
    fun selectionContinues_toBelowText() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }
        }

        startSelection(tag1)
        dragHandleTo(Handle.SelectionEnd, offset = characterBox(tag2, 3).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 4, selectableId = 2)
    }

    @Test
    fun selectionContinues_toAboveText() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }
        }

        startSelection(tag2, offset = 6) // second word should be selected
        dragHandleTo(Handle.SelectionStart, offset = characterBox(tag1, 5).bottomLeft)

        assertAnchorInfo(selection.value?.start, offset = 5, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 9, selectableId = 2)
    }

    @Test
    fun selectionContinues_toNextText_skipsDisableSelection() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString(textContent),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
                DisableSelection {
                    BasicText(
                        AnnotatedString(textContent),
                        Modifier.fillMaxWidth().testTag(tag2),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    )
                }
            }
        }

        startSelection(tag1)
        dragHandleTo(Handle.SelectionEnd, offset = characterBox(tag2, 3).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = textContent.length, selectableId = 1)
    }

    @Test
    fun selectionHandle_remainsInComposition_whenTextIsOverflowed_clipped_softwrapDisabled() {
        createSelectionContainer {
            Column {
                BasicText(
                    AnnotatedString("$textContent ".repeat(100)),
                    Modifier.fillMaxWidth().testTag(tag1),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    softWrap = false,
                )
                DisableSelection {
                    BasicText(
                        textContent,
                        Modifier.fillMaxWidth(),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        softWrap = false,
                    )
                }
                BasicText(
                    AnnotatedString("$textContent ".repeat(100)),
                    Modifier.fillMaxWidth().testTag(tag2),
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                    softWrap = false,
                )
            }
        }

        startSelection(tag1)
        dragHandleTo(Handle.SelectionEnd, offset = characterBox(tag2, 3).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 4, selectableId = 2)
    }

    @Test
    fun allTextIsSelected_whenTextIsOverflowed_clipped_maxLines1() =
        with(rule.density) {
            val longText = "$textContent ".repeat(100)
            createSelectionContainer {
                Column {
                    BasicText(
                        AnnotatedString(longText),
                        Modifier.fillMaxWidth().testTag(tag1),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        maxLines = 1,
                    )
                }
            }

            startSelection(tag1)
            dragHandleTo(
                handle = Handle.SelectionEnd,
                offset = characterBox(tag1, 4).bottomRight + Offset(x = 0f, y = fontSize.toPx()),
            )

            assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(selection.value?.end, offset = longText.length, selectableId = 1)
        }

    @Test
    fun allTextIsSelected_whenTextIsOverflowed_ellipsized_maxLines1() =
        with(rule.density) {
            val longText = "$textContent ".repeat(100)
            createSelectionContainer {
                Column {
                    BasicText(
                        AnnotatedString(longText),
                        Modifier.fillMaxWidth().testTag(tag1),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            startSelection(tag1)
            dragHandleTo(
                handle = Handle.SelectionEnd,
                offset = characterBox(tag1, 4).bottomRight + Offset(x = 0f, y = fontSize.toPx()),
            )

            assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(selection.value?.end, offset = longText.length, selectableId = 1)
        }

    @Test
    fun selectionIncludes_noHeightText() {
        lateinit var clipboard: Clipboard
        createSelectionContainer {
            clipboard = LocalClipboard.current
            rememberCoroutineScope().launch(start = CoroutineStart.UNDISPATCHED) {
                clipboard.setClipEntry(
                    AnnotatedString("Clipboard content at start of test.").toClipEntry()
                )
            }
            Column {
                BasicText(text = "Hello", modifier = Modifier.fillMaxWidth().testTag(tag1))
                BasicText(text = "THIS SHOULD NOT CAUSE CRASH", modifier = Modifier.height(0.dp))
                BasicText(text = "World", modifier = Modifier.fillMaxWidth().testTag(tag2))
            }
        }

        startSelection(tag1)
        dragHandleTo(handle = Handle.SelectionEnd, offset = characterBox(tag2, 4).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 5, selectableId = 3)
    }

    @Test
    fun selectionIncludes_noWidthText() {
        lateinit var clipboard: Clipboard
        createSelectionContainer {
            clipboard = LocalClipboard.current
            rememberCoroutineScope().launch(start = CoroutineStart.UNDISPATCHED) {
                clipboard.setClipEntry(
                    AnnotatedString("Clipboard content at start of test.").toClipEntry()
                )
            }
            Column {
                BasicText(text = "Hello", modifier = Modifier.fillMaxWidth().testTag(tag1))
                BasicText(text = "THIS SHOULD NOT CAUSE CRASH", modifier = Modifier.width(0.dp))
                BasicText(text = "World", modifier = Modifier.fillMaxWidth().testTag(tag2))
            }
        }

        startSelection(tag1)
        dragHandleTo(handle = Handle.SelectionEnd, offset = characterBox(tag2, 4).bottomRight)

        assertAnchorInfo(selection.value?.start, offset = 0, selectableId = 1)
        assertAnchorInfo(selection.value?.end, offset = 5, selectableId = 3)
    }

    /**
     * Regression test for b/372053402 - Modifier.weight not working on SelectionContainer.
     *
     * Lay out a selection container with a weight next to a fixed size box in a row. We expect the
     * selection container to take up the remaining space in the row that the box does not use.
     */
    @Test
    fun selectionContainer_layoutWeightApplies() {
        val density = Density(1f)
        with(density) {
            val rowSize = 100
            val boxSize = 10
            val expectedSelConWidth = rowSize - boxSize

            val rowTag = "row"
            val boxTag = "box"
            val selConTag = "sel"
            val textTag = "text"

            rule.setContent {
                CompositionLocalProvider(LocalDensity provides density) {
                    Row(Modifier.size(rowSize.toDp()).testTag(rowTag)) {
                        SelectionContainer(Modifier.weight(1f).testTag(selConTag)) {
                            TestText("text ".repeat(100).trim(), Modifier.testTag(textTag))
                        }
                        Box(Modifier.size(boxSize.toDp()).testTag(boxTag))
                    }
                }
            }

            fun layoutCoordinatesForTag(tag: String): LayoutCoordinates =
                rule.onNodeWithTag(tag).fetchSemanticsNode().layoutInfo.coordinates

            val rowCoords = layoutCoordinatesForTag(rowTag)
            fun boundsInRow(tag: String): IntRect =
                rowCoords
                    .localBoundingBoxOf(layoutCoordinatesForTag(tag), clipBounds = false)
                    .roundToIntRect()

            val rowBounds = IntRect(IntOffset.Zero, rowCoords.size)
            val selConBounds = boundsInRow(selConTag)
            val textBounds = boundsInRow(textTag)
            val boxBounds = boundsInRow(boxTag)

            assertThatIntRect(rowBounds)
                .isEqualTo(top = 0, left = 0, right = rowSize, bottom = rowSize)
            assertThatIntRect(selConBounds)
                .isEqualTo(top = 0, left = 0, right = expectedSelConWidth, bottom = rowSize)
            assertThatIntRect(textBounds)
                .isEqualTo(top = 0, left = 0, right = expectedSelConWidth, bottom = rowSize)
            assertThatIntRect(boxBounds)
                .isEqualTo(top = 0, left = expectedSelConWidth, right = rowSize, bottom = boxSize)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun selection_doesCopy_whenCopyKeyEventSent() = runTest {
        lateinit var clipboardManager: ClipboardManager
        lateinit var clipboard: Clipboard
        createSelectionContainer {
            clipboardManager = LocalClipboardManager.current
            clipboard = LocalClipboard.current
            clipboardManager.setText(AnnotatedString("Clipboard content at start of test."))
            Column {
                BasicText(text = "ExpectedText", modifier = Modifier.fillMaxWidth().testTag(tag1))
            }
        }

        startSelection(tag1)

        rule.onNodeWithTag(tag1).performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.C)
            keyUp(Key.C)
            keyUp(Key.CtrlLeft)
        }

        rule.runOnIdle { assertThat(clipboardManager.getText()?.text).isEqualTo("ExpectedText") }
        assertThat(clipboard.getClipEntry()?.readText()).isEqualTo("ExpectedText")
    }

    @Test
    fun buttonWithTextClickInsideSelectionContainer() {
        var clickCounter = 0
        createSelectionContainer {
            Box(Modifier.clickable { clickCounter++ }) {
                BasicText(
                    text = "Button",
                    modifier = Modifier.align(Alignment.Center).testTag(tag1),
                )
            }
        }
        rule.onNodeWithTag(tag1, useUnmergedTree = true).performClick()
        rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
    }

    @Test
    fun buttonClickClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            createSelectionContainer {
                Column {
                    TestText(textContent)
                    TestButton(Modifier.size(50.dp).testTag(tag1), onClick = { clickCounter++ }) {
                        TestText("Button")
                    }
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Long Press "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performClick()

            // Assert.
            rule.runOnIdle { assertThat(selection.value).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    @Test
    fun buttonClickInsideDisableSelectionClearsSelection() =
        with(rule.density) {
            var clickCounter = 0
            createSelectionContainer {
                Column {
                    TestText(textContent)
                    DisableSelection {
                        TestButton(
                            Modifier.size(50.dp).testTag(tag1),
                            onClick = { clickCounter++ },
                        ) {
                            TestText("Button")
                        }
                    }
                }
            }
            val characterSize = fontSize.toPx()

            // Act. Long Press "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }
            rule.onNodeWithTag(tag1, useUnmergedTree = true).performClick()

            // Assert.
            rule.runOnIdle { assertThat(selection.value).isNull() }
            rule.runOnIdle { assertThat(clickCounter).isEqualTo(1) }
        }

    private fun startSelection(tag: String, offset: Int = 0) {
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        val boundingBox = textLayoutResult.getBoundingBox(offset)
        rule.onNodeWithTag(tag).performTouchInput { longClick(boundingBox.center) }
    }

    private fun dragHandleTo(handle: Handle, offset: Offset) {
        val position =
            rule
                .onNode(isSelectionHandle(handle))
                .fetchSemanticsNode()
                .config[SelectionHandleInfoKey]
                .position
        // selection handles are anchored at the bottom of a line.

        rule.onNode(isSelectionHandle(handle)).performTouchInput {
            val delta = offset - position
            down(center)
            movePastSlopBy(delta)
            up()
        }
    }

    /**
     * Moves the first pointer by [delta] past the touch slop threshold on each axis. If [delta] is
     * 0 on either axis it will stay 0.
     */
    private fun TouchInjectionScope.movePastSlopBy(delta: Offset) {
        val slop =
            Offset(
                x = viewConfiguration.touchSlop * delta.x.sign,
                y = viewConfiguration.touchSlop * delta.y.sign,
            )
        moveBy(delta + slop)
    }
}

private fun ComposeTestRule.rootWidth(): Dp = onRoot().getUnclippedBoundsInRoot().width

/**
 * Returns the text layout result caught by [SemanticsActions.GetTextLayoutResult] under this node.
 * Throws an AssertionError if the node has not defined GetTextLayoutResult semantics action.
 */
fun SemanticsNodeInteraction.fetchTextLayoutResult(): TextLayoutResult {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
    return textLayoutResults.first()
}
