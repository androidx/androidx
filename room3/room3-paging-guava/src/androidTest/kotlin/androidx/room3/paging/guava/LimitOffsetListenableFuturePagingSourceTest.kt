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

package androidx.room3.paging.guava

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.FilteringCoroutineContext
import androidx.testutils.FilteringExecutor
import androidx.testutils.TestExecutor
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures.addCallback
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val tableName: String = "TestItem"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class LimitOffsetListenableFuturePagingSourceTest {

    @JvmField @Rule val countingTaskExecutorRule = CountingTaskExecutorRule()

    @Test
    fun initialEmptyLoad_futureIsDone() = setupAndRun { db ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db = db)

        runTest {
            val listenableFuture = pagingSource.refresh()
            val page = listenableFuture.await() as LoadResult.Page

            assertThat(page.data).isEmpty()
            assertTrue(listenableFuture.isDone)
        }
    }

    @Test
    fun initialLoad_returnsFutureImmediately() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db = db)

            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.refresh()
            // ensure future is returned even as its result is still pending
            assertFalse(listenableFuture.isDone)
            assertThat(pagingSource.itemCount.get()).isEqualTo(-1)

            queryExecutor.executeAllAndDrainTasks() // run loadFuture

            val page = listenableFuture.await() as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun append_returnsFutureImmediately() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)

            pagingSource.bypassInitialLoad(100)

            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.append(key = 20)
            // ensure future is returned even as its result is still pending
            assertFalse(listenableFuture.isDone)

            queryExecutor.executeAllAndDrainTasks() // run loadFuture

            val page = listenableFuture.await() as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(ITEMS_LIST.subList(20, 25))
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun prepend_returnsFutureImmediately() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.bypassInitialLoad(100)

            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.prepend(key = 20)
            // ensure future is returned even as its result is still pending
            assertFalse(listenableFuture.isDone)

            queryExecutor.executeAllAndDrainTasks() // run loadFuture

            val page = listenableFuture.await() as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun append_returnsInvalid() = setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        queryContext.filterFunction = { ctx, _ ->
            ctx[CoroutineName]
                ?.name
                ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
        }
        val listenableFuture = pagingSource.append(key = 50)

        pagingSource.invalidate() // imitate refreshVersionsAsync invalidating the PagingSource
        assertTrue(pagingSource.invalid)

        queryExecutor.executeAllAndDrainTasks() // run loadFuture

        val result = listenableFuture.await()
        assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun prepend_returnsInvalid() = setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        queryContext.filterFunction = { ctx, _ ->
            ctx[CoroutineName]
                ?.name
                ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
        }
        val listenableFuture = pagingSource.prepend(key = 50)

        pagingSource.invalidate() // imitate refreshVersionsAsync invalidating the PagingSource
        assertTrue(pagingSource.invalid)

        queryExecutor.executeAllAndDrainTasks() // run loadFuture

        val result = listenableFuture.await()
        assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun refresh_consecutively() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        val pagingSource2 = LimitOffsetListenableFuturePagingSourceImpl(db)

        val listenableFuture1 = pagingSource.refresh(key = 10)
        val listenableFuture2 = pagingSource2.refresh(key = 15)

        // check that first Future completes first. If the first future didn't complete first,
        // this await() would not return.
        val page1 = listenableFuture1.await() as LoadResult.Page
        assertThat(page1.data).containsExactlyElementsIn(ITEMS_LIST.subList(10, 25))

        val page2 = listenableFuture2.await() as LoadResult.Page
        assertThat(page2.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 30))
    }

    @Test
    fun append_consecutively() = setupAndRunWithTestExecutor { db, _, queryExecutor ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture1 = pagingSource.append(key = 10)
        val listenableFuture2 = pagingSource.append(key = 15)

        queryExecutor.executeAllAndDrainTasks()

        val page1 = listenableFuture1.await() as LoadResult.Page
        assertThat(page1.data).containsExactlyElementsIn(ITEMS_LIST.subList(10, 15))

        val page2 = listenableFuture2.await() as LoadResult.Page
        assertThat(page2.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))

        assertTrue(listenableFuture1.isDone)
        assertTrue(listenableFuture2.isDone)
    }

    @Test
    fun prepend_consecutively() = setupAndRunWithTestExecutor { db, _, queryExecutor ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture1 = pagingSource.prepend(key = 25)
        val listenableFuture2 = pagingSource.prepend(key = 20)

        queryExecutor.executeAllAndDrainTasks()

        val page1 = listenableFuture1.await() as LoadResult.Page
        assertThat(page1.data).containsExactlyElementsIn(ITEMS_LIST.subList(20, 25))

        val page2 = listenableFuture2.await() as LoadResult.Page
        assertThat(page2.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))

        assertTrue(listenableFuture1.isDone)
        assertTrue(listenableFuture2.isDone)
    }

    @Test
    fun refresh_onSuccess() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)

        val listenableFuture = pagingSource.refresh(key = 30)

        var onSuccessReceived = false
        val callbackExecutor = TestExecutor()
        listenableFuture.onSuccess(callbackExecutor) { result ->
            val page = result as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(ITEMS_LIST.subList(30, 45))
            onSuccessReceived = true
        }

        // wait until Room db's refresh load is complete
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertTrue(listenableFuture.isDone)

        callbackExecutor.executeAll()

        // make sure onSuccess callback was executed
        assertTrue(onSuccessReceived)
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun append_onSuccess() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture = pagingSource.append(key = 20)
        // ensure future is returned even as its result is still pending
        assertFalse(listenableFuture.isDone)

        var onSuccessReceived = false
        val callbackExecutor = TestExecutor()
        listenableFuture.onSuccess(callbackExecutor) { result ->
            val page = result as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(ITEMS_LIST.subList(20, 25))
            onSuccessReceived = true
        }
        // let room db complete load
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        callbackExecutor.executeAll()

        // make sure onSuccess callback was executed
        assertTrue(onSuccessReceived)
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun prepend_onSuccess() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture = pagingSource.prepend(key = 40)
        // ensure future is returned even as its result is still pending
        assertFalse(listenableFuture.isDone)

        var onSuccessReceived = false
        val callbackExecutor = TestExecutor()
        listenableFuture.onSuccess(callbackExecutor) { result ->
            val page = result as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(ITEMS_LIST.subList(35, 40))
            onSuccessReceived = true
        }
        // let room db complete load
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        callbackExecutor.executeAll()

        // make sure onSuccess callback was executed
        assertTrue(onSuccessReceived)
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun append_awaitThrowsCancellationException() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.bypassInitialLoad(100)

            // queue up the append first
            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.append(key = 20)

            listenableFuture.cancel(true)
            queryExecutor.executeAllAndDrainTasks()

            // await() should throw after cancellation
            assertFailsWith<CancellationException> { listenableFuture.await() }
        }

    @Test
    fun prepend_awaitThrowsCancellationException() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.bypassInitialLoad(100)

            // queue up the prepend first
            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.prepend(key = 30)

            listenableFuture.cancel(true)
            queryExecutor.executeAllAndDrainTasks()

            // await() should throw after cancellation
            assertFailsWith<CancellationException> { listenableFuture.await() }
        }

    @Test
    fun refresh_canceledFutureRunsOnFailureCallback() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)

            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.refresh(key = 30)

            val callbackExecutor = TestExecutor()
            var onFailureReceived = false
            listenableFuture.onFailure(callbackExecutor) { throwable ->
                assertThat(throwable).isInstanceOf<CancellationException>()
                onFailureReceived = true
            }

            // now cancel future and execute the refresh load. The refresh should not complete.
            listenableFuture.cancel(true)
            queryExecutor.executeAllAndDrainTasks()

            callbackExecutor.executeAll()

            // make sure onFailure callback was executed
            assertTrue(onFailureReceived)
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun append_canceledFutureRunsOnFailureCallback2() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.bypassInitialLoad(100)

            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.append(key = 20)

            val callbackExecutor = TestExecutor()
            var onFailureReceived = false
            listenableFuture.onFailure(callbackExecutor) { throwable ->
                assertThat(throwable).isInstanceOf<CancellationException>()
                onFailureReceived = true
            }

            // now cancel future and execute the append load. The append should not complete.
            listenableFuture.cancel(true)
            queryExecutor.executeAllAndDrainTasks()

            callbackExecutor.executeAll()

            // make sure onFailure callback was executed
            assertTrue(onFailureReceived)
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun prepend_canceledFutureRunsOnFailureCallback() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.bypassInitialLoad(100)

            // queue up the prepend first
            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.prepend(key = 30)

            val callbackExecutor = TestExecutor()
            var onFailureReceived = false
            listenableFuture.onFailure(callbackExecutor) { throwable ->
                assertThat(throwable).isInstanceOf<CancellationException>()
                onFailureReceived = true
            }

            // now cancel future and execute the prepend which should not complete.
            listenableFuture.cancel(true)
            queryExecutor.executeAllAndDrainTasks()

            callbackExecutor.executeAll()

            // make sure onFailure callback was executed
            assertTrue(onFailureReceived)
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun refresh_AfterCancellation() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture = pagingSource.prepend(key = 50)

        listenableFuture.cancel(true)
        assertFailsWith<CancellationException> { listenableFuture.await() }

        // new gen after query from previous gen was cancelled
        val pagingSource2 = LimitOffsetListenableFuturePagingSourceImpl(db)
        val listenableFuture2 = pagingSource2.refresh()
        val result = listenableFuture2.await() as LoadResult.Page

        // the new generation should load as usual
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
    }

    @Test
    fun appendAgain_afterFutureCanceled() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture = pagingSource.append(key = 30)

        listenableFuture.cancel(true)
        assertFailsWith<CancellationException> { listenableFuture.await() }
        assertTrue(listenableFuture.isDone)
        assertFalse(pagingSource.invalid)

        val listenableFuture2 = pagingSource.append(key = 30)

        val result = listenableFuture2.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(30, 35))
        assertTrue(listenableFuture2.isDone)
    }

    @Test
    fun prependAgain_afterFutureCanceled() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.bypassInitialLoad(100)

        val listenableFuture = pagingSource.prepend(key = 30)

        listenableFuture.cancel(true)
        assertFailsWith<CancellationException> { listenableFuture.await() }
        assertFalse(pagingSource.invalid)
        assertTrue(listenableFuture.isDone)

        val listenableFuture2 = pagingSource.prepend(key = 30)

        val result = listenableFuture2.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(25, 30))
        assertTrue(listenableFuture2.isDone)
    }

    @Test
    fun append_insertInvalidatesPagingSource() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db = db)
            pagingSource.bypassInitialLoad(100)

            // queue up the append first
            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.append(key = 20)

            countingTaskExecutorRule.drainTasks(1, TimeUnit.SECONDS)

            // run this async separately from queryExecutor
            run { db.getDao().addItem(TestItem(101)) }

            countingTaskExecutorRule.drainTasks(1, TimeUnit.SECONDS)
            queryExecutor.executeAll()

            val result = listenableFuture.await()
            assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
            assertThat(pagingSource.invalid)
        }

    @Test
    fun prepend_insertInvalidatesPagingSource() =
        setupAndRunWithTestExecutor { db, queryContext, queryExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db = db)
            pagingSource.bypassInitialLoad(100)

            // queue up the append first
            queryContext.filterFunction = { ctx, _ ->
                ctx[CoroutineName]
                    ?.name
                    ?.equals("LimitOffsetListenableFuturePagingSource.loadFuture") != true
            }
            val listenableFuture = pagingSource.prepend(key = 20)

            // run this async separately from queryExecutor
            run { db.getDao().addItem(TestItem(101)) }

            countingTaskExecutorRule.drainTasks(1, TimeUnit.SECONDS)
            queryExecutor.executeAll()

            val result = listenableFuture.await()
            assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
            assertThat(pagingSource.invalid)
        }

    @Test
    fun test_jumpSupport() = setupAndRun { db ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        assertTrue(pagingSource.jumpingSupported)
    }

    private fun setupAndRun(test: suspend (LimitOffsetTestDb) -> Unit) {
        val db =
            Room.inMemoryDatabaseBuilder<LimitOffsetTestDb>(
                    ApplicationProvider.getApplicationContext()
                )
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(
                    ArchTaskExecutor.getIOThreadExecutor().asCoroutineDispatcher()
                )
                .build()

        runTest { test(db) }
        tearDown(db)
    }

    private fun setupAndRunWithTestExecutor(
        test: suspend (LimitOffsetTestDb, FilteringCoroutineContext, FilteringExecutor) -> Unit
    ) {
        val executorService =
            object : AbstractExecutorService() {
                override fun execute(command: Runnable?) {
                    ArchTaskExecutor.getIOThreadExecutor().execute(command)
                }

                override fun shutdown() {}

                override fun shutdownNow(): List<Runnable?> = emptyList()

                override fun isShutdown(): Boolean = false

                override fun isTerminated(): Boolean = false

                override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = false
            }
        val queryContext = FilteringCoroutineContext(executorService)
        val queryExecutor = queryContext.executor
        val db =
            Room.inMemoryDatabaseBuilder<LimitOffsetTestDb>(
                    ApplicationProvider.getApplicationContext()
                )
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(queryContext)
                .build()

        runTest {
            db.getDao().addAllItems(ITEMS_LIST)
            queryExecutor.executeAllAndDrainTasks() // InvalidationTracker from the addAllItems
            test(db, queryContext, queryExecutor)
        }
        tearDown(db)
    }

    private fun tearDown(db: LimitOffsetTestDb) {
        db.close()
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    private fun FilteringExecutor.executeAllAndDrainTasks() {
        this.executeAll()
        countingTaskExecutorRule.drainTasks(1, TimeUnit.SECONDS)
    }
}

