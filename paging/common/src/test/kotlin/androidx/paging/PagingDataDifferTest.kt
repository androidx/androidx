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

import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@RunWith(JUnit4::class)
class PagingDataDifferTest {
    private val testScope = TestCoroutineScope()

    @Before
    fun setup() {
        Dispatchers.setMain(
            testScope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun collectFrom_static() = testScope.runBlockingTest {
        pauseDispatcher {
            val differ = SimpleDiffer()
            val receiver = object : UiReceiver {
                val hintsAdded = mutableListOf<ViewportHint>()
                var didRetry = false
                var didRefresh = false

                override fun addHint(hint: ViewportHint) {
                    hintsAdded.add(hint)
                }

                override fun retry() {
                    didRetry = true
                }

                override fun refresh() {
                    didRefresh = true
                }
            }

            val job1 = launch {
                differ.collectFrom(infinitelySuspendingPagingData(receiver), dummyPresenterCallback)
            }
            advanceUntilIdle()
            job1.cancel()

            val job2 = launch {
                differ.collectFrom(PagingData.empty(), dummyPresenterCallback)
            }
            advanceUntilIdle()
            job2.cancel()

            // Static replacement should also replace the UiReceiver from previous generation.
            differ.retry()
            differ.refresh()
            advanceUntilIdle()

            assertFalse { receiver.didRetry }
            assertFalse { receiver.didRefresh }
        }
    }

    @Test
    fun collectFrom_twice() = testScope.runBlockingTest {
        val differ = SimpleDiffer()

        launch { differ.collectFrom(infinitelySuspendingPagingData(), dummyPresenterCallback) }
            .cancel()
        launch { differ.collectFrom(infinitelySuspendingPagingData(), dummyPresenterCallback) }
            .cancel()
    }

    @Test
    fun collectFrom_twiceConcurrently() = testScope.runBlockingTest {
        val differ = SimpleDiffer()

        val job = launch {
            differ.collectFrom(infinitelySuspendingPagingData(), dummyPresenterCallback)
        }
        assertFailsWith<IllegalStateException> {
            differ.collectFrom(infinitelySuspendingPagingData(), dummyPresenterCallback)
        }

        job.cancel()
    }

    @Test
    fun retry() = testScope.runBlockingTest {
        val differ = SimpleDiffer()
        val receiver = UiReceiverFake()

        val job = launch {
            differ.collectFrom(infinitelySuspendingPagingData(receiver), dummyPresenterCallback)
        }

        differ.retry()

        assertEquals(1, receiver.retryEvents.size)

        job.cancel()
    }

    @Test
    fun refresh() = testScope.runBlockingTest {
        val differ = SimpleDiffer()
        val receiver = UiReceiverFake()

        val job = launch {
            differ.collectFrom(infinitelySuspendingPagingData(receiver), dummyPresenterCallback)
        }

        differ.refresh()

        assertEquals(1, receiver.refreshEvents.size)

        job.cancel()
    }

    @Test
    fun listUpdateFlow() = testScope.runBlockingTest {
        val differ = SimpleDiffer()
        val pageEventFlow = flowOf<PageEvent<Int>>(
            Refresh(listOf(), 0, 0, CombinedLoadStates.IDLE_SOURCE),
            Prepend(listOf(), 0, CombinedLoadStates.IDLE_SOURCE),
            Drop(PREPEND, 0, 0),
            Refresh(listOf(TransformablePage(0, listOf(0))), 0, 0, CombinedLoadStates.IDLE_SOURCE)
        )

        val pagingData = PagingData(pageEventFlow, dummyReceiver)

        // Start collection for ListUpdates before collecting from differ to prevent conflation
        // from affecting the expected events.
        val listUpdates = mutableListOf<Unit>()
        val listUpdateJob = launch {
            differ.dataRefreshFlow.collect { listUpdates.add(it) }
        }

        val job = launch {
            differ.collectFrom(pagingData, dummyPresenterCallback)
        }

        advanceUntilIdle()
        assertThat(listUpdates)
            .isEqualTo(pageEventFlow.toListChangedEvents().toList())

        listUpdateJob.cancel()
        job.cancel()
    }

    @Test
    fun listUpdateCallback() = testScope.runBlockingTest {
        val differ = SimpleDiffer()
        val pageEventFlow = flowOf<PageEvent<Int>>(
            Refresh(listOf(), 0, 0, CombinedLoadStates.IDLE_SOURCE),
            Prepend(listOf(), 0, CombinedLoadStates.IDLE_SOURCE),
            Drop(PREPEND, 0, 0),
            Refresh(listOf(TransformablePage(0, listOf(0))), 0, 0, CombinedLoadStates.IDLE_SOURCE)
        )

        val pagingData = PagingData(pageEventFlow, dummyReceiver)

        // Start listening for ListUpdates before collecting from differ to prevent conflation
        // from affecting the expected events.
        val listUpdates = mutableListOf<Unit>()
        differ.addDataRefreshListener {
            listUpdates.add(Unit)
        }

        val job = launch {
            differ.collectFrom(pagingData, dummyPresenterCallback)
        }

        advanceUntilIdle()
        assertThat(listUpdates)
            .isEqualTo(pageEventFlow.toListChangedEvents().toList())

        job.cancel()
    }
}

private fun <T : Any> Flow<PageEvent<T>>.toListChangedEvents() = mapNotNull { event ->
    when (event) {
        is Insert -> when (event.loadType) {
            REFRESH -> Unit
            else -> null
        }
        else -> null
    }
}

private fun infinitelySuspendingPagingData(receiver: UiReceiver = dummyReceiver) =
    PagingData(
        flow { emit(suspendCancellableCoroutine<PageEvent<Int>> { }) },
        receiver
    )

private class UiReceiverFake : UiReceiver {
    val hints = mutableListOf<ViewportHint>()
    val retryEvents = mutableListOf<Unit>()
    val refreshEvents = mutableListOf<Unit>()

    override fun addHint(hint: ViewportHint) {
        hints.add(hint)
    }

    override fun retry() {
        retryEvents.add(Unit)
    }

    override fun refresh() {
        refreshEvents.add(Unit)
    }
}

private class SimpleDiffer : PagingDataDiffer<Int>() {
    override suspend fun performDiff(
        previousList: NullPaddedList<Int>,
        newList: NullPaddedList<Int>,
        newCombinedLoadStates: CombinedLoadStates,
        lastAccessedIndex: Int
    ): Int? = null
}

internal val dummyReceiver = object : UiReceiver {
    override fun addHint(hint: ViewportHint) {}

    override fun retry() {}

    override fun refresh() {}
}

private val dummyPresenterCallback = object : PresenterCallback {
    override fun onInserted(position: Int, count: Int) {}

    override fun onChanged(position: Int, count: Int) {}

    override fun onRemoved(position: Int, count: Int) {}

    override fun onStateUpdate(loadType: LoadType, fromMediator: Boolean, loadState: LoadState) {}
}
