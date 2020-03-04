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

import androidx.paging.LoadState.Idle
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Insert.Companion.End
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.Insert.Companion.Start
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onStart
import kotlin.math.absoluteValue

/**
 * Internal state of [Pager] whose updates can be consumed as a [Flow]<[PageEvent]<[Value]>>.
 */
@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class PagerState<Key : Any, Value : Any>(
    private val pageSize: Int,
    private val maxSize: Int
) {
    private val _pages = mutableListOf<Page<Key, Value>>()
    internal val pages: List<Page<Key, Value>> = _pages
    private var initialPageIndex = 0
    internal var placeholdersStart = COUNT_UNDEFINED
    internal var placeholdersEnd = COUNT_UNDEFINED

    internal var prependLoadId = 0
        private set
    internal var appendLoadId = 0
        private set
    private val prependLoadIdCh = Channel<Int>(Channel.CONFLATED)
    private val appendLoadIdCh = Channel<Int>(Channel.CONFLATED)

    internal val failedHintsByLoadType = mutableMapOf<LoadType, ViewportHint>()
    internal val loadStates = mutableMapOf<LoadType, LoadState>(
        REFRESH to Idle,
        START to Idle,
        END to Idle
    )

    fun consumePrependGenerationIdAsFlow(): Flow<Int> {
        return prependLoadIdCh.consumeAsFlow()
            .onStart { prependLoadIdCh.offer(prependLoadId) }
    }

    fun consumeAppendGenerationIdAsFlow(): Flow<Int> {
        return appendLoadIdCh.consumeAsFlow()
            .onStart { appendLoadIdCh.offer(appendLoadId) }
    }

    /**
     * Convert a loaded [Page] into a [PageEvent] for [Pager.pageEventCh].
     *
     * Note: This method should be called after state updated by [insert]
     *
     * TODO: Move this into Pager, which owns pageEventCh, since this logic is sensitive to its
     *  implementation.
     */
    internal fun Page<Key, Value>.toPageEvent(
        loadType: LoadType,
        placeholdersEnabled: Boolean
    ): PageEvent<Value> {
        val sourcePageIndex = when (loadType) {
            REFRESH -> 0
            START -> 0 - initialPageIndex
            END -> pages.size - initialPageIndex - 1
        }
        val pages = listOf(TransformablePage(sourcePageIndex, data, data.size, null))
        return when (loadType) {
            REFRESH -> Refresh(
                pages = pages,
                placeholdersStart = if (placeholdersEnabled) placeholdersStart else 0,
                placeholdersEnd = if (placeholdersEnabled) placeholdersEnd else 0,
                loadStates = loadStates.toMap()
            )
            START -> Start(
                pages = pages,
                placeholdersStart = if (placeholdersEnabled) placeholdersStart else 0,
                loadStates = loadStates.toMap()
            )
            END -> End(
                pages = pages,
                placeholdersEnd = if (placeholdersEnabled) placeholdersEnd else 0,
                loadStates = loadStates.toMap()
            )
        }
    }

    /**
     * @return true if insert was applied, false otherwise.
     */
    fun insert(loadId: Int, loadType: LoadType, page: Page<Key, Value>): Boolean {
        when (loadType) {
            REFRESH -> {
                check(pages.isEmpty()) { "cannot receive multiple init calls" }
                check(loadId == 0) { "init loadId must be the initial value, 0" }

                _pages.add(page)
                initialPageIndex = 0
                placeholdersEnd = if (page.itemsAfter != COUNT_UNDEFINED) page.itemsAfter else 0
                placeholdersStart = if (page.itemsBefore != COUNT_UNDEFINED) page.itemsBefore else 0
            }
            START -> {
                check(pages.isNotEmpty()) { "should've received an init before prepend" }

                // Skip this insert if it is the result of a cancelled job due to page drop
                if (loadId != prependLoadId) return false

                _pages.add(0, page)
                initialPageIndex++
                placeholdersStart = if (page.itemsBefore == COUNT_UNDEFINED) {
                    (placeholdersStart - page.data.size).coerceAtLeast(0)
                } else {
                    page.itemsBefore
                }

                // Clear error on successful insert
                failedHintsByLoadType.remove(START)
            }
            END -> {
                check(pages.isNotEmpty()) { "should've received an init before append" }

                // Skip this insert if it is the result of a cancelled job due to page drop
                if (loadId != appendLoadId) return false

                _pages.add(page)
                placeholdersEnd = if (page.itemsAfter == COUNT_UNDEFINED) {
                    (placeholdersEnd - page.data.size).coerceAtLeast(0)
                } else {
                    page.itemsAfter
                }

                // Clear error on successful insert
                failedHintsByLoadType.remove(END)
            }
        }

        return true
    }

    fun drop(loadType: LoadType, pageCount: Int, placeholdersRemaining: Int) {
        check(pages.size >= pageCount) {
            "invalid drop count. have ${pages.size} but wanted to drop $pageCount"
        }

        loadStates[loadType] = Idle

        when (loadType) {
            START -> {
                repeat(pageCount) { _pages.removeAt(0) }
                initialPageIndex -= pageCount
                this.placeholdersStart = placeholdersRemaining

                prependLoadId++
                prependLoadIdCh.offer(prependLoadId)
            }
            END -> {
                repeat(pageCount) { _pages.removeAt(pages.size - 1) }
                this.placeholdersEnd = placeholdersRemaining

                appendLoadId++
                appendLoadIdCh.offer(appendLoadId)
            }
            else -> throw IllegalArgumentException("cannot drop $loadType")
        }
    }

    fun dropInfo(loadType: LoadType): DropInfo? {
        // Never drop below 2 pages as this can cause UI flickering with certain configs and it's
        // much more important to protect against this behaviour over respecting a config where
        // maxSize is set unusually (probably incorrectly) strict.
        if (pages.size <= 2) return null

        when (loadType) {
            REFRESH -> throw IllegalArgumentException(
                "Drop LoadType must be START or END, but got $loadType"
            )
            START -> {
                // TODO: Incrementally compute this.
                val currentSize = pages.sumBy { it.data.size }
                @Suppress("DEPRECATION")
                if (maxSize != PagedList.Config.MAX_SIZE_UNBOUNDED && currentSize > maxSize) {
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
                @Suppress("DEPRECATION")
                if (maxSize != PagedList.Config.MAX_SIZE_UNBOUNDED && currentSize > maxSize) {
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
     *
     * Note: If an invalid / out-of-date sourcePageIndex is passed, it will be coerced to the
     * closest pageIndex within the range of [pages]
     *
     * TODO: Handle pages.isEmpty (lastIndex returns -1)
     */
    internal suspend fun <T> ViewportHint.withCoercedHint(
        block: suspend (indexInPage: Int, pageIndex: Int, hintOffset: Int) -> T
    ): T {
        var indexInPage = indexInPage
        var pageIndex = sourcePageIndex + initialPageIndex
        var hintOffset = 0

        // Coerce pageIndex to >= 0, snap indexInPage to 0 if pageIndex is coerced.
        if (pageIndex < 0) {
            hintOffset = (pageIndex.absoluteValue - 1) * pageSize

            pageIndex = 0
            indexInPage = 0
        }

        // Reduce indexInPage by incrementing pageIndex while indexInPage is outside the bounds of
        // the page referenced by pageIndex.
        while (pageIndex < pages.lastIndex && indexInPage > pages[pageIndex].data.lastIndex) {
            indexInPage -= pages[pageIndex].data.size
            pageIndex++
        }

        // Coerce pageIndex to <= pages.lastIndex, snap indexInPage to last index if pageIndex is
        // coerced.
        if (pageIndex > pages.lastIndex) {
            val itemsInSkippedPages = (pageIndex - pages.lastIndex - 1) * pageSize
            hintOffset = itemsInSkippedPages + indexInPage + 1

            pageIndex = pages.lastIndex
            indexInPage = pages.lastOrNull()?.data?.lastIndex ?: 0
        }

        return block(indexInPage, pageIndex, hintOffset)
    }
}

internal class DropInfo(val pageCount: Int, val placeholdersRemaining: Int)
