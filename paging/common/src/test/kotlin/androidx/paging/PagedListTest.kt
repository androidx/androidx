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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertFails

@RunWith(JUnit4::class)
class PagedListTest {
    companion object {
        private val ITEMS = List(100) { "$it" }
        private val config = Config(10)
        private val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
                callback.onResult(listOf("a"), 0, 1)
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
                fail()
            }
        }
    }

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
        runBlocking {
            val pagedList = PagedList.create(
                ListDataSource(ITEMS),
                GlobalScope,
                mainThread,
                backgroundThread,
                backgroundThread,
                null,
                config,
                0
            )

            backgroundThread.executeAll()
            assertEquals(ITEMS.subList(0, 30), pagedList)
            success = true
        }
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
            runBlocking {
                PagedList.create(
                    dataSource,
                    GlobalScope,
                    mainThread,
                    backgroundThread,
                    backgroundThread,
                    null,
                    config,
                    0
                )

                backgroundThread.executeAll()
                success = true
            }
        }
        assert(!success)
    }

    @Test
    fun defaults() = runBlocking {
        val pagedList = PagedList(
            dataSource = dataSource,
            config = config,
            fetchExecutor = DirectExecutor,
            notifyExecutor = DirectExecutor
        )

        assertEquals(dataSource, pagedList.dataSource)
        assertEquals(config, pagedList.config)
    }
}
