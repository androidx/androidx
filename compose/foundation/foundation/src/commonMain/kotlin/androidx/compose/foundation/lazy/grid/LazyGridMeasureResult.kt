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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.offsetOnMainAxis
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/**
 * The result of the measure pass for lazy list layout.
 */
internal class LazyGridMeasureResult(
    // properties defining the scroll position:
    /** The new first visible line of items.*/
    val firstVisibleLine: LazyGridMeasuredLine?,
    /** The new value for [LazyGridState.firstVisibleItemScrollOffset].*/
    var firstVisibleLineScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction.*/
    var canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass.*/
    var consumedScroll: Float,
    /** MeasureResult defining the layout.*/
    measureResult: MeasureResult,
    /** True when extra remeasure is required. */
    val remeasureNeeded: Boolean,
    // properties representing the info needed for LazyListLayoutInfo:
    /** see [LazyGridLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<LazyGridMeasuredItem>,
    /** see [LazyGridLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [LazyGridLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [LazyGridLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** see [LazyGridLayoutInfo.reverseLayout] */
    override val reverseLayout: Boolean,
    /** see [LazyGridLayoutInfo.orientation] */
    override val orientation: Orientation,
    /** see [LazyGridLayoutInfo.afterContentPadding] */
    override val afterContentPadding: Int,
    /** see [LazyGridLayoutInfo.mainAxisItemSpacing] */
    override val mainAxisItemSpacing: Int
) : LazyGridLayoutInfo, MeasureResult by measureResult {

    val canScrollBackward
        get() = (firstVisibleLine?.index ?: 0) != 0 || firstVisibleLineScrollOffset != 0

    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset

    /**
     * Tries to apply a scroll [delta] for this layout info. In some cases we can apply small
     * scroll deltas by just changing the offsets for each [visibleItemsInfo].
     * But we can only do so if after applying the delta we would not need to compose a new item
     * or dispose an item which is currently visible. In this case this function will not apply
     * the [delta] and return false.
     *
     * @return true if we can safely apply a passed scroll [delta] to this layout info.
     * If true is returned, only the placement phase is needed to apply new offsets.
     * If false is returned, it means we have to rerun the full measure phase to apply the [delta].
     */
    fun tryToApplyScrollWithoutRemeasure(delta: Int): Boolean {
        if (remeasureNeeded || visibleItemsInfo.isEmpty() || firstVisibleLine == null ||
            // applying this delta will change firstVisibleLineScrollOffset
            (firstVisibleLineScrollOffset - delta) !in
            0 until firstVisibleLine.mainAxisSizeWithSpacings
        ) {
            return false
        }
        val first = visibleItemsInfo.first()
        val last = visibleItemsInfo.last()
        if (first.nonScrollableItem || last.nonScrollableItem) {
            // non scrollable items require special handling.
            return false
        }
        val canApply = if (delta < 0) {
            // scrolling forward
            val deltaToFirstItemChange = first.offsetOnMainAxis(orientation) +
                first.mainAxisSizeWithSpacings - viewportStartOffset
            val deltaToLastItemChange = last.offsetOnMainAxis(orientation) +
                last.mainAxisSizeWithSpacings - viewportEndOffset
            minOf(deltaToFirstItemChange, deltaToLastItemChange) > -delta
        } else {
            // scrolling backward
            val deltaToFirstItemChange =
                viewportStartOffset - first.offsetOnMainAxis(orientation)
            val deltaToLastItemChange =
                viewportEndOffset - last.offsetOnMainAxis(orientation)
            minOf(deltaToFirstItemChange, deltaToLastItemChange) > delta
        }
        return if (canApply) {
            firstVisibleLineScrollOffset -= delta
            visibleItemsInfo.fastForEach {
                it.applyScrollDelta(delta)
            }
            consumedScroll = delta.toFloat()
            if (!canScrollForward && delta > 0) {
                // we scrolled backward, so now we can scroll forward
                canScrollForward = true
            }
            true
        } else {
            false
        }
    }
}
