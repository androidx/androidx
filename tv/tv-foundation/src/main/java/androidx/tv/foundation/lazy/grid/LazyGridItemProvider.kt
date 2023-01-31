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

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.DelegatingLazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.rememberLazyNearestItemsRangeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@ExperimentalFoundationApi
internal interface LazyGridItemProvider : LazyLayoutItemProvider {
    val spanLayoutProvider: LazyGridSpanLayoutProvider
    val hasCustomSpans: Boolean

    fun TvLazyGridItemSpanScope.getSpan(index: Int): TvGridItemSpan
}

@ExperimentalFoundationApi
@Composable
internal fun rememberLazyGridItemProvider(
    state: TvLazyGridState,
    content: TvLazyGridScope.() -> Unit,
): LazyGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    val nearestItemsRangeState = rememberLazyNearestItemsRangeState(
        firstVisibleItemIndex = remember(state) {
            { state.firstVisibleItemIndex }
        },
        slidingWindowSize = { NearestItemsSlidingWindowSize },
        extraItemCount = { NearestItemsExtraItemCount }
    )

    return remember(nearestItemsRangeState) {
        val itemProviderState: State<LazyGridItemProvider> = derivedStateOf {
            val gridScope = TvLazyGridScopeImpl().apply(latestContent.value)
            LazyGridItemProviderImpl(
                gridScope.intervals,
                gridScope.hasCustomSpans,
                nearestItemsRangeState.value
            )
        }

        object : LazyGridItemProvider,
            LazyLayoutItemProvider by DelegatingLazyLayoutItemProvider(itemProviderState) {
            override val spanLayoutProvider: LazyGridSpanLayoutProvider
                get() = itemProviderState.value.spanLayoutProvider

            override val hasCustomSpans: Boolean
                get() = itemProviderState.value.hasCustomSpans

            override fun TvLazyGridItemSpanScope.getSpan(index: Int): TvGridItemSpan =
                with(itemProviderState.value) {
                    getSpan(index)
                }
        }
    }
}

@ExperimentalFoundationApi
private class LazyGridItemProviderImpl(
    private val intervals: IntervalList<LazyGridIntervalContent>,
    override val hasCustomSpans: Boolean,
    nearestItemsRange: IntRange
) : LazyGridItemProvider, LazyLayoutItemProvider by LazyLayoutItemProvider(
    intervals = intervals,
    nearestItemsRange = nearestItemsRange,
    itemContent = { interval, index ->
        interval.value.item.invoke(TvLazyGridItemScopeImpl, index - interval.startIndex)
    }
) {
    override val spanLayoutProvider: LazyGridSpanLayoutProvider =
        LazyGridSpanLayoutProvider(this)

    override fun TvLazyGridItemSpanScope.getSpan(index: Int): TvGridItemSpan {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return interval.value.span.invoke(this, localIntervalIndex)
    }
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