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
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@FlowPreview
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(JUnit4::class)
class PageFetcherTest {
    private val testScope = TestCoroutineScope()
    private val pagedSourceFactory = { TestPagedSource() }
    private val config = PagedList.Config(
        pageSize = 1,
        prefetchDistance = 1,
        enablePlaceholders = true,
        initialLoadSizeHint = 2,
        maxSize = 3
    )

    @Test
    fun initialize() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagedDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }
        fetcherState.job.cancel()
    }

    @Test
    fun refresh() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagedDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }

        pageFetcher.refresh()
        advanceUntilIdle()

        assertEquals(2, fetcherState.pagedDataList.size)
        assertTrue { fetcherState.pageEventLists[1].isNotEmpty() }
        fetcherState.job.cancel()
    }

    @Test
    fun refreshFromPagedSource() = testScope.runBlockingTest {
        var pagedSource: PagedSource<Int, Int>? = null
        val pagedSourceFactory = {
            TestPagedSource().also { pagedSource = it }
        }
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)

        advanceUntilIdle()

        assertEquals(1, fetcherState.pagedDataList.size)
        assertTrue { fetcherState.pageEventLists[0].isNotEmpty() }

        val oldPagedSource = pagedSource
        oldPagedSource?.invalidate()
        advanceUntilIdle()

        assertEquals(2, fetcherState.pagedDataList.size)
        assertTrue { fetcherState.pageEventLists[1].isNotEmpty() }
        assertNotEquals(oldPagedSource, pagedSource)
        assertTrue { oldPagedSource!!.invalid }
        fetcherState.job.cancel()
    }

    @Test
    fun collectTwice() = testScope.runBlockingTest {
        val pageFetcher = PageFetcher(pagedSourceFactory, 50, config)
        val fetcherState = collectFetcherState(pageFetcher)
        val fetcherState2 = collectFetcherState(pageFetcher)
        advanceUntilIdle()
        fetcherState.job.cancel()
        fetcherState2.job.cancel()
        advanceUntilIdle()
        assertThat(fetcherState.pagedDataList.size).isEqualTo(1)
        assertThat(fetcherState2.pagedDataList.size).isEqualTo(1)
        assertThat(fetcherState.pageEventLists.first()).isNotEmpty()
        assertThat(fetcherState2.pageEventLists.first()).isNotEmpty()
    }
}

internal class FetcherState<T : Any>(
    val pagedDataList: ArrayList<PagedData<T>>,
    val pageEventLists: ArrayList<ArrayList<PageEvent<T>>>,
    val job: Job
)

@FlowPreview
@ExperimentalCoroutinesApi
internal fun CoroutineScope.collectFetcherState(fetcher: PageFetcher<Int, Int>): FetcherState<Int> {
    val pagedDataList: ArrayList<PagedData<Int>> = ArrayList()
    val pageEventLists: ArrayList<ArrayList<PageEvent<Int>>> = ArrayList()

    val job = launch {
        fetcher.flow.collectIndexed { index, pagedData ->
            pagedDataList.add(index, pagedData)
            pageEventLists.add(index, ArrayList())
            launch { pagedData.flow.toList(pageEventLists[index]) }
        }
    }

    return FetcherState(pagedDataList, pageEventLists, job)
}

internal fun assertEvents(expected: List<PageEvent<Int>>, actual: List<PageEvent<Int>>) {
    try {
        assertEquals(expected, actual)
    } catch (e: Throwable) {
        val msg = e.localizedMessage
            .replace("),", "),\n")
            .replace("<[", "<[\n ")
            .replace("actual", "\nactual")
            .lines()
            .toMutableList()

        if (expected.count() != actual.count()) throw AssertionError(msg.joinToString("\n"))

        var index = 0
        for (i in 0 until expected.count()) {
            if (expected[i] != actual[i]) {
                index = i
                break
            }
        }
        msg[index + 1] = msg[index + 1].prependIndent(" >")
        msg[msg.count() / 2 + index + 1] = msg[msg.count() / 2 + index + 1].prependIndent(" >")
        throw AssertionError(msg.joinToString("\n"))
    }
}
