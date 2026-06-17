/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnFirstLayoutItemProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState

/**
 * Creates and remembers a [TransformingLazyColumnFirstLayoutItemProvider] that aligns the layout
 * relative to the top edge (or bottom edge, if `reverseLayout` is enabled) of the first visible
 * item in the viewport during layout changes.
 *
 * This provider maintains a static start edge during dynamic content updates (such as item
 * additions, removals, or size changes). Since the first visible item changes rapidly during an
 * active scroll, tracking it continuously does not provide a stable visual reference. The provider
 * is suspended during motion, allowing the list to fall back to its default layout behavior, which
 * accurately tracks the scroll gesture, while also avoiding unnecessary computational overhead.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnFirstVisibleItemProviderSample
 * @param state The [TransformingLazyColumnState] of the [TransformingLazyColumn].
 */
@Composable
public fun rememberTransformingLazyColumnFirstVisibleItemProvider(
    state: TransformingLazyColumnState
): TransformingLazyColumnFirstLayoutItemProvider {
    return remember(state) {
        TransformingLazyColumnFirstLayoutItemProvider { current ->
            // Yield to default center-anchored behavior during scroll to ensure accurate
            // scroll tracking and prevent expansion animations from fighting the user's gesture.
            if (state.isScrollInProgress)
                return@TransformingLazyColumnFirstLayoutItemProvider current
            val visibleItems = state.layoutInfo.visibleItems
            if (visibleItems.isEmpty()) return@TransformingLazyColumnFirstLayoutItemProvider current
            val firstItem = visibleItems.first()
            TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
                // Prioritize locating by key over index across list changes
                key = firstItem.key,
                index = firstItem.index,
                itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start,
                offset = firstItem.offset,
            )
        }
    }
}

/**
 * Creates and remembers a [TransformingLazyColumnFirstLayoutItemProvider] that aligns the layout
 * relative to a specific item (identified by [itemKey]), keeping its chosen [itemEdge] edge
 * visually fixed on screen during layout changes.
 *
 * When passed to [TransformingLazyColumn], this provider ensures the specified item's edge remains
 * at its previous screen offset across dynamic content or size changes. If the item is not visible
 * (or [itemKey] returns null), it falls back to the default layout behavior.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnFirstLayoutItemProviderSample
 * @param state The [TransformingLazyColumnState] of the [TransformingLazyColumn].
 * @param itemKey A lambda returning the unique key of the item to use as the first item to layout.
 *   If the lambda returns null, or if the item with the specified key is not currently visible on
 *   screen, [TransformingLazyColumn] will fall back to its default layout behavior.
 * @param itemEdge A lambda returning the [TransformingLazyColumnFirstLayoutItemProvider.ItemEdge]
 *   (Start or End) of the item to keep fixed. Defaults to locking the
 *   [TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start] edge.
 */
@Composable
public fun rememberTransformingLazyColumnFirstLayoutItemProvider(
    state: TransformingLazyColumnState,
    itemKey: () -> Any?,
    itemEdge: () -> TransformingLazyColumnFirstLayoutItemProvider.ItemEdge = {
        TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start
    },
): TransformingLazyColumnFirstLayoutItemProvider {
    val currentItemKey = rememberUpdatedState(itemKey)
    val currentItemEdge = rememberUpdatedState(itemEdge)
    return remember(state) {
        TransformingLazyColumnFirstLayoutItemProvider { current ->
            val key =
                currentItemKey.value()
                    ?: return@TransformingLazyColumnFirstLayoutItemProvider current
            val activeType = currentItemEdge.value()
            val layoutItem =
                state.layoutInfo.visibleItems.fastFirstOrNull { it.key == key }
                    ?: return@TransformingLazyColumnFirstLayoutItemProvider current
            when (activeType) {
                TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start -> {
                    TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
                        // Prioritize locating by key over index across list changes
                        key = layoutItem.key,
                        index = layoutItem.index,
                        itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start,
                        offset = layoutItem.offset,
                    )
                }
                TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.End -> {
                    TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
                        // Prioritize locating by key over index across list changes
                        key = layoutItem.key,
                        index = layoutItem.index,
                        itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.End,
                        offset = layoutItem.offset + layoutItem.transformedHeight,
                    )
                }
                else -> current
            }
        }
    }
}
