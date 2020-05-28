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
import androidx.paging.LoadType.REFRESH
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.MergeAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * [RecyclerView.Adapter] base class for presenting paged data from [PagingData]s in
 * a [RecyclerView].
 *
 * This class is a convenience wrapper around [AsyncPagingDataDiffer] that implements common default
 * behavior for item counting, and listening to update events.
 *
 * To present a [Pager], use [collectLatest][kotlinx.coroutines.flow.collectLatest] to observe
 * [Pager.flow] and call [submitData] whenever a new [PagingData] is emitted.
 *
 * If using RxJava and LiveData extensions on [Pager], use the non-suspending overload of
 * [submitData], which accepts a [Lifecycle].
 *
 * [PagingDataAdapter] listens to internal [PagingData] loading events as
 * [pages][PagingSource.LoadResult.Page] are loaded, and uses [DiffUtil] on a background thread to
 * compute fine grained updates as updated content in the form of new PagingData objects are
 * received.
 *
 * @sample androidx.paging.samples.pagingDataAdapterSample
 */
abstract class PagingDataAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecyclerView.Adapter<VH>() {
    private val differ = AsyncPagingDataDiffer(
        mainDispatcher = mainDispatcher,
        workerDispatcher = workerDispatcher,
        diffCallback = diffCallback,
        updateCallback = AdapterListUpdateCallback(this)
    )

    /**
     * Note: [getItemId] is final, because stable IDs are unnecessary and therefore unsupported.
     *
     * [PagingDataAdapter]'s async diffing means that efficient change animations are handled for
     * you, without the performance drawbacks of [RecyclerView.Adapter.notifyDataSetChanged].
     * Instead, the diffCallback parameter of the [PagingDataAdapter] serves the same
     * functionality - informing the adapter and [RecyclerView] how items are changed and moved.
     */
    final override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }

    /**
     * Present a [PagingData] until it is invalidated by a call to [refresh] or
     * [PagingSource.invalidate].
     *
     * [submitData] should be called on the same [CoroutineDispatcher] where updates will be
     * dispatched to UI, typically [Dispatchers.Main] (this is done for you if you use
     * `lifecycleScope.launch {}`).
     *
     * This method is typically used when collecting from a [Flow] produced by [Pager]. For RxJava
     * or LiveData support, use the non-suspending overload of [submitData], which accepts a
     * [Lifecycle].
     *
     * Note: This method suspends while it is actively presenting page loads from a [PagingData],
     * until the [PagingData] is invalidated. Although cancellation will propagate to this call
     * automatically, collecting from a [Pager.flow] with the intention of presenting the most
     * up-to-date representation of your backing dataset should typically be done using
     * [collectLatest][kotlinx.coroutines.flow.collectLatest].
     *
     * @sample androidx.paging.samples.submitDataFlowSample
     *
     * @see [Pager]
     */
    suspend fun submitData(pagingData: PagingData<T>) {
        differ.submitData(pagingData)
    }

    /**
     * Present a [PagingData] until it is either invalidated or another call to [submitData] is
     * made.
     *
     * This method is typically used when observing a RxJava or LiveData stream produced by [Pager].
     * For [Flow] support, use the suspending overload of [submitData], which automates cancellation
     * via [CoroutineScope][kotlinx.coroutines.CoroutineScope] instead of relying of [Lifecycle].
     *
     * @sample androidx.paging.samples.submitDataLiveDataSample
     * @sample androidx.paging.samples.submitDataRxSample
     *
     * @see submitData
     * @see [Pager]
     */
    fun submitData(lifecycle: Lifecycle, pagingData: PagingData<T>) {
        differ.submitData(lifecycle, pagingData)
    }

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     * [PagingDataAdapter].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    fun retry() {
        differ.retry()
    }

    /**
     * Refresh the data presented by this [PagingDataAdapter].
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
        differ.refresh()
    }

    protected fun getItem(position: Int) = differ.getItem(position)

    override fun getItemCount() = differ.itemCount

    /**
     * Add a [CombinedLoadStates] listener to observe the loading state of the current [PagingData].
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect the current [CombinedLoadStates].
     *
     * @param listener [LoadStates] listener to receive updates.
     *
     * @see removeLoadStateListener
     */
    fun addLoadStateListener(listener: (CombinedLoadStates) -> Unit) {
        differ.addLoadStateListener(listener)
    }

    /**
     * Remove a previously registered [CombinedLoadStates] listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    fun removeLoadStateListener(listener: (CombinedLoadStates) -> Unit) {
        differ.removeLoadStateListener(listener)
    }

    /**
     * Create a [MergeAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.APPEND] [LoadState] as a list item at the end of the presented list.
     *
     * @see LoadStateAdapter
     * @see withLoadStateHeaderAndFooter
     * @see withLoadStateFooter
     */
    fun withLoadStateHeader(
        header: LoadStateAdapter<*>
    ): MergeAdapter {
        addLoadStateListener { loadStates ->
            header.loadState = loadStates.prepend
        }
        return MergeAdapter(header, this)
    }

    /**
     * Create a [MergeAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.PREPEND] [LoadState] as a list item at the start of the presented list.
     *
     * @see LoadStateAdapter
     * @see withLoadStateHeaderAndFooter
     * @see withLoadStateHeader
     */
    fun withLoadStateFooter(
        footer: LoadStateAdapter<*>
    ): MergeAdapter {
        addLoadStateListener { loadStates ->
            footer.loadState = loadStates.append
        }
        return MergeAdapter(this, footer)
    }

    /**
     * Create a [MergeAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.PREPEND] and [LoadType.APPEND] [LoadState]s as list items at the start and end
     * respectively.
     *
     * @see LoadStateAdapter
     * @see withLoadStateHeader
     * @see withLoadStateFooter
     */
    fun withLoadStateHeaderAndFooter(
        header: LoadStateAdapter<*>,
        footer: LoadStateAdapter<*>
    ): MergeAdapter {
        addLoadStateListener { loadStates ->
                header.loadState = loadStates.prepend
                footer.loadState = loadStates.append
        }
        return MergeAdapter(header, this, footer)
    }

    /**
     * A [Flow] of [Unit] that is emitted when new [PagingData] generations are submitted and
     * displayed.
     */
    @ExperimentalPagingApi
    val dataRefreshFlow: Flow<Unit> = differ.dataRefreshFlow

    /**
     * Add a listener to observe new [PagingData] generations.
     *
     * @param listener called whenever a new [PagingData] is submitted and displayed.
     *
     * @see removeDataRefreshListener
     */
    @ExperimentalPagingApi
    fun addDataRefreshListener(listener: () -> Unit) {
        differ.addDataRefreshListener(listener)
    }

    /**
     * Remove a previously registered listener for new [PagingData] generations.
     *
     * @param listener Previously registered listener.
     *
     * @see addDataRefreshListener
     */
    @ExperimentalPagingApi
    fun removeDataRefreshListener(listener: () -> Unit) {
        differ.removeDataRefreshListener(listener)
    }
}
