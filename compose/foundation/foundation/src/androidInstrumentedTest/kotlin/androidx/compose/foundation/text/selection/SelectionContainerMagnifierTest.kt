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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.RequiresDevice
import androidx.test.filters.SdkSuppress
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
internal class SelectionContainerMagnifierTest : AbstractSelectionMagnifierTests() {

    @Composable
    override fun TestContent(
        text: String,
        modifier: Modifier,
        style: TextStyle,
        onTextLayout: (TextLayoutResult) -> Unit,
        maxLines: Int
    ) {
        SelectionContainer(modifier) {
            BasicText(text, style = style, onTextLayout = onTextLayout, maxLines = maxLines)
        }
    }

    @RequiresDevice // b/264702195
    @Test
    fun magnifier_goesToLastLine_whenSelectionEndDraggedBelowTextBounds_whenTextOverflowed() {
        checkMagnifierAsHandleGoesOutOfBoundsUsingMaxLines(Handle.SelectionEnd)
    }

    @RequiresDevice // b/264702195
    @Test
    fun magnifier_hidden_whenSelectionStartDraggedBelowTextBounds_whenTextOverflowed() {
        checkMagnifierAsHandleGoesOutOfBoundsUsingMaxLines(Handle.SelectionStart)
    }

    // Regression - magnifier on an empty RTL Text should appear on right side, not left
    @Test
    fun magnifier_whenRtlLayoutWithEmptyLine_positionedOnRightSide() {
        val nonEmptyTag = "nonEmpty"
        val emptyTag = "empty"
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SelectionContainer(
                    modifier = Modifier
                        // Center the text to give the magnifier lots of room to move.
                        .fillMaxSize()
                        .wrapContentSize()
                        .testTag(tag),
                ) {
                    Column(Modifier.width(IntrinsicSize.Max)) {
                        BasicText(
                            text = "בבבב",
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(nonEmptyTag)
                        )
                        BasicText(
                            text = "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(emptyTag)
                        )
                    }
                }
            }
        }

        val placedPosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot
        fun assertMagnifierAt(expected: Offset) {
            val actual = getMagnifierCenterOffset(rule, requireSpecified = true) - placedPosition
            assertThatOffset(actual).equalsWithTolerance(expected)
        }

        // start selection at first character
        val firstPressOffset = rule.onNodeWithTag(nonEmptyTag).fetchTextLayoutResult()
            .getBoundingBox(0).centerRight - Offset(1f, 0f)

        rule.onNodeWithTag(tag).performTouchInput {
            longPress(firstPressOffset)
        }
        rule.waitForIdle()
        assertMagnifierAt(firstPressOffset)

        val emptyTextPosition = rule.onNodeWithTag(emptyTag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(emptyTag).fetchTextLayoutResult()
        val emptyTextCenterY =
            textLayoutResult.size.height / 2f + emptyTextPosition.y - placedPosition.y
        val secondOffset = Offset(firstPressOffset.x, emptyTextCenterY)
        rule.onNodeWithTag(tag).performTouchInput {
            moveTo(secondOffset)
        }
        rule.waitForIdle()

        val expectedX = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.width
        assertMagnifierAt(Offset(expectedX, emptyTextCenterY))
    }
}
