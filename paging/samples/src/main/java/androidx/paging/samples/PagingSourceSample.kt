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

@file:Suppress("unused")

package androidx.paging.samples

import androidx.annotation.Sampled
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import java.io.IOException
import retrofit2.HttpException

internal class MyBackendService {
    data class RemoteResult(
        val items: List<Item>,
        val prev: String?,
        val next: String?
    )

    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun searchItems(pageKey: String?): RemoteResult {
        throw NotImplementedError()
    }

    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun searchItems(pageNumber: Int): RemoteResult {
        throw NotImplementedError()
    }

    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun itemsAfter(itemKey: String?): RemoteResult {
        throw NotImplementedError()
    }
}

@Sampled
fun itemKeyedPagingSourceSample() {
    /**
     * Sample item-keyed [PagingSource], which uses String tokens to load pages.
     *
     * Loads Items from network requests via Retrofit to a backend service.
     */
    class MyPagingSource(
        val myBackend: MyBackendService
    ) : PagingSource<String, Item>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, Item> {
            // Retrofit calls that return the body type throw either IOException for network
            // failures, or HttpException for any non-2xx HTTP status codes. This code reports all
            // errors to the UI, but you can inspect/wrap the exceptions to provide more context.
            return try {
                // Suspending network load via Retrofit. This doesn't need to be wrapped in a
                // withContext(Dispatcher.IO) { ... } block since Retrofit's Coroutine
                // CallAdapter dispatches on a worker thread.
                val response = myBackend.searchItems(params.key)
                LoadResult.Page(
                    data = response.items,
                    prevKey = response.prev,
                    nextKey = response.next
                )
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: HttpException) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<String, Item>): String? {
            return state.anchorPosition?.let { state.closestItemToPosition(it)?.id }
        }
    }
}

@Sampled
fun pageKeyedPagingSourceSample() {
    /**
     * Sample page-keyed PagingSource, which uses Int page number to load pages.
     *
     * Loads Items from network requests via Retrofit to a backend service.
     *
     * Note that the key type is Int, since we're using page number to load a page.
     */
    class MyPagingSource(
        val myBackend: MyBackendService
    ) : PagingSource<Int, Item>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {

            // Retrofit calls that return the body type throw either IOException for network
            // failures, or HttpException for any non-2xx HTTP status codes. This code reports all
            // errors to the UI, but you can inspect/wrap the exceptions to provide more context.
            return try {
                // Key may be null during a refresh, if no explicit key is passed into Pager
                // construction. Use 0 as default, because our API is indexed started at index 0
                val pageNumber = params.key ?: 0

                // Suspending network load via Retrofit. This doesn't need to be wrapped in a
                // withContext(Dispatcher.IO) { ... } block since Retrofit's Coroutine
                // CallAdapter dispatches on a worker thread.
                val response = myBackend.searchItems(pageNumber)

                // Since 0 is the lowest page number, return null to signify no more pages should
                // be loaded before it.
                val prevKey = if (pageNumber > 0) pageNumber - 1 else null

                // This API defines that it's out of data when a page returns empty. When out of
                // data, we return `null` to signify no more pages should be loaded
                val nextKey = if (response.items.isNotEmpty()) pageNumber + 1 else null
                LoadResult.Page(
                    data = response.items,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: HttpException) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Item>): Int? {
            return state.anchorPosition?.let {
                state.closestPageToPosition(it)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
            }
        }
    }
}

@Sampled
fun pageKeyedPage() {
    // One common method of pagination is to use next (and optionally previous) tokens.
    // The below code shows you how to
    data class NetworkResponseObject(
        val items: List<Item>,
        val next: String,
        val approximateItemsRemaining: Int
    )

    // The following shows how you use convert such a response loaded in PagingSource.load() to
    // a Page, which can be returned from that method
    fun NetworkResponseObject.toPage() = LoadResult.Page(
        data = items,
        prevKey = null, // this implementation can only append, can't load a prepend
        nextKey = next, // next token will be the params.key of a subsequent append load
        itemsAfter = approximateItemsRemaining
    )
}

@Sampled
fun pageIndexedPage() {
    // If you load by page number, the response may not define how to load the next page.
    data class NetworkResponseObject(
        val items: List<Item>
    )

    // The following shows how you use the current page number (e.g., the current key in
    // PagingSource.load() to convert a response into a Page, which can be returned from that method
    fun NetworkResponseObject.toPage(pageNumber: Int): LoadResult.Page<Int, Item> {
        return LoadResult.Page(
            data = items,
            // Since 0 is the lowest page number, return null to signify no more pages
            // should be loaded before it.
            prevKey = if (pageNumber > 0) pageNumber - 1 else null,
            // This API defines that it's out of data when a page returns empty. When out of
            // data, we return `null` to signify no more pages should be loaded
            // If the response instead
            nextKey = if (items.isNotEmpty()) pageNumber + 1 else null
        )
    }
}
