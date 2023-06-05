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

import androidx.kruth.assertThat
import androidx.paging.ContiguousPagedListTest.Companion.EXCEPTION
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Drop
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.RemoteMediatorMock.LoadEvent
import androidx.paging.TestPagingSource.Companion.LOAD_ERROR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
class PageFetcherSnapshotTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val retryBus = ConflatedEventBus<Unit>()
    private val pagingSourceFactory = suspend {
        TestPagingSource(loadDelay = 1000).also {
            currentPagingSource = it
        }
    }

    private var currentPagingSource: TestPagingSource? = null
    private val config = PagingConfig(
        pageSize = 1,
        prefetchDistance = 1,
        enablePlaceholders = true,
        initialLoadSize = 2,
        maxSize = 3
    )

    @Test
    fun loadStates_prependDone() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 1, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(1..2)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(
                pageOffset = -1,
                range = 0..0,
                startState = NotLoading.Complete
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_prependDoneThenDrop() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 1, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(1..2)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(
                pageOffset = -1,
                range = 0..0,
                startState = NotLoading.Complete
            )
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = -1,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(
                appendLocal = Loading,
                prependLocal = NotLoading.Complete
            ),
            Drop<Int>(
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

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_appendDone() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 97..98)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(pageOffset = 1, range = 99..99, endState = NotLoading.Complete)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_appendDoneThenDrop() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 97..98)

        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(
                pageOffset = 1,
                range = 99..99,
                startState = NotLoading.Incomplete,
                endState = NotLoading.Complete
            )
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 1
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(
                prependLocal = Loading,
                appendLocal = NotLoading.Complete
            ),
            Drop<Int>(
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

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_refreshStart() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 0, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(
                range = 0..1,
                startState = NotLoading.Complete,
                endState = NotLoading.Incomplete
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun loadStates_refreshEnd() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 98, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(
                range = 98..99,
                startState = NotLoading.Incomplete,
                endState = NotLoading.Complete
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun initialize() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..51)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun initialize_bufferedHint() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()

        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..51),
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(pageOffset = -1, range = 49..49)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun prepend() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..51)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(pageOffset = -1, range = 49..49)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun prependAndDrop() = testScope.runTest {
        withContext(coroutineContext) {
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(range = 50..51)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
                createPrepend(pageOffset = -1, range = 48..49)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
                Drop<Int>(
                    loadType = APPEND,
                    minPageOffset = 0,
                    maxPageOffset = 0,
                    placeholdersRemaining = 50
                ),
                createPrepend(pageOffset = -2, range = 46..47)
            )

            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependAndSkipDrop_prefetchWindow() = testScope.runTest {
        withContext(coroutineContext) {
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(range = 50..54)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 4,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
                createPrepend(
                    pageOffset = -1,
                    range = 49..49,
                    startState = Loading
                ),
                createPrepend(pageOffset = -2, range = 48..48)
            )

            // Make sure the job didn't complete exceptionally
            assertFalse { fetcherState.job.isCancelled }

            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependAndDropWithCancellation() = testScope.runTest {
        withContext(coroutineContext) {
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(range = 50..51)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
                createPrepend(pageOffset = -1, range = 48..49)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
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
            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 1,
                    presentedItemsBefore = 3,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = -1,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
                localLoadStateUpdate<Int>(
                    prependLocal = Loading,
                    appendLocal = Loading
                ),
                Drop<Int>(
                    loadType = APPEND,
                    minPageOffset = 0,
                    maxPageOffset = 0,
                    placeholdersRemaining = 50
                ),
                createPrepend(pageOffset = -2, range = 46..47)
            )

            fetcherState.job.cancel()
        }
    }

    @Test
    fun prependMultiplePages() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..52)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
            createPrepend(pageOffset = -2, range = 48..48)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun prepend_viewportHintPrioritizesGenerationId() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..52)
        )

        // PREPEND a few pages.
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
            createPrepend(pageOffset = -2, range = 48..48)
        )

        // APPEND a few pages causing PREPEND pages to drop
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 4,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = -2,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            Drop<Int>(
                loadType = PREPEND,
                minPageOffset = -2,
                maxPageOffset = -2,
                placeholdersRemaining = 49
            ),
            createAppend(pageOffset = 1, range = 53..53, endState = Loading),
            Drop<Int>(
                loadType = PREPEND,
                minPageOffset = -1,
                maxPageOffset = -1,
                placeholdersRemaining = 50
            ),
            createAppend(pageOffset = 2, range = 54..54)
        )

        // PREPEND a page, this hint would normally be ignored, but has a newer generationId.
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 3,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 2
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            Drop<Int>(
                loadType = APPEND,
                minPageOffset = 2,
                maxPageOffset = 2,
                placeholdersRemaining = 46
            ),
            createPrepend(pageOffset = -1, range = 49..49)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun rapidViewportHints() = testScope.runTest {
        val config = PagingConfig(
            pageSize = 10,
            prefetchDistance = 5,
            enablePlaceholders = true,
            initialLoadSize = 10,
            maxSize = 100
        )
        val pageFetcher = PageFetcher(pagingSourceFactory, 0, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(0..9, startState = NotLoading.Complete)
        )
        withContext(coroutineContext) {
            val receiver = fetcherState.pagingDataList[0].hintReceiver
            // send a bunch of access hints while collection is paused
            (0..9).forEach { pos ->
                receiver.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = pos,
                        presentedItemsBefore = pos,
                        presentedItemsAfter = 9 - pos,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
            }
        }

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(
                appendLocal = Loading,
                prependLocal = NotLoading.Complete
            ),
            createAppend(
                pageOffset = 1,
                range = 10..19,
                startState = NotLoading.Complete,
                endState = NotLoading.Incomplete
            ),
        )

        fetcherState.job.cancel()
    }

    @Test
    fun append() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..51)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(1, 52..52)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun appendMultiplePages() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..52)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(
                pageOffset = 1,
                range = 53..53,
                startState = NotLoading.Incomplete,
                endState = Loading
            ),
            createAppend(2, 54..54)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun appendAndDrop() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..51)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(pageOffset = 1, range = 52..53)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 1,
                indexInPage = 1,
                presentedItemsBefore = 3,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 1
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            Drop<Int>(
                loadType = PREPEND,
                minPageOffset = 0,
                maxPageOffset = 0,
                placeholdersRemaining = 52
            ),
            createAppend(pageOffset = 2, range = 54..55)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun appendAndSkipDrop_prefetchWindow() = testScope.runTest {
        withContext(coroutineContext) {
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(range = 50..54)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 4,
                    presentedItemsBefore = 4,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(appendLocal = Loading),
                createAppend(
                    pageOffset = 1,
                    range = 55..55,
                    endState = Loading
                ),
                createAppend(pageOffset = 2, range = 56..56)
            )

            fetcherState.job.cancel()
        }
    }

    @Test
    fun appendAndDropWithCancellation() = testScope.runTest {
        withContext(coroutineContext) {
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
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(range = 50..51)
            )

            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 1,
                    presentedItemsBefore = 1,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(appendLocal = Loading),
                createAppend(pageOffset = 1, range = 52..53)
            )

            // Start hint processing until load starts, but hasn't finished.
            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 1,
                    indexInPage = 1,
                    presentedItemsBefore = 3,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 1
                )
            )
            advanceTimeBy(500)
            fetcherState.pagingDataList[0].hintReceiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 3,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 1
                )
            )
            advanceUntilIdle()
            assertThat(fetcherState.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(appendLocal = Loading),
                localLoadStateUpdate<Int>(
                    appendLocal = Loading,
                    prependLocal = Loading
                ),
                Drop<Int>(
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

            fetcherState.job.cancel()
        }
    }

    @Test
    fun append_viewportHintPrioritizesGenerationId() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..52)
        )

        // APPEND a few pages.
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(pageOffset = 1, range = 53..53, endState = Loading),
            createAppend(pageOffset = 2, range = 54..54)
        )

        // PREPEND a few pages causing APPEND pages to drop
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 4,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 2
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            Drop<Int>(
                loadType = APPEND,
                minPageOffset = 2,
                maxPageOffset = 2,
                placeholdersRemaining = 46
            ),
            createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
            Drop<Int>(
                loadType = APPEND,
                minPageOffset = 1,
                maxPageOffset = 1,
                placeholdersRemaining = 47
            ),
            createPrepend(pageOffset = -2, range = 48..48)
        )

        // APPEND a page, this hint would normally be ignored, but has a newer generationId.
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 3,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = -2,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            Drop<Int>(
                loadType = PREPEND,
                minPageOffset = -2,
                maxPageOffset = -2,
                placeholdersRemaining = 49
            ),
            createAppend(pageOffset = 1, range = 53..53)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun invalidateNoScroll() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..51)
        )

        pageFetcher.refresh()
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(
                range = 0..1,
                startState = NotLoading.Complete,
            )
        )

        fetcherState.job.cancel()
    }

    @Test
    fun invalidateAfterScroll() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..51)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(1, 52..52)
        )

        pageFetcher.refresh()
        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(51..52)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun close_cancelsCollectionBeforeInitialLoad() = testScope.runTest {
        // Infinitely suspending PagingSource which never finishes loading anything.
        val pagingSource = object : PagingSource<Int, Int>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
                delay(2000)
                fail("Should never get here")
            }

            override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
        }
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

        collectSnapshotData(pager) { _, job ->

            // Start the initial load, but do not let it finish.
            advanceTimeBy(500)

            // Close pager, then advance time by enough to allow initial load to finish.
            pager.close()
            advanceTimeBy(1500)

            assertTrue { !job.isActive }
        }
    }

    @Test
    fun retry() = testScope.runTest {
        withContext(coroutineContext) {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { state, _ ->
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    createRefresh(range = 50..51)
                )

                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    localLoadStateUpdate<Int>(appendLocal = Error(LOAD_ERROR)),
                )

                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    createAppend(pageOffset = 1, range = 52..52)
                )
            }
        }
    }

    @Test
    fun retryNothing() = testScope.runTest {
        withContext(coroutineContext) {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { state, _ ->

                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    createRefresh(range = 50..51)
                )

                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    createAppend(pageOffset = 1, range = 52..52)
                )
                retryBus.send(Unit)
                advanceUntilIdle()
                assertTrue { state.newEvents().isEmpty() }
            }
        }
    }

    @Test
    fun retryTwice() = testScope.runTest {
        withContext(coroutineContext) {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { state, _ ->

                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    createRefresh(range = 50..51)
                )
                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    localLoadStateUpdate<Int>(appendLocal = Error(LOAD_ERROR)),
                )
                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    createAppend(pageOffset = 1, range = 52..52)
                )
                retryBus.send(Unit)
                advanceUntilIdle()
                assertTrue { state.newEvents().isEmpty() }
            }
        }
    }

    @Test
    fun retryBothDirections() = testScope.runTest {
        withContext(coroutineContext) {
            val config = PagingConfig(
                pageSize = 1,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 4
            )
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { state, _ ->
                // Initial REFRESH
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    createRefresh(range = 50..51)
                )

                // Failed APPEND
                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    localLoadStateUpdate<Int>(appendLocal = Error(LOAD_ERROR)),
                )

                // Failed PREPEND
                pageSource.errorNextLoad = true
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(
                        prependLocal = Loading,
                        appendLocal = Error(LOAD_ERROR)
                    ),
                    localLoadStateUpdate<Int>(
                        prependLocal = Error(LOAD_ERROR),
                        appendLocal = Error(LOAD_ERROR)
                    ),
                )

                // Retry should trigger in both directions.
                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(
                        prependLocal = Loading,
                        appendLocal = Error(LOAD_ERROR),
                    ),
                    localLoadStateUpdate<Int>(
                        prependLocal = Loading,
                        appendLocal = Loading,
                    ),
                    createPrepend(
                        pageOffset = -1,
                        range = 49..49,
                        startState = NotLoading.Incomplete,
                        endState = Loading
                    ),
                    createAppend(pageOffset = 1, range = 52..52)
                )
            }
        }
    }

    @Test
    fun retry_errorDoesNotEnableHints() = testScope.runTest {
        withContext(StandardTestDispatcher(testScheduler)) {
            val pageSource = object : PagingSource<Int, Int>() {
                var nextResult: LoadResult<Int, Int>? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
                    val result = nextResult
                    nextResult = null
                    return result ?: LoadResult.Error(LOAD_ERROR)
                }

                override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
            }
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { pageEvents, _ ->
                // Successful REFRESH
                pageSource.nextResult = Page(
                    data = listOf(0, 1),
                    prevKey = -1,
                    nextKey = 1,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    localRefresh(
                        pages = listOf(TransformablePage(listOf(0, 1))),
                        placeholdersBefore = 50,
                        placeholdersAfter = 48,
                    )
                )

                // Hint to trigger APPEND
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    localLoadStateUpdate<Int>(appendLocal = Error(LOAD_ERROR)),
                )

                // Retry failed APPEND
                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(appendLocal = Loading),
                    localLoadStateUpdate<Int>(appendLocal = Error(LOAD_ERROR)),
                )

                // This hint should be ignored even though in the non-error state it would
                // re-emit for APPEND due to greater presenterIndex value.
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 2,
                        presentedItemsBefore = 2,
                        presentedItemsAfter = -1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEmpty()

                // Hint to trigger PREPEND
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 0,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 1,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(
                        prependLocal = Loading,
                        appendLocal = Error(LOAD_ERROR),
                    ),
                    localLoadStateUpdate<Int>(
                        prependLocal = Error(LOAD_ERROR),
                        appendLocal = Error(LOAD_ERROR),
                    ),
                )

                // Retry failed hints, both PREPEND and APPEND should trigger.
                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(
                        prependLocal = Loading,
                        appendLocal = Error(LOAD_ERROR),
                    ),
                    localLoadStateUpdate<Int>(
                        prependLocal = Loading,
                        appendLocal = Loading
                    ),
                    localLoadStateUpdate<Int>(
                        prependLocal = Error(LOAD_ERROR),
                        appendLocal = Loading,
                    ),
                    localLoadStateUpdate<Int>(
                        prependLocal = Error(LOAD_ERROR),
                        appendLocal = Error(LOAD_ERROR),
                    ),
                )

                // This hint should be ignored even though in the non-error state it would
                // re-emit for PREPEND due to smaller presenterIndex value.
                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = -1,
                        presentedItemsBefore = 0,
                        presentedItemsAfter = 2,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )
                advanceUntilIdle()
                assertThat(pageEvents.newEvents()).isEmpty()
            }

            testScope.advanceUntilIdle()
        }
    }

    @Test
    fun retryRefresh() = testScope.runTest {
        withContext(coroutineContext) {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { state, _ ->

                pageSource.errorNextLoad = true
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    localLoadStateUpdate<Int>(refreshLocal = Error(LOAD_ERROR)),
                )

                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    createRefresh(50..51)
                )
            }
        }
    }

    @Test
    fun retryRefreshWithBufferedHint() = testScope.runTest {
        withContext(coroutineContext) {
            val pageSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pageSource, config, retryFlow = retryBus.flow)
            collectSnapshotData(pager) { state, _ ->
                pageSource.errorNextLoad = true
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    localLoadStateUpdate<Int>(refreshLocal = Error(LOAD_ERROR)),
                )
                pager.accessHint(
                    ViewportHint.Access(
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

                retryBus.send(Unit)
                advanceUntilIdle()
                assertThat(state.newEvents()).containsExactly(
                    localLoadStateUpdate<Int>(refreshLocal = Loading),
                    createRefresh(range = 50..51),
                    localLoadStateUpdate<Int>(prependLocal = Loading),
                    createPrepend(pageOffset = -1, range = 49..49)
                )
            }
        }
    }

    @Test
    fun retry_remotePrepend() = runTest {
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

        var createdPagingSource = false
        val factory = suspend {
            check(!createdPagingSource)
            createdPagingSource = true
            TestPagingSource(items = List(2) { it })
        }
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = factory,
            config = config,
            remoteMediator = remoteMediator
        )

        pager.collectEvents {
            awaitIdle()
            retry()
            awaitIdle()
            retry()
            awaitIdle()
            assertThat(
                remoteMediator.loadEventCounts()
            ).containsExactlyEntriesIn(
                mapOf(
                    PREPEND to 3,
                    APPEND to 1,
                    REFRESH to 0
                )
            )
        }
    }

    @Test
    fun retry_remoteAppend() = runTest {
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

        var createdPagingSource = false
        val factory = suspend {
            check(!createdPagingSource)
            createdPagingSource = true
            TestPagingSource(items = List(2) { it })
        }
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = factory,
            config = config,
            remoteMediator = remoteMediator
        )

        pager.collectEvents {
            // Resolve initial load.
            awaitIdle()
            retry()
            awaitIdle()
            retry()
            awaitIdle()
            assertThat(
                remoteMediator.loadEventCounts()
            ).containsExactlyEntriesIn(
                mapOf(
                    PREPEND to 1,
                    APPEND to 3,
                    REFRESH to 0
                )
            )
        }
    }

    @Test
    fun disablePlaceholders_refresh() = testScope.runTest {
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

        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            localRefresh(createRefresh(range = 50..51).pages)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun disablePlaceholders_prepend() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            localRefresh(createRefresh(range = 50..51).pages)
        )
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            localPrepend(createPrepend(-1, 49..49).pages)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun disablePlaceholders_append() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            localRefresh(createRefresh(range = 50..51).pages)
        )

        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 1,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            localAppend(createAppend(1, 52..52).pages)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun neverDropBelowTwoPages() = testScope.runTest {
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
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(range = 50..52)
        )
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 2,
                presentedItemsBefore = 2,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(pageOffset = 1, range = 53..53)
        )

        fetcherState.job.cancel()
    }

    @Test
    fun currentPagingState_pagesEmptyWithHint() = testScope.runTest {
        withContext(coroutineContext) {
            val pagingSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)
            pager.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            assertThat(pager.currentPagingState()).isEqualTo(
                PagingState<Int, Int>(
                    pages = listOf(),
                    anchorPosition = 0,
                    config = config,
                    leadingPlaceholderCount = 0
                )
            )
        }
    }

    /**
     * Verify we re-use previous PagingState for remote refresh if there are no pages loaded.
     */
    @Test
    fun currentPagingState_ignoredOnEmptyPages() = testScope.runTest {
        val remoteMediator = RemoteMediatorMock()
        val pagingSource = pagingSourceFactory()
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = pagingSource,
            config = config,
            retryFlow = retryBus.flow,
            remoteMediatorConnection = RemoteMediatorAccessor(testScope, remoteMediator)
        )
        pager.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 1,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        assertThat(pager.currentPagingState()).isEqualTo(
            PagingState<Int, Int>(
                pages = listOf(),
                anchorPosition = 0,
                config = config,
                leadingPlaceholderCount = 0
            )
        )
    }

    @Test
    fun currentPagingState_loadedIndex() = testScope.runTest {
        withContext(coroutineContext) {
            val pagingSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { _, _ ->
                advanceUntilIdle()

                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = 1,
                        presentedItemsBefore = 1,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )

                val pagingState = pager.currentPagingState()
                assertNotNull(pagingState)
                assertEquals(51, pagingState.anchorPosition)

                // Assert from anchorPosition in placeholdersBefore
                assertEquals(50, pagingState.closestItemToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(50, pagingState.closestItemToPosition(50))
                assertEquals(51, pagingState.closestItemToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(51, pagingState.closestItemToPosition(90))

                val loadedPage = Page(
                    data = listOf(50, 51),
                    prevKey = 49,
                    nextKey = 52,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                assertEquals(listOf(loadedPage), pagingState.pages)
                // Assert from anchorPosition in placeholdersBefore
                assertEquals(loadedPage, pagingState.closestPageToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(loadedPage, pagingState.closestPageToPosition(50))
                assertEquals(loadedPage, pagingState.closestPageToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(loadedPage, pagingState.closestPageToPosition(90))
            }
        }
    }

    @Test
    fun currentPagingState_placeholdersBefore() = testScope.runTest {
        withContext(coroutineContext) {
            val pagingSource = pagingSourceFactory()
            val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

            collectSnapshotData(pager) { _, _ ->
                advanceUntilIdle()

                pager.accessHint(
                    ViewportHint.Access(
                        pageOffset = 0,
                        indexInPage = -40,
                        presentedItemsBefore = -40,
                        presentedItemsAfter = 0,
                        originalPageOffsetFirst = 0,
                        originalPageOffsetLast = 0
                    )
                )

                val pagingState = pager.currentPagingState()
                assertNotNull(pagingState)
                assertEquals(10, pagingState.anchorPosition)
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
                    pagingState.pages
                )

                // Assert from anchorPosition in placeholdersBefore
                assertEquals(50, pagingState.closestItemToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(50, pagingState.closestItemToPosition(50))
                assertEquals(51, pagingState.closestItemToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(51, pagingState.closestItemToPosition(90))

                val loadedPage = Page(
                    data = listOf(50, 51),
                    prevKey = 49,
                    nextKey = 52,
                    itemsBefore = 50,
                    itemsAfter = 48
                )
                // Assert from anchorPosition in placeholdersBefore
                assertEquals(loadedPage, pagingState.closestPageToPosition(10))
                // Assert from anchorPosition in loaded indices
                assertEquals(loadedPage, pagingState.closestPageToPosition(50))
                assertEquals(loadedPage, pagingState.closestPageToPosition(51))
                // Assert from anchorPosition in placeholdersAfter
                assertEquals(loadedPage, pagingState.closestPageToPosition(90))
            }
        }
    }

    @Test
    fun currentPagingState_noHint() = testScope.runTest {
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = TestPagingSource(loadDelay = 100),
            config = config,
            retryFlow = retryBus.flow
        )

        assertThat(pager.currentPagingState()).isEqualTo(
            PagingState<Int, Int>(
                pages = listOf(),
                anchorPosition = null,
                config = config,
                leadingPlaceholderCount = 0,
            )
        )
    }

    @Test
    fun retry_ignoresNewSignalsWhileProcessing() = testScope.runTest {
        val pagingSource = pagingSourceFactory()
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)
        collectSnapshotData(pager) { state, _ ->
            pagingSource.errorNextLoad = true
            advanceUntilIdle()
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                localLoadStateUpdate<Int>(refreshLocal = Error(LOAD_ERROR)),
            )

            pagingSource.errorNextLoad = true
            retryBus.send(Unit)
            // Should be ignored by pager as it's still processing previous retry.
            retryBus.send(Unit)
            advanceUntilIdle()
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                localLoadStateUpdate<Int>(refreshLocal = Error(LOAD_ERROR)),
            )
        }
    }

    /**
     * The case where all pages from presenter have been dropped in fetcher, so instead of
     * counting dropped pages against prefetchDistance, we should clamp that logic to only count
     * pages that have been loaded.
     */
    @Test
    fun doLoad_prependPresenterPagesDropped() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..51)
        )

        // Send a hint from a presenter state that only sees pages well after the pages loaded in
        // fetcher state:
        // [hint], [50, 51], [52], [53], [54], [55]
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = 4,
                indexInPage = -6,
                presentedItemsBefore = -6,
                presentedItemsAfter = 2,
                originalPageOffsetFirst = 4,
                originalPageOffsetLast = 6
            )
        )
        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(prependLocal = Loading),
            createPrepend(pageOffset = -1, range = 49..49, startState = Loading),
            createPrepend(pageOffset = -2, range = 48..48, startState = NotLoading.Incomplete),
        )

        fetcherState.job.cancel()
    }

    /**
     * The case where all pages from presenter have been dropped in fetcher, so instead of
     * counting dropped pages against prefetchDistance, we should clamp that logic to only count
     * pages that have been loaded.
     */
    @Test
    fun doLoad_appendPresenterPagesDropped() = testScope.runTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()
        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(refreshLocal = Loading),
            createRefresh(50..51)
        )

        // Send a hint from a presenter state that only sees pages well before the pages loaded in
        // fetcher state:
        // [46], [47], [48], [49], [50, 51], [hint]
        fetcherState.pagingDataList[0].hintReceiver.accessHint(
            ViewportHint.Access(
                pageOffset = -4,
                indexInPage = 6,
                presentedItemsBefore = 2,
                presentedItemsAfter = -6,
                originalPageOffsetFirst = -6,
                originalPageOffsetLast = -4
            )
        )
        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(appendLocal = Loading),
            createAppend(pageOffset = 1, range = 52..52, endState = Loading),
            createAppend(pageOffset = 2, range = 53..53, endState = NotLoading.Incomplete),
        )

        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_initialLoadErrorTriggersLocal() = testScope.runTest {
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun initialize(): InitializeAction {
                return InitializeAction.LAUNCH_INITIAL_REFRESH
            }

            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                return MediatorResult.Error(EXCEPTION)
            }
        }

        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = pagingSourceFactory,
            config = PagingConfig(1),
            remoteMediator = remoteMediator
        )

        val expected = listOf(
            listOf(
                remoteLoadStateUpdate(
                    refreshLocal = Loading,
                ),
                remoteLoadStateUpdate(
                    refreshLocal = Loading,
                    refreshRemote = Loading,
                ),
                remoteLoadStateUpdate(
                    refreshLocal = Loading,
                    refreshRemote = Error(EXCEPTION),
                ),
                createRefresh(
                    range = 0..2,
                    remoteLoadStatesOf(
                        refresh = Error(EXCEPTION),
                        prependLocal = NotLoading.Complete,
                        refreshRemote = Error(EXCEPTION),
                    ),
                ),
                // since remote refresh failed and launch initial refresh is requested,
                // we won't receive any append/prepend events
            )
        )

        pager.assertEventByGeneration(expected)
    }

    @Test
    fun remoteMediator_initialLoadTriggersPrepend() = testScope.runTest {
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                currentPagingSource!!.invalidate()
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
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = pagingSourceFactory,
            config = config,
            remoteMediator = remoteMediator
        )

        pager.pageEvents().take(4).toList()
        assertEquals(1, remoteMediator.loadEvents.size)
        assertEquals(PREPEND, remoteMediator.loadEvents[0].loadType)
        assertNotNull(remoteMediator.loadEvents[0].state)
    }

    @Test
    fun remoteMediator_initialLoadTriggersAppend() = testScope.runTest {
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                super.load(loadType, state)
                currentPagingSource!!.invalidate()
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
        val fetcher = PageFetcher(
            initialKey = 99,
            pagingSourceFactory = pagingSourceFactory,
            config = config,
            remoteMediator = remoteMediator
        )
        // taking 4 events:
        // local load, local insert, append state change to loading, local load w/ new append result
        // 4th one is necessary as the Loading state change is done optimistically before the
        // remote mediator is invoked
        fetcher.pageEvents().take(4).toList()
        assertEquals(1, remoteMediator.loadEvents.size)
        assertEquals(APPEND, remoteMediator.loadEvents[0].loadType)
        assertNotNull(remoteMediator.loadEvents[0].state)
    }

    @Test
    fun remoteMediator_remoteRefreshCachesPreviousPagingState() = testScope.runTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = RemoteMediatorMock().apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
            loadCallback = { _, _ -> RemoteMediator.MediatorResult.Success(true) }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = { TestPagingSource(items = listOf(0)) },
            config = config,
            remoteMediator = remoteMediator
        )

        val state = collectFetcherState(pager)

        // Let the initial page load; loaded data should be [0]
        advanceUntilIdle()
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent<Int, Int>(
                loadType = REFRESH,
                state = PagingState(
                    pages = listOf(),
                    anchorPosition = null,
                    config = config,
                    leadingPlaceholderCount = 0,
                ),
            )
        )

        // Explicit call to refresh, which should trigger remote refresh with cached PagingState.
        pager.refresh()
        advanceUntilIdle()

        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(
                loadType = REFRESH,
                state = PagingState(
                    pages = listOf(
                        Page(
                            data = listOf(0),
                            prevKey = null,
                            nextKey = null,
                            itemsBefore = 0,
                            itemsAfter = 0,
                        ),
                    ),
                    anchorPosition = null,
                    config = config,
                    leadingPlaceholderCount = 0,
                ),
            )
        )

        state.job.cancel()
    }

    @Test
    fun sourceOnlyInitialLoadState() = testScope.runTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = { TestPagingSource(items = listOf(0)) },
            config = config,
        )

        val state = collectFetcherState(pager)
        assertThat(state.newEvents()).containsExactly(
            localLoadStateUpdate<Int>(
                refreshLocal = Loading
            ),
        )

        advanceUntilIdle()

        assertThat(state.newEvents()).containsExactly(
            localRefresh(
                pages = listOf(
                    TransformablePage(data = listOf(0)),
                ),
                source = loadStates(
                    refresh = NotLoading.Incomplete,
                    prepend = NotLoading.Complete,
                    append = NotLoading.Complete,
                ),
            ),
        )

        state.job.cancel()
    }

    @Test
    fun remoteInitialLoadState() = testScope.runTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = RemoteMediatorMock().apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
            loadCallback = { _, _ ->
                withContext(coroutineContext) {
                    delay(50)
                    RemoteMediator.MediatorResult.Success(true)
                }
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = { TestPagingSource(items = listOf(0), loadDelay = 100) },
            config = config,
            remoteMediator = remoteMediator,
        )

        val state = collectFetcherState(pager)
        advanceTimeBy(1)

        assertThat(state.newEvents()).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading
            ),
            remoteLoadStateUpdate<Int>(
                refreshRemote = Loading,
                refreshLocal = Loading,
            ),
        )

        advanceUntilIdle()

        assertThat(state.newEvents()).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshRemote = NotLoading.Incomplete,
                prependRemote = NotLoading.Complete,
                appendRemote = NotLoading.Complete,
                refreshLocal = Loading,
            ),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(data = listOf(0))
                ),
                source = loadStates(
                    refresh = NotLoading.Incomplete,
                    prepend = NotLoading.Complete,
                    append = NotLoading.Complete,
                ),
                mediator = loadStates(
                    refresh = NotLoading.Incomplete,
                    prepend = NotLoading.Complete,
                    append = NotLoading.Complete,
                )
            ),
        )

        state.job.cancel()
    }

    @Test
    fun remoteMediator_remoteRefreshEndOfPaginationReached() = testScope.runTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediator = RemoteMediatorMock().apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
            loadCallback = { _, _ ->
                RemoteMediator.MediatorResult.Success(true)
            }
        }

        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 2,
            enablePlaceholders = true,
            initialLoadSize = 1,
            maxSize = 5
        )
        val pager = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = { TestPagingSource(items = listOf(0)) },
            config = config,
            remoteMediator = remoteMediator
        )

        val state = collectFetcherState(pager)

        advanceUntilIdle()

        assertThat(state.newEvents()).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = NotLoading.Incomplete,
                prependRemote = NotLoading.Complete,
                appendRemote = NotLoading.Complete,
            ),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf(0),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = null
                    )
                ),
                source = loadStates(
                    append = NotLoading.Complete,
                    prepend = NotLoading.Complete,
                ),
                mediator = loadStates(
                    refresh = NotLoading.Incomplete,
                    append = NotLoading.Complete,
                    prepend = NotLoading.Complete,
                ),
            )
        )
        state.job.cancel()
    }

    @Test
    fun remoteMediator_endOfPaginationNotReachedLoadStatePrepend() = testScope.runTest {
        val pagingSources = mutableListOf<TestPagingSource>()
        var remotePrependStarted = false
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                // on first advance, we let local refresh complete first before
                // triggering remote prepend load
                delay(300)
                super.load(loadType, state)
                remotePrependStarted = true
                // on second advance, we let super.load() start but don't return result yet
                delay(500)
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
        val fetcher = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = {
                pagingSourceFactory().also {
                    pagingSources.add(it)
                }
            },
            config = config,
            remoteMediator = remoteMediator
        )
        val fetcherState = collectFetcherState(fetcher)
        advanceTimeBy(1200) // let local refresh complete

        // assert first gen events
        val expectedFirstGen = listOf(
            remoteLoadStateUpdate(refreshLocal = Loading),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(0)
                    )
                ),
                placeholdersAfter = 99,
                source = loadStates(prepend = NotLoading.Complete)
            ),
            remoteLoadStateUpdate(
                prependLocal = NotLoading.Complete,
                prependRemote = Loading,
            ),
        )
        assertThat(fetcherState.newEvents()).containsExactlyElementsIn(expectedFirstGen).inOrder()

        // let remote prepend start loading but don't let it complete
        advanceTimeBy(300)
        assertTrue(remotePrependStarted)

        // invalidate first PagingSource while remote is prepending
        pagingSources[0].invalidate()
        assertTrue(pagingSources[0].invalid)

        // allow Mediator prepend and second gen local Refresh to complete
        // due to TestPagingSource loadDay(1000ms), the remote load will complete first
        advanceTimeBy(1300)

        val expectedSecondGen = listOf(
            remoteLoadStateUpdate(
                refreshLocal = Loading,
                prependRemote = Loading,
            ),
            remoteLoadStateUpdate(
                refreshLocal = Loading,
                prependRemote = NotLoading.Incomplete,
            ),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(0)
                    )
                ),
                placeholdersAfter = 99,
                source = loadStates(prepend = NotLoading.Complete)
            )
        )
        assertThat(fetcherState.newEvents().take(3))
            .containsExactlyElementsIn(expectedSecondGen).inOrder()
        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_endOfPaginationReachedLoadStatePrepend() = testScope.runTest {
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
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
        val fetcher = PageFetcher(
            initialKey = 0,
            pagingSourceFactory = pagingSourceFactory,
            config = config,
            remoteMediator = remoteMediator
        )

        fetcher.assertEventByGeneration(
            listOf(
                listOf(
                    remoteLoadStateUpdate(refreshLocal = Loading),
                    remoteRefresh(
                        pages = listOf(
                            TransformablePage(
                                originalPageOffset = 0,
                                data = listOf(0)
                            )
                        ),
                        placeholdersBefore = 0,
                        placeholdersAfter = 99,
                        source = loadStates(prepend = NotLoading.Complete)
                    ),
                    remoteLoadStateUpdate(
                        prependLocal = NotLoading.Complete,
                        prependRemote = Loading
                    ),
                    remoteLoadStateUpdate(
                        prependLocal = NotLoading.Complete,
                        prependRemote = NotLoading.Complete
                    ),
                )
            )
        )
    }

    @Test
    fun remoteMediator_prependEndOfPaginationReachedLocalThenRemote() = testScope.runTest {
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
        val fetcher = PageFetcher(
            initialKey = 1,
            pagingSourceFactory = pagingSourceFactory,
            config = config,
            remoteMediator = remoteMediator
        )

        fetcher.collectEvents {
            awaitEventCount(2)
            val refreshEvents = listOf(
                remoteLoadStateUpdate(refreshLocal = Loading),
                remoteRefresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf(1, 2, 3)
                        )
                    ),
                    placeholdersBefore = 1,
                    placeholdersAfter = 96,
                )
            )
            assertThat(eventsByGeneration[0]).isEqualTo(refreshEvents)
            accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 2,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            val postHintEvents = listOf(
                remoteLoadStateUpdate(prependLocal = Loading),
                remotePrepend(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = -1,
                            data = listOf(0)
                        )
                    ),
                    placeholdersBefore = 0,
                    source = loadStates(prepend = NotLoading.Complete)
                ),
                remoteLoadStateUpdate(
                    prependLocal = NotLoading.Complete,
                    prependRemote = Loading
                ),
                remoteLoadStateUpdate(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete,
                ),
            )
            awaitEventCount(refreshEvents.size + postHintEvents.size)
            assertEquals(
                eventsByGeneration[0],
                refreshEvents + postHintEvents
            )
        }
    }

    @Test
    fun remoteMediator_endOfPaginationNotReachedLoadStateAppend() = testScope.runTest {
        val pagingSources = mutableListOf<TestPagingSource>()
        var remoteAppendStarted = false
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                // on first advance, we let local refresh complete first before
                // triggering remote append load
                delay(300)
                super.load(loadType, state)
                remoteAppendStarted = true
                // on second advance, we let super.load() start but don't return result yet
                delay(500)
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
        val fetcher = PageFetcher(
            initialKey = 99,
            pagingSourceFactory = {
                pagingSourceFactory().also {
                    it.getRefreshKeyResult = 99
                    pagingSources.add(it)
                }
            },
            config = config,
            remoteMediator = remoteMediator
        )
        val fetcherState = collectFetcherState(fetcher)
        advanceTimeBy(1200) // let local refresh complete

        // assert first gen events
        val expectedFirstGen = listOf(
            remoteLoadStateUpdate(refreshLocal = Loading),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(99)
                    )
                ),
                placeholdersBefore = 99,
                source = loadStates(append = NotLoading.Complete)
            ),
            remoteLoadStateUpdate(
                appendLocal = NotLoading.Complete,
                appendRemote = Loading
            ),
        )
        assertThat(fetcherState.newEvents()).containsExactlyElementsIn(expectedFirstGen).inOrder()

        // let remote append start loading but don't let it complete
        advanceTimeBy(300)
        assertTrue(remoteAppendStarted)

        // invalidate first PagingSource while remote is loading an append
        pagingSources[0].invalidate()
        assertTrue(pagingSources[0].invalid)

        // allow Mediator append and second gen local Refresh to complete
        // due to TestPagingSource loadDay(1000ms), the remote load will complete first
        advanceTimeBy(1300)

        val expectedSecondGen = listOf(
            remoteLoadStateUpdate(
                refreshLocal = Loading,
                appendRemote = Loading,
            ),
            remoteLoadStateUpdate(
                refreshLocal = Loading,
                appendRemote = NotLoading.Incomplete,
            ),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(99)
                    )
                ),
                placeholdersBefore = 99,
                source = loadStates(append = NotLoading.Complete)
            ),
        )
        assertThat(fetcherState.newEvents().take(3))
            .containsExactlyElementsIn(expectedSecondGen).inOrder()
        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_endOfPaginationReachedLoadStateAppend() = testScope.runTest {
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
        val pager = PageFetcher(
            initialKey = 99,
            pagingSourceFactory = pagingSourceFactory,
            config = config,
            remoteMediator = remoteMediator
        )

        val expected: List<List<PageEvent<Int>>> = listOf(
            listOf(
                remoteLoadStateUpdate(refreshLocal = Loading),
                remoteRefresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf(99)
                        )
                    ),
                    placeholdersBefore = 99,
                    placeholdersAfter = 0,
                    source = loadStates(append = NotLoading.Complete)
                ),
                remoteLoadStateUpdate(
                    appendLocal = NotLoading.Complete,
                    appendRemote = Loading,
                ),
                remoteLoadStateUpdate(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete
                ),
            )
        )
        pager.assertEventByGeneration(expected)
    }

    @Test
    fun remoteMediator_appendEndOfPaginationReachedLocalThenRemote() = testScope.runTest {
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
        val pager = PageFetcher(
            initialKey = 96,
            pagingSourceFactory = pagingSourceFactory,
            config = config,
            remoteMediator = remoteMediator
        )
        pager.collectEvents {
            val initialEvents = listOf(
                remoteLoadStateUpdate(refreshLocal = Loading),
                remoteRefresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf(96, 97, 98)
                        )
                    ),
                    placeholdersBefore = 96,
                    placeholdersAfter = 1,
                )
            )
            awaitEventCount(initialEvents.size)
            assertEvents(initialEvents, eventsByGeneration[0])
            accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 48,
                    presentedItemsBefore = 48,
                    presentedItemsAfter = -46,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            val postHintEvents = listOf(
                remoteLoadStateUpdate(appendLocal = Loading),
                remoteAppend(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf(99)
                        )
                    ),
                    source = loadStates(append = NotLoading.Complete)
                ),
                remoteLoadStateUpdate(
                    appendLocal = NotLoading.Complete,
                    appendRemote = Loading
                ),
                remoteLoadStateUpdate(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete
                ),
            )
            awaitEventCount(initialEvents.size + postHintEvents.size)
            assertThat(eventsByGeneration[0]).isEqualTo(initialEvents + postHintEvents)
        }
    }

    @Test
    fun remoteMediator_immediateInvalidation() = runTest {
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
                // Wait for remote events to get sent and observed by PageFetcher, but don't let
                // source REFRESH complete yet until we invalidate.
                advanceTimeBy(500)
                currentPagingSource!!.invalidate()
                // Wait for second generation to start before letting remote REFRESH finish, but
                // ensure that remote REFRESH finishes before source REFRESH does.
                delay(100)
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
        val pager = PageFetcher(
            initialKey = 50,
            pagingSourceFactory = {
                pagingSourceFactory().also {
                    it.getRefreshKeyResult = 30
                }
            },
            config = config,
            remoteMediator = remoteMediator
        )
        val fetcherState = collectFetcherState(pager)
        advanceUntilIdle()
        assertThat(fetcherState.pageEventLists).hasSize(2)
        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
        )
        assertThat(fetcherState.pageEventLists[1]).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = NotLoading.Incomplete,
            ),
            // getRefreshKey() = null is used over initialKey due to invalidation.
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(30)
                    )
                ),
                placeholdersBefore = 30,
                placeholdersAfter = 69,
            ),
        )

        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_initialRefreshSuccess() = testScope.runTest {
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

                // Wait for advanceUntilIdle()
                delay(1)

                currentPagingSource!!.invalidate()
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
        val pager = PageFetcher(
            initialKey = 50,
            pagingSourceFactory = {
                pagingSourceFactory().also {
                    it.getRefreshKeyResult = 30
                }
            },
            config = config,
            remoteMediator = remoteMediator
        )
        val fetcherState = collectFetcherState(pager)
        advanceUntilIdle()

        assertThat(fetcherState.pageEventLists.size).isEqualTo(2)
        assertThat(fetcherState.pageEventLists[0]).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = NotLoading.Incomplete,
            ),
        )
        assertThat(fetcherState.pageEventLists[1]).containsExactly(
            // Invalidate happens before RemoteMediator returns.
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = NotLoading.Incomplete,
            ),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(30)
                    )
                ),
                placeholdersBefore = 30,
                placeholdersAfter = 69,
            ),
        )

        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_initialRefreshSuccessEndOfPagination() = testScope.runTest {
        val remoteMediator = object : RemoteMediatorMock(loadDelay = 2000) {
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
        val pager = PageFetcher(
            initialKey = 50,
            pagingSourceFactory = {
                TestPagingSource().apply {
                    nextLoadResult = Page(
                        data = listOf(50),
                        prevKey = null,
                        nextKey = null,
                        itemsBefore = 50,
                        itemsAfter = 49
                    )
                }
            },
            config = config,
            remoteMediator = remoteMediator
        )

        val fetcherState = collectFetcherState(pager)

        advanceTimeBy(1000)
        runCurrent()

        assertThat(fetcherState.newEvents()).containsExactly(
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
            ),
            remoteLoadStateUpdate<Int>(
                refreshLocal = Loading,
                refreshRemote = Loading
            ),
            remoteRefresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf(50)
                    )
                ),
                placeholdersBefore = 50,
                placeholdersAfter = 49,
                source = loadStates(
                    append = NotLoading.Complete,
                    prepend = NotLoading.Complete,
                ),
                mediator = loadStates(refresh = Loading),
            ),
        )

        advanceUntilIdle()

        assertThat(fetcherState.newEvents()).containsExactly(
            remoteLoadStateUpdate<Int>(
                prependLocal = NotLoading.Complete,
                appendLocal = NotLoading.Complete,
                prependRemote = NotLoading.Complete,
                appendRemote = NotLoading.Complete,
                refreshRemote = NotLoading.Incomplete,
            ),
        )

        fetcherState.job.cancel()
    }

    @Test
    fun jump() = testScope.runTest {
        withContext(coroutineContext) {
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
                config = config,
                retryFlow = retryBus.flow,
                previousPagingState = null,
            ) {
                didJump = true
            }
            // Trigger collection on flow to init jump detection job.
            val job = launch { pager.pageEventFlow.collect { } }

            advanceUntilIdle()

            pager.accessHint(
                ViewportHint.Access(
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
                retryFlow = retryBus.flow
            )
        }
    }

    @Test
    fun jump_idempotent_prependOrAppend() = testScope.runTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1,
            enablePlaceholders = true,
            initialLoadSize = 2,
            maxSize = 3,
            jumpThreshold = 10
        )
        var didJump = 0
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryBus.flow,
            previousPagingState = null,
        ) {
            didJump++
        }
        // Trigger collection on flow to init jump detection job.
        val job = launch { pager.pageEventFlow.collect { } }

        advanceUntilIdle()

        // This would trigger both append and prepend because of processHint logic
        pager.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = -50,
                presentedItemsBefore = -50,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        advanceUntilIdle()

        // even though both append / prepend flows sent jumping hint, should only trigger
        // jump once
        assertThat(didJump).isEqualTo(1)

        job.cancel()
    }

    @Test
    fun jump_idempotent_multipleJumpHints() = testScope.runTest {
        val config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1,
            enablePlaceholders = true,
            initialLoadSize = 2,
            maxSize = 3,
            jumpThreshold = 10
        )
        var didJump = 0
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = pagingSourceFactory(),
            config = config,
            retryFlow = retryBus.flow,
            previousPagingState = null,
        ) {
            didJump++
        }
        // Trigger collection on flow to init jump detection job.
        val job = launch { pager.pageEventFlow.collect { } }

        advanceUntilIdle()

        // This would trigger both append and prepend because of processHint logic
        pager.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = -50,
                presentedItemsBefore = -50,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )

        // send second jump hint as well
        pager.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = -50,
                presentedItemsBefore = -50,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )

        advanceUntilIdle()

        // even though both append / prepend flows sent jumping hint, and a second jump hint
        // was sent, they should only trigger jump once
        assertThat(didJump).isEqualTo(1)

        job.cancel()
    }

    @Test
    fun keyReuse_unsupported_success() = testScope.runTest {
        withContext(coroutineContext) {
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

                    override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
                },
                config = config,
                retryFlow = retryBus.flow
            )

            // Trigger collection on flow.
            val job = launch {
                pager.pageEventFlow.collect { }
            }

            advanceUntilIdle()

            // Trigger first prepend with key = 0
            pager.accessHint(
                ViewportHint.Access(
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
                ViewportHint.Access(
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
    fun keyReuse_unsupported_failure() = testScope.runTest {
        withContext(coroutineContext) {
            val pager = PageFetcherSnapshot(
                initialKey = 50,
                pagingSource = object : PagingSource<Int, Int>() {
                    override val keyReuseSupported = false

                    override suspend fun load(params: LoadParams<Int>) = when (params) {
                        is LoadParams.Refresh -> Page(listOf(0, 0), 0, 0)
                        else -> Page(listOf(0), 0, 0)
                    }

                    override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
                },
                config = config,
                retryFlow = retryBus.flow
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
                ViewportHint.Access(
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
                ViewportHint.Access(
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
    fun keyReuse_supported() = testScope.runTest {
        withContext(coroutineContext) {
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

                    override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
                },
                config = config,
                retryFlow = retryBus.flow
            )

            // Trigger collection on flow.
            val job = launch {
                pager.pageEventFlow.collect { }
            }

            advanceUntilIdle()

            // Trigger first prepend with key = 0
            pager.accessHint(
                ViewportHint.Access(
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
                ViewportHint.Access(
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
    fun initializeHintAfterEmpty() = testScope.runTest {
        val pageFetcherSnapshot = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = TestPagingSource(),
            config = config,
            retryFlow = emptyFlow(),
        )
        collectSnapshotData(pageFetcherSnapshot) { state, _ ->
            advanceUntilIdle()
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(range = 50..51),
            )

            pageFetcherSnapshot.accessHint(ViewportHint.Initial(0, 0, 0, 0))
            advanceUntilIdle()
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
                localLoadStateUpdate<Int>(
                    appendLocal = Loading,
                    prependLocal = Loading
                ),
                createPrepend(pageOffset = -1, range = 49..49, endState = Loading),
                createAppend(pageOffset = 1, range = 52..52),
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun pageEventSentAfterChannelClosed() = runTest {
        val pager = PageFetcherSnapshot(
            initialKey = 50,
            pagingSource = TestPagingSource(loadDelay = 100),
            config = config,
            retryFlow = retryBus.flow
        )

        val deferred = GlobalScope.async {
            pager.pageEventFlow.collect { }
        }
        pager.close()

        deferred.await()
    }

    @Test
    fun generationalViewportHint_shouldPrioritizeOver_presenterUpdates() {
        val prependHint = GenerationalViewportHint(
            generationId = 0,
            hint = ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = -10,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        val prependHintWithPresenterUpdate = GenerationalViewportHint(
            generationId = 0,
            hint = ViewportHint.Access(
                pageOffset = -10,
                indexInPage = 0,
                presentedItemsBefore = -5,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = -10,
                originalPageOffsetLast = 0
            )
        )
        assertTrue { prependHintWithPresenterUpdate.shouldPrioritizeOver(prependHint, PREPEND) }

        val appendHint = GenerationalViewportHint(
            generationId = 0,
            hint = ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = -10,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        val appendHintWithPresenterUpdate = GenerationalViewportHint(
            generationId = 0,
            hint = ViewportHint.Access(
                pageOffset = 10,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = -5,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 10
            )
        )
        assertTrue { appendHintWithPresenterUpdate.shouldPrioritizeOver(appendHint, APPEND) }
    }

    @Test
    fun generationalViewportHint_shouldPrioritizeAccessOverInitial() {
        val accessHint = GenerationalViewportHint(
            generationId = 0,
            hint = ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 0,
                presentedItemsBefore = 0,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )
        val initialHint = GenerationalViewportHint(
            generationId = 0,
            hint = ViewportHint.Initial(
                presentedItemsBefore = 0,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )

        assertTrue { accessHint.shouldPrioritizeOver(initialHint, PREPEND) }
        assertFalse { initialHint.shouldPrioritizeOver(accessHint, PREPEND) }
        assertTrue { accessHint.shouldPrioritizeOver(initialHint, APPEND) }
        assertFalse { initialHint.shouldPrioritizeOver(accessHint, APPEND) }
    }

    @Test
    fun close_cancelsCollectionFromLoadResultInvalid() = testScope.runTest {
        val pagingSource = object : PagingSource<Int, Int>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
                return LoadResult.Invalid()
            }

            override fun getRefreshKey(state: PagingState<Int, Int>): Int? {
                fail("should not reach here")
            }
        }
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

        collectSnapshotData(pager) { _, job ->

            // Start initial load but this load should return LoadResult.Invalid
            // wait some time for the invalid result handler to close the page event flow
            advanceTimeBy(1000)

            assertTrue { !job.isActive }
        }
    }

    @Test
    fun refresh_cancelsCollectionFromLoadResultInvalid() = testScope.runTest {
        val pagingSource = TestPagingSource()
        pagingSource.nextLoadResult = LoadResult.Invalid()

        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

        collectSnapshotData(pager) { state, job ->

            // Start initial load but this load should return LoadResult.Invalid
            // Wait some time for the result handler to close the page event flow
            advanceUntilIdle()

            // The flow's last page event should be the original Loading event before it
            // was closed by the invalid result handler
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
            )
            // make sure no more new events are sent to UI
            assertThat(state.newEvents()).isEmpty()
            assertTrue(pagingSource.invalid)
            assertTrue { !job.isActive }
        }
    }

    @Test
    fun append_cancelsCollectionFromLoadResultInvalid() = testScope.runTest {
        val pagingSource = TestPagingSource()
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

        collectSnapshotData(pager) { state, job ->

            advanceUntilIdle()

            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(50..51)
            )
            // append a page
            pager.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 1,
                    presentedItemsBefore = 1,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            // now return LoadResult.Invalid
            pagingSource.nextLoadResult = LoadResult.Invalid()

            advanceUntilIdle()

            // Only a Loading update for Append should be sent and it should not complete
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(appendLocal = Loading),
            )
            assertTrue(pagingSource.invalid)
            assertThat(state.newEvents()).isEmpty()
            assertThat(!job.isActive)
        }
    }

    @Test
    fun prepend_cancelsCollectionFromLoadResultInvalid() = testScope.runTest {
        val pagingSource = TestPagingSource()
        val pager = PageFetcherSnapshot(50, pagingSource, config, retryFlow = retryBus.flow)

        collectSnapshotData(pager) { state, job ->

            advanceUntilIdle()

            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(refreshLocal = Loading),
                createRefresh(50..51)
            )
            // now prepend
            pager.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = -1,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            // now return LoadResult.Invalid.
            pagingSource.nextLoadResult = LoadResult.Invalid()

            advanceUntilIdle()

            // Only a Loading update for Prepend should be sent and it should not complete
            assertThat(state.newEvents()).containsExactly(
                localLoadStateUpdate<Int>(prependLocal = Loading),
            )
            assertTrue(pagingSource.invalid)
            assertThat(state.newEvents()).isEmpty()
            assertThat(!job.isActive)
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
    internal suspend fun <T : Any> CoroutineScope.collectSnapshotData(
        pageFetcherSnapshot: PageFetcherSnapshot<*, T>,
        block: suspend (state: CollectedPageEvents<T>, job: Job) -> Unit
    ) {
        if (pageFetcherSnapshot.remoteMediatorConnection != null) {
            throw IllegalArgumentException("cannot test fetcher with remote mediator here")
        }
        val pageEvents = ArrayList<PageEvent<T>>()
        val job: Job = launch { pageFetcherSnapshot.pageEventFlow.collect { pageEvents.add(it) } }
        block(CollectedPageEvents(pageEvents), job)
        job.cancel()
    }

    internal fun <T : Any> PageFetcher<*, T>.pageEvents(): Flow<PageEvent<T>> {
        return flow.flatMapLatest {
            it.flow
        }
    }

    internal suspend fun <T : Any> PageFetcher<*, T>.collectEvents(
        block: (suspend MultiGenerationCollectionScope<T>.() -> Unit)
    ): List<List<PageEvent<T>>> {
        val collectionScope = MultiGenerationCollectionScopeImpl<T>()
        val eventsByGeneration = collectionScope.eventsByGeneration
        coroutineScope {
            val collectionJob = launch(start = CoroutineStart.LAZY) {
                flow.flatMapLatest { data ->
                    collectionScope.uiReceiver = data.uiReceiver
                    collectionScope.hintReceiver = data.hintReceiver
                    val generationEvents = mutableListOf<PageEvent<T>>().also {
                        eventsByGeneration.add(it)
                    }
                    collectionScope.generationCount.value = eventsByGeneration.size
                    data.flow.onEach {
                        generationEvents.add(it)
                        collectionScope.eventCount.value += 1
                    }
                }.collect() // just keep collecting, block will cancel eventually
            }
            launch {
                collectionScope.stopped.await()
                collectionJob.cancel()
            }
            launch {
                collectionScope.block()
                collectionScope.stop()
            }
            collectionJob.join()
        }

        return eventsByGeneration
    }

    private suspend fun <T : Any> PageFetcher<*, T>.assertEventByGeneration(
        expected: List<List<PageEvent<T>>>
    ) {
        val total = expected.sumOf { it.size }
        val actual = collectEvents {
            awaitEventCount(total)
            stop()
        }
        testScope.runCurrent()
        expected.forEachIndexed { index, list ->
            assertThat(actual.getOrNull(index)
            ?: emptyList<PageEvent<T>>()).containsExactlyElementsIn(list).inOrder()
        }
        assertThat(actual.size).isEqualTo(expected.size)
    }

    internal interface MultiGenerationCollectionScope<T : Any> {
        val eventCount: StateFlow<Int>
        val generationCount: StateFlow<Int>
        val eventsByGeneration: List<List<PageEvent<T>>>
        val uiReceiver: UiReceiver?
        val hintReceiver: HintReceiver?
        suspend fun stop()
        fun accessHint(viewportHint: ViewportHint) {
            hintReceiver!!.accessHint(viewportHint)
        }

        fun retry() {
            uiReceiver!!.retry()
        }

        suspend fun TestScope.awaitIdle() {
            yield()
            advanceUntilIdle()
        }

        suspend fun awaitEventCount(limit: Int) {
            eventCount.takeWhile {
                it < limit
            }.collect()
        }
    }

    internal class MultiGenerationCollectionScopeImpl<T : Any>(
        override val eventCount: MutableStateFlow<Int> = MutableStateFlow(0),
        override val generationCount: MutableStateFlow<Int> = MutableStateFlow(0),
        override val eventsByGeneration: MutableList<List<PageEvent<T>>> = mutableListOf(),
        override var uiReceiver: UiReceiver? = null,
        override var hintReceiver: HintReceiver? = null,
    ) : MultiGenerationCollectionScope<T> {
        val stopped = CompletableDeferred<Unit>()
        override suspend fun stop() {
            stopped.complete(Unit)
        }
    }
}