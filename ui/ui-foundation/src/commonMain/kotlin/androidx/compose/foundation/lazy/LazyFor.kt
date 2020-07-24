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

package androidx.compose.foundation.lazy

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.SubcomposeLayout
import androidx.ui.core.clipToBounds
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.compose.foundation.gestures.rememberScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.ui.core.Alignment
import androidx.ui.core.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.unit.dp

/**
 * A vertically scrolling list that only composes and lays out the currently visible items.
 *
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param contentPadding convenience param to specify a padding around the whole content. This will
 * add padding for the content after it has been clipped, which is not possible via [modifier]
 * param. Note that it is *not* a padding applied for each item's content
 * @param horizontalGravity the horizontal gravity applied to the items
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked vertically. Note that [LazyColumnFor] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 */
@Composable
fun <T> LazyColumnFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable (T) -> Unit
) {
    LazyFor(
        items = items,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalGravity = horizontalGravity,
        itemContent = itemContent,
        isVertical = true
    )
}

@Deprecated(
    "LazyColumnItems was renamed to LazyColumnFor",
    ReplaceWith("LazyColumnFor(items, modifier, contentPadding, horizontalGravity, itemContent)")
)
@Composable
fun <T> LazyColumnItems(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable (T) -> Unit
) {
    LazyColumnFor(
        items = items,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalGravity = horizontalGravity,
        itemContent = itemContent
    )
}

/**
 * A horizontally scrolling list that only composes and lays out the currently visible items.
 *
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param contentPadding convenience param to specify a padding around the whole content. This will
 * add padding for the content after it has been clipped, which is not possible via [modifier]
 * param. Note that it is *not* a padding applied for each item's content
 * @param verticalGravity the vertical gravity applied to the items
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked horizontally. Note that [LazyRowFor] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 */
@Composable
fun <T> LazyRowFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    verticalGravity: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable (T) -> Unit
) {
    LazyFor(
        items = items,
        modifier = modifier,
        contentPadding = contentPadding,
        itemContent = itemContent,
        verticalGravity = verticalGravity,
        isVertical = false
    )
}

@Deprecated(
    "LazyRowItems was renamed to LazyRowFor",
    ReplaceWith("LazyRowFor(items, modifier, contentPadding, horizontalGravity, itemContent)")
)
@Composable
fun <T> LazyRowItems(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    verticalGravity: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable (T) -> Unit
) {
    LazyRowFor(
        items = items,
        modifier = modifier,
        contentPadding = contentPadding,
        itemContent = itemContent,
        verticalGravity = verticalGravity
    )
}

@Composable
@OptIn(ExperimentalSubcomposeLayoutApi::class)
private fun <T> LazyFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding,
    itemContent: @Composable (T) -> Unit,
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    verticalGravity: Alignment.Vertical = Alignment.Top,
    isVertical: Boolean
) {
    val state = remember { LazyForState<T>(isVertical = isVertical) }
    SubcomposeLayout<DataIndex>(
        modifier
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                controller = rememberScrollableController(consumeScrollDelta = state.onScrollDelta)
            )
            .clipToBounds()
            .padding(contentPadding)
            .then(state.remeasurementModifier)
    ) { constraints ->
        state.measure(this, constraints, items, itemContent, horizontalGravity, verticalGravity)
    }
}
