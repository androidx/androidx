/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.collection.IntList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMinByOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastSumBy
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.wear.compose.foundation.lazy.layout.hasAnimations
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope

private val DEBUG_TLC_LAYOUT = false

internal class TransformingLazyColumnContentPaddingMeasurementStrategy(
    contentPadding: PaddingValues,
    private val density: Density,
    layoutDirection: LayoutDirection,
    private val graphicsContext: GraphicsContext,
    private val itemAnimator: LazyLayoutItemAnimator<TransformingLazyColumnMeasuredItem>,
    private val isScrollInProgress: () -> Boolean,
    private val reverseLayout: Boolean,
    private val firstLayoutItemProvider: () -> TransformingLazyColumnFirstLayoutItemProvider?,
) : TransformingLazyColumnMeasurementStrategy {
    override val rightContentPadding: Int =
        with(density) { contentPadding.calculateRightPadding(layoutDirection).roundToPx() }

    override val leftContentPadding: Int =
        with(density) { contentPadding.calculateLeftPadding(layoutDirection).roundToPx() }

    inner class MeasurementScope(
        var visibleItems: ArrayDeque<TransformingLazyColumnMeasuredItem>,
        var itemSpacing: Int,
        var beforeContentPadding: Int,
        var afterContentPadding: Int,
        var itemsCount: Int,
        var maxHeight: Int,
    ) {
        val isAtStartOrOverscrolledBackwards: Boolean
            get() = with(visibleItems.first()) { index == 0 && offset >= beforeContentPadding }

        val isAtEndOrOverscrolledForward: Boolean
            get() =
                with(visibleItems.last()) {
                    index == itemsCount - 1 &&
                        offset + transformedHeight <=
                            containerConstraints.maxHeight - afterContentPadding
                }

        fun addVisibleItemsBefore(measuredItemProvider: MeasuredItemProvider): Unit =
            with(visibleItems) {
                val minOffset = 0
                val minIndex = 0
                val item = first()
                var nextBottomOffset = item.offset - itemSpacing
                var nextIndex = item.index - 1

                while (nextBottomOffset >= minOffset && nextIndex >= minIndex) {
                    val additionalItem =
                        resolveMeasuredItemForFixedBottomOffset(
                            index = nextIndex,
                            targetBottomOffset = nextBottomOffset,
                            maxHeight = maxHeight,
                            measuredItemProvider = measuredItemProvider,
                        )
                    addFirst(additionalItem)
                    nextBottomOffset -= additionalItem.transformedHeight + itemSpacing
                    nextIndex -= 1 // Indexes must be incremental.
                }
                recalculateBeforePaddings()
            }

        fun addVisibleItemsAfter(measuredItemProvider: MeasuredItemProvider): Unit =
            with(visibleItems) {
                val maxOffset: Int = maxHeight
                val maxIndex: Int = itemsCount - 1
                val item = last()
                var nextTopOffset = item.offset + item.transformedHeight + itemSpacing
                var nextIndex = item.index + 1

                while (nextTopOffset < maxOffset && nextIndex <= maxIndex) {
                    val additionalItem =
                        resolveMeasuredItemForFixedTopOffset(
                            index = nextIndex,
                            targetTopOffset = nextTopOffset,
                            maxHeight = maxHeight,
                            measuredItemProvider = measuredItemProvider,
                        )
                    nextTopOffset += additionalItem.transformedHeight + itemSpacing
                    add(additionalItem)
                    nextIndex += 1 // Indexes must be incremental.
                }
                recalculateAfterPaddings()
            }

        private fun recalculateBeforePaddings() {
            val minimumBeforeContentPadding =
                visibleItems
                    .firstOrNull()
                    ?.takeIf { it.index == 0 }
                    ?.let {
                        with(density) {
                            if (!reverseLayout) {
                                it.minimumTopContentPadding?.roundToPx() ?: 0
                            } else {
                                it.minimumBottomContentPadding?.roundToPx() ?: 0
                            }
                        }
                    } ?: initialBeforeContentPadding
            beforeContentPadding = max(initialBeforeContentPadding, minimumBeforeContentPadding)
        }

        private fun recalculateAfterPaddings() {
            val minimumAfterContentPadding =
                visibleItems
                    .lastOrNull()
                    ?.takeIf { it.index == itemsCount - 1 }
                    ?.let {
                        with(density) {
                            if (!reverseLayout) {
                                it.minimumBottomContentPadding?.roundToPx() ?: 0
                            } else {
                                it.minimumTopContentPadding?.roundToPx() ?: 0
                            }
                        }
                    } ?: initialAfterContentPadding
            afterContentPadding = max(initialAfterContentPadding, minimumAfterContentPadding)
        }

        fun correctLayout(anchorItem: TransformingLazyColumnMeasuredItem): Unit =
            with(visibleItems) {
                // Correct items below the new anchor item.
                var itemIndex = anchorItem.index - first().index + 1
                var previousOffset =
                    anchorItem.let { it.offset + it.transformedHeight } + itemSpacing
                while (itemIndex < count()) {
                    this[itemIndex].moveBelow(previousOffset)
                    previousOffset =
                        this[itemIndex].let { it.offset + it.transformedHeight } + itemSpacing
                    itemIndex += 1
                }

                // Correct items above the new anchor item.
                itemIndex = anchorItem.index - first().index - 1
                previousOffset = anchorItem.offset - itemSpacing
                while (itemIndex >= 0) {
                    this[itemIndex].moveAbove(previousOffset)
                    previousOffset = this[itemIndex].offset - itemSpacing
                    itemIndex -= 1
                }
            }

        fun anchorItem(): TransformingLazyColumnMeasuredItem? =
            with(visibleItems) {
                if (isEmpty()) return null
                val maxHeight = maxHeight
                fastForEach {
                    // Item covers the center of the container.
                    if (
                        it.offset < maxHeight / 2 &&
                            it.offset + it.transformedHeight > maxHeight / 2
                    )
                        return it
                }

                return fastMinByOrNull { abs(it.offset + it.transformedHeight / 2 - maxHeight / 2) }
            }

        /**
         * Try to approach both ends of the list with the help of gradient descent. Use overscrolled
         * delta as a weight function, move anchor item by that amount, see how much overscroll
         * happened and repeat.
         *
         * Since there is no control of client's transformedHeight function, this algorithm might
         * not settle and the max repetition count is used.
         */
        private fun gradientDescent(
            delta: List<TransformingLazyColumnMeasuredItem>.() -> Int
        ): Int =
            with(visibleItems) {
                if (isEmpty()) {
                    return 0
                }
                var deltaValue = delta(this)
                var repetitions = 0
                var totalMoved = 0
                while (abs(deltaValue) > 1 && repetitions < GRADIENT_DESCENT_REPETITIONS) {
                    val anchorItem = anchorItem() ?: return totalMoved
                    anchorItem.moveBy(-deltaValue, MeasurementDirection.DOWNWARD)
                    totalMoved -= deltaValue
                    correctLayout(anchorItem)
                    deltaValue = delta(this)
                    repetitions += 1
                }
                return totalMoved
            }

        /**
         * Pins the content to the start of the scrollable area. This is used to correct overscroll
         * at the beginning of the list or when content fits the screen.
         */
        fun pinToStart(): Int = gradientDescent { first().offset - beforeContentPadding }

        /**
         * Pins the content to the end of the scrollable area. This is used to correct overscroll at
         * the end of the list.
         */
        fun pinToEnd(): Int = gradientDescent {
            last().offset + last().transformedHeight - maxHeight + afterContentPadding
        }

        fun restoreLayoutTopToBottom(): Int =
            if (!reverseLayout) {
                pinToStart()
            } else {
                pinToEnd()
            }

        fun restoreLayoutBottomToTop(): Int =
            if (!reverseLayout) {
                pinToEnd()
            } else {
                pinToStart()
            }

        fun restoreLayoutCentered(): Int = gradientDescent {
            val topSpace = first().offset - beforeContentPadding
            val bottomSpace =
                maxHeight - last().offset - last().transformedHeight - afterContentPadding
            (topSpace - bottomSpace) / 2
        }

        fun fitsScreen(): Boolean =
            with(visibleItems) {
                val totalHeight =
                    fastSumBy { it.transformedHeight } +
                        itemSpacing * (itemsCount - 1) +
                        beforeContentPadding +
                        afterContentPadding
                return totalHeight < maxHeight &&
                    first().index == 0 &&
                    last().index == itemsCount - 1
            }
    }

    private var measurementScope = MeasurementScope(ArrayDeque(), 0, 0, 0, 0, 0)

    override fun measure(
        itemsCount: Int,
        measuredItemProvider: MeasuredItemProvider,
        keyIndexMap: LazyLayoutKeyIndexMap,
        verticalArrangement: Arrangement.Vertical,
        containerConstraints: Constraints,
        anchorItemKey: Any,
        anchorItemIndex: Int,
        anchorItemScrollOffset: Int,
        lastMeasuredAnchorItemHeight: Int,
        coroutineScope: CoroutineScope,
        density: Density,
        scrollToBeConsumed: Float,
        pinnedItems: IntList,
        layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
    ): TransformingLazyColumnMeasureResult {
        if (itemsCount == 0) {
            return emptyMeasureResult(
                containerConstraints = containerConstraints,
                beforeContentPadding = initialBeforeContentPadding,
                afterContentPadding = initialAfterContentPadding,
                layout = layout,
            )
        }

        val itemSpacingPx = with(density) { verticalArrangement.spacing.roundToPx() }
        measurementScope.itemSpacing = itemSpacingPx

        val (anchorItemIndex, previousAnchorPresent) =
            keyIndexMap.getIndex(anchorItemKey).let {
                // If no item for this key was found, getIndex returns -1. In this case we use
                // anchorItemIndex as an anchor. We can also assume that as there is no anchor with
                // this key, it is not present and was probably deleted or was not yet initialised.
                if (it == -1) anchorItemIndex to false else it to true
            }
        // Restore the position of anchor item from the previous measurement.
        val defaultPreviousAnchorItem =
            if (lastMeasuredAnchorItemHeight > 0) {
                val offset =
                    containerConstraints.maxHeight / 2 -
                        lastMeasuredAnchorItemHeight / 2 -
                        anchorItemScrollOffset

                measuredItemProvider.downwardMeasuredItem(
                    anchorItemIndex,
                    // If the previous anchor item is deleted, the item at the same index
                    // becomes the new anchor and inherits the offset of the deleted item.
                    // If the original anchor's top was off-screen, this inherited offset
                    // could also place the new anchor off-screen.
                    // To prevent this, we coerce the new anchor's top offset to be at least 0,
                    // ensuring it remains visible on screen.
                    offset = if (previousAnchorPresent) offset else offset.coerceAtLeast(0),
                    maxHeight = containerConstraints.maxHeight,
                )
            } else {
                measuredItemProvider
                    .upwardMeasuredItem(
                        anchorItemIndex,
                        offset = containerConstraints.maxHeight / 2 - anchorItemScrollOffset,
                        maxHeight = containerConstraints.maxHeight,
                    )
                    .also { it.offset += it.transformedHeight / 2 }
            }

        val activeFirstLayoutItemProvider: TransformingLazyColumnFirstLayoutItemProvider? =
            firstLayoutItemProvider()

        val firstLayoutItem =
            activeFirstLayoutItemProvider?.let { provider ->
                val defaultCenterItemInfo =
                    TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
                        index = defaultPreviousAnchorItem.index,
                        itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start,
                        offset = defaultPreviousAnchorItem.offset,
                        key = defaultPreviousAnchorItem.key,
                    )
                val info =
                    Snapshot.withoutReadObservation {
                        provider.getFirstLayoutItem(defaultCenterItemInfo)
                    }
                if (info == defaultCenterItemInfo) {
                    defaultPreviousAnchorItem
                } else {
                    val firstLayoutItemIndex =
                        info.key?.let { key ->
                            keyIndexMap.getIndex(key).takeIf { index -> index != -1 }
                        } ?: info.index

                    val resolvedIndex = firstLayoutItemIndex.coerceIn(0 until itemsCount)

                    if (
                        info.itemEdge == TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.End
                    ) {
                        resolveMeasuredItemForFixedBottomOffset(
                            index = resolvedIndex,
                            targetBottomOffset = info.offset,
                            maxHeight = containerConstraints.maxHeight,
                            measuredItemProvider = measuredItemProvider,
                        )
                    } else {
                        resolveMeasuredItemForFixedTopOffset(
                            index = resolvedIndex,
                            targetTopOffset = info.offset,
                            maxHeight = containerConstraints.maxHeight,
                            measuredItemProvider = measuredItemProvider,
                        )
                    }
                }
            } ?: defaultPreviousAnchorItem

        var canScrollForward = true
        var canScrollBackward = true
        var anchorItem: TransformingLazyColumnMeasuredItem
        var actuallyVisibleItems: List<TransformingLazyColumnMeasuredItem>
        // It triggers a remeasure on state change: once at the start of a scroll
        // (`shouldAnimate` = false), and once at the end to cache the final item state for
        // subsequent animations (`shouldAnimate` = true).
        val shouldAnimate = !isScrollInProgress()

        val scrollDelta = scrollToBeConsumed.fastRoundToInt()
        var scrollAdjustment = 0

        with(measurementScope) {
            this.itemsCount = itemsCount
            this.itemSpacing = itemSpacingPx
            this.maxHeight = containerConstraints.maxHeight
            this.beforeContentPadding = initialBeforeContentPadding
            this.afterContentPadding = initialAfterContentPadding
            this.visibleItems.clear()

            fun TransformingLazyColumnMeasuredItem.isVisible(): Boolean =
                offset + transformedHeight > 0 && offset < containerConstraints.maxHeight

            visibleItems.add(firstLayoutItem)

            val isDefaultAnchor = firstLayoutItem === defaultPreviousAnchorItem
            // Move first layout item to the new position.
            // If it's the default anchor, or if the custom anchor is unscaled,
            // we apply the linear scroll delta directly to it (legacy behavior).
            if (
                isDefaultAnchor ||
                    firstLayoutItem.measuredHeight == firstLayoutItem.transformedHeight
            ) {
                firstLayoutItem.offset += scrollDelta
                // Add the rest of the items.
                addVisibleItemsAfter(measuredItemProvider)
                addVisibleItemsBefore(measuredItemProvider)
            } else {
                // For scaled edge items.
                // Reconstruct the pre-scroll layout exactly as it was.
                addVisibleItemsAfter(measuredItemProvider)
                addVisibleItemsBefore(measuredItemProvider)
                if (scrollDelta != 0) {
                    // Find the most stable unscaled item in the center.
                    val anchorItem = anchorItem() ?: firstLayoutItem
                    // Apply linear scroll delta to the stable center item.
                    anchorItem.offset += scrollDelta
                    //  Propagate the layout changes relative to the center.
                    correctLayout(anchorItem)
                    // The scroll might have revealed new gaps at the top or bottom.
                    // Fill them. (These will only execute if there is actual empty space).
                    addVisibleItemsAfter(measuredItemProvider)
                    addVisibleItemsBefore(measuredItemProvider)
                }
            }

            fun restoreLayoutIfNeeded() {
                if (fitsScreen()) {
                    // List is shorter than container.
                    // Since we can't check what type the given arrangement is (mainly because the
                    // class used to implement Arrangement.spacedBy is not public), we "use it",
                    // asking it to arrange two small items in a big space, and see where they are
                    // put, to see if it's one of the arrangements we know. If we can't identify it,
                    // maybe because is a custom arrangement or an unsupported one, we default to
                    // top to bottom
                    val itemSize = 10
                    val spaceAvailable = 1000
                    val pilotArrangementResult = IntArray(2) { 0 }
                    val pilotItems = intArrayOf(itemSize, itemSize)
                    with(verticalArrangement) {
                        density.arrange(spaceAvailable, pilotItems, pilotArrangementResult)
                    }
                    // How much space is there between the two items, on top of the spacing in the
                    // arrangement
                    val extraSpacingBetweenItems =
                        pilotArrangementResult[1] -
                            pilotArrangementResult[0] -
                            itemSize -
                            itemSpacingPx

                    if (
                        pilotArrangementResult[1] == spaceAvailable - itemSize &&
                            abs(extraSpacingBetweenItems) <= 1
                    ) {
                        // Bottom Arrangement
                        restoreLayoutBottomToTop()
                    } else if (
                        abs(
                            pilotArrangementResult[0] -
                                (spaceAvailable - itemSize - pilotArrangementResult[1])
                        ) <= 1 && abs(extraSpacingBetweenItems) <= 1
                    ) {
                        // Center Arrangement
                        restoreLayoutCentered()
                    } else {
                        // Top Arrangement - the default.
                        restoreLayoutTopToBottom()
                    }
                    canScrollBackward = false
                    canScrollForward = false
                } else if (isAtStartOrOverscrolledBackwards) {
                    // Top item moved where it is not supposed to be.
                    // Pinning top item to the top most position.
                    scrollAdjustment += pinToStart()
                    addVisibleItemsAfter(measuredItemProvider)
                    canScrollBackward = false
                } else if (isAtEndOrOverscrolledForward) {
                    // Bottom item moved where it is not supposed to be.
                    // Pinning top item to the bottom most position.
                    scrollAdjustment += pinToEnd()
                    addVisibleItemsBefore(measuredItemProvider)
                    canScrollForward = false
                }
            }

            restoreLayoutIfNeeded()
            // Calculate new anchor item.
            anchorItem =
                anchorItem()
                    ?: return emptyMeasureResult(
                        containerConstraints = containerConstraints,
                        beforeContentPadding = beforeContentPadding,
                        afterContentPadding = afterContentPadding,
                        layout = layout,
                    )

            if (anchorItem.key != anchorItemKey) {
                // Anchor item was updated.
                correctLayout(anchorItem)

                // Most probably previous anchor item is smaller now, might need to add items before
                // or after.
                addVisibleItemsAfter(measuredItemProvider)
                addVisibleItemsBefore(measuredItemProvider)
            }
            restoreLayoutIfNeeded()

            actuallyVisibleItems =
                visibleItems.fastFilter { it.isVisible() || (shouldAnimate && it.hasAnimations()) }
        }

        // Determine index range of visible items to find items pinned outside visible bounds.
        // If actuallyVisibleItems is non-empty, firstIndex and lastIndex are the indices of the
        // first and last visible items. Otherwise (e.g. overscroll or large content padding),
        // fall back to the index of the first item at/after offset 0, the item after the last
        // measured item, or the anchor item index.
        val firstIndex =
            if (actuallyVisibleItems.isNotEmpty()) {
                actuallyVisibleItems.first().index
            } else {
                measurementScope.visibleItems.fastFirstOrNull { it.offset >= 0 }?.index
                    ?: if (measurementScope.visibleItems.isNotEmpty())
                        measurementScope.visibleItems.last().index + 1
                    else anchorItem.index
            }
        val lastIndex =
            if (actuallyVisibleItems.isNotEmpty()) {
                actuallyVisibleItems.last().index
            } else {
                firstIndex - 1
            }

        val extraBeforeBottomOffset =
            if (actuallyVisibleItems.isNotEmpty()) {
                actuallyVisibleItems.first().offset - itemSpacingPx
            } else {
                0
            }
        val extraItemsBefore =
            measurePinnedItemsBefore(
                pinnedItems = pinnedItems,
                firstIndex = firstIndex,
                measuredItemProvider = measuredItemProvider,
                bottomOffset = extraBeforeBottomOffset,
                maxHeight = containerConstraints.maxHeight,
                itemSpacingPx = itemSpacingPx,
            )

        val extraAfterTopOffset =
            if (actuallyVisibleItems.isNotEmpty()) {
                actuallyVisibleItems.last().offset +
                    actuallyVisibleItems.last().transformedHeight +
                    itemSpacingPx
            } else {
                containerConstraints.maxHeight
            }
        val extraItemsAfter =
            measurePinnedItemsAfter(
                pinnedItems = pinnedItems,
                lastIndex = lastIndex,
                measuredItemProvider = measuredItemProvider,
                topOffset = extraAfterTopOffset,
                maxHeight = containerConstraints.maxHeight,
                itemSpacingPx = itemSpacingPx,
            )

        val positionedItems =
            if (extraItemsBefore.isEmpty() && extraItemsAfter.isEmpty()) {
                actuallyVisibleItems
            } else {
                ArrayList<TransformingLazyColumnMeasuredItem>(
                        extraItemsBefore.size + actuallyVisibleItems.size + extraItemsAfter.size
                    )
                    .apply {
                        addAll(extraItemsBefore)
                        addAll(actuallyVisibleItems)
                        addAll(extraItemsAfter)
                    }
            }

        itemAnimator.onMeasured(
            shouldAnimate = shouldAnimate,
            positionedItems = positionedItems,
            keyIndexMap = keyIndexMap,
            layoutMinOffset = 0,
            layoutMaxOffset = containerConstraints.maxHeight,
            itemSpacing = itemSpacingPx,
            coroutineScope = coroutineScope,
            graphicsContext = graphicsContext,
        )

        val childConstraints =
            Constraints(
                maxHeight = Constraints.Infinity,
                maxWidth =
                    (containerConstraints.maxWidth - leftContentPadding - rightContentPadding)
                        .coerceAtLeast(0),
            )

        positionedItems.fastForEach { it.markMeasured() }

        val appliedScroll = scrollDelta + scrollAdjustment
        val consumedScroll =
            if (scrollDelta.sign == appliedScroll.sign && abs(scrollDelta) >= abs(appliedScroll)) {
                appliedScroll.toFloat()
            } else {
                scrollToBeConsumed
            }

        return TransformingLazyColumnMeasureResult(
                anchorItemKey = anchorItem.key,
                anchorItemIndex = anchorItem.index,
                anchorItemScrollOffset =
                    anchorItem.let {
                        containerConstraints.maxHeight / 2 - it.transformedHeight / 2 - it.offset
                    },
                visibleItems = actuallyVisibleItems,
                positionedItems = positionedItems,
                totalItemsCount = itemsCount,
                lastMeasuredItemHeight = anchorItem.transformedHeight,
                canScrollForward = canScrollForward,
                canScrollBackward = canScrollBackward,
                coroutineScope = coroutineScope,
                density = density,
                itemSpacing = itemSpacingPx,
                beforeContentPadding = measurementScope.beforeContentPadding,
                afterContentPadding = measurementScope.afterContentPadding,
                childConstraints = childConstraints,
                reverseLayout = reverseLayout,
                consumedScroll = consumedScroll,
                measureResult =
                    layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                        positionedItems.fastForEach { it.place(this) }
                    },
            )
            .also {
                if (DEBUG_TLC_LAYOUT) {
                    it.checkLayoutIsCorrect()
                }
            }
    }

    private val initialBeforeContentPadding: Int =
        with(density) {
            if (!reverseLayout) {
                contentPadding.calculateTopPadding().roundToPx()
            } else {
                contentPadding.calculateBottomPadding().roundToPx()
            }
        }

    private val initialAfterContentPadding: Int =
        with(density) {
            if (!reverseLayout) {
                contentPadding.calculateBottomPadding().roundToPx()
            } else {
                contentPadding.calculateTopPadding().roundToPx()
            }
        }

    private fun resolveMeasuredItemForFixedTopOffset(
        index: Int,
        targetTopOffset: Int,
        maxHeight: Int,
        measuredItemProvider: MeasuredItemProvider,
    ): TransformingLazyColumnMeasuredItem {
        val item =
            measuredItemProvider.downwardMeasuredItem(
                index = index,
                offset = targetTopOffset,
                maxHeight = maxHeight,
            )
        if (
            targetTopOffset + item.measuredHeight / 2 >= maxHeight / 2 ||
                item.measuredHeight == item.transformedHeight
        ) {
            return item
        }

        var lowBottom = targetTopOffset
        var highBottom = targetTopOffset + item.transformedHeight
        var bestBottom = highBottom
        var minDiff = Int.MAX_VALUE
        var repetitions = 0
        while (lowBottom <= highBottom && repetitions < OFFSET_RESOLVE_REPETITIONS) {
            val candidateBottom = lowBottom + (highBottom - lowBottom) / 2
            item.moveAbove(candidateBottom)
            val evaluatedTop = item.offset
            val diff = evaluatedTop - targetTopOffset
            val absDiff = abs(diff)
            if (absDiff < minDiff) {
                minDiff = absDiff
                bestBottom = candidateBottom
            }
            if (diff == 0) break

            // Heuristic projection (secant / fixed-point jump):
            // Assuming the transformed height stays relatively constant, adjusting candidateBottom
            // by `diff` directly projects our next bound towards the target offset.
            if (diff > 0) {
                highBottom = candidateBottom - diff
            } else {
                lowBottom = candidateBottom - diff
            }
            repetitions++
        }
        // Ensure the mutated item settles at the position that yielded the minimal difference
        if (item.offset != targetTopOffset) {
            item.moveAbove(bestBottom)
        }
        return item
    }

    private fun resolveMeasuredItemForFixedBottomOffset(
        index: Int,
        targetBottomOffset: Int,
        maxHeight: Int,
        measuredItemProvider: MeasuredItemProvider,
    ): TransformingLazyColumnMeasuredItem {
        val item =
            measuredItemProvider.upwardMeasuredItem(
                index = index,
                offset = targetBottomOffset,
                maxHeight = maxHeight,
            )
        if (
            targetBottomOffset - item.measuredHeight / 2 <= maxHeight / 2 ||
                item.measuredHeight == item.transformedHeight
        ) {
            return item
        }

        var lowTop = targetBottomOffset - item.transformedHeight
        var highTop = targetBottomOffset
        var bestTop = lowTop
        var minDiff = Int.MAX_VALUE
        var repetitions = 0
        while (lowTop <= highTop && repetitions < OFFSET_RESOLVE_REPETITIONS) {
            val candidateTop = lowTop + (highTop - lowTop) / 2
            item.moveBelow(candidateTop)
            val evaluatedBottom = item.offset + item.transformedHeight
            val diff = evaluatedBottom - targetBottomOffset
            val absDiff = abs(diff)
            if (absDiff < minDiff) {
                minDiff = absDiff
                bestTop = candidateTop
            }
            if (diff == 0) break

            // Heuristic projection (secant / fixed-point jump):
            // Assuming the transformed height stays relatively constant, adjusting candidateTop
            // by `diff` directly projects our next bound towards the target offset.
            if (diff > 0) {
                highTop = candidateTop - diff
            } else {
                lowTop = candidateTop - diff
            }
            repetitions++
        }
        // Ensure the mutated item settles at the position that yielded the minimal difference
        if (item.offset + item.transformedHeight != targetBottomOffset) {
            item.moveBelow(bestTop)
        }
        return item
    }

    private companion object {
        const val GRADIENT_DESCENT_REPETITIONS = 4
        const val OFFSET_RESOLVE_REPETITIONS = 10
    }
}

