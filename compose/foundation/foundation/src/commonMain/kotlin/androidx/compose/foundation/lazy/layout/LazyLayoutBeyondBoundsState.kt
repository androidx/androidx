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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.util.fastForEach
import kotlin.math.min

internal interface LazyLayoutBeyondBoundsState {

    fun remeasure()

    val itemCount: Int

    val hasVisibleItems: Boolean

    val firstPlacedIndex: Int

    val lastPlacedIndex: Int
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutItemProvider.calculateLazyLayoutPinnedIndices(
    pinnedItemList: LazyLayoutPinnedItemList,
    beyondBoundsInfo: LazyLayoutBeyondBoundsInfo,
): List<Int> {
    if (!beyondBoundsInfo.hasIntervals() && pinnedItemList.isEmpty()) {
        return emptyList()
    } else {
        val pinnedItems = mutableListOf<Int>()
        val beyondBoundsRange = if (beyondBoundsInfo.hasIntervals()) {
            beyondBoundsInfo.start..min(beyondBoundsInfo.end, itemCount - 1)
        } else {
            IntRange.EMPTY
        }
        pinnedItemList.fastForEach {
            val index = findIndexByKey(it.key, it.index)
            if (index in beyondBoundsRange) return@fastForEach
            if (index !in 0 until itemCount) return@fastForEach
            pinnedItems.add(index)
        }
        for (i in beyondBoundsRange) {
            pinnedItems.add(i)
        }
        return pinnedItems
    }
}