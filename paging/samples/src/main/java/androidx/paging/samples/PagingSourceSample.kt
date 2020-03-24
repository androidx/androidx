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
import retrofit2.HttpException
import java.io.IOException

private class MyBackendService {
    data class RemoteResult(
        val items: List<Item>,
        val prev: String,
        val next: String
    )
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun searchUsers(searchTerm: String, pageKey: String?): RemoteResult {
        throw NotImplementedError()
    }
}

@Sampled
fun pagingSourceSample() {
    /**
     * Sample PagingSource which loads `Item`s from network requests via Retrofit to a backend
     * service, which uses String tokens to load pages (each response has a next/previous token).
     */
    class MyPagingSource(
        val myBackend: MyBackendService,
        val searchTerm: String
    ) : PagingSource<String, Item>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, Item> {
            // Retrofit calls that return the body type throw either IOException for network
            // failures, or HttpException for any non-2xx HTTP status codes. This code reports all
            // errors to the UI, but you can inspect/wrap the exceptions to provide more context.
            return try {
                // suspending network load, executes automatically on worker thread
                val response = myBackend.searchUsers(searchTerm, params.key)
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
    }
}