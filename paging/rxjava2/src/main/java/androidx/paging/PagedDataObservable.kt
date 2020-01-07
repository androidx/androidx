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

@file:JvmName("PagedDataObservable")

package androidx.paging

import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asObservable

/**
 * Construct the primary RxJava Paging reactive stream: `Observable<PagedData<T>>`.
 *
 * Creates a stream of PagedData objects, each of which represents a single generation of
 * paginated data. These objects can be transformed to alter data as it loads, and presented in a
 * `RecyclerView`.
 *
 * @see PagedDataFlow
 */
@Suppress("FunctionName", "unused")
@UseExperimental(ExperimentalCoroutinesApi::class)
@JvmName("create")
fun <Key : Any, Value : Any> PagedDataObservable(
    config: PagingConfig,
    initialKey: Key?,
    pagedSourceFactory: () -> PagedSource<Key, Value>
): Observable<PagedData<Value>> =
    PagedDataFlow(config, initialKey, pagedSourceFactory).asObservable()

/**
 * Construct the primary RxJava Paging reactive stream: `Observable<PagedData<T>>`.
 *
 * Creates a stream of PagedData objects, each of which represents a single generation of
 * paginated data. These objects can be transformed to alter data as it loads, and presented in a
 * `RecyclerView`.
 *
 * @see PagedDataFlow
 */
@Suppress("FunctionName", "unused")
@UseExperimental(ExperimentalCoroutinesApi::class)
@JvmName("create")
fun <Key : Any, Value : Any> PagedDataObservable(
    config: PagingConfig,
    pagedSourceFactory: () -> PagedSource<Key, Value>
): Observable<PagedData<Value>> =
    PagedDataFlow(config, null, pagedSourceFactory).asObservable()
