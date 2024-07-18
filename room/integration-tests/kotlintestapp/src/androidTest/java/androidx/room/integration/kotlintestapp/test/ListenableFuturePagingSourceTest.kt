/*
 * Copyright 2022 The Android Open Source Project
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
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.paging.ListenableFuturePagingSource
import androidx.paging.Pager
import androidx.paging.PagingState
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.testutil.ItemStore
import androidx.room.integration.kotlintestapp.testutil.PagingDb
import androidx.room.integration.kotlintestapp.testutil.PagingEntity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.FilteringExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ListenableFuturePagingSourceTest {

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var db: PagingDb
    private lateinit var itemStore: ItemStore

    // Multiple threads are necessary to prevent deadlock, since Room will acquire a thread to
    // dispatch on, when using the query / transaction dispatchers.
    private val queryExecutor = FilteringExecutor(Executors.newFixedThreadPool(2))
    private val mainThreadQueries = mutableListOf<Pair<String, String>>()
    private val pagingSources = mutableListOf<ListenableFuturePagingSourceImpl>()

    @Before
    fun init() {
        coroutineScope = CoroutineScope(Dispatchers.Main)
        itemStore = ItemStore(coroutineScope)

        val mainThread: Thread = runBlocking(Dispatchers.Main) { Thread.currentThread() }
        db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    PagingDb::class.java
                )
                .setQueryCallback(
                    object : RoomDatabase.QueryCallback {
                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                            if (Thread.currentThread() === mainThread) {
                                mainThreadQueries.add(sqlQuery to Throwable().stackTraceToString())
                            }
                        }
                    }
                ) {
                    // instantly execute the log callback so that we can check the thread.
                    it.run()
                }
                .setQueryExecutor(queryExecutor)
                .build()
    }

    @After
    fun tearDown() {
        // Check no mainThread queries happened.
        assertThat(mainThreadQueries).isEmpty()
        coroutineScope.cancel()
    }

    @Test
    fun refresh_canceledCoroutine_cancelsFuture() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        // filter right away to block initial load
        queryExecutor.filterFunction = { runnable ->
            // filtering out the transform async function called inside loadFuture
            // filtering as String b/c `AbstractTransformFuture` is a package-private class
            runnable.javaClass.enclosingClass?.toString()?.contains("AbstractTransformFuture") !=
                true
        }

        runTest {
            val expectError =
                assertFailsWith<AssertionError> { itemStore.awaitInitialLoad(timeOutDuration = 2) }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            val futures = pagingSources[0].futures
            assertThat(futures.size).isEqualTo(1)
            assertThat(futures[0].isDone).isFalse() // initial load future is pending

            // now cancel collection which should also cancel the future
            coroutineScope.cancel()

            // just making sure no new futures are created, and ensuring that the pending future
            // is now cancelled
            assertThat(futures.size).isEqualTo(1)
            assertThat(futures[0].isCancelled).isTrue()
            assertThat(futures[0].isDone).isTrue()
        }
    }

    @Test
    fun append_canceledCoroutine_cancelsFuture() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        runTest {
            itemStore.awaitInitialLoad()

            val futures = pagingSources[0].futures
            assertThat(futures.size).isEqualTo(1)
            assertThat(futures[0].isDone).isTrue() // initial load future is complete

            queryExecutor.filterFunction = { runnable ->
                // filtering out the transform async function called inside loadFuture
                // filtering as String b/c `AbstractTransformFuture` is a package-private class
                runnable.javaClass.enclosingClass
                    ?.toString()
                    ?.contains("AbstractTransformFuture") != true
            }

            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) { itemStore.get(10) }

            // await should fail because we have blocked the paging source' async function,
            // which calls nonInitialLoad in this case, from executing
            val expectError =
                assertFailsWith<AssertionError> {
                    assertThat(itemStore.awaitItem(index = 10, timeOutDuration = 2))
                        .isEqualTo(items[10])
                }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")
            queryExecutor.awaitDeferredSizeAtLeast(1)

            // even though the load runnable was blocked, a new future should have been returned
            assertThat(futures.size).isEqualTo(2)
            // ensure future is pending
            assertThat(futures[1].isDone).isFalse()

            // now cancel collection which should also cancel the future
            coroutineScope.cancel()

            // just making sure no new futures are created, and ensuring that the pending future
            // is now cancelled
            assertThat(futures.size).isEqualTo(2)
            assertThat(futures[1].isCancelled).isTrue()
            assertThat(futures[1].isDone).isTrue()
        }
    }

    @Test
    fun prepend_canceledCoroutine_cancelsFuture() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        runTest {
            itemStore.awaitInitialLoad()

            val futures = pagingSources[0].futures
            assertThat(futures.size).isEqualTo(1)
            assertThat(futures[0].isDone).isTrue() // initial load future is complete

            queryExecutor.filterFunction = { runnable ->
                // filtering out the transform async function called inside loadFuture
                // filtering as String b/c `AbstractTransformFuture` is a package-private class
                runnable.javaClass.enclosingClass
                    ?.toString()
                    ?.contains("AbstractTransformFuture") != true
            }

            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) { itemStore.get(40) }

            // await should fail because we have blocked the paging source' async function,
            // which calls nonInitialLoad in this case, from executing
            val expectError =
                assertFailsWith<AssertionError> {
                    assertThat(itemStore.awaitItem(index = 40, timeOutDuration = 2))
                        .isEqualTo(items[40])
                }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")
            queryExecutor.awaitDeferredSizeAtLeast(1)

            // even though the load runnable was blocked, a new future should have been returned
            assertThat(futures.size).isEqualTo(2)
            // ensure future is pending
            assertThat(futures[1].isDone).isFalse()

            // now cancel collection which should also cancel the future
            coroutineScope.cancel()

            // just making sure no new futures are created, and ensuring that the pending future
            // is now cancelled
            assertThat(futures.size).isEqualTo(2)
            assertThat(futures[1].isCancelled).isTrue()
            assertThat(futures[1].isDone).isTrue()
        }
    }

    private fun runTest(
        pager: Pager<Int, PagingEntity> =
            Pager(config = CONFIG) {
                val baseSource = db.getDao().loadItemsListenableFuture()
                // to get access to the futures returned from loadFuture. Also to
                // mimic real use case of wrapping the source returned from Room.
                ListenableFuturePagingSourceImpl(baseSource).also { pagingSources.add(it) }
            },
        block: suspend () -> Unit
    ) {
        val collection =
            coroutineScope.launch(Dispatchers.Main) {
                pager.flow.collectLatest { itemStore.collectFrom(it) }
            }
        runBlocking {
            try {
                block()
            } finally {
                collection.cancelAndJoin()
            }
        }
    }
}

private class ListenableFuturePagingSourceImpl(
    private val baseSource: ListenableFuturePagingSource<Int, PagingEntity>
) : ListenableFuturePagingSource<Int, PagingEntity>() {

    val futures = mutableListOf<ListenableFuture<LoadResult<Int, PagingEntity>>>()

    override fun getRefreshKey(state: PagingState<Int, PagingEntity>): Int? {
        return baseSource.getRefreshKey(state)
    }

    override fun loadFuture(
        params: LoadParams<Int>
    ): ListenableFuture<LoadResult<Int, PagingEntity>> {
        return baseSource.loadFuture(params).also { futures.add(it) }
    }
}
