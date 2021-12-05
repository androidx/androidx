/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Measures and calculates the positions for the currently visible items. The result is produced
 * as a [LazyListMeasureResult] which contains all the calculations.
 */
internal fun measureLazyList(
    itemsCount: Int,
    itemProvider: LazyMeasuredItemProvider,
    mainAxisAvailableSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    firstVisibleItemIndex: DataIndex,
    firstVisibleItemScrollOffset: Int,
    scrollToBeConsumed: Float,
    constraints: Constraints,
    isVertical: Boolean,
    headerIndexes: List<Int>,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
    layoutDirection: LayoutDirection,
    placementAnimator: LazyListItemPlacementAnimator,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): LazyListMeasureResult {
    require(beforeContentPadding >= 0)
    require(afterContentPadding >= 0)
    if (itemsCount <= 0) {
        // empty data set. reset the current scroll and report zero size
        return LazyListMeasureResult(
            firstVisibleItem = null,
            firstVisibleItemScrollOffset = 0,
            canScrollForward = false,
            consumedScroll = 0f,
            measureResult = layout(constraints.minWidth, constraints.minHeight) {},
            visibleItemsInfo = emptyList(),
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = afterContentPadding,
            totalItemsCount = 0,
        )
    } else {
        var currentFirstItemIndex = firstVisibleItemIndex
        var currentFirstItemScrollOffset = firstVisibleItemScrollOffset
        if (currentFirstItemIndex.value >= itemsCount) {
            // the data set has been updated and now we have less items that we were
            // scrolled to before
            currentFirstItemIndex = DataIndex(itemsCount - 1)
            currentFirstItemScrollOffset = 0
        }

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = scrollToBeConsumed.roundToInt()

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        currentFirstItemScrollOffset -= scrollDelta

        // if the current scroll offset is less than minimally possible
        if (currentFirstItemIndex == DataIndex(0) && currentFirstItemScrollOffset < 0) {
            scrollDelta += currentFirstItemScrollOffset
            currentFirstItemScrollOffset = 0
        }

        // this will contain all the MeasuredItems representing the visible items
        val visibleItems = mutableListOf<LazyMeasuredItem>()

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
        currentFirstItemScrollOffset -= beforeContentPadding

        // define min and max offsets (min offset currently includes beforeContentPadding)
        val minOffset = -beforeContentPadding
        val maxOffset = mainAxisAvailableSize

        // max of cross axis sizes of all visible items
        var maxCrossAxis = 0

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstItemScrollOffset should be visible. compose them and update
        // firstItemScrollOffset
        while (currentFirstItemScrollOffset < 0 && currentFirstItemIndex > DataIndex(0)) {
            val previous = DataIndex(currentFirstItemIndex.value - 1)
            val measuredItem = itemProvider.getAndMeasure(previous)
            visibleItems.add(0, measuredItem)
            maxCrossAxis = maxOf(maxCrossAxis, measuredItem.crossAxisSize)
            currentFirstItemScrollOffset += measuredItem.sizeWithSpacings
            currentFirstItemIndex = previous
        }
        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (currentFirstItemScrollOffset < minOffset) {
            scrollDelta += currentFirstItemScrollOffset
            currentFirstItemScrollOffset = minOffset
        }

        // neutralize previously added start padding as we stopped filling the before content padding
        currentFirstItemScrollOffset += beforeContentPadding

        var index = currentFirstItemIndex
        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
        var currentMainAxisOffset = -currentFirstItemScrollOffset

        // first we need to skip items we already composed while composing backward
        visibleItems.fastForEach {
            index++
            currentMainAxisOffset += it.sizeWithSpacings
        }

        // then composing visible items forward until we fill the whole viewport.
        // we want to have at least one item in visibleItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while ((currentMainAxisOffset <= maxMainAxis || visibleItems.isEmpty()) &&
            index.value < itemsCount
        ) {
            val measuredItem = itemProvider.getAndMeasure(index)
            currentMainAxisOffset += measuredItem.sizeWithSpacings

            if (currentMainAxisOffset <= minOffset && index.value != itemsCount - 1) {
                // this item is offscreen and will not be placed. advance firstVisibleItemIndex
                currentFirstItemIndex = index + 1
                currentFirstItemScrollOffset -= measuredItem.sizeWithSpacings
            } else {
                maxCrossAxis = maxOf(maxCrossAxis, measuredItem.crossAxisSize)
                visibleItems.add(measuredItem)
            }

            index++
        }

        // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
        // lets try to scroll back if we have enough items before firstVisibleItemIndex.
        if (currentMainAxisOffset < maxOffset) {
            val toScrollBack = maxOffset - currentMainAxisOffset
            currentFirstItemScrollOffset -= toScrollBack
            currentMainAxisOffset += toScrollBack
            while (currentFirstItemScrollOffset < beforeContentPadding &&
                currentFirstItemIndex > DataIndex(0)
            ) {
                val previousIndex = DataIndex(currentFirstItemIndex.value - 1)
                val measuredItem = itemProvider.getAndMeasure(previousIndex)
                visibleItems.add(0, measuredItem)
                maxCrossAxis = maxOf(maxCrossAxis, measuredItem.crossAxisSize)
                currentFirstItemScrollOffset += measuredItem.sizeWithSpacings
                currentFirstItemIndex = previousIndex
            }
            scrollDelta += toScrollBack
            if (currentFirstItemScrollOffset < 0) {
                scrollDelta += currentFirstItemScrollOffset
                currentMainAxisOffset += currentFirstItemScrollOffset
                currentFirstItemScrollOffset = 0
            }
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough items to fill the offered space or it
        // can be larger if items were resized, or if, for example, we were previously
        // displaying the item 15, but now we have only 10 items in total in the data set.
        val consumedScroll = if (scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
            abs(scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            scrollToBeConsumed
        }

        // the initial offset for items from visibleItems list
        val visibleItemsScrollOffset = -currentFirstItemScrollOffset
        var firstItem = visibleItems.first()

        // even if we compose items to fill before content padding we should ignore items fully
        // located there for the state's scroll position calculation (first item + first offset)
        if (beforeContentPadding > 0) {
            for (i in visibleItems.indices) {
                val size = visibleItems[i].sizeWithSpacings
                if (size <= currentFirstItemScrollOffset && i != visibleItems.lastIndex) {
                    currentFirstItemScrollOffset -= size
                    firstItem = visibleItems[i + 1]
                } else {
                    break
                }
            }
        }

        val layoutWidth =
            constraints.constrainWidth(if (isVertical) maxCrossAxis else currentMainAxisOffset)
        val layoutHeight =
            constraints.constrainHeight(if (isVertical) currentMainAxisOffset else maxCrossAxis)

        val positionedItems = calculateItemsOffsets(
            items = visibleItems,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            finalMainAxisOffset = currentMainAxisOffset,
            maxOffset = maxOffset,
            itemsScrollOffset = visibleItemsScrollOffset,
            isVertical = isVertical,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = density,
            layoutDirection = layoutDirection
        )

        val headerItem = if (headerIndexes.isNotEmpty()) {
            findOrComposeLazyListHeader(
                composedVisibleItems = positionedItems,
                itemProvider = itemProvider,
                headerIndexes = headerIndexes,
                beforeContentPadding = beforeContentPadding,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight
            )
        } else {
            null
        }

        placementAnimator.onMeasured(
            consumedScroll = consumedScroll.toInt(),
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            reverseLayout = reverseLayout,
            positionedItems = positionedItems,
            itemProvider = itemProvider
        )

        val maximumVisibleOffset = minOf(currentMainAxisOffset, maxOffset) + afterContentPadding

        return LazyListMeasureResult(
            firstVisibleItem = firstItem,
            firstVisibleItemScrollOffset = currentFirstItemScrollOffset,
            canScrollForward = currentMainAxisOffset > maxOffset,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedItems.fastForEach {
                    if (it !== headerItem) {
                        it.place(this)
                    }
                }
                // the header item should be placed (drawn) after all other items
                headerItem?.place(this)
            },
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = maximumVisibleOffset,
            visibleItemsInfo = positionedItems,
            totalItemsCount = itemsCount,
        )
    }
}

