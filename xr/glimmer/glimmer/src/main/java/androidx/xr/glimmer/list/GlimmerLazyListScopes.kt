/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf

/**
 * Receiver scope being used by the item content parameter of [GlimmerLazyColumn]. It helps define
 * scoped-based extensions that only apply to [GlimmerLazyColumn] items.
 */
@Stable public sealed interface GlimmerLazyListItemScope

/** Receiver scope which is used by [GlimmerLazyColumn]. */
@GlimmerLazyListScopeMarker
public sealed interface GlimmerLazyListScope {
    /**
     * Adds a single item.
     *
     * @param key a stable and unique key representing the item. Using the same key for multiple
     *   items in the list is not allowed. Type of the key should be saveable via Bundle on Android.
     *   If null is passed the position in the list will represent the key. When you specify the key
     *   the scroll position will be maintained based on the key, which means if you add/remove
     *   items before the current visible item the item with the given key will be kept as the first
     *   visible one.
     * @param contentType the type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content the content of the item
     */
    public fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable GlimmerLazyListItemScope.() -> Unit,
    )

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key for
     *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
     *   Android. If null is passed the position in the list will represent the key. When you
     *   specify the key the scroll position will be maintained based on the key, which means if you
     *   add/remove items before the current visible item the item with the given key will be kept
     *   as the first visible one.
     * @param contentType a factory of the content types for the item. The item compositions of the
     *   same type could be reused more efficiently. Note that null is a valid type and items of
     *   such type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    public fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable GlimmerLazyListItemScope.(index: Int) -> Unit,
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the list will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling 'requestScrollToItem' on the
 *   'GlimmerLazyListState'.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
public inline fun <T> GlimmerLazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable GlimmerLazyListItemScope.(item: T) -> Unit,
) {
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        contentType = { index: Int -> contentType(items[index]) },
    ) {
        itemContent(items[it])
    }
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the list will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
public inline fun <T> GlimmerLazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable GlimmerLazyListItemScope.(index: Int, item: T) -> Unit,
) {
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        contentType = { index -> contentType(index, items[index]) },
    ) {
        itemContent(it, items[it])
    }
}

internal class IntervalContent(content: GlimmerLazyListScope.() -> Unit) :
    LazyLayoutIntervalContent<GlimmerLazyListInterval>(), GlimmerLazyListScope {
    override val intervals: MutableIntervalList<GlimmerLazyListInterval> = MutableIntervalList()

    init {
        apply(content)
    }

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable (GlimmerLazyListItemScope.() -> Unit),
    ) {
        intervals.addInterval(
            1,
            GlimmerLazyListInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() },
            ),
        )
    }

    override fun items(
        count: Int,
        key: ((Int) -> Any)?,
        contentType: (Int) -> Any?,
        itemContent: @Composable (GlimmerLazyListItemScope.(Int) -> Unit),
    ) {
        intervals.addInterval(
            count,
            GlimmerLazyListInterval(key = key, type = contentType, item = itemContent),
        )
    }
}

internal class GlimmerLazyListItemScopeImpl : GlimmerLazyListItemScope {

    private var maxWidthState = mutableIntStateOf(Int.MAX_VALUE)
    private var maxHeightState = mutableIntStateOf(Int.MAX_VALUE)

    fun setMaxSize(width: Int, height: Int) {
        maxWidthState.intValue = width
        maxHeightState.intValue = height
    }
}

internal class GlimmerLazyListInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable GlimmerLazyListItemScope.(index: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval
