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

import androidx.collection.IntList
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Suppress("PrimitiveInCollection")
@OptIn(ExperimentalFoundationApi::class)
internal interface LazyGridItemProvider : LazyLayoutItemProvider {
    val keyIndexMap: LazyLayoutKeyIndexMap
    val spanLayoutProvider: LazyGridSpanLayoutProvider
    val headerIndexes: IntList
}

@Composable
internal fun rememberLazyGridItemProviderLambda(
    state: LazyGridState,
    content: LazyGridScope.() -> Unit,
): () -> LazyGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val intervalContentState =
            derivedStateOf(referentialEqualityPolicy()) {
                LazyGridIntervalContent(latestContent.value)
            }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value
                val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                LazyGridItemProviderImpl(
                    state = state,
                    intervalContent = intervalContent,
                    keyIndexMap = map
                )
            }
        itemProviderState::value
    }
}

private class LazyGridItemProviderImpl(
    private val state: LazyGridState,
    private val intervalContent: LazyGridIntervalContent,
    override val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyGridItemProvider {

    override val itemCount: Int
        get() = intervalContent.itemCount

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override val headerIndexes: IntList
        get() = intervalContent.headerIndexes

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(LazyGridItemScopeImpl, localIndex)
            }
        }
    }

    override val spanLayoutProvider: LazyGridSpanLayoutProvider
        get() = intervalContent.spanLayoutProvider

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyGridItemProviderImpl) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
