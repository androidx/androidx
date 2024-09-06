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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy

/**
 * An implementation of [LazyLayoutScrollScope] that can be used with LazyStaggeredGrids.
 *
 * @param state The [LazyStaggeredGridState] associated with the layout where this custom scroll
 *   should be performed.
 * @param scrollScope The base [ScrollScope] where the scroll session was created.
 * @return An implementation of [LazyLayoutScrollScope] that works with
 *   [LazyHorizontalStaggeredGrid] and [LazyVerticalStaggeredGrid].
 * @sample androidx.compose.foundation.samples.LazyStaggeredGridCustomScrollUsingLazyLayoutScrollScopeSample
 */
fun LazyLayoutScrollScope(
    state: LazyStaggeredGridState,
    scrollScope: ScrollScope
): LazyLayoutScrollScope {

    return object : LazyLayoutScrollScope, ScrollScope by scrollScope {
        override val firstVisibleItemIndex: Int
            get() = state.firstVisibleItemIndex

        override val firstVisibleItemScrollOffset: Int
            get() = state.firstVisibleItemScrollOffset

        override val lastVisibleItemIndex: Int
            get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        override val itemCount: Int
            get() = state.layoutInfo.totalItemsCount

        override fun snapToItem(index: Int, offset: Int) {
            with(state) { snapToItemInternal(index, offset, forceRemeasure = true) }
        }

        override fun calculateDistanceTo(targetIndex: Int, targetOffset: Int): Int {
            val layoutInfo = state.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return 0
            val visibleItem =
                layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
            return if (visibleItem == null) {
                val averageMainAxisItemSize = calculateVisibleItemsAverageSize(layoutInfo)

                val laneCount = state.laneCount
                val lineDiff = targetIndex / laneCount - firstVisibleItemIndex / laneCount
                averageMainAxisItemSize * lineDiff - firstVisibleItemScrollOffset
            } else {
                if (layoutInfo.orientation == Orientation.Vertical) {
                    visibleItem.offset.y
                } else {
                    visibleItem.offset.x
                }
            } + targetOffset
        }

        private fun calculateVisibleItemsAverageSize(layoutInfo: LazyStaggeredGridLayoutInfo): Int {
            val visibleItems = layoutInfo.visibleItemsInfo
            val itemSizeSum =
                visibleItems.fastSumBy {
                    if (layoutInfo.orientation == Orientation.Vertical) {
                        it.size.height
                    } else {
                        it.size.width
                    }
                }
            return itemSizeSum / visibleItems.size + layoutInfo.mainAxisItemSpacing
        }
    }
}
