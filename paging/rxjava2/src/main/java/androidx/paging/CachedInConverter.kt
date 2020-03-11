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

@file:JvmMultifileClass
@file:JvmName("RxPaging")

package androidx.paging

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableConverter
import io.reactivex.Observable
import io.reactivex.ObservableConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable

/**
 * This converter caches a stream of [PagingData] within a [CoroutineScope].
 *
 * [CachedInConverter] is a custom converter which implements both [FlowableConverter] and
 * [ObservableConverter] for use with the `as` operator.
 *
 * This class is intended for Java users. Kotlin users can instead use the [cachedIn] extension for
 * simplicity.
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as ViewModelScope, which automatically cancels after the
 * [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 *
 * @see cachedIn
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
class CachedInConverter<T : Any>(
    private val scope: CoroutineScope
) : FlowableConverter<PagingData<T>, Flowable<PagingData<T>>>,
    ObservableConverter<PagingData<T>, Observable<PagingData<T>>> {
    override fun apply(pagingDataFlowable: Flowable<PagingData<T>>): Flowable<PagingData<T>> {
        return pagingDataFlowable.cachedIn(scope)
    }

    override fun apply(pagingDataObservable: Observable<PagingData<T>>): Observable<PagingData<T>> {
        return pagingDataObservable.cachedIn(scope)
    }
}

/**
 * Converts an [Observable]<[PagingData]> to be cached within a [CoroutineScope] using
 * [CachedInConverter].
 *
 * This extension is intended for Kotlin callers. Java callers should use [CachedInConverter].
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as ViewModelScope, which automatically cancels after the
 * [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 *
 * @see CachedInConverter
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T : Any> Observable<PagingData<T>>.cachedIn(scope: CoroutineScope): Observable<PagingData<T>> {
    return toFlowable(BackpressureStrategy.LATEST)
        .asFlow()
        .cachedIn(scope)
        .asObservable()
}

/**
 * Converts a [Flowable]<[PagingData]> to be cached within a [CoroutineScope] using
 * [CachedInConverter].
 *
 * This extension is intended for Kotlin callers. Java callers should use [CachedInConverter].
 *
 * @param scope The [CoroutineScope] where the page cache will be kept alive. Typically this
 * would be a managed scope such as ViewModelScope, which automatically cancels after the
 * [PagingData] stream is no longer needed. Otherwise, the provided [CoroutineScope] must be
 * manually cancelled to avoid memory leaks.
 *
 * @see CachedInConverter
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T : Any> Flowable<PagingData<T>>.cachedIn(scope: CoroutineScope): Flowable<PagingData<T>> {
    return asFlow()
        .cachedIn(scope)
        .asFlowable()
}
