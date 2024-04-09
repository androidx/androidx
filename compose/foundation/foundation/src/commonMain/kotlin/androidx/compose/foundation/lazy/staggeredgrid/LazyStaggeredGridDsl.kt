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

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Vertical staggered grid layout that composes and lays out only items currently visible on screen.
 *
 * Sample:
 * @sample androidx.compose.foundation.samples.LazyVerticalStaggeredGridSample
 *
 * Sample with custom item spans:
 * @sample androidx.compose.foundation.samples.LazyVerticalStaggeredGridSpanSample
 *
 * @param columns description of the size and number of staggered grid columns.
 * @param modifier modifier to apply to the layout.
 * @param state state object that can be used to control and observe staggered grid state.
 * @param contentPadding padding around the content.
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are
 * laid out in the reverse order and [LazyStaggeredGridState.firstVisibleItemIndex] == 0 means
 * that grid is scrolled to the bottom.
 * @param verticalItemSpacing vertical spacing between items.
 * @param horizontalArrangement arrangement specifying horizontal spacing between items. The item
 *  arrangement specifics are ignored for now.
 * @param flingBehavior logic responsible for handling fling.
 * @param userScrollEnabled whether scroll with gestures or accessibility actions are allowed. It is
 *  still possible to scroll programmatically through state when [userScrollEnabled] is set to false
 * @param content a lambda describing the staggered grid content. Inside this block you can use
 *  [LazyStaggeredGridScope.items] to present list of items or [LazyStaggeredGridScope.item] for a
 *  single one.
 */
@Composable
fun LazyVerticalStaggeredGrid(
    columns: StaggeredGridCells,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalItemSpacing: Dp = 0.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyStaggeredGridScope.() -> Unit
) {
    LazyStaggeredGrid(
        modifier = modifier,
        orientation = Orientation.Vertical,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        mainAxisSpacing = verticalItemSpacing,
        crossAxisSpacing = horizontalArrangement.spacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        slots = rememberColumnSlots(columns, horizontalArrangement, contentPadding),
        content = content
    )
}

/** calculates sizes for columns used in staggered grid measure */
@Composable
private fun rememberColumnSlots(
    columns: StaggeredGridCells,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues
) = remember<LazyGridStaggeredGridSlotsProvider>(
    columns,
    horizontalArrangement,
    contentPadding,
) {
    LazyStaggeredGridSlotCache { constraints ->
        require(constraints.maxWidth != Constraints.Infinity) {
            "LazyVerticalStaggeredGrid's width should be bound by parent."
        }
        val horizontalPadding =
            contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                contentPadding.calculateEndPadding(LayoutDirection.Ltr)
        val gridWidth = constraints.maxWidth - horizontalPadding.roundToPx()
        with(columns) {
            calculateCrossAxisCellSizes(
                gridWidth,
                horizontalArrangement.spacing.roundToPx()
            ).let { sizes ->
                val positions = IntArray(sizes.size)
                with(horizontalArrangement) {
                    // Arrange with Ltr here, as placement will reverse positions if needed
                    arrange(gridWidth, sizes, LayoutDirection.Ltr, positions)
                }
                LazyStaggeredGridSlots(positions, sizes)
            }
        }
    }
}

/**
 * Horizontal staggered grid layout that composes and lays out only items currently
 * visible on screen.
 *
 * Sample:
 * @sample androidx.compose.foundation.samples.LazyHorizontalStaggeredGridSample
 *
 * Sample with custom item spans:
 * @sample androidx.compose.foundation.samples.LazyHorizontalStaggeredGridSpanSample
 *
 * @param rows description of the size and number of staggered grid columns.
 * @param modifier modifier to apply to the layout.
 * @param state state object that can be used to control and observe staggered grid state.
 * @param contentPadding padding around the content.
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are
 * laid out in the reverse order and [LazyStaggeredGridState.firstVisibleItemIndex] == 0 means
 * that grid is scrolled to the end.
 * @param verticalArrangement arrangement specifying vertical spacing between items. The item
 *  arrangement specifics are ignored for now.
 * @param horizontalItemSpacing horizontal spacing between items.
 * @param flingBehavior logic responsible for handling fling.
 * @param userScrollEnabled whether scroll with gestures or accessibility actions are allowed. It is
 *  still possible to scroll programmatically through state when [userScrollEnabled] is set to false
 * @param content a lambda describing the staggered grid content. Inside this block you can use
 *  [LazyStaggeredGridScope.items] to present list of items or [LazyStaggeredGridScope.item] for a
 *  single one.
 */
