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

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach

internal interface LazyLayoutMeasuredItem {
    val index: Int
    val key: Any
    val isVertical: Boolean
    val mainAxisSizeWithSpacings: Int
    val placeablesCount: Int
    var nonScrollableItem: Boolean
    val constraints: Constraints
    val lane: Int
    val span: Int

    fun getOffset(index: Int): IntOffset

    fun position(mainAxisOffset: Int, crossAxisOffset: Int, layoutWidth: Int, layoutHeight: Int)

    fun getParentData(index: Int): Any?
}

internal interface LazyLayoutMeasuredItemProvider<T : LazyLayoutMeasuredItem> {
    fun getAndMeasure(index: Int, lane: Int, span: Int, constraints: Constraints): T
}

internal fun <T : LazyLayoutMeasuredItem> updatedVisibleItems(
    noExtraItems: Boolean,
    currentVisibleItems: List<T>,
    positionedItems: List<T>,
    stickingItems: List<T>
): List<T> {
    if (positionedItems.isEmpty() || currentVisibleItems.isEmpty()) return emptyList()
    val firstVisibleIndex =
        if (noExtraItems) positionedItems.first().index else currentVisibleItems.first().index
    val lastVisibleIndex =
        if (noExtraItems) positionedItems.last().index else currentVisibleItems.last().index

    val finalVisibleItems = stickingItems.toMutableList()

    // positioned items between firstVisibleIndex and lastVisibleIndex
    positionedItems.fastForEach {
        if (it.index in firstVisibleIndex..lastVisibleIndex) finalVisibleItems.add(it)
    }

    finalVisibleItems.sortWith(LazyLayoutMeasuredItemIndexComparator)

    return finalVisibleItems
}

private val LazyLayoutMeasuredItem.mainAxisOffset
    get() = getOffset(0).let { if (isVertical) it.y else it.x }

private val LazyLayoutMeasuredItemIndexComparator =
    Comparator<LazyLayoutMeasuredItem> { a, b -> a.index.compareTo(b.index) }
