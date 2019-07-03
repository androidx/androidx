/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.paging.DataSource.InvalidatedCallback
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Scheduler
import io.reactivex.functions.Cancellable
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor

/**
 * Builder for `Observable<PagedList>` or `Flowable<PagedList>`, given a [DataSource.Factory] and a
 * [PagedList.Config].
 *
 * The required parameters are in the constructor, so you can simply construct and build, or
 * optionally enable extra features (such as initial load key, or BoundaryCallback).
 *
 * The returned observable/flowable will already be subscribed on the [setFetchScheduler], and will
 * perform all loading on that scheduler. It will already be observed on [setNotifyScheduler], and
 * will dispatch new PagedLists, as well as their updates to that scheduler.
 *
 * @param Key Type of input valued used to load data from the DataSource. Must be integer if you're
 *            using PositionalDataSource.
 * @param Value Item type being presented.
 *
 * @constructor Creates a RxPagedListBuilder with required parameters.
 * @param dataSourceFactory DataSource factory providing DataSource generations.
 * @param config Paging configuration.
 */
class RxPagedListBuilder<Key : Any, Value : Any>(
    private val dataSourceFactory: DataSource.Factory<Key, Value>,
    private val config: PagedList.Config
) {
    private var initialLoadKey: Key? = null
    private var boundaryCallback: PagedList.BoundaryCallback<Value>? = null
    private lateinit var notifyExecutor: Executor
    private lateinit var notifyScheduler: Scheduler
    private lateinit var fetchExecutor: Executor
    private lateinit var fetchScheduler: Scheduler

    /**
     * Creates a RxPagedListBuilder with required parameters.
     *
     * This method is a convenience for:
     * ```
     * RxPagedListBuilder(dataSourceFactory,
     *         new PagedList.Config.Builder().setPageSize(pageSize).build())
     * ```
     *
     * @param dataSourceFactory [DataSource.Factory] providing DataSource generations.
     * @param pageSize Size of pages to load.
     */
    constructor(dataSourceFactory: DataSource.Factory<Key, Value>, pageSize: Int) : this(
        dataSourceFactory,
        PagedList.Config.Builder().setPageSize(pageSize).build()
    )

    /**
     * First loading key passed to the first PagedList/DataSource.
     *
     * When a new PagedList/DataSource pair is created after the first, it acquires a load key from
     * the previous generation so that data is loaded around the position already being observed.
     *
     * @param key Initial load key passed to the first PagedList/DataSource.
     * @return this
     */
    fun setInitialLoadKey(key: Key?) = apply {
        initialLoadKey = key
    }

    /**
     * Sets a [androidx.paging.PagedList.BoundaryCallback] on each PagedList created,
     * typically used to load additional data from network when paging from local storage.
     *
     * Pass a BoundaryCallback to listen to when the PagedList runs out of data to load. If this
     * method is not called, or `null` is passed, you will not be notified when each
     * DataSource runs out of data to provide to its PagedList.
     *
     * If you are paging from a DataSource.Factory backed by local storage, you can set a
     * BoundaryCallback to know when there is no more information to page from local storage.
     * This is useful to page from the network when local storage is a cache of network data.
     *
     * Note that when using a BoundaryCallback with a `Observable<PagedList>`, method calls
     * on the callback may be dispatched multiple times - one for each PagedList/DataSource
     * pair. If loading network data from a BoundaryCallback, you should prevent multiple
     * dispatches of the same method from triggering multiple simultaneous network loads.
     *
     * @param boundaryCallback The boundary callback for listening to PagedList load state.
     * @return this
     */
    fun setBoundaryCallback(boundaryCallback: PagedList.BoundaryCallback<Value>?) = apply {
        this.boundaryCallback = boundaryCallback
    }

    /**
     * Sets scheduler which will be used for observing new PagedLists, as well as loading updates
     * within the PagedLists.
     *
     * If not set, defaults to the UI thread.
     *
     * The built [Observable] / [Flowable] will be observed on this scheduler, so that the thread
     * receiving PagedLists will also receive the internal updates to the PagedList.
     *
     * @param scheduler Scheduler that receives PagedList updates, and where [PagedList.Callback]
     * calls are dispatched. Generally, this is the UI/main thread.
     * @return this
     */
    fun setNotifyScheduler(scheduler: Scheduler) = apply {
        notifyExecutor = when (scheduler) {
            is Executor -> scheduler
            else -> ScheduledExecutor(scheduler)
        }
        notifyScheduler = scheduler
    }

    /**
     * Sets scheduler which will be used for background fetching of PagedLists, as well as on-demand
     * fetching of pages inside.
     *
     * If not set, defaults to the Arch components I/O thread pool.
     *
     * The built [Observable] / [Flowable] will be subscribed on this scheduler.
     *
     * @param scheduler [Scheduler] used to fetch from DataSources, generally a background thread
     * pool for e.g. I/O or network loading.
     * @return this
     */
    fun setFetchScheduler(scheduler: Scheduler) = apply {
        fetchExecutor = when (scheduler) {
            is Executor -> scheduler
            else -> ScheduledExecutor(scheduler)
        }
        fetchScheduler = scheduler
    }

    /**
     * Constructs a `Observable<PagedList>`.
     *
     * The returned Observable will already be observed on the [notifyScheduler], and subscribed on
     * the [fetchScheduler].
     *
     * @return The [Observable] of PagedLists
     */
    fun buildObservable(): Observable<PagedList<Value>> {
        if (!::notifyExecutor.isInitialized) {
            val scheduledExecutor = ScheduledExecutor(ArchTaskExecutor.getMainThreadExecutor())
            notifyExecutor = scheduledExecutor
            notifyScheduler = scheduledExecutor
        }
        if (!::fetchExecutor.isInitialized) {
            val scheduledExecutor = ScheduledExecutor(ArchTaskExecutor.getIOThreadExecutor())
            fetchExecutor = scheduledExecutor
            fetchScheduler = scheduledExecutor
        }

        return Observable
            .create(
                PagingObservableOnSubscribe(
                    initialLoadKey,
                    config,
                    boundaryCallback,
                    dataSourceFactory,
                    notifyExecutor,
                    fetchExecutor
                )
            )
            .observeOn(notifyScheduler)
            .subscribeOn(fetchScheduler)
    }

    /**
     * Constructs a `Flowable<PagedList>`.
     *
     * The returned Observable will already be observed on the [notifyScheduler], and subscribed on
     * the [fetchScheduler].
     *
     * @param backpressureStrategy BackpressureStrategy for the [Flowable] to use.
     * @return The [Flowable] of PagedLists
     */
    fun buildFlowable(backpressureStrategy: BackpressureStrategy): Flowable<PagedList<Value>> {
        return buildObservable().toFlowable(backpressureStrategy)
    }

    internal class PagingObservableOnSubscribe<Key : Any, Value : Any>(
        private val initialLoadKey: Key?,
        private val config: PagedList.Config,
        private val boundaryCallback: PagedList.BoundaryCallback<Value>?,
        private val dataSourceFactory: DataSource.Factory<Key, Value>,
        private val notifyExecutor: Executor,
        private val fetchExecutor: Executor
    ) : ObservableOnSubscribe<PagedList<Value>>, InvalidatedCallback, Cancellable, Runnable {
        private lateinit var list: PagedList<Value>
        private var dataSource: DataSource<Key, Value>? = null
        private lateinit var emitter: ObservableEmitter<PagedList<Value>>

        override fun onInvalidated() {
            if (!emitter.isDisposed) {
                fetchExecutor.execute(this)
            }
        }

        override fun subscribe(emitter: ObservableEmitter<PagedList<Value>>) {
            this.emitter = emitter
            emitter.setCancellable(this)

            // known that subscribe is already on fetchScheduler
            emitter.onNext(createPagedList())
        }

        override fun cancel() {
            dataSource?.removeInvalidatedCallback(this)
        }

        override fun run() {
            // fetch data, run on fetchExecutor
            emitter.onNext(createPagedList())
        }

        // TODO: Convert this runBlocking to async with a subscribeOn.
        // for getLastKey cast, and Builder.build()
        private fun createPagedList(): PagedList<Value> = runBlocking {
            @Suppress("UNCHECKED_CAST")
            val initializeKey = if (::list.isInitialized) list.lastKey as Key? else initialLoadKey

            do {
                dataSource?.removeInvalidatedCallback(this@PagingObservableOnSubscribe)
                val newDataSource = dataSourceFactory.create()
                newDataSource.addInvalidatedCallback(this@PagingObservableOnSubscribe)
                dataSource = newDataSource

                @Suppress("DEPRECATION")
                list = PagedList.Builder(newDataSource, config)
                    .setNotifyExecutor(notifyExecutor)
                    .setFetchExecutor(fetchExecutor)
                    .setBoundaryCallback(boundaryCallback)
                    .setInitialKey(initializeKey)
                    .build()
            } while (list.isDetached)
            list
        }
    }
}