@Composable
fun LazyHorizontalStaggeredGrid(
    rows: StaggeredGridCells,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    horizontalItemSpacing: Dp = 0.dp,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyStaggeredGridScope.() -> Unit
) {
    LazyStaggeredGrid(
        modifier = modifier,
        orientation = Orientation.Horizontal,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        mainAxisSpacing = horizontalItemSpacing,
        crossAxisSpacing = verticalArrangement.spacing,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        slots = rememberRowSlots(rows, verticalArrangement, contentPadding),
        content = content
    )
}

/** calculates sizes for rows used in staggered grid measure */
@Composable
private fun rememberRowSlots(
    rows: StaggeredGridCells,
    verticalArrangement: Arrangement.Vertical,
    contentPadding: PaddingValues
) = remember<LazyGridStaggeredGridSlotsProvider>(
    rows,
    verticalArrangement,
    contentPadding,
) {
    LazyStaggeredGridSlotCache { constraints ->
        require(constraints.maxHeight != Constraints.Infinity) {
            "LazyHorizontalStaggeredGrid's height should be bound by parent."
        }
        val verticalPadding = contentPadding.calculateTopPadding() +
            contentPadding.calculateBottomPadding()
        val gridHeight = constraints.maxHeight - verticalPadding.roundToPx()
        with(rows) {
            calculateCrossAxisCellSizes(
                gridHeight,
                verticalArrangement.spacing.roundToPx()
            ).let { sizes ->
                val positions = IntArray(sizes.size)
                with(verticalArrangement) {
                    arrange(gridHeight, sizes, positions)
                }
                LazyStaggeredGridSlots(positions, sizes)
            }
        }
    }
}

// Note: Implementing function interface is prohibited in K/JS (class A: () -> Unit)
// therefore we workaround this limitation by inheriting a fun interface instead
internal fun interface LazyGridStaggeredGridSlotsProvider {
    fun invoke(density: Density, constraints: Constraints): LazyStaggeredGridSlots
}

/** measurement cache to avoid recalculating row/column sizes on each scroll. */
private class LazyStaggeredGridSlotCache(
    private val calculation: Density.(Constraints) -> LazyStaggeredGridSlots
) : LazyGridStaggeredGridSlotsProvider {
    private var cachedConstraints = Constraints()
    private var cachedDensity: Float = 0f
    private var cachedSizes: LazyStaggeredGridSlots? = null

    override fun invoke(density: Density, constraints: Constraints): LazyStaggeredGridSlots {
        with(density) {
            if (
                cachedSizes != null &&
                cachedConstraints == constraints &&
                cachedDensity == this.density
            ) {
                return cachedSizes!!
            }

            cachedConstraints = constraints
            cachedDensity = this.density
            return calculation(constraints).also {
                cachedSizes = it
            }
        }
    }
}

/** Dsl marker for [LazyStaggeredGridScope] below */
@DslMarker
internal annotation class LazyStaggeredGridScopeMarker

/**
 * Receiver scope for [LazyVerticalStaggeredGrid] and [LazyHorizontalStaggeredGrid]
 */
@LazyStaggeredGridScopeMarker
sealed interface LazyStaggeredGridScope {

