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

package androidx.tv.foundation.lazy.grid

import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

/**
 * Represents one measured item of the lazy grid. It can in fact consist of multiple placeables
 * if the user emit multiple layout nodes in the item callback.
 */
internal class LazyGridMeasuredItem(
    override val index: Int,
    override val key: Any,
    val isVertical: Boolean,
    /**
     * Cross axis size is the same for all [placeables]. Take it as parameter for the case when
     * [placeables] is empty.
     */
    val crossAxisSize: Int,
    mainAxisSpacing: Int,
    private val reverseLayout: Boolean,
    private val layoutDirection: LayoutDirection,
    private val beforeContentPadding: Int,
    private val afterContentPadding: Int,
    private val placeables: List<Placeable>,
    /**
     * The offset which shouldn't affect any calculations but needs to be applied for the final
     * value passed into the place() call.
     */
    private val visualOffset: IntOffset,
    override val contentType: Any?
) : TvLazyGridItemInfo {
    /**
     * Main axis size of the item - the max main axis size of the placeables.
     */
    val mainAxisSize: Int

    /**
     * The max main axis size of the placeables plus mainAxisSpacing.
     */
    val mainAxisSizeWithSpacings: Int

    val placeablesCount: Int get() = placeables.size

    private var mainAxisLayoutSize: Int = Unset
    private var minMainAxisOffset: Int = 0
    private var maxMainAxisOffset: Int = 0

    fun getParentData(index: Int) = placeables[index].parentData

    init {
        var maxMainAxis = 0
        placeables.fastForEach {
            maxMainAxis = maxOf(maxMainAxis, if (isVertical) it.height else it.width)
        }
        mainAxisSize = maxMainAxis
        mainAxisSizeWithSpacings = (maxMainAxis + mainAxisSpacing).coerceAtLeast(0)
    }

    override val size: IntSize = if (isVertical) {
        IntSize(crossAxisSize, mainAxisSize)
    } else {
        IntSize(mainAxisSize, crossAxisSize)
    }
    override var offset: IntOffset = IntOffset.Zero
        private set
    val crossAxisOffset get() = if (isVertical) offset.x else offset.y
    override var row: Int = TvLazyGridItemInfo.UnknownRow
        private set
    override var column: Int = TvLazyGridItemInfo.UnknownColumn
        private set

    /**
     * Calculates positions for the inner placeables at [mainAxisOffset], [crossAxisOffset].
     * [layoutWidth] and [layoutHeight] should be provided to not place placeables which are ended
     * up outside of the viewport (for example one item consist of 2 placeables, and the first one
     * is not going to be visible, so we don't place it as an optimization, but place the second
     * one). If [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun position(
        mainAxisOffset: Int,
        crossAxisOffset: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        row: Int = TvLazyGridItemInfo.UnknownRow,
        column: Int = TvLazyGridItemInfo.UnknownColumn
    ) {
        mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        val crossAxisLayoutSize = if (isVertical) layoutWidth else layoutHeight
        @Suppress("NAME_SHADOWING")
        val crossAxisOffset = if (isVertical && layoutDirection == LayoutDirection.Rtl) {
            crossAxisLayoutSize - crossAxisOffset - crossAxisSize
        } else {
            crossAxisOffset
        }
        offset = if (isVertical) {
            IntOffset(crossAxisOffset, mainAxisOffset)
        } else {
            IntOffset(mainAxisOffset, crossAxisOffset)
        }
        this.row = row
        this.column = column
        minMainAxisOffset = -beforeContentPadding
        maxMainAxisOffset = mainAxisLayoutSize + afterContentPadding
    }

    fun place(
        scope: Placeable.PlacementScope,
    ) = with(scope) {
        require(mainAxisLayoutSize != Unset) { "position() should be called first" }
        repeat(placeablesCount) { index ->
            val placeable = placeables[index]
            val minOffset = minMainAxisOffset - placeable.mainAxisSize
            val maxOffset = maxMainAxisOffset

            var offset = offset
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

private const val Unset = Int.MIN_VALUE
