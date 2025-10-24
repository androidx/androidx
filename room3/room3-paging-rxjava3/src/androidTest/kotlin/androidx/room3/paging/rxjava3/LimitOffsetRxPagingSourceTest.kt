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

package androidx.room3.paging.rxjava3

import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
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
import androidx.test.filters.MediumTest
import androidx.testutils.TestExecutor
import androidx.testutils.withTestTimeout
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val tableName: String = "TestItem"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class LimitOffsetRxPagingSourceTest {

    @JvmField @Rule val countingTaskExecutorRule = CountingTaskExecutorRule()

    @After
    fun tearDown() {
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertTrue(countingTaskExecutorRule.isIdle)
    }

    @Test
    fun initialLoad_empty() = setupAndRun { db ->
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.refresh()
        val result = single.await() as LoadResult.Page
        assertThat(result.data).isEmpty()
    }

    @Test
    fun initialLoad() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.refresh()
        val result = single.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
    }

    @Test
    fun simpleAppend() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.append(key = 15)
        val result = single.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))
    }

    @Test
    fun simplePrepend() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.prepend(key = 20)
        val result = single.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))
    }

    @Test
    fun refresh_singleImmediatelyReturn() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.refresh()

        val observer = single.test()
        observer.assertNotComplete()

        // let room complete its tasks
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)

        val result = observer.values().first() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
        observer.assertComplete()
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun append_singleImmediatelyReturn() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.append(key = 10)

        val observer = single.test()
        observer.assertNotComplete()

        // let room complete its tasks
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)

        val result = observer.values().first() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(10, 15))
        observer.assertComplete()
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun prepend_singleImmediatelyReturn() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.prepend(key = 15)

        val observer = single.test()
        observer.assertNotComplete()

        // let room complete its tasks
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)

        val result = observer.values().first() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(10, 15))
        observer.assertComplete()
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun dbUpdate_invalidatesPagingSource() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.append(key = 50)

        // trigger load to register observer
        single.await()
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)

        // this should cause refreshVersionsSync to invalidate pagingSource
        db.getDao().addItem(TestItem(113))
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)

        assertTrue(pagingSource.invalid)

        val single2 = pagingSource.append(key = 55)
        val result = single2.await()
        assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
    }

    @Test
    fun append_returnsInvalid() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.append(key = 50)

        // this should cause load to return LoadResult.Invalid
        pagingSource.invalidate()
        assertTrue(pagingSource.invalid)

        // trigger load
        val result = single.await()

        // let room complete its tasks
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
    }

    @Test
    fun prepend_returnsInvalid() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.prepend(key = 50)

        // this should cause load to return LoadResult.Invalid
        pagingSource.invalidate()
        assertTrue(pagingSource.invalid)

        // trigger load
        val observer = single.test()

        // let room complete its tasks
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        val result = observer.values().first()
        assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
        observer.dispose()
    }

    @Test
    fun refresh_consecutively() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        val single = pagingSource.refresh()
        val result = single.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))

        val pagingSource2 = LimitOffsetRxPagingSourceImpl(db)
        val single2 = pagingSource2.refresh()
        val result2 = single2.await() as LoadResult.Page
        assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
    }

    @Test
    fun append_consecutively() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)

        val single = pagingSource.append(key = 15)
        val result = single.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))

        val single2 = pagingSource.append(key = 40)
        val result2 = single2.await() as LoadResult.Page
        assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(40, 45))

        val single3 = pagingSource.append(key = 45) // sequential append
        val result3 = single3.await() as LoadResult.Page
        assertThat(result3.data).containsExactlyElementsIn(ITEMS_LIST.subList(45, 50))
    }

    @Test
    fun prepend_consecutively() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)

        val single = pagingSource.prepend(key = 15)
        val result = single.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(10, 15))

        val single2 = pagingSource.prepend(key = 40)
        val result2 = single2.await() as LoadResult.Page
        assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(35, 40))

        val single3 = pagingSource.prepend(key = 45) // sequential prepend
        val result3 = single3.await() as LoadResult.Page
        assertThat(result3.data).containsExactlyElementsIn(ITEMS_LIST.subList(40, 45))
    }

    @Test
    fun refreshAgain_afterDispose() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)

        var isDisposed = false
        val single =
            pagingSource
                .refresh()
                // dispose right after subscription
                .doOnSubscribe { disposable -> disposable.dispose() }
                .doOnDispose { isDisposed = true }
                .doOnSuccess { assertWithMessage("The single should not succeed").fail() }
                .doOnError { assertWithMessage("The single should not error out").fail() }

        assertFailsWith<AssertionError> { withTestTimeout(2) { single.await() } }
        assertTrue(isDisposed)
        assertFalse(pagingSource.invalid)

        // using same paging source
        val single2 = pagingSource.refresh()
        val result2 = single2.await() as LoadResult.Page
        assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(0, 15))
    }

    @Test
    fun appendAgain_afterDispose() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)

        var isDisposed = false
        val single =
            pagingSource
                .append(key = 15)
                // dispose right after subscription
                .doOnSubscribe { disposable -> disposable.dispose() }
                .doOnDispose { isDisposed = true }
                .doOnSuccess { assertWithMessage("The single should not succeed").fail() }
                .doOnError { assertWithMessage("The single should not error out").fail() }

        assertFailsWith<AssertionError> { withTestTimeout(2) { single.await() } }
        assertTrue(isDisposed)
        assertFalse(pagingSource.invalid)

        // try with same key same paging source
        val single2 = pagingSource.append(key = 15)
        val result2 = single2.await() as LoadResult.Page
        assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))
    }

    @Test
    fun prependAgain_afterDispose() = setupAndRun { db ->
        db.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)

        var isDisposed = false
        val single =
            pagingSource
                .prepend(key = 40)
                // dispose right after subscription
                .doOnSubscribe { disposable -> disposable.dispose() }
                .doOnDispose { isDisposed = true }
                .doOnSuccess { assertWithMessage("The single should not succeed").fail() }
                .doOnError { assertWithMessage("The single should not error out").fail() }

        assertFailsWith<AssertionError> { withTestTimeout(2) { single.await() } }
        assertTrue(isDisposed)
        assertFalse(pagingSource.invalid)

        // try with same key same paging source
        val single2 = pagingSource.prepend(key = 40)
        val result2 = single2.await() as LoadResult.Page
        assertThat(result2.data).containsExactlyElementsIn(ITEMS_LIST.subList(35, 40))
    }

    @Test
    fun assert_usesQueryExecutor() {
        val queryExecutor = TestExecutor()
        val testDb =
            Room.inMemoryDatabaseBuilder<LimitOffsetTestDb>(
                    ApplicationProvider.getApplicationContext()
                )
                .setDriver(AndroidSQLiteDriver())
                .setQueryExecutor(queryExecutor)
                .build()

        testDb.getDao().addAllItems(ITEMS_LIST)
        queryExecutor.executeAll() // add items first

        runTest {
            assertFalse(queryExecutor.executeAll()) // make sure its idle now
            val pagingSource = LimitOffsetRxPagingSourceImpl(testDb)
            val single = pagingSource.append(key = 15)

            var resultReceived = false
            // subscribe to single
            launch {
                val result = single.await() as LoadResult.Page
                assertThat(result.data).containsExactlyElementsIn(ITEMS_LIST.subList(15, 20))
                resultReceived = true
            }

            advanceUntilIdle()

            // execute Single's await()
            assertTrue(queryExecutor.executeAll())

            advanceUntilIdle()

            assertTrue(resultReceived)
            assertFalse(queryExecutor.executeAll())
        }
        testDb.close()
    }

    @Test
    fun cancelledCoroutine_disposesSingle() {
        val testDb =
            Room.inMemoryDatabaseBuilder<LimitOffsetTestDb>(
                    ApplicationProvider.getApplicationContext()
                )
                .setDriver(AndroidSQLiteDriver())
                .build()

        testDb.getDao().addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetRxPagingSourceImpl(testDb)

        runBlocking {
            var isDisposed = false
            val single =
                pagingSource
                    .refresh()
                    .doOnSubscribe { Thread.sleep(300) } // subscribe but delay the load
                    .doOnSuccess { assertWithMessage("The single should not succeed").fail() }
                    .doOnError { assertWithMessage("The single should not error out").fail() }
                    .doOnDispose { isDisposed = true }

            val job = launch { single.await() }
            job.start()
            delay(100) // start single.await() to subscribe but don't let it complete
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertTrue(isDisposed)
        }
        // need to drain before closing testDb or else will throw SQLiteConnectionPool exception
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        testDb.close()
    }

    @Test
    fun jumping_enabled() = setupAndRun { db ->
        val pagingSource = LimitOffsetRxPagingSourceImpl(db)
        assertTrue(pagingSource.jumpingSupported)
    }

    private fun setupAndRun(test: suspend (LimitOffsetTestDb) -> Unit) {
        val db =
            Room.inMemoryDatabaseBuilder<LimitOffsetTestDb>(
                    ApplicationProvider.getApplicationContext()
                )
                .setDriver(AndroidSQLiteDriver())
                .build()

        runTest { test(db) }
        db.close()
    }
}