    /**
     * Add a single item to the staggered grid.
     *
     * @param key a stable and unique key representing the item. The key
     *  MUST be saveable via Bundle on Android. If set to null (by default), the position of the
     *  item will be used as a key instead.
     *  Using the same key for multiple items in the staggered grid is not allowed.
     *  This can be overridden by calling [LazyStaggeredGridState.requestScrollToItem].
     *
     *  When you specify the key the scroll position will be maintained based on the key, which
     *  means if you add/remove items before the current visible item the item with the given key
     *  will be kept as the first visible one.
     * @param contentType a content type representing the item. Content for item of
     *  the same type can be reused more efficiently. null is a valid type as well and items
     *  of such type will be considered compatible.
     * @param span a custom span for this item. Spans configure how many lanes defined by
     *  [StaggeredGridCells] the item will occupy. By default each item will take one lane.
     * @param content composable content displayed by current item
     */
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        span: StaggeredGridItemSpan? = null,
        content: @Composable LazyStaggeredGridItemScope.() -> Unit
    )

    /**
     * Add a [count] of items to the staggered grid.
     *
     * @param count number of items to add.
     * @param key a factory of stable and unique keys representing the item. The key
     *  MUST be saveable via Bundle on Android. If set to null (by default), the position of the
     *  item will be used as a key instead.
     *  Using the same key for multiple items in the staggered grid is not allowed.
     *  This can be overridden by calling [LazyStaggeredGridState.requestScrollToItem].
     *
     *  When you specify the key the scroll position will be maintained based on the key, which
     *  means if you add/remove items before the current visible item the item with the given key
     *  will be kept as the first visible one.
     * @param contentType a factory of content types representing the item. Content for item of
     *  the same type can be reused more efficiently. null is a valid type as well and items
     *  of such type will be considered compatible.
     *  @param span a factory of custom spans for this item. Spans configure how many lanes defined
     *  by [StaggeredGridCells] the item will occupy. By default each item will take one lane.
     * @param itemContent composable content displayed by item on provided position
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        span: ((index: Int) -> StaggeredGridItemSpan)? = null,
        itemContent: @Composable LazyStaggeredGridItemScope.(index: Int) -> Unit
    )
}

/**
 * Add a list of items to the staggered grid.
 *
 * @param items a data list to present
 * @param key a factory of stable and unique keys representing the item. The key
 *  MUST be saveable via Bundle on Android. If set to null (by default), the position of the
 *  item will be used as a key instead.
 *  Using the same key for multiple items in the staggered grid is not allowed.
 *  This can be overridden by calling [LazyStaggeredGridState.requestScrollToItem].
 *
 *  When you specify the key the scroll position will be maintained based on the key, which
 *  means if you add/remove items before the current visible item the item with the given key
 *  will be kept as the first visible one.
 * @param contentType a factory of content types representing the item. Content for item of
 *  the same type can be reused more efficiently. null is a valid type as well and items
 *  of such type will be considered compatible.
 * @param span a factory of custom spans for this item. Spans configure how many lanes defined
 *  by [StaggeredGridCells] the item will occupy. By default each item will take one lane.
 * @param itemContent composable content displayed by the provided item
 */
inline fun <T> LazyStaggeredGridScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline contentType: (item: T) -> Any? = { null },
    noinline span: ((item: T) -> StaggeredGridItemSpan)? = null,
    crossinline itemContent: @Composable LazyStaggeredGridItemScope.(item: T) -> Unit
) {
    items(
        count = items.size,
        key = key?.let {
            { index -> key(items[index]) }
        },
        contentType = { index -> contentType(items[index]) },
        span = span?.let {
            { index -> span(items[index]) }
        },
        itemContent = { index -> itemContent(items[index]) }
    )
}

/**
 * Add a list of items with index-aware content to the staggered grid.
 *
 * @param items a data list to present
 * @param key a factory of stable and unique keys representing the item. The key
 *  MUST be saveable via Bundle on Android. If set to null (by default), the position of the
 *  item will be used as a key instead.
 *  Using the same key for multiple items in the staggered grid is not allowed.
 *  This can be overridden by calling [LazyStaggeredGridState.requestScrollToItem].
 *
 *  When you specify the key the scroll position will be maintained based on the key, which
 *  means if you add/remove items before the current visible item the item with the given key
 *  will be kept as the first visible one.
 * @param contentType a factory of content types representing the item. Content for item of
 *  the same type can be reused more efficiently. null is a valid type as well and items
 *  of such type will be considered compatible.
 * @param span a factory of custom spans for this item. Spans configure how many lanes defined
 *  by [StaggeredGridCells] the item will occupy. By default each item will take one lane.
 * @param itemContent composable content displayed given item and index
 */
