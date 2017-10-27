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

package android.arch.paging

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import java.util.Collections

@RunWith(Parameterized::class)
class ContiguousPagedListTest(private val mCounted: Boolean) {
    private val mMainThread = TestExecutor()
    private val mBackgroundThread = TestExecutor()

    private class Item(position: Int) {
        val name: String = "Item $position"

        override fun toString(): String {
            return name
        }
    }

    private inner class TestSource(val listData: List<Item> = ITEMS)
            : PositionalDataSource<Item>() {
        override fun countItems(): Int {
            return if (mCounted) {
                listData.size
            } else {
                DataSource.COUNT_UNDEFINED
            }
        }

        private fun getClampedRange(startInc: Int, endExc: Int, reverse: Boolean): List<Item> {
            val list = listData.subList(Math.max(0, startInc), Math.min(listData.size, endExc))
            if (reverse) {
                Collections.reverse(list)
            }
            return list
        }

        override fun loadAfter(startIndex: Int, pageSize: Int): List<Item>? {
            return getClampedRange(startIndex, startIndex + pageSize, false)
        }

        override fun loadBefore(startIndex: Int, pageSize: Int): List<Item>? {
            return getClampedRange(startIndex - pageSize + 1, startIndex + 1, true)
        }
    }

    private fun verifyRange(start: Int, count: Int, actual: PagedStorage<*, Item>) {
        if (mCounted) {
            // assert nulls + content
            val expected = arrayOfNulls<Item>(ITEMS.size)
            System.arraycopy(ITEMS.toTypedArray(), start, expected, start, count)
            assertArrayEquals(expected, actual.toTypedArray())

            val expectedTrailing = ITEMS.size - start - count
            assertEquals(ITEMS.size, actual.size)
            assertEquals((ITEMS.size - start - expectedTrailing),
                    actual.storageCount)
            assertEquals(start, actual.leadingNullCount)
            assertEquals(expectedTrailing, actual.trailingNullCount)

        } else {
            assertEquals(ITEMS.subList(start, start + count), actual)

            assertEquals(count, actual.size)
            assertEquals(actual.size, actual.storageCount)
            assertEquals(0, actual.leadingNullCount)
            assertEquals(0, actual.trailingNullCount)
        }
    }

    private fun verifyRange(start: Int, count: Int, actual: PagedList<Item>) {
        verifyRange(start, count, actual.mStorage)
    }

    private fun verifyRange(start: Int, count: Int, actual: PageResult<Int, Item>) {
        if (mCounted) {
            assertEquals(start, actual.leadingNulls)
            assertEquals(ITEMS.size - start - count, actual.trailingNulls)
            assertEquals(0, actual.positionOffset)
        } else {
            assertEquals(0, actual.leadingNulls)
            assertEquals(0, actual.trailingNulls)
            assertEquals(start, actual.positionOffset)
        }
        assertEquals(ITEMS.subList(start, start + count), actual.page.items)
    }

    private fun verifyInitialLoad(start: Int, count : Int, initialPosition: Int, initialLoadSize: Int) {
        @Suppress("UNCHECKED_CAST")
        val receiver = mock(PageResult.Receiver::class.java) as PageResult.Receiver<Int, Item>

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(PageResult::class.java)
                as ArgumentCaptor<PageResult<Int, Item>>

        TestSource().loadInitial(initialPosition, initialLoadSize, true, receiver)

        verify(receiver).onPageResult(captor.capture())
        verifyNoMoreInteractions(receiver)
        verifyRange(start, count, captor.value)
    }

    @Test
    fun initialLoad() {
        verifyInitialLoad(30, 40, 50, 40)
        verifyInitialLoad(0, 10, 5, 10)
        verifyInitialLoad(90, 10, 95, 10)
    }

    private fun createCountedPagedList(
            initialPosition: Int,
            pageSize: Int = 20,
            initLoadSize: Int = 40,
            prefetchDistance: Int = 20,
            listData: List<Item> = ITEMS,
            boundaryCallback: PagedList.BoundaryCallback<Item>? = null)
            : ContiguousPagedList<Int, Item> {
        return ContiguousPagedList(
                TestSource(listData), mMainThread, mBackgroundThread, boundaryCallback,
                PagedList.Config.Builder()
                        .setInitialLoadSizeHint(initLoadSize)
                        .setPageSize(pageSize)
                        .setPrefetchDistance(prefetchDistance)
                        .build(),
                initialPosition)
    }

    @Test
    fun construct() {
        val pagedList = createCountedPagedList(0)
        verifyRange(0, 40, pagedList)
    }

