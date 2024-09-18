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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.roundToInt

private interface MeasuredItemProvider {
    /**
     * Creates a [LazyColumnMeasuredItem] with the given index and offset with the position
     * calculated from top to bottom.
     */
    fun downwardMeasuredItem(index: Int, offset: Int): LazyColumnMeasuredItem

    /**
     * Creates a [LazyColumnMeasuredItem] with the given index and offset with the position
     * calculated bottom up.
     */
    fun upwardMeasuredItem(index: Int, offset: Int): LazyColumnMeasuredItem
}

/**
 * Measures the visible items for a [LazyColumn].
 *
 * @param itemsCount The total number of items in the list.
 * @param measuredItemProvider A provider that returns the measured items.
 * @param itemSpacing The spacing between items.
 * @param containerConstraints The constraints for the list.
 * @param anchorItemIndex The index of the anchor item. Anchor item is a visible item used to
 *   position the rest of the items before and after it.
 * @param anchorItemScrollOffset The scroll offset of the anchor item. Anchor item is a visible item
 *   used to position the rest of the items before and after it.
 * @param scrollToBeConsumed The amount of scroll to be consumed.
 * @param layout A function that lays out the items.
 */
// TODO(artemiy): Add support for overscroll and scroll margins.
private fun measureLazyColumn(
    itemsCount: Int,
    measuredItemProvider: MeasuredItemProvider,
    itemSpacing: Int,
    containerConstraints: Constraints,
    anchorItemIndex: Int,
    anchorItemScrollOffset: Int,
    lastMeasuredItemHeight: Int,
    scrollToBeConsumed: Float,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): LazyColumnMeasureResult {
    if (itemsCount == 0) {
        return LazyColumnMeasureResult(
            anchorItemIndex = 0,
            anchorItemScrollOffset = 0,
            visibleItems = emptyList(),
            totalItemsCount = 0,
            lastMeasuredItemHeight = Int.MIN_VALUE,
            canScrollForward = false,
            canScrollBackward = false,
            measureResult = layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {}
        )
    }

    val visibleItems = ArrayDeque<LazyColumnMeasuredItem>()
    var canScrollForward = true
    var canScrollBackward = true

    // Place center item
    val centerItem =
        if (lastMeasuredItemHeight > 0) {
            measuredItemProvider.downwardMeasuredItem(
                anchorItemIndex,
                anchorItemScrollOffset - lastMeasuredItemHeight + containerConstraints.maxHeight / 2
            )
        } else {
            measuredItemProvider.upwardMeasuredItem(
                anchorItemIndex,
                anchorItemScrollOffset + containerConstraints.maxHeight / 2
            )
        }
    centerItem.offset += scrollToBeConsumed.roundToInt()

    if (
        centerItem.index == 0 &&
            centerItem.offset + centerItem.height / 2 >= containerConstraints.maxHeight / 2
    ) {
        canScrollBackward = false
        centerItem.offset = containerConstraints.maxHeight / 2 - centerItem.height / 2
    }
    if (
        centerItem.index == itemsCount - 1 &&
            centerItem.offset + centerItem.height / 2 <= containerConstraints.maxHeight / 2
    ) {
        canScrollForward = false
        centerItem.offset = containerConstraints.maxHeight / 2 - centerItem.height / 2
    }

    visibleItems.add(centerItem)

    var bottomOffset = centerItem.offset + centerItem.height + itemSpacing
    var bottomPassIndex = anchorItemIndex + 1

    while (bottomOffset < containerConstraints.maxHeight && bottomPassIndex < itemsCount) {
        val item = measuredItemProvider.downwardMeasuredItem(bottomPassIndex, bottomOffset)
        bottomOffset += item.height + itemSpacing
        visibleItems.add(item)
        bottomPassIndex += 1
    }

    var topOffset = centerItem.offset - itemSpacing
    var topPassIndex = anchorItemIndex - 1

    while (topOffset >= 0 && topPassIndex >= 0) {
        val additionalItem = measuredItemProvider.upwardMeasuredItem(topPassIndex, topOffset)
        visibleItems.addFirst(additionalItem)
        topOffset -= additionalItem.height + itemSpacing
        topPassIndex -= 1
    }

    if (visibleItems.isEmpty()) {
        return LazyColumnMeasureResult(
            anchorItemIndex = 0,
            anchorItemScrollOffset = 0,
            visibleItems = emptyList(),
            totalItemsCount = 0,
            lastMeasuredItemHeight = Int.MIN_VALUE,
            canScrollForward = false,
            canScrollBackward = false,
            measureResult = layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {}
        )
    }

    val anchorItem =
        visibleItems.minByOrNull {
            abs(it.offset + it.height / 2 - containerConstraints.maxHeight / 2)
        } ?: centerItem

    return LazyColumnMeasureResult(
        anchorItemIndex = anchorItem.index,
        anchorItemScrollOffset =
            anchorItem.let { it.offset + it.height - containerConstraints.maxHeight / 2 },
        visibleItems = visibleItems,
        totalItemsCount = itemsCount,
        lastMeasuredItemHeight = anchorItem.height,
        canScrollForward = canScrollForward,
        canScrollBackward = canScrollBackward,
        measureResult =
            layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                visibleItems.fastForEach { it.place(this) }
            }
    )
}

