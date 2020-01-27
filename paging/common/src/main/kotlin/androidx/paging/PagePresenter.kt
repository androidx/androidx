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

import androidx.annotation.RestrictTo
import androidx.paging.LoadState.Done
import androidx.paging.LoadState.Idle
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Insert.Companion.Refresh

/**
 * Callbacks for the presenter/adapter to listen to the state of pagination data.
 *
 * Note that these won't map directly to PageEvents, since PageEvents can cause several adapter
 * events that should all be dispatched to the presentation layer at once - as part of the same
 * frame.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PresenterCallback {
    fun onChanged(position: Int, count: Int)
    fun onInserted(position: Int, count: Int)
    fun onRemoved(position: Int, count: Int)
    fun onStateUpdate(loadType: LoadType, loadState: LoadState)
}

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

    override var placeholdersStart: Int = insertEvent.placeholdersStart
        private set
    override var placeholdersEnd: Int = insertEvent.placeholdersEnd
        private set

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    override fun toString(): String {
        val items = List(storageCount) { getFromStorage(it) }.joinToString()
        return "[($placeholdersStart placeholders), $items, ($placeholdersEnd placeholders)]"
    }

    fun get(index: Int): T? {
        checkIndex(index)

        val localIndex = index - placeholdersStart
        if (localIndex < 0 || localIndex >= storageCount) {
            return null
        }
        return getFromStorage(localIndex)
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

    /**
     * For a given index location, returns a ViewportHint reporting the nearest page and index.
     */
    fun loadAround(index: Int): ViewportHint {
        checkIndex(index)

        var pageIndex = 0
        var indexInPage = index - placeholdersStart
        while (indexInPage >= pages[pageIndex].data.size && pageIndex < pages.lastIndex) {
            // index doesn't appear in current page, keep looking!
            indexInPage -= pages[pageIndex].data.size
            pageIndex++
        }
        return pages[pageIndex].getLoadHint(indexInPage)
    }

    override val size: Int
        get() = placeholdersStart + storageCount + placeholdersEnd

    val loadedCount: Int
        get() = storageCount

    private fun List<TransformablePage<T>>.fullCount() = sumBy { it.data.size }

    fun processEvent(pageEvent: PageEvent<T>, callback: PresenterCallback) {
        when (pageEvent) {
            is PageEvent.Insert -> insertPage(pageEvent, callback)
            is PageEvent.Drop -> dropPages(pageEvent, callback)
            is PageEvent.LoadStateUpdate -> {
                callback.onStateUpdate(pageEvent.loadType, pageEvent.loadState)
            }
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
    private fun insertPage(insert: PageEvent.Insert<T>, callback: PresenterCallback) {
        val count = insert.pages.fullCount()
        val oldSize = size
        when (insert.loadType) {
            REFRESH -> throw IllegalArgumentException()
            START -> {
                val placeholdersChangedCount = minOf(placeholdersStart, count)
                val placeholdersChangedPos = placeholdersStart - placeholdersChangedCount

                val itemsInsertedCount = count - placeholdersChangedCount
                val itemsInsertedPos = 0

                // first update all state...
                pages.addAll(0, insert.pages)
                storageCount += count
                placeholdersStart = insert.placeholdersStart

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
            END -> {
                val placeholdersChangedCount = minOf(placeholdersEnd, count)
                val placeholdersChangedPos = placeholdersStart + storageCount

                val itemsInsertedCount = count - placeholdersChangedCount
                val itemsInsertedPos = placeholdersChangedPos + placeholdersChangedCount

                // first update all state...
                pages.addAll(pages.size, insert.pages)
                storageCount += count
                placeholdersEnd = insert.placeholdersEnd

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
        insert.loadStates.entries.forEach { callback.onStateUpdate(it.key, it.value) }
    }

    private fun dropPages(drop: PageEvent.Drop<T>, callback: PresenterCallback) {
        val oldSize = size
        if (drop.loadType == START) {
            val removeCount = pages.take(drop.count).fullCount()

            val placeholdersChangedCount = minOf(drop.placeholdersRemaining, removeCount)
            val placeholdersChangedPos = placeholdersStart + removeCount - placeholdersChangedCount

            val itemsRemovedCount = removeCount - placeholdersChangedCount
            val itemsRemovedPos = 0

            // first update all state...
            for (i in 0 until drop.count) {
                pages.removeAt(0)
            }
            storageCount -= removeCount
            placeholdersStart = drop.placeholdersRemaining

            // ... then trigger callbacks, so callbacks won't see inconsistent state
            callback.onChanged(placeholdersChangedPos, placeholdersChangedCount)
            callback.onRemoved(itemsRemovedPos, itemsRemovedCount)
            val placeholderInsertedCount = size - oldSize + itemsRemovedCount
            if (placeholderInsertedCount > 0) {
                callback.onInserted(0, placeholderInsertedCount)
            } else if (placeholderInsertedCount < 0) {
                callback.onRemoved(0, -placeholderInsertedCount)
            }

            // dropping from start implies start is idle
            callback.onStateUpdate(START, Idle)
        } else {
            val removeCount = pages.takeLast(drop.count).fullCount()

            val placeholdersChangedCount = minOf(drop.placeholdersRemaining, removeCount)
            val placeholdersChangedPos = placeholdersStart + storageCount - removeCount

            val itemsRemovedCount = removeCount - placeholdersChangedCount
            val itemsRemovedPos = placeholdersChangedPos + placeholdersChangedCount

            // first update all state...
            for (i in 0 until drop.count) {
                pages.removeAt(pages.lastIndex)
            }
            storageCount -= removeCount
            placeholdersEnd = drop.placeholdersRemaining

            // ... then trigger callbacks, so callbacks won't see inconsistent state
            callback.onChanged(placeholdersChangedPos, placeholdersChangedCount)
            callback.onRemoved(itemsRemovedPos, itemsRemovedCount)
            val placeholderInsertedCount = size - oldSize + itemsRemovedCount
            if (placeholderInsertedCount > 0) {
                callback.onInserted(size, placeholderInsertedCount)
            } else if (placeholderInsertedCount < 0) {
                callback.onRemoved(size, -placeholderInsertedCount)
            }

            // dropping from end implies end is idle
            callback.onStateUpdate(END, Idle)
        }
    }

    internal companion object {
        private val INITIAL = PagePresenter<Any>(
            Refresh(listOf(), 0, 0, mapOf(REFRESH to Idle, START to Done, END to Done))
        )

        @Suppress("UNCHECKED_CAST", "SyntheticAccessor")
        internal fun <T : Any> initial(): PagePresenter<T> = INITIAL as PagePresenter<T>
    }
}