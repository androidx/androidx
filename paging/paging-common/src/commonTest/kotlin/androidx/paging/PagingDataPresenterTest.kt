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

import androidx.kruth.assertThat
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.PREPEND
import androidx.paging.PageEvent.Drop
import androidx.paging.PagingSource.LoadResult
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * run some tests with cached-in to ensure caching does not change behavior in the single
 * consumer cases.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
class PagingDataPresenterTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun collectFrom_static() = testScope.runTest {
        val presenter = SimplePresenter()
        val receiver = UiReceiverFake()

        val job1 = launch {
            presenter.collectFrom(infinitelySuspendingPagingData(receiver))
        }
        advanceUntilIdle()
        job1.cancel()

        val job2 = launch {
            presenter.collectFrom(PagingData.empty())
        }
        advanceUntilIdle()
        job2.cancel()

        // Static replacement should also replace the UiReceiver from previous generation.
        presenter.retry()
        presenter.refresh()
        advanceUntilIdle()

        assertFalse { receiver.retryEvents.isNotEmpty() }
        assertFalse { receiver.refreshEvents.isNotEmpty() }
    }

    @Test
    fun collectFrom_twice() = testScope.runTest {
        val presenter = SimplePresenter()

        launch { presenter.collectFrom(infinitelySuspendingPagingData()) }
            .cancel()
        launch { presenter.collectFrom(infinitelySuspendingPagingData()) }
            .cancel()
    }

    @Test
    fun collectFrom_twiceConcurrently() = testScope.runTest {
        val presenter = SimplePresenter()

        val job1 = launch {
            presenter.collectFrom(infinitelySuspendingPagingData())
        }

        // Ensure job1 is running.
        assertTrue { job1.isActive }

        val job2 = launch {
            presenter.collectFrom(infinitelySuspendingPagingData())
        }

        // job2 collection should complete job1 but not cancel.
        assertFalse { job1.isCancelled }
        assertTrue { job1.isCompleted }
        job2.cancel()
    }

    @Test
    fun retry() = testScope.runTest {
        val presenter = SimplePresenter()
        val receiver = UiReceiverFake()

        val job = launch {
            presenter.collectFrom(infinitelySuspendingPagingData(receiver))
        }

        presenter.retry()
        assertEquals(1, receiver.retryEvents.size)

        job.cancel()
    }

    @Test
    fun refresh() = testScope.runTest {
        val presenter = SimplePresenter()
        val receiver = UiReceiverFake()

        val job = launch {
            presenter.collectFrom(infinitelySuspendingPagingData(receiver))
        }

        presenter.refresh()

        assertEquals(1, receiver.refreshEvents.size)

        job.cancel()
    }

    @Test
    fun uiReceiverSetImmediately() = testScope.runTest {
        val presenter = SimplePresenter()
        val receiver = UiReceiverFake()
        val pagingData1 = infinitelySuspendingPagingData(uiReceiver = receiver)

        val job1 = launch {
            presenter.collectFrom(pagingData1)
        }
        assertTrue(job1.isActive) // ensure job started

        assertThat(receiver.refreshEvents).hasSize(0)

        presenter.refresh()
        // double check that the pagingdata's receiver was registered and had received refresh call
        // before any PageEvent is collected/presented
        assertThat(receiver.refreshEvents).hasSize(1)

        job1.cancel()
    }

    @Test
    fun hintReceiverSetAfterNewListPresented() = testScope.runTest {
        val presenter = SimplePresenter()

        // first generation, load something so next gen can access index to trigger hint
        val hintReceiver1 = HintReceiverFake()
        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4)))),
        )

        val job1 = launch {
            presenter.collectFrom(PagingData(flow, dummyUiReceiver, hintReceiver1))
        }

        // access any loaded item to make sure hint is sent
        presenter[3]
        assertThat(hintReceiver1.hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 3,
                presentedItemsBefore = 3,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0,
            )
        )

        // trigger second generation
        presenter.refresh()

        // second generation
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver2 = HintReceiverFake()
        val job2 = launch {
            presenter.collectFrom(
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver2)
            )
        }

        // we send the initial load state. this should NOT cause second gen hint receiver
        // to register
        pageEventCh.trySend(
            localLoadStateUpdate(refreshLocal = Loading)
        )
        assertThat(presenter.nonNullLoadStateFlow.first()).isEqualTo(
            localLoadStatesOf(refreshLocal = Loading)
        )

        // ensure both hint receivers are idle before sending a hint
        assertThat(hintReceiver1.hints).isEmpty()
        assertThat(hintReceiver2.hints).isEmpty()

        // try sending a hint, should be sent to first receiver
        presenter[4]
        assertThat(hintReceiver1.hints).hasSize(1)
        assertThat(hintReceiver2.hints).isEmpty()

        // now we send actual refresh load and make sure its presented
        pageEventCh.trySend(
            localRefresh(
                pages = listOf(TransformablePage(listOf(20, 21, 22, 23, 24))),
                placeholdersBefore = 20,
                placeholdersAfter = 75
            ),
        )
        assertThat(presenter.snapshot().items).containsExactlyElementsIn(20 until 25)

        // access any loaded item to make sure hint is sent to proper receiver
        presenter[3]
        // second receiver was registered and received the initial viewport hint
        assertThat(hintReceiver1.hints).isEmpty()
        assertThat(hintReceiver2.hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = -17,
                presentedItemsBefore = -17,
                presentedItemsAfter = 21,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0,
            )
        )

        job2.cancel()
        job1.cancel()
    }

    @Test
    fun refreshOnLatestGenerationReceiver() = refreshOnLatestGenerationReceiver(false)

    @Test
    fun refreshOnLatestGenerationReceiver_collectWithCachedIn() =
        refreshOnLatestGenerationReceiver(true)

    private fun refreshOnLatestGenerationReceiver(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, _,
        uiReceivers, hintReceivers ->
        // first gen
        advanceUntilIdle()
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)

        // append a page so we can cache an anchorPosition of [8]
        presenter[8]
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 12)

        // trigger gen 2, the refresh signal should to sent to gen 1
        presenter.refresh()
        assertThat(uiReceivers[0].refreshEvents).hasSize(1)
        assertThat(uiReceivers[1].refreshEvents).hasSize(0)

        // trigger gen 3, refresh signal should be sent to gen 2
        presenter.refresh()
        assertThat(uiReceivers[0].refreshEvents).hasSize(1)
        assertThat(uiReceivers[1].refreshEvents).hasSize(1)
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(8 until 17)

        // access any item to make sure gen 3 receiver is recipient of the hint
        presenter[0]
        assertThat(hintReceivers[2].hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 8,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0,
            )
        )
    }

    @Test
    fun retryOnLatestGenerationReceiver() = retryOnLatestGenerationReceiver(false)

    @Test
    fun retryOnLatestGenerationReceiver_collectWithCachedIn() =
        retryOnLatestGenerationReceiver(true)

    private fun retryOnLatestGenerationReceiver(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, pagingSources,
        uiReceivers, hintReceivers ->

        // first gen
        advanceUntilIdle()
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)

        // append a page so we can cache an anchorPosition of [8]
        presenter[8]
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 12)

        // trigger gen 2, the refresh signal should be sent to gen 1
        presenter.refresh()
        assertThat(uiReceivers[0].refreshEvents).hasSize(1)
        assertThat(uiReceivers[1].refreshEvents).hasSize(0)

        // to recreate a real use-case of retry based on load error
        pagingSources[1].errorNextLoad = true
        advanceUntilIdle()
        // presenter should still have first gen presenter
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 12)

        // retry should be sent to gen 2 even though it wasn't presented
        presenter.retry()
        assertThat(uiReceivers[0].retryEvents).hasSize(0)
        assertThat(uiReceivers[1].retryEvents).hasSize(1)
        advanceUntilIdle()

        // will retry with the correct cached hint
        assertThat(presenter.snapshot()).containsExactlyElementsIn(8 until 17)

        // access any item to ensure gen 2 receiver was recipient of the initial hint
        presenter[0]
        assertThat(hintReceivers[1].hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 8,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0,
            )
        )
    }

    @Test
    fun refreshAfterStaticList() = testScope.runTest {
        val presenter = SimplePresenter()

        val pagingData1 = PagingData.from(listOf(1, 2, 3))
        val job1 = launch { presenter.collectFrom(pagingData1) }
        assertTrue(job1.isCompleted)
        assertThat(presenter.snapshot()).containsAtLeastElementsIn(listOf(1, 2, 3))

        val uiReceiver = UiReceiverFake()
        val pagingData2 = infinitelySuspendingPagingData(uiReceiver = uiReceiver)
        val job2 = launch { presenter.collectFrom(pagingData2) }
        assertTrue(job2.isActive)

        // even though the second paging data never presented, it should be receiver of the refresh
        presenter.refresh()
        assertThat(uiReceiver.refreshEvents).hasSize(1)

        job2.cancel()
    }

    @Test
    fun retryAfterStaticList() = testScope.runTest {
        val presenter = SimplePresenter()

        val pagingData1 = PagingData.from(listOf(1, 2, 3))
        val job1 = launch { presenter.collectFrom(pagingData1) }
        assertTrue(job1.isCompleted)
        assertThat(presenter.snapshot()).containsAtLeastElementsIn(listOf(1, 2, 3))

        val uiReceiver = UiReceiverFake()
        val pagingData2 = infinitelySuspendingPagingData(uiReceiver = uiReceiver)
        val job2 = launch { presenter.collectFrom(pagingData2) }
        assertTrue(job2.isActive)

        // even though the second paging data never presented, it should be receiver of the retry
        presenter.retry()
        assertThat(uiReceiver.retryEvents).hasSize(1)

        job2.cancel()
    }

    @Test
    fun hintCalculationBasedOnCurrentGeneration() = testScope.runTest {
        val presenter = SimplePresenter()

        // first generation
        val hintReceiver1 = HintReceiverFake()
        val uiReceiver1 = UiReceiverFake()
        val flow = flowOf(
            localRefresh(
                pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4))),
                placeholdersBefore = 0,
                placeholdersAfter = 95
            )
        )

        val job1 = launch {
            presenter.collectFrom(PagingData(flow, uiReceiver1, hintReceiver1))
        }
        // access any item make sure hint is sent
        presenter[3]
        assertThat(hintReceiver1.hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 3,
                presentedItemsBefore = 3,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0,
            )
        )

        // jump to another position, triggers invalidation
        presenter[20]
        assertThat(hintReceiver1.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 20,
                    presentedItemsBefore = 20,
                    presentedItemsAfter = -16,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                ),
            )
        )

        // jump invalidation happens
        presenter.refresh()
        assertThat(uiReceiver1.refreshEvents).hasSize(1)

        // second generation
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver2 = HintReceiverFake()
        val job2 = launch {
            presenter.collectFrom(
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver2)
            )
        }

        // jump to another position while second gen is loading. It should be sent to first gen.
        presenter[40]
        assertThat(hintReceiver1.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 40,
                    presentedItemsBefore = 40,
                    presentedItemsAfter = -36,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                ),
            )
        )
        assertThat(hintReceiver2.hints).isEmpty()

        // gen 2 initial load
        pageEventCh.trySend(
            localRefresh(
                pages = listOf(TransformablePage(listOf(20, 21, 22, 23, 24))),
                placeholdersBefore = 20,
                placeholdersAfter = 75
            ),
        )
        // access any item make sure hint is sent
        presenter[3]
        assertThat(hintReceiver2.hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = -17,
                presentedItemsBefore = -17,
                presentedItemsAfter = 21,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0,
            )
        )

        // jumping to index 50. Hint.indexInPage should be adjusted accordingly based on
        // the placeholdersBefore of new presenter. It should be
        // (index - placeholdersBefore) = 50 - 20 = 30
        presenter[50]
        assertThat(hintReceiver2.hints).isEqualTo(
            listOf(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 30,
                    presentedItemsBefore = 30,
                    presentedItemsAfter = -26,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                ),
            )
        )

        job2.cancel()
        job1.cancel()
    }

    @Test
    fun fetch_loadHintResentWhenUnfulfilled() = testScope.runTest {
        val presenter = SimplePresenter()

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

        val hintReceiver = HintReceiverFake()
        val job = launch {
            presenter.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver)
                    .filter { it % 2 != 0 }
            )
        }

        // Initial state:
        // [null, null, [-1], [1], [3], null, null]
        assertNull(presenter[0])
        assertThat(hintReceiver.hints).isEqualTo(
            listOf(
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

        // Insert a new page, PagingDataPresenter should try to resend hint since index 0 still points
        // to a placeholder:
        // [null, null, [], [-1], [1], [3], null, null]
        pageEventCh.trySend(
            localPrepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
            )
        )
        assertThat(hintReceiver.hints).isEqualTo(
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
        assertThat(hintReceiver.hints).isEmpty()

        // This index points to a valid placeholder that ends up removed by filter().
        assertNull(presenter[5])
        assertThat(hintReceiver.hints).containsExactly(
            ViewportHint.Access(
                pageOffset = 1,
                indexInPage = 2,
                presentedItemsBefore = 5,
                presentedItemsAfter = -2,
                originalPageOffsetFirst = -3,
                originalPageOffsetLast = 1
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
        assertThat(hintReceiver.hints).isEqualTo(
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
        assertThat(hintReceiver.hints).isEmpty()

        job.cancel()
    }

    @Test
    fun fetch_loadHintResentUnlessPageDropped() = testScope.runTest {
        val presenter = SimplePresenter()

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

        val hintReceiver = HintReceiverFake()
        val job = launch {
            presenter.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver)
                    .filter { it % 2 != 0 }
            )
        }

        // Initial state:
        // [null, null, [-1], [1], [3], null, null]
        assertNull(presenter[0])
        assertThat(hintReceiver.hints).containsExactly(
            ViewportHint.Access(
                pageOffset = -1,
                indexInPage = -2,
                presentedItemsBefore = -2,
                presentedItemsAfter = 4,
                originalPageOffsetFirst = -1,
                originalPageOffsetLast = 1
            )
        )

        // Insert a new page, PagingDataPresenter should try to resend hint since index 0 still points
        // to a placeholder:
        // [null, null, [], [-1], [1], [3], null, null]
        pageEventCh.trySend(
            localPrepend(
                pages = listOf(TransformablePage(-2, listOf())),
                placeholdersBefore = 2,
            )
        )
        assertThat(hintReceiver.hints).isEqualTo(
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
    fun peek() = testScope.runTest {
        val presenter = SimplePresenter()
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

        val hintReceiver = HintReceiverFake()
        val job = launch {
            presenter.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver)
            )
        }

        // Check that peek fetches the correct placeholder
        assertThat(presenter.peek(4)).isEqualTo(0)

        // Check that peek fetches the correct placeholder
        assertNull(presenter.peek(0))

        // Check that peek does not trigger page fetch.
        assertThat(hintReceiver.hints).isEmpty()

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedListener_empty() = testScope.runTest {
        val presenter = SimplePresenter()
        val listenerEvents = mutableListOf<Unit>()
        presenter.addOnPagesUpdatedListener {
            listenerEvents.add(Unit)
        }

        presenter.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(1)

        // No change to LoadState or presented list should still trigger the listener.
        presenter.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(2)

        val pager = Pager(PagingConfig(pageSize = 1)) { TestPagingSource(items = listOf()) }
        val job = launch {
            pager.flow.collectLatest { presenter.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(2)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(3)

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedListener_insertDrop() = testScope.runTest {
        val presenter = SimplePresenter()
        val listenerEvents = mutableListOf<Unit>()
        presenter.addOnPagesUpdatedListener {
            listenerEvents.add(Unit)
        }

        val pager = Pager(PagingConfig(pageSize = 1, maxSize = 4), initialKey = 50) {
            TestPagingSource()
        }
        val job = launch {
            pager.flow.collectLatest { presenter.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(0)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(1)

        // Trigger PREPEND.
        presenter[50]
        assertThat(listenerEvents.size).isEqualTo(1)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(2)

        // Trigger APPEND + Drop
        presenter[52]
        assertThat(listenerEvents.size).isEqualTo(2)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(4)

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedFlow_empty() = testScope.runTest {
        val presenter = SimplePresenter()
        val listenerEvents = mutableListOf<Unit>()
        val job1 = launch {
            presenter.onPagesUpdatedFlow.collect {
                listenerEvents.add(Unit)
            }
        }

        presenter.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(1)

        // No change to LoadState or presented list should still trigger the listener.
        presenter.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(2)

        val pager = Pager(PagingConfig(pageSize = 1)) { TestPagingSource(items = listOf()) }
        val job2 = launch {
            pager.flow.collectLatest { presenter.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(2)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(3)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun onPagingDataPresentedFlow_insertDrop() = testScope.runTest {
        val presenter = SimplePresenter()
        val listenerEvents = mutableListOf<Unit>()
        val job1 = launch {
            presenter.onPagesUpdatedFlow.collect {
                listenerEvents.add(Unit)
            }
        }

        val pager = Pager(PagingConfig(pageSize = 1, maxSize = 4), initialKey = 50) {
            TestPagingSource()
        }
        val job2 = launch {
            pager.flow.collectLatest { presenter.collectFrom(it) }
        }

        // Should wait for new generation to load and apply it first.
        assertThat(listenerEvents.size).isEqualTo(0)

        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(1)

        // Trigger PREPEND.
        presenter[50]
        assertThat(listenerEvents.size).isEqualTo(1)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(2)

        // Trigger APPEND + Drop
        presenter[52]
        assertThat(listenerEvents.size).isEqualTo(2)
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(4)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun onPagingDataPresentedFlow_buffer() = testScope.runTest {
        val presenter = SimplePresenter()
        val listenerEvents = mutableListOf<Unit>()

        // Trigger update, which should get ignored due to onPagesUpdatedFlow being hot.
        presenter.collectFrom(PagingData.empty())

        val job = launch {
            presenter.onPagesUpdatedFlow.collect {
                listenerEvents.add(Unit)
                // Await advanceUntilIdle() before accepting another event.
                delay(100)
            }
        }

        // Previous update before collection happened should be ignored.
        assertThat(listenerEvents.size).isEqualTo(0)

        // Trigger update; should get immediately received.
        presenter.collectFrom(PagingData.empty())
        assertThat(listenerEvents.size).isEqualTo(1)

        // Trigger 64 update while collector is still processing; should all get buffered.
        repeat(64) { presenter.collectFrom(PagingData.empty()) }

        // Trigger another update while collector is still processing; should cause event to drop.
        presenter.collectFrom(PagingData.empty())

        // Await all; we should now receive the buffered event.
        advanceUntilIdle()
        assertThat(listenerEvents.size).isEqualTo(65)

        job.cancel()
    }

    @Test
    fun loadStateFlow_synchronouslyUpdates() = testScope.runTest {
        val presenter = SimplePresenter()
        var combinedLoadStates: CombinedLoadStates? = null
        var itemCount = -1
        val loadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
                combinedLoadStates = it
                itemCount = presenter.size
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
            pager.flow.collectLatest { presenter.collectFrom(it) }
        }

        // Initial refresh
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(10, itemCount)
        assertEquals(10, presenter.size)

        // Append
        presenter[9]
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(20, itemCount)
        assertEquals(20, presenter.size)

        // Prepend
        presenter[0]
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(30, itemCount)
        assertEquals(30, presenter.size)

        job.cancel()
        loadStateJob.cancel()
    }

    @Test
    fun loadStateFlow_hasNoInitialValue() = testScope.runTest {
        val presenter = SimplePresenter()

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates).isEmpty()

        // Add a real value and now we should emit to collector.
        presenter.collectFrom(
            PagingData.empty(
                sourceLoadStates = loadStates(
                    prepend = NotLoading.Complete,
                    append = NotLoading.Complete
                )
            )
        )
        assertThat(combinedLoadStates).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )

        // Should emit real values to new collectors immediately
        val newCombinedLoadStates = mutableListOf<CombinedLoadStates>()
        val newLoadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
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
    fun loadStateFlow_preservesLoadStatesOnEmptyList() = testScope.runTest {
        val presenter = SimplePresenter()

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a static list without load states, which should not send anything.
        presenter.collectFrom(PagingData.empty())
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a real LoadStateUpdate.
        presenter.collectFrom(
            PagingData(
                flow = flowOf(
                    remoteLoadStateUpdate(
                        refreshLocal = Loading,
                        prependLocal = Loading,
                        appendLocal = Loading,
                        refreshRemote = Loading,
                        prependRemote = Loading,
                        appendRemote = Loading,
                    )
                ),
                uiReceiver = PagingData.NOOP_UI_RECEIVER,
                hintReceiver = PagingData.NOOP_HINT_RECEIVER
            )
        )
        assertThat(combinedLoadStates.getAllAndClear()).containsExactly(
            remoteLoadStatesOf(
                refresh = Loading,
                prepend = Loading,
                append = Loading,
                refreshLocal = Loading,
                prependLocal = Loading,
                appendLocal = Loading,
                refreshRemote = Loading,
                prependRemote = Loading,
                appendRemote = Loading,
            )
        )

        // Send a static list without load states, which should preserve the previous state.
        presenter.collectFrom(PagingData.empty())
        // Existing observers should not receive any updates
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()
        // New observers should receive the previous state.
        val newCombinedLoadStates = mutableListOf<CombinedLoadStates>()
        val newLoadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
                newCombinedLoadStates.add(it)
            }
        }
        assertThat(newCombinedLoadStates.getAllAndClear()).containsExactly(
            remoteLoadStatesOf(
                refresh = Loading,
                prepend = Loading,
                append = Loading,
                refreshLocal = Loading,
                prependLocal = Loading,
                appendLocal = Loading,
                refreshRemote = Loading,
                prependRemote = Loading,
                appendRemote = Loading,
            )
        )

        loadStateJob.cancel()
        newLoadStateJob.cancel()
    }

    @Test
    fun loadStateFlow_preservesLoadStatesOnStaticList() = testScope.runTest {
        val presenter = SimplePresenter()

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a static list without load states, which should not send anything.
        presenter.collectFrom(PagingData.from(listOf(1)))
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a real LoadStateUpdate.
        presenter.collectFrom(
            PagingData(
                flow = flowOf(
                    remoteLoadStateUpdate(
                        refreshLocal = Loading,
                        prependLocal = Loading,
                        appendLocal = Loading,
                        refreshRemote = Loading,
                        prependRemote = Loading,
                        appendRemote = Loading,
                    )
                ),
                uiReceiver = PagingData.NOOP_UI_RECEIVER,
                hintReceiver = PagingData.NOOP_HINT_RECEIVER
            )
        )
        assertThat(combinedLoadStates.getAllAndClear()).containsExactly(
            remoteLoadStatesOf(
                refresh = Loading,
                prepend = Loading,
                append = Loading,
                refreshLocal = Loading,
                prependLocal = Loading,
                appendLocal = Loading,
                refreshRemote = Loading,
                prependRemote = Loading,
                appendRemote = Loading,
            )
        )

        // Send a static list without load states, which should preserve the previous state.
        presenter.collectFrom(PagingData.from(listOf(1)))
        // Existing observers should not receive any updates
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()
        // New observers should receive the previous state.
        val newCombinedLoadStates = mutableListOf<CombinedLoadStates>()
        val newLoadStateJob = launch {
            presenter.nonNullLoadStateFlow.collect {
                newCombinedLoadStates.add(it)
            }
        }
        assertThat(newCombinedLoadStates.getAllAndClear()).containsExactly(
            remoteLoadStatesOf(
                refresh = Loading,
                prepend = Loading,
                append = Loading,
                refreshLocal = Loading,
                prependLocal = Loading,
                appendLocal = Loading,
                refreshRemote = Loading,
                prependRemote = Loading,
                appendRemote = Loading,
            )
        )

        loadStateJob.cancel()
        newLoadStateJob.cancel()
    }

    @Test
    fun loadStateFlow_deduplicate() = testScope.runTest {
        val presenter = SimplePresenter()

        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        backgroundScope.launch {
            presenter.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }

        presenter.collectFrom(
            PagingData(
                flow = flowOf(
                    remoteLoadStateUpdate(
                        prependLocal = Loading,
                        appendLocal = Loading,
                    ),
                    remoteLoadStateUpdate(
                        appendLocal = Loading,
                    ),
                    // duplicate update
                    remoteLoadStateUpdate(
                        appendLocal = Loading,
                    ),
                ),
                uiReceiver = PagingData.NOOP_UI_RECEIVER,
                hintReceiver = PagingData.NOOP_HINT_RECEIVER
            )
        )
        advanceUntilIdle()
        assertThat(combinedLoadStates).containsExactly(
            remoteLoadStatesOf(
                prependLocal = Loading,
                appendLocal = Loading,
            ),
            remoteLoadStatesOf(
                appendLocal = Loading,
            )
        )
    }

    @Test
    fun loadStateFlowListeners_deduplicate() = testScope.runTest {
        val presenter = SimplePresenter()
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()

        presenter.addLoadStateListener {
            combinedLoadStates.add(it)
        }

        presenter.collectFrom(
            PagingData(
                flow = flowOf(
                    remoteLoadStateUpdate(
                        prependLocal = Loading,
                        appendLocal = Loading,
                    ),
                    remoteLoadStateUpdate(
                        appendLocal = Loading,
                    ),
                    // duplicate update
                    remoteLoadStateUpdate(
                        appendLocal = Loading,
                    ),
                ),
                uiReceiver = PagingData.NOOP_UI_RECEIVER,
                hintReceiver = PagingData.NOOP_HINT_RECEIVER
            )
        )
        advanceUntilIdle()
        assertThat(combinedLoadStates).containsExactly(
            remoteLoadStatesOf(
                prependLocal = Loading,
                appendLocal = Loading,
            ),
            remoteLoadStatesOf(
                appendLocal = Loading,
            )
        )
    }

    @Test
    fun addLoadStateListener_SynchronouslyUpdates() = testScope.runTest {
        val presenter = SimplePresenter()
        var combinedLoadStates: CombinedLoadStates? = null
        var itemCount = -1
        presenter.addLoadStateListener {
            combinedLoadStates = it
            itemCount = presenter.size
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
            pager.flow.collectLatest { presenter.collectFrom(it) }
        }

        // Initial refresh
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(10, itemCount)
        assertEquals(10, presenter.size)

        // Append
        presenter[9]
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(20, itemCount)
        assertEquals(20, presenter.size)

        // Prepend
        presenter[0]
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(30, itemCount)
        assertEquals(30, presenter.size)

        job.cancel()
    }

    @Test
    fun addLoadStateListener_hasNoInitialValue() = testScope.runTest {
        val presenter = SimplePresenter()
        val combinedLoadStateCapture = CombinedLoadStatesCapture()

        // Adding a new listener without a real value should not trigger it.
        presenter.addLoadStateListener(combinedLoadStateCapture)
        assertThat(combinedLoadStateCapture.newEvents()).isEmpty()

        // Add a real value and now the listener should trigger.
        presenter.collectFrom(
            PagingData.empty(
                sourceLoadStates = loadStates(
                    prepend = NotLoading.Complete,
                    append = NotLoading.Complete,
                )
            )
        )
        assertThat(combinedLoadStateCapture.newEvents()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )

        // Should emit real values to new listeners immediately
        val newCombinedLoadStateCapture = CombinedLoadStatesCapture()
        presenter.addLoadStateListener(newCombinedLoadStateCapture)
        assertThat(newCombinedLoadStateCapture.newEvents()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )
    }

    @Test
    fun uncaughtException() = testScope.runTest {
        val presenter = SimplePresenter()
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
        val deferred = async(Job()) {
            presenter.collectFrom(pagingData)
        }

        advanceUntilIdle()
        assertFailsWith<IllegalStateException> { deferred.await() }
    }

    @Test
    fun handledLoadResultInvalid() = testScope.runTest {
        val presenter = SimplePresenter()
        var generation = 0
        val pager = Pager(
            PagingConfig(1),
        ) {
            TestPagingSource().also {
                if (generation == 0) {
                    it.nextLoadResult = LoadResult.Invalid()
                }
                generation++
            }
        }

        val pagingData = pager.flow.first()
        val deferred = async {
            // only returns if flow is closed, or work canclled, or exception thrown
            // in this case it should cancel due LoadResult.Invalid causing collectFrom to return
            presenter.collectFrom(pagingData)
        }

        advanceUntilIdle()
        // this will return only if presenter.collectFrom returns
        deferred.await()
    }

    @Test
    fun refresh_pagingDataEvent() = refresh_pagingDataEvent(false)

    @Test
    fun refresh_pagingDataEvent_collectWithCachedIn() = refresh_pagingDataEvent(true)

    private fun refresh_pagingDataEvent(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter, _, _, _ ->
            // execute queued initial REFRESH
            advanceUntilIdle()

            val event = PageStore(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf(50, 51, 52, 53, 54, 55, 56, 57, 58),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = null,
                    )
                ),
                placeholdersBefore = 0,
                placeholdersAfter = 0,
            ) as PlaceholderPaddedList<Int>

            assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)
            assertThat(presenter.newEvents()).containsExactly(
                PagingDataEvent.Refresh(
                    previousList = PageStore.initial<Int>(null) as PlaceholderPaddedList<Int>,
                    newList = event
                )
            )

            presenter.refresh()

            // execute second REFRESH load
            advanceUntilIdle()

            // // second refresh loads from initialKey = 0 because anchorPosition/refreshKey is null
            assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
            assertThat(presenter.newEvents()).containsExactly(
                PagingDataEvent.Refresh(
                    previousList = event,
                    newList = PageStore(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffsets = intArrayOf(0),
                                data = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8),
                                hintOriginalPageOffset = 0,
                                hintOriginalIndices = null,
                            )
                        ),
                        placeholdersBefore = 0,
                        placeholdersAfter = 0,
                    ) as PlaceholderPaddedList<Int>
                )
            )
        }

    @Test
    fun append_pagingDataEvent() = append_pagingDataEvent(false)

    @Test
    fun append_pagingDataEvent_collectWithCachedIn() = append_pagingDataEvent(true)

    private fun append_pagingDataEvent(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, _, _, _ ->

            // initial REFRESH
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)

            // trigger append
            presenter[7]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 12)
            assertThat(presenter.newEvents().last()).isEqualTo(
                PagingDataEvent.Append(
                    startIndex = 9,
                    inserted = listOf(9, 10, 11),
                    newPlaceholdersAfter = 0,
                    oldPlaceholdersAfter = 0
                )
            )
        }

    @Test
    fun appendDrop_pagingDataEvent() = appendDrop_pagingDataEvent(false)

    @Test
    fun appendDrop_pagingDataEvent_collectWithCachedIn() = appendDrop_pagingDataEvent(true)

    private fun appendDrop_pagingDataEvent(collectWithCachedIn: Boolean) =
        runTest(
            collectWithCachedIn,
            initialKey = 96,
            config = PagingConfig(
                pageSize = 1,
                maxSize = 4,
                enablePlaceholders = false
            )
        ) { presenter, _, _, _ ->
            // initial REFRESH
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(96 until 99)

            // trigger append to reach max page size
            presenter[2]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(96 until 100)
            assertThat(presenter.newEvents().last()).isEqualTo(
                PagingDataEvent.Append(
                    startIndex = 3,
                    inserted = listOf(99),
                    newPlaceholdersAfter = 0,
                    oldPlaceholdersAfter = 0
                )
            )
            // trigger prepend and drop from append direction
            presenter[0]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(95 until 99)
            // drop is processed before inserts
            assertThat(presenter.newEvents().first()).isEqualTo(
                PagingDataEvent.DropAppend<Int>(
                    startIndex = 3,
                    dropCount = 1,
                    newPlaceholdersAfter = 0,
                    oldPlaceholdersAfter = 0
                )
            )
        }

    @Test
    fun prepend_pagingDataEvent() = prepend_pagingDataEvent(false)

    @Test
    fun prepend_pagingDataEvent_collectWithCachedIn() = prepend_pagingDataEvent(true)

    private fun prepend_pagingDataEvent(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter, _, _, _ ->

            // initial REFRESH
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)

            // trigger prepend
            presenter[0]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(47 until 59)
            assertThat(presenter.newEvents().last()).isEqualTo(
                PagingDataEvent.Prepend(
                    inserted = listOf(47, 48, 49),
                    newPlaceholdersBefore = 0,
                    oldPlaceholdersBefore = 0
                )
            )
        }

    @Test
    fun prependDrop_pagingDataEvent() = prependDrop_pagingDataEvent(false)

    @Test
    fun prependDrop_pagingDataEvent_collectWithCachedIn() = prependDrop_pagingDataEvent(true)

    private fun prependDrop_pagingDataEvent(collectWithCachedIn: Boolean) =
        runTest(
            collectWithCachedIn,
            initialKey = 1,
            config = PagingConfig(
                pageSize = 1,
                maxSize = 4,
                enablePlaceholders = false
            )
        ) { presenter, _, _, _ ->
            // initial REFRESH
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(1 until 4)

            // trigger prepend to reach max page size
            presenter[0]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 4)
            assertThat(presenter.newEvents().last()).isEqualTo(
                PagingDataEvent.Prepend(
                    inserted = listOf(0),
                    newPlaceholdersBefore = 0,
                    oldPlaceholdersBefore = 0
                )
            )

            // trigger append and drop from prepend direction
            presenter[3]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(1 until 5)
            // drop is processed before insert
            assertThat(presenter.newEvents().first()).isEqualTo(
                PagingDataEvent.DropPrepend<Int>(
                    dropCount = 1,
                    newPlaceholdersBefore = 0,
                    oldPlaceholdersBefore = 0
                )
            )
        }

    @Test
    fun refresh_loadStates() = refresh_loadStates(false)

    @Test
    fun refresh_loadStates_collectWithCachedIn() = refresh_loadStates(true)

    private fun refresh_loadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter,
        pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // execute queued initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )

        presenter.refresh()

        // execute second REFRESH load
        advanceUntilIdle()

        // second refresh loads from initialKey = 0 because anchorPosition/refreshKey is null
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete)
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refresh_loadStates_afterEndOfPagination() = refresh_loadStates_afterEndOfPagination(false)

    @Test
    fun refresh_loadStates_afterEndOfPagination_collectWithCachedIn() =
        refresh_loadStates_afterEndOfPagination(true)

    private fun refresh_loadStates_afterEndOfPagination(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, _, _, _ ->
        val loadStateCallbacks = mutableListOf<CombinedLoadStates>()
        presenter.addLoadStateListener {
            loadStateCallbacks.add(it)
        }
        val collectLoadStates = launch { presenter.collectLoadStates() }
        // execute initial refresh
        advanceUntilIdle()
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                refreshLocal = Loading
            ),
            localLoadStatesOf(
                refreshLocal = NotLoading(endOfPaginationReached = false),
                prependLocal = NotLoading(endOfPaginationReached = true)
            )
        )
        loadStateCallbacks.clear()
        presenter.refresh()
        // after a refresh, make sure the loading event comes in 1 piece w/ the end of pagination
        // reset
        advanceUntilIdle()
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                refreshLocal = Loading,
                prependLocal = NotLoading(endOfPaginationReached = false)
            ),
            localLoadStatesOf(
                refreshLocal = NotLoading(endOfPaginationReached = false),
                prependLocal = NotLoading(endOfPaginationReached = true)
            ),
        )
        assertThat(loadStateCallbacks).containsExactly(
            localLoadStatesOf(
                refreshLocal = Loading,
                prependLocal = NotLoading(endOfPaginationReached = false)
            ),
            localLoadStatesOf(
                refreshLocal = NotLoading(endOfPaginationReached = false),
                prependLocal = NotLoading(endOfPaginationReached = true)
            ),
        )
        collectLoadStates.cancel()
    }

    // TODO(b/195028524) the tests from here on checks the state after Invalid/Error results.
    //  Upon changes due to b/195028524, the asserts on these tests should see a new resetting
    //  LoadStateUpdate event

    @Test
    fun appendInvalid_loadStates() = appendInvalid_loadStates(false)

    @Test
    fun appendInvalid_loadStates_collectWithCachedIn() = appendInvalid_loadStates(true)

    private fun appendInvalid_loadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        // normal append
        presenter[8]

        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 12)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            localLoadStatesOf(prependLocal = NotLoading.Complete)
        )

        // do invalid append which will return LoadResult.Invalid
        presenter[11]
        pagingSources[0].nextLoadResult = LoadResult.Invalid()

        // using advanceTimeBy instead of advanceUntilIdle, otherwise this invalid APPEND + subsequent
        // REFRESH will auto run consecutively and we won't be able to assert them incrementally
        advanceTimeBy(1001)

        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
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
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(11 until 20)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun appendDrop_loadStates() = appendDrop_loadStates(false)

    @Test
    fun appendDrop_loadStates_collectWithCachedIn() = appendDrop_loadStates(true)

    private fun appendDrop_loadStates(collectWithCachedIn: Boolean) =
        runTest(
            collectWithCachedIn,
            initialKey = 96,
            config = PagingConfig(
                pageSize = 1,
                maxSize = 4,
                enablePlaceholders = false
            )
        ) { presenter, _, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(96 until 99)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            // ensure append has reached end of pagination
            localLoadStatesOf(),
        )

        // trigger append to reach max page size
        presenter[2]
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(96 until 100)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(appendLocal = Loading),
            localLoadStatesOf(appendLocal = NotLoading.Complete),
        )

        // trigger prepend and drop from append direction
        presenter[0]
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(95 until 99)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading, appendLocal = NotLoading.Complete),
            // page from the end is dropped so now appendLocal should be NotLoading.Incomplete
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(),
        )
        collectLoadStates.cancel()
    }

    @Test
    fun prependInvalid_loadStates() = prependInvalid_loadStates(false)

    @Test
    fun prependInvalid_loadStates_collectWithCachedIn() = prependInvalid_loadStates(true)

    private fun prependInvalid_loadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter,
        pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            // all local states NotLoading.Incomplete
            localLoadStatesOf(),
        )

        // normal prepend to ensure LoadStates for Page returns remains the same
        presenter[0]

        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(47 until 59)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            // all local states NotLoading.Incomplete
            localLoadStatesOf(),
        )

        // do an invalid prepend which will return LoadResult.Invalid
        presenter[0]
        pagingSources[0].nextLoadResult = LoadResult.Invalid()
        advanceTimeBy(1001)

        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            // the invalid prepend
            localLoadStatesOf(prependLocal = Loading),
            // REFRESH on new paging source. Append/Prepend local states is reset because the
            // LoadStateUpdate from refresh sends the full map of a local LoadStates which was
            // initialized as IDLE upon new Snapshot.
            localLoadStatesOf(refreshLocal = Loading),
        )

        // the LoadResult.Invalid from failed PREPEND triggers new pagingSource + initial REFRESH
        advanceUntilIdle()

        // load starts from 0 again because the provided initialKey = 50 is not multi-generational
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependDrop_loadStates() = prependDrop_loadStates(false)

    @Test
    fun prependDrop_loadStates_collectWithCachedIn() = prependDrop_loadStates(true)

    private fun prependDrop_loadStates(collectWithCachedIn: Boolean) =
        runTest(
            collectWithCachedIn,
            initialKey = 1,
            config = PagingConfig(
                pageSize = 1,
                maxSize = 4,
                enablePlaceholders = false
            )
        ) { presenter, _, _, _ ->
            val collectLoadStates = launch { presenter.collectLoadStates() }

            // initial REFRESH
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(1 until 4)
            assertThat(presenter.newCombinedLoadStates()).containsExactly(
                localLoadStatesOf(refreshLocal = Loading),
                // ensure append has reached end of pagination
                localLoadStatesOf(),
            )

            // trigger prepend to reach max page size
            presenter[0]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 4)
            assertThat(presenter.newCombinedLoadStates()).containsExactly(
                localLoadStatesOf(prependLocal = Loading),
                localLoadStatesOf(prependLocal = NotLoading.Complete),
            )

            // trigger append and drop from prepend direction
            presenter[3]
            advanceUntilIdle()

            assertThat(presenter.snapshot()).containsExactlyElementsIn(1 until 5)
            assertThat(presenter.newCombinedLoadStates()).containsExactly(
                localLoadStatesOf(prependLocal = NotLoading.Complete, appendLocal = Loading),
                // first page is dropped so now prependLocal should be NotLoading.Incomplete
                localLoadStatesOf(appendLocal = Loading),
                localLoadStatesOf(),
            )

            collectLoadStates.cancel()
        }

    @Test
    fun refreshInvalid_loadStates() = refreshInvalid_loadStates(false)

    @Test
    fun refreshInvalid_loadStates_collectWithCachedIn() = refreshInvalid_loadStates(true)

    private fun refreshInvalid_loadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter,
        pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // execute queued initial REFRESH load which will return LoadResult.Invalid()
        pagingSources[0].nextLoadResult = LoadResult.Invalid()
        advanceTimeBy(1001)

        assertThat(presenter.snapshot()).isEmpty()
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            // invalid first refresh. The second refresh state update that follows is identical to
            // this LoadStates so it gets de-duped
            localLoadStatesOf(refreshLocal = Loading),
        )

        // execute second REFRESH load
        advanceUntilIdle()

        // second refresh still loads from initialKey = 50 because anchorPosition/refreshKey is null
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
            )
        )

        collectLoadStates.cancel()
    }

    @Test
    fun appendError_retryLoadStates() = appendError_retryLoadStates(false)

    @Test
    fun appendError_retryLoadStates_collectWithCachedIn() = appendError_retryLoadStates(true)

    private fun appendError_retryLoadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)

        // append returns LoadResult.Error
        presenter[8]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = LoadState.Error(exception)
            ),
        )
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)

        // retry append
        presenter.retry()
        advanceUntilIdle()

        // make sure append success
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 12)
        // no reset
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = Loading
            ),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependError_retryLoadStates() = prependError_retryLoadStates(false)

    @Test
    fun prependError_retryLoadStates_collectWithCachedIn() = prependError_retryLoadStates(true)

    private fun prependError_retryLoadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter,
        pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )

        assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)

        // prepend returns LoadResult.Error
        presenter[0]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(prependLocal = LoadState.Error(exception)),
        )
        assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)

        // retry prepend
        presenter.retry()

        advanceUntilIdle()

        // make sure prepend success
        assertThat(presenter.snapshot()).containsExactlyElementsIn(47 until 59)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshError_retryLoadStates() = refreshError_retryLoadStates(false)

    @Test
    fun refreshError_retryLoadStates_collectWithCachedIn() = refreshError_retryLoadStates(true)

    private fun refreshError_retryLoadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial load returns LoadResult.Error
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(refreshLocal = LoadState.Error(exception)),
        )
        assertThat(presenter.snapshot()).isEmpty()

        // retry refresh
        presenter.retry()

        advanceUntilIdle()

        // refresh retry does not trigger new gen
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        // Goes directly from Error --> Loading without resetting refresh to NotLoading
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependError_refreshLoadStates() = prependError_refreshLoadStates(false)

    @Test
    fun prependError_refreshLoadStates_collectWithCachedIn() = prependError_refreshLoadStates(true)

    private fun prependError_refreshLoadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn, initialKey = 50) { presenter,
        pagingSources, _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // initial REFRESH
        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )
        assertThat(presenter.size).isEqualTo(9)
        assertThat(presenter.snapshot()).containsExactlyElementsIn(50 until 59)

        // prepend returns LoadResult.Error
        presenter[0]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(prependLocal = LoadState.Error(exception)),
        )

        // refresh() should reset local LoadStates and trigger new REFRESH
        presenter.refresh()
        advanceUntilIdle()

        // Initial load starts from 0 because initialKey is single gen.
        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            // second gen REFRESH load. The Error prepend state was automatically reset to
            // NotLoading.
            localLoadStatesOf(refreshLocal = Loading),
            // REFRESH complete
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshError_refreshLoadStates() = refreshError_refreshLoadStates(false)

    @Test
    fun refreshError_refreshLoadStates_collectWithCachedIn() = refreshError_refreshLoadStates(true)

    private fun refreshError_refreshLoadStates(collectWithCachedIn: Boolean) =
        runTest(collectWithCachedIn) { presenter, pagingSources,
        _, _ ->
        val collectLoadStates = launch { presenter.collectLoadStates() }

        // the initial load will return LoadResult.Error
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(refreshLocal = LoadState.Error(exception)),
        )
        assertThat(presenter.snapshot()).isEmpty()

        // refresh should trigger new generation
        presenter.refresh()

        advanceUntilIdle()

        assertThat(presenter.snapshot()).containsExactlyElementsIn(0 until 9)
        // Goes directly from Error --> Loading without resetting refresh to NotLoading
        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun remoteRefresh_refreshStatePersists() = testScope.runTest {
        val presenter = SimplePresenter()
        val remoteMediator = RemoteMediatorMock(loadDelay = 1500).apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        val pager = Pager(
            PagingConfig(pageSize = 3, enablePlaceholders = false),
            remoteMediator = remoteMediator,
        ) {
            TestPagingSource(loadDelay = 500, items = emptyList())
        }

        val collectLoadStates = launch { presenter.collectLoadStates() }
        val job = launch {
            pager.flow.collectLatest {
                presenter.collectFrom(it)
            }
        }
        // allow local refresh to complete but not remote refresh
        advanceTimeBy(600)

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
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
        presenter.refresh()

        // allow local refresh to complete but not remote refresh
        advanceTimeBy(600)

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
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

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
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

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
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

    @Test
    fun recollectOnNewPresenter_initialLoadStates() = testScope.runTest {
        val pager = Pager(
            config = PagingConfig(pageSize = 3, enablePlaceholders = false),
            initialKey = 50,
            pagingSourceFactory = { TestPagingSource() }
        ).flow.cachedIn(this)

        val presenter = SimplePresenter()
        backgroundScope.launch { presenter.collectLoadStates() }

        val job = launch {
            pager.collectLatest {
                presenter.collectFrom(it)
            }
        }
        advanceUntilIdle()

        assertThat(presenter.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf()
        )

        // we start a separate presenter to recollect on cached Pager.flow
        val presenter2 = SimplePresenter()
        backgroundScope.launch { presenter2.collectLoadStates() }

        val job2 = launch {
            pager.collectLatest {
                presenter2.collectFrom(it)
            }
        }
        advanceUntilIdle()

        assertThat(presenter2.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf()
        )

        job.cancel()
        job2.cancel()
        coroutineContext.cancelChildren()
    }

    @Test
    fun cachedData() {
        val data = List(50) { it }
        val cachedPagingData = createCachedPagingData(data)
        val simplePresenter = SimplePresenter(cachedPagingData)
        assertThat(simplePresenter.snapshot()).isEqualTo(data)
        assertThat(simplePresenter.size).isEqualTo(data.size)
    }

    @Test
    fun emptyCachedData() {
        val cachedPagingData = createCachedPagingData(emptyList())
        val simplePresenter = SimplePresenter(cachedPagingData)
        assertThat(simplePresenter.snapshot()).isEmpty()
        assertThat(simplePresenter.size).isEqualTo(0)
    }

    @Test
    fun cachedLoadStates() {
        val data = List(50) { it }
        val localStates = loadStates(refresh = Loading)
        val mediatorStates = loadStates()
        val cachedPagingData = createCachedPagingData(
            data = data,
            sourceLoadStates = localStates,
            mediatorLoadStates = mediatorStates
        )
        val simplePresenter = SimplePresenter(cachedPagingData)
        val expected = simplePresenter.loadStateFlow.value
        assertThat(expected).isNotNull()
        assertThat(expected!!.source).isEqualTo(localStates)
        assertThat(expected.mediator).isEqualTo(mediatorStates)
    }

    @Test
    fun cachedData_doesNotSetHintReceiver() = testScope.runTest {
        val data = List(50) { it }
        val hintReceiver = HintReceiverFake()
        val cachedPagingData = createCachedPagingData(
            data = data,
            sourceLoadStates = loadStates(refresh = Loading),
            mediatorLoadStates = null,
            hintReceiver = hintReceiver
        )
        val presenter = SimplePresenter(cachedPagingData)

        // access item
        presenter[5]
        assertThat(hintReceiver.hints).hasSize(0)

        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4)))),
        )
        val hintReceiver2 = HintReceiverFake()

        val job1 = launch {
            presenter.collectFrom(PagingData(flow, dummyUiReceiver, hintReceiver2))
        }

        // access item, hint should be sent to the first uncached PagingData
        presenter[3]
        assertThat(hintReceiver.hints).hasSize(0)
        assertThat(hintReceiver2.hints).hasSize(1)
        job1.cancel()
    }

    @Test
    fun cachedData_doesNotSetUiReceiver() = testScope.runTest {
        val data = List(50) { it }
        val uiReceiver = UiReceiverFake()
        val cachedPagingData = createCachedPagingData(
            data = data,
            sourceLoadStates = loadStates(refresh = Loading),
            mediatorLoadStates = null,
            uiReceiver = uiReceiver
        )
        val presenter = SimplePresenter(cachedPagingData)
        presenter.refresh()
        advanceUntilIdle()
        assertThat(uiReceiver.refreshEvents).hasSize(0)

        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4)))),
        )
        val uiReceiver2 = UiReceiverFake()
        val job1 = launch {
            presenter.collectFrom(PagingData(flow, uiReceiver2, dummyHintReceiver))
        }
        presenter.refresh()
        assertThat(uiReceiver.refreshEvents).hasSize(0)
        assertThat(uiReceiver2.refreshEvents).hasSize(1)
        job1.cancel()
    }

    @Test
    fun cachedData_thenRealData() = testScope.runTest {
        val data = List(2) { it }
        val cachedPagingData = createCachedPagingData(
            data = data,
            sourceLoadStates = loadStates(refresh = Loading),
            mediatorLoadStates = null,
        )
        val presenter = SimplePresenter(cachedPagingData)
        val data2 = List(10) { it }
        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(data2))),
        )
        val job1 = launch {
            presenter.collectFrom(PagingData(flow, dummyUiReceiver, dummyHintReceiver))
        }

        assertThat(presenter.snapshot()).isEqualTo(data2)
        job1.cancel()
    }

    @Test
    fun cachedData_thenLoadError() = testScope.runTest {
        val data = List(3) { it }
        val cachedPagingData = createCachedPagingData(
            data = data,
            sourceLoadStates = loadStates(refresh = Loading),
            mediatorLoadStates = null,
        )
        val presenter = SimplePresenter(cachedPagingData)

        val channel = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver = HintReceiverFake()
        val uiReceiver = UiReceiverFake()
        val job1 = launch {
            presenter.collectFrom(PagingData(channel.consumeAsFlow(), uiReceiver, hintReceiver))
        }
        val error = LoadState.Error(Exception())
        channel.trySend(
            localLoadStateUpdate(refreshLocal = error)
        )
        assertThat(presenter.nonNullLoadStateFlow.first()).isEqualTo(
            localLoadStatesOf(refreshLocal = error)
        )

        // ui receiver is set upon processing a LoadStateUpdate so we can still trigger
        // refresh/retry
        presenter.refresh()
        assertThat(uiReceiver.refreshEvents).hasSize(1)
        // but hint receiver is only set if presenter has presented a refresh from this PagingData
        // which did not happen in this case
        presenter[2]
        assertThat(hintReceiver.hints).hasSize(0)
        job1.cancel()
    }

    private fun runTest(
        collectWithCachedIn: Boolean,
        initialKey: Int? = null,
        config: PagingConfig = PagingConfig(pageSize = 3, enablePlaceholders = false),
        block: TestScope.(
            presenter: SimplePresenter,
            pagingSources: List<TestPagingSource>,
            uiReceivers: List<TrackableUiReceiverWrapper>,
            hintReceivers: List<TrackableHintReceiverWrapper>
        ) -> Unit
    ) = testScope.runTest {
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager = Pager(
            config = config,
            initialKey = initialKey,
            pagingSourceFactory = {
                TestPagingSource(
                    loadDelay = 1000,
                ).also { pagingSources.add(it) }
            }
        )
        val presenter = SimplePresenter()
        val uiReceivers = mutableListOf<TrackableUiReceiverWrapper>()
        val hintReceivers = mutableListOf<TrackableHintReceiverWrapper>()

        val collection = launch {
            pager.flow
            .map { pagingData ->
                PagingData(
                    flow = pagingData.flow,
                    uiReceiver = TrackableUiReceiverWrapper(pagingData.uiReceiver)
                        .also { uiReceivers.add(it) },
                    hintReceiver = TrackableHintReceiverWrapper(pagingData.hintReceiver)
                        .also { hintReceivers.add(it) }
                )
            }.let {
                if (collectWithCachedIn) {
                    it.cachedIn(this)
                } else {
                    it
                }
            }.collect {
                presenter.collectFrom(it)
            }
        }

        try {
            block(presenter, pagingSources, uiReceivers, hintReceivers)
        } finally {
            collection.cancel()
        }
    }
}

private fun infinitelySuspendingPagingData(
    uiReceiver: UiReceiver = dummyUiReceiver,
    hintReceiver: HintReceiver = dummyHintReceiver
) = PagingData(
    flow { emit(suspendCancellableCoroutine<PageEvent<Int>> { }) },
    uiReceiver,
    hintReceiver
)

private fun createCachedPagingData(
    data: List<Int>,
    placeholdersBefore: Int = 0,
    placeholdersAfter: Int = 0,
    uiReceiver: UiReceiver = PagingData.NOOP_UI_RECEIVER,
    hintReceiver: HintReceiver = PagingData.NOOP_HINT_RECEIVER,
    sourceLoadStates: LoadStates = LoadStates.IDLE,
    mediatorLoadStates: LoadStates? = null,
): PagingData<Int> =
    PagingData(
        flow = emptyFlow(),
        uiReceiver = uiReceiver,
        hintReceiver = hintReceiver,
        cachedPageEvent = {
            PageEvent.Insert.Refresh(
                pages = listOf(TransformablePage(0, data)),
                placeholdersBefore = placeholdersBefore,
                placeholdersAfter = placeholdersAfter,
                sourceLoadStates = sourceLoadStates,
                mediatorLoadStates = mediatorLoadStates
            )
        }
    )

private class UiReceiverFake : UiReceiver {
    val retryEvents = mutableListOf<Unit>()
    val refreshEvents = mutableListOf<Unit>()

    override fun retry() {
        retryEvents.add(Unit)
    }

    override fun refresh() {
        refreshEvents.add(Unit)
    }
}

private class HintReceiverFake : HintReceiver {
    private val _hints = mutableListOf<ViewportHint>()
    val hints: List<ViewportHint>
        get() {
            val result = _hints.toList()
            @OptIn(ExperimentalStdlibApi::class)
            repeat(result.size) { _hints.removeFirst() }
            return result
        }

    override fun accessHint(viewportHint: ViewportHint) {
        _hints.add(viewportHint)
    }
}

private class TrackableUiReceiverWrapper(
    private val receiver: UiReceiver? = null,
) : UiReceiver {
    val retryEvents = mutableListOf<Unit>()
    val refreshEvents = mutableListOf<Unit>()

    override fun retry() {
        retryEvents.add(Unit)
        receiver?.retry()
    }

    override fun refresh() {
        refreshEvents.add(Unit)
        receiver?.refresh()
    }
}

private class TrackableHintReceiverWrapper(
    private val receiver: HintReceiver? = null,
) : HintReceiver {
    private val _hints = mutableListOf<ViewportHint>()
    val hints: List<ViewportHint>
        get() {
            val result = _hints.toList()
            @OptIn(ExperimentalStdlibApi::class)
            repeat(result.size) { _hints.removeFirst() }
            return result
        }

    override fun accessHint(viewportHint: ViewportHint) {
        _hints.add(viewportHint)
        receiver?.accessHint(viewportHint)
    }
}

