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
import java.util.AbstractList

/**
 * Class holding the pages of data backing a [PagedList], presenting sparse loaded data as a List.
 *
 * This class only holds data, and does not have any notion of the ideas of async loads, or
 * prefetching.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class PagedStorage<T : Any> : AbstractList<T>, Pager.AdjacentProvider<T> {
    private val pages: ArrayList<List<T>>

    var leadingNullCount: Int = 0
        private set

    var trailingNullCount: Int = 0
        private set

    var positionOffset: Int = 0
        private set
    /**
     * Number of loaded items held by [pages].
     */
    var storageCount: Int = 0
        private set

    var numberPrepended: Int = 0
        private set
    var numberAppended: Int = 0
        private set

    val middleOfLoadedRange
        get() = leadingNullCount + positionOffset + storageCount / 2

    // ------------- Adjacent Provider interface ------------------

    override val firstLoadedItem
        // guaranteed to have pages, and every page is non-empty, so this is safe
        get() = pages.first().first()

    override val lastLoadedItem
        // guaranteed to have pages, and every page is non-empty, so this is safe
        get() = pages.last().last()

    override val firstLoadedItemIndex
        get() = leadingNullCount + positionOffset

    override val lastLoadedItemIndex
        get() = leadingNullCount + storageCount - 1 + positionOffset

    constructor() {
        leadingNullCount = 0
        pages = ArrayList()
        trailingNullCount = 0
        positionOffset = 0
        storageCount = 0
        numberPrepended = 0
        numberAppended = 0
    }

    constructor(leadingNulls: Int, page: List<T>, trailingNulls: Int) : this() {
        init(leadingNulls, page, trailingNulls, 0)
    }

    private constructor(other: PagedStorage<T>) {
        leadingNullCount = other.leadingNullCount
        pages = ArrayList(other.pages)
        trailingNullCount = other.trailingNullCount
        positionOffset = other.positionOffset
        storageCount = other.storageCount
        numberPrepended = other.numberPrepended
        numberAppended = other.numberAppended
    }

    fun snapshot() = PagedStorage(this)

    private fun init(leadingNulls: Int, page: List<T>, trailingNulls: Int, positionOffset: Int) {
        leadingNullCount = leadingNulls
        pages.clear()
        pages.add(page)
        trailingNullCount = trailingNulls

        this.positionOffset = positionOffset
        storageCount = page.size

        numberPrepended = 0
        numberAppended = 0
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun init(
        leadingNulls: Int,
        page: List<T>,
        trailingNulls: Int,
        positionOffset: Int,
        callback: Callback
    ) {
        init(leadingNulls, page, trailingNulls, positionOffset)
        callback.onInitialized(size)
    }

    override fun get(index: Int): T? {
        // is it definitely outside 'pages'?
        val localIndex = index - leadingNullCount

        when {
            index < 0 || index >= size ->
                throw IndexOutOfBoundsException("Index: $index, Size: $size")
            localIndex < 0 || localIndex >= storageCount -> return null
        }

        var localPageIndex = 0
        var pageInternalIndex: Int

        // Since we don't know if page sizes are regular, we walk to correct page.
        pageInternalIndex = localIndex
        val localPageCount = pages.size
        while (localPageIndex < localPageCount) {
            val pageSize = pages[localPageIndex].size
            if (pageSize > pageInternalIndex) {
                // stop, found the page
                break
            }
            pageInternalIndex -= pageSize
            localPageIndex++
        }
        return pages[localPageIndex][pageInternalIndex]
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Callback {
        fun onInitialized(count: Int)
        fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int)
        fun onPageAppended(endPosition: Int, changed: Int, added: Int)
        fun onPagePlaceholderInserted(pageIndex: Int)
        fun onPageInserted(start: Int, count: Int)
        fun onPagesRemoved(startOfDrops: Int, count: Int)
        fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int)
    }

    override val size
        get() = leadingNullCount + storageCount + trailingNullCount

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
                storageCount - page.size >= requiredRemaining
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
            val removed = page.size
            totalRemoved += removed
            storageCount -= removed
        }

        if (totalRemoved > 0) {
            if (insertNulls) {
                // replace removed items with nulls
                val previousLeadingNulls = leadingNullCount
                leadingNullCount += totalRemoved
                callback.onPagesSwappedToPlaceholder(previousLeadingNulls, totalRemoved)
            } else {
                // simply remove, and handle offset
                positionOffset += totalRemoved
                callback.onPagesRemoved(leadingNullCount, totalRemoved)
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
            val removed = page.size
            totalRemoved += removed
            storageCount -= removed
        }

        if (totalRemoved > 0) {
            val newEndPosition = leadingNullCount + storageCount
            if (insertNulls) {
                // replace removed items with nulls
                trailingNullCount += totalRemoved
                callback.onPagesSwappedToPlaceholder(newEndPosition, totalRemoved)
            } else {
                // items were just removed, signal
                callback.onPagesRemoved(newEndPosition, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    // ---------------- Contiguous API -------------------

    internal fun prependPage(page: List<T>, callback: Callback) {
        val count = page.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }

        pages.add(0, page)
        storageCount += count

        val changedCount = minOf(leadingNullCount, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            leadingNullCount -= changedCount
        }
        positionOffset -= addedCount
        numberPrepended += count

        callback.onPagePrepended(leadingNullCount, changedCount, addedCount)
    }

    internal fun appendPage(page: List<T>, callback: Callback) {
        val count = page.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }

        pages.add(page)
        storageCount += count

        val changedCount = minOf(trailingNullCount, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            trailingNullCount -= changedCount
        }
        numberAppended += count
        callback.onPageAppended(
            leadingNullCount + storageCount - count,
            changedCount, addedCount
        )
    }

    override fun onPageResultResolution(
        type: PagedList.LoadType,
        result: DataSource.BaseResult<T>
    ) {
        // ignored
    }

    override fun toString(): String =
        "leading $leadingNullCount, storage $storageCount, trailing $trailingNullCount " +
                pages.joinToString(" ")
}
