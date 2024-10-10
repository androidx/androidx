/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.lazyLayoutBeyondBoundsModifier
import androidx.compose.foundation.lazy.layout.lazyLayoutSemantics
import androidx.compose.foundation.scrollingContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyStaggeredGrid(
    /** State controlling the scroll position */
    state: LazyStaggeredGridState,
    /** The layout orientation of the grid */
    orientation: Orientation,
    /** Cross axis positions and sizes of slots per line, e.g. the columns for vertical grid. */
    slots: LazyGridStaggeredGridSlotsProvider,
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean = true,
    /** The overscroll effect to render and dispatch events to */
    overscrollEffect: OverscrollEffect?,
    /** The vertical spacing for items/lines. */
    mainAxisSpacing: Dp = 0.dp,
    /** The horizontal spacing for items/lines. */
    crossAxisSpacing: Dp = 0.dp,
    /** The content of the grid */
    content: LazyStaggeredGridScope.() -> Unit
) {
    val itemProviderLambda = rememberStaggeredGridItemProviderLambda(state, content)
    val coroutineScope = rememberCoroutineScope()
    val graphicsContext = LocalGraphicsContext.current
    val measurePolicy =
        rememberStaggeredGridMeasurePolicy(
            state,
            itemProviderLambda,
            contentPadding,
            reverseLayout,
            orientation,
            mainAxisSpacing,
            crossAxisSpacing,
            coroutineScope,
            slots,
            graphicsContext
        )
    val semanticState = rememberLazyStaggeredGridSemanticState(state, reverseLayout)

    val beyondBoundsModifier =
        if (userScrollEnabled) {
            Modifier.lazyLayoutBeyondBoundsModifier(
                state = rememberLazyStaggeredGridBeyondBoundsState(state = state),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                orientation = orientation,
            )
        } else {
            Modifier
        }

    LazyLayout(
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .then(state.awaitLayoutModifier)
                .lazyLayoutSemantics(
                    itemProviderLambda = itemProviderLambda,
                    state = semanticState,
                    orientation = orientation,
                    userScrollEnabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                )
                .then(beyondBoundsModifier)
                .then(state.itemAnimator.modifier)
                .scrollingContainer(
                    state = state,
                    orientation = orientation,
                    enabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                    flingBehavior = flingBehavior,
                    interactionSource = state.mutableInteractionSource,
                    overscrollEffect = overscrollEffect
                ),
        prefetchState = state.prefetchState,
        itemProvider = itemProviderLambda,
        measurePolicy = measurePolicy
    )
}

/** Slot configuration of staggered grid */
internal class LazyStaggeredGridSlots(val positions: IntArray, val sizes: IntArray)
