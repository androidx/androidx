/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.REFRESH
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class LivePagedListBuilderTest {
    private val backgroundExecutor = TestExecutor()
    private val lifecycleOwner = TestLifecycleOwner()

    private data class LoadStateEvent(
        val type: LoadType,
        val state: LoadState
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                fail("IO executor should be overwritten")
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }
        })
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    class MockPagingSourceFactory {
        fun create(): PagingSource<Int, String> {
            return MockPagingSource()
        }

        var throwable: Throwable? = null

        fun enqueueError() {
            throwable = EXCEPTION
        }

        private inner class MockPagingSource : PagingSource<Int, String>() {
            override suspend fun load(params: LoadParams<Int>) = when (params) {
                is LoadParams.Refresh -> loadInitial(params)
                else -> loadRange()
            }

            override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

            private fun loadInitial(params: LoadParams<Int>): LoadResult<Int, String> {
                if (params is LoadParams.Refresh) {
                    assertEquals(6, params.loadSize)
                } else {
                    assertEquals(2, params.loadSize)
                }

                throwable?.let { error ->
                    throwable = null
                    return LoadResult.Error(error)
                }

                val data = listOf("a", "b")
                return LoadResult.Page(
                    data = data,
                    prevKey = null,
                    nextKey = 2,
                    itemsBefore = 0,
                    itemsAfter = 2
                )
            }

            private fun loadRange(): LoadResult<Int, String> {
                val data = listOf("c", "d")
                return LoadResult.Page(
                    data = data,
                    prevKey = 2,
                    nextKey = null
                )
            }
        }
    }

    @Test
    fun initialValueAllowsGetDataSource() {
        val livePagedList = LivePagedListBuilder(MockPagingSourceFactory()::create, 2)
            .build()

        // Calling .dataSource should never throw from the initial paged list.
        livePagedList.value!!.dataSource
    }

    @Test
    fun initialValueOnMainThread() {
        // Reset ArchTaskExecutor delegate so that main thread != default test executor, to
        // represent the common case when writing tests.
        ArchTaskExecutor.getInstance().setDelegate(null)

        LivePagedListBuilder(MockPagingSourceFactory()::create, 2)
            .build()
    }

    @Test
    fun executorBehavior() {
        // specify a background dispatcher via builder, and verify it gets used for all loads,
        // overriding default IO dispatcher
        val livePagedList = LivePagedListBuilder(MockPagingSourceFactory()::create, 2)
            .setFetchExecutor(backgroundExecutor)
            .build()

        val pagedListHolder: Array<PagedList<String>?> = arrayOfNulls(1)

        livePagedList.observe(lifecycleOwner) { newList ->
            pagedListHolder[0] = newList
        }

        // initially, immediately get passed empty initial list
        assertNotNull(pagedListHolder[0])
        assertTrue(pagedListHolder[0] is InitialPagedList<*, *>)

        // flush loadInitial, done with passed executor
        drain()

        val pagedList = pagedListHolder[0]
        assertNotNull(pagedList)
        assertEquals(listOf("a", "b", null, null), pagedList)

        // flush loadRange
        pagedList!!.loadAround(2)
        drain()

        assertEquals(listOf("a", "b", "c", "d"), pagedList)
    }

    @Test
    fun failedLoad() {
        val factory = MockPagingSourceFactory()
        factory.enqueueError()

        val livePagedList = LivePagedListBuilder(factory::create, 2)
            .setFetchExecutor(backgroundExecutor)
            .build()

        val pagedListHolder: Array<PagedList<String>?> = arrayOfNulls(1)

        livePagedList.observe(lifecycleOwner) { newList ->
            pagedListHolder[0] = newList
        }

        val loadStates = mutableListOf<LoadStateEvent>()

        // initially, immediately get passed empty initial list
        val initPagedList = pagedListHolder[0]
        assertNotNull(initPagedList!!)
        assertTrue(initPagedList is InitialPagedList<*, *>)

        val loadStateChangedCallback = { type: LoadType, state: LoadState ->
            if (type == REFRESH) {
                loadStates.add(LoadStateEvent(type, state))
            }
        }
        initPagedList.addWeakLoadStateListener(loadStateChangedCallback)

        // flush loadInitial, done with passed dispatcher
        drain()

        assertSame(initPagedList, pagedListHolder[0])
        // TODO: Investigate removing initial IDLE state from callback updates.
        assertEquals(
            listOf(
                LoadStateEvent(
                    REFRESH, NotLoading(endOfPaginationReached = false)
                ),
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(REFRESH, Error(EXCEPTION))
            ),
            loadStates
        )

        initPagedList.retry()
        assertSame(initPagedList, pagedListHolder[0])

        // flush loadInitial, should succeed now
        drain()

        assertNotSame(initPagedList, pagedListHolder[0])
        assertEquals(listOf("a", "b", null, null), pagedListHolder[0])

        assertEquals(
            listOf(
                LoadStateEvent(
                    REFRESH,
                    NotLoading(endOfPaginationReached = false)
                ),
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(REFRESH, Error(EXCEPTION)),
                LoadStateEvent(REFRESH, Loading)
            ),
            loadStates
        )

        // the IDLE result shows up on the next PagedList
        initPagedList.removeWeakLoadStateListener(loadStateChangedCallback)
        pagedListHolder[0]!!.addWeakLoadStateListener(loadStateChangedCallback)
        assertEquals(
            listOf(
                LoadStateEvent(
                    REFRESH,
                    NotLoading(endOfPaginationReached = false)
                ),
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(REFRESH, Error(EXCEPTION)),
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(
                    REFRESH,
                    NotLoading(endOfPaginationReached = false)
                )
            ),
            loadStates
        )
    }

    @Test
    fun legacyPagingSourcePageSize() {
        val dataSources = mutableListOf<DataSource<Int, Int>>()
        val pagedLists = mutableListOf<PagedList<Int>>()
        val requestedLoadSizes = mutableListOf<Int>()
        val livePagedList = LivePagedListBuilder(
            pagingSourceFactory = object : DataSource.Factory<Int, Int>() {
                override fun create(): DataSource<Int, Int> {
                    return object : PositionalDataSource<Int>() {
                        override fun loadInitial(
                            params: LoadInitialParams,
                            callback: LoadInitialCallback<Int>
                        ) {
                            requestedLoadSizes.add(params.requestedLoadSize)
                            callback.onResult(listOf(1, 2, 3), 0)
                        }

                        override fun loadRange(
                            params: LoadRangeParams,
                            callback: LoadRangeCallback<Int>
                        ) {
                            requestedLoadSizes.add(params.loadSize)
                        }
                    }.also {
                        dataSources.add(it)
                    }
                }
            }.asPagingSourceFactory(backgroundExecutor.asCoroutineDispatcher()),
            config = PagedList.Config.Builder()
                .setPageSize(3)
                .setInitialLoadSizeHint(3)
                .setEnablePlaceholders(false)
                .build()
        ).setFetchExecutor(backgroundExecutor)
            .build()

        livePagedList.observeForever { pagedLists.add(it) }

        drain()
        assertThat(requestedLoadSizes).containsExactly(3)

        pagedLists.last().loadAround(2)
        drain()
        assertThat(requestedLoadSizes).containsExactly(3, 3)

        dataSources[0].invalidate()
        drain()
        assertThat(requestedLoadSizes).containsExactly(3, 3, 3)

        pagedLists.last().loadAround(2)
        drain()
        assertThat(requestedLoadSizes).containsExactly(3, 3, 3, 3)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = backgroundExecutor.executeAll()
        } while (executed)
    }

    companion object {
        val EXCEPTION = Exception("")
    }
}
