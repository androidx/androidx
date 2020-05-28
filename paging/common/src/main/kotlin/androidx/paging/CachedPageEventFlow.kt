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
import androidx.paging.multicast.Multicaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

/**
 * An intermediate flow producer that flattens previous page events and gives any new downstream
 * just those events instead of the full history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CachedPageEventFlow<T : Any>(
    src: Flow<PageEvent<T>>,
    scope: CoroutineScope
) {
    private val pageController = FlattenedPageController<T>()

    /**
     * Shared upstream.
     * Note that, if upstream flow ends, re-subscribing to this will trigger a restart of it but
     * cached data will still be delivered immediately.
     */
    private val multicastedSrc = Multicaster(
        scope = scope,
        bufferSize = 0,
        source = src.withIndex(),
        onEach = pageController::record,
        keepUpstreamAlive = true
    )

    suspend fun close() {
        multicastedSrc.close()
    }

    val downstreamFlow = channelFlow {
        // get a new snapshot. this will immediately hook us to the upstream channel
        val snapshot = pageController.createTemporaryDownstream()
        var lastReceivedHistoryIndex = Int.MIN_VALUE
        // first, dispatch everything in the snapshot
        // these are events that sync us to the current state + other events that might be
        // emitted by the upstream between the time of snapshot and us connecting to the upstream
        val historyCollection = launch {
            snapshot.consumeHistory().collect {
                lastReceivedHistoryIndex = it.index
                send(it.value)
            }
        }
        val activeStreamCollection = launch {
            multicastedSrc.flow.catch { throwable: Throwable ->
                // ignore ClosedSendChannelException, possible race condition
                // watch the following issue to catch a more explicit error
                // https://github.com/dropbox/Store/issues/45
                if (throwable !is ClosedSendChannelException) {
                    throw throwable
                }
            }.onCompletion {
                // if main upstream finishes, make sure to close the history stream
                // otherwise it might stay open forever
                snapshot.close()
            }.collect {
                // we got a value from real stream, close history
                snapshot.close()
                // wait for it to be done so that all events there are dispatched
                historyCollection.join()
                // now if it is new enough, emit it, otherwise, skip it
                if (it.index > lastReceivedHistoryIndex) {
                    send(it.value)
                }
            }
        }
        joinAll(activeStreamCollection, historyCollection)
    }
}

/**
 * This intermediate class is used to ensure new downstream does not miss any events.
 *
 * There is a race condition where, while it is collecting the initial state, there might be more
 * events dispatched by the real source which will be missed because new downstream is not
 * subscribed to the real upstream yet.
 *
 * FlattenedPageController tracks these temporary channels and sends each event into them as well
 * so that they are not lost until the new downstream connects to the real upstream.
 * After that connection is established, this channel is closed.
 *
 * Each value is indexed to avoid sending the same event multiple times.
 */
private class TemporaryDownstream<T : Any> {
    /**
     * List of events that sync us to current state + any other events that might arrive while
     * we are doing the initial sync
     */
    private val historyChannel: Channel<IndexedValue<PageEvent<T>>> = Channel(Channel.UNLIMITED)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consumeHistory() = historyChannel.consumeAsFlow()

    /**
     * Tries to send the value to the history channel.
     * Returns false if send fails (e.g. channel is already closed)
     */
    suspend fun send(event: IndexedValue<PageEvent<T>>): Boolean {
        return try {
            historyChannel.send(event)
            true
        } catch (closed: ClosedSendChannelException) {
            false
        }
    }

    fun close() {
        historyChannel.close()
    }
}

private class FlattenedPageController<T : Any> {
    private val list = FlattenedPageEventStorage<T>()
    private var snapshots = listOf<TemporaryDownstream<T>>()
    private val lock = Mutex()
    /**
     * Record the event.
     * This sends the event into storage but also into any other active TemporaryDownstream.
     */
    suspend fun record(event: IndexedValue<PageEvent<T>>) {
        lock.withLock {
            list.add(event.value)
            snapshots = snapshots.filter {
                it.send(event)
            }
        }
    }

