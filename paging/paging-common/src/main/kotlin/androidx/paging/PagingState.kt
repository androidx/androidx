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
public class PagingState<Key : Any, Value : Any> constructor(
    /**
     * Loaded pages of data in the list.
     */
    public val pages: List<Page<Key, Value>>,
    /**
     * Most recently accessed index in the list, including placeholders.
     *
     * `null` if no access in the [PagingData] has been made yet. E.g., if this snapshot was
     * generated before or during the first load.
     */
    public val anchorPosition: Int?,
    /**
     * [PagingConfig] that was given when initializing the [PagingData] stream.
     */
    public val config: PagingConfig,
    /**
     * Number of placeholders before the first loaded item if placeholders are enabled, otherwise 0.
     */
    @IntRange(from = 0)
    private val leadingPlaceholderCount: Int
) {

    override fun equals(other: Any?): Boolean {
        return other is PagingState<*, *> &&
            pages == other.pages &&
            anchorPosition == other.anchorPosition &&
            config == other.config &&
            leadingPlaceholderCount == other.leadingPlaceholderCount
    }

    override fun hashCode(): Int {
        return pages.hashCode() + anchorPosition.hashCode() + config.hashCode() +
            leadingPlaceholderCount.hashCode()
    }

    /**
     * Coerces [anchorPosition] to closest loaded value in [pages].
     *
     * This function can be called with [anchorPosition] to fetch the loaded item that is closest
     * to the last accessed index in the list.
     *
     * @param anchorPosition Index in the list, including placeholders.
     *
     * @return The closest loaded [Value] in [pages] to the provided [anchorPosition]. `null` if
     * all loaded [pages] are empty.
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
     * Coerces an index in the list, including placeholders, to closest loaded page in [pages].
     *
     * This function can be called with [anchorPosition] to fetch the loaded page that is closest
     * to the last accessed index in the list.
     *
     * @param anchorPosition Index in the list, including placeholders.
     *
     * @return The closest loaded [Value] in [pages] to the provided [anchorPosition]. `null` if
     * all loaded [pages] are empty.
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
     * was created, `false` otherwise.
     */
    public fun isEmpty(): Boolean = pages.all { it.data.isEmpty() }

    /**
     * @return The first loaded item in the list or `null` if all loaded pages are empty or no pages
     * were loaded when this [PagingState] was created.
     */
    public fun firstItemOrNull(): Value? {
        return pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
    }

    /**
     * @return The last loaded item in the list or `null` if all loaded pages are empty or no pages
     * were loaded when this [PagingState] was created.
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
        block: (pageIndex: Int, index: Int) -> T
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