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

import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.roundToInt

internal class TransformingLazyColumnCenterBoundsMeasurementStrategy :
    TransformingLazyColumnMeasurementStrategy {
    // TODO: artemiy - Add those as horizontal paddings on API level.
    override val leftContentPadding: Int = 0
    override val rightContentPadding: Int = 0

    /**
     * @param itemsCount The total number of items in the list.
     * @param measuredItemProvider A provider that returns the measured items.
     * @param itemSpacing The spacing between items.
     * @param containerConstraints The constraints for the list.
     * @param anchorItemIndex The index of the anchor item. Anchor item is a visible item used to
     *   position the rest of the items before and after it. Should be from 0 (inclusive) to
     *   [itemsCount] (exclusive).
     * @param anchorItemScrollOffset The scroll offset of the anchor item. Anchor item is a visible
     *   item used to position the rest of the items before and after it.
     * @param scrollToBeConsumed The amount of scroll to be consumed.
     * @param layout A function that lays out the items.
     */
    override fun measure(
        itemsCount: Int,
        measuredItemProvider: MeasuredItemProvider,
        itemSpacing: Int,
        containerConstraints: Constraints,
        anchorItemIndex: Int,
        anchorItemScrollOffset: Int,
        lastMeasuredAnchorItemHeight: Int,
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

        if (
            centerItem.index == 0 &&
                centerItem.offset + centerItem.transformedHeight / 2 >
                    containerConstraints.maxHeight / 2
        ) {
            centerItem.pinToCenter()
            canScrollBackward = false
        }
        if (
            centerItem.index == itemsCount - 1 &&
                centerItem.offset + centerItem.transformedHeight / 2 <=
                    containerConstraints.maxHeight / 2
        ) {
            centerItem.pinToCenter()
            canScrollForward = false
        }

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
            measureResult =
                layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                    visibleItems.fastForEach { it.place(this) }
                }
        )
    }
}
