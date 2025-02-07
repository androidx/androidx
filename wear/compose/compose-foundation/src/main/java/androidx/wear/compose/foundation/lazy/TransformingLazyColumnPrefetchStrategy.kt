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

import androidx.compose.runtime.Stable
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.wear.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.wear.compose.foundation.lazy.layout.PrefetchScheduler

/**
 * Implementations of this interface control which indices of a TransformingLazyColumn should be
 * prefetched (precomposed and premeasured during idle time) as the user interacts with it.
 *
 * Implementations should invoke [TransformingLazyColumnPrefetchScope.schedulePrefetch] to schedule
 * prefetches from the [onScroll] and [onVisibleItemsUpdated] callbacks. If any of the returned
 * PrefetchHandles no longer need to be prefetched, use
 * [LazyLayoutPrefetchState.PrefetchHandle.cancel] to cancel the request.
 */
internal interface TransformingLazyColumnPrefetchStrategy {

    /**
     * A [PrefetchScheduler] implementation which will be used to execute prefetch requests for this
     * strategy implementation. If null, the default [PrefetchScheduler] for the platform will be
     * used.
     */
    val prefetchScheduler: PrefetchScheduler?
        get() = null

    /**
     * onScroll is invoked when the TransformingLazyColumn scrolls, whether or not the visible items
     * have changed. If the visible items have also changed, then this will be invoked in the same
     * frame *after* [onVisibleItemsUpdated].
     *
     * @param delta the change in scroll direction. Delta < 0 indicates scrolling down while delta >
     *   0 indicates scrolling up.
     * @param measureResult the current [TransformingLazyColumnMeasureResult].
     */
    fun TransformingLazyColumnPrefetchScope.onScroll(
        delta: Float,
        measureResult: TransformingLazyColumnMeasureResult
    )

    /**
     * onVisibleItemsUpdated is invoked when the TransformingLazyColumn scrolls if the visible items
     * have changed.
     *
     * @param measureResult the current [TransformingLazyColumnMeasureResult].
     */
    fun TransformingLazyColumnPrefetchScope.onVisibleItemsUpdated(
        measureResult: TransformingLazyColumnMeasureResult
    )

    /**
     * onNestedPrefetch is invoked when a parent LazyLayout has prefetched content which contains
     * this TransformingLazyColumn. It gives this TransformingLazyColumn a chance to request
     * prefetch for some of its own children before coming onto screen.
     *
     * Implementations can use [NestedPrefetchScope.schedulePrefetch] to schedule child prefetches.
     * For example, this is useful if this TransformingLazyColumn is a LazyRow that is a child of a
     * LazyColumn: in that case, [onNestedPrefetch] can schedule the children it expects to be
     * visible when it comes onto screen, giving the LazyLayout infra a chance to compose these
     * children ahead of time and reduce jank.
     *
     * Generally speaking, [onNestedPrefetch] should only request prefetch for children that it
     * expects to actually be visible when this list is scrolled into view.
     *
     * @param anchorItemIndex the index of the first visible item. It should be used to start
     *   prefetching from the correct index in case the list has been created at a non-zero offset.
     */
    fun NestedPrefetchScope.onNestedPrefetch(anchorItemIndex: Int)
}

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
 * Creates an instance of the default [TransformingLazyColumnPrefetchStrategy], allowing for
 * customization of the nested prefetch count.
 *
 * @param nestedPrefetchItemCount specifies how many inner items should be prefetched when this
 *   TransformingLazyColumn is nested inside another LazyLayout. For example, if this is the state
 *   for a horizontal TransformingLazyColumn nested in a vertical TransformingLazyColumn, you might
 *   want to set this to the number of items that will be visible when this list is scrolled into
 *   view.
 */
internal fun TransformingLazyColumnPrefetchStrategy(
    nestedPrefetchItemCount: Int = 2
): TransformingLazyColumnPrefetchStrategy =
    DefaultTransformingLazyColumnPrefetchStrategy(nestedPrefetchItemCount)

/**
 * The default prefetching strategy for TransformingLazyColumns - this will be used automatically if
 * no other strategy is provided.
 */
@Stable
internal class DefaultTransformingLazyColumnPrefetchStrategy(
    private val nestedPrefetchItemCount: Int = 2
) : TransformingLazyColumnPrefetchStrategy {

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

    override fun TransformingLazyColumnPrefetchScope.onScroll(
        delta: Float,
        measureResult: TransformingLazyColumnMeasureResult
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
                    indexToPrefetch !=
                        this@DefaultTransformingLazyColumnPrefetchStrategy.indexToPrefetch
                ) {
                    if (wasScrollingForward != scrollingForward) {
                        // the scrolling direction has been changed which means the last prefetched
                        // is not going to be reached anytime soon so it is safer to dispose it.
                        // if this item is already visible it is safe to call the method anyway
                        // as it will be no-op
                        currentPrefetchHandle?.cancel()
                    }
                    this@DefaultTransformingLazyColumnPrefetchStrategy.wasScrollingForward =
                        scrollingForward
                    this@DefaultTransformingLazyColumnPrefetchStrategy.indexToPrefetch =
                        indexToPrefetch
                    currentPrefetchHandle = schedulePrefetch(indexToPrefetch)
                }
                if (scrollingForward) {
                    val lastItem = measureResult.visibleItems.last()
                    val spacing = measureResult.itemSpacing
                    val distanceToPrefetchItem =
                        lastItem.offset + lastItem.measuredHeight + spacing -
                            measureResult.viewportSize.height -
                            measureResult.afterContentPadding
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToPrefetchItem < -delta) {
                        currentPrefetchHandle?.markAsUrgent()
                    }
                } else {
                    val firstItem = measureResult.visibleItems.first()
                    val distanceToPrefetchItem =
                        measureResult.beforeContentPadding - firstItem.offset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToPrefetchItem < delta) {
                        currentPrefetchHandle?.markAsUrgent()
                    }
                }
            }
        }
    }

    override fun TransformingLazyColumnPrefetchScope.onVisibleItemsUpdated(
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

    override fun NestedPrefetchScope.onNestedPrefetch(anchorItemIndex: Int) {
        repeat(nestedPrefetchItemCount) { i -> schedulePrefetch(anchorItemIndex + i) }
    }
}
