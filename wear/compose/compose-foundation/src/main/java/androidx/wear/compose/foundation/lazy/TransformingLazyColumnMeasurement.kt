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
import androidx.collection.emptyIntList
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace
import kotlinx.coroutines.CoroutineScope

internal fun interface MeasuredItemProvider {
    /**
     * Creates a [TransformingLazyColumnMeasuredItem] with the given index and offset with the
     * position calculated from top.
     */
    fun measuredItem(
        index: Int,
        offset: Int,
        measurementDirection: MeasurementDirection,
        progressProvider: (Int) -> TransformingLazyColumnItemScrollProgress,
    ): TransformingLazyColumnMeasuredItem
}

@Composable
internal fun rememberTransformingLazyColumnMeasurePolicy(
    itemProviderLambda: () -> TransformingLazyColumnItemProvider,
    state: TransformingLazyColumnState,
    coroutineScope: CoroutineScope,
    horizontalAlignment: Alignment.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    measurementStrategy: TransformingLazyColumnMeasurementStrategy,
    reverseLayout: Boolean,
): LazyLayoutMeasurePolicy =
    remember(
        itemProviderLambda,
        state,
        coroutineScope,
        horizontalAlignment,
        verticalArrangement,
        measurementStrategy,
    ) {
        LazyLayoutMeasurePolicy { containerConstraints ->
            val placeablesCache = mutableIntObjectMapOf<List<Placeable>>()
            fun LazyLayoutMeasureScope.getPlaceables(
                index: Int,
                constraints: Constraints,
            ): List<Placeable> {
                return placeablesCache.getOrPut(index) {
                    val measurables = compose(index)
                    List(measurables.size) { i -> measurables[i].measure(constraints) }
                        .also { placeablesCache[index] = it }
                }
            }

            val childConstraints =
                Constraints(
                    maxHeight = Constraints.Infinity,
                    maxWidth =
                        (containerConstraints.maxWidth -
                                measurementStrategy.leftContentPadding -
                                measurementStrategy.rightContentPadding)
                            .coerceAtLeast(0),
                )
            val itemProvider = itemProviderLambda()

            val measuredItemProvider =
                MeasuredItemProvider { index, offset, measurementDirection, progressProvider ->
                    val placeables = getPlaceables(index, childConstraints)
                    // TODO(artemiy): Add support for multiple items.
                    val placeable = placeables.lastOrNull()
                    val key = itemProvider.getKey(index)
                    TransformingLazyColumnMeasuredItem(
                        index = index,
                        placeable = placeable,
                        offset = offset,
                        containerConstraints = containerConstraints,
                        measureScrollProgress = progressProvider(placeable?.height ?: 0),
                        measurementDirection = measurementDirection,
                        horizontalAlignment = horizontalAlignment,
                        layoutDirection = layoutDirection,
                        reverseLayout = reverseLayout,
                        key = key,
                        spacing = verticalArrangement.spacing.roundToPx(),
                        leftPadding = measurementStrategy.leftContentPadding,
                        rightPadding = measurementStrategy.rightContentPadding,
                        animationProvider = { state.animator.getAnimation(key) },
                        contentType = itemProvider.getContentType(index),
                    )
                }

            val itemsCount = itemProviderLambda().itemCount

            val anchorItemKey: Any
            val anchorItemIndex: Int
            val anchorItemScrollOffset: Int
            val lastMeasuredAnchorItemHeight: Int
            val scrollToBeConsumed: Float
            Snapshot.withoutReadObservation {
                anchorItemKey = state.anchorItemKey
                anchorItemIndex =
                    if (itemsCount == 0) 0 else state.anchorItemIndex.coerceIn(0 until itemsCount)
                anchorItemScrollOffset = state.anchorItemScrollOffset
                lastMeasuredAnchorItemHeight = state.lastMeasuredAnchorItemHeight
                scrollToBeConsumed = state.scrollToBeConsumed
            }

            val pinnedItems =
                itemProvider.calculateLazyLayoutPinnedIndices(pinnedItemList = state.pinnedItems)

            Snapshot.withMutableSnapshot {
                    trace("wear-compose:tlc:measure") {
                        measurementStrategy.measure(
                            itemsCount = itemsCount,
                            keyIndexMap = itemProvider.keyIndexMap,
                            measuredItemProvider = measuredItemProvider,
                            verticalArrangement = verticalArrangement,
                            containerConstraints = containerConstraints,
                            scrollToBeConsumed = scrollToBeConsumed,
                            anchorItemKey = anchorItemKey,
                            anchorItemIndex = anchorItemIndex,
                            anchorItemScrollOffset = anchorItemScrollOffset,
                            lastMeasuredAnchorItemHeight = lastMeasuredAnchorItemHeight,
                            coroutineScope = coroutineScope,
                            density = this,
                            pinnedItems = pinnedItems,
                            layout = { width, height, placement ->
                                layout(
                                    containerConstraints.constrainWidth(width),
                                    containerConstraints.constrainHeight(height),
                                    emptyMap(),
                                    placement,
                                )
                            },
                        )
                    }
                }
                .also { state.applyMeasureResult(it) }
        }
    }

internal enum class MeasurementDirection {
    /**
     * Indicates that the item is being measured downward. This corresponds to using
     * [TransformingLazyColumnItemScrollProgress.downwardMeasuredItemScrollProgress].
     */
    DOWNWARD,

    /**
     * Indicates that the item is being measured upward This corresponds to using
     * [TransformingLazyColumnItemScrollProgress.upwardMeasuredItemScrollProgress].
     */
    UPWARD,
}

private fun TransformingLazyColumnItemProvider.calculateLazyLayoutPinnedIndices(
    pinnedItemList: LazyLayoutPinnedItemList
): IntList {
    if (pinnedItemList.isEmpty()) {
        return emptyIntList()
    } else {
        val pinnedItems = mutableIntListOf()
        pinnedItemList.fastForEach {
            val index = findIndexByKey(it.key, it.index)
            if (index in 0 until itemCount) {
                pinnedItems.add(index)
            }
        }
        pinnedItems.sort()
        return pinnedItems
    }
}

/**
 * Finds the position of the item with the given key in the lists. This logic allows us to detect
 * when there were items added or removed before our current first item.
 */
private fun TransformingLazyColumnItemProvider.findIndexByKey(key: Any?, lastKnownIndex: Int): Int {
    if (key == null || itemCount == 0) {
        // there were no real items during the previous measure
        return lastKnownIndex
    }
    if (lastKnownIndex < itemCount && key == getKey(lastKnownIndex)) {
        // this item is still at the same index
        return lastKnownIndex
    }
    val newIndex = getIndex(key)
    if (newIndex != -1) {
        return newIndex
    }
    // fallback to the previous index if we don't know the new index of the item
    return lastKnownIndex
}
