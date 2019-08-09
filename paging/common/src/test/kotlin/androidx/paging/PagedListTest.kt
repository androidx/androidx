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
import androidx.paging.PagedList.LoadStateManager
import androidx.paging.PagedList.LoadType.REFRESH
import androidx.paging.futures.DirectDispatcher
import androidx.testutils.TestDispatcher
import androidx.testutils.TestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFails

@RunWith(JUnit4::class)
class PagedListTest {
    companion object {
        private val ITEMS = List(100) { "$it" }
        private val config = Config(10)

        private val pagedSource = object : PagedSource<Int, String>() {
            override val keyProvider = KeyProvider.Positional

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
                when (params.loadType) {
                    LoadType.INITIAL -> LoadResult(
                        data = listOf("a"),
                        itemsBefore = 0,
                        itemsAfter = 0
                    )
                    else -> throw NotImplementedError("Test should fail if we get here")
                }

            override fun isRetryableError(error: Throwable) = false
        }
    }

    private val testCoroutineScope = CoroutineScope(EmptyCoroutineContext)
    private val mainThread = TestDispatcher()
    private val backgroundThread = TestDispatcher()

    @Test
    fun createLegacy() {
        @Suppress("DEPRECATION")
        val pagedList = PagedList.Builder(ListDataSource(ITEMS), 100)
            .setNotifyExecutor(TestExecutor())
            .setFetchExecutor(TestExecutor())
            .build()
        // if build succeeds without flushing an executor, success!
        assertEquals(ITEMS, pagedList)
    }

    @Test
    fun createAsync() {
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(false)
            .build()
        var success = false

        val job = testCoroutineScope.async(backgroundThread) {
            val pagedList = PagedList.create(
                PagedSourceWrapper(ListDataSource(ITEMS)),
                testCoroutineScope,
                mainThread,
                backgroundThread,
                backgroundThread,
                null,
                config,
                0
            )

            assertEquals(ITEMS.subList(0, 30), pagedList)
            success = true
        }

        backgroundThread.executeAll()
        runBlocking { job.await() }

        assert(success)
    }

    @Test
    fun createAsyncThrow() {
        val pagedSource = object : PagedSource<Int, String>() {
            override val keyProvider = KeyProvider.Positional

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                throw Exception()
            }

            override fun isRetryableError(error: Throwable) = false
        }

        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(false)
            .build()
        var success = false
        assertFails {
            val job = testCoroutineScope.async(backgroundThread) {
                PagedList.create(
                    pagedSource,
                    testCoroutineScope,
                    mainThread,
                    backgroundThread,
                    backgroundThread,
                    null,
                    config,
                    0
                )

                success = true
            }

            backgroundThread.executeAll()
            runBlocking { job.await() }
        }
        assert(!success)
    }

    @Test
    fun defaults() = runBlocking {
        val pagedList = PagedList.Builder(pagedSource, config)
            .setNotifyDispatcher(DirectDispatcher)
            .setFetchDispatcher(DirectDispatcher)
            .buildAsync()

        assertEquals(pagedSource, pagedList.pagedSource)
        assertEquals(config, pagedList.config)
    }

    @Test
    fun setState_Error() {
        var onStateChangeCalls = 0
        val loadStateManager = object : LoadStateManager() {
            override fun onStateChanged(type: PagedList.LoadType, state: PagedList.LoadState) {
                onStateChangeCalls++
            }
        }

        loadStateManager.setState(REFRESH, PagedList.LoadState.Error(EXCEPTION, true))
        loadStateManager.setState(REFRESH, PagedList.LoadState.Error(EXCEPTION, true))

        assertEquals(1, onStateChangeCalls)
    }
}
