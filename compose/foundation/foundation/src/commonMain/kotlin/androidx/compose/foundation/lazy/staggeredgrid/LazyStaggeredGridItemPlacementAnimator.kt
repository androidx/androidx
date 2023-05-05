/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateItemModifierNode
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * Handles the item placement animations when it is set via
 * [LazyStaggeredGridItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyStaggeredGridItemPlacementAnimator {
    // state containing relevant info for active items.
    private val keyToItemInfoMap = mutableMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyIndexMap: LazyLayoutKeyIndexMap = LazyLayoutKeyIndexMap

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = LinkedHashSet<Any>()
    private val movingInFromStartBound = mutableListOf<LazyStaggeredGridPositionedItem>()
    private val movingInFromEndBound = mutableListOf<LazyStaggeredGridPositionedItem>()
    private val movingAwayToStartBound = mutableListOf<LazyStaggeredGridMeasuredItem>()
    private val movingAwayToEndBound = mutableListOf<LazyStaggeredGridMeasuredItem>()

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        consumedScroll: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        positionedItems: MutableList<LazyStaggeredGridPositionedItem>,
        itemProvider: LazyStaggeredGridMeasureProvider,
        isVertical: Boolean,
        laneCount: Int
    ) {
        if (!positionedItems.fastAny { it.hasAnimations } && keyToItemInfoMap.isEmpty()) {
            // no animations specified - no work needed
            reset()
            return
        }

        val previousFirstVisibleIndex = firstVisibleIndex
        firstVisibleIndex = positionedItems.firstOrNull()?.index ?: 0
        val previousKeyToIndexMap = keyIndexMap
        keyIndexMap = itemProvider.keyIndexMap

        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth

        // the consumed scroll is considered as a delta we don't need to animate
        val scrollOffset = if (isVertical) {
            IntOffset(0, consumedScroll)
        } else {
            IntOffset(consumedScroll, 0)
        }

        // first add all items we had in the previous run
        movingAwayKeys.addAll(keyToItemInfoMap.keys)
        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            // remove items we have in the current one as they are still visible.
            movingAwayKeys.remove(item.key)
            if (item.hasAnimations) {
                val itemInfo = keyToItemInfoMap[item.key]
                // there is no state associated with this item yet
                if (itemInfo == null) {
                    keyToItemInfoMap[item.key] =
                        ItemInfo(item.lane, item.span, item.crossAxisOffset)
                    val previousIndex = previousKeyToIndexMap.getIndex(item.key)
                    if (previousIndex != -1 && item.index != previousIndex) {
                        if (previousIndex < previousFirstVisibleIndex) {
                            // the larger index will be in the start of the list
                            movingInFromStartBound.add(item)
                        } else {
                            movingInFromEndBound.add(item)
                        }
                    } else {
                        initializeNode(
                            item,
                            item.offset.let { if (item.isVertical) it.y else it.x }
                        )
                    }
                } else {
                    item.forEachNode {
                        if (it.rawOffset != LazyLayoutAnimateItemModifierNode.NotInitialized) {
                            it.rawOffset += scrollOffset
                        }
                    }
                    itemInfo.lane = item.lane
                    itemInfo.span = item.span
                    itemInfo.crossAxisOffset = item.crossAxisOffset
                    startAnimationsIfNeeded(item)
                }
            } else {
                // no animation, clean up if needed
                keyToItemInfoMap.remove(item.key)
            }
        }

        val accumulatedOffsetPerLane = IntArray(laneCount) { 0 }
        if (movingInFromStartBound.isNotEmpty()) {
            movingInFromStartBound.sortByDescending { previousKeyToIndexMap.getIndex(it.key) }
            movingInFromStartBound.fastForEach { item ->
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize
                val mainAxisOffset = 0 - accumulatedOffsetPerLane[item.lane]
                initializeNode(item, mainAxisOffset)
                startAnimationsIfNeeded(item)
            }
            accumulatedOffsetPerLane.fill(0)
        }
        if (movingInFromEndBound.isNotEmpty()) {
            movingInFromEndBound.sortBy { previousKeyToIndexMap.getIndex(it.key) }
            movingInFromEndBound.fastForEach { item ->
                val mainAxisOffset = mainAxisLayoutSize + accumulatedOffsetPerLane[item.lane]
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize
                initializeNode(item, mainAxisOffset)
                startAnimationsIfNeeded(item)
            }
            accumulatedOffsetPerLane.fill(0)
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val itemInfo = keyToItemInfoMap.getValue(key)
            val newIndex = keyIndexMap.getIndex(key)

            if (newIndex == -1) {
                keyToItemInfoMap.remove(key)
            } else {
                val item = itemProvider.getAndMeasure(
                    newIndex,
                    SpanRange(itemInfo.lane, itemInfo.span)
                )
                // check if we have any active placement animation on the item
                var inProgress = false
                repeat(item.placeablesCount) {
                    if (item.getParentData(it).node?.isAnimationInProgress == true) {
                        inProgress = true
                        return@repeat
                    }
                }
                if ((!inProgress && newIndex == previousKeyToIndexMap.getIndex(key))) {
                    keyToItemInfoMap.remove(key)
                } else {
                    if (newIndex < firstVisibleIndex) {
                        movingAwayToStartBound.add(item)
                    } else {
                        movingAwayToEndBound.add(item)
                    }
                }
            }
        }

        if (movingAwayToStartBound.isNotEmpty()) {
            movingAwayToStartBound.sortByDescending { keyIndexMap.getIndex(it.key) }
            movingAwayToStartBound.fastForEach { item ->
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize
                val mainAxisOffset = 0 - accumulatedOffsetPerLane[item.lane]

                val itemInfo = keyToItemInfoMap.getValue(item.key)
                val positionedItem =
                    item.position(mainAxisOffset, itemInfo.crossAxisOffset, mainAxisLayoutSize)
                positionedItems.add(positionedItem)
                startAnimationsIfNeeded(positionedItem)
            }
            accumulatedOffsetPerLane.fill(0)
        }
        if (movingAwayToEndBound.isNotEmpty()) {
            movingAwayToEndBound.sortBy { keyIndexMap.getIndex(it.key) }
            movingAwayToEndBound.fastForEach { item ->
                val mainAxisOffset = mainAxisLayoutSize + accumulatedOffsetPerLane[item.lane]
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize

                val itemInfo = keyToItemInfoMap.getValue(item.key)
                val positionedItem =
                    item.position(mainAxisOffset, itemInfo.crossAxisOffset, mainAxisLayoutSize)
                positionedItems.add(positionedItem)
                startAnimationsIfNeeded(positionedItem)
            }
        }

        movingInFromStartBound.clear()
        movingInFromEndBound.clear()
        movingAwayToStartBound.clear()
        movingAwayToEndBound.clear()
        movingAwayKeys.clear()
    }

    /**
     * Should be called when the animations are not needed for the next positions change,
     * for example when we snap to a new position.
     */
    fun reset() {
        keyToItemInfoMap.clear()
        keyIndexMap = LazyLayoutKeyIndexMap
        firstVisibleIndex = -1
    }

    private fun initializeNode(
        item: LazyStaggeredGridPositionedItem,
        mainAxisOffset: Int
    ) {
        val firstPlaceableOffset = item.offset

        val targetFirstPlaceableOffset = if (item.isVertical) {
            firstPlaceableOffset.copy(y = mainAxisOffset)
        } else {
            firstPlaceableOffset.copy(x = mainAxisOffset)
        }

        // initialize offsets
        item.forEachNode { node ->
            val diffToFirstPlaceableOffset =
                item.offset - firstPlaceableOffset
            node.rawOffset = targetFirstPlaceableOffset + diffToFirstPlaceableOffset
        }
    }

    private fun startAnimationsIfNeeded(item: LazyStaggeredGridPositionedItem) {
        item.forEachNode { node ->
            val newTarget = item.offset
            val currentTarget = node.rawOffset
            if (currentTarget != LazyLayoutAnimateItemModifierNode.NotInitialized &&
                currentTarget != newTarget
            ) {
                node.animatePlacementDelta(newTarget - currentTarget)
            }
            node.rawOffset = newTarget
        }
    }

    private val Any?.node get() = this as? LazyLayoutAnimateItemModifierNode

    private val LazyStaggeredGridPositionedItem.hasAnimations: Boolean
        get() {
            forEachNode { return true }
            return false
        }

    private inline fun LazyStaggeredGridPositionedItem.forEachNode(
        block: (LazyLayoutAnimateItemModifierNode) -> Unit
    ) {
        repeat(placeablesCount) { index ->
            getParentData(index).node?.let(block)
        }
    }
}

private class ItemInfo(
    var lane: Int,
    var span: Int,
    var crossAxisOffset: Int
)
