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

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets

/**
 * A lazy vertical grid layout. It composes only visible rows of the grid.
 *
 * @param columns describes the count and the size of the grid's columns,
 * see [TvGridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items will be
 * laid out in the reverse order  and [TvLazyGridState.firstVisibleItemIndex] == 0 means
 * that grid is scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 * [verticalArrangement],
 * e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321### (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param pivotOffsets offsets that are used when implementing Scrolling with Offset
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param pivotOffsets offsets of child element within the parent and starting edge of the child
 * from the pivot defined by the parentOffset.
 * @param content the [TvLazyGridScope] which describes the content
 */
@Composable
fun TvLazyVerticalGrid(
    columns: TvGridCells,
    modifier: Modifier = Modifier,
    state: TvLazyGridState = rememberTvLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    userScrollEnabled: Boolean = true,
    pivotOffsets: PivotOffsets = PivotOffsets(),
    content: TvLazyGridScope.() -> Unit
) {
    LazyGrid(
        slots = rememberColumnWidthSums(columns, horizontalArrangement, contentPadding),
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        isVertical = true,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        userScrollEnabled = userScrollEnabled,
        content = content,
        pivotOffsets = pivotOffsets
    )
}

/**
 * A lazy horizontal grid layout. It composes only visible columns of the grid.
 *
 * @param rows a class describing how cells form rows, see [TvGridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout, when `true` items will be
 * composed from the end to the start and [TvLazyGridState.firstVisibleItemIndex] == 0 will mean
 * the first item is located at the end.
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param pivotOffsets offsets that are used when implementing Scrolling with Offset
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param pivotOffsets offsets of child element within the parent and starting edge of the child
 * from the pivot defined by the parentOffset.
 * @param content the [TvLazyGridScope] which describes the content
 */
@Composable
fun TvLazyHorizontalGrid(
    rows: TvGridCells,
    modifier: Modifier = Modifier,
    state: TvLazyGridState = rememberTvLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    userScrollEnabled: Boolean = true,
    pivotOffsets: PivotOffsets = PivotOffsets(),
    content: TvLazyGridScope.() -> Unit
) {
    LazyGrid(
        slots = rememberRowHeightSums(rows, verticalArrangement, contentPadding),
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        isVertical = false,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        userScrollEnabled = userScrollEnabled,
        pivotOffsets = pivotOffsets,
        content = content
    )
}

/** Returns prefix sums of column widths. */
@Composable
private fun rememberColumnWidthSums(
    columns: TvGridCells,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues
) = remember<Density.(Constraints) -> LazyGridSlots>(
    columns,
    horizontalArrangement,
    contentPadding,
) {
    GridSlotCache { constraints ->
        require(constraints.maxWidth != Constraints.Infinity) {
            "LazyVerticalGrid's width should be bound by parent."
        }
        val horizontalPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
            contentPadding.calculateEndPadding(LayoutDirection.Ltr)
        val gridWidth = constraints.maxWidth - horizontalPadding.roundToPx()
        with(columns) {
            calculateCrossAxisCellSizes(
                gridWidth,
                horizontalArrangement.spacing.roundToPx()
            ).toIntArray().let { sizes ->
                val positions = IntArray(sizes.size)
                with(horizontalArrangement) {
                    arrange(gridWidth, sizes, LayoutDirection.Ltr, positions)
                }
                LazyGridSlots(sizes, positions)
            }
        }
    }
}

/** Returns prefix sums of row heights. */
@Composable
private fun rememberRowHeightSums(
    rows: TvGridCells,
    verticalArrangement: Arrangement.Vertical,
    contentPadding: PaddingValues
) = remember<Density.(Constraints) -> LazyGridSlots>(
    rows,
    verticalArrangement,
    contentPadding,
) {
    GridSlotCache { constraints ->
        require(constraints.maxHeight != Constraints.Infinity) {
            "LazyHorizontalGrid's height should be bound by parent."
        }
        val verticalPadding = contentPadding.calculateTopPadding() +
            contentPadding.calculateBottomPadding()
        val gridHeight = constraints.maxHeight - verticalPadding.roundToPx()
        with(rows) {
            calculateCrossAxisCellSizes(
                gridHeight,
                verticalArrangement.spacing.roundToPx()
            ).toIntArray().let { sizes ->
                val positions = IntArray(sizes.size)
                with(verticalArrangement) {
                    arrange(gridHeight, sizes, positions)
                }
                LazyGridSlots(sizes, positions)
            }
        }
    }
}

