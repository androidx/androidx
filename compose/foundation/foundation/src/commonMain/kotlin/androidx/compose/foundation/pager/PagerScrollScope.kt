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

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import kotlin.math.roundToInt

/**
 * A [LazyLayoutScrollScope] that allows customization of animated scroll in [Pager]. The scope
 * contains information about the layout where animated scroll can be performed as well as the
 * necessary tools to do that respecting the scroll mutation priority.
 *
 * @param state The [PagerState] associated with the layout where this animated scroll should be
 *   performed.
 * @param scrollScope The base [ScrollScope] where the scroll session was created.
 * @return An implementation of [LazyLayoutScrollScope] that works with [HorizontalPager] and
 *   [VerticalPager].
 * @sample androidx.compose.foundation.samples.PagerCustomScrollUsingLazyLayoutScrollScopeSample
 */
fun LazyLayoutScrollScope(state: PagerState, scrollScope: ScrollScope): LazyLayoutScrollScope {
    return object : LazyLayoutScrollScope, ScrollScope by scrollScope {

        override val firstVisibleItemIndex: Int
            get() = state.firstVisiblePage

        override val firstVisibleItemScrollOffset: Int
            get() = state.firstVisiblePageOffset

        override val lastVisibleItemIndex: Int
            get() = state.layoutInfo.visiblePagesInfo.last().index

        override val itemCount: Int
            get() = state.pageCount

        override fun snapToItem(index: Int, offset: Int) {
            val offsetFraction = offset / state.pageSizeWithSpacing.toFloat()
            state.snapToItem(index, offsetFraction, forceRemeasure = true)
        }

        override fun calculateDistanceTo(targetIndex: Int, targetOffset: Int): Int {
            return ((targetIndex - state.currentPage) * state.pageSizeWithSpacing -
                    state.currentPageOffsetFraction * state.pageSizeWithSpacing + targetOffset)
                .roundToInt()
        }
    }
}
