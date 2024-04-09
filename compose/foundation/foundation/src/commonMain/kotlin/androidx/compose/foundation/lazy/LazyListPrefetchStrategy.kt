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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.PrefetchExecutor
import androidx.compose.runtime.Stable

/**
 * Implementations of this interface control which indices of a LazyList should be prefetched
 * (precomposed and premeasured during idle time) as the user interacts with it.
 *
 * Implementations should invoke [LazyListPrefetchScope.schedulePrefetch] to schedule prefetches
 * from the [onScroll] and [onVisibleItemsUpdated] callbacks. If any of the returned PrefetchHandles
 * no longer need to be prefetched, use [LazyLayoutPrefetchState.PrefetchHandle.cancel] to cancel
 * the request.
 */
@ExperimentalFoundationApi
interface LazyListPrefetchStrategy {

    /**
     * A PrefetchExecutor implementation which will be used to execute prefetch requests for this
     * strategy implementation. If null, the default PrefetchExecutor for the platform will be used.
     */
    val prefetchExecutor: PrefetchExecutor?
        get() = null

    /**
     * onScroll is invoked when the LazyList scrolls, whether or not the visible items have changed.
     * If the visible items have also changed, then this will be invoked in the same frame *after*
     * [onVisibleItemsUpdated].
     *
     * [delta] can be used to understand scroll direction: delta < 0 indicates scrolling down while
     * delta > 0 indicates scrolling up.
     */
    fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo)

    /**
     * onVisibleItemsUpdated is invoked when the LazyList scrolls if the visible items have changed.
     * Info about these visible items can be found in [layoutInfo]'s
     * [LazyListLayoutInfo.visibleItemsInfo].
     */
    fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo)
}

/**
 * Scope for callbacks in [LazyListPrefetchStrategy] which allows prefetches to be requested.
 */
@ExperimentalFoundationApi
interface LazyListPrefetchScope {

    /**
     * Schedules a prefetch for the given index. Requests are executed in the order they're
     * requested. If a requested prefetch is no longer necessary (for example, due to changing
     * scroll direction), the request should be canceled via
     * [LazyLayoutPrefetchState.PrefetchHandle.cancel].
     *
     * See [PrefetchExecutor].
     */
    fun schedulePrefetch(index: Int): LazyLayoutPrefetchState.PrefetchHandle
}

/**
 * The default prefetching strategy for LazyLists - this will be used automatically if no other
 * strategy is provided.
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
internal class DefaultLazyListPrefetchStrategy : LazyListPrefetchStrategy {

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var indexToPrefetch = -1

    /**
     * The handle associated with the current index from [indexToPrefetch].
     */
    private var currentPrefetchHandle: LazyLayoutPrefetchState.PrefetchHandle? = null

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to
     * detect the scrolling direction change.
     */
    private var wasScrollingForward = false

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val scrollingForward = delta < 0
            val indexToPrefetch = if (scrollingForward) {
                layoutInfo.visibleItemsInfo.last().index + 1
            } else {
                layoutInfo.visibleItemsInfo.first().index - 1
            }
            if (indexToPrefetch != this@DefaultLazyListPrefetchStrategy.indexToPrefetch &&
                indexToPrefetch in 0 until layoutInfo.totalItemsCount
            ) {
                if (wasScrollingForward != scrollingForward) {
                    // the scrolling direction has been changed which means the last prefetched
                    // is not going to be reached anytime soon so it is safer to dispose it.
                    // if this item is already visible it is safe to call the method anyway
                    // as it will be no-op
                    currentPrefetchHandle?.cancel()
                }
                this@DefaultLazyListPrefetchStrategy.wasScrollingForward = scrollingForward
                this@DefaultLazyListPrefetchStrategy.indexToPrefetch = indexToPrefetch
                currentPrefetchHandle = schedulePrefetch(
                    indexToPrefetch
                )
            }
        }
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        if (indexToPrefetch != -1 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val expectedPrefetchIndex = if (wasScrollingForward) {
                layoutInfo.visibleItemsInfo.last().index + 1
            } else {
                layoutInfo.visibleItemsInfo.first().index - 1
            }
            if (indexToPrefetch != expectedPrefetchIndex) {
                indexToPrefetch = -1
                currentPrefetchHandle?.cancel()
                currentPrefetchHandle = null
            }
        }
    }
}
