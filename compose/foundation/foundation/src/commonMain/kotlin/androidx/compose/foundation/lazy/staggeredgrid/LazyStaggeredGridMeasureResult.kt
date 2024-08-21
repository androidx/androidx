/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

/**
 * Information about layout state of individual item in lazy staggered grid.
 *
 * @see [LazyStaggeredGridLayoutInfo]
 */
sealed interface LazyStaggeredGridItemInfo {
    /** Relative offset from the start of the staggered grid. */
    val offset: IntOffset

    /** Index of the item. */
    val index: Int

    /**
     * Column (for vertical staggered grids) or row (for horizontal staggered grids) that the item
     * is in.
     */
    val lane: Int

    /** Key of the item passed in [LazyStaggeredGridScope.items] */
    val key: Any

    /**
     * Item size in pixels. If item contains multiple layouts, the size is calculated as a sum of
     * their sizes.
     */
    val size: IntSize

    /** The content type of the item which was passed to the item() or items() function. */
    val contentType: Any?
}

/**
 * Information about layout state of lazy staggered grids. Can be retrieved from
 * [LazyStaggeredGridState.layoutInfo].
 */
// todo(b/182882362): expose more information about layout state
sealed interface LazyStaggeredGridLayoutInfo {
    /** Orientation of the staggered grid. */
    val orientation: Orientation

    /** The list of [LazyStaggeredGridItemInfo] per each visible item ordered by index. */
    val visibleItemsInfo: List<LazyStaggeredGridItemInfo>

    /** The total count of items passed to staggered grid. */
    val totalItemsCount: Int

    /** Layout viewport (content + content padding) size in pixels. */
    val viewportSize: IntSize

    /**
     * The start offset of the layout's viewport in pixels. You can think of it as a minimum offset
     * which would be visible. Can be negative if non-zero [beforeContentPadding] was applied as the
     * content displayed in the content padding area is still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport in pixels. You can think of it as a maximum offset
     * which would be visible. It is the size of the lazy grid layout minus [beforeContentPadding].
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportEndOffset: Int

    /** Content padding in pixels applied before the items in scroll direction. */
    val beforeContentPadding: Int

    /** Content padding in pixels applied after the items in scroll direction. */
    val afterContentPadding: Int

    /** The spacing between items in scroll direction. */
    val mainAxisItemSpacing: Int
}

internal fun LazyStaggeredGridLayoutInfo.findVisibleItem(
    itemIndex: Int
): LazyStaggeredGridItemInfo? {
    if (visibleItemsInfo.isEmpty()) {
        return null
    }

    if (itemIndex !in visibleItemsInfo.first().index..visibleItemsInfo.last().index) {
        return null
    }

    val index = visibleItemsInfo.binarySearch { it.index - itemIndex }
    return visibleItemsInfo.getOrNull(index)
}