private class LimitOffsetRxPagingSourceImpl(
    db: RoomDatabase,
    query: String = "SELECT * FROM $tableName ORDER BY id ASC",
) :
    LimitOffsetRxPagingSource<TestItem>(
        db = db,
        sourceQuery = RoomRawQuery(query),
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

private fun LimitOffsetRxPagingSource<TestItem>.refresh(
    key: Int? = null
): Single<LoadResult<Int, TestItem>> {
    return loadSingle(createLoadParam(loadType = LoadType.REFRESH, key = key))
}

private fun LimitOffsetRxPagingSource<TestItem>.append(
    key: Int? = -1
): Single<LoadResult<Int, TestItem>> {
    itemCount.set(ITEMS_LIST.size) // to bypass check for initial load
    refreshComplete.set(true)
    return loadSingle(createLoadParam(loadType = LoadType.APPEND, key = key))
}

private fun LimitOffsetRxPagingSource<TestItem>.prepend(
    key: Int? = -1
): Single<LoadResult<Int, TestItem>> {
    itemCount.set(ITEMS_LIST.size) // to bypass check for initial load
    refreshComplete.set(true)
    return loadSingle(createLoadParam(loadType = LoadType.PREPEND, key = key))
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
): LoadParams<Int> {
    return when (loadType) {
        LoadType.REFRESH -> {
            LoadParams.Refresh(
                key = key,
                loadSize = initialLoadSize,
                placeholdersEnabled = placeholdersEnabled,
            )
        }
        LoadType.APPEND -> {
            LoadParams.Append(
                key = key ?: -1,
                loadSize = pageSize,
                placeholdersEnabled = placeholdersEnabled,
            )
        }
        LoadType.PREPEND -> {
            LoadParams.Prepend(
                key = key ?: -1,
                loadSize = pageSize,
                placeholdersEnabled = placeholdersEnabled,
            )
        }
    }
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

    @Query("SELECT COUNT(*) from $tableName") fun itemCount(): Int
}
