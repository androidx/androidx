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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class PageFetcher<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> PagingSource<Key, Value>,
    private val initialKey: Key?,
    private val config: PagingConfig,
    @OptIn(ExperimentalPagingApi::class)
    private val remoteMediator: RemoteMediator<Key, Value>? = null
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
    val flow: Flow<PagingData<Value>> = channelFlow {
        val remoteMediatorAccessor = remoteMediator?.let {
            RemoteMediatorAccessor(this, it)
        }
        refreshEvents
            .flow
            .onStart {
                @OptIn(ExperimentalPagingApi::class)
                emit(remoteMediatorAccessor?.initialize() == LAUNCH_INITIAL_REFRESH)
            }
            .scan(null) { previousGeneration: GenerationInfo<Key, Value>?,
                triggerRemoteRefresh: Boolean ->
                var pagingSource = generateNewPagingSource(
                    previousGeneration?.snapshot?.pagingSource
                )
                while (pagingSource.invalid) {
                    pagingSource = generateNewPagingSource(
                        previousGeneration?.snapshot?.pagingSource
                    )
                }

                var previousPagingState = previousGeneration?.snapshot?.refreshKeyInfo()
                // If previous generation was invalidated before anchorPosition was established,
                // re-use last PagingState that successfully loaded pages and has an anchorPosition.
                // This prevents rapid invalidation from deleting the anchorPosition if the
                // previous generation didn't have time to load before getting invalidated. We
                // check for anchorPosition before overriding to prevent empty PagingState from
                // overriding a PagingState with pages loaded, but no anchorPosition.
                if (previousPagingState?.anchorPosition == null &&
                    previousGeneration?.state?.anchorPosition != null
                ) {
                    previousPagingState = previousGeneration.state
                }

                @OptIn(ExperimentalPagingApi::class)
                val initialKey: Key? = previousPagingState?.let { pagingSource.getRefreshKey(it) }
                    ?: initialKey

                previousGeneration?.snapshot?.close()

                GenerationInfo(
                    snapshot = PageFetcherSnapshot<Key, Value>(
                        initialKey = initialKey,
                        pagingSource = pagingSource,
                        config = config,
                        retryFlow = retryEvents.flow,
                        // Only trigger remote refresh on refresh signals that do not originate from
                        // initialization or PagingSource invalidation.
                        triggerRemoteRefresh = triggerRemoteRefresh,
                        remoteMediatorConnection = remoteMediatorAccessor,
                        invalidate = this@PageFetcher::refresh
                    ),
                    state = previousPagingState,
                )
            }
            .filterNotNull()
            .mapLatest { generation ->
                val downstreamFlow = if (remoteMediatorAccessor == null) {
                    generation.snapshot.pageEventFlow
                } else {
                    generation.snapshot.injectRemoteEvents(remoteMediatorAccessor)
                }
                PagingData(
                    flow = downstreamFlow,
                    receiver = PagerUiReceiver(generation.snapshot, retryEvents)
                )
            }
            .collect { send(it) }
    }

    private fun PageFetcherSnapshot<Key, Value>.injectRemoteEvents(
        accessor: RemoteMediatorAccessor<Key, Value>
    ): Flow<PageEvent<Value>> = channelFlow {
        suspend fun dispatchIfValid(type: LoadType, state: LoadState) {
            // not loading events are sent w/ insert-drop events.
            if (PageEvent.LoadStateUpdate.canDispatchWithoutInsert(state, fromMediator = true)) {
                send(
                    PageEvent.LoadStateUpdate<Value>(type, true, state)
                )
            } else {
                // ignore. Some invalidation will happened and we'll send the event there instead
            }
        }
        launch {
            var prev = LoadStates.IDLE
            accessor.state.collect {
                if (prev.refresh != it.refresh) {
                    dispatchIfValid(REFRESH, it.refresh)
                }
                if (prev.prepend != it.prepend) {
                    dispatchIfValid(PREPEND, it.prepend)
                }
                if (prev.append != it.append) {
                    dispatchIfValid(APPEND, it.append)
                }
                prev = it
            }
        }

        this@injectRemoteEvents.pageEventFlow.collect {
            // only insert events have combinedLoadStates.
            if (it is PageEvent.Insert<Value>) {
                send(
                    it.copy(
                        combinedLoadStates = CombinedLoadStates(
                            it.combinedLoadStates.source,
                            accessor.state.value
                        )
                    )
                )
            } else {
                send(it)
            }
        }
    }

    fun refresh() {
        refreshEvents.send(true)
    }

    private fun invalidate() {
        refreshEvents.send(false)
    }

    private fun generateNewPagingSource(
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

        return pagingSource
    }

    inner class PagerUiReceiver<Key : Any, Value : Any> constructor(
        private val pageFetcherSnapshot: PageFetcherSnapshot<Key, Value>,
        private val retryEventBus: ConflatedEventBus<Unit>
    ) : UiReceiver {
        override fun accessHint(viewportHint: ViewportHint) {
            pageFetcherSnapshot.accessHint(viewportHint)
        }

        override fun retry() {
            retryEventBus.send(Unit)
        }

        override fun refresh() = this@PageFetcher.refresh()
    }

    private class GenerationInfo<Key : Any, Value : Any>(
        val snapshot: PageFetcherSnapshot<Key, Value>,
        val state: PagingState<Key, Value>?
    )
}