private class LimitOffsetListenableFuturePagingSourceImpl(
    db: RoomDatabase,
    queryString: String = "SELECT * FROM $tableName ORDER BY id ASC",
) :
    LimitOffsetListenableFuturePagingSource<TestItem>(
        sourceQuery = RoomRawQuery(queryString),
        db = db,
        tables = arrayOf(tableName),
    ) {

    override suspend fun convertRows(
        limitOffsetQuery: RoomRawQuery,
        itemCount: Int,
    ): List<TestItem> {
        return performSuspending(db, isReadOnly = true, inTransaction = false) { connection ->
            connection.prepare(limitOffsetQuery.sql).use { statement ->
                val stmtIndexOfId = getColumnIndexOrThrow(statement, "id")
                buildList {
                    while (statement.step()) {
                        val tmpId = statement.getInt(stmtIndexOfId)
                        add(TestItem(tmpId))
                    }
                }
            }
        }
    }
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.bypassInitialLoad(itemCount: Int) {
    this.itemCount.set(itemCount) // bypass check for initial load
    this.refreshComplete.set(true)
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.refresh(
    key: Int? = null
): ListenableFuture<LoadResult<Int, TestItem>> {
    return loadFuture(createLoadParam(loadType = LoadType.REFRESH, key = key))
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.append(
    key: Int? = -1
): ListenableFuture<LoadResult<Int, TestItem>> {
    return loadFuture(createLoadParam(loadType = LoadType.APPEND, key = key))
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.prepend(
    key: Int? = -1
): ListenableFuture<LoadResult<Int, TestItem>> {
    return loadFuture(createLoadParam(loadType = LoadType.PREPEND, key = key))
}

private val CONFIG = PagingConfig(pageSize = 5, enablePlaceholders = true, initialLoadSize = 15)

private val ITEMS_LIST = createItemsForDb(0, 100)

private fun createItemsForDb(startId: Int, count: Int): List<TestItem> {
    return List(count) { TestItem(id = it + startId) }
}

private fun createLoadParam(
    loadType: LoadType,
    key: Int? = null,
    initialLoadSize: Int = CONFIG.initialLoadSize,
    pageSize: Int = CONFIG.pageSize,
    placeholdersEnabled: Boolean = CONFIG.enablePlaceholders,
): PagingSource.LoadParams<Int> {
    return when (loadType) {
        LoadType.REFRESH -> {
            PagingSource.LoadParams.Refresh(
                key = key,
                loadSize = initialLoadSize,
                placeholdersEnabled = placeholdersEnabled,
            )
        }
        LoadType.APPEND -> {
            PagingSource.LoadParams.Append(
                key = key ?: -1,
                loadSize = pageSize,
                placeholdersEnabled = placeholdersEnabled,
            )
        }
        LoadType.PREPEND -> {
            PagingSource.LoadParams.Prepend(
                key = key ?: -1,
                loadSize = pageSize,
                placeholdersEnabled = placeholdersEnabled,
            )
        }
    }
}

private fun ListenableFuture<LoadResult<Int, TestItem>>.onSuccess(
    executor: Executor,
    onSuccessCallback: (LoadResult<Int, TestItem>?) -> Unit,
) {
    addCallback(
        this,
        object : FutureCallback<LoadResult<Int, TestItem>> {
            override fun onSuccess(result: LoadResult<Int, TestItem>?) {
                onSuccessCallback(result)
            }

            override fun onFailure(t: Throwable) {
                assertWithMessage(
                        "Expected onSuccess callback instead of onFailure, " +
                            "received ${t.localizedMessage}"
                    )
                    .fail()
            }
        },
        executor,
    )
}

private fun ListenableFuture<LoadResult<Int, TestItem>>.onFailure(
    executor: Executor,
    onFailureCallback: (Throwable) -> Unit,
) {
    addCallback(
        this,
        object : FutureCallback<LoadResult<Int, TestItem>> {
            override fun onSuccess(result: LoadResult<Int, TestItem>?) {
                assertWithMessage(
                        "Expected onFailure callback instead of onSuccess, " +
                            "received result $result"
                    )
                    .fail()
            }

            override fun onFailure(t: Throwable) {
                onFailureCallback(t)
            }
        },
        executor,
    )
}

@Database(entities = [TestItem::class], version = 1, exportSchema = false)
abstract class LimitOffsetTestDb : RoomDatabase() {
    abstract fun getDao(): TestItemDao
}

@Entity(tableName = "TestItem")
data class TestItem(@PrimaryKey val id: Int, val value: String = "item $id")

@Dao
interface TestItemDao {
    @Insert fun addAllItems(testItems: List<TestItem>)

    @Insert fun addItem(testItem: TestItem)

    @Query("SELECT * FROM TestItem ORDER BY id ASC") fun getAllItems(): List<TestItem>
}
