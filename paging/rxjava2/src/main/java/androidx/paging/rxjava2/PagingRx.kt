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
@OptIn(ExperimentalCoroutinesApi::class)
val <Key : Any, Value : Any> Pager<Key, Value>.observable: Observable<PagingData<Value>>
    get() = flow
        .conflate()
        .asObservable()

/**
 * A [Flowable] of [PagingData], which mirrors the stream provided by [Pager.flow], but exposes
 * it as a [Flowable].
 */
@OptIn(ExperimentalCoroutinesApi::class)
val <Key : Any, Value : Any> Pager<Key, Value>.flowable: Flowable<PagingData<Value>>
    get() = flow
        .conflate()
        .asFlowable()

/**
 * Operator which caches an [Observable] of [PagingData] within a [CoroutineScope].
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as `ViewModel.viewModelScope`, which automatically cancels after
 * the [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> Observable<PagingData<T>>.cachedIn(scope: CoroutineScope): Observable<PagingData<T>> {
    return toFlowable(BackpressureStrategy.LATEST)
        .asFlow()
        .cachedIn(scope)
        .asObservable()
}

/**
 * Operator which caches a [Flowable] of [PagingData] within a [CoroutineScope].
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as `ViewModel.viewModelScope`, which automatically cancels after
 * the [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> Flowable<PagingData<T>>.cachedIn(scope: CoroutineScope): Flowable<PagingData<T>> {
    return asFlow()
        .cachedIn(scope)
        .asFlowable()
}
