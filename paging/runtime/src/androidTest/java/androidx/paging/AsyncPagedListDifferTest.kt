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

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.filters.SmallTest
import androidx.testutils.TestExecutor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class AsyncPagedListDifferTest {
    private val mainThread = TestExecutor()
    private val diffThread = TestExecutor()
    private val pageLoadingThread = TestExecutor()

    private fun createDiffer(
        listUpdateCallback: ListUpdateCallback = IGNORE_CALLBACK
    ): AsyncPagedListDiffer<String> {
        val differ = AsyncPagedListDiffer(
            listUpdateCallback,
            AsyncDifferConfig.Builder(STRING_DIFF_CALLBACK)
                .setBackgroundThreadExecutor(diffThread)
                .build()
        )
        // by default, use ArchExecutor
        assertEquals(differ.mainThreadExecutor, ArchTaskExecutor.getMainThreadExecutor())
        differ.mainThreadExecutor = mainThread
        return differ
    }

    private fun <V : Any> createPagedListFromListAndPos(
        config: PagedList.Config,
        data: List<V>,
        initialKey: Int
    ): PagedList<V> = runBlocking {
        @Suppress("DEPRECATION")
        PagedList.Builder(ListDataSource(data), config)
            .setInitialKey(initialKey)
            .setNotifyExecutor(mainThread)
            .setFetchExecutor(pageLoadingThread)
            .build()
    }

    @Test
    fun initialState() {
        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)
        assertEquals(null, differ.currentList)
        assertEquals(0, differ.itemCount)
        verifyZeroInteractions(callback)
    }

    @Test
    fun setFullList() {
        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)
        differ.submitList(StringPagedList(0, 0, "a", "b"))

        assertEquals(2, differ.itemCount)
        assertEquals("a", differ.getItem(0))
        assertEquals("b", differ.getItem(1))

        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getEmpty() {
        val differ = createDiffer()
        differ.getItem(0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getNegative() {
        val differ = createDiffer()
        differ.submitList(StringPagedList(0, 0, "a", "b"))
        differ.getItem(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getPastEnd() {
        val differ = createDiffer()
        differ.submitList(StringPagedList(0, 0, "a", "b"))
        differ.getItem(2)
    }

    @Test
    fun simpleStatic() {
        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)

        assertEquals(0, differ.itemCount)

        differ.submitList(StringPagedList(2, 2, "a", "b"))

        verify(callback).onInserted(0, 6)
        verifyNoMoreInteractions(callback)
        assertEquals(6, differ.itemCount)

        assertNull(differ.getItem(0))
        assertNull(differ.getItem(1))
        assertEquals("a", differ.getItem(2))
        assertEquals("b", differ.getItem(3))
        assertNull(differ.getItem(4))
        assertNull(differ.getItem(5))
    }

    @Test
    fun submitListReuse() {
        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)
        val origList = StringPagedList(2, 2, "a", "b")

        // set up original list
        differ.submitList(origList)
        verify(callback).onInserted(0, 6)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        // submit new list, but don't let it finish
        differ.submitList(StringPagedList(0, 0, "c", "d"))
        drainExceptDiffThread()
        verifyNoMoreInteractions(callback)

        // resubmit original list, which should be final observable state
        differ.submitList(origList)
        drain()
        assertEquals(origList, differ.currentList)
    }

    @Test
    fun pagingInContent() {
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(4)
            .setPageSize(2)
            .setPrefetchDistance(2)
            .build()

        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)

        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2))
        verify(callback).onInserted(0, ALPHABET_LIST.size)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        // get without triggering prefetch...
        differ.getItem(1)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        // get triggering prefetch...
        differ.getItem(2)
        verifyNoMoreInteractions(callback)
        drain()
        verify(callback).onChanged(4, 2, null)
        verifyNoMoreInteractions(callback)

        // get with no data loaded nearby...
        differ.getItem(12)
        verifyNoMoreInteractions(callback)
        drain()

        // NOTE: tiling is currently disabled, so tiles at 6 and 8 are required to load around 12
        for (pos in 6..14 step 2) {
            verify(callback).onChanged(pos, 2, null)
        }
        verifyNoMoreInteractions(callback)

        // finally, clear
        differ.submitList(null)
        verify(callback).onRemoved(0, 26)
        drain()
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun simpleSwap() {
        // Page size large enough to load
        val config = PagedList.Config.Builder()
            .setPageSize(50)
            .build()

        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)

        // initial list missing one item (immediate)
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(0, 25), 0))
        verify(callback).onInserted(0, 25)
        verifyNoMoreInteractions(callback)
        assertEquals(differ.itemCount, 25)
        drain()
        verifyNoMoreInteractions(callback)

        // pass second list with full data
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 0))
        verifyNoMoreInteractions(callback)
        drain()
        verify(callback).onInserted(25, 1)
        verifyNoMoreInteractions(callback)
        assertEquals(differ.itemCount, 26)

        // finally, clear (immediate)
        differ.submitList(null)
        verify(callback).onRemoved(0, 26)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun newPageWhileDiffing() {
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(4)
            .setPageSize(2)
            .setPrefetchDistance(2)
            .build()

        val callback = mock<ListUpdateCallback>()
        val differ = createDiffer(callback)

        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2))
        verify(callback).onInserted(0, ALPHABET_LIST.size)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)

        // trigger page loading
        differ.getItem(10)
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2))
        verifyNoMoreInteractions(callback)

        // drain page fetching, but list became immutable, page will be ignored
        drainExceptDiffThread()
        verifyNoMoreInteractions(callback)
        assertNotNull(differ.currentList)
        assertTrue(differ.currentList!!.isImmutable)

        // flush diff, which signals nothing, since 1st pagedlist == 2nd pagedlist
        diffThread.executeAll()
        mainThread.executeAll()
        verifyNoMoreInteractions(callback)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)

        // finally, a full flush will complete the swap-triggered load within the new list
        drain()
        verify(callback).onChanged(8, 2, null)
    }

    @Test
    fun itemCountUpdatedBeforeListUpdateCallbacks() {
        // verify that itemCount is updated in the differ before dispatching ListUpdateCallbacks

        val expectedCount = intArrayOf(0)
        // provides access to differ, which must be constructed after callback
        val differAccessor = arrayOf<AsyncPagedListDiffer<*>?>(null)

        val callback = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                assertEquals(expectedCount[0], differAccessor[0]!!.itemCount)
            }

            override fun onRemoved(position: Int, count: Int) {
                assertEquals(expectedCount[0], differAccessor[0]!!.itemCount)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                fail("not expected")
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                fail("not expected")
            }
        }

        val differ = createDiffer(callback)
        differAccessor[0] = differ

        val config = PagedList.Config.Builder()
            .setPageSize(20)
            .build()

        // in the fast-add case...
        expectedCount[0] = 5
        assertEquals(0, differ.itemCount)
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(0, 5), 0))
        assertEquals(5, differ.itemCount)

        // in the slow, diff on BG thread case...
        expectedCount[0] = 10
        assertEquals(5, differ.itemCount)
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(0, 10), 0))
        drain()
        assertEquals(10, differ.itemCount)

        // and in the fast-remove case
        expectedCount[0] = 0
        assertEquals(10, differ.itemCount)
        differ.submitList(null)
        assertEquals(0, differ.itemCount)
    }

    @Test
    fun loadAroundHandlePrepend() {
        val differ = createDiffer()

        val config = PagedList.Config.Builder()
            .setPageSize(5)
            .setEnablePlaceholders(false)
            .build()

        // initialize, initial key position is 0
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(10, 20), 0))
        differ.currentList!!.loadAround(0)
        drain()
        assertEquals(0, differ.currentList!!.lastKey)

        // if 10 items are prepended, lastKey should be updated to point to same item
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(0, 20), 0))
        drain()
        assertEquals(10, differ.currentList!!.lastKey)
    }

    @Test
    fun submitSubset() {
        // Page size large enough to load
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(4)
            .setPageSize(2)
            .setPrefetchDistance(1)
            .setEnablePlaceholders(false)
            .build()

        val differ = createDiffer()

        // mock RecyclerView interaction, where we load list where initial load doesn't fill the
        // viewport, and it needs to load more
        val first = createPagedListFromListAndPos(config, ALPHABET_LIST, 0)
        differ.submitList(first)
        assertEquals(4, differ.itemCount)
        for (i in 0..3) {
            differ.getItem(i)
        }
        // load more
        drain()
        assertEquals(6, differ.itemCount)
        for (i in 4..5) {
            differ.getItem(i)
        }

        // Update comes along with same initial data - a subset of current PagedList
        val second = createPagedListFromListAndPos(config, ALPHABET_LIST, 0)
        differ.submitList(second)
        assertEquals(4, second.size)

        // RecyclerView doesn't trigger any binds in this case, so no getItem() calls to
        // AsyncPagedListDiffer / calls to PagedList.loadAround

        // finish diff, but no further loading
        diffThread.executeAll()
        mainThread.executeAll()

        // 2nd list starts out at size 4
        assertEquals(4, second.size)
        assertEquals(4, differ.itemCount)

        // but grows, despite not having explicit bind-triggered loads
        drain()
        assertEquals(6, second.size)
        assertEquals(6, differ.itemCount)
    }

    @Test
    fun emptyPagedLists() {
        val differ = createDiffer()
        differ.submitList(StringPagedList(0, 0, "a", "b"))
        differ.submitList(StringPagedList(0, 0))
        // verify that committing a diff with a empty final list doesn't crash
        drain()
    }

    @Test
    fun pagedListListener() {
        val differ = createDiffer()

        @Suppress("UNCHECKED_CAST")
        val listener = mock<AsyncPagedListDiffer.PagedListListener<String>>()
        differ.addPagedListListener(listener)

        val callback = mock<Runnable>()

        // first - simple insert
        val first = StringPagedList(2, 2, "a", "b")
        verifyZeroInteractions(listener)
        differ.submitList(first, callback)
        verify(listener).onCurrentListChanged(null, first)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // second - async update
        val second = StringPagedList(2, 2, "c", "d")
        differ.submitList(second, callback)
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
        drain()
        verify(listener).onCurrentListChanged(first, second)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // third - same list - only triggers callback
        differ.submitList(second, callback)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
        reset(callback)

        // fourth - null
        differ.submitList(null, callback)
        verify(listener).onCurrentListChanged(second, null)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // remove listener, see nothing
        differ.removePagedListListener(listener)
        differ.submitList(first)
        drain()
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun addRemovePagedListCallback() {
        val differ = createDiffer()
        val noopCallback = { _: PagedList<String>?, _: PagedList<String>? -> }
        differ.addPagedListListener(noopCallback)
        assert(differ.listeners.size == 1)
        differ.removePagedListListener { _: PagedList<String>?, _: PagedList<String>? -> }
        assert(differ.listeners.size == 1)
        differ.removePagedListListener(noopCallback)
        assert(differ.listeners.size == 0)
    }

    private fun drainExceptDiffThread() {
        var executed: Boolean
        do {
            executed = pageLoadingThread.executeAll()
            executed = mainThread.executeAll() or executed
        } while (executed)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = pageLoadingThread.executeAll()
            executed = diffThread.executeAll() or executed
            executed = mainThread.executeAll() or executed
        } while (executed)
    }

    companion object {
        private val ALPHABET_LIST = List(26) { "" + ('a' + it) }

        private val STRING_DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

        private val IGNORE_CALLBACK = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {}

            override fun onRemoved(position: Int, count: Int) {}

            override fun onMoved(fromPosition: Int, toPosition: Int) {}

            override fun onChanged(position: Int, count: Int, payload: Any?) {}
        }
    }
}
