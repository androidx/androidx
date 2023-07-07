/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.paging.testing

import androidx.paging.Pager
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadParams.Append
import androidx.paging.PagingSource.LoadParams.Prepend
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingState

/**
 * An implementation of [PagingSource] that loads data from a static [dataList]. The source
 * of data is expected to be immutable. This [PagingSource] should be be invalidated
 * externally whenever the [dataList] passed to this [PagingSource] becomes obsolete.
 */
internal class StaticListPagingSource<Value : Any>(
    private val dataList: List<Value>
) : PagingSource<Int, Value>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        val key = params.key ?: 0
        val indexStart = computeIndexStart(params, key)
        val indexEnd = computeIndexEnd(params, key, indexStart)
        val data = dataList.slice(indexStart..indexEnd)
        return LoadResult.Page(
            data = data,
            prevKey = if (indexStart <= 0 || data.isEmpty()) null else indexStart - 1,
            nextKey = if (indexEnd >= dataList.lastIndex || data.isEmpty()) null else indexEnd + 1,
            itemsBefore = indexStart,
            itemsAfter = dataList.lastIndex - indexEnd
        )
    }

    /**
     * Returns the index to start loading from the [dataList] for each [load] call.
     *
     * Prepend: The [key] represents the index before the first loaded Page's first index, i.e.
     * if first loaded page contains items in index[25, 26, 27], then [key] = 24. The `startIndex`
     * is offset negatively by [LoadParams.loadSize]. For example, if [key] = 24 and loadSize = 5,
     * then the startIndex = 19, such that items in index[20, 21, 22, 23, 24] are loaded.
     *
     * Refresh: The [key] may derive from either [getRefreshKey] or from the `initialKey` passed
     * in to [Pager]. If the [key] is larger than [dataList].lastIndex (i.e. an initialKey
     * that is out of bounds), the `startIndex` will be offset negatively from the lastIndex
     * by [LoadParams.loadSize] such that it will start loading from the last page.
     * For example if index[0 - 49] is available with [key] = 55 and loadSize = 5,
     * the `startIndex` will be offset to 45 such that items in index[45, 46, 47, 48, 49] are
     * loaded. The largest possible `startIndex` that can be returned for
     * Refresh is [dataList].lastIndex.
     *
     * Negative startIndices are clipped to 0.
     */
    private fun computeIndexStart(params: LoadParams<Int>, key: Int): Int {
        return when (params) {
            is Prepend -> (key - params.loadSize + 1)
            is Append -> key
            is Refresh ->
                if (key >= dataList.lastIndex) {
                    (dataList.size - params.loadSize)
                } else {
                    key
                }
        }.coerceAtLeast(0)
    }

    /**
     * Returns the index to stop loading from the [dataList] for each [load] call.
     *
     * Prepend: The [key] represents the index before the first loaded Page's first index, i.e.
     * if first loaded page contains items in index[25, 26, 27], then [key] = 24. If loadSize
     * exceeds available data to load, i.e. loadSize = 5 with only items in index[0, 1, 2] are
     * available, the `endIndex` is clipped to the value of [key]. Reusing example with [key] = 2
     * and items[0, 1, 2] available, the `startIndex` = 0 and the `endIndex` of `4` is clipped
     * to `2` such that only items [0, 1, 2] are loaded.
     *
     * Refresh: The [key] may derive from either [getRefreshKey] or from the `initialKey` passed
     * in to [Pager]. As long as the `endIndex` is within bounds of [dataList] indices, `endIndex`
     * will load the maximum amount of available data as requested by [LoadParams.loadSize]. If
     * the [key] is out of bounds, it will be clipped to a maximum value of [dataList].lastIndex.
     */
    private fun computeIndexEnd(params: LoadParams<Int>, key: Int, startIndex: Int): Int {
        val defaultOffset = startIndex + params.loadSize - 1
        return when (params) {
            is Prepend -> defaultOffset.coerceAtMost(key)
            else -> defaultOffset.coerceAtMost(dataList.lastIndex)
        }
    }

    /**
     * Returns the key to be used as the [LoadParams.key] for the next generation's Refresh load.
     *
     * It is unknown whether anchorPosition represents the item at the top of the screen or item at
     * the bottom of the screen. To ensure the number of items loaded is enough to fill up the
     * screen, half of loadSize is loaded before the anchorPosition and the other half is
     * loaded after the anchorPosition -- anchorPosition becomes the middle item.
     *
     * Negative refreshKeys are clipped to 0.
    */
    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return when (val anchorPosition = state.anchorPosition) {
            null -> null
            else -> (anchorPosition - state.config.initialLoadSize / 2).coerceAtLeast(0)
        }
    }

    override val jumpingSupported: Boolean
        get() = true
}
