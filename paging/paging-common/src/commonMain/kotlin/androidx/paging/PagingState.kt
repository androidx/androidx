/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.IntRange
import androidx.paging.PagingSource.LoadResult.Page

/**
 * Snapshot state of Paging system including the loaded [pages], the last accessed [anchorPosition],
 * and the [config] used.
 */
public class PagingState<Key : Any, Value : Any>
constructor(
    /** Loaded pages of data in the list. */
    public val pages: List<Page<Key, Value>>,
    /**
     * Most recently accessed index in the list, including placeholders.
     *
     * `null` if no access in the [PagingData] has been made yet. E.g., if this snapshot was
     * generated before or during the first load.
     */
    public val anchorPosition: Int?,
    /** [PagingConfig] that was given when initializing the [PagingData] stream. */
    public val config: PagingConfig,
    /**
     * Number of placeholders before the first loaded item if placeholders are enabled, otherwise 0.
     */
    @IntRange(from = 0) private val leadingPlaceholderCount: Int,
) {

    override fun equals(other: Any?): Boolean {
        return other is PagingState<*, *> &&
            pages == other.pages &&
            anchorPosition == other.anchorPosition &&
            config == other.config &&
            leadingPlaceholderCount == other.leadingPlaceholderCount
    }

    override fun hashCode(): Int {
        return pages.hashCode() +
            anchorPosition.hashCode() +
            config.hashCode() +
            leadingPlaceholderCount.hashCode()
    }

    /**
     * Coerces [anchorPosition] to closest loaded value in [pages].
     *
     * This function can be called with [anchorPosition] to fetch the loaded item that is closest to
     * the last accessed index in the list.
     *
     * @param anchorPosition Index in the list, including placeholders.
     * @return The closest loaded [Value] in [pages] to the provided [anchorPosition]. `null` if all
     *   loaded [pages] are empty.
     */
    public fun closestItemToPosition(anchorPosition: Int): Value? {
        if (pages.all { it.data.isEmpty() }) return null

        anchorPositionToPagedIndices(anchorPosition) { pageIndex, index ->
            val firstNonEmptyPage = pages.first { it.data.isNotEmpty() }
            val lastNonEmptyPage = pages.last { it.data.isNotEmpty() }
            return when {
                index < 0 -> firstNonEmptyPage.data.first()
                pageIndex == pages.lastIndex && index > pages.last().data.lastIndex -> {
                    lastNonEmptyPage.data.last()
                }
                else -> pages[pageIndex].data[index]
            }
        }
    }

    /**
     * Coerces [anchorPosition] to closest loaded value in [pages] that matches [predicate].
     *
     * This function can be called with [anchorPosition] to fetch the loaded item that is closest to
     * the last accessed index in the list.
     *
     * This function searches for the matching predicate amongst items that are positioned both
     * before and after the [anchorPosition] with this order of priority:
     * 1. item closest to anchorPosition
     * 2. item before anchorPosition
     *
     * This means that given an anchorPosition of item[10], if item[8] and item[11] both match the
     * predicate, then item[11] will be returned since it is closer to item[10]. If two matching
     * items have the same proximity, then the item that comes first will be returned. So given an
     * anchorPosition of item[10], if item[9] and item[11] both match the predicate, then item[9]
     * will be returned since it comes before item[11].
     *
     * This method should be avoided if possible if used on Lists that do not support random access,
     * otherwise performance will take a big hit.
     *
     * @param anchorPosition Index in the list, including placeholders.
     * @param predicate the predicate that matches target item
     * @return The closest loaded [Value] in [pages] to the provided [anchorPosition] that matches
     *   the [predicate]. `null` if all loaded [pages] are empty or if none of the loaded values
     *   match [predicate]
     */
    public fun closestItemAroundPosition(
        anchorPosition: Int,
        predicate: (value: Value) -> Boolean,
    ): Value? {
        if (pages.all { it.data.isEmpty() }) return null

        anchorPositionToPagedIndices(anchorPosition) { pageIndex, index ->
            val firstNonEmptyPageIndex = pages.indexOfFirst { it.data.isNotEmpty() }
            val lastNonEmptyPageIndex = pages.indexOfLast { it.data.isNotEmpty() }
            var prependComplete = false
            var appendComplete = false

            var prependedPageIndex = -1
            var prependedPage: Page<Key, Value>? = null
            var prependedLocalIndex = -1

            var appendedPageIndex = -1
            var appendedPage: Page<Key, Value>? = null
            var appendedLocalIndex = -1

            when {
                // if anchorPos is in placeholdersBefore, only search towards the right
                // starting from first loaded item
                index < 0 -> {
                    prependComplete = true
                    appendedPageIndex = firstNonEmptyPageIndex
                    appendedPage = pages[firstNonEmptyPageIndex]
                    appendedLocalIndex = 0
                }
                // if anchorPos is in placeholdersAfter, only search towards the left
                // starting from last loaded item
                pageIndex == pages.lastIndex && index > pages.last().data.lastIndex -> {
                    appendComplete = true
                    prependedPageIndex = lastNonEmptyPageIndex
                    prependedPage = pages[lastNonEmptyPageIndex]
                    prependedLocalIndex = prependedPage.data.lastIndex
                }
                // otherwise, search both directions starting from the anchorPos
                else -> {
                    prependedPageIndex = pageIndex
                    prependedPage = pages[pageIndex]
                    prependedLocalIndex = index

                    appendedPageIndex = pageIndex
                    appendedPage = pages[pageIndex]
                    appendedLocalIndex = index
                }
            }

            // on each loop, we move by one index in both directions
            while (!prependComplete || !appendComplete) {
                if (!prependComplete) {
                    // iterate to next page if done with current page
                    while (prependedLocalIndex < 0 && --prependedPageIndex >= 0) {
                        prependedPage = pages[prependedPageIndex]
                        prependedLocalIndex = prependedPage.data.lastIndex
                    }
                    // mark prependComplete if we reached the end of prepended pages
                    if (prependedPageIndex < 0) {
                        prependComplete = true
                    } else {
                        // otherwise check if next item matches predicate
                        val prevItem = prependedPage!!.data[prependedLocalIndex]
                        if (predicate(prevItem)) return prevItem
                        prependedLocalIndex--
                    }
                }

                if (!appendComplete) {
                    // iterate to next page if done with current page
                    while (
                        appendedLocalIndex > appendedPage!!.data.lastIndex &&
                            ++appendedPageIndex <= pages.lastIndex
                    ) {
                        appendedPage = pages[appendedPageIndex]
                        appendedLocalIndex = if (appendedPage.data.isEmpty()) Int.MAX_VALUE else 0
                    }
                    // mark appendComplete if we reached the end of appended pages
                    if (appendedPageIndex > pages.lastIndex) {
                        appendComplete = true
                    } else {
                        // otherwise check if next item matches predicate
                        val nextItem = appendedPage.data[appendedLocalIndex]
                        if (predicate(nextItem)) return nextItem
                        appendedLocalIndex++
                    }
                }
            }
            // no items matching predicate found
            return null
        }
    }

    /**
     * Coerces an index in the list, including placeholders, to closest loaded page in [pages].
     *
     * This function can be called with [anchorPosition] to fetch the loaded page that is closest to
     * the last accessed index in the list.
     *
     * @param anchorPosition Index in the list, including placeholders.
     * @return The closest loaded [Value] in [pages] to the provided [anchorPosition]. `null` if all
     *   loaded [pages] are empty.
     */
    public fun closestPageToPosition(anchorPosition: Int): Page<Key, Value>? {
        if (pages.all { it.data.isEmpty() }) return null

        anchorPositionToPagedIndices(anchorPosition) { pageIndex, index ->
            return when {
                index < 0 -> pages.first()
                else -> pages[pageIndex]
            }
        }
    }

    /**
     * @return `true` if all loaded pages are empty or no pages were loaded when this [PagingState]
     *   was created, `false` otherwise.
     */
    public fun isEmpty(): Boolean = pages.all { it.data.isEmpty() }

    /**
     * @return The first loaded item in the list or `null` if all loaded pages are empty or no pages
     *   were loaded when this [PagingState] was created.
     */
    public fun firstItemOrNull(): Value? {
        return pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
    }

    /**
     * @return The last loaded item in the list or `null` if all loaded pages are empty or no pages
     *   were loaded when this [PagingState] was created.
     */
    public fun lastItemOrNull(): Value? {
        return pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
    }

    override fun toString(): String {
        return "PagingState(pages=$pages, anchorPosition=$anchorPosition, config=$config, " +
            "leadingPlaceholderCount=$leadingPlaceholderCount)"
    }

    internal inline fun <T> anchorPositionToPagedIndices(
        anchorPosition: Int,
        block: (pageIndex: Int, index: Int) -> T,
    ): T {
        var pageIndex = 0
        var index = anchorPosition - leadingPlaceholderCount
        while (pageIndex < pages.lastIndex && index > pages[pageIndex].data.lastIndex) {
            index -= pages[pageIndex].data.size
            pageIndex++
        }

        return block(pageIndex, index)
    }
}
