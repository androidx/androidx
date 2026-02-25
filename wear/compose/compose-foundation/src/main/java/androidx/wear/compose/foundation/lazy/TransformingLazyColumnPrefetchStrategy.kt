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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.runtime.Stable

/**
 * Scope for callbacks in [TransformingLazyColumnPrefetchStrategy] which allows prefetches to be
 * requested.
 */
internal interface TransformingLazyColumnPrefetchScope {

    /**
     * Schedules a prefetch for the given index. Requests are executed in the order they're
     * requested. If a requested prefetch is no longer necessary (for example, due to changing
     * scroll direction), the request should be canceled via
     * [LazyLayoutPrefetchState.PrefetchHandle.cancel].
     *
     * See [PrefetchScheduler].
     *
     * @param index the index of the child to prefetch
     */
    fun schedulePrefetch(index: Int): LazyLayoutPrefetchState.PrefetchHandle
}

/**
 * The default prefetching strategy for TransformingLazyColumns - this will be used automatically if
 * no other strategy is provided.
 */
@Stable
internal class TransformingLazyColumnPrefetchStrategy() {

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var indexToPrefetch = -1

    /** The handle associated with the current index from [indexToPrefetch]. */
    private var currentPrefetchHandle: LazyLayoutPrefetchState.PrefetchHandle? = null

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to detect
     * the scrolling direction change.
     */
    private var wasScrollingForward = false

    fun TransformingLazyColumnPrefetchScope.onScroll(
        delta: Float,
        measureResult: TransformingLazyColumnMeasureResult,
    ) {
        if (measureResult.visibleItems.isNotEmpty()) {
            val scrollingForward = delta < 0
            val indexToPrefetch =
                if (scrollingForward) {
                    measureResult.visibleItems.last().index + 1
                } else {
                    measureResult.visibleItems.first().index - 1
                }
            if (indexToPrefetch in 0 until measureResult.totalItemsCount) {
                if (
                    indexToPrefetch != this@TransformingLazyColumnPrefetchStrategy.indexToPrefetch
                ) {
                    if (wasScrollingForward != scrollingForward) {
                        // the scrolling direction has been changed which means the last prefetched
                        // is not going to be reached anytime soon so it is safer to dispose it.
                        // if this item is already visible it is safe to call the method anyway
                        // as it will be no-op
                        currentPrefetchHandle?.cancel()
                    }
                    this@TransformingLazyColumnPrefetchStrategy.wasScrollingForward =
                        scrollingForward
                    this@TransformingLazyColumnPrefetchStrategy.indexToPrefetch = indexToPrefetch
                    currentPrefetchHandle = schedulePrefetch(indexToPrefetch)
                }
                if (scrollingForward) {
                    val lastItem = measureResult.visibleItems.last()
                    val spacing = measureResult.itemSpacing
                    val distanceToPrefetchItem =
                        lastItem.offset + lastItem.measuredHeight + spacing -
                            measureResult.viewportSize.height
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToPrefetchItem < -delta) {
                        currentPrefetchHandle?.markAsUrgent()
                    }
                } else {
                    val firstItem = measureResult.visibleItems.first()
                    val distanceToPrefetchItem = -firstItem.offset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToPrefetchItem < delta) {
                        currentPrefetchHandle?.markAsUrgent()
                    }
                }
            }
        }
    }

    fun TransformingLazyColumnPrefetchScope.onVisibleItemsUpdated(
        measureResult: TransformingLazyColumnMeasureResult
    ) {
        if (indexToPrefetch != -1 && measureResult.visibleItems.isNotEmpty()) {
            val expectedPrefetchIndex =
                if (wasScrollingForward) {
                    measureResult.visibleItems.last().index + 1
                } else {
                    measureResult.visibleItems.first().index - 1
                }
            if (indexToPrefetch != expectedPrefetchIndex) {
                indexToPrefetch = -1
                currentPrefetchHandle?.cancel()
                currentPrefetchHandle = null
            }
        }
    }
}