/**
 * Calculates [LazyMeasuredItem]s offsets.
 */
private fun calculateItemsOffsets(
    items: List<LazyMeasuredItem>,
    layoutWidth: Int,
    layoutHeight: Int,
    finalMainAxisOffset: Int,
    maxOffset: Int,
    itemsScrollOffset: Int,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
    layoutDirection: LayoutDirection
): MutableList<LazyListPositionedItem> {
    val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
    val hasSpareSpace = finalMainAxisOffset < minOf(mainAxisLayoutSize, maxOffset)
    if (hasSpareSpace) {
        check(itemsScrollOffset == 0)
    }

    val positionedItems = ArrayList<LazyListPositionedItem>(items.size)

    if (hasSpareSpace) {
        val itemsCount = items.size
        val sizes = IntArray(itemsCount) { index ->
            val reverseLayoutAwareIndex = if (!reverseLayout) index else itemsCount - index - 1
            items[reverseLayoutAwareIndex].size
        }
        val offsets = IntArray(itemsCount) { 0 }
        if (isVertical) {
            with(requireNotNull(verticalArrangement)) {
                density.arrange(mainAxisLayoutSize, sizes, offsets)
            }
        } else {
            with(requireNotNull(horizontalArrangement)) {
                density.arrange(mainAxisLayoutSize, sizes, layoutDirection, offsets)
            }
        }
        offsets.forEachIndexed { index, absoluteOffset ->
            val reverseLayoutAwareIndex = if (!reverseLayout) index else itemsCount - index - 1
            val item = items[reverseLayoutAwareIndex]
            val relativeOffset = if (reverseLayout) {
                mainAxisLayoutSize - absoluteOffset - item.size
            } else {
                absoluteOffset
            }
            val addIndex = if (reverseLayout) 0 else positionedItems.size
            positionedItems.add(addIndex, item.position(relativeOffset, layoutWidth, layoutHeight))
        }
    } else {
        var currentMainAxis = itemsScrollOffset
        items.fastForEach {
            positionedItems.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += it.sizeWithSpacings
        }
    }
    return positionedItems
}
