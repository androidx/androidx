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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An intermediate flow producer that flattens previous page events and gives any new downstream
 * just those events instead of the full history.
 */
internal class CachedPageEventFlow<T : Any>(
    src: Flow<PageEvent<T>>,
    scope: CoroutineScope
) {
    private val pageController = FlattenedPageController<T>()

    /**
     * Shared flow for downstreams where we dispatch each event coming from upstream.
     * This only has reply = 1 so it does not keep the previous events. Meanwhile, it still buffers
     * them for active subscribers.
     * A final `null` value is emitted as the end of stream message once the job is complete.
     */
    private val mutableSharedSrc = MutableSharedFlow<IndexedValue<PageEvent<T>>?>(
        replay = 1,
        extraBufferCapacity = Channel.UNLIMITED,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    /**
     * Shared flow used for downstream which also sends the history. Each downstream connects to
     * this where it first receives a history event and then any other event that was emitted by
     * the upstream.
     */
    private val sharedForDownstream = mutableSharedSrc.onSubscription {
        val history = pageController.getStateAsEvents()
        // start the job if it has not started yet. We do this after capturing the history so that
        // the first subscriber does not receive any history.
        job.start()
        history.forEach {
            emit(it)
        }
    }

    /**
     * The actual job that collects the upstream.
     */
    private val job = scope.launch(start = CoroutineStart.LAZY) {
        src.withIndex()
            .collect {
                mutableSharedSrc.emit(it)
                pageController.record(it)
            }
    }.also {
        it.invokeOnCompletion {
            // Emit a final `null` message to the mutable shared flow.
            // Even though, this tryEmit might technically fail, it shouldn't because we have
            // unlimited buffer in the shared flow.
            mutableSharedSrc.tryEmit(null)
        }
    }

    fun close() {
        job.cancel()
    }

    val downstreamFlow = flow {
        // track max event index we've seen to avoid race condition between history and the shared
        // stream
        var maxEventIndex = Int.MIN_VALUE
        sharedForDownstream
            .takeWhile {
                // shared flow cannot finish hence we have a special marker to finish it
                it != null
            }
            .collect { indexedValue ->
                // we take until null so this cannot be null
                if (indexedValue!!.index > maxEventIndex) {
                    emit(indexedValue.value)
                    maxEventIndex = indexedValue.index
                }
            }
    }

    /**
     * Returns cached data as PageEvent.Insert. Null if cached data is empty (for example on
     * initial refresh).
     */
    internal fun getCachedEvent(): PageEvent.Insert<T>? = pageController.getCachedEvent()
}

private class FlattenedPageController<T : Any> {
    private val list = FlattenedPageEventStorage<T>()
    private val lock = Mutex()
    private var maxEventIndex = -1

    /**
     * Record the event.
     */
    suspend fun record(event: IndexedValue<PageEvent<T>>) {
        lock.withLock {
            maxEventIndex = event.index
            list.add(event.value)
        }
    }

    /**
     * Create a list of events that represents the current state of the list.
     */
    suspend fun getStateAsEvents(): List<IndexedValue<PageEvent<T>>> {
        return lock.withLock {
            // condensed events to bring downstream up to the current state
            val catchupEvents = list.getAsEvents()
            val startEventIndex = maxEventIndex - catchupEvents.size + 1
            catchupEvents.mapIndexed { index, pageEvent ->
                IndexedValue(
                    index = startEventIndex + index,
                    value = pageEvent
                )
            }
        }
    }

    fun getCachedEvent(): PageEvent.Insert<T>? = list.getAsEvents().firstOrNull()?.let {
        if (it is PageEvent.Insert && it.loadType == LoadType.REFRESH) it else null
    }
}

/**
 * Keeps a list of page events and can dispatch them at once as PageEvent instead of multiple
 * events.
 *
 * There is no synchronization in this code so it should be used with locks around if necessary.
 */
@VisibleForTesting
internal class FlattenedPageEventStorage<T : Any> {
    private var placeholdersBefore: Int = 0
    private var placeholdersAfter: Int = 0
    private val pages = ArrayDeque<TransformablePage<T>>()

    /**
     * Note - this is initialized without remote state, since we don't know if we have remote
     * data once we start getting events. This is fine, since downstream needs to handle this
     * anyway - remote state being added after initial, empty, PagingData.
     */
    private val sourceStates = MutableLoadStateCollection()
    private var mediatorStates: LoadStates? = null

    /**
     * Tracks if we ever received an event from upstream to prevent sending the initial IDLE state
     * to new downstream subscribers.
     */
    private var receivedFirstEvent: Boolean = false
    fun add(event: PageEvent<T>) {
        receivedFirstEvent = true
        when (event) {
            is PageEvent.Insert<T> -> handleInsert(event)
            is PageEvent.Drop<T> -> handlePageDrop(event)
            is PageEvent.LoadStateUpdate<T> -> handleLoadStateUpdate(event)
            is PageEvent.StaticList -> handleStaticList(event)
        }
    }

    private fun handlePageDrop(event: PageEvent.Drop<T>) {
        // TODO: include state in drop event for simplicity, instead of reconstructing behavior.
        //  This allows upstream to control how drop affects states (e.g. letting drop affect both
        //  remote and local)
        sourceStates.set(event.loadType, LoadState.NotLoading.Incomplete)

        when (event.loadType) {
            LoadType.PREPEND -> {
                placeholdersBefore = event.placeholdersRemaining
                repeat(event.pageCount) { pages.removeFirst() }
            }
            LoadType.APPEND -> {
                placeholdersAfter = event.placeholdersRemaining
                repeat(event.pageCount) { pages.removeLast() }
            }
            else -> throw IllegalArgumentException("Page drop type must be prepend or append")
        }
    }

    private fun handleInsert(event: PageEvent.Insert<T>) {
        sourceStates.set(event.sourceLoadStates)
        mediatorStates = event.mediatorLoadStates

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
        sourceStates.set(event.source)
        mediatorStates = event.mediator
    }

    private fun handleStaticList(event: PageEvent.StaticList<T>) {
        if (event.sourceLoadStates != null) {
            sourceStates.set(event.sourceLoadStates)
        }

        if (event.mediatorLoadStates != null) {
            mediatorStates = event.mediatorLoadStates
        }

        pages.clear()
        placeholdersAfter = 0
        placeholdersBefore = 0
        pages.add(TransformablePage(originalPageOffset = 0, data = event.data))
    }

    fun getAsEvents(): List<PageEvent<T>> {
        if (!receivedFirstEvent) {
            return emptyList()
        }
        val events = mutableListOf<PageEvent<T>>()
        val source = sourceStates.snapshot()
        if (pages.isNotEmpty()) {
            events.add(
                PageEvent.Insert.Refresh(
                    pages = pages.toList(),
                    placeholdersBefore = placeholdersBefore,
                    placeholdersAfter = placeholdersAfter,
                    sourceLoadStates = source,
                    mediatorLoadStates = mediatorStates
                )
            )
        } else {
            events.add(
                PageEvent.LoadStateUpdate(
                    source = source,
                    mediator = mediatorStates
                )
            )
        }

        return events
    }
}
