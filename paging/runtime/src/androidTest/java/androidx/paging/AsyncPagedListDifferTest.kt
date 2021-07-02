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

@file:Suppress("DEPRECATION")

package androidx.paging

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.paging.DiffingChangePayload.PLACEHOLDER_POSITION_CHANGE
import androidx.paging.ListUpdateCallbackFake.OnChangedEvent
import androidx.paging.ListUpdateCallbackFake.OnInsertedEvent
import androidx.paging.ListUpdateCallbackFake.OnRemovedEvent
import androidx.paging.PagedListListenerFake.OnCurrentListChangedEvent
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SmallTest
@RunWith(AndroidJUnit4::class)
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
    ): PagedList<V> {
        // unblock page loading thread to allow build to succeed
        pageLoadingThread.autoRun = true
        return PagedList.Builder(TestPositionalDataSource(data), config)
            .setInitialKey(initialKey)
            .setNotifyExecutor(mainThread)
            .setFetchExecutor(pageLoadingThread)
            .build()
            .also {
                pageLoadingThread.autoRun = false
            }
    }

    @Test
    fun initialState() {
        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)
        assertEquals(null, differ.currentList)
        assertEquals(0, differ.itemCount)
        assertEquals(0, callback.interactions)
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun setFullList() {
        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)
        differ.submitList(StringPagedList(0, 0, "a", "b"))

        assertEquals(2, differ.itemCount)
        assertEquals("a", differ.getItem(0))
        assertEquals("b", differ.getItem(1))

        assertEquals(OnInsertedEvent(0, 2), callback.onInsertedEvents[0])
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(1, callback.interactions)
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
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
        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)

        assertEquals(0, differ.itemCount)

        differ.submitList(StringPagedList(2, 2, "a", "b"))

        assertEquals(OnInsertedEvent(0, 6), callback.onInsertedEvents[0])
        assertEquals(1, callback.interactions)
        assertEquals(6, differ.itemCount)

        assertNull(differ.getItem(0))
        assertNull(differ.getItem(1))
        assertEquals("a", differ.getItem(2))
        assertEquals("b", differ.getItem(3))
        assertNull(differ.getItem(4))
        assertNull(differ.getItem(5))
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun nullpadded() {
        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)

        assertEquals(0, differ.itemCount)

        differ.submitList(
            StringPagedList(
                leadingNulls = 0,
                trailingNulls = 0, "a", "b"
            )
        )

        fun submitAndAssert(
            stringPagedList: PagedList<String>,
            vararg expected: Any?
        ) {
            val prevEventsSize = callback.allEvents.size
            differ.submitList(stringPagedList)
            drain()
            assertThat(
                callback.allEvents.subList(prevEventsSize, callback.allEvents.size)
            ).containsExactlyElementsIn(
                expected
            ).inOrder()
        }
        // prepend nulls
        submitAndAssert(
            StringPagedList(
                leadingNulls = 4,
                trailingNulls = 0,
                items = arrayOf("a", "b")
            ),
            OnInsertedEvent(0, 4)
        )
        // remove leading nulls
        submitAndAssert(
            StringPagedList(
                leadingNulls = 0,
                trailingNulls = 0,
                items = arrayOf("a", "b")
            ),
            OnRemovedEvent(0, 4)
        )
        // append nulls
        submitAndAssert(
            StringPagedList(
                leadingNulls = 0,
                trailingNulls = 3,
                items = arrayOf("a", "b")
            ),
            OnInsertedEvent(2, 3)
        )
        // remove trailing nulls
        submitAndAssert(
            StringPagedList(
                leadingNulls = 0,
                trailingNulls = 0,
                items = arrayOf("a", "b")
            ),
            OnRemovedEvent(2, 3)
        )
        // add nulls on both ends
        submitAndAssert(
            StringPagedList(
                leadingNulls = 3,
                trailingNulls = 2,
                items = arrayOf("a", "b")
            ),
            OnInsertedEvent(0, 3),
            OnInsertedEvent(5, 2)
        )
        // remove some nulls from both ends
        submitAndAssert(
            StringPagedList(
                leadingNulls = 1,
                trailingNulls = 1,
                items = arrayOf("a", "b")
            ),
            OnRemovedEvent(0, 2),
            OnChangedEvent(0, 1, PLACEHOLDER_POSITION_CHANGE),
            OnRemovedEvent(4, 1),
            OnChangedEvent(3, 1, PLACEHOLDER_POSITION_CHANGE),
        )
        // add to leading, remove from trailing
        submitAndAssert(
            StringPagedList(
                leadingNulls = 5,
                trailingNulls = 0,
                items = arrayOf("a", "b")
            ),
            OnChangedEvent(0, 1, PLACEHOLDER_POSITION_CHANGE),
            OnInsertedEvent(0, 4),
            OnRemovedEvent(7, 1)
        )
        // add trailing, remove from leading
        submitAndAssert(
            StringPagedList(
                leadingNulls = 1,
                trailingNulls = 3,
                items = arrayOf("a", "b")
            ),
            OnRemovedEvent(0, 4),
            OnChangedEvent(0, 1, PLACEHOLDER_POSITION_CHANGE),
            OnInsertedEvent(3, 3)
        )
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun submitListReuse() {
        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)
        val origList = StringPagedList(2, 2, "a", "b")

        // set up original list
        differ.submitList(origList)
        assertEquals(OnInsertedEvent(0, 6), callback.onInsertedEvents[0])
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(1, callback.interactions)

        // submit new list, but don't let it finish
        differ.submitList(StringPagedList(0, 0, "c", "d"))
        drainExceptDiffThread()
        assertEquals(1, callback.interactions)

        // resubmit original list, which should be final observable state
        differ.submitList(origList)
        drain()
        assertEquals(origList, differ.currentList)
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun pagingInContent() {
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(4)
            .setPageSize(2)
            .setPrefetchDistance(2)
            .build()

        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)

        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2))
        assertEquals(OnInsertedEvent(0, ALPHABET_LIST.size), callback.onInsertedEvents[0])
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(1, callback.interactions)

        // get without triggering prefetch...
        differ.getItem(1)
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(1, callback.interactions)

        // get triggering prefetch...
        differ.getItem(2)
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(OnChangedEvent(4, 2, null), callback.onChangedEvents[0])
        assertEquals(2, callback.interactions)

        // get with no data loaded nearby...
        differ.getItem(12)
        assertEquals(2, callback.interactions)
        drain()

        // NOTE: tiling is currently disabled, so tiles at 6 and 8 are required to load around 12
        for (pos in 6..14 step 2) {
            assertEquals(OnChangedEvent(pos, 2, null), callback.onChangedEvents[(pos - 6) / 2 + 1])
        }
        assertEquals(7, callback.interactions)

        // finally, clear
        differ.submitList(null)
        assertEquals(OnRemovedEvent(0, 26), callback.onRemovedEvents[0])
        drain()
        assertEquals(8, callback.interactions)
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun simpleSwap() {
        // Page size large enough to load
        val config = PagedList.Config.Builder()
            .setPageSize(50)
            .build()

        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)

        // initial list missing one item (immediate)
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(0, 25), 0))
        assertEquals(OnInsertedEvent(0, 25), callback.onInsertedEvents[0])
        assertEquals(1, callback.interactions)
        assertEquals(differ.itemCount, 25)
        drain()
        assertEquals(1, callback.interactions)

        // pass second list with full data
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 0))
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(OnInsertedEvent(25, 1), callback.onInsertedEvents[1])
        assertEquals(2, callback.interactions)
        assertEquals(differ.itemCount, 26)

        // finally, clear (immediate)
        differ.submitList(null)
        assertEquals(OnRemovedEvent(0, 26), callback.onRemovedEvents[0])
        assertEquals(3, callback.interactions)
        drain()
        assertEquals(3, callback.interactions)
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun oldListUpdateIgnoredWhileDiffing() {
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(4)
            .setPageSize(2)
            .setPrefetchDistance(2)
            .build()

        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)

        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 4))
        assertEquals(OnInsertedEvent(0, ALPHABET_LIST.size), callback.onInsertedEvents[0])
        assertEquals(1, callback.interactions)
        drain()
        assertEquals(1, callback.interactions)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)

        // trigger page loading
        differ.getItem(10)
        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 4))
        assertEquals(1, callback.interactions)

        // drain page fetching, but list became immutable, page will be ignored
        drainExceptDiffThread()
        assertEquals(1, callback.interactions)
        assertNotNull(differ.currentList)
        assertTrue(differ.currentList!!.isImmutable)

        // flush diff, which signals nothing, since 1st pagedlist == 2nd pagedlist
        diffThread.executeAll()
        mainThread.executeAll()
        assertEquals(1, callback.interactions)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)

        // finally, a full flush will complete the swap-triggered load within the new list
        drain()
        assertEquals(OnChangedEvent(6, 2, null), callback.onChangedEvents[0])
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
    }

    @Test
    fun newPageChangesDeferredDuringDiff() {
        val config = Config(
            initialLoadSizeHint = 4,
            pageSize = 2,
            prefetchDistance = 2
        )

        val callback = ListUpdateCallbackFake()
        val differ = createDiffer(callback)

        differ.submitList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2))
        assertEquals(OnInsertedEvent(0, ALPHABET_LIST.size), callback.onInsertedEvents[0])
        assertEquals(callback.toString(), 1, callback.interactions)
        drain()
        assertEquals(callback.toString(), 1, callback.interactions)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)

        // trigger page loading in new list, after submitting (and thus snapshotting)
        val newList = createPagedListFromListAndPos(config, ALPHABET_LIST, 2)
        differ.submitList(newList)
        newList.loadAround(4)
        assertEquals(callback.toString(), 1, callback.interactions)

        // drain page fetching, but list became immutable, page changes aren't dispatched yet
        drainExceptDiffThread()
        assertEquals(callback.toString(), 1, callback.interactions)
        assertNotNull(differ.currentList)
        assertTrue(differ.currentList!!.isImmutable)

        // flush diff, which signals nothing, since 1st pagedlist == 2nd pagedlist
        diffThread.executeAll()
        mainThread.executeAll()
        assertEquals(OnChangedEvent(4, 2, null), callback.onChangedEvents[0])
        assertEquals(OnChangedEvent(6, 2, null), callback.onChangedEvents[1])
        assertEquals(callback.toString(), 3, callback.interactions)
        assertNotNull(differ.currentList)
        assertFalse(differ.currentList!!.isImmutable)
        assertThat(differ.itemCount).isEqualTo(callback.itemCountFromEvents())
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

        val listener = PagedListListenerFake<String>()
        differ.addPagedListListener(listener)

        val callback = RunnableFake()

        // first - simple insert
        val first = StringPagedList(2, 2, "a", "b")
        assertEquals(0, listener.onCurrentListChangedEvents.size)
        differ.submitList(first, callback)
        assertEquals(OnCurrentListChangedEvent(null, first), listener.onCurrentListChangedEvents[0])
        assertEquals(1, listener.onCurrentListChangedEvents.size)
        assertEquals(1, callback.runEvents.size)

        // second - async update
        val second = StringPagedList(2, 2, "c", "d")
        differ.submitList(second, callback)
        assertEquals(1, listener.onCurrentListChangedEvents.size)
        assertEquals(1, callback.runEvents.size)
        drain()
        assertEquals(
            OnCurrentListChangedEvent(first, second),
            listener.onCurrentListChangedEvents[1]
        )
        assertEquals(2, listener.onCurrentListChangedEvents.size)
        assertEquals(2, callback.runEvents.size)

        // third - same list - only triggers callback
        differ.submitList(second, callback)
        assertEquals(2, listener.onCurrentListChangedEvents.size)
        assertEquals(3, callback.runEvents.size)
        drain()
        assertEquals(2, listener.onCurrentListChangedEvents.size)
        assertEquals(3, callback.runEvents.size)

        // fourth - null
        differ.submitList(null, callback)
        assertEquals(
            OnCurrentListChangedEvent(second, null),
            listener.onCurrentListChangedEvents[2]
        )
        assertEquals(3, listener.onCurrentListChangedEvents.size)
        assertEquals(4, callback.runEvents.size)

        // remove listener, see nothing
        differ.removePagedListListener(listener)
        differ.submitList(first)
        drain()
        assertEquals(3, listener.onCurrentListChangedEvents.size)
        assertEquals(4, callback.runEvents.size)
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

    @Test
    fun submitList_initialPagedListEvents() {
        val config = PagedList.Config.Builder().apply {
            setPageSize(1)
        }.build()
        val listUpdateCallback = ListUpdateCapture()
        val loadStateListener = LoadStateCapture()
        val differ = createDiffer(listUpdateCallback).apply {
            addLoadStateListener(loadStateListener)
        }

        // Initial state.
        drain()
        assertThat(listUpdateCallback.newEvents()).isEmpty()
        assertThat(loadStateListener.newEvents()).containsExactly(
            LoadStateEvent(
                loadType = LoadType.REFRESH,
                loadState = LoadState.NotLoading(endOfPaginationReached = false)
            ),
            LoadStateEvent(
                loadType = LoadType.PREPEND,
                loadState = LoadState.NotLoading(endOfPaginationReached = false)
            ),
            LoadStateEvent(
                loadType = LoadType.APPEND,
                loadState = LoadState.NotLoading(endOfPaginationReached = false)
            ),
        )

        // First InitialPagedList.
        differ.submitList(
            InitialPagedList(
                coroutineScope = CoroutineScope(EmptyCoroutineContext),
                notifyDispatcher = mainThread.asCoroutineDispatcher(),
                backgroundDispatcher = diffThread.asCoroutineDispatcher(),
                config = config,
                initialLastKey = null
            )
        )
        drain()
        assertThat(listUpdateCallback.newEvents()).containsExactly(
            ListUpdateEvent.Inserted(position = 0, count = 0),
        )
        assertThat(loadStateListener.newEvents()).isEmpty()

        // Real PagedList with non-empty data.
        differ.submitList(
            createPagedListFromListAndPos(config, ALPHABET_LIST, 0)
        )
        drain()
        assertThat(listUpdateCallback.newEvents()).containsExactly(
            ListUpdateEvent.Inserted(position = 0, count = 26)
        )
        assertThat(loadStateListener.newEvents()).containsExactly(
            LoadStateEvent(
                loadType = LoadType.PREPEND,
                loadState = LoadState.NotLoading(endOfPaginationReached = true)
            ),
        )

        // Second InitialPagedList.
        differ.submitList(
            InitialPagedList(
                coroutineScope = CoroutineScope(EmptyCoroutineContext),
                notifyDispatcher = mainThread.asCoroutineDispatcher(),
                backgroundDispatcher = diffThread.asCoroutineDispatcher(),
                config = config,
                initialLastKey = null
            )
        )
        drain()
        assertThat(listUpdateCallback.newEvents()).isEmpty()
        assertThat(loadStateListener.newEvents()).containsExactly(
            LoadStateEvent(
                loadType = LoadType.REFRESH,
                loadState = LoadState.Loading
            ),
            LoadStateEvent(
                loadType = LoadType.PREPEND,
                loadState = LoadState.NotLoading(endOfPaginationReached = false)
            ),
        )
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
