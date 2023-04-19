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

@file:Suppress("DEPRECATION")

package androidx.paging

import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagedList.Config
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingSource.LoadResult.Page
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class LegacyPageFetcherTest {
    private val testDispatcher = StandardTestDispatcher()
    private val data = List(9) { "$it" }

    inner class ImmediateListDataSource(val data: List<String>) : PagingSource<Int, String>() {
        var invalidData = false

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            val key = params.key ?: 0

            val (start, end) = when (params) {
                is Refresh -> key to key + params.loadSize
                is LoadParams.Prepend -> key - params.loadSize to key
                is LoadParams.Append -> key to key + params.loadSize
            }.let { (start, end) ->
                start.coerceAtLeast(0) to end.coerceAtMost(data.size)
            }

            if (invalidData) {
                invalidData = false
                return LoadResult.Invalid()
            }
            return Page(
                data = data.subList(start, end),
                prevKey = if (start > 0) start else null,
                nextKey = if (end < data.size) end else null,
                itemsBefore = start,
                itemsAfter = data.size - end
            )
        }

        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null
    }

    private fun rangeResult(start: Int, end: Int) = Page(
        data = data.subList(start, end),
        prevKey = if (start > 0) start else null,
        nextKey = if (end < data.size) end else null,
        itemsBefore = start,
        itemsAfter = data.size - end
    )

    private data class Result(
        val type: LoadType,
        val pageResult: LoadResult<*, String>
    )

    private class MockConsumer : LegacyPageFetcher.PageConsumer<String> {
        private val results: MutableList<Result> = arrayListOf()
        private val stateChanges: MutableList<StateChange> = arrayListOf()

        var storage: PagedStorage<String>? = null

        fun takeResults(): List<Result> {
            val ret = results.map { it }
            results.clear()
            return ret
        }

        fun takeStateChanges(): List<StateChange> {
            val ret = stateChanges.map { it }
            stateChanges.clear()
            return ret
        }

        override fun onPageResult(type: LoadType, page: Page<*, String>): Boolean {
            when (type) {
                PREPEND -> storage?.prependPage(page)
                APPEND -> storage?.appendPage(page)
                REFRESH -> {
                    // Nothing
                }
            }

            results.add(Result(type, page))
            return false
        }

        override fun onStateChanged(type: LoadType, state: LoadState) {
            stateChanges.add(StateChange(type, state))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun createPager(
        consumer: MockConsumer,
        start: Int = 0,
        end: Int = 10
    ): LegacyPageFetcher<Int, String> {
        val config = Config(2, 2, true, 10, Config.MAX_SIZE_UNBOUNDED)
        val pagingSource = ImmediateListDataSource(data)

        val initialResult = runBlocking {
            pagingSource.load(
                Refresh(
                    key = start,
                    loadSize = end - start,
                    placeholdersEnabled = config.enablePlaceholders,
                )
            )
        }

        val initialData = (initialResult as Page).data
        val storage = PagedStorage(
            start,
            initialResult,
            data.size - initialData.size - start
        )
        consumer.storage = storage

        @Suppress("UNCHECKED_CAST")
        return LegacyPageFetcher(
            GlobalScope,
            config,
            pagingSource,
            testDispatcher,
            testDispatcher,
            consumer,
            storage as LegacyPageFetcher.KeyProvider<Int>
        )
    }

    @Test
    fun simplePagerAppend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 2, 6)

        assertTrue(consumer.takeResults().isEmpty())
        assertTrue(consumer.takeStateChanges().isEmpty())

        pager.tryScheduleAppend()

        assertTrue(consumer.takeResults().isEmpty())
        assertEquals(
            listOf(StateChange(APPEND, Loading)),
            consumer.takeStateChanges()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(Result(APPEND, rangeResult(6, 8))), consumer.takeResults())
        assertEquals(
            listOf(
                StateChange(
                    APPEND,
                    NotLoading(endOfPaginationReached = false)
                )
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun simplePagerPrepend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 4, 8)

        pager.trySchedulePrepend()

        assertTrue(consumer.takeResults().isEmpty())
        assertEquals(
            listOf(StateChange(PREPEND, Loading)),
            consumer.takeStateChanges()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(Result(PREPEND, rangeResult(2, 4))),
            consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(
                    PREPEND,
                    NotLoading(endOfPaginationReached = false)
                )
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun doubleAppend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 2, 6)

        pager.tryScheduleAppend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                Result(APPEND, rangeResult(6, 8))
            ),
            consumer.takeResults()
        )

        assertEquals(
            listOf(
                StateChange(APPEND, Loading),
                StateChange(
                    APPEND,
                    NotLoading(endOfPaginationReached = false)
                )
            ),
            consumer.takeStateChanges()
        )

        pager.tryScheduleAppend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                Result(APPEND, rangeResult(8, 9))
            ),
            consumer.takeResults()
        )

        assertEquals(
            listOf(
                StateChange(APPEND, Loading),
                StateChange(
                    APPEND,
                    NotLoading(endOfPaginationReached = false)
                )
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun doublePrepend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 4, 8)

        pager.trySchedulePrepend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                Result(PREPEND, rangeResult(2, 4))
            ),
            consumer.takeResults()
        )

        assertEquals(
            listOf(
                StateChange(PREPEND, Loading),
                StateChange(
                    PREPEND, NotLoading(endOfPaginationReached = false)
                )
            ),
            consumer.takeStateChanges()
        )

        pager.trySchedulePrepend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                Result(PREPEND, rangeResult(0, 2))
            ),
            consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(PREPEND, Loading),
                StateChange(
                    PREPEND, NotLoading(endOfPaginationReached = false)
                )
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun emptyAppend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 0, 9)

        pager.tryScheduleAppend()

        // Pager triggers an immediate empty response here, so we don't need to flush the executor
        assertEquals(
            listOf(Result(APPEND, Page.empty<Int, String>())),
            consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(APPEND, NotLoading(endOfPaginationReached = true))
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun emptyPrepend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 0, 9)

        pager.trySchedulePrepend()

        // Pager triggers an immediate empty response here, so we don't need to flush the executor
        assertEquals(
            listOf(Result(PREPEND, Page.empty<Int, String>())),
            consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(
                    PREPEND,
                    NotLoading(endOfPaginationReached = true)
                )
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun append_invalidData() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 0, 3)

        // try a normal append first
        pager.tryScheduleAppend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(consumer.takeResults()).containsExactly(
            Result(APPEND, rangeResult(3, 5))
        )
        assertThat(consumer.takeStateChanges()).containsExactly(
            StateChange(APPEND, Loading),
            StateChange(APPEND, NotLoading.Incomplete)
        )

        // now make next append return LoadResult.Invalid
        val pagingSource = pager.source as ImmediateListDataSource
        pagingSource.invalidData = true

        pager.tryScheduleAppend()
        testDispatcher.scheduler.advanceUntilIdle()

        // the load should return before returning any data
        assertThat(consumer.takeResults()).isEmpty()
        assertThat(consumer.takeStateChanges()).containsExactly(
            StateChange(APPEND, Loading),
        )

        // exception handler should invalidate the paging source and result in fetcher to be
        // detached
        assertTrue(pagingSource.invalid)
        assertTrue(pager.isDetached)
    }

    @Test
    fun prepend_invalidData() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 6, 9)

        // try a normal prepend first
        pager.trySchedulePrepend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(consumer.takeResults()).containsExactly(
            Result(PREPEND, rangeResult(4, 6))
        )
        assertThat(consumer.takeStateChanges()).containsExactly(
            StateChange(PREPEND, Loading),
            StateChange(PREPEND, NotLoading.Incomplete)
        )

        // now make next prepend throw error
        val pagingSource = pager.source as ImmediateListDataSource
        pagingSource.invalidData = true

        pager.trySchedulePrepend()
        testDispatcher.scheduler.advanceUntilIdle()

        // the load should return before returning any data
        assertThat(consumer.takeResults()).isEmpty()
        assertThat(consumer.takeStateChanges()).containsExactly(
            StateChange(PREPEND, Loading),
        )

        // exception handler should invalidate the paging source and result in fetcher to be
        // detached
        assertTrue(pagingSource.invalid)
        assertTrue(pager.isDetached)
    }
}
