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

package androidx.wear.compose.foundation.lazy.layout

import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

/**
 * Handles the item animations when it is set via "animateItem" modifiers.
 *
 * This class is responsible for:
 * - animating item appearance for the new items.
 * - detecting when item position changed, figuring our start/end offsets and starting the
 *   animations for placement animations.
 * - animating item disappearance for the removed items.
 */
internal class LazyLayoutItemAnimator<T : LazyLayoutMeasuredItem> {
    // state containing relevant info for active items.
    private val keyToItemInfoMap = mutableScatterMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyIndexMap: LazyLayoutKeyIndexMap? = null

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = mutableScatterSetOf<Any>()
    private val movingAwayToStartBoundKeys = mutableListOf<Any>()
    private val movingAwayToEndBoundKeys = mutableListOf<Any>()
    private val movingInFromStartBound = mutableListOf<T>()
    private val movingInFromEndBound = mutableListOf<T>()
    private val disappearingItems = mutableListOf<LazyLayoutItemAnimation>()
    private var displayingNode: DrawModifierNode? = null

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        shouldAnimate: Boolean,
        positionedItems: List<T>,
        keyIndexMap: LazyLayoutKeyIndexMap,
        layoutMinOffset: Int,
        layoutMaxOffset: Int,
        itemSpacing: Int,
        coroutineScope: CoroutineScope,
        graphicsContext: GraphicsContext,
    ) {
        val previousKeyToIndexMap = this.keyIndexMap
        this.keyIndexMap = keyIndexMap

        val previousFirstVisibleIndex = firstVisibleIndex
        firstVisibleIndex = positionedItems.firstOrNull()?.index ?: 0

        // If `shouldAnimate` is false, a scroll is in progress. Prioritize performance by
        // releasing all animation resources. They will be recreated when the scroll ends.
        if (!shouldAnimate) {
            releaseAnimations()
            return
        }

        val hasAnimations = positionedItems.fastAny { it.hasAnimations() }
        if (!hasAnimations && keyToItemInfoMap.isEmpty()) {
            // no animations specified - no work needed
            return
        }

        positionedItems.fastForEach { item ->
            getAnimation(item.key)?.let {
                it.transformedHeight = item.transformedHeight
                it.measuredHeight = item.measuredHeight
                it.measurementDirection = item.measurementDirection
            }
        }

        // first add all items we had in the previous run
        keyToItemInfoMap.forEachKey { movingAwayKeys.add(it) }

        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            // remove items we have in the current one as they are still visible.
            movingAwayKeys.remove(item.key)
            getAnimation(item.key)?.let { animation -> disappearingItems.remove(animation) }
            if (item.hasAnimations()) {
                val itemInfo = keyToItemInfoMap[item.key]

                val previousIndex = previousKeyToIndexMap?.getIndex(item.key) ?: -1
                val shouldAnimateAppearance = previousIndex == -1 && previousKeyToIndexMap != null

                // there is no state associated with this item yet
                if (itemInfo == null) {
                    val newItemInfo = ItemInfo()
                    newItemInfo.updateAnimation(
                        item,
                        coroutineScope,
                        graphicsContext,
                        layoutMinOffset,
                        layoutMaxOffset,
                    )
                    keyToItemInfoMap[item.key] = newItemInfo

                    if (item.index != previousIndex && previousIndex != -1) {
                        if (previousIndex < previousFirstVisibleIndex) {
                            // the larger index will be in the start of the list
                            movingInFromStartBound.add(item)
                        } else {
                            movingInFromEndBound.add(item)
                        }
                    } else if (
                        // Since the list can move down, add the item to animate in from the start.
                        item.index == previousIndex && item.index < previousFirstVisibleIndex
                    ) {
                        movingInFromStartBound.add(item)
                    } else {
                        initializeAnimation(item, item.getOffset().y, newItemInfo)
                        if (shouldAnimateAppearance) {
                            newItemInfo.animation?.animateAppearance()
                        }
                    }
                } else {
                    itemInfo.updateAnimation(
                        item,
                        coroutineScope,
                        graphicsContext,
                        layoutMinOffset,
                        layoutMaxOffset,
                    )
                    if (shouldAnimateAppearance) {
                        itemInfo.animation?.let {
                            if (it.isDisappearanceAnimationInProgress) {
                                disappearingItems.remove(it)
                                displayingNode?.invalidateDraw()
                            }
                            it.animateAppearance()
                        }
                    }
                    startPlacementAnimationsIfNeeded(item)
                }
            } else {
                removeInfoForKey(item.key)
            }
        }

        if (previousKeyToIndexMap != null) {
            if (movingInFromStartBound.isNotEmpty()) {
                var accumulatedOffset = 0
                movingInFromStartBound.sortByDescending { previousKeyToIndexMap.getIndex(it.key) }
                val startOffset =
                    positionedItems
                        .fastFirstOrNull {
                            // Find the item right below the first moving in item in previous order
                            // as the anchor item.
                            previousKeyToIndexMap.getIndex(it.key) ==
                                previousKeyToIndexMap.getIndex(movingInFromStartBound[0].key) + 1
                        }
                        ?.let { getAnimation(it.key)?.logicalOffset?.y }
                        // If the anchor item is removed, fallback to the layoutMinOffset.
                        ?: layoutMinOffset
                movingInFromStartBound.fastForEach { item ->
                    accumulatedOffset += item.mainAxisSizeWithSpacings
                    val mainAxisOffset = startOffset - accumulatedOffset
                    initializeAnimation(item, mainAxisOffset)
                    startPlacementAnimationsIfNeeded(item)
                }
            }
            if (movingInFromEndBound.isNotEmpty()) {
                var accumulatedOffset = 0
                movingInFromEndBound.sortBy { previousKeyToIndexMap.getIndex(it.key) }
                val startOffset =
                    positionedItems
                        .fastFirstOrNull {
                            // Find the item right above the first moving in item in previous order
                            // as the anchor item.
                            previousKeyToIndexMap.getIndex(it.key) ==
                                previousKeyToIndexMap.getIndex(movingInFromEndBound[0].key) - 1
                        }
                        ?.let {
                            getAnimation(it.key)?.logicalOffset?.run {
                                it.mainAxisSizeWithSpacings + y
                            }
                        }
                        // If the anchor item is removed, fallback to the layoutMaxOffset.
                        ?: layoutMaxOffset

                movingInFromEndBound.fastForEach { item ->
                    accumulatedOffset += item.mainAxisSizeWithSpacings
                    val mainAxisOffset =
                        startOffset + accumulatedOffset - item.mainAxisSizeWithSpacings
                    initializeAnimation(item, mainAxisOffset)
                    startPlacementAnimationsIfNeeded(item)
                }
            }
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val info = keyToItemInfoMap[key] ?: return@forEach
            val newIndex = keyIndexMap.getIndex(key)

            if (newIndex != -1) {
                // Item moved out of bounds (moving away)!
                val animation = info.animation
                if (animation != null) {
                    val isAlreadyMovingAway = disappearingItems.contains(animation)
                    if (isAlreadyMovingAway && !animation.isPlacementAnimationInProgress) {
                        removeInfoForKey(key)
                        disappearingItems.remove(animation)
                        displayingNode?.invalidateDraw()
                    } else {
                        if (newIndex < firstVisibleIndex) {
                            movingAwayToStartBoundKeys.add(key)
                        } else {
                            movingAwayToEndBoundKeys.add(key)
                        }
                    }
                } else {
                    removeInfoForKey(key)
                }
                return@forEach
            }

            // Truly removed (deleted) item case:
            var isProgress = false
            info.animation?.let { animation ->
                if (animation.isDisappearanceAnimationInProgress) {
                    isProgress = true
                } else if (animation.isDisappearanceAnimationFinished) {
                    animation.release()
                    info.animation = null
                    disappearingItems.remove(animation)
                    displayingNode?.invalidateDraw()
                } else {
                    if (animation.layer != null) {
                        animation.animateDisappearance()
                    }
                    if (animation.isDisappearanceAnimationInProgress) {
                        if (!disappearingItems.contains(animation)) {
                            disappearingItems.add(animation)
                        }
                        displayingNode?.invalidateDraw()
                        isProgress = true
                    } else {
                        animation.release()
                        info.animation = null
                    }
                }
            }
            if (!isProgress) {
                removeInfoForKey(key)
            }
        }

        if (movingAwayToStartBoundKeys.isNotEmpty()) {
            var accumulatedOffset = 0
            movingAwayToStartBoundKeys.sortByDescending { keyIndexMap.getIndex(it) }
            val startOffset = layoutMinOffset
            movingAwayToStartBoundKeys.fastForEach { key ->
                val info = keyToItemInfoMap[key] ?: return@fastForEach
                accumulatedOffset += info.animation?.transformedHeight ?: 0
                val mainAxisOffset = startOffset - accumulatedOffset
                startMovingAwayAnimation(info, mainAxisOffset)
                accumulatedOffset += itemSpacing
            }
        }

        if (movingAwayToEndBoundKeys.isNotEmpty()) {
            var accumulatedOffset = 0
            movingAwayToEndBoundKeys.sortBy { keyIndexMap.getIndex(it) }
            val startOffset = layoutMaxOffset
            movingAwayToEndBoundKeys.fastForEach { key ->
                val info = keyToItemInfoMap[key] ?: return@fastForEach
                val mainAxisOffset = startOffset + accumulatedOffset
                startMovingAwayAnimation(info, mainAxisOffset)
                accumulatedOffset += (info.animation?.transformedHeight ?: 0) + itemSpacing
            }
        }

        movingAwayToStartBoundKeys.clear()
        movingAwayToEndBoundKeys.clear()
        movingInFromStartBound.clear()
        movingInFromEndBound.clear()
        movingAwayKeys.clear()
    }

    private fun removeInfoForKey(key: Any) {
        keyToItemInfoMap.remove(key)?.animation?.release()
    }

    private fun startMovingAwayAnimation(info: ItemInfo, targetOffset: Int) {
        val animation = info.animation ?: return
        val currentTarget = animation.rawOffset
        val newTarget = IntOffset(currentTarget.x, targetOffset)

        if (!disappearingItems.contains(animation)) {
            disappearingItems.add(animation)
            displayingNode?.invalidateDraw()
        }

        if (currentTarget != LazyLayoutItemAnimation.NotInitialized && currentTarget != newTarget) {
            animation.animatePlacementDelta(newTarget - currentTarget, isMovingAway = true)
        }

        animation.rawOffset = newTarget
        animation.logicalOffset = newTarget
    }

    /**
     * Should be called when the animations are not needed for the next positions change, for
     * example when we snap to a new position.
     */
    fun reset() {
        releaseAnimations()
        keyIndexMap = null
        firstVisibleIndex = -1
    }

    fun releaseAnimations() {
        if (keyToItemInfoMap.isNotEmpty()) {
            keyToItemInfoMap.forEachValue { it.animation?.release() }
            keyToItemInfoMap.clear()
        }
    }

    private fun initializeAnimation(
        item: T,
        mainAxisOffset: Int,
        itemInfo: ItemInfo = keyToItemInfoMap[item.key]!!,
    ) {
        val firstPlaceableOffset = item.getOffset()

        val targetFirstPlaceableOffset = firstPlaceableOffset.copy(y = mainAxisOffset)

        // initialize offsets
        itemInfo.animation?.let { animation ->
            val diffToFirstPlaceableOffset = item.getOffset() - firstPlaceableOffset
            animation.rawOffset = targetFirstPlaceableOffset + diffToFirstPlaceableOffset
        }
    }

    private fun startPlacementAnimationsIfNeeded(item: T, isMovingAway: Boolean = false) {
        val itemInfo = keyToItemInfoMap[item.key]!!
        itemInfo.animation?.let { animation ->
            val newTarget = item.getOffset()
            val currentTarget = animation.rawOffset
            if (
                currentTarget != LazyLayoutItemAnimation.NotInitialized &&
                    currentTarget != newTarget
            ) {
                animation.animatePlacementDelta(newTarget - currentTarget, isMovingAway)
            }
            animation.rawOffset = newTarget
        }
    }

    fun getAnimation(key: Any): LazyLayoutItemAnimation? = keyToItemInfoMap[key]?.animation

    private inner class ItemInfo {
        /** Animation associated with an item. */
        var animation: LazyLayoutItemAnimation? = null

        var constraints: Constraints? = null
        var crossAxisOffset: Int = 0

        private val isRunningPlacement
            get() = animation?.isRunningMovingAwayAnimation == true

        var layoutMinOffset = 0
            private set

        var layoutMaxOffset = 0
            private set

        fun updateAnimation(
            positionedItem: T,
            coroutineScope: CoroutineScope,
            graphicsContext: GraphicsContext,
            layoutMinOffset: Int,
            layoutMaxOffset: Int,
            crossAxisOffset: Int = positionedItem.crossAxisOffset,
        ) {
            if (!isRunningPlacement) {
                this.layoutMinOffset = layoutMinOffset
                this.layoutMaxOffset = layoutMaxOffset
            }
            constraints = positionedItem.constraints
            this.crossAxisOffset = crossAxisOffset
            val specs = positionedItem.parentData.specs
            if (specs == null) {
                animation?.release()
                animation = null
            } else {
                val animation =
                    animation
                        ?: LazyLayoutItemAnimation(
                                coroutineScope = coroutineScope,
                                graphicsContext = graphicsContext,
                                // until b/329417380 is fixed we have to trigger any
                                // invalidation in
                                // order for the layer properties change to be applied:
                                onLayerPropertyChanged = { displayingNode?.invalidateDraw() },
                                containerHeight = layoutMaxOffset - layoutMinOffset,
                                transformedHeight = positionedItem.transformedHeight,
                                measuredHeight = positionedItem.measuredHeight,
                                measurementDirection = positionedItem.measurementDirection,
                            )
                            .also { animation = it }
                animation.fadeInSpec = specs.fadeInSpec
                animation.placementSpec = specs.placementSpec
                animation.fadeOutSpec = specs.fadeOutSpec
            }
        }
    }

    internal data class DisplayingDisappearingItemsElement(
        private val animator: LazyLayoutItemAnimator<*>,
        private val reverseLayout: Boolean,
    ) : ModifierNodeElement<DisplayingDisappearingItemsNode>() {
        override fun create() = DisplayingDisappearingItemsNode(animator, reverseLayout)

        override fun update(node: DisplayingDisappearingItemsNode) {
            node.setAnimator(animator)
            node.setReverseLayout(reverseLayout)
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "DisplayingDisappearingItemsElement"
        }
    }

    internal data class DisplayingDisappearingItemsNode(
        private var animator: LazyLayoutItemAnimator<*>,
        private var reverseLayout: Boolean,
    ) : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawContent()
            // Draw disappearing items on top of the active visible content to make sure
            // exiting/moving items are drawn on top of the remaining items.
            animator.disappearingItems.fastForEach {
                val layer = it.layer ?: return@fastForEach
                val x = it.rawOffset.x.toFloat() + it.placementDelta.x.toFloat()
                val logicalY = it.rawOffset.y.toFloat() + it.placementDelta.y.toFloat()
                val y =
                    if (!reverseLayout) {
                        logicalY
                    } else {
                        // Mirror Y coordinate across the container's center for reverse layout
                        size.height - it.transformedHeight - logicalY
                    }
                translate(x - layer.topLeft.x.toFloat(), y - layer.topLeft.y.toFloat()) {
                    drawLayer(layer)
                }
            }
        }

        override fun onAttach() {
            animator.displayingNode = this
        }

        override fun onDetach() {
            animator.reset()
        }

        fun setAnimator(animator: LazyLayoutItemAnimator<*>) {
            if (this.animator != animator) {
                if (node.isAttached) {
                    this.animator.reset()
                    animator.displayingNode = this
                    this.animator = animator
                }
            }
        }

        fun setReverseLayout(reverseLayout: Boolean) {
            if (this.reverseLayout != reverseLayout) {
                this.reverseLayout = reverseLayout
                invalidateDraw()
            }
        }
    }
}

internal fun Modifier.lazyLayoutItemAnimator(
    animator: LazyLayoutItemAnimator<*>,
    reverseLayout: Boolean,
): Modifier =
    this.then(LazyLayoutItemAnimator.DisplayingDisappearingItemsElement(animator, reverseLayout))
