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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Insert.Companion.EMPTY_REFRESH_LOCAL
import androidx.paging.internal.BUGANIZER_URL

/**
 * Presents post-transform paging data as a list, with list update notifications when PageEvents are
 * dispatched.
 */
internal class PageStore<T : Any>(
    pages: List<TransformablePage<T>>,
    placeholdersBefore: Int,
    placeholdersAfter: Int,
) : PlaceholderPaddedList<T> {
    constructor(
        insertEvent: PageEvent.Insert<T>
    ) : this(
        pages = insertEvent.pages,
        placeholdersBefore = insertEvent.placeholdersBefore,
        placeholdersAfter = insertEvent.placeholdersAfter,
    )

    private val pages: MutableList<TransformablePage<T>> = pages.toMutableList()
    override var dataCount: Int = pages.fullCount()
        private set

    private val originalPageOffsetFirst: Int
        get() = pages.first().originalPageOffsets.minOrNull()!!

    private val originalPageOffsetLast: Int
        get() = pages.last().originalPageOffsets.maxOrNull()!!

    override var placeholdersBefore: Int = placeholdersBefore
        private set

    override var placeholdersAfter: Int = placeholdersAfter
        private set

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    override fun toString(): String {
        val items = List(dataCount) { getItem(it) }.joinToString()
        return "[($placeholdersBefore placeholders), $items, ($placeholdersAfter placeholders)]"
    }

    fun get(index: Int): T? {
        checkIndex(index)

        val localIndex = index - placeholdersBefore
        if (localIndex < 0 || localIndex >= dataCount) {
            return null
        }
        return getItem(localIndex)
    }

    fun snapshot(): ItemSnapshotList<T> {
        return ItemSnapshotList(placeholdersBefore, placeholdersAfter, pages.flatMap { it.data })
    }

    override fun getItem(index: Int): T {
        var pageIndex = 0
        var indexInPage = index

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
        get() = placeholdersBefore + dataCount + placeholdersAfter

    private fun List<TransformablePage<T>>.fullCount() = sumOf { it.data.size }

    fun processEvent(pageEvent: PageEvent<T>): PagingDataEvent<T> {
        return when (pageEvent) {
            is PageEvent.Insert -> insertPage(pageEvent)
            is PageEvent.Drop -> dropPages(pageEvent)
            else ->
                throw IllegalStateException(
                    """Paging received an event to process StaticList or LoadStateUpdate while
                |processing Inserts and Drops. If you see this exception, it is most
                |likely a bug in the library. Please file a bug so we can fix it at:
                |$BUGANIZER_URL"""
                        .trimMargin()
                )
        }
    }

    fun initializeHint(): ViewportHint.Initial {
        val presentedItems = dataCount
        return ViewportHint.Initial(
            presentedItemsBefore = presentedItems / 2,
            presentedItemsAfter = presentedItems / 2,
            originalPageOffsetFirst = originalPageOffsetFirst,
            originalPageOffsetLast = originalPageOffsetLast
        )
    }

    fun accessHintForPresenterIndex(index: Int): ViewportHint.Access {
        var pageIndex = 0
        var indexInPage = index - placeholdersBefore
        while (indexInPage >= pages[pageIndex].data.size && pageIndex < pages.lastIndex) {
            // index doesn't appear in current page, keep looking!
            indexInPage -= pages[pageIndex].data.size
            pageIndex++
        }

        return pages[pageIndex].viewportHintFor(
            index = indexInPage,
            presentedItemsBefore = index - placeholdersBefore,
            presentedItemsAfter = size - index - placeholdersAfter - 1,
            originalPageOffsetFirst = originalPageOffsetFirst,
            originalPageOffsetLast = originalPageOffsetLast
        )
    }

    /**
     * Insert the event's page to the storage and return a [PagingDataEvent] to be dispatched to
     * presenters.
     */
    private fun insertPage(insert: PageEvent.Insert<T>): PagingDataEvent<T> {
        val insertSize = insert.pages.fullCount()
        return when (insert.loadType) {
            REFRESH ->
                throw IllegalStateException(
                    """Paging received a refresh event in the middle of an actively loading generation
                |of PagingData. If you see this exception, it is most likely a bug in the library.
                |Please file a bug so we can fix it at:
                |$BUGANIZER_URL"""
                        .trimMargin()
                )
            PREPEND -> {
                val oldPlaceholdersBefore = placeholdersBefore
                // update all states
                pages.addAll(0, insert.pages)
                dataCount += insertSize
                placeholdersBefore = insert.placeholdersBefore

                PagingDataEvent.Prepend(
                    inserted = insert.pages.flatMap { it.data },
                    newPlaceholdersBefore = placeholdersBefore,
                    oldPlaceholdersBefore = oldPlaceholdersBefore
                )
            }
            APPEND -> {
                val oldPlaceholdersAfter = placeholdersAfter
                val oldDataCount = dataCount
                // update all states
                pages.addAll(pages.size, insert.pages)
                dataCount += insertSize
                placeholdersAfter = insert.placeholdersAfter

                PagingDataEvent.Append(
                    startIndex = placeholdersBefore + oldDataCount,
                    inserted = insert.pages.flatMap { it.data },
                    newPlaceholdersAfter = placeholdersAfter,
                    oldPlaceholdersAfter = oldPlaceholdersAfter
                )
            }
        }
    }

    /**
     * @param pageOffsetsToDrop originalPageOffset of pages that were dropped
     * @return The number of items dropped
     */
    private fun dropPagesWithOffsets(pageOffsetsToDrop: IntRange): Int {
        var removeCount = 0
        val pageIterator = pages.iterator()
        while (pageIterator.hasNext()) {
            val page = pageIterator.next()
            if (page.originalPageOffsets.any { pageOffsetsToDrop.contains(it) }) {
                removeCount += page.data.size
                pageIterator.remove()
            }
        }

        return removeCount
    }

    /**
     * Helper which converts a [PageEvent.Drop] to a [PagingDataEvent] by dropping all pages that
     * depend on the n-lowest or n-highest originalPageOffsets.
     */
    private fun dropPages(drop: PageEvent.Drop<T>): PagingDataEvent<T> {
        // update states
        val itemDropCount = dropPagesWithOffsets(drop.minPageOffset..drop.maxPageOffset)
        dataCount -= itemDropCount

        return if (drop.loadType == PREPEND) {
            val oldPlaceholdersBefore = placeholdersBefore
            placeholdersBefore = drop.placeholdersRemaining

            PagingDataEvent.DropPrepend(
                dropCount = itemDropCount,
                newPlaceholdersBefore = placeholdersBefore,
                oldPlaceholdersBefore = oldPlaceholdersBefore,
            )
        } else {
            val oldPlaceholdersAfter = placeholdersAfter
            placeholdersAfter = drop.placeholdersRemaining

            PagingDataEvent.DropAppend(
                startIndex = placeholdersBefore + dataCount,
                dropCount = itemDropCount,
                newPlaceholdersAfter = drop.placeholdersRemaining,
                oldPlaceholdersAfter = oldPlaceholdersAfter,
            )
        }
    }

    internal companion object {
        // TODO(b/205350267): Replace this with a static list that does not emit CombinedLoadStates.
        private val INITIAL = PageStore(EMPTY_REFRESH_LOCAL)

        @Suppress("UNCHECKED_CAST", "SyntheticAccessor")
        internal fun <T : Any> initial(event: PageEvent.Insert<T>?): PageStore<T> =
            if (event != null) {
                PageStore(event)
            } else {
                INITIAL as PageStore<T>
            }
    }
}
