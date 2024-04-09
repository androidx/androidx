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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.ExpectedText.EITHER
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.ExpectedText.FIRST
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.ExpectedText.SECOND
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestHorizontal.CENTER
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestHorizontal.LEFT
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestHorizontal.RIGHT
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.ABOVE
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.BELOW
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.NO_OVERLAP_BELONGS_TO_FIRST
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.NO_OVERLAP_BELONGS_TO_SECOND
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.ON_FIRST
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.ON_SECOND
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.OVERLAP_BELONGS_TO_FIRST
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.OVERLAP_BELONGS_TO_SECOND
import androidx.compose.foundation.text.selection.gestures.MultiTextMinTouchBoundsSelectionGesturesTest.TestVertical.OVERLAP_EQUIDISTANT
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.longClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
internal class MultiTextMinTouchBoundsSelectionGesturesTest(
    private val horizontal: TestHorizontal,
    private val vertical: TestVertical,
    private val expectedText: ExpectedText,
) : AbstractSelectionGesturesTest() {
    // dp and sp is the same with our density
    private val dpLen = 20.dp
    private val spLen = 20.sp

    /**
     * When two 20x20 texts are stacked with 20 space between,
     * we want the touch targets to overlap a little for this test,
     * so we pick a minTouchTarget size of an additional 12 on each side.
     *
     * With this setup, on the y-axis:
     *   * 0..20 is the first text
     *   * 40..60 is the second text
     *   * -12..32 is the first minTouchTarget
     *   * 28..72 is the second minTouchTarget
     *
     * Given the above:
     *   * 21..27 belongs solely to the first text
     *   * 29 overlaps both, but is closer to the first text
     *   * 30 overlaps both, and is equidistant to both
     *   * 31 overlaps both, but is closer to the second text
     *   * 32..39 belongs solely to the second text
     */
    private val touchTargetDpLen = dpLen + 12.dp * 2

    enum class TestHorizontal(
        val x: Float,
        /** The x-value we would coerce to in order to get the x coordinate onto a box. */
        val coercedX: Float,
    ) {
        LEFT(x = -6f, coercedX = 1f),
        CENTER(x = 10f, coercedX = 10f),
        RIGHT(x = 26f, coercedX = 19f)
    }

    enum class TestVertical(
        val y: Float,
        /** The y-value we would coerce to in order to get the y coordinate onto a box. */
        val coercedY: Float,
    ) {
        ABOVE(y = -6f, coercedY = 1f),
        ON_FIRST(y = 10f, coercedY = 10f),
        NO_OVERLAP_BELONGS_TO_FIRST(y = 25f, coercedY = 19f),
        OVERLAP_BELONGS_TO_FIRST(y = 29f, coercedY = 19f),
        OVERLAP_EQUIDISTANT(y = 30f, coercedY = 19f),
        OVERLAP_BELONGS_TO_SECOND(y = 31f, coercedY = 41f),
        NO_OVERLAP_BELONGS_TO_SECOND(y = 35f, coercedY = 41f),
        ON_SECOND(y = 50f, coercedY = 50f),
        BELOW(y = 66f, coercedY = 59f);
    }

    enum class ExpectedText(val selectableId: Long?) {
        FIRST(1L),
        SECOND(2L),
        EITHER(null),
    }

    override val pointerAreaTag = "selectionContainer"
    private val text = "A"
    private val textStyle = TextStyle(fontSize = spLen, fontFamily = fontFamily)
    private val minTouchTargetSize = DpSize(touchTargetDpLen, touchTargetDpLen)
    private val testViewConfiguration =
        TestViewConfiguration(minimumTouchTargetSize = minTouchTargetSize)

    private val selection = mutableStateOf<Selection?>(null)

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.testTag(pointerAreaTag)
        ) {
            CompositionLocalProvider(LocalViewConfiguration provides testViewConfiguration) {
                Column(verticalArrangement = Arrangement.spacedBy(dpLen)) {
                    repeat(2) { BasicText(text = text, style = textStyle) }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "horizontal={0}, vertical={1} expectedId={2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(LEFT, ABOVE, FIRST),
            arrayOf(LEFT, ON_FIRST, FIRST),
            arrayOf(LEFT, NO_OVERLAP_BELONGS_TO_FIRST, FIRST),
            arrayOf(LEFT, OVERLAP_BELONGS_TO_FIRST, FIRST),
            arrayOf(LEFT, OVERLAP_EQUIDISTANT, EITHER),
            arrayOf(LEFT, OVERLAP_BELONGS_TO_SECOND, SECOND),
            arrayOf(LEFT, NO_OVERLAP_BELONGS_TO_SECOND, SECOND),
            arrayOf(LEFT, ON_SECOND, SECOND),
            arrayOf(LEFT, BELOW, SECOND),
            arrayOf(CENTER, ABOVE, FIRST),
            arrayOf(CENTER, ON_FIRST, FIRST),
            arrayOf(CENTER, NO_OVERLAP_BELONGS_TO_FIRST, FIRST),
            arrayOf(CENTER, OVERLAP_BELONGS_TO_FIRST, FIRST),
            arrayOf(CENTER, OVERLAP_EQUIDISTANT, EITHER),
            arrayOf(CENTER, OVERLAP_BELONGS_TO_SECOND, SECOND),
            arrayOf(CENTER, NO_OVERLAP_BELONGS_TO_SECOND, SECOND),
            arrayOf(CENTER, ON_SECOND, SECOND),
            arrayOf(CENTER, BELOW, SECOND),
            arrayOf(RIGHT, ABOVE, FIRST),
            arrayOf(RIGHT, ON_FIRST, FIRST),
            arrayOf(RIGHT, NO_OVERLAP_BELONGS_TO_FIRST, FIRST),
            arrayOf(RIGHT, OVERLAP_BELONGS_TO_FIRST, FIRST),
            arrayOf(RIGHT, OVERLAP_EQUIDISTANT, EITHER),
            arrayOf(RIGHT, OVERLAP_BELONGS_TO_SECOND, SECOND),
            arrayOf(RIGHT, NO_OVERLAP_BELONGS_TO_SECOND, SECOND),
            arrayOf(RIGHT, ON_SECOND, SECOND),
            arrayOf(RIGHT, BELOW, SECOND),
        )
    }

    @Test
    fun minTouchTargetSelectionGestureTest() = runTest {
        performTouchGesture { longClick(Offset(horizontal.x, vertical.y)) }
    }

    // Regression test for b/325307463
    @Test
    fun dragIntoMinTouchTargetSelectionGestureTest() = runTest {
        performTouchGesture {
            longPress(Offset(horizontal.coercedX, vertical.coercedY))
            // The crash involved a quick drag from on the text to off the text
            // causing a race of some state not being set before the drag is executed,
            // so we want to force the moveTo immediately after the long press finishes.
            moveTo(Offset(horizontal.x, vertical.y), delayMillis = 0L)
        }
    }

    fun runTest(block: () -> Unit) {
        block()

        val expectedSelectableId = expectedText.selectableId
        if (expectedSelectableId == null) {
            // verify something is selected
            assertThat(selection).isNotNull()
        } else {
            assertSelectedSelectableIs(expectedSelectableId)
        }
    }

    private fun assertSelectedSelectableIs(selectableId: Long) {
        val expectedSelection = Selection(
            start = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 0, selectableId),
            end = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 1, selectableId),
            handlesCrossed = false,
        )
        assertThat(selection.value).isEqualTo(expectedSelection)
    }
}
