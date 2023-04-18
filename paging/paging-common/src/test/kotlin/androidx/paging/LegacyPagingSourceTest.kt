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

import androidx.paging.PagingSource.LoadResult.Page
import androidx.testutils.TestDispatcher
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class LegacyPagingSourceTest {
    private val fakePagingState = PagingState(
        pages = listOf(
            Page<Int, String>(
                data = listOf("fakeData"),
                prevKey = null,
                nextKey = null
            )
        ),
        anchorPosition = 0,
        config = PagingConfig(
            pageSize = 1,
            prefetchDistance = 1
        ),
        leadingPlaceholderCount = 0
    )

    @Test
    fun init_invalidDataSource() {
        val testContext = EmptyCoroutineContext
        val dataSource = object : DataSource<Int, Int>(KeyType.ITEM_KEYED) {
            var isInvalidCalls = 0

            override val isInvalid: Boolean
                get() {
                    isInvalidCalls++
                    return true
                }

            override suspend fun load(params: Params<Int>): BaseResult<Int> {
                return BaseResult(listOf(), null, null)
            }

            override fun getKeyInternal(item: Int): Int = 0
        }

        val pagingSource = LegacyPagingSource(
            fetchContext = testContext,
            dataSource = dataSource,
        )

        assertEquals(1, dataSource.isInvalidCalls)
        assertThat(pagingSource.invalid).isTrue()
        assertThat(dataSource.isInvalid).isTrue()
    }

    @Test
    fun item() {
        @Suppress("DEPRECATION")
        val dataSource = object : ItemKeyedDataSource<Int, String>() {
            override fun loadInitial(
                params: LoadInitialParams<Int>,
                callback: LoadInitialCallback<String>
            ) {
                Assert.fail("loadInitial not expected")
            }

            override fun loadAfter(
                params: LoadParams<Int>,
                callback: LoadCallback<String>
            ) {
                Assert.fail("loadAfter not expected")
            }

            override fun loadBefore(
                params: LoadParams<Int>,
                callback: LoadCallback<String>
            ) {
                Assert.fail("loadBefore not expected")
            }

            override fun getKey(item: String) = item.hashCode()
        }
        val pagingSource = LegacyPagingSource(
            fetchContext = Dispatchers.Unconfined,
            dataSource
        )

        // Check that jumpingSupported is disabled.
        assertFalse { pagingSource.jumpingSupported }

        // Check that invalidation propagates successfully in both directions.
        val refreshKey = pagingSource.getRefreshKey(fakePagingState)
        assertEquals("fakeData".hashCode(), refreshKey)

        assertFalse { pagingSource.invalid }
        assertFalse { dataSource.isInvalid }

        pagingSource.invalidate()

        assertTrue { pagingSource.invalid }
        assertTrue { dataSource.isInvalid }
    }

    @Test
    fun page() {
        @Suppress("DEPRECATION")
        val dataSource = object : PageKeyedDataSource<Int, String>() {
            override fun loadInitial(
                params: LoadInitialParams<Int>,
                callback: LoadInitialCallback<Int, String>
            ) {
                Assert.fail("loadInitial not expected")
            }

            override fun loadBefore(
                params: LoadParams<Int>,
                callback: LoadCallback<Int, String>
            ) {
                Assert.fail("loadBefore not expected")
            }

            override fun loadAfter(
                params: LoadParams<Int>,
                callback: LoadCallback<Int, String>
            ) {
                Assert.fail("loadAfter not expected")
            }
        }
        val pagingSource = LegacyPagingSource(
            fetchContext = Dispatchers.Unconfined,
            dataSource = dataSource
        )

        // Check that jumpingSupported is disabled.
        assertFalse { pagingSource.jumpingSupported }

        // Check that invalidation propagates successfully in both directions.
        val refreshKey = pagingSource.getRefreshKey(fakePagingState)
        assertEquals(refreshKey, null)

        assertFalse { pagingSource.invalid }
        assertFalse { dataSource.isInvalid }

        pagingSource.invalidate()

        assertTrue { pagingSource.invalid }
        assertTrue { dataSource.isInvalid }
    }

    @Test
    fun positional() {
        val pagingSource = LegacyPagingSource(
            fetchContext = Dispatchers.Unconfined,
            dataSource = createTestPositionalDataSource()
        )

        // Check that jumpingSupported is enabled.
        assertTrue { pagingSource.jumpingSupported }

        assertEquals(
            4,
            pagingSource.getRefreshKey(
                PagingState(
                    pages = listOf(
                        Page(
                            data = listOf("fakeData"),
                            prevKey = 4,
                            nextKey = 5
                        )
                    ),
                    anchorPosition = 0,
                    config = PagingConfig(
                        pageSize = 1,
                        prefetchDistance = 1
                    ),
                    leadingPlaceholderCount = 0
                )
            )
        )

        assertEquals(
            6,
            pagingSource.getRefreshKey(
                PagingState(
                    pages = listOf(
                        Page(
                            data = listOf("fakeData"),
                            prevKey = 4,
                            nextKey = 5
                        )
                    ),
                    anchorPosition = 2,
                    config = PagingConfig(
                        pageSize = 1,
                        prefetchDistance = 1
                    ),
                    leadingPlaceholderCount = 0
                )
            )
        )
    }

    @Test
    fun invalidateFromPagingSource() {
        val pagingSource = LegacyPagingSource(
            fetchContext = Dispatchers.Unconfined,
            dataSource = createTestPositionalDataSource()
        )
        val dataSource = pagingSource.dataSource

        var kotlinInvalidated = false
        dataSource.addInvalidatedCallback {
            kotlinInvalidated = true
        }
        var javaInvalidated = false
        dataSource.addInvalidatedCallback { javaInvalidated = true }

        assertFalse { pagingSource.invalid }
        assertFalse { dataSource.isInvalid }

        pagingSource.invalidate()

        assertTrue { pagingSource.invalid }
        assertTrue { dataSource.isInvalid }
        assertTrue { kotlinInvalidated }
        assertTrue { javaInvalidated }
    }

    @Test
    fun invalidateFromDataSource() {
        val pagingSource = LegacyPagingSource(
            fetchContext = Dispatchers.Unconfined,
            dataSource = createTestPositionalDataSource()
        )
        val dataSource = pagingSource.dataSource

        var kotlinInvalidated = false
        dataSource.addInvalidatedCallback {
            kotlinInvalidated = true
        }
        var javaInvalidated = false
        dataSource.addInvalidatedCallback(object : DataSource.InvalidatedCallback {
            override fun onInvalidated() {
                javaInvalidated = true
            }
        })

        assertFalse { pagingSource.invalid }
        assertFalse { dataSource.isInvalid }

        dataSource.invalidate()

        assertTrue { pagingSource.invalid }
        assertTrue { dataSource.isInvalid }
        assertTrue { kotlinInvalidated }
        assertTrue { javaInvalidated }
    }

    @Suppress("DEPRECATION")
    @Test
    fun createDataSourceOnFetchDispatcher() {
        val methodCalls = mutableMapOf<String, MutableList<Thread>>()

        val dataSourceFactory = object : DataSource.Factory<Int, String>() {
            override fun create(): DataSource<Int, String> {
                return ThreadCapturingDataSource { methodName ->
                    methodCalls.getOrPut(methodName) {
                        mutableListOf()
                    }.add(Thread.currentThread())
                }
            }
        }

        // create an executor special to the legacy data source
        val executor = Executors.newSingleThreadExecutor()

        // extract the thread instance from the executor. we'll use it to assert calls later
        var dataSourceThread: Thread? = null
        executor.submit {
            dataSourceThread = Thread.currentThread()
        }.get()

        val pager = Pager(
            config = PagingConfig(10, enablePlaceholders = false),
            pagingSourceFactory = dataSourceFactory.asPagingSourceFactory(
                executor.asCoroutineDispatcher()
            )
        )
        // collect from pager. we take only 2 paging data generations and only take 1 PageEvent
        // from them
        runBlocking {
            pager.flow.take(2).collectLatest { pagingData ->
                // wait until first insert happens
                pagingData.flow.filter {
                    it is PageEvent.Insert
                }.first()
                pagingData.uiReceiver.refresh()
            }
        }
        // validate method calls (to ensure test did run as expected) and their threads.
        assertThat(methodCalls["<init>"]).hasSize(2)
        assertThat(methodCalls["<init>"]?.toSet()).containsExactly(dataSourceThread)
        assertThat(methodCalls["addInvalidatedCallback"]).hasSize(2)
        assertThat(methodCalls["addInvalidatedCallback"]?.toSet()).containsExactly(dataSourceThread)
        assertThat(methodCalls["loadInitial"]).hasSize(2)
        assertThat(methodCalls).containsKey("isInvalid")
        assertThat(methodCalls["loadInitial"]?.toSet()).containsExactly(dataSourceThread)
        // TODO b/174625633 this should also be 2
        assertThat(methodCalls["removeInvalidatedCallback"]).hasSize(1)
    }

    @Test
    fun dataSourceInvalidateBeforePagingSourceInvalidateCallbackAdded() {
        val dataSourceFactory = object : DataSource.Factory<Int, String>() {
            val dataSources = mutableListOf<DataSource<Int, String>>()
            var i = 0

            override fun create(): DataSource<Int, String> {
                return when (i++) {
                    0 -> createTestPositionalDataSource().apply {
                        // Invalidate before we give LegacyPagingSource a chance to register
                        // invalidate callback.
                        invalidate()
                    }
                    else -> createTestPositionalDataSource()
                }.also { dataSources.add(it) }
            }
        }

        val testDispatcher = TestDispatcher()
        val pagingSourceFactory = dataSourceFactory.asPagingSourceFactory(
            fetchDispatcher = testDispatcher
        ).let {
            { it() as LegacyPagingSource }
        }

        val pagingSource0 = pagingSourceFactory()
        testDispatcher.executeAll()
        assertTrue { pagingSource0.dataSource.isInvalid }
        assertTrue { pagingSource0.invalid }
        assertTrue { dataSourceFactory.dataSources[0].isInvalid }
        assertEquals(dataSourceFactory.dataSources[0], pagingSource0.dataSource)

        val pagingSource1 = pagingSourceFactory()
        testDispatcher.executeAll()
        assertFalse { pagingSource1.dataSource.isInvalid }
        assertFalse { pagingSource1.invalid }
        assertFalse { dataSourceFactory.dataSources[1].isInvalid }
        assertEquals(dataSourceFactory.dataSources[1], pagingSource1.dataSource)

        assertEquals(2, dataSourceFactory.dataSources.size)
    }

    @Suppress("DEPRECATION")
    private fun createTestPositionalDataSource(expectInitialLoad: Boolean = false) =
        object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
                if (!expectInitialLoad) {
                    Assert.fail("loadInitial not expected")
                } else {
                    callback.onResult(listOf(), 0)
                }
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
                Assert.fail("loadRange not expected")
            }
        }

    /**
     * A data source implementation which tracks method calls and their threads.
     */
    @Suppress("DEPRECATION")
    class ThreadCapturingDataSource(
        private val recordMethodCall: (methodName: String) -> Unit
    ) : PositionalDataSource<String>() {
        init {
            recordMethodCall("<init>")
        }

        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<String>
        ) {
            recordMethodCall("loadInitial")
            callback.onResult(
                data = emptyList(),
                position = 0,
            )
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
            recordMethodCall("loadRange")
            callback.onResult(data = emptyList())
        }

        override val isInvalid: Boolean
            get() {
                // this is important because room's implementation might run a db query to
                // update invalidations.
                recordMethodCall("isInvalid")
                return super.isInvalid
            }

        override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            recordMethodCall("addInvalidatedCallback")
            super.addInvalidatedCallback(onInvalidatedCallback)
        }

        override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            recordMethodCall("removeInvalidatedCallback")
            super.removeInvalidatedCallback(onInvalidatedCallback)
        }

        override fun invalidate() {
            recordMethodCall("invalidate")
            super.invalidate()
        }
    }
}
