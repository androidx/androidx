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
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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

    private inner class TestSource : PositionalDataSource<Item>() {
        override fun countItems(): Int {
            return if (mCounted) {
                ITEMS.size
            } else {
                DataSource.COUNT_UNDEFINED
            }
        }

        private fun getClampedRange(startInc: Int, endExc: Int, reverse: Boolean): List<Item> {
            val list = ITEMS.subList(Math.max(0, startInc), Math.min(ITEMS.size, endExc))
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

    private fun verifyRange(start: Int, count: Int, actual: NullPaddedList<Item>) {
        if (mCounted) {
            val expectedLeading = start
            val expectedTrailing = ITEMS.size - start - count
            assertEquals(ITEMS.size, actual.size)
            assertEquals((ITEMS.size - expectedLeading - expectedTrailing), actual.loadedCount)
            assertEquals(expectedLeading, actual.leadingNullCount)
            assertEquals(expectedTrailing, actual.trailingNullCount)

            for (i in 0..actual.loadedCount - 1) {
                assertSame(ITEMS[i + start], actual[i + start])
            }
        } else {
            assertEquals(count, actual.size)
            assertEquals(actual.size, actual.loadedCount)
            assertEquals(0, actual.leadingNullCount)
            assertEquals(0, actual.trailingNullCount)

            for (i in 0..actual.loadedCount - 1) {
                assertSame(ITEMS[i + start], actual[i])
            }
        }
    }

    private fun verifyRange(start: Int, count: Int, actual: PagedStorage<*, Item>) {
        if (mCounted) {
            val expected = arrayOfNulls<Item>(ITEMS.size)
            System.arraycopy(ITEMS.toTypedArray(), start, expected, start, count)
            assertArrayEquals(expected, actual.toTypedArray())


            val expectedLeading = start
            val expectedTrailing = ITEMS.size - start - count
            assertEquals(ITEMS.size, actual.size)
            assertEquals((ITEMS.size - expectedLeading - expectedTrailing),
                    actual.storageCount)
            assertEquals(expectedLeading, actual.leadingNullCount)
            assertEquals(expectedTrailing, actual.trailingNullCount)

        } else {
            val expected = arrayOfNulls<Item>(count)
            System.arraycopy(ITEMS.toTypedArray(), start, expected, 0, count)
            assertArrayEquals(expected, actual.toTypedArray())

            assertEquals(count, actual.size)
            assertEquals(actual.size, actual.storageCount)
            assertEquals(0, actual.leadingNullCount)
            assertEquals(0, actual.trailingNullCount)
        }
    }

    private fun verifyRange(start: Int, count: Int, actual: PagedList<Item>) {
        verifyRange(start, count, actual.mStorage)
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
    fun initialLoad() {
        verifyRange(30, 40, TestSource().loadInitial(50, 40, true)!!)

        verifyRange(0, 10, TestSource().loadInitial(5, 10, true)!!)

        verifyRange(90, 10, TestSource().loadInitial(95, 10, true)!!)
    }


    private fun createCountedPagedList(
            config: PagedList.Config, initialPosition: Int): ContiguousPagedList<Int, Item> {
        return ContiguousPagedList(
                TestSource(), mMainThread, mBackgroundThread,
                config,
                initialPosition)
    }

    private fun createCountedPagedList(initialPosition: Int): ContiguousPagedList<Int, Item> {
        return createCountedPagedList(
                PagedList.Config.Builder()
                        .setInitialLoadSizeHint(40)
                        .setPageSize(20)
                        .setPrefetchDistance(20)
                        .build(),
                initialPosition)
    }

    @Test
    fun construct() {
        val pagedList = createCountedPagedList(0)
        verifyRange(0, 40, pagedList)
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
        val pagedList = createCountedPagedList(
                PagedList.Config.Builder()
                        .setInitialLoadSizeHint(10)
                        .setPageSize(10)
                        .setPrefetchDistance(30)
                        .build(),
                0)
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
