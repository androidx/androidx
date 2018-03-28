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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    private fun verifyLoadedPages(list: List<Item>, vararg loadedPages: Int) {
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

    private fun createTiledPagedList(loadPosition: Int, initPageCount: Int,
            prefetchDistance: Int = PAGE_SIZE,
            listData: List<Item> = ITEMS,
            boundaryCallback: PagedList.BoundaryCallback<Item>? = null): TiledPagedList<Item> {
        return TiledPagedList(
                ListDataSource(listData), mMainThread, mBackgroundThread, boundaryCallback,
                PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setInitialLoadSizeHint(PAGE_SIZE * initPageCount)
                        .setPrefetchDistance(prefetchDistance)
                        .build(),
                loadPosition)
    }

    @Test
    fun getDataSource() {
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 1)
        assertTrue(pagedList.dataSource is ListDataSource<Item>)

        // snapshot keeps same DataSource
        assertSame(pagedList.dataSource,
                (pagedList.snapshot() as SnapshotPagedList<Item>).dataSource)
    }

    @Test
    fun initialLoad_onePage() {
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 1)
        verifyLoadedPages(pagedList, 0, 1)
    }

    @Test
    fun initialLoad_onePageOffset() {
        val pagedList = createTiledPagedList(loadPosition = 10, initPageCount = 1)
        verifyLoadedPages(pagedList, 0, 1)
    }

    @Test
    fun initialLoad_full() {
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 100)
        verifyLoadedPages(pagedList, 0, 1, 2, 3, 4)
    }

    @Test
    fun initialLoad_end() {
        val pagedList = createTiledPagedList(loadPosition = 44, initPageCount = 2)
        verifyLoadedPages(pagedList, 3, 4)
    }

    @Test
    fun initialLoad_multiple() {
        val pagedList = createTiledPagedList(loadPosition = 9, initPageCount = 2)
        verifyLoadedPages(pagedList, 0, 1)
    }

    @Test
    fun initialLoad_offset() {
        val pagedList = createTiledPagedList(loadPosition = 41, initPageCount = 2)
        verifyLoadedPages(pagedList, 3, 4)
    }

    @Test
    fun initialLoad_initializesLastKey() {
        val pagedList = createTiledPagedList(loadPosition = 44, initPageCount = 2)
        assertEquals(44, pagedList.lastKey)
    }

    @Test
    fun initialLoadAsync() {
        val dataSource = AsyncListDataSource(ITEMS)
        val pagedList = TiledPagedList(
                dataSource, mMainThread, mBackgroundThread, null,
                PagedList.Config.Builder().setPageSize(10).build(), 0)

        assertTrue(pagedList.isEmpty())
        drain()
        assertTrue(pagedList.isEmpty())
        dataSource.flush()
        assertTrue(pagedList.isEmpty())
        mBackgroundThread.executeAll()
        assertTrue(pagedList.isEmpty())

        // Data source defers callbacks until flush, which posts result to main thread
        mMainThread.executeAll()
        assertFalse(pagedList.isEmpty())
    }

    @Test
    fun addWeakCallbackEmpty() {
        val dataSource = AsyncListDataSource(ITEMS)
        val pagedList = TiledPagedList(
                dataSource, mMainThread, mBackgroundThread, null,
                PagedList.Config.Builder().setPageSize(10).build(), 0)

        // capture empty snapshot
        val emptySnapshot = pagedList.snapshot()
        assertTrue(pagedList.isEmpty())
        assertTrue(emptySnapshot.isEmpty())

        // data added in asynchronously
        dataSource.flush()
        drain()
        assertFalse(pagedList.isEmpty())

        // verify that adding callback works with empty start point
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(emptySnapshot, callback)
        verify(callback).onInserted(0, pagedList.size)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun append() {
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 1)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyLoadedPages(pagedList, 0, 1)
        verifyZeroInteractions(callback)

        pagedList.loadAround(15)

        verifyLoadedPages(pagedList, 0, 1)

        drain()

        verifyLoadedPages(pagedList, 0, 1, 2)
        verify(callback).onChanged(20, 10)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prepend() {
        val pagedList = createTiledPagedList(loadPosition = 44, initPageCount = 2)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyLoadedPages(pagedList, 3, 4)
        verifyZeroInteractions(callback)

        pagedList.loadAround(35)
        drain()

        verifyLoadedPages(pagedList, 2, 3, 4)
        verify<PagedList.Callback>(callback).onChanged(20, 10)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun loadWithGap() {
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 1)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyLoadedPages(pagedList, 0, 1)
        verifyZeroInteractions(callback)

        pagedList.loadAround(44)
        drain()

        verifyLoadedPages(pagedList, 0, 1, 3, 4)
        verify(callback).onChanged(30, 10)
        verify(callback).onChanged(40, 5)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun tinyPrefetchTest() {
        val pagedList = createTiledPagedList(
                loadPosition = 0, initPageCount = 1, prefetchDistance = 1)
        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(null, callback)
        verifyLoadedPages(pagedList, 0, 1)
        verifyZeroInteractions(callback)

        pagedList.loadAround(33)
        drain()

        verifyLoadedPages(pagedList, 0, 1, 3)
        verify(callback).onChanged(30, 10)
        verifyNoMoreInteractions(callback)

        pagedList.loadAround(44)
        drain()

        verifyLoadedPages(pagedList, 0, 1, 3, 4)
        verify(callback).onChanged(40, 5)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun appendCallbackAddedLate() {
        val pagedList = createTiledPagedList(
                loadPosition = 0, initPageCount = 1, prefetchDistance = 0)
        verifyLoadedPages(pagedList, 0, 1)

        pagedList.loadAround(25)
        drain()
        verifyLoadedPages(pagedList, 0, 1, 2)

        // snapshot at 30 items
        val snapshot = pagedList.snapshot()
        verifyLoadedPages(snapshot, 0, 1, 2)

        pagedList.loadAround(35)
        pagedList.loadAround(44)
        drain()
        verifyLoadedPages(pagedList, 0, 1, 2, 3, 4)
        verifyLoadedPages(snapshot, 0, 1, 2)

        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(snapshot, callback)
        verify(callback).onChanged(30, 20)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependCallbackAddedLate() {
        val pagedList = createTiledPagedList(
                loadPosition = 44, initPageCount = 2, prefetchDistance = 0)
        verifyLoadedPages(pagedList, 3, 4)

        pagedList.loadAround(25)
        drain()
        verifyLoadedPages(pagedList, 2, 3, 4)

        // snapshot at 30 items
        val snapshot = pagedList.snapshot()
        verifyLoadedPages(snapshot, 2, 3, 4)

        pagedList.loadAround(15)
        pagedList.loadAround(5)
        drain()
        verifyLoadedPages(pagedList, 0, 1, 2, 3, 4)
        verifyLoadedPages(snapshot, 2, 3, 4)

        val callback = mock(PagedList.Callback::class.java)
        pagedList.addWeakCallback(snapshot, callback)
        verify(callback).onChanged(0, 20)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun placeholdersDisabled() {
        // disable placeholders with config, so we create a contiguous version of the pagedlist
        val config = PagedList.Config.Builder()
                .setPageSize(PAGE_SIZE)
                .setPrefetchDistance(PAGE_SIZE)
                .setInitialLoadSizeHint(PAGE_SIZE)
                .setEnablePlaceholders(false)
                .build()
        val pagedList = PagedList.Builder<Int, Item>(ListDataSource(ITEMS), config)
                .setNotifyExecutor(mMainThread)
                .setFetchExecutor(mBackgroundThread)
                .setInitialKey(20)
                .build()

        assertTrue(pagedList.isContiguous)

        @Suppress("UNCHECKED_CAST")
        val contiguousPagedList = pagedList as ContiguousPagedList<Int, Item>
        assertEquals(0, contiguousPagedList.mStorage.leadingNullCount)
        assertEquals(PAGE_SIZE, contiguousPagedList.mStorage.storageCount)
        assertEquals(0, contiguousPagedList.mStorage.trailingNullCount)
    }

    @Test
    fun boundaryCallback_empty() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 1,
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
    fun boundaryCallback_immediate() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 1,
                listData = ITEMS.subList(0, 2), boundaryCallback = boundaryCallback)
        assertEquals(2, pagedList.size)

        // nothing yet
        verifyZeroInteractions(boundaryCallback)

        // callbacks posted, since creation often happens on BG thread
        drain()
        verify(boundaryCallback).onItemAtFrontLoaded(ITEMS[0])
        verify(boundaryCallback).onItemAtEndLoaded(ITEMS[1])
        verifyNoMoreInteractions(boundaryCallback)
    }

    @Test
    fun boundaryCallback_delayedUntilLoaded() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val pagedList = createTiledPagedList(loadPosition = 20, initPageCount = 1,
                boundaryCallback = boundaryCallback)
        verifyLoadedPages(pagedList, 1, 2) // 0, 3, and 4 not loaded yet

        // nothing yet, even after drain
        verifyZeroInteractions(boundaryCallback)
        drain()
        verifyZeroInteractions(boundaryCallback)

        pagedList.loadAround(0)
        pagedList.loadAround(44)

        // still nothing, since items aren't loaded...
        verifyZeroInteractions(boundaryCallback)

        drain()
        // first/last items loaded now, so callbacks dispatched
        verify(boundaryCallback).onItemAtFrontLoaded(ITEMS.first())
        verify(boundaryCallback).onItemAtEndLoaded(ITEMS.last())
        verifyNoMoreInteractions(boundaryCallback)
    }

    @Test
    fun boundaryCallback_delayedUntilNearbyAccess() {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val pagedList = createTiledPagedList(loadPosition = 0, initPageCount = 5,
                prefetchDistance = 2, boundaryCallback = boundaryCallback)
        verifyLoadedPages(pagedList, 0, 1, 2, 3, 4)

        // all items loaded, but no access near ends, so no callbacks
        verifyZeroInteractions(boundaryCallback)
        drain()
        verifyZeroInteractions(boundaryCallback)

        pagedList.loadAround(0)
        pagedList.loadAround(44)

        // callbacks not posted immediately
        verifyZeroInteractions(boundaryCallback)

        drain()

        // items accessed, so now posted callbacks are run
        verify(boundaryCallback).onItemAtFrontLoaded(ITEMS.first())
        verify(boundaryCallback).onItemAtEndLoaded(ITEMS.last())
        verifyNoMoreInteractions(boundaryCallback)
    }

    private fun validateCallbackForSize(initPageCount: Int, itemCount: Int) {
        @Suppress("UNCHECKED_CAST")
        val boundaryCallback =
                mock(PagedList.BoundaryCallback::class.java) as PagedList.BoundaryCallback<Item>
        val listData = ITEMS.subList(0, itemCount)
        val pagedList = createTiledPagedList(
                loadPosition = 0,
                initPageCount = initPageCount,
                prefetchDistance = 0,
                boundaryCallback = boundaryCallback,
                listData = listData)
        assertNotNull(pagedList[pagedList.size - 1 - PAGE_SIZE])
        assertNull(pagedList.last()) // not completed loading

        // no access near list beginning, so no callbacks yet
        verifyNoMoreInteractions(boundaryCallback)
        drain()
        verifyNoMoreInteractions(boundaryCallback)

        // trigger front boundary callback (via access)
        pagedList.loadAround(0)
        drain()
        verify(boundaryCallback).onItemAtFrontLoaded(listData.first())
        verifyNoMoreInteractions(boundaryCallback)

        // trigger end boundary callback (via load)
        pagedList.loadAround(pagedList.size - 1)
        drain()
        verify(boundaryCallback).onItemAtEndLoaded(listData.last())
        verifyNoMoreInteractions(boundaryCallback)
    }

    @Test
    fun boundaryCallbackPageSize1() {
        // verify different alignments of last page still trigger boundaryCallback correctly
        validateCallbackForSize(2, 3 * PAGE_SIZE - 2)
        validateCallbackForSize(2, 3 * PAGE_SIZE - 1)
        validateCallbackForSize(2, 3 * PAGE_SIZE)
        validateCallbackForSize(3, 3 * PAGE_SIZE + 1)
        validateCallbackForSize(3, 3 * PAGE_SIZE + 2)
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