    /**
     * Creates a temporary downstream which will have events that are necessary to sync the
     * current state + any other events that might arrive while that sync is in progress.
     */
    suspend fun createTemporaryDownstream(): TemporaryDownstream<T> {
        return lock.withLock {
            TemporaryDownstream<T>().also { snap ->
                list.getAsEvents().forEachIndexed { index, pageEvent ->
                    snap.send(
                        IndexedValue(
                            index = Int.MIN_VALUE + index,
                            value = pageEvent
                        )
                    )
                }
            }
        }
    }
}

/**
 * Keeps a list of page events and can dispatch them at once as PageEvent instead of multiple
 * events.
 *
 * There is no synchronization in this code so it should be used with locks around if necessary.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal class FlattenedPageEventStorage<T : Any> {
    private var placeholdersBefore: Int = 0
    private var placeholdersAfter: Int = 0
    private val pages = ArrayDeque<TransformablePage<T>>()

    /**
     * Note - this is initialized without remote state, since we don't know if we have remote
     * data once we start getting events. This is fine, since downstream needs to handle this
     * anyway - remote state being added after initial, empty, PagingData.
     */
    private val loadStates = MutableLoadStateCollection(hasRemoteState = false)
    fun add(event: PageEvent<T>) {
        when (event) {
            is PageEvent.Insert<T> -> handleInsert(event)
            is PageEvent.Drop<T> -> handlePageDrop(event)
            is PageEvent.LoadStateUpdate<T> -> handleLoadStateUpdate(event)
        }
    }

    private fun handlePageDrop(event: PageEvent.Drop<T>) {
        // TODO: include state in drop event for simplicity, instead of reconstructing behavior.
        //  This allows upstream to control how drop affects states (e.g. letting drop affect both
        //  remote and local)
        loadStates.set(event.loadType, false, LoadState.NotLoading.Idle)

        when (event.loadType) {
            LoadType.PREPEND -> {
                placeholdersBefore = event.placeholdersRemaining
                repeat(event.count) {
                    pages.removeFirst()
                }
            }
            LoadType.APPEND -> {
                placeholdersAfter = event.placeholdersRemaining
                repeat(event.count) {
                    pages.removeLast()
                }
            }
            else -> throw IllegalArgumentException("Page drop type must be prepend or append")
        }
    }

    private fun handleInsert(event: PageEvent.Insert<T>) {
        loadStates.set(event.combinedLoadStates)
        when (event.loadType) {
            LoadType.REFRESH -> {
                pages.clear()
                placeholdersAfter = event.placeholdersAfter
                placeholdersBefore = event.placeholdersBefore
                pages.addAll(event.pages)
            }
            LoadType.PREPEND -> {
                placeholdersBefore = event.placeholdersBefore
                (event.pages.size - 1 downTo 0).forEach {
                    pages.addFirst(event.pages[it])
                }
            }
            LoadType.APPEND -> {
                placeholdersAfter = event.placeholdersAfter
                pages.addAll(event.pages)
            }
        }
    }

    private fun handleLoadStateUpdate(event: PageEvent.LoadStateUpdate<T>) {
        loadStates.set(event.loadType, event.fromMediator, event.loadState)
    }

    fun getAsEvents(): List<PageEvent<T>> {
        val events = mutableListOf<PageEvent<T>>()
        if (pages.isNotEmpty()) {
            events.add(
                PageEvent.Insert.Refresh(
                    pages = pages.toList(),
                    placeholdersBefore = placeholdersBefore,
                    placeholdersAfter = placeholdersAfter,
                    combinedLoadStates = loadStates.snapshot()
                )
            )
        } else {
            loadStates.forEach { type, fromMediator, state ->
                // Should be mostly safe to ignore NotLoading states since they don't need to be
                // communicated... but it's unclear if this is true when endOfPagination = true
                if (state is LoadState.Loading || state is LoadState.Error) {
                    events.add(PageEvent.LoadStateUpdate(type, fromMediator, state))
                }
            }
        }

        return events
    }
}