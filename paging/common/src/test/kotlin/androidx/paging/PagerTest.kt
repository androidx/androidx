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

import androidx.paging.LoadState.Idle
import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import androidx.paging.PageEvent.StateUpdate
import androidx.paging.TestPagedSource.Companion.items
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
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
    private val pagedSourceFactory = { TestPagedSource() }
    private val config = PagedList.Config(
        pageSize = 1,
        prefetchDistance = 1,
        enablePlaceholders = true,
        initialLoadSizeHint = 2,
        maxSize = 3
    )

    private fun pages(
        pageOffset: Int,
        range: IntRange
    ) = listOf(
        TransformablePage(
            originalPageOffset = pageOffset,
            data = items.slice(range),
            sourcePageSize = range.count(),
            originalIndices = null
        )
    )

    private fun createRefresh(range: IntRange) = Insert.Refresh(
        pages = pages(0, range),
        placeholdersStart = range.first.coerceAtLeast(0),
        placeholdersEnd = (items.size - range.last - 1).coerceAtLeast(0)
    )

    private fun createPrepend(pageOffset: Int, range: IntRange) = Insert.Start(
        pages = pages(pageOffset, range),
        placeholdersStart = range.first.coerceAtLeast(0)
    )

    private fun createAppend(pageOffset: Int, range: IntRange) = Insert.End(
        pages = pages(pageOffset, range),
        placeholdersEnd = (items.size - range.last - 1).coerceAtLeast(0)
    )

    @Test
    fun initialize() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            createRefresh(50..51),
            StateUpdate(REFRESH, Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun prepend() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            createRefresh(50..51),
            StateUpdate(REFRESH, Idle),
            StateUpdate(START, Loading),
            createPrepend(-1, 49..49),
            StateUpdate(START, Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun prependMultiplePages() = testScope.runBlockingTest {
        val config = PagedList.Config(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSizeHint = 3,
            maxSize = 5
        )
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            createRefresh(50..52),
            StateUpdate(REFRESH, Idle),
            StateUpdate(START, Loading),
            createPrepend(-1, 49..49),
            createPrepend(-2, 48..48),
            StateUpdate(START, Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun append() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            createRefresh(50..51),
            StateUpdate(REFRESH, Idle),
            StateUpdate(END, Loading),
            createAppend(1, 52..52),
            StateUpdate(END, Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendMultiplePages() = testScope.runBlockingTest {
        val config = PagedList.Config(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSizeHint = 3,
            maxSize = 5
        )
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 2))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            createRefresh(50..52),
            StateUpdate(REFRESH, Idle),
            StateUpdate(END, Loading),
            createAppend(1, 53..53),
            createAppend(2, 54..54),
            StateUpdate(END, Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendAndDrop() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 1))
        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(1, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            StateUpdate(REFRESH, Loading),
            createRefresh(50..51),
            StateUpdate(REFRESH, Idle),
            StateUpdate(END, Loading),
            createAppend(1, 52..52),
            StateUpdate(END, Idle),
            StateUpdate(END, Loading),
            createAppend(2, 53..53),
            Drop(START, 1, 52),
            StateUpdate(END, Idle)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendAndDropWithCancellation() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 1))
            advanceUntilIdle()
            fetcherState.pagedDataList[0].hintReceiver(ViewportHint(1, 0))
            advanceTimeBy(500)
            fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 0))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                StateUpdate(REFRESH, Loading),
                createRefresh(50..51),
                StateUpdate(REFRESH, Idle),
                StateUpdate(END, Loading),
                createAppend(1, 52..52),
                StateUpdate(END, Idle),
                StateUpdate(END, Loading),
                StateUpdate(START, Loading),
                createAppend(2, 53..53),
                Drop(START, 1, 52),
                StateUpdate(END, Idle),
                StateUpdate(START, Idle)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    // TODO: Start and end loads should happen at the same time for the same hint!
    @Test
    fun competingPrependAndAppendWithDropping() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagedList.Config(
                pageSize = 1,
                prefetchDistance = 2,
                enablePlaceholders = true,
                initialLoadSizeHint = 1,
                maxSize = 3
            )
            val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 0))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                StateUpdate(REFRESH, Loading),
                createRefresh(50..50),
                StateUpdate(REFRESH, Idle),
                StateUpdate(START, Loading),
                StateUpdate(END, Loading),
                createPrepend(-1, 49..49),
                createAppend(1, 51..51),
                createPrepend(-2, 48..48),
                Drop(END, 1, 49),
                StateUpdate(START, Idle),
                StateUpdate(END, Idle)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    @Test
    fun invalidateNoScroll() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<List<PageEvent<Int>>> = listOf(
            listOf(
                StateUpdate(REFRESH, Loading),
                createRefresh(50..51),
                StateUpdate(REFRESH, Idle)
            ),
            listOf(
                StateUpdate(REFRESH, Loading),
                createRefresh(50..51),
                StateUpdate(REFRESH, Idle)
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
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagedDataList[0].hintReceiver(ViewportHint(0, 1))
        advanceUntilIdle()

        val expected: List<List<PageEvent<Int>>> = listOf(
            listOf(
                StateUpdate(REFRESH, Loading),
                createRefresh(50..51),
                StateUpdate(REFRESH, Idle),
                StateUpdate(END, Loading),
                createAppend(1, 52..52),
                StateUpdate(END, Idle)
            ),
            listOf(
                StateUpdate(REFRESH, Loading),
                createRefresh(51..52),
                StateUpdate(REFRESH, Idle)
            )
        )

        assertEvents(expected[0], fetcherState.pageEventLists[0])

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEvents(expected[1], fetcherState.pageEventLists[1])
        fetcherState.job.cancel()
    }
}
