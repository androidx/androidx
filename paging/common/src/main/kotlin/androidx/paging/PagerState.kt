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

import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Insert
import androidx.paging.PagedList.Config.Companion.MAX_SIZE_UNBOUNDED
import androidx.paging.PagedSource.LoadResult.Page
import androidx.paging.PagedSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

/**
 * Internal state of [Pager] whose updates can be consumed as a [Flow]<[PageEvent]<[Value]>>.
 */
@FlowPreview
@ExperimentalCoroutinesApi
internal class PagerState<Key : Any, Value : Any>(private val maxSize: Int) {
    // TODO: Consider moving the page event channel into Pager
    private val pageEventCh = Channel<PageEvent<Value>>()
    private val pages = mutableListOf<Page<Key, Value>>()
    private var initialPageIndex = 0
    private var placeholdersStart = COUNT_UNDEFINED
    private var placeholdersEnd = COUNT_UNDEFINED

    private var prependLoadId = 0
    private var appendLoadId = 0
    private val prependLoadIdCh = Channel<Int>(Channel.CONFLATED)
    private val appendLoadIdCh = Channel<Int>(Channel.CONFLATED)

    private val loadStates = mutableMapOf<LoadType, LoadState>(
        REFRESH to LoadState.Idle,
        START to LoadState.Idle,
        END to LoadState.Idle
    )

    fun consumePrependGenerationIdAsFlow(): Flow<Int> {
        prependLoadIdCh.offer(prependLoadId)
        return prependLoadIdCh.consumeAsFlow()
    }

    fun consumeAppendGenerationIdAsFlow(): Flow<Int> {
        appendLoadIdCh.offer(appendLoadId)
        return appendLoadIdCh.consumeAsFlow()
    }

    fun consumeAsFlow() = pageEventCh.consumeAsFlow()

    suspend fun updateLoadState(loadType: LoadType, loadState: LoadState) {
        if (loadStates[loadType] != loadState) {
            loadStates[loadType] = loadState
            pageEventCh.send(PageEvent.StateUpdate(loadType, loadState))
        }
    }

    /**
     * @return true if insert was applied, false otherwise.
     */
    suspend fun insert(loadId: Int, loadType: LoadType, page: Page<Key, Value>): Boolean {
        when (loadType) {
            REFRESH -> {
                check(pages.isEmpty()) { "cannot receive multiple init calls" }
                check(loadId == 0) { "init loadId must be the initial value, 0" }

                pages.add(page)
                initialPageIndex = 0
                placeholdersEnd = if (page.itemsAfter != COUNT_UNDEFINED) page.itemsAfter else 0
                placeholdersStart = if (page.itemsBefore != COUNT_UNDEFINED) page.itemsBefore else 0
            }
            START -> {
                check(pages.isNotEmpty()) { "should've received an init before prepend" }

                // Skip this insert if it is the result of a cancelled job due to page drop
                if (loadId != prependLoadId) return false

                pages.add(0, page)
                initialPageIndex++
                placeholdersStart = if (page.itemsBefore == COUNT_UNDEFINED) {
                    (placeholdersStart - page.data.size).coerceAtLeast(0)
                } else {
                    page.itemsBefore
                }
            }
            END -> {
                check(pages.isNotEmpty()) { "should've received an init before append" }

                // Skip this insert if it is the result of a cancelled job due to page drop
                if (loadId != appendLoadId) return false

                pages.add(page)
                placeholdersEnd = if (page.itemsAfter == COUNT_UNDEFINED) {
                    (placeholdersEnd - page.data.size).coerceAtLeast(0)
                } else {
                    page.itemsAfter
                }
            }
        }

        val sourcePageIndex = when (loadType) {
            REFRESH -> 0
            START -> 0 - initialPageIndex
            END -> pages.size - initialPageIndex - 1
        }
        val pages = listOf(TransformablePage(sourcePageIndex, page.data, page.data.size, null))
        val pageEvent: PageEvent<Value> = when (loadType) {
            REFRESH -> Insert.Refresh(pages, placeholdersStart, placeholdersEnd)
            START -> Insert.Start(pages, placeholdersStart)
            END -> Insert.End(pages, placeholdersEnd)
        }
        pageEventCh.send(pageEvent)

        return true
    }

    suspend fun drop(loadType: LoadType, pageCount: Int, placeholdersRemaining: Int) {
        check(pages.size >= pageCount) {
            "invalid drop count. have ${pages.size} but wanted to drop $pageCount"
        }
        when (loadType) {
            START -> {
                repeat(pageCount) { pages.removeAt(0) }
                initialPageIndex -= pageCount
                this.placeholdersStart = placeholdersRemaining

                prependLoadId++
                prependLoadIdCh.offer(prependLoadId)
            }
            END -> {
                repeat(pageCount) { pages.removeAt(pages.size - 1) }
                this.placeholdersEnd = placeholdersRemaining

                appendLoadId++
                appendLoadIdCh.offer(appendLoadId)
            }
            else -> throw IllegalArgumentException("cannot drop $loadType")
        }
        pageEventCh.send(PageEvent.Drop(loadType, pageCount, placeholdersRemaining))
    }

