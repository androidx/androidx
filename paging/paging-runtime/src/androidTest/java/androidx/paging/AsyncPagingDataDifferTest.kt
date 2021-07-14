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

import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.paging.DiffingChangePayload.ITEM_TO_PLACEHOLDER
import androidx.paging.ListUpdateEvent.Changed
import androidx.paging.ListUpdateEvent.Inserted
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

sealed class ListUpdateEvent {
    data class Changed(val position: Int, val count: Int, val payload: Any?) : ListUpdateEvent()

    data class Moved(val fromPosition: Int, val toPosition: Int) : ListUpdateEvent()

    data class Inserted(val position: Int, val count: Int) : ListUpdateEvent()

    data class Removed(val position: Int, val count: Int) : ListUpdateEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AsyncPagingDataDifferTest {
    private val testScope = TestCoroutineScope()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(
        testScope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    )

    private val listUpdateCapture = ListUpdateCapture()
    private val differ = AsyncPagingDataDiffer(
        diffCallback = object : DiffUtil.ItemCallback<Int>() {
            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }
        },
        updateCallback = listUpdateCapture,
        workerDispatcher = Dispatchers.Main
    )

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun performDiff_fastPathLoadStates() = testScope.runBlockingTest {
        val loadEvents = mutableListOf<CombinedLoadStates>()
        differ.addLoadStateListener { loadEvents.add(it) }

        val pager = Pager(
            config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2
            ),
            initialKey = 50
        ) {
            TestPagingSource()
        }

        val job = launch {
            pager.flow.collect {
                differ.submitData(it)
            }
        }

        advanceUntilIdle()

        // Assert that all load state updates are sent, even when differ enters fast path for
        // empty previous list.
        assertEvents(
            listOf(
                localLoadStatesOf(),
                localLoadStatesOf(refreshLocal = Loading),
                localLoadStatesOf(refreshLocal = NotLoading(endOfPaginationReached = false)),
            ),
            loadEvents
        )
        loadEvents.clear()

        job.cancel()

        differ.submitData(TestLifecycleOwner().lifecycle, PagingData.empty())
        advanceUntilIdle()

        // Assert that all load state updates are sent, even when differ enters fast path for
        // empty next list.
        assertEvents(
            expected = listOf(
                localLoadStatesOf(
                    refreshLocal = NotLoading(endOfPaginationReached = false),
                    prependLocal = NotLoading(endOfPaginationReached = true),
                    appendLocal = NotLoading(endOfPaginationReached = true),
                ),
            ),
            actual = loadEvents
        )
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun performDiff_fastPathLoadStatesFlow() = testScope.runBlockingTest {
        val loadEvents = mutableListOf<CombinedLoadStates>()
        val loadEventJob = launch {
            differ.loadStateFlow.collect { loadEvents.add(it) }
        }

        val pager = Pager(
            config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2
            ),
            initialKey = 50
        ) {
            TestPagingSource()
        }

        val job = launch {
            pager.flow.collect {
                differ.submitData(it)
            }
        }

        advanceUntilIdle()

        // Assert that all load state updates are sent, even when differ enters fast path for
        // empty previous list.
        assertEvents(
            listOf(
                localLoadStatesOf(),
                localLoadStatesOf(refreshLocal = Loading),
                localLoadStatesOf(refreshLocal = NotLoading(endOfPaginationReached = false)),
            ),
            loadEvents
        )
        loadEvents.clear()

        job.cancel()

        differ.submitData(TestLifecycleOwner().lifecycle, PagingData.empty())
        advanceUntilIdle()

        // Assert that all load state updates are sent, even when differ enters fast path for
        // empty next list.
        assertEvents(
            expected = listOf(
                localLoadStatesOf(
                    refreshLocal = NotLoading(endOfPaginationReached = false),
                    prependLocal = NotLoading(endOfPaginationReached = true),
                    appendLocal = NotLoading(endOfPaginationReached = true),
                ),
            ),
            actual = loadEvents
        )

