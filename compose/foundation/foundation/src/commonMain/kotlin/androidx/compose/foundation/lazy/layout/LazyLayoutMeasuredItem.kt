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

package androidx.compose.foundation.lazy.layout

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach

/**
 * Represents a Lazy Layout item that has been measured, called [LazyLayoutMeasureScope.compose] and
 * measured.
 */
internal interface LazyLayoutMeasuredItem {
    /** The index of the item */
    val index: Int

    /** A key associated with this item */
    val key: Any

    /**
     * The placeables resulted in this item's measurement. Items may have multiple pleaceables
     * because [LazyLayoutItemProvider.Item] can emit multiple composables.
     */
    val placeables: List<Placeable>

    /**
     * The size of the pleceables when combined in the horizontal axis in accordance with this
     * Layout's policy (e.g. if it's a list with arrangements).
     */
    val horizontalAxisSize: Int

    /**
     * The size of the pleceables when combined in the vertical axis in accordance with this
     * Layout's policy (e.g. if it's a list with arrangements).
     */
    val verticalAxisSize: Int

    /** The spacing used on the horizontal axis in this layout. */
    val horizontalAxisSpacing: Int

    /** The spacing used on the vertical axis in this layout. */
    val verticalAxisSpacing: Int

    /** The constraints that were used to measure this item. */
    val constraints: Constraints

    /**
     * In a 1 dimensional space, a lane is an item's position along the main axis (e.g. in a column
     * it's the column number, in the row it's the row number and so on). In a 2-dimensional space
     * the implementor decides the convention.
     */
    val lane: Int

    /** The number of lanes this item's spans. */
    val span: Int

    /**
     * The offset applied to the placeable in [placeableIndex].
     *
     * @throws IllegalStateException If this is called before [position].
     */
    fun getOffset(placeableIndex: Int): IntOffset

    /**
     * Calculate the offset of [placeables] based on the layout information.
     *
     * @param horizontalAxisOffset The horizontal offset where the placeables will be placed in the
     *   layout.
     * @param verticalAxisOffset The vertical offset where the placeables will be placed in the
     *   layout.
     * @param layoutWidth The layout width
     * @param layoutHeight The layout height
     */
    fun position(
        horizontalAxisOffset: Int,
        verticalAxisOffset: Int,
        layoutWidth: Int,
        layoutHeight: Int,
    )

    /** Disable this item's scrollability. Scroll deltas won't be applied to this item's offset. */
    fun makeNonScrollable()
}

internal abstract class LazyLayoutMeasuredItemProvider<T : LazyLayoutMeasuredItem> {
    /**
     * A cache of the previously composed items. It allows us to support [get] re-executions with
     * the same index during the same measure pass.
     */
    private val placeablesCache = mutableIntObjectMapOf<List<Placeable>>()

    abstract fun getAndMeasure(index: Int, lane: Int, span: Int, constraints: Constraints): T

    fun LazyLayoutMeasureScope.getPlaceables(
        index: Int,
        constraints: Constraints,
    ): List<Placeable> {
        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable != null) {
            cachedPlaceable
        } else {
            val measurables = compose(index)
            List(measurables.size) { i -> measurables[i].measure(constraints) }
                .also { placeablesCache[index] = it }
        }
    }
}

internal fun <T : LazyLayoutMeasuredItem> updatedVisibleItems(
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    positionedItems: List<T>,
    stickingItems: List<T>,
): List<T> {
    if (positionedItems.isEmpty()) return emptyList()

    val finalVisibleItems = stickingItems.toMutableList()

    // positioned items between firstVisibleIndex and lastVisibleIndex
    positionedItems.fastForEach {
        if (it.index in firstVisibleIndex..lastVisibleIndex) finalVisibleItems.add(it)
    }

    finalVisibleItems.sortWith(LazyLayoutMeasuredItemIndexComparator)

    return finalVisibleItems
}

private val LazyLayoutMeasuredItemIndexComparator =
    Comparator<LazyLayoutMeasuredItem> { a, b -> a.index.compareTo(b.index) }

internal val LazyLayoutMeasuredItem.placeablesCount: Int
    get() = placeables.size

internal fun LazyLayoutMeasuredItem.mainAxisSizeWithSpacings(isVertical: Boolean): Int {
    return if (isVertical) {
        verticalAxisSize + verticalAxisSpacing
    } else {
        horizontalAxisSize + horizontalAxisSpacing
    }
}

internal fun LazyLayoutMeasuredItem.getParentData(index: Int) = placeables[index].parentData
