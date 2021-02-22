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

package androidx.paging.rxjava2

import androidx.paging.PagingSource
import io.reactivex.Single
import kotlinx.coroutines.rx2.await

/**
 * Rx-based compatibility wrapper around [PagingSource]'s suspending APIs.
 *
 * @sample androidx.paging.samples.rxPagingSourceSample
 */
abstract class RxPagingSource<Key : Any, Value : Any> : PagingSource<Key, Value>() {
    /**
     * Loading API for [PagingSource].
     *
     * Implement this method to trigger your async load (e.g. from database or network).
     */
    abstract fun loadSingle(params: LoadParams<Key>): Single<LoadResult<Key, Value>>

    final override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        return loadSingle(params).await()
    }
}