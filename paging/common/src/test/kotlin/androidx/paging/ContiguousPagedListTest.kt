/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.paging.PagedList.LoadState.IDLE
import androidx.paging.PagedList.LoadState.LOADING
import androidx.paging.PagedList.LoadState.RETRYABLE_ERROR
import androidx.paging.futures.DirectExecutor
import androidx.testutils.TestExecutor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ContiguousPagedListTest(private val placeholdersEnabled: Boolean) {
    private val mainThread = TestExecutor()
    private val backgroundThread = TestExecutor()

    private class Item(position: Int) {
        val pos: Int = position
        val name: String = "Item $position"

        override fun toString(): String {
            return name
        }
    }

    /**
     * Note: we use a non-positional dataSource here because we want to avoid the initial load size
     * and alignment restrictions. These tests were written before positional+contiguous enforced
     * these behaviors.
     */
    private inner class TestSource(val listData: List<Item> = ITEMS) :
        ItemKeyedDataSource<Int, Item>() {
        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Item>
        ) {
            val initPos = params.requestedInitialKey ?: 0
            val start = maxOf(initPos - params.requestedLoadSize / 2, 0)

            val result = getClampedRange(start, start + params.requestedLoadSize)
            if (result == null) {
                callback.onError(Exception())
            } else {
                if (placeholdersEnabled) {
                    callback.onResult(result, start, listData.size)
                } else {
                    callback.onResult(result)
                }
            }
        }

        override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Item>) {
            val result = getClampedRange(params.key + 1, params.key + 1 + params.requestedLoadSize)
            if (result == null) {
                callback.onError(Exception())
            } else {
                callback.onResult(result)
            }
        }

        override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Item>) {
            val result = getClampedRange(params.key - params.requestedLoadSize, params.key)
            if (result == null) {
                callback.onError(Exception())
            } else {
                callback.onResult(result)
            }
        }

        override fun getKey(item: Item): Int = item.pos

        private fun getClampedRange(startInc: Int, endExc: Int): List<Item>? {
            val matching = errorIndices.filter { it in startInc until endExc }
            if (matching.isNotEmpty()) {
                // found indices with errors enqueued - fail to load them
                errorIndices.removeAll(matching)
                return null
            }
            return listData.subList(maxOf(0, startInc), minOf(listData.size, endExc))
        }

        fun enqueueErrorForIndex(index: Int) {
            errorIndices.add(index)
        }

        val errorIndices = mutableListOf<Int>()

        override fun isRetryableError(error: Throwable): Boolean {
            return true
        }
    }

    private fun DataSource<*, Item>.enqueueErrorForIndex(index: Int) {
        (this as TestSource).enqueueErrorForIndex(index)
    }

    private fun <E> MutableList<E>.getAllAndClear(): List<E> {
        val data = this.toList()
        this.clear()
        return data
    }

    private fun <E : Any> PagedList<E>.addLoadStateCapture(desiredType: PagedList.LoadType):
            MutableList<PagedList.LoadState> {
        val list = mutableListOf<PagedList.LoadState>()
        this.addWeakLoadStateListener { type, state, _ ->
            if (type == desiredType) {
                list.add(state)
            }
        }
        return list
    }

    private fun verifyRange(start: Int, count: Int, actual: PagedStorage<Item>) {
        if (placeholdersEnabled) {
            // assert nulls + content
            val expected = arrayOfNulls<Item>(ITEMS.size)
            System.arraycopy(ITEMS.toTypedArray(), start, expected, start, count)
            assertEquals(expected.toList(), actual)

            val expectedTrailing = ITEMS.size - start - count
            assertEquals(ITEMS.size, actual.size)
            assertEquals(start, actual.leadingNullCount)
            assertEquals(expectedTrailing, actual.trailingNullCount)
        } else {
            assertEquals(ITEMS.subList(start, start + count), actual)

            assertEquals(count, actual.size)
            assertEquals(0, actual.leadingNullCount)
            assertEquals(0, actual.trailingNullCount)
        }
        assertEquals(count, actual.storageCount)
    }

    private fun verifyRange(start: Int, count: Int, actual: PagedList<Item>) {
        verifyRange(start, count, actual.storage)
        assertEquals(count, actual.loadedCount)
    }

    private fun createCountedPagedList(
        initialPosition: Int?,
        pageSize: Int = 20,
        initLoadSize: Int = 40,
        prefetchDistance: Int = 20,
        listData: List<Item> = ITEMS,
        boundaryCallback: PagedList.BoundaryCallback<Item>? = null,
        maxSize: Int = PagedList.Config.MAX_SIZE_UNBOUNDED,
        dataSource: DataSource<Int, Item> = TestSource(listData)
    ): ContiguousPagedList<Int, Item> {
        val ret = runBlocking {
            PagedList.create(
                dataSource,
                GlobalScope,
                mainThread,
                backgroundThread,
                DirectExecutor,
                boundaryCallback,
                PagedList.Config.Builder()
                    .setPageSize(pageSize)
                    .setInitialLoadSizeHint(initLoadSize)
                    .setPrefetchDistance(prefetchDistance)
                    .setMaxSize(maxSize)
                    .setEnablePlaceholders(placeholdersEnabled)
                    .build(),
                initialPosition
            )
        }
        @Suppress("UNCHECKED_CAST")
        return ret as ContiguousPagedList<Int, Item>
    }

    @Test
    fun construct() {
        val pagedList = createCountedPagedList(0)
        verifyRange(0, 40, pagedList)
    }

    @Test
    fun getDataSource() {
        val pagedList = createCountedPagedList(0)
        assertTrue(pagedList.dataSource is TestSource)

        // snapshot keeps same DataSource
        assertSame(
            pagedList.dataSource,
            (pagedList.snapshot() as SnapshotPagedList<Item>).dataSource
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun loadAroundNegative() {
        val pagedList = createCountedPagedList(0)
        pagedList.loadAround(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun loadAroundTooLarge() {
        val pagedList = createCountedPagedList(0)
        pagedList.loadAround(pagedList.size)
    }

    private fun verifyCallback(
        callback: PagedList.Callback,
        countedPosition: Int,
        uncountedPosition: Int
    ) {
        if (placeholdersEnabled) {
            verify(callback).onChanged(countedPosition, 20)
        } else {
            verify(callback).onInserted(uncountedPosition, 20)
        }
    }

    private fun verifyCallback(callback: PagedList.Callback, position: Int) {
        verifyCallback(callback, position, position)
    }

    private fun verifyDropCallback(
        callback: PagedList.Callback,
        countedPosition: Int,
        uncountedPosition: Int
    ) {
        if (placeholdersEnabled) {
            verify(callback).onChanged(countedPosition, 20)
        } else {
            verify(callback).onRemoved(uncountedPosition, 20)
        }
    }

    private fun verifyDropCallback(callback: PagedList.Callback, position: Int) {
        verifyDropCallback(callback, position, position)
    }

    @Test
    fun append() {
        val pagedList = createCountedPagedList(0)
        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)
        verifyRange(0, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(35)
        drain()

        verifyRange(0, 60, pagedList)
        verifyCallback(callback, 40)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prepend() {
        val pagedList = createCountedPagedList(80)
        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)
        verifyRange(60, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(if (placeholdersEnabled) 65 else 5)
        drain()

        verifyRange(40, 60, pagedList)
        verifyCallback(callback, 40, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun outwards() {
        val pagedList = createCountedPagedList(40)
        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)
        verifyRange(20, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(if (placeholdersEnabled) 55 else 35)
        drain()

        verifyRange(20, 60, pagedList)
        verifyCallback(callback, 60, 40)
        verifyNoMoreInteractions(callback)

        pagedList.loadAround(if (placeholdersEnabled) 25 else 5)
        drain()

        verifyRange(0, 80, pagedList)
        verifyCallback(callback, 0, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prefetchRequestedPrepend() {
        assertEquals(10, ContiguousPagedList.getPrependItemsRequested(10, 0, 0))
        assertEquals(15, ContiguousPagedList.getPrependItemsRequested(10, 0, 5))
        assertEquals(0, ContiguousPagedList.getPrependItemsRequested(1, 41, 40))
        assertEquals(1, ContiguousPagedList.getPrependItemsRequested(1, 40, 40))
    }

    @Test
    fun prefetchRequestedAppend() {
        assertEquals(10, ContiguousPagedList.getAppendItemsRequested(10, 9, 10))
        assertEquals(15, ContiguousPagedList.getAppendItemsRequested(10, 9, 5))
        assertEquals(0, ContiguousPagedList.getAppendItemsRequested(1, 8, 10))
        assertEquals(1, ContiguousPagedList.getAppendItemsRequested(1, 9, 10))
    }

    @Test
    fun prefetchFront() {
        val pagedList = createCountedPagedList(
            initialPosition = 50,
            pageSize = 20,
            initLoadSize = 20,
            prefetchDistance = 1
        )
        verifyRange(40, 20, pagedList)

        // access adjacent to front, shouldn't trigger prefetch
        pagedList.loadAround(if (placeholdersEnabled) 41 else 1)
        drain()
        verifyRange(40, 20, pagedList)

        // access front item, should trigger prefetch
        pagedList.loadAround(if (placeholdersEnabled) 40 else 0)
        drain()
        verifyRange(20, 40, pagedList)
    }

    @Test
    fun prefetchEnd() {
        val pagedList = createCountedPagedList(
            initialPosition = 50,
            pageSize = 20,
            initLoadSize = 20,
            prefetchDistance = 1
        )
        verifyRange(40, 20, pagedList)

        // access adjacent from end, shouldn't trigger prefetch
        pagedList.loadAround(if (placeholdersEnabled) 58 else 18)
        drain()
        verifyRange(40, 20, pagedList)

        // access end item, should trigger prefetch
        pagedList.loadAround(if (placeholdersEnabled) 59 else 19)
        drain()
        verifyRange(40, 40, pagedList)
    }

    @Test
    fun pageDropEnd() {
        val pagedList = createCountedPagedList(
            initialPosition = 0,
            pageSize = 20,
            initLoadSize = 20,
            prefetchDistance = 1,
            maxSize = 70
        )
        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)
        verifyRange(0, 20, pagedList)
        verifyZeroInteractions(callback)

        // load 2nd page
        pagedList.loadAround(19)
        drain()
        verifyRange(0, 40, pagedList)
        verifyCallback(callback, 20)
        verifyNoMoreInteractions(callback)

        // load 3rd page
        pagedList.loadAround(39)
        drain()
        verifyRange(0, 60, pagedList)
        verifyCallback(callback, 40)
        verifyNoMoreInteractions(callback)

        // load 4th page, drop 1st
        pagedList.loadAround(59)
        drain()
        verifyRange(20, 60, pagedList)
        verifyCallback(callback, 60)
        verifyDropCallback(callback, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun pageDropFront() {
        val pagedList = createCountedPagedList(
            initialPosition = 90,
            pageSize = 20,
            initLoadSize = 20,
            prefetchDistance = 1,
            maxSize = 70
        )
        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)
        verifyRange(80, 20, pagedList)
        verifyZeroInteractions(callback)

        // load 4th page
        pagedList.loadAround(if (placeholdersEnabled) 80 else 0)
        drain()
        verifyRange(60, 40, pagedList)
        verifyCallback(callback, 60, 0)
        verifyNoMoreInteractions(callback)
        reset(callback)

        // load 3rd page
        pagedList.loadAround(if (placeholdersEnabled) 60 else 0)
        drain()
        verifyRange(40, 60, pagedList)
        verifyCallback(callback, 40, 0)
        verifyNoMoreInteractions(callback)
        reset(callback)

        // load 2nd page, drop 5th
        pagedList.loadAround(if (placeholdersEnabled) 40 else 0)
        drain()
        verifyRange(20, 60, pagedList)
        verifyCallback(callback, 20, 0)
        verifyDropCallback(callback, 80, 60)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun pageDropCancelPrepend() {
        // verify that, based on most recent load position, a prepend can be dropped as it arrives
        val pagedList = createCountedPagedList(
            initialPosition = 2,
            pageSize = 1,
            initLoadSize = 1,
            prefetchDistance = 1,
            maxSize = 3
        )

        // load 3 pages - 2nd, 3rd, 4th
        pagedList.loadAround(if (placeholdersEnabled) 2 else 0)
        drain()
        verifyRange(1, 3, pagedList)

        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)

        // start a load at the beginning...
        pagedList.loadAround(if (placeholdersEnabled) 1 else 0)

        backgroundThread.executeAll()

        // but before page received, access near end of list
        pagedList.loadAround(if (placeholdersEnabled) 3 else 2)
        verifyZeroInteractions(callback)
        mainThread.executeAll()
        // and the load at the beginning is dropped without signaling callback
        verifyNoMoreInteractions(callback)
        verifyRange(1, 3, pagedList)

        drain()
        if (placeholdersEnabled) {
            verify(callback).onChanged(4, 1)
            verify(callback).onChanged(1, 1)
        } else {
            verify(callback).onInserted(3, 1)
            verify(callback).onRemoved(0, 1)
        }
        verifyRange(2, 3, pagedList)
    }

    @Test
    fun pageDropCancelAppend() {
        // verify that, based on most recent load position, an append can be dropped as it arrives
        val pagedList = createCountedPagedList(
            initialPosition = 2,
            pageSize = 1,
            initLoadSize = 1,
            prefetchDistance = 1,
            maxSize = 3
        )

        // load 3 pages - 2nd, 3rd, 4th
        pagedList.loadAround(if (placeholdersEnabled) 2 else 0)
        drain()

        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)

        // start a load at the end...
        pagedList.loadAround(if (placeholdersEnabled) 3 else 2)

        backgroundThread.executeAll()

        // but before page received, access near front of list
        pagedList.loadAround(if (placeholdersEnabled) 1 else 0)
        verifyZeroInteractions(callback)
        mainThread.executeAll()
        // and the load at the end is dropped without signaling callback
        verifyNoMoreInteractions(callback)
        verifyRange(1, 3, pagedList)

        drain()
        if (placeholdersEnabled) {
            verify(callback).onChanged(0, 1)
            verify(callback).onChanged(3, 1)
        } else {
            verify(callback).onInserted(0, 1)
            verify(callback).onRemoved(3, 1)
        }
        verifyRange(0, 3, pagedList)
    }

    @Test
    fun loadingListenerAppend() {
        val pagedList = createCountedPagedList(0)
        val states = pagedList.addLoadStateCapture(PagedList.LoadType.END)

        // No loading going on currently
        assertEquals(listOf(IDLE), states.getAllAndClear())
        verifyRange(0, 40, pagedList)

        // trigger load
        pagedList.loadAround(35)
        mainThread.executeAll()
        assertEquals(listOf(LOADING), states.getAllAndClear())
        verifyRange(0, 40, pagedList)

        // load finishes
        drain()
        assertEquals(listOf(IDLE), states.getAllAndClear())
        verifyRange(0, 60, pagedList)

        pagedList.dataSource.enqueueErrorForIndex(65)

        // trigger load which will error
        pagedList.loadAround(55)
        mainThread.executeAll()
        assertEquals(listOf(LOADING), states.getAllAndClear())
        verifyRange(0, 60, pagedList)

        // load now in error state
        drain()
        assertEquals(listOf(RETRYABLE_ERROR), states.getAllAndClear())
        verifyRange(0, 60, pagedList)

        // retry
        pagedList.retry()
        mainThread.executeAll()
        assertEquals(listOf(LOADING), states.getAllAndClear())

        // load finishes
        drain()
        assertEquals(listOf(IDLE), states.getAllAndClear())
        verifyRange(0, 80, pagedList)
    }

    @Test
    fun pageDropCancelPrependError() {
        // verify a prepend in error state can be dropped
        val pagedList = createCountedPagedList(
            initialPosition = 2,
            pageSize = 1,
            initLoadSize = 1,
            prefetchDistance = 1,
            maxSize = 3
        )
        val states = pagedList.addLoadStateCapture(PagedList.LoadType.START)

        // load 3 pages - 2nd, 3rd, 4th
        pagedList.loadAround(if (placeholdersEnabled) 2 else 0)
        drain()
        verifyRange(1, 3, pagedList)
        assertEquals(listOf(IDLE, LOADING, IDLE), states.getAllAndClear())

        // start a load at the beginning, which will fail
        pagedList.dataSource.enqueueErrorForIndex(0)
        pagedList.loadAround(if (placeholdersEnabled) 1 else 0)
        drain()
        verifyRange(1, 3, pagedList)
        assertEquals(listOf(LOADING, RETRYABLE_ERROR), states.getAllAndClear())

        // but without that failure being retried, access near end of list, which drops the error
        pagedList.loadAround(if (placeholdersEnabled) 3 else 2)
        drain()
        assertEquals(listOf(IDLE), states.getAllAndClear())
        verifyRange(2, 3, pagedList)
    }

    @Test
    fun pageDropCancelAppendError() {
        // verify an append in error state can be dropped
        val pagedList = createCountedPagedList(
            initialPosition = 2,
            pageSize = 1,
            initLoadSize = 1,
            prefetchDistance = 1,
            maxSize = 3
        )
        val states = pagedList.addLoadStateCapture(PagedList.LoadType.END)

        // load 3 pages - 2nd, 3rd, 4th
        pagedList.loadAround(if (placeholdersEnabled) 2 else 0)
        drain()
        verifyRange(1, 3, pagedList)
        assertEquals(listOf(IDLE, LOADING, IDLE), states.getAllAndClear())

        // start a load at the end, which will fail
        pagedList.dataSource.enqueueErrorForIndex(4)
        pagedList.loadAround(if (placeholdersEnabled) 3 else 2)
        drain()
        verifyRange(1, 3, pagedList)
        assertEquals(listOf(LOADING, RETRYABLE_ERROR), states.getAllAndClear())

        // but without that failure being retried, access near start of list, which drops the error
        pagedList.loadAround(if (placeholdersEnabled) 1 else 0)
        drain()
        assertEquals(listOf(IDLE), states.getAllAndClear())
        verifyRange(0, 3, pagedList)
    }

    @Test
    fun errorIntoDrop() {
        // have an error, move loading range, error goes away
        val pagedList = createCountedPagedList(0)
        val states = mutableListOf<PagedList.LoadState>()
        pagedList.addWeakLoadStateListener { type, state, _ ->
            if (type == PagedList.LoadType.END) {
                states.add(state)
            }
        }

        pagedList.dataSource.enqueueErrorForIndex(45)
        pagedList.loadAround(35)
        drain()
        assertEquals(listOf(IDLE, LOADING, RETRYABLE_ERROR), states.getAllAndClear())
        verifyRange(0, 40, pagedList)
    }

    @Test
    fun distantPrefetch() {
        val pagedList = createCountedPagedList(
            0,
            initLoadSize = 10,
            pageSize = 10,
            prefetchDistance = 30
        )

        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(null, callback)
        verifyRange(0, 10, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(5)
        drain()

        verifyRange(0, 40, pagedList)

        pagedList.loadAround(6)
        drain()

        // although our prefetch window moves forward, no new load triggered
        verifyRange(0, 40, pagedList)
    }

    @Test
    fun appendCallbackAddedLate() {
        val pagedList = createCountedPagedList(0)
        verifyRange(0, 40, pagedList)

        pagedList.loadAround(35)
        drain()
        verifyRange(0, 60, pagedList)

        // snapshot at 60 items
        val snapshot = pagedList.snapshot() as PagedList<Item>
        verifyRange(0, 60, snapshot)

        // load more items...
        pagedList.loadAround(55)
        drain()
        verifyRange(0, 80, pagedList)
        verifyRange(0, 60, snapshot)

        // and verify the snapshot hasn't received them
        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(snapshot, callback)
        verifyCallback(callback, 60)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependCallbackAddedLate() {
        val pagedList = createCountedPagedList(80)
        verifyRange(60, 40, pagedList)

        pagedList.loadAround(if (placeholdersEnabled) 65 else 5)
        drain()
        verifyRange(40, 60, pagedList)

        // snapshot at 60 items
        val snapshot = pagedList.snapshot() as PagedList<Item>
        verifyRange(40, 60, snapshot)

        pagedList.loadAround(if (placeholdersEnabled) 45 else 5)
        drain()
        verifyRange(20, 80, pagedList)
        verifyRange(40, 60, snapshot)

        val callback = mock<PagedList.Callback>()
        pagedList.addWeakCallback(snapshot, callback)
        verifyCallback(callback, 40, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun initialLoad_lastLoad() {
        val pagedList = createCountedPagedList(
            initialPosition = 4,
            initLoadSize = 20,
            pageSize = 10,
            dataSource = ListDataSource(ITEMS)
        )
        // With positional DataSource, last load is param passed
        assertEquals(4, pagedList.lastLoad)
        verifyRange(0, 20, pagedList)
    }

    @Test
    fun initialLoad_lastLoadComputed() {
        val pagedList = createCountedPagedList(
            initialPosition = null,
            initLoadSize = 20
        )
        // last load is middle of initial load
        assertEquals(10, pagedList.lastLoad)
        verifyRange(0, 20, pagedList)
    }

    @Test
    fun addWeakCallbackEmpty() {
        val pagedList = createCountedPagedList(0)
        val callback = mock<PagedList.Callback>()
        verifyRange(0, 40, pagedList)

        // capture empty snapshot
        val initSnapshot = pagedList.snapshot()
        assertEquals(pagedList, initSnapshot)

        // verify that adding callback notifies nothing going from empty -> empty
        pagedList.addWeakCallback(initSnapshot, callback)
        verifyZeroInteractions(callback)
        pagedList.removeWeakCallback(callback)

        pagedList.loadAround(35)
        drain()
        verifyRange(0, 60, pagedList)

        // verify that adding callback notifies insert going from empty -> content
        pagedList.addWeakCallback(initSnapshot, callback)
        verifyCallback(callback, 40)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun boundaryCallback_empty() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback = mock<PagedList.BoundaryCallback<Item>>()
        val pagedList = createCountedPagedList(
            0,
            listData = ArrayList(), boundaryCallback = boundaryCallback
        )
        assertEquals(0, pagedList.size)

        // nothing yet
        verifyNoMoreInteractions(boundaryCallback)

        // onZeroItemsLoaded posted, since creation often happens on BG thread
        drain()
        verify(boundaryCallback).onZeroItemsLoaded()
        verifyNoMoreInteractions(boundaryCallback)
    }

    @Test
    fun boundaryCallback_singleInitialLoad() {
        val shortList = ITEMS.subList(0, 4)
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback = mock<PagedList.BoundaryCallback<Item>>()
        val pagedList = createCountedPagedList(
            0, listData = shortList,
            initLoadSize = shortList.size, boundaryCallback = boundaryCallback
        )
        assertEquals(shortList.size, pagedList.size)

        // nothing yet
        verifyNoMoreInteractions(boundaryCallback)

        // onItemAtFrontLoaded / onItemAtEndLoaded posted, since creation often happens on BG thread
        drain()
        pagedList.loadAround(0)
        drain()
        verify(boundaryCallback).onItemAtFrontLoaded(shortList.first())
        verify(boundaryCallback).onItemAtEndLoaded(shortList.last())
        verifyNoMoreInteractions(boundaryCallback)
    }

    @Test
    fun boundaryCallback_delayed() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback = mock<PagedList.BoundaryCallback<Item>>()
        val pagedList = createCountedPagedList(
            90,
            initLoadSize = 20, prefetchDistance = 5, boundaryCallback = boundaryCallback
        )
        verifyRange(80, 20, pagedList)

        // nothing yet
        verifyZeroInteractions(boundaryCallback)
        drain()
        verifyZeroInteractions(boundaryCallback)

        // loading around last item causes onItemAtEndLoaded
        pagedList.loadAround(if (placeholdersEnabled) 99 else 19)
        drain()
        verifyRange(80, 20, pagedList)
        verify(boundaryCallback).onItemAtEndLoaded(ITEMS.last())
        verifyNoMoreInteractions(boundaryCallback)

        // prepending doesn't trigger callback...
        pagedList.loadAround(if (placeholdersEnabled) 80 else 0)
        drain()
        verifyRange(60, 40, pagedList)
        verifyZeroInteractions(boundaryCallback)

        // ...load rest of data, still no dispatch...
        pagedList.loadAround(if (placeholdersEnabled) 60 else 0)
        drain()
        pagedList.loadAround(if (placeholdersEnabled) 40 else 0)
        drain()
        pagedList.loadAround(if (placeholdersEnabled) 20 else 0)
        drain()
        verifyRange(0, 100, pagedList)
        verifyZeroInteractions(boundaryCallback)

        // ... finally try prepend, see 0 items, which will dispatch front callback
        pagedList.loadAround(0)
        drain()
        verify(boundaryCallback).onItemAtFrontLoaded(ITEMS.first())
        verifyNoMoreInteractions(boundaryCallback)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = backgroundThread.executeAll()
            executed = mainThread.executeAll() || executed
        } while (executed)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "counted:{0}")
        fun parameters(): Array<Array<Boolean>> {
            return arrayOf(arrayOf(true), arrayOf(false))
        }

        private val ITEMS = List(100) { Item(it) }
    }
}
