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

import androidx.paging.PagedList.Config
import androidx.paging.PagedSource.LoadResult
import androidx.paging.futures.DirectDispatcher
import androidx.testutils.TestExecutor
import kotlinx.coroutines.GlobalScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PagerTest {
    val testExecutor = TestExecutor()

    inner class ImmediateListDataSource(private val data: List<String>) :
        PositionalDataSource<String>() {

        init {
            initExecutor(testExecutor)
        }

        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<String>) {
            executor.execute {
                val totalCount = data.size

                val position = computeInitialLoadPosition(params, totalCount)
                val loadSize = computeInitialLoadSize(params, position, totalCount)

                val sublist = data.subList(position, position + loadSize)
                callback.onResult(sublist, position, totalCount)
            }
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
            executor.execute {
                val position = params.startPosition
                val end = minOf(position + params.loadSize, data.size)
                callback.onResult(data.subList(position, end))
            }
        }
    }

    val data = List(9) { "$it" }

    private fun rangeResult(start: Int, end: Int) =
        PositionalDataSource.RangeResult(data.subList(start, end)).toLoadResult<Int>()

    private data class Result(
        val type: LoadType,
        val pageResult: LoadResult<*, String>
    )

    private class MockConsumer : Pager.PageConsumer<String> {
        private val results: MutableList<Result> = arrayListOf()
        private val stateChanges: MutableList<StateChange> = arrayListOf()

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

        override fun onPageResult(type: LoadType, page: LoadResult.Page<*, String>): Boolean {
            results.add(Result(type, page))
            return false
        }

        override fun onStateChanged(type: LoadType, state: LoadState) {
            stateChanges.add(StateChange(type, state))
        }
    }

    private fun createPager(
        consumer: MockConsumer,
        start: Int = 0,
        end: Int = 10
    ): Pager<Int, String> {
        val initialData = data.subList(start, end)
        val initialResult = LoadResult.Page<Int, String>(
            data = initialData,
            itemsBefore = start,
            itemsAfter = data.size - initialData.size - start
        )

        return Pager(
            GlobalScope,
            Config(2, 2, true, 10, Config.MAX_SIZE_UNBOUNDED),
            PagedSourceWrapper(ImmediateListDataSource(data)),
            DirectDispatcher,
            DirectDispatcher,
            consumer,
            initialResult
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
            listOf(StateChange(LoadType.END, LoadState.Loading)),
            consumer.takeStateChanges()
        )

        testExecutor.executeAll()

        assertEquals(listOf(Result(LoadType.END, rangeResult(6, 8))), consumer.takeResults())
        assertEquals(
            listOf(StateChange(LoadType.END, LoadState.Idle)),
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
            listOf(StateChange(LoadType.START, LoadState.Loading)),
            consumer.takeStateChanges()
        )

        testExecutor.executeAll()

        assertEquals(listOf(Result(LoadType.START, rangeResult(2, 4))), consumer.takeResults())
        assertEquals(
            listOf(StateChange(LoadType.START, LoadState.Idle)),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun doubleAppend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 2, 6)

        pager.tryScheduleAppend()
        testExecutor.executeAll()
        pager.tryScheduleAppend()
        testExecutor.executeAll()

        assertEquals(
            listOf(
                Result(LoadType.END, rangeResult(6, 8)),
                Result(LoadType.END, rangeResult(8, 9))
            ), consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(LoadType.END, LoadState.Loading),
                StateChange(LoadType.END, LoadState.Idle),
                StateChange(LoadType.END, LoadState.Loading),
                StateChange(LoadType.END, LoadState.Idle)
            ),
            consumer.takeStateChanges()
        )
    }

    @Test
    fun doublePrepend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 4, 8)

        pager.trySchedulePrepend()
        testExecutor.executeAll()
        pager.trySchedulePrepend()
        testExecutor.executeAll()

        assertEquals(
            listOf(
                Result(LoadType.START, rangeResult(2, 4)),
                Result(LoadType.START, rangeResult(0, 2))
            ), consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(LoadType.START, LoadState.Loading),
                StateChange(LoadType.START, LoadState.Idle),
                StateChange(LoadType.START, LoadState.Loading),
                StateChange(LoadType.START, LoadState.Idle)
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
            listOf(Result(LoadType.END, LoadResult.Page.empty<Int, String>())),
            consumer.takeResults()
        )
        assertEquals(
            listOf(StateChange(LoadType.END, LoadState.Done)),
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
            listOf(Result(LoadType.START, LoadResult.Page.empty<Int, String>())),
            consumer.takeResults()
        )
        assertEquals(
            listOf(StateChange(LoadType.START, LoadState.Done)),
            consumer.takeStateChanges()
        )
    }
}
