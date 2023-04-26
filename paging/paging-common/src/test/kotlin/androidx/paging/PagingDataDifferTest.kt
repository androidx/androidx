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
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@RunWith(Parameterized::class)
class PagingDataDifferTest(
    /**
     * run some tests with cached-in to ensure caching does not change behavior in the single
     * consumer cases.
     */
    private val collectWithCachedIn: Boolean
) {
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @get:Rule
    val dispatcherRule = MainDispatcherRule(
        testScope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    )

    @Test
    fun collectFrom_static() = testScope.runTest {
        withContext(coroutineContext) {
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
    fun collectFrom_twice() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        launch { differ.collectFrom(infinitelySuspendingPagingData()) }
            .cancel()
        launch { differ.collectFrom(infinitelySuspendingPagingData()) }
            .cancel()
    }

    @Test
    fun collectFrom_twiceConcurrently() = testScope.runTest {
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
    fun retry() = testScope.runTest {
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
    fun refresh() = testScope.runTest {
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
    fun uiReceiverSetImmediately() = testScope.runTest {
        val differ = SimpleDiffer(differCallback = dummyDifferCallback)
        val receiver = UiReceiverFake()
        val pagingData1 = infinitelySuspendingPagingData(uiReceiver = receiver)

        val job1 = launch {
            differ.collectFrom(pagingData1)
        }
        assertTrue(job1.isActive) // ensure job started

        assertThat(receiver.refreshEvents).hasSize(0)

        differ.refresh()
        // double check that the pagingdata's receiver was registered and had received refresh call
        // before any PageEvent is collected/presented
        assertThat(receiver.refreshEvents).hasSize(1)

        job1.cancel()
    }

    @Test
    fun hintReceiverSetAfterNewListPresented() = testScope.runTest {
        val differ = SimpleDiffer(differCallback = dummyDifferCallback)

        // first generation, load something so next gen can access index to trigger hint
        val hintReceiver1 = HintReceiverFake()
        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4)))),
        )

        val job1 = launch {
            differ.collectFrom(PagingData(flow, dummyUiReceiver, hintReceiver1))
        }
        assertThat(hintReceiver1.hints).hasSize(1) // initial hint

        // trigger second generation
        differ.refresh()

        // second generation
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver2 = HintReceiverFake()
        val job2 = launch {
            differ.collectFrom(
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver2)
            )
        }

        // we send the initial load state. this should NOT cause second gen hint receiver
        // to register
        pageEventCh.trySend(
            localLoadStateUpdate(refreshLocal = Loading)
        )
        assertThat(differ.nonNullLoadStateFlow.first()).isEqualTo(
            localLoadStatesOf(refreshLocal = Loading)
        )

        // ensure both hint receivers are idle before sending a hint
        assertThat(hintReceiver1.hints).isEmpty()
        assertThat(hintReceiver2.hints).isEmpty()

        // try sending a hint, should be sent to first receiver
        differ[4]
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
        assertThat(differ.snapshot().items).containsExactlyElementsIn(20 until 25)

        // second receiver was registered and received the initial viewport hint
        assertThat(hintReceiver1.hints).isEmpty()
        assertThat(hintReceiver2.hints).isEqualTo(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 2,
                    presentedItemsAfter = 2,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
        )

        job2.cancel()
        job1.cancel()
    }

    @Test
    fun refreshOnLatestGenerationReceiver() = runTest { differ, loadDispatcher, _,
        uiReceivers, hintReceivers ->
        // first gen
        loadDispatcher.scheduler.advanceUntilIdle()
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)

        // append a page so we can cache an anchorPosition of [8]
        differ[8]
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 12)

        // trigger gen 2, the refresh signal should to sent to gen 1
        differ.refresh()
        assertThat(uiReceivers[0].refreshEvents).hasSize(1)
        assertThat(uiReceivers[1].refreshEvents).hasSize(0)

        // trigger gen 3, refresh signal should be sent to gen 2
        differ.refresh()
        assertThat(uiReceivers[0].refreshEvents).hasSize(1)
        assertThat(uiReceivers[1].refreshEvents).hasSize(1)
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(8 until 17)

        // gen 3 receiver should be recipient of the initial hint
        assertThat(hintReceivers[2].hints).containsExactlyElementsIn(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 4,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
        )
    }

    @Test
    fun retryOnLatestGenerationReceiver() = runTest { differ, loadDispatcher, pagingSources,
        uiReceivers, hintReceivers ->

        // first gen
        loadDispatcher.scheduler.advanceUntilIdle()
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)

        // append a page so we can cache an anchorPosition of [8]
        differ[8]
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 12)

        // trigger gen 2, the refresh signal should be sent to gen 1
        differ.refresh()
        assertThat(uiReceivers[0].refreshEvents).hasSize(1)
        assertThat(uiReceivers[1].refreshEvents).hasSize(0)

        // to recreate a real use-case of retry based on load error
        pagingSources[1].errorNextLoad = true
        loadDispatcher.scheduler.advanceUntilIdle()
        // differ should still have first gen presenter
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 12)

        // retry should be sent to gen 2 even though it wasn't presented
        differ.retry()
        assertThat(uiReceivers[0].retryEvents).hasSize(0)
        assertThat(uiReceivers[1].retryEvents).hasSize(1)
        loadDispatcher.scheduler.advanceUntilIdle()

        // will retry with the correct cached hint
        assertThat(differ.snapshot()).containsExactlyElementsIn(8 until 17)

        // gen 2 receiver was recipient of the initial hint
        assertThat(hintReceivers[1].hints).containsExactlyElementsIn(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 4,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
        )
    }

    @Test
    fun refreshAfterStaticList() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        val pagingData1 = PagingData.from(listOf(1, 2, 3))
        val job1 = launch { differ.collectFrom(pagingData1) }
        assertTrue(job1.isCompleted)
        assertThat(differ.snapshot()).containsAtLeastElementsIn(listOf(1, 2, 3))

        val uiReceiver = UiReceiverFake()
        val pagingData2 = infinitelySuspendingPagingData(uiReceiver = uiReceiver)
        val job2 = launch { differ.collectFrom(pagingData2) }
        assertTrue(job2.isActive)

        // even though the second paging data never presented, it should be receiver of the refresh
        differ.refresh()
        assertThat(uiReceiver.refreshEvents).hasSize(1)

        job2.cancel()
    }

    @Test
    fun retryAfterStaticList() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        val pagingData1 = PagingData.from(listOf(1, 2, 3))
        val job1 = launch { differ.collectFrom(pagingData1) }
        assertTrue(job1.isCompleted)
        assertThat(differ.snapshot()).containsAtLeastElementsIn(listOf(1, 2, 3))

        val uiReceiver = UiReceiverFake()
        val pagingData2 = infinitelySuspendingPagingData(uiReceiver = uiReceiver)
        val job2 = launch { differ.collectFrom(pagingData2) }
        assertTrue(job2.isActive)

        // even though the second paging data never presented, it should be receiver of the retry
        differ.retry()
        assertThat(uiReceiver.retryEvents).hasSize(1)

        job2.cancel()
    }

    @Test
    fun hintCalculationBasedOnCurrentGeneration() = testScope.runTest {
        val differ = SimpleDiffer(differCallback = dummyDifferCallback)

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
            differ.collectFrom(PagingData(flow, uiReceiver1, hintReceiver1))
        }
        assertThat(hintReceiver1.hints).isEqualTo(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 2,
                    presentedItemsAfter = 2,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                ),
            )
        )

        // jump to another position, triggers invalidation
        differ[20]
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
        differ.refresh()
        assertThat(uiReceiver1.refreshEvents).hasSize(1)

        // second generation
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver2 = HintReceiverFake()
        val job2 = launch {
            differ.collectFrom(
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver2)
            )
        }

        // jump to another position while second gen is loading. It should be sent to first gen.
        differ[40]
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

        assertThat(hintReceiver2.hints).isEqualTo(
            listOf(
                ViewportHint.Initial(
                    presentedItemsBefore = 2,
                    presentedItemsAfter = 2,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
        )

        // jumping to index 50. Hint.indexInPage should be adjusted accordingly based on
        // the placeholdersBefore of new presenter. It should be
        // (index - placeholdersBefore) = 50 - 20 = 30
        differ[50]
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

        val hintReceiver = HintReceiverFake()
        val job = launch {
            differ.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver)
                    .filter { it % 2 != 0 }
            )
        }

        // Initial state:
        // [null, null, [-1], [1], [3], null, null]
        assertNull(differ[0])
        assertThat(hintReceiver.hints).isEqualTo(
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
        assertNull(differ[5])
        assertThat(hintReceiver.hints).isEqualTo(
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

        val hintReceiver = HintReceiverFake()
        val job = launch {
            differ.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver)
                    .filter { it % 2 != 0 }
            )
        }

        // Initial state:
        // [null, null, [-1], [1], [3], null, null]
        assertNull(differ[0])
        assertThat(hintReceiver.hints).isEqualTo(
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

        val hintReceiver = HintReceiverFake()
        val job = launch {
            differ.collectFrom(
                // Filter the original list of 10 items to 5, removing even numbers.
                PagingData(pageEventCh.consumeAsFlow(), dummyUiReceiver, hintReceiver)
            )
        }

        // Check that peek fetches the correct placeholder
        assertThat(differ.peek(4)).isEqualTo(0)

        // Check that peek fetches the correct placeholder
        assertNull(differ.peek(0))

        // Check that peek does not trigger page fetch.
        assertThat(hintReceiver.hints).isEqualTo(
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
    fun initialHint_emptyRefresh() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val pageEventCh = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver = HintReceiverFake()
        val job = launch {
            differ.collectFrom(
                PagingData(
                    pageEventCh.consumeAsFlow(),
                    dummyUiReceiver,
                    hintReceiver
                )
            )
        }

        pageEventCh.trySend(
            localRefresh(pages = listOf(TransformablePage(emptyList())))
        )

        assertThat(hintReceiver.hints).isEqualTo(
            listOf(ViewportHint.Initial(0, 0, 0, 0))
        )

        job.cancel()
    }

    @Test
    fun onPagingDataPresentedListener_empty() = testScope.runTest {
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
    fun onPagingDataPresentedListener_insertDrop() = testScope.runTest {
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
    fun onPagingDataPresentedFlow_empty() = testScope.runTest {
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
    fun onPagingDataPresentedFlow_insertDrop() = testScope.runTest {
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
    fun onPagingDataPresentedFlow_buffer() = testScope.runTest {
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
    fun loadStateFlow_synchronouslyUpdates() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        var combinedLoadStates: CombinedLoadStates? = null
        var itemCount = -1
        val loadStateJob = launch {
            differ.nonNullLoadStateFlow.collect {
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
    fun loadStateFlow_hasNoInitialValue() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            differ.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates).isEmpty()

        // Add a real value and now we should emit to collector.
        differ.collectFrom(
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
            differ.nonNullLoadStateFlow.collect {
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
        val differ = SimpleDiffer(dummyDifferCallback)

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            differ.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a static list without load states, which should not send anything.
        differ.collectFrom(PagingData.empty())
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a real LoadStateUpdate.
        differ.collectFrom(
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
        differ.collectFrom(PagingData.empty())
        // Existing observers should not receive any updates
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()
        // New observers should receive the previous state.
        val newCombinedLoadStates = mutableListOf<CombinedLoadStates>()
        val newLoadStateJob = launch {
            differ.nonNullLoadStateFlow.collect {
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
        val differ = SimpleDiffer(dummyDifferCallback)

        // Should not immediately emit without a real value to a new collector.
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        val loadStateJob = launch {
            differ.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a static list without load states, which should not send anything.
        differ.collectFrom(PagingData.from(listOf(1)))
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()

        // Send a real LoadStateUpdate.
        differ.collectFrom(
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
        differ.collectFrom(PagingData.from(listOf(1)))
        // Existing observers should not receive any updates
        assertThat(combinedLoadStates.getAllAndClear()).isEmpty()
        // New observers should receive the previous state.
        val newCombinedLoadStates = mutableListOf<CombinedLoadStates>()
        val newLoadStateJob = launch {
            differ.nonNullLoadStateFlow.collect {
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
        val differ = SimpleDiffer(dummyDifferCallback)

        val combinedLoadStates = mutableListOf<CombinedLoadStates>()
        backgroundScope.launch {
            differ.nonNullLoadStateFlow.collect {
                combinedLoadStates.add(it)
            }
        }

        differ.collectFrom(
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
        val differ = SimpleDiffer(dummyDifferCallback)
        val combinedLoadStates = mutableListOf<CombinedLoadStates>()

        differ.addLoadStateListener {
            combinedLoadStates.add(it)
        }

        differ.collectFrom(
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
        val differ = SimpleDiffer(dummyDifferCallback)
        withContext(coroutineContext) {
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
    fun addLoadStateListener_hasNoInitialValue() = testScope.runTest {
        val differ = SimpleDiffer(dummyDifferCallback)
        val combinedLoadStateCapture = CombinedLoadStatesCapture()

        // Adding a new listener without a real value should not trigger it.
        differ.addLoadStateListener(combinedLoadStateCapture)
        assertThat(combinedLoadStateCapture.newEvents()).isEmpty()

        // Add a real value and now the listener should trigger.
        differ.collectFrom(
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
        differ.addLoadStateListener(newCombinedLoadStateCapture)
        assertThat(newCombinedLoadStateCapture.newEvents()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
            )
        )
    }

    @Test
    fun uncaughtException() = testScope.runTest {
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
        val deferred = testScope.async(Job()) {
            differ.collectFrom(pagingData)
        }

        advanceUntilIdle()
        assertFailsWith<IllegalStateException> { deferred.await() }
    }

    @Test
    fun handledLoadResultInvalid() = testScope.runTest {
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
        pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // execute queued initial REFRESH
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )

        differ.refresh()

        // execute second REFRESH load
        loadDispatcher.scheduler.advanceUntilIdle()

        // second refresh still loads from initialKey = 50 because anchorPosition/refreshKey is null
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete)
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refresh_loadStates_afterEndOfPagination() = runTest { differ, loadDispatcher, _, _, _ ->
        val loadStateCallbacks = mutableListOf<CombinedLoadStates>()
        differ.addLoadStateListener {
            loadStateCallbacks.add(it)
        }
        val collectLoadStates = differ.collectLoadStates()
        // execute initial refresh
        loadDispatcher.scheduler.advanceUntilIdle()
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                refreshLocal = Loading
            ),
            localLoadStatesOf(
                refreshLocal = NotLoading(endOfPaginationReached = false),
                prependLocal = NotLoading(endOfPaginationReached = true)
            )
        )
        loadStateCallbacks.clear()
        differ.refresh()
        // after a refresh, make sure the loading event comes in 1 piece w/ the end of pagination
        // reset
        loadDispatcher.scheduler.advanceUntilIdle()
        assertThat(differ.newCombinedLoadStates()).containsExactly(
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
    fun appendInvalid_loadStates() = runTest { differ, loadDispatcher, pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        // normal append
        differ[8]

        loadDispatcher.scheduler.advanceUntilIdle()

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

        // using advanceTimeBy instead of advanceUntilIdle, otherwise this invalid APPEND + subsequent
        // REFRESH will auto run consecutively and we won't be able to assert them incrementally
        loadDispatcher.scheduler.advanceTimeBy(1001)

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
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(11 until 20)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun prependInvalid_loadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            // all local states NotLoading.Incomplete
            localLoadStatesOf(),
        )

        // normal prepend to ensure LoadStates for Page returns remains the same
        differ[0]

        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(47 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            // all local states NotLoading.Incomplete
            localLoadStatesOf(),
        )

        // do an invalid prepend which will return LoadResult.Invalid
        differ[0]
        pagingSources[0].nextLoadResult = LoadResult.Invalid()
        loadDispatcher.scheduler.advanceTimeBy(1001)

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
        loadDispatcher.scheduler.advanceUntilIdle()

        // load starts from 0 again because the provided initialKey = 50 is not multi-generational
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshInvalid_loadStates() = runTest(initialKey = 50) { differ, loadDispatcher,
        pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // execute queued initial REFRESH load which will return LoadResult.Invalid()
        pagingSources[0].nextLoadResult = LoadResult.Invalid()
        loadDispatcher.scheduler.advanceTimeBy(1001)

        assertThat(differ.snapshot()).isEmpty()
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            // invalid first refresh. The second refresh state update that follows is identical to
            // this LoadStates so it gets de-duped
            localLoadStatesOf(refreshLocal = Loading),
        )

        // execute second REFRESH load
        loadDispatcher.scheduler.advanceUntilIdle()

        // second refresh still loads from initialKey = 50 because anchorPosition/refreshKey is null
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(
                prependLocal = NotLoading.Complete,
            )
        )

        collectLoadStates.cancel()
    }

    @Test
    fun appendError_retryLoadStates() = runTest { differ, loadDispatcher, pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )
        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)

        // append returns LoadResult.Error
        differ[8]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.scheduler.advanceUntilIdle()

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
        loadDispatcher.scheduler.advanceUntilIdle()

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
        pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(),
        )

        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)

        // prepend returns LoadResult.Error
        differ[0]
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(prependLocal = LoadState.Error(exception)),
        )
        assertThat(differ.snapshot()).containsExactlyElementsIn(50 until 59)

        // retry prepend
        differ.retry()

        loadDispatcher.scheduler.advanceUntilIdle()

        // make sure prepend success
        assertThat(differ.snapshot()).containsExactlyElementsIn(47 until 59)
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun refreshError_retryLoadStates() = runTest { differ, loadDispatcher, pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // initial load returns LoadResult.Error
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(refreshLocal = LoadState.Error(exception)),
        )
        assertThat(differ.snapshot()).isEmpty()

        // retry refresh
        differ.retry()

        loadDispatcher.scheduler.advanceUntilIdle()

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
        pagingSources, _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // initial REFRESH
        loadDispatcher.scheduler.advanceUntilIdle()

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

        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(prependLocal = Loading),
            localLoadStatesOf(prependLocal = LoadState.Error(exception)),
        )

        // refresh() should reset local LoadStates and trigger new REFRESH
        differ.refresh()
        loadDispatcher.scheduler.advanceUntilIdle()

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
    fun refreshError_refreshLoadStates() = runTest { differ, loadDispatcher, pagingSources,
        _, _ ->
        val collectLoadStates = differ.collectLoadStates()

        // the initial load will return LoadResult.Error
        val exception = Throwable()
        pagingSources[0].nextLoadResult = LoadResult.Error(exception)

        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(refreshLocal = LoadState.Error(exception)),
        )
        assertThat(differ.snapshot()).isEmpty()

        // refresh should trigger new generation
        differ.refresh()

        loadDispatcher.scheduler.advanceUntilIdle()

        assertThat(differ.snapshot()).containsExactlyElementsIn(0 until 9)
        // Goes directly from Error --> Loading without resetting refresh to NotLoading
        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf(prependLocal = NotLoading.Complete),
        )

        collectLoadStates.cancel()
    }

    @Test
    fun remoteRefresh_refreshStatePersists() = testScope.runTest {
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

    @Test
    fun recollectOnNewDiffer_initialLoadStates() = testScope.runTest {
        val pager = Pager(
            config = PagingConfig(pageSize = 3, enablePlaceholders = false),
            initialKey = 50,
            pagingSourceFactory = { TestPagingSource() }
        ).flow.cachedIn(this)

        val differ = SimpleDiffer(
            differCallback = dummyDifferCallback,
            coroutineScope = backgroundScope,
        )
        differ.collectLoadStates()

        val job = launch {
            pager.collectLatest {
                differ.collectFrom(it)
            }
        }
        advanceUntilIdle()

        assertThat(differ.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf(refreshLocal = Loading),
            localLoadStatesOf()
        )

        // we start a separate differ to recollect on cached Pager.flow
        val differ2 = SimpleDiffer(
            differCallback = dummyDifferCallback,
            coroutineScope = backgroundScope,
        )
        differ2.collectLoadStates()

        val job2 = launch {
            pager.collectLatest {
                differ2.collectFrom(it)
            }
        }
        advanceUntilIdle()

        assertThat(differ2.newCombinedLoadStates()).containsExactly(
            localLoadStatesOf()
        )

        job.cancel()
        job2.cancel()
        testScope.coroutineContext.cancelChildren()
    }

    @Test
    fun cachedData() {
        val data = List(50) { it }
        val cachedPagingData = createCachedPagingData(data)
        val simpleDiffer = SimpleDiffer(dummyDifferCallback, cachedPagingData)
        assertThat(simpleDiffer.snapshot()).isEqualTo(data)
        assertThat(simpleDiffer.size).isEqualTo(data.size)
    }

    @Test
    fun emptyCachedData() {
        val cachedPagingData = createCachedPagingData(emptyList())
        val simpleDiffer = SimpleDiffer(dummyDifferCallback, cachedPagingData)
        assertThat(simpleDiffer.snapshot()).isEmpty()
        assertThat(simpleDiffer.size).isEqualTo(0)
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
        val simpleDiffer = SimpleDiffer(dummyDifferCallback, cachedPagingData)
        val expected = simpleDiffer.loadStateFlow.value
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
        val differ = SimpleDiffer(dummyDifferCallback, cachedPagingData)
        differ[5]
        assertThat(hintReceiver.hints).hasSize(0)

        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4)))),
        )
        val hintReceiver2 = HintReceiverFake()

        val job1 = launch {
            differ.collectFrom(PagingData(flow, dummyUiReceiver, hintReceiver2))
        }
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
        val differ = SimpleDiffer(dummyDifferCallback, cachedPagingData)
        differ.refresh()
        advanceUntilIdle()
        assertThat(uiReceiver.refreshEvents).hasSize(0)

        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(listOf(0, 1, 2, 3, 4)))),
        )
        val uiReceiver2 = UiReceiverFake()
        val job1 = launch {
            differ.collectFrom(PagingData(flow, uiReceiver2, dummyHintReceiver))
        }
        differ.refresh()
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
        val differ = SimpleDiffer(dummyDifferCallback, cachedPagingData)
        val data2 = List(10) { it }
        val flow = flowOf(
            localRefresh(pages = listOf(TransformablePage(data2))),
        )
        val job1 = launch {
            differ.collectFrom(PagingData(flow, dummyUiReceiver, dummyHintReceiver))
        }

        assertThat(differ.snapshot()).isEqualTo(data2)
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
        val differ = SimpleDiffer(dummyDifferCallback, cachedPagingData)

        val channel = Channel<PageEvent<Int>>(Channel.UNLIMITED)
        val hintReceiver = HintReceiverFake()
        val uiReceiver = UiReceiverFake()
        val job1 = launch {
            differ.collectFrom(PagingData(channel.consumeAsFlow(), uiReceiver, hintReceiver))
        }
        val error = LoadState.Error(Exception())
        channel.trySend(
            localLoadStateUpdate(refreshLocal = error)
        )
        assertThat(differ.nonNullLoadStateFlow.first()).isEqualTo(
            localLoadStatesOf(refreshLocal = error)
        )

        // ui receiver is set upon processing a LoadStateUpdate so we can still trigger
        // refresh/retry
        differ.refresh()
        assertThat(uiReceiver.refreshEvents).hasSize(1)
        // but hint receiver is only set if differ has presented a refresh from this PagingData
        // which did not happen in this case
        differ[2]
        assertThat(hintReceiver.hints).hasSize(0)
        job1.cancel()
    }

    private fun runTest(
        loadDispatcher: TestDispatcher = StandardTestDispatcher(),
        initialKey: Int? = null,
        pagingSources: MutableList<TestPagingSource> = mutableListOf(),
        pager: Pager<Int, Int> =
            Pager(
                config = PagingConfig(pageSize = 3, enablePlaceholders = false),
                initialKey = initialKey,
                pagingSourceFactory = {
                    TestPagingSource(
                        loadDelay = 1000,
                        loadContext = loadDispatcher,
                    ).also { pagingSources.add(it) }
                }
            ),
        block: (
            differ: SimpleDiffer,
            loadDispatcher: TestDispatcher,
            pagingSources: List<TestPagingSource>,
            uiReceivers: List<TrackableUiReceiverWrapper>,
            hintReceivers: List<TrackableHintReceiverWrapper>
        ) -> Unit
    ) = testScope.runTest {
        val differ = SimpleDiffer(
            differCallback = dummyDifferCallback,
            coroutineScope = this,
        )
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
                differ.collectFrom(it)
            }
        }

        try {
            block(differ, loadDispatcher, pagingSources, uiReceivers, hintReceivers)
        } finally {
            collection.cancel()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useCachedIn_{0}")
        fun params() = arrayOf(true, false)
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

@OptIn(ExperimentalCoroutinesApi::class)
private class SimpleDiffer(
    differCallback: DifferCallback,
    cachedPagingData: PagingData<Int>? = null,
    val coroutineScope: CoroutineScope = TestScope(UnconfinedTestDispatcher())
) : PagingDataDiffer<Int>(differCallback = differCallback, cachedPagingData = cachedPagingData) {
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

    val nonNullLoadStateFlow = loadStateFlow.filterNotNull()

    fun newCombinedLoadStates(): List<CombinedLoadStates?> {
        val newCombinedLoadStates = _localLoadStates.toList()
        _localLoadStates.clear()
        return newCombinedLoadStates
    }

    fun collectLoadStates(): Job {
        return coroutineScope.launch {
            nonNullLoadStateFlow.collect { combinedLoadStates ->
                _localLoadStates.add(combinedLoadStates)
            }
        }
    }
}

internal val dummyUiReceiver = object : UiReceiver {
    override fun retry() {}
    override fun refresh() {}
}

internal val dummyHintReceiver = object : HintReceiver {
    override fun accessHint(viewportHint: ViewportHint) {}
}

private val dummyDifferCallback = object : DifferCallback {
    override fun onInserted(position: Int, count: Int) {}

    override fun onChanged(position: Int, count: Int) {}

    override fun onRemoved(position: Int, count: Int) {}
}
