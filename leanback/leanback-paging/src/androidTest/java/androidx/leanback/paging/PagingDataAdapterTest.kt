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
package androidx.leanback.paging

import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.TestPagingSource
import androidx.paging.assertEvents
import androidx.paging.localLoadStatesOf
import androidx.recyclerview.widget.DiffUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.MainDispatcherRule
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PagingDataAdapterTest {

    private val testScope = TestScope(StandardTestDispatcher())

    @get:Rule
    val dispatcherRule = MainDispatcherRule(
        testScope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    )

    /*
     * Testing get(), size()
     */
    @Test
    fun testGetItem() = testScope.runTest {
        val pagingSource = TestPagingSource()
        val pagingDataAdapter =
            PagingDataAdapter(
                diffCallback = DiffCallback,
                workerDispatcher = Dispatchers.Main
            )
        val pager = Pager(
            config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2
            ),
            initialKey = 50
        ) {
            pagingSource
        }
        val job = launch {
            pager.flow.collect {
                pagingDataAdapter.submitData(it)
            }
        }
        testScheduler.advanceUntilIdle()
        job.cancel()
        assertEquals(null, pagingDataAdapter.get(90))
        assertEquals(pagingSource.items.get(51), pagingDataAdapter.get(51))
        assertEquals(pagingSource.items.size, pagingDataAdapter.size())
    }

    /*
     * Testing loadStateListener callbacks
     */
    @Test
    fun testLoadStateListenerCallbacks() = testScope.runTest {
        val pagingDataAdapter =
            PagingDataAdapter(
                diffCallback = DiffCallback,
                workerDispatcher = Dispatchers.Main
            )
        val loadEvents = mutableListOf<CombinedLoadStates>()
        pagingDataAdapter.addLoadStateListener { loadEvents.add(it) }
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
                pagingDataAdapter.submitData(it)
            }
        }
        testScheduler.advanceUntilIdle()
        // Assert that all load state updates are sent, even when differ enters fast path for
        // empty previous list.
        assertEvents(
            listOf(
                localLoadStatesOf(refreshLocal = LoadState.Loading),
                localLoadStatesOf(
                    refreshLocal = LoadState.NotLoading(endOfPaginationReached = false)
                ),
            ),
            loadEvents
        )
        loadEvents.clear()
        job.cancel()

        pagingDataAdapter.submitData(TestLifecycleOwner().lifecycle, PagingData.empty())
        testScheduler.advanceUntilIdle()
        // Assert that all load state updates are sent, even when differ enters fast path for
        // empty next list.
        assertEvents(
            expected = listOf(
                localLoadStatesOf(
                    refreshLocal = LoadState.NotLoading(endOfPaginationReached = false),
                    prependLocal = LoadState.NotLoading(endOfPaginationReached = true),
                    appendLocal = LoadState.NotLoading(endOfPaginationReached = true)
                )
            ),
            actual = loadEvents
        )
    }

    @Test
    fun snapshot() = testScope.runTest {
        val pagingSource = TestPagingSource()
        val pagingDataAdapter =
            PagingDataAdapter(
                diffCallback = DiffCallback,
                workerDispatcher = Dispatchers.Main
            )
        val pager = Pager(
            config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2
            ),
            initialKey = 50
        ) {
            pagingSource
        }
        val job = launch {
            pager.flow.collectLatest {
                pagingDataAdapter.submitData(it)
            }
        }

        assertEquals(listOf(), pagingDataAdapter.snapshot())

        testScheduler.advanceUntilIdle()
        assertEquals(
            List(50) { null } + listOf(50, 51) + List(48) { null },
            pagingDataAdapter.snapshot()
        )

        job.cancel()
    }

    @Test
    fun peek() = testScope.runTest {
        val pagingSource = TestPagingSource()
        val pagingDataAdapter =
            PagingDataAdapter(
                diffCallback = DiffCallback,
                workerDispatcher = Dispatchers.Main
            )
        val pager = Pager(
            config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2
            ),
            initialKey = 50
        ) {
            pagingSource
        }
        val job = launch {
            pager.flow.collectLatest {
                pagingDataAdapter.submitData(it)
            }
        }

        testScheduler.advanceUntilIdle()
        assertEquals(null, pagingDataAdapter.peek(0))
        assertEquals(50, pagingDataAdapter.peek(50))
        assertEquals(null, pagingDataAdapter.peek(99))

        job.cancel()
    }
}

private object DiffCallback : DiffUtil.ItemCallback<Int>() {
    override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
        return oldItem == newItem
    }
}
