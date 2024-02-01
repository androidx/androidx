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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.DefaultLayerBlock
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimation.Companion.NotInitialized
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

/**
 * Represents one measured item of the lazy list. It can in fact consist of multiple placeables
 * if the user emit multiple layout nodes in the item callback.
 */
internal class LazyListMeasuredItem @ExperimentalFoundationApi constructor(
    override val index: Int,
    private val placeables: List<Placeable>,
    val isVertical: Boolean,
    private val horizontalAlignment: Alignment.Horizontal?,
    private val verticalAlignment: Alignment.Vertical?,
    private val layoutDirection: LayoutDirection,
    private val reverseLayout: Boolean,
    private val beforeContentPadding: Int,
    private val afterContentPadding: Int,
    /**
     * Extra spacing to be added to [size] aside from the sum of the [placeables] size. It
     * is usually representing the spacing after the item.
     */
    private val spacing: Int,
    /**
     * The offset which shouldn't affect any calculations but needs to be applied for the final
     * value passed into the place() call.
     */
    private val visualOffset: IntOffset,
    override val key: Any,
    override val contentType: Any?,
    private val animator: LazyListItemAnimator
) : LazyListItemInfo {
    override var offset: Int = 0
        private set

    /**
     * Sum of the main axis sizes of all the inner placeables.
     */
    override val size: Int

    /**
     * Sum of the main axis sizes of all the inner placeables and [spacing].
     */
    val sizeWithSpacings: Int

    /**
     * Max of the cross axis sizes of all the inner placeables.
     */
    val crossAxisSize: Int

    /**
     * True when this item is not supposted to react on scroll delta. for example sticky header,
     * or items being animated away out of the bounds are non scrollable.
     */
    var nonScrollableItem: Boolean = false

    private var mainAxisLayoutSize: Int = Unset
    private var minMainAxisOffset: Int = 0
    private var maxMainAxisOffset: Int = 0

    // optimized for storing x and y offsets for each placeable one by one.
    // array's size == placeables.size * 2, first we store x, then y.
    private val placeableOffsets: IntArray

    init {
        var mainAxisSize = 0
        var maxCrossAxis = 0
        placeables.fastForEach {
            mainAxisSize += if (isVertical) it.height else it.width
            maxCrossAxis = maxOf(maxCrossAxis, if (!isVertical) it.height else it.width)
        }
        size = mainAxisSize
        sizeWithSpacings = (size + spacing).coerceAtLeast(0)
        crossAxisSize = maxCrossAxis
        placeableOffsets = IntArray(placeables.size * 2)
    }

    val placeablesCount: Int get() = placeables.size

    fun getParentData(index: Int) = placeables[index].parentData

    /**
     * Calculates positions for the inner placeables at [offset] main axis position.
     * If [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun position(
        offset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        this.offset = offset
        mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        var mainAxisOffset = offset
        placeables.fastForEachIndexed { index, placeable ->
            val indexInArray = index * 2
            if (isVertical) {
                placeableOffsets[indexInArray] =
                    requireNotNull(horizontalAlignment) {
                        "null horizontalAlignment when isVertical == true"
                    }.align(placeable.width, layoutWidth, layoutDirection)
                placeableOffsets[indexInArray + 1] = mainAxisOffset
                mainAxisOffset += placeable.height
            } else {
                placeableOffsets[indexInArray] = mainAxisOffset
                placeableOffsets[indexInArray + 1] =
                    requireNotNull(verticalAlignment) {
                        "null verticalAlignment when isVertical == false"
                    }.align(placeable.height, layoutHeight)
                mainAxisOffset += placeable.width
            }
        }
        minMainAxisOffset = -beforeContentPadding
        maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding
    }

    fun getOffset(index: Int) =
        IntOffset(placeableOffsets[index * 2], placeableOffsets[index * 2 + 1])

    fun applyScrollDelta(delta: Int, updateAnimations: Boolean) {
        if (nonScrollableItem) {
            return
        }
        offset += delta
        repeat(placeableOffsets.size) { index ->
            // placeableOffsets consist of x and y pairs for each placeable.
            // if isVertical is true then the main axis offsets are located at indexes 1, 3, 5 etc.
            if ((isVertical && index % 2 == 1) || (!isVertical && index % 2 == 0)) {
                placeableOffsets[index] += delta
            }
        }
        if (updateAnimations) {
            repeat(placeablesCount) { index ->
                val animation = animator.getAnimation(key, index)
                if (animation != null) {
                    animation.rawOffset = animation.rawOffset.copy { mainAxis -> mainAxis + delta }
                }
            }
        }
    }

    fun place(
        scope: Placeable.PlacementScope,
        isLookingAhead: Boolean
    ) = with(scope) {
        require(mainAxisLayoutSize != Unset) { "position() should be called first" }
        repeat(placeablesCount) { index ->
            val placeable = placeables[index]
            val minOffset = minMainAxisOffset - placeable.mainAxisSize
            val maxOffset = maxMainAxisOffset
            var offset = getOffset(index)
            val animation = animator.getAnimation(key, index)
            val layerBlock: GraphicsLayerScope.() -> Unit
            if (animation != null) {
                if (isLookingAhead) {
                    // Skip animation in lookahead pass
                    animation.lookaheadOffset = offset
                } else {
                    val targetOffset = if (animation.lookaheadOffset != NotInitialized) {
                        animation.lookaheadOffset
                    } else {
                        offset
                    }
                    val animatedOffset = targetOffset + animation.placementDelta
                    // cancel the animation if current and target offsets are both out of the bounds
                    if ((targetOffset.mainAxis <= minOffset &&
                            animatedOffset.mainAxis <= minOffset) ||
                        (targetOffset.mainAxis >= maxOffset &&
                            animatedOffset.mainAxis >= maxOffset)
                    ) {
                        animation.cancelPlacementAnimation()
                    }
                    offset = animatedOffset
                }
                layerBlock = animation.layerBlock
            } else {
                layerBlock = DefaultLayerBlock
            }
            if (reverseLayout) {
                offset = offset.copy { mainAxisOffset ->
                    mainAxisLayoutSize - mainAxisOffset - placeable.mainAxisSize
                }
            }
            offset += visualOffset
            if (isVertical) {
                placeable.placeWithLayer(offset, layerBlock = layerBlock)
            } else {
                placeable.placeRelativeWithLayer(offset, layerBlock = layerBlock)
            }
        }
    }

    private val IntOffset.mainAxis get() = if (isVertical) y else x
    private val Placeable.mainAxisSize get() = if (isVertical) height else width
    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        IntOffset(if (isVertical) x else mainAxisMap(x), if (isVertical) mainAxisMap(y) else y)
}

private const val Unset = Int.MIN_VALUE
