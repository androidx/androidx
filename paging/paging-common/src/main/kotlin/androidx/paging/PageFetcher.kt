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
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.internal.BUGANIZER_URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

internal class PageFetcher<Key : Any, Value : Any>(
    private val pagingSourceFactory: suspend () -> PagingSource<Key, Value>,
    private val initialKey: Key?,
    private val config: PagingConfig,
    @OptIn(ExperimentalPagingApi::class)
    remoteMediator: RemoteMediator<Key, Value>? = null
) {
    /**
     * Channel of refresh signals that would trigger a new instance of [PageFetcherSnapshot].
     * Signals sent to this channel should be `true` if a remote REFRESH load should be triggered,
     * `false` otherwise.
     *
     * NOTE: This channel is conflated, which means it has a buffer size of 1, and will always
     *  broadcast the latest value received.
     */
    private val refreshEvents = ConflatedEventBus<Boolean>()

    private val retryEvents = ConflatedEventBus<Unit>()

    // The object built by paging builder can maintain the scope so that on rotation we don't stop
    // the paging.
    val flow: Flow<PagingData<Value>> = simpleChannelFlow {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediatorAccessor = remoteMediator?.let {
            RemoteMediatorAccessor(this, it)
        }

        refreshEvents
            .flow
            .onStart {
                @OptIn(ExperimentalPagingApi::class)
                emit(remoteMediatorAccessor?.initialize() == LAUNCH_INITIAL_REFRESH)
            }
            .simpleScan(null) { previousGeneration: GenerationInfo<Key, Value>?,
                triggerRemoteRefresh: Boolean ->
                // Enable refresh if this is the first generation and we have LAUNCH_INITIAL_REFRESH
                // or if this generation was started due to [refresh] being invoked.
                if (triggerRemoteRefresh) {
                    remoteMediatorAccessor?.allowRefresh()
                }

                val pagingSource = generateNewPagingSource(
                    previousPagingSource = previousGeneration?.snapshot?.pagingSource
                )

                var previousPagingState = previousGeneration?.snapshot?.currentPagingState()

                // If cached PagingState had pages loaded, but previous generation didn't, use
                // the cached PagingState to handle cases where invalidation happens too quickly,
                // so that getRefreshKey and remote refresh at least have some data to work with.
                if (previousPagingState?.pages.isNullOrEmpty() &&
                    previousGeneration?.state?.pages?.isNotEmpty() == true
                ) {
                    previousPagingState = previousGeneration.state
                }

                // If previous generation was invalidated before anchorPosition was established,
                // re-use last PagingState that successfully loaded pages and has an anchorPosition.
                // This prevents rapid invalidation from deleting the anchorPosition if the
                // previous generation didn't have time to load before getting invalidated.
                if (previousPagingState?.anchorPosition == null &&
                    previousGeneration?.state?.anchorPosition != null
                ) {
                    previousPagingState = previousGeneration.state
                }

                val initialKey: Key? = when (previousPagingState) {
                    null -> initialKey
                    else -> pagingSource.getRefreshKey(previousPagingState).also {
                        log(DEBUG) { "Refresh key $it returned from PagingSource $pagingSource" }
                    }
                }

                previousGeneration?.snapshot?.close()
                previousGeneration?.job?.cancel()

                GenerationInfo(
                    snapshot = PageFetcherSnapshot(
                        initialKey = initialKey,
                        pagingSource = pagingSource,
                        config = config,
                        retryFlow = retryEvents.flow,
                        // Only trigger remote refresh on refresh signals that do not originate from
                        // initialization or PagingSource invalidation.
                        remoteMediatorConnection = remoteMediatorAccessor,
                        jumpCallback = this@PageFetcher::refresh,
                        previousPagingState = previousPagingState,
                    ),
                    state = previousPagingState,
                    job = Job(),
                )
            }
            .filterNotNull()
            .simpleMapLatest { generation ->
                val downstreamFlow = generation.snapshot
                    .injectRemoteEvents(generation.job, remoteMediatorAccessor)
                    .onEach { log(VERBOSE) { "Sent $it" } }

                PagingData(
                    flow = downstreamFlow,
                    uiReceiver = PagerUiReceiver(retryEvents),
                    hintReceiver = PagerHintReceiver(generation.snapshot)
                )
            }
            .collect(::send)
    }

    fun refresh() {
        refreshEvents.send(true)
    }

    private fun invalidate() {
        refreshEvents.send(false)
    }

    private fun PageFetcherSnapshot<Key, Value>.injectRemoteEvents(
        job: Job,
        accessor: RemoteMediatorAccessor<Key, Value>?
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
                                sourceEvent.copy(
                                    sourceLoadStates = sourceEvent.sourceLoadStates,
                                    mediatorLoadStates = remoteState,
                                )
                            }
                            is Drop -> {
                                sourceStates.set(
                                    type = sourceEvent.loadType,
                                    state = LoadState.NotLoading.Incomplete
                                )
                                sourceEvent
                            }
                            is LoadStateUpdate -> {
                                sourceStates.set(sourceEvent.source)
                                LoadStateUpdate(
                                    source = sourceEvent.source,
                                    mediator = remoteState,
                                )
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
                        LoadStateUpdate(
                            source = sourceStates.snapshot(),
                            mediator = remoteState,
                        )
                    }
                }
                .collect { send(it) }
        }
    }

    private suspend fun generateNewPagingSource(
        previousPagingSource: PagingSource<Key, Value>?
    ): PagingSource<Key, Value> {
        val pagingSource = pagingSourceFactory()
        if (pagingSource is LegacyPagingSource) {
            pagingSource.setPageSize(config.pageSize)
        }
        // Ensure pagingSourceFactory produces a new instance of PagingSource.
        check(pagingSource !== previousPagingSource) {
            """
            An instance of PagingSource was re-used when Pager expected to create a new
            instance. Ensure that the pagingSourceFactory passed to Pager always returns a
            new instance of PagingSource.
            """.trimIndent()
        }

        // Hook up refresh signals from PagingSource.
        pagingSource.registerInvalidatedCallback(::invalidate)
        previousPagingSource?.unregisterInvalidatedCallback(::invalidate)
        previousPagingSource?.invalidate() // Note: Invalidate is idempotent.
        log(DEBUG) { "Generated new PagingSource $pagingSource" }

        return pagingSource
    }

    inner class PagerUiReceiver(private val retryEventBus: ConflatedEventBus<Unit>) : UiReceiver {
        override fun retry() {
            retryEventBus.send(Unit)
        }

        override fun refresh() = this@PageFetcher.refresh()
    }

    inner class PagerHintReceiver<Key : Any, Value : Any> constructor(
        @VisibleForTesting
        internal val pageFetcherSnapshot: PageFetcherSnapshot<Key, Value>,
    ) : HintReceiver {

        override fun accessHint(viewportHint: ViewportHint) {
            pageFetcherSnapshot.accessHint(viewportHint)
        }
    }

    private class GenerationInfo<Key : Any, Value : Any>(
        val snapshot: PageFetcherSnapshot<Key, Value>,
        val state: PagingState<Key, Value>?,
        val job: Job,
    )
}
