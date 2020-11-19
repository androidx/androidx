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
import androidx.annotation.RestrictTo
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagePresenter.ProcessPageEventCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.CopyOnWriteArrayList

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class PagingDataDiffer<T : Any>(
    private val differCallback: DifferCallback,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private var presenter: PagePresenter<T> = PagePresenter.initial()
    private var receiver: UiReceiver? = null
    private val combinedLoadStates = MutableLoadStateCollection()
    private val loadStateListeners = CopyOnWriteArrayList<(CombinedLoadStates) -> Unit>()

    private val collectFromRunner = SingleRunner()

    /**
     * Track whether [lastAccessedIndex] points to a loaded item in the list or a placeholder
     * after applying transformations to loaded pages. `true` if [lastAccessedIndex] points to a
     * placeholder, `false` if [lastAccessedIndex] points to a loaded item after transformations.
     *
     * [lastAccessedIndexUnfulfilled] is used to track whether resending [lastAccessedIndex] as a
     * hint is necessary, since in cases of aggressive filtering, an index may be unfulfilled
     * after being sent to [PageFetcher], which is only capable of handling prefetchDistance
     * before transformations.
     */
    @Volatile
    private var lastAccessedIndexUnfulfilled: Boolean = false

    /**
     * Track last index access so it can be forwarded to new generations after DiffUtil runs and
     * it is transformed to an index in the new list.
     */
    @Volatile
    private var lastAccessedIndex: Int = 0

    private val processPageEventCallback = object : ProcessPageEventCallback {
        override fun onChanged(position: Int, count: Int) {
            differCallback.onChanged(position, count)
        }

        override fun onInserted(position: Int, count: Int) {
            differCallback.onInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            differCallback.onRemoved(position, count)
        }

        override fun onStateUpdate(
            loadType: LoadType,
            fromMediator: Boolean,
            loadState: LoadState
        ) {
            val currentLoadState = combinedLoadStates.get(loadType, fromMediator)

            // No change, skip update + dispatch.
            if (currentLoadState == loadState) return

            combinedLoadStates.set(loadType, fromMediator, loadState)
            val newLoadStates = combinedLoadStates.snapshot()
            loadStateListeners.forEach { it(newLoadStates) }
        }
    }

    private fun dispatchLoadStates(states: CombinedLoadStates) {
        if (combinedLoadStates.snapshot() == states) return

        combinedLoadStates.set(states)
        loadStateListeners.forEach { it(states) }
    }

    /**
     * @return Transformed result of [lastAccessedIndex] as an index of [newList] using the diff
     * result between [previousList] and [newList]. Null if [newList] or [previousList] lists are
     * empty, where it does not make sense to transform [lastAccessedIndex].
     */
    abstract suspend fun presentNewList(
        previousList: NullPaddedList<T>,
        newList: NullPaddedList<T>,
        newCombinedLoadStates: CombinedLoadStates,
        lastAccessedIndex: Int
    ): Int?

    open fun postEvents(): Boolean = false

    suspend fun collectFrom(pagingData: PagingData<T>) = collectFromRunner.runInIsolation {
        receiver = pagingData.receiver

        // TODO: Validate only empty pages between separator pages and its dependent pages.
        pagingData.flow.collect { event ->
            withContext<Unit>(mainDispatcher) {
                if (event is PageEvent.Insert && event.loadType == REFRESH) {
                    lastAccessedIndexUnfulfilled = false

                    val newPresenter = PagePresenter(event)
                    val transformedLastAccessedIndex = presentNewList(
                        previousList = presenter,
                        newList = newPresenter,
                        newCombinedLoadStates = event.combinedLoadStates,
                        lastAccessedIndex = lastAccessedIndex
                    )
                    presenter = newPresenter

                    // Dispatch LoadState updates as soon as we are done diffing, but after setting
                    // presenter.
                    dispatchLoadStates(event.combinedLoadStates)

                    if (transformedLastAccessedIndex == null) {
                        // Send an initialize hint in case the new list is empty, which would
                        // prevent a ViewportHint.Access from ever getting sent since there are
                        // no items to bind from initial load.
                        receiver?.accessHint(newPresenter.initializeHint())
                    } else {
                        // Transform the last loadAround index from the old list to the new list
                        // by passing it through the DiffResult, and pass it forward as a
                        // ViewportHint within the new list to the next generation of Pager.
                        // This ensures prefetch distance for the last ViewportHint from the old
                        // list is respected in the new list, even if invalidation interrupts
                        // the prepend / append load that would have fulfilled it in the old
                        // list.
                        lastAccessedIndex = transformedLastAccessedIndex
                        receiver?.accessHint(
                            newPresenter.accessHintForPresenterIndex(transformedLastAccessedIndex)
                        )
                    }
                } else {
                    if (postEvents()) {
                        yield()
                    }

                    // Send event to presenter to be shown to the UI.
                    presenter.processEvent(event, processPageEventCallback)

                    // Reset lastAccessedIndexUnfulfilled if a page is dropped, to avoid infinite
                    // loops when maxSize is insufficiently large.
                    if (event is PageEvent.Drop) {
                        lastAccessedIndexUnfulfilled = false
                    }

                    // If index points to a placeholder after transformations, resend it unless
                    // there are no more items to load.
                    if (event is PageEvent.Insert) {
                        val prependDone =
                            event.combinedLoadStates.prepend.endOfPaginationReached
                        val appendDone = event.combinedLoadStates.append.endOfPaginationReached
                        val canContinueLoading = !(event.loadType == PREPEND && prependDone) &&
                            !(event.loadType == APPEND && appendDone)

                        if (!canContinueLoading) {
                            // Reset lastAccessedIndexUnfulfilled since endOfPaginationReached
                            // means there are no more pages to load that could fulfill this index.
                            lastAccessedIndexUnfulfilled = false
                        } else if (lastAccessedIndexUnfulfilled) {
                            val shouldResendHint =
                                lastAccessedIndex < presenter.placeholdersBefore ||
                                    lastAccessedIndex > presenter.placeholdersBefore +
                                    presenter.storageCount

                            if (shouldResendHint) {
                                receiver?.accessHint(
                                    presenter.accessHintForPresenterIndex(lastAccessedIndex)
                                )
                            } else {
                                // lastIndex fulfilled, so reset lastAccessedIndexUnfulfilled.
                                lastAccessedIndexUnfulfilled = false
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the presented item at the specified position, notifying Paging of the item access to
     * trigger any loads necessary to fulfill [prefetchDistance][PagingConfig.prefetchDistance].
     *
     * @param index Index of the presented item to return, including placeholders.
     * @return The presented item at position [index], `null` if it is a placeholder.
     */
    operator fun get(@IntRange(from = 0) index: Int): T? {
        lastAccessedIndexUnfulfilled = true
        lastAccessedIndex = index

        receiver?.accessHint(presenter.accessHintForPresenterIndex(index))
        return presenter.get(index)
    }

    /**
     * Returns the presented item at the specified position, without notifying Paging of the item
     * access that would normally trigger page loads.
     *
     * @param index Index of the presented item to return, including placeholders.
     * @return The presented item at position [index], `null` if it is a placeholder
     */
    fun peek(@IntRange(from = 0) index: Int): T? {
        return presenter.get(index)
    }

    /**
     * Returns a new [ItemSnapshotList] representing the currently presented items, including any
     * placeholders if they are enabled.
     */
    fun snapshot(): ItemSnapshotList<T> = presenter.snapshot()

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     * [PagingDataDiffer].
     *
     * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
     * within the same generation of [PagingData].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    fun retry() {
        receiver?.retry()
    }

    /**
     * Refresh the data presented by this [PagingDataDiffer].
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
        receiver?.refresh()
    }

    /**
     * @return Total number of presented items, including placeholders.
     */
    val size: Int
        get() = presenter.size

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _combinedLoadState = MutableStateFlow(combinedLoadStates.snapshot())

    /**
     * A hot [Flow] of [CombinedLoadStates] that emits a snapshot whenever the loading state of the
     * current [PagingData] changes.
     *
     * This flow is conflated, so it buffers the last update to [CombinedLoadStates] and
     * immediately delivers the current load states on collection.
     *
     * @sample androidx.paging.samples.loadStateFlowSample
     */
    @OptIn(FlowPreview::class)
    val loadStateFlow: Flow<CombinedLoadStates>
        get() = _combinedLoadState

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        addLoadStateListener {
            _combinedLoadState.value = it
        }
    }

    /**
     * Add a [CombinedLoadStates] listener to observe the loading state of the current [PagingData].
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect the current [CombinedLoadStates].
     *
     * @param listener [LoadStates] listener to receive updates.
     *
     * @see removeLoadStateListener
     *
     * @sample androidx.paging.samples.addLoadStateListenerSample
     */
    fun addLoadStateListener(listener: (CombinedLoadStates) -> Unit) {
        // Note: Important to add the listener first before sending off events, in case the
        // callback triggers removal, which could lead to a leak if the listener is added
        // afterwards.
        loadStateListeners.add(listener)
        listener(combinedLoadStates.snapshot())
    }

    /**
     * Remove a previously registered [CombinedLoadStates] listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    fun removeLoadStateListener(listener: (CombinedLoadStates) -> Unit) {
        loadStateListeners.remove(listener)
    }
}

/**
 * Callback for the presenter/adapter to listen to the state of pagination data.
 *
 * Note that these won't map directly to PageEvents, since PageEvents can cause several adapter
 * events that should all be dispatched to the presentation layer at once - as part of the same
 * frame.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface DifferCallback {
    fun onChanged(position: Int, count: Int)
    fun onInserted(position: Int, count: Int)
    fun onRemoved(position: Int, count: Int)
}
