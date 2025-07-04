/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.min

@Composable
internal fun rememberGlimmerListMeasurePolicy(
    /** Items provider of the list. */
    itemProviderLambda: () -> GlimmerListItemProvider,
    /** The state of the list. */
    state: ListState,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** Number of items to layout before and after the visible items */
    orientation: Orientation,
    /** The alignment to align items horizontally */
    horizontalAlignment: Alignment.Horizontal?,
    /** The alignment to align items vertically */
    verticalAlignment: Alignment.Vertical?,
    /** The horizontal arrangement for items */
    horizontalArrangement: Arrangement.Horizontal?,
    /** The vertical arrangement for items */
    verticalArrangement: Arrangement.Vertical?,
): LazyLayoutMeasureScope.(Constraints) -> MeasureResult =
    remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
        state,
        contentPadding,
        reverseLayout,
        orientation,
        horizontalAlignment,
        verticalAlignment,
        horizontalArrangement,
        verticalArrangement,
    ) {
        { containerConstraints ->
            checkScrollableContainerConstraints(containerConstraints, orientation)
            val layoutProperties =
                resolveLayoutProperties(
                    orientation = orientation,
                    contentPadding = contentPadding,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = verticalArrangement,
                    containerConstraints = containerConstraints,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                )
            val itemProvider = itemProviderLambda()
            // this will update the scope used by the item composables
            itemProvider.itemScope.setMaxSize(
                width = layoutProperties.contentConstraints.maxWidth,
                height = layoutProperties.contentConstraints.maxHeight,
            )

            val itemsCount = itemProvider.itemCount

            val measuredItemProvider =
                GlimmerListMeasuredItemProvider(
                    constraints = layoutProperties.contentConstraints,
                    layoutProperties = layoutProperties,
                    itemProvider = itemProvider,
                    measureScope = this,
                )

            val firstVisibleItemIndex: Int
            val firstVisibleScrollOffset: Int
            Snapshot.withoutReadObservation {
                firstVisibleItemIndex =
                    state.updateScrollPositionIfTheFirstItemWasMoved(
                        itemProvider,
                        state.firstVisibleItemIndex,
                    )
                firstVisibleScrollOffset = state.firstVisibleItemScrollOffset
            }

            val pinnedIndices: IntList =
                calculateLazyLayoutPinnedIndices(
                    itemProvider = itemProvider,
                    pinnedItemList = state.pinnedItems,
                    beyondBoundsInfo = state.beyondBoundsInfo,
                )

            val density = this
            with(layoutProperties) {
                measureGlimmerList(
                        itemsCount = itemsCount,
                        measuredItemProvider = measuredItemProvider,
                        firstVisibleItemIndex = firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = firstVisibleScrollOffset,
                        scrollToBeConsumed = state.scrollToBeConsumed,
                        pinnedIndices = pinnedIndices,
                        reverseLayout = reverseLayout,
                        density = density,
                        layout = { width, height, placement ->
                            layout(
                                containerConstraints.constrainWidth(width + totalHorizontalPadding),
                                containerConstraints.constrainHeight(height + totalVerticalPadding),
                                emptyMap(),
                                placement,
                            )
                        },
                    )
                    .also { state.applyMeasureResult(it) }
            }
        }
    }

private fun calculateLazyLayoutPinnedIndices(
    itemProvider: LazyLayoutItemProvider,
    pinnedItemList: LazyLayoutPinnedItemList,
    beyondBoundsInfo: LazyLayoutBeyondBoundsInfo,
): IntList {
    if (!beyondBoundsInfo.hasIntervals() && pinnedItemList.isEmpty()) {
        return emptyIntList()
    }
    val pinnedItems = MutableIntList()
    val beyondBoundsRange =
        if (beyondBoundsInfo.hasIntervals()) {
            beyondBoundsInfo.start..min(beyondBoundsInfo.end, itemProvider.itemCount - 1)
        } else {
            IntRange.EMPTY
        }
    pinnedItemList.fastForEach {
        val index = itemProvider.findIndexByKey(it.key, it.index)
        if (index in beyondBoundsRange) return@fastForEach
        if (index !in 0 until itemProvider.itemCount) return@fastForEach
        pinnedItems.add(index)
    }
    for (i in beyondBoundsRange) {
        pinnedItems.add(i)
    }
    return pinnedItems
}
