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
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

internal class LivePagedList<Key : Any, Value : Any>(
    private val coroutineScope: CoroutineScope,
    initialKey: Key?,
    private val config: PagedList.Config,
    private val boundaryCallback: PagedList.BoundaryCallback<Value>?,
    private val pagedSourceFactory: () -> PagedSource<Key, Value>,
    private val notifyDispatcher: CoroutineDispatcher,
    private val fetchDispatcher: CoroutineDispatcher
) : LiveData<PagedList<Value>>() {
    private var currentData: PagedList<Value>
    private var currentJob: Job? = null

    private val callback = { invalidate(true) }

    private val refreshRetryCallback = Runnable { invalidate(true) }

    init {
        currentData = InitialPagedList(
            pagedSourceFactory(),
            coroutineScope,
            config,
            initialKey
        )
        currentData.setRetryCallback(refreshRetryCallback)
        value = currentData
    }

    override fun onActive() {
        super.onActive()
        invalidate(false)
    }

    private fun invalidate(force: Boolean) {
        // work is already ongoing, not forcing, so skip invalidate
        if (currentJob != null && !force) return

        currentJob?.cancel()
        currentJob = coroutineScope.launch(fetchDispatcher) {
            currentData.pagedSource.unregisterInvalidatedCallback(callback)
            val pagedSource = pagedSourceFactory()
            pagedSource.registerInvalidatedCallback(callback)

            withContext(notifyDispatcher) {
                currentData.setInitialLoadState(REFRESH, Loading)
            }

            @Suppress("UNCHECKED_CAST")
            val lastKey = currentData.lastKey as Key?
            val params = config.toRefreshLoadParams(lastKey)
            when (val initialResult = pagedSource.load(params)) {
                is PagedSource.LoadResult.Error -> {
                    currentData.setInitialLoadState(REFRESH, Error(initialResult.throwable))
                }
                is PagedSource.LoadResult.Page -> {
                    val pagedList = PagedList.create(
                        pagedSource,
                        initialResult,
                        coroutineScope,
                        notifyDispatcher,
                        fetchDispatcher,
                        boundaryCallback,
                        config,
                        lastKey
                    )
                    onItemUpdate(currentData, pagedList)
                    currentData = pagedList
                    postValue(pagedList)
                }
            }
        }
    }

    private fun onItemUpdate(previous: PagedList<Value>, next: PagedList<Value>) {
        previous.setRetryCallback(null)
        next.setRetryCallback(refreshRetryCallback)
    }
}

/**
 * Constructs a `LiveData<PagedList>`, from this `DataSource.Factory`, convenience for
 * [LivePagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first [PagedList] is deferred
 * until the [LiveData] is observed.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagedSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param fetchExecutor [Executor] for fetching data from [PagedSource]s.
 *
 * @see LivePagedListBuilder
 */
@Deprecated("DataSource is deprecated and has been replaced by PagedSource")
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toLiveData(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor()
): LiveData<PagedList<Value>> {
    @Suppress("DEPRECATION")
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
 * No work (such as loading) is done immediately, the creation of the first [PagedList] is deferred
 * until the [LiveData] is observed.
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagedSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @see LivePagedListBuilder
 */
@Deprecated("DataSource is deprecated and has been replaced by PagedSource")
fun <Key : Any, Value : Any> DataSource.Factory<Key, Value>.toLiveData(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor()
): LiveData<PagedList<Value>> {
    @Suppress("DEPRECATION")
    return LivePagedListBuilder(this, Config(pageSize))
        .setInitialLoadKey(initialLoadKey)
        .setBoundaryCallback(boundaryCallback)
        .setFetchExecutor(fetchExecutor)
        .build()
}

/**
 * Constructs a `LiveData<PagedList>`, from this PagedSource factory, convenience for
 * [LivePagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first [PagedList] is deferred
 * until the [LiveData] is observed.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagedSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param coroutineScope Set the [CoroutineScope] that page loads should be launched within. The
 * set [coroutineScope] allows a [PagedSource] to cancel running load operations when the results
 * are no longer needed - for example, when the containing activity is destroyed.
 *
 * Defaults to [GlobalScope].
 * @param fetchDispatcher [CoroutineDispatcher] for fetching data from [PagedSource]s.
 *
 * @see LivePagedListBuilder
 */
fun <Key : Any, Value : Any> (() -> PagedSource<Key, Value>).toLiveData(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    coroutineScope: CoroutineScope = GlobalScope,
    fetchDispatcher: CoroutineDispatcher = Dispatchers.IO
): LiveData<PagedList<Value>> {
    return LivePagedList(
        coroutineScope,
        initialLoadKey,
        config,
        boundaryCallback,
        this,
        Dispatchers.Main.immediate,
        fetchDispatcher
    )
}

/**
 * Constructs a `LiveData<PagedList>`, from this PagedSource factory, convenience for
 * [LivePagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first [PagedList] is deferred
 * until the [LiveData] is observed.
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first [PagedList] / [PagedSource].
 * @param boundaryCallback The boundary callback for listening to [PagedList] load state.
 * @param coroutineScope Set the [CoroutineScope] that page loads should be launched within. The
 * set [coroutineScope] allows a [PagedSource] to cancel running load operations when the results
 * are no longer needed - for example, when the containing activity is destroyed.
 *
 * Defaults to [GlobalScope].
 * @param fetchDispatcher [CoroutineDispatcher] for fetching data from [PagedSource]s.
 *
 * @see LivePagedListBuilder
 */
fun <Key : Any, Value : Any> (() -> PagedSource<Key, Value>).toLiveData(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    coroutineScope: CoroutineScope = GlobalScope,
    fetchDispatcher: CoroutineDispatcher = Dispatchers.IO
): LiveData<PagedList<Value>> {
    return LivePagedList(
        coroutineScope,
        initialLoadKey,
        PagedList.Config.Builder().setPageSize(pageSize).build(),
        boundaryCallback,
        this,
        Dispatchers.Main.immediate,
        fetchDispatcher
    )
}
