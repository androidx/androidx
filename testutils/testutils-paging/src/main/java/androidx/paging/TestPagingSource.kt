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

import kotlinx.coroutines.delay

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
    val items: List<Int> = Companion.ITEMS
) : PagingSource<Int, Int>() {
    var errorNextLoad = false

    init {
        if (!counted) {
            throw NotImplementedError(
                "TODO: Implement this for uncounted case, and add " +
                        "appropriate test cases to PageFetcher, Pager, and PagerState."
            )
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
        val key = params.key ?: 0

        val isPrepend = params is LoadParams.Prepend
        val start = if (isPrepend) key - params.loadSize + 1 else key
        val end = if (isPrepend) key + 1 else key + params.loadSize

        // This delay allows tests running withing DelayController APIs to control the order of
        // execution of events.
        delay(1000)

        if (errorNextLoad) {
            errorNextLoad = false
            return LoadResult.Error(LOAD_ERROR)
        }

        return LoadResult.Page(
            items.subList(start, end),
            if (start > 0) start - 1 else null,
            if (end < items.size) end else null,
            start,
            items.size - end
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Int>): Int? {
        return state.anchorPosition
    }

    companion object {
        val ITEMS = List(100) { it }
        val LOAD_ERROR = Exception("Exception from TestPagingSource.errorNextLoad")
    }
}
