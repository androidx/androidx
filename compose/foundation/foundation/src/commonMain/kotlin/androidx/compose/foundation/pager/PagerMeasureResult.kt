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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

internal class PagerMeasureResult(
    override val visiblePagesInfo: List<MeasuredPage>,
    override val pageSize: Int,
    override val pageSpacing: Int,
    override val afterContentPadding: Int,
    override val orientation: Orientation,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val reverseLayout: Boolean,
    override val outOfBoundsPageCount: Int,
    val firstVisiblePage: MeasuredPage?,
    val currentPage: MeasuredPage?,
    var currentPageOffsetFraction: Float,
    var firstVisiblePageScrollOffset: Int,
    var canScrollForward: Boolean,
    override val snapPosition: SnapPosition,
    measureResult: MeasureResult,
    /** True when extra remeasure is required. */
    val remeasureNeeded: Boolean,
    val extraPagesBefore: List<MeasuredPage> = emptyList(),
    val extraPagesAfter: List<MeasuredPage> = emptyList()
) : PagerLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset

    val canScrollBackward
        get() = (firstVisiblePage?.index ?: 0) != 0 || firstVisiblePageScrollOffset != 0

    /**
     * Tries to apply a scroll [delta] for this layout info. In some cases we can apply small
     * scroll deltas by just changing the offsets for each [visiblePagesInfo].
     * But we can only do so if after applying the delta we would not need to compose a new item
     * or dispose an item which is currently visible. In this case this function will not apply
     * the [delta] and return false.
     *
     * @return true if we can safely apply a passed scroll [delta] to this layout info.
     * If true is returned, only the placement phase is needed to apply new offsets.
     * If false is returned, it means we have to rerun the full measure phase to apply the [delta].
     */
    fun tryToApplyScrollWithoutRemeasure(delta: Int): Boolean {
        val pageSizeWithSpacing = pageSize + pageSpacing
        if (remeasureNeeded || visiblePagesInfo.isEmpty() || firstVisiblePage == null ||
            // applying this delta will change firstVisibleItem
            (firstVisiblePageScrollOffset - delta) !in 0 until pageSizeWithSpacing
        ) {
            return false
        }

        val deltaFraction = if (pageSizeWithSpacing != 0) {
            (delta / pageSizeWithSpacing.toFloat())
        } else {
            0.0f
        }

        val newCurrentPageOffsetFraction = currentPageOffsetFraction - deltaFraction
        if (currentPage == null ||
            //  applying this delta will change current page
            newCurrentPageOffsetFraction >= MaxPageOffset ||
            newCurrentPageOffsetFraction <= MinPageOffset
        ) {
            return false
        }

        val first =
            if (extraPagesBefore.isEmpty()) visiblePagesInfo.first() else extraPagesBefore.first()
        val last =
            if (extraPagesAfter.isEmpty()) visiblePagesInfo.last() else extraPagesAfter.last()

        val canApply = if (delta < 0) {
            // scrolling forward
            val deltaToFirstItemChange =
                first.offset + pageSizeWithSpacing - viewportStartOffset
            val deltaToLastItemChange =
                last.offset + pageSizeWithSpacing - viewportEndOffset
            minOf(deltaToFirstItemChange, deltaToLastItemChange) > -delta
        } else {
            // scrolling backward
            val deltaToFirstItemChange =
                viewportStartOffset - first.offset
            val deltaToLastItemChange =
                viewportEndOffset - last.offset
            minOf(deltaToFirstItemChange, deltaToLastItemChange) > delta
        }
        return if (canApply) {
            currentPageOffsetFraction -= deltaFraction
            firstVisiblePageScrollOffset -= delta
            visiblePagesInfo.fastForEach {
                it.applyScrollDelta(delta)
            }
            extraPagesBefore.fastForEach {
                it.applyScrollDelta(delta)
            }
            extraPagesAfter.fastForEach {
                it.applyScrollDelta(delta)
            }
            if (!canScrollForward && delta > 0) {
                // we scrolled backward, so now we can scroll forward.
                canScrollForward = true
            }
            true
        } else {
            false
        }
    }
}
