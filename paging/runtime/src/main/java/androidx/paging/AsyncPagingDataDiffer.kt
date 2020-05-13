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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Helper class for mapping a [PagingData] into a
 * [RecyclerView.Adapter][androidx.recyclerview.widget.RecyclerView.Adapter].
 *
 * For simplicity, [PagingDataAdapter] can often be used in place of this class.
 * [AsyncPagingDataDiffer] is exposed for complex cases, and where overriding [PagingDataAdapter] to
 * support paging isn't convenient.
 */
class AsyncPagingDataDiffer<T : Any>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val updateCallback: ListUpdateCallback
) {
    internal val callback = object : PresenterCallback {
        override fun onInserted(position: Int, count: Int) {
            updateCallback.onInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) =
            updateCallback.onRemoved(position, count)

        override fun onChanged(position: Int, count: Int) {
            // NOTE: pass a null payload to convey null -> item, or item -> null
            updateCallback.onChanged(position, count, null)
        }

        override fun onStateUpdate(loadType: LoadType, loadState: LoadState) {
            when (loadType) {
                REFRESH -> {
                    if (loadState != loadStates[REFRESH]) {
                        loadStates[REFRESH] = loadState
                        dispatchLoadState(REFRESH, loadState)
                    }
                }
                PREPEND -> {
                    if (loadState != loadStates[PREPEND]) {
                        loadStates[PREPEND] = loadState
                        dispatchLoadState(PREPEND, loadState)
                    }
                }
                APPEND -> {
                    if (loadState != loadStates[APPEND]) {
                        loadStates[APPEND] = loadState
                        dispatchLoadState(APPEND, loadState)
                    }
                }
            }
        }

        private fun dispatchLoadState(type: LoadType, state: LoadState) {
            loadStateListeners.forEach { it(type, state) }
        }
    }

    /** True if we're currently executing [getItem] */
    internal var inGetItem: Boolean = false

    private val differBase = object : PagingDataDiffer<T>(mainDispatcher) {
        override suspend fun performDiff(
            previousList: NullPaddedList<T>,
            newList: NullPaddedList<T>,
            newLoadStates: Map<LoadType, LoadState>,
            lastAccessedIndex: Int
        ): Int? {
            return withContext(mainDispatcher) {
                when {
                    previousList.size == 0 -> {
                        // fast path for no items -> some items
                        callback.onInserted(0, newList.size)
                        newLoadStates.entries.forEach { callback.onStateUpdate(it.key, it.value) }
                        return@withContext null
                    }
                    newList.size == 0 -> {
                        // fast path for some items -> no items
                        callback.onRemoved(0, previousList.size)
                        newLoadStates.entries.forEach { callback.onStateUpdate(it.key, it.value) }
                        return@withContext null
                    }
                    else -> { // full diff
                        val diffResult = withContext(workerDispatcher) {
                            previousList.computeDiff(newList, diffCallback)
                        }

                        previousList.dispatchDiff(updateCallback, newList, diffResult)
                        newLoadStates.entries.forEach { callback.onStateUpdate(it.key, it.value) }

                        return@withContext previousList.transformAnchorIndex(
                            diffResult = diffResult,
                            newList = newList,
                            oldPosition = lastAccessedIndex
                        )
                    }
                }
            }
        }

        /**
         * Return if [getItem] is running to post any data modifications.
         *
         * This must be done because RecyclerView can't be modified during an onBind, when
         * [getItem] is generally called.
         */
        override fun postEvents(): Boolean {
            return inGetItem
        }
    }

    private val job = AtomicReference<Job?>(null)

    /**
     * Present a [PagingData] until it is either invalidated or another call to [submitData] is
     * made.
     *
     * [submitData] should be called on the same [CoroutineDispatcher] where updates will be
     * dispatched to UI, typically [Dispatchers.Main]. (this is done for you if you use
     * `lifecycleScope.launch {}`).
     *
     * This method is typically used when collecting from a [Flow][kotlinx.coroutines.flow.Flow]
     * produced by [Pager]. For RxJava or LiveData support, use the non-suspending overload of
     * [submitData], which accepts a [Lifecycle].
     *
     * @see [Pager]
     */
    suspend fun submitData(pagingData: PagingData<T>) {
        try {
            job.get()?.cancelAndJoin()
        } finally {
            differBase.collectFrom(pagingData, callback)
        }
    }

    /**
     * Present a [PagingData] until it is either invalidated or another call to [submitData] is
     * made.
     *
     * This method is typically used when observing a RxJava or LiveData stream produced by [Pager].
     * For [Flow][kotlinx.coroutines.flow.Flow] support, use the suspending overload of
     * [submitData], which automates cancellation via
     * [CoroutineScope][kotlinx.coroutines.CoroutineScope] instead of relying of [Lifecycle].
     *
     * @see submitData
     * @see [Pager]
     */
    fun submitData(lifecycle: Lifecycle, pagingData: PagingData<T>) {
        var oldJob: Job?
        var newJob: Job
        do {
            oldJob = job.get()
            newJob = lifecycle.coroutineScope.launch(start = CoroutineStart.LAZY) {
                oldJob?.cancelAndJoin()
                differBase.collectFrom(pagingData, callback)
            }
        } while (!job.compareAndSet(oldJob, newJob))
        newJob.start()
    }

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     * [AsyncPagingDataDiffer].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    fun retry() {
        differBase.retry()
    }

    /**
     * Refresh the data presented by this [AsyncPagingDataDiffer].
     *
     * [refresh] triggers the creation of a new [PagingData] with a new instance of [PagingSource]
     * to represent an updated snapshot of the backing dataset. If a [RemoteMediator] is set,
     * calling [refresh] will also trigger a call to [RemoteMediator.load] with [LoadType] [REFRESH]
     * to allow [RemoteMediator] to check for updates to the dataset backing [PagingSource].
     *
     * Note: This API is intended for UI-driven refresh signals, such as swipe-to-refresh.
     * Invalidation due repository-layer signals, such as DB-updates, should instead use
     * [PagingSource.invalidate].
     *
     * @see PagingSource.invalidate
     *
     * @sample androidx.paging.samples.refreshSample
     */
    fun refresh() {
        differBase.refresh()
    }

    /**
     * Get the item from the current PagedList at the specified index.
     *
     * Note that this operates on both loaded items and null padding within the PagedList.
     *
     * @param index Index of item to get, must be >= 0, and < [itemCount]
     * @return The item, or null, if a null placeholder is at the specified position.
     */
    fun getItem(index: Int): T? {
        try {
            inGetItem = true
            return differBase[index]
        } finally {
            inGetItem = false
        }
    }

    /**
     * Get the number of items currently presented by this Differ. This value can be directly
     * returned to [androidx.recyclerview.widget.RecyclerView.Adapter.getItemCount].
     *
     * @return Number of items being presented.
     */
    val itemCount: Int
        get() = differBase.size

    internal val loadStateListeners: MutableList<(LoadType, LoadState) -> Unit> =
        CopyOnWriteArrayList()

    internal val loadStates = mutableMapOf<LoadType, LoadState>(
        REFRESH to LoadState.NotLoading(endOfPaginationReached = false, fromMediator = false),
        PREPEND to LoadState.NotLoading(endOfPaginationReached = false, fromMediator = false),
        APPEND to LoadState.NotLoading(endOfPaginationReached = false, fromMediator = false)
    )

    /**
     * Add a listener to observe the loading state.
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect current [LoadType.REFRESH], [LoadType.PREPEND], and [LoadType.APPEND] states.
     *
     * @param listener to receive [LoadState] updates.
     *
     * @see removeLoadStateListener
     */
    fun addLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        // Note: Important to add the listener first before sending off events, in case the
        // callback triggers removal, which could lead to a leak if the listener is added
        // afterwards.
        loadStateListeners.add(listener)
        listener(REFRESH, loadStates[REFRESH]!!)
        if (loadStateListeners.contains(listener)) listener(PREPEND, loadStates[PREPEND]!!)
        if (loadStateListeners.contains(listener)) listener(APPEND, loadStates[APPEND]!!)
    }

    /**
     * Remove a previously registered load state listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    fun removeLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        loadStateListeners.remove(listener)
    }
}