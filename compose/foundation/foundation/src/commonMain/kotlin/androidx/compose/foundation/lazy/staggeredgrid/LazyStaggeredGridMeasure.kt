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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimation.Companion.NotInitialized
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItem
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItemProvider
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLaneInfo.Companion.LaneFullSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLaneInfo.Companion.LaneUnset
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMaxOfOrDefault
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope

private const val DebugLoggingEnabled = false

private inline fun <T> withDebugLogging(
    scope: LazyLayoutMeasureScope,
    block: LazyLayoutMeasureScope.() -> T,
): T {
    val result =
        if (DebugLoggingEnabled) {
            try {
                println("╭──────{ measure start }───────────")
                with(scope, block)
            } finally {
                println("╰──────{ measure done }────────────")
            }
        } else {
            with(scope, block)
        }
    return result
}

private fun Array<ArrayDeque<LazyStaggeredGridMeasuredItem>>.debugRender(): String =
    if (DebugLoggingEnabled) {
        @Suppress("ListIterator") map { items -> items.map { it.index } }.toString()
    } else {
        ""
    }

private inline fun debugLog(message: () -> String) {
    if (DebugLoggingEnabled) {
        println("│ - ${message()}")
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutMeasureScope.measureStaggeredGrid(
    state: LazyStaggeredGridState,
    pinnedItems: List<Int>,
    itemProvider: LazyStaggeredGridItemProvider,
    resolvedSlots: LazyStaggeredGridSlots,
    constraints: Constraints,
    isVertical: Boolean,
    reverseLayout: Boolean,
    contentOffset: IntOffset,
    mainAxisAvailableSize: Int,
    mainAxisSpacing: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    coroutineScope: CoroutineScope,
    isInLookaheadScope: Boolean,
    isLookingAhead: Boolean,
    approachLayoutInfo: LazyStaggeredGridLayoutInfo?,
    graphicsContext: GraphicsContext,
): LazyStaggeredGridMeasureResult {
    val context =
        LazyStaggeredGridMeasureContext(
            state = state,
            pinnedItems = pinnedItems,
            itemProvider = itemProvider,
            resolvedSlots = resolvedSlots,
            constraints = constraints,
            isVertical = isVertical,
            contentOffset = contentOffset,
            mainAxisAvailableSize = mainAxisAvailableSize,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            reverseLayout = reverseLayout,
            mainAxisSpacing = mainAxisSpacing,
            measureScope = this,
            coroutineScope = coroutineScope,
            isInLookaheadScope = isInLookaheadScope,
            isLookingAhead = isLookingAhead,
            approachLayoutInfo = approachLayoutInfo,
            graphicsContext = graphicsContext,
        )

    val initialItemIndices: IntArray
    val initialItemOffsets: IntArray

    // ensure scroll position is up to date
    val firstVisibleIndices =
        state.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, state.scrollPosition.indices)
    val firstVisibleOffsets = state.scrollPosition.scrollOffsets

    initialItemIndices =
        if (firstVisibleIndices.size == context.laneCount) {
            firstVisibleIndices
        } else {
            // Grid got resized (or we are in a initial state)
            // Adjust indices accordingly
            context.laneInfo.reset()
            IntArray(context.laneCount).apply {
                // Try to adjust indices in case grid got resized
                for (lane in indices) {
                    this[lane] =
                        if (
                            lane < firstVisibleIndices.size &&
                                firstVisibleIndices[lane] != LaneUnset
                        ) {
                            firstVisibleIndices[lane]
                        } else {
                            if (lane == 0) {
                                0
                            } else {
                                maxInRange(SpanRange(0, lane)) + 1
                            }
                        }
                    // Ensure spans are updated to be in correct range
                    context.laneInfo.setLane(this[lane], lane)
                }
            }
        }
    initialItemOffsets =
        if (firstVisibleOffsets.size == context.laneCount) {
            firstVisibleOffsets
        } else {
            // Grid got resized (or we are in a initial state)
            // Adjust offsets accordingly
            IntArray(context.laneCount).apply {
                // Adjust offsets to match previously set ones
                for (lane in indices) {
                    this[lane] =
                        if (lane < firstVisibleOffsets.size) {
                            firstVisibleOffsets[lane]
                        } else {
                            if (lane == 0) 0 else this[lane - 1]
                        }
                }
            }
        }

    return context.measure(
        initialScrollDelta =
            state.scrollToBeConsumed(isLookingAhead = isLookingAhead).fastRoundToInt(),
        initialItemIndices = initialItemIndices,
        initialItemOffsets = initialItemOffsets,
        canRestartMeasure = true,
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridMeasureContext(
    val state: LazyStaggeredGridState,
    val pinnedItems: List<Int>,
    val itemProvider: LazyStaggeredGridItemProvider,
    val resolvedSlots: LazyStaggeredGridSlots,
    val constraints: Constraints,
    val isVertical: Boolean,
    val measureScope: LazyLayoutMeasureScope,
    val mainAxisAvailableSize: Int,
    val contentOffset: IntOffset,
    val beforeContentPadding: Int,
    val afterContentPadding: Int,
    val reverseLayout: Boolean,
    val mainAxisSpacing: Int,
    val coroutineScope: CoroutineScope,
    val isInLookaheadScope: Boolean,
    val isLookingAhead: Boolean,
    val approachLayoutInfo: LazyStaggeredGridLayoutInfo?,
    val graphicsContext: GraphicsContext,
) {
    val measuredItemProvider =
        object :
            LazyStaggeredGridMeasureProvider(
                isVertical = isVertical,
                itemProvider = itemProvider,
                measureScope = measureScope,
                resolvedSlots = resolvedSlots,
            ) {
            override fun createItem(
                index: Int,
                lane: Int,
                span: Int,
                key: Any,
                contentType: Any?,
                placeables: List<Placeable>,
                constraints: Constraints,
            ) =
                LazyStaggeredGridMeasuredItem(
                    index = index,
                    key = key,
                    placeables = placeables,
                    isVertical = isVertical,
                    spacing = mainAxisSpacing,
                    lane = lane,
                    span = span,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    contentType = contentType,
                    animator = state.itemAnimator,
                    constraints = constraints,
                )
        }

    val laneInfo = state.laneInfo

    val laneCount = resolvedSlots.sizes.size

    fun LazyStaggeredGridItemProvider.isFullSpan(itemIndex: Int): Boolean =
        spanProvider.isFullSpan(itemIndex)

    fun LazyStaggeredGridItemProvider.getSpanRange(itemIndex: Int, lane: Int): SpanRange {
        val isFullSpan = spanProvider.isFullSpan(itemIndex)
        val span = if (isFullSpan) laneCount else 1
        val targetLane = if (isFullSpan) 0 else lane
        return SpanRange(targetLane, span)
    }

    inline val SpanRange.isFullSpan: Boolean
        get() = size != 1

    inline val SpanRange.laneInfo: Int
        get() = if (isFullSpan) LaneFullSpan else start
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyStaggeredGridMeasureContext.measure(
    initialScrollDelta: Int,
    initialItemIndices: IntArray,
    initialItemOffsets: IntArray,
    canRestartMeasure: Boolean,
): LazyStaggeredGridMeasureResult {
    withDebugLogging(measureScope) {
        val itemCount = itemProvider.itemCount

        if (itemCount <= 0 || laneCount == 0) {
            var layoutWidth = constraints.minWidth
            var layoutHeight = constraints.minHeight
            state.itemAnimator.onMeasured(
                consumedScroll = 0,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight,
                positionedItems = mutableListOf(),
                keyIndexMap = measuredItemProvider.keyIndexMap,
                itemProvider = measuredItemProvider,
                laneCount = laneCount,
                isVertical = isVertical,
                isLookingAhead = isLookingAhead,
                hasLookaheadOccurred = isInLookaheadScope,
                layoutMinOffset = 0,
                layoutMaxOffset = 0,
                coroutineScope = coroutineScope,
                graphicsContext = graphicsContext,
            )

            if (!isLookingAhead) {
                val disappearingItemsSize = state.itemAnimator.minSizeToFitDisappearingItems
                if (disappearingItemsSize != IntSize.Zero) {
                    layoutWidth = constraints.constrainWidth(disappearingItemsSize.width)
                    layoutHeight = constraints.constrainHeight(disappearingItemsSize.height)
                }
            }
            return LazyStaggeredGridMeasureResult(
                firstVisibleItemIndices = initialItemIndices,
                firstVisibleItemScrollOffsets = initialItemOffsets,
                consumedScroll = 0f,
                measureResult = layout(layoutWidth, layoutHeight) {},
                canScrollForward = false,
                isVertical = isVertical,
                visibleItemsInfo = emptyList(),
                remeasureNeeded = false,
                totalItemsCount = itemCount,
                viewportSize = IntSize(constraints.minWidth, constraints.minHeight),
                viewportStartOffset = -beforeContentPadding,
                viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                mainAxisItemSpacing = mainAxisSpacing,
                slots = resolvedSlots,
                spanProvider = itemProvider.spanProvider,
                density = this,
                scrollBackAmount = 0f,
                coroutineScope = coroutineScope,
            )
        }

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = initialScrollDelta

        val firstItemIndices = initialItemIndices.copyOf()
        val firstItemOffsets = initialItemOffsets.copyOf()

        // will be set to true if we composed some items only to know their size and apply scroll,
        // while in the end this item will not end up in the visible viewport. we will need an
        // extra remeasure in order to dispose such items.
        var remeasureNeeded = false

        // update spans in case item count is lower than before
        ensureIndicesInRange(firstItemIndices, itemCount)

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        firstItemOffsets.offsetBy(-scrollDelta)

        // this will contain all the MeasuredItems representing the visible items
        val measuredItems = Array(laneCount) { ArrayDeque<LazyStaggeredGridMeasuredItem>(16) }

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
        firstItemOffsets.offsetBy(-beforeContentPadding)

        fun hasSpaceBeforeFirst(): Boolean {
            for (lane in firstItemIndices.indices) {
                val itemIndex = firstItemIndices[lane]
                val itemOffset = firstItemOffsets[lane]

                if (itemOffset < maxOf(-mainAxisSpacing, 0) && itemIndex > 0) {
                    return true
                }
            }

            return false
        }

        debugLog {
            "up from indices: ${firstItemIndices.toList()}, offsets: ${firstItemOffsets.toList()}"
        }

        var laneToCheckForGaps = -1

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstItemOffset should be visible. compose them and update
        // firstItemOffsets
        while (hasSpaceBeforeFirst()) {
            // staggered grid always keeps item index increasing top to bottom
            // the first item that should contain something before it must have the largest index
            // among the rest
            val laneIndex = firstItemIndices.indexOfMaxValue()
            val itemIndex = firstItemIndices[laneIndex]

            // other lanes might have smaller offsets than the one chosen above, which indicates
            // incorrect measurement (e.g. item was deleted or it changed size)
            // correct this by offsetting affected lane back to match currently chosen offset
            for (i in firstItemOffsets.indices) {
                if (
                    firstItemIndices[i] != firstItemIndices[laneIndex] &&
                        firstItemOffsets[i] < firstItemOffsets[laneIndex]
                ) {
                    // If offset of the lane is smaller than currently chosen lane,
                    // offset the lane to be where current value of the chosen index is.
                    firstItemOffsets[i] = firstItemOffsets[laneIndex]
                }
            }

            val previousItemIndex = findPreviousItemIndex(itemIndex, laneIndex)
            if (previousItemIndex < 0) {
                laneToCheckForGaps = laneIndex
                break
            }

            val spanRange = itemProvider.getSpanRange(previousItemIndex, laneIndex)
            laneInfo.setLane(previousItemIndex, spanRange.laneInfo)
            val measuredItem =
                measuredItemProvider.getAndMeasure(index = previousItemIndex, span = spanRange)

            val offset = firstItemOffsets.maxInRange(spanRange)
            val gaps = if (spanRange.isFullSpan) laneInfo.getGaps(previousItemIndex) else null
            spanRange.forEach { lane ->
                firstItemIndices[lane] = previousItemIndex
                val gap = if (gaps == null) 0 else gaps[lane]
                val newOffset = offset + measuredItem.mainAxisSizeWithSpacings + gap
                firstItemOffsets[lane] = newOffset
                // newOffset here is negative. if this item will not be inside of the viewport
                // this means we composed this item, but will not need it.
                if (mainAxisAvailableSize + newOffset <= 0) {
                    remeasureNeeded = true
                }
            }
        }
        debugLog { "up done. measured items: ${measuredItems.debugRender()}" }

        fun misalignedStart(referenceLane: Int): Boolean {
            // If we scrolled past the first item in the lane, we have a point of reference
            // to re-align items.

            // Case 1: Each lane has laid out all items, but offsets do no match
            for (lane in firstItemIndices.indices) {
                val misalignedOffsets =
                    findPreviousItemIndex(firstItemIndices[lane], lane) == LaneUnset &&
                        firstItemOffsets[lane] != firstItemOffsets[referenceLane]

                if (misalignedOffsets) {
                    return true
                }
            }
            // Case 2: Some lanes are still missing items, and there's no space left to place them
            for (lane in firstItemIndices.indices) {
                val moreItemsInOtherLanes =
                    findPreviousItemIndex(firstItemIndices[lane], lane) != LaneUnset &&
                        firstItemOffsets[lane] >= firstItemOffsets[referenceLane]

                if (moreItemsInOtherLanes) {
                    return true
                }
            }
            // Case 3: the first item is in the wrong lane (it should always be in
            // the first one)
            val firstItemLane = laneInfo.getLane(0)
            return firstItemLane != 0 && firstItemLane != LaneUnset && firstItemLane != LaneFullSpan
        }

        // define min offset (currently includes beforeContentPadding)
        val minOffset = -beforeContentPadding

        // we scrolled backward, but there were not enough items to fill the start. this means
        // some amount of scroll should be left over
        if (firstItemOffsets[0] < minOffset) {
            val notConsumedScrollDelta = minOffset - firstItemOffsets[0]
            firstItemOffsets.offsetBy(minOffset - firstItemOffsets[0])
            scrollDelta -= notConsumedScrollDelta
            debugLog { "up, correcting scroll delta from ${firstItemOffsets[0]} to $minOffset" }
        }

        // neutralize previously added start padding as we stopped filling the before content
        // padding
        firstItemOffsets.offsetBy(beforeContentPadding)

        laneToCheckForGaps =
            if (laneToCheckForGaps == -1) firstItemIndices.indexOf(0) else laneToCheckForGaps

        // re-check if columns are aligned after measure
        if (laneToCheckForGaps != -1) {
            val lane = laneToCheckForGaps
            if (misalignedStart(lane) && canRestartMeasure) {
                laneInfo.reset()
                return measure(
                    initialScrollDelta = scrollDelta,
                    initialItemIndices = IntArray(firstItemIndices.size) { -1 },
                    initialItemOffsets = IntArray(firstItemOffsets.size) { firstItemOffsets[lane] },
                    canRestartMeasure = false,
                )
            }
        }

        // start measuring down from first item indices/offsets decided above to ensure correct
        // arrangement.
        // this means we are calling measure second time on items previously measured in this
        // function, but LazyLayout caches them, so no overhead.
        val currentItemIndices = firstItemIndices.copyOf()
        val currentItemOffsets = IntArray(firstItemOffsets.size) { -firstItemOffsets[it] }

        val minVisibleOffset = minOffset + mainAxisSpacing
        val maxOffset = (mainAxisAvailableSize + afterContentPadding).coerceAtLeast(0)

        debugLog {
            "down current, indices: ${currentItemIndices.toList()}, " +
                "offsets: ${currentItemOffsets.toList()}"
        }

        // current item should be pointing to the index of previously measured item below,
        // as lane assignments must be decided based on size and offset of all previous items
        // this loop makes sure to measure items that were initially passed to the current item
        // indices with correct item order
        var initialItemsMeasured = 0
        var initialLaneToMeasure = currentItemIndices.indexOfMinValue()
        while (initialLaneToMeasure != -1 && initialItemsMeasured < laneCount) {
            val itemIndex = currentItemIndices[initialLaneToMeasure]
            val laneIndex = initialLaneToMeasure

            initialLaneToMeasure = currentItemIndices.indexOfMinValue(minBound = itemIndex)
            initialItemsMeasured++

            if (itemIndex < 0) continue

            val spanRange = itemProvider.getSpanRange(itemIndex, laneIndex)
            val measuredItem = measuredItemProvider.getAndMeasure(itemIndex, spanRange)

            laneInfo.setLane(itemIndex, spanRange.laneInfo)
            val offset = currentItemOffsets.maxInRange(spanRange)
            spanRange.forEach { lane ->
                currentItemOffsets[lane] = offset + measuredItem.mainAxisSizeWithSpacings
                currentItemIndices[lane] = itemIndex
                measuredItems[lane].addLast(measuredItem)
            }

            // item is not visible if both start and end bounds are outside of the visible range.
            if (
                offset < minVisibleOffset && currentItemOffsets[spanRange.start] <= minVisibleOffset
            ) {
                // We scrolled past measuredItem, and it is not visible anymore. We measured it
                // for correct positioning of other items, but there's no need to place it.
                // Mark it as not visible and filter below.
                measuredItem.isVisible = false
                remeasureNeeded = true
            }

            if (spanRange.isFullSpan) {
                // full span items overwrite other slots if we measure it here, so skip measuring
                // the rest of the slots
                initialItemsMeasured = laneCount
            }
        }

        debugLog { "current filled, measured: ${measuredItems.debugRender()}" }
        debugLog {
            "down from indices: ${currentItemIndices.toList()}, " +
                "offsets: ${currentItemOffsets.toList()}"
        }

        // then composing visible items forward until we fill the whole viewport.
        // we want to have at least one item in measuredItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (
            currentItemOffsets.any {
                it < maxOffset || it <= 0 // filling beforeContentPadding area
            } || measuredItems.all { it.isEmpty() }
        ) {
            val currentLaneIndex = currentItemOffsets.indexOfMinValue()
            val previousItemIndex = currentItemIndices.max()
            val itemIndex = previousItemIndex + 1

            if (itemIndex >= itemCount) {
                break
            }

            val spanRange = itemProvider.getSpanRange(itemIndex, currentLaneIndex)

            laneInfo.setLane(itemIndex, spanRange.laneInfo)
            val measuredItem = measuredItemProvider.getAndMeasure(itemIndex, spanRange)

            val offset = currentItemOffsets.maxInRange(spanRange)
            val gaps =
                if (spanRange.isFullSpan) {
                    laneInfo.getGaps(itemIndex) ?: IntArray(laneCount)
                } else {
                    null
                }
            spanRange.forEach { lane ->
                if (gaps != null) {
                    gaps[lane] = offset - currentItemOffsets[lane]
                }
                currentItemIndices[lane] = itemIndex
                currentItemOffsets[lane] = offset + measuredItem.mainAxisSizeWithSpacings
                measuredItems[lane].addLast(measuredItem)
            }
            laneInfo.setGaps(itemIndex, gaps)

            // item is not visible if both start and end bounds are outside of the visible range.
            if (
                offset < minVisibleOffset && currentItemOffsets[spanRange.start] <= minVisibleOffset
            ) {
                // We scrolled past measuredItem, and it is not visible anymore. We measured it
                // for correct positioning of other items, but there's no need to place it.
                // Mark it as not visible and filter below.
                measuredItem.isVisible = false
            }
        }

        debugLog { "down done. measured items: ${measuredItems.debugRender()}" }

        // some measured items are offscreen, remove them from the list and adjust indices/offsets
        for (laneIndex in measuredItems.indices) {
            val laneItems = measuredItems[laneIndex]

            while (laneItems.size > 1 && !laneItems.first().isVisible) {
                val item = laneItems.removeFirst()
                val gaps = if (item.span != 1) laneInfo.getGaps(item.index) else null
                firstItemOffsets[laneIndex] -=
                    item.mainAxisSizeWithSpacings + if (gaps == null) 0 else gaps[laneIndex]
            }

            firstItemIndices[laneIndex] = laneItems.firstOrNull()?.index ?: LaneUnset
        }

        // ensure no spacing for the last item
        if (currentItemIndices.any { it == itemCount - 1 }) {
            currentItemOffsets.offsetBy(-mainAxisSpacing)
        }

        debugLog { "removed invisible items: ${measuredItems.debugRender()}" }
        debugLog {
            "back up, indices: ${firstItemIndices.toList()}, " +
                "offsets: ${firstItemOffsets.toList()}"
        }

        val preScrollBackScrollDelta = scrollDelta
        // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
        // lets try to scroll back if we have enough items before firstVisibleItemIndex.
        if (currentItemOffsets.all { it < mainAxisAvailableSize }) {
            val maxOffsetLane = currentItemOffsets.indexOfMaxValue()
            val toScrollBack = mainAxisAvailableSize - currentItemOffsets[maxOffsetLane]
            firstItemOffsets.offsetBy(-toScrollBack)
            currentItemOffsets.offsetBy(toScrollBack)

            var gapDetected = false
            while (firstItemOffsets.any { it < beforeContentPadding }) {
                // We choose the minimum offset value and try to put items on top.
                // Note that it is different from initial pass up where we selected largest index
                // instead. The reason is that we already distributed items on downward pass and
                // gap would be incorrect if those are moved.
                var laneIndex = firstItemOffsets.indexOfMinValue()
                val nextLaneIndex = firstItemIndices.indexOfMaxValue()

                if (laneIndex != nextLaneIndex) {
                    if (firstItemOffsets[laneIndex] == firstItemOffsets[nextLaneIndex]) {
                        // If the offsets are the same, it means that there's no gap here.
                        // In this case, we should choose the lane where item would go normally.
                        laneIndex = nextLaneIndex
                    } else {
                        // If min offset lane doesn't have largest value, it means items are
                        // misaligned.
                        // The correct thing here is to restart measure. We will measure up to the
                        // end and restart measure from there after this pass.
                        gapDetected = true
                    }
                }

                val currentIndex =
                    if (firstItemIndices[laneIndex] == -1) {
                        itemCount
                    } else {
                        firstItemIndices[laneIndex]
                    }

                val previousIndex = findPreviousItemIndex(currentIndex, laneIndex)

                if (previousIndex < 0) {
                    if ((gapDetected || misalignedStart(laneIndex)) && canRestartMeasure) {
                        laneInfo.reset()
                        return measure(
                            initialScrollDelta = scrollDelta,
                            initialItemIndices = IntArray(firstItemIndices.size) { -1 },
                            initialItemOffsets =
                                IntArray(firstItemOffsets.size) { firstItemOffsets[laneIndex] },
                            canRestartMeasure = false,
                        )
                    }
                    break
                }

                val spanRange = itemProvider.getSpanRange(previousIndex, laneIndex)
                laneInfo.setLane(previousIndex, spanRange.laneInfo)
                val measuredItem =
                    measuredItemProvider.getAndMeasure(index = previousIndex, spanRange)

                val offset = firstItemOffsets.maxInRange(spanRange)
                val gaps = if (spanRange.isFullSpan) laneInfo.getGaps(previousIndex) else null
                spanRange.forEach { lane ->
                    if (firstItemOffsets[lane] != offset) {
                        // Some items below fully spanned item don't match it exactly. We skip over,
                        // but this should be corrected through remeasure.
                        gapDetected = true
                    }

                    measuredItems[lane].addFirst(measuredItem)
                    firstItemIndices[lane] = previousIndex
                    val gap = if (gaps == null) 0 else gaps[lane]
                    firstItemOffsets[lane] = offset + measuredItem.mainAxisSizeWithSpacings + gap
                }
            }

            // If incorrectly offset lanes were detected before, restart measure from the current
            // point. Incorrectly offset items will be redistributed to the correct lanes on the
            // downward pass.
            if (gapDetected && canRestartMeasure) {
                laneInfo.reset()
                return measure(
                    initialScrollDelta = scrollDelta,
                    initialItemIndices = firstItemIndices,
                    initialItemOffsets = firstItemOffsets,
                    canRestartMeasure = false,
                )
            }

            scrollDelta += toScrollBack

            val minOffsetLane = firstItemOffsets.indexOfMinValue()
            if (firstItemOffsets[minOffsetLane] < 0) {
                val offsetValue = firstItemOffsets[minOffsetLane]
                scrollDelta += offsetValue
                currentItemOffsets.offsetBy(offsetValue)
                firstItemOffsets.offsetBy(-offsetValue)
            }
        }

        debugLog { "measured: ${measuredItems.debugRender()}" }
        debugLog {
            "first indices: ${firstItemIndices.toList()}, offsets: ${firstItemOffsets.toList()}"
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough items to fill the offered space or it
        // can be larger if items were resized, or if, for example, we were previously
        // displaying the item 15, but now we have only 10 items in total in the data set.
        val scrollToBeConsumed = state.scrollToBeConsumed(isLookingAhead)
        val consumedScroll =
            if (
                scrollToBeConsumed.fastRoundToInt().sign == scrollDelta.sign &&
                    abs(scrollToBeConsumed.fastRoundToInt()) >= abs(scrollDelta)
            ) {
                scrollDelta.toFloat()
            } else {
                scrollToBeConsumed
            }

        val unconsumedScroll = scrollToBeConsumed - consumedScroll
        // When scrolling to the bottom via gesture, there could be scrollback due to
        // not being able to consume the whole scroll. In that case, the amount of
        // scrollBack is the inverse of unconsumed scroll.
        val scrollBackAmount: Float =
            if (isLookingAhead && scrollDelta > preScrollBackScrollDelta && unconsumedScroll <= 0) {
                scrollDelta - preScrollBackScrollDelta + unconsumedScroll
            } else 0f
        val itemScrollOffsets = firstItemOffsets.copyOf().transform { -it }

        // even if we compose items to fill before content padding we should ignore items fully
        // located there for the state's scroll position calculation (first item + first offset)
        debugLog { "adjusting for content padding" }
        if (beforeContentPadding > mainAxisSpacing) {
            for (laneIndex in measuredItems.indices) {
                val laneItems = measuredItems[laneIndex]
                for (i in laneItems.indices) {
                    val item = laneItems[i]
                    val gaps = laneInfo.getGaps(item.index)
                    val size =
                        item.mainAxisSizeWithSpacings + if (gaps == null) 0 else gaps[laneIndex]
                    if (
                        i != laneItems.lastIndex &&
                            firstItemOffsets[laneIndex] != 0 &&
                            firstItemOffsets[laneIndex] >= size
                    ) {
                        firstItemOffsets[laneIndex] -= size
                        firstItemIndices[laneIndex] = laneItems[i + 1].index
                    } else {
                        break
                    }
                }
            }
        }

        debugLog {
            "final first indices: ${firstItemIndices.toList()}, " +
                "offsets: ${firstItemOffsets.toList()}"
        }

        // end measure

        // start placement

        val contentPadding = beforeContentPadding + afterContentPadding
        var layoutWidth =
            if (isVertical) {
                constraints.maxWidth
            } else {
                constraints.constrainWidth(currentItemOffsets.max() + contentPadding)
            }
        var layoutHeight =
            if (isVertical) {
                constraints.constrainHeight(currentItemOffsets.max() + contentPadding)
            } else {
                constraints.maxHeight
            }

        val mainAxisLayoutSize =
            min(if (isVertical) layoutHeight else layoutWidth, mainAxisAvailableSize).let {
                // The offsets are calculated in [-beforePad; size + afterPad] interval
                // Ensure the layout size used for positioning (and reverse layout calculation)
                // is in the same interval.
                it - beforeContentPadding + afterContentPadding
            }

        debugLog { "pinned items: $pinnedItems" }

        var extraItemOffset = itemScrollOffsets[0]
        val extraItemsBefore =
            calculateExtraItems(
                position = {
                    extraItemOffset -= it.mainAxisSizeWithSpacings
                    it.position(
                        mainAxis = extraItemOffset,
                        crossAxis = 0,
                        mainAxisLayoutSize = mainAxisLayoutSize,
                    )
                },
                filter = { itemIndex ->
                    val lane = laneInfo.getLane(itemIndex)
                    when (lane) {
                        LaneUnset,
                        LaneFullSpan -> {
                            measuredItems.all {
                                val firstIndex = it.firstOrNull()?.index ?: -1
                                firstIndex > itemIndex
                            }
                        }
                        else -> {
                            val firstIndex = measuredItems[lane].firstOrNull()?.index ?: -1
                            firstIndex > itemIndex
                        }
                    }
                },
                beforeVisibleBounds = true,
            )

        debugLog {
            "extra items before: ${extraItemsBefore.fastJoinToString { it.index.toString() }}"
        }

        val visibleItems =
            calculateVisibleItems(
                measuredItems,
                itemScrollOffsets,
                mainAxisLayoutSize,
                minOffset,
                maxOffset,
            )

        extraItemOffset = itemScrollOffsets[0]

        val itemsRetainedForLookahead =
            itemsRetainedForLookahead(
                lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: -1,
                itemCount,
                isLookingAhead,
                position = { item, crossAxis ->
                    item.position(
                        mainAxis = extraItemOffset,
                        crossAxis = crossAxis,
                        mainAxisLayoutSize = mainAxisLayoutSize,
                    )
                    extraItemOffset += item.mainAxisSizeWithSpacings
                },
            )
        val extraItemsAfter =
            calculateExtraItems(
                position = {
                    it.position(
                        mainAxis = extraItemOffset,
                        crossAxis = 0,
                        mainAxisLayoutSize = mainAxisLayoutSize,
                    )
                    extraItemOffset += it.mainAxisSizeWithSpacings
                },
                filter = { itemIndex ->
                    if (itemIndex >= itemCount) {
                        return@calculateExtraItems false
                    }
                    // Also filter out items already in itemsRetainedForLookahead
                    if (itemsRetainedForLookahead?.fastAny { it.index == itemIndex } == true) {
                        return@calculateExtraItems false
                    }
                    val lane = laneInfo.getLane(itemIndex)
                    when (lane) {
                        LaneUnset,
                        LaneFullSpan -> {
                            currentItemIndices.all { it < itemIndex }
                        }
                        else -> {
                            currentItemIndices[lane] < itemIndex
                        }
                    }
                },
                beforeVisibleBounds = false,
            )

        debugLog {
            "extra items after: ${extraItemsAfter.fastJoinToString { it.index.toString() }}"
        }

        val positionedItems = mutableListOf<LazyStaggeredGridMeasuredItem>()
        positionedItems.addAll(extraItemsBefore)
        positionedItems.addAll(visibleItems)
        if (itemsRetainedForLookahead != null) {
            positionedItems.addAll(itemsRetainedForLookahead)
        }
        positionedItems.addAll(extraItemsAfter)

        debugLog { "positioned: $positionedItems" }

        state.itemAnimator.onMeasured(
            consumedScroll = consumedScroll.toInt(),
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            positionedItems = positionedItems,
            keyIndexMap = measuredItemProvider.keyIndexMap,
            itemProvider = measuredItemProvider,
            isVertical = isVertical,
            laneCount = laneCount,
            isLookingAhead = isLookingAhead,
            hasLookaheadOccurred = isInLookaheadScope,
            layoutMinOffset = firstItemOffsets.min(),
            layoutMaxOffset = currentItemOffsets.max() + contentPadding,
            coroutineScope = coroutineScope,
            graphicsContext = graphicsContext,
        )

        if (!isLookingAhead) {
            val disappearingItemsSize = state.itemAnimator.minSizeToFitDisappearingItems
            if (disappearingItemsSize != IntSize.Zero) {
                val oldMainAxisSize = if (isVertical) layoutHeight else layoutWidth
                layoutWidth =
                    constraints.constrainWidth(maxOf(layoutWidth, disappearingItemsSize.width))
                layoutHeight =
                    constraints.constrainHeight(maxOf(layoutHeight, disappearingItemsSize.height))
                val newMainAxisSize = if (isVertical) layoutHeight else layoutWidth
                if (newMainAxisSize != oldMainAxisSize) {
                    positionedItems.fastForEach { it.updateMainAxisLayoutSize(newMainAxisSize) }
                }
            }
        }

        // end placement

        // only scroll forward if the last item is not on screen or fully visible
        val canScrollForward =
            currentItemOffsets.any { it > mainAxisAvailableSize } ||
                currentItemIndices.all { it < itemCount - 1 }

        return LazyStaggeredGridMeasureResult(
            firstVisibleItemIndices = firstItemIndices,
            firstVisibleItemScrollOffsets = firstItemOffsets,
            consumedScroll = consumedScroll,
            measureResult =
                layout(layoutWidth, layoutHeight) {
                    // Tagging as motion frame of reference placement, meaning the placement
                    // contains scrolling. This allows the consumer of this placement offset to
                    // differentiate this offset vs. offsets from structural changes. Generally
                    // speaking, this signals a preference to directly apply changes rather than
                    // animating, to avoid a chasing effect to scrolling.
                    withMotionFrameOfReferencePlacement {
                        positionedItems.fastForEach { item ->
                            item.place(scope = this, context = this@measure, isLookingAhead)
                        }
                    }

                    // we attach it during the placement so LazyStaggeredGridState can trigger
                    // re-placement
                    state.placementScopeInvalidator.attachToScope()
                },
            scrollBackAmount = scrollBackAmount,
            canScrollForward = canScrollForward,
            isVertical = isVertical,
            visibleItemsInfo = visibleItems,
            remeasureNeeded = remeasureNeeded,
            totalItemsCount = itemCount,
            viewportSize = IntSize(layoutWidth, layoutHeight),
            viewportStartOffset = minOffset,
            viewportEndOffset = maxOffset,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = mainAxisSpacing,
            slots = resolvedSlots,
            spanProvider = itemProvider.spanProvider,
            density = this,
            coroutineScope = coroutineScope,
        )
    }
}

private fun LazyStaggeredGridMeasureContext.calculateVisibleItems(
    measuredItems: Array<ArrayDeque<LazyStaggeredGridMeasuredItem>>,
    itemScrollOffsets: IntArray,
    mainAxisLayoutSize: Int,
    minOffset: Int,
    maxOffset: Int,
): List<LazyStaggeredGridMeasuredItem> {
    val positionedItems = ArrayList<LazyStaggeredGridMeasuredItem>(measuredItems.sumOf { it.size })
    while (measuredItems.any { it.isNotEmpty() }) {
        // find the next item to position
        val laneIndex = measuredItems.indexOfMinBy { it.firstOrNull()?.index ?: Int.MAX_VALUE }
        val item = measuredItems[laneIndex].removeFirst()

        if (item.lane != laneIndex) {
            continue
        }

        val spanRange = SpanRange(item.lane, item.span)
        val mainAxisOffset = itemScrollOffsets.maxInRange(spanRange)
        val crossAxisOffset = resolvedSlots.positions[laneIndex]
        val minEdge = mainAxisOffset
        val maxEdge = mainAxisOffset + item.mainAxisSize

        // Ensure that the item is positioned only when it is within visible viewport
        if (maxEdge >= minOffset && minEdge <= maxOffset) {
            item.position(
                mainAxis = mainAxisOffset,
                crossAxis = crossAxisOffset,
                mainAxisLayoutSize = mainAxisLayoutSize,
            )
            positionedItems += item
        }
        spanRange.forEach { lane ->
            itemScrollOffsets[lane] = mainAxisOffset + item.mainAxisSizeWithSpacings
        }
    }
    return positionedItems
}

/**
 * Retain the items from last approach pass until they are no longer needed in the approach. This
 * avoids disposing items in lookahead too early, which would lead to the items being composed in a
 * different node later in the approach and lose all its internal states.
 */
private inline fun LazyStaggeredGridMeasureContext.itemsRetainedForLookahead(
    lastVisibleItemIndex: Int,
    itemsCount: Int,
    isLookingAhead: Boolean,
    position: (LazyStaggeredGridMeasuredItem, Int) -> Unit,
): List<LazyStaggeredGridMeasuredItem>? {
    var list: MutableList<LazyStaggeredGridMeasuredItem>? = null

    if (isLookingAhead) {
        // Check if there's any item that needs to be composed based on last approachLayoutInfo
        if (approachLayoutInfo != null && approachLayoutInfo.visibleItemsInfo.isNotEmpty()) {
            // Find first item with index > end. Note that `visibleItemsInfo.last()` may not have
            // the largest index as the last few items could be added to animate item placement.
            val firstItem =
                approachLayoutInfo.visibleItemsInfo.run {
                    var found: LazyStaggeredGridItemInfo? = null
                    for (i in size - 1 downTo 0) {
                        if (
                            this[i].index > lastVisibleItemIndex &&
                                (i == 0 || this[i - 1].index <= lastVisibleItemIndex)
                        ) {
                            found = this[i]
                            break
                        }
                    }
                    found
                }
            val lastVisibleItem = approachLayoutInfo.visibleItemsInfo.last()
            if (firstItem != null) {
                for (i in firstItem.index..min(lastVisibleItem.index, itemsCount - 1)) {
                    if (list?.fastAny { it.index == i } != true) {
                        if (list == null) list = mutableListOf()
                        val lane =
                            approachLayoutInfo.visibleItemsInfo
                                .fastFirstOrNull { it.index == i }
                                ?.lane ?: 0
                        val spanRange = itemProvider.getSpanRange(i, lane)
                        val item = measuredItemProvider.getAndMeasure(i, spanRange)
                        list.add(item)
                        val crossAxisOffset =
                            resolvedSlots.positions.let { if (it.size > lane) it[lane] else 0 }
                        // Position items that are no longer in the lookahead based on their
                        // last seen crossAxisOffset, so their position animation for disappearance
                        // will only have motion along the mainAxis, which produces a more
                        // pleasant and less chaotic overall look.
                        position(item, crossAxisOffset)
                    }
                }
            }
        }
    }
    return list
}

private inline fun LazyStaggeredGridMeasureContext.calculateExtraItems(
    position: (LazyStaggeredGridMeasuredItem) -> Unit,
    filter: (itemIndex: Int) -> Boolean,
    beforeVisibleBounds: Boolean,
): List<LazyStaggeredGridMeasuredItem> {
    var result: MutableList<LazyStaggeredGridMeasuredItem>? = null

    pinnedItems.fastForEach(beforeVisibleBounds) { index ->
        if (filter(index)) {
            val spanRange = itemProvider.getSpanRange(index, 0)
            if (result == null) {
                result = mutableListOf()
            }
            val measuredItem = measuredItemProvider.getAndMeasure(index, spanRange)
            position(measuredItem)
            result?.add(measuredItem)
        }
    }

    return result ?: emptyList()
}

private inline fun <T> List<T>.fastForEach(reverse: Boolean = false, action: (T) -> Unit) {
    if (reverse) fastForEachReversed(action) else fastForEach(action)
}

@JvmInline
internal value class SpanRange private constructor(val packedValue: Long) {
    constructor(lane: Int, span: Int) : this(packInts(lane, lane + span))

    inline val start
        get(): Int = unpackInt1(packedValue)

    inline val end
        get(): Int = unpackInt2(packedValue)

    inline val size
        get(): Int = end - start
}

private inline fun SpanRange.forEach(block: (Int) -> Unit) {
    for (i in start until end) {
        block(i)
    }
}

private fun IntArray.offsetBy(delta: Int) {
    for (i in indices) {
        this[i] = this[i] + delta
    }
}

private fun IntArray.maxInRange(indexRange: SpanRange): Int {
    var max = Int.MIN_VALUE
    indexRange.forEach { max = maxOf(max, this[it]) }
    return max
}

internal fun IntArray.indexOfMinValue(minBound: Int = Int.MIN_VALUE): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        if (this[i] in (minBound + 1) until min) {
            min = this[i]
            result = i
        }
    }

    return result
}

private inline fun <T> Array<T>.indexOfMinBy(block: (T) -> Int): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        val value = block(this[i])
        if (min > value) {
            min = value
            result = i
        }
    }

    return result
}

