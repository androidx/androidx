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

import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH

/**
 * Presents post-transform paging data as a list, with list update notifications when
 * PageEvents are dispatched.
 */
internal class PagePresenter<T : Any>(
    insertEvent: PageEvent.Insert<T>
) : NullPaddedList<T> {
    private val pages: MutableList<TransformablePage<T>> = insertEvent.pages.toMutableList()
    override var storageCount: Int = insertEvent.pages.fullCount()
        private set

    val firstPageIndex: Int
        get() = pages.first().originalPageOffset
    val lastPageIndex: Int
        get() = pages.last().originalPageOffset
    override var placeholdersBefore: Int = insertEvent.placeholdersBefore
        private set
    override var placeholdersAfter: Int = insertEvent.placeholdersAfter
        private set

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    override fun toString(): String {
        val items = List(storageCount) { getFromStorage(it) }.joinToString()
        return "[($placeholdersBefore placeholders), $items, ($placeholdersAfter placeholders)]"
    }

    fun get(index: Int): T? {
        checkIndex(index)

        val localIndex = index - placeholdersBefore
        if (localIndex < 0 || localIndex >= storageCount) {
            return null
        }
        return getFromStorage(localIndex)
    }

    fun snapshot(): ItemSnapshotList<T> {
        return ItemSnapshotList(
            placeholdersBefore,
            placeholdersAfter,
            pages.flatMap { it.data }
        )
    }

    override fun getFromStorage(localIndex: Int): T {
        var pageIndex = 0
        var indexInPage = localIndex

        // Since we don't know if page sizes are regular, we walk to correct page.
        val localPageCount = pages.size
        while (pageIndex < localPageCount) {
            val pageSize = pages[pageIndex].data.size
            if (pageSize > indexInPage) {
                // stop, found the page
                break
            }
            indexInPage -= pageSize
            pageIndex++
        }
        return pages[pageIndex].data[indexInPage]
    }

    override val size: Int
        get() = placeholdersBefore + storageCount + placeholdersAfter

    private fun List<TransformablePage<T>>.fullCount() = sumBy { it.data.size }

    fun processEvent(pageEvent: PageEvent<T>, callback: ProcessPageEventCallback) {
        when (pageEvent) {
            is PageEvent.Insert -> insertPage(pageEvent, callback)
            is PageEvent.Drop -> dropPages(pageEvent, callback)
            is PageEvent.LoadStateUpdate -> {
                callback.onStateUpdate(
                    loadType = pageEvent.loadType,
                    fromMediator = pageEvent.fromMediator,
                    loadState = pageEvent.loadState
                )
            }
        }
    }

    fun presenterIndexToHint(index: Int): ViewportHint {
        var pageIndex = 0
        var indexInPage = index - placeholdersBefore
        while (indexInPage >= pages[pageIndex].data.size && pageIndex < pages.lastIndex) {
            // index doesn't appear in current page, keep looking!
            indexInPage -= pages[pageIndex].data.size
            pageIndex++
        }

        val originalIndices = pages[pageIndex].originalIndices
        return ViewportHint(
            pageOffset = pages[pageIndex].originalPageOffset,
            indexInPage = if (originalIndices != null && indexInPage in originalIndices.indices) {
                originalIndices[indexInPage]
            } else {
                indexInPage
            },
            presentedItemsBefore = index - placeholdersBefore,
            presentedItemsAfter = size - index - placeholdersAfter - 1,
            originalPageOffsetFirst = firstPageIndex,
            originalPageOffsetLast = lastPageIndex

        )
    }

    /**
     * Insert the event's page to the presentation list, and dispatch associated callbacks for
     * change (placeholder becomes real item) or insert (real item is appended).
     *
     * For each insert (or removal) there are three potential events:
     *
     * 1) change
     *     this covers any placeholder/item conversions, and is done first
     *
     * 2) item insert/remove
     *     this covers any remaining items that are inserted/removed, but aren't swapping with
     *     placeholders
     *
     * 3) placeholder insert/remove
     *     after the above, placeholder count can be wrong for a number of reasons - approximate
     *     counting or filtering are the most common. In either case, we adjust placeholders at
     *     the far end of the list, so that they don't trigger animations near the user.
     */
    private fun insertPage(insert: PageEvent.Insert<T>, callback: ProcessPageEventCallback) {
        val count = insert.pages.fullCount()
        val oldSize = size
        when (insert.loadType) {
            REFRESH -> throw IllegalArgumentException()
            PREPEND -> {
                val placeholdersChangedCount = minOf(placeholdersBefore, count)
                val placeholdersChangedPos = placeholdersBefore - placeholdersChangedCount

                val itemsInsertedCount = count - placeholdersChangedCount
                val itemsInsertedPos = 0

                // first update all state...
                pages.addAll(0, insert.pages)
                storageCount += count
                placeholdersBefore = insert.placeholdersBefore

                // ... then trigger callbacks, so callbacks won't see inconsistent state
                callback.onChanged(placeholdersChangedPos, placeholdersChangedCount)
                callback.onInserted(itemsInsertedPos, itemsInsertedCount)
                val placeholderInsertedCount = size - oldSize - itemsInsertedCount
                if (placeholderInsertedCount > 0) {
                    callback.onInserted(0, placeholderInsertedCount)
                } else if (placeholderInsertedCount < 0) {
                    callback.onRemoved(0, -placeholderInsertedCount)
                }
            }
            APPEND -> {
                val placeholdersChangedCount = minOf(placeholdersAfter, count)
                val placeholdersChangedPos = placeholdersBefore + storageCount

                val itemsInsertedCount = count - placeholdersChangedCount
                val itemsInsertedPos = placeholdersChangedPos + placeholdersChangedCount

                // first update all state...
                pages.addAll(pages.size, insert.pages)
                storageCount += count
                placeholdersAfter = insert.placeholdersAfter

                // ... then trigger callbacks, so callbacks won't see inconsistent state
                callback.onChanged(placeholdersChangedPos, placeholdersChangedCount)
                callback.onInserted(itemsInsertedPos, itemsInsertedCount)
                val placeholderInsertedCount = size - oldSize - itemsInsertedCount
                if (placeholderInsertedCount > 0) {
                    callback.onInserted(
                        position = size - placeholderInsertedCount,
                        count = placeholderInsertedCount
                    )
                } else if (placeholderInsertedCount < 0) {
                    callback.onRemoved(size, -placeholderInsertedCount)
                }
            }
        }
        insert.combinedLoadStates.forEach { type, fromMediator, state ->
            callback.onStateUpdate(type, fromMediator, state)
        }
    }

    private fun dropPages(drop: PageEvent.Drop<T>, callback: ProcessPageEventCallback) {
        val oldSize = size
        if (drop.loadType == PREPEND) {
            val removeCount = pages.take(drop.count).fullCount()

            val placeholdersChangedCount = minOf(drop.placeholdersRemaining, removeCount)
            val placeholdersChangedPos = placeholdersBefore + removeCount -
                    placeholdersChangedCount

            val itemsRemovedCount = removeCount - placeholdersChangedCount
            val itemsRemovedPos = 0

            // first update all state...
            for (i in 0 until drop.count) {
                pages.removeAt(0)
            }
            storageCount -= removeCount
            placeholdersBefore = drop.placeholdersRemaining

            // ... then trigger callbacks, so callbacks won't see inconsistent state
            callback.onChanged(placeholdersChangedPos, placeholdersChangedCount)
            callback.onRemoved(itemsRemovedPos, itemsRemovedCount)
            val placeholderInsertedCount = size - oldSize + itemsRemovedCount
            if (placeholderInsertedCount > 0) {
                callback.onInserted(0, placeholderInsertedCount)
            } else if (placeholderInsertedCount < 0) {
                callback.onRemoved(0, -placeholderInsertedCount)
            }

            // Dropping from prepend direction implies NotLoading(endOfPaginationReached = false).
            callback.onStateUpdate(
                loadType = PREPEND,
                fromMediator = false,
                loadState = NotLoading.Incomplete
            )
        } else {
            val removeCount = pages.takeLast(drop.count).fullCount()

            val placeholdersChangedCount = minOf(drop.placeholdersRemaining, removeCount)
            val placeholdersChangedPos = placeholdersBefore + storageCount - removeCount

            val itemsRemovedCount = removeCount - placeholdersChangedCount
            val itemsRemovedPos = placeholdersChangedPos + placeholdersChangedCount

            // first update all state...
            for (i in 0 until drop.count) {
                pages.removeAt(pages.lastIndex)
            }
            storageCount -= removeCount
            placeholdersAfter = drop.placeholdersRemaining

            // ... then trigger callbacks, so callbacks won't see inconsistent state
            callback.onChanged(placeholdersChangedPos, placeholdersChangedCount)
            callback.onRemoved(itemsRemovedPos, itemsRemovedCount)
            val placeholderInsertedCount = size - oldSize + itemsRemovedCount
            if (placeholderInsertedCount > 0) {
                callback.onInserted(size, placeholderInsertedCount)
            } else if (placeholderInsertedCount < 0) {
                callback.onRemoved(size, -placeholderInsertedCount)
            }

            // Dropping from append direction implies NotLoading(endOfPaginationReached = false).
            callback.onStateUpdate(
                loadType = APPEND,
                fromMediator = false,
                loadState = NotLoading.Incomplete
            )
        }
    }

    internal companion object {
        private val INITIAL = PagePresenter<Any>(PageEvent.Insert.EMPTY_REFRESH_LOCAL)

        @Suppress("UNCHECKED_CAST", "SyntheticAccessor")
        internal fun <T : Any> initial(): PagePresenter<T> = INITIAL as PagePresenter<T>
    }

    /**
     * Callback to communicate events from [PagePresenter] to [PagingDataDiffer]
     */
    internal interface ProcessPageEventCallback {
        fun onChanged(position: Int, count: Int)
        fun onInserted(position: Int, count: Int)
        fun onRemoved(position: Int, count: Int)
        fun onStateUpdate(loadType: LoadType, fromMediator: Boolean, loadState: LoadState)
    }
}