internal class LazyStaggeredGridMeasureResult(
    val firstVisibleItemIndices: IntArray,
    val firstVisibleItemScrollOffsets: IntArray,
    val consumedScroll: Float,
    val measureResult: MeasureResult,
    val canScrollForward: Boolean,
    val isVertical: Boolean,
    /** True when extra remeasure is required. */
    val remeasureNeeded: Boolean,
    val slots: LazyStaggeredGridSlots,
    val spanProvider: LazyStaggeredGridSpanProvider,
    val density: Density,
    override val totalItemsCount: Int,
    override val visibleItemsInfo: List<LazyStaggeredGridMeasuredItem>,
    override val viewportSize: IntSize,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val beforeContentPadding: Int,
    override val afterContentPadding: Int,
    override val mainAxisItemSpacing: Int,
    val coroutineScope: CoroutineScope
) : LazyStaggeredGridLayoutInfo, MeasureResult by measureResult {

    val canScrollBackward
        // only scroll backward if the first item is not on screen or fully visible
        get() = !(firstVisibleItemIndices[0] == 0 && firstVisibleItemScrollOffsets[0] <= 0)

    override val orientation: Orientation =
        if (isVertical) Orientation.Vertical else Orientation.Horizontal

    /**
     * Creates a new layout info with applying a scroll [delta] for this layout info. In some cases
     * we can apply small scroll deltas by just changing the offsets for each [visibleItemsInfo].
     * But we can only do so if after applying the delta we would not need to compose a new item or
     * dispose an item which is currently visible. In this case this function will not apply the
     * [delta] and return null.
     *
     * @return new layout info if we can safely apply a passed scroll [delta] to this layout info.
     *   If If new layout info is returned, only the placement phase is needed to apply new offsets.
     *   If null is returned, it means we have to rerun the full measure phase to apply the [delta].
     */
    fun copyWithScrollDeltaWithoutRemeasure(delta: Int): LazyStaggeredGridMeasureResult? {
        if (
            remeasureNeeded ||
                visibleItemsInfo.isEmpty() ||
                firstVisibleItemIndices.isEmpty() ||
                firstVisibleItemScrollOffsets.isEmpty()
        ) {
            return null
        }
        val mainAxisMax = viewportEndOffset - afterContentPadding
        visibleItemsInfo.fastForEach {
            // non scrollable items require special handling.
            if (
                it.nonScrollableItem ||
                    // applying delta will make this item to cross the 0th pixel, this means
                    // that firstVisibleItemIndices will change. we require a remeasure for it.
                    it.mainAxisOffset <= 0 != it.mainAxisOffset + delta <= 0
            ) {
                return null
            }
            if (it.mainAxisOffset <= viewportStartOffset) {
                // we compare with viewportStartOffset in order to know when the item will became
                // not visible anymore, and with 0 to know when the firstVisibleItemIndices will
                // change. when we have a beforeContentPadding those values will not be the same.
                val canApply =
                    if (delta < 0) { // scrolling forward
                        it.mainAxisOffset + it.mainAxisSizeWithSpacings - viewportStartOffset >
                            -delta
                    } else { // scrolling backward
                        viewportStartOffset - it.mainAxisOffset > delta
                    }
                if (!canApply) return null
            }
            // item is partially visible at the bottom.
            if (it.mainAxisOffset + it.mainAxisSizeWithSpacings >= mainAxisMax) {
                val canApply =
                    if (delta < 0) { // scrolling forward
                        it.mainAxisOffset + it.mainAxisSizeWithSpacings - viewportEndOffset > -delta
                    } else { // scrolling backward
                        viewportEndOffset - it.mainAxisOffset > delta
                    }
                if (!canApply) return null
            }
        }
        visibleItemsInfo.fastForEach { it.applyScrollDelta(delta) }
        return LazyStaggeredGridMeasureResult(
            firstVisibleItemIndices = firstVisibleItemIndices,
            firstVisibleItemScrollOffsets =
                IntArray(firstVisibleItemScrollOffsets.size) { index ->
                    firstVisibleItemScrollOffsets[index] - delta
                },
            consumedScroll = delta.toFloat(),
            measureResult = measureResult,
            canScrollForward =
                canScrollForward || delta > 0, // we scrolled backward, so now we can scroll forward
            isVertical = isVertical,
            remeasureNeeded = remeasureNeeded,
            slots = slots,
            spanProvider = spanProvider,
            density = density,
            totalItemsCount = totalItemsCount,
            visibleItemsInfo = visibleItemsInfo,
            viewportSize = viewportSize,
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = mainAxisItemSpacing,
            coroutineScope = coroutineScope,
        )
    }
}

private val EmptyArray = IntArray(0)

internal val EmptyLazyStaggeredGridLayoutInfo =
    LazyStaggeredGridMeasureResult(
        firstVisibleItemIndices = EmptyArray,
        firstVisibleItemScrollOffsets = EmptyArray,
        consumedScroll = 0f,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0
                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            },
        canScrollForward = false,
        isVertical = false,
        visibleItemsInfo = emptyList(),
        totalItemsCount = 0,
        remeasureNeeded = false,
        viewportSize = IntSize.Zero,
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        beforeContentPadding = 0,
        afterContentPadding = 0,
        mainAxisItemSpacing = 0,
        slots = LazyStaggeredGridSlots(EmptyArray, EmptyArray),
        spanProvider = LazyStaggeredGridSpanProvider(MutableIntervalList()),
        density = Density(1f),
        coroutineScope = CoroutineScope(EmptyCoroutineContext)
    )
