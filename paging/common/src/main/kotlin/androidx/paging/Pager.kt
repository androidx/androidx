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

import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Idle
import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PagedSource.LoadParams
import androidx.paging.PagedSource.LoadResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds a generation of pageable data, a snapshot of data loaded by [PagedSource]. An instance
 * of [Pager] and its corresponding [PagerState] should be launched within a scope that is
 * cancelled when [PagedSource.invalidate] is called.
 */
@ExperimentalCoroutinesApi
@FlowPreview
internal class Pager<Key : Any, Value : Any>(
    internal val initialKey: Key?,
    private val pagedSource: PagedSource<Key, Value>,
    private val config: PagedList.Config
) {
    private val hintChannel = BroadcastChannel<ViewportHint>(Channel.BUFFERED)
    private var lastHint: ViewportHint? = null

    private val stateLock = Mutex()
    private val state = PagerState<Key, Value>(config.maxSize)

    fun addHint(hint: ViewportHint) {
        lastHint = hint
        hintChannel.offer(hint)
    }

    fun create(): Flow<PageEvent<Value>> = channelFlow {
        launch { state.consumeAsFlow().collect { send(it) } }
        state.doInitialLoad()

        val prependJob = launch {
            state.consumePrependGenerationIdAsFlow()
                .transformLatest<Int, GenerationalViewportHint> { generationId ->
                    // Reset state to Idle and setup a new flow for consuming incoming load hints.
                    // Subsequent generationIds are normally triggered by cancellation.
                    stateLock.withLock { state.updateLoadState(START, Idle) }
                    val generationalHints = hintChannel.asFlow()
                        .conflate()
                        .map { hint -> GenerationalViewportHint(generationId, hint) }
                    emitAll(generationalHints)
                }
                .scan(GenerationalViewportHint(0, ViewportHint.MAX_VALUE)) { acc, it ->
                    if (acc.hint < it.hint) acc else it
                }
                .filter { it != GenerationalViewportHint(0, ViewportHint.MAX_VALUE) }
                .distinctUntilChanged()
                .conflate()
                .collect { generationalHint -> state.doLoad(START, generationalHint) }
        }

        val appendJob = launch {
            state.consumeAppendGenerationIdAsFlow()
                .transformLatest<Int, GenerationalViewportHint> { generationId ->
                    // Reset state to Idle and setup a new flow for consuming incoming load hints.
                    // Subsequent generationIds are normally triggered by cancellation.
                    stateLock.withLock { state.updateLoadState(END, Idle) }
                    val generationalHints = hintChannel.asFlow()
                        .conflate()
                        .map { hint -> GenerationalViewportHint(generationId, hint) }
                    emitAll(generationalHints)
                }
                .scan(GenerationalViewportHint(0, ViewportHint.MIN_VALUE)) { acc, it ->
                    if (acc.hint > it.hint) acc else it
                }
                .filter { it != GenerationalViewportHint(0, ViewportHint.MIN_VALUE) }
                .distinctUntilChanged()
                .conflate()
                .collect { generationalHint -> state.doLoad(END, generationalHint) }
        }

        // TODO: Test if we can remove this!
        joinAll(prependJob, appendJob)
    }

    suspend fun refreshKeyInfo(): RefreshInfo<Key, Value>? {
        return lastHint?.let { hint ->
            stateLock.withLock {
                with(state) {
                    hint.withCoercedHint { indexInPage, pageIndex, _, _ ->
                        state.refreshInfo(indexInPage, pageIndex)
                    }
                }
            }
        }
    }

    private suspend fun PagedSource<Key, Value>.load(loadType: LoadType, key: Key?) = load(
        LoadParams(
            loadType = loadType,
            key = key,
            loadSize = if (loadType == REFRESH) config.initialLoadSizeHint else config.pageSize,
            placeholdersEnabled = config.enablePlaceholders,
            pageSize = config.pageSize
        )
    )

    private suspend fun PagerState<Key, Value>.doInitialLoad() {
        stateLock.withLock { updateLoadState(REFRESH, Loading) }

        val result = pagedSource.load(REFRESH, initialKey)
        stateLock.withLock {
            when (result) {
                is LoadResult.Page<Key, Value> -> {
                    insert(0, REFRESH, result)
                    updateLoadState(REFRESH, Idle)
                }
                is LoadResult.Error -> updateLoadState(REFRESH, Error(result.throwable))
            }
        }
    }

    // TODO: Consider making this a transform operation which emits PageEvents
    private suspend fun PagerState<Key, Value>.doLoad(
        loadType: LoadType,
        generationalHint: GenerationalViewportHint
    ) {
        require(loadType != REFRESH) { "Use doInitialLoad for LoadType == REFRESH" }

        var loadKey: Key? = stateLock.withLock {
            with(state) {
                generationalHint.hint.withCoercedHint { indexInPage, pageIndex, _, _ ->
                    nextLoadKeyOrNull(
                        loadType,
                        generationalHint.generationId,
                        indexInPage,
                        pageIndex
                    )?.also {
                        updateLoadState(loadType, Loading)
                    }
                }
            }
        }

        loop@ while (loadKey != null) {
            when (val result: LoadResult<Key, Value> = pagedSource.load(loadType, loadKey)) {
                is LoadResult.Page<Key, Value> -> {
                    val insertApplied = stateLock.withLock {
                        insert(generationalHint.generationId, loadType, result)
                    }

                    // Break if insert was skipped due to cancellation
                    if (!insertApplied) break@loop
                }
                is LoadResult.Error -> {
                    stateLock.withLock { updateLoadState(loadType, Error(result.throwable)) }
                    return
                }
            }

            stateLock.withLock {
                val dropType = when (loadType) {
                    START -> END
                    else -> START
                }

                state.dropInfo(dropType)?.let { info ->
                    state.drop(dropType, info.pageCount, info.placeholdersRemaining)
                }

                loadKey = with(state) {
                    generationalHint.hint.withCoercedHint { indexInPage, pageIndex, _, _ ->
                        nextLoadKeyOrNull(
                            loadType,
                            generationalHint.generationId,
                            indexInPage,
                            pageIndex
                        )
                    }
                }
            }
        }

        stateLock.withLock { updateLoadState(loadType, Idle) }
    }

    /**
     * The next load key for a [loadType] or `null` if we should stop loading in that direction.
     */
    private fun PagerState<Key, Value>.nextLoadKeyOrNull(
        loadType: LoadType,
        generationId: Int,
        indexInPage: Int,
        pageIndex: Int
    ): Key? = when (loadType) {
        START -> nextPrependKey(generationId, pageIndex, indexInPage, config.prefetchDistance)
        END -> nextAppendKey(generationId, pageIndex, indexInPage, config.prefetchDistance)
        REFRESH -> throw IllegalArgumentException("Just use initialKey directly")
    }
}

/**
 * Generation of cancel token not [Pager]. [generationId] is used to differentiate between loads
 * from jobs that have been cancelled, but continued to run to completion.
 */
private data class GenerationalViewportHint(val generationId: Int, val hint: ViewportHint)
