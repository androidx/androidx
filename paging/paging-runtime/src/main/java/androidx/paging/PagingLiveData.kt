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

@file:JvmName("PagingLiveData")

package androidx.paging

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

/**
 * A [LiveData] of [PagingData], which mirrors the stream provided by [Pager.flow], but exposes it
 * as a [LiveData].
 */
val <Key : Any, Value : Any> Pager<Key, Value>.liveData: LiveData<PagingData<Value>>
    get() = flow.asLiveData()

/**
 * Operator which caches a [LiveData] of [PagingData] within the scope of a [Lifecycle].
 *
 * [cachedIn] multicasts pages loaded and transformed by a [PagingData], allowing multiple
 * observers on the same instance of [PagingData] to receive the same events, avoiding redundant
 * work, but comes at the cost of buffering those pages in memory.
 *
 * Calling [cachedIn] is required to allow calling
 * [submitData][androidx.paging.AsyncPagingDataAdapter] on the same instance of [PagingData]
 * emitted by [Pager] or any of its transformed derivatives, as reloading data from scratch on the
 * same generation of [PagingData] is an unsupported operation.
 *
 * @param lifecycle The [Lifecycle] where the page cache will be kept alive.
 */
fun <T : Any> LiveData<PagingData<T>>.cachedIn(lifecycle: Lifecycle) = asFlow()
    .cachedIn(lifecycle.coroutineScope)
    .asLiveData()

/**
 * Operator which caches a [LiveData] of [PagingData] within the scope of a [ViewModel].
 *
 * [cachedIn] multicasts pages loaded and transformed by a [PagingData], allowing multiple
 * observers on the same instance of [PagingData] to receive the same events, avoiding redundant
 * work, but comes at the cost of buffering those pages in memory.
 *
 * Calling [cachedIn] is required to allow calling
 * [submitData][androidx.paging.AsyncPagingDataAdapter] on the same instance of [PagingData]
 * emitted by [Pager] or any of its transformed derivatives, as reloading data from scratch on the
 * same generation of [PagingData] is an unsupported operation.
 *
 * @param viewModel The [ViewModel] whose [viewModelScope] will dictate how long the page
 * cache will be kept alive.
 */
fun <T : Any> LiveData<PagingData<T>>.cachedIn(viewModel: ViewModel) = asFlow()
    .cachedIn(viewModel.viewModelScope)
    .asLiveData()

/**
 * Operator which caches a [LiveData] of [PagingData] within a [CoroutineScope].
 *
 * [cachedIn] multicasts pages loaded and transformed by a [PagingData], allowing multiple
 * observers on the same instance of [PagingData] to receive the same events, avoiding redundant
 * work, but comes at the cost of buffering those pages in memory.
 *
 * Calling [cachedIn] is required to allow calling
 * [submitData][androidx.paging.AsyncPagingDataAdapter] on the same instance of [PagingData]
 * emitted by [Pager] or any of its transformed derivatives, as reloading data from scratch on the
 * same generation of [PagingData] is an unsupported operation.
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as `ViewModel.viewModelScope`, which automatically cancels after
 * the [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 */
fun <T : Any> LiveData<PagingData<T>>.cachedIn(scope: CoroutineScope) = asFlow()
    .cachedIn(scope)
    .asLiveData()
