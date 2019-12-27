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
import androidx.paging.PagedSource.LoadParams
import androidx.paging.PagedSource.LoadResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds a generation of pageable data, a snapshot of data loaded by [PagedSource]. An instance
 * of [Pager] and its corresponding [PagerState] should be launched within a scope that is
 * cancelled when [PagedSource.invalidate] is called.
 */
@FlowPreview
@ExperimentalCoroutinesApi
internal class Pager<Key : Any, Value : Any>(
    internal val initialKey: Key?,
    internal val pagedSource: PagedSource<Key, Value>,
    private val config: PagedList.Config
) {
    private val hintChannel = BroadcastChannel<ViewportHint>(BUFFERED)
    private var lastHint: ViewportHint? = null

    private val pageEventCh = Channel<PageEvent<Value>>(BUFFERED)
    private val stateLock = Mutex()
    private val state = PagerState<Key, Value>(config.pageSize, config.maxSize)

    fun addHint(hint: ViewportHint) {
        lastHint = hint
        hintChannel.offer(hint)
    }
    private val created = AtomicBoolean(false)
    fun create(): Flow<PageEvent<Value>> = channelFlow {
        check(created.compareAndSet(false, true)) {
            "cannot collect twice from pager"
        }
        launch { pageEventCh.consumeAsFlow().collect { send(it) } }
        state.doInitialLoad()

        val prependJob = launch {
            state.consumePrependGenerationIdAsFlow()
                .transformLatest<Int, GenerationalViewportHint> { generationId ->
                    // Reset state to Idle and setup a new flow for consuming incoming load hints.
                    // Subsequent generationIds are normally triggered by cancellation.
                    stateLock.withLock {
                        // Skip this generationId of loads if there is no more to load in this
                        // direction. In the case of the terminal page getting dropped, a new
                        // generationId will be sent after load state is updated to Idle.
                        if (state.loadStates[START] == Done) return@transformLatest
                        else state.updateLoadState(START, Idle)
                    }

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
                    stateLock.withLock {
                        // Skip this generationId of loads if there is no more to load in this
                        // direction. In the case of the terminal page getting dropped, a new
                        // generationId will be sent after load state is updated to Idle.
                        if (state.loadStates[END] == Done) return@transformLatest
                        else state.updateLoadState(END, Idle)
                    }

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
                    hint.withCoercedHint { indexInPage, pageIndex, _ ->
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
                    val insertApplied = insert(0, REFRESH, result)

                    updateLoadState(REFRESH, Done)
                    if (result.prevKey == null) updateLoadState(START, Done)
                    if (result.nextKey == null) updateLoadState(END, Done)

                    // Send insert event after load state updates, so that Done / Idle is
                    // correctly reflected in the insert event. Note that we only send the event
                    // if the insert was successfully applied in the case of cancellation due to
                    // page dropping.
                    if (insertApplied) pageEventCh.send(result.toPageEvent(REFRESH))
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
                generationalHint.hint.withCoercedHint { indexInPage, pageIndex, hintOffset ->
                    nextLoadKeyOrNull(
                        loadType,
                        generationalHint.generationId,
                        indexInPage,
                        pageIndex,
                        hintOffset
                    )?.also {
                        updateLoadState(loadType, Loading)
                    }
                }
            }
        }

        // Keeping track of the previous Insert event separately allows for LoadState merging.
        var lastInsertedPage: LoadResult.Page<Key, Value>? = null
        // Keep track of whether the LoadState should be updated to Idle or Done when this load
        // loop terminates due to fulfilling prefetchDistance.
        var updateLoadStateToDone = false
        loop@ while (loadKey != null) {
            // Send all pageEvents except the last one for this loop, since the last
            // insert event can only be sent after LoadState has been updated.
            lastInsertedPage?.let { previousResult ->
                val pageEvent = stateLock.withLock { previousResult.toPageEvent(loadType) }
                pageEventCh.send(pageEvent)
            }
            lastInsertedPage = null // Reset lastInsertedPage in case of error or cancellation.

            when (val result: LoadResult<Key, Value> = pagedSource.load(loadType, loadKey)) {
                is LoadResult.Page<Key, Value> -> {
                    val insertApplied = stateLock.withLock {
                        insert(generationalHint.generationId, loadType, result)
                    }

                    // Break if insert was skipped due to cancellation
                    if (!insertApplied) break@loop

                    // Send Done instead of Idle if no more data to load in current direction.
                    if (loadType == START && result.prevKey == null) updateLoadStateToDone = true
                    else if (loadType == END && result.nextKey == null) updateLoadStateToDone = true

                    // Set the Page to be sent to pageEventCh.
                    lastInsertedPage = result
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
                    state.updateLoadState(dropType, Idle)
                    state.drop(dropType, info.pageCount, info.placeholdersRemaining)
                    pageEventCh.send(Drop(dropType, info.pageCount, info.placeholdersRemaining))
                }

                loadKey =
                    generationalHint.hint.withCoercedHint { indexInPage, pageIndex, hintOffset ->
                        nextLoadKeyOrNull(
                            loadType,
                            generationalHint.generationId,
                            indexInPage,
                            pageIndex,
                            hintOffset
                        )
                    }
            }
        }

        stateLock.withLock {
            updateLoadState(loadType, if (updateLoadStateToDone) Done else Idle)

            // Send the last page event from previous successful insert, now that LoadState has
            // been updated.
            lastInsertedPage?.let { previousResult ->
                val pageEvent = previousResult.toPageEvent(loadType)
                pageEventCh.send(pageEvent)
            }
        }
    }

    private suspend fun PagerState<Key, Value>.updateLoadState(
        loadType: LoadType,
        loadState: LoadState
    ) {
        // De-dupe state updates if state hasn't changed.
        if (loadStates[loadType] != loadState) {
            loadStates[loadType] = loadState
            pageEventCh.send(PageEvent.StateUpdate(loadType, loadState))
        }
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
            config.prefetchDistance + hintOffset
        )
        END -> nextAppendKey(
            generationId,
            pageIndex,
            indexInPage,
            config.prefetchDistance + hintOffset
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

        val itemsIncludingPage = (pageIndex until pages.size).sumBy { pages[it].data.size }
        val shouldLoad = indexInPage + 1 + prefetchDistance > itemsIncludingPage
        return if (shouldLoad) pages.last().nextKey else null
    }
}

/**
 * Generation of cancel token not [Pager]. [generationId] is used to differentiate between loads
 * from jobs that have been cancelled, but continued to run to completion.
 */
private data class GenerationalViewportHint(val generationId: Int, val hint: ViewportHint)
