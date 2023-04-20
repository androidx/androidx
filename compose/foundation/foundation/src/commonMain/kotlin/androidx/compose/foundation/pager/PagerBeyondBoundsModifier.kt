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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsState
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsModifierLocal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * This modifier is used to measure and place additional pages when the Pager receives a
 * request to layout pages beyond the visible bounds.
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.pagerBeyondBoundsModifier(
    state: PagerState,
    beyondBoundsPageCount: Int,
    reverseLayout: Boolean,
    orientation: Orientation
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    return this then remember(
        state,
        beyondBoundsPageCount,
        reverseLayout,
        layoutDirection,
        orientation
    ) {
        LazyLayoutBeyondBoundsModifierLocal(
            PagerBeyondBoundsState(state, beyondBoundsPageCount),
            state.beyondBoundsInfo,
            reverseLayout,
            layoutDirection,
            orientation
        )
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
        get() = state.layoutInfo.pagesCount
    override val hasVisibleItems: Boolean
        get() = state.layoutInfo.visiblePagesInfo.isNotEmpty()
    override val firstVisibleIndex: Int
        get() = maxOf(0, state.firstVisiblePage - beyondBoundsPageCount)
    override val lastVisibleIndex: Int
        get() = minOf(
            itemCount - 1,
            state.layoutInfo.visiblePagesInfo.last().index + beyondBoundsPageCount
        )
}