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

import androidx.paging.LoadState.Done
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Idle
import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.End
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.Insert.Companion.Start
import androidx.paging.PageEvent.StateUpdate
import androidx.paging.TestPagingSource.Companion.LOAD_ERROR
import androidx.paging.TestPagingSource.Companion.items
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@FlowPreview
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(JUnit4::class)
class PagerTest {
    private val testScope = TestCoroutineScope()
    private val pagingSourceFactory = { TestPagingSource() }
    private val config = PagingConfig(
        pageSize = 1,
        prefetchDistance = 1,
        enablePlaceholders = true,
        initialLoadSize = 2,
        maxSize = 3
    )

    private fun pages(
        pageOffset: Int,
        range: IntRange
    ) = listOf(
        TransformablePage(
            originalPageOffset = pageOffset,
            data = items.slice(range),
            originalPageSize = range.count(),
            originalIndices = null
        )
    )

    private fun createRefresh(
        range: IntRange,
        startState: LoadState = Idle,
        endState: LoadState = Idle
    ) = Refresh(
        pages = pages(0, range),
        placeholdersStart = range.first.coerceAtLeast(0),
        placeholdersEnd = (items.size - range.last - 1).coerceAtLeast(0),
        loadStates = mapOf(REFRESH to Idle, START to startState, END to endState)
    )

    private fun createPrepend(
        pageOffset: Int,
        range: IntRange,
        startState: LoadState = Idle,
        endState: LoadState = Idle
    ) = Start(
        pages = pages(pageOffset, range),
        placeholdersStart = range.first.coerceAtLeast(0),
        loadStates = mapOf(REFRESH to Idle, START to startState, END to endState)
    )

    private fun createAppend(
        pageOffset: Int,
        range: IntRange,
        startState: LoadState = Idle,
        endState: LoadState = Idle
    ) = End(
        pages = pages(pageOffset, range),
        placeholdersEnd = (items.size - range.last - 1).coerceAtLeast(0),
        loadStates = mapOf(REFRESH to Idle, START to startState, END to endState)
    )

