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

import androidx.annotation.VisibleForTesting
import androidx.paging.CombineSource.RECEIVER
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.internal.BUGANIZER_URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

internal class PageFetcher<Key : Any, Value : Any>(
    private val pagingSourceFactory: suspend () -> PagingSource<Key, Value>,
    private val initialKey: Key?,
    private val config: PagingConfig,
    @OptIn(ExperimentalPagingApi::class) remoteMediator: RemoteMediator<Key, Value>? = null,
) {
    /**
     * Channel of refresh signals that would trigger a new instance of [PageFetcherSnapshot].
     * Signals sent to this channel should be `true` if a remote REFRESH load should be triggered,
     * `false` otherwise.
     *
     * NOTE: This channel is conflated, which means it has a buffer size of 1, and will always
     * broadcast the latest value received.
     */
    private val loadRequests = ConflatedEventBus<LoadRequest>()

    private val retryEvents = ConflatedEventBus<Unit>()

    // The object built by paging builder can maintain the scope so that on rotation we don't stop
    // the paging.
    val flow: Flow<PagingData<Value>> = simpleChannelFlow {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediatorAccessor = remoteMediator?.let { RemoteMediatorAccessor(this, it) }

        loadRequests.flow
            .onStart {
                @OptIn(ExperimentalPagingApi::class)
                emit(
                    LoadRequest.Refresh(
                        remoteMediatorAccessor?.initialize() == LAUNCH_INITIAL_REFRESH,
                        RefreshType.Initial,
                    )
                )
            }
            .simpleScan(null) {
                currentGeneration: GenerationInfo<Key, Value>?,
                loadRequest: LoadRequest ->
                // if REFRESH, create new PagingSource, PagingState, PageFetcherSnapshot, and
                // finally
                // new PagingData
                if (loadRequest is LoadRequest.Refresh) {
                    // Enable refresh if this is the first generation and we have
                    // LAUNCH_INITIAL_REFRESH
                    // or if this generation was started due to [refresh] being invoked.
                    if (loadRequest.triggerRemoteRefresh) {
                        remoteMediatorAccessor?.allowRefresh()
                    }

                    // initial refresh
                    if (currentGeneration == null) {
                        GenerationInfo(
                            snapshot =
                                PageFetcherSnapshot(
                                    initialKey = initialKey,
                                    pagingSource = generateNewPagingSource(null),
                                    config = config,
                                    retryFlow = retryEvents.flow,
                                    // Only trigger remote refresh on refresh signals that do not
                                    // originate
                                    // from
                                    // initialization or PagingSource invalidation.
                                    remoteMediatorConnection = remoteMediatorAccessor,
                                    jumpCallback = this@PageFetcher::refresh,
                                    cachedInitialState = null,
                                ),
                            cachedInitialState = null,
                            job = Job(),
                        )
                    } else {
                        // create a new generation of PagingSource and PageFetcherSnapshot
                        // based on current state
                        val newPagingSource =
                            generateNewPagingSource(
                                currentPagingSource = currentGeneration.snapshot.pagingSource
                            )

                        var currentPagingState = currentGeneration.snapshot.currentPagingState()

                        // If cached PagingState had pages loaded, but current generation didn't,
                        // use
                        // the cached PagingState to handle cases where invalidation happens too
                        // quickly,
                        // so that getRefreshKey and remote refresh at least have some data to work
                        // with.
                        if (
                            currentPagingState.pages.isEmpty() &&
                                currentGeneration.cachedInitialState?.pages?.isNotEmpty() == true
                        ) {
                            currentPagingState = currentGeneration.cachedInitialState
                        }

                        // If previous generation was invalidated before anchorPosition was
                        // established,
                        // re-use last PagingState that successfully loaded pages and has an
                        // anchorPosition.
                        // This prevents rapid invalidation from deleting the anchorPosition if the
                        // previous generation didn't have time to load before getting invalidated.
                        if (
                            currentPagingState.anchorPosition == null &&
                                currentGeneration.cachedInitialState?.anchorPosition != null
                        ) {
                            currentPagingState = currentGeneration.cachedInitialState
                        }

                        val (refreshKey: Key?, refreshSize: Int) =
                            when {
                                loadRequest.type == RefreshType.Anchor ->
                                    newPagingSource.getRefreshKey(currentPagingState).also {
                                        log(DEBUG) {
                                            "Refresh key $it returned from PagingSource $newPagingSource"
                                        }
                                    } to config.initialLoadSize
                                loadRequest.type is RefreshType.Item -> {
                                    val item = loadRequest.type.item
                                    val page =
                                        currentPagingState.pages.firstOrNull {
                                            it.data.contains(item)
                                        }
                                    requireNotNull(page) {
                                        "Invalid Refresh item. Item $item not found in ${currentPagingState.pages.sumOf { it.data.size }} loaded items."
                                    }
                                    currentGeneration.snapshot.getLoadKey(page).also {
                                        log(DEBUG) { "Refresh key $it based around item $item" }
                                    } to config.initialLoadSize
                                }
                                loadRequest.type == RefreshType.All ->
                                    currentGeneration.snapshot
                                        .getLoadKey(currentPagingState.pages.first())
                                        .also {
                                            log(DEBUG) {
                                                "Refresh key $it from first item ${currentPagingState.pages.first().first()}"
                                            }
                                        } to currentPagingState.pages.sumOf { it.data.size }

                                else -> throw IllegalStateException("should not get here")
                            }

                        currentGeneration.snapshot.close()
                        currentGeneration.job.cancel()

                        GenerationInfo(
                            snapshot =
                                PageFetcherSnapshot(
                                    initialKey = refreshKey,
                                    pagingSource = newPagingSource,
                                    config = config,
                                    retryFlow = retryEvents.flow,
                                    // Only trigger remote refresh on refresh signals that do not
                                    // originate
                                    // from
                                    // initialization or PagingSource invalidation.
                                    remoteMediatorConnection = remoteMediatorAccessor,
                                    initialLoadSize = refreshSize,
                                    jumpCallback = this@PageFetcher::refresh,
                                    cachedInitialState = currentPagingState,
                                ),
                            cachedInitialState = currentPagingState,
                            job = Job(),
                        )
                    }
                } else {
                    requireNotNull(currentGeneration) {
                        "Append or Prepend request should be sent after a Refresh. " +
                            "This error indicates a bug in the Paging library. Please file a bug report in Buganizer."
                    }
                    // This forced load does not set lastAccessedIndex. This is so that we
                    // can continue to support
                    // refreshes based on scroll position even if there were forced loads just
                    // before
                    // the refresh.
                    currentGeneration.snapshot.forceSetHint(loadRequest.loadType)
                    // A new generation should only happen after Refreshes. So for append/prepends
                    // we keep the same generation going.
                    currentGeneration
                }
            }
            .filterNotNull()
            // append/prepends reuse the same generation, and only new Generations (refresh) should
            // trigger
            // a new PagingData
            .distinctUntilChanged()
            .simpleMapLatest { generation ->
                val downstreamFlow =
                    generation.snapshot
                        .injectRemoteEvents(generation.job, remoteMediatorAccessor)
                        .onEach { log(VERBOSE) { "Sent $it" } }

                PagingData(
                    flow = downstreamFlow,
                    uiReceiver = PagerUiReceiver(retryEvents),
                    hintReceiver = PagerHintReceiver(generation.snapshot),
                )
            }
            .collect(::send)
    }

    fun load(loadType: LoadType) {
        val request =
            when (loadType) {
                APPEND -> LoadRequest.Append
                PREPEND -> LoadRequest.Prepend
                REFRESH -> throw IllegalArgumentException("Load only applies to APPEND or PREPEND")
            }
        loadRequests.send(request)
    }

    fun refreshAll() {
        loadRequests.send(LoadRequest.Refresh(triggerRemoteRefresh = true, type = RefreshType.All))
    }

    fun refresh(item: Value? = null) {
        val refreshType = if (item == null) RefreshType.Anchor else RefreshType.Item(item)
        loadRequests.send(LoadRequest.Refresh(triggerRemoteRefresh = true, type = refreshType))
    }

    private fun invalidate() {
        loadRequests.send(LoadRequest.Refresh(false, RefreshType.Anchor))
    }

    private fun PageFetcherSnapshot<Key, Value>.injectRemoteEvents(
        job: Job,
        accessor: RemoteMediatorAccessor<Key, Value>?,
    ): Flow<PageEvent<Value>> {
        if (accessor == null) return pageEventFlow

        val sourceStates = MutableLoadStateCollection()
        // We wrap this in a cancelableChannelFlow to allow co-operative cancellation, otherwise
        // RemoteMediatorAccessor's StateFlow will keep this Flow running on old generations.
        return cancelableChannelFlow(job) {
            accessor.state
                // Note: Combine waits for PageFetcherSnapshot to emit an event before sending
                // anything. This avoids sending the initial idle state, since it would cause
                // load state flickering on rapid invalidation.
                .combineWithoutBatching(pageEventFlow) { remoteState, sourceEvent, updateFrom ->
                    if (updateFrom != RECEIVER) {
                        when (sourceEvent) {
                            is Insert -> {
                                sourceStates.set(sourceEvent.sourceLoadStates)
                                @Suppress("DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING")
                                sourceEvent.copy(
                                    sourceLoadStates = sourceEvent.sourceLoadStates,
                                    mediatorLoadStates = remoteState,
                                )
                            }
                            is Drop -> {
                                sourceStates.set(
                                    type = sourceEvent.loadType,
                                    state = LoadState.NotLoading.Incomplete,
                                )
                                sourceEvent
                            }
                            is LoadStateUpdate -> {
                                sourceStates.set(sourceEvent.source)
                                LoadStateUpdate(source = sourceEvent.source, mediator = remoteState)
                            }
                            is PageEvent.StaticList -> {
                                throw IllegalStateException(
                                    """Paging generated an event to display a static list that
                                        | originated from a paginated source. If you see this
                                        | exception, it is most likely a bug in the library.
                                        | Please file a bug so we can fix it at:
                                        | $BUGANIZER_URL"""
                                        .trimMargin()
                                )
                            }
                        }
                    } else {
                        LoadStateUpdate(source = sourceStates.snapshot(), mediator = remoteState)
                    }
                }
                .collect { send(it) }
        }
    }

    private suspend fun generateNewPagingSource(
        currentPagingSource: PagingSource<Key, Value>?
    ): PagingSource<Key, Value> {
        val newPagingSource = pagingSourceFactory()
        if (newPagingSource is CompatLegacyPagingSource) {
            newPagingSource.setPageSize(config.pageSize)
        }
        // Ensure pagingSourceFactory produces a new instance of PagingSource.
        check(newPagingSource !== currentPagingSource) {
            """
            An instance of PagingSource was re-used when Pager expected to create a new
            instance. Ensure that the pagingSourceFactory passed to Pager always returns a
            new instance of PagingSource.
            """
                .trimIndent()
        }

        // Hook up refresh signals from PagingSource.
        newPagingSource.registerInvalidatedCallback(::invalidate)
        currentPagingSource?.unregisterInvalidatedCallback(::invalidate)
        currentPagingSource?.invalidate() // Note: Invalidate is idempotent.
        log(DEBUG) { "Generated new PagingSource $newPagingSource" }

        return newPagingSource
    }

    inner class PagerUiReceiver(private val retryEventBus: ConflatedEventBus<Unit>) : UiReceiver {
        override fun retry() {
            retryEventBus.send(Unit)
        }

        override fun refresh() = this@PageFetcher.refresh()
    }

    inner class PagerHintReceiver<Key : Any, Value : Any>
    constructor(
        @get:VisibleForTesting internal val pageFetcherSnapshot: PageFetcherSnapshot<Key, Value>
    ) : HintReceiver {

        override fun processHint(viewportHint: ViewportHint) {
            pageFetcherSnapshot.processHint(viewportHint)
        }
    }

    /**
     * A generation of loaded snapshot and load state. A refresh creates a new generation.
     *
     * @param cachedInitialState the pre-existing PagingState (if any) when this Generation was
     *   created. It represents the generation prior to this generation and serves as the fallback
     *   state if this generation were invalidated before it established any states (i.e.
     *   consecutive refreshes).
     */
    private class GenerationInfo<Key : Any, Value : Any>(
        val snapshot: PageFetcherSnapshot<Key, Value>,
        val cachedInitialState: PagingState<Key, Value>?,
        val job: Job,
    )

    private sealed class LoadRequest(val loadType: LoadType) {

        class Refresh(val triggerRemoteRefresh: Boolean, val type: RefreshType) :
            LoadRequest(REFRESH)

        object Append : LoadRequest(APPEND)

        object Prepend : LoadRequest(PREPEND)
    }

    private sealed class RefreshType {
        object Initial : RefreshType()

        object All : RefreshType()

        object Anchor : RefreshType()

        data class Item(val item: Any) : RefreshType()
    }
}
