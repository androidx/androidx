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

@file:JvmName("PagingRx")
@file:JvmMultifileClass

package androidx.paging.rxjava2

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable

/**
 * An [Observable] of [PagingData], which mirrors the stream provided by [Pager.flow], but exposes
 * it as an [Observable].
 */
// Both annotations are needed here see: https://youtrack.jetbrains.com/issue/KT-45227
@ExperimentalCoroutinesApi
@get:ExperimentalCoroutinesApi
val <Key : Any, Value : Any> Pager<Key, Value>.observable: Observable<PagingData<Value>>
    get() = flow
        .conflate()
        .asObservable()

/**
 * A [Flowable] of [PagingData], which mirrors the stream provided by [Pager.flow], but exposes
 * it as a [Flowable].
 */
// Both annotations are needed here see: https://youtrack.jetbrains.com/issue/KT-45227
@ExperimentalCoroutinesApi
@get:ExperimentalCoroutinesApi
val <Key : Any, Value : Any> Pager<Key, Value>.flowable: Flowable<PagingData<Value>>
    get() = flow
        .conflate()
        .asFlowable()

/**
 * Operator which caches an [Observable] of [PagingData] within a [CoroutineScope].
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
@ExperimentalCoroutinesApi
fun <T : Any> Observable<PagingData<T>>.cachedIn(scope: CoroutineScope): Observable<PagingData<T>> {
    return toFlowable(BackpressureStrategy.LATEST)
        .asFlow()
        .cachedIn(scope)
        .asObservable()
}

/**
 * Operator which caches a [Flowable] of [PagingData] within a [CoroutineScope].
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
@ExperimentalCoroutinesApi
fun <T : Any> Flowable<PagingData<T>>.cachedIn(scope: CoroutineScope): Flowable<PagingData<T>> {
    return asFlow()
        .cachedIn(scope)
        .asFlowable()
}
