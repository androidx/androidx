/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.test.filters.SmallTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.text.TextRange
import androidx.ui.text.style.TextDirection
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class SelectionTest {
    @Test
    fun anchorInfo_constructor() {
        val coordinates = PxPosition(x = 2.px, y = 3.px)
        val direction = TextDirection.Ltr
        val offset = 0
        val layoutCoordinates: LayoutCoordinates = mock()

        val anchor = Selection.AnchorInfo(
            coordinates = coordinates,
            direction = direction,
            offset = offset,
            layoutCoordinates = layoutCoordinates
        )

        assertThat(anchor.coordinates).isEqualTo(coordinates)
        assertThat(anchor.direction).isEqualTo(direction)
        assertThat(anchor.offset).isEqualTo(offset)
        assertThat(anchor.layoutCoordinates).isEqualTo(layoutCoordinates)
    }

    @Test
    fun selection_constructor() {
        val startOffset = 0
        val endOffset = 6
        val startAnchor = Selection.AnchorInfo(
            coordinates = PxPosition(x = 2.px, y = 3.px),
            direction = TextDirection.Ltr,
            offset = startOffset,
            layoutCoordinates = null
        )
        val endAnchor = Selection.AnchorInfo(
            coordinates = PxPosition(x = 300.px, y = 3.px),
            direction = TextDirection.Ltr,
            offset = endOffset,
            layoutCoordinates = null
        )
        val handleCrossed = false

        val selection = Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = handleCrossed
        )

        assertThat(selection.start).isEqualTo(startAnchor)
        assertThat(selection.end).isEqualTo(endAnchor)
        assertThat(selection.handlesCrossed).isEqualTo(handleCrossed)
    }

    @Test
    fun selection_merge_handles_not_cross() {
        val startOffset1 = 9
        val endOffset1 = 20
        val startCoordinates1 = PxPosition(20.px, 20.px)
        val endCoordinates1 = PxPosition(50.px, 50.px)
        val layoutCoordinates1: LayoutCoordinates = mock()
        val startAnchor1 = Selection.AnchorInfo(
            coordinates = startCoordinates1,
            direction = TextDirection.Ltr,
            offset = startOffset1,
            layoutCoordinates = layoutCoordinates1
        )
        val endAnchor1 = Selection.AnchorInfo(
            coordinates = endCoordinates1,
            direction = TextDirection.Ltr,
            offset = endOffset1,
            layoutCoordinates = null
        )
        val selection1 = Selection(
            start = startAnchor1,
            end = endAnchor1,
            handlesCrossed = false
        )
        val startOffset2 = 0
        val endOffset2 = 30
        val startCoordinates2 = PxPosition(0.px, 20.px)
        val endCoordinates2 = PxPosition(50.px, 300.px)
        val layoutCoordinates2: LayoutCoordinates = mock()
        val startAnchor2 = Selection.AnchorInfo(
            coordinates = startCoordinates2,
            direction = TextDirection.Ltr,
            offset = startOffset2,
            layoutCoordinates = null
        )
        val endAnchor2 = Selection.AnchorInfo(
            coordinates = endCoordinates2,
            direction = TextDirection.Ltr,
            offset = endOffset2,
            layoutCoordinates = layoutCoordinates2
        )
        val selection2 = Selection(
            start = startAnchor2,
            end = endAnchor2,
            handlesCrossed = false
        )

        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(startOffset1)
        assertThat(selection.end.offset).isEqualTo(endOffset2)
        assertThat(selection.start.coordinates).isEqualTo(startCoordinates1)
        assertThat(selection.end.coordinates).isEqualTo(endCoordinates2)
        assertThat(selection.start.layoutCoordinates).isEqualTo(layoutCoordinates1)
        assertThat(selection.end.layoutCoordinates).isEqualTo(layoutCoordinates2)
        assertThat(selection.handlesCrossed).isFalse()
    }

    @Test
    fun selection_merge_handles_cross() {
        val startOffset1 = 20
        val endOffset1 = 9
        val startCoordinates1 = PxPosition(50.px, 50.px)
        val endCoordinates1 = PxPosition(20.px, 20.px)
        val layoutCoordinates1: LayoutCoordinates = mock()
        val startAnchor1 = Selection.AnchorInfo(
            coordinates = startCoordinates1,
            direction = TextDirection.Ltr,
            offset = startOffset1,
            layoutCoordinates = null
        )
        val endAnchor1 = Selection.AnchorInfo(
            coordinates = endCoordinates1,
            direction = TextDirection.Ltr,
            offset = endOffset1,
            layoutCoordinates = layoutCoordinates1
        )
        val selection1 = Selection(
            start = startAnchor1,
            end = endAnchor1,
            handlesCrossed = true
        )
        val startOffset2 = 30
        val endOffset2 = 0
        val startCoordinates2 = PxPosition(50.px, 300.px)
        val endCoordinates2 = PxPosition(0.px, 20.px)
        val layoutCoordinates2: LayoutCoordinates = mock()
        val startAnchor2 = Selection.AnchorInfo(
            coordinates = startCoordinates2,
            direction = TextDirection.Ltr,
            offset = startOffset2,
            layoutCoordinates = layoutCoordinates2
        )
        val endAnchor2 = Selection.AnchorInfo(
            coordinates = endCoordinates2,
            direction = TextDirection.Ltr,
            offset = endOffset2,
            layoutCoordinates = null
        )
        val selection2 = Selection(
            start = startAnchor2,
            end = endAnchor2,
            handlesCrossed = true
        )

        val selection = selection1.merge(selection2)

        assertThat(selection.start.offset).isEqualTo(startOffset2)
        assertThat(selection.end.offset).isEqualTo(endOffset1)
        assertThat(selection.start.coordinates).isEqualTo(startCoordinates2)
        assertThat(selection.end.coordinates).isEqualTo(endCoordinates1)
        assertThat(selection.start.layoutCoordinates).isEqualTo(layoutCoordinates2)
        assertThat(selection.end.layoutCoordinates).isEqualTo(layoutCoordinates1)
        assertThat(selection.handlesCrossed).isTrue()
    }

    @Test
    fun selection_toTextRange_handles_not_cross() {
        val startOffset = 0
        val endOffset = 6
        val startAnchor = Selection.AnchorInfo(
            coordinates = PxPosition(x = 2.px, y = 3.px),
            direction = TextDirection.Ltr,
            offset = startOffset,
            layoutCoordinates = null
        )
        val endAnchor = Selection.AnchorInfo(
            coordinates = PxPosition(x = 300.px, y = 3.px),
            direction = TextDirection.Ltr,
            offset = endOffset,
            layoutCoordinates = null
        )
        val selection = Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = false
        )

        val textRange = selection.toTextRange()

        assertThat(textRange).isEqualTo(TextRange(startOffset, endOffset))
    }

    @Test
    fun selection_toTextRange_handles_cross() {
        val startOffset = 6
        val endOffset = 0
        val startAnchor = Selection.AnchorInfo(
            coordinates = PxPosition(x = 300.px, y = 3.px),
            direction = TextDirection.Ltr,
            offset = startOffset,
            layoutCoordinates = null
        )
        val endAnchor = Selection.AnchorInfo(
            coordinates = PxPosition(x = 2.px, y = 3.px),
            direction = TextDirection.Ltr,
            offset = endOffset,
            layoutCoordinates = null
        )
        val selection = Selection(
            start = startAnchor,
            end = endAnchor,
            handlesCrossed = false
        )

        val textRange = selection.toTextRange()

        assertThat(textRange).isEqualTo(TextRange(startOffset, endOffset))
    }
}
