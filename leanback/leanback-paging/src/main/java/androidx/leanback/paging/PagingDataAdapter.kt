/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.leanback.paging

import androidx.annotation.IntRange
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.Lifecycle
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * An [ObjectAdapter] implemented with an [AsyncPagingDataDiffer].
 * It is an analogue of [androidx.paging.PagingDataAdapter] for leanback widgets.
 * @param T Type of the item in the list.
 */
class PagingDataAdapter<T : Any> : ObjectAdapter {

    private val diffCallback: DiffUtil.ItemCallback<T>
    private val mainDispatcher: CoroutineDispatcher
    private val workerDispatcher: CoroutineDispatcher
    private val differ: AsyncPagingDataDiffer<T>
    private val listUpdateCallback: ListUpdateCallback =
        object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)
            }

            override fun onChanged(
                position: Int,
                count: Int,
                payload: Any?
            ) {
                notifyItemRangeChanged(position, count, payload)
            }
        }

    /**
     * Constructs an adapter
     * @param diffCallback The [DiffUtil.ItemCallback] instance to compare items in the list.
     * @param mainDispatcher The [CoroutineDispatcher] to be used for foreground operations
     * @param workerDispatcher The [CoroutineDispatcher] to be used for computing diff
     */
    @JvmOverloads
    constructor(
        diffCallback: DiffUtil.ItemCallback<T>,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : super() {

        this.diffCallback = diffCallback
        this.mainDispatcher = mainDispatcher
        this.workerDispatcher = workerDispatcher
        this.differ = AsyncPagingDataDiffer<T>(
            diffCallback = diffCallback,
            updateCallback = listUpdateCallback,
            mainDispatcher = mainDispatcher,
            workerDispatcher = workerDispatcher
        )
    }

    /**
     * Constructs an adapter
     * @param presenter [Presenter]
     * @param diffCallback The [DiffUtil.ItemCallback] instance to compare items in the list.
     * @param mainDispatcher The [CoroutineDispatcher] to be used for foreground operations
     * @param workerDispatcher The [CoroutineDispatcher] to be used for computing diff
     */
    @JvmOverloads
    constructor(
        presenter: Presenter,
        diffCallback: DiffUtil.ItemCallback<T>,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : super(presenter) {

        this.diffCallback = diffCallback
        this.mainDispatcher = mainDispatcher
        this.workerDispatcher = workerDispatcher
        this.differ = AsyncPagingDataDiffer<T>(
            diffCallback = diffCallback,
            updateCallback = listUpdateCallback,
            mainDispatcher = mainDispatcher,
            workerDispatcher = workerDispatcher
        )
    }

    /**
     * Constructs an adapter
     * @param presenterSelector [PresenterSelector]
     * @param diffCallback The [DiffUtil.ItemCallback] instance to compare items in the list.
     * @param mainDispatcher The [CoroutineDispatcher] to be used for foreground operations
     * @param workerDispatcher The [CoroutineDispatcher] to be used for computing diff
     */
    @JvmOverloads
    constructor(
        presenterSelector: PresenterSelector,
        diffCallback: DiffUtil.ItemCallback<T>,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : super(presenterSelector) {

        this.diffCallback = diffCallback
        this.mainDispatcher = mainDispatcher
        this.workerDispatcher = workerDispatcher
        this.differ = AsyncPagingDataDiffer<T>(
            diffCallback = diffCallback,
            updateCallback = listUpdateCallback,
            mainDispatcher = mainDispatcher,
            workerDispatcher = workerDispatcher
        )
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
     * @see submitData
     * @see [Pager]
     */
    fun submitData(lifecycle: Lifecycle, pagingData: PagingData<T>) {
        differ.submitData(lifecycle, pagingData)
    }

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     *
     *  [PagingDataAdapter].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    fun retry() {
        differ.retry()
    }

    /**
     * Refresh the data presented by this [PagingDataAdapter].
     *
     * [refresh] triggers the creation of a new [PagingData] with a new instance of [PagingSource]
     * to represent an updated snapshot of the backing dataset. If a [RemoteMediator] is set,
     * calling [refresh] will also trigger a call to [RemoteMediator.load] with [LoadType] REFRESH]
     * to allow [RemoteMediator] to check for updates to the dataset backing [PagingSource].
     *
     * Note: This API is intended for UI-driven refresh signals, such as swipe-to-refresh.
     * Invalidation due repository-layer signals, such as DB-updates, should instead use
     * [PagingSource.invalidate].
     *
     * @see PagingSource.invalidate
     *
     */
    fun refresh() {
        differ.refresh()
    }

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

    /**
     * A hot [Flow] of [CombinedLoadStates] that emits a snapshot whenever the loading state of the
     * current [PagingData] changes.
     *
     * This flow is conflated, so it buffers the last update to [CombinedLoadStates] and
     * immediately delivers the current load states on collection.
     */
    val loadStateFlow: Flow<CombinedLoadStates>
        get() = differ.loadStateFlow

    /**
     * Add a [CombinedLoadStates] listener to observe the loading state of the current [PagingData].
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect the current [CombinedLoadStates].
     *
     * @param listener [CombinedLoadStates] listener to receive updates.
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
     * @return Total number of presented items, including placeholders.
     */
    override fun size(): Int {
        return differ.itemCount
    }

    /**
     * Returns the item for the given position. It will return null
     * if placeholders are enabled and data is not yet loaded.
     */
    override fun get(position: Int): T? {
        return differ.getItem(position)
    }
}
