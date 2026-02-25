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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFilter
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
                var topOffset = item.offset - itemSpacing
                var topPassIndex = item.index - 1

                while (topOffset >= minOffset && topPassIndex >= minIndex) {
                    val additionalItem =
                        measuredItemProvider.upwardMeasuredItem(
                            topPassIndex,
                            topOffset,
                            maxHeight = maxHeight,
                        )
                    addFirst(additionalItem)
                    topOffset -= additionalItem.transformedHeight + itemSpacing
                    topPassIndex -= 1 // Indexes must be incremental.
                }
                recalculateBeforePaddings()
            }

        fun addVisibleItemsAfter(measuredItemProvider: MeasuredItemProvider): Unit =
            with(visibleItems) {
                val maxOffset: Int = maxHeight
                val maxIndex: Int = itemsCount - 1
                val item = last()
                var bottomOffset = item.offset + item.transformedHeight + itemSpacing
                var bottomPassIndex = item.index + 1

                while (bottomOffset < maxOffset && bottomPassIndex <= maxIndex) {
                    val additionalItem =
                        measuredItemProvider.downwardMeasuredItem(
                            bottomPassIndex,
                            bottomOffset,
                            maxHeight = maxHeight,
                        )
                    bottomOffset += additionalItem.transformedHeight + itemSpacing
                    add(additionalItem)
                    bottomPassIndex += 1 // Indexes must be incremental.
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
        layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
    ): TransformingLazyColumnMeasureResult {
        val itemSpacingPx = with(density) { verticalArrangement.spacing.roundToPx() }
        measurementScope.itemSpacing = itemSpacingPx

        val (anchorItemIndex, previousAnchorPresent) =
            keyIndexMap.getIndex(anchorItemKey).let {
                // If no item for this key was found, getIndex returns -1. In this case we use
                // anchorItemIndex as an anchor. We can also assume that as there is no anchor with
                // this key, it is not present and was probably deleted or was not yet initialised.
                if (it == -1) anchorItemIndex to false else it to true
            }

        if (itemsCount == 0) {
            return emptyMeasureResult(
                containerConstraints = containerConstraints,
                beforeContentPadding = initialBeforeContentPadding,
                afterContentPadding = initialAfterContentPadding,
                layout = layout,
            )
        }

        // Restore the position of anchor item from the previous measurement.
        val previousAnchorItem =
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
            this.itemSpacing = itemSpacing
            this.maxHeight = containerConstraints.maxHeight
            this.beforeContentPadding = initialBeforeContentPadding
            this.afterContentPadding = initialAfterContentPadding
            this.visibleItems.clear()

            fun TransformingLazyColumnMeasuredItem.isVisible(): Boolean =
                offset + transformedHeight > 0 && offset < containerConstraints.maxHeight

            visibleItems.add(previousAnchorItem)

            // Move previous anchor item to the new position.
            // This is done to make sure we only apply scroll to the items that are not scaled and
            // therefore it visually looks like content is following user's finger as it gets
            // scrolled.
            previousAnchorItem.offset += scrollDelta

            // Add the rest of the items.
            addVisibleItemsAfter(measuredItemProvider)
            addVisibleItemsBefore(measuredItemProvider)

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

        itemAnimator.onMeasured(
            shouldAnimate = shouldAnimate,
            positionedItems = actuallyVisibleItems,
            keyIndexMap = keyIndexMap,
            layoutMinOffset = 0,
            layoutMaxOffset = containerConstraints.maxHeight,
            coroutineScope = coroutineScope,
            graphicsContext = graphicsContext,
        )

        val childConstraints =
            Constraints(
                maxHeight = Constraints.Infinity,
                maxWidth = containerConstraints.maxWidth - leftContentPadding - rightContentPadding,
            )

        actuallyVisibleItems.fastForEach { it.markMeasured() }

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
                        actuallyVisibleItems.fastForEach { it.place(this) }
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

    private companion object {
        const val GRADIENT_DESCENT_REPETITIONS = 4
    }
}
