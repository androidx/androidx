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

import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import androidx.paging.RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class PageFetcherTest {
    private val testScope = TestCoroutineScope()
    private val pagingSourceFactory = { TestPagingSource() }
    private val config = PagingConfig(
        pageSize = 1,
        prefetchDistance = 1,
        enablePlaceholders = true,
        initialLoadSize = 2,
        maxSize = 3
    )

    @Test
    fun initialize() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }
        fetcherState.job.cancel()
    }

    @Test
    fun refresh() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEquals(2, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[1].isNotEmpty() }
        fetcherState.job.cancel()
    }

    @Test
    fun refresh_fromPagingSource() = testScope.runBlockingTest {
        var pagingSource: PagingSource<Int, Int>? = null
        val pagingSourceFactory = { TestPagingSource().also { pagingSource = it } }
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }

        val oldPagingSource = pagingSource
        oldPagingSource?.invalidate()
        advanceUntilIdle()

        assertEquals(2, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[1].isNotEmpty() }
        assertNotEquals(oldPagingSource, pagingSource)
        assertTrue { oldPagingSource!!.invalid }
        fetcherState.job.cancel()
    }

    @Test
    fun refresh_callsInvalidate() = testScope.runBlockingTest {
        var pagingSource: PagingSource<Int, Int>? = null
        val pagingSourceFactory = { TestPagingSource().also { pagingSource = it } }
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        var didCallInvalidate = false
        pagingSource?.registerInvalidatedCallback { didCallInvalidate = true }

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEquals(2, fetcherState.pagingDataList.size)
        assertTrue { fetcherState.pageEventLists[1].isNotEmpty() }
        assertTrue { didCallInvalidate }
        fetcherState.job.cancel()
    }

    @Test
    fun refresh_closesCollection() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)

        var pagingDataCount = 0
        var didFinish = false
        val job = launch {
            pageFetcher.flow.collect { pagedData ->
                pagingDataCount++
                pagedData.flow
                    .onCompletion {
                        didFinish = true
                    }
                    // Return immediately to avoid blocking cancellation. This is analogous to
                    // logic which would process a single PageEvent and doesn't suspend
                    // indefinitely, which is what we expect to happen.
                    .collect { }
            }
        }

        advanceUntilIdle()

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEquals(2, pagingDataCount)
        assertTrue { didFinish }
        job.cancel()
    }

    @Test
    fun refresh_closesUncollectedPageEventCh() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)

        val pagingDatas = mutableListOf<PagingData<Int>>()
        val didFinish = mutableListOf<Boolean>()
        val job = launch {
            pageFetcher.flow.collectIndexed { index, pagingData ->
                pagingDatas.add(pagingData)
                if (index != 1) {
                    pagingData.flow
                        .onStart {
                            didFinish.add(false)
                        }
                        .onCompletion {
                            if (index < 2) didFinish[index] = true
                        }
                        // Return immediately to avoid blocking cancellation. This is analogous to
                        // logic which would process a single PageEvent and doesn't suspend
                        // indefinitely, which is what we expect to happen.
                        .collect { }
                }
            }
        }

        advanceUntilIdle()

        pageFetcher.refresh()
        pageFetcher.refresh()
        advanceUntilIdle()

        assertEquals(3, pagingDatas.size)
        pauseDispatcher {
            // This should complete immediately without advanceUntilIdle().
            val deferred = async { pagingDatas[1].flow.collect { } }
            deferred.await()
        }

        assertEquals(listOf(true, false), didFinish)
        job.cancel()
    }

    @Test
    fun collectTwice() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)
        val fetcherState2 = collectFetcherState(pageFetcher)
        advanceUntilIdle()
        fetcherState.job.cancel()
        fetcherState2.job.cancel()
        advanceUntilIdle()
        assertThat(fetcherState.pagingDataList.size).isEqualTo(1)
        assertThat(fetcherState2.pagingDataList.size).isEqualTo(1)
        assertThat(fetcherState.pageEventLists.first()).isNotEmpty()
        assertThat(fetcherState2.pageEventLists.first()).isNotEmpty()
    }

    @Test
    fun remoteMediator_initializeSkip() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediatorMock = RemoteMediatorMock().apply {
            initializeResult = SKIP_INITIAL_REFRESH
        }
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config, remoteMediatorMock)

        advanceUntilIdle()

        // Assert onInitialize is not called until collection.
        assertTrue { remoteMediatorMock.initializeEvents.isEmpty() }

        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, remoteMediatorMock.initializeEvents.size)
        assertEquals(0, remoteMediatorMock.loadEvents.size)

        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_initializeLaunch() = testScope.runBlockingTest {
        @OptIn(ExperimentalPagingApi::class)
        val remoteMediatorMock = RemoteMediatorMock().apply {
            initializeResult = LAUNCH_INITIAL_REFRESH
        }
        val pageFetcher = PageFetcher(pagingSourceFactory, 50, config, remoteMediatorMock)

        advanceUntilIdle()

        // Assert onInitialize is not called until collection.
        assertTrue { remoteMediatorMock.initializeEvents.isEmpty() }

        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, remoteMediatorMock.initializeEvents.size)
        assertEquals(1, remoteMediatorMock.loadEvents.size)

        fetcherState.job.cancel()
    }

    @Test
    fun remoteMediator_load() = testScope.runBlockingTest {
        val remoteMediatorMock = RemoteMediatorMock()
        val pageFetcher = PageFetcher(pagingSourceFactory, 97, config, remoteMediatorMock)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        // Assert onBoundary is not called for non-terminal page load.
        assertTrue { remoteMediatorMock.loadEvents.isEmpty() }

        fetcherState.pagingDataList[0].receiver.accessHint(
            ViewportHint.Access(
                pageOffset = 0,
                indexInPage = 1,
                presentedItemsBefore = 0,
                presentedItemsAfter = 0,
                originalPageOffsetFirst = 0,
                originalPageOffsetLast = 0
            )
        )

        advanceUntilIdle()

        // Assert onBoundary is called for terminal page load.
        assertEquals(1, remoteMediatorMock.loadEvents.size)
        assertEquals(APPEND, remoteMediatorMock.loadEvents[0].loadType)

        fetcherState.job.cancel()
    }

    @Test
    fun jump() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSources = mutableListOf<PagingSource<Int, Int>>()
            val pagingSourceFactory = { TestPagingSource().also { pagingSources.add(it) } }
            val config = PagingConfig(
                pageSize = 1,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 2,
                maxSize = 3,
                jumpThreshold = 10
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

            // Jump due to sufficiently large presentedItemsBefore
            fetcherState.pagingDataList[0].receiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    // indexInPage value is incorrect, but should not be considered for jumps
                    indexInPage = 0,
                    presentedItemsBefore = -20,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertTrue { pagingSources[0].invalid }
            // Assert no new events added to current generation
            assertEquals(2, fetcherState.pageEventLists[0].size)
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    createRefresh(range = 50..51)
                )
            )

            // Jump due to sufficiently large presentedItemsAfter
            fetcherState.pagingDataList[1].receiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    // indexInPage value is incorrect, but should not be considered for jumps
                    indexInPage = 0,
                    presentedItemsBefore = 0,
                    presentedItemsAfter = -20,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0
                )
            )
            advanceUntilIdle()
            assertTrue { pagingSources[1].invalid }
            // Assert no new events added to current generation
            assertEquals(2, fetcherState.pageEventLists[1].size)
            assertThat(fetcherState.newEvents()).isEqualTo(
                listOf<PageEvent<Int>>(
                    LoadStateUpdate(REFRESH, false, Loading),
                    createRefresh(range = 50..51)
                )
            )

            fetcherState.job.cancel()
        }
    }

    @Test
    fun checksFactoryForNewInstance() = testScope.runBlockingTest {
        pauseDispatcher {
            val pagingSource = TestPagingSource()
            val config = PagingConfig(
                pageSize = 1,
                prefetchDistance = 1,
                initialLoadSize = 2
            )
            val pageFetcher = PageFetcher({ pagingSource }, 50, config)
            val job = testScope.launch {
                assertFailsWith<IllegalStateException> {
                    pageFetcher.flow.collect { }
                }
            }

            advanceUntilIdle()

            pageFetcher.refresh()
            advanceUntilIdle()

            assertTrue { job.isCompleted }
        }
    }

    @ExperimentalStdlibApi
    @Test
    fun pagingSourceInvalidBeforeCallbackAdded() = testScope.runBlockingTest {
        var invalidatesFromAdapter = 0
        var i = 0
        var pagingSource: TestPagingSource? = null
        val pager = Pager(PagingConfig(10)) {
            i++
            TestPagingSource().also {
                if (i == 1) {
                    it.invalidate()
                }

                advanceUntilIdle()
                it.registerInvalidatedCallback { invalidatesFromAdapter++ }
                pagingSource = it
            }
        }

        val job = launch {
            pager.flow.collectLatest { pagingData ->
                TestPagingDataDiffer<Int>(
                    testScope.coroutineContext[CoroutineDispatcher.Key]!!
                ).collectFrom(pagingData)
            }
        }

        advanceUntilIdle()
        pagingSource!!.invalidate()
        advanceUntilIdle()

        assertEquals(1, invalidatesFromAdapter)
        job.cancel()
    }

    @Test
    fun invalidateBeforeAccessPreservesPagingState() = testScope.runBlockingTest {
        pauseDispatcher {
            val config = PagingConfig(
                pageSize = 1,
                prefetchDistance = 1,
                enablePlaceholders = true,
                initialLoadSize = 3,
            )
            val pagingSources = mutableListOf<TestPagingSource>()
            val pageFetcher = PageFetcher(
                pagingSourceFactory = {
                    TestPagingSource(loadDelay = 1000).also {
                        pagingSources.add(it)
                    }
                },
                initialKey = 50,
                config = config,
            )

            lateinit var pagingData: PagingData<Int>
            val job = launch() {
                pageFetcher.flow.collectLatest {
                    pagingData = it
                    it.flow.collect { }
                }
            }

            advanceUntilIdle()

            // Trigger access to allow PagingState to get populated for next generation.
            pagingData.receiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 1,
                    presentedItemsBefore = 1,
                    presentedItemsAfter = 1,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
            advanceUntilIdle()

            // Invalidate first generation, instantiating second generation.
            pagingSources[0].invalidate()

            // Invalidate second generation before it has a chance to complete initial load.
            advanceTimeBy(500)
            pagingSources[1].invalidate()

            // Wait for all non-canceled loads to complete.
            advanceUntilIdle()

            // Verify 3 generations were instantiated.
            assertThat(pagingSources.size).isEqualTo(3)

            // First generation should use initialKey.
            assertThat(pagingSources[0].getRefreshKeyCalls).isEmpty()

            // Second generation should receive getRefreshKey call with state from first generation.
            assertThat(pagingSources[1].getRefreshKeyCalls).isEqualTo(
                listOf(
                    PagingState(
                        pages = pagingSources[0].loadedPages,
                        anchorPosition = 51,
                        config = config,
                        leadingPlaceholderCount = 50,
                    )
                )
            )

            // Verify second generation was invalidated before any pages loaded.
            assertThat(pagingSources[1].loadedPages).isEmpty()

            // Third generation should receive getRefreshKey call with state from first generation.
            assertThat(pagingSources[0].loadedPages.size).isEqualTo(1)
            assertThat(pagingSources[2].getRefreshKeyCalls).isEqualTo(
                listOf(
                    PagingState(
                        pages = pagingSources[0].loadedPages,
                        anchorPosition = 51,
                        config = config,
                        leadingPlaceholderCount = 50,
                    )
                )
            )

            advanceUntilIdle()
            // Trigger APPEND in third generation.
            pagingData.receiver.accessHint(
                ViewportHint.Access(
                    pageOffset = 0,
                    indexInPage = 2,
                    presentedItemsBefore = 2,
                    presentedItemsAfter = 0,
                    originalPageOffsetFirst = 0,
                    originalPageOffsetLast = 0,
                )
            )
            advanceUntilIdle()

            // Invalidate third generation, instantiating fourth generation with new PagingState.
            pagingSources[2].invalidate()
            advanceUntilIdle()

            // Fourth generation should receive getRefreshKey call with state from third generation.
            assertThat(pagingSources[2].loadedPages.size).isEqualTo(2)
            assertThat(pagingSources[3].getRefreshKeyCalls).isEqualTo(
                listOf(
                    PagingState(
                        pages = pagingSources[2].loadedPages,
                        anchorPosition = 53,
                        config = config,
                        leadingPlaceholderCount = 51,
                    )
                )
            )

            job.cancel()
        }
    }
}

