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
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class MultiTextRowRegressionTest : AbstractSelectionGesturesTest() {

    override val pointerAreaTag = "selectionContainer"
    val line = "Test Text"

    private val selection = mutableStateOf<Selection?>(null)

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.testTag(pointerAreaTag),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    BasicText(
                        text = line,
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        modifier = Modifier.padding(top = 8.dp).testTag("1"),
                    )
                    BasicText(
                        text = "$line\n$line\n$line",
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        modifier = Modifier.testTag("2"),
                    )
                    BasicText(
                        text = line,
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        modifier = Modifier.padding(top = 24.dp).testTag("3"),
                    )
                }
            }
        }
    }

    @Test
    fun selectMiddle_moveEndHandleToLeft() {
        performTouchGesture { longPress(characterPosition(2, 0)) }

        assertSelection(
            startOffset = 0,
            startSelectableId = 2,
            endSelectableId = 2,
            endOffset = 4,
            handlesCrossed = false,
        )

        withHandlePressed(Handle.SelectionEnd) { moveHandleTo(characterPosition(2, 19)) }

        assertSelection(
            startOffset = 0,
            startSelectableId = 2,
            endSelectableId = 2,
            endOffset = 19,
            handlesCrossed = false,
        )

        // now move the handle to the top left
        withHandlePressed(Handle.SelectionEnd) { moveHandleTo(characterPosition(1, 0)) }

        // no crash is good enough but make sure that the selection is also an expected value
        assertSelection(
            startOffset = 0,
            startSelectableId = 2,
            endSelectableId = 1,
            endOffset = 0,
            handlesCrossed = true,
        )
    }

    private fun characterPosition(selectableId: Int, offset: Int): Offset {
        val tag = "$selectableId"
        val pointerAreaPosition =
            rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().positionInRoot
        val nodePosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        return textLayoutResult
            .getBoundingBox(offset)
            .translate(nodePosition - pointerAreaPosition)
            .centerLeft
            .nudge(HorizontalDirection.END)
    }

    private fun assertSelection(
        startOffset: Int,
        startSelectableId: Int,
        endSelectableId: Int,
        endOffset: Int,
        handlesCrossed: Boolean,
    ) {
        assertThat(selection.value)
            .isEqualTo(
                Selection(
                    start =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = startOffset,
                            selectableId = startSelectableId.toLong(),
                        ),
                    end =
                        Selection.AnchorInfo(
                            direction = ResolvedTextDirection.Ltr,
                            offset = endOffset,
                            selectableId = endSelectableId.toLong(),
                        ),
                    handlesCrossed = handlesCrossed,
                )
            )
    }
}
