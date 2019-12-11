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
import androidx.paging.PagedList.Builder
import androidx.paging.PagedList.LoadStateManager
import androidx.testutils.TestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
class PagedListTest {
    companion object {
        private val ITEMS = List(100) { "$it" }
        private val config = Config(10)

        private val pagedSource = object : PagedSource<Int, String>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
                when (params.loadType) {
                    REFRESH -> LoadResult.Page(
                        data = listOf("a"),
                        prevKey = null,
                        nextKey = null
                    )
                    else -> throw NotImplementedError("Test should fail if we get here")
                }
        }
    }

    private val testCoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @Test
    fun createLegacy() {
        @Suppress("DEPRECATION")
        val pagedList = Builder(ListDataSource(ITEMS), 100)
            .setNotifyExecutor(TestExecutor())
            .setFetchExecutor(TestExecutor())
            .build()
        // if build succeeds without flushing an executor, success!
        assertEquals(ITEMS, pagedList)
    }

    @Test
    fun createNoInitialPageThrow() {
        runBlocking {
            val pagedSource = object : PagedSource<Int, String>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                    throw IllegalStateException()
                }
            }
            assertFailsWith<IllegalStateException> {
                PagedList.create(
                    pagedSource,
                    null,
                    testCoroutineScope,
                    DirectDispatcher,
                    DirectDispatcher,
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
            val pagedSource = object : PagedSource<Int, String>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                    return LoadResult.Error(exception)
                }
            }

            // create doesn't differentiate between throw vs error runnable, which is why
            // PagedList.Builder without the initial page is deprecated
            assertFailsWith<IllegalStateException> {
                PagedList.create(
                    pagedSource,
                    null,
                    testCoroutineScope,
                    DirectDispatcher,
                    DirectDispatcher,
                    null,
                    Config(10),
                    0
                )
            }
        }
    }

    @Test
    fun defaults() = runBlocking {
        val initialPage = pagedSource.load(
            PagedSource.LoadParams(
                REFRESH,
                key = null,
                loadSize = 10,
                placeholdersEnabled = false,
                pageSize = 10
            )
        ) as PagedSource.LoadResult.Page
        val pagedList = Builder(pagedSource, initialPage, config)
            .setNotifyDispatcher(DirectDispatcher)
            .setFetchDispatcher(DirectDispatcher)
            .build()

        assertEquals(pagedSource, pagedList.pagedSource)
        assertEquals(config, pagedList.config)
    }

    @Test
    fun setState_Error() {
        var onStateChangeCalls = 0
        val loadStateManager = object : LoadStateManager() {
            override fun onStateChanged(type: LoadType, state: LoadState) {
                onStateChangeCalls++
            }
        }

        loadStateManager.setState(REFRESH, LoadState.Error(EXCEPTION))
        loadStateManager.setState(REFRESH, LoadState.Error(EXCEPTION))

        assertEquals(1, onStateChangeCalls)
    }
}
