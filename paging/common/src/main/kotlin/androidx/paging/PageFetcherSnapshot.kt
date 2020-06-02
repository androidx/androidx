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
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
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
 * of [PageFetcherSnapshot] and its corresponding [PagerState] should be launched within a scope
 * that is cancelled when [PagingSource.invalidate] is called.
 */
internal class PageFetcherSnapshot<Key : Any, Value : Any>(
    internal val initialKey: Key?,
    internal val pagingSource: PagingSource<Key, Value>,
    private val config: PagingConfig,
    private val retryFlow: Flow<Unit>,
    private val triggerRemoteRefresh: Boolean = false,
    private val remoteMediatorAccessor: RemoteMediatorAccessor<Key, Value>? = null,
    private val invalidate: () -> Unit = {}
) {
    init {
        require(config.jumpThreshold == COUNT_UNDEFINED || pagingSource.jumpingSupported) {
            "PagingConfig.jumpThreshold was set, but the associated PagingSource has not marked " +
                    "support for jumps by overriding PagingSource.jumpingSupported to true."
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val hintChannel = BroadcastChannel<ViewportHint>(CONFLATED)
    private var lastHint: ViewportHint? = null

    private val pageEventChCollected = AtomicBoolean(false)
    private val pageEventCh = Channel<PageEvent<Value>>(BUFFERED)
    private val stateLock = Mutex()
    private val state = PagerState<Key, Value>(
        pageSize = config.pageSize,
        maxSize = config.maxSize,
        hasRemoteState = remoteMediatorAccessor != null
    )

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
                    // Handle refresh failure.
                    stateLock.withLock { state.failedHintsByLoadType.remove(REFRESH) }
                        ?.let { refreshFailure ->
                            retryLoadError(refreshFailure)

                            val newRefreshFailure = stateLock.withLock {
                                state.failedHintsByLoadType[REFRESH]
                            }
                            if (newRefreshFailure == null) {
                                startConsumingHints()
                            }
                        }

                    // Handle prepend / append failures.

                    // Reset load state to allow loads in this direction.
                    stateLock.withLock { state.failedHintsByLoadType.remove(PREPEND) }
                        ?.also { loadError -> retryLoadError(loadError) }

                    // Reset load state to allow loads in this direction.
                    stateLock.withLock { state.failedHintsByLoadType.remove(APPEND) }
                        ?.also { loadError -> retryLoadError(loadError) }
                }
        }

        // Trigger RemoteMediator initialization blockingly.
        if (triggerRemoteRefresh) {
            remoteMediatorAccessor?.run {
                val pagingState = stateLock.withLock { state.currentPagingState(null) }
                doBoundaryCall(this@cancelableChannelFlow, REFRESH, pagingState)
            }
        }

        // Setup finished, we can start the initial load if RemoteMediator didn't throw an error.
        if (stateLock.withLock { state.failedHintsByLoadType[REFRESH] } == null) {
            doInitialLoad(this, state)
        }

        // Only start collection on ViewportHints if the initial load succeeded.
        if (stateLock.withLock { state.failedHintsByLoadType[REFRESH] } == null) {
            startConsumingHints()
        }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun CoroutineScope.retryLoadError(failure: LoadError<Key, Value>) {
        when {
            failure is LoadError.Hint<Key, Value> && failure.loadType == REFRESH -> {
                doInitialLoad(this, state)
            }
            failure is LoadError.Hint<Key, Value> -> {
                @OptIn(ExperimentalCoroutinesApi::class)
                hintChannel.offer(failure.viewportHint)
            }
            failure is LoadError.Mediator<Key, Value> -> {
                remoteMediatorAccessor?.run {
                    val pagingState = stateLock.withLock { state.currentPagingState(lastHint) }
                    launch {
                        doBoundaryCall(this@retryLoadError, failure.loadType, pagingState)
                    }
                }
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
                        if (state.loadStates.get(PREPEND, false) == NotLoading.Done) {
                            return@transformLatest
                        } else if (state.failedHintsByLoadType[PREPEND] == null) {
                            state.loadStates.set(PREPEND, false, NotLoading.Idle)
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
                .collect { generationalHint -> doLoad(this, state, PREPEND, generationalHint) }
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
                        if (state.loadStates.get(APPEND, false) == NotLoading.Done) {
                            return@transformLatest
                        } else if (state.failedHintsByLoadType[APPEND] == null) {
                            state.loadStates.set(APPEND, false, NotLoading.Idle)
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
                    doLoad(this, state, APPEND, generationalHint)
                }
        }
    }

    private fun loadParams(loadType: LoadType, key: Key?) = LoadParams.create(
        loadType = loadType,
        key = key,
        loadSize = if (loadType == REFRESH) config.initialLoadSize else config.pageSize,
        placeholdersEnabled = config.enablePlaceholders,
        pageSize = config.pageSize
    )

    private suspend fun doInitialLoad(scope: CoroutineScope, state: PagerState<Key, Value>) {
        stateLock.withLock { state.setLoading(REFRESH, false) }

        val params = loadParams(REFRESH, initialKey)
        when (val result = pagingSource.load(params)) {
            is Page<Key, Value> -> {
                val insertApplied = stateLock.withLock { state.insert(0, REFRESH, result) }

                // Update loadStates which are sent along with this load's Insert PageEvent.
                stateLock.withLock {
                    state.loadStates.set(REFRESH, false, NotLoading.Idle)
                    if (result.prevKey == null) {
                        state.loadStates.set(
                            PREPEND, false, NotLoading.instance(
                                endOfPaginationReached = remoteMediatorAccessor == null,
                                fromMediator = false
                            )
                        )
                    }
                    if (result.nextKey == null) {
                        state.loadStates.set(
                            APPEND, false, NotLoading.instance(
                                endOfPaginationReached = remoteMediatorAccessor == null,
                                fromMediator = false
                            )
                        )
                    }
                }

                // Send insert event after load state updates, so that endOfPaginationReached is
                // correctly reflected in the insert event. Note that we only send the event if the
                // insert was successfully applied in the case of cancellation due to page dropping.
                if (insertApplied) {
                    stateLock.withLock {
                        with(state) {
                            pageEventCh.send(result.toPageEvent(REFRESH, config.enablePlaceholders))
                        }
                    }
                }

                // Launch any RemoteMediator boundary calls after applying initial insert.
                if (remoteMediatorAccessor != null) {
                    if (result.prevKey == null || result.nextKey == null) {
                        val pagingState = stateLock.withLock { state.currentPagingState(lastHint) }

                        if (result.prevKey == null) {
                            remoteMediatorAccessor.doBoundaryCall(scope, PREPEND, pagingState)
                        }

                        if (result.nextKey == null) {
                            remoteMediatorAccessor.doBoundaryCall(scope, APPEND, pagingState)
                        }
                    }
                }
            }
            is LoadResult.Error -> stateLock.withLock {
                state.setHintError(
                    loadType = REFRESH,
                    fromMediator = false,
                    loadState = Error(result.throwable),
                    hint = ViewportHint.DUMMY_VALUE
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
                    )?.also { setLoading(loadType, false) }
                }
            }
        }

        // Keep track of whether endOfPaginationReached so we can update LoadState accordingly when
        // this load loop terminates due to fulfilling prefetchDistance.
        var endOfPaginationReached = false
        loop@ while (loadKey != null) {
            val params = loadParams(loadType, loadKey)
            val result: LoadResult<Key, Value> = pagingSource.load(params)
            when (result) {
                is Page<Key, Value> -> {
                    // First, check for common error case where the same key is re-used to load
                    // new pages, often resulting in infinite loops.
                    val nextKey = when (loadType) {
                        PREPEND -> result.prevKey
                        APPEND -> result.nextKey
                        else -> throw IllegalArgumentException(
                            "Use doInitialLoad for LoadType == REFRESH"
                        )
                    }

                    check(
                        pagingSource.keyReuseSupported || nextKey != loadKey
                    ) {
                        val keyFieldName = if (loadType == PREPEND) "prevKey" else "nextKey"
                        """The same value, $loadKey, was passed as the $keyFieldName in two
                            | sequential Pages loaded from a PagingSource. Re-using load keys in
                            | PagingSource is often an error, and must be explicitly enabled by
                            | overriding PagingSource.keyReuseSupported.
                            """.trimMargin()
                    }

                    val insertApplied = stateLock.withLock {
                        state.insert(generationalHint.generationId, loadType, result)
                    }

                    // Break if insert was skipped due to cancellation
                    if (!insertApplied) break@loop

                    // Set endOfPaginationReached to false if no more data to load in current
                    // direction.
                    if ((loadType == PREPEND && result.prevKey == null) ||
                        (loadType == APPEND && result.nextKey == null)
                    ) {
                        endOfPaginationReached = true
                    }
                }
                is LoadResult.Error -> {
                    stateLock.withLock {
                        state.setHintError(
                            loadType = loadType,
                            fromMediator = false,
                            loadState = Error(result.throwable),
                            hint = generationalHint.hint
                        )
                    }
                    return
                }
            }

            val dropType = when (loadType) {
                PREPEND -> APPEND
                else -> PREPEND
            }

            stateLock.withLock {
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

                // Update load state to success if this is the final load result for this
                // load hint, and only if we didn't error out.
                if (loadKey == null && state.failedHintsByLoadType[loadType] == null) {
                    state.loadStates.set(
                        type = loadType,
                        remote = false,
                        state = NotLoading.instance(endOfPaginationReached, false)
                    )
                }

                // Send page event for successful insert, now that PagerState has been updated.
                val pageEvent = with(state) {
                    result.toPageEvent(loadType, config.enablePlaceholders)
                }

                pageEventCh.send(pageEvent)
            }

            val endsPrepend = params is LoadParams.Prepend && result.prevKey == null
            val endsAppend = params is LoadParams.Append && result.nextKey == null
            if (remoteMediatorAccessor != null && (endsPrepend || endsAppend)) {
                val pagingState = stateLock.withLock { state.currentPagingState(lastHint) }

                if (endsPrepend) {
                    remoteMediatorAccessor.doBoundaryCall(scope, PREPEND, pagingState)
                }

                if (endsAppend) {
                    remoteMediatorAccessor.doBoundaryCall(scope, APPEND, pagingState)
                }
            }
        }
    }

    private suspend fun RemoteMediatorAccessor<Key, Value>.doBoundaryCall(
        coroutineScope: CoroutineScope,
        loadType: LoadType,
        pagingState: PagingState<Key, Value>
    ) {
        stateLock.withLock { state.setLoading(loadType, true) }
        @OptIn(ExperimentalPagingApi::class)
        when (val mediatorResult = load(coroutineScope, loadType, pagingState)) {
            is RemoteMediator.MediatorResult.Error -> {
                stateLock.withLock {
                    this@PageFetcherSnapshot.state.setMediatorError(
                        loadType,
                        Error(mediatorResult.throwable)
                    )
                }
            }
            is RemoteMediator.MediatorResult.Success -> {
                if (loadType != REFRESH) {
                    stateLock.withLock {
                        this@PageFetcherSnapshot.state.loadStates.set(
                            loadType, true, NotLoading.instance(
                                endOfPaginationReached = mediatorResult.endOfPaginationReached,
                                fromMediator = true
                            )
                        )

                        val page = Page<Key, Value>(listOf(), null, null)
                        var loadId = when (loadType) {
                            REFRESH -> 0
                            PREPEND -> state.prependLoadId
                            APPEND -> state.appendLoadId
                        }

                        // Keep trying to insert with latest loadId until we succeed.
                        while (!state.insert(loadId, loadType, page)) {
                            loadId = when (loadType) {
                                REFRESH -> 0
                                PREPEND -> state.prependLoadId
                                APPEND -> state.appendLoadId
                            }
                        }

                        // Push an empty insert event to update LoadState.
                        val pageEvent = with(state) {
                            page.toPageEvent(loadType, config.enablePlaceholders)
                        }

                        pageEventCh.send(pageEvent)
                    }
                }
            }
        }
    }

    private suspend fun PagerState<Key, Value>.setLoading(
        loadType: LoadType,
        fromMediator: Boolean
    ) {
        if (loadStates.set(loadType, fromMediator, Loading)) {
            pageEventCh.send(
                LoadStateUpdate(loadType, fromMediator, Loading)
            )
        }
    }

    private suspend fun PagerState<Key, Value>.setHintError(
        loadType: LoadType,
        fromMediator: Boolean,
        loadState: Error,
        hint: ViewportHint
    ) {
        if (loadStates.set(loadType, fromMediator, loadState)) {
            pageEventCh.send(LoadStateUpdate(loadType, fromMediator, loadState))
        }

        // Save the hint for retry on incoming retry signal, typically sent from user interaction.
        failedHintsByLoadType[loadType] = LoadError.Hint(loadType, hint)
    }

    private suspend fun PagerState<Key, Value>.setMediatorError(
        loadType: LoadType,
        loadState: Error
    ) {
        if (loadStates.set(loadType, true, loadState)) {
            pageEventCh.send(LoadStateUpdate(loadType, true, loadState))
        }

        // Save the hint for retry on incoming retry signal, typically sent from user interaction.
        failedHintsByLoadType[loadType] = LoadError.Mediator(loadType)
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
        PREPEND -> nextPrependKey(
            generationId,
            pageIndex,
            indexInPage,
            config.prefetchDistance + hintOffset.absoluteValue
        )
        APPEND -> nextAppendKey(
            generationId,
            pageIndex,
            indexInPage,
            config.prefetchDistance + hintOffset.absoluteValue
        )
        REFRESH -> throw IllegalArgumentException("Just use initialKey directly")
    }

    /**
     * The key to use to load next page to prepend or null if we should stop loading in this
     * direction for the provided [prefetchDistance] and [loadId].
     */
    private fun PagerState<Key, Value>.nextPrependKey(
        loadId: Int,
        pageIndex: Int,
        indexInPage: Int,
        prefetchDistance: Int
    ): Key? {
        if (loadId != prependLoadId) return null
        if (failedHintsByLoadType[PREPEND] != null) return null

        val itemsBeforePage = (0 until pageIndex).sumBy { pages[it].data.size }
        val shouldLoad = itemsBeforePage + indexInPage < prefetchDistance
        return if (shouldLoad) pages.first().prevKey else null
    }

    /**
     * The key to use to load next page to append or null if we should stop loading in this
     * direction for the provided [prefetchDistance] and [loadId].
     */
    private fun PagerState<Key, Value>.nextAppendKey(
        loadId: Int,
        pageIndex: Int,
        indexInPage: Int,
        prefetchDistance: Int
    ): Key? {
        if (loadId != appendLoadId) return null
        if (failedHintsByLoadType[APPEND] != null) return null

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

                lastAccessedIndex + state.placeholdersBefore
            }
        }

        return PagingState(
            pages = state.pages.toList(),
            anchorPosition = anchorPosition,
            config = config,
            placeholdersBefore = state.placeholdersBefore
        )
    }
}

/**
 * Generation of cancel token not [PageFetcherSnapshot]. [generationId] is used to differentiate
 * between loads from jobs that have been cancelled, but continued to run to completion.
 */
private data class GenerationalViewportHint(val generationId: Int, val hint: ViewportHint) {
    companion object {
        val PREPEND_INITIAL_VALUE = GenerationalViewportHint(0, ViewportHint.MAX_VALUE)
        val APPEND_INITIAL_VALUE = GenerationalViewportHint(0, ViewportHint.MIN_VALUE)
    }
}
