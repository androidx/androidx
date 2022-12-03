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

package androidx.tv.foundation.lazy.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets

/* Copied from
 compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyDsl.kt
  and modified */

/**
 * Receiver scope which is used by [TvLazyColumn] and [TvLazyRow].
 */
@TvLazyListScopeMarker
sealed interface TvLazyListScope {
    /**
     * Adds a single item.
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param contentType the type of the content of this item. The item compositions of the same
     * type could be reused more efficiently. Note that null is a valid type and items of such
     * type will be considered compatible.
     * @param content the content of the item
     */
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable TvLazyListItemScope.() -> Unit
    )

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param contentType a factory of the content types for the item. The item compositions of
     * the same type could be reused more efficiently. Note that null is a valid type and items of such
     * type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable TvLazyListItemScope.(index: Int) -> Unit
    )

    /**
     * Adds a sticky header item, which will remain pinned even when scrolling after it.
     * The header will remain pinned until the next header will take its place.
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param contentType the type of the content of this item. The item compositions of the same
     * type could be reused more efficiently. Note that null is a valid type and items of such
     * type will be considered compatible.
     * @param content the content of the header
     */
    @ExperimentalTvFoundationApi
    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable TvLazyListItemScope.() -> Unit
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable TvLazyListItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable TvLazyListItemScope.(index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyListScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable TvLazyListItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> TvLazyListScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable TvLazyListItemScope.(index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}

/**
 * The horizontally scrolling list that only composes and lays out the currently visible items.
 * The [content] block defines a DSL which allows you to emit items of different types. For
 * example you can use [TvLazyListScope.item] to add a single item and [TvLazyListScope.items] to add
 * a list of items.
 *
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first item or after the last one. If you want to add a spacing
 * between each item use [horizontalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are
 * laid out in the reverse order and [TvLazyListState.firstVisibleItemIndex] == 0 means
 * that row is scrolled to the end. Note that [reverseLayout] does not change the behavior of
 * [horizontalArrangement], e.g. with [Arrangement.Start] [123###] becomes [321###].
 * @param horizontalArrangement The horizontal arrangement of the layout's children. This allows
 * to add a spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param verticalAlignment the vertical alignment applied to the items
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param pivotOffsets offsets of child element within the parent and starting edge of the child
 * from the pivot defined by the parentOffset.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [TvLazyListScope.item] to add a single item or [TvLazyListScope.items] to add a list of items.
 */
@Composable
fun TvLazyRow(
    modifier: Modifier = Modifier,
    state: TvLazyListState = rememberTvLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    userScrollEnabled: Boolean = true,
    pivotOffsets: PivotOffsets = PivotOffsets(),
    content: TvLazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        isVertical = false,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        content = content,
        pivotOffsets = pivotOffsets
    )
}

/**
 * The vertically scrolling list that only composes and lays out the currently visible items.
 * The [content] block defines a DSL which allows you to emit items of different types. For
 * example you can use [TvLazyListScope.item] to add a single item and [TvLazyListScope.items] to add
 * a list of items.
 *
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content. This will add padding for the.
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first item or after the last one. If you want to add a spacing
 * between each item use [verticalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are
 * laid out in the reverse order and [TvLazyListState.firstVisibleItemIndex] == 0 means
 * that column is scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 * [verticalArrangement],
 * e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321### (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows
 * to add a spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param content a block which describes the content. Inside this block you can use methods like
 * @param pivotOffsets offsets of child element within the parent and starting edge of the child
 * from the pivot defined by the parentOffset.
 * [TvLazyListScope.item] to add a single item or [TvLazyListScope.items] to add a list of items.
 */
@Composable
fun TvLazyColumn(
    modifier: Modifier = Modifier,
    state: TvLazyListState = rememberTvLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    pivotOffsets: PivotOffsets = PivotOffsets(),
    content: TvLazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = true,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        content = content,
        pivotOffsets = pivotOffsets
    )
}
