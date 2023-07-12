/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.paging.compose.samples

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.math.ceil
import kotlinx.coroutines.delay

internal class TestBackend(
    private val backendDataList: List<String>,
    private val loadDelay: Long = 1000,
) {
    val DataBatchSize = 5

    class DesiredLoadResultPageResponse(
        val data: List<String>
    )

    /**
     * Returns [DataBatchSize] items for a key
     */
    fun searchItemsByKey(key: Int): DesiredLoadResultPageResponse {
        val maxKey = ceil(backendDataList.size.toFloat() / DataBatchSize).toInt()

        if (key >= maxKey) {
            return DesiredLoadResultPageResponse(emptyList())
        }

        val from = key * DataBatchSize
        val to = minOf((key + 1) * DataBatchSize, backendDataList.size)
        val currentSublist = backendDataList.subList(from, to)

        return DesiredLoadResultPageResponse(currentSublist)
    }

    fun getAllData() = TestPagingSource(this, loadDelay)
}

internal class TestPagingSource(
    private val backend: TestBackend,
    private val loadDelay: Long,
) : PagingSource<Int, String>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        // Simulate latency
        delay(loadDelay)

        val pageNumber = params.key ?: 0

        val response = backend.searchItemsByKey(pageNumber)

        // Since 0 is the lowest page number, return null to signify no more pages should
        // be loaded before it.
        val prevKey = if (pageNumber > 0) pageNumber - 1 else null

        // This API defines that it's out of data when a page returns empty. When out of
        // data, we return `null` to signify no more pages should be loaded
        val nextKey = if (response.data.isNotEmpty()) pageNumber + 1 else null

        return LoadResult.Page(
            data = response.data,
            prevKey = prevKey,
            nextKey = nextKey
        )
    }

    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}