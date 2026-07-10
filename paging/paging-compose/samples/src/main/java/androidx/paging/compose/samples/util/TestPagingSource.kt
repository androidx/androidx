/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.paging.compose.samples.util

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.math.ceil
import kotlinx.coroutines.delay

internal class TestPagingSource<V : Any>(
    private val backend: TestBackend<V>,
    private val loadDelay: Long,
    private val transform: (Int) -> V,
) : PagingSource<Int, V>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, V> {
        // Simulate latency
        delay(loadDelay)

        // calculate how many pages need to be loaded to fulfill requested loadSize, round up
        val pageCount = ceil((params.loadSize / backend.pageSize.toDouble())).toInt()
        val pageNumber = params.key ?: 0

        val data = mutableListOf<Int>()
        for (i in pageNumber..<pageNumber + pageCount) {
            val response = backend.getItemsByKey(i, backend.pageSize)
            data.addAll(response.data)
        }

        val finalData = data.map(transform)
        // Since 0 is the lowest page number, return null to signify no more pages should
        // be loaded before it.
        val prevKey = if (pageNumber > 0) pageNumber - 1 else null

        // This API defines that it's out of data when a page returns empty. When out of
        // data, we return `null` to signify no more pages should be loaded
        val nextKey = if (data.isNotEmpty()) pageNumber + pageCount else null

        val itemsBefore = pageNumber * backend.pageSize
        val itemsAfter = backend.maxSize - data.size - itemsBefore - 1

        val page =
            LoadResult.Page(
                data = finalData,
                prevKey = prevKey,
                nextKey = nextKey,
                itemsBefore = itemsBefore,
                itemsAfter = itemsAfter,
            )
        return page
    }

    override fun getRefreshKey(state: PagingState<Int, V>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}
