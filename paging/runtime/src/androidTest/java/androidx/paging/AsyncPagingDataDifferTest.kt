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
import androidx.paging.ListUpdateEvent.Changed
import androidx.paging.ListUpdateEvent.Inserted
import androidx.paging.ListUpdateEvent.Moved
import androidx.paging.ListUpdateEvent.Removed
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.REFRESH
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.filters.SmallTest
import androidx.testutils.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertTrue

private class ListUpdateCapture : ListUpdateCallback {
    val events = mutableListOf<ListUpdateEvent>()

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        events.add(Changed(position, count, payload))
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        events.add(Moved(fromPosition, toPosition))
    }

    override fun onInserted(position: Int, count: Int) {
        events.add(Inserted(position, count))
    }

    override fun onRemoved(position: Int, count: Int) {
        events.add(Removed(position, count))
    }
}

private sealed class ListUpdateEvent {
    data class Changed(val position: Int, val count: Int, val payload: Any?) : ListUpdateEvent()

    data class Moved(val fromPosition: Int, val toPosition: Int) : ListUpdateEvent()

    data class Inserted(val position: Int, val count: Int) : ListUpdateEvent()

    data class Removed(val position: Int, val count: Int) : ListUpdateEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
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
                REFRESH to Loading,
                REFRESH to NotLoading(endOfPaginationReached = false)
            ).toCombinedLoadStatesLocal(),
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
                    appendLocal = NotLoading(endOfPaginationReached = false)
                ),
                localLoadStatesOf(
                    refreshLocal = NotLoading(endOfPaginationReached = false),
                    prependLocal = NotLoading(endOfPaginationReached = true),
                    appendLocal = NotLoading(endOfPaginationReached = true)
                )
            ),
            actual = loadEvents
        )
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

            // Load END [52] to fulfill prefetch distance
            differ.getItem(51)
            advanceUntilIdle()

            // Load REFRESH [51, 52]
            // Load START [50] to fulfill prefetch distance of transformed index
            currentPagedSource!!.invalidate()
            advanceUntilIdle()

            val expected = listOf(
                Inserted(0, 100), // [(50 placeholders), 50, 51, (48 placeholders)]
                Changed(52, 1, null), // [(50 placeholders), 50, 51, 52, (47 placeholders)]
                Inserted(53, 0), // ignored
                Inserted(0, 1), // [(51 placeholders), 50, 51, 52, (47 placeholders)]
                Removed(51, 1), // [(51 placeholders), 51, 52, (47 placeholders)]
                Changed(50, 1, null), // [(50 placeholders), 50, 51, 52, (47 placeholders)]
                Inserted(0, 0) // ignored
            )

            assertEvents(expected, listUpdateCapture.events)

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
}