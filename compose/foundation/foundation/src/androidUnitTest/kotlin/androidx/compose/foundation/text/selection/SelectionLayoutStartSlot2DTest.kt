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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.selection.Direction.AFTER
import androidx.compose.foundation.text.selection.Direction.BEFORE
import androidx.compose.foundation.text.selection.Direction.ON
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextRange
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * The visual representation of the 2D tests looks like a box with for texts laid out like this:
 * ```
 *           LEFT  MIDDLE_LEFT MIDDLE MIDDLE_RIGHT  RIGHT
 *            |         |         |        |          |
 *  TOP           ┌───────────────────────────────┐
 *                │ ┌────────┐         ┌────────┐ │
 *  MIDDLE_TOP    │ │ text 1 │         │ text 2 │ │
 *                │ └────────┘         └────────┘ │
 *  MIDDLE        │                               │
 *                │ ┌────────┐         ┌────────┐ │
 *  MIDDLE_BOTTOM │ │ text 3 │         │ text 4 │ │
 *                │ └────────┘         └────────┘ │
 *  BOTTOM        └───────────────────────────────┘
 * ```
 * The labels on the x and y axis that will be referenced in the below tests.
 */
open class SelectionLayout2DTest {
    enum class TestHorizontal {
        LEFT, MIDDLE_LEFT, MIDDLE, MIDDLE_RIGHT, RIGHT
    }

    enum class TestVertical {
        TOP,
        MIDDLE_TOP,
        MIDDLE,
        MIDDLE_BOTTOM,
        BOTTOM
    }

    internal fun getDirectionsForX(horizontal: TestHorizontal): List<Direction> =
        when (horizontal) {
            TestHorizontal.LEFT -> listOf(BEFORE, BEFORE, BEFORE, BEFORE)
            TestHorizontal.MIDDLE_LEFT -> listOf(ON, BEFORE, ON, BEFORE)
            TestHorizontal.MIDDLE -> listOf(AFTER, BEFORE, AFTER, BEFORE)
            TestHorizontal.MIDDLE_RIGHT -> listOf(AFTER, ON, AFTER, ON)
            TestHorizontal.RIGHT -> listOf(AFTER, AFTER, AFTER, AFTER)
        }

    internal fun getDirectionsForY(vertical: TestVertical): List<Direction> =
        when (vertical) {
            TestVertical.TOP -> listOf(BEFORE, BEFORE, BEFORE, BEFORE)
            TestVertical.MIDDLE_TOP -> listOf(ON, ON, BEFORE, BEFORE)
            TestVertical.MIDDLE -> listOf(AFTER, AFTER, BEFORE, BEFORE)
            TestVertical.MIDDLE_BOTTOM -> listOf(AFTER, AFTER, ON, ON)
            TestVertical.BOTTOM -> listOf(AFTER, AFTER, AFTER, AFTER)
        }

    /** Calls [getTextFieldSelectionLayout] to get a [SelectionLayout]. */
    @OptIn(ExperimentalContracts::class)
    internal fun buildSelectionLayoutForTest(
        currentPosition: Offset = Offset(25f, 5f),
        previousHandlePosition: Offset = Offset.Unspecified,
        containerCoordinates: LayoutCoordinates = MockCoordinates(),
        isStartHandle: Boolean = false,
        previousSelection: Selection? = null,
        selectableIdOrderingComparator: Comparator<Long> = naturalOrder(),
        block: SelectionLayoutBuilder.() -> Unit,
    ): SelectionLayout {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return SelectionLayoutBuilder(
                currentPosition = currentPosition,
                previousHandlePosition = previousHandlePosition,
                containerCoordinates = containerCoordinates,
                isStartHandle = isStartHandle,
                previousSelection = previousSelection,
                selectableIdOrderingComparator = selectableIdOrderingComparator,
            )
            .run {
                block()
                assertNotNull(build())
            }
    }

    internal fun SelectionLayoutBuilder.appendInfoForTest(
        selectableId: Long = 1L,
        text: String = "hello",
        rawStartHandleOffset: Int = 0,
        startXHandleDirection: Direction = ON,
        startYHandleDirection: Direction = ON,
        rawEndHandleOffset: Int = 5,
        endXHandleDirection: Direction = ON,
        endYHandleDirection: Direction = ON,
        rawPreviousHandleOffset: Int = -1,
        rtlRanges: List<IntRange> = emptyList(),
        wordBoundaries: List<TextRange> = listOf(),
        lineBreaks: List<Int> = emptyList(),
    ): SelectableInfo {
        val layoutResult = getTextLayoutResultMock(
            text = text,
            rtlCharRanges = rtlRanges,
            wordBoundaries = wordBoundaries,
            lineBreaks = lineBreaks,
        )
        return appendInfo(
            selectableId = selectableId,
            rawStartHandleOffset = rawStartHandleOffset,
            startXHandleDirection = startXHandleDirection,
            startYHandleDirection = startYHandleDirection,
            rawEndHandleOffset = rawEndHandleOffset,
            endXHandleDirection = endXHandleDirection,
            endYHandleDirection = endYHandleDirection,
            rawPreviousHandleOffset = rawPreviousHandleOffset,
            textLayoutResult = layoutResult
        )
    }
}

