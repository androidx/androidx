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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class MultiText2dSelectionGesturesTest : AbstractSelectionGesturesTest() {

    // 3 x 3 grid of texts
    private val sideLength = 3

    override val pointerAreaTag = "selectionContainer"
    val line = "Test Text"
    val text = "$line\n$line"

    private val selection = mutableStateOf<Selection?>(null)

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.testTag(pointerAreaTag)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                repeat(sideLength) { i ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(sideLength) { j ->
                            BasicText(
                                text = text,
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = fontSize,
                                ),
                                modifier = Modifier
                                    .padding(24.dp)
                                    .testTag("${i * sideLength + j + 1}"),
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun dragUpperLeftText() {
        dragTest(
            dragPosition = characterPosition(1, 6),
            selectableId = 1,
            offset = 5,
            crossed = true
        )
    }

    @Test
    fun dragUpperCenterText() {
        dragTest(
            dragPosition = characterPosition(2, 6),
            selectableId = 2,
            offset = 5,
            crossed = true
        )
    }

    @Test
    fun dragUpperRightText() {
        dragTest(
            dragPosition = characterPosition(3, 6),
            selectableId = 3,
            offset = 6,
            crossed = true
        )
    }

    @Test
    fun dragLeftText() {
        dragTest(
            dragPosition = characterPosition(4, 6),
            selectableId = 4,
            offset = 5,
            crossed = true
        )
    }

    @Test
    fun dragSameText() {
        dragTest(
            dragPosition = characterPosition(5, 10),
            selectableId = 5,
            offset = 10,
            crossed = false
        )
    }

    @Test
    fun dragRightText() {
        dragTest(
            dragPosition = characterPosition(6, 5),
            selectableId = 6,
            offset = 9,
            crossed = false
        )
    }

    @Test
    fun dragLowerLeftText() {
        dragTest(
            dragPosition = characterPosition(7, 5),
            selectableId = 7,
            offset = 5,
            crossed = false
        )
    }

    @Test
    fun dragLowerCenterText() {
        dragTest(
            dragPosition = characterPosition(8, 5),
            selectableId = 8,
            offset = 9,
            crossed = false
        )
    }

    @Test
    fun dragLowerRightText() {
        dragTest(
            dragPosition = characterPosition(9, 5),
            selectableId = 9,
            offset = 9,
            crossed = false
        )
    }

    @Test
    fun dragTopContainer() {
        dragTest(
            dragPosition = topEnd,
            selectableId = 1,
            offset = 0,
            crossed = true
        )
    }

    @Test
    fun dragBetweenFirstAndSecondRow() {
        dragTest(
            dragPosition = betweenSelectables(2, 5),
            selectableId = 4,
            offset = 0,
            crossed = true
        )
    }

    @Test
    fun dragBetweenSecondAndThirdRow() {
        dragTest(
            dragPosition = betweenSelectables(5, 8),
            selectableId = 6,
            offset = 19,
            crossed = false
        )
    }

    @Test
    fun dragBottomContainer() {
        dragTest(
            dragPosition = bottomStart,
            selectableId = 9,
            offset = 19,
            crossed = false
        )
    }

    @Test
    fun dragLeftContainer() {
        dragTest(
            // the offset should fall between lines,
            // nudge it up so the position is on the upper line
            dragPosition = centerStart.nudge(yDirection = VerticalDirection.UP),
            selectableId = 4,
            offset = 0,
            crossed = true
        )
    }

    @Test
    fun dragBetweenLeftAndCenterTexts() {
        dragTest(
            dragPosition = betweenSelectables(4, 5).nudge(yDirection = VerticalDirection.UP),
            selectableId = 5,
            offset = 0,
            crossed = true
        )
    }

    @Test
    fun dragBetweenCenterAndRightTexts() {
        dragTest(
            dragPosition = betweenSelectables(5, 6).nudge(yDirection = VerticalDirection.UP),
            selectableId = 5,
            offset = 9,
            crossed = false
        )
    }

    @Test
    fun dragRightContainer() {
        dragTest(
            dragPosition = centerEnd.nudge(yDirection = VerticalDirection.UP),
            selectableId = 6,
            offset = 9,
            crossed = false
        )
    }

    private fun dragTest(
        dragPosition: Offset,
        selectableId: Int,
        offset: Int,
        crossed: Boolean,
    ) {
        performTouchGesture {
            longPress(characterPosition(5, 6))
        }

        assertSelection(
            startSelectableId = 5,
            startOffset = 5,
            endSelectableId = 5,
            endOffset = 9,
            handlesCrossed = false
        )

        touchDragTo(dragPosition)

        assertSelection(
            startSelectableId = 5,
            startOffset = 5,
            endSelectableId = selectableId,
            endOffset = offset,
            handlesCrossed = crossed
        )

        performTouchGesture {
            up()
        }

        assertSelection(
            startSelectableId = 5,
            startOffset = 5,
            endSelectableId = selectableId,
            endOffset = offset,
            handlesCrossed = crossed
        )
    }

    // selectableIds are
    //  1  2  3
    //  4  5  6
    //  7  8  9
    private fun characterPosition(selectableId: Int, offset: Int): Offset {
        val tag = "$selectableId"
        val pointerAreaPosition =
            rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().positionInRoot
        val nodePosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset)
            .translate(nodePosition - pointerAreaPosition)
            .centerLeft
            .nudge(HorizontalDirection.END)
    }

    private fun betweenSelectables(selectableId1: Int, selectableId2: Int): Offset {
        val pointerAreaPosition =
            rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().positionInRoot

        fun nodeCenter(selectableId: Int): Offset {
            val tag = "$selectableId"
            val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
            return node.boundsInRoot.center - pointerAreaPosition
        }

        return lerp(nodeCenter(selectableId1), nodeCenter(selectableId2), 0.5f)
    }

    private fun assertSelection(
        startSelectableId: Int,
        startOffset: Int,
        endSelectableId: Int,
        endOffset: Int,
        handlesCrossed: Boolean,
    ) {
        assertThat(selection.value)
            .isEqualTo(
                Selection(
                    start = Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = startOffset,
                        selectableId = startSelectableId.toLong()
                    ),
                    end = Selection.AnchorInfo(
                        direction = ResolvedTextDirection.Ltr,
                        offset = endOffset,
                        selectableId = endSelectableId.toLong()
                    ),
                    handlesCrossed = handlesCrossed,
                )
            )
    }
}
