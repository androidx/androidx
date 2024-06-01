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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionManagerGetSelectedRegionRectTest {
    private val rectSideLength = 15f

    private val emptyResultRect =
        Rect(
            left = Float.POSITIVE_INFINITY,
            top = Float.POSITIVE_INFINITY,
            right = Float.NEGATIVE_INFINITY,
            bottom = Float.NEGATIVE_INFINITY
        )

    @Test
    fun whenNoSelectables_returnsDefaultEmpty() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs = listOf(),
                containerCoordinates = FakeCoordinates()
            )
        assertThat(result).isEqualTo(emptyResultRect)
    }

    @Test
    fun whenOnlySelectableHasNoLayoutCoordinates_returnsDefaultEmpty() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesInLine(size = 10),
                            selection = getSelection(startOffset = 0, endOffset = 10),
                            rootOffset = null,
                        ),
                    ),
                containerCoordinates = FakeCoordinates()
            )
        assertThat(result).isEqualTo(emptyResultRect)
    }

    @Test
    fun whenCollapsedSelection_returnsDefaultEmpty() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesInLine(size = 10),
                            selection = getSelection(startOffset = 5, endOffset = 5),
                        ),
                    ),
                containerCoordinates = FakeCoordinates()
            )
        assertThat(result).isEqualTo(emptyResultRect)
    }

    @Test
    fun whenNoWrapping_rectIsShortAndWide() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesInLine(size = 10),
                            selection = getSelection(startOffset = 0, endOffset = 10),
                        ),
                    ),
                containerCoordinates = FakeCoordinates()
            )
        val expected = rectInUnits(left = 0, top = 0, right = 10, bottom = 1)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenWrapping_rectIsSlightlyTallerAndSkinnier() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesWrapped(size = 10, lineLength = 5),
                            selection = getSelection(startOffset = 0, endOffset = 10),
                        ),
                    ),
                containerCoordinates = FakeCoordinates(),
            )
        val expected = rectInUnits(left = 0, top = 0, right = 5, bottom = 2)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenRootOffsetsChangeEqually_resultDoesNotChange() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesWrapped(size = 10, lineLength = 5),
                            selection = getSelection(startOffset = 0, endOffset = 10),
                            rootOffset = offsetInUnits(x = 1, y = 1),
                        ),
                    ),
                containerCoordinates = FakeCoordinates(rootOffset = offsetInUnits(x = 1, y = 1)),
            )
        val expected = rectInUnits(left = 0, top = 0, right = 5, bottom = 2)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenDifferentCoordinates_resultChangesByTheDifference() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesWrapped(size = 10, lineLength = 5),
                            selection = getSelection(startOffset = 0, endOffset = 10),
                            rootOffset = offsetInUnits(x = 2, y = 2),
                        ),
                    ),
                containerCoordinates = FakeCoordinates(rootOffset = offsetInUnits(x = 1, y = 1)),
            )
        val expected =
            rectInUnits(left = 0, top = 0, right = 5, bottom = 2)
                .translate(offsetInUnits(x = 1, y = 1))
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenTwoSelectables_resultTracksBoth() {
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair(
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 0, endOffset = 5),
                        ),
                        getPair(
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 0, endOffset = 5),
                            rootOffset = offsetInUnits(x = 0, y = 1),
                        ),
                    ),
                containerCoordinates = FakeCoordinates(),
            )
        val expected = rectInUnits(left = 0, top = 0, right = 5, bottom = 2)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenMultipleSelectables_resultSurroundsSmallestPossibleRect() {
        // These subselections are nonsense, but are used
        // so that we can test various aspects of the function
        val result =
            getSelectedRegionRect(
                selectableSubSelectionPairs =
                    listOf(
                        getPair( // not used due to collapsed selection
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 4, endOffset = 4),
                            rootOffset = offsetInUnits(x = 0, y = 0),
                        ),
                        getPair( // coordinates has this indented 4 units
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 0, endOffset = 4),
                            rootOffset = offsetInUnits(x = 4, y = 1),
                        ),
                        getPair( // selection only contains offset 4
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 4, endOffset = 5),
                            rootOffset = offsetInUnits(x = 0, y = 2),
                        ),
                        getPair( // doesn't have coordinates, ignored
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 0, endOffset = 5),
                            rootOffset = null,
                        ),
                        getPair( // selection only contains offset 2
                            boundingBoxes = boundingBoxesInLine(size = 5),
                            selection = getSelection(startOffset = 2, endOffset = 3),
                            rootOffset = offsetInUnits(x = 0, y = 3),
                        ),
                    ),
                containerCoordinates = FakeCoordinates(),
            )
        val expected =
            rectInUnits(
                left = 2, // offset 2 from the 5th pair
                top = 1, // 2nd pair has first selected char
                right = 8, // 2nd pair has 4 unit indent and 4th char selected
                bottom = 4 // 5th pair has final selected char
            )
        assertThat(result).isEqualTo(expected)
    }

    private fun getPair(
        boundingBoxes: Map<Int, Rect>,
        selection: Selection,
        rootOffset: Offset? = Offset.Zero,
    ): Pair<Selectable, Selection> = fakeSelectable(boundingBoxes, rootOffset) to selection

    private fun boundingBoxesInLine(size: Int): Map<Int, Rect> =
        boundingBoxesWrapped(size, Int.MAX_VALUE)

    private fun boundingBoxesWrapped(size: Int, lineLength: Int): Map<Int, Rect> =
        (0 until size).associateWith {
            val lineOffset = it % lineLength
            val line = it / lineLength
            rectInUnits(left = lineOffset, top = line, right = lineOffset + 1, bottom = line + 1)
        }

    private fun rectInUnits(left: Int, top: Int, right: Int, bottom: Int): Rect =
        Rect(
            left = rectSideLength * left,
            top = rectSideLength * top,
            right = rectSideLength * right,
            bottom = rectSideLength * bottom,
        )

    private fun offsetInUnits(x: Int, y: Int): Offset =
        Offset(
            x = rectSideLength * x,
            y = rectSideLength * y,
        )

    private fun fakeSelectable(
        boundingBoxes: Map<Int, Rect>,
        rootOffset: Offset? = Offset.Zero,
    ): Selectable =
        FakeSelectable().apply {
            this.boundingBoxes = boundingBoxes
            this.layoutCoordinatesToReturn = rootOffset?.let { FakeCoordinates(it) }
        }

    private class FakeCoordinates(private val rootOffset: Offset = Offset.Zero) :
        LayoutCoordinates {
        override fun localToRoot(relativeToLocal: Offset): Offset = rootOffset + relativeToLocal

        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset
        ): Offset {
            val rootCoordinates = sourceCoordinates.localToRoot(relativeToSource)
            return rootCoordinates - rootOffset
        }

        // FAKES
        override val size: IntSize
            get() = fake()

        override val providedAlignmentLines: Set<AlignmentLine>
            get() = fake()

        override val parentLayoutCoordinates: LayoutCoordinates
            get() = fake()

        override val parentCoordinates: LayoutCoordinates
            get() = fake()

        override val isAttached: Boolean
            get() = fake()

        override fun windowToLocal(relativeToWindow: Offset): Offset = fake()

        override fun localToWindow(relativeToLocal: Offset): Offset = fake()

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean
        ): Rect = fake()

        override fun get(alignmentLine: AlignmentLine): Int = fake()

        private fun fake(): Nothing {
            throw UnsupportedOperationException("This fake does not support this.")
        }
    }
}
