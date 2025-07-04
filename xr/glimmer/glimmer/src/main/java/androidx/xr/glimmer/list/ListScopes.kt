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
 * Receiver scope being used by the item content parameter of [VerticalList]. It helps define
 * scoped-based extensions that only apply to [VerticalList] items.
 */
@Stable public sealed interface ListItemScope

/** Receiver scope which is used by [VerticalList]. */
public sealed interface ListScope {
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
        content: @Composable ListItemScope.() -> Unit,
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
        itemContent: @Composable ListItemScope.(index: Int) -> Unit,
    )
}

internal class IntervalContent(content: ListScope.() -> Unit) :
    LazyLayoutIntervalContent<GlimmerListInterval>(), ListScope {
    override val intervals: MutableIntervalList<GlimmerListInterval> = MutableIntervalList()

    init {
        apply(content)
    }

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable (ListItemScope.() -> Unit),
    ) {
        intervals.addInterval(
            1,
            GlimmerListInterval(
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
        itemContent: @Composable (ListItemScope.(Int) -> Unit),
    ) {
        intervals.addInterval(
            count,
            GlimmerListInterval(key = key, type = contentType, item = itemContent),
        )
    }
}

internal class GlimmerListItemScopeImpl : ListItemScope {

    private var maxWidthState = mutableIntStateOf(Int.MAX_VALUE)
    private var maxHeightState = mutableIntStateOf(Int.MAX_VALUE)

    fun setMaxSize(width: Int, height: Int) {
        maxWidthState.intValue = width
        maxHeightState.intValue = height
    }
}

internal class GlimmerListInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable ListItemScope.(index: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval
