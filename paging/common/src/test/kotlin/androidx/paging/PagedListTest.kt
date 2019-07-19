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

import androidx.paging.futures.DirectExecutor
import androidx.testutils.TestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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
            override val keyProvider = KeyProvider.Positional<String>()

            private var _invalid = false
            override val invalid: Boolean
                get() = _invalid

            override fun invalidate() {
                _invalid = true
            }

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
                when (params.loadType) {
                    LoadType.INITIAL -> LoadResult(
                        0,
                        0,
                        data = listOf("a"),
                        offset = 0,
                        counted = true
                    )
                    else -> throw NotImplementedError("Test should fail if we get here")
                }

            override fun isRetryableError(error: Throwable) = false
        }
    }

    private val testCoroutineScope = CoroutineScope(EmptyCoroutineContext)
    private val mainThread = TestExecutor()
    private val backgroundThread = TestExecutor()

    @Test
    fun createLegacy() = runBlocking {
        @Suppress("DEPRECATION")
        val pagedList = PagedList.Builder(ListDataSource(ITEMS), 100)
            .setNotifyExecutor(mainThread)
            .setFetchExecutor(backgroundThread)
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

        val job = testCoroutineScope.async(backgroundThread.asCoroutineDispatcher()) {
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
        val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
                callback.onError(Exception())
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
                fail("no load range expected")
            }
        }

        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(false)
            .build()
        var success = false
        assertFails {
            val job = testCoroutineScope.async(backgroundThread.asCoroutineDispatcher()) {
                PagedList.create(
                    PagedSourceWrapper(dataSource),
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
            .setNotifyExecutor(DirectExecutor)
            .setFetchExecutor(DirectExecutor)
            .buildAsync()

        assertEquals(pagedSource, pagedList.pagedSource)
        assertEquals(config, pagedList.config)
    }
}
