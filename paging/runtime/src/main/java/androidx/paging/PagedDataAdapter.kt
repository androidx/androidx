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

import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class PagedDataAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : RecyclerView.Adapter<VH>() {
    private val differ = AsyncPagedDataDiffer(
        diffCallback = diffCallback,
        updateCallback = AdapterListUpdateCallback(this),
        mainDispatcher = mainDispatcher
    )

    fun connect(flow: Flow<PagedData<T>>, scope: CoroutineScope) {
        differ.connect(flow, scope)
    }

    protected open fun getItem(position: Int) = differ.getItem(position)

    override fun getItemCount() = differ.itemCount

    /**
     * Called when the [LoadState] for a particular type of load (START, END, REFRESH) has
     * changed.
     *
     * REFRESH events can be used to drive a `SwipeRefreshLayout`, or START/END events
     * can be used to drive loading spinner items in the Adapter.
     *
     * @param type [LoadType] Can be START, END, or REFRESH
     * @param state [LoadState] Idle, Loading, Done, or Error.
     */
    open fun onLoadStateChanged(type: LoadType, state: LoadState) {
    }
}
