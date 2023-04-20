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
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateItemModifierNode
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

/**
 * Represents one measured item of the lazy list. It can in fact consist of multiple placeables
 * if the user emit multiple layout nodes in the item callback.
 */
internal class LazyListMeasuredItem @ExperimentalFoundationApi constructor(
    val index: Int,
    private val placeables: List<Placeable>,
    private val isVertical: Boolean,
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
    val key: Any,
) {
    /**
     * Sum of the main axis sizes of all the inner placeables.
     */
    val size: Int

    /**
     * Sum of the main axis sizes of all the inner placeables and [spacing].
     */
    val sizeWithSpacings: Int

    /**
     * Max of the cross axis sizes of all the inner placeables.
     */
    val crossAxisSize: Int

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
    ): LazyListPositionedItem {
        val wrappers = mutableListOf<LazyListPlaceableWrapper>()
        val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        var mainAxisOffset = offset
        placeables.fastForEach {
            val placeableOffset = if (isVertical) {
                val x = requireNotNull(horizontalAlignment)
                    .align(it.width, layoutWidth, layoutDirection)
                IntOffset(x, mainAxisOffset)
            } else {
                val y = requireNotNull(verticalAlignment).align(it.height, layoutHeight)
                IntOffset(mainAxisOffset, y)
            }
            mainAxisOffset += if (isVertical) it.height else it.width
            wrappers.add(LazyListPlaceableWrapper(placeableOffset, it))
        }
        return LazyListPositionedItem(
            offset = offset,
            index = this.index,
            key = key,
            size = size,
            minMainAxisOffset = -beforeContentPadding,
            maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding,
            isVertical = isVertical,
            wrappers = wrappers,
            visualOffset = visualOffset,
            reverseLayout = reverseLayout,
            mainAxisLayoutSize = mainAxisLayoutSize
        )
    }
}

internal class LazyListPositionedItem(
    override val offset: Int,
    override val index: Int,
    override val key: Any,
    override val size: Int,
    private val minMainAxisOffset: Int,
    private val maxMainAxisOffset: Int,
    val isVertical: Boolean,
    private val wrappers: List<LazyListPlaceableWrapper>,
    private val visualOffset: IntOffset,
    private val reverseLayout: Boolean,
    private val mainAxisLayoutSize: Int
) : LazyListItemInfo {
    val placeablesCount: Int get() = wrappers.size

    fun getOffset(index: Int) = wrappers[index].offset

    fun getParentData(index: Int) = wrappers[index].placeable.parentData

    fun place(
        scope: Placeable.PlacementScope,
    ) = with(scope) {
        repeat(placeablesCount) { index ->
            val placeable = wrappers[index].placeable
            val minOffset = minMainAxisOffset - placeable.mainAxisSize
            val maxOffset = maxMainAxisOffset
            var offset = getOffset(index)
            val animateNode = getParentData(index) as? LazyLayoutAnimateItemModifierNode
            if (animateNode != null) {
                val animatedOffset = offset + animateNode.placementDelta
                // cancel the animation if current and target offsets are both out of the bounds.
                if ((offset.mainAxis <= minOffset && animatedOffset.mainAxis <= minOffset) ||
                    (offset.mainAxis >= maxOffset && animatedOffset.mainAxis >= maxOffset)
                ) {
                    animateNode.cancelAnimation()
                }
                offset = animatedOffset
            }
            if (reverseLayout) {
                offset = offset.copy { mainAxisOffset ->
                    mainAxisLayoutSize - mainAxisOffset - placeable.mainAxisSize
                }
            }
            offset += visualOffset
            if (isVertical) {
                placeable.placeWithLayer(offset)
            } else {
                placeable.placeRelativeWithLayer(offset)
            }
        }
    }

    private val IntOffset.mainAxis get() = if (isVertical) y else x
    private val Placeable.mainAxisSize get() = if (isVertical) height else width
    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        IntOffset(if (isVertical) x else mainAxisMap(x), if (isVertical) mainAxisMap(y) else y)
}

internal class LazyListPlaceableWrapper(
    val offset: IntOffset,
    val placeable: Placeable
)
