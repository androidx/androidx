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

import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asObservable

/**
 * Builder for primary Paging reactive stream: `Observable<PagedData<T>>`.
 *
 * Creates a stream of PagedData objects, each of which represents a single paged generation.
 * They can be transformed to alter data as it loads, and presented in a `RecyclerView`.
 *
 * @see PagedDataFlowBuilder
 */
class RxPagedDataFlowBuilder<Key : Any, Value : Any>(
    pagedSourceFactory: () -> PagedSource<Key, Value>,
    config: PagedList.Config
) {
    private val baseBuilder = PagedDataFlowBuilder(pagedSourceFactory, config)

    fun setInitialKey(initialKey: Key?) = apply {
        baseBuilder.setInitialKey(initialKey)
    }

    @ExperimentalCoroutinesApi
    fun build(): Observable<PagedData<Value>> = baseBuilder.build().asObservable()
}
