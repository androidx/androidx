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

import androidx.annotation.IntRange
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.REFRESH
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.ALLOW
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT
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
 * *State Restoration*: To be able to restore [RecyclerView] state (e.g. scroll position) after a
 * configuration change / application recreate, [PagingDataAdapter] calls
 * [RecyclerView.Adapter.setStateRestorationPolicy] with
 * [RecyclerView.Adapter.StateRestorationPolicy.PREVENT] upon initialization and waits for the
 * first page to load before allowing state restoration.
 * Any other call to [RecyclerView.Adapter.setStateRestorationPolicy] by the application will
 * disable this logic and will rely on the user set value.
 *
 * @sample androidx.paging.samples.pagingDataAdapterSample
 */
abstract class PagingDataAdapter<T : Any, VH : RecyclerView.ViewHolder> @JvmOverloads constructor(
    diffCallback: DiffUtil.ItemCallback<T>,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecyclerView.Adapter<VH>() {

    /**
     * Track whether developer called [setStateRestorationPolicy] or not to decide whether the
     * automated state restoration should apply or not.
     */
    private var userSetRestorationPolicy = false

    override fun setStateRestorationPolicy(strategy: StateRestorationPolicy) {
        userSetRestorationPolicy = true
        super.setStateRestorationPolicy(strategy)
    }

    private val differ = AsyncPagingDataDiffer(
        diffCallback = diffCallback,
        updateCallback = AdapterListUpdateCallback(this),
        mainDispatcher = mainDispatcher,
        workerDispatcher = workerDispatcher
    )

    init {
        // Wait on state restoration until the first insert event.
        super.setStateRestorationPolicy(PREVENT)

        fun considerAllowingStateRestoration() {
            if (stateRestorationPolicy == PREVENT && !userSetRestorationPolicy) {
                this@PagingDataAdapter.stateRestorationPolicy = ALLOW
            }
        }

        // Watch for adapter insert before triggering state restoration. This is almost redundant
        // with loadState below, but can handle cached case.
        @Suppress("LeakingThis")
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                considerAllowingStateRestoration()
                unregisterAdapterDataObserver(this)
                super.onItemRangeInserted(positionStart, itemCount)
            }
        })

        // Watch for loadState update before triggering state restoration. This is almost
        // redundant with data observer above, but can handle empty page case.
        addLoadStateListener(object : Function1<CombinedLoadStates, Unit> {
            // Ignore the first event we get, which is always the initial state, since we only
            // want to observe for Insert events.
            private var ignoreNextEvent = true

            override fun invoke(loadStates: CombinedLoadStates) {
                if (ignoreNextEvent) {
                    ignoreNextEvent = false
                } else if (loadStates.source.refresh is NotLoading) {
                    considerAllowingStateRestoration()
                    removeLoadStateListener(this)
                }
            }
        })
    }

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
     * Stable ids are unsupported by [PagingDataAdapter]. Calling this method is an error and will
     * result in an [UnsupportedOperationException].
     *
     * @param hasStableIds Whether items in data set have unique identifiers or not.
     *
     * @throws UnsupportedOperationException Always thrown, since this is unsupported by
     * [PagingDataAdapter].
     */
    final override fun setHasStableIds(hasStableIds: Boolean) {
        throw UnsupportedOperationException("Stable ids are unsupported on PagingDataAdapter.")
    }

    /**
     * Present a [PagingData] until it is invalidated by a call to [refresh] or
     * [PagingSource.invalidate].
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
     * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
     * within the same generation of [PagingData].
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

    /**
     * Returns the presented item at the specified position, notifying Paging of the item access to
     * trigger any loads necessary to fulfill [prefetchDistance][PagingConfig.prefetchDistance].
     *
     * @param position Index of the presented item to return, including placeholders.
     * @return The presented item at [position], `null` if it is a placeholder
     */
    protected fun getItem(@IntRange(from = 0) position: Int) = differ.getItem(position)

    /**
     * Returns the presented item at the specified position, without notifying Paging of the item
     * access that would normally trigger page loads.
     *
     * @param index Index of the presented item to return, including placeholders.
     * @return The presented item at position [index], `null` if it is a placeholder.
     */
    fun peek(@IntRange(from = 0) index: Int) = differ.peek(index)

    /**
     * Returns a new [ItemSnapshotList] representing the currently presented items, including any
     * placeholders if they are enabled.
     */
    fun snapshot(): ItemSnapshotList<T> = differ.snapshot()

    override fun getItemCount() = differ.itemCount

    /**
     * A hot [Flow] of [CombinedLoadStates] that emits a snapshot whenever the loading state of the
     * current [PagingData] changes.
     *
     * This flow is conflated, so it buffers the last update to [CombinedLoadStates] and
     * immediately delivers the current load states on collection.
     */
    val loadStateFlow: Flow<CombinedLoadStates> = differ.loadStateFlow

    /**
     * A hot [Flow] that emits after the pages presented to the UI are updated, even if the
     * actual items presented don't change.
     *
     * An update is triggered from one of the following:
     *   * [submitData] is called and initial load completes, regardless of any differences in
     *     the loaded data
     *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is inserted
     *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is dropped
     *
     * Note: This is a [SharedFlow][kotlinx.coroutines.flow.SharedFlow] configured to replay
     * 0 items with a buffer of size 64. If a collector lags behind page updates, it may
     * trigger multiple times for each intermediate update that was presented while your collector
     * was still working. To avoid this behavior, you can
     * [conflate][kotlinx.coroutines.flow.conflate] this [Flow] so that you only receive the latest
     * update, which is useful in cases where you are simply updating UI and don't care about
     * tracking the exact number of page updates.
     */
    val onPagesUpdatedFlow: Flow<Unit> = differ.onPagesUpdatedFlow

    /**
     * Add a [CombinedLoadStates] listener to observe the loading state of the current [PagingData].
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect the current [CombinedLoadStates].
     *
     * @param listener [LoadStates] listener to receive updates.
     *
     * @see removeLoadStateListener
     * @sample androidx.paging.samples.addLoadStateListenerSample
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
     * Add a listener which triggers after the pages presented to the UI are updated, even if the
     * actual items presented don't change.
     *
     * An update is triggered from one of the following:
     *   * [submitData] is called and initial load completes, regardless of any differences in
     *     the loaded data
     *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is inserted
     *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is dropped
     *
     * @param listener called after pages presented are updated.
     *
     * @see removeOnPagesUpdatedListener
     */
    fun addOnPagesUpdatedListener(listener: () -> Unit) {
        differ.addOnPagesUpdatedListener(listener)
    }

    /**
     * Remove a previously registered listener for new [PagingData] generations completing
     * initial load and presenting to the UI.
     *
     * @param listener Previously registered listener.
     *
     * @see addOnPagesUpdatedListener
     */
    fun removeOnPagesUpdatedListener(listener: () -> Unit) {
        differ.removeOnPagesUpdatedListener(listener)
    }

    /**
     * Create a [ConcatAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.PREPEND] [LoadState] as a list item at the end of the presented list.
     *
     * @see LoadStateAdapter
     * @see withLoadStateHeaderAndFooter
     * @see withLoadStateFooter
     */
    fun withLoadStateHeader(
        header: LoadStateAdapter<*>
    ): ConcatAdapter {
        addLoadStateListener { loadStates ->
            header.loadState = loadStates.prepend
        }
        return ConcatAdapter(header, this)
    }

    /**
     * Create a [ConcatAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.APPEND] [LoadState] as a list item at the start of the presented list.
     *
     * @see LoadStateAdapter
     * @see withLoadStateHeaderAndFooter
     * @see withLoadStateHeader
     */
    fun withLoadStateFooter(
        footer: LoadStateAdapter<*>
    ): ConcatAdapter {
        addLoadStateListener { loadStates ->
            footer.loadState = loadStates.append
        }
        return ConcatAdapter(this, footer)
    }

    /**
     * Create a [ConcatAdapter] with the provided [LoadStateAdapter]s displaying the
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
    ): ConcatAdapter {
        addLoadStateListener { loadStates ->
            header.loadState = loadStates.prepend
            footer.loadState = loadStates.append
        }
        return ConcatAdapter(header, this, footer)
    }
}
