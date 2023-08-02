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

import androidx.compose.foundation.lazy.layout.LazyLayoutAnimation
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimationSpecsNode
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

/**
 * Handles the item animations when it is set via [LazyItemScope.animateItemPlacement].
 *
 * This class is responsible for:
 * - animating item appearance for the new items.
 * - detecting when item position changed, figuring our start/end offsets and starting the
 * animations for placement animations.
 */
internal class LazyListItemAnimator {
    // state containing relevant info for active items.
    private val keyToItemInfoMap = mutableMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyIndexMap: LazyLayoutKeyIndexMap? = null

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = LinkedHashSet<Any>()
    private val movingInFromStartBound = mutableListOf<LazyListMeasuredItem>()
    private val movingInFromEndBound = mutableListOf<LazyListMeasuredItem>()
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
        positionedItems: MutableList<LazyListMeasuredItem>,
        itemProvider: LazyListMeasuredItemProvider,
        isVertical: Boolean,
        isLookingAhead: Boolean,
        hasLookaheadOccurred: Boolean,
        coroutineScope: CoroutineScope
    ) {
        val previousKeyToIndexMap = this.keyIndexMap
        val keyIndexMap = itemProvider.keyIndexMap
        this.keyIndexMap = keyIndexMap

        val hasAnimations = positionedItems.fastAny { it.hasAnimations }
        if (!hasAnimations && keyToItemInfoMap.isEmpty()) {
            // no animations specified - no work needed
            reset()
            return
        }

        val previousFirstVisibleIndex = firstVisibleIndex
        firstVisibleIndex = positionedItems.firstOrNull()?.index ?: 0

        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth

        // the consumed scroll is considered as a delta we don't need to animate
        val scrollOffset = if (isVertical) {
            IntOffset(0, consumedScroll)
        } else {
            IntOffset(consumedScroll, 0)
        }

        // Only setup animations when we have access to target value in the current pass, which
        // means lookahead pass, or regular pass when not in a lookahead scope.
        val shouldSetupAnimation = isLookingAhead || !hasLookaheadOccurred
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
                    val newItemInfo = ItemInfo()
                    newItemInfo.updateAnimation(item, coroutineScope)
                    keyToItemInfoMap[item.key] = newItemInfo
                    val previousIndex = previousKeyToIndexMap?.getIndex(item.key) ?: -1
                    if (item.index != previousIndex && previousIndex != -1) {
                        if (previousIndex < previousFirstVisibleIndex) {
                            // the larger index will be in the start of the list
                            movingInFromStartBound.add(item)
                        } else {
                            movingInFromEndBound.add(item)
                        }
                    } else {
                        initializeAnimation(
                            item,
                            item.getOffset(0).let { if (item.isVertical) it.y else it.x },
                            newItemInfo
                        )
                        if (previousIndex == -1 && previousKeyToIndexMap != null) {
                            newItemInfo.animations.forEach {
                                it?.animateAppearance()
                            }
                        }
                    }
                } else {
                    if (shouldSetupAnimation) {
                        itemInfo.updateAnimation(item, coroutineScope)
                        itemInfo.animations.forEach { animation ->
                            if (animation != null &&
                                animation.rawOffset != LazyLayoutAnimation.NotInitialized
                            ) {
                                animation.rawOffset += scrollOffset
                            }
                        }
                        startPlacementAnimationsIfNeeded(item)
                    }
                }
            } else {
                // no animation, clean up if needed
                keyToItemInfoMap.remove(item.key)
            }
        }

        var accumulatedOffset = 0
        if (shouldSetupAnimation && previousKeyToIndexMap != null) {
            movingInFromStartBound.sortByDescending { previousKeyToIndexMap.getIndex(it.key) }
            movingInFromStartBound.fastForEach { item ->
                accumulatedOffset += item.size
                val mainAxisOffset = 0 - accumulatedOffset
                initializeAnimation(item, mainAxisOffset)
                startPlacementAnimationsIfNeeded(item)
            }
            accumulatedOffset = 0
            movingInFromEndBound.sortBy { previousKeyToIndexMap.getIndex(it.key) }
            movingInFromEndBound.fastForEach { item ->
                val mainAxisOffset = mainAxisLayoutSize + accumulatedOffset
                accumulatedOffset += item.size
                initializeAnimation(item, mainAxisOffset)
                startPlacementAnimationsIfNeeded(item)
            }
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val newIndex = keyIndexMap.getIndex(key)

            if (newIndex == -1) {
                keyToItemInfoMap.remove(key)
            } else {
                val item = itemProvider.getAndMeasure(newIndex)
                val itemInfo = keyToItemInfoMap.getValue(key)
                // check if we have any active placement animation on the item
                val inProgress =
                    itemInfo.animations.any { it?.isPlacementAnimationInProgress == true }
                if ((!inProgress && newIndex == previousKeyToIndexMap?.getIndex(key))) {
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
        movingAwayToStartBound.sortByDescending { keyIndexMap.getIndex(it.key) }
        movingAwayToStartBound.fastForEach { item ->
            accumulatedOffset += item.size
            val mainAxisOffset = 0 - accumulatedOffset

            item.position(mainAxisOffset, layoutWidth, layoutHeight)
            if (shouldSetupAnimation) {
                startPlacementAnimationsIfNeeded(item)
            }
        }

        accumulatedOffset = 0
        movingAwayToEndBound.sortBy { keyIndexMap.getIndex(it.key) }
        movingAwayToEndBound.fastForEach { item ->
            val mainAxisOffset = mainAxisLayoutSize + accumulatedOffset
            accumulatedOffset += item.size

            item.position(mainAxisOffset, layoutWidth, layoutHeight)
            if (shouldSetupAnimation) {
                startPlacementAnimationsIfNeeded(item)
            }
        }

        // This adds the new items to the list of positioned items while keeping the index of
        // the positioned items sorted in ascending order.
        positionedItems.addAll(0, movingAwayToStartBound.apply { reverse() })
        positionedItems.addAll(movingAwayToEndBound)

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
        item: LazyListMeasuredItem,
        mainAxisOffset: Int,
        itemInfo: ItemInfo = keyToItemInfoMap.getValue(item.key)
    ) {
        val firstPlaceableOffset = item.getOffset(0)

        val targetFirstPlaceableOffset = if (item.isVertical) {
            firstPlaceableOffset.copy(y = mainAxisOffset)
        } else {
            firstPlaceableOffset.copy(x = mainAxisOffset)
        }

        // initialize offsets
        itemInfo.animations.forEachIndexed { placeableIndex, animation ->
            if (animation != null) {
                val diffToFirstPlaceableOffset =
                    item.getOffset(placeableIndex) - firstPlaceableOffset
                animation.rawOffset = targetFirstPlaceableOffset + diffToFirstPlaceableOffset
            }
        }
    }

    private fun startPlacementAnimationsIfNeeded(item: LazyListMeasuredItem) {
        val itemInfo = keyToItemInfoMap.getValue(item.key)
        itemInfo.animations.forEachIndexed { placeableIndex, animation ->
            if (animation != null) {
                val newTarget = item.getOffset(placeableIndex)
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

    private val LazyListMeasuredItem.hasAnimations: Boolean
        get() {
            repeat(placeablesCount) { index ->
                getParentData(index).specs?.let {
                    // found at least one
                    return true
                }
            }
            return false
        }

    private class ItemInfo {
        /**
         * This array will have the same amount of elements as there are placeables on the item.
         * If the element is not null this means there are specs associated with the given placeable.
         */
        var animations = EmptyArray
            private set

        fun updateAnimation(positionedItem: LazyListMeasuredItem, coroutineScope: CoroutineScope) {
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
                    val animation = animations[index] ?: LazyLayoutAnimation(coroutineScope).also {
                        animations[index] = it
                    }
                    animation.appearanceSpec = specs.appearanceSpec
                    animation.placementSpec = specs.placementSpec
                }
            }
        }
    }
}

private val Any?.specs get() = this as? LazyLayoutAnimationSpecsNode

private val EmptyArray = emptyArray<LazyLayoutAnimation?>()
