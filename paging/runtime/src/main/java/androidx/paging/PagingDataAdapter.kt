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
 * To present a [Flow]<[PagingData]>, you would connect generally use
 * [collectLatest][kotlinx.coroutines.flow.collectLatest], and
 * call [presentData] on the latest generation of [PagingData].
 *
 * If you are using RxJava or LiveData as your reactive stream API, you would typically connect
 * those to [submitData].
 *
 * PagingDataAdapter listens to internal PagingData loading events as
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
     * Present the new [PagingData], and suspend as long as it is not invalidated.
     *
     * This method should be called on the same [CoroutineDispatcher] where updates will be
     * dispatched to UI, typically [Dispatchers.Main].
     *
     * This method is typically used when observing a [Flow]. For a RxJava or LiveData stream,
     * see [submitData]
     *
     * @sample androidx.paging.samples.presentDataSample
     * @see [submitData]
     */
    suspend fun presentData(pagingData: PagingData<T>) {
        differ.presentData(pagingData)
    }

    /**
     * Present the new PagingData until the next call to submitData.
     *
     * This method is typically used when observing a RxJava or LiveData stream. For [Flow], see
     * [presentData]
     *
     * @sample androidx.paging.samples.submitDataSample
     * @see [presentData]
     */
    fun submitData(lifecycle: Lifecycle, pagingData: PagingData<T>) {
        differ.submitData(lifecycle, pagingData)
    }

    fun retry() {
        differ.retry()
    }

    fun refresh() {
        differ.refresh()
    }

    protected open fun getItem(position: Int) = differ.getItem(position)

    override fun getItemCount() = differ.itemCount

    /**
     * Add a [LoadState] listener to observe the loading state of the current [PagingData].
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect current [LoadType.REFRESH], [LoadType.START], and [LoadType.END] states.
     *
     * @param listener [LoadState] listener to receive updates.
     *
     * @see removeLoadStateListener
     */
    open fun addLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        differ.addLoadStateListener(listener)
    }

    /**
     * Remove a previously registered [LoadState] listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    open fun removeLoadStateListener(listener: (LoadType, LoadState) -> Unit) {
        differ.removeLoadStateListener(listener)
    }

    /**
     * Create a [MergeAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.END] [LoadState] as a list item at the end of the presented list.
     */
    fun withLoadStateHeader(
        header: LoadStateAdapter<*>
    ): MergeAdapter {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.START) {
                header.loadState = loadState
            }
        }
        return MergeAdapter(header, this)
    }

    /**
     * Create a [MergeAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.START] [LoadState] as a list item at the start of the presented list.
     */
    fun withLoadStateFooter(
        footer: LoadStateAdapter<*>
    ): MergeAdapter {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.END) {
                footer.loadState = loadState
            }
        }
        return MergeAdapter(this, footer)
    }

    /**
     * Create a [MergeAdapter] with the provided [LoadStateAdapter]s displaying the
     * [LoadType.START] and [LoadType.END] [LoadState]s as list items at the start and end
     * respectively.
     */
    fun withLoadStateHeaderAndFooter(
        header: LoadStateAdapter<*>,
        footer: LoadStateAdapter<*>
    ): MergeAdapter {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.START) {
                header.loadState = loadState
            } else if (loadType == LoadType.END) {
                footer.loadState = loadState
            }
        }
        return MergeAdapter(header, this, footer)
    }
}
