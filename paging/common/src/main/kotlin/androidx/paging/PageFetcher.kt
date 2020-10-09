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

import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class PageFetcher<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> PagingSource<Key, Value>,
    private val initialKey: Key?,
    private val config: PagingConfig,
    @OptIn(ExperimentalPagingApi::class)
    remoteMediator: RemoteMediator<Key, Value>? = null
) {
    private val remoteMediatorAccessor = remoteMediator?.let { RemoteMediatorAccessor(it) }

    /**
     * Channel of refresh signals that would trigger a new instance of [PageFetcherSnapshot].
     * Signals sent to this channel should be `true` if a remote REFRESH load should be triggered,
     * `false` otherwise.
     *
     * NOTE: This channel is conflated, which means it has a buffer size of 1, and will always
     *  broadcast the latest value received.
     */
    private val refreshChannel = ConflatedBroadcastChannel<Boolean>()

    private val retryChannel = ConflatedBroadcastChannel<Unit>()

    // The object built by paging builder can maintain the scope so that on rotation we don't stop
    // the paging.
    val flow: Flow<PagingData<Value>> = channelFlow {
        refreshChannel.asFlow()
            .onStart {
                @OptIn(ExperimentalPagingApi::class)
                emit(remoteMediatorAccessor?.initialize() == LAUNCH_INITIAL_REFRESH)
            }
            .scan(null) { previousGeneration: PageFetcherSnapshot<Key, Value>?,
                triggerRemoteRefresh ->
                var pagingSource = generateNewPagingSource(previousGeneration?.pagingSource)
                while (pagingSource.invalid) {
                    pagingSource = generateNewPagingSource(previousGeneration?.pagingSource)
                }

                @OptIn(ExperimentalPagingApi::class)
                val initialKey: Key? = previousGeneration?.refreshKeyInfo()
                    ?.let { pagingSource.getRefreshKey(it) }
                    ?: initialKey

                previousGeneration?.close()

                PageFetcherSnapshot<Key, Value>(
                    initialKey = initialKey,
                    pagingSource = pagingSource,
                    config = config,
                    retryFlow = retryChannel.asFlow(),
                    // Only trigger remote refresh on refresh signals that do not originate from
                    // initialization or PagingSource invalidation.
                    triggerRemoteRefresh = triggerRemoteRefresh,
                    remoteMediatorAccessor = remoteMediatorAccessor,
                    invalidate = this@PageFetcher::refresh
                )
            }
            .filterNotNull()
            .mapLatest { generation ->
                PagingData(generation.pageEventFlow, PagerUiReceiver(generation, retryChannel))
            }
            .collect { send(it) }
    }

    fun refresh() {
        refreshChannel.offer(true)
    }

    private fun invalidate() {
        refreshChannel.offer(false)
    }

    private fun generateNewPagingSource(
        previousPagingSource: PagingSource<Key, Value>?
    ): PagingSource<Key, Value> {
        val pagingSource = pagingSourceFactory()

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
        private val retryChannel: SendChannel<Unit>
    ) : UiReceiver {
        override fun accessHint(viewportHint: ViewportHint) {
            pageFetcherSnapshot.accessHint(viewportHint)
        }

        override fun retry() {
            retryChannel.offer(Unit)
        }

        override fun refresh() = this@PageFetcher.refresh()
    }
}
