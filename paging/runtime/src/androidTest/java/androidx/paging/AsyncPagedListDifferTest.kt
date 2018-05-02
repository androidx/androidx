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

import android.support.test.filters.SmallTest
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

@SmallTest
@RunWith(JUnit4::class)
class AsyncPagedListDifferTest {
    private val mMainThread = TestExecutor()
    private val mDiffThread = TestExecutor()
    private val mPageLoadingThread = TestExecutor()

    private fun <T> createDiffer(listUpdateCallback: ListUpdateCallback,
                                 diffCallback: DiffUtil.ItemCallback<T>): AsyncPagedListDiffer<T> {
        val differ = AsyncPagedListDiffer(listUpdateCallback,
                AsyncDifferConfig.Builder<T>(diffCallback)
                        .setBackgroundThreadExecutor(mDiffThread)
                        .build())
        // by default, use ArchExecutor
        assertEquals(differ.mMainThreadExecutor, ArchTaskExecutor.getMainThreadExecutor())
        differ.mMainThreadExecutor = mMainThread
        return differ
    }

    private fun <V> createPagedListFromListAndPos(
            config: PagedList.Config, data: List<V>, initialKey: Int): PagedList<V> {
        return PagedList.Builder<Int, V>(ListDataSource(data), config)
                .setInitialKey(initialKey)
                .setNotifyExecutor(mMainThread)
                .setFetchExecutor(mPageLoadingThread)
                .build()
    }

    @Test
    fun initialState() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)
        assertEquals(null, differ.currentList)
        assertEquals(0, differ.itemCount)
        verifyZeroInteractions(callback)
    }

    @Test
    fun setFullList() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)
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
        val differ = createDiffer(IGNORE_CALLBACK, STRING_DIFF_CALLBACK)
        differ.getItem(0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getNegative() {
        val differ = createDiffer(IGNORE_CALLBACK, STRING_DIFF_CALLBACK)
        differ.submitList(StringPagedList(0, 0, "a", "b"))
        differ.getItem(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getPastEnd() {
        val differ = createDiffer(IGNORE_CALLBACK, STRING_DIFF_CALLBACK)
        differ.submitList(StringPagedList(0, 0, "a", "b"))
        differ.getItem(2)
    }

    @Test
    fun simpleStatic() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)

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
    fun pagingInContent() {
        val config = PagedList.Config.Builder()
                .setInitialLoadSizeHint(4)
                .setPageSize(2)
                .setPrefetchDistance(2)
                .build()

        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)

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
        verify(callback).onChanged(10, 2, null)
        verify(callback).onChanged(12, 2, null)
        verify(callback).onChanged(14, 2, null)
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

        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)

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

        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)

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

        // finally full drain, which signals nothing, since 1st pagedlist == 2nd pagedlist
        drain()
        verifyNoMoreInteractions(callback)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)
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

        val differ = createDiffer(callback, STRING_DIFF_CALLBACK)
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

    private fun drainExceptDiffThread() {
        var executed: Boolean
        do {
            executed = mPageLoadingThread.executeAll()
            executed = mMainThread.executeAll() or executed
        } while (executed)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = mPageLoadingThread.executeAll()
            executed = mDiffThread.executeAll() or executed
            executed = mMainThread.executeAll() or executed
        } while (executed)
    }

    companion object {
        private val ALPHABET_LIST = List(26) { "" + 'a' + it }

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
