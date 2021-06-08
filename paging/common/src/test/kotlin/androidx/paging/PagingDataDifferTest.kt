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
