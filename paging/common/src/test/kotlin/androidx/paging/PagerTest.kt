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
import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.TestPagingSource.Companion.LOAD_ERROR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@RunWith(JUnit4::class)
class PagerTest {
    private val testScope = TestCoroutineScope()
    private val retryCh = ConflatedBroadcastChannel<Unit>()
    private val pagingSourceFactory = { TestPagingSource() }
    private val config = PagingConfig(
        pageSize = 1,
        prefetchDistance = 1,
        enablePlaceholders = true,
        initialLoadSize = 2,
        maxSize = 3
    )

    @Test
    fun loadStates_prependDone() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 1, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(1..2),
            LoadStateUpdate(START, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(1..2),
            LoadStateUpdate(START, Loading),
            createPrepend(pageOffset = -1, range = 0..0, startState = Done),
            LoadStateUpdate(END, Loading),
            Drop(START, 1, 1),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 97..98),
            LoadStateUpdate(END, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 97..98),
            LoadStateUpdate(END, Loading),
            createAppend(pageOffset = 1, range = 99..99, startState = Idle, endState = Done),
            LoadStateUpdate(START, Loading),
            Drop(END, 1, 1),
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
            LoadStateUpdate(REFRESH, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 50..51),
            LoadStateUpdate(START, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 50..51),
            LoadStateUpdate(START, Loading),
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
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(range = 50..51),
                LoadStateUpdate(START, Loading),
                createPrepend(pageOffset = -1, range = 49..49),
                LoadStateUpdate(START, Loading),
                Drop(END, 1, 50),
                createPrepend(pageOffset = -2, range = 48..48)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependAndSkipDrop_prefetchWindow() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(
                pagingSourceFactory = pagingSourceFactory,
                initialKey = 50,
                config = PagingConfig(
                    pageSize = 1,
                    prefetchDistance = 2,
                    enablePlaceholders = true,
                    initialLoadSize = 5,
                    maxSize = 5
                )
            )
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(range = 50..54),
                LoadStateUpdate(START, Loading),
                createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
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
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(range = 50..51),
                LoadStateUpdate(START, Loading),
                createPrepend(pageOffset = -1, range = 49..49),
                LoadStateUpdate(START, Loading),
                LoadStateUpdate(END, Loading),
                Drop(END, 1, 50),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(50..52),
            LoadStateUpdate(START, Loading),
            createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(50..51),
            LoadStateUpdate(END, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(50..52),
            LoadStateUpdate(END, Loading),
            createAppend(1, 53..53, startState = Idle, endState = Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 50..51),
            LoadStateUpdate(END, Loading),
            createAppend(pageOffset = 1, range = 52..52),
            LoadStateUpdate(END, Loading),
            Drop(START, 1, 52),
            createAppend(pageOffset = 2, range = 53..53)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun appendAndSkipDrop_prefetchWindow() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(
                pagingSourceFactory = pagingSourceFactory,
                initialKey = 50,
                config = PagingConfig(
                    pageSize = 1,
                    prefetchDistance = 2,
                    enablePlaceholders = true,
                    initialLoadSize = 5,
                    maxSize = 5
                )
            )
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 4))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(range = 50..54),
                LoadStateUpdate(END, Loading),
                createAppend(pageOffset = 1, range = 55..55, endState = Loading),
                createAppend(pageOffset = 2, range = 56..56)
            )

            assertEvents(expected, fetcherState.pageEventLists[0])
            fetcherState.job.cancel()
        }
    }

    @Test
    fun appendAndDropWithCancellation() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 1))
            advanceUntilIdle()
            // Start hint processing until load starts, but hasn't finished.
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(1, 0))
            advanceTimeBy(500)
            fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 0))
            advanceUntilIdle()

            val expected: List<PageEvent<Int>> = listOf(
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(range = 50..51),
                LoadStateUpdate(END, Loading),
                createAppend(pageOffset = 1, range = 52..52),
                LoadStateUpdate(END, Loading),
                LoadStateUpdate(START, Loading),
                Drop(START, 1, 52),
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
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(50..51)
            ),
            listOf(
                LoadStateUpdate(REFRESH, Loading),
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
                LoadStateUpdate(REFRESH, Loading),
                createRefresh(50..51),
                LoadStateUpdate(END, Loading),
                createAppend(1, 52..52)
            ),
            listOf(
                LoadStateUpdate(REFRESH, Loading),
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
    fun close_cancelsCollectionBeforeInitialLoad() = testScope.runBlockingTest {
        // Infinitely suspending PagingSource which never finishes loading anything.
        val pagingSource = object : PagingSource<Int, Int>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
                delay(2000)
                fail("Should never get here")
            }
        }
        val pager = Pager(50, pagingSource, config, retryFlow = retryCh.asFlow())

        collectPagerData(pager) { _, job ->

            // Start the initial load, but do not let it finish.
            advanceTimeBy(500)

            // Close pager, then advance time by enough to allow initial load to finish.
            pager.close()
            advanceTimeBy(1500)

            assertTrue { !job.isActive }
        }
    }

    @Test
    fun retry() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { pageEvents, _ ->
                val expected = listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, Loading),
                    createRefresh(50..51),
                    LoadStateUpdate(END, Loading),
                    LoadStateUpdate(END, Error(LOAD_ERROR)),
                    LoadStateUpdate(END, Loading),
                    createAppend(1, 52..52)
                )

                advanceUntilIdle()
                pageSource.errorNextLoad = true
                pager.addHint(ViewportHint(0, 1))
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()

                assertEvents(expected, pageEvents)
            }
        }
    }

    @Test
    fun retryNothing() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { pageEvents, _ ->
                val expected = listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, Loading),
                    createRefresh(50..51),
                    LoadStateUpdate(END, Loading),
                    createAppend(1, 52..52)
                )

                advanceUntilIdle()
                pager.addHint(ViewportHint(0, 1))
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()

                assertEvents(expected, pageEvents)
            }
        }
    }

    @Test
    fun retryTwice() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { pageEvents, _ ->
                val expected = listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, Loading),
                    createRefresh(50..51),
                    LoadStateUpdate(END, Loading),
                    LoadStateUpdate(END, Error(LOAD_ERROR)),
                    LoadStateUpdate(END, Loading),
                    createAppend(1, 52..52)
                )

                advanceUntilIdle()
                pageSource.errorNextLoad = true
                pager.addHint(ViewportHint(0, 1))
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()

                assertEvents(expected, pageEvents)
            }
        }
    }

    @Test
    fun retryBothDirections() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { pageEvents, _ ->
                val expected = listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, Loading),
                    createRefresh(50..51),
                    LoadStateUpdate(END, Loading),
                    LoadStateUpdate(END, Error(LOAD_ERROR)),
                    LoadStateUpdate(START, Loading),
                    LoadStateUpdate(START, Error(LOAD_ERROR)),
                    LoadStateUpdate(START, Loading),
                    LoadStateUpdate(END, Loading),
                    createPrepend(
                        pageOffset = -1, range = 49..49, startState = Idle, endState = Loading
                    ),
                    Drop(START, 1, 50),
                    createAppend(1, 52..52)
                )

                advanceUntilIdle()
                pageSource.errorNextLoad = true
                pager.addHint(ViewportHint(0, 1))
                advanceUntilIdle()
                pageSource.errorNextLoad = true
                pager.addHint(ViewportHint(0, 0))
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()

                assertEvents(expected, pageEvents)
            }
        }
    }

    @Test
    fun retryRefresh() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { pageEvents, _ ->
                val expected = listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, Loading),
                    LoadStateUpdate(REFRESH, Error(LOAD_ERROR)),
                    LoadStateUpdate(REFRESH, Loading),
                    createRefresh(50..51)
                )

                pageSource.errorNextLoad = true
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()

                assertEvents(expected, pageEvents)
            }
        }
    }

    @Test
    fun retryRefreshWithBufferedHint() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = Pager(50, pageSource, config, retryFlow = retryCh.asFlow())
            collectPagerData(pager) { pageEvents, _ ->
                val expected = listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, Loading),
                    LoadStateUpdate(REFRESH, Error(LOAD_ERROR)),
                    LoadStateUpdate(REFRESH, Loading),
                    createRefresh(50..51),
                    LoadStateUpdate(START, Loading),
                    createPrepend(pageOffset = -1, range = 49..49)
                )

                pageSource.errorNextLoad = true
                advanceUntilIdle()
                pager.addHint(ViewportHint(0, 0))
                advanceUntilIdle()
                retryCh.offer(Unit)
                advanceUntilIdle()

                assertEvents(expected, pageEvents)
            }
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
            LoadStateUpdate(REFRESH, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.loadStates) },
            LoadStateUpdate(START, Loading),
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
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.loadStates) },
            LoadStateUpdate(END, Loading),
            createAppend(1, 52..52).let { End(it.pages, 0, it.loadStates) }
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun neverDropBelowTwoPages() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1,
            enablePlaceholders = true,
            initialLoadSize = 3,
            maxSize = 3
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        fetcherState.pagingDataList[0].receiver.addHint(ViewportHint(0, 2))
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            LoadStateUpdate(REFRESH, Loading),
            createRefresh(range = 50..52),
            LoadStateUpdate(END, Loading),
            createAppend(pageOffset = 1, range = 53..53)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun refreshKeyInfo_nullHint() = testScope.runBlockingTest {
        val pagingSource = pagingSourceFactory()
        val pager = Pager(50, pagingSource, config, retryFlow = retryCh.asFlow())
        assertNull(pager.refreshKeyInfo())
    }

    @Test
    fun refreshKeyInfo_pagesEmpty() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = pagingSourceFactory()
            val pager = Pager(50, pagingSource, config, retryFlow = retryCh.asFlow())
            pager.addHint(ViewportHint(0, 0))
            assertNull(pager.refreshKeyInfo())
        }
    }

    @Test
    fun refreshKeyInfo_loadedIndex() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = pagingSourceFactory()
            val pager = Pager(50, pagingSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { _, _ ->
                advanceUntilIdle()

                pager.addHint(ViewportHint(0, 1))

                val refreshKeyInfo = pager.refreshKeyInfo()
                assertNotNull(refreshKeyInfo)
                assertEquals(51, refreshKeyInfo.anchorPosition)

                // Assert from anchorPosition in placeholdersStart
                assertEquals(50, refreshKeyInfo.closestItemToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(50, refreshKeyInfo.closestItemToPosition(50))
                assertEquals(51, refreshKeyInfo.closestItemToPosition(51))
                // Assert from anchorPosition in placeholdersEnd
                assertEquals(51, refreshKeyInfo.closestItemToPosition(90))

                val loadedPage = Page(
                    data = listOf(50, 51),
                    prevKey = 49,
                    nextKey = 52,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                assertEquals(listOf(loadedPage), refreshKeyInfo.pages)
                // Assert from anchorPosition in placeholdersStart
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(50))
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(51))
                // Assert from anchorPosition in placeholdersEnd
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(90))
            }
        }
    }

    @Test
    fun refreshKeyInfo_placeholdersStart() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = pagingSourceFactory()
            val pager = Pager(50, pagingSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { _, _ ->
                advanceUntilIdle()

                pager.addHint(ViewportHint(0, -40))

                val refreshKeyInfo = pager.refreshKeyInfo()
                assertNotNull(refreshKeyInfo)
                assertEquals(10, refreshKeyInfo.anchorPosition)
                assertEquals(
                    listOf(
                        Page(
                            data = listOf(50, 51),
                            prevKey = 49,
                            nextKey = 52,
                            itemsBefore = 50,
                            itemsAfter = 48
                        )
                    ),
                    refreshKeyInfo.pages
                )

                // Assert from anchorPosition in placeholdersStart
                assertEquals(50, refreshKeyInfo.closestItemToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(50, refreshKeyInfo.closestItemToPosition(50))
                assertEquals(51, refreshKeyInfo.closestItemToPosition(51))
                // Assert from anchorPosition in placeholdersEnd
                assertEquals(51, refreshKeyInfo.closestItemToPosition(90))

                val loadedPage = Page(
                    data = listOf(50, 51),
                    prevKey = 49,
                    nextKey = 52,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                // Assert from anchorPosition in placeholdersStart
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(50))
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(51))
                // Assert from anchorPosition in placeholdersEnd
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(90))
            }
        }
    }

    @Test
    fun retry_ignoresNewSignalsWhileProcessing() = testScope.runBlockingTest {
        val pagingSource = pagingSourceFactory()
        val pager = Pager(50, pagingSource, config, retryFlow = retryCh.asFlow())
        collectPagerData(pager) { pageEvents, _ ->
            pagingSource.errorNextLoad = true
            advanceUntilIdle()

            pagingSource.errorNextLoad = true
            retryCh.offer(Unit)
            // Should be ignored by pager as it's still processing previous retry.
            retryCh.offer(Unit)
            advanceUntilIdle()

            assertEvents(
                listOf(
                    LoadStateUpdate(REFRESH, Loading),
                    LoadStateUpdate(REFRESH, Error(LOAD_ERROR)),
                    LoadStateUpdate(REFRESH, Loading),
                    LoadStateUpdate(REFRESH, Error(LOAD_ERROR))
                ),
                pageEvents
            )
        }
    }

    @Test
    fun remoteMediator_initialLoadTriggersPrepend() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock()
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = Pager(
            initialKey = 0,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { _, _ ->
            advanceUntilIdle()

            assertEquals(1, remoteMediator.loadEvents.size)
            assertEquals(START, remoteMediator.loadEvents[0].loaddType)
            assertNotNull(remoteMediator.loadEvents[0].state)
        }
    }

    @Test
    fun remoteMediator_initialLoadTriggersAppend() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock()
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = Pager(
            initialKey = 99,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { _, _ ->
            advanceUntilIdle()

            assertEquals(1, remoteMediator.loadEvents.size)
            assertEquals(END, remoteMediator.loadEvents[0].loaddType)
            assertNotNull(remoteMediator.loadEvents[0].state)
        }
    }

    @Test
    fun jump() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagingConfig(
                pageSize = 1,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 3,
                jumpThreshold = 10
            )
            var didJump = false
            val pager = Pager(50, pagingSourceFactory(), config, retryFlow = retryCh.asFlow()) {
                didJump = true
            }
            // Trigger collection on flow to init jump detection job.
            val job = launch { pager.pageEventFlow.collect { } }

            advanceUntilIdle()

            pager.addHint(ViewportHint(0, -50))
            advanceUntilIdle()

            assertTrue { didJump }

            job.cancel()
        }
    }

    @Test
    fun jump_requiresPagingSourceOptIn() {
        assertFailsWith<IllegalArgumentException> {
            Pager(
                initialKey = 50,
                pagingSource = TestPagingSource(jumpingSupported = false),
                config = PagingConfig(pageSize = 1, prefetchDistance = 1, jumpThreshold = 1),
                retryFlow = retryCh.asFlow()
            )
        }
    }
}

@Suppress("SuspendFunctionOnCoroutineScope")
internal suspend fun <T : Any> CoroutineScope.collectPagerData(
    pager: Pager<*, T>,
    block: suspend (pageEvents: ArrayList<PageEvent<T>>, job: Job) -> Unit
) {
    val pageEvents = ArrayList<PageEvent<T>>()
    val job: Job = launch { pager.pageEventFlow.collect { pageEvents.add(it) } }
    block(pageEvents, job)
    job.cancel()
}