private fun IntArray.indexOfMaxValue(): Int {
    var result = -1
    var max = Int.MIN_VALUE
    for (i in indices) {
        if (max < this[i]) {
            max = this[i]
            result = i
        }
    }

    return result
}

private inline fun IntArray.transform(block: (Int) -> Int): IntArray {
    for (i in indices) {
        this[i] = block(this[i])
    }
    return this
}

private fun LazyStaggeredGridMeasureContext.ensureIndicesInRange(
    indices: IntArray,
    itemCount: Int,
) {
    // reverse traverse to make sure last items are recorded to the latter lanes
    for (i in indices.indices.reversed()) {
        while (indices[i] >= itemCount || !laneInfo.assignedToLane(indices[i], i)) {
            indices[i] = findPreviousItemIndex(indices[i], i)
        }
        if (indices[i] >= 0) {
            // reserve item for span
            if (!itemProvider.isFullSpan(indices[i])) {
                laneInfo.setLane(indices[i], i)
            }
        }
    }
}

private fun LazyStaggeredGridMeasureContext.findPreviousItemIndex(item: Int, lane: Int): Int =
    laneInfo.findPreviousItemIndex(item, lane)

@OptIn(ExperimentalFoundationApi::class)
internal abstract class LazyStaggeredGridMeasureProvider(
    private val isVertical: Boolean,
    private val itemProvider: LazyStaggeredGridItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val resolvedSlots: LazyStaggeredGridSlots,
) : LazyLayoutMeasuredItemProvider<LazyStaggeredGridMeasuredItem>() {
    private fun childConstraints(slot: Int, span: Int): Constraints {
        // resolved slots contain [offset, size] pair per each slot.
        val crossAxisSize =
            if (span == 1) {
                resolvedSlots.sizes[slot]
            } else {
                val start = resolvedSlots.positions[slot]
                val endSlot = slot + span - 1
                val end = resolvedSlots.positions[endSlot] + resolvedSlots.sizes[endSlot]
                end - start
            }

        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun getAndMeasure(index: Int, span: SpanRange): LazyStaggeredGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)

        val slotCount = resolvedSlots.sizes.size
        val spanStart = span.start.coerceAtMost(slotCount - 1)
        val spanSize = span.size.coerceAtMost(slotCount - spanStart)

        val constraints = childConstraints(spanStart, spanSize)
        val placeables = measureScope.getPlaceables(index, constraints)
        return createItem(index, spanStart, spanSize, key, contentType, placeables, constraints)
    }

    override fun getAndMeasure(
        index: Int,
        lane: Int,
        span: Int,
        constraints: Constraints,
    ): LazyStaggeredGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val contentType = itemProvider.getContentType(index)

        val placeables = measureScope.getPlaceables(index, constraints)
        return createItem(index, lane, span, key, contentType, placeables, constraints)
    }

    val keyIndexMap: LazyLayoutKeyIndexMap
        get() = itemProvider.keyIndexMap

    abstract fun createItem(
        index: Int,
        lane: Int,
        span: Int,
        key: Any,
        contentType: Any?,
        placeables: List<Placeable>,
        constraints: Constraints,
    ): LazyStaggeredGridMeasuredItem
}

