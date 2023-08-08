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
import androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@OptIn(ExperimentalFoundationApi::class)
internal interface LazyStaggeredGridItemProvider : LazyLayoutItemProvider {
    val spanProvider: LazyStaggeredGridSpanProvider
    val keyIndexMap: LazyLayoutKeyIndexMap
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberStaggeredGridItemProviderLambda(
    state: LazyStaggeredGridState,
    content: LazyStaggeredGridScope.() -> Unit,
): () -> LazyStaggeredGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val intervalContentState = derivedStateOf(referentialEqualityPolicy()) {
            LazyStaggeredGridIntervalContent(latestContent.value)
        }
        val itemProviderState = derivedStateOf(referentialEqualityPolicy()) {
            val intervalContent = intervalContentState.value
            val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
            LazyStaggeredGridItemProviderImpl(
                state = state,
                intervalContent = intervalContent,
                keyIndexMap = map
            )
        }
        itemProviderState::value
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridItemProviderImpl(
    private val state: LazyStaggeredGridState,
    private val intervalContent: LazyStaggeredGridIntervalContent,
    override val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyStaggeredGridItemProvider {

    override val itemCount: Int get() = intervalContent.itemCount

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(LazyStaggeredGridItemScopeImpl, localIndex)
            }
        }
    }

    override val spanProvider get() = intervalContent.spanProvider

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyStaggeredGridItemProviderImpl) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
