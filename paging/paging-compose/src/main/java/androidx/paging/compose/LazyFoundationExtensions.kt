/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.paging.compose

import androidx.paging.PagingConfig
import androidx.compose.foundation.lazy.grid.LazyGridScope

/**
 * Returns a factory of stable and unique keys representing the item.
 *
 * Keys are generated with the key lambda that is passed in. If null is passed in, keys will
 * default to index-based [PagingPlaceholderKey]. If [PagingConfig.enablePlaceholders] is true,
 * LazyPagingItems may return null items. Null items will also automatically default to
 * [PagingPlaceholderKey].
 *
 * This factory can be applied to Lazy foundations such as [LazyGridScope.items] or Pagers.
 * Examples:
 * @sample androidx.paging.compose.samples.PagingWithHorizontalPager
 * @sample androidx.paging.compose.samples.PagingWithLazyGrid
 *
 * @param [key] a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. When you specify the key the scroll position will be maintained
 * based on the key, which means if you add/remove items before the current visible item the
 * item with the given key will be kept as the first visible one.
 */
public fun <T : Any> LazyPagingItems<T>.itemKey(
    key: ((item: @JvmSuppressWildcards T) -> Any)? = null
): (index: Int) -> Any {
    return { index ->
        val item = peek(index)
        if (key == null || item == null) {
            PagingPlaceholderKey(index)
        } else {
            key(item)
        }
    }
}
