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

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

open class AsyncPagedDataDiffer<T : Any>(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val updateCallback: ListUpdateCallback
) {
    internal val callback = object : PagedList.Callback() {
        override fun onInserted(position: Int, count: Int) =
            updateCallback.onInserted(position, count)

        override fun onRemoved(position: Int, count: Int) =
            updateCallback.onRemoved(position, count)

        override fun onChanged(position: Int, count: Int) {
            // NOTE: pass a null payload to convey null -> item
            updateCallback.onChanged(position, count, null)
        }
    }

    private val differBase = object : PagedDataDiffer<T>(mainDispatcher, workerDispatcher) {
        override suspend fun performDiff(previous: NullPaddedList<T>, new: NullPaddedList<T>) {
            withContext(mainDispatcher) {
                when {
                    previous.size == 0 -> // fast path for no items -> some items
                        callback.onInserted(0, new.size)
                    new.size == 0 -> // fast path for some items -> no items
                        callback.onRemoved(0, previous.size)
                    else -> { // full diff
                        val diffResult = withContext(workerDispatcher) {
                            previous.computeDiff(new, diffCallback)
                        }
                        previous.dispatchDiff(updateCallback, new, diffResult)
                    }
                }
            }
        }
    }

    fun connect(flow: Flow<PagedData<T>>, scope: CoroutineScope) {
        differBase.connect(flow, scope, callback)
    }

    fun retry() {
        differBase.retry()
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
}