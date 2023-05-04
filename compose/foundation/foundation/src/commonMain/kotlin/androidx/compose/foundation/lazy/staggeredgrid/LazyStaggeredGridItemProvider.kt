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

@OptIn(ExperimentalFoundationApi::class)
internal interface LazyStaggeredGridItemProvider : LazyLayoutItemProvider {
    val spanProvider: LazyStaggeredGridSpanProvider
    val keyToIndexMap: LazyLayoutKeyIndexMap
}

@Composable
internal fun rememberStaggeredGridItemProvider(
    state: LazyStaggeredGridState,
    content: LazyStaggeredGridScope.() -> Unit,
): LazyStaggeredGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        LazyStaggeredGridItemProviderImpl(
            state,
            { latestContent.value },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridItemProviderImpl(
    private val state: LazyStaggeredGridState,
    private val latestContent: () -> (LazyStaggeredGridScope.() -> Unit)
) : LazyStaggeredGridItemProvider {
    private val staggeredGridContent by derivedStateOf(referentialEqualityPolicy()) {
        LazyStaggeredGridIntervalContent(latestContent())
    }

    override val keyToIndexMap: LazyLayoutKeyIndexMap by NearestRangeKeyIndexMapState(
        firstVisibleItemIndex = { state.firstVisibleItemIndex },
        slidingWindowSize = { 90 },
        extraItemCount = { 200 },
        content = { staggeredGridContent }
    )

    override val itemCount: Int get() = staggeredGridContent.itemCount

    override fun getKey(index: Int): Any = staggeredGridContent.getKey(index)

    override fun getIndex(key: Any): Int = keyToIndexMap[key]

    override fun getContentType(index: Int): Any? = staggeredGridContent.getContentType(index)

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            staggeredGridContent.withInterval(index) { localIndex, content ->
                content.item(LazyStaggeredGridItemScopeImpl, localIndex)
            }
        }
    }

    override val spanProvider get() = staggeredGridContent.spanProvider
}