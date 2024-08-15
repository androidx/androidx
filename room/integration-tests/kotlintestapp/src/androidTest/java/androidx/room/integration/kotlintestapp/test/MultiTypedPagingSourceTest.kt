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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.testutil.ItemStore
import androidx.room.integration.kotlintestapp.testutil.PagingDb
import androidx.room.integration.kotlintestapp.testutil.PagingEntity
import androidx.room.integration.kotlintestapp.testutil.PagingEntityDao
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.testutils.FilteringExecutor
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This test intentionally uses real dispatchers to mimic the real use case.
 *
 * Runs paging source integration tests against different variants of Room Paging implementations
 */
@RunWith(Parameterized::class)
@MediumTest
class MultiTypedPagingSourceTest(
    private val pagingSourceFactory: (PagingEntityDao) -> PagingSource<Int, PagingEntity>,
) {
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var db: PagingDb
    private lateinit var itemStore: ItemStore

    // Multiple threads are necessary to prevent deadlock, since Room will acquire a thread to
    // dispatch on, when using the query / transaction dispatchers.
    private val queryExecutor = FilteringExecutor(Executors.newFixedThreadPool(2))
    private val mainThreadQueries = mutableListOf<Pair<String, String>>()
    private val pagingSources = mutableListOf<PagingSource<Int, PagingEntity>>()

    @Before
    fun init() {
        coroutineScope = CoroutineScope(Dispatchers.Main)
        itemStore = ItemStore(coroutineScope)
        db = buildAndReturnDb(queryExecutor, mainThreadQueries)
    }

    @After
    fun tearDown() {
        // Check no mainThread queries happened.
        assertThat(mainThreadQueries).isEmpty()
        coroutineScope.cancel()
        pagingSources.clear()
    }

    @Test
    fun simple_emptyStart_thenAddAnItem() {
        simple_emptyStart_thenAddAnItem(preOpenDb = true)
    }

    @Test
    fun simple_emptyStart_thenAddAnItem_withInitiallyClosedDatabase() {
        simple_emptyStart_thenAddAnItem(preOpenDb = false)
    }

    @Test
    fun loadEverything() {
        // open db
        val items = createItems(startId = 15, count = 50)
        db.getDao().insert(items)
        runTest {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    items.createExpected(fromIndex = 0, toIndex = CONFIG.initialLoadSize)
                )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(20)
                assertThat(itemStore.awaitItem(20)).isEqualTo(items[20])
            }

            // now access to the end of the list, it should load everything as we disabled jumping
            withContext(Dispatchers.Main) {
                itemStore.get(items.size - 1)
                assertThat(itemStore.awaitItem(items.size - 1)).isEqualTo(items.last())
            }
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun loadEverything_inReverse() {
        // open db
        val items = createItems(startId = 0, count = 100)
        db.getDao().insert(items)
        val pager = Pager(config = CONFIG, initialKey = 98) { db.getDao().loadItems() }
        runTest(pager) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    items.createExpected(
                        // Paging 3 implementation loads starting from initial key
                        fromIndex = 98,
                        toIndex = 100
                    )
                )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(40)
                assertThat(itemStore.awaitItem(40)).isEqualTo(items[40])
            }

            // now access to the beginning of the list, it should load everything as we don't
            // support jumping
            withContext(Dispatchers.Main) {
                itemStore.get(0)
                assertThat(itemStore.awaitItem(0)).isEqualTo(items[0])
            }

            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun keyTooLarge_returnLastPage() {
        val items = createItems(startId = 0, count = 50)
        db.getDao().insert(items)

        val pager = Pager(config = CONFIG, initialKey = 80) { db.getDao().loadItems() }
        runTest(pager = pager) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    // should return last page when key is too large
                    items.createExpected(
                        fromIndex = 41,
                        toIndex = 50,
                    )
                )
            // now trigger a prepend
            withContext(Dispatchers.Main) {
                itemStore.get(20)
                assertThat(itemStore.awaitItem(20)).isEqualTo(items[20])
            }
        }
    }

    @Test
    fun jumping() {
        val items = createItems(startId = 0, count = 200)
        db.getDao().insert(items)

        val config =
            PagingConfig(
                pageSize = 3,
                initialLoadSize = 9,
                enablePlaceholders = true,
                jumpThreshold = 80
            )
        val pager =
            Pager(
                config = config,
            ) {
                db.getDao().loadItems()
            }
        runTest(pager = pager) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    items.createExpected(
                        fromIndex = 0,
                        toIndex = config.initialLoadSize,
                    )
                )
            // now trigger a jump, accessed index needs to be larger than jumpThreshold
            withContext(Dispatchers.Main) { itemStore.get(120) }
            // the jump should trigger a refresh load with new generation
            itemStore.awaitGeneration(2)
            itemStore.awaitInitialLoad()
            // the refresh should load around anchorPosition of 120, with refresh key as 116
            // and null placeholders before and after
            assertThat(itemStore.peekItems())
                .containsExactlyElementsIn(
                    items.createExpected(
                        fromIndex = 116,
                        toIndex = 116 + config.initialLoadSize,
                    )
                )
        }
    }

    @Test
    @Ignore // b/287517337, b/287477564, b/287366097, b/287085166
    fun prependWithDelayedInvalidation() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        val pager =
            Pager(
                config = CONFIG,
                initialKey = 20,
                pagingSourceFactory = { db.getDao().loadItems().also { pagingSources.add(it) } }
            )

        runTest(pager) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    // should load starting from initial Key = 30
                    items.createExpected(fromIndex = 20, toIndex = 20 + CONFIG.initialLoadSize)
                )

            // now do some changes in the database but don't let change notifications go through
            // to the data source. it should not crash :)
            queryExecutor.filterFunction = {
                // TODO(b/): Avoid relying on function name, very brittle.
                !it.toString().contains("refreshInvalidationAsync")
            }

            db.getDao().deleteItems(items.subList(0, 60).map { it.id })

            // make sure we blocked the refresh runnable until after the exception generates a
            // new paging source
            queryExecutor.awaitDeferredSizeAtLeast(1)

            // Now get more items. The pagingSource's load() will check for invalidation and then
            // return LoadResult.Invalid, causing a second generation paging source to be generated.
            itemStore.get(2)

            itemStore.awaitGeneration(2)
            assertTrue(pagingSources[0].invalid)
            itemStore.awaitInitialLoad()

            // the initial load triggers a call to refreshVersionsAsync which calls
            // mRefreshRunnable. The runnable is getting filtered out but we need this one to
            // complete, so we executed the latest queued mRefreshRunnable.
            assertThat(queryExecutor.deferredSize()).isEqualTo(2)
            queryExecutor.executeLatestDeferred()
            assertThat(queryExecutor.deferredSize()).isEqualTo(1)

            // it might be reloaded in any range so just make sure everything is there
            // expects 30 items because items 60 - 89 left in database, so presenter should have
            // items 60-68 from initialLoad + 21 null placeholders
            assertThat(itemStore.peekItems()).hasSize(30)
            withContext(Dispatchers.Main) {
                (0 until 10).forEach { itemStore.get(it) }
                // now ensure all of them are loaded
                // only waiting for 9 items because because the 10th item and onwards are nulls from
                // placeholders
                (0 until 9).forEach {
                    assertThat(itemStore.awaitItem(it)).isEqualTo(items[60 + it])
                }
            }

            // Runs the original invalidationTracker.refreshRunnable.
            // Note that the second initial load's call to mRefreshRunnable resets the flag to
            // false, so this mRefreshRunnable will not detect changes in the table anymore.
            queryExecutor.executeAll()

            itemStore.awaitInitialLoad()

            // make sure only two generations of paging sources have been created
            assertTrue(!pagingSources[1].invalid)

            // if a third generation is created, awaitGeneration(3) will return instead of timing
            // out.
            val expectError = assertFailsWith<AssertionError> { itemStore.awaitGeneration(3) }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            assertThat(itemStore.currentGenerationId).isEqualTo(2)
            assertThat(pagingSources.size).isEqualTo(2)
        }
    }

    @FlakyTest(bugId = 260592924)
    @Test
    fun prependWithBlockingObserver() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        val pager =
            Pager(
                config = CONFIG,
                initialKey = 20,
                pagingSourceFactory = { db.getDao().loadItems().also { pagingSources.add(it) } }
            )

        // to block the PagingSource's observer, this observer needs to be registered first
        val blockingObserver =
            object : InvalidationTracker.Observer("PagingEntity") {
                // make sure observer blocks the time longer than the timeout of waiting for
                // paging source invalidation, so that we can assert new generation failure later
                override fun onInvalidated(tables: Set<String>) {
                    Thread.sleep(3_500)
                }
            }
        db.invalidationTracker.addObserver(blockingObserver)

        runTest(pager) {
            val initialLoad = itemStore.awaitInitialLoad()
            val initialItems =
                items.createExpected(fromIndex = 20, toIndex = 20 + CONFIG.initialLoadSize)
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    // should load starting from initial Key = 20
                    initialItems
                )

            db.getDao().deleteItems(items.subList(0, 60).map { it.id })

            // Now get more items. The pagingSource's load() will check for invalidation.
            // Normally the check would return "invalidation = true" but in this test case,
            // room's invalidation flag has already been reset but observer notification is delayed.
            // This means the paging source is not being invalidated.
            itemStore.get(10)

            val expectError = assertFailsWith<AssertionError> { itemStore.awaitGeneration(2) }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            // and stale PagingSource would return item 70 instead of item 10
            withContext(Dispatchers.Main) {
                assertThat(itemStore.awaitItem(10)).isEqualTo(items[70])
            }
            assertFalse(pagingSources[0].invalid)

            // prepend again
            itemStore.get(0)

            // the blocking observer's callback should complete now and the PagingSource should be
            // invalidated successfully
            itemStore.awaitGeneration(2)
            assertTrue(pagingSources[0].invalid)
            assertFalse(pagingSources[1].invalid)
        }
    }

    @Ignore // b/261205680
    @Test
    fun appendWithDelayedInvalidation() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)
        runTest {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    items.createExpected(fromIndex = 0, toIndex = CONFIG.initialLoadSize)
                )

            // now do some changes in the database but don't let change notifications go through
            // to the data source. it should not crash :)
            queryExecutor.filterFunction = {
                // TODO(b/): Avoid relying on function name, very brittle.
                !it.toString().contains("refreshInvalidation")
            }

            db.getDao().deleteItems(items.subList(0, 80).map { it.id })

            // make sure we blocked the refresh runnable until after the exception generates a
            // new paging source
            queryExecutor.awaitDeferredSizeAtLeast(1)

            // Now get more items. The pagingSource's load() will check for invalidation and then
            // return LoadResult.Invalid, causing a second generation paging source to be generated.
            itemStore.get(70)

            itemStore.awaitGeneration(2)
            assertTrue(pagingSources[0].invalid)
            // initial load is executed but refreshVersionsAsync's call to mRefreshRunnable is
            // actually queued up here
            itemStore.awaitInitialLoad()
            // the initial load triggers a call to refreshVersionsAsync which calls
            // mRefreshRunnable. The runnable is getting filtered out but we need this one to
            // complete, so we executed the latest queued mRefreshRunnable.
            assertThat(queryExecutor.deferredSize()).isEqualTo(2)
            queryExecutor.executeLatestDeferred()
            assertThat(queryExecutor.deferredSize()).isEqualTo(1)

            // second paging source should be generated
            assertThat(pagingSources.size).isEqualTo(2)

            // it might be reloaded in any range so just make sure everything is there
            assertThat(itemStore.peekItems()).hasSize(10)
            withContext(Dispatchers.Main) {
                (0 until 10).forEach { itemStore.get(it) }
                // now ensure all of them are loaded
                (0 until 10).forEach {
                    assertThat(itemStore.awaitItem(it)).isEqualTo(items[80 + it])
                }
            }

            // Runs the original invalidationTracker.refreshRunnable.
            // Note that the second initial load's call to mRefreshRunnable resets the flag to
            // false, so this mRefreshRunnable will not detect changes in the table anymore.
            queryExecutor.executeAll()

            itemStore.awaitInitialLoad()

            // make sure only two generations of paging sources have been created
            assertTrue(!pagingSources[1].invalid)

            // if a third generation is created, awaitGeneration(3) will return instead of timing
            // out.
            val expectError = assertFailsWith<AssertionError> { itemStore.awaitGeneration(3) }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            assertThat(itemStore.currentGenerationId).isEqualTo(2)
            assertThat(pagingSources.size).isEqualTo(2)
        }
    }

    private fun simple_emptyStart_thenAddAnItem(preOpenDb: Boolean) {
        if (preOpenDb) {
            // trigger db open
            db.openHelper.writableDatabase
        }

        runTest {
            itemStore.awaitGeneration(1)
            itemStore.awaitInitialLoad()
            assertThat(itemStore.peekItems()).isEmpty()

            val entity = PagingEntity(id = 1, value = "foo")
            db.getDao().insert(entity)
            itemStore.awaitGeneration(2)
            itemStore.awaitInitialLoad()
            assertThat(itemStore.peekItems()).containsExactly(entity)
        }
    }

    private fun runTest(
        pager: Pager<Int, PagingEntity> =
            Pager(
                config = CONFIG,
                pagingSourceFactory = {
                    pagingSourceFactory(db.getDao()).also { pagingSources.add(it) }
                }
            ),
        block: suspend () -> Unit
    ) {
        runTestWithPager(coroutineScope, itemStore, pager, block)
    }

    private companion object {
        /** Runs this test class against all variants of Room Paging */
        @Parameterized.Parameters(name = "pagingSourceFactory={0}")
        @JvmStatic
        fun parameters() =
            listOf(
                PagingEntityDao::loadItems,
                PagingEntityDao::loadItemsListenableFuture,
                PagingEntityDao::loadItemsRx2,
                PagingEntityDao::loadItemsRx3
            )
    }
}

