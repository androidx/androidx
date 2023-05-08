/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.paging.ContiguousPagedListTest.Companion.EXCEPTION
import androidx.paging.LoadType.REFRESH
import androidx.testutils.TestDispatcher
import androidx.testutils.TestExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executor
import kotlin.concurrent.thread
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(JUnit4::class)
class PagedListTest {
    companion object {
        private val ITEMS = List(100) { "$it" }
        private val config = Config(10)

        private val pagingSource = object : PagingSource<Int, String>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
                when (params) {
                    is LoadParams.Refresh -> LoadResult.Page(
                        data = listOf("a"),
                        prevKey = null,
                        nextKey = null
                    )
                    else -> throw NotImplementedError("Test should fail if we get here")
                }

            override fun getRefreshKey(state: PagingState<Int, String>): Int? = null
        }
    }

    private val testCoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Test
    fun createLegacy() {
        val slowFetchExecutor = Executor {
            // just be slow to ensure `build()` really waited on fetch to complete.
            // but still run it on another thread to ensure we are not blocking the test here
            thread {
                Thread.sleep(1000)
                it.run()
            }
        }
        @Suppress("DEPRECATION")
        val pagedList = PagedList.Builder(TestPositionalDataSource(ITEMS), 100)
            .setNotifyExecutor(TestExecutor())
            .setFetchExecutor(slowFetchExecutor)
            .build()
        // if build succeeds without flushing an executor, success!
        assertEquals(ITEMS, pagedList)
    }

    @Test
    fun createNoInitialPageThrow() {
        runBlocking {
            val pagingSource = object : PagingSource<Int, String>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                    throw IllegalStateException()
                }

                override fun getRefreshKey(state: PagingState<Int, String>): Int? = null
            }
            assertFailsWith<IllegalStateException> {
                @Suppress("DEPRECATION")
                PagedList.create(
                    pagingSource,
                    null,
                    testCoroutineScope,
                    Dispatchers.Default,
                    Dispatchers.IO,
                    null,
                    Config(10),
                    0
                )
            }
        }
    }

    @Test
    fun createNoInitialPageError() {
        runBlocking {
            val exception = IllegalStateException()
            val pagingSource = object : PagingSource<Int, String>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                    return LoadResult.Error(exception)
                }

                override fun getRefreshKey(state: PagingState<Int, String>): Int? = null
            }

            // create doesn't differentiate between throw vs error runnable, which is why
            // PagedList.Builder without the initial page is deprecated
            assertFailsWith<IllegalStateException> {
                @Suppress("DEPRECATION")
                PagedList.create(
                    pagingSource,
                    null,
                    testCoroutineScope,
                    Dispatchers.Default,
                    Dispatchers.IO,
                    null,
                    Config(10),
                    0
                )
            }
        }
    }

    @Test
    fun createNoInitialPageInvalidResult() {
        runBlocking {
            val pagingSource = object : PagingSource<Int, String>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                    return LoadResult.Invalid()
                }

                override fun getRefreshKey(state: PagingState<Int, String>): Int? {
                    fail("should not reach here")
                }
            }

            val expectedException = assertFailsWith<IllegalStateException> {
                @Suppress("DEPRECATION")
                PagedList.create(
                    pagingSource,
                    initialPage = null,
                    testCoroutineScope,
                    Dispatchers.Default,
                    Dispatchers.IO,
                    boundaryCallback = null,
                    Config(10),
                    key = 0
                )
            }
            assertThat(expectedException.message).isEqualTo(
                "Failed to create PagedList. The provided PagingSource returned " +
                    "LoadResult.Invalid, but a LoadResult.Page was expected. To use a " +
                    "PagingSource which supports invalidation, use a PagedList builder that " +
                    "accepts a factory method for PagingSource or DataSource.Factory, such as " +
                    "LivePagedList."
            )
        }
    }

    @Test
    fun defaults() = runBlocking {
        val initialPage = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false,
            )
        ) as PagingSource.LoadResult.Page

        @Suppress("DEPRECATION")
        val pagedList = PagedList.Builder(pagingSource, initialPage, config)
            .setNotifyDispatcher(Dispatchers.Default)
            .setFetchDispatcher(Dispatchers.IO)
            .build()

        assertEquals(pagingSource, pagedList.pagingSource)
        assertEquals(config, pagedList.config)
    }

    @Test
    fun setState_Error() {
        var onStateChangeCalls = 0

        @Suppress("DEPRECATION")
        val loadStateManager = object : PagedList.LoadStateManager() {
            override fun onStateChanged(type: LoadType, state: LoadState) {
                onStateChangeCalls++
            }
        }

        loadStateManager.setState(REFRESH, LoadState.Error(EXCEPTION))
        loadStateManager.setState(REFRESH, LoadState.Error(EXCEPTION))

        assertEquals(1, onStateChangeCalls)
    }

    @Test
    fun dispatchStateChange_dispatchesOnNotifyDispatcher() {
        val notifyDispatcher = TestDispatcher()

        @Suppress("DEPRECATION")
        val pagedList = object : PagedList<String>(
            pagingSource,
            testCoroutineScope,
            notifyDispatcher,
            PagedStorage(),
            config
        ) {
            override val lastKey: Any? = null

            override val isDetached: Boolean = true

            override fun dispatchCurrentLoadState(callback: (LoadType, LoadState) -> Unit) {}

            override fun loadAroundInternal(index: Int) {}

            override fun detach() {}
        }

        assertTrue { notifyDispatcher.queue.isEmpty() }

        pagedList.dispatchStateChangeAsync(REFRESH, LoadState.Loading)
        assertEquals(1, notifyDispatcher.queue.size)

        pagedList.dispatchStateChangeAsync(REFRESH, LoadState.NotLoading.Incomplete)
        assertEquals(2, notifyDispatcher.queue.size)
    }
}
