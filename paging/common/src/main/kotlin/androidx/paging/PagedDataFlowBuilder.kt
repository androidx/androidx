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

import kotlinx.coroutines.flow.Flow

/**
 * Builder for primary Paging reactive stream: `Flow<PagedData<T>>`.
 *
 * Creates a stream of PagedData objects, each of which represents a single paged generation.
 * They can be transformed to alter data as it loads, and presented in a `RecyclerView`.
 */
class PagedDataFlowBuilder<Key : Any, Value : Any>(
    private val pagedSourceFactory: () -> PagedSource<Key, Value>,
    private val config: PagedList.Config
) {
    private var initialKey: Key? = null

    fun setInitialKey(initialKey: Key?) = apply {
        this.initialKey = initialKey
    }

    fun build(): Flow<PagedData<Value>> =
        PageFetcher(pagedSourceFactory, initialKey, config).flow
}
