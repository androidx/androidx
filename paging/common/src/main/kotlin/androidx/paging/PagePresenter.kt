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

import androidx.paging.PagedList.Callback

/**
 * Presents post-transform paging data as a list, with PagedList.Callback list update notifications
 * when modification events occur.
 *
 * Provides the following data operations:
 *  - Insert Page
 *  - Drop Pages
 *  - Snapshot
 *
 *  TODO:
 *   - state observation APIs
 */
internal class PagePresenter<T : Any> {
    private val pages: MutableList<TransformedPage<T>>
    private var storageCount: Int

    private var placeholdersBefore: Int
    private var placeholdersAfter: Int

    constructor(insertEvent: PageEvent.Insert<T>) {
        pages = insertEvent.pages.toMutableList()
        storageCount = insertEvent.pages.fullCount()
        placeholdersBefore = insertEvent.placeholdersBefore
        placeholdersAfter = insertEvent.placeholdersAfter
    }

    private constructor(other: PagePresenter<T>) {
        pages = other.pages.toMutableList()
        storageCount = other.storageCount
        placeholdersBefore = other.placeholdersBefore
        placeholdersAfter = other.placeholdersAfter
    }

    fun copy(): PagePresenter<T> = PagePresenter(this)

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    fun get(index: Int): T? {
        checkIndex(index)

        val localIndex = index - placeholdersBefore
        if (localIndex < 0 || localIndex >= storageCount) {
            return null
        }

        var pageIndex = 0
        var indexInPage: Int

        // Since we don't know if page sizes are regular, we walk to correct page.
        indexInPage = localIndex
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

    /**
     * For a given index location, returns a LoadHint reporting the nearest page and index.
     */
    fun loadAround(index: Int): LoadHint {
        checkIndex(index)

        var pageIndex = 0
        var indexInPage = index - placeholdersBefore
        while (indexInPage >= pages[pageIndex].data.size && pageIndex < pages.lastIndex) {
            // index doesn't appear in current page, keep looking!
            indexInPage -= pages[pageIndex].data.size
            pageIndex++
        }
        return pages[pageIndex].getLoadHint(indexInPage)
    }

    val size: Int
        get() = placeholdersBefore + storageCount + placeholdersAfter

    val loadedCount: Int
        get() = storageCount

    private fun List<TransformedPage<T>>.fullCount() = sumBy { it.data.size }

    fun processEvent(pageEvent: PageEvent<T>, callback: Callback) {
        when (pageEvent) {
            is PageEvent.Insert -> insertPage(pageEvent, callback)
            is PageEvent.Drop -> dropPages(pageEvent, callback)
            is PageEvent.StateUpdate -> TODO()
        }
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
    private fun insertPage(insert: PageEvent.Insert<T>, callback: Callback) {
        val count = insert.pages.fullCount()
        val oldSize = size
        when (insert.loadType) {
            LoadType.REFRESH -> throw IllegalArgumentException()
            LoadType.START -> {
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
            LoadType.END -> {
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
                    callback.onInserted(size - placeholderInsertedCount, placeholderInsertedCount)
                } else if (placeholderInsertedCount < 0) {
                    callback.onRemoved(size, -placeholderInsertedCount)
                }
            }
        }
    }

    private fun dropPages(drop: PageEvent.Drop<T>, callback: Callback) {
        val oldSize = size
        if (drop.loadType == LoadType.START) {
            val removeCount = pages.take(drop.count).fullCount()

            val placeholdersChangedCount = minOf(drop.placeholdersRemaining, removeCount)
            val placeholdersChangedPos = placeholdersBefore + removeCount - placeholdersChangedCount

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
        }
    }
}