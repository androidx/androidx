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

package androidx.compose.foundation.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

internal class PagerMeasureResult(
    override val visiblePagesInfo: List<MeasuredPage>,
    override val pageSize: Int,
    override val pageSpacing: Int,
    override val afterContentPadding: Int,
    override val orientation: Orientation,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val reverseLayout: Boolean,
    override val beyondViewportPageCount: Int,
    val firstVisiblePage: MeasuredPage?,
    val currentPage: MeasuredPage?,
    val currentPageOffsetFraction: Float,
    val firstVisiblePageScrollOffset: Int,
    val canScrollForward: Boolean,
    override val snapPosition: SnapPosition,
    private val measureResult: MeasureResult,
    /** True when extra remeasure is required. */
    val remeasureNeeded: Boolean,
    val extraPagesBefore: List<MeasuredPage> = emptyList(),
    val extraPagesAfter: List<MeasuredPage> = emptyList(),
    val coroutineScope: CoroutineScope,
    val density: Density,
    val childConstraints: Constraints,
) : PagerLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)

    override val beforeContentPadding: Int
        get() = -viewportStartOffset

    val canScrollBackward
        get() = (firstVisiblePage?.index ?: 0) != 0 || firstVisiblePageScrollOffset != 0

    /**
     * Creates a new layout info with applying a scroll [delta] for this layout info. In some cases
     * we can apply small scroll deltas by just changing the offsets for each [visiblePagesInfo].
     * But we can only do so if after applying the delta we would not need to compose a new item or
     * dispose an item which is currently visible. In this case this function will not apply the
     * [delta] and return null.
     *
     * @return new layout info if we can safely apply a passed scroll [delta] to this layout info.
     *   If If new layout info is returned, only the placement phase is needed to apply new offsets.
     *   If null is returned, it means we have to rerun the full measure phase to apply the [delta].
     */
    fun copyWithScrollDeltaWithoutRemeasure(delta: Int): PagerMeasureResult? {
        val pageSizeWithSpacing = pageSize + pageSpacing
        if (
            remeasureNeeded ||
                visiblePagesInfo.isEmpty() ||
                firstVisiblePage == null ||
                // applying this delta will change firstVisibleItem
                (firstVisiblePageScrollOffset - delta) !in 0 until pageSizeWithSpacing
        ) {
            return null
        }

        val deltaFraction =
            if (pageSizeWithSpacing != 0) {
                (delta / pageSizeWithSpacing.toFloat())
            } else {
                0.0f
            }

        val newCurrentPageOffsetFraction = currentPageOffsetFraction - deltaFraction
        if (
            currentPage == null ||
                //  applying this delta will change current page
                newCurrentPageOffsetFraction >= MaxPageOffset ||
                newCurrentPageOffsetFraction <= MinPageOffset
        ) {
            return null
        }

        val first = visiblePagesInfo.first()
        val last = visiblePagesInfo.last()

        val canApply =
            if (delta < 0) {
                // scrolling forward
                val deltaToFirstItemChange =
                    first.offset + pageSizeWithSpacing - viewportStartOffset
                val deltaToLastItemChange = last.offset + pageSizeWithSpacing - viewportEndOffset
                minOf(deltaToFirstItemChange, deltaToLastItemChange) > -delta
            } else {
                // scrolling backward
                val deltaToFirstItemChange = viewportStartOffset - first.offset
                val deltaToLastItemChange = viewportEndOffset - last.offset
                minOf(deltaToFirstItemChange, deltaToLastItemChange) > delta
            }
        return if (canApply) {
            visiblePagesInfo.fastForEach { it.applyScrollDelta(delta) }
            extraPagesBefore.fastForEach { it.applyScrollDelta(delta) }
            extraPagesAfter.fastForEach { it.applyScrollDelta(delta) }

            PagerMeasureResult(
                visiblePagesInfo = visiblePagesInfo,
                pageSize = pageSize,
                pageSpacing = pageSpacing,
                afterContentPadding = afterContentPadding,
                orientation = orientation,
                viewportStartOffset = viewportStartOffset,
                viewportEndOffset = viewportEndOffset,
                reverseLayout = reverseLayout,
                beyondViewportPageCount = beyondViewportPageCount,
                firstVisiblePage = firstVisiblePage,
                currentPage = currentPage,
                currentPageOffsetFraction = currentPageOffsetFraction - deltaFraction,
                firstVisiblePageScrollOffset = firstVisiblePageScrollOffset - delta,
                canScrollForward =
                    canScrollForward ||
                        delta > 0, // we scrolled backward, so now we can scroll forward
                snapPosition = snapPosition,
                measureResult = measureResult,
                remeasureNeeded = remeasureNeeded,
                extraPagesBefore = extraPagesBefore,
                extraPagesAfter = extraPagesAfter,
                coroutineScope = coroutineScope,
                density = density,
                childConstraints = childConstraints,
            )
        } else {
            null
        }
    }
}
