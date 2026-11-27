/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimationSpecsNode
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimation.Companion.NotInitialized
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItem
import androidx.compose.foundation.lazy.layout.StateBasedLazyLayoutItemAnimation
import androidx.compose.foundation.lazy.layout.UnspecifiedSize
import androidx.compose.foundation.lazy.layout.mirroredForRtl
import androidx.compose.foundation.lazy.layout.placeablesCount
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEachIndexed

/**
 * Represents one measured item of the lazy list. It can in fact consist of multiple placeables if
 * the user emit multiple layout nodes in the item callback.
 */
internal class LazyListMeasuredItem
constructor(
    override val index: Int,
    override val placeables: List<Placeable>,
    private val isVertical: Boolean,
    private val horizontalAlignment: Alignment.Horizontal?,
    private val verticalAlignment: Alignment.Vertical?,
    private val layoutDirection: LayoutDirection,
    private val reverseLayout: Boolean,
    private val beforeContentPadding: Int,
    private val afterContentPadding: Int,
    /**
     * Extra spacing to be added to [size] aside from the sum of the [placeables] size. It is
     * usually representing the spacing after the item.
     */
    private val spacing: Int,
    /**
     * The offset which shouldn't affect any calculations but needs to be applied for the final
     * value passed into the place() call.
     */
    private val visualOffset: IntOffset,
    override val key: Any,
    override val contentType: Any?,
    private val animator: LazyLayoutItemAnimator<LazyListMeasuredItem>,
    override val constraints: Constraints,
    canExpandWhenAppearing: Boolean,
) : LazyListItemInfo, LazyLayoutMeasuredItem {
    override var offset: Int = 0
        private set

    /** Sum of the main axis sizes of all the inner placeables. */
    override val size: Int

    /** In lists we only have one lane. */
    override val lane: Int = 0
    /** And each item takes one span. */
    override val span: Int = 1

    override val horizontalAxisSize: Int
    override val verticalAxisSize: Int
    override val horizontalAxisSpacing: Int
    override val verticalAxisSpacing: Int

    /** Max of the cross axis sizes of all the inner placeables. */
    val crossAxisSize: Int

    /**
     * True when this item is not supposted to react on scroll delta. for example sticky header, or
     * items being animated away out of the bounds are non scrollable.
     */
    var nonScrollableItem: Boolean = false

    private var mainAxisLayoutSize: Int = Unset
    private var minMainAxisOffset: Int = 0
    private var maxMainAxisOffset: Int = 0

    // optimized for storing x and y offsets for each placeable one by one.
    // the first placeables.size * 2 elements are the offsets, first we store x, then y.
    // when a placeable has an animated size they are followed by the width and height of the
    // bounds of each placeable, so the array's size == placeables.size * 4.
    private val placeableOffsets: IntArray

    override val hasAnimatedSize: Boolean
        get() = placeableOffsets.size > placeables.size * 2

    init {
        var mainAxisSize = 0
        var maxCrossAxis = 0
        var offsetsAndBounds: IntArray? = null
        placeables.fastForEachIndexed { placeableIndex, placeable ->
            var width = placeable.width
            var height = placeable.height
            val specs = placeable.parentData as? LazyLayoutAnimationSpecsNode
            if (specs?.enterTransition?.config?.changeSize != null) {
                val boundsSize =
                    animator.animatedBoundsSize(
                        key,
                        placeableIndex,
                        placeable,
                        constraints,
                        canExpandWhenAppearing,
                    )
                if (boundsSize != UnspecifiedSize) {
                    val array =
                        offsetsAndBounds
                            ?: offsetsWithMeasuredSizes().also { offsetsAndBounds = it }
                    if (isVertical) {
                        width = boundsSize.width
                        height = maxOf(boundsSize.height, minOf(-spacing, height))
                    } else {
                        width = maxOf(boundsSize.width, minOf(-spacing, width))
                        height = boundsSize.height
                    }
                    val boundsIndex = (placeables.size + placeableIndex) * 2
                    array[boundsIndex] = width
                    array[boundsIndex + 1] = height
                }
            }
            mainAxisSize += if (isVertical) height else width
            maxCrossAxis = maxOf(maxCrossAxis, if (!isVertical) height else width)
        }
        placeableOffsets = offsetsAndBounds ?: IntArray(placeables.size * 2)
        size = mainAxisSize
        crossAxisSize = maxCrossAxis
        if (isVertical) {
            verticalAxisSpacing = spacing
            verticalAxisSize = size
            horizontalAxisSize = crossAxisSize
            horizontalAxisSpacing = 0
        } else {
            verticalAxisSpacing = 0
            verticalAxisSize = crossAxisSize

            horizontalAxisSize = size
            horizontalAxisSpacing = spacing
        }
    }

    /** Sum of the main axis sizes of all the inner placeables and [spacing]. */
    val mainAxisSizeWithSpacings: Int
        get() =
            if (isVertical) {
                    verticalAxisSize + verticalAxisSpacing
                } else {
                    horizontalAxisSize + horizontalAxisSpacing
                }
                .coerceAtLeast(0)

    fun getParentData(index: Int) = placeables[index].parentData

    override fun position(
        horizontalAxisOffset: Int,
        verticalAxisOffset: Int,
        layoutWidth: Int,
        layoutHeight: Int,
    ) {
        position(horizontalAxisOffset, layoutWidth, layoutHeight)
    }

    override fun makeNonScrollable() {
        nonScrollableItem = true
    }

    /**
     * Calculates positions for the inner placeables at [mainAxisOffset] main axis position. If
     * [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun position(mainAxisOffset: Int, layoutWidth: Int, layoutHeight: Int) {
        this.offset = mainAxisOffset
        mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        @Suppress("NAME_SHADOWING") var mainAxisOffset = mainAxisOffset
        val hasBounds = hasAnimatedSize
        placeables.fastForEachIndexed { index, placeable ->
            val indexInArray = index * 2
            val width = if (hasBounds) boundsWidth(index) else placeable.width
            val height = if (hasBounds) boundsHeight(index) else placeable.height
            if (isVertical) {
                placeableOffsets[indexInArray] =
                    requirePreconditionNotNull(horizontalAlignment) {
                            "null horizontalAlignment when isVertical == true"
                        }
                        .align(width, layoutWidth, layoutDirection)
                placeableOffsets[indexInArray + 1] = mainAxisOffset
                mainAxisOffset += height
            } else {
                placeableOffsets[indexInArray] = mainAxisOffset
                placeableOffsets[indexInArray + 1] =
                    requirePreconditionNotNull(verticalAlignment) {
                            "null verticalAlignment when isVertical == false"
                        }
                        .align(height, layoutHeight)
                mainAxisOffset += width
            }
        }
        minMainAxisOffset = -beforeContentPadding
        maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding
    }

    /**
     * Update a [mainAxisLayoutSize] when the size did change after last [position] call. Knowing
     * the final size is important for calculating the final position in reverse layout.
     */
    fun updateMainAxisLayoutSize(mainAxisLayoutSize: Int) {
        this.mainAxisLayoutSize = mainAxisLayoutSize
        maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding
    }

    override fun getOffset(placeableIndex: Int) =
        if (placeableIndex == 0 && placeablesCount == 0) {
            if (isVertical) IntOffset(0, offset) else IntOffset(offset, 0)
        } else {
            IntOffset(
                placeableOffsets[placeableIndex * 2],
                placeableOffsets[placeableIndex * 2 + 1],
            )
        }

    fun applyScrollDelta(delta: Int, updateAnimations: Boolean) {
        if (nonScrollableItem) {
            return
        }
        offset += delta
        repeat(placeables.size * 2) { index ->
            // placeableOffsets consist of x and y pairs for each placeable.
            // if isVertical is true then the main axis offsets are located at indexes 1, 3, 5 etc.
            // 1 when odd, 0 when even
            val oddEven = index and 1
            if ((isVertical && oddEven != 0) || (!isVertical && oddEven == 0)) {
                placeableOffsets[index] += delta
            }
        }
        if (updateAnimations) {
            repeat(placeablesCount) { index ->
                val animation = animator.getAnimation(key, index)
                if (animation != null) {
                    animation.targetOffset =
                        animation.targetOffset.copy { mainAxis -> mainAxis + delta }
                }
            }
        }
    }

    fun place(scope: Placeable.PlacementScope, isLookingAhead: Boolean) =
        with(scope) {
            requirePrecondition(mainAxisLayoutSize != Unset) { "position() should be called first" }
            val hasBounds = hasAnimatedSize
            repeat(placeablesCount) { index ->
                val placeable = placeables[index]
                val minOffset = minMainAxisOffset - placeable.mainAxisSize
                val maxOffset = maxMainAxisOffset
                var offset = getOffset(index)
                val animation = animator.getAnimation(key, index)
                val layer: GraphicsLayer?
                if (animation != null) {
                    if (isLookingAhead) {
                        // Skip animation in lookahead pass
                        animation.lookaheadOffset = offset
                    } else {
                        val targetOffset =
                            if (animation.lookaheadOffset != NotInitialized) {
                                animation.lookaheadOffset
                            } else {
                                offset
                            }
                        val animatedOffset = targetOffset + animation.placementDelta
                        // cancel the animation if current and target offsets are both out of the
                        // bounds
                        if (
                            (targetOffset.mainAxis <= minOffset &&
                                animatedOffset.mainAxis <= minOffset) ||
                                (targetOffset.mainAxis >= maxOffset &&
                                    animatedOffset.mainAxis >= maxOffset)
                        ) {
                            animation.cancelPlacementAnimation()
                        }
                        offset = animatedOffset
                    }
                    layer = animation.layer
                } else {
                    layer = null
                }
                val boundsMainAxisSize =
                    if (hasBounds) {
                        if (isVertical) boundsHeight(index) else boundsWidth(index)
                    } else {
                        placeable.mainAxisSize
                    }
                if (reverseLayout) {
                    offset = offset.copy { mainAxisOffset ->
                        mainAxisLayoutSize - mainAxisOffset - boundsMainAxisSize
                    }
                }
                offset += visualOffset
                var zIndex = 0f
                if (hasBounds && animation is StateBasedLazyLayoutItemAnimation) {
                    offset +=
                        animation.placeWithinBounds(
                            placeable,
                            IntSize(boundsWidth(index), boundsHeight(index)),
                        )
                    if (animation.isClippedToBounds && !nonScrollableItem) {
                        zIndex = ClippedItemZIndex
                    }
                }
                if (!isLookingAhead) {
                    animation?.placementOffset =
                        if (!isVertical && layoutDirection == LayoutDirection.Rtl) {
                            offset.mirroredForRtl(
                                layoutWidth =
                                    mainAxisLayoutSize + beforeContentPadding + afterContentPadding,
                                itemWidth = placeable.width,
                            )
                        } else {
                            offset
                        }
                }
                if (isVertical) {
                    if (layer != null) {
                        placeable.placeWithLayer(offset, layer, zIndex)
                    } else {
                        placeable.placeWithLayer(offset)
                    }
                } else {
                    if (layer != null) {
                        placeable.placeRelativeWithLayer(offset, layer, zIndex)
                    } else {
                        placeable.placeRelativeWithLayer(offset)
                    }
                }
            }
        }

    private fun StateBasedLazyLayoutItemAnimation.placeWithinBounds(
        placeable: Placeable,
        boundsSize: IntSize,
    ): IntOffset {
        val contentOffset = contentOffsetIn(boundsSize)
        onPlaced(boundsSize, contentOffset)
        return if (!isVertical && layoutDirection == LayoutDirection.Rtl) {
            IntOffset(boundsSize.width - placeable.width - contentOffset.x, contentOffset.y)
        } else {
            contentOffset
        }
    }

    private fun offsetsWithMeasuredSizes(): IntArray {
        val array = IntArray(placeables.size * 4)
        placeables.fastForEachIndexed { index, placeable ->
            val boundsIndex = (placeables.size + index) * 2
            array[boundsIndex] = placeable.width
            array[boundsIndex + 1] = placeable.height
        }
        return array
    }

    private fun boundsWidth(index: Int) = placeableOffsets[(placeables.size + index) * 2]

    private fun boundsHeight(index: Int) = placeableOffsets[(placeables.size + index) * 2 + 1]

    private val IntOffset.mainAxis
        get() = if (isVertical) y else x

    private val Placeable.mainAxisSize
        get() = if (isVertical) height else width

    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        if (isVertical) IntOffset(x, mainAxisMap(y)) else IntOffset(mainAxisMap(x), y)
}

private const val Unset = Int.MIN_VALUE

private const val ClippedItemZIndex = -1f
