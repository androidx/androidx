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

import androidx.paging.PagingSource.LoadResult.Page

/**
 * State of Paging system passed to [PagingSource.getRefreshKey] when fetching key for
 * the initial load or refresh.
 */
class PagingState<Key : Any, Value : Any> internal constructor(
    val pages: List<Page<Key, Value>>,
    /**
     * Most recently accessed index in the list, including placeholders.
     */
    val anchorPosition: Int,
    val initialLoadSize: Int,
    private val placeholdersStart: Int
) {
    init {
        require(pages.isNotEmpty()) {
            "Cannot instantiate PagingState without any loaded pages."
        }
    }

    /**
     * Coerces [anchorPosition] to closest loaded value in [pages].
     */
    fun closestItemToPosition(anchorPosition: Int): Value {
        anchorPositionToPagedIndices(anchorPosition) { pageIndex, index ->
            return when {
                index < 0 -> pages.first().data.first()
                pageIndex == pages.lastIndex && index > pages.last().data.lastIndex -> {
                    pages.last().data.last()
                }
                else -> pages[pageIndex].data[index]
            }
        }
    }

    /**
     * Coerces [anchorPosition] to closest loaded page in [pages].
     */
    fun closestPageToPosition(anchorPosition: Int): Page<Key, Value> {
        anchorPositionToPagedIndices(anchorPosition) { pageIndex, index ->
            return when {
                index < 0 -> pages.first()
                else -> pages[pageIndex]
            }
        }
    }

    private inline fun <T> anchorPositionToPagedIndices(
        anchorPosition: Int,
        block: (pageIndex: Int, index: Int) -> T
    ): T {
        var pageIndex = 0
        var index = anchorPosition - placeholdersStart
        while (pageIndex < pages.lastIndex && index > pages[pageIndex].data.lastIndex) {
            index -= pages[pageIndex].data.size
            pageIndex++
        }

        return block(pageIndex, index)
    }
}