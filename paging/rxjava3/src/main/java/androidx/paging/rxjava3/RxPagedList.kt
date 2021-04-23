/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging.rxjava3

import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PagingSource
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler

@Suppress("DEPRECATION")
private fun <Key : Any, Value : Any> createRxPagedListBuilder(
    dataSourceFactory: DataSource.Factory<Key, Value>,
    config: PagedList.Config,
    initialLoadKey: Key?,
    boundaryCallback: PagedList.BoundaryCallback<Value>?,
    fetchScheduler: Scheduler?,
    notifyScheduler: Scheduler?
): RxPagedListBuilder<Key, Value> {
    val builder = RxPagedListBuilder(dataSourceFactory, config)
        .setInitialLoadKey(initialLoadKey)
        .setBoundaryCallback(boundaryCallback)
    if (fetchScheduler != null) builder.setFetchScheduler(fetchScheduler)
    if (notifyScheduler != null) builder.setNotifyScheduler(notifyScheduler)
    return builder
}

@Suppress("DEPRECATION")
private fun <Key : Any, Value : Any> createRxPagedListBuilder(
    pagingSourceFactory: () -> PagingSource<Key, Value>,
    config: PagedList.Config,
    initialLoadKey: Key?,
    boundaryCallback: PagedList.BoundaryCallback<Value>?,
    fetchScheduler: Scheduler?,
    notifyScheduler: Scheduler?
): RxPagedListBuilder<Key, Value> {
    val builder = RxPagedListBuilder(pagingSourceFactory, config)
        .setInitialLoadKey(initialLoadKey)
        .setBoundaryCallback(boundaryCallback)
    if (fetchScheduler != null) builder.setFetchScheduler(fetchScheduler)
    if (notifyScheduler != null) builder.setNotifyScheduler(notifyScheduler)
    return builder
}

/**
 * Constructs a `Observable<PagedList>` from this [DataSource.Factory], convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Observable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [DataSource].
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [DataSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 *
 * @see RxPagedListBuilder
 * @see toFlowable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(
                config.pageSize,
                config.prefetchDistance,
                config.enablePlaceholders,
                config.initialLoadSizeHint,
                config.maxSize
            ),
            initialLoadKey,
            this.asPagingSourceFactory(fetchScheduler?.asCoroutineDispatcher() ?: Dispatchers.IO)
        ).observable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.observable",
        "kotlinx.coroutines.rx3.asCoroutineDispatcher",
        "kotlinx.coroutines.Dispatchers"
    )
)
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toObservable(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null
): Observable<PagedList<Value>> {
    return createRxPagedListBuilder(
        dataSourceFactory = this,
        config = config,
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildObservable()
}

/**
 * Constructs a `Observable<PagedList>` from this [DataSource.Factory], convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Observable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param pageSize Size of pages to load.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [DataSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [DataSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 *
 * @see RxPagedListBuilder
 * @see toFlowable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(pageSize),
            initialLoadKey,
            this.asPagingSourceFactory(fetchScheduler?.asCoroutineDispatcher() ?: Dispatchers.IO)
        ).observable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.observable",
        "kotlinx.coroutines.rx3.asCoroutineDispatcher",
        "kotlinx.coroutines.Dispatchers"
    )
)
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toObservable(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null
): Observable<PagedList<Value>> {
    return createRxPagedListBuilder(
        dataSourceFactory = this,
        config = Config(pageSize),
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildObservable()
}

/**
 * Constructs a `Flowable<PagedList>`, from this [DataSource.Factory], convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Flowable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [DataSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [DataSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 * @param backpressureStrategy [BackpressureStrategy] for the [Flowable] to use.
 *
 * @see RxPagedListBuilder
 * @see toObservable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(
                config.pageSize,
                config.prefetchDistance,
                config.enablePlaceholders,
                config.initialLoadSizeHint,
                config.maxSize
            ),
            initialLoadKey,
            this.asPagingSourceFactory(fetchScheduler?.asCoroutineDispatcher() ?: Dispatchers.IO)
        ).flowable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.flowable",
        "kotlinx.coroutines.rx3.asCoroutineDispatcher",
        "kotlinx.coroutines.Dispatchers"
    )
)
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toFlowable(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<PagedList<Value>> {
    return createRxPagedListBuilder(
        dataSourceFactory = this,
        config = config,
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildFlowable(backpressureStrategy)
}

/**
 * Constructs a `Flowable<PagedList>`, from this [DataSource.Factory], convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Flowable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [DataSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [DataSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 * @param backpressureStrategy [BackpressureStrategy] for the [Flowable] to use.
 *
 * @see RxPagedListBuilder
 * @see toObservable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(pageSize),
            initialLoadKey,
            this.asPagingSourceFactory(fetchScheduler?.asCoroutineDispatcher() ?: Dispatchers.IO)
        ).flowable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.flowable",
        "kotlinx.coroutines.rx3.asCoroutineDispatcher",
        "kotlinx.coroutines.Dispatchers"
    )
)
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toFlowable(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<PagedList<Value>> {
    return createRxPagedListBuilder(
        dataSourceFactory = this,
        config = Config(pageSize),
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildFlowable(backpressureStrategy)
}

/**
 * Constructs a `Observable<PagedList>` from this [PagingSource] factory, convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Observable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagingSource].
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [PagingSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 *
 * @see RxPagedListBuilder
 * @see toFlowable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(
                config.pageSize,
                config.prefetchDistance,
                config.enablePlaceholders,
                config.initialLoadSizeHint,
                config.maxSize
            ),
            initialLoadKey,
            this.asPagingSourceFactory(fetchScheduler?.asCoroutineDispatcher() ?: Dispatchers.IO)
        ).observable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.observable",
        "kotlinx.coroutines.rx3.asCoroutineDispatcher",
        "kotlinx.coroutines.Dispatchers"
    )
)
fun <Key : Any, Value : Any> (() -> PagingSource<Key, Value>).toObservable(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null
): Observable<PagedList<Value>> {
    return createRxPagedListBuilder(
        pagingSourceFactory = this,
        config = config,
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildObservable()
}

/**
 * Constructs a `Observable<PagedList>` from this [PagingSource] factory, convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Observable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param pageSize Size of pages to load.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagingSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [PagingSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 *
 * @see RxPagedListBuilder
 * @see toFlowable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(pageSize),
            initialLoadKey,
            this
        ).observable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.observable"
    )
)
fun <Key : Any, Value : Any> (() -> PagingSource<Key, Value>).toObservable(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null
): Observable<PagedList<Value>> {
    return createRxPagedListBuilder(
        pagingSourceFactory = this,
        config = Config(pageSize),
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildObservable()
}

/**
 * Constructs a `Flowable<PagedList>`, from this [PagingSource] factory, convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Flowable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagingSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [PagingSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 * @param backpressureStrategy [BackpressureStrategy] for the [Flowable] to use.
 *
 * @see RxPagedListBuilder
 * @see toObservable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(
                config.pageSize,
                config.prefetchDistance,
                config.enablePlaceholders,
                config.initialLoadSizeHint,
                config.maxSize
            ),
            initialLoadKey,
            this
        ).flowable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.flowable"
    )
)
fun <Key : Any, Value : Any> (() -> PagingSource<Key, Value>).toFlowable(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<PagedList<Value>> {
    return createRxPagedListBuilder(
        pagingSourceFactory = this,
        config = config,
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildFlowable(backpressureStrategy)
}

/**
 * Constructs a `Flowable<PagedList>`, from this [PagingSource] factory, convenience for
 * [RxPagedListBuilder].
 *
 * The returned [Flowable] will already be subscribed on the [fetchScheduler], and will perform
 * all loading on that scheduler. It will already be observed on [notifyScheduler], and will
 * dispatch new [PagedList]s, as well as their updates to that scheduler.
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagingSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param notifyScheduler [Scheduler] that receives [PagedList] updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI / main thread.
 * @param fetchScheduler [Scheduler] used to fetch from [PagingSource]s, generally a background
 * thread pool for e.g. I/O or network loading.
 * @param backpressureStrategy [BackpressureStrategy] for the [Flowable] to use.
 *
 * @see RxPagedListBuilder
 * @see toObservable
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "PagedList is deprecated and has been replaced by PagingData",
    replaceWith = ReplaceWith(
        """Pager(
            PagingConfig(pageSize),
            initialLoadKey,
            this
        ).flowable""",
        "androidx.paging.Pager",
        "androidx.paging.PagingConfig",
        "androidx.paging.rxjava3.flowable"
    )
)
fun <Key : Any, Value : Any> (() -> PagingSource<Key, Value>).toFlowable(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchScheduler: Scheduler? = null,
    notifyScheduler: Scheduler? = null,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<PagedList<Value>> {
    return createRxPagedListBuilder(
        pagingSourceFactory = this,
        config = Config(pageSize),
        initialLoadKey = initialLoadKey,
        boundaryCallback = boundaryCallback,
        fetchScheduler = fetchScheduler,
        notifyScheduler = notifyScheduler
    ).buildFlowable(backpressureStrategy)
}
