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
import androidx.paging.PagingSource.LoadResult.Page
import java.util.AbstractList

/**
 * Class holding the pages of data backing a [PagedList], presenting sparse loaded data as a List.
 *
 * This class only holds data, and does not have any notion of the ideas of async loads, or
 * prefetching.
 */
internal class PagedStorage<T : Any> :
    AbstractList<T>,
    LegacyPageFetcher.KeyProvider<Any>,
    NullPaddedList<T> {
    private val pages = mutableListOf<Page<*, T>>()

    internal val firstLoadedItem: T
        get() = pages.first().data.first()
    internal val lastLoadedItem: T
        get() = pages.last().data.last()

    override var placeholdersBefore: Int = 0
        private set

    override var placeholdersAfter: Int = 0
        private set

    var positionOffset: Int = 0
        private set

    private var counted = true

    /**
     * Number of loaded items held by [pages].
     */
    override var storageCount: Int = 0
        private set

    /**
     * Last accessed index for loadAround in storage space
     */
    private var lastLoadAroundLocalIndex: Int = 0
    var lastLoadAroundIndex: Int
        get() = placeholdersBefore + lastLoadAroundLocalIndex
        set(value) {
            lastLoadAroundLocalIndex = (value - placeholdersBefore).coerceIn(0, storageCount - 1)
        }

    val middleOfLoadedRange: Int
        get() = placeholdersBefore + storageCount / 2

    constructor()

    constructor(
        leadingNulls: Int,
        page: Page<*, T>,
        trailingNulls: Int
    ) : this() {
        init(leadingNulls, page, trailingNulls, 0, true)
    }

    private constructor(other: PagedStorage<T>) {
        pages.addAll(other.pages)
        placeholdersBefore = other.placeholdersBefore
        placeholdersAfter = other.placeholdersAfter
        positionOffset = other.positionOffset
        counted = other.counted
        storageCount = other.storageCount
        lastLoadAroundLocalIndex = other.lastLoadAroundLocalIndex
    }

    fun snapshot() = PagedStorage(this)

    private fun init(
        leadingNulls: Int,
        page: Page<*, T>,
        trailingNulls: Int,
        positionOffset: Int,
        counted: Boolean
    ) {
        placeholdersBefore = leadingNulls
        pages.clear()
        pages.add(page)
        placeholdersAfter = trailingNulls

        this.positionOffset = positionOffset
        storageCount = page.data.size
        this.counted = counted

        lastLoadAroundLocalIndex = page.data.size / 2
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun init(
        leadingNulls: Int,
        page: Page<*, T>,
        trailingNulls: Int,
        positionOffset: Int,
        callback: Callback,
        counted: Boolean = true
    ) {
        init(leadingNulls, page, trailingNulls, positionOffset, counted)
        callback.onInitialized(size)
    }

    // ------------- Adjacent Provider interface ------------------

    override val prevKey: Any?
        get() = if (!counted || placeholdersBefore + positionOffset > 0) {
            pages.first().prevKey
        } else {
            null
        }

    override val nextKey: Any?
        get() = if (!counted || placeholdersAfter > 0) {
            pages.last().nextKey
        } else {
            null
        }

    /**
     * Traverse to the page and pageInternalIndex of localIndex.
     *
     * Bounds check (between 0 and storageCount) must be performed  before calling this function.
     */
    private inline fun <V> traversePages(
        localIndex: Int,
        crossinline onLastPage: (page: Page<*, T>, pageInternalIndex: Int) -> V
    ): V {
        var localPageIndex = 0
        var pageInternalIndex: Int = localIndex

        // Since we don't know if page sizes are regular, we walk to correct page.
        val localPageCount = pages.size
        while (localPageIndex < localPageCount) {
            val pageSize = pages[localPageIndex].data.size
            if (pageSize > pageInternalIndex) {
                // stop, found the page
                break
            }
            pageInternalIndex -= pageSize
            localPageIndex++
        }
        return onLastPage(pages[localPageIndex], pageInternalIndex)
    }

    /**
     * Walk through the list of pages to find the data at local index
     */
    override fun getFromStorage(localIndex: Int): T =
        traversePages(localIndex) { page, pageInternalIndex ->
            page.data[pageInternalIndex]
        }

    fun getRefreshKeyInfo(@Suppress("DEPRECATION") config: PagedList.Config): PagingState<*, T>? {
        if (pages.isEmpty()) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return PagingState(
            pages = pages.toList() as List<Page<Any, T>>,
            anchorPosition = lastLoadAroundIndex,
            config = PagingConfig(
                config.pageSize,
                config.prefetchDistance,
                config.enablePlaceholders,
                config.initialLoadSizeHint,
                config.maxSize
            ),
            leadingPlaceholderCount = placeholdersBefore
        )
    }

    override fun get(index: Int): T? {
        // is it definitely outside 'pages'?
        val localIndex = index - placeholdersBefore

        return when {
            index < 0 || index >= size ->
                throw IndexOutOfBoundsException("Index: $index, Size: $size")
            localIndex < 0 || localIndex >= storageCount -> null
            else -> getFromStorage(localIndex)
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Callback {
        fun onInitialized(count: Int)
        fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int)
        fun onPageAppended(endPosition: Int, changed: Int, added: Int)
        fun onPagesRemoved(startOfDrops: Int, count: Int)
        fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int)
    }

    override val size
        get() = placeholdersBefore + storageCount + placeholdersAfter

    // ---------------- Trimming API -------------------
    // Trimming is always done at the beginning or end of the list, as content is loaded.
    // In addition to trimming pages in the storage, we also support pre-trimming pages (dropping
    // them just before they're added) to avoid dispatching an add followed immediately by a trim.
    //
    // Note - we avoid trimming down to a single page to reduce chances of dropping page in
    // viewport, since we don't strictly know the viewport. If trim is aggressively set to size of a
    // single page, trimming while the user can see a page boundary is dangerous. To be safe, we
    // just avoid trimming in these cases entirely.

    private fun needsTrim(maxSize: Int, requiredRemaining: Int, localPageIndex: Int): Boolean {
        val page = pages[localPageIndex]
        return storageCount > maxSize &&
            pages.size > 2 &&
            storageCount - page.data.size >= requiredRemaining
    }

    fun needsTrimFromFront(maxSize: Int, requiredRemaining: Int) =
        needsTrim(maxSize, requiredRemaining, 0)

    fun needsTrimFromEnd(maxSize: Int, requiredRemaining: Int) =
        needsTrim(maxSize, requiredRemaining, pages.size - 1)

    fun shouldPreTrimNewPage(maxSize: Int, requiredRemaining: Int, countToBeAdded: Int) =
        storageCount + countToBeAdded > maxSize &&
            pages.size > 1 &&
            storageCount >= requiredRemaining

    internal fun trimFromFront(
        insertNulls: Boolean,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ): Boolean {
        var totalRemoved = 0
        while (needsTrimFromFront(maxSize, requiredRemaining)) {
            val page = pages.removeAt(0)
            val removed = page.data.size
            totalRemoved += removed
            storageCount -= removed
        }
        lastLoadAroundLocalIndex = (lastLoadAroundLocalIndex - totalRemoved).coerceAtLeast(0)

        if (totalRemoved > 0) {
            if (insertNulls) {
                // replace removed items with nulls
                val previousLeadingNulls = placeholdersBefore
                placeholdersBefore += totalRemoved
                callback.onPagesSwappedToPlaceholder(previousLeadingNulls, totalRemoved)
            } else {
                // simply remove, and handle offset
                positionOffset += totalRemoved
                callback.onPagesRemoved(placeholdersBefore, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    internal fun trimFromEnd(
        insertNulls: Boolean,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ): Boolean {
        var totalRemoved = 0
        while (needsTrimFromEnd(maxSize, requiredRemaining)) {
            val page = pages.removeAt(pages.size - 1)
            val removed = page.data.size
            totalRemoved += removed
            storageCount -= removed
        }
        lastLoadAroundLocalIndex = lastLoadAroundLocalIndex.coerceAtMost(storageCount - 1)

        if (totalRemoved > 0) {
            val newEndPosition = placeholdersBefore + storageCount
            if (insertNulls) {
                // replace removed items with nulls
                placeholdersAfter += totalRemoved
                callback.onPagesSwappedToPlaceholder(newEndPosition, totalRemoved)
            } else {
                // items were just removed, signal
                callback.onPagesRemoved(newEndPosition, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    // ---------------- Contiguous API -------------------

    internal fun prependPage(page: Page<*, T>, callback: Callback? = null) {
        val count = page.data.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }

        pages.add(0, page)
        storageCount += count

        val changedCount = minOf(placeholdersBefore, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            placeholdersBefore -= changedCount
        }
        positionOffset -= addedCount
        callback?.onPagePrepended(placeholdersBefore, changedCount, addedCount)
    }

    internal fun appendPage(page: Page<*, T>, callback: Callback? = null) {
        val count = page.data.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }

        pages.add(page)
        storageCount += count

        val changedCount = minOf(placeholdersAfter, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            placeholdersAfter -= changedCount
        }

        callback?.onPageAppended(
            placeholdersBefore + storageCount - count,
            changedCount, addedCount
        )
    }

    override fun toString(): String =
        "leading $placeholdersBefore, storage $storageCount, trailing $placeholdersAfter " +
            pages.joinToString(" ")
}