private fun measurePinnedItemsBefore(
    pinnedItems: IntList,
    firstIndex: Int,
    measuredItemProvider: MeasuredItemProvider,
    bottomOffset: Int,
    maxHeight: Int,
    itemSpacingPx: Int,
): List<TransformingLazyColumnMeasuredItem> {
    var list: MutableList<TransformingLazyColumnMeasuredItem>? = null
    var currentBottomOffset = bottomOffset
    pinnedItems.forEachReversed { index ->
        if (index < firstIndex) {
            if (list == null) list = mutableListOf()
            val item =
                measuredItemProvider.upwardMeasuredItem(
                    index,
                    offset = currentBottomOffset,
                    maxHeight = maxHeight,
                )
            currentBottomOffset -= item.transformedHeight + itemSpacingPx
            list.add(item)
        }
    }
    return list?.apply { reverse() } ?: emptyList()
}

private fun measurePinnedItemsAfter(
    pinnedItems: IntList,
    lastIndex: Int,
    measuredItemProvider: MeasuredItemProvider,
    topOffset: Int,
    maxHeight: Int,
    itemSpacingPx: Int,
): List<TransformingLazyColumnMeasuredItem> {
    var list: MutableList<TransformingLazyColumnMeasuredItem>? = null
    var currentTopOffset = topOffset
    pinnedItems.forEach { index ->
        if (index > lastIndex) {
            if (list == null) list = mutableListOf()
            val item =
                measuredItemProvider.downwardMeasuredItem(
                    index,
                    offset = currentTopOffset,
                    maxHeight = maxHeight,
                )
            currentTopOffset += item.transformedHeight + itemSpacingPx
            list.add(item)
        }
    }
    return list ?: emptyList()
}