inline fun <T> LazyStaggeredGridScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    noinline span: ((index: Int, item: T) -> StaggeredGridItemSpan)? = null,
    crossinline itemContent: @Composable LazyStaggeredGridItemScope.(index: Int, item: T) -> Unit
) {
    items(
        count = items.size,
        key = key?.let {
            { index -> key(index, items[index]) }
        },
        contentType = { index -> contentType(index, items[index]) },
        span = span?.let {
            { index -> span(index, items[index]) }
        },
        itemContent = { index -> itemContent(index, items[index]) }
    )
}

/**
 * Add an array of items to the staggered grid.
 *
 * @param items a data array to present
 * @param key a factory of stable and unique keys representing the item. The key
 *  MUST be saveable via Bundle on Android. If set to null (by default), the position of the
 *  item will be used as a key instead.
 *  Using the same key for multiple items in the staggered grid is not allowed.
 *  This can be overridden by calling [LazyStaggeredGridState.requestScrollToItem].
 *
 *  When you specify the key the scroll position will be maintained based on the key, which
 *  means if you add/remove items before the current visible item the item with the given key
 *  will be kept as the first visible one.
 * @param contentType a factory of content types representing the item. Content for item of
 *  the same type can be reused more efficiently. null is a valid type as well and items
 *  of such type will be considered compatible.
 * @param span a factory of custom spans for this item. Spans configure how many lanes defined
 *  by [StaggeredGridCells] the item will occupy. By default each item will take one lane.
 * @param itemContent composable content displayed by the provided item
 */
inline fun <T> LazyStaggeredGridScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline contentType: (item: T) -> Any? = { null },
    noinline span: ((item: T) -> StaggeredGridItemSpan)? = null,
    crossinline itemContent: @Composable LazyStaggeredGridItemScope.(item: T) -> Unit
) {
    items(
        count = items.size,
        key = key?.let {
            { index -> key(items[index]) }
        },
        contentType = { index -> contentType(items[index]) },
        span = span?.let {
            { index -> span(items[index]) }
        },
        itemContent = { index -> itemContent(items[index]) }
    )
}

/**
 * Add an array of items with index-aware content to the staggered grid.
 *
 * @param items a data array to present
 * @param key a factory of stable and unique keys representing the item. The key
 *  MUST be saveable via Bundle on Android. If set to null (by default), the position of the
 *  item will be used as a key instead.
 *  Using the same key for multiple items in the staggered grid is not allowed.
 *  This can be overridden by calling [LazyStaggeredGridState.requestScrollToItem].
 *
 *  When you specify the key the scroll position will be maintained based on the key, which
 *  means if you add/remove items before the current visible item the item with the given key
 *  will be kept as the first visible one.
 * @param contentType a factory of content types representing the item. Content for item of
 *  the same type can be reused more efficiently. null is a valid type as well and items
 *  of such type will be considered compatible.
 * @param span a factory of custom spans for this item. Spans configure how many lanes defined
 *  by [StaggeredGridCells] the item will occupy. By default each item will take one lane.
 * @param itemContent composable content displayed given item and index
 */
inline fun <T> LazyStaggeredGridScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    noinline span: ((index: Int, item: T) -> StaggeredGridItemSpan)? = null,
    crossinline itemContent: @Composable LazyStaggeredGridItemScope.(index: Int, item: T) -> Unit
) {
    items(
        count = items.size,
        key = key?.let {
            { index -> key(index, items[index]) }
        },
        contentType = { index -> contentType(index, items[index]) },
        span = span?.let {
            { index -> span(index, items[index]) }
        },
        itemContent = { index -> itemContent(index, items[index]) }
    )
}
