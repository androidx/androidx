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

import androidx.testutils.DirectDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * [PagingSource] for testing which pages through a list of conesecutive integers from 0..99 where
 * position == key == value.
 *
 * Note: This class has a delay of 1000ms is built into its load method and is meant to be used
 * with APIs from [kotlinx.coroutines.test.DelayController].
 */
class TestPagingSource(
    counted: Boolean = true,
    override val jumpingSupported: Boolean = true,
    val items: List<Int> = ITEMS,
    private val loadDelay: Long = 1000,
    private val loadDispatcher: CoroutineDispatcher = DirectDispatcher
) : PagingSource<Int, Int>() {
    var errorNextLoad = false
    var nextLoadResult: LoadResult<Int, Int>? = null

    val getRefreshKeyCalls = mutableListOf<PagingState<Int, Int>>()
    val loadedPages = mutableListOf<LoadResult.Page<Int, Int>>()

    init {
        if (!counted) {
            throw NotImplementedError(
                "TODO: Implement this for uncounted case, and add " +
                    "appropriate test cases to PageFetcher, Pager, and PagerState."
            )
        }
    }

    override val keyReuseSupported: Boolean
        get() = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
        // This delay allows tests running within DelayController APIs to control the order of
        // execution of events.
        delay(loadDelay)

        return withContext(loadDispatcher) { getLoadResult(params) }
    }

    private fun getLoadResult(params: LoadParams<Int>): LoadResult<Int, Int> {
        val key = params.key ?: 0

        val isPrepend = params is LoadParams.Prepend
        val start = (if (isPrepend) key - params.loadSize + 1 else key)
            .coerceAtLeast(0)
        val end = (if (isPrepend) key + 1 else key + params.loadSize)
            .coerceAtMost(items.size)

        if (errorNextLoad) {
            errorNextLoad = false
            return LoadResult.Error(LOAD_ERROR)
        }

        val nextLoadResult = nextLoadResult
        if (nextLoadResult != null) {
            this.nextLoadResult = null
            return nextLoadResult
        }

        return LoadResult.Page(
            items.subList(start, end),
            if (start > 0) start - 1 else null,
            if (end < items.size) end else null,
            start,
            items.size - end
        ).also {
            loadedPages.add(it)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Int>): Int? {
        getRefreshKeyCalls.add(state)
        return state.anchorPosition
    }

    companion object {
        val ITEMS = List(100) { it }
        val LOAD_ERROR = Exception("Exception from TestPagingSource.errorNextLoad")
    }
}