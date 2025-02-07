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

import androidx.collection.mutableObjectIntMapOf
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.bottomItemScrollProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.topItemScrollProgress
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.wear.compose.foundation.lazy.layout.hasAnimations
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope

internal class TransformingLazyColumnContentPaddingMeasurementStrategy(
    contentPadding: PaddingValues,
    density: Density,
    layoutDirection: LayoutDirection,
    private val graphicsContext: GraphicsContext,
    private val itemAnimator: LazyLayoutItemAnimator<TransformingLazyColumnMeasuredItem>
) : TransformingLazyColumnMeasurementStrategy {
    override val rightContentPadding: Int =
        with(density) { contentPadding.calculateRightPadding(layoutDirection).roundToPx() }

    override val leftContentPadding: Int =
        with(density) { contentPadding.calculateLeftPadding(layoutDirection).roundToPx() }

    override fun measure(
        itemsCount: Int,
        measuredItemProvider: MeasuredItemProvider,
        keyIndexMap: LazyLayoutKeyIndexMap,
        itemSpacing: Int,
        containerConstraints: Constraints,
        anchorItemIndex: Int,
        anchorItemScrollOffset: Int,
        lastMeasuredAnchorItemHeight: Int,
        coroutineScope: CoroutineScope,
        density: Density,
        scrollToBeConsumed: Float,
        layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
    ): TransformingLazyColumnMeasureResult {
        if (itemsCount == 0) {
            return emptyMeasureResult(
                containerConstraints = containerConstraints,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                layout = layout
            )
        }

        // Place the center item
        val centerItem =
            if (lastMeasuredAnchorItemHeight > 0) {
                measuredItemProvider.downwardMeasuredItem(
                    anchorItemIndex,
                    anchorItemScrollOffset - lastMeasuredAnchorItemHeight / 2 +
                        containerConstraints.maxHeight / 2,
                    maxHeight = containerConstraints.maxHeight
                )
            } else {
                measuredItemProvider
                    .upwardMeasuredItem(
                        anchorItemIndex,
                        anchorItemScrollOffset + containerConstraints.maxHeight / 2,
                        maxHeight = containerConstraints.maxHeight
                    )
                    .also { it.offset += it.transformedHeight / 2 }
            }

        var canScrollForward = true
        var canScrollBackward = true

        // TODO: use a lightweight object for base calculations instead of
        // TransformingLazyColumnMeasuredItem
        val visibleItemsArr =
            Array(MEASURE_PASSES) { ArrayDeque<TransformingLazyColumnMeasuredItem>() }

        fun TransformingLazyColumnMeasuredItem.isVisible(): Boolean =
            offset + transformedHeight > 0 && offset < containerConstraints.maxHeight

        val minIndex = Array(MEASURE_PASSES) { Int.MAX_VALUE }
        val maxIndex = Array(MEASURE_PASSES) { Int.MIN_VALUE }

        repeat(MEASURE_PASSES) { measurePass ->
            canScrollForward = true
            canScrollBackward = true

            val visibleItems = visibleItemsArr[measurePass]

            visibleItems.add(centerItem.copy())

            addVisibleItemsAfter(
                itemSpacing = itemSpacing,
                containerConstraints = containerConstraints,
                itemsCount = itemsCount,
                measuredItemProvider = measuredItemProvider,
                visibleItems = visibleItems
            )

            addVisibleItemsBefore(
                itemSpacing = itemSpacing,
                measuredItemProvider = measuredItemProvider,
                containerConstraints = containerConstraints,
                visibleItems = visibleItems
            )

            if (visibleItems.isEmpty()) {
                return emptyMeasureResult(
                    containerConstraints = containerConstraints,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    layout = layout
                )
            }

            val totalHeight =
                visibleItems.sumOf { it.transformedHeight } +
                    itemSpacing * (itemsCount - 1) +
                    beforeContentPadding +
                    afterContentPadding

            if (
                totalHeight < containerConstraints.maxHeight &&
                    visibleItems.first().index == 0 &&
                    visibleItems.last().index == itemsCount - 1
            ) {
                restoreLayoutTopToBottom(visibleItems, itemSpacing, containerConstraints)
                canScrollBackward = false
                canScrollForward = false
            } else if (overscrolledBackwards(visibleItems.first())) {
                restoreLayoutTopToBottom(visibleItems, itemSpacing, containerConstraints)
                addVisibleItemsAfter(
                    itemSpacing = itemSpacing,
                    itemsCount = itemsCount,
                    measuredItemProvider = measuredItemProvider,
                    containerConstraints = containerConstraints,
                    visibleItems = visibleItems
                )
                canScrollBackward = false
            } else if (
                overscrolledForward(
                    visibleItems.last(),
                    itemsCount - 1,
                    containerConstraints.maxHeight
                )
            ) {
                restoreLayoutBottomToTop(visibleItems, itemSpacing, containerConstraints)
                addVisibleItemsBefore(
                    itemSpacing = itemSpacing,
                    measuredItemProvider = measuredItemProvider,
                    containerConstraints = containerConstraints,
                    visibleItems = visibleItems
                )
                canScrollForward = false
            }

            // If we just finished the first pass, We can apply the scroll now, and see what
            // changes.
            if (measurePass == 0) {
                centerItem.offset += scrollToBeConsumed.roundToInt()
            }

            visibleItems.fastForEach {
                if (it.isVisible()) {
                    maxIndex[measurePass] = max(maxIndex[measurePass], it.index)
                    minIndex[measurePass] = min(minIndex[measurePass], it.index)
                }
            }
        }

        // Ensure that we have all the information on both the firstPassOffsets & positionedItems,
        // By checking what is in one and not in the other, and computing their positions.
        val absMinIndex = minIndex.min()
        val absMaxIndex = maxIndex.max()

        repeat(MEASURE_PASSES) { measurePass ->
            addVisibleItemsBefore(
                itemSpacing,
                measuredItemProvider,
                containerConstraints,
                visibleItemsArr[measurePass],
                minOffset = Int.MIN_VALUE, // No limit on offset
                minIndex = absMinIndex
            )
            addVisibleItemsAfter(
                itemSpacing,
                containerConstraints,
                itemsCount,
                measuredItemProvider,
                visibleItemsArr[measurePass],
                maxOffset = Int.MAX_VALUE, // No limit on offset
                maxIndex = absMaxIndex
            )
        }

        val firstPassOffsets = mutableObjectIntMapOf<Any>()
        visibleItemsArr[0].fastForEach { firstPassOffsets[it.key] = it.offset }

        // We don't need the first pass results anymore.
        visibleItemsArr[0].clear()

        val actuallyVisibleItems =
            visibleItemsArr[1].fastFilter { it.isVisible() || it.hasAnimations() }

        val anchorItem =
            actuallyVisibleItems.anchorItem(containerConstraints.maxHeight)
                ?: return emptyMeasureResult(
                    containerConstraints = containerConstraints,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    layout = layout
                )

        actuallyVisibleItems.fastForEach { item ->
            itemAnimator.getAnimation(item.key, 0)?.let {
                it.transformedHeight = item.transformedHeight
            }
        }

        itemAnimator.onMeasured(
            positionedItems = actuallyVisibleItems,
            firstPassOffsetsPerItemKey = firstPassOffsets,
            keyIndexMap = keyIndexMap,
            isLookingAhead = false,
            hasLookaheadOccurred = false,
            layoutMinOffset = 0,
            layoutMaxOffset = containerConstraints.maxHeight,
            coroutineScope = coroutineScope,
            graphicsContext = graphicsContext,
        )

        actuallyVisibleItems.fastForEach { it.isInMeasure = false }
        val childConstraints =
            Constraints(
                maxHeight = Constraints.Infinity,
                maxWidth = containerConstraints.maxWidth - leftContentPadding - rightContentPadding
            )

        return TransformingLazyColumnMeasureResult(
            anchorItemIndex = anchorItem.index,
            anchorItemScrollOffset =
                anchorItem.let {
                    it.offset + it.transformedHeight / 2 - containerConstraints.maxHeight / 2
                },
            visibleItems = actuallyVisibleItems,
            totalItemsCount = itemsCount,
            lastMeasuredItemHeight = anchorItem.transformedHeight,
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
            coroutineScope = coroutineScope,
            density = density,
            itemSpacing = itemSpacing,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            childConstraints = childConstraints,
            measureResult =
                layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                    actuallyVisibleItems.fastForEach { it.place(this) }
                }
        )
    }

    // We need to do 2 measure passes, one before and one after applying the scroll.
    private val MEASURE_PASSES = 2

    private fun addVisibleItemsBefore(
        itemSpacing: Int,
        measuredItemProvider: MeasuredItemProvider,
        containerConstraints: Constraints,
        visibleItems: ArrayDeque<TransformingLazyColumnMeasuredItem>,
        minOffset: Int = 0,
        minIndex: Int = 0
    ) {
        val item = visibleItems.first()
        var topOffset = item.offset - itemSpacing
        var topPassIndex = item.index - 1

        while (topOffset >= minOffset && topPassIndex >= minIndex) {
            val additionalItem =
                measuredItemProvider.upwardMeasuredItem(
                    topPassIndex,
                    topOffset,
                    maxHeight = containerConstraints.maxHeight
                )
            visibleItems.addFirst(additionalItem)
            topOffset -= additionalItem.transformedHeight + itemSpacing
            topPassIndex -= 1
        }
    }

    private fun addVisibleItemsAfter(
        itemSpacing: Int,
        containerConstraints: Constraints,
        itemsCount: Int,
        measuredItemProvider: MeasuredItemProvider,
        visibleItems: ArrayDeque<TransformingLazyColumnMeasuredItem>,
        maxOffset: Int = containerConstraints.maxHeight,
        maxIndex: Int = itemsCount - 1
    ) {
        val item = visibleItems.last()
        var bottomOffset = item.offset + item.transformedHeight + itemSpacing
        var bottomPassIndex = item.index + 1

        while (bottomOffset < maxOffset && bottomPassIndex <= maxIndex) {
            val additionalItem =
                measuredItemProvider.downwardMeasuredItem(
                    bottomPassIndex,
                    bottomOffset,
                    maxHeight = containerConstraints.maxHeight
                )
            bottomOffset += additionalItem.transformedHeight + itemSpacing
            visibleItems.add(additionalItem)
            bottomPassIndex += 1
        }
    }

    private fun List<TransformingLazyColumnMeasuredItem>.anchorItem(
        maxHeight: Int
    ): TransformingLazyColumnMeasuredItem? {
        if (isEmpty()) return null

        fastForEach {
            // Item covers the center of the container.
            if (it.offset < maxHeight / 2 && it.offset + it.transformedHeight > maxHeight / 2)
                return it
        }

        return minBy { abs(it.offset + it.transformedHeight / 2 - maxHeight / 2) }
    }

    private val beforeContentPadding: Int =
        with(density) { contentPadding.calculateTopPadding().roundToPx() }

    private val afterContentPadding: Int =
        with(density) { contentPadding.calculateBottomPadding().roundToPx() }

    private fun restoreLayoutTopToBottom(
        visibleItems: ArrayDeque<TransformingLazyColumnMeasuredItem>,
        itemSpacing: Int,
        containerConstraints: Constraints
    ) {
        var previousOffset = beforeContentPadding
        visibleItems.fastForEachIndexed { idx, item ->
            item.measureScrollProgress =
                bottomItemScrollProgress(
                    // TODO: artemiy - Investigate why this is needed.
                    if (idx == 0) previousOffset - itemSpacing else previousOffset,
                    item.measuredHeight,
                    containerConstraints.maxHeight
                )
            item.offset = previousOffset
            previousOffset += item.transformedHeight + itemSpacing
        }
    }

    private fun restoreLayoutBottomToTop(
        visibleItems: ArrayDeque<TransformingLazyColumnMeasuredItem>,
        itemSpacing: Int,
        containerConstraints: Constraints
    ) {
        var bottomLineOffset = containerConstraints.maxHeight - afterContentPadding
        for (idx in visibleItems.indices.reversed()) {
            visibleItems[idx].measureScrollProgress =
                topItemScrollProgress(
                    // TODO: artemiy - Investigate why this is needed.
                    if (idx == 0) bottomLineOffset + 2 * itemSpacing else bottomLineOffset,
                    visibleItems[idx].measuredHeight,
                    containerConstraints.maxHeight
                )
            visibleItems[idx].offset = bottomLineOffset - visibleItems[idx].transformedHeight
            bottomLineOffset = visibleItems[idx].offset - itemSpacing
        }
    }

    private fun overscrolledBackwards(
        visibleItem: TransformingLazyColumnMeasuredItem,
    ): Boolean = visibleItem.let { it.index == 0 && it.offset >= beforeContentPadding }

    private fun overscrolledForward(
        visibleItem: TransformingLazyColumnMeasuredItem,
        index: Int,
        maxHeight: Int
    ): Boolean =
        visibleItem.let {
            it.index == index && it.offset + it.transformedHeight < maxHeight - afterContentPadding
        }
}
