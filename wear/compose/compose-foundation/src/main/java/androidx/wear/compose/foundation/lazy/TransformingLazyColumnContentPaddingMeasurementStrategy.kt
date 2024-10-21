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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.bottomItemScrollProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.topItemScrollProgress
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope

internal class TransformingLazyColumnContentPaddingMeasurementStrategy(
    contentPadding: PaddingValues,
    intrinsicMeasureScope: IntrinsicMeasureScope,
) : TransformingLazyColumnMeasurementStrategy {
    override val rightContentPadding: Int =
        with(intrinsicMeasureScope) {
            contentPadding.calculateRightPadding(layoutDirection).roundToPx()
        }

    override val leftContentPadding: Int =
        with(intrinsicMeasureScope) {
            contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
        }

    override fun measure(
        itemsCount: Int,
        measuredItemProvider: MeasuredItemProvider,
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
            return emptyMeasureResult(containerConstraints, layout)
        }
        val visibleItems = ArrayDeque<TransformingLazyColumnMeasuredItem>()
        var canScrollForward = true
        var canScrollBackward = true

        // Place center item
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
        centerItem.offset += scrollToBeConsumed.roundToInt()

        visibleItems.add(centerItem)

        var bottomOffset = centerItem.offset + centerItem.transformedHeight + itemSpacing
        var bottomPassIndex = anchorItemIndex + 1

        while (bottomOffset < containerConstraints.maxHeight && bottomPassIndex < itemsCount) {
            val item =
                measuredItemProvider.downwardMeasuredItem(
                    bottomPassIndex,
                    bottomOffset,
                    maxHeight = containerConstraints.maxHeight
                )
            bottomOffset += item.transformedHeight + itemSpacing
            visibleItems.add(item)
            bottomPassIndex += 1
        }
        var topOffset = centerItem.offset - itemSpacing
        var topPassIndex = anchorItemIndex - 1

        while (topOffset >= 0 && topPassIndex >= 0) {
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

        if (visibleItems.isEmpty()) {
            return emptyMeasureResult(containerConstraints, layout)
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
        } else if (overscrolledBackwards(visibleItems.first(), 0)) {
            restoreLayoutTopToBottom(visibleItems, itemSpacing, containerConstraints)
            canScrollBackward = false
        } else if (
            overscrolledForward(visibleItems.last(), itemsCount - 1, containerConstraints.maxHeight)
        ) {
            restoreLayoutBottomToTop(visibleItems, itemSpacing, containerConstraints)
            canScrollForward = false
        }

        val anchorItem =
            visibleItems.minBy {
                abs(it.offset + it.transformedHeight / 2 - containerConstraints.maxHeight / 2)
            }

        return TransformingLazyColumnMeasureResult(
            anchorItemIndex = anchorItem.index,
            anchorItemScrollOffset =
                anchorItem.let {
                    it.offset + it.transformedHeight / 2 - containerConstraints.maxHeight / 2
                },
            visibleItems = visibleItems,
            totalItemsCount = itemsCount,
            lastMeasuredItemHeight = anchorItem.transformedHeight,
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
            coroutineScope = coroutineScope,
            density = density,
            itemSpacing = itemSpacing,
            measureResult =
                layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                    visibleItems.fastForEach { it.place(this) }
                }
        )
    }

    private val beforeContentPadding: Int =
        with(intrinsicMeasureScope) { contentPadding.calculateTopPadding().roundToPx() }

    private val afterContentPadding: Int =
        with(intrinsicMeasureScope) { contentPadding.calculateBottomPadding().roundToPx() }

    private fun restoreLayoutTopToBottom(
        visibleItems: ArrayDeque<TransformingLazyColumnMeasuredItem>,
        itemSpacing: Int,
        containerConstraints: Constraints
    ) {
        var previousOffset = beforeContentPadding
        visibleItems.fastForEachIndexed { idx, item ->
            item.scrollProgress =
                bottomItemScrollProgress(
                    // TODO: artemiy - Investigate why this is needed.
                    if (idx == 0) previousOffset - itemSpacing else previousOffset,
                    item.placeable.height,
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
            visibleItems[idx].scrollProgress =
                topItemScrollProgress(
                    // TODO: artemiy - Investigate why this is needed.
                    if (idx == 0) bottomLineOffset + 2 * itemSpacing else bottomLineOffset,
                    visibleItems[idx].placeable.height,
                    containerConstraints.maxHeight
                )
            visibleItems[idx].offset = bottomLineOffset - visibleItems[idx].transformedHeight
            bottomLineOffset = visibleItems[idx].offset - itemSpacing
        }
    }

    private fun overscrolledBackwards(
        visibleItem: TransformingLazyColumnMeasuredItem,
        index: Int
    ): Boolean = visibleItem.let { it.index == index && it.offset >= beforeContentPadding }

    private fun overscrolledForward(
        visibleItem: TransformingLazyColumnMeasuredItem,
        index: Int,
        maxHeight: Int
    ): Boolean =
        visibleItem.let {
            it.index == index && it.offset + it.transformedHeight < maxHeight - afterContentPadding
        }
}
