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
import androidx.paging.PagingState
import androidx.paging.rxjava2.RxPagingSource
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import retrofit2.HttpException

private class RxBackendService {
    data class RemoteResult(
        val items: List<Item>,
        val prev: String,
        val next: String
    )

    @Suppress("UNUSED_PARAMETER")
    fun searchUsers(searchTerm: String, pageKey: String?): Single<RemoteResult> {
        throw NotImplementedError()
    }
}

@Sampled
fun rxPagingSourceSample() {
    /**
     * Sample RxPagingSource which loads `Item`s from network requests via Retrofit to a backend
     * service, which uses String tokens to load pages (each response has a next/previous token).
     */
    class MyPagingSource(
        val myBackend: RxBackendService,
        val searchTerm: String
    ) : RxPagingSource<String, Item>() {
        override fun loadSingle(params: LoadParams<String>): Single<LoadResult<String, Item>> {
            return myBackend
                // Single-based network load
                .searchUsers(searchTerm, params.key)
                .subscribeOn(Schedulers.io())
                .map<LoadResult<String, Item>> { result ->
                    LoadResult.Page(
                        data = result.items,
                        prevKey = result.prev,
                        nextKey = result.next
                    )
                }
                .onErrorReturn { e ->
                    when (e) {
                        // Retrofit calls that return the body type throw either IOException for
                        // network failures, or HttpException for any non-2xx HTTP status codes.
                        // This code reports all errors to the UI, but you can inspect/wrap the
                        // exceptions to provide more context.
                        is IOException -> LoadResult.Error(e)
                        is HttpException -> LoadResult.Error(e)
                        else -> throw e
                    }
                }
        }

        override fun getRefreshKey(state: PagingState<String, Item>): String? {
            return state.anchorPosition?.let { state.closestItemToPosition(it)?.id }
        }
    }
}
