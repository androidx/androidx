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

import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

open class AsyncPagingDataDiffer<T : Any>(
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
                START -> {
                    if (loadState != loadStates[START]) {
                        loadStates[START] = loadState
                        dispatchLoadState(START, loadState)
                    }
                }
                END -> {
                    if (loadState != loadStates[END]) {
                        loadStates[END] = loadState
                        dispatchLoadState(END, loadState)
                    }
                }
            }
        }

        private fun dispatchLoadState(type: LoadType, state: LoadState) {
            loadStateListeners.forEach { it(type, state) }
        }
    }

    private val differBase = object : PagingDataDiffer<T>(mainDispatcher, workerDispatcher) {
        override suspend fun performDiff(
            previousList: NullPaddedList<T>,
            newList: NullPaddedList<T>,
            newLoadStates: Map<LoadType, LoadState>
        ) {
            withContext(mainDispatcher) {
                when {
                    previousList.size == 0 -> // fast path for no items -> some items
                        callback.onInserted(0, newList.size)
                    newList.size == 0 -> // fast path for some items -> no items
                        callback.onRemoved(0, previousList.size)
                    else -> { // full diff
                        val diffResult = withContext(workerDispatcher) {
                            previousList.computeDiff(newList, diffCallback)
                        }
                        previousList.dispatchDiff(updateCallback, newList, diffResult)
                        newLoadStates.entries.forEach { callback.onStateUpdate(it.key, it.value) }
                    }
                }
            }
        }
    }

    fun connect(flow: Flow<PagingData<T>>, scope: CoroutineScope) {
        differBase.connect(flow, scope, callback)
    }

    fun retry() {
        differBase.retry()
    }

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
    open fun getItem(index: Int): T? = differBase[index]

    /**
     * Get the number of items currently presented by this Differ. This value can be directly
     * returned to [androidx.recyclerview.widget.RecyclerView.Adapter.getItemCount].
     *
     * @return Number of items being presented.
     */
    open val itemCount: Int
        get() = differBase.size

    internal val loadStateListeners: MutableList<(LoadType, LoadState) -> Unit> =
        CopyOnWriteArrayList()

    internal val loadStates = mutableMapOf<LoadType, LoadState>(
        REFRESH to LoadState.Idle,
        START to LoadState.Idle,
        END to LoadState.Idle
    )

    /**
     * Add a listener to observe the loading state.
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect current [LoadType.REFRESH], [LoadType.START], and [LoadType.END] states.
     *
     * @param listener [LoadStateListener] to receive updates.
     *
     * @see removeLoadStateListener
     */
    open fun addLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        // Note: Important to add the listener first before sending off events, in case the
        // callback triggers removal, which could lead to a leak if the listener is added
        // afterwards.
        loadStateListeners.add(listener)
        listener(REFRESH, loadStates[REFRESH]!!)
        if (loadStateListeners.contains(listener)) listener(START, loadStates[START]!!)
        if (loadStateListeners.contains(listener)) listener(END, loadStates[END]!!)
    }

    /**
     * Remove a previously registered load state listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    open fun removeLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        loadStateListeners.remove(listener)
    }
}