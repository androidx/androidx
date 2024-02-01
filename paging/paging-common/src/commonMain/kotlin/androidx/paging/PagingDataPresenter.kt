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
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import androidx.paging.PageEvent.StaticList
import androidx.paging.internal.CopyOnWriteArrayList
import androidx.paging.internal.appendMediatorStatesIfNotNull
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmSuppressWildcards
import kotlin.jvm.Volatile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * The class that connects the UI layer to the underlying Paging operations. Takes input from
 * UI presenters and outputs Paging events (Loads, LoadStateUpdate) in response.
 *
 * Paging front ends that implement this class will be able to access loaded data, LoadStates,
 * and callbacks from LoadState or Page updates. This class also exposes the
 * [PagingDataEvent] from a [PagingData] for custom logic on how to present Loads, Drops, and
 * other Paging events.
 *
 * For implementation examples, refer to [AsyncPagingDataDiffer] for RecyclerView,
 * or [LazyPagingItems] for Compose.
 *
 * @param [mainContext] The coroutine context that core Paging operations will run on.
 * Defaults to [Dispatchers.Main]. Main operations executed within this context include
 * but are not limited to:
 * 1. flow collection on a [PagingData] for Loads, LoadStateUpdate etc.
 * 2. emitting [CombinedLoadStates] to the [loadStateFlow]
 * 3. invoking LoadState and PageUpdate listeners
 * 4. invoking [presentPagingDataEvent]
 *
 * @param [cachedPagingData] a [PagingData] that will initialize this PagingDataPresenter with
 * any LoadStates or loaded data contained within it.
 */