/** measurement cache to avoid recalculating row/column sizes on each scroll. */
private class GridSlotCache(
    private val calculation: Density.(Constraints) -> LazyGridSlots
) : (Density, Constraints) -> LazyGridSlots {
    private var cachedConstraints = Constraints()
    private var cachedDensity: Float = 0f
    private var cachedSizes: LazyGridSlots? = null

    override fun invoke(density: Density, constraints: Constraints): LazyGridSlots {
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

/**
 * This class describes the count and the sizes of columns in vertical grids,
 * or rows in horizontal grids.
 */
@Stable
interface TvGridCells {
    /**
     * Calculates the number of cells and their cross axis size based on
     * [availableSize] and [spacing].
     *
     * For example, in vertical grids, [spacing] is passed from the grid's [Arrangement.Horizontal].
     * The [Arrangement.Horizontal] will also be used to arrange items in a row if the grid is wider
     * than the calculated sum of columns.
     *
     * Note that the calculated cross axis sizes will be considered in an RTL-aware manner --
     * if the grid is vertical and the layout direction is RTL, the first width in the returned
     * list will correspond to the rightmost column.
     *
     * @param availableSize available size on cross axis, e.g. width of [TvLazyVerticalGrid].
     * @param spacing cross axis spacing, e.g. horizontal spacing for [TvLazyVerticalGrid].
     * The spacing is passed from the corresponding [Arrangement] param of the lazy grid.
     */
    fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int>

    /**
     * Defines a grid with fixed number of rows or columns.
     *
     * For example, for the vertical [TvLazyVerticalGrid] Fixed(3) would mean that
     * there are 3 columns 1/3 of the parent width.
     */
    class Fixed(private val count: Int) : TvGridCells {
        init {
            require(count > 0) { "grid with no rows/columns" }
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return -count // Different sign from Adaptive.
        }

        override fun equals(other: Any?): Boolean {
            return other is Fixed && count == other.count
        }
    }

    /**
     * Defines a grid with as many rows or columns as possible on the condition that
     * every cell has at least [minSize] space and all extra space distributed evenly.
     *
     * For example, for the vertical [TvLazyVerticalGrid] Adaptive(20.dp) would mean that
     * there will be as many columns as possible and every column will be at least 20.dp
     * and all the columns will have equal width. If the screen is 88.dp wide then
     * there will be 4 columns 22.dp each.
     */
    class Adaptive(private val minSize: Dp) : TvGridCells {
        init {
            require(minSize > 0.dp) { "Grid requires a positive minSize" }
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1)
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return minSize.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is Adaptive && minSize == other.minSize
        }
    }

    /**
     * Defines a grid with as many rows or columns as possible on the condition that
     * every cell takes exactly [size] space. The remaining space will be arranged through
     * [TvLazyVerticalGrid] arrangements on corresponding axis. If [size] is larger than
     * container size, the cell will be size to match the container.
     *
     * For example, for the vertical [TvLazyVerticalGrid] FixedSize(20.dp) would mean that
     * there will be as many columns as possible and every column will be exactly 20.dp.
     * If the screen is 88.dp wide tne there will be 4 columns 20.dp each with remaining 8.dp
     * distributed through [Arrangement.Horizontal].
     */
    class FixedSize(private val size: Dp) : TvGridCells {
        init {
            require(size > 0.dp) { "Provided size $size should be larger than zero." }
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            val cellSize = size.roundToPx()
            return if (cellSize + spacing < availableSize + spacing) {
                val cellCount = (availableSize + spacing) / (cellSize + spacing)
                List(cellCount) { cellSize }
            } else {
                List(1) { availableSize }
            }
        }

        override fun hashCode(): Int {
            return size.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is FixedSize && size == other.size
        }
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) {
        slotSize + if (it < remainingPixels) 1 else 0
    }
}

/**
 * Receiver scope which is used by [TvLazyVerticalGrid].
 */
@TvLazyGridScopeMarker
sealed interface TvLazyGridScope {
    /**
     * Adds a single item to the scope.
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the grid is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the grid will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param span the span of the item. Default is 1x1. It is good practice to leave it `null`
     * when this matches the intended behavior, as providing a custom implementation impacts
     * performance
     * @param contentType the type of the content of this item. The item compositions of the same
     * type could be reused more efficiently. Note that null is a valid type and items of such
     * type will be considered compatible.
     * @param content the content of the item
     */
    fun item(
        key: Any? = null,
        span: (TvLazyGridItemSpanScope.() -> TvGridItemSpan)? = null,
        contentType: Any? = null,
        content: @Composable TvLazyGridItemScope.() -> Unit
    )

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key
     * for multiple items in the grid is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the grid will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param span define custom spans for the items. Default is 1x1. It is good practice to
     * leave it `null` when this matches the intended behavior, as providing a custom
     * implementation impacts performance
     * @param contentType a factory of the content types for the item. The item compositions of
     * the same type could be reused more efficiently. Note that null is a valid type and items
     * of such type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        span: (TvLazyGridItemSpanScope.(index: Int) -> TvGridItemSpan)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable TvLazyGridItemScope.(index: Int) -> Unit
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to
 * leave it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyGridScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (TvLazyGridItemSpanScope.(item: T) -> TvGridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable TvLazyGridItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    span = if (span != null) { { span(items[it]) } } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyGridScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (TvLazyGridItemSpanScope.(index: Int, item: T) -> TvGridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable TvLazyGridItemScope.(index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    span = if (span != null) { { span(it, items[it]) } } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyGridScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (TvLazyGridItemSpanScope.(item: T) -> TvGridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable TvLazyGridItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    span = if (span != null) { { span(items[it]) } } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyGridScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (TvLazyGridItemSpanScope.(index: Int, item: T) -> TvGridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable TvLazyGridItemScope.(index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    span = if (span != null) { { span(it, items[it]) } } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}