    fun dropInfo(loadType: LoadType): DropInfo? {
        when (loadType) {
            REFRESH -> throw IllegalArgumentException(
                "Drop LoadType must be START or END, but got $loadType"
            )
            START -> {
                // TODO: Incrementally compute this.
                val currentSize = pages.sumBy { it.data.size }
                if (maxSize != MAX_SIZE_UNBOUNDED && currentSize > maxSize) {
                    var pageCount = 0
                    var itemCount = 0
                    pages.takeWhile {
                        pageCount++
                        itemCount += it.data.size
                        currentSize - itemCount > maxSize
                    }

                    return DropInfo(pageCount, placeholdersStart + itemCount)
                }
            }
            END -> {
                // TODO: Incrementally compute this.
                val currentSize = pages.sumBy { it.data.size }
                if (maxSize != MAX_SIZE_UNBOUNDED && currentSize > maxSize) {
                    var pageCount = 0
                    var itemCount = 0
                    pages.takeLastWhile {
                        pageCount++
                        itemCount += it.data.size
                        currentSize - itemCount > maxSize
                    }
                    return DropInfo(pageCount, placeholdersEnd + itemCount)
                }
            }
        }

        return null
    }

    /**
     * The key to use to load next page to prepend or null if we should stop loading in this
     * direction for the provided prefetchDistance and loadId.
     */
    internal fun nextPrependKey(
        loadId: Int,
        pageIndex: Int,
        indexInPage: Int,
        prefetchDistance: Int
    ): Key? {
        if (loadId != prependLoadId) return null

        val itemsBeforePage = (0 until pageIndex).sumBy { pages[it].data.size }
        val shouldLoad = itemsBeforePage + indexInPage < prefetchDistance
        return if (shouldLoad) pages.first().prevKey else null
    }

    /**
     * The key to use to load next page to append or null if we should stop loading in this
     * direction for the provided prefetchDistance and loadId.
     */
    internal fun nextAppendKey(
        loadId: Int,
        pageIndex: Int,
        indexInPage: Int,
        prefetchDistance: Int
    ): Key? {
        if (loadId != appendLoadId) return null

        val itemsIncludingPage = (pageIndex until pages.size).sumBy { pages[it].data.size }
        val shouldLoad = indexInPage + 1 + prefetchDistance > itemsIncludingPage
        return if (shouldLoad) pages.last().nextKey else null
    }

    /**
     * @param indexInPage Index in [Page] with index [pageIndex]
     * @param pageIndex Index in [pages]
     *
     * @return Information needed to request a refresh key from [PagedSource] via
     * [PagedSource.getRefreshKeyFromPage] if available, null otherwise, which should direct the
     * [PageFetcher] to simply use initialKey.
     */
    internal fun refreshInfo(indexInPage: Int, pageIndex: Int): RefreshInfo<Key, Value>? {
        if (pages.isEmpty()) return null

        // Try to find the page and use prev page's next key
        return when {
            pageIndex < 0 -> RefreshInfo(0, pages.first())
            pageIndex > pages.size -> {
                val lastPage = pages.last()
                RefreshInfo(lastPage.data.size - 1, lastPage)
            }
            else -> RefreshInfo(indexInPage, pages[pageIndex])
        }
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
     * * pageIndex - See the description for indexInPage, this is index of page in [pages]
     * coerced from [ViewportHint.sourcePageIndex]
     * * itemsStart - Remaining placeholders before the items currently loaded in [pages]]
     * * itemsEnd - Remaining placeholders after the items currently loaded in [pages]]
     */
    internal suspend fun <T> ViewportHint.withCoercedHint(
        block: suspend (indexInPage: Int, pageIndex: Int, itemsStart: Int, itemsEnd: Int) -> T
    ): T {
        var itemsStart = 0
        var itemsEnd = 0

        var pageIndex = sourcePageIndex + initialPageIndex

        // We walk the list every time here as itemsStart and itemsEnd are dependent on
        // ViewportHint.indexInPage and ViewportHint.sourcePageIndex, which need to be coerced to
        // valid values within the current state.
        pages.forEachIndexed { index, page ->
            when {
                index < pageIndex -> itemsStart += page.data.size
                index == pageIndex -> {
                    itemsStart += indexInPage
                    itemsEnd += page.data.size - indexInPage
                }
                else -> itemsEnd += page.data.size
            }
        }

        // If hint refers to a page that has been dropped, return immediately since it's
        // impossible to adjust pageIndex and indexInPage based on what has been loaded.
        if (pageIndex == -1) {
            return block(indexInPage, pageIndex, itemsStart, itemsEnd)
        }

        // Coerce indexInPage, which may have a value outside the bounds of the page size
        // referenced by sourcePageIndex.
        var indexInPage = indexInPage
        while (indexInPage !in pages[pageIndex].data.indices) {
            pageIndex++
            indexInPage -= pages[pageIndex].data.size
        }

        return block(indexInPage, pageIndex, itemsStart, itemsEnd)
    }
}

internal class DropInfo(val pageCount: Int, val placeholdersRemaining: Int)

internal class RefreshInfo<Key : Any, Value : Any>(
    val indexInPage: Int,
    val page: Page<Key, Value>
)