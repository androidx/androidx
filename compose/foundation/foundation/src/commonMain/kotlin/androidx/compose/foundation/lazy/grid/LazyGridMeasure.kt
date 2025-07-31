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
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.foundation.lazy.layout.StickyItemsPlacement
import androidx.compose.foundation.lazy.layout.applyStickyItems
import androidx.compose.foundation.lazy.layout.updatedVisibleItems
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope

/**
 * Measures and calculates the positions for the currently visible items. The result is produced as
 * a [LazyGridMeasureResult] which contains all the calculations.
 */
internal fun measureLazyGrid(
    itemsCount: Int,
    measuredLineProvider: LazyGridMeasuredLineProvider,
    measuredItemProvider: LazyGridMeasuredItemProvider,
    mainAxisAvailableSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    spaceBetweenLines: Int,
    firstVisibleLineIndex: Int,
    firstVisibleLineScrollOffset: Int,
    scrollToBeConsumed: Float,
    constraints: Constraints,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
    itemAnimator: LazyLayoutItemAnimator<LazyGridMeasuredItem>,
    slotsPerLine: Int,
    pinnedItems: List<Int>,
    isInLookaheadScope: Boolean,
    isLookingAhead: Boolean,
    approachLayoutInfo: LazyGridLayoutInfo?,
    coroutineScope: CoroutineScope,
    placementScopeInvalidator: ObservableScopeInvalidator,
    graphicsContext: GraphicsContext,
    prefetchInfoRetriever: (line: Int) -> List<Pair<Int, Constraints>>,
    lineIndexProvider: (itemIndex: Int) -> Int,
    stickyItemsScrollBehavior: StickyItemsPlacement?,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
): LazyGridMeasureResult {
    requirePrecondition(beforeContentPadding >= 0) { "negative beforeContentPadding" }
    requirePrecondition(afterContentPadding >= 0) { "negative afterContentPadding" }
    if (itemsCount <= 0) {
        // empty data set. reset the current scroll and report zero size
        var layoutWidth = constraints.minWidth
        var layoutHeight = constraints.minHeight
        itemAnimator.onMeasured(
            consumedScroll = 0,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            positionedItems = mutableListOf(),
            keyIndexMap = measuredItemProvider.keyIndexMap,
            itemProvider = measuredItemProvider,
            isVertical = isVertical,
            laneCount = slotsPerLine,
            isLookingAhead = isLookingAhead,
            hasLookaheadOccurred = isInLookaheadScope,
            layoutMinOffset = 0,
            layoutMaxOffset = 0,
            coroutineScope = coroutineScope,
            graphicsContext = graphicsContext,
        )
        if (!isLookingAhead) {
            val disappearingItemsSize = itemAnimator.minSizeToFitDisappearingItems
            if (disappearingItemsSize != IntSize.Zero) {
                layoutWidth = constraints.constrainWidth(disappearingItemsSize.width)
                layoutHeight = constraints.constrainHeight(disappearingItemsSize.height)
            }
        }
        return LazyGridMeasureResult(
            firstVisibleLine = null,
            firstVisibleLineScrollOffset = 0,
            canScrollForward = false,
            consumedScroll = 0f,
            measureResult = layout(layoutWidth, layoutHeight) {},
            scrollBackAmount = 0f,
            visibleItemsInfo = emptyList(),
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            totalItemsCount = 0,
            reverseLayout = reverseLayout,
            orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = spaceBetweenLines,
            remeasureNeeded = false,
            density = density,
            slotsPerLine = slotsPerLine,
            coroutineScope = coroutineScope,
            prefetchInfoRetriever = prefetchInfoRetriever,
            lineIndexProvider = lineIndexProvider,
        )
    } else {
        var currentFirstLineIndex = firstVisibleLineIndex
        var currentFirstLineScrollOffset = firstVisibleLineScrollOffset

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = scrollToBeConsumed.fastRoundToInt()

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        currentFirstLineScrollOffset -= scrollDelta

        // if the current scroll offset is less than minimally possible
        if (currentFirstLineIndex == 0 && currentFirstLineScrollOffset < 0) {
            scrollDelta += currentFirstLineScrollOffset
            currentFirstLineScrollOffset = 0
        }

        // this will contain all the MeasuredItems representing the visible lines
        val visibleLines = ArrayDeque<LazyGridMeasuredLine>()

        // define min and max offsets
        val minOffset = -beforeContentPadding + if (spaceBetweenLines < 0) spaceBetweenLines else 0
        val maxOffset = mainAxisAvailableSize

        // include the start padding so we compose items in the padding area and neutralise item
        // spacing (if the spacing is negative this will make sure the previous item is composed)
        // before starting scrolling forward we will remove it back
        currentFirstLineScrollOffset += minOffset

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstLineScrollOffset should be visible. compose them and update
        // firstLineScrollOffset
        while (currentFirstLineScrollOffset < 0 && currentFirstLineIndex > 0) {
            val previous = currentFirstLineIndex - 1
            val measuredLine = measuredLineProvider.getAndMeasure(previous)
            visibleLines.add(0, measuredLine)
            currentFirstLineScrollOffset += measuredLine.mainAxisSizeWithSpacings
            currentFirstLineIndex = previous
        }

        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (currentFirstLineScrollOffset < minOffset) {
            val notConsumedScrollDelta = minOffset - currentFirstLineScrollOffset
            currentFirstLineScrollOffset = minOffset
            scrollDelta -= notConsumedScrollDelta
        }

        // neutralize previously added padding as we stopped filling the before content padding
        currentFirstLineScrollOffset -= minOffset

        var index = currentFirstLineIndex
        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
        var currentMainAxisOffset = -currentFirstLineScrollOffset

        // will be set to true if we composed some items only to know their size and apply scroll,
        // while in the end this item will not end up in the visible viewport. we will need an
        // extra remeasure in order to dispose such items.
        var remeasureNeeded = false

        // first we need to skip lines we already composed while composing backward
        var indexInVisibleLines = 0
        while (indexInVisibleLines < visibleLines.size) {
            if (currentMainAxisOffset >= maxMainAxis) {
                // this item is out of the bounds and will not be visible.
                visibleLines.removeAt(indexInVisibleLines)
                remeasureNeeded = true
            } else {
                index++
                currentMainAxisOffset += visibleLines[indexInVisibleLines].mainAxisSizeWithSpacings
                indexInVisibleLines++
            }
        }

        // then composing visible lines forward until we fill the whole viewport.
        // we want to have at least one line in visibleItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (
            index < itemsCount &&
                (currentMainAxisOffset < maxMainAxis ||
                    currentMainAxisOffset <= 0 || // filling beforeContentPadding area
                    visibleLines.isEmpty())
        ) {
            val measuredLine = measuredLineProvider.getAndMeasure(index)
            if (measuredLine.isEmpty()) {
                break
            }

            currentMainAxisOffset += measuredLine.mainAxisSizeWithSpacings
            if (
                currentMainAxisOffset <= minOffset &&
                    measuredLine.items.last().index != itemsCount - 1
            ) {
                // this line is offscreen and will not be visible. advance firstVisibleLineIndex
                currentFirstLineIndex = index + 1
                currentFirstLineScrollOffset -= measuredLine.mainAxisSizeWithSpacings
                remeasureNeeded = true
            } else {
                visibleLines.add(measuredLine)
            }
            index++
        }

        val preScrollBackScrollDelta = scrollDelta
        // we didn't fill the whole viewport with lines starting from firstVisibleLineIndex.
        // lets try to scroll back if we have enough lines before firstVisibleLineIndex.
        if (currentMainAxisOffset < maxOffset) {
            val toScrollBack = maxOffset - currentMainAxisOffset
            currentFirstLineScrollOffset -= toScrollBack
            currentMainAxisOffset += toScrollBack
            while (
                currentFirstLineScrollOffset < beforeContentPadding && currentFirstLineIndex > 0
            ) {
                val previousIndex = currentFirstLineIndex - 1
                val measuredLine = measuredLineProvider.getAndMeasure(previousIndex)
                visibleLines.add(0, measuredLine)
                currentFirstLineScrollOffset += measuredLine.mainAxisSizeWithSpacings
                currentFirstLineIndex = previousIndex
            }
            scrollDelta += toScrollBack
            if (currentFirstLineScrollOffset < 0) {
                scrollDelta += currentFirstLineScrollOffset
                currentMainAxisOffset += currentFirstLineScrollOffset
                currentFirstLineScrollOffset = 0
            }
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough lines to fill the offered space or it
        // can be larger if lines were resized, or if, for example, we were previously
        // displaying the line 15, but now we have only 10 lines in total in the data set.
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

        // the initial offset for lines from visibleLines list
        requirePrecondition(currentFirstLineScrollOffset >= 0) { "negative initial offset" }
        val visibleLinesScrollOffset = -currentFirstLineScrollOffset
        var firstLine = visibleLines.first()

        val firstItemIndex = firstLine.items.firstOrNull()?.index ?: 0
        val lastItemIndex = visibleLines.lastOrNull()?.items?.lastOrNull()?.index ?: 0
        val extraItemsBefore =
            calculateExtraItems(
                pinnedItems = pinnedItems,
                measuredItemProvider = measuredItemProvider,
                measuredLineProvider = measuredLineProvider,
                filter = { it in 0 until firstItemIndex },
            )

        val linesRetainedForLookahead =
            linesRetainedForLookahead(
                lastVisibleItemIndex = lastItemIndex,
                itemsCount = itemsCount,
                measuredLineProvider,
                isLookingAhead = isLookingAhead,
                visibleLines = visibleLines,
                lastApproachLayoutInfo = approachLayoutInfo,
            )

        val extraItemsAfter =
            calculateExtraItems(
                pinnedItems = pinnedItems,
                measuredItemProvider = measuredItemProvider,
                measuredLineProvider = measuredLineProvider,
                filter = {
                    it in (lastItemIndex + 1) until itemsCount &&
                        (!isLookingAhead ||
                            !linesRetainedForLookahead.fastAny { line ->
                                line.items.any { item -> item.index == it }
                            })
                },
            )

        // even if we compose lines to fill before content padding we should ignore lines fully
        // located there for the state's scroll position calculation (first line + first offset)
        if (beforeContentPadding > 0 || spaceBetweenLines < 0) {
            for (i in visibleLines.indices) {
                val size = visibleLines[i].mainAxisSizeWithSpacings
                if (
                    currentFirstLineScrollOffset != 0 &&
                        size <= currentFirstLineScrollOffset &&
                        i != visibleLines.lastIndex
                ) {
                    currentFirstLineScrollOffset -= size
                    firstLine = visibleLines[i + 1]
                } else {
                    break
                }
            }
        }

        var layoutWidth =
            if (isVertical) {
                constraints.maxWidth
            } else {
                constraints.constrainWidth(currentMainAxisOffset)
            }
        var layoutHeight =
            if (isVertical) {
                constraints.constrainHeight(currentMainAxisOffset)
            } else {
                constraints.maxHeight
            }

        val positionedItems =
            calculateItemsOffsets(
                lines =
                    if (linesRetainedForLookahead.isEmpty()) visibleLines
                    else visibleLines + linesRetainedForLookahead,
                itemsBefore = extraItemsBefore,
                itemsAfter = extraItemsAfter,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight,
                finalMainAxisOffset = currentMainAxisOffset,
                maxOffset = maxOffset,
                firstLineScrollOffset = visibleLinesScrollOffset,
                isVertical = isVertical,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement,
                reverseLayout = reverseLayout,
                density = density,
            )

        itemAnimator.onMeasured(
            consumedScroll = consumedScroll.toInt(),
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            positionedItems = positionedItems,
            keyIndexMap = measuredItemProvider.keyIndexMap,
            itemProvider = measuredItemProvider,
            isVertical = isVertical,
            laneCount = slotsPerLine,
            isLookingAhead = isLookingAhead,
            hasLookaheadOccurred = isInLookaheadScope,
            layoutMinOffset = currentFirstLineScrollOffset,
            layoutMaxOffset = currentMainAxisOffset,
            coroutineScope = coroutineScope,
            graphicsContext = graphicsContext,
        )

        if (!isLookingAhead) {
            val disappearingItemsSize = itemAnimator.minSizeToFitDisappearingItems
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

        // apply sticky items logic.
        val stickingItems =
            stickyItemsScrollBehavior.applyStickyItems(
                firstItemIndex,
                lastItemIndex,
                positionedItems,
                measuredItemProvider.headerIndices,
                beforeContentPadding,
                afterContentPadding,
                layoutWidth,
                layoutHeight,
            ) {
                val span = measuredLineProvider.spanOf(it)
                val childConstraints = measuredLineProvider.childConstraints(0, span)
                measuredItemProvider.getAndMeasure(
                    index = it,
                    constraints = childConstraints,
                    lane = 0,
                    span = span,
                )
            }

        return LazyGridMeasureResult(
            firstVisibleLine = firstLine,
            firstVisibleLineScrollOffset = currentFirstLineScrollOffset,
            canScrollForward = lastItemIndex != itemsCount - 1 || currentMainAxisOffset > maxOffset,
            consumedScroll = consumedScroll,
            measureResult =
                layout(layoutWidth, layoutHeight) {
                    // Tagging as motion frame of reference placement, meaning the placement
                    // contains scrolling. This allows the consumer of this placement offset to
                    // differentiate this offset vs. offsets from structural changes. Generally
                    // speaking, this signals a preference to directly apply changes rather than
                    // animating, to avoid a chasing effect to scrolling.
                    withMotionFrameOfReferencePlacement {
                        positionedItems.fastForEach { it.place(this, isLookingAhead) }
                        stickingItems.fastForEach { it.place(this, isLookingAhead) }
                    }
                    // we attach it during the placement so LazyGridState can trigger re-placement
                    placementScopeInvalidator.attachToScope()
                },
            scrollBackAmount = scrollBackAmount,
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            visibleItemsInfo =
                updatedVisibleItems(firstItemIndex, lastItemIndex, positionedItems, stickingItems),
            totalItemsCount = itemsCount,
            reverseLayout = reverseLayout,
            orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = spaceBetweenLines,
            remeasureNeeded = remeasureNeeded,
            density = density,
            slotsPerLine = slotsPerLine,
            coroutineScope = coroutineScope,
            prefetchInfoRetriever = prefetchInfoRetriever,
            lineIndexProvider = lineIndexProvider,
        )
    }
}

private inline fun calculateExtraItems(
    pinnedItems: List<Int>,
    measuredItemProvider: LazyGridMeasuredItemProvider,
    measuredLineProvider: LazyGridMeasuredLineProvider,
    filter: (Int) -> Boolean,
): List<LazyGridMeasuredItem> {
    var items: MutableList<LazyGridMeasuredItem>? = null

    pinnedItems.fastForEach { index ->
        if (filter(index)) {
            val span = measuredLineProvider.spanOf(index)
            val constraints = measuredLineProvider.childConstraints(0, span)
            val measuredItem =
                measuredItemProvider.getAndMeasure(
                    index = index,
                    constraints = constraints,
                    lane = 0,
                    span = span,
                )
            if (items == null) {
                items = mutableListOf()
            }
            items?.add(measuredItem)
        }
    }

    return items ?: emptyList()
}

/**
 * Retain the items from last approach pass until they are no longer needed in the approach. This
 * avoids disposing items in lookahead too early, which would lead to the items being composed in a
 * different node later in the approach and lose all its internal states.
 */
private fun linesRetainedForLookahead(
    lastVisibleItemIndex: Int,
    itemsCount: Int,
    measuredLineProvider: LazyGridMeasuredLineProvider,
    isLookingAhead: Boolean,
    visibleLines: List<LazyGridMeasuredLine>,
    lastApproachLayoutInfo: LazyGridLayoutInfo?,
): List<LazyGridMeasuredLine> {
    var list: MutableList<LazyGridMeasuredLine>? = null

    if (isLookingAhead) {
        // Check if there's any item that needs to be composed based on last approachLayoutInfo
        if (
            lastApproachLayoutInfo != null && lastApproachLayoutInfo.visibleItemsInfo.isNotEmpty()
        ) {
            // Find first item with index > end. Note that `visibleItemsInfo.last()` may not have
            // the largest index as the last few items could be added to animate item placement.
            val firstItem =
                lastApproachLayoutInfo.visibleItemsInfo.run {
                    var found: LazyGridItemInfo? = null
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
            val lastVisibleItem = lastApproachLayoutInfo.visibleItemsInfo.last()
            var lineIndex = visibleLines.lastOrNull()?.let { it.index + 1 } ?: 0
            if (firstItem != null) {
                for (i in firstItem.index..min(lastVisibleItem.index, itemsCount - 1)) {
                    if (list?.fastAny { it.items.any { it.index == i } } != true) {
                        if (list == null) list = mutableListOf()
                        val measuredLine = measuredLineProvider.getAndMeasure(lineIndex = lineIndex)
                        lineIndex++
                        list.add(measuredLine)
                    }
                }
            }
        }
    }
    return list ?: emptyList()
}

/** Calculates [LazyGridMeasuredLine]s offsets. */
private fun calculateItemsOffsets(
    lines: List<LazyGridMeasuredLine>,
    itemsBefore: List<LazyGridMeasuredItem>,
    itemsAfter: List<LazyGridMeasuredItem>,
    layoutWidth: Int,
    layoutHeight: Int,
    finalMainAxisOffset: Int,
    maxOffset: Int,
    firstLineScrollOffset: Int,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
): MutableList<LazyGridMeasuredItem> {
    val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
    val hasSpareSpace = finalMainAxisOffset < min(mainAxisLayoutSize, maxOffset)
    if (hasSpareSpace) {
        checkPrecondition(firstLineScrollOffset == 0) { "non-zero firstLineScrollOffset" }
    }

    val positionedItems = ArrayList<LazyGridMeasuredItem>(lines.fastSumBy { it.items.size })

    if (hasSpareSpace) {
        requirePrecondition(itemsBefore.isEmpty() && itemsAfter.isEmpty()) { "no items" }
        val linesCount = lines.size
        fun Int.reverseAware() = if (!reverseLayout) this else linesCount - this - 1

        val sizes = IntArray(linesCount) { index -> lines[index.reverseAware()].mainAxisSize }
        val offsets = IntArray(linesCount)
        if (isVertical) {
            with(requirePreconditionNotNull(verticalArrangement) { "null verticalArrangement" }) {
                density.arrange(mainAxisLayoutSize, sizes, offsets)
            }
        } else {
            with(
                requirePreconditionNotNull(horizontalArrangement) { "null horizontalArrangement" }
            ) {
                // Enforces Ltr layout direction as it is mirrored with placeRelative later.
                density.arrange(mainAxisLayoutSize, sizes, LayoutDirection.Ltr, offsets)
            }
        }

        val reverseAwareOffsetIndices =
            if (reverseLayout) offsets.indices.reversed() else offsets.indices

        for (index in reverseAwareOffsetIndices) {
            val absoluteOffset = offsets[index]
            // when reverseLayout == true, offsets are stored in the reversed order to items
            val line = lines[index.reverseAware()]
            val relativeOffset =
                if (reverseLayout) {
                    // inverse offset to align with scroll direction for positioning
                    mainAxisLayoutSize - absoluteOffset - line.mainAxisSize
                } else {
                    absoluteOffset
                }
            positionedItems.addAllFromArray(
                line.position(relativeOffset, layoutWidth, layoutHeight)
            )
        }
    } else {
        var currentMainAxis = firstLineScrollOffset

        itemsBefore.fastForEachReversed {
            currentMainAxis -= it.mainAxisSizeWithSpacings
            it.position(currentMainAxis, 0, layoutWidth, layoutHeight)
            positionedItems.add(it)
        }

        currentMainAxis = firstLineScrollOffset
        lines.fastForEach {
            positionedItems.addAllFromArray(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += it.mainAxisSizeWithSpacings
        }

        itemsAfter.fastForEach {
            it.position(currentMainAxis, 0, layoutWidth, layoutHeight)
            positionedItems.add(it)
            currentMainAxis += it.mainAxisSizeWithSpacings
        }
    }
    return positionedItems
}

// Faster version of addAll that does not create a list for each array
private fun <T> MutableList<T>.addAllFromArray(arr: Array<T>) {
    for (item in arr) {
        add(item)
    }
}
