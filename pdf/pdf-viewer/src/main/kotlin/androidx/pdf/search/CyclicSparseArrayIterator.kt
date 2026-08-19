/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.search

import android.util.SparseArray
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.search.model.QueryResultsIndex
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A cyclic iterator implementation over SparseArray.
 *
 * @param visiblePage current visible page to the user, used to init current result.
 */
internal class CyclicSparseArrayIterator(private val visiblePage: Int) {
    private val lock = ReentrantLock()

    /** The search data over which [CyclicSparseArrayIterator] will iterate. */
    private val searchData: SparseArray<List<PageMatchBounds>> = SparseArray()

    /** Page number of the currently selected result in [searchData]. */
    private var currentPageNum: Int = -1

    /** Index of result selected on current page. */
    private var searchIndexOnPage: Int = 0

    private val matchedPagesCount: Int
        get() = searchData.size()

    /** Get the current state of selected search result. */
    fun current(): QueryResultsIndex =
        lock.withLock {
            QueryResultsIndex(pageNum = currentPageNum, resultBoundsIndex = searchIndexOnPage)
        }

    /** Move to the next element in the current page, or to the next page cyclically. */
    fun next(): QueryResultsIndex =
        lock.withLock {
            val pageNumIndex = searchData.indexOfKey(currentPageNum)
            val resultsOnPage = searchData.valueAt(pageNumIndex)

            // Move to the next result in the current page
            searchIndexOnPage = (searchIndexOnPage + 1) % resultsOnPage.size

            // If we're at the end of the current page, move to the next page
            if (searchIndexOnPage == 0) {
                val nextPageIndex = (pageNumIndex + 1) % matchedPagesCount
                currentPageNum = searchData.keyAt(nextPageIndex)
            }

            current()
        }

    /** Move to the previous element in the page list, or to the previous page cyclically. */
    fun prev(): QueryResultsIndex =
        lock.withLock {
            val pageNumIndex = searchData.indexOfKey(currentPageNum)
            val resultsOnPage = searchData.valueAt(pageNumIndex)

            // Move to the previous item in the current page
            searchIndexOnPage = (searchIndexOnPage - 1 + resultsOnPage.size) % resultsOnPage.size

            // If we're at the beginning of the current page, move to the previous page
            if (searchIndexOnPage == resultsOnPage.size - 1) {
                val prevPageIndex = (pageNumIndex - 1 + matchedPagesCount) % matchedPagesCount
                currentPageNum = searchData.keyAt(prevPageIndex)
                // update the search index of page to last result on updated page
                searchIndexOnPage = searchData.valueAt(prevPageIndex).lastIndex
            }

            current()
        }

    /**
     * Moves the [searchIndexOnPage] to the provided index on the current page. This can be utilized
     * in scenarios where restoring the current result is needed.
     *
     * @throws [IndexOutOfBoundsException] if the provided index is outside the bounds of the
     *   results on the current page.
     */
    fun moveToIndex(index: Int) =
        lock.withLock {
            val pageNumIndex = searchData.indexOfKey(currentPageNum)
            val resultSizeOnCurrentPage = searchData.valueAt(pageNumIndex).size
            if (index !in 0 until resultSizeOnCurrentPage)
                throw IndexOutOfBoundsException(
                    "Provided index is out of range in selected page results."
                )

            searchIndexOnPage = index
        }

    /**
     * Adds search results for a page and initializes the selected result index if this is the first
     * match found.
     */
    fun addMatches(pageNum: Int, matches: List<PageMatchBounds>) =
        lock.withLock {
            val wasEmpty = searchData.size() == 0
            searchData.put(pageNum, matches)

            if (wasEmpty) {
                currentPageNum = findClosestMatchPageNum(visiblePage)
                searchIndexOnPage = 0
            }
        }

    /** Returns a shallow copy of the accumulated search results. */
    fun cloneData(): SparseArray<List<PageMatchBounds>> = lock.withLock { searchData.clone() }

    /**
     * Find the closest page with search results from current visible page in forward direction.
     *
     * @param currentPageNum current visible page.
     * @return the page number of the closest match.
     */
    private fun findClosestMatchPageNum(currentPageNum: Int): Int {
        val searchIndex = searchData.indexOfKey(currentPageNum)
        if (searchIndex >= 0) return currentPageNum

        // searchIndex is negative, matchIndex is its bitwise inversion.
        val matchIndex = searchIndex.inv()
        val validMatchIndex = if (matchIndex >= matchedPagesCount) 0 else matchIndex
        return searchData.keyAt(validMatchIndex)
    }
}
