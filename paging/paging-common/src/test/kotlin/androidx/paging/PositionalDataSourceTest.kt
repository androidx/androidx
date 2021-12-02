/*
 * Copyright 2017 The Android Open Source Project
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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.concurrent.Executor

@Suppress("DEPRECATION")
@RunWith(JUnit4::class)
class PositionalDataSourceTest {
    private fun computeInitialLoadPos(
        requestedStartPosition: Int,
        requestedLoadSize: Int,
        pageSize: Int,
        totalCount: Int
    ): Int {
        val params = PositionalDataSource.LoadInitialParams(
            requestedStartPosition, requestedLoadSize, pageSize, true
        )
        return PositionalDataSource.computeInitialLoadPosition(params, totalCount)
    }

    @Test
    fun computeInitialLoadPositionZero() {
        assertEquals(
            0,
            computeInitialLoadPos(
                requestedStartPosition = 0,
                requestedLoadSize = 30,
                pageSize = 10,
                totalCount = 100
            )
        )
    }

    @Test
    fun computeInitialLoadPositionRequestedPositionIncluded() {
        assertEquals(
            10,
            computeInitialLoadPos(
                requestedStartPosition = 10,
                requestedLoadSize = 10,
                pageSize = 10,
                totalCount = 100
            )
        )
    }

    @Test
    fun computeInitialLoadPositionRound() {
        assertEquals(
            10,
            computeInitialLoadPos(
                requestedStartPosition = 13,
                requestedLoadSize = 30,
                pageSize = 10,
                totalCount = 100
            )
        )
    }

    @Test
    fun computeInitialLoadPositionEndAdjusted() {
        assertEquals(
            70,
            computeInitialLoadPos(
                requestedStartPosition = 99,
                requestedLoadSize = 30,
                pageSize = 10,
                totalCount = 100
            )
        )
    }

    @Test
    fun computeInitialLoadPositionEndAdjustedAndAligned() {
        assertEquals(
            70,
            computeInitialLoadPos(
                requestedStartPosition = 99,
                requestedLoadSize = 35,
                pageSize = 10,
                totalCount = 100
            )
        )
    }

    private fun validatePositionOffset(enablePlaceholders: Boolean) = runBlocking {
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(enablePlaceholders)
            .build()
        val success = mutableListOf(false)
        val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
                if (enablePlaceholders) {
                    // 36 - ((10 * 3) / 2) = 21, round down to 20
                    assertEquals(20, params.requestedStartPosition)
                } else {
                    // 36 - ((10 * 3) / 2) = 21, no rounding
                    assertEquals(21, params.requestedStartPosition)
                }

                callback.onResult(listOf("a", "b"), 0, 2)
                success[0] = true
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
                fail("loadRange not expected")
            }
        }

        @Suppress("DEPRECATION")
        PagedList.Builder(dataSource, config)
            .setFetchExecutor(Executor { it.run() })
            .setNotifyExecutor(Executor { it.run() })
            .setInitialKey(36)
            .build()
        assertTrue(success[0])
    }

    @Test
    fun initialPositionOffset() {
        validatePositionOffset(true)
    }

    @Test
    fun initialPositionOffsetAsContiguous() {
        validatePositionOffset(false)
    }

    private fun performLoadInitial(
        enablePlaceholders: Boolean = true,
        invalidateDataSource: Boolean = false,
        callbackInvoker: (callback: PositionalDataSource.LoadInitialCallback<String>) -> Unit
    ) {
        val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
                if (invalidateDataSource) {
                    // invalidate data source so it's invalid when onResult() called
                    invalidate()
                }
                callbackInvoker(callback)
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
                fail("loadRange not expected")
            }
        }

        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(enablePlaceholders)
            .build()

        val params = PositionalDataSource.LoadInitialParams(
            0,
            config.initialLoadSizeHint,
            config.pageSize,
            config.enablePlaceholders
        )

        runBlocking { dataSource.loadInitial(params) }
    }

    @Test
    fun initialLoadCallbackSuccess() = performLoadInitial {
        // LoadInitialCallback correct usage
        it.onResult(listOf("a", "b"), 0, 2)
    }

    @Test(expected = IllegalStateException::class)
    fun initialLoadCallbackRequireTotalCount() = performLoadInitial(enablePlaceholders = true) {
        // LoadInitialCallback requires 3 args when placeholders enabled
        it.onResult(listOf("a", "b"), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackNotPageSizeMultiple() = performLoadInitial {
        // Positional LoadInitialCallback can't accept result that's not a multiple of page size
        val elevenLetterList = List(11) { index -> "" + ('a' + index) }
        it.onResult(elevenLetterList, 0, 12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackListTooBig() = performLoadInitial {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b", "c"), 0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackPositionTooLarge() = performLoadInitial {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b"), 1, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackPositionNegative() = performLoadInitial {
        // LoadInitialCallback can't accept negative position
        it.onResult(listOf("a", "b", "c"), -1, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackEmptyCannotHavePlaceholders() = performLoadInitial {
        // LoadInitialCallback can't accept empty result unless data set is empty
        it.onResult(emptyList(), 0, 2)
    }

    @Test
    fun initialLoadCallbackSuccessTwoArg() = performLoadInitial(enablePlaceholders = false) {
        // LoadInitialCallback correct 2 arg usage
        it.onResult(listOf("a", "b"), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackPosNegativeTwoArg() = performLoadInitial(enablePlaceholders = false) {
        // LoadInitialCallback can't accept negative position
        it.onResult(listOf("a", "b"), -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackEmptyWithOffset() = performLoadInitial(enablePlaceholders = false) {
        // LoadInitialCallback can't accept empty result unless pos is 0
        it.onResult(emptyList(), 1)
    }

    @Test
    fun initialLoadCallbackInvalidTwoArg() = performLoadInitial(invalidateDataSource = true) {
        // LoadInitialCallback doesn't throw on invalid args if DataSource is invalid
        it.onResult(emptyList(), 1)
    }

    @Test
    fun initialLoadCallbackInvalidThreeArg() = performLoadInitial(invalidateDataSource = true) {
        // LoadInitialCallback doesn't throw on invalid args if DataSource is invalid
        it.onResult(emptyList(), 0, 1)
    }

    private abstract class WrapperDataSource<in A : Any, B : Any>(
        private val source: PositionalDataSource<A>
    ) : PositionalDataSource<B>() {
        override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            source.addInvalidatedCallback(onInvalidatedCallback)
        }

        override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            source.removeInvalidatedCallback(onInvalidatedCallback)
        }

        override fun invalidate() = source.invalidate()

        override val isInvalid
            get() = source.isInvalid

        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<B>) {
            source.loadInitial(
                params,
                object : LoadInitialCallback<A>() {
                    override fun onResult(data: List<A>, position: Int, totalCount: Int) {
                        callback.onResult(convert(data), position, totalCount)
                    }

                    override fun onResult(data: List<A>, position: Int) {
                        callback.onResult(convert(data), position)
                    }
                }
            )
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<B>) {
            source.loadRange(
                params,
                object : LoadRangeCallback<A>() {
                    override fun onResult(data: List<A>) {
                        callback.onResult(convert(data))
                    }
                }
            )
        }

        protected abstract fun convert(source: List<A>): List<B>
    }

    private class StringWrapperDataSource<in A : Any>(source: PositionalDataSource<A>) :
        WrapperDataSource<A, String>(source) {
        override fun convert(source: List<A>): List<String> {
            return source.map { it.toString() }
        }
    }

    class ListDataSource<T : Any>(
        val list: List<T>,
        val counted: Boolean = true
    ) : PositionalDataSource<T> () {
        private var error = false

        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
            if (error) {
                error = false
                return
            }
            val totalCount = list.size

            val position = computeInitialLoadPosition(params, totalCount)
            val loadSize = computeInitialLoadSize(params, position, totalCount)

            // for simplicity, we could return everything immediately,
            // but we tile here since it's expected behavior
            val sublist = list.subList(position, position + loadSize)

            if (counted) {
                callback.onResult(sublist, position, totalCount)
            } else {
                callback.onResult(sublist, position)
            }
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
            if (error) {
                error = false
                return
            }
            callback.onResult(
                list.subList(params.startPosition, params.startPosition + params.loadSize)
            )
        }

        fun enqueueError() {
            error = true
        }
    }

    private fun verifyWrappedDataSource(
        createWrapper: (PositionalDataSource<Int>) -> PositionalDataSource<String>
    ) {
        val orig = ListDataSource(listOf(0, 5, 4, 8, 12))
        val wrapper = createWrapper(orig)

        // load initial
        @Suppress("UNCHECKED_CAST")
        val loadInitialCallback = mock(PositionalDataSource.LoadInitialCallback::class.java)
            as PositionalDataSource.LoadInitialCallback<String>
        val initParams = PositionalDataSource.LoadInitialParams(0, 2, 1, true)
        wrapper.loadInitial(initParams, loadInitialCallback)
        verify(loadInitialCallback).onResult(listOf("0", "5"), 0, 5)
        // load initial - error
        orig.enqueueError()
        wrapper.loadInitial(initParams, loadInitialCallback)
        verifyNoMoreInteractions(loadInitialCallback)

        // load range
        @Suppress("UNCHECKED_CAST")
        val loadRangeCallback = mock(PositionalDataSource.LoadRangeCallback::class.java)
            as PositionalDataSource.LoadRangeCallback<String>
        wrapper.loadRange(PositionalDataSource.LoadRangeParams(2, 3), loadRangeCallback)
        verify(loadRangeCallback).onResult(listOf("4", "8", "12"))
        // load range - error
        orig.enqueueError()
        wrapper.loadRange(PositionalDataSource.LoadRangeParams(2, 3), loadRangeCallback)
        verifyNoMoreInteractions(loadRangeCallback)

        // check invalidation behavior
        val invalidCallback = mock(DataSource.InvalidatedCallback::class.java)
        wrapper.addInvalidatedCallback(invalidCallback)
        orig.invalidate()
        verify(invalidCallback).onInvalidated()
        verifyNoMoreInteractions(invalidCallback)

        // verify invalidation
        orig.invalidate()
        assertTrue(wrapper.isInvalid)
    }

    @Test
    fun testManualWrappedDataSource() = verifyWrappedDataSource {
        StringWrapperDataSource(it)
    }

    @Test
    fun testListConverterWrappedDataSource() = verifyWrappedDataSource { dataSource ->
        dataSource.mapByPage { page -> page.map { it.toString() } }
    }

    @Test
    fun testItemConverterWrappedDataSource() = verifyWrappedDataSource { dataSource ->
        dataSource.map { it.toString() }
    }

    @Test
    fun testInvalidateToWrapper() {
        val orig = ListDataSource(listOf(0, 1, 2))
        val wrapper = orig.map { it.toString() }

        orig.invalidate()
        assertTrue(wrapper.isInvalid)
    }

    @Test
    fun testInvalidateFromWrapper() {
        val orig = ListDataSource(listOf(0, 1, 2))
        val wrapper = orig.map { it.toString() }

        wrapper.invalidate()
        assertTrue(orig.isInvalid)
    }

    @Test
    fun addRemoveInvalidateFunction() {
        val datasource = ListDataSource(listOf(0, 1, 2))
        val noopCallback = { }
        datasource.addInvalidatedCallback(noopCallback)
        assert(datasource.invalidateCallbackCount == 1)
        datasource.removeInvalidatedCallback { }
        assert(datasource.invalidateCallbackCount == 1)
        datasource.removeInvalidatedCallback(noopCallback)
        assert(datasource.invalidateCallbackCount == 0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testScope = TestCoroutineScope()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun verifyRefreshIsTerminal(counted: Boolean): Unit = testScope.runBlockingTest {
        val dataSource = ListDataSource(list = listOf(0, 1, 2), counted = counted)
        dataSource.load(
            DataSource.Params(
                type = LoadType.REFRESH,
                key = 0,
                initialLoadSize = 3,
                placeholdersEnabled = false,
                pageSize = 1
            )
        ).apply {
            assertEquals(listOf(0, 1, 2), data)
            // prepends always return prevKey = null if they return the first item
            assertEquals(null, prevKey)
            // appends only return nextKey if they return the last item, and are counted
            assertEquals(if (counted) null else 3, nextKey)
        }

        dataSource.load(
            DataSource.Params(
                type = LoadType.PREPEND,
                key = 1,
                initialLoadSize = 3,
                placeholdersEnabled = false,
                pageSize = 1
            )
        ).apply {
            // prepends should return prevKey = null if they return the first item
            assertEquals(listOf(0), data)
            assertEquals(null, prevKey)
            assertEquals(1, nextKey)
        }
    }

    @Test
    fun terminalResultCounted() = verifyRefreshIsTerminal(counted = true)

    @Test
    fun terminalResultUncounted() = verifyRefreshIsTerminal(counted = false)
}