/** Tests the secondary constructor of Room Paging implementations via RawQuery */
@RunWith(Parameterized::class)
@SmallTest
class MultiTypedPagingSourceTestWithRawQuery(
    private val pagingSourceFactoryRaw:
        (PagingEntityDao, SimpleSQLiteQuery) -> PagingSource<Int, PagingEntity>
) {
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var db: PagingDb
    private lateinit var itemStore: ItemStore

    // Multiple threads are necessary to prevent deadlock, since Room will acquire a thread to
    // dispatch on, when using the query / transaction dispatchers.
    private val queryExecutor = FilteringExecutor(Executors.newFixedThreadPool(2))
    private val mainThreadQueries = mutableListOf<Pair<String, String>>()

    @Before
    fun init() {
        coroutineScope = CoroutineScope(Dispatchers.Main)
        itemStore = ItemStore(coroutineScope)
        db = buildAndReturnDb(queryExecutor, mainThreadQueries)
    }

    @After
    fun tearDown() {
        // Check no mainThread queries happened.
        assertThat(mainThreadQueries).isEmpty()
        coroutineScope.cancel()
    }

    @Test
    fun loadEverythingRawQuery() {
        // open db
        val items = createItems(startId = 15, count = 50)
        db.getDao().insert(items)
        val query = SimpleSQLiteQuery("SELECT * FROM PagingEntity ORDER BY id ASC")
        runTest(query) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    items.createExpected(fromIndex = 0, toIndex = CONFIG.initialLoadSize)
                )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(20)
                assertThat(itemStore.awaitItem(20)).isEqualTo(items[20])
            }
            // now access to the end of the list, it should load everything as we disabled jumping
            withContext(Dispatchers.Main) {
                itemStore.get(items.size - 1)
                assertThat(itemStore.awaitItem(items.size - 1)).isEqualTo(items.last())
            }
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    @Ignore // b/312434479
    fun loadEverythingRawQuery_inReverse() {
        // open db
        val items = createItems(startId = 0, count = 100)
        db.getDao().insert(items)
        val query = SimpleSQLiteQuery("SELECT * FROM PagingEntity ORDER BY id ASC")
        val pager =
            Pager(config = CONFIG, initialKey = 98) { pagingSourceFactoryRaw(db.getDao(), query) }
        runTest(query, pager) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    items.createExpected(
                        // Paging 3 implementation loads starting from initial key
                        fromIndex = 98,
                        toIndex = 100
                    )
                )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(40)
                assertThat(itemStore.awaitItem(40)).isEqualTo(items[40])
            }
            // now access to the beginning of the list, it should load everything as we don't
            // support jumping
            withContext(Dispatchers.Main) {
                itemStore.get(0)
                assertThat(itemStore.awaitItem(0)).isEqualTo(items[0])
            }
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun rawQuery_userSuppliedLimitOffset() {
        val items = createItems(startId = 15, count = 70)
        db.getDao().insert(items)

        val query =
            SimpleSQLiteQuery("SELECT * FROM PagingEntity ORDER BY id ASC LIMIT 30 OFFSET 5")
        runTest(query) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    // returns items 20 to 28 with 21 null place holders after
                    items.createBoundedExpected(
                        fromIndex = 5,
                        toIndex = 5 + CONFIG.initialLoadSize,
                        toPlaceholderIndex = 35,
                    )
                )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(15)
                // item 15 is offset by 5 = 20
                assertThat(itemStore.awaitItem(15)).isEqualTo(items[20])
            }

            // normally itemStore.get(50) is valid, but user-set LIMIT should bound item count to 30
            // itemStore.get(50) should now become invalid
            val expectedException =
                assertFailsWith<IndexOutOfBoundsException> {
                    withContext(Dispatchers.Main) { itemStore.get(50) }
                }
            assertThat(expectedException.message).isEqualTo("Index: 50, Size: 30")
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun rawQuery_multipleArguments() {
        val items = createItems(startId = 0, count = 80)
        db.getDao().insert(items)
        val query =
            SimpleSQLiteQuery(
                "SELECT * " +
                    "FROM PagingEntity " +
                    "WHERE id > 49 AND id < 76 " +
                    "ORDER BY id ASC " +
                    "LIMIT 20"
            )
        runTest(query) {
            val initialLoad = itemStore.awaitInitialLoad()
            assertThat(initialLoad)
                .containsExactlyElementsIn(
                    // returns items 50 to 58 with 11 null place holders after
                    items.createBoundedExpected(
                        fromIndex = 50,
                        toIndex = 50 + CONFIG.initialLoadSize,
                        toPlaceholderIndex = 70,
                    )
                )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(15)
                // item 15 is offset by 50 because of `WHERE id > 49` arg
                assertThat(itemStore.awaitItem(15)).isEqualTo(items[65])
            }

            // normally itemStore.get(50) is valid, but user-set LIMIT should bound item count to 20
            val expectedException =
                assertFailsWith<IndexOutOfBoundsException> {
                    withContext(Dispatchers.Main) { itemStore.get(50) }
                }
            assertThat(expectedException.message).isEqualTo("Index: 50, Size: 20")
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    private fun runTest(
        query: SimpleSQLiteQuery,
        pager: Pager<Int, PagingEntity> =
            Pager(
                config = CONFIG,
                pagingSourceFactory = { pagingSourceFactoryRaw(db.getDao(), query) }
            ),
        block: suspend () -> Unit
    ) {
        runTestWithPager(coroutineScope, itemStore, pager, block)
    }

    private companion object {
        /** Runs this test class against all variants of Room Paging */
        @Parameterized.Parameters(name = "pagingSourceFactory={0}")
        @JvmStatic
        fun parameters() =
            listOf(
                PagingEntityDao::loadItemsRaw,
                PagingEntityDao::loadItemsRawListenableFuture,
                PagingEntityDao::loadItemsRawRx2,
                PagingEntityDao::loadItemsRawRx3
            )
    }
}

