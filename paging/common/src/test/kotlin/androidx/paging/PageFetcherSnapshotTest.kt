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

import androidx.paging.ContiguousPagedListTest.Companion.EXCEPTION
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.TestPagingSource.Companion.LOAD_ERROR
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@RunWith(JUnit4::class)
class PageFetcherSnapshotTest {
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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(1..2)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(PREPEND, false, Loading),
                createPrepend(
                    pageOffset = -1,
                    range = 0..0,
                    startState = NotLoading.Complete
                )
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_prependDoneThenDrop() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 1, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(1..2)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(PREPEND, false, Loading),
                createPrepend(
                    pageOffset = -1,
                    range = 0..0,
                    startState = NotLoading.Complete
                )
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = -1,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                Drop(
                    loadType = PREPEND,
                    minPageOffset = -1,
                    maxPageOffset = -1,
                    placeholdersRemaining = 1
                ),
                createAppend(
                    pageOffset = 1,
                    range = 3..3,
                    startState = NotLoading.Incomplete,
                    endState = NotLoading.Incomplete
                )
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_appendDone() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 97..98)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(pageOffset = 1, range = 99..99, endState = NotLoading.Complete)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_appendDoneThenDrop() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 97..98)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(
                    pageOffset = 1,
                    range = 99..99,
                    startState = NotLoading.Incomplete,
                    endState = NotLoading.Complete
                )
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 1
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(PREPEND, false, Loading),
                Drop(
                    loadType = APPEND,
                    minPageOffset = 1,
                    maxPageOffset = 1,
                    placeholdersRemaining = 1
                ),
                createPrepend(
                    pageOffset = -1,
                    range = 96..96,
                    startState = NotLoading.Incomplete,
                    endState = NotLoading.Incomplete
                )
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_refreshStart() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 0, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            LoadStateUpdate(REFRESH, false, Loading),
            createRefresh(
                range = 0..1,
                startState = NotLoading.Complete,
                endState = NotLoading.Incomplete
            )
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
            LoadStateUpdate(REFRESH, false, Loading),
            createRefresh(
                range = 98..99,
                startState = NotLoading.Incomplete,
                endState = NotLoading.Complete
            )
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
            LoadStateUpdate(REFRESH, false, Loading),
            createRefresh(range = 50..51)
        )

        assertEvents(expected, fetcherState.pageEventLists[0])
        fetcherState.job.cancel()
    }

    @Test
    fun initialize_bufferedHint() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()

        val expected: List<PageEvent<Int>> = listOf(
            LoadStateUpdate(REFRESH, false, Loading),
            createRefresh(range = 50..51),
            LoadStateUpdate(PREPEND, false, Loading),
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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 50..51)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(PREPEND, false, Loading),
                createPrepend(pageOffset = -1, range = 49..49)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun prependAndDrop() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 4
            )
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            // Make sure the job didn't complete exceptionally
            assertFalse { fetcherState.job.isCancelled }
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    createRefresh(range = 50..51)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertFalse { fetcherState.job.isCancelled }
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(PREPEND, false, Loading),
                    createPrepend(pageOffset = -1, range = 48..49)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = -1,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 3,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertFalse { fetcherState.job.isCancelled }
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(PREPEND, false, Loading),
                    Drop(
                        loadType = APPEND,
                        minPageOffset = 0,
                        maxPageOffset = 0,
                        placeholdersRemaining = 50
                    ),
                    createPrepend(pageOffset = -2, range = 46..47)
                )
            )

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
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(loadType = REFRESH, fromMediator = false, loadState = Loading),
                    createRefresh(range = 50..54)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(loadType = PREPEND, fromMediator = false, loadState = Loading),
                    createPrepend(
                        pageOffset = -1,
                        range = 49..49,
                        startState = Loading
                    ),
                    createPrepend(pageOffset = -2, range = 48..48)
                )
            )

            // Make sure the job didn't complete exceptionally
            assertFalse { fetcherState.job.isCancelled }

            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependAndDropWithCancellation() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 4
            )
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    createRefresh(range = 50..51)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(PREPEND, false, Loading),
                    createPrepend(pageOffset = -1, range = 48..49)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = -1,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 3,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            // Start hint processing until load starts, but hasn't finished.
            advanceTimeBy(500)
            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 1,
                    presentedItemsBefore = 3,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(PREPEND, false, Loading),
                    LoadStateUpdate(APPEND, false, Loading),
                    Drop(
                        loadType = APPEND,
                        minPageOffset = 0,
                        maxPageOffset = 0,
                        placeholdersRemaining = 50
                    ),
                    createPrepend(pageOffset = -2, range = 46..47)
                )
            )

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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(50..52)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(PREPEND, false, Loading),
                createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
                createPrepend(pageOffset = -2, range = 48..48)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun prepend_viewportHintPrioritizesGenerationId() = testScope.runBlockingTest {
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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = REFRESH, fromMediator = false, loadState = Loading),
                createRefresh(range = 50..52)
            )
        )

        // PREPEND a few pages.
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = PREPEND, fromMediator = false, loadState = Loading),
                createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
                createPrepend(pageOffset = -2, range = 48..48)
            )
        )

        // APPEND a few pages causing PREPEND pages to drop
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 4,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = -2,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = APPEND, fromMediator = false, loadState = Loading),
                Drop(
                    loadType = PREPEND,
                    minPageOffset = -2,
                    maxPageOffset = -2,
                    placeholdersRemaining = 49
                ),
                createAppend(pageOffset = 1, range = 53..53, endState = Loading),
                Drop(
                    loadType = PREPEND,
                    minPageOffset = -1,
                    maxPageOffset = -1,
                    placeholdersRemaining = 50
                ),
                createAppend(pageOffset = 2, range = 54..54)
            )
        )

        // PREPEND a page, this hint would normally be ignored, but has a newer generationId.
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 3,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 2
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = PREPEND, fromMediator = false, loadState = Loading),
                Drop(
                    loadType = APPEND,
                    minPageOffset = 2,
                    maxPageOffset = 2,
                    placeholdersRemaining = 46
                ),
                createPrepend(pageOffset = -1, range = 49..49)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun append() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(50..51)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(1, 52..52)
            )
        )

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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(50..52)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(
                    pageOffset = 1,
                    range = 53..53,
                    startState = NotLoading.Incomplete,
                    endState = Loading
                ),
                createAppend(2, 54..54)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun appendAndDrop() = testScope.runBlockingTest {
        val config = PagingConfig(
            pageSize = 2,
            prefetchDistance = 1,
            enablePlaceholders = true,
            initialLoadSize = 2,
            maxSize = 4
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 50..51)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(pageOffset = 1, range = 52..53)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 1,
                indexInPage = 1,
                presentedItemsBefore = 3,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 1
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                Drop(
                    loadType = PREPEND,
                    minPageOffset = 0,
                    maxPageOffset = 0,
                    placeholdersRemaining = 52
                ),
                createAppend(pageOffset = 2, range = 54..55)
            )
        )

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
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    createRefresh(range = 50..54)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 4,
                    presentedItemsBefore = 4,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(APPEND, false, Loading),
                    createAppend(
                        pageOffset = 1,
                        range = 55..55,
                        endState = Loading
                    ),
                    createAppend(pageOffset = 2, range = 56..56)
                )
            )

            fetcherState.job.cancel()
        }
    }

    @Test
    fun appendAndDropWithCancellation() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagingConfig(
                pageSize = 2,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 4
            )
            val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
            val fetcherState = collectFetcherState(pageFetcher)

            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    createRefresh(range = 50..51)
                )
            )

            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 1,
                    presentedItemsBefore = 1,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(APPEND, false, Loading),
                    createAppend(pageOffset = 1, range = 52..53)
                )
            )

            // Start hint processing until load starts, but hasn't finished.
            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 1,
                    indexInPage = 1,
                    presentedItemsBefore = 3,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 1
                )
            )
            advanceTimeBy(500)
            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 3,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 1
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(APPEND, false, Loading),
                    LoadStateUpdate(PREPEND, false, Loading),
                    Drop(
                        loadType = PREPEND,
                        minPageOffset = 0,
                        maxPageOffset = 0,
                        placeholdersRemaining = 52
                    ),
                    createAppend(
                        pageOffset = 2,
                        range = 54..55,
                        startState = NotLoading.Incomplete,
                        endState = NotLoading.Incomplete
                    )
                )
            )

            fetcherState.job.cancel()
        }
    }

    @Test
    fun append_viewportHintPrioritizesGenerationId() = testScope.runBlockingTest {
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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = REFRESH, fromMediator = false, loadState = Loading),
                createRefresh(range = 50..52)
            )
        )

        // APPEND a few pages.
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = APPEND, fromMediator = false, loadState = Loading),
                createAppend(pageOffset = 1, range = 53..53, endState = Loading),
                createAppend(pageOffset = 2, range = 54..54)
            )
        )

        // PREPEND a few pages causing APPEND pages to drop
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 4,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 2
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = PREPEND, fromMediator = false, loadState = Loading),
                Drop(
                    loadType = APPEND,
                    minPageOffset = 2,
                    maxPageOffset = 2,
                    placeholdersRemaining = 46
                ),
                createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
                Drop(
                    loadType = APPEND,
                    minPageOffset = 1,
                    maxPageOffset = 1,
                    placeholdersRemaining = 47
                ),
                createPrepend(pageOffset = -2, range = 48..48)
            )
        )

        // APPEND a page, this hint would normally be ignored, but has a newer generationId.
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 3,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = -2,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(loadType = APPEND, fromMediator = false, loadState = Loading),
                Drop(
                    loadType = PREPEND,
                    minPageOffset = -2,
                    maxPageOffset = -2,
                    placeholdersRemaining = 49
                ),
                createAppend(pageOffset = 1, range = 53..53)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun invalidateNoScroll() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(50..51)
            )
        )

        pageFetcher.refresh()
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(50..51)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun invalidateAfterScroll() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(50..51)
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(1, 52..52)
            )
        )

        pageFetcher.refresh()
        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(51..52)
            )
        )

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
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryCh.asFlow())

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
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { state, _ ->
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createRefresh(range = 50..51)
                    )
                )

                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Error(LOAD_ERROR)
                        )
                    )
                )

                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createAppend(pageOffset = 1, range = 52..52)
                    )
                )
            }
        }
    }

    @Test
    fun retryNothing() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { state, _ ->

                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createRefresh(range = 50..51)
                    )
                )

                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createAppend(pageOffset = 1, range = 52..52)
                    )
                )
                retryCh.offer(Unit)
                advanceUntilIdle()
                assertTrue { state.newEvents().isEmpty() }
            }
        }
    }

    @Test
    fun retryTwice() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { state, _ ->

                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createRefresh(range = 50..51)
                    )
                )
                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Error(LOAD_ERROR)
                        )
                    )
                )
                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createAppend(pageOffset = 1, range = 52..52)
                    )
                )
                retryCh.offer(Unit)
                advanceUntilIdle()
                assertTrue { state.newEvents().isEmpty() }
            }
        }
    }

    @Test
    fun retryBothDirections() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagingConfig(
                pageSize = 1,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 4
            )
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { state, _ ->
                // Initial REFRESH
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createRefresh(range = 50..51)
                    )
                )

                // Failed APPEND
                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Error(LOAD_ERROR)
                        )
                    )
                )

                // Failed PREPEND
                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = PREPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = PREPEND,
                            fromMediator = false,
                            loadState = Error(LOAD_ERROR)
                        )
                    )
                )

                // Retry should trigger in both directions.
                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = PREPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createPrepend(
                            pageOffset = -1,
                            range = 49..49,
                            startState = NotLoading.Incomplete,
                            endState = Loading
                        ),
                        createAppend(pageOffset = 1, range = 52..52)
                    )
                )
            }
        }
    }

    @Test
    fun retry_errorDoesNotEnableHints() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = object : PagingSource<Int, Int>() {
                var nextResult: LoadResult<Int, Int>? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
                    val result = nextResult
                    nextResult = null
                    return result ?: LoadResult.Error(LOAD_ERROR)
                }
            }
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { pageEvents, _ ->
                // Successful REFRESH
                pageSource.nextResult = Page(
                    data = listOf(0, 1),
                    prevKey = -1,
                    nextKey = 1,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(REFRESH, false, Loading),
                        Refresh(
                            pages = listOf(TransformablePage(listOf(0, 1))),
                            placeholdersBefore = 50,
                            placeholdersAfter = 48,
                            combinedLoadStates = CombinedLoadStates.IDLE_SOURCE
                        )
                    )
                )

                // Hint to trigger APPEND
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(APPEND, false, Loading),
                        LoadStateUpdate(APPEND, false, Error(LOAD_ERROR))
                    )
                )

                // Retry failed APPEND
                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(APPEND, false, Loading),
                        LoadStateUpdate(APPEND, false, Error(LOAD_ERROR))
                    )
                )

                // This hint should be ignored even though in the non-error state it would
                // re-emit for APPEND due to greater presenterIndex value.
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 2,
                        presentedItemsBefore = 2,
                        presentedItemsAfter = -1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(listOf<PageEvent<Int>>())

                // Hint to trigger PREPEND
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(PREPEND, false, Loading),
                        LoadStateUpdate(PREPEND, false, Error(LOAD_ERROR))
                    )
                )

                // Retry failed hints, both PREPEND and APPEND should trigger.
                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(PREPEND, false, Loading),
                        LoadStateUpdate(APPEND, false, Loading),
                        LoadStateUpdate(PREPEND, false, Error(LOAD_ERROR)),
                        LoadStateUpdate(APPEND, false, Error(LOAD_ERROR))
                    )
                )

                // This hint should be ignored even though in the non-error state it would
                // re-emit for PREPEND due to smaller presenterIndex value.
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = -1,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 2,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEqualTo(listOf<PageEvent<Int>>())
            }
        }
    }

    @Test
    fun retryRefresh() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { state, _ ->

                pageSource.errorNextLoad = true
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(REFRESH, false, Loading),
                        LoadStateUpdate(REFRESH, false, Error(LOAD_ERROR))
                    )
                )

                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(REFRESH, false, Loading),
                        createRefresh(50..51)
                    )
                )
            }
        }
    }

    @Test
    fun retryRefreshWithBufferedHint() = testScope.runBlockingTest {
        pauseDispatcher {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryCh.asFlow())
            collectPagerData(pager) { state, _ ->
                pageSource.errorNextLoad = true
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Error(LOAD_ERROR)
                        )
                    )
                )
                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertTrue { state.newEvents().isEmpty() }

                retryCh.offer(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createRefresh(range = 50..51),
                        LoadStateUpdate(
                            loadType = PREPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        createPrepend(pageOffset = -1, range = 49..49)
                    )
                )
            }
        }
    }

    @Test
    fun retry_remotePrepend() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)

                return if (loadType == PREPEND) {
                    MediatorResult.Error(EXCEPTION)
                } else {
                    MediatorResult.Success(endOfPaginationReached = true)
                }
            }
        }

        val pageSource = TestPagingSource(items = List(2) { it })
        val pager = PageFetcherSnapshot(
            initialKey = 0,
            pagingSource = pageSource,
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator),
            triggerRemoteRefresh = false
        )

        collectPagerData(pager) { _, _ ->
            // Resolve initial load.
            advanceUntilIdle()

            retryCh.offer(Unit)
            advanceUntilIdle()

            retryCh.offer(Unit)
            advanceUntilIdle()

            assertEquals(3, remoteMediator.loadEvents.filter { it.loadType == PREPEND }.size)
        }
    }

    @Test
    fun retry_remoteAppend() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)

                return if (loadType == APPEND) {
                    MediatorResult.Error(EXCEPTION)
                } else {
                    MediatorResult.Success(endOfPaginationReached = true)
                }
            }
        }

        val pageSource = TestPagingSource(items = List(2) { it })
        val pager = PageFetcherSnapshot(
            initialKey = 0,
            pagingSource = pageSource,
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator),
            triggerRemoteRefresh = false
        )

        collectPagerData(pager) { _, _ ->
            // Resolve initial load.
            advanceUntilIdle()

            retryCh.offer(Unit)
            advanceUntilIdle()

            retryCh.offer(Unit)
            advanceUntilIdle()

            assertEquals(3, remoteMediator.loadEvents.filter { it.loadType == APPEND }.size)
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
            LoadStateUpdate(REFRESH, false, Loading),
            createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.combinedLoadStates) }
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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.combinedLoadStates) }
            )
        )
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(PREPEND, false, Loading),
                createPrepend(-1, 49..49).let { Prepend(it.pages, 0, it.combinedLoadStates) }
            )
        )

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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 50..51).let { Refresh(it.pages, 0, 0, it.combinedLoadStates) }
            )
        )

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(1, 52..52).let { Append(it.pages, 0, it.combinedLoadStates) }
            )
        )

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
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(range = 50..52)
            )
        )
        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).isEqualTo(
            listOf<PageEvent<Int>>(
                LoadStateUpdate(APPEND, false, Loading),
                createAppend(pageOffset = 1, range = 53..53)
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun refreshKeyInfo_nullHint() = testScope.runBlockingTest {
        val pagingSource = pagingSourceFactory()
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryCh.asFlow())
        assertNull(pager.refreshKeyInfo())
    }

    @Test
    fun refreshKeyInfo_pagesEmpty() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryCh.asFlow())
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            assertNull(pager.refreshKeyInfo())
        }
    }

    @Test
    fun refreshKeyInfo_loadedIndex() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { _, _ ->
                advanceUntilIdle()

                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )

                val refreshKeyInfo = pager.refreshKeyInfo()
                assertNotNull(refreshKeyInfo)
                assertEquals(51, refreshKeyInfo.anchorPosition)

                // Assert from anchorPosition in placeholdersBefore
                assertEquals(50, refreshKeyInfo.closestItemToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(50, refreshKeyInfo.closestItemToPosition(50))
                assertEquals(51, refreshKeyInfo.closestItemToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(51, refreshKeyInfo.closestItemToPosition(90))

                val loadedPage = Page(
                    data = listOf(50, 51),
                    prevKey = 49,
                    nextKey = 52,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                assertEquals(listOf(loadedPage), refreshKeyInfo.pages)
                // Assert from anchorPosition in placeholdersBefore
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(50))
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(90))
            }
        }
    }

    @Test
    fun refreshKeyInfo_placeholdersBefore() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryCh.asFlow())

            collectPagerData(pager) { _, _ ->
                advanceUntilIdle()

                pager.accessHint(
                    ViewportHint(
                        pageOffset = 0,
                        indexInPage = -40,
                        presentedItemsBefore = -40,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )

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

                // Assert from anchorPosition in placeholdersBefore
                assertEquals(50, refreshKeyInfo.closestItemToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(50, refreshKeyInfo.closestItemToPosition(50))
                assertEquals(51, refreshKeyInfo.closestItemToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(51, refreshKeyInfo.closestItemToPosition(90))

                val loadedPage = Page(
                    data = listOf(50, 51),
                    prevKey = 49,
                    nextKey = 52,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                // Assert from anchorPosition in placeholdersBefore
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(50))
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(loadedPage, refreshKeyInfo.closestPageToPosition(90))
            }
        }
    }

    @Test
    fun pageFetcherSnapshot_currentPagingState() = testScope.runBlockingTest {
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = TestPagingSource(loadDelay = 100),
            config = config,
            retryFlow = retryCh.asFlow()
        )

        assertEquals(null, pager.refreshKeyInfo())
    }

    @Test
    fun retry_ignoresNewSignalsWhileProcessing() = testScope.runBlockingTest {
        val pagingSource = pagingSourceFactory()
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryCh.asFlow())
        collectPagerData(pager) { state, _ ->
            pagingSource.errorNextLoad = true
            advanceUntilIdle()
            assertThat(state.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    LoadStateUpdate(REFRESH, false, Error(LOAD_ERROR))
                )
            )

            pagingSource.errorNextLoad = true
            retryCh.offer(Unit)
            // Should be ignored by pager as it's still processing previous retry.
            retryCh.offer(Unit)
            advanceUntilIdle()
            assertThat(state.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    LoadStateUpdate(REFRESH, false, Error(LOAD_ERROR))
                )
            )
        }
    }

    @Test
    fun remoteMediator_initialLoadErrorTriggersLocal() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                return MediatorResult.Error(EXCEPTION)
            }
        }

        val pager = PageFetcherSnapshot(
            initialKey = 0,
            pagingSource = pagingSourceFactory(),
            config = PagingConfig(1),
            retryFlow = retryCh.asFlow(),
            triggerRemoteRefresh = true,
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()

            val expected = listOf<PageEvent<Int>>(
                LoadStateUpdate(REFRESH, true, Loading),
                LoadStateUpdate(REFRESH, true, Error(EXCEPTION)),
                LoadStateUpdate(REFRESH, false, Loading),
                createRefresh(0..2, remoteLoadStatesOf(refreshRemote = Error(EXCEPTION))),
                LoadStateUpdate(PREPEND, true, Loading),
                LoadStateUpdate(PREPEND, true, Error(EXCEPTION))
            )

            assertThat(state.pageEvents).isEqualTo(expected)
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
        val pager = PageFetcherSnapshot(
            initialKey = 0,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { _, _ ->
            advanceUntilIdle()

            assertEquals(1, remoteMediator.loadEvents.size)
            assertEquals(PREPEND, remoteMediator.loadEvents[0].loadType)
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
        val pager = PageFetcherSnapshot(
            initialKey = 99,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { _, _ ->
            advanceUntilIdle()

            assertEquals(1, remoteMediator.loadEvents.size)
            assertEquals(APPEND, remoteMediator.loadEvents[0].loadType)
            assertNotNull(remoteMediator.loadEvents[0].state)
        }
    }

    @Test
    fun remoteMediator_endOfPaginationNotReachedLoadStatePrepend() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = false)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 0,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()

            assertEvents(
                listOf(
                    LoadStateUpdate(
                        loadType = REFRESH,
                        fromMediator = false,
                        loadState = Loading
                    ),
                    Refresh(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 0,
                                data = listOf(0)
                            )
                        ),
                        placeholdersBefore = 0,
                        placeholdersAfter = 99,
                        combinedLoadStates = remoteLoadStatesOf()
                    ),
                    LoadStateUpdate(
                        loadType = PREPEND,
                        fromMediator = true,
                        loadState = Loading
                    ),
                    Prepend(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = -1,
                                data = listOf()
                            )
                        ),
                        placeholdersBefore = 0,
                        combinedLoadStates = remoteLoadStatesOf()
                    )
                ),
                state.pageEvents
            )
        }
    }

    @Test
    fun remoteMediator_endOfPaginationReachedLoadStatePrepend() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 0,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()

            assertEvents(
                listOf(
                    LoadStateUpdate(
                        loadType = REFRESH,
                        fromMediator = false,
                        loadState = Loading
                    ),
                    Refresh(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 0,
                                data = listOf(0)
                            )
                        ),
                        placeholdersBefore = 0,
                        placeholdersAfter = 99,
                        combinedLoadStates = remoteLoadStatesOf()
                    ),
                    LoadStateUpdate(
                        loadType = PREPEND,
                        fromMediator = true,
                        loadState = Loading
                    ),
                    Prepend(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = -1,
                                data = listOf()
                            )
                        ),
                        placeholdersBefore = 0,
                        combinedLoadStates = remoteLoadStatesOf(
                            prependRemote = NotLoading.Complete
                        )
                    )
                ),
                state.pageEvents
            )
        }
    }

    @Test
    fun remoteMediator_prependEndOfPaginationReachedLocalThenRemote() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 3,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 1,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()
            assertThat(state.newEvents())
                .isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        Refresh(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = 0,
                                    data = listOf(1, 2, 3)
                                )
                            ),
                            placeholdersBefore = 1,
                            placeholdersAfter = 96,
                            combinedLoadStates = remoteLoadStatesOf()
                        )
                    )
                )

            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 2,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )

            advanceUntilIdle()

            assertThat(state.newEvents())
                .isEqualTo(
                    listOf<PageEvent<Int>>(

                        LoadStateUpdate(
                            loadType = PREPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        Prepend(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = -1,
                                    data = listOf(0)
                                )
                            ),
                            placeholdersBefore = 0,
                            combinedLoadStates = remoteLoadStatesOf(
                                prependLocal = NotLoading.Complete
                            )
                        ),
                        LoadStateUpdate(
                            loadType = PREPEND,
                            fromMediator = true,
                            loadState = Loading
                        ),
                        Prepend(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = -2,
                                    data = listOf()
                                )
                            ),
                            placeholdersBefore = 0,
                            combinedLoadStates = remoteLoadStatesOf(
                                prependLocal = NotLoading.Complete,
                                prependRemote = NotLoading.Complete
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun remoteMediator_endOfPaginationNotReachedLoadStateAppend() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = false)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 99,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()

            assertEvents(
                listOf(
                    LoadStateUpdate(
                        loadType = REFRESH,
                        fromMediator = false,
                        loadState = Loading
                    ),
                    Refresh(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 0,
                                data = listOf(99)
                            )
                        ),
                        placeholdersBefore = 99,
                        placeholdersAfter = 0,
                        combinedLoadStates = remoteLoadStatesOf()
                    ),
                    LoadStateUpdate(
                        loadType = APPEND,
                        fromMediator = true,
                        loadState = Loading
                    ),
                    Append(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 1,
                                data = listOf()
                            )
                        ),
                        placeholdersAfter = 0,
                        combinedLoadStates = remoteLoadStatesOf()
                    )
                ),
                state.pageEvents
            )
        }
    }

    @Test
    fun remoteMediator_endOfPaginationReachedLoadStateAppend() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 99,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()

            assertEvents(
                listOf(
                    LoadStateUpdate(
                        loadType = REFRESH,
                        fromMediator = false,
                        loadState = Loading
                    ),
                    Refresh(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 0,
                                data = listOf(99)
                            )
                        ),
                        placeholdersBefore = 99,
                        placeholdersAfter = 0,
                        combinedLoadStates = remoteLoadStatesOf()
                    ),
                    LoadStateUpdate(
                        loadType = APPEND,
                        fromMediator = true,
                        loadState = Loading
                    ),
                    Append(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 1,
                                data = listOf()
                            )
                        ),
                        placeholdersAfter = 0,
                        combinedLoadStates = remoteLoadStatesOf(
                            appendRemote = NotLoading.Complete
                        )
                    )
                ),
                state.pageEvents
            )
        }
    }

    @Test
    fun remoteMediator_appendEndOfPaginationReachedLocalThenRemote() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 3,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 96,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()
            assertThat(state.newEvents())
                .isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        Refresh(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = 0,
                                    data = listOf(96, 97, 98)
                                )
                            ),
                            placeholdersBefore = 96,
                            placeholdersAfter = 1,
                            combinedLoadStates = remoteLoadStatesOf()
                        )
                    )
                )

            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 48,
                    presentedItemsBefore = 48,
                    presentedItemsAfter = -46,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            assertThat(state.newEvents())
                .isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        Append(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = 1,
                                    data = listOf(99)
                                )
                            ),
                            placeholdersAfter = 0,
                            combinedLoadStates = remoteLoadStatesOf(
                                appendLocal = NotLoading.Complete
                            )
                        ),
                        LoadStateUpdate(
                            loadType = APPEND,
                            fromMediator = true,
                            loadState = Loading
                        ),
                        Append(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = 2,
                                    data = listOf()
                                )
                            ),
                            placeholdersAfter = 0,
                            combinedLoadStates = remoteLoadStatesOf(
                                appendLocal = NotLoading.Complete,
                                appendRemote = NotLoading.Complete
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun remoteMediator_initialRefreshSuccess() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun initialize(): InitializeAction {
                super.initialize()
                return InitializeAction.LAUNCH_INITIAL_REFRESH
            }

            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryCh.asFlow(),
            triggerRemoteRefresh = true,
            remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)
        )

        collectPagerData(pager) { state, _ ->
            advanceUntilIdle()

            assertThat(state.pageEvents)
                .isEqualTo(
                    listOf<PageEvent<Int>>(
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = true,
                            loadState = Loading
                        ),
                        LoadStateUpdate(
                            loadType = REFRESH,
                            fromMediator = false,
                            loadState = Loading
                        ),
                        Refresh(
                            pages = listOf(
                                TransformablePage(
                                    originalPageOffset = 0,
                                    data = listOf(50)
                                )
                            ),
                            placeholdersBefore = 50,
                            placeholdersAfter = 49,
                            combinedLoadStates = remoteLoadStatesOf()
                        )
                    )
                )
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
            val pager = PageFetcherSnapshot(
                initialKey = 50,
                pagingSource = pagingSourceFactory(),
                config = config, retryFlow = retryCh.asFlow()
            ) {
                didJump = true
            }
            // Trigger collection on flow to init jump detection job.
            val job = launch { pager.pageEventFlow.collect { } }

            advanceUntilIdle()

            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = -50,
                    presentedItemsBefore = -50,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            assertTrue { didJump }

            job.cancel()
        }
    }

    @Test
    fun jump_requiresPagingSourceOptIn() {
        assertFailsWith<IllegalArgumentException> {
            PageFetcherSnapshot(
                initialKey = 50,
                pagingSource = TestPagingSource(jumpingSupported = false),
                config = PagingConfig(pageSize = 1, prefetchDistance = 1, jumpThreshold = 1),
                retryFlow = retryCh.asFlow()
            )
        }
    }

    @Test
    fun keyReuse_unsupported_success() = testScope.runBlockingTest {
        pauseDispatcher {
            val pager = PageFetcherSnapshot(
                initialKey = 50,
                pagingSource = object : PagingSource<Int, Int>() {
                    var loads = 0

                    override val keyReuseSupported: Boolean
                        get() = true

                    override suspend fun load(params: LoadParams<Int>) = when (params) {
                        is LoadParams.Refresh -> Page(listOf(0), 0, 0)
                        else -> Page<Int, Int>(
                            listOf(),
                            if (loads < 3) loads else null,
                            if (loads < 3) loads else null
                        )
                    }.also {
                        loads++
                    }
                },
                config = config,
                retryFlow = retryCh.asFlow()
            )

            // Trigger collection on flow.
            val job = launch {
                pager.pageEventFlow.collect { }
            }

            advanceUntilIdle()

            // Trigger first prepend with key = 0
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            // Trigger second prepend with key = 0
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            job.cancel()
        }
    }

    @Test
    fun keyReuse_unsupported_failure() = testScope.runBlockingTest {
        pauseDispatcher {
            val pager = PageFetcherSnapshot(
                initialKey = 50,
                pagingSource = object : PagingSource<Int, Int>() {
                    override val keyReuseSupported = false

                    override suspend fun load(params: LoadParams<Int>) = when (params) {
                        is LoadParams.Refresh -> Page(listOf(0, 0), 0, 0)
                        else -> Page(listOf(0), 0, 0)
                    }
                },
                config = config,
                retryFlow = retryCh.asFlow()
            )

            // Trigger collection on flow.
            launch {
                // Assert second prepend re-using key = 0 leads to IllegalStateException
                assertFailsWith<IllegalStateException> {
                    pager.pageEventFlow.collect { }
                }
            }

            advanceUntilIdle()

            // Trigger first prepend with key = 0
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            // Trigger second prepend with key = 0
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
        }
    }

    @Test
    fun keyReuse_supported() = testScope.runBlockingTest {
        pauseDispatcher {
            val pager = PageFetcherSnapshot(
                initialKey = 50,
                pagingSource = object : PagingSource<Int, Int>() {
                    var loads = 0

                    override val keyReuseSupported: Boolean
                        get() = true

                    override suspend fun load(params: LoadParams<Int>) = when (params) {
                        is LoadParams.Refresh -> Page(listOf(0), 0, 0)
                        else -> Page<Int, Int>(
                            listOf(),
                            if (loads < 3) 0 else null,
                            if (loads < 3) 0 else null
                        )
                    }.also {
                        loads++
                    }
                },
                config = config,
                retryFlow = retryCh.asFlow()
            )

            // Trigger collection on flow.
            val job = launch {
                pager.pageEventFlow.collect { }
            }

            advanceUntilIdle()

            // Trigger first prepend with key = 0
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            // Trigger second prepend with key = 0
            pager.accessHint(
                ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()

            job.cancel()
        }
    }

    @Test
    fun pageEventSentAfterChannelClosed() {
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = TestPagingSource(loadDelay = 100),
            config = config,
            retryFlow = retryCh.asFlow()
        )

        val deferred = GlobalScope.async {
            pager.pageEventFlow.collect { }
        }
        pager.close()

        runBlocking { deferred.await() }
    }

    @Test
    fun conflatePrioritizingPrefetchDistance_prioritizesUpdates() = testScope.runBlockingTest {
        val prependHintCh = Channel<GenerationalViewportHint>()
        val prependHints = mutableListOf<GenerationalViewportHint>()
        val prependJob = launch {
            prependHintCh.consumeAsFlow()
                .conflatePrioritizingPrefetchDistance(PREPEND)
                .collect { prependHints.add(it) }
        }

        prependHintCh.send(
            GenerationalViewportHint(
                generationId = 0,
                hint = ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = -10,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
        )
        advanceUntilIdle()
        prependHintCh.send(
            GenerationalViewportHint(
                generationId = 0, hint = ViewportHint(
                    pageOffset = -10,
                    indexInPage = 0,
                    presentedItemsBefore = -5,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = -10,
                    originalPageOffsetLast = 0
                )
            )
        )
        advanceUntilIdle()
        assertThat(prependHints).isEqualTo(
            listOf(
                GenerationalViewportHint(
                    generationId = 0,
                    hint = ViewportHint(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = -10,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                ),
                GenerationalViewportHint(
                    generationId = 0,
                    hint = ViewportHint(
                        pageOffset = -10,
                        indexInPage = 0,
                        presentedItemsBefore = -5,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = -10,
                        originalPageOffsetLast = 0
                    )
                )
            )
        )
        prependJob.cancel()

        val appendHintCh = Channel<GenerationalViewportHint>()
        val appendHints = mutableListOf<GenerationalViewportHint>()
        val appendJob = launch {
            appendHintCh.consumeAsFlow()
                .conflatePrioritizingPrefetchDistance(APPEND)
                .collect { appendHints.add(it) }
        }

        appendHintCh.send(
            GenerationalViewportHint(
                generationId = 0,
                hint = ViewportHint(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = -10,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
        )
        advanceUntilIdle()
        appendHintCh.send(
            GenerationalViewportHint(
                generationId = 0,
                hint = ViewportHint(
                    pageOffset = 10,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = -5,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 10
                )
            )
        )
        advanceUntilIdle()
        assertThat(appendHints).isEqualTo(
            listOf(
                GenerationalViewportHint(
                    generationId = 0,
                    hint = ViewportHint(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = -10,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                ),
                GenerationalViewportHint(
                    generationId = 0,
                    hint = ViewportHint(
                        pageOffset = 10,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = -5,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 10
                    )
                )
            )
        )
        appendJob.cancel()
    }
}

internal class CollectedPageEvents<T : Any>(val pageEvents: ArrayList<PageEvent<T>>) {
    var lastIndex = 0
    fun newEvents(): List<PageEvent<T>> = when {
        pageEvents.isEmpty() -> pageEvents.toList()
        lastIndex > pageEvents.lastIndex -> listOf()
        else -> pageEvents.lastIndex.let {
            val result = pageEvents.slice(lastIndex..it)
            lastIndex = it + 1
            result
        }
    }
}

@Suppress("SuspendFunctionOnCoroutineScope")
internal suspend fun <T : Any> CoroutineScope.collectPagerData(
    pageFetcherSnapshot: PageFetcherSnapshot<*, T>,
    block: suspend (state: CollectedPageEvents<T>, job: Job) -> Unit
) {
    val pageEvents = ArrayList<PageEvent<T>>()
    val job: Job = launch { pageFetcherSnapshot.pageEventFlow.collect { pageEvents.add(it) } }
    block(CollectedPageEvents(pageEvents), job)
    job.cancel()
}