private class SimplePresenter(
    cachedPagingData: PagingData<Int>? = null,
) : PagingDataPresenter<Int>(
    mainContext = EmptyCoroutineContext,
    cachedPagingData = cachedPagingData
) {
    private val _localLoadStates = mutableListOf<CombinedLoadStates>()

    val nonNullLoadStateFlow = loadStateFlow.filterNotNull()

    fun newCombinedLoadStates(): List<CombinedLoadStates?> {
        val newCombinedLoadStates = _localLoadStates.toList()
        _localLoadStates.clear()
        return newCombinedLoadStates
    }

    suspend fun collectLoadStates() {
        nonNullLoadStateFlow.collect { combinedLoadStates ->
            _localLoadStates.add(combinedLoadStates)
        }
    }

    private val _pagingDataEvents = mutableListOf<PagingDataEvent<Int>>()

    fun newEvents(): List<PagingDataEvent<Int>> {
        val newEvents = _pagingDataEvents.toList()
        _pagingDataEvents.clear()
        return newEvents
    }

    override suspend fun presentPagingDataEvent(event: PagingDataEvent<Int>) {
        _pagingDataEvents.add(event)
    }
}

internal val dummyUiReceiver = object : UiReceiver {
    override fun retry() {}
    override fun refresh() {}
}

internal val dummyHintReceiver = object : HintReceiver {
    override fun accessHint(viewportHint: ViewportHint) {}
}