        loadEventJob.cancel()
    }

    @Test
    fun lastAccessedIndex() = testScope.runBlockingTest {
        pauseDispatcher {
            var currentPagedSource: TestPagingSource? = null
            val pager = Pager(
                config = PagingConfig(
                    pageSize = 1,
                    prefetchDistance = 1,
                    enablePlaceholders = true,
                    initialLoadSize = 2
                ),
                initialKey = 50
            ) {
                currentPagedSource = TestPagingSource()
                currentPagedSource!!
            }

            val job = launch { pager.flow.collectLatest { differ.submitData(it) } }

            // Load REFRESH [50, 51]
            advanceUntilIdle()

            assertEvents(
                listOf(
                    Inserted(0, 100), // [(50 placeholders), 50, 51, (48 placeholders)]
                ),
                listUpdateCapture.newEvents()
            )

            // Load APPEND [52] to fulfill prefetch distance
            differ.getItem(51)
            advanceUntilIdle()

            assertEvents(
                // TODO(b/182510751): Every change event here should have payload.
                listOf(
                    Changed(52, 1, null), // [(50 placeholders), 50, 51, 52, (47 placeholders)]
                ),
                listUpdateCapture.newEvents()
            )

            // Load REFRESH [51, 52]
            // Load PREPEND [50] to fulfill prefetch distance of transformed index
            currentPagedSource!!.invalidate()
            advanceUntilIdle()

            assertEvents(
                // TODO(b/182510751): Every change event here should have payload.
                listOf(
                    // refresh
                    Changed(50, 1, ITEM_TO_PLACEHOLDER), // 50 got unloaded
                    // fix prefetch, 50 got reloaded
                    Changed(50, 1, null), // [(50 placeholders), 50, 51, 52, (47 placeholders)]
                ),
                listUpdateCapture.newEvents()
            )

            job.cancel()
        }
    }

    @Test
    fun presentData_cancelsLastSubmit() = testScope.runBlockingTest {
        pauseDispatcher {
            val pager = Pager(
                config = PagingConfig(2),
                initialKey = 50
            ) { TestPagingSource() }
            val pager2 = Pager(
                config = PagingConfig(2),
                initialKey = 50
            ) { TestPagingSource() }

            val lifecycle = TestLifecycleOwner()
            var jobSubmitted = false
            val job = launch {
                pager.flow.collectLatest {
                    differ.submitData(lifecycle.lifecycle, it)
                    jobSubmitted = true
                }
            }

            advanceUntilIdle()

            val job2 = launch {
                pager2.flow.collectLatest {
                    differ.submitData(it)
                }
            }

            advanceUntilIdle()

            assertTrue(jobSubmitted)

            job.cancel()
            job2.cancel()
        }
    }

    @Test
    fun submitData_cancelsLast() = testScope.runBlockingTest {
        pauseDispatcher {
            val pager = Pager(
                config = PagingConfig(2),
                initialKey = 50
            ) { TestPagingSource() }
            val pager2 = Pager(
                config = PagingConfig(2),
                initialKey = 50
            ) { TestPagingSource() }

            val lifecycle = TestLifecycleOwner()
            var jobSubmitted = false
            val job = launch {
                pager.flow.collectLatest {
                    differ.submitData(lifecycle.lifecycle, it)
                    jobSubmitted = true
                }
            }

            advanceUntilIdle()

            var job2Submitted = false
            val job2 = launch {
                pager2.flow.collectLatest {
                    differ.submitData(lifecycle.lifecycle, it)
                    job2Submitted = true
                }
            }

            advanceUntilIdle()

            assertTrue(jobSubmitted)
            assertTrue(job2Submitted)

            job.cancel()
            job2.cancel()
        }
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun submitData_guaranteesOrder() = testScope.runBlockingTest {
        val pager = Pager(config = PagingConfig(2, enablePlaceholders = false), initialKey = 50) {
            TestPagingSource()
        }

        val reversedDispatcher = object : CoroutineDispatcher() {
            var lastBlock: Runnable? = null
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                // Save the first block to be dispatched, then run second one first after receiving
                // calls to dispatch both.
                val lastBlock = lastBlock
                if (lastBlock == null) {
                    this.lastBlock = block
                } else {
                    block.run()
                    lastBlock.run()
                }
            }
        }

        val lifecycle = TestLifecycleOwner()
        differ.submitData(lifecycle.lifecycle, PagingData.empty())
        differ.submitData(lifecycle.lifecycle, pager.flow.first()) // Loads 6 items

        // Ensure the second call wins when dispatched in order of execution.
        advanceUntilIdle()
        assertEquals(6, differ.itemCount)

        val reversedLifecycle = TestLifecycleOwner(coroutineDispatcher = reversedDispatcher)
        differ.submitData(reversedLifecycle.lifecycle, PagingData.empty())
        differ.submitData(reversedLifecycle.lifecycle, pager.flow.first()) // Loads 6 items

        // Ensure the second call wins when dispatched in reverse order of execution.
        advanceUntilIdle()
        assertEquals(6, differ.itemCount)
    }

    @Test
    fun submitData_cancelsLastSuspendSubmit() = testScope.runBlockingTest {
        pauseDispatcher {
            val pager = Pager(
                config = PagingConfig(2),
                initialKey = 50
            ) { TestPagingSource() }
            val pager2 = Pager(
                config = PagingConfig(2),
                initialKey = 50
            ) { TestPagingSource() }

            val lifecycle = TestLifecycleOwner()
            var jobSubmitted = false
            val job = launch {
                pager.flow.collectLatest {
                    jobSubmitted = true
                    differ.submitData(it)
                }
            }

            advanceUntilIdle()

            var job2Submitted = false
            val job2 = launch {
                pager2.flow.collectLatest {
                    job2Submitted = true
                    differ.submitData(lifecycle.lifecycle, it)
                }
            }

            advanceUntilIdle()

            assertTrue(jobSubmitted)
            assertTrue(job2Submitted)

            job.cancel()
            job2.cancel()
        }
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun submitData_doesNotCancelCollectionsCoroutine() = testScope.runBlockingTest {
        lateinit var source1: TestPagingSource
        lateinit var source2: TestPagingSource
        val pager = Pager(
            config = PagingConfig(
                pageSize = 5, enablePlaceholders = false, prefetchDistance = 1,
                initialLoadSize = 17
            ),
            initialKey = 50
        ) {
            TestPagingSource().also {
                source1 = it
            }
        }
        val pager2 = Pager(
            config = PagingConfig(
                pageSize = 7, enablePlaceholders = false, prefetchDistance = 1,
                initialLoadSize = 19
            ),
            initialKey = 50
        ) {
            TestPagingSource().also {
                source2 = it
            }
        }
        val job1 = launch {
            pager.flow.collectLatest(differ::submitData)
        }
        advanceUntilIdle()
        assertEquals(17, differ.itemCount)
        val job2 = launch {
            pager2.flow.collectLatest(differ::submitData)
        }
        advanceUntilIdle()
        assertEquals(26, differ.itemCount)

        // now if pager1 gets an invalidation, it overrides pager2
        source1.invalidate()
        advanceUntilIdle()
        assertEquals(22, differ.itemCount)

        // now if we refresh via differ, it should go into source 1
        differ.refresh()
        advanceUntilIdle()
        assertEquals(22, differ.itemCount)

        // now manual set data that'll clear both
        differ.submitData(PagingData.empty())
        advanceUntilIdle()
        assertEquals(0, differ.itemCount)

        // if source2 has new value, we reconnect to that
        source2.invalidate()
        advanceUntilIdle()
        assertEquals(19, differ.itemCount)

        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @SdkSuppress(minSdkVersion = 21) // b/189492631
    @Test
    fun loadStateFlowSynchronouslyUpdates() = testScope.runBlockingTest {
        var combinedLoadStates: CombinedLoadStates? = null
        var itemCount = -1
        val loadStateJob = launch {
            differ.loadStateFlow.collect {
                combinedLoadStates = it
                itemCount = differ.itemCount
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
            pager.flow.collectLatest { differ.submitData(it) }
        }

        // Initial refresh
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(10, itemCount)
        assertEquals(10, differ.itemCount)

        // Append
        differ.getItem(9)
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(20, itemCount)
        assertEquals(20, differ.itemCount)

        // Prepend
        differ.getItem(0)
        advanceUntilIdle()
        assertEquals(localLoadStatesOf(), combinedLoadStates)
        assertEquals(30, itemCount)
        assertEquals(30, differ.itemCount)

        job.cancel()
        loadStateJob.cancel()
    }

    @Test
    fun loadStateListenerSynchronouslyUpdates() = testScope.runBlockingTest {
        pauseDispatcher {
            var combinedLoadStates: CombinedLoadStates? = null
            var itemCount = -1
            differ.addLoadStateListener {
                combinedLoadStates = it
                itemCount = differ.itemCount
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
                pager.flow.collectLatest { differ.submitData(it) }
            }

            // Initial refresh
            advanceUntilIdle()
            assertEquals(localLoadStatesOf(), combinedLoadStates)
            assertEquals(10, itemCount)
            assertEquals(10, differ.itemCount)

            // Append
            differ.getItem(9)
            advanceUntilIdle()
            assertEquals(localLoadStatesOf(), combinedLoadStates)
            assertEquals(20, itemCount)
            assertEquals(20, differ.itemCount)

            // Prepend
            differ.getItem(0)
            advanceUntilIdle()
            assertEquals(localLoadStatesOf(), combinedLoadStates)
            assertEquals(30, itemCount)
            assertEquals(30, differ.itemCount)

            job.cancel()
        }
    }

    @Test
    fun listUpdateCallbackSynchronouslyUpdates() = testScope.runBlockingTest {
        pauseDispatcher {
            // Keep track of .snapshot() result within each ListUpdateCallback
            val initialSnapshot: ItemSnapshotList<Int> = ItemSnapshotList(0, 0, emptyList())
            var onInsertedSnapshot = initialSnapshot
            var onRemovedSnapshot = initialSnapshot

            val listUpdateCallback = object : ListUpdateCallback {
                lateinit var differ: AsyncPagingDataDiffer<Int>

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    // TODO: Trigger this callback so we can assert state at this point as well
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    // TODO: Trigger this callback so we can assert state at this point as well
                }

                override fun onInserted(position: Int, count: Int) {
                    onInsertedSnapshot = differ.snapshot()
                }

                override fun onRemoved(position: Int, count: Int) {
                    onRemovedSnapshot = differ.snapshot()
                }
            }

            val differ = AsyncPagingDataDiffer(
                diffCallback = object : DiffUtil.ItemCallback<Int>() {
                    override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                        return oldItem == newItem
                    }

                    override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                        return oldItem == newItem
                    }
                },
                updateCallback = listUpdateCallback,
                mainDispatcher = Dispatchers.Main,
                workerDispatcher = Dispatchers.Main,
            ).also {
                listUpdateCallback.differ = it
            }

            // Initial insert; this only triggers onInserted
            differ.submitData(PagingData.from(listOf(0)))
            advanceUntilIdle()

            val firstList = ItemSnapshotList(0, 0, listOf(0))
            assertEquals(firstList, differ.snapshot())
            assertEquals(firstList, onInsertedSnapshot)
            assertEquals(initialSnapshot, onRemovedSnapshot)

            // Switch item to 1; this triggers onInserted + onRemoved
            differ.submitData(PagingData.from(listOf(1)))
            advanceUntilIdle()

            val secondList = ItemSnapshotList(0, 0, listOf(1))
            assertEquals(secondList, differ.snapshot())
            assertEquals(secondList, onInsertedSnapshot)
            assertEquals(secondList, onRemovedSnapshot)
        }
    }
}