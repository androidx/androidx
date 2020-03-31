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

import androidx.paging.LoadState.Done
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Idle
import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue

/**
 * Holds a generation of pageable data, a snapshot of data loaded by [PagingSource]. An instance
 * of [Pager] and its corresponding [PagerState] should be launched within a scope that is
 * cancelled when [PagingSource.invalidate] is called.
 */
internal class Pager<Key : Any, Value : Any>(
    internal val initialKey: Key?,
    internal val pagingSource: PagingSource<Key, Value>,
    private val config: PagingConfig,
    private val remoteMediatorAccessor: RemoteMediatorAccessor<Key, Value>? = null,
    private val retryFlow: Flow<Unit>,
    private val invalidate: () -> Unit = {}
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val hintChannel = BroadcastChannel<ViewportHint>(CONFLATED)
    private var lastHint: ViewportHint? = null

    private val pageEventChCollected = AtomicBoolean(false)
    private val pageEventCh = Channel<PageEvent<Value>>(BUFFERED)
    private val stateLock = Mutex()
    private val state = PagerState<Key, Value>(config.pageSize, config.maxSize)

    private val pageEventChannelFlowJob = Job()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pageEventFlow: Flow<PageEvent<Value>> = cancelableChannelFlow(pageEventChannelFlowJob) {
        check(pageEventChCollected.compareAndSet(false, true)) {
            "cannot collect twice from pager"
        }

        // Start collection on pageEventCh, which the rest of this class uses to send PageEvents
        // to this flow.
        launch { pageEventCh.consumeAsFlow().collect { send(it) } }

        // Wrap collection behind a RendezvousChannel to prevent the RetryChannel from buffering
        // retry signals.
        val retryChannel = Channel<Unit>(Channel.RENDEZVOUS)
        launch { retryFlow.collect { retryChannel.offer(it) } }

        // Start collection on retry signals.
        launch {
            retryChannel.consumeAsFlow()
                .collect {
                    // Handle refresh failure. Re-attempt doInitialLoad if the last attempt failed,
                    val refreshFailure = stateLock.withLock {
                        state.failedHintsByLoadType.remove(REFRESH)
                    }
                    refreshFailure?.let {
                        doInitialLoad(this, state)

                        val newRefreshFailure = stateLock.withLock {
                            state.failedHintsByLoadType[REFRESH]
                        }
                        if (newRefreshFailure == null) {
                            startConsumingHints()
                        }
                    }

                    // Handle prepend / append failures.
                    stateLock.withLock {
                        state.failedHintsByLoadType[START]?.also { loadError ->
                            // Reset load state to allow loads in this direction.
                            state.failedHintsByLoadType.remove(START)
                            handleLoadError(loadError)
                        }
                        state.failedHintsByLoadType[END]?.also { loadError ->
                            // Reset load state to allow loads in this direction.
                            state.failedHintsByLoadType.remove(END)
                            handleLoadError(loadError)
                        }
                    }
                }
        }

        // Setup finished, we can now start the initial load.
        doInitialLoad(this, state)

        // Only start collection on ViewportHints if the initial load succeeded.
        if (stateLock.withLock { state.failedHintsByLoadType[REFRESH] } == null) {
            startConsumingHints()
        }
    }

    private suspend fun CoroutineScope.handleLoadError(failure: LoadError<Key, Value>) {
        when (failure) {
            is LoadError.Hint<Key, Value> -> {
                @OptIn(ExperimentalCoroutinesApi::class)
                hintChannel.offer(failure.viewportHint)
            }
            is LoadError.Mediator<Key, Value> -> {
                remoteMediatorAccessor?.load(this, failure.loadType, failure.state)
            }
        }
    }

    fun addHint(hint: ViewportHint) {
        lastHint = hint
        @OptIn(ExperimentalCoroutinesApi::class)
        hintChannel.offer(hint)
    }

    fun close() {
        pageEventChannelFlowJob.cancel()
    }

    suspend fun refreshKeyInfo(): PagingState<Key, Value>? {
        return stateLock.withLock {
            lastHint?.let { lastHint ->
                if (state.pages.isEmpty()) {
                    // Default to initialKey if no pages loaded.
                    null
                } else {
                    state.currentPagingState(lastHint)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun CoroutineScope.startConsumingHints() {
        // Pseudo-tiling via invalidation on jumps.
        if (config.jumpThreshold != COUNT_UNDEFINED) {
            launch {
                hintChannel.asFlow()
                    .collect { hint ->
                        stateLock.withLock {
                            with(state) {
                                hint.withCoercedHint { _, _, hintOffset ->
                                    if (hintOffset.absoluteValue >= config.jumpThreshold) {
                                        invalidate()
                                    }
                                }
                            }
                        }
                    }
            }
        }

        launch {
            state.consumePrependGenerationIdAsFlow()
                .transformLatest { generationId ->
                    // Reset state to Idle and setup a new flow for consuming incoming load hints.
                    // Subsequent generationIds are normally triggered by cancellation.
                    stateLock.withLock {
                        // Skip this generationId of loads if there is no more to load in this
                        // direction. In the case of the terminal page getting dropped, a new
                        // generationId will be sent after load state is updated to Idle.
                        if (state.loadStates[START] == Done) {
                            return@transformLatest
                        } else if (state.failedHintsByLoadType[START] == null) {
                            state.loadStates[START] = Idle
                        }
                    }

                    @OptIn(FlowPreview::class)
                    val generationalHints = hintChannel.asFlow()
                        // Prevent infinite loop when competing prepend / append cancel each other
                        .drop(if (generationId == 0) 0 else 1)
                        .map { hint -> GenerationalViewportHint(generationId, hint) }
                    emitAll(generationalHints)
                }
                .scan(GenerationalViewportHint.PREPEND_INITIAL_VALUE) { acc, it ->
                    if (acc.hint < it.hint) acc else it
                }
                .filter { it != GenerationalViewportHint.PREPEND_INITIAL_VALUE }
                .conflate()
                .collect { generationalHint -> doLoad(this, state, START, generationalHint) }
        }

        launch {
            state.consumeAppendGenerationIdAsFlow()
                .transformLatest<Int, GenerationalViewportHint> { generationId ->
                    // Reset state to Idle and setup a new flow for consuming incoming load hints.
                    // Subsequent generationIds are normally triggered by cancellation.
                    stateLock.withLock {
                        // Skip this generationId of loads if there is no more to load in this
                        // direction. In the case of the terminal page getting dropped, a new
                        // generationId will be sent after load state is updated to Idle.
                        if (state.loadStates[END] == Done) {
                            return@transformLatest
                        } else if (state.failedHintsByLoadType[END] == null) {
                            state.loadStates[END] = Idle
                        }
                    }

                    @OptIn(FlowPreview::class)
                    val generationalHints = hintChannel.asFlow()
                        // Prevent infinite loop when competing prepend / append cancel each other
                        .drop(if (generationId == 0) 0 else 1)
                        .map { hint -> GenerationalViewportHint(generationId, hint) }
                    emitAll(generationalHints)
                }
                .scan(GenerationalViewportHint.APPEND_INITIAL_VALUE) { acc, it ->
                    if (acc.hint > it.hint) acc else it
                }
                .filter { it != GenerationalViewportHint.APPEND_INITIAL_VALUE }
                .conflate()
                .collect { generationalHint ->
                    doLoad(this, state, END, generationalHint)
                }
        }
    }

    private fun loadParams(loadType: LoadType, key: Key?) = LoadParams(
        loadType = loadType,
        key = key,
        loadSize = if (loadType == REFRESH) config.initialLoadSize else config.pageSize,
        placeholdersEnabled = config.enablePlaceholders,
        pageSize = config.pageSize
    )

    private suspend fun doInitialLoad(scope: CoroutineScope, state: PagerState<Key, Value>) {
        stateLock.withLock { state.setLoading(REFRESH) }

        val params = loadParams(REFRESH, initialKey)
        val result = pagingSource.load(params)

        stateLock.withLock {
            when (result) {
                is Page<Key, Value> -> {
                    val insertApplied = state.insert(0, REFRESH, result)

                    // If remoteMediator is set, we allow its result to dictate LoadState, otherwise
                    // we simply rely on the load result.
                    if (remoteMediatorAccessor == null) {
                        // Update loadStates which are sent along with this load's Insert PageEvent.
                        state.loadStates[REFRESH] = Idle
                        if (result.prevKey == null) state.loadStates[START] = Done
                        if (result.nextKey == null) state.loadStates[END] = Done
                    } else {
                        state.loadStates[REFRESH] = Idle
                        if (result.prevKey == null || result.nextKey == null) {
                            val pagingState = state.currentPagingState(lastHint)

                            if (result.prevKey == null) {
                                state.setLoading(START)
                                remoteMediatorAccessor.launchBoundaryCall(scope, START, pagingState)
                            }

                            if (result.nextKey == null) {
                                state.setLoading(END)
                                remoteMediatorAccessor.launchBoundaryCall(scope, END, pagingState)
                            }
                        }
                    }

                    // Send insert event after load state updates, so that Done / Idle is
                    // correctly reflected in the insert event. Note that we only send the event
                    // if the insert was successfully applied in the case of cancellation due to
                    // page dropping.
                    if (insertApplied) {
                        with(state) {
                            pageEventCh.send(result.toPageEvent(REFRESH, config.enablePlaceholders))
                        }
                    }
                }
                is LoadResult.Error -> state.setHintError(
                    REFRESH,
                    Error(result.throwable),
                    ViewportHint.DUMMY_VALUE
                )
            }
        }
    }

    // TODO: Consider making this a transform operation which emits PageEvents
    private suspend fun doLoad(
        scope: CoroutineScope,
        state: PagerState<Key, Value>,
        loadType: LoadType,
        generationalHint: GenerationalViewportHint
    ) {
        require(loadType != REFRESH) { "Use doInitialLoad for LoadType == REFRESH" }

        var loadKey: Key? = stateLock.withLock {
            with(state) {
                generationalHint.hint.withCoercedHint { indexInPage, pageIndex, hintOffset ->
                    nextLoadKeyOrNull(
                        loadType,
                        generationalHint.generationId,
                        indexInPage,
                        pageIndex,
                        hintOffset
                    )?.also { setLoading(loadType) }
                }
            }
        }

        // Keep track of whether the LoadState should be updated to Idle or Done when this load
        // loop terminates due to fulfilling prefetchDistance.
        var updateLoadStateToDone = false
        loop@ while (loadKey != null) {
            val params = loadParams(loadType, loadKey)
            val result: LoadResult<Key, Value> = pagingSource.load(params)
            when (result) {
                is Page<Key, Value> -> {
                    val insertApplied = stateLock.withLock {
                        state.insert(generationalHint.generationId, loadType, result)
                    }

                    // Break if insert was skipped due to cancellation
                    if (!insertApplied) break@loop

                    // Send Done instead of Idle if no more data to load in current direction.
                    if ((loadType == START && result.prevKey == null) ||
                        (loadType == END && result.nextKey == null)
                    ) {
                        updateLoadStateToDone = true
                    }
                }
                is LoadResult.Error -> {
                    stateLock.withLock {
                        state.setHintError(loadType, Error(result.throwable), generationalHint.hint)
                    }
                    return
                }
            }

            stateLock.withLock {
                val dropType = when (loadType) {
                    START -> END
                    else -> START
                }

                state.dropInfo(dropType, generationalHint.hint, config.prefetchDistance)
                    ?.let { info ->
                        state.drop(dropType, info.pageCount, info.placeholdersRemaining)
                        pageEventCh.send(Drop(dropType, info.pageCount, info.placeholdersRemaining))
                    }

                with(state) {
                    loadKey = generationalHint.hint
                        .withCoercedHint { indexInPage, pageIndex, hintOffset ->
                            state.nextLoadKeyOrNull(
                                loadType,
                                generationalHint.generationId,
                                indexInPage,
                                pageIndex,
                                hintOffset
                            )
                        }
                }

                if (remoteMediatorAccessor == null) {
                    // Update load state to success if this is the final load result for this
                    // load hint, and only if we didn't error out.
                    if (loadKey == null && state.failedHintsByLoadType[loadType] == null) {
                        state.loadStates[loadType] = if (updateLoadStateToDone) Done else Idle
                    }
                } else {
                    val pagingState = state.currentPagingState(lastHint)

                    if (params.loadType == START && result.prevKey == null) {
                        state.setLoading(START)
                        remoteMediatorAccessor.launchBoundaryCall(scope, START, pagingState)
                    }

                    if (params.loadType == END && result.nextKey == null) {
                        state.setLoading(END)
                        remoteMediatorAccessor.launchBoundaryCall(scope, END, pagingState)
                    }
                }

                // Send page event for successful insert, now that PagerState has been updated.
                val pageEvent = with(state) {
                    result.toPageEvent(loadType, config.enablePlaceholders)
                }
                pageEventCh.send(pageEvent)
            }
        }
    }

    private fun RemoteMediatorAccessor<Key, Value>.launchBoundaryCall(
        coroutineScope: CoroutineScope,
        loadType: LoadType,
        pagingState: PagingState<Key, Value>
    ) {
        coroutineScope.launch {
            when (val boundaryResult = load(coroutineScope, loadType, pagingState)) {
                is RemoteMediator.MediatorResult.Error -> {
                    this@Pager.state.setBoundaryError(
                        loadType,
                        Error(boundaryResult.throwable),
                        this@Pager.lastHint
                    )
                }
                is RemoteMediator.MediatorResult.Success -> {
                    if (!boundaryResult.hasMoreData) {
                        this@Pager.state.loadStates[loadType] = Done
                    }
                }
            }
        }
    }

    private suspend fun PagerState<Key, Value>.setLoading(loadType: LoadType) {
        if (loadStates[loadType] != Loading) {
            loadStates[loadType] = Loading
            pageEventCh.send(LoadStateUpdate(loadType, Loading))
        }
    }

    private suspend fun PagerState<Key, Value>.setHintError(
        loadType: LoadType,
        loadState: Error,
        hint: ViewportHint
    ) {
        if (loadStates[loadType] !is Error) {
            loadStates[loadType] = loadState
            pageEventCh.send(LoadStateUpdate(loadType, loadState))
        }

        // Save the hint for retry on incoming retry signal, typically sent from user interaction.
        failedHintsByLoadType[loadType] = LoadError.Hint(hint)
    }

    // TODO: We need a map of desired behaviors:
    //   DB idle/done/load, NW * -> reflect NW state
    //   DB error, NW load -> reflect DB error? configurable?
    //   DB error, NW error -> reflect NW error? configurable?
    private suspend fun PagerState<Key, Value>.setBoundaryError(
        loadType: LoadType,
        loadState: Error,
        hint: ViewportHint?
    ) {
        if (loadStates[loadType] !is Error) {
            loadStates[loadType] = loadState
            pageEventCh.send(LoadStateUpdate(loadType, loadState))
        }

        // Save the hint for retry on incoming retry signal, typically sent from user interaction.
        failedHintsByLoadType[loadType] = LoadError.Mediator(loadType, currentPagingState(hint))
    }

    /**
     * The next load key for a [loadType] or `null` if we should stop loading in that direction.
     */
    private fun PagerState<Key, Value>.nextLoadKeyOrNull(
        loadType: LoadType,
        generationId: Int,
        indexInPage: Int,
        pageIndex: Int,
        hintOffset: Int
    ): Key? = when (loadType) {
        START -> nextPrependKey(
            generationId,
            pageIndex,
            indexInPage,
            config.prefetchDistance + hintOffset.absoluteValue
        )
        END -> nextAppendKey(
            generationId,
            pageIndex,
            indexInPage,
            config.prefetchDistance + hintOffset.absoluteValue
        )
        REFRESH -> throw IllegalArgumentException("Just use initialKey directly")
    }

    /**
     * The key to use to load next page to prepend or null if we should stop loading in this
     * direction for the provided prefetchDistance and loadId.
     */
    private fun PagerState<Key, Value>.nextPrependKey(
        loadId: Int,
        pageIndex: Int,
        indexInPage: Int,
        prefetchDistance: Int
    ): Key? {
        if (loadId != prependLoadId) return null
        if (failedHintsByLoadType[START] != null) return null

        val itemsBeforePage = (0 until pageIndex).sumBy { pages[it].data.size }
        val shouldLoad = itemsBeforePage + indexInPage < prefetchDistance
        return if (shouldLoad) pages.first().prevKey else null
    }

    /**
     * The key to use to load next page to append or null if we should stop loading in this
     * direction for the provided prefetchDistance and loadId.
     */
    private fun PagerState<Key, Value>.nextAppendKey(
        loadId: Int,
        pageIndex: Int,
        indexInPage: Int,
        prefetchDistance: Int
    ): Key? {
        if (loadId != appendLoadId) return null
        if (failedHintsByLoadType[END] != null) return null

        val itemsIncludingPage = (pageIndex until pages.size).sumBy { pages[it].data.size }
        val shouldLoad = indexInPage + 1 + prefetchDistance > itemsIncludingPage
        return if (shouldLoad) pages.last().nextKey else null
    }

    private suspend fun PagerState<Key, Value>.currentPagingState(
        lastHint: ViewportHint?
    ): PagingState<Key, Value> {
        val anchorPosition = when {
            state.pages.isEmpty() -> null
            lastHint == null -> null
            else -> lastHint.withCoercedHint { indexInPage, pageIndex, _ ->
                var lastAccessedIndex = indexInPage
                var i = 0
                while (i < pageIndex) {
                    lastAccessedIndex += pages[i].data.size
                    i++
                }

                lastAccessedIndex + state.placeholdersStart
            }
        }

        return PagingState(
            pages = state.pages.toList(),
            anchorPosition = anchorPosition,
            config = config,
            placeholdersStart = state.placeholdersStart
        )
    }
}

/**
 * Generation of cancel token not [Pager]. [generationId] is used to differentiate between loads
 * from jobs that have been cancelled, but continued to run to completion.
 */
private data class GenerationalViewportHint(val generationId: Int, val hint: ViewportHint) {
    companion object {
        val PREPEND_INITIAL_VALUE = GenerationalViewportHint(0, ViewportHint.MAX_VALUE)
        val APPEND_INITIAL_VALUE = GenerationalViewportHint(0, ViewportHint.MIN_VALUE)
    }
}