    @Test
    fun loadStates_prependDone() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 1, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(1..2),
            StateUpdate(START, Loading),
            StateUpdate(START, Done),
            createPrepend(pageOffset = -1, range = 0..0, startState = Done)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_prependDoneThenDrop() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 1, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(1..2),
            StateUpdate(START, Loading),
            StateUpdate(START, Done),
            createPrepend(pageOffset = -1, range = 0..0, startState = Done),
            StateUpdate(START, Idle),
            StateUpdate(END, Loading),
            Drop(START, 1, 1),
            StateUpdate(END, Idle),
            createAppend(pageOffset = 1, range = 3..3, startState = Idle, endState = Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_appendDone() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 97..98),
            StateUpdate(END, Loading),
            StateUpdate(END, Done),
            createAppend(pageOffset = 1, range = 99..99, endState = Done)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_appendDoneThenDrop() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 97..98),
            StateUpdate(END, Loading),
            StateUpdate(END, Done),
            createAppend(pageOffset = 1, range = 99..99, startState = Idle, endState = Done),
            StateUpdate(START, Loading),
            StateUpdate(END, Idle),
            Drop(END, 1, 1),
            StateUpdate(START, Idle),
            createPrepend(pageOffset = -1, range = 96..96, startState = Idle, endState = Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_refreshStart() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 0, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            StateUpdate(START, Done),
            createRefresh(range = 0..1, startState = Done, endState = Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_refreshEnd() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 98, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            StateUpdate(END, Done),
            createRefresh(range = 98..99, startState = Idle, endState = Done)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun initialize() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun initialize_bufferedHint() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51),
            StateUpdate(START, Loading),
            StateUpdate(START, Idle),
            createPrepend(pageOffset = -1, range = 49..49)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun prepend() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51),
            StateUpdate(START, Loading),
            StateUpdate(START, Idle),
            createPrepend(pageOffset = -1, range = 49..49)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun prependAndDrop() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(-1, 0))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(range = 50..51),
                StateUpdate(START, Loading),
                StateUpdate(START, Idle),
                createPrepend(pageOffset = -1, range = 49..49),
                StateUpdate(START, Loading),
                Drop(END, 1, 50),
                StateUpdate(START, Idle),
                createPrepend(pageOffset = -2, range = 48..48)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependAndDropWithCancellation() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(-1, 0))
            // Start hint processing until load starts, but hasn't finished.
            advanceTimeBy(500)
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(range = 50..51),
                StateUpdate(START, Loading),
                StateUpdate(START, Idle),
                createPrepend(pageOffset = -1, range = 49..49),
                StateUpdate(START, Loading),
                StateUpdate(END, Loading),
                StateUpdate(END, Idle),
                Drop(END, 1, 50),
                StateUpdate(START, Idle),
                createPrepend(pageOffset = -2, range = 48..48)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependMultiplePages() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 3,
            maxSize = 5
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(50..52),
            StateUpdate(START, Loading),
            createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
            StateUpdate(START, Idle),
            createPrepend(-2, 48..48)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun append() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(50..51),
            StateUpdate(END, Loading),
            StateUpdate(END, Idle),
            createAppend(1, 52..52)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendMultiplePages() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 3,
            maxSize = 5
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 2))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(50..52),
            StateUpdate(END, Loading),
            createAppend(1, 53..53, startState = Idle, endState = Loading),
            StateUpdate(END, Idle),
            createAppend(2, 54..54)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendAndDrop() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(1, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51),
            StateUpdate(END, Loading),
            StateUpdate(END, Idle),
            createAppend(pageOffset = 1, range = 52..52),
            StateUpdate(END, Loading),
            Drop(START, 1, 52),
            StateUpdate(END, Idle),
            createAppend(pageOffset = 2, range = 53..53)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendAndDropWithCancellation() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(1, 0))
            // Start hint processing until load starts, but hasn't finished.
            advanceTimeBy(500)
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(range = 50..51),
                StateUpdate(END, Loading),
                StateUpdate(END, Idle),
                createAppend(pageOffset = 1, range = 52..52),
                StateUpdate(END, Loading),
                StateUpdate(START, Loading),
                StateUpdate(START, Idle),
                Drop(START, 1, 52),
                StateUpdate(END, Idle),
                createAppend(pageOffset = 2, range = 53..53, startState = Idle, endState = Idle)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    @Test
    fun invalidateNoScroll() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<List<PageEvent<Int>>> = listOf(
            listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51)
            ),
            listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51)
            )
        )

        assertEvents(expected[0], fetcherState.pageEventLists[0])

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEvents(expected[1], fetcherState.pageEventLists[1])
        fetcherState.job.cancel()
    }

    @Test
    fun invalidateAfterScroll() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<List<PageEvent<Int>>> = listOf(
            listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51),
                StateUpdate(END, Loading),
                StateUpdate(END, Idle),
                createAppend(1, 52..52)
            ),
            listOf(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(51..52)
            )
        )

        assertEvents(expected[0], fetcherState.pageEventLists[0])

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEvents(expected[1], fetcherState.pageEventLists[1])
        fetcherState.job.cancel()
    }

    @Test
    fun retry() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config)

            val pageEvents = ArrayList<PageEvent<Int>>()
            val job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }

            val expected = listOf<PageEvent<Int>>(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51),
                StateUpdate(END, Loading),
                StateUpdate(END, Error(LOAD_ERROR)),
                StateUpdate(END, Loading),
                StateUpdate(END, Idle),
                createAppend(1, 52..52)
            )

            advanceUntilIdle()
            pageSource.errorNextLoad = true
            pager.addHint(ViewportHint(0, 1))
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()

            assertEvents(expected, pageEvents)
            job.cancel()
        }
    }

    @Test
    fun retryNothing() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config)

            val pageEvents = ArrayList<PageEvent<Int>>()
            val job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }

            val expected = listOf<PageEvent<Int>>(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51),
                StateUpdate(END, Loading),
                StateUpdate(END, Idle),
                createAppend(1, 52..52)
            )

            advanceUntilIdle()
            pager.addHint(ViewportHint(0, 1))
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()

            assertEvents(expected, pageEvents)
            job.cancel()
        }
    }

    @Test
    fun retryTwice() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config)

            val pageEvents = ArrayList<PageEvent<Int>>()
            val job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }

            val expected = listOf<PageEvent<Int>>(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51),
                StateUpdate(END, Loading),
                StateUpdate(END, Error(LOAD_ERROR)),
                StateUpdate(END, Loading),
                StateUpdate(END, Idle),
                createAppend(1, 52..52)
            )

            advanceUntilIdle()
            pageSource.errorNextLoad = true
            pager.addHint(ViewportHint(0, 1))
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()

            assertEvents(expected, pageEvents)
            job.cancel()
        }
    }

    @Test
    fun retryBothDirections() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config)

            val pageEvents = ArrayList<PageEvent<Int>>()
            val job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }

            val expected = listOf<PageEvent<Int>>(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51),
                StateUpdate(END, Loading),
                StateUpdate(END, Error(LOAD_ERROR)),
                StateUpdate(START, Loading),
                StateUpdate(START, Error(LOAD_ERROR)),
                StateUpdate(START, Loading),
                StateUpdate(END, Loading),
                StateUpdate(START, Idle),
                createPrepend(
                    pageOffset = -1, range = 49..49, startState = Idle, endState = Loading
                ),
                Drop(START, 1, 50),
                StateUpdate(END, Idle),
                createAppend(1, 52..52)
            )

            advanceUntilIdle()
            pageSource.errorNextLoad = true
            pager.addHint(ViewportHint(0, 1))
            advanceUntilIdle()
            pageSource.errorNextLoad = true
            pager.addHint(ViewportHint(0, 0))
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()

            assertEvents(expected, pageEvents)
            job.cancel()
        }
    }

    @Test
    fun retryRefresh() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config)

            val pageEvents = ArrayList<PageEvent<Int>>()
            val job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }

            val expected = listOf<PageEvent<Int>>(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Error(LOAD_ERROR)),
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51)
            )

            pageSource.errorNextLoad = true
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()

            assertEvents(expected, pageEvents)
            job.cancel()
        }
    }

    @Test
    fun retryRefreshWithBufferedHint() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config)

            val pageEvents = ArrayList<PageEvent<Int>>()
            val job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }

            val expected = listOf<PageEvent<Int>>(
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Error(LOAD_ERROR)),
                StateUpdate(REFRESH, Loading),
                StateUpdate(REFRESH, Idle),
                createRefresh(50..51),
                StateUpdate(START, Loading),
                StateUpdate(START, Idle),
                createPrepend(pageOffset = -1, range = 49..49)
            )

            pageSource.errorNextLoad = true
            advanceUntilIdle()
            pager.addHint(ViewportHint(0, 0))
            advanceUntilIdle()
            pager.retry()
            advanceUntilIdle()

            assertEvents(expected, pageEvents)
            job.cancel()
        }
    }

    @Test
    fun disablePlaceholders_refresh() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1,
            enablePlaceholders = false,
            initialLoadSize = 2,
            maxSize = 3
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.loadStates) }
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun disablePlaceholders_prepend() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1,
            enablePlaceholders = false,
            initialLoadSize = 2,
            maxSize = 3
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.loadStates) },
            StateUpdate(START, Loading),
            StateUpdate(START, Idle),
            createPrepend(-1, 49..49).let { Start(it.pages, 0, it.loadStates) }
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun disablePlaceholders_append() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1,
            enablePlaceholders = false,
            initialLoadSize = 2,
            maxSize = 3
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            StateUpdate(REFRESH, Idle),
            createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.loadStates) },
            StateUpdate(END, Loading),
            StateUpdate(END, Idle),
            createAppend(1, 52..52).let { End(it.pages, 0, it.loadStates) }
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }
}