internal class LazyStaggeredGridMeasuredItem(
    override val index: Int,
    override val key: Any,
    private val placeables: List<Placeable>,
    override val isVertical: Boolean,
    spacing: Int,
    override val lane: Int,
    override val span: Int,
    private val beforeContentPadding: Int,
    private val afterContentPadding: Int,
    override val contentType: Any?,
    private val animator: LazyLayoutItemAnimator<LazyStaggeredGridMeasuredItem>,
    override val constraints: Constraints,
) : LazyStaggeredGridItemInfo, LazyLayoutMeasuredItem {
    var isVisible = true

    override val placeablesCount: Int
        get() = placeables.size

    override fun getParentData(index: Int) = placeables[index].parentData

    val mainAxisSize: Int =
        placeables.fastMaxOfOrDefault(0) { placeable ->
            if (isVertical) placeable.height else placeable.width
        }

    override val mainAxisSizeWithSpacings: Int = (mainAxisSize + spacing).coerceAtLeast(0)

    val crossAxisSize: Int =
        placeables.fastMaxOfOrDefault(0) { if (isVertical) it.width else it.height }

    private var mainAxisLayoutSize: Int = Unset
    private var minMainAxisOffset: Int = 0
    private var maxMainAxisOffset: Int = 0

    /**
     * True when this item is not supposed to react on scroll delta. for example items being
     * animated away out of the bounds are non scrollable.
     */
    override var nonScrollableItem: Boolean = false

    override val size: IntSize =
        if (isVertical) {
            IntSize(crossAxisSize, mainAxisSize)
        } else {
            IntSize(mainAxisSize, crossAxisSize)
        }
    override var offset: IntOffset = IntOffset.Zero
        private set

    override fun getOffset(index: Int): IntOffset = offset

    fun position(mainAxis: Int, crossAxis: Int, mainAxisLayoutSize: Int) {
        this.mainAxisLayoutSize = mainAxisLayoutSize
        minMainAxisOffset = -beforeContentPadding
        maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding
        offset =
            if (isVertical) {
                IntOffset(crossAxis, mainAxis)
            } else {
                IntOffset(mainAxis, crossAxis)
            }
    }

    override fun position(
        mainAxisOffset: Int,
        crossAxisOffset: Int,
        layoutWidth: Int,
        layoutHeight: Int,
    ) {
        position(mainAxisOffset, crossAxisOffset, if (isVertical) layoutHeight else layoutWidth)
    }

    val mainAxisOffset
        get() = if (!isVertical) offset.x else offset.y

    fun place(
        scope: Placeable.PlacementScope,
        context: LazyStaggeredGridMeasureContext,
        isLookingAhead: Boolean,
    ) =
        with(context) {
            requirePrecondition(mainAxisLayoutSize != Unset) { "position() should be called first" }
            with(scope) {
                placeables.fastForEachIndexed { index, placeable ->
                    val minOffset = minMainAxisOffset - placeable.mainAxisSize
                    val maxOffset = maxMainAxisOffset

                    var offset = offset
                    val animation = animator.getAnimation(key, index)
                    val layer: GraphicsLayer?
                    if (animation != null) {
                        if (isLookingAhead) {
                            // Skip animation in lookahead pass
                            animation.lookaheadOffset = offset
                        } else {
                            val targetOffset =
                                if (animation.lookaheadOffset != NotInitialized) {
                                    animation.lookaheadOffset
                                } else {
                                    offset
                                }
                            val animatedOffset = targetOffset + animation.placementDelta
                            // cancel the animation if current and target offsets are both out of
                            // the
                            // bounds.
                            if (
                                (offset.mainAxis <= minOffset &&
                                    animatedOffset.mainAxis <= minOffset) ||
                                    (offset.mainAxis >= maxOffset &&
                                        animatedOffset.mainAxis >= maxOffset)
                            ) {
                                animation.cancelPlacementAnimation()
                            }
                            offset = animatedOffset
                        }
                        layer = animation.layer
                    } else {
                        layer = null
                    }
                    if (reverseLayout) {
                        offset =
                            offset.copy { mainAxisOffset ->
                                mainAxisLayoutSize - mainAxisOffset - placeable.mainAxisSize
                            }
                    }
                    offset += contentOffset
                    if (!isLookingAhead) {
                        animation?.finalOffset = offset
                    }
                    if (layer != null) {
                        placeable.placeRelativeWithLayer(offset, layer)
                    } else {
                        placeable.placeRelativeWithLayer(offset)
                    }
                }
            }
        }

    /**
     * Update a [mainAxisLayoutSize] when the size did change after last [position] call. Knowing
     * the final size is important for calculating the final position in reverse layout.
     */
    fun updateMainAxisLayoutSize(mainAxisLayoutSize: Int) {
        this.mainAxisLayoutSize = mainAxisLayoutSize
        maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding
    }

    fun applyScrollDelta(delta: Int, updateAnimations: Boolean) {
        if (nonScrollableItem) {
            return
        }
        offset = offset.copy { it + delta }
        if (updateAnimations) {
            repeat(placeablesCount) { index ->
                val animation = animator.getAnimation(key, index)
                if (animation != null) {
                    animation.rawOffset = animation.rawOffset.copy { mainAxis -> mainAxis + delta }
                }
            }
        }
    }

    private val IntOffset.mainAxis
        get() = if (isVertical) y else x

    private inline val Placeable.mainAxisSize
        get() = if (isVertical) height else width

    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        IntOffset(if (isVertical) x else mainAxisMap(x), if (isVertical) mainAxisMap(y) else y)

    override fun toString(): String =
        if (DebugLoggingEnabled) {
            "{$index at $offset}"
        } else {
            super.toString()
        }
}

private const val Unset = Int.MIN_VALUE
