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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
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

@FlowPreview
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
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
        assertFailsWith<ClosedSendChannelException> { pagingDatas[1].flow.collect { } }
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
}

internal class FetcherState<T : Any>(
    val pagingDataList: ArrayList<PagingData<T>>,
    val pageEventLists: ArrayList<ArrayList<PageEvent<T>>>,
    val job: Job
)

@FlowPreview
@ExperimentalCoroutinesApi
internal fun CoroutineScope.collectFetcherState(fetcher: PageFetcher<Int, Int>): FetcherState<Int> {
    val pagingDataList: ArrayList<PagingData<Int>> = ArrayList()
    val pageEventLists: ArrayList<ArrayList<PageEvent<Int>>> = ArrayList()

    val job = launch {
        fetcher.flow.collectIndexed { index, pagingData ->
            pagingDataList.add(index, pagingData)
            pageEventLists.add(index, ArrayList())
            launch { pagingData.flow.toList(pageEventLists[index]) }
        }
    }

    return FetcherState(pagingDataList, pageEventLists, job)
}
