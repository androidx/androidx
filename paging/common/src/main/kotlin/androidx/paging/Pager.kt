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

import androidx.paging.PagedList.LoadState
import androidx.paging.PagedList.LoadType
import androidx.paging.futures.FutureCallback
import androidx.paging.futures.addCallback
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class Pager<K : Any, V : Any>(
    val config: PagedList.Config,
    val source: PagedSource<K, V>,
    val notifyExecutor: Executor,
    private val fetchExecutor: Executor,
    val pageConsumer: PageConsumer<V>,
    adjacentProvider: AdjacentProvider<V>?,
    result: DataSource.BaseResult<V>
) {
    private val totalCount: Int
    private val adjacentProvider: AdjacentProvider<V>
    private var prevKey: K? = null
    private var nextKey: K? = null
    private val detached = AtomicBoolean(false)

    var loadStateManager = object : PagedList.LoadStateManager() {
        override fun onStateChanged(type: LoadType, state: LoadState, error: Throwable?) =
            pageConsumer.onStateChanged(type, state, error)
    }

    val isDetached
        get() = detached.get()

    init {
        this.adjacentProvider = adjacentProvider ?: SimpleAdjacentProvider()
        @Suppress("UNCHECKED_CAST")
        prevKey = result.prevKey as K?
        @Suppress("UNCHECKED_CAST")
        nextKey = result.nextKey as K?
        this.adjacentProvider.onPageResultResolution(LoadType.REFRESH, result)
        totalCount = result.totalCount()
    }

    private fun listenTo(
        type: LoadType,
        future: ListenableFuture<out PagedSource.LoadResult<K, V>>
    ) {
        // First listen on the BG thread if the DataSource is invalid, since it can be expensive
        future.addListener(Runnable {
            // if invalid, drop result on the floor
            if (source.invalid) {
                detach()
                return@Runnable
            }

            // Source has been verified to be valid after producing data, so sent data to UI
            future.addCallback(
                object : FutureCallback<PagedSource.LoadResult<K, V>> {
                    override fun onSuccess(value: PagedSource.LoadResult<K, V>) =
                        onLoadSuccess(type, object : DataSource.BaseResult<V>(
                            value.data,
                            value.prevKey,
                            value.nextKey,
                            value.itemsBefore,
                            value.itemsAfter,
                            value.offset,
                            value.counted
                        ) {})

                    override fun onError(throwable: Throwable) = onLoadError(type, throwable)
                },
                notifyExecutor
            )
        }, fetchExecutor)
    }

    internal interface PageConsumer<V : Any> {
        /**
         * @return `true` if we need to fetch more
         */
        fun onPageResult(type: LoadType, pageResult: DataSource.BaseResult<V>): Boolean

        fun onStateChanged(type: LoadType, state: LoadState, error: Throwable?)
    }

    internal interface AdjacentProvider<V : Any> {
        val firstLoadedItem: V?
        val lastLoadedItem: V?
        val firstLoadedItemIndex: Int
        val lastLoadedItemIndex: Int

        /**
         * Notify the [AdjacentProvider] of new loaded data, to update first/last item/index.
         *
         * NOTE: this data may not be committed (e.g. it may be dropped due to max size). Up to the
         * implementation of the AdjacentProvider to handle this (generally by ignoring this call if
         * dropping is supported).
         */
        fun onPageResultResolution(type: LoadType, result: DataSource.BaseResult<V>)
    }

    fun onLoadSuccess(type: LoadType, value: DataSource.BaseResult<V>) {
        if (isDetached) return // abort!

        adjacentProvider.onPageResultResolution(type, value)

        if (pageConsumer.onPageResult(type, value)) {
            when (type) {
                LoadType.START -> {
                    @Suppress("UNCHECKED_CAST")
                    prevKey = value.prevKey as K?
                    schedulePrepend()
                }
                LoadType.END -> {
                    @Suppress("UNCHECKED_CAST")
                    nextKey = value.nextKey as K?
                    scheduleAppend()
                }
                else -> throw IllegalStateException("Can only fetch more during append/prepend")
            }
        } else {
            val state = if (value.data.isEmpty()) LoadState.DONE else LoadState.IDLE
            loadStateManager.setState(type, state, null)
        }
    }

    fun onLoadError(type: LoadType, throwable: Throwable) {
        if (isDetached) return // abort!

        // TODO: handle nesting
        val state = when {
            source.isRetryableError(throwable) -> LoadState.RETRYABLE_ERROR
            else -> LoadState.ERROR
        }
        loadStateManager.setState(type, state, throwable)
    }

    fun trySchedulePrepend() {
        if (loadStateManager.start == LoadState.IDLE) schedulePrepend()
    }

    fun tryScheduleAppend() {
        if (loadStateManager.end == LoadState.IDLE) scheduleAppend()
    }

    private fun canPrepend() = when (totalCount) {
        // don't know count / position from initial load, so be conservative, return true
        DataSource.BaseResult.TOTAL_COUNT_UNKNOWN -> true
        // position is known, do we have space left?
        else -> adjacentProvider.firstLoadedItemIndex > 0
    }

    private fun canAppend() = when (totalCount) {
        // don't know count / position from initial load, so be conservative, return true
        DataSource.BaseResult.TOTAL_COUNT_UNKNOWN -> true
        // count is known, do we have space left?
        else -> adjacentProvider.lastLoadedItemIndex < totalCount - 1
    }

    private fun schedulePrepend() {
        if (!canPrepend()) {
            onLoadSuccess(LoadType.START, DataSource.BaseResult.empty())
            return
        }

        val key = when (val keyProvider = source.keyProvider) {
            is PagedSource.KeyProvider.Positional -> {
                @Suppress("UNCHECKED_CAST")
                (adjacentProvider.firstLoadedItemIndex - 1) as K
            }
            is PagedSource.KeyProvider.PageKey -> prevKey
            is PagedSource.KeyProvider.ItemKey -> keyProvider.getKey(
                adjacentProvider.firstLoadedItem!!
            )
        }

        loadStateManager.setState(LoadType.START, LoadState.LOADING, null)
        listenTo(
            LoadType.START,
            source.load(
                PagedSource.LoadParams(
                    PagedSource.LoadType.START,
                    key,
                    config.initialLoadSizeHint,
                    config.enablePlaceholders,
                    config.pageSize
                )
            )
        )
    }

    private fun scheduleAppend() {
        if (!canAppend()) {
            onLoadSuccess(LoadType.END, DataSource.BaseResult.empty())
            return
        }

        val key = when (val keyProvider = source.keyProvider) {
            is PagedSource.KeyProvider.Positional ->
                @Suppress("UNCHECKED_CAST")
                (adjacentProvider.lastLoadedItemIndex + 1) as K
            is PagedSource.KeyProvider.PageKey -> nextKey
            is PagedSource.KeyProvider.ItemKey -> keyProvider.getKey(
                adjacentProvider.lastLoadedItem!!
            )
        }

        loadStateManager.setState(LoadType.END, LoadState.LOADING, null)
        listenTo(
            LoadType.END,
            source.load(
                PagedSource.LoadParams(
                    PagedSource.LoadType.END,
                    key,
                    config.initialLoadSizeHint,
                    config.enablePlaceholders,
                    config.pageSize
                )
            )
        )
    }

    fun retry() {
        if (loadStateManager.start == LoadState.RETRYABLE_ERROR) schedulePrepend()
        if (loadStateManager.end == LoadState.RETRYABLE_ERROR) scheduleAppend()
    }

    fun detach() = detached.set(true)

    internal class SimpleAdjacentProvider<V : Any> : AdjacentProvider<V> {
        override var firstLoadedItemIndex: Int = 0
            private set
        override var lastLoadedItemIndex: Int = 0
            private set
        override var firstLoadedItem: V? = null
            private set
        override var lastLoadedItem: V? = null
            private set

        private var counted: Boolean = false
        private var leadingUnloadedCount: Int = 0
        private var trailingUnloadedCount: Int = 0

        override fun onPageResultResolution(type: LoadType, result: DataSource.BaseResult<V>) {
            if (result.data.isEmpty()) return

            if (type == LoadType.START) {
                firstLoadedItemIndex -= result.data.size
                firstLoadedItem = result.data[0]
                if (counted) {
                    leadingUnloadedCount -= result.data.size
                }
            } else if (type == LoadType.END) {
                lastLoadedItemIndex += result.data.size
                lastLoadedItem = result.data.last()
                if (counted) {
                    trailingUnloadedCount -= result.data.size
                }
            } else {
                firstLoadedItemIndex = result.leadingNulls + result.offset
                lastLoadedItemIndex = firstLoadedItemIndex + result.data.size - 1
                firstLoadedItem = result.data[0]
                lastLoadedItem = result.data.last()

                if (result.counted) {
                    counted = true
                    leadingUnloadedCount = result.leadingNulls
                    trailingUnloadedCount = result.trailingNulls
                }
            }
        }
    }
}
