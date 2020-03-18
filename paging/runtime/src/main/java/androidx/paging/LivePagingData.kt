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

@file:JvmName("LivePagingData")

package androidx.paging

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineScope

/**
 * Construct the primary Paging reactive stream: `LiveData<PagingData<T>>`.
 *
 * Creates a stream of [PagingData] objects, each of which represents a single generation of
 * paginated data. These objects can be transformed to alter data as it loads, and presented in a
 * [RecyclerView][androidx.recyclerview.widget.RecyclerView].
 */
@Suppress("FunctionName", "unused")
@JvmOverloads
@JvmName("create")
fun <Key : Any, Value : Any> LivePagingData(
    config: PagingConfig,
    initialKey: Key? = null,
    pagingSourceFactory: () -> PagingSource<Key, Value>
): LiveData<PagingData<Value>> =
    PagingDataFlow(config, initialKey, pagingSourceFactory).asLiveData()

/**
 * Operator which caches a stream of [PagingData] within the scope of a [Lifecycle].
 *
 * @param lifecycle The [Lifecycle] where the page cache will be kept alive.
 */
fun <T : Any> LiveData<PagingData<T>>.cachedIn(lifecycle: Lifecycle) = asFlow()
    .cachedIn(lifecycle.coroutineScope)
    .asLiveData()

/**
 * Operator which caches a stream of [PagingData] within a [CoroutineScope].
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as ViewModelScope, which automatically cancels after the
 * [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 */
fun <T : Any> LiveData<PagingData<T>>.cachedIn(scope: CoroutineScope) = asFlow()
    .cachedIn(scope)
    .asLiveData()
