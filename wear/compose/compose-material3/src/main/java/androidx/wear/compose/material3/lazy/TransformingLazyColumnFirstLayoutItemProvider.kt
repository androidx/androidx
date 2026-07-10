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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnFirstLayoutItemProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState

/**
 * Creates and remembers a [TransformingLazyColumnFirstLayoutItemProvider] that delegates to the
 * provided [itemInfo] lambda.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnFirstLayoutItemProviderSample
 * @param itemInfo A lambda returning the [TransformingLazyColumnFirstLayoutItemProvider.ItemInfo]
 *   of the item to use as the first item to layout, or null to fall back to the default layout
 *   behavior.
 */
@Composable
public fun rememberTransformingLazyColumnFirstLayoutItemProvider(
    itemInfo: () -> TransformingLazyColumnFirstLayoutItemProvider.ItemInfo?
): TransformingLazyColumnFirstLayoutItemProvider {
    val currentItemInfo by rememberUpdatedState(itemInfo)
    return remember {
        TransformingLazyColumnFirstLayoutItemProvider { centerItem ->
            currentItemInfo() ?: centerItem
        }
    }
}

/**
 * Returns the [TransformingLazyColumnFirstLayoutItemProvider.ItemInfo] for the first visible item
 * aligned to its [TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start] edge, or null if
 * there are no visible items.
 *
 * This extension is useful when building a custom [TransformingLazyColumnFirstLayoutItemProvider]
 * that needs to ensure the first visible item maintains a static start edge during dynamic content
 * updates (such as item additions, removals, or size changes), avoiding visual jumps in the
 * viewport.
 *
 * Since the first visible item changes rapidly during an active scroll, tracking it continuously
 * does not provide a stable visual reference. When using this property with
 * [rememberTransformingLazyColumnFirstLayoutItemProvider], it is highly recommended to yield to the
 * default layout behavior by returning null when [TransformingLazyColumnState.isScrollInProgress]
 * is true. This allows the list to fall back to its default layout behavior, which accurately
 * tracks the scroll gesture, while also avoiding unnecessary computational overhead.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnFirstVisibleItemLayoutItemProviderSample
 */
public val TransformingLazyColumnState.firstVisibleItemLayoutItemInfo:
    TransformingLazyColumnFirstLayoutItemProvider.ItemInfo?
    get() =
        layoutInfo.visibleItems.firstOrNull()?.let {
            TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
                key = null,
                index = it.index,
                itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start,
                offset = it.offset,
            )
        }

/**
 * Returns the [TransformingLazyColumnFirstLayoutItemProvider.ItemInfo] for the visible item with
 * the given [itemKey], aligned to the specified [itemEdge]. Returns null if the item is not
 * currently visible on screen.
 *
 * This helper is useful when building a custom [TransformingLazyColumnFirstLayoutItemProvider] that
 * needs to track and stabilize the position of a specific key (e.g. an expanding item) during
 * content size animations.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnFirstLayoutItemProviderSample
 * @param itemKey The unique key of the item to search for.
 * @param itemEdge The [TransformingLazyColumnFirstLayoutItemProvider.ItemEdge] (Start or End) of
 *   the item to use for the layout reference.
 */
public fun TransformingLazyColumnState.layoutItemInfoOf(
    itemKey: Any,
    itemEdge: TransformingLazyColumnFirstLayoutItemProvider.ItemEdge,
): TransformingLazyColumnFirstLayoutItemProvider.ItemInfo? =
    layoutInfo.visibleItems
        .fastFirstOrNull { it.key == itemKey }
        ?.let { item ->
            val targetOffset =
                if (itemEdge == TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.Start) {
                    item.offset
                } else {
                    item.offset + item.transformedHeight
                }
            TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(
                key = item.key,
                index = item.index,
                itemEdge = itemEdge,
                offset = targetOffset,
            )
        }
