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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateScrollScope
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy

@ExperimentalFoundationApi
internal class LazyStaggeredGridAnimateScrollScope(
    private val state: LazyStaggeredGridState
) : LazyLayoutAnimateScrollScope {

    override val firstVisibleItemIndex: Int get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int get() = state.layoutInfo.totalItemsCount

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        with(state) {
            snapToItemInternal(index, scrollOffset, forceRemeasure = true)
        }
    }

    override fun calculateDistanceTo(targetIndex: Int): Float {
        val visibleItem =
            state.layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
        return if (visibleItem == null) {
            val averageMainAxisItemSize = visibleItemsAverageSize

            val laneCount = state.laneCount
            val lineDiff = targetIndex / laneCount - firstVisibleItemIndex / laneCount
            averageMainAxisItemSize * lineDiff.toFloat() - firstVisibleItemScrollOffset
        } else {
            if (state.layoutInfo.orientation == Orientation.Vertical) {
                visibleItem.offset.y
            } else {
                visibleItem.offset.x
            }.toFloat()
        }
    }

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }

    private val visibleItemsAverageSize: Int
        get() {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val itemSizeSum = visibleItems.fastSumBy {
                if (layoutInfo.orientation == Orientation.Vertical) {
                    it.size.height
                } else {
                    it.size.width
                }
            }
            return itemSizeSum / visibleItems.size + layoutInfo.mainAxisItemSpacing
        }
}
