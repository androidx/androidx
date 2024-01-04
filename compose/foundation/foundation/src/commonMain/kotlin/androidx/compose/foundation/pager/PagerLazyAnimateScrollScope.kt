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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateScrollScope
import androidx.compose.ui.util.fastFirstOrNull

/**
 * A [LazyLayoutAnimateScrollScope] that allows customization of animated scroll in [Pager].
 * The scope contains information about the layout where animated scroll can be performed as well as
 * the necessary tools to do that respecting the scroll mutation priority.
 *
 */
@ExperimentalFoundationApi
internal fun PagerLazyAnimateScrollScope(state: PagerState): LazyLayoutAnimateScrollScope {
    return object : LazyLayoutAnimateScrollScope {

        override val firstVisibleItemIndex: Int get() = state.firstVisiblePage

        override val firstVisibleItemScrollOffset: Int get() = state.firstVisiblePageOffset

        override val lastVisibleItemIndex: Int
            get() =
                state.layoutInfo.visiblePagesInfo.last().index

        override val itemCount: Int get() = state.pageCount

        override fun getVisibleItemScrollOffset(index: Int): Int {
            return state.layoutInfo.visiblePagesInfo.fastFirstOrNull { it.index == index }?.offset
                ?: 0
        }

        override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
            state.snapToItem(index, scrollOffset)
        }

        override fun calculateDistanceTo(targetIndex: Int, targetItemOffset: Int): Float {
            return (targetIndex - state.currentPage) * visibleItemsAverageSize.toFloat() +
                targetItemOffset
        }

        override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
            state.scroll(block = block)
        }

        override val visibleItemsAverageSize: Int
            get() = state.pageSize + state.pageSpacing
    }
}
