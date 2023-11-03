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

@file:Suppress("unused", "UnstableApiUsage")

package androidx.paging.samples

import androidx.annotation.Sampled
import androidx.paging.ListenableFuturePagingSource
import androidx.paging.PagingState
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.util.concurrent.Executor
import retrofit2.HttpException

data class RemoteResult(
    val items: List<Item>,
    val prev: String,
    val next: String
)

private class GuavaBackendService {
    @Suppress("UNUSED_PARAMETER")
    fun searchUsers(searchTerm: String, pageKey: String?): FluentFuture<RemoteResult> {
        throw NotImplementedError()
    }
}

lateinit var networkExecutor: Executor

@Sampled
fun listenableFuturePagingSourceSample() {
    class MyListenableFuturePagingSource(
        val myBackend: GuavaBackendService,
        val searchTerm: String
    ) : ListenableFuturePagingSource<String, Item>() {
        override fun loadFuture(
            params: LoadParams<String>
        ): ListenableFuture<LoadResult<String, Item>> {
            return myBackend
                .searchUsers(
                    searchTerm = searchTerm,
                    pageKey = params.key
                )
                .transform<LoadResult<String, Item>>(
                    { response ->
                        LoadResult.Page(
                            data = response!!.items,
                            prevKey = response.prev,
                            nextKey = response.next
                        )
                    },
                    networkExecutor
                )
                // Retrofit calls that return the body type throw either IOException for
                // network failures, or HttpException for any non-2xx HTTP status codes.
                // This code reports all errors to the UI, but you can inspect/wrap the
                // exceptions to provide more context.
                .catching(
                    IOException::class.java,
                    { t: IOException? -> LoadResult.Error(t!!) },
                    networkExecutor
                )
                .catching(
                    HttpException::class.java,
                    { t: HttpException? -> LoadResult.Error(t!!) },
                    networkExecutor
                )
        }

        override fun getRefreshKey(state: PagingState<String, Item>): String? {
            return state.anchorPosition?.let { state.closestItemToPosition(it)?.id }
        }
    }
}