private fun buildAndReturnDb(
    queryExecutor: FilteringExecutor,
    mainThreadQueries: MutableList<Pair<String, String>>
): PagingDb {
    val mainThread: Thread = runBlocking(Dispatchers.Main) { Thread.currentThread() }
    return Room.inMemoryDatabaseBuilder(
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

private fun runTestWithPager(
    coroutineScope: CoroutineScope,
    itemStore: ItemStore,
    pager: Pager<Int, PagingEntity>,
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

internal fun createItems(startId: Int, count: Int): List<PagingEntity> {
    return List(count) { pos -> PagingEntity(id = pos + startId) }
}

/** Created an expected elements list from the current list. */
internal fun List<PagingEntity>.createExpected(
    fromIndex: Int,
    toIndex: Int,
): List<PagingEntity?> {
    val result = mutableListOf<PagingEntity?>()
    (0 until fromIndex).forEach { _ -> result.add(null) }
    result.addAll(this.subList(fromIndex, toIndex))
    (toIndex until size).forEach { _ -> result.add(null) }
    return result
}

internal fun List<PagingEntity>.createBoundedExpected(
    fromIndex: Int,
    toIndex: Int,
    toPlaceholderIndex: Int,
): List<PagingEntity?> {
    val result = mutableListOf<PagingEntity?>()
    result.addAll(this.subList(fromIndex, toIndex))
    (toIndex until toPlaceholderIndex).forEach { _ -> result.add(null) }
    return result
}

internal val CONFIG =
    PagingConfig(
        pageSize = 3,
        initialLoadSize = 9,
        enablePlaceholders = true,
    )
