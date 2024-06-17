/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateScrollScope
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy

internal class LazyListAnimateScrollScope(private val state: LazyListState) :
    LazyLayoutAnimateScrollScope {

    override val firstVisibleItemIndex: Int
        get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int
        get() = state.layoutInfo.totalItemsCount

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        state.snapToItemIndexInternal(index, scrollOffset, forceRemeasure = true)
    }

    override fun calculateDistanceTo(targetIndex: Int): Float {
        val layoutInfo = state.layoutInfo
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val visibleItem = layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
        return if (visibleItem == null) {
            val averageSize = calculateVisibleItemsAverageSize(layoutInfo)
            val indexesDiff = targetIndex - firstVisibleItemIndex
            (averageSize * indexesDiff).toFloat() - firstVisibleItemScrollOffset
        } else {
            visibleItem.offset.toFloat()
        }
    }

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }

    private fun calculateVisibleItemsAverageSize(layoutInfo: LazyListLayoutInfo): Int {
        val visibleItems = layoutInfo.visibleItemsInfo
        val itemsSum = visibleItems.fastSumBy { it.size }
        return itemsSum / visibleItems.size + layoutInfo.mainAxisItemSpacing
    }
}
