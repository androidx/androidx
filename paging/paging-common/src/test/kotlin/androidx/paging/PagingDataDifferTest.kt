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

import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.PREPEND
import androidx.paging.PageEvent.Drop
import androidx.paging.PagingSource.LoadResult
import androidx.testutils.DirectDispatcher
import androidx.testutils.MainDispatcherRule
import androidx.testutils.TestDispatcher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertFailsWith
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
            localRefresh(
                pages = listOf(TransformablePage(0, listOf(0, 1))),
                placeholdersBefore = 4,
                placeholdersAfter = 4,
            )
        )
        pageEventCh.trySend(
            localPrepend(
                pages = listOf(TransformablePage(-1, listOf(-1, -2))),
                placeholdersBefore = 2,
            )
        )
        pageEventCh.trySend(
            localAppend(
                pages = listOf(TransformablePage(1, listOf(2, 3))),
                placeholdersAfter = 2,
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
            localPrepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
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
            localPrepend(
                pages = listOf(TransformablePage(-3, listOf(-3, -4))),
                placeholdersBefore = 0,
                source = loadStates(prepend = NotLoading.Complete)
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
            localAppend(
                pages = listOf(TransformablePage(2, listOf())),
                placeholdersAfter = 2,
                source = loadStates(prepend = NotLoading.Complete)
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
            localAppend(
                pages = listOf(TransformablePage(3, listOf(4, 5))),
                placeholdersAfter = 0,
                source = loadStates(prepend = NotLoading.Complete, append = NotLoading.Complete)
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
            localRefresh(
                pages = listOf(TransformablePage(0, listOf(0, 1))),
                placeholdersBefore = 4,
                placeholdersAfter = 4,
            )
        )
        pageEventCh.trySend(
            localPrepend(
                pages = listOf(TransformablePage(-1, listOf(-1, -2))),
                placeholdersBefore = 2,
            )
        )
        pageEventCh.trySend(
            localAppend(
                pages = listOf(TransformablePage(1, listOf(2, 3))),
                placeholdersAfter = 2,
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
            localPrepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
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
            localPrepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
            )
        )

        job.cancel()
    }

    @Test
    fun peek() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        pageEventCh.trySend(
            localRefresh(
                pages = listOf(TransformablePage(0, listOf(0, 1))),
                placeholdersBefore = 4,
                placeholdersAfter = 4,
            )
        )
        pageEventCh.trySend(
            localPrepend(
                pages = listOf(TransformablePage(-1, listOf(-1, -2))),
                placeholdersBefore = 2,
            )
        )
        pageEventCh.trySend(
            localAppend(
                pages = listOf(TransformablePage(1, listOf(2, 3))),
                placeholdersAfter = 2,
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
            localRefresh(pages = listOf(TransformablePage(emptyList())))
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
    fun loadStateFlow_synchronouslyUpdates() = testScope.runBlockingTest {
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
    fun loadStateFlow_hasNoInitialValue() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            differ.loadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates).isEmpty()

        // Add a real value and now we should emit to collector.
        differ.collectFrom(PagingData.empty())
        assertThat(combinedLoadStates).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )

        // Should emit real values to new collectors immediately
        val newCombinedLoadStates = mutableListOf<CombinedLoadStates>()
        val newLoadStateJob = launch {
            differ.loadStateFlow.collect {
                newCombinedLoadStates.add(it)
            }
        }
        assertThat(newCombinedLoadStates).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )

        loadStateJob.cancel()
        newLoadStateJob.cancel()
    }

    @Test
    fun addLoadStateListener_SynchronouslyUpdates() = testScope.runBlockingTest {
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

    @Test
    fun addLoadStateListener_hasNoInitialValue() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val combinedLoadStateCapture = CombinedLoadStatesCapture()

        // Adding a new listener without a real value should not trigger it.
        differ.addLoadStateListener(combinedLoadStateCapture)
        assertThat(combinedLoadStateCapture.newEvents()).isEmpty()

        // Add a real value and now the listener should trigger.
        differ.collectFrom(PagingData.empty())
        assertThat(combinedLoadStateCapture.newEvents()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )

        // Should emit real values to new listeners immediately
        val newCombinedLoadStateCapture = CombinedLoadStatesCapture()
        differ.addLoadStateListener(newCombinedLoadStateCapture)
        assertThat(newCombinedLoadStateCapture.newEvents()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )
    }

    @Test
    fun uncaughtException() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val pager = Pager(
            PagingConfig(1),
        ) {
            object : PagingSource<Int, Int>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
                    throw IllegalStateException()
                }

                override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
            }
        }

        val pagingData = pager.flow.first()
        val deferred = testScope.async {
            differ.collectFrom(pagingData)
        }

        advanceUntilIdle()
        assertFailsWith<IllegalStateException> { deferred.await() }
    }

    @Test
    fun handledLoadResultInvalid() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        var generation = 0
        val pager = Pager(
            PagingConfig(1),
        ) {
            TestPagingSource().also {
                if (generation == 0) {
                    it.nextLoadResult = PagingSource.LoadResult.Invalid()
                }
                generation++
            }
        }

        val pagingData = pager.flow.first()
        val deferred = testScope.async {
            // only returns if flow is closed, or work canclled, or exception thrown
            // in this case it should cancel due LoadResult.Invalid causing collectFrom to return
            differ.collectFrom(pagingData)
        }

        advanceUntilIdle()
        // this will return only if differ.collectFrom returns
        deferred.await()
    }

    @Test
    fun refresh_loadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // execute queued initial REFRESH
        loadDispatcher.queue.poll()?.run()

        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )

        differ.refresh()

        // execute second REFRESH load
        loadDispatcher.queue.poll()?.run()

        // second refresh still loads from initialKey = 50 because anchorPosition/refreshKey is null
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf()
        )

        collectLoadStates.cancel()
    }

    // TODO(b/195028524) the tests from here on checks the state after Invalid/Error results.
    //  Upon changes due to b/195028524, the asserts on these tests should see a new resetting
    //  LoadStateUpdate event

    @Test
    fun appendInvalid_loadStates() = runTest { differ, loadDispatcher, pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.executeAll()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        // normal append
        differ[8]

        loadDispatcher.executeAll()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 12)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            localLoadStatesOf(prependLocal = NotLoading.Complete)
        )

        // do invalid append which will return LoadResult.Invalid
        differ[11]
        pagingSources[0].nextLoadResult = LoadResult.Invalid()

        // using poll().run() instead of executeAll, otherwise this invalid APPEND + subsequent
        // REFRESH will auto run consecutively and we won't be able to assert them incrementally
        loadDispatcher.queue.poll()?.run()

        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // the invalid append
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            // REFRESH on new paging source. Append/Prepend local states is reset because the
            // LoadStateUpdate from refresh sends the full map of a local LoadStates which was
            // initialized as IDLE upon new Snapshot.
            localLoadStatesOf(
                refreshLocal = Loading,
            ),
        )

        // the LoadResult.Invalid from failed APPEND triggers new pagingSource + initial REFRESH
        loadDispatcher.queue.poll()?.run()

        assertThat(differ.snapshot()).containsExactlyElementsIn(11 until 20)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependInvalid_loadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.executeAll()

        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            // all local states NotLoading.Incomplete
            localLoadStatesOf(),
        )

        // normal prepend to ensure LoadStates for Page returns remains the same
        differ[0]

        loadDispatcher.executeAll()

        assertThat(differ.snapshot()).containsExactlyElementsIn(47 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            // all local states NotLoading.Incomplete
            localLoadStatesOf(),
        )

        // do an invalid prepend which will return LoadResult.Invalid
        differ[0]
        pagingSources[0].nextLoadResult = LoadResult.Invalid()
        loadDispatcher.queue.poll()?.run()

        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // the invalid prepend
            localLoadStatesOf(prependLocal = Loading),
            // REFRESH on new paging source. Append/Prepend local states is reset because the
            // LoadStateUpdate from refresh sends the full map of a local LoadStates which was
            // initialized as IDLE upon new Snapshot.
            localLoadStatesOf(refreshLocal = Loading),
        )

        // the LoadResult.Invalid from failed PREPEND triggers new pagingSource + initial REFRESH
        loadDispatcher.queue.poll()?.run()

        // load starts from 0 again because the provided initialKey = 50 is not multi-generational
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshInvalid_loadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // execute queued initial REFRESH load which will return LoadResult.Invalid()
        pagingSources[0].nextLoadResult = LoadResult.Invalid()
        loadDispatcher.queue.poll()?.run()

        assertThat(differ.snapshot()).isEmpty()
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // invalid first refresh. The second refresh state update that follows is identical to
            // this LoadStates so it gets de-duped
            localLoadStatesOf(refreshLocal = Loading),
        )

        // execute second REFRESH load
        loadDispatcher.queue.poll()?.run()

        // second refresh still loads from initialKey = 50 because anchorPosition/refreshKey is null
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // all local states NotLoading.Incomplete
            localLoadStatesOf()
        )

        collectLoadStates.cancel()
    }

    @Test
    fun appendError_retryLoadStates() = runTest { differ, loadDispatcher, pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.executeAll()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)

        // append returns LoadResult.Error
        differ[8]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.queue.poll()?.run()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = LoadState.Error(exception)
            ),
        )
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)

        // retry append
        differ.retry()
        loadDispatcher.queue.poll()?.run()

        // make sure append success
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 12)
        // no reset
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependError_retryLoadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.executeAll()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )

        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)

        // prepend returns LoadResult.Error
        differ[0]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.queue.poll()?.run()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(prependLocal = LoadState.Error(exception)),
        )
        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)

        // retry prepend
        differ.retry()

        loadDispatcher.queue.poll()?.run()

        // make sure prepend success
        assertThat(differ.snapshot()).containsExactlyElementsIn(47 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshError_retryLoadStates() = runTest() { differ, loadDispatcher, pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // initial load returns LoadResult.Error
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.executeAll()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(refreshLocal = LoadState.Error(exception)),
        )
        assertThat(differ.snapshot()).isEmpty()

        // retry refresh
        differ.retry()

        loadDispatcher.queue.poll()?.run()

        // refresh retry does not trigger new gen
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        // Goes directly from Error --> Loading without resetting refresh to NotLoading
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependError_refreshLoadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.executeAll()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )
        assertThat(differ.size).isEqualTo(9)
        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)

        // prepend returns LoadResult.Error
        differ[0]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.queue.poll()?.run()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(prependLocal = LoadState.Error(exception)),
        )

        // refresh() should reset local LoadStates and trigger new REFRESH
        differ.refresh()
        loadDispatcher.queue.poll()?.run()

        // Initial load starts from 0 because initialKey is single gen.
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // second gen REFRESH load. The Error prepend state was automatically reset to
            // NotLoading.
            localLoadStatesOf(refreshLocal = Loading),
            // REFRESH complete
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshError_refreshLoadStates() = runTest() { differ, loadDispatcher, pagingSources ->
        val collectLoadStates = differ.collectLoadStates()

        // the initial load will return LoadResult.Error
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.executeAll()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(refreshLocal = LoadState.Error(exception)),
        )
        assertThat(differ.snapshot()).isEmpty()

        // refresh should trigger new generation
        differ.refresh()

        loadDispatcher.queue.poll()?.run()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        // Goes directly from Error --> Loading without resetting refresh to NotLoading
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun remoteRefresh_refreshStatePersists() = testScope.runBlockingTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val remoteMediator = RemoteMediatorMock(loadDelay = 1500).apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        val pager = Pager(
            PagingConfig(pageSize = 3, enablePlaceholders = false),
            remoteMediator = remoteMediator,
        ) {
            TestPagingSource(loadDelay = 500, items = emptyList())
        }

        val collectLoadStates = differ.collectLoadStates()
        val job = launch {
            pager.flow.collectLatest {
                differ.collectFrom(it)
            }
        }
        // allow local refresh to complete but not remote refresh
        advanceTimeBy(600)

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // local starts loading
            remoteLoadStatesOf(
                refreshLocal = Loading,
            ),
            // remote starts loading
            remoteLoadStatesOf(
                refresh = Loading,
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
            // local load returns with empty data, mediator is still loading
            remoteLoadStatesOf(
                refresh = Loading,
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
                refreshRemote = Loading,
            ),
        )

        // refresh triggers new generation & LoadState reset
        differ.refresh()

        // allow local refresh to complete but not remote refresh
        advanceTimeBy(600)

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // local starts second refresh while mediator continues remote refresh from before
            remoteLoadStatesOf(
                refresh = Loading,
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
            // local load returns empty data
            remoteLoadStatesOf(
                refresh = Loading,
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
                refreshRemote = Loading,
            ),
        )

        // allow remote refresh to complete
        advanceTimeBy(600)

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // remote refresh returns empty and triggers remote append/prepend
            remoteLoadStatesOf(
                prepend = Loading,
                append = Loading,
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
                prependRemote = Loading,
                appendRemote = Loading,
            ),
        )

        // allow remote append and prepend to complete
        advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // prepend completes first
            remoteLoadStatesOf(
                append = Loading,
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
                appendRemote = Loading,
            ),
            remoteLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            ),
        )

        job.cancel()
        collectLoadStates.cancel()
    }

    private fun runTest(
        scope: CoroutineScope = CoroutineScope(DirectDispatcher),
        differ: SimpleDiffer = SimpleDiffer(
            differCallback = dummyDifferCallback,
            coroutineScope = scope,
        ),
        loadDispatcher: TestDispatcher = TestDispatcher(),
        initialKey: Int? = null,
        pagingSources: MutableList<TestPagingSource> = mutableListOf(),
        pager: Pager<Int, Int> =
            Pager(
                config = PagingConfig(pageSize = 3, enablePlaceholders = false),
                initialKey = initialKey,
                pagingSourceFactory = {
                    TestPagingSource(
                        loadDelay = 0,
                        loadDispatcher = loadDispatcher,
                    ).also { pagingSources.add(it) }
                }
            ),
        block: (SimpleDiffer, TestDispatcher, MutableList<TestPagingSource>) -> Unit
    ) {
        val collection = scope.launch {
            pager.flow.collect {
                differ.collectFrom(it)
            }
        }
        scope.run {
            try {
                block(differ, loadDispatcher, pagingSources)
            } finally {
                collection.cancel()
            }
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

@OptIn(ExperimentalCoroutinesApi::class)
private class SimpleDiffer(
    differCallback: DifferCallback,
    val coroutineScope: CoroutineScope = TestCoroutineScope()
) : PagingDataDiffer<Int>(differCallback) {
    override suspend fun presentNewList(
        previousList: NullPaddedList<Int>,
        newList: NullPaddedList<Int>,
        lastAccessedIndex: Int,
        onListPresentable: () -> Unit
    ): Int? {
        onListPresentable()
        return null
    }

    private val _localLoadStates = mutableListOf<CombinedLoadStates>()

    fun newCombinedLoadStates(): List<CombinedLoadStates?> {
        val newCombinedLoadStates = _localLoadStates.toList()
        _localLoadStates.clear()
        return newCombinedLoadStates
    }

    fun collectLoadStates(): Job {
        return coroutineScope.launch {
            loadStateFlow.collect { combinedLoadStates ->
                _localLoadStates.add(combinedLoadStates)
            }
        }
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
