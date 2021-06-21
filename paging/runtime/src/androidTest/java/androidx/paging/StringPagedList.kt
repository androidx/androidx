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

import androidx.paging.PagingSource.LoadResult.Error
import androidx.paging.PagingSource.LoadResult.Page
import androidx.testutils.DirectDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking

private class FakeSource<Value : Any>(
    private val leadingNulls: Int,
    private val trailingNulls: Int,
    private val data: List<Value>
) : PagingSource<Any, Value>() {
    override suspend fun load(params: LoadParams<Any>): LoadResult<Any, Value> {
        if (params is LoadParams.Refresh) {
            return Page(
                data = data,
                prevKey = null,
                nextKey = null,
                itemsBefore = leadingNulls,
                itemsAfter = trailingNulls
            )
        }
        // TODO: prevent null-key load start/end
        return Error(
            IllegalArgumentException("This test source only supports initial load")
        )
    }

    override fun getRefreshKey(state: PagingState<Any, Value>): Any? = null
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("TestFunctionName", "DEPRECATION")
fun StringPagedList(
    leadingNulls: Int,
    trailingNulls: Int,
    vararg items: String
): PagedList<String> = runBlocking {
    PagedList.create(
        initialPage = Page<Any, String>(
            data = items.toList(),
            prevKey = null,
            nextKey = null,
            itemsBefore = leadingNulls,
            itemsAfter = trailingNulls
        ),
        pagingSource = FakeSource(leadingNulls, trailingNulls, items.toList()),
        coroutineScope = GlobalScope,
        notifyDispatcher = DirectDispatcher,
        fetchDispatcher = DirectDispatcher,
        boundaryCallback = null,
        config = Config(1, prefetchDistance = 0),
        key = null
    )
}