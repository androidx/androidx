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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsModifierLocal
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * This modifier is used to measure and place additional items when the lazyList receives a
 * request to layout items beyond the visible bounds.
 */
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.lazyStaggeredGridBeyondBoundsModifier(
    state: LazyStaggeredGridState,
    reverseLayout: Boolean,
    orientation: Orientation
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    val beyondBoundsState = remember(state) {
        LazyStaggeredGridBeyondBoundsState(state)
    }
    return this then remember(
        beyondBoundsState,
        state,
        reverseLayout,
        layoutDirection,
        orientation
    ) {
        LazyLayoutBeyondBoundsModifierLocal(
            beyondBoundsState,
            state.beyondBoundsInfo,
            reverseLayout,
            layoutDirection,
            orientation
        )
    }
}

internal class LazyStaggeredGridBeyondBoundsState(
    val state: LazyStaggeredGridState,
) : LazyLayoutBeyondBoundsState {

    override fun remeasure() {
        state.remeasurement?.forceRemeasure()
    }

    override val itemCount: Int
        get() = state.layoutInfo.totalItemsCount
    override val hasVisibleItems: Boolean
        get() = state.layoutInfo.visibleItemsInfo.isNotEmpty()
    override val firstPlacedIndex: Int
        get() = state.firstVisibleItemIndex
    override val lastPlacedIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.last().index
}