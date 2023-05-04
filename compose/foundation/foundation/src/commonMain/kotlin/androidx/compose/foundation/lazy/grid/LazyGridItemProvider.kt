/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMapState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@ExperimentalFoundationApi
internal interface LazyGridItemProvider : LazyLayoutItemProvider {
    val keyToIndexMap: LazyLayoutKeyIndexMap
    val spanLayoutProvider: LazyGridSpanLayoutProvider
}

@ExperimentalFoundationApi
@Composable
internal fun rememberLazyGridItemProvider(
    state: LazyGridState,
    content: LazyGridScope.() -> Unit,
): LazyGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        LazyGridItemProviderImpl(
            state,
            { latestContent.value },
        )
    }
}

@ExperimentalFoundationApi
private class LazyGridItemProviderImpl(
    private val state: LazyGridState,
    private val latestContent: () -> (LazyGridScope.() -> Unit)
) : LazyGridItemProvider {
    private val gridContent by derivedStateOf(referentialEqualityPolicy()) {
        LazyGridIntervalContent(latestContent())
    }

    override val keyToIndexMap: LazyLayoutKeyIndexMap by NearestRangeKeyIndexMapState(
        firstVisibleItemIndex = { state.firstVisibleItemIndex },
        slidingWindowSize = { NearestItemsSlidingWindowSize },
        extraItemCount = { NearestItemsExtraItemCount },
        content = { gridContent }
    )

    override val itemCount: Int get() = gridContent.itemCount

    override fun getKey(index: Int): Any = gridContent.getKey(index)

    override fun getContentType(index: Int): Any? = gridContent.getContentType(index)

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            gridContent.withInterval(index) { localIndex, content ->
                content.item(LazyGridItemScopeImpl, localIndex)
            }
        }
    }

    override val spanLayoutProvider: LazyGridSpanLayoutProvider
        get() = gridContent.spanLayoutProvider

    override fun getIndex(key: Any): Int = keyToIndexMap[key]
}

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
private const val NearestItemsSlidingWindowSize = 90

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private const val NearestItemsExtraItemCount = 200