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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(JUnit4::class)
class PositionalDataSourceTest {
    private fun computeInitialLoadPos(
            requestedStartPosition: Int,
            requestedLoadSize: Int,
            pageSize: Int,
            totalCount: Int): Int {
        val params = PositionalDataSource.LoadInitialParams(
                requestedStartPosition, requestedLoadSize, pageSize, true)
        return PositionalDataSource.computeInitialLoadPosition(params, totalCount)
    }

    @Test
    fun computeInitialLoadPositionZero() {
        assertEquals(0, computeInitialLoadPos(
                requestedStartPosition = 0,
                requestedLoadSize = 30,
                pageSize = 10,
                totalCount = 100))
    }

    @Test
    fun computeInitialLoadPositionRequestedPositionIncluded() {
        assertEquals(10, computeInitialLoadPos(
                requestedStartPosition = 10,
                requestedLoadSize = 10,
                pageSize = 10,
                totalCount = 100))
    }

    @Test
    fun computeInitialLoadPositionRound() {
        assertEquals(10, computeInitialLoadPos(
                requestedStartPosition = 13,
                requestedLoadSize = 30,
                pageSize = 10,
                totalCount = 100))
    }

    @Test
    fun computeInitialLoadPositionEndAdjusted() {
        assertEquals(70, computeInitialLoadPos(
                requestedStartPosition = 99,
                requestedLoadSize = 30,
                pageSize = 10,
                totalCount = 100))
    }

    @Test
    fun computeInitialLoadPositionEndAdjustedAndAligned() {
        assertEquals(70, computeInitialLoadPos(
                requestedStartPosition = 99,
                requestedLoadSize = 35,
                pageSize = 10,
                totalCount = 100))
    }

    @Test
    fun fullLoadWrappedAsContiguous() {
        // verify that prepend / append work correctly with a PositionalDataSource, made contiguous
        val config = PagedList.Config.Builder()
                .setPageSize(10)
                .setInitialLoadSizeHint(10)
                .setEnablePlaceholders(true)
                .build()
        val dataSource: PositionalDataSource<Int> = ListDataSource((0..99).toList())
        val testExecutor = TestExecutor()
        val pagedList = ContiguousPagedList(dataSource.wrapAsContiguousWithoutPlaceholders(),
                testExecutor, testExecutor, null, config, 15,
                ContiguousPagedList.LAST_LOAD_UNSPECIFIED)

        assertEquals((10..19).toList(), pagedList)

        // prepend + append work correctly
        pagedList.loadAround(5)
        testExecutor.executeAll()
        assertEquals((0..29).toList(), pagedList)

        // and load the rest of the data to be sure further appends work
        for (i in (2..9)) {
            pagedList.loadAround(i * 10 - 5)
            testExecutor.executeAll()
            assertEquals((0..i * 10 + 9).toList(), pagedList)
        }
    }

    private fun performLoadInitial(
            enablePlaceholders: Boolean = true,
            invalidateDataSource: Boolean = false,
            callbackInvoker: (callback: PositionalDataSource.LoadInitialCallback<String>) -> Unit) {
        val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                    params: LoadInitialParams,
                    callback: LoadInitialCallback<String>) {
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
        if (enablePlaceholders) {
            TiledPagedList(dataSource, FailExecutor(), FailExecutor(), null, config, 0)
        } else {
            ContiguousPagedList(dataSource.wrapAsContiguousWithoutPlaceholders(),
                    FailExecutor(), FailExecutor(), null, config, null,
                    ContiguousPagedList.LAST_LOAD_UNSPECIFIED)
        }
    }

