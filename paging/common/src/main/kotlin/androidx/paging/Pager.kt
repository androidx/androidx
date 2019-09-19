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

import androidx.paging.PagedSource.KeyProvider
import androidx.paging.PagedSource.LoadParams
import androidx.paging.PagedSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class Pager<K : Any, V : Any>(
    private val pagedListScope: CoroutineScope,
    val config: PagedList.Config,
    val source: PagedSource<K, V>,
    private val notifyDispatcher: CoroutineDispatcher,
    private val fetchDispatcher: CoroutineDispatcher,
    val pageConsumer: PageConsumer<V>,
    result: PagedSource.LoadResult.Page<K, V>,
    private val adjacentProvider: AdjacentProvider<V> = SimpleAdjacentProvider()
) {
    private val totalCount: Int
    private var prevKey: K? = null
    private var nextKey: K? = null
    private val detached = AtomicBoolean(false)

    var loadStateManager = object : PagedList.LoadStateManager() {
        override fun onStateChanged(type: LoadType, state: LoadState) {
            pageConsumer.onStateChanged(type, state)
        }
    }

    val isDetached
        get() = detached.get()

    init {
        prevKey = result.prevKey
        nextKey = result.nextKey
        this.adjacentProvider.onPageResultResolution(LoadType.REFRESH, result)
        totalCount = when (result.counted) {
            // only one of leadingNulls / offset may be used
            true -> result.itemsBefore + result.data.size + result.itemsAfter
            else -> COUNT_UNDEFINED
        }
    }

    private fun scheduleLoad(type: LoadType, params: LoadParams<K>) {
        // Listen on the BG thread if the paged source is invalid, since it can be expensive.
        pagedListScope.launch(fetchDispatcher) {
            val value = source.load(params)

            // if invalid, drop result on the floor
            if (source.invalid) {
                detach()
                return@launch
            }

            // Source has been verified to be valid after producing data, so sent data to UI
            launch(notifyDispatcher) {
                when (value) {
                    is PagedSource.LoadResult.Page -> onLoadSuccess(type, value)
                    is PagedSource.LoadResult.Error -> onLoadError(type, value.throwable)
                }
            }
        }
    }

    private fun onLoadSuccess(type: LoadType, value: PagedSource.LoadResult.Page<K, V>) {
        if (isDetached) return // abort!

        adjacentProvider.onPageResultResolution(type, value)

        if (pageConsumer.onPageResult(type, value)) {
            when (type) {
                LoadType.START -> {
                    prevKey = value.prevKey
                    schedulePrepend()
                }
                LoadType.END -> {
                    nextKey = value.nextKey
                    scheduleAppend()
                }
                else -> throw IllegalStateException("Can only fetch more during append/prepend")
            }
        } else {
            if (value.data.isEmpty()) {
                loadStateManager.setState(type, LoadState.Done)
            } else {
                loadStateManager.setState(type, LoadState.Idle)
            }
        }
    }

    private fun onLoadError(type: LoadType, throwable: Throwable) {
        if (isDetached) return // abort!

        val state = LoadState.Error(throwable)
        loadStateManager.setState(type, state)
    }

    fun trySchedulePrepend() {
        if (loadStateManager.startState is LoadState.Idle) schedulePrepend()
    }

    fun tryScheduleAppend() {
        if (loadStateManager.endState is LoadState.Idle) scheduleAppend()
    }

    private fun canPrepend() = when (totalCount) {
        // don't know count / position from initial load, so be conservative, return true
        COUNT_UNDEFINED -> true
        // position is known, do we have space left?
        else -> adjacentProvider.firstLoadedItemIndex > 0
    }

    private fun canAppend() = when (totalCount) {
        // don't know count / position from initial load, so be conservative, return true
        COUNT_UNDEFINED -> true
        // count is known, do we have space left?
        else -> adjacentProvider.lastLoadedItemIndex < totalCount - 1
    }

    private fun schedulePrepend() {
        if (!canPrepend()) {
            onLoadSuccess(LoadType.START, PagedSource.LoadResult.Page.empty())
            return
        }

        val key = when (val keyProvider = source.keyProvider) {
            is KeyProvider.Positional -> {
                @Suppress("UNCHECKED_CAST")
                (adjacentProvider.firstLoadedItemIndex - 1) as K
            }
            is KeyProvider.PageKey -> prevKey
            is KeyProvider.ItemKey -> keyProvider.getKey(adjacentProvider.firstLoadedItem!!)
        }

        loadStateManager.setState(LoadType.START, LoadState.Loading)

        val loadParams = LoadParams(
            LoadType.START,
            key,
            config.pageSize,
            config.enablePlaceholders,
            config.pageSize
        )
        scheduleLoad(LoadType.START, loadParams)
    }

    private fun scheduleAppend() {
        if (!canAppend()) {
            onLoadSuccess(LoadType.END, PagedSource.LoadResult.Page.empty())
            return
        }

        val key = when (val keyProvider = source.keyProvider) {
            is KeyProvider.Positional -> {
                @Suppress("UNCHECKED_CAST")
                (adjacentProvider.lastLoadedItemIndex + 1) as K
            }
            is KeyProvider.PageKey -> nextKey
            is KeyProvider.ItemKey -> keyProvider.getKey(
                adjacentProvider.lastLoadedItem!!
            )
        }

        loadStateManager.setState(LoadType.END, LoadState.Loading)
        val loadParams = LoadParams(
            LoadType.END,
            key,
            config.pageSize,
            config.enablePlaceholders,
            config.pageSize
        )
        scheduleLoad(LoadType.END, loadParams)
    }

    fun retry() {
        loadStateManager.startState.run {
            if (this is LoadState.Error) schedulePrepend()
        }
        loadStateManager.endState.run {
            if (this is LoadState.Error) scheduleAppend()
        }
    }

    fun detach() = detached.set(true)

    internal interface PageConsumer<V : Any> {
        /**
         * @return `true` if we need to fetch more
         */
        fun onPageResult(type: LoadType, page: PagedSource.LoadResult.Page<*, V>): Boolean

        fun onStateChanged(type: LoadType, state: LoadState)
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
        fun onPageResultResolution(type: LoadType, result: PagedSource.LoadResult.Page<*, V>)
    }

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

        override fun onPageResultResolution(
            type: LoadType,
            result: PagedSource.LoadResult.Page<*, V>
        ) {
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
                firstLoadedItemIndex = result.itemsBefore
                lastLoadedItemIndex = firstLoadedItemIndex + result.data.size - 1
                firstLoadedItem = result.data[0]
                lastLoadedItem = result.data.last()

                counted = result.counted
                if (counted) {
                    leadingUnloadedCount = result.itemsBefore
                    trailingUnloadedCount = result.itemsAfter
                }
            }
        }
    }
}