public abstract class PagingDataPresenter<T : Any> (
    private val mainContext: CoroutineContext = Dispatchers.Main,
    cachedPagingData: PagingData<T>? = null,
) {
    private var hintReceiver: HintReceiver? = null
    private var uiReceiver: UiReceiver? = null
    private var pageStore: PageStore<T> = PageStore.initial(cachedPagingData?.cachedEvent())
    private val combinedLoadStatesCollection = MutableCombinedLoadStateCollection().apply {
        cachedPagingData?.cachedEvent()?.let { set(it.sourceLoadStates, it.mediatorLoadStates) }
    }
    private val onPagesUpdatedListeners = CopyOnWriteArrayList<() -> Unit>()

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

    /**
     * Handler for [PagingDataEvent] emitted by [PagingData].
     *
     * When a [PagingData] is submitted to this PagingDataPresenter through [collectFrom],
     * page loads, drops, or LoadStateUpdates will be emitted to presenters as [PagingDataEvent]
     * through this method.
     *
     * Presenter layers that communicate directly with [PagingDataPresenter] should override
     * this method to handle the [PagingDataEvent] accordingly. For example by diffing two
     * [PagingDataEvent.Refresh] lists, or appending the inserted list of data from
     * [PagingDataEvent.Prepend] or [PagingDataEvent.Append].
     *
     */
    public abstract suspend fun presentPagingDataEvent(
        event: PagingDataEvent<T>,
    ): @JvmSuppressWildcards Unit

    public suspend fun collectFrom(pagingData: PagingData<T>): @JvmSuppressWildcards Unit {
        collectFromRunner.runInIsolation {
            uiReceiver = pagingData.uiReceiver
            pagingData.flow.collect { event ->
                log(VERBOSE) { "Collected $event" }
                withContext(mainContext) {
                    /**
                     * The hint receiver of a new generation is set only after it has been
                     * presented. This ensures that:
                     *
                     * 1. while new generation is still loading, access hints (and jump hints) will
                     * be sent to current generation.
                     *
                     * 2. the access hint sent from presentNewList will have the correct
                     * placeholders and indexInPage adjusted according to new pageStore's most
                     * recent state
                     *
                     * Ensuring that viewport hints are sent to the correct generation helps
                     * synchronize fetcher/pageStore in the correct calculation of the
                     * next anchorPosition.
                     */
                    when {
                        event is StaticList -> {
                            presentNewList(
                                pages = listOf(
                                    TransformablePage(
                                        originalPageOffset = 0,
                                        data = event.data,
                                    )
                                ),
                                placeholdersBefore = 0,
                                placeholdersAfter = 0,
                                dispatchLoadStates = event.sourceLoadStates != null ||
                                    event.mediatorLoadStates != null,
                                sourceLoadStates = event.sourceLoadStates,
                                mediatorLoadStates = event.mediatorLoadStates,
                                newHintReceiver = pagingData.hintReceiver
                            )
                        }
                        event is Insert && (event.loadType == REFRESH) -> {
                            presentNewList(
                                pages = event.pages,
                                placeholdersBefore = event.placeholdersBefore,
                                placeholdersAfter = event.placeholdersAfter,
                                dispatchLoadStates = true,
                                sourceLoadStates = event.sourceLoadStates,
                                mediatorLoadStates = event.mediatorLoadStates,
                                newHintReceiver = pagingData.hintReceiver
                            )
                        }
                        event is Insert -> {
                            // Process APPEND/PREPEND and send to presenter
                            presentPagingDataEvent(pageStore.processEvent(event))

                            // dispatch load states
                            combinedLoadStatesCollection.set(
                                sourceLoadStates = event.sourceLoadStates,
                                remoteLoadStates = event.mediatorLoadStates,
                            )

                            // If index points to a placeholder after transformations, resend it unless
                            // there are no more items to load.
                            val source = combinedLoadStatesCollection.stateFlow.value?.source
                            checkNotNull(source) {
                                "PagingDataPresenter.combinedLoadStatesCollection.stateFlow " +
                                    "should not hold null CombinedLoadStates after Insert event."
                            }
                            val prependDone = source.prepend.endOfPaginationReached
                            val appendDone = source.append.endOfPaginationReached
                            val canContinueLoading = !(event.loadType == PREPEND && prependDone) &&
                                !(event.loadType == APPEND && appendDone)

                            /**
                             *  If the insert is empty due to aggressive filtering, another hint
                             *  must be sent to fetcher-side to notify that PagingDataPresenter
                             *  received the page, since fetcher estimates prefetchDistance based on
                             *  page indices presented by PagingDataPresenter and we cannot rely on a
                             *  new item being bound to trigger another hint since the presented
                             *  page is empty.
                             */
                            val emptyInsert = event.pages.all { it.data.isEmpty() }
                            if (!canContinueLoading) {
                                // Reset lastAccessedIndexUnfulfilled since endOfPaginationReached
                                // means there are no more pages to load that could fulfill this
                                // index.
                                lastAccessedIndexUnfulfilled = false
                            } else if (lastAccessedIndexUnfulfilled || emptyInsert) {
                                val shouldResendHint = emptyInsert ||
                                    lastAccessedIndex < pageStore.placeholdersBefore ||
                                    lastAccessedIndex > pageStore.placeholdersBefore +
                                        pageStore.dataCount

                                if (shouldResendHint) {
                                    hintReceiver?.accessHint(
                                        pageStore.accessHintForPresenterIndex(lastAccessedIndex)
                                    )
                                } else {
                                    // lastIndex fulfilled, so reset lastAccessedIndexUnfulfilled.
                                    lastAccessedIndexUnfulfilled = false
                                }
                            }
                        }
                        event is Drop -> {
                            // Process DROP and send to presenter
                            presentPagingDataEvent(pageStore.processEvent(event))

                            // dispatch load states
                            combinedLoadStatesCollection.set(
                                type = event.loadType,
                                remote = false,
                                state = LoadState.NotLoading.Incomplete
                            )

                            // Reset lastAccessedIndexUnfulfilled if a page is dropped, to avoid
                            // infinite loops when maxSize is insufficiently large.
                            lastAccessedIndexUnfulfilled = false
                        }
                        event is PageEvent.LoadStateUpdate -> {
                            combinedLoadStatesCollection.set(
                                sourceLoadStates = event.source,
                                remoteLoadStates = event.mediator,
                            )
                        }
                    }
                    // Notify page updates after pageStore processes them.
                    //
                    // Note: This is not redundant with LoadStates because it does not de-dupe
                    // in cases where LoadState does not change, which would happen on cached
                    // PagingData collections.
                    if (event is Insert || event is Drop || event is StaticList) {
                        onPagesUpdatedListeners.forEach { it() }
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
    @MainThread
    public operator fun get(@IntRange(from = 0) index: Int): T? {
        lastAccessedIndexUnfulfilled = true
        lastAccessedIndex = index

        log(VERBOSE) { "Accessing item index[$index]" }
        hintReceiver?.accessHint(pageStore.accessHintForPresenterIndex(index))
        return pageStore.get(index)
    }

    /**
     * Returns the presented item at the specified position, without notifying Paging of the item
     * access that would normally trigger page loads.
     *
     * @param index Index of the presented item to return, including placeholders.
     * @return The presented item at position [index], `null` if it is a placeholder
     */
    @MainThread
    public fun peek(@IntRange(from = 0) index: Int): T? {
        return pageStore.get(index)
    }

    /**
     * Returns a new [ItemSnapshotList] representing the currently presented items, including any
     * placeholders if they are enabled.
     */
    public fun snapshot(): ItemSnapshotList<T> = pageStore.snapshot()

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     * [PagingDataPresenter].
     *
     * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
     * within the same generation of [PagingData].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    public fun retry() {
        log(DEBUG) { "Retry signal received" }
        uiReceiver?.retry()
    }

    /**
     * Refresh the data presented by this [PagingDataPresenter].
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
    public fun refresh() {
        log(DEBUG) { "Refresh signal received" }
        uiReceiver?.refresh()
    }

    /**
     * @return Total number of presented items, including placeholders.
     */
    public val size: Int
        get() = pageStore.size

    /**
     * A hot [Flow] of [CombinedLoadStates] that emits a snapshot whenever the loading state of the
     * current [PagingData] changes.
     *
     * This flow is conflated. It buffers the last update to [CombinedLoadStates] and immediately
     * delivers the current load states on collection, unless this [PagingDataPresenter] has not been
     * hooked up to a [PagingData] yet, and thus has no state to emit.
     *
     * @sample androidx.paging.samples.loadStateFlowSample
     */
    public val loadStateFlow: StateFlow<CombinedLoadStates?> =
        combinedLoadStatesCollection.stateFlow

    private val _onPagesUpdatedFlow: MutableSharedFlow<Unit> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = DROP_OLDEST,
    )

    /**
     * A hot [Flow] that emits after the pages presented to the UI are updated, even if the
     * actual items presented don't change.
     *
     * An update is triggered from one of the following:
     *   * [collectFrom] is called and initial load completes, regardless of any differences in
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
    public val onPagesUpdatedFlow: Flow<Unit>
        get() = _onPagesUpdatedFlow.asSharedFlow()

    init {
        addOnPagesUpdatedListener {
            _onPagesUpdatedFlow.tryEmit(Unit)
        }
    }

    /**
     * Add a listener which triggers after the pages presented to the UI are updated, even if the
     * actual items presented don't change.
     *
     * An update is triggered from one of the following:
     *   * [collectFrom] is called and initial load completes, regardless of any differences in
     *     the loaded data
     *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is inserted
     *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is dropped
     *
     * @param listener called after pages presented are updated.
     *
     * @see removeOnPagesUpdatedListener
     */
    public fun addOnPagesUpdatedListener(listener: () -> Unit) {
        onPagesUpdatedListeners.add(listener)
    }

    /**
     * Remove a previously registered listener for updates to presented pages.
     *
     * @param listener Previously registered listener.
     *
     * @see addOnPagesUpdatedListener
     */
    public fun removeOnPagesUpdatedListener(listener: () -> Unit) {
        onPagesUpdatedListeners.remove(listener)
    }

    /**
     * Add a [CombinedLoadStates] listener to observe the loading state of the current [PagingData].
     *
     * As new [PagingData] generations are submitted and displayed, the listener will be notified to
     * reflect the current [CombinedLoadStates].
     *
     * When a new listener is added, it will be immediately called with the current
     * [CombinedLoadStates], unless this [PagingDataPresenter] has not been hooked up to a [PagingData]
     * yet, and thus has no state to emit.
     *
     * @param listener [LoadStates] listener to receive updates.
     *
     * @see removeLoadStateListener
     *
     * @sample androidx.paging.samples.addLoadStateListenerSample
     */
    public fun addLoadStateListener(listener: (@JvmSuppressWildcards CombinedLoadStates) -> Unit) {
        combinedLoadStatesCollection.addListener(listener)
    }

    /**
     * Remove a previously registered [CombinedLoadStates] listener.
     *
     * @param listener Previously registered listener.
     * @see addLoadStateListener
     */
    public fun removeLoadStateListener(
        listener: (@JvmSuppressWildcards CombinedLoadStates) -> Unit
    ) {
        combinedLoadStatesCollection.removeListener(listener)
    }

    private suspend fun presentNewList(
        pages: List<TransformablePage<T>>,
        placeholdersBefore: Int,
        placeholdersAfter: Int,
        dispatchLoadStates: Boolean,
        sourceLoadStates: LoadStates?,
        mediatorLoadStates: LoadStates?,
        newHintReceiver: HintReceiver,
    ) {
        require(!dispatchLoadStates || sourceLoadStates != null) {
            "Cannot dispatch LoadStates in PagingDataPresenter without source LoadStates set."
        }

        lastAccessedIndexUnfulfilled = false

        val newPageStore = PageStore(
            pages = pages,
            placeholdersBefore = placeholdersBefore,
            placeholdersAfter = placeholdersAfter,
        )
        // must capture previousList states here before we update pageStore
        val previousList = pageStore as PlaceholderPaddedList<T>

        // update the store here before event is sent to ensure that snapshot() returned in
        // UI update callbacks (onChanged, onInsert etc) reflects the new list
        pageStore = newPageStore
        hintReceiver = newHintReceiver

        // send event to UI
        presentPagingDataEvent(
            PagingDataEvent.Refresh(
                newList = newPageStore as PlaceholderPaddedList<T>,
                previousList = previousList,
            )
        )
        log(DEBUG) {
            appendMediatorStatesIfNotNull(mediatorLoadStates) {
                """Presenting data (
                            |   first item: ${pages.firstOrNull()?.data?.firstOrNull()}
                            |   last item: ${pages.lastOrNull()?.data?.lastOrNull()}
                            |   placeholdersBefore: $placeholdersBefore
                            |   placeholdersAfter: $placeholdersAfter
                            |   hintReceiver: $newHintReceiver
                            |   sourceLoadStates: $sourceLoadStates
                        """
            }
        }
        // We may want to skip dispatching load states if triggered by a static list which wants to
        // preserve the previous state.
        if (dispatchLoadStates) {
            // Dispatch LoadState updates as soon as we are done diffing, but after
            // setting new pageStore.
            combinedLoadStatesCollection.set(sourceLoadStates!!, mediatorLoadStates)
        }
        if (newPageStore.size == 0) {
            // Send an initialize hint in case the new list is empty (no items or placeholders),
            // which would prevent a ViewportHint.Access from ever getting sent since there are
            // no items to bind from initial load. Without this hint, paging would stall on
            // an empty list because prepend/append would be not triggered.
            hintReceiver?.accessHint(newPageStore.initializeHint())
        }
    }
}

/**
 * Payloads used to dispatch change events.
 * Could become a public API post 3.0 in case developers want to handle it more effectively.
 *
 * Sending these change payloads is critical for the common case where DefaultItemAnimator won't
 * animate them and re-use the same view holder if possible.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class DiffingChangePayload {
    ITEM_TO_PLACEHOLDER,
    PLACEHOLDER_TO_ITEM,
    PLACEHOLDER_POSITION_CHANGE
}