@SmallTest
@RunWith(Parameterized::class)
class SelectionLayoutStartSlot2DTest(
    private val vertical: TestVertical,
    private val horizontal: TestHorizontal,
    private val expectedSlot: Int,
) : SelectionLayout2DTest() {
    companion object {
        @JvmStatic
        @Parameters(name = "verticalPosition={0}, horizontalPosition={1} expectedSlot={2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(TestVertical.TOP, TestHorizontal.LEFT, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.MIDDLE_LEFT, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.MIDDLE, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.MIDDLE_RIGHT, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.RIGHT, 0),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.LEFT, 0),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.MIDDLE_LEFT, 1),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.MIDDLE, 2),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.MIDDLE_RIGHT, 3),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.RIGHT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.LEFT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.MIDDLE_LEFT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.MIDDLE, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.MIDDLE_RIGHT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.RIGHT, 4),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.LEFT, 4),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.MIDDLE_LEFT, 5),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.MIDDLE, 6),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.MIDDLE_RIGHT, 7),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.RIGHT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.LEFT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.MIDDLE_LEFT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.MIDDLE, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.MIDDLE_RIGHT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.RIGHT, 8),
        )
    }

    // Test the start slot. end slot handle directions will always point to the 4th selectable.
    @Test
    fun startSlot2dTest() {
        val (xDirection1, xDirection2, xDirection3, xDirection4) = getDirectionsForX(horizontal)
        val (yDirection1, yDirection2, yDirection3, yDirection4) = getDirectionsForY(vertical)
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startXHandleDirection = xDirection1,
                startYHandleDirection = yDirection1,
                endXHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startXHandleDirection = xDirection2,
                startYHandleDirection = yDirection2,
                endXHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 3L,
                startXHandleDirection = xDirection3,
                startYHandleDirection = yDirection3,
                endXHandleDirection = AFTER,
                endYHandleDirection = ON,
            )
            appendInfoForTest(
                selectableId = 4L,
                startXHandleDirection = xDirection4,
                startYHandleDirection = yDirection4,
                endXHandleDirection = ON,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.endSlot).isEqualTo(7)
        assertThat(layout.startSlot).isEqualTo(expectedSlot)
    }
}

@SmallTest
@RunWith(Parameterized::class)
class SelectionLayoutEndSlot2DTest(
    private val vertical: TestVertical,
    private val horizontal: TestHorizontal,
    private val expectedSlot: Int,
) : SelectionLayout2DTest() {
    companion object {
        @JvmStatic
        @Parameters(name = "verticalPosition={0}, horizontalPosition={1} expectedSlot={2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(TestVertical.TOP, TestHorizontal.LEFT, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.MIDDLE_LEFT, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.MIDDLE, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.MIDDLE_RIGHT, 0),
            arrayOf(TestVertical.TOP, TestHorizontal.RIGHT, 0),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.LEFT, 0),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.MIDDLE_LEFT, 1),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.MIDDLE, 2),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.MIDDLE_RIGHT, 3),
            arrayOf(TestVertical.MIDDLE_TOP, TestHorizontal.RIGHT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.LEFT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.MIDDLE_LEFT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.MIDDLE, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.MIDDLE_RIGHT, 4),
            arrayOf(TestVertical.MIDDLE, TestHorizontal.RIGHT, 4),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.LEFT, 4),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.MIDDLE_LEFT, 5),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.MIDDLE, 6),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.MIDDLE_RIGHT, 7),
            arrayOf(TestVertical.MIDDLE_BOTTOM, TestHorizontal.RIGHT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.LEFT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.MIDDLE_LEFT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.MIDDLE, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.MIDDLE_RIGHT, 8),
            arrayOf(TestVertical.BOTTOM, TestHorizontal.RIGHT, 8),
        )
    }

    // Test the end slot. start slot handle directions will always point to the 1st selectable.
    @Test
    fun endSlot2dTest() {
        val (xDirection1, xDirection2, xDirection3, xDirection4) = getDirectionsForX(horizontal)
        val (yDirection1, yDirection2, yDirection3, yDirection4) = getDirectionsForY(vertical)
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startXHandleDirection = ON,
                startYHandleDirection = ON,
                endXHandleDirection = xDirection1,
                endYHandleDirection = yDirection1,
            )
            appendInfoForTest(
                selectableId = 2L,
                startXHandleDirection = BEFORE,
                startYHandleDirection = ON,
                endXHandleDirection = xDirection2,
                endYHandleDirection = yDirection2,
            )
            appendInfoForTest(
                selectableId = 3L,
                startXHandleDirection = ON,
                startYHandleDirection = BEFORE,
                endXHandleDirection = xDirection3,
                endYHandleDirection = yDirection3,
            )
            appendInfoForTest(
                selectableId = 4L,
                startXHandleDirection = BEFORE,
                startYHandleDirection = BEFORE,
                endXHandleDirection = xDirection4,
                endYHandleDirection = yDirection4,
            )
        }
        assertThat(layout.startSlot).isEqualTo(1)
        assertThat(layout.endSlot).isEqualTo(expectedSlot)
    }
}
