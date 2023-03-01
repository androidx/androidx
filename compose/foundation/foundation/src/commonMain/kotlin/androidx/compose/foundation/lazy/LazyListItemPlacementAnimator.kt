/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateItemModifierNode
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * Handles the item placement animations when it is set via [LazyItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyListItemPlacementAnimator {
    // contains the keys of the active items with animation node.
    private val activeKeys = mutableSetOf<Any>()

    // snapshot of the key to index map used for the last measuring.
    private var keyToIndexMap: LazyLayoutKeyIndexMap = LazyLayoutKeyIndexMap.Empty

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = LinkedHashSet<Any>()
    private val movingInFromStartBound = mutableListOf<LazyListPositionedItem>()
    private val movingInFromEndBound = mutableListOf<LazyListPositionedItem>()
    private val movingAwayToStartBound = mutableListOf<LazyListMeasuredItem>()
    private val movingAwayToEndBound = mutableListOf<LazyListMeasuredItem>()

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        consumedScroll: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        positionedItems: MutableList<LazyListPositionedItem>,
        itemProvider: LazyListMeasuredItemProvider,
        isVertical: Boolean
    ) {
        if (!positionedItems.fastAny { it.hasAnimations } && activeKeys.isEmpty()) {
            // no animations specified - no work needed
            reset()
            return
        }

        val previousFirstVisibleIndex = firstVisibleIndex
        firstVisibleIndex = positionedItems.firstOrNull()?.index ?: 0
        val previousKeyToIndexMap = keyToIndexMap
        keyToIndexMap = itemProvider.keyToIndexMap

        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth

        // the consumed scroll is considered as a delta we don't need to animate
        val scrollOffset = if (isVertical) {
            IntOffset(0, consumedScroll)
        } else {
            IntOffset(consumedScroll, 0)
        }

        // first add all items we had in the previous run
        movingAwayKeys.addAll(activeKeys)
        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            // remove items we have in the current one as they are still visible.
            movingAwayKeys.remove(item.key)
            if (item.hasAnimations) {
                if (!activeKeys.contains(item.key)) {
                    activeKeys += item.key
                    val previousIndex = previousKeyToIndexMap[item.key]
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
                            item.getOffset(0).let { if (item.isVertical) it.y else it.x }
                        )
                    }
                } else {
                    repeat(item.placeablesCount) { placeableIndex ->
                        item.getParentData(placeableIndex).node?.apply {
                            if (rawOffset != LazyLayoutAnimateItemModifierNode.NotInitialized) {
                                rawOffset += scrollOffset
                            }
                        }
                    }
                    startAnimationsIfNeeded(item)
                }
            } else {
                // no animation, clean up if needed
                activeKeys.remove(item.key)
            }
        }

        var currentMainAxisOffset = 0
        movingInFromStartBound.sortByDescending { previousKeyToIndexMap[it.key] }
        movingInFromStartBound.fastForEach { item ->
            val mainAxisOffset = 0 - currentMainAxisOffset - item.size
            currentMainAxisOffset += item.size
            initializeNode(item, mainAxisOffset)
            startAnimationsIfNeeded(item)
        }
        currentMainAxisOffset = 0
        movingInFromEndBound.sortBy { previousKeyToIndexMap[it.key] }
        movingInFromEndBound.fastForEach { item ->
            val mainAxisOffset = mainAxisLayoutSize + currentMainAxisOffset
            currentMainAxisOffset += item.size
            initializeNode(item, mainAxisOffset)
            startAnimationsIfNeeded(item)
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val newIndex = keyToIndexMap[key]

            if (newIndex == -1) {
                activeKeys.remove(key)
            } else {
                val item = itemProvider.getAndMeasure(DataIndex(newIndex))
                // check if we have any active placement animation on the item
                var inProgress = false
                repeat(item.placeablesCount) {
                    if (item.getParentData(it).node?.isAnimationInProgress == true) {
                        inProgress = true
                        return@repeat
                    }
                }
                if ((!inProgress && newIndex == previousKeyToIndexMap[key])) {
                    activeKeys.remove(key)
                } else {
                    if (newIndex < firstVisibleIndex) {
                        movingAwayToStartBound.add(item)
                    } else {
                        movingAwayToEndBound.add(item)
                    }
                }
            }
        }

        currentMainAxisOffset = 0
        movingAwayToStartBound.sortByDescending { keyToIndexMap[it.key] }
        movingAwayToStartBound.fastForEach { item ->
            val mainAxisOffset = 0 - currentMainAxisOffset - item.size
            currentMainAxisOffset += item.size

            val positionedItem = item.position(mainAxisOffset, layoutWidth, layoutHeight)
            positionedItems.add(positionedItem)
            startAnimationsIfNeeded(positionedItem)
        }
        currentMainAxisOffset = 0
        movingAwayToEndBound.sortBy { keyToIndexMap[it.key] }
        movingAwayToEndBound.fastForEach { item ->
            val mainAxisOffset = mainAxisLayoutSize + currentMainAxisOffset
            currentMainAxisOffset += item.size

            val positionedItem = item.position(mainAxisOffset, layoutWidth, layoutHeight)
            positionedItems.add(positionedItem)
            startAnimationsIfNeeded(positionedItem)
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
        activeKeys.clear()
        keyToIndexMap = LazyLayoutKeyIndexMap.Empty
        firstVisibleIndex = -1
    }

    private fun initializeNode(
        item: LazyListPositionedItem,
        mainAxisOffset: Int
    ) {
        val firstPlaceableOffset = item.getOffset(0)

        val targetFirstPlaceableOffset = if (item.isVertical) {
            firstPlaceableOffset.copy(y = mainAxisOffset)
        } else {
            firstPlaceableOffset.copy(x = mainAxisOffset)
        }

        // initialize offsets
        repeat(item.placeablesCount) { placeableIndex ->
            val node = item.getParentData(placeableIndex).node
            if (node != null) {
                val diffToFirstPlaceableOffset =
                    item.getOffset(placeableIndex) - firstPlaceableOffset
                node.rawOffset = targetFirstPlaceableOffset + diffToFirstPlaceableOffset
            }
        }
    }

    private fun startAnimationsIfNeeded(item: LazyListPositionedItem) {
        repeat(item.placeablesCount) { placeableIndex ->
            val node = item.getParentData(placeableIndex).node
            if (node != null) {
                val newTarget = item.getOffset(placeableIndex)
                val currentTarget = node.rawOffset
                if (currentTarget == LazyLayoutAnimateItemModifierNode.NotInitialized) {
                    node.rawOffset = newTarget
                } else if (currentTarget != newTarget) {
                    node.rawOffset = newTarget
                    node.animatePlacementDelta(newTarget - currentTarget)
                }
            }
        }
    }

    private val Any?.node get() = this as? LazyLayoutAnimateItemModifierNode

    private val LazyListPositionedItem.hasAnimations: Boolean
        get() {
            repeat(placeablesCount) { index ->
                if (getParentData(index).node != null) {
                    return true
                }
            }
            return false
        }
}
