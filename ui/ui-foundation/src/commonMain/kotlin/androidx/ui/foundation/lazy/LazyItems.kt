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

import androidx.compose.Applier
import androidx.compose.Composable
import androidx.compose.ExperimentalComposeApi
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.emit
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.LayoutNode
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
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
    LazyItems(items, modifier, itemContent, isVertical = true)
}

/**
 * A horizontally scrolling list that only composes and lays out the currently visible items.
 *
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked horizontally. Note that [LazyRowItems] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 */
@Composable
fun <T> LazyRowItems(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    LazyItems(items, modifier, itemContent, isVertical = false)
}

@Composable
@OptIn(ExperimentalLayoutNodeApi::class)
private fun <T> LazyItems(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
    isVertical: Boolean
) {
    val state = remember { LazyItemsState<T>(isVertical = isVertical) }
    @OptIn(ExperimentalComposeApi::class)
    state.recomposer = currentComposer.recomposer
    state.itemContent = itemContent
    state.items = items
    state.compositionRef = compositionReference()
    state.forceRecompose = true

    val dragDirection = if (isVertical) DragDirection.Vertical else DragDirection.Horizontal
    val materialized = currentComposer.materialize(
        modifier
            .scrollable(
                dragDirection = dragDirection,
                scrollableState = androidx.ui.foundation.gestures.ScrollableState(
                    onScrollDeltaConsumptionRequested =
                    state.onScrollDeltaConsumptionRequestedListener
                )
            )
            .clipToBounds()
    )
    @OptIn(ExperimentalComposeApi::class)
    emit<LayoutNode, Applier<Any>>(
        ctor = LayoutEmitHelper.constructor,
        update = {
            set(materialized, LayoutEmitHelper.setModifier)
            set(state.rootNodeRef, LayoutEmitHelper.setRef)
            set(state.measureBlocks, LayoutEmitHelper.setMeasureBlocks)
        }
    )
    state.recomposeIfAttached()
    onDispose {
        state.disposeAllChildren()
    }
}

/**
 * Object of pre-allocated lambdas used to make emits to LayoutNodes allocation-less.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
private object LayoutEmitHelper {
    val constructor: () -> LayoutNode = { LayoutNode() }
    val setModifier: LayoutNode.(Modifier) -> Unit = { this.modifier = it }
    val setMeasureBlocks: LayoutNode.(LayoutNode.MeasureBlocks) -> Unit =
        { this.measureBlocks = it }
    val setRef: LayoutNode.(Ref<LayoutNode>) -> Unit = { it.value = this }
}
