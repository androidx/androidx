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

/*
 * We don't expect Java callers for the Flow APIs, but this and JvmName on every method below
 * leaves the possibility open.
 */
@file:JvmName("PagingDataFlow")

package androidx.paging

import kotlinx.coroutines.flow.Flow

/**
 * Construct the primary Paging reactive stream: `Flow<PagingData<T>>`.
 *
 * Creates a stream of [PagingData] objects, each of which represents a single generation of
 * paginated data. These objects can be transformed to alter data as it loads, and presented in a
 * `RecyclerView`.
 */
@Suppress("FunctionName")
@JvmName("create")
fun <Key : Any, Value : Any> PagingDataFlow(
    config: PagingConfig,
    initialKey: Key?,
    pagingSourceFactory: () -> PagingSource<Key, Value>
): Flow<PagingData<Value>> =
    PageFetcher(pagingSourceFactory, initialKey, config).flow

/**
 * Construct the primary Paging reactive stream: `Flow<PagingData<T>>`.
 *
 * Creates a stream of [PagingData] objects, each of which represents a single generation of
 * paginated data. These objects can be transformed to alter data as it loads, and presented in a
 * `RecyclerView`.
 */
@Suppress("FunctionName")
@JvmName("create")
fun <Key : Any, Value : Any> PagingDataFlow(
    config: PagingConfig,
    pagingSourceFactory: () -> PagingSource<Key, Value>
): Flow<PagingData<Value>> =
    PageFetcher(pagingSourceFactory, null, config).flow
