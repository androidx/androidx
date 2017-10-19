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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

@RunWith(JUnit4::class)
class TiledPagedListTest {
    private val mMainThread = TestExecutor()
    private val mBackgroundThread = TestExecutor()

    private class Item(position: Int) {
        val name: String = "Item $position"

        override fun toString(): String {
            return name
        }
    }

    private class TestTiledSource : TiledDataSource<Item>() {
        override fun countItems(): Int {
            return ITEMS.size
        }

        override fun loadRange(startPosition: Int, count: Int): List<Item> {
            val endPosition = Math.min(ITEMS.size, startPosition + count)
            return ITEMS.subList(startPosition, endPosition)
        }
    }

    private fun verifyRange(list: List<Item>, vararg loadedPages: Int) {
        val loadedPageList = loadedPages.asList()
        assertEquals(ITEMS.size, list.size)
        for (i in list.indices) {
            if (loadedPageList.contains(i / PAGE_SIZE)) {
                assertSame("Index $i", ITEMS[i], list[i])
            } else {
                assertNull("Index $i", list[i])
            }
        }
    }

    private fun createTiledPagedList(loadPosition: Int, initPages: Int,
            prefetchDistance: Int = PAGE_SIZE): TiledPagedList<Item> {
        val source = TestTiledSource()
        return TiledPagedList(
                source, mMainThread, mBackgroundThread,
                PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setInitialLoadSizeHint(PAGE_SIZE * initPages)
                        .setPrefetchDistance(prefetchDistance)
                        .build(),
                loadPosition)
    }

    @Test
    fun computeFirstLoadPosition_zero() {
        assertEquals(0, TiledPagedList.computeFirstLoadPosition(0, 30, 10, 100))
    }

    @Test
    fun computeFirstLoadPosition_requestedPositionIncluded() {
        assertEquals(0, TiledPagedList.computeFirstLoadPosition(10, 10, 10, 100))
    }

    @Test
    fun computeFirstLoadPosition_endAdjusted() {
        assertEquals(70, TiledPagedList.computeFirstLoadPosition(99, 30, 10, 100))
    }

    @Test
    fun initialLoad_onePage() {
        val pagedList = createTiledPagedList(0, 1)
        verifyRange(pagedList, 0, 1)
    }

    @Test
    fun initialLoad_onePageOffset() {
        val pagedList = createTiledPagedList(10, 1)
        verifyRange(pagedList, 0, 1)
    }

    @Test
    fun initialLoad_full() {
        val pagedList = createTiledPagedList(0, 100)
        verifyRange(pagedList, 0, 1, 2, 3, 4)
    }

    @Test
    fun initialLoad_end() {
        val pagedList = createTiledPagedList(44, 2)
        verifyRange(pagedList, 3, 4)
    }

    @Test
    fun initialLoad_multiple() {
        val pagedList = createTiledPagedList(9, 2)
        verifyRange(pagedList, 0, 1)
    }

    @Test
    fun initialLoad_offset() {
        val pagedList = createTiledPagedList(41, 2)
        verifyRange(pagedList, 3, 4)
    }

    @Test
    fun append() {
        val pagedList = createTiledPagedList(0, 1)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(pagedList, 0, 1)
        verifyZeroInteractions(callback)

        pagedList.loadAround(15)

        verifyRange(pagedList, 0, 1)

        drain()

        verifyRange(pagedList, 0, 1, 2)
        verify(callback).onChanged(20, 10)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prepend() {
        val pagedList = createTiledPagedList(44, 2)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(pagedList, 3, 4)
        verifyZeroInteractions(callback)

        pagedList.loadAround(35)
        drain()

        verifyRange(pagedList, 2, 3, 4)
        verify<PagedList.Callback>(callback).onChanged(20, 10)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun loadWithGap() {
        val pagedList = createTiledPagedList(0, 1)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(pagedList, 0, 1)
        verifyZeroInteractions(callback)

        pagedList.loadAround(44)
        drain()

        verifyRange(pagedList, 0, 1, 3, 4)
        verify(callback).onChanged(30, 10)
        verify(callback).onChanged(40, 5)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun tinyPrefetchTest() {
        val pagedList = createTiledPagedList(0, 1, 1)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyRange(pagedList, 0, 1)
        verifyZeroInteractions(callback)

        pagedList.loadAround(33)
        drain()

        verifyRange(pagedList, 0, 1, 3)
        verify(callback).onChanged(30, 10)
        verifyNoMoreInteractions(callback)

        pagedList.loadAround(44)
        drain()

        verifyRange(pagedList, 0, 1, 3, 4)
        verify(callback).onChanged(40, 5)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun appendCallbackAddedLate() {
        val pagedList = createTiledPagedList(0, 1, 0)
        verifyRange(pagedList, 0, 1)

        pagedList.loadAround(25)
        drain()
        verifyRange(pagedList, 0, 1, 2)

        // snapshot at 30 items
        val snapshot = pagedList.snapshot()
        verifyRange(snapshot, 0, 1, 2)

        pagedList.loadAround(35)
        pagedList.loadAround(44)
        drain()
        verifyRange(pagedList, 0, 1, 2, 3, 4)
        verifyRange(snapshot, 0, 1, 2)

        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(snapshot, callback)
        verify(callback).onChanged(30, 20)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependCallbackAddedLate() {
        val pagedList = createTiledPagedList(44, 2, 0)
        verifyRange(pagedList, 3, 4)

        pagedList.loadAround(25)
        drain()
        verifyRange(pagedList, 2, 3, 4)

        // snapshot at 30 items
        val snapshot = pagedList.snapshot()
        verifyRange(snapshot, 2, 3, 4)

        pagedList.loadAround(15)
        pagedList.loadAround(5)
        drain()
        verifyRange(pagedList, 0, 1, 2, 3, 4)
        verifyRange(snapshot, 2, 3, 4)

        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(snapshot, callback)
        verify(callback).onChanged(0, 20)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun placeholdersDisabled() {
        // disable placeholders with config, so we create a contiguous version of the pagedlist
        val pagedList = PagedList.Builder<Int, Item>()
                .setDataSource(TestTiledSource())
                .setMainThreadExecutor(mMainThread)
                .setBackgroundThreadExecutor(mBackgroundThread)
                .setConfig(PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setPrefetchDistance(PAGE_SIZE)
                        .setInitialLoadSizeHint(PAGE_SIZE)
                        .setEnablePlaceholders(false)
                        .build())
                .setInitialKey(20)
                .build()

        assertTrue(pagedList.isContiguous)

        @Suppress("UNCHECKED_CAST")
        val contiguousPagedList = pagedList as ContiguousPagedList<Int, Item>
        assertEquals(0, contiguousPagedList.mStorage.leadingNullCount)
        assertEquals(PAGE_SIZE, contiguousPagedList.mStorage.storageCount)
        assertEquals(0, contiguousPagedList.mStorage.trailingNullCount)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = mBackgroundThread.executeAll()
            executed = mMainThread.executeAll() || executed
        } while (executed)
    }

    companion object {
        // use a page size that's not an even divisor of ITEMS.size() to test end conditions
        private val PAGE_SIZE = 10

        private val ITEMS = List(45) { Item(it) }
    }
}
