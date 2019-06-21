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

import androidx.paging.PagedList.LoadState.DONE
import androidx.paging.PagedList.LoadState.IDLE
import androidx.paging.PagedList.LoadState.LOADING
import androidx.paging.PagedList.LoadType.END
import androidx.paging.PagedList.LoadType.START
import androidx.paging.futures.DirectExecutor
import androidx.testutils.TestExecutor
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
        PositionalDataSource.RangeResult(data.subList(start, end))

    private data class Result(
        val type: PagedList.LoadType,
        val pageResult: DataSource.BaseResult<String>
    )

    private data class StateChange(
        val type: PagedList.LoadType,
        val state: PagedList.LoadState,
        val error: Throwable? = null
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

        override fun onPageResult(
            type: PagedList.LoadType,
            pageResult: DataSource.BaseResult<String>
        ): Boolean {
            results.add(Result(type, pageResult))
            return false
        }

        override fun onStateChanged(
            type: PagedList.LoadType,
            state: PagedList.LoadState,
            error: Throwable?
        ) {
            stateChanges.add(StateChange(type, state, error))
        }
    }

    private fun createPager(consumer: MockConsumer, start: Int = 0, end: Int = 10) = Pager(
        PagedList.Config(2, 2, true, 10, PagedList.Config.MAX_SIZE_UNBOUNDED),
        PagedSourceWrapper(ImmediateListDataSource(data)),
        DirectExecutor,
        DirectExecutor,
        consumer,
        null,
        PositionalDataSource.InitialResult(data.subList(start, end), start, data.size)
    )

    @Test
    fun simplePagerAppend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 2, 6)

        assertTrue(consumer.takeResults().isEmpty())
        assertTrue(consumer.takeStateChanges().isEmpty())

        pager.tryScheduleAppend()

        assertTrue(consumer.takeResults().isEmpty())
        assertEquals(
            consumer.takeStateChanges(), listOf(
                StateChange(END, PagedList.LoadState.LOADING)
            )
        )

        testExecutor.executeAll()

        assertEquals(
            listOf(Result(END, rangeResult(6, 8))),
            consumer.takeResults()
        )
        assertEquals(
            listOf(StateChange(END, IDLE)),
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
            consumer.takeStateChanges(), listOf(
                StateChange(START, LOADING)
            )
        )

        testExecutor.executeAll()

        assertEquals(
            listOf(Result(START, rangeResult(2, 4))),
            consumer.takeResults()
        )
        assertEquals(
            listOf(StateChange(START, IDLE)),
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
                Result(END, rangeResult(6, 8)),
                Result(END, rangeResult(8, 9))
            ), consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(END, LOADING),
                StateChange(END, IDLE),
                StateChange(END, LOADING),
                StateChange(END, IDLE)
            ), consumer.takeStateChanges()
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
                Result(START, rangeResult(2, 4)),
                Result(START, rangeResult(0, 2))
            ), consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(START, LOADING),
                StateChange(START, IDLE),
                StateChange(START, LOADING),
                StateChange(START, IDLE)
            ), consumer.takeStateChanges()
        )
    }

    @Test
    fun emptyAppend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 0, 9)

        pager.tryScheduleAppend()

        // Pager triggers an immediate empty response here, so we don't need to flush the executor
        assertEquals(
            listOf(
                Result(END, DataSource.BaseResult.empty())
            ), consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(END, DONE)
            ), consumer.takeStateChanges()
        )
    }

    @Test
    fun emptyPrepend() {
        val consumer = MockConsumer()
        val pager = createPager(consumer, 0, 9)

        pager.trySchedulePrepend()

        // Pager triggers an immediate empty response here, so we don't need to flush the executor
        assertEquals(
            listOf(
                Result(START, DataSource.BaseResult.empty())
            ), consumer.takeResults()
        )
        assertEquals(
            listOf(
                StateChange(START, DONE)
            ), consumer.takeStateChanges()
        )
    }
}
