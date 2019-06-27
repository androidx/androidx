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

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.LiveData
import androidx.paging.futures.FutureCallback
import androidx.paging.futures.addCallback
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executor

internal class LivePagedList<Key : Any, Value : Any>(
    private val coroutineScope: CoroutineScope,
    initialKey: Key?,
    private val config: PagedList.Config,
    private val boundaryCallback: PagedList.BoundaryCallback<Value>?,
    private val dataSourceFactory: DataSource.Factory<Key, Value>,
    private val notifyExecutor: Executor,
    private val fetchExecutor: Executor
) : LiveData<PagedList<Value>>(), FutureCallback<PagedList<Value>> {
    private var currentData: PagedList<Value>
    private var currentFuture: ListenableFuture<PagedList<Value>>? = null

    private val callback = { invalidate(true) }

    private val refreshRetryCallback = Runnable { invalidate(true) }

    init {
        currentData =
            InitialPagedList(dataSourceFactory.create(), coroutineScope, config, initialKey)
        onSuccess(currentData)
    }

    override fun onActive() {
        super.onActive()
        invalidate(false)
    }

    override fun onError(throwable: Throwable) {
        val loadState = if (currentData.dataSource.isRetryableError(throwable)) {
            PagedList.LoadState.RETRYABLE_ERROR
        } else {
            PagedList.LoadState.ERROR
        }

        currentData.setInitialLoadState(loadState, throwable)
    }

    override fun onSuccess(value: PagedList<Value>) {
        onItemUpdate(currentData, value)
        currentData = value
        setValue(value)
    }

    private fun invalidate(force: Boolean) {
        // work is already ongoing, not forcing, so skip invalidate
        if (currentFuture != null && !force) return

        currentFuture?.cancel(false)
        currentFuture = createPagedList().also { it.addCallback(this, notifyExecutor) }
    }

    private fun onItemUpdate(previous: PagedList<Value>, next: PagedList<Value>) {
        previous.setRetryCallback(null)
        next.setRetryCallback(refreshRetryCallback)
    }

    private fun createPagedList(): ListenableFuture<PagedList<Value>> {
        val dataSource = dataSourceFactory.create()
        currentData.dataSource.removeInvalidatedCallback(callback)
        dataSource.addInvalidatedCallback(callback)
        currentData.setInitialLoadState(PagedList.LoadState.LOADING, null)

        @Suppress("UNCHECKED_CAST") // getLastKey guaranteed to be of 'Key' type
        val lastKey = currentData.lastKey as Key?
        return PagedList.create(
            dataSource,
            coroutineScope,
            notifyExecutor,
            fetchExecutor,
            fetchExecutor,
            boundaryCallback,
            config,
            lastKey
        )
    }
}

/**
 * Constructs a `LiveData<PagedList>`, from this `DataSource.Factory`, convenience for
 * [LivePagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first PagedList is is
 * deferred until the LiveData is observed.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @see LivePagedListBuilder
 */
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toLiveData(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor()
): LiveData<PagedList<Value>> {
    return LivePagedListBuilder(this, config)
        .setInitialLoadKey(initialLoadKey)
        .setBoundaryCallback(boundaryCallback)
        .setFetchExecutor(fetchExecutor)
        .build()
}

/**
 * Constructs a `LiveData<PagedList>`, from this `DataSource.Factory`, convenience for
 * [LivePagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first PagedList is is
 * deferred until the LiveData is observed.
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @see LivePagedListBuilder
 */
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toLiveData(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor()
): LiveData<PagedList<Value>> {
    return LivePagedListBuilder(this, Config(pageSize))
        .setInitialLoadKey(initialLoadKey)
        .setBoundaryCallback(boundaryCallback)
        .setFetchExecutor(fetchExecutor)
        .build()
}