    @Test
    fun initialLoadCallbackSuccess() = performLoadInitial {
        // LoadInitialCallback correct usage
        it.onResult(listOf("a", "b"), 0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackNotPageSizeMultiple() = performLoadInitial {
        // Positional LoadInitialCallback can't accept result that's not a multiple of page size
        val elevenLetterList = List(11) { "" + 'a' + it }
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

    @Test(expected = IllegalStateException::class)
    fun initialLoadCallbackRequireTotalCount() = performLoadInitial(enablePlaceholders = true) {
        // LoadInitialCallback requires 3 args when placeholders enabled
        it.onResult(listOf("a", "b"), 0)
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

    private abstract class WrapperDataSource<in A, B>(private val source: PositionalDataSource<A>)
            : PositionalDataSource<B>() {
        override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            source.addInvalidatedCallback(onInvalidatedCallback)
        }

        override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            source.removeInvalidatedCallback(onInvalidatedCallback)
        }

        override fun invalidate() {
            source.invalidate()
        }

        override fun isInvalid(): Boolean {
            return source.isInvalid
        }

        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<B>) {
            source.loadInitial(params, object : LoadInitialCallback<A>() {
                override fun onResult(data: List<A>, position: Int, totalCount: Int) {
                    callback.onResult(convert(data), position, totalCount)
                }

                override fun onResult(data: List<A>, position: Int) {
                    callback.onResult(convert(data), position)
                }
            })
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<B>) {
            source.loadRange(params, object : LoadRangeCallback<A>() {
                override fun onResult(data: List<A>) {
                    callback.onResult(convert(data))
                }
            })
        }

        protected abstract fun convert(source: List<A>): List<B>
    }

    private class StringWrapperDataSource<in A>(source: PositionalDataSource<A>)
            : WrapperDataSource<A, String>(source) {
        override fun convert(source: List<A>): List<String> {
            return source.map { it.toString() }
        }
    }

    private fun verifyWrappedDataSource(
            createWrapper: (PositionalDataSource<Int>) -> PositionalDataSource<String>) {
        val orig = ListDataSource(listOf(0, 5, 4, 8, 12))
        val wrapper = createWrapper(orig)

        // load initial
        @Suppress("UNCHECKED_CAST")
        val loadInitialCallback = mock(PositionalDataSource.LoadInitialCallback::class.java)
                as PositionalDataSource.LoadInitialCallback<String>

        wrapper.loadInitial(PositionalDataSource.LoadInitialParams(0, 2, 1, true),
                loadInitialCallback)
        verify(loadInitialCallback).onResult(listOf("0", "5"), 0, 5)
        verifyNoMoreInteractions(loadInitialCallback)

        // load range
        @Suppress("UNCHECKED_CAST")
        val loadRangeCallback = mock(PositionalDataSource.LoadRangeCallback::class.java)
                as PositionalDataSource.LoadRangeCallback<String>

        wrapper.loadRange(PositionalDataSource.LoadRangeParams(2, 3), loadRangeCallback)
        verify(loadRangeCallback).onResult(listOf("4", "8", "12"))
        verifyNoMoreInteractions(loadRangeCallback)

        // check invalidation behavior
        val invalCallback = mock(DataSource.InvalidatedCallback::class.java)
        wrapper.addInvalidatedCallback(invalCallback)
        orig.invalidate()
        verify(invalCallback).onInvalidated()
        verifyNoMoreInteractions(invalCallback)

        // verify invalidation
        orig.invalidate()
        assertTrue(wrapper.isInvalid)
    }

    @Test
    fun testManualWrappedDataSource() = verifyWrappedDataSource {
        StringWrapperDataSource(it)
    }

    @Test
    fun testListConverterWrappedDataSource() = verifyWrappedDataSource {
        it.mapByPage { it.map { it.toString() } }
    }

    @Test
    fun testItemConverterWrappedDataSource() = verifyWrappedDataSource {
        it.map { it.toString() }
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
    fun testInvalidateToWrapper_contiguous() {
        val orig = ListDataSource(listOf(0, 1, 2))
        val wrapper = orig.wrapAsContiguousWithoutPlaceholders()

        orig.invalidate()
        assertTrue(wrapper.isInvalid)
    }

    @Test
    fun testInvalidateFromWrapper_contiguous() {
        val orig = ListDataSource(listOf(0, 1, 2))
        val wrapper = orig.wrapAsContiguousWithoutPlaceholders()

        wrapper.invalidate()
        assertTrue(orig.isInvalid)
    }
}
