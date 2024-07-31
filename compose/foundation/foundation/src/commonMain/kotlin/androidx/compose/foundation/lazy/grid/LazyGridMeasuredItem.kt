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

import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItem
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach

/**
 * Represents one measured item of the lazy grid. It can in fact consist of multiple placeables if
 * the user emit multiple layout nodes in the item callback.
 */
internal class LazyGridMeasuredItem(
    override val index: Int,
    override val key: Any,
    override val isVertical: Boolean,
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
    override val contentType: Any?,
    private val animator: LazyLayoutItemAnimator<LazyGridMeasuredItem>,
    override val constraints: Constraints,
    override val lane: Int,
    override val span: Int
) : LazyGridItemInfo, LazyLayoutMeasuredItem {
    /** Main axis size of the item - the max main axis size of the placeables. */
    val mainAxisSize: Int

    /** The max main axis size of the placeables plus mainAxisSpacing. */
    override val mainAxisSizeWithSpacings: Int

    override val placeablesCount: Int
        get() = placeables.size

    private var mainAxisLayoutSize: Int = Unset
    private var minMainAxisOffset: Int = 0
    private var maxMainAxisOffset: Int = 0

    override fun getParentData(index: Int) = placeables[index].parentData

    init {
        var maxMainAxis = 0
        placeables.fastForEach {
            maxMainAxis = maxOf(maxMainAxis, if (isVertical) it.height else it.width)
        }
        mainAxisSize = maxMainAxis
        mainAxisSizeWithSpacings = (maxMainAxis + mainAxisSpacing).coerceAtLeast(0)
    }

    override val size: IntSize =
        if (isVertical) {
            IntSize(crossAxisSize, mainAxisSize)
        } else {
            IntSize(mainAxisSize, crossAxisSize)
        }
    override var offset: IntOffset = IntOffset.Zero
        private set

    override var row: Int = LazyGridItemInfo.UnknownRow
        private set

    override var column: Int = LazyGridItemInfo.UnknownColumn
        private set

    override fun getOffset(index: Int): IntOffset = offset

    /**
     * True when this item is not supposed to react on scroll delta. for example items being
     * animated away out of the bounds are non scrollable.
     */
    override var nonScrollableItem: Boolean = false

    override fun position(
        mainAxisOffset: Int,
        crossAxisOffset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        position(
            mainAxisOffset,
            crossAxisOffset,
            layoutWidth,
            layoutHeight,
            LazyGridItemInfo.UnknownRow,
            LazyGridItemInfo.UnknownColumn
        )
    }

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
        row: Int,
        column: Int
    ) {
        mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        val crossAxisLayoutSize = if (isVertical) layoutWidth else layoutHeight
        @Suppress("NAME_SHADOWING")
        val crossAxisOffset =
            if (isVertical && layoutDirection == LayoutDirection.Rtl) {
                crossAxisLayoutSize - crossAxisOffset - crossAxisSize
            } else {
                crossAxisOffset
            }
        offset =
            if (isVertical) {
                IntOffset(crossAxisOffset, mainAxisOffset)
            } else {
                IntOffset(mainAxisOffset, crossAxisOffset)
            }
        this.row = row
        this.column = column
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

    fun applyScrollDelta(delta: Int) {
        if (nonScrollableItem) {
            return
        }
        offset = offset.copy { it + delta }
        repeat(placeablesCount) { index ->
            val animation = animator.getAnimation(key, index)
            if (animation != null) {
                animation.rawOffset = animation.rawOffset.copy { mainAxis -> mainAxis + delta }
            }
        }
    }

    fun place(
        scope: Placeable.PlacementScope,
    ) =
        with(scope) {
            requirePrecondition(mainAxisLayoutSize != Unset) { "position() should be called first" }
            repeat(placeablesCount) { index ->
                val placeable = placeables[index]
                val minOffset = minMainAxisOffset - placeable.mainAxisSize
                val maxOffset = maxMainAxisOffset

                var offset = offset
                val animation = animator.getAnimation(key, index)
                val layer: GraphicsLayer?
                if (animation != null) {
                    val animatedOffset = offset + animation.placementDelta
                    // cancel the animation if current and target offsets are both out of the
                    // bounds.
                    if (
                        (offset.mainAxis <= minOffset && animatedOffset.mainAxis <= minOffset) ||
                            (offset.mainAxis >= maxOffset && animatedOffset.mainAxis >= maxOffset)
                    ) {
                        animation.cancelPlacementAnimation()
                    }
                    offset = animatedOffset
                    layer = animation.layer
                } else {
                    layer = null
                }
                if (reverseLayout) {
                    offset =
                        offset.copy { mainAxisOffset ->
                            mainAxisLayoutSize - mainAxisOffset - placeable.mainAxisSize
                        }
                }
                offset += visualOffset
                animation?.finalOffset = offset
                if (isVertical) {
                    if (layer != null) {
                        placeable.placeWithLayer(offset, layer)
                    } else {
                        placeable.placeWithLayer(offset)
                    }
                } else {
                    if (layer != null) {
                        placeable.placeRelativeWithLayer(offset, layer)
                    } else {
                        placeable.placeRelativeWithLayer(offset)
                    }
                }
            }
        }

    private val IntOffset.mainAxis
        get() = if (isVertical) y else x

    private val Placeable.mainAxisSize
        get() = if (isVertical) height else width

    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        IntOffset(if (isVertical) x else mainAxisMap(x), if (isVertical) mainAxisMap(y) else y)
}

private const val Unset = Int.MIN_VALUE
