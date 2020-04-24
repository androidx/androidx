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

import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.RemoteMediator.InitializeAction
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.RemoteMediator.MediatorResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class PageFetcher<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> PagingSource<Key, Value>,
    private val initialKey: Key?,
    private val config: PagingConfig,
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
                emit(remoteMediatorAccessor?.onInitialize() == LAUNCH_INITIAL_REFRESH)
            }
            .scan(null) { previousGeneration: PageFetcherSnapshot<Key, Value>?,
                          triggerRemoteRefresh ->
                val pagingSource = pagingSourceFactory()
                val initialKey = previousGeneration?.refreshKeyInfo()
                    ?.let { pagingSource.getRefreshKey(it) }
                    ?: initialKey

                // Hook up refresh signals from DataSource / PagingSource.
                pagingSource.registerInvalidatedCallback(::invalidate)
                previousGeneration?.pagingSource?.unregisterInvalidatedCallback(::invalidate)
                previousGeneration?.pagingSource?.invalidate() // Note: Invalidate is idempotent.
                previousGeneration?.close()

                PageFetcherSnapshot(
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

    inner class PagerUiReceiver<Key : Any, Value : Any> constructor(
        private val pageFetcherSnapshot: PageFetcherSnapshot<Key, Value>,
        private val retryChannel: SendChannel<Unit>
    ) : UiReceiver {
        override fun addHint(hint: ViewportHint) = pageFetcherSnapshot.addHint(hint)

        override fun retry() {
            retryChannel.offer(Unit)
        }

        override fun refresh() = this@PageFetcher.refresh()
    }
}

/**
 * Usage of [RemoteMediator] within [PageFetcher] and [PageFetcherSnapshot] should always be
 * accessed behind this class, which handles state tracking of active remote jobs.
 */
internal class RemoteMediatorAccessor<Key : Any, Value : Any>(
    private val remoteMediator: RemoteMediator<Key, Value>
) {
    private val jobsByLoadTypeLock = Mutex()
    private val jobsByLoadType = HashMap<LoadType, Deferred<MediatorResult>>()

    suspend fun onInitialize(): InitializeAction {
        return remoteMediator.initialize()
    }

    /**
     * Launches a remote load request with the backing [MediatorResult] if no current
     * [kotlinx.coroutines.Job] for the passed [LoadType] is running, otherwise returns the
     * result of the existing [kotlinx.coroutines.Job].
     */
    internal suspend fun load(
        scope: CoroutineScope,
        loadType: LoadType,
        state: PagingState<Key, Value>
    ): MediatorResult {
        val deferred = jobsByLoadTypeLock.withLock {
            if (jobsByLoadType[loadType]?.isActive != true) {
                // List of RemoteMediator.load jobs that were registered prior to this one.
                val existingJobs = jobsByLoadType.values.toList() // Immutable copy.
                val existingBoundaryJobs = listOfNotNull(jobsByLoadType[START], jobsByLoadType[END])

                jobsByLoadType[loadType] = scope.async {
                    if (loadType == REFRESH) {
                        // Since RemoteMediator is expected to perform writes to the local DB
                        // in the common case, it's not safe to just cancel and proceed with
                        // REFRESH here. If we do that, the REFRESH could race with e.g. the
                        // START job, and it's unsafe for an old START to land in the DB after
                        // a newer REFRESH. Due to cooperative cancellation, the START job may
                        // not actually realize it's cancelled before performing its write.
                        existingBoundaryJobs.forEach { it.cancel() }
                        existingBoundaryJobs.joinAll()
                    }

                    // Only allow one active RemoteMediator.load at a time, by joining all jobs
                    // registered in jobsByLoadType before this one.
                    existingJobs.forEach { it.join() }

                    remoteMediator.load(loadType, state)
                }
            }

            jobsByLoadType.getValue(loadType)
        }

        return deferred.await()
    }
}