internal class FetcherState<T : Any>(
    val pagingDataList: ArrayList<PagingData<T>>,
    val pageEventLists: ArrayList<ArrayList<PageEvent<T>>>,
    val job: Job
) {
    private var lastPageEventListIndex = -1
    var lastIndex = -1

    fun newEvents(): List<PageEvent<T>>? {
        if (lastPageEventListIndex != pageEventLists.lastIndex) {
            lastPageEventListIndex = pageEventLists.lastIndex
            lastIndex = -1
        }

        val pageEvents = pageEventLists.lastOrNull()?.toMutableList() ?: listOf<PageEvent<T>>()
        return pageEvents.drop(lastIndex + 1).also {
            lastIndex = pageEvents.lastIndex
        }
    }
}

internal fun CoroutineScope.collectFetcherState(fetcher: PageFetcher<Int, Int>): FetcherState<Int> {
    val pagingDataList: ArrayList<PagingData<Int>> = ArrayList()
    val pageEventLists: ArrayList<ArrayList<PageEvent<Int>>> = ArrayList()

    val job = launch {
        @OptIn(ExperimentalCoroutinesApi::class)
        fetcher.flow.collectIndexed { index, pagingData ->
            pagingDataList.add(index, pagingData)
            pageEventLists.add(index, ArrayList())
            launch { pagingData.flow.toList(pageEventLists[index]) }
        }
    }

    return FetcherState(pagingDataList, pageEventLists, job)
}
