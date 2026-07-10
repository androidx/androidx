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

package androidx.compose.foundation.lazy.layout

import androidx.collection.IntList
import androidx.collection.emptyIntList
import androidx.collection.mutableIntListOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.util.fastForEach
import kotlin.math.min

internal interface LazyLayoutBeyondBoundsState {

    val itemCount: Int

    val hasVisibleItems: Boolean

    val firstPlacedIndex: Int

    val lastPlacedIndex: Int

    fun itemsPerViewport(): Int
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutItemProvider.calculateLazyLayoutPinnedIndices(
    pinnedItemList: LazyLayoutPinnedItemList,
    beyondBoundsInfo: LazyLayoutBeyondBoundsInfo,
): IntList {
    val pinnedItemList = pinnedItemList.toList()
    if (!beyondBoundsInfo.hasIntervals() && pinnedItemList.isEmpty()) {
        return emptyIntList()
    } else {
        val pinnedItems = mutableIntListOf()
        val beyondBoundsStart: Int
        val beyondBoundsEnd: Int
        if (beyondBoundsInfo.hasIntervals()) {
            beyondBoundsStart = beyondBoundsInfo.start
            beyondBoundsEnd = min(beyondBoundsInfo.end, itemCount - 1)
        } else {
            // Empty range
            beyondBoundsStart = 1
            beyondBoundsEnd = 0
        }

        pinnedItemList.fastForEach {
            val index = findIndexByKey(it.key, it.index)
            if (index in beyondBoundsStart..beyondBoundsEnd) return@fastForEach
            if (index !in 0 until itemCount) return@fastForEach
            pinnedItems.add(index)
        }

        for (i in beyondBoundsStart..beyondBoundsEnd) {
            pinnedItems.add(i)
        }

        pinnedItems.sort()
        return pinnedItems
    }
}
