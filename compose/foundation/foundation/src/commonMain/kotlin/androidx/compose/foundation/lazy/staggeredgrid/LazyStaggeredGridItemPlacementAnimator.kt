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

import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimation
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimationSpecsNode
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

/**
 * Handles the item placement animations when it is set via
 * [LazyStaggeredGridItemScope.animateItemPlacement].
 *
 * This class is responsible for detecting when item position changed, figuring our start/end
 * offsets and starting the animations.
 */
internal class LazyStaggeredGridItemPlacementAnimator {
    // state containing relevant info for active items.
    private val keyToItemInfoMap = mutableScatterMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyIndexMap: LazyLayoutKeyIndexMap = LazyLayoutKeyIndexMap

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = mutableScatterSetOf<Any>()
    private val movingInFromStartBound = mutableListOf<LazyStaggeredGridMeasuredItem>()
    private val movingInFromEndBound = mutableListOf<LazyStaggeredGridMeasuredItem>()
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
        positionedItems: MutableList<LazyStaggeredGridMeasuredItem>,
        itemProvider: LazyStaggeredGridMeasureProvider,
        isVertical: Boolean,
        laneCount: Int,
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
        keyToItemInfoMap.forEachKey { movingAwayKeys.add(it) }
        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            // remove items we have in the current one as they are still visible.
            movingAwayKeys.remove(item.key)
            if (item.hasAnimations) {
                val itemInfo = keyToItemInfoMap[item.key]
                // there is no state associated with this item yet
                if (itemInfo == null) {
                    val newItemInfo = ItemInfo(item.lane, item.span, item.crossAxisOffset)
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
                initializeAnimation(item, mainAxisOffset)
                startAnimationsIfNeeded(item)
            }
            accumulatedOffsetPerLane.fill(0)
        }
        if (movingInFromEndBound.isNotEmpty()) {
            movingInFromEndBound.sortBy { previousKeyToIndexMap.getIndex(it.key) }
            movingInFromEndBound.fastForEach { item ->
                val mainAxisOffset = mainAxisLayoutSize + accumulatedOffsetPerLane[item.lane]
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize
                initializeAnimation(item, mainAxisOffset)
                startAnimationsIfNeeded(item)
            }
            accumulatedOffsetPerLane.fill(0)
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val itemInfo = keyToItemInfoMap[key]!!
            val newIndex = keyIndexMap.getIndex(key)

            if (newIndex == -1) {
                keyToItemInfoMap.remove(key)
            } else {
                val item = itemProvider.getAndMeasure(
                    newIndex,
                    SpanRange(itemInfo.lane, itemInfo.span)
                )
                item.nonScrollableItem = true
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

        if (movingAwayToStartBound.isNotEmpty()) {
            movingAwayToStartBound.sortByDescending { keyIndexMap.getIndex(it.key) }
            movingAwayToStartBound.fastForEach { item ->
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize
                val mainAxisOffset = 0 - accumulatedOffsetPerLane[item.lane]

                val itemInfo = keyToItemInfoMap[item.key]!!
                item.position(mainAxisOffset, itemInfo.crossAxisOffset, mainAxisLayoutSize)
                positionedItems.add(item)
                startAnimationsIfNeeded(item)
            }
            accumulatedOffsetPerLane.fill(0)
        }
        if (movingAwayToEndBound.isNotEmpty()) {
            movingAwayToEndBound.sortBy { keyIndexMap.getIndex(it.key) }
            movingAwayToEndBound.fastForEach { item ->
                val mainAxisOffset = mainAxisLayoutSize + accumulatedOffsetPerLane[item.lane]
                accumulatedOffsetPerLane[item.lane] += item.mainAxisSize

                val itemInfo = keyToItemInfoMap[item.key]!!
                item.position(mainAxisOffset, itemInfo.crossAxisOffset, mainAxisLayoutSize)
                positionedItems.add(item)
                startAnimationsIfNeeded(item)
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

    private fun initializeAnimation(
        item: LazyStaggeredGridMeasuredItem,
        mainAxisOffset: Int,
        itemInfo: ItemInfo = keyToItemInfoMap[item.key]!!
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

    private fun startAnimationsIfNeeded(item: LazyStaggeredGridMeasuredItem) {
        val itemInfo = keyToItemInfoMap[item.key]!!
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

    fun getAnimation(key: Any, placeableIndex: Int): LazyLayoutAnimation? {
        return if (keyToItemInfoMap.isEmpty()) {
            null
        } else {
            keyToItemInfoMap[key]?.animations?.get(placeableIndex)
        }
    }

    private val LazyStaggeredGridMeasuredItem.hasAnimations: Boolean
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
    var lane: Int,
    var span: Int,
    var crossAxisOffset: Int
) {
    /**
     * This array will have the same amount of elements as there are placeables on the item.
     * If the element is not null this means there are specs associated with the given placeable.
     */
    var animations = EmptyArray
        private set

    fun updateAnimation(
        positionedItem: LazyStaggeredGridMeasuredItem,
        coroutineScope: CoroutineScope
    ) {
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
