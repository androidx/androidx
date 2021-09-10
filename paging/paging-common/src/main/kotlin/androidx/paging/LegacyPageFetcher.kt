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

import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.PagingSource.LoadParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class LegacyPageFetcher<K : Any, V : Any>(
    private val pagedListScope: CoroutineScope,
    @Suppress("DEPRECATION")
    val config: PagedList.Config,
    val source: PagingSource<K, V>,
    private val notifyDispatcher: CoroutineDispatcher,
    private val fetchDispatcher: CoroutineDispatcher,
    val pageConsumer: PageConsumer<V>,
    private val keyProvider: KeyProvider<K>
) {
    private val detached = AtomicBoolean(false)

    @Suppress("DEPRECATION")
    var loadStateManager = object : PagedList.LoadStateManager() {
        override fun onStateChanged(type: LoadType, state: LoadState) {
            // Don't need to post - PagedList will already have done that
            pageConsumer.onStateChanged(type, state)
        }
    }

    val isDetached
        get() = detached.get()

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
                    is PagingSource.LoadResult.Page -> onLoadSuccess(type, value)
                    is PagingSource.LoadResult.Error -> onLoadError(type, value.throwable)
                    is PagingSource.LoadResult.Invalid -> onLoadInvalid()
                }
            }
        }
    }

    private fun onLoadSuccess(type: LoadType, value: PagingSource.LoadResult.Page<K, V>) {
        if (isDetached) return // abort!

        if (pageConsumer.onPageResult(type, value)) {
            when (type) {
                LoadType.PREPEND -> schedulePrepend()
                LoadType.APPEND -> scheduleAppend()
                else -> throw IllegalStateException("Can only fetch more during append/prepend")
            }
        } else {
            loadStateManager.setState(
                type,
                if (value.data.isEmpty()) NotLoading.Complete else NotLoading.Incomplete
            )
        }
    }

    private fun onLoadError(type: LoadType, throwable: Throwable) {
        if (isDetached) return // abort!

        val state = LoadState.Error(throwable)
        loadStateManager.setState(type, state)
    }

    private fun onLoadInvalid() {
        source.invalidate()
        detach()
    }

    fun trySchedulePrepend() {
        val startState = loadStateManager.startState
        if (startState is NotLoading && !startState.endOfPaginationReached) {
            schedulePrepend()
        }
    }

    fun tryScheduleAppend() {
        val endState = loadStateManager.endState
        if (endState is NotLoading && !endState.endOfPaginationReached) {
            scheduleAppend()
        }
    }

    private fun schedulePrepend() {
        val key = keyProvider.prevKey
        if (key == null) {
            onLoadSuccess(LoadType.PREPEND, PagingSource.LoadResult.Page.empty())
            return
        }

        loadStateManager.setState(LoadType.PREPEND, Loading)

        val loadParams = LoadParams.Prepend(
            key,
            config.pageSize,
            config.enablePlaceholders,
        )
        scheduleLoad(LoadType.PREPEND, loadParams)
    }

    private fun scheduleAppend() {
        val key = keyProvider.nextKey
        if (key == null) {
            onLoadSuccess(LoadType.APPEND, PagingSource.LoadResult.Page.empty())
            return
        }

        loadStateManager.setState(LoadType.APPEND, Loading)
        val loadParams = LoadParams.Append(
            key,
            config.pageSize,
            config.enablePlaceholders,
        )
        scheduleLoad(LoadType.APPEND, loadParams)
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
        fun onPageResult(type: LoadType, page: PagingSource.LoadResult.Page<*, V>): Boolean

        fun onStateChanged(type: LoadType, state: LoadState)
    }

    internal interface KeyProvider<K : Any> {
        val prevKey: K?
        val nextKey: K?
    }
}
