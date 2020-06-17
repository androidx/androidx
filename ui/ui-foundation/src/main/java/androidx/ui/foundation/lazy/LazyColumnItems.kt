/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.lazy

import androidx.compose.Composable
import androidx.compose.ExperimentalComposeApi
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.materialize
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.scrollable
import androidx.ui.layout.Spacer

/**
 * A vertically scrolling list that only composes and lays out the currently visible items.
 *
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked vertically. Note that [LazyColumnItems] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 */
@Composable
fun <T> LazyColumnItems(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    val state = remember { LazyItemsState<T>() }
    @OptIn(ExperimentalComposeApi::class)
    state.recomposer = currentComposer.recomposer
    state.itemContent = itemContent
    state.items = items
    state.context = ContextAmbient.current
    state.compositionRef = compositionReference()
    state.forceRecompose = true

    androidx.ui.core.LayoutNode(
        modifier = currentComposer.materialize(
            modifier
                .scrollable(
                    dragDirection = DragDirection.Vertical,
                    scrollableState = androidx.ui.foundation.gestures.ScrollableState(
                        onScrollDeltaConsumptionRequested =
                        state.onScrollDeltaConsumptionRequestedListener
                    )
                )
                .clipToBounds()
        ),
        ref = state.rootNodeRef,
        measureBlocks = state.measureBlocks
    )
    state.recomposeIfAttached()
    onDispose {
        state.disposeAllChildren()
    }
}
