/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.lazy.layout.LazyLayoutAnimation
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimationSpecsNode
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

/**
 * Handles the item placement animations when it is set via [LazyGridItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyGridItemPlacementAnimator {
    // state containing relevant info for active items.
    private val keyToItemInfoMap = mutableMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyIndexMap: LazyLayoutKeyIndexMap = LazyLayoutKeyIndexMap.Empty

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = LinkedHashSet<Any>()
    private val movingInFromStartBound = mutableListOf<LazyGridMeasuredItem>()
    private val movingInFromEndBound = mutableListOf<LazyGridMeasuredItem>()
    private val movingAwayToStartBound = mutableListOf<LazyGridMeasuredItem>()
    private val movingAwayToEndBound = mutableListOf<LazyGridMeasuredItem>()

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        consumedScroll: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        positionedItems: MutableList<LazyGridMeasuredItem>,
        itemProvider: LazyGridMeasuredItemProvider,
        spanLayoutProvider: LazyGridSpanLayoutProvider,
        isVertical: Boolean,
        coroutineScope: CoroutineScope
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
                    val newItemInfo = ItemInfo(item.crossAxisSize, item.crossAxisOffset)
                    newItemInfo.updateAnimation(item, coroutineScope)
                    keyToItemInfoMap[item.key] = newItemInfo
                    val previousIndex = previousKeyToIndexMap.getIndex(item.key)
                    if (previousIndex != -1 && item.index != previousIndex) {
                        if (previousIndex < previousFirstVisibleIndex) {
                            // the larger index will be in the start of the list
                            movingInFromStartBound.add(item)
                        } else {
                            movingInFromEndBound.add(item)
                        }
                    } else {
                        initializeAnimation(
                            item,
                            item.offset.let { if (item.isVertical) it.y else it.x },
                            newItemInfo
                        )
                    }
                } else {
                    itemInfo.animations.forEach { animation ->
                        if (animation != null &&
                            animation.rawOffset != LazyLayoutAnimation.NotInitialized
                        ) {
                            animation.rawOffset += scrollOffset
                        }
                    }
                    itemInfo.crossAxisSize = item.crossAxisSize
                    itemInfo.crossAxisOffset = item.crossAxisOffset
                    startAnimationsIfNeeded(item)
                }
            } else {
                // no animation, clean up if needed
                keyToItemInfoMap.remove(item.key)
            }
        }

        var accumulatedOffset = 0
        var previousLine = -1
        var previousLineMainAxisSize = 0
        movingInFromStartBound.sortByDescending { previousKeyToIndexMap.getIndex(it.key) }
        movingInFromStartBound.fastForEach { item ->
            val line = if (isVertical) item.row else item.column
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.mainAxisSize)
            } else {
                accumulatedOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.mainAxisSize
                previousLine = line
            }
            val mainAxisOffset = 0 - accumulatedOffset - item.mainAxisSize
            initializeAnimation(item, mainAxisOffset)
            startAnimationsIfNeeded(item)
        }
        accumulatedOffset = 0
        previousLine = -1
        previousLineMainAxisSize = 0
        movingInFromEndBound.sortBy { previousKeyToIndexMap.getIndex(it.key) }
        movingInFromEndBound.fastForEach { item ->
            val line = if (isVertical) item.row else item.column
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.mainAxisSize)
            } else {
                accumulatedOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.mainAxisSize
                previousLine = line
            }
            val mainAxisOffset = mainAxisLayoutSize + accumulatedOffset
            initializeAnimation(item, mainAxisOffset)
            startAnimationsIfNeeded(item)
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
                    constraints = if (isVertical) {
                        Constraints.fixedWidth(itemInfo.crossAxisSize)
                    } else {
                        Constraints.fixedHeight(itemInfo.crossAxisSize)
                    }
                )
                // check if we have any active placement animation on the item
                val inProgress =
                    itemInfo.animations.any { it?.isPlacementAnimationInProgress == true }
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

        accumulatedOffset = 0
        previousLine = -1
        previousLineMainAxisSize = 0
        movingAwayToStartBound.sortByDescending { keyIndexMap.getIndex(it.key) }
        movingAwayToStartBound.fastForEach { item ->
            val line = spanLayoutProvider.getLineIndexOfItem(item.index)
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.mainAxisSize)
            } else {
                accumulatedOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.mainAxisSize
                previousLine = line
            }
            val mainAxisOffset = 0 - accumulatedOffset - item.mainAxisSize

            val itemInfo = keyToItemInfoMap.getValue(item.key)

            item.position(
                mainAxisOffset = mainAxisOffset,
                crossAxisOffset = itemInfo.crossAxisOffset,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight
            )
            positionedItems.add(item)
            startAnimationsIfNeeded(item)
        }
        accumulatedOffset = 0
        previousLine = -1
        previousLineMainAxisSize = 0
        movingAwayToEndBound.sortBy { keyIndexMap.getIndex(it.key) }
        movingAwayToEndBound.fastForEach { item ->
            val line = spanLayoutProvider.getLineIndexOfItem(item.index)
            if (line != -1 && line == previousLine) {
                previousLineMainAxisSize = maxOf(previousLineMainAxisSize, item.mainAxisSize)
            } else {
                accumulatedOffset += previousLineMainAxisSize
                previousLineMainAxisSize = item.mainAxisSize
                previousLine = line
            }
            val mainAxisOffset = mainAxisLayoutSize + accumulatedOffset

            val itemInfo = keyToItemInfoMap.getValue(item.key)
            item.position(
                mainAxisOffset = mainAxisOffset,
                crossAxisOffset = itemInfo.crossAxisOffset,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight,
            )

            positionedItems.add(item)
            startAnimationsIfNeeded(item)
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
        keyIndexMap = LazyLayoutKeyIndexMap.Empty
        firstVisibleIndex = -1
    }

    private fun initializeAnimation(
        item: LazyGridMeasuredItem,
        mainAxisOffset: Int,
        itemInfo: ItemInfo = keyToItemInfoMap.getValue(item.key)
    ) {
        val firstPlaceableOffset = item.offset

        val targetFirstPlaceableOffset = if (item.isVertical) {
            firstPlaceableOffset.copy(y = mainAxisOffset)
        } else {
            firstPlaceableOffset.copy(x = mainAxisOffset)
        }

        // initialize offsets
        itemInfo.animations.forEach { animation ->
            if (animation != null) {
                val diffToFirstPlaceableOffset =
                    item.offset - firstPlaceableOffset
                animation.rawOffset = targetFirstPlaceableOffset + diffToFirstPlaceableOffset
            }
        }
    }

    private fun startAnimationsIfNeeded(item: LazyGridMeasuredItem) {
        val itemInfo = keyToItemInfoMap.getValue(item.key)
        itemInfo.animations.forEach { animation ->
            if (animation != null) {
                val newTarget = item.offset
                val currentTarget = animation.rawOffset
                if (currentTarget != LazyLayoutAnimation.NotInitialized &&
                    currentTarget != newTarget
                ) {
                    animation.animatePlacementDelta(newTarget - currentTarget)
                }
                animation.rawOffset = newTarget
            }
        }
    }

    fun getAnimation(key: Any, placeableIndex: Int): LazyLayoutAnimation? =
        keyToItemInfoMap[key]?.animations?.get(placeableIndex)

    private val LazyGridMeasuredItem.hasAnimations: Boolean
        get() {
            repeat(placeablesCount) { index ->
                getParentData(index).specs?.let {
                    // found at least one
                    return true
                }
            }
            return false
        }
}

private class ItemInfo(
    var crossAxisSize: Int,
    var crossAxisOffset: Int
) {
    /**
     * This array will have the same amount of elements as there are placeables on the item.
     * If the element is not null this means there are specs associated with the given placeable.
     */
    var animations = EmptyArray
        private set

    fun updateAnimation(positionedItem: LazyGridMeasuredItem, coroutineScope: CoroutineScope) {
        for (i in positionedItem.placeablesCount until animations.size) {
            animations[i]?.stopAnimations()
        }
        if (animations.size != positionedItem.placeablesCount) {
            animations = animations.copyOf(positionedItem.placeablesCount)
        }
        repeat(positionedItem.placeablesCount) { index ->
            val specs = positionedItem.getParentData(index).specs
            if (specs == null) {
                animations[index]?.stopAnimations()
                animations[index] = null
            } else {
                val item = animations[index] ?: LazyLayoutAnimation(coroutineScope).also {
                    animations[index] = it
                }
                item.appearanceSpec = specs.appearanceSpec
                item.placementSpec = specs.placementSpec
            }
        }
    }
}

private val Any?.specs get() = this as? LazyLayoutAnimationSpecsNode

private val EmptyArray = emptyArray<LazyLayoutAnimation?>()