private fun bottomItemScrollProgress(
    offset: Int,
    height: Int,
    containerHeight: Int
): LazyColumnItemScrollProgress =
    LazyColumnItemScrollProgressImpl(
        topOffsetFraction = offset.toFloat() / containerHeight.toFloat(),
        bottomOffsetFraction = (offset + height).toFloat() / containerHeight.toFloat(),
    )

private fun topItemScrollProgress(
    offset: Int,
    height: Int,
    containerHeight: Int
): LazyColumnItemScrollProgress =
    LazyColumnItemScrollProgressImpl(
        topOffsetFraction = (offset - height).toFloat() / containerHeight.toFloat(),
        bottomOffsetFraction = offset / containerHeight.toFloat(),
    )

internal data class LazyColumnItemScrollProgressImpl(
    override val topOffsetFraction: Float,
    override val bottomOffsetFraction: Float
) : LazyColumnItemScrollProgress

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun rememberLazyColumnMeasurePolicy(
    itemProviderLambda: () -> LazyColumnItemProvider,
    state: LazyColumnState,
    horizontalAlignment: Alignment.Horizontal,
    verticalArrangement: Arrangement.Vertical,
): LazyLayoutMeasureScope.(Constraints) -> MeasureResult =
    remember(itemProviderLambda, state, horizontalAlignment, verticalArrangement) {
        { containerConstraints ->
            val measuredItemProvider =
                object : MeasuredItemProvider {
                    override fun downwardMeasuredItem(
                        index: Int,
                        offset: Int
                    ): LazyColumnMeasuredItem {
                        val itemProvider = itemProviderLambda()
                        val childConstraints =
                            Constraints(
                                maxHeight = Constraints.Infinity,
                                maxWidth = containerConstraints.maxWidth
                            )
                        val placeables = measure(index, childConstraints)
                        // TODO(artemiy): Add support for multiple items.
                        val content = placeables.last()
                        val scrollProgress =
                            bottomItemScrollProgress(
                                offset = offset,
                                height = content.height,
                                containerHeight = containerConstraints.maxHeight
                            )
                        return LazyColumnMeasuredItem(
                            index = index,
                            placeable = content,
                            offset = offset,
                            containerConstraints = containerConstraints,
                            scrollProgress = scrollProgress,
                            horizontalAlignment = horizontalAlignment,
                            layoutDirection = layoutDirection,
                            key = itemProvider.getKey(index),
                            contentType = itemProvider.getContentType(index),
                        )
                    }

                    override fun upwardMeasuredItem(
                        index: Int,
                        offset: Int
                    ): LazyColumnMeasuredItem {
                        val itemProvider = itemProviderLambda()
                        val childConstraints =
                            Constraints(
                                maxHeight = Constraints.Infinity,
                                maxWidth = containerConstraints.maxWidth
                            )
                        val placeables = measure(index, childConstraints)
                        // TODO(artemiy): Add support for multiple items.
                        val content = placeables.last()
                        val scrollProgress =
                            topItemScrollProgress(
                                offset = offset,
                                height = content.height,
                                containerHeight = containerConstraints.maxHeight
                            )
                        val item =
                            LazyColumnMeasuredItem(
                                index = index,
                                placeable = content,
                                offset = offset,
                                containerConstraints = containerConstraints,
                                scrollProgress = scrollProgress,
                                horizontalAlignment = horizontalAlignment,
                                layoutDirection = layoutDirection,
                                key = itemProvider.getKey(index),
                                contentType = itemProvider.getContentType(index),
                            )
                        item.offset -= item.height
                        return item
                    }
                }

            Snapshot.withMutableSnapshot {
                    measureLazyColumn(
                        itemsCount = itemProviderLambda().itemCount,
                        measuredItemProvider = measuredItemProvider,
                        itemSpacing = verticalArrangement.spacing.roundToPx(),
                        containerConstraints = containerConstraints,
                        scrollToBeConsumed = state.scrollToBeConsumed,
                        anchorItemIndex = state.anchorItemIndex,
                        anchorItemScrollOffset = state.anchorItemScrollOffset,
                        lastMeasuredItemHeight = state.lastMeasuredAnchorItemHeight,
                        layout = { width, height, placement ->
                            layout(
                                containerConstraints.constrainWidth(width),
                                containerConstraints.constrainHeight(height),
                                emptyMap(),
                                placement
                            )
                        }
                    )
                }
                .also { state.applyMeasureResult(it) }
        }
    }
