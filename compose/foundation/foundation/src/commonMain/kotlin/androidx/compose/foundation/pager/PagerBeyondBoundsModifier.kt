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
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberPagerBeyondBoundsState(
    state: PagerState,
    beyondBoundsPageCount: Int
): LazyLayoutBeyondBoundsState {
    return remember(state, beyondBoundsPageCount) {
        PagerBeyondBoundsState(state, beyondBoundsPageCount)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class PagerBeyondBoundsState(
    private val state: PagerState,
    private val beyondBoundsPageCount: Int
) : LazyLayoutBeyondBoundsState {
    override fun remeasure() {
        state.remeasurement?.forceRemeasure()
    }

    override val itemCount: Int
        get() = state.pageCount
    override val hasVisibleItems: Boolean
        get() = state.layoutInfo.visiblePagesInfo.isNotEmpty()
    override val firstPlacedIndex: Int
        get() = maxOf(0, state.firstVisiblePage - beyondBoundsPageCount)
    override val lastPlacedIndex: Int
        get() = minOf(
            itemCount - 1,
            state.layoutInfo.visiblePagesInfo.last().index + beyondBoundsPageCount
        )
}
