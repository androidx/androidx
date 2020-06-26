/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import androidx.annotation.CheckResult
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PagingConfig.Companion.MAX_SIZE_UNBOUNDED
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onStart

/**
 * Internal state of [PageFetcherSnapshot] whose updates can be consumed as a [Flow] of [PageEvent].
 */
internal class PageFetcherSnapshotState<Key : Any, Value : Any>(
    private val config: PagingConfig,
    hasRemoteState: Boolean
) {
    private val _pages = mutableListOf<Page<Key, Value>>()
    internal val pages: List<Page<Key, Value>> = _pages
    private var initialPageIndex = 0

    private var _placeholdersBefore = 0
    internal var placeholdersBefore
        get() = when {
            config.enablePlaceholders -> _placeholdersBefore
            else -> 0
        }
        set(value) {
            _placeholdersBefore = when (value) {
                COUNT_UNDEFINED -> 0
                else -> value
            }
        }

    private var _placeholdersAfter = 0
    internal var placeholdersAfter
        get() = when {
            config.enablePlaceholders -> _placeholdersAfter
            else -> 0
        }
        set(value) {
            _placeholdersAfter = when (value) {
                COUNT_UNDEFINED -> 0
                else -> value
            }
        }

    internal var prependLoadId = 0
        private set
    internal var appendLoadId = 0
        private set
    private val prependLoadIdCh = Channel<Int>(Channel.CONFLATED)
    private val appendLoadIdCh = Channel<Int>(Channel.CONFLATED)

    /**
     * Cache previous ViewportHint which triggered any failed PagingSource APPEND / PREPEND that
     * we can later retry. This is so we always trigger loads based on hints, instead of having
     * two different ways to trigger.
     */
    internal val failedHintsByLoadType = mutableMapOf<LoadType, ViewportHint>()
    internal val loadStates = MutableLoadStateCollection(hasRemoteState)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consumePrependGenerationIdAsFlow(): Flow<Int> {
        return prependLoadIdCh.consumeAsFlow()
            .onStart { prependLoadIdCh.offer(prependLoadId) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consumeAppendGenerationIdAsFlow(): Flow<Int> {
        return appendLoadIdCh.consumeAsFlow()
            .onStart { appendLoadIdCh.offer(appendLoadId) }
    }

    /**
     * Convert a loaded [Page] into a [PageEvent] for [PageFetcherSnapshot.pageEventCh].
     *
     * Note: This method should be called after state updated by [insert]
     *
     * TODO: Move this into Pager, which owns pageEventCh, since this logic is sensitive to its
     *  implementation.
     */
    internal fun Page<Key, Value>.toPageEvent(loadType: LoadType): PageEvent<Value> {
        val sourcePageIndex = when (loadType) {
            REFRESH -> 0
            PREPEND -> 0 - initialPageIndex
            APPEND -> pages.size - initialPageIndex - 1
        }
        val pages = listOf(TransformablePage(sourcePageIndex, data, data.size, null))
        return when (loadType) {
            REFRESH -> Refresh(
                pages = pages,
                placeholdersBefore = placeholdersBefore,
                placeholdersAfter = placeholdersAfter,
                combinedLoadStates = loadStates.snapshot()
            )
            PREPEND -> Prepend(
                pages = pages,
                placeholdersBefore = placeholdersBefore,
                combinedLoadStates = loadStates.snapshot()
            )
            APPEND -> Append(
                pages = pages,
                placeholdersAfter = placeholdersAfter,
                combinedLoadStates = loadStates.snapshot()
            )
        }
    }

    /**
     * @return true if insert was applied, false otherwise.
     */
    @CheckResult
    fun insert(loadId: Int, loadType: LoadType, page: Page<Key, Value>): Boolean {
        when (loadType) {
            REFRESH -> {
                check(pages.isEmpty()) { "cannot receive multiple init calls" }
                check(loadId == 0) { "init loadId must be the initial value, 0" }

                _pages.add(page)
                initialPageIndex = 0
                placeholdersAfter = page.itemsAfter
                placeholdersBefore = page.itemsBefore
            }
            PREPEND -> {
                check(pages.isNotEmpty()) { "should've received an init before prepend" }

                // Skip this insert if it is the result of a cancelled job due to page drop
                if (loadId != prependLoadId) return false

                _pages.add(0, page)
                initialPageIndex++
                placeholdersBefore = if (page.itemsBefore == COUNT_UNDEFINED) {
                    (placeholdersBefore - page.data.size).coerceAtLeast(0)
                } else {
                    page.itemsBefore
                }

                // Clear error on successful insert
                failedHintsByLoadType.remove(PREPEND)
            }
            APPEND -> {
                check(pages.isNotEmpty()) { "should've received an init before append" }

                // Skip this insert if it is the result of a cancelled job due to page drop
                if (loadId != appendLoadId) return false

                _pages.add(page)
                placeholdersAfter = if (page.itemsAfter == COUNT_UNDEFINED) {
                    (placeholdersAfter - page.data.size).coerceAtLeast(0)
                } else {
                    page.itemsAfter
                }

                // Clear error on successful insert
                failedHintsByLoadType.remove(APPEND)
            }
        }

        return true
    }

    fun drop(loadType: LoadType, pageCount: Int, placeholdersRemaining: Int) {
        check(pages.size >= pageCount) {
            "invalid drop count. have ${pages.size} but wanted to drop $pageCount"
        }

        // Reset load state to NotLoading(endOfPaginationReached = false).
        failedHintsByLoadType.remove(loadType)
        loadStates.set(loadType, false, NotLoading.Incomplete)

        when (loadType) {
            PREPEND -> {
                repeat(pageCount) { _pages.removeAt(0) }
                initialPageIndex -= pageCount

                placeholdersBefore = placeholdersRemaining

                prependLoadId++
                prependLoadIdCh.offer(prependLoadId)
            }
            APPEND -> {
                repeat(pageCount) { _pages.removeAt(pages.size - 1) }

                placeholdersAfter = placeholdersRemaining

                appendLoadId++
                appendLoadIdCh.offer(appendLoadId)
            }
            else -> throw IllegalArgumentException("cannot drop $loadType")
        }
    }

    suspend fun dropInfo(
        loadType: LoadType,
        loadHint: ViewportHint
    ): DropInfo? {
        // Never drop below 2 pages as this can cause UI flickering with certain configs and it's
        // much more important to protect against this behaviour over respecting a config where
        // maxSize is set unusually (probably incorrectly) strict.
        if (pages.size <= 2) return null

        when (loadType) {
            REFRESH -> throw IllegalArgumentException(
                "Drop LoadType must be START or END, but got $loadType"
            )
            PREPEND -> {
                // Compute the first pageIndex of the first loaded page fulfilling
                // prefetchDistance.
                val prefetchWindowStartPageIndex =
                    loadHint.withCoercedHint { indexInPage, pageIndex, _ ->
                        var prefetchWindowStartPageIndex = pageIndex
                        var prefetchWindowItems = config.prefetchDistance - (indexInPage + 1)
                        while (prefetchWindowStartPageIndex > 0 && prefetchWindowItems > 0) {
                            prefetchWindowItems -= pages[prefetchWindowStartPageIndex].data.size
                            prefetchWindowStartPageIndex--
                        }

                        prefetchWindowStartPageIndex
                    }

                // TODO: Incrementally compute this.
                val currentSize = pages.sumBy { it.data.size }
                if (
                    config.maxSize != MAX_SIZE_UNBOUNDED && currentSize > config.maxSize &&
                    prefetchWindowStartPageIndex > 0
                ) {
                    var pageCount = 0
                    var itemCount = 0
                    pages.takeWhile {
                        pageCount++
                        itemCount += it.data.size

                        currentSize - itemCount > config.maxSize &&
                                // Do not drop pages that would fulfill prefetchDistance.
                                pageCount < prefetchWindowStartPageIndex
                    }

                    val placeholdersRemaining = when {
                        config.enablePlaceholders -> placeholdersBefore + itemCount
                        else -> 0
                    }

                    return DropInfo(pageCount, placeholdersRemaining)
                }
            }
            APPEND -> {
                // Compute the last pageIndex of the loaded page fulfilling
                // prefetchDistance.
                val prefetchWindowEndPageIndex =
                    loadHint.withCoercedHint { indexInPage, pageIndex, _ ->
                        var prefetchWindowEndPageIndex = pageIndex
                        var prefetchWindowItems =
                            config.prefetchDistance - pages[pageIndex].data.size + indexInPage
                        while (
                            prefetchWindowEndPageIndex < pages.lastIndex &&
                            prefetchWindowItems > 0
                        ) {
                            prefetchWindowItems -= pages[prefetchWindowEndPageIndex].data.size
                            prefetchWindowEndPageIndex++
                        }

                        prefetchWindowEndPageIndex
                    }

                // TODO: Incrementally compute this.
                val currentSize = pages.sumBy { it.data.size }
                if (
                    config.maxSize != MAX_SIZE_UNBOUNDED && currentSize > config.maxSize &&
                    prefetchWindowEndPageIndex < pages.lastIndex
                ) {
                    var pageCount = 0
                    var itemCount = 0
                    pages.takeLastWhile {
                        pageCount++
                        itemCount += it.data.size

                        currentSize - itemCount > config.maxSize &&
                                // Do not drop pages that would fulfill prefetchDistance.
                                pages.lastIndex - pageCount > prefetchWindowEndPageIndex
                    }

                    val placeholdersRemaining = when {
                        config.enablePlaceholders -> placeholdersAfter + itemCount
                        else -> 0
                    }

                    return DropInfo(pageCount, placeholdersRemaining)
                }
            }
        }

        return null
    }

    /**
     * Calls the specified [block] with a [ViewportHint] that has been coerced with respect to the
     * current state of [pages].
     *
     * The follow parameters are provided into the specified [block]:
     * * indexInPage - Coerced from [ViewportHint.indexInPage], the index within page specified by
     * pageIndex. If the page specified by [ViewportHint.sourcePageIndex] cannot fulfill the
     * specified indexInPage, pageIndex will be incremented to a valid value and indexInPage will
     * be decremented.
     * * pageIndex - See the description for indexInPage, index in [pages] coerced from
     * [ViewportHint.sourcePageIndex]
     * * hintOffset - The numbers of items the hint was snapped by when coercing within the
     * bounds of loaded pages.
     *
     * Note: If an invalid / out-of-date sourcePageIndex is passed, it will be coerced to the
     * closest pageIndex within the range of [pages]
     */
    internal suspend fun <T> ViewportHint.withCoercedHint(
        block: suspend (indexInPage: Int, pageIndex: Int, hintOffset: Int) -> T
    ): T {
        if (pages.isEmpty()) {
            throw IllegalStateException("Cannot coerce hint when no pages have loaded")
        }

        var indexInPage = indexInPage
        var pageIndex = sourcePageIndex + initialPageIndex
        var hintOffset = 0

        // Coerce pageIndex to >= 0, snap indexInPage to 0 if pageIndex is coerced.
        if (pageIndex < 0) {
            hintOffset = pageIndex * config.pageSize + indexInPage

            pageIndex = 0
            indexInPage = 0
        } else if (pageIndex > pages.lastIndex) {
            // Number of items after last loaded item that this hint refers to.
            hintOffset = (pageIndex - pages.lastIndex - 1) * config.pageSize + indexInPage + 1

            pageIndex = pages.lastIndex
            indexInPage = pages.last().data.lastIndex
        } else {
            if (indexInPage !in pages[pageIndex].data.indices) {
                hintOffset = indexInPage
            }

            // Reduce indexInPage by incrementing pageIndex while indexInPage is outside the bounds
            // of the page referenced by pageIndex.
            while (pageIndex < pages.lastIndex && indexInPage > pages[pageIndex].data.lastIndex) {
                hintOffset -= pages[pageIndex].data.size
                indexInPage -= pages[pageIndex].data.size
                pageIndex++
            }
        }

        return block(indexInPage, pageIndex, hintOffset)
    }
}

internal class DropInfo(val pageCount: Int, val placeholdersRemaining: Int)
