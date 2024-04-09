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

package androidx.tv.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.tv.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.tv.foundation.lazy.layout.NearestRangeKeyIndexMap

@ExperimentalFoundationApi
internal interface LazyListItemProvider : LazyLayoutItemProvider {
    val keyIndexMap: LazyLayoutKeyIndexMap
    /** The list of indexes of the sticky header items */
    val headerIndexes: List<Int>
    /** The scope used by the item content lambdas */
    val itemScope: TvLazyListItemScopeImpl
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberLazyListItemProviderLambda(
    state: TvLazyListState,
    content: TvLazyListScope.() -> Unit
): () -> LazyListItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val scope = TvLazyListItemScopeImpl()
        val intervalContentState = derivedStateOf(referentialEqualityPolicy()) {
            TvLazyListIntervalContent(latestContent.value)
        }
        val itemProviderState = derivedStateOf(referentialEqualityPolicy()) {
            val intervalContent = intervalContentState.value
            val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
            LazyListItemProviderImpl(
                state = state,
                intervalContent = intervalContent,
                itemScope = scope,
                keyIndexMap = map
            )
        }
        itemProviderState::value
    }
}

@ExperimentalFoundationApi
private class LazyListItemProviderImpl constructor(
    private val state: TvLazyListState,
    private val intervalContent: TvLazyListIntervalContent,
    override val itemScope: TvLazyListItemScopeImpl,
    override val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyListItemProvider {

    override val itemCount: Int get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(itemScope, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override val headerIndexes: List<Int> get() = intervalContent.headerIndexes

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyListItemProviderImpl) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
