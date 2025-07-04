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
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal fun rememberGlimmerListItemProviderLambda(
    state: ListState,
    content: ListScope.() -> Unit,
): () -> GlimmerListItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val scope = GlimmerListItemScopeImpl()
        val intervalContentState =
            derivedStateOf(referentialEqualityPolicy()) { IntervalContent(latestContent.value) }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value
                val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                GlimmerListItemProvider(
                    state = state,
                    intervalContent = intervalContent,
                    itemScope = scope,
                    keyIndexMap = map,
                )
            }
        itemProviderState::value
    }
}

internal class GlimmerListItemProvider(
    val itemScope: GlimmerListItemScopeImpl,
    val intervalContent: LazyLayoutIntervalContent<GlimmerListInterval>,
    val keyIndexMap: NearestRangeKeyIndexMap,
    private val state: ListState,
) : LazyLayoutItemProvider {
    override val itemCount: Int
        get() = intervalContent.itemCount

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

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlimmerListItemProvider) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
