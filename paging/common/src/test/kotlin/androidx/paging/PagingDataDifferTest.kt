/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.PREPEND
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@RunWith(JUnit4::class)
class PagingDataDifferTest {
    private val testScope = TestCoroutineScope()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(
        testScope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    )

    @Test
    fun collectFrom_static() = testScope.runBlockingTest {
        pauseDispatcher {
            val differ = SimpleDiffer(dummyDifferCallback)
            val receiver = UiReceiverFake()

            val job1 = launch {
                differ.collectFrom(infinitelySuspendingPagingData(receiver))
            }
            advanceUntilIdle()
            job1.cancel()

            val job2 = launch {
                differ.collectFrom(PagingData.empty())
            }
            advanceUntilIdle()
            job2.cancel()

            // Static replacement should also replace the UiReceiver from previous generation.
            differ.retry()
            differ.refresh()
            advanceUntilIdle()

            assertFalse { receiver.retryEvents.isNotEmpty() }
            assertFalse { receiver.refreshEvents.isNotEmpty() }
        }
    }

    @Test
    fun collectFrom_twice() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        launch { differ.collectFrom(infinitelySuspendingPagingData()) }
            .cancel()
        launch { differ.collectFrom(infinitelySuspendingPagingData()) }
            .cancel()
    }

    @Test
    fun collectFrom_twiceConcurrently() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        val job1 = launch {
            differ.collectFrom(infinitelySuspendingPagingData())
        }

        // Ensure job1 is running.
        assertTrue { job1.isActive }

        val job2 = launch {
            differ.collectFrom(infinitelySuspendingPagingData())
        }

        // job2 collection should complete job1 but not cancel.
        assertFalse { job1.isCancelled }
        assertTrue { job1.isCompleted }
        job2.cancel()
    }

    @Test
    fun retry() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val receiver = UiReceiverFake()

        val job = launch {
            differ.collectFrom(infinitelySuspendingPagingData(receiver))
        }

        differ.retry()

        assertEquals(1, receiver.retryEvents.size)

        job.cancel()
    }

    @Test
    fun refresh() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val receiver = UiReceiverFake()

        val job = launch {
            differ.collectFrom(infinitelySuspendingPagingData(receiver))
        }

        differ.refresh()

        assertEquals(1, receiver.refreshEvents.size)

        job.cancel()
    }

    @Test
    fun fetch_loadHintResentWhenUnfulfilled() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        pageEventCh.trySend(
            Refresh(
                pages = listOf(TransformablePage(0, listOf(0, 1))),
                placeholdersBefore = 4,
                placeholdersAfter = 4,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-1, listOf(-1, -2))),
                placeholdersBefore = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        pageEventCh.trySend(
            Append(
                pages = listOf(TransformablePage(1, listOf(2, 3))),
                placeholdersAfter = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )

        val receiver = UiReceiverFake()
        val job = launch {
            differ.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), receiver).filter { it % 2 != 0 }
            )
        }

        // Initial state:
        // [null, null, [-1], [1], [3], null, null]
        assertNull(differ[0])
        assertThat(receiver.hints).isEqualTo(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                ),
                ViewportHint.Access(
                    pageOffset = -1,
                    indexInPage = -2,
                    presentedItemsBefore = -2,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 1
                ),
            )
        )

        // Insert a new page, PagingDataDiffer should try to resend hint since index 0 still points
        // to a placeholder:
        // [null, null, [], [-1], [1], [3], null, null]
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        assertThat(receiver.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = -2,
                    indexInPage = -2,
                    presentedItemsBefore = -2,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = -2,
                    originalPageOffsetLast = 1
                )
            )
        )

        // Now index 0 has been loaded:
        // [[-3], [], [-1], [1], [3], null, null]
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-3, listOf(-3, -4))),
                placeholdersBefore = 0,
                combinedLoadStates = localLoadStatesOf(
                    refreshLocal = NotLoading.Incomplete,
                    prependLocal = NotLoading.Complete,
                    appendLocal = NotLoading.Incomplete
                )
            )
        )
        assertThat(receiver.hints).isEmpty()

        // This index points to a valid placeholder that ends up removed by filter().
        assertNull(differ[5])
        assertThat(receiver.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = 1,
                    indexInPage = 2,
                    presentedItemsBefore = 5,
                    presentedItemsAfter = -2,
                    originalPageOffsetFirst = -3,
                    originalPageOffsetLast = 1
                )
            )
        )

        // Should only resend the hint for index 5, since index 0 has already been loaded:
        // [[-3], [], [-1], [1], [3], [], null, null]
        pageEventCh.trySend(
            Append(
                pages = listOf(TransformablePage(2, listOf())),
                placeholdersAfter = 2,
                combinedLoadStates = localLoadStatesOf(
                    refreshLocal = NotLoading.Incomplete,
                    prependLocal = NotLoading.Complete,
                    appendLocal = NotLoading.Incomplete
                )
            )
        )
        assertThat(receiver.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = 2,
                    indexInPage = 1,
                    presentedItemsBefore = 5,
                    presentedItemsAfter = -2,
                    originalPageOffsetFirst = -3,
                    originalPageOffsetLast = 2
                )
            )
        )

        // Index 5 hasn't loaded, but we are at the end of the list:
        // [[-3], [], [-1], [1], [3], [], [5]]
        pageEventCh.trySend(
            Append(
                pages = listOf(TransformablePage(3, listOf(4, 5))),
                placeholdersAfter = 0,
                combinedLoadStates = localLoadStatesOf(
                    NotLoading.Incomplete,
                    NotLoading.Complete,
                    NotLoading.Complete
                )

            )
        )
        assertThat(receiver.hints).isEmpty()

        job.cancel()
    }

    @Test
    fun fetch_loadHintResentUnlessPageDropped() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        pageEventCh.trySend(
            Refresh(
                pages = listOf(TransformablePage(0, listOf(0, 1))),
                placeholdersBefore = 4,
                placeholdersAfter = 4,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-1, listOf(-1, -2))),
                placeholdersBefore = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        pageEventCh.trySend(
            Append(
                pages = listOf(TransformablePage(1, listOf(2, 3))),
                placeholdersAfter = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )

        val receiver = UiReceiverFake()
        val job = launch {
            differ.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), receiver).filter { it % 2 != 0 }
            )
        }

        // Initial state:
        // [null, null, [-1], [1], [3], null, null]
        assertNull(differ[0])
        assertThat(receiver.hints).isEqualTo(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                ),
                ViewportHint.Access(
                    pageOffset = -1,
                    indexInPage = -2,
                    presentedItemsBefore = -2,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 1
                ),
            )
        )

        // Insert a new page, PagingDataDiffer should try to resend hint since index 0 still points
        // to a placeholder:
        // [null, null, [], [-1], [1], [3], null, null]
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        assertThat(receiver.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = -2,
                    indexInPage = -2,
                    presentedItemsBefore = -2,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = -2,
                    originalPageOffsetLast = 1
                )
            )
        )

        // Drop the previous page, which reset resendable index state in the PREPEND direction.
        // [null, null, [-1], [1], [3], null, null]
        pageEventCh.trySend(
            Drop(
                loadType = PREPEND,
                minPageOffset = -2,
                maxPageOffset = -2,
                placeholdersRemaining = 2
            )
        )

        // Re-insert the previous page, which should not trigger resending the index due to
        // previous page drop:
        // [[-3], [], [-1], [1], [3], null, null]
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )

        job.cancel()
    }

    @Test
    fun peek() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        pageEventCh.trySend(
            Refresh(
                pages = listOf(TransformablePage(0, listOf(0, 1))),
                placeholdersBefore = 4,
                placeholdersAfter = 4,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        pageEventCh.trySend(
            Prepend(
                pages = listOf(TransformablePage(-1, listOf(-1, -2))),
                placeholdersBefore = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )
        pageEventCh.trySend(
            Append(
                pages = listOf(TransformablePage(1, listOf(2, 3))),
                placeholdersAfter = 2,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )

        val receiver = UiReceiverFake()
        val job = launch {
            differ.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), receiver)
            )
        }

        // Check that peek fetches the correct placeholder
        assertThat(differ.peek(4)).isEqualTo(0)

        // Check that peek fetches the correct placeholder
        assertNull(differ.peek(0))

        // Check that peek does not trigger page fetch.
        assertThat(receiver.hints).isEqualTo(
            listOf<ViewportHint>(
                ViewportHint.Initial(
                    presentedItemsBefore = 1,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
        )

        job.cancel()
    }

    @Test
    fun initialHint_emptyRefresh() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val uiReceiver = UiReceiverFake()
        val job = launch {
            differ.collectFrom(PagingData(pageEventCh.consumeAsFlow(), uiReceiver))
        }

        pageEventCh.trySend(
            Refresh(
                pages = listOf(TransformablePage(emptyList())),
                placeholdersBefore = 0,
                placeholdersAfter = 0,
                combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
            )
        )

        assertThat(uiReceiver.hints).isEqualTo(
            listOf(ViewportHint.Initial(0, 0, 0, 0))
        )

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedListener_empty() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val listenerEvents = mutableListOf<Unit>()
        differ.addOnPagesUpdatedListener {
            listenerEvents.add(Unit)
        }

        differ.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(1)

        // No change to LoadState or presented list should still trigger the listener.
        differ.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(2)

        val pager = Pager(PagingConfig(pageSize = 1)) { TestPagingSource(items = listOf()) }
        val job = testScope.launch {
            pager.flow.collectLatest { differ.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(2)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(3)

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedListener_insertDrop() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val listenerEvents = mutableListOf<Unit>()
        differ.addOnPagesUpdatedListener {
            listenerEvents.add(Unit)
        }

        val pager = Pager(PagingConfig(pageSize = 1, maxSize = 4), initialKey = 50) {
            TestPagingSource()
        }
        val job = testScope.launch {
            pager.flow.collectLatest { differ.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(0)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(1)

        // Trigger PREPEND.
        differ[50]
        assertThat(listenerEvents.size).isEqualTo(1)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(2)

        // Trigger APPEND + Drop
        differ[52]
        assertThat(listenerEvents.size).isEqualTo(2)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(4)

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedFlow_empty() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val listenerEvents = mutableListOf<Unit>()
        val job1 = testScope.launch {
            differ.onPagesUpdatedFlow.collect {
                listenerEvents.add(Unit)
            }
        }

        differ.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(1)

        // No change to LoadState or presented list should still trigger the listener.
        differ.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(2)

        val pager = Pager(PagingConfig(pageSize = 1)) { TestPagingSource(items = listOf()) }
        val job2 = testScope.launch {
            pager.flow.collectLatest { differ.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(2)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(3)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun onPagingDataPresentedFlow_insertDrop() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val listenerEvents = mutableListOf<Unit>()
        val job1 = testScope.launch {
            differ.onPagesUpdatedFlow.collect {
                listenerEvents.add(Unit)
            }
        }

        val pager = Pager(PagingConfig(pageSize = 1, maxSize = 4), initialKey = 50) {
            TestPagingSource()
        }
        val job2 = testScope.launch {
            pager.flow.collectLatest { differ.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(0)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(1)

        // Trigger PREPEND.
        differ[50]
        assertThat(listenerEvents.size).isEqualTo(1)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(2)

        // Trigger APPEND + Drop
        differ[52]
        assertThat(listenerEvents.size).isEqualTo(2)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(4)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun onPagingDataPresentedFlow_buffer() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val listenerEvents = mutableListOf<Unit>()

        // Trigger update, which should get ignored due to onPagesUpdatedFlow being hot.
        differ.collectFrom(PagingData.empty())

        val job = testScope.launch {
            differ.onPagesUpdatedFlow.collect {
                listenerEvents.add(Unit)
                // Await advanceUntilIdle() before accepting another event.
                delay(100)
            }
        }

        // Previous update before collection happened should be ignored.
        assertThat(listenerEvents.size).isEqualTo(0)

        // Trigger update; should get immediately received.
        differ.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(1)

        // Trigger 64 update while collector is still processing; should all get buffered.
        repeat(64) { differ.collectFrom(PagingData.empty()) }

        // Trigger another update while collector is still processing; should cause event to drop.
        differ.collectFrom(PagingData.empty())

        // Await all; we should now receive the buffered event.
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(65)

        job.cancel()
    }

    @Test
    fun loadStateFlowSynchronouslyUpdates() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        var combinedLoadStates: CombinedLoadStates? = null
        var itemCount = -1
        val loadStateJob = launch {
            differ.loadStateFlow.collect {
                combinedLoadStates = it
                itemCount = differ.size
            }
        }

        val pager = Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                initialLoadSize = 10,
                prefetchDistance = 1
            ),
            initialKey = 50
        ) { TestPagingSource() }
        val job = launch {
            pager.flow.collectLatest { differ.collectFrom(it) }
        }

        // Initial refresh
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(10, itemCount)
        assertEquals(10, differ.size)

        // Append
        differ[9]
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(20, itemCount)
        assertEquals(20, differ.size)

        // Prepend
        differ[0]
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(30, itemCount)
        assertEquals(30, differ.size)

        job.cancel()
        loadStateJob.cancel()
    }

    @Test
    fun loadStateListenerSynchronouslyUpdates() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        pauseDispatcher {
            var combinedLoadStates: CombinedLoadStates? = null
            var itemCount = -1
            differ.addLoadStateListener {
                combinedLoadStates = it
                itemCount = differ.size
            }

            val pager = Pager(
                config = PagingConfig(
                    pageSize = 10,
                    enablePlaceholders = false,
                    initialLoadSize = 10,
                    prefetchDistance = 1
                ),
                initialKey = 50
            ) { TestPagingSource() }
            val job = launch {
                pager.flow.collectLatest { differ.collectFrom(it) }
            }

            // Initial refresh
            advanceUntilIdle()
            assertEquals(localLoadStatesOf(), combinedLoadStates)
            assertEquals(10, itemCount)
            assertEquals(10, differ.size)

            // Append
            differ[9]
            advanceUntilIdle()
            assertEquals(localLoadStatesOf(), combinedLoadStates)
            assertEquals(20, itemCount)
            assertEquals(20, differ.size)

            // Prepend
            differ[0]
            advanceUntilIdle()
            assertEquals(localLoadStatesOf(), combinedLoadStates)
            assertEquals(30, itemCount)
            assertEquals(30, differ.size)

            job.cancel()
        }
    }
}

private fun infinitelySuspendingPagingData(receiver: UiReceiver = dummyReceiver) = PagingData(
    flow { emit(suspendCancellableCoroutine<PageEvent<Int>> { }) },
    receiver
)

private class UiReceiverFake : UiReceiver {
    private val _hints = mutableListOf<ViewportHint>()
    val hints: List<ViewportHint>
        get() {
            val result = _hints.toList()
            @OptIn(ExperimentalStdlibApi::class)
            repeat(result.size) { _hints.removeFirst() }
            return result
        }

    val retryEvents = mutableListOf<Unit>()
    val refreshEvents = mutableListOf<Unit>()

    override fun accessHint(viewportHint: ViewportHint) {
        _hints.add(viewportHint)
    }

    override fun retry() {
        retryEvents.add(Unit)
    }

    override fun refresh() {
        refreshEvents.add(Unit)
    }
}

private class SimpleDiffer(differCallback: DifferCallback) : PagingDataDiffer<Int>(differCallback) {
    override suspend fun presentNewList(
        previousList: NullPaddedList<Int>,
        newList: NullPaddedList<Int>,
        newCombinedLoadStates: CombinedLoadStates,
        lastAccessedIndex: Int,
        onListPresentable: () -> Unit
    ): Int? {
        onListPresentable()
        return null
    }
}

internal val dummyReceiver = object : UiReceiver {
    override fun accessHint(viewportHint: ViewportHint) {}
    override fun retry() {}
    override fun refresh() {}
}

private val dummyDifferCallback = object : DifferCallback {
    override fun onInserted(position: Int, count: Int) {}

    override fun onChanged(position: Int, count: Int) {}

    override fun onRemoved(position: Int, count: Int) {}
}
