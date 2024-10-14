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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.bottomItemScrollProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.topItemScrollProgress

internal interface TransformingLazyColumnMeasurementStrategy {
    /**
     * Measures the visible items for a [TransformingLazyColumn].
     *
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
    fun measure(
        itemsCount: Int,
        measuredItemProvider: MeasuredItemProvider,
        itemSpacing: Int,
        containerConstraints: Constraints,
        anchorItemIndex: Int,
        anchorItemScrollOffset: Int,
        lastMeasuredAnchorItemHeight: Int,
        scrollToBeConsumed: Float,
        layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
    ): TransformingLazyColumnMeasureResult

    val leftContentPadding: Int
    val rightContentPadding: Int
}

internal fun MeasuredItemProvider.downwardMeasuredItem(index: Int, offset: Int, maxHeight: Int) =
    measuredItem(index, offset) { height ->
        bottomItemScrollProgress(offset = offset, height = height, containerHeight = maxHeight)
    }

internal fun MeasuredItemProvider.upwardMeasuredItem(index: Int, offset: Int, maxHeight: Int) =
    measuredItem(index, offset) { height ->
            topItemScrollProgress(offset = offset, height = height, containerHeight = maxHeight)
        }
        .also { it.offset -= it.transformedHeight }

internal fun emptyMeasureResult(
    containerConstraints: Constraints,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): TransformingLazyColumnMeasureResult =
    TransformingLazyColumnMeasureResult(
        anchorItemIndex = 0,
        anchorItemScrollOffset = 0,
        visibleItems = emptyList(),
        totalItemsCount = 0,
        lastMeasuredItemHeight = Int.MIN_VALUE,
        canScrollForward = false,
        canScrollBackward = false,
        measureResult = layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {}
    )