    private fun verifyCallback(callback: PagedList.Callback, countedPosition: Int,
            uncountedPosition: Int) {
        if (mCounted) {
            verify(callback).onChanged(countedPosition, 20)
        } else {
            verify(callback).onInserted(uncountedPosition, 20)
        }
    }

    @Test
    fun append() {
        val pagedList = createCountedPagedList(0)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(0, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(35)
        drain()

        verifyRange(0, 60, pagedList)
        verifyCallback(callback, 40, 40)
        verifyNoMoreInteractions(callback)
    }


    @Test
    fun prepend() {
        val pagedList = createCountedPagedList(80)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(60, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(if (mCounted) 65 else 5)
        drain()

        verifyRange(40, 60, pagedList)
        verifyCallback(callback, 40, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun outwards() {
        val pagedList = createCountedPagedList(50)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(30, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(if (mCounted) 65 else 35)
        drain()

        verifyRange(30, 60, pagedList)
        verifyCallback(callback, 70, 40)
        verifyNoMoreInteractions(callback)

        pagedList.loadAround(if (mCounted) 35 else 5)
        drain()

        verifyRange(10, 80, pagedList)
        verifyCallback(callback, 10, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun multiAppend() {
        val pagedList = createCountedPagedList(0)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(0, 40, pagedList)
        verifyZeroInteractions(callback)

        pagedList.loadAround(55)
        drain()

        verifyRange(0, 80, pagedList)
        verifyCallback(callback, 40, 40)
        verifyCallback(callback, 60, 60)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun distantPrefetch() {
        val pagedList = createCountedPagedList(0,
                initLoadSize = 10, pageSize = 10, prefetchDistance = 30)
        val callback = mock(PagedList.Callback::class.java)
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
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(snapshot, callback)
        verifyCallback(callback, 60, 60)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependCallbackAddedLate() {
        val pagedList = createCountedPagedList(80)
        verifyRange(60, 40, pagedList)

        pagedList.loadAround(if (mCounted) 65 else 5)
        drain()
        verifyRange(40, 60, pagedList)

        // snapshot at 60 items
        val snapshot = pagedList.snapshot() as PagedList<Item>
        verifyRange(40, 60, snapshot)

        pagedList.loadAround(if (mCounted) 45 else 5)
        drain()
        verifyRange(20, 80, pagedList)
        verifyRange(40, 60, snapshot)

        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(snapshot, callback)
        verifyCallback(callback, 40, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun boundaryCallback_empty() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val pagedList = createCountedPagedList(0,
                listData = ArrayList(), boundaryCallback = boundaryCallback)
        assertEquals(0, pagedList.size)

        // nothing yet
        verifyNoMoreInteractions(boundaryCallback)

        // onZeroItemsLoaded posted, since creation often happens on BG thread
        drain()
        verify(boundaryCallback).onZeroItemsLoaded()
        verifyNoMoreInteractions(boundaryCallback)
    }

    @Test
    fun boundaryCallback_delayed() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val pagedList = createCountedPagedList(90,
                initLoadSize = 20, prefetchDistance = 5, boundaryCallback = boundaryCallback)
        verifyRange(80, 20, pagedList)


        // nothing yet
        verifyZeroInteractions(boundaryCallback)
        drain()
        verifyZeroInteractions(boundaryCallback)

        // loading around last item causes onItemAtEndLoaded
        pagedList.loadAround(if (mCounted) 99 else 19)
        drain()
        verifyRange(80, 20, pagedList)
        verify(boundaryCallback).onItemAtEndLoaded(
                any(), eq(ITEMS.last()), eq(if (mCounted) 100 else 20))
        verifyNoMoreInteractions(boundaryCallback)


        // prepending doesn't trigger callback...
        pagedList.loadAround(if (mCounted) 80 else 0)
        drain()
        verifyRange(60, 40, pagedList)
        verifyZeroInteractions(boundaryCallback)

        // ...load rest of data, still no dispatch...
        pagedList.loadAround(if (mCounted) 60 else 0)
        drain()
        pagedList.loadAround(if (mCounted) 40 else 0)
        drain()
        pagedList.loadAround(if (mCounted) 20 else 0)
        drain()
        verifyRange(0, 100, pagedList)
        verifyZeroInteractions(boundaryCallback)

        // ... finally try prepend, see 0 items, which will dispatch front callback
        pagedList.loadAround(0)
        drain()
        verify(boundaryCallback).onItemAtFrontLoaded(any(), eq(ITEMS.first()), eq(100))
        verifyNoMoreInteractions(boundaryCallback)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = mBackgroundThread.executeAll()
            executed = mMainThread.executeAll() || executed
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
