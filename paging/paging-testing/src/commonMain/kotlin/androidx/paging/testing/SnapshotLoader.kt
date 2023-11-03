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

package androidx.paging.testing

import androidx.annotation.VisibleForTesting
import androidx.paging.DifferCallback
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
import androidx.paging.PagingSource
import androidx.paging.testing.LoaderCallback.CallbackType.ON_INSERTED
import androidx.paging.testing.internal.AtomicInt
import androidx.paging.testing.internal.AtomicRef
import kotlin.jvm.JvmSuppressWildcards
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Contains the public APIs for load operations in tests.
 *
 * Tracks generational information and provides the listener to [DifferCallback] on
 * [PagingDataDiffer] operations.
 */
@VisibleForTesting
public class SnapshotLoader<Value : Any> internal constructor(
    private val differ: CompletablePagingDataDiffer<Value>,
    private val errorHandler: LoadErrorHandler,
) {
    internal val generations = MutableStateFlow(Generation())

    /**
     * Refresh the data that is presented on the UI.
     *
     * [refresh] triggers a new generation of [PagingData] / [PagingSource]
     * to represent an updated snapshot of the backing dataset.
     *
     * This fake paging operation mimics UI-driven refresh signals such as swipe-to-refresh.
     */
    public suspend fun refresh(): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading(errorHandler)
        differ.refresh()
        differ.awaitNotLoading(errorHandler)
    }

    /**
     * Imitates scrolling down paged items, [appending][APPEND] data until the given
     * predicate returns false.
     *
     * Note: This API loads an item before passing it into the predicate. This means the
     * loaded pages may include the page which contains the item that does not match the
     * predicate. For example, if pageSize = 2, the predicate
     * {item: Int -> item < 3 } will return items [[1, 2],[3, 4]] where [3, 4] is the page
     * containing the boundary item[3] not matching the predicate.
     *
     * The loaded pages are also dependent on [PagingConfig] settings such as
     * [PagingConfig.prefetchDistance]:
     * - if `prefetchDistance` > 0, the resulting appends will include prefetched items.
     * For example, if pageSize = 2 and prefetchDistance = 2, the predicate
     * {item: Int -> item < 3 } will load items [[1, 2], [3, 4], [5, 6]] where [5, 6] is the
     * prefetched page.
     *
     * @param [predicate] the predicate to match (return true) to continue append scrolls
     */
    public suspend fun appendScrollWhile(
        predicate: (item: @JvmSuppressWildcards Value) -> @JvmSuppressWildcards Boolean
    ): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading(errorHandler)
        appendOrPrependScrollWhile(LoadType.APPEND, predicate)
        differ.awaitNotLoading(errorHandler)
    }

    /**
     * Imitates scrolling up paged items, [prepending][PREPEND] data until the given
     * predicate returns false.
     *
     * Note: This API loads an item before passing it into the predicate. This means the
     * loaded pages may include the page which contains the item that does not match the
     * predicate. For example, if pageSize = 2, initialKey = 3, the predicate
     * {item: Int -> item >= 3 } will return items [[1, 2],[3, 4]] where [1, 2] is the page
     * containing the boundary item[2] not matching the predicate.
     *
     * The loaded pages are also dependent on [PagingConfig] settings such as
     * [PagingConfig.prefetchDistance]:
     * - if `prefetchDistance` > 0, the resulting prepends will include prefetched items.
     * For example, if pageSize = 2, initialKey = 3, and prefetchDistance = 2, the predicate
     * {item: Int -> item > 4 } will load items [[1, 2], [3, 4], [5, 6]] where both [1,2] and
     * [5, 6] are the prefetched pages.
     *
     * @param [predicate] the predicate to match (return true) to continue prepend scrolls
     */
    public suspend fun prependScrollWhile(
        predicate: (item: @JvmSuppressWildcards Value) -> @JvmSuppressWildcards Boolean
    ): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading(errorHandler)
        appendOrPrependScrollWhile(LoadType.PREPEND, predicate)
        differ.awaitNotLoading(errorHandler)
    }

    private suspend fun appendOrPrependScrollWhile(
        loadType: LoadType,
        predicate: (item: Value) -> Boolean
    ) {
        do {
            // awaits for next item where the item index is determined based on
            // this generation's lastAccessedIndex. If null, it means there are no more
            // items to load for this loadType.
            val item = awaitNextItem(loadType) ?: return
        } while (predicate(item))
    }

    /**
     * Imitates scrolling from current index to the target index. It waits for an item to be loaded
     * in before triggering load on next item. Returns all available data that has been scrolled
     * through.
     *
     * The scroll direction (prepend or append) is dependent on current index and target index. In
     * general, scrolling to a smaller index triggers [PREPEND] while scrolling to a larger
     * index triggers [APPEND].
     *
     * When [PagingConfig.enablePlaceholders] is false, the [index] is scoped within currently
     * loaded items. For example, in a list of items(0-20) with currently loaded items(10-15),
     * index[0] = item(10), index[4] = item(15).
     *
     * Supports [index] beyond currently loaded items when [PagingConfig.enablePlaceholders]
     * is false:
     * 1. For prepends, it supports negative indices for as long as there are still available
     * data to load from. For example, take a list of items(0-20), pageSize = 1, with currently
     * loaded items(10-15). With index[0] = item(10), a `scrollTo(-4)` will scroll to item(6) and
     * update index[0] = item(6).
     * 2. For appends, it supports indices >= loadedDataSize. For example, take a list of
     * items(0-20), pageSize = 1, with currently loaded items(10-15). With
     * index[4] = item(15), a `scrollTo(7)` will scroll to item(18) and update
     * index[7] = item(18).
     * Note that both examples does not account for prefetches.

     * The [index] accounts for separators/headers/footers where each one of those consumes one
     * scrolled index.
     *
     * For both append/prepend, this function stops loading prior to fulfilling requested scroll
     * distance if there are no more data to load from.
     *
     * @param [index] The target index to scroll to
     *
     * @see [flingTo] for faking a scroll that continues scrolling without waiting for items to
     * be loaded in. Supports jumping.
     */
    public suspend fun scrollTo(index: Int): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading(errorHandler)
        appendOrPrependScrollTo(index)
        differ.awaitNotLoading(errorHandler)
    }

    /**
     * Scrolls from current index to targeted [index].
     *
     * Internally this method scrolls until it fulfills requested index
     * differential (Math.abs(requested index - current index)) rather than scrolling
     * to the exact requested index. This is because item indices can shift depending on scroll
     * direction and placeholders. Therefore we try to fulfill the expected amount of scrolling
     * rather than the actual requested index.
     */
    private suspend fun appendOrPrependScrollTo(index: Int) {
        val startIndex = generations.value.lastAccessedIndex.get()
        val loadType = if (startIndex > index) LoadType.PREPEND else LoadType.APPEND
        val scrollCount = abs(startIndex - index)
        awaitScroll(loadType, scrollCount)
    }

    /**
     * Imitates flinging from current index to the target index. It will continue scrolling
     * even as data is being loaded in. Returns all available data that has been scrolled
     * through.
     *
     * The scroll direction (prepend or append) is dependent on current index and target index. In
     * general, scrolling to a smaller index triggers [PREPEND] while scrolling to a larger
     * index triggers [APPEND].
     *
     * This function will scroll into placeholders. This means jumping is supported when
     * [PagingConfig.enablePlaceholders] is true and the amount of placeholders traversed
     * has reached [PagingConfig.jumpThreshold]. Jumping is disabled when
     * [PagingConfig.enablePlaceholders] is false.
     *
     * When [PagingConfig.enablePlaceholders] is false, the [index] is scoped within currently
     * loaded items. For example, in a list of items(0-20) with currently loaded items(10-15),
     * index[0] = item(10), index[4] = item(15).
     *
     * Supports [index] beyond currently loaded items when [PagingConfig.enablePlaceholders]
     * is false:
     * 1. For prepends, it supports negative indices for as long as there are still available
     * data to load from. For example, take a list of items(0-20), pageSize = 1, with currently
     * loaded items(10-15). With index[0] = item(10), a `scrollTo(-4)` will scroll to item(6) and
     * update index[0] = item(6).
     * 2. For appends, it supports indices >= loadedDataSize. For example, take a list of
     * items(0-20), pageSize = 1, with currently loaded items(10-15). With
     * index[4] = item(15), a `scrollTo(7)` will scroll to item(18) and update
     * index[7] = item(18).
     * Note that both examples does not account for prefetches.

     * The [index] accounts for separators/headers/footers where each one of those consumes one
     * scrolled index.
     *
     * For both append/prepend, this function stops loading prior to fulfilling requested scroll
     * distance if there are no more data to load from.
     *
     * @param [index] The target index to scroll to
     *
     * @see [scrollTo] for faking scrolls that awaits for placeholders to load before continuing
     * to scroll.
     */
    public suspend fun flingTo(index: Int): @JvmSuppressWildcards Unit {
        differ.awaitNotLoading(errorHandler)
        appendOrPrependFlingTo(index)
        differ.awaitNotLoading(errorHandler)
    }

    /**
     * We start scrolling from startIndex +/- 1 so we don't accidentally trigger
     * a prefetch on the opposite direction.
     */
    private suspend fun appendOrPrependFlingTo(index: Int) {
        val startIndex = generations.value.lastAccessedIndex.get()
        val loadType = if (startIndex > index) LoadType.PREPEND else LoadType.APPEND

        when (loadType) {
            LoadType.PREPEND -> prependFlingTo(startIndex, index)
            LoadType.APPEND -> appendFlingTo(startIndex, index)
        }
    }

    /**
     * Prepend flings to target index.
     *
     * If target index is negative, from index[0] onwards it will normal scroll until it fulfills
     * remaining distance.
     */
    private suspend fun prependFlingTo(startIndex: Int, index: Int) {
        var lastAccessedIndex = startIndex
        val endIndex = maxOf(0, index)
        // first, fast scroll to index or zero
        for (i in startIndex - 1 downTo endIndex) {
            differ[i]
            lastAccessedIndex = i
        }
        setLastAccessedIndex(lastAccessedIndex)
        // for negative indices, we delegate remainder of scrolling (distance below zero)
        // to the awaiting version.
        if (index < 0) {
            val scrollCount = abs(index)
            flingToOutOfBounds(LoadType.PREPEND, lastAccessedIndex, scrollCount)
        }
    }

    /**
     * Append flings to target index.
     *
     * If target index is beyond [PagingDataDiffer.size] - 1, from index(differ.size) and onwards,
     * it will normal scroll until it fulfills remaining distance.
     */
    private suspend fun appendFlingTo(startIndex: Int, index: Int) {
        var lastAccessedIndex = startIndex
        val endIndex = minOf(index, differ.size - 1)
        // first, fast scroll to endIndex
        for (i in startIndex + 1..endIndex) {
            differ[i]
            lastAccessedIndex = i
        }
        setLastAccessedIndex(lastAccessedIndex)
        // for indices at or beyond differ.size, we delegate remainder of scrolling (distance
        // beyond differ.size) to the awaiting version.
        if (index >= differ.size) {
            val scrollCount = index - lastAccessedIndex
            flingToOutOfBounds(LoadType.APPEND, lastAccessedIndex, scrollCount)
        }
    }

    /**
     * Delegated work from [flingTo] that is responsible for scrolling to indices that is
     * beyond the range of [0 to differ.size-1].
     *
     * When [PagingConfig.enablePlaceholders] is true, this function is no-op because
     * there is no more data to load from.
     *
     * When [PagingConfig.enablePlaceholders] is false, its delegated work to [awaitScroll]
     * essentially loops (trigger next page --> await for next page) until
     * it fulfills remaining (out of bounds) requested scroll distance.
     */
    private suspend fun flingToOutOfBounds(
        loadType: LoadType,
        lastAccessedIndex: Int,
        scrollCount: Int
    ) {
        // Wait for the page triggered by differ[lastAccessedIndex] to load in. This gives us the
        // offsetIndex for next differ.get() because the current lastAccessedIndex is already the
        // boundary index, such that differ[lastAccessedIndex +/- 1] will throw IndexOutOfBounds.
        val (_, offsetIndex) = awaitLoad(lastAccessedIndex)
        setLastAccessedIndex(offsetIndex)
        // starts loading from the offsetIndex and scrolls the remaining requested distance
        awaitScroll(loadType, scrollCount)
    }

    private suspend fun awaitScroll(loadType: LoadType, scrollCount: Int) {
        repeat(scrollCount) {
            awaitNextItem(loadType) ?: return
        }
    }

    /**
     * Triggers load for next item, awaits for it to be loaded and returns the loaded item.
     *
     * It calculates the next load index based on loadType and this generation's
     * [Generation.lastAccessedIndex]. The lastAccessedIndex is updated when item is loaded in.
     */
    private suspend fun awaitNextItem(loadType: LoadType): Value? {
        // Get the index to load from. Return if index is invalid.
        val index = nextIndexOrNull(loadType) ?: return null
        // OffsetIndex accounts for items that are prepended when placeholders are disabled,
        // as the new items shift the position of existing items. The offsetIndex (which may
        // or may not be the same as original index) is stored as lastAccessedIndex after load and
        // becomes the basis for next load index.
        val (item, offsetIndex) = awaitLoad(index)
        setLastAccessedIndex(offsetIndex)
        return item
    }

    /**
     * Get and update the index to load from. Returns null if next index is out of bounds.
     *
     * This method computes the next load index based on the [LoadType] and
     * [Generation.lastAccessedIndex]
     */
    private fun nextIndexOrNull(loadType: LoadType): Int? {
        val currIndex = generations.value.lastAccessedIndex.get()
        return when (loadType) {
            LoadType.PREPEND -> {
                if (currIndex <= 0) {
                    return null
                }
                currIndex - 1
            }
            LoadType.APPEND -> {
                if (currIndex >= differ.size - 1) {
                    return null
                }
                currIndex + 1
            }
        }
    }

    // Executes actual loading by accessing the PagingDataDiffer
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun awaitLoad(index: Int): Pair<Value, Int> {
        differ[index]
        differ.awaitNotLoading(errorHandler)
        var offsetIndex = index

        // awaits for the item to be loaded
        return generations.map { generation ->
            // reset callbackState to null so it doesn't get applied on the next load
            val callbackState = generation.callbackState.getAndSet(null)
            // offsetIndex accounts for items prepended when placeholders are disabled. This
            // is necessary because the new items shift the position of existing items, and
            // the index no longer tracks the correct item.
            offsetIndex += callbackState?.computeIndexOffset() ?: 0
            differ.peek(offsetIndex)
        }.filterNotNull().first() to offsetIndex
    }

    /**
     * Computes the offset to add to the index when loading items from differ.
     *
     * The purpose of this is to address shifted item positions when new items are prepended
     * with placeholders disabled. For example, loaded items(10-12) in the NullPaddedList
     * would have item(12) at differ[2]. If we prefetched items(7-9), item(12) would now be in
     * differ[5].
     *
     * Without the offset, [PREPEND] operations would call differ[1] to load next item(11)
     * which would now yield item(8) instead of item(11). The offset would add the
     * inserted count to the next load index such that after prepending 3 new items(7-9), the next
     * [PREPEND] operation would call differ[1+3 = 4] to properly load next item(11).
     *
     * This method is essentially no-op unless the callback meets three conditions:
     * - is type [DifferCallback.onChanged]
     * - position is 0 as we only care about item prepended to front of list
     * - inserted count > 0
     */
    private fun LoaderCallback.computeIndexOffset(): Int {
        return if (type == ON_INSERTED && position == 0) count else 0
    }

    private fun setLastAccessedIndex(index: Int) {
        generations.value.lastAccessedIndex.set(index)
    }

    /**
     * The callback to be invoked by DifferCallback on a single generation.
     * Increase the callbackCount to notify SnapshotLoader that the dataset has updated
     */
    internal fun onDataSetChanged(gen: Generation, callback: LoaderCallback) {
        val currGen = generations.value
        // we make sure the generation with the dataset change is still valid because we
        // want to disregard callbacks on stale generations
        if (gen.id == currGen.id) {
            generations.value = gen.copy(
                callbackCount = currGen.callbackCount + 1,
                callbackState = currGen.callbackState.apply { set(callback) }
            )
        }
    }

    private enum class LoadType {
        PREPEND,
        APPEND
    }
}

internal data class Generation(
    /**
     * Id of the current Paging generation. Incremented on each new generation (when a new
     * PagingData is received).
     */
    val id: Int = -1,

    /**
     * A count of the number of times Paging invokes a [DifferCallback] callback within a single
     * generation. Incremented on each [DifferCallback] callback invoked, i.e. on item inserted.
     *
     * The callbackCount enables [SnapshotLoader] to await for a requested item and continue
     * loading next item only after a callback is invoked.
     */
    val callbackCount: Int = 0,

    /**
     * Temporarily stores the latest [DifferCallback] to track prepends to the beginning of list.
     * Value is reset to null once read.
     */
    val callbackState: AtomicRef<LoaderCallback?> = AtomicRef(null),

    /**
     * Tracks the last accessed(peeked) index on the differ for this generation
      */
    var lastAccessedIndex: AtomicInt = AtomicInt(0)
)

internal data class LoaderCallback(
    val type: CallbackType,
    val position: Int,
    val count: Int,
) {
    internal enum class CallbackType {
        ON_CHANGED,
        ON_INSERTED,
        ON_REMOVED,
    }
}
