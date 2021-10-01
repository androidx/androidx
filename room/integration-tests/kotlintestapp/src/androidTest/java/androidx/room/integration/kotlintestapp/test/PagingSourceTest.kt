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

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.awaitPendingRefresh
import androidx.room.pendingRefresh
import androidx.room.refreshRunnable
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.base.MainThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.FilteringExecutor
import androidx.testutils.withTestTimeout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * This test intentionally uses real dispatchers to mimic the real use case.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class PagingSourceTest {
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var db: Paging3Db
    private lateinit var itemStore: ItemStore

    // Multiple threads are necessary to prevent deadlock, since Room will acquire a thread to
    // dispatch on, when using the query / transaction dispatchers.
    private val queryExecutor = FilteringExecutor(delegate = Executors.newFixedThreadPool(2))
    private val mainThreadQueries = mutableListOf<Pair<String, String>>()
    private val pagingSources = mutableListOf<PagingSource<Int, PagingEntity>>()

    @Before
    fun init() {
        coroutineScope = CoroutineScope(Dispatchers.Main)
        itemStore = ItemStore(coroutineScope)

        val mainThread: Thread = runBlocking(Dispatchers.Main) {
            Thread.currentThread()
        }
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Paging3Db::class.java
        ).setQueryCallback(
            { sqlQuery, _ ->
                if (Thread.currentThread() === mainThread) {
                    mainThreadQueries.add(
                        sqlQuery to Throwable().stackTraceToString()
                    )
                }
            },
            {
                // instantly execute the log callback so that we can check the thread.
                it.run()
            }
        ).setQueryExecutor(queryExecutor)
            .build()
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
        db.dao.insert(items)
        runTest {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    fromIndex = 0,
                    toIndex = CONFIG.initialLoadSize
                )
            )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(20)
            }
            assertThat(itemStore.awaitItem(20)).isEqualTo(items[20])
            // now access to the end of the list, it should load everything as we disabled jumping
            withContext(Dispatchers.Main) {
                itemStore.get(items.size - 1)
            }
            assertThat(itemStore.awaitItem(items.size - 1)).isEqualTo(items.last())
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun loadEverythingRawQuery() {
        // open db
        val items = createItems(startId = 15, count = 50)
        db.dao.insert(items)
        val query = SimpleSQLiteQuery(
            "SELECT * FROM PagingEntity ORDER BY id ASC"
        )
        val pager = Pager(
            config = CONFIG,
            pagingSourceFactory = { db.dao.loadItemsRaw(query) }
        )
        runTest(pager = pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    fromIndex = 0,
                    toIndex = CONFIG.initialLoadSize
                )
            )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(20)
            }
            assertThat(itemStore.awaitItem(20)).isEqualTo(items[20])
            // now access to the end of the list, it should load everything as we disabled jumping
            withContext(Dispatchers.Main) {
                itemStore.get(items.size - 1)
            }
            assertThat(itemStore.awaitItem(items.size - 1)).isEqualTo(items.last())
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun loadEverything_inReverse() {
        // open db
        val items = createItems(startId = 0, count = 100)
        db.dao.insert(items)
        val pager = Pager(
            config = CONFIG,
            initialKey = 98
        ) {
            db.dao.loadItems()
        }
        runTest(pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    // Paging 3 implementation loads starting from initial key
                    fromIndex = 98,
                    toIndex = 100
                )
            )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(40)
            }
            assertThat(itemStore.awaitItem(40)).isEqualTo(items[40])
            // now access to the beginning of the list, it should load everything as we don't
            // support jumping
            withContext(Dispatchers.Main) {
                itemStore.get(0)
            }
            assertThat(itemStore.awaitItem(0)).isEqualTo(items[0])
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun loadEverythingRawQuery_inReverse() {
        // open db
        val items = createItems(startId = 0, count = 100)
        db.dao.insert(items)
        val query = SimpleSQLiteQuery(
            "SELECT * FROM PagingEntity ORDER BY id ASC"
        )
        val pager = Pager(
            config = CONFIG,
            initialKey = 98,
            pagingSourceFactory = { db.dao.loadItemsRaw(query) }
        )
        runTest(pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    // Paging 3 implementation loads starting from initial key
                    fromIndex = 98,
                    toIndex = 100
                )
            )
            // now access more items that should trigger loading more
            withContext(Dispatchers.Main) {
                itemStore.get(40)
            }
            assertThat(itemStore.awaitItem(40)).isEqualTo(items[40])
            // now access to the beginning of the list, it should load everything as we don't
            // support jumping
            withContext(Dispatchers.Main) {
                itemStore.get(0)
            }
            assertThat(itemStore.awaitItem(0)).isEqualTo(items[0])
            assertThat(itemStore.peekItems()).isEqualTo(items)
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun rawQuery_userSuppliedLimitOffset() {
        val items = createItems(startId = 15, count = 70)
        db.dao.insert(items)

        val query = SimpleSQLiteQuery(
            "SELECT * FROM PagingEntity ORDER BY id ASC LIMIT 30 OFFSET 5"
        )
        val pager = Pager(
            config = CONFIG,
            pagingSourceFactory = { db.dao.loadItemsRaw(query) }
        )

        runTest(pager = pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
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
            }
            // item 15 is offset by 5 = 20
            assertThat(itemStore.awaitItem(15)).isEqualTo(items[20])
            // normally itemStore.get(50) is valid, but user-set LIMIT should bound item count to 30
            // itemStore.get(50) should now become invalid
            val expectedException = assertFailsWith<IndexOutOfBoundsException> {
                withContext(Dispatchers.Main) {
                    itemStore.get(50)
                }
            }
            assertThat(expectedException.message).isEqualTo("Index: 50, Size: 30")
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun rawQuery_multipleArguments() {
        val items = createItems(startId = 0, count = 80)
        db.dao.insert(items)
        val query = SimpleSQLiteQuery(
            "SELECT * " +
                "FROM PagingEntity " +
                "WHERE id > 49 AND id < 76 " +
                "ORDER BY id ASC " +
                "LIMIT 20"
        )
        val pager = Pager(
            config = CONFIG,
            pagingSourceFactory = { db.dao.loadItemsRaw(query) }
        )

        runTest(pager = pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
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
            }
            // item 15 is offset by 50 because of `WHERE id > 49` arg
            assertThat(itemStore.awaitItem(15)).isEqualTo(items[65])
            // normally itemStore.get(50) is valid, but user-set LIMIT should bound item count to 20
            val expectedException = assertFailsWith<IndexOutOfBoundsException> {
                withContext(Dispatchers.Main) {
                    itemStore.get(50)
                }
            }
            assertThat(expectedException.message).isEqualTo("Index: 50, Size: 20")
            assertThat(itemStore.currentGenerationId).isEqualTo(1)
        }
    }

    @Test
    fun keyTooLarge_returnLastPage() {
        val items = createItems(startId = 0, count = 50)
        db.dao.insert(items)

        val pager = Pager(
            config = CONFIG,
            initialKey = 80
        ) {
            db.dao.loadItems()
        }
        runTest(pager = pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                // should return last page when key is too large
                items.createExpected(
                    fromIndex = 41,
                    toIndex = 50,
                )
            )
            // now trigger a prepend
            withContext(Dispatchers.Main) {
                itemStore.get(20)
            }
            assertThat(itemStore.awaitItem(20)).isEqualTo(items[20])
        }
    }

    @Test
    fun jumping() {
        val items = createItems(startId = 0, count = 200)
        db.dao.insert(items)

        val config = PagingConfig(
            pageSize = 3,
            initialLoadSize = 9,
            enablePlaceholders = true,
            jumpThreshold = 80
        )
        val pager = Pager(
            config = config,
        ) {
            db.dao.loadItems()
        }
        runTest(pager = pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    fromIndex = 0,
                    toIndex = config.initialLoadSize,
                )
            )
            // now trigger a jump, accessed index needs to be larger than jumpThreshold
            withContext(Dispatchers.Main) {
                itemStore.get(120)
            }
            // the jump should trigger a refresh load with new generation
            itemStore.awaitGeneration(2)
            itemStore.awaitInitialLoad()
            // the refresh should load around anchorPosition of 120, with refresh key as 116
            // and null placeholders before and after
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    fromIndex = 116,
                    toIndex = 116 + config.initialLoadSize,
                )
            )
        }
    }

    @Test
    fun prependWithDelayedInvalidation() {
        val items = createItems(startId = 0, count = 90)
        db.dao.insert(items)

        val pager = Pager(
            config = CONFIG,
            initialKey = 20,
            pagingSourceFactory = { db.dao.loadItems().also { pagingSources.add(it) } }
        )

        runTest(pager) {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                // should load starting from initial Key = 30
                items.createExpected(
                    fromIndex = 20,
                    toIndex = 20 + CONFIG.initialLoadSize
                )
            )
            assertThat(db.invalidationTracker.pendingRefresh).isFalse()
            // now do some changes in the database but don't let change notifications go through
            // to the data source. it should not crash :)
            queryExecutor.filterFunction = { runnable ->
                runnable !== db.invalidationTracker.refreshRunnable
            }
            db.dao.deleteItems(
                items.subList(0, 60).map { it.id }
            )
            // make sure invalidation requests a refresh
            db.invalidationTracker.awaitPendingRefresh()

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
                (0 until 10).forEach {
                    itemStore.get(it)
                }
            }
            // now ensure all of them are loaded
            // only waiting for 9 items because because the 10th item and onwards are nulls from
            // placeholders
            (0 until 9).forEach {
                assertThat(
                    itemStore.awaitItem(it)
                ).isEqualTo(
                    items[60 + it]
                )
            }

            // Runs the original invalidationTracker.refreshRunnable.
            // Note that the second initial load's call to mRefreshRunnable resets the flag to
            // false, so this mRefreshRunnable will not detect changes in the table anymore.
            assertThat(db.invalidationTracker.pendingRefresh).isFalse()
            queryExecutor.executeAll()

            itemStore.awaitInitialLoad()

            // make sure only two generations of paging sources have been created
            assertTrue(!pagingSources[1].invalid)

            // if a third generation is created, awaitGeneration(3) will return instead of timing
            // out.
            val expectError = assertFailsWith<AssertionError> {
                itemStore.awaitGeneration(3)
            }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            assertThat(itemStore.currentGenerationId).isEqualTo(2)
            assertThat(pagingSources.size).isEqualTo(2)
        }
    }

    @Test
    fun prependWithBlockingObserver() {
        val items = createItems(startId = 0, count = 90)
        db.dao.insert(items)

        val pager = Pager(
            config = CONFIG,
            initialKey = 20,
            pagingSourceFactory = { db.dao.loadItems().also { pagingSources.add(it) } }
        )

        // to block the PagingSource's observer, this observer needs to be registered first
        val blockingObserver = object : InvalidationTracker.Observer("PagingEntity") {
            // make sure observer blocks the time longer than the timeout of waiting for
            // paging source invalidation, so that we can assert new generation failure later
            override fun onInvalidated(tables: MutableSet<String>) {
                Thread.sleep(3_500)
            }
        }
        db.invalidationTracker.addWeakObserver(
            blockingObserver
        )

        runTest(pager) {
            itemStore.awaitInitialLoad()
            val initialItems = items.createExpected(
                fromIndex = 20,
                toIndex = 20 + CONFIG.initialLoadSize
            )
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                // should load starting from initial Key = 20
                initialItems
            )
            assertThat(db.invalidationTracker.pendingRefresh).isFalse()

            db.dao.deleteItems(
                items.subList(0, 60).map { it.id }
            )

            // Now get more items. The pagingSource's load() will check for invalidation.
            // Normally the check would return "invalidation = true" but in this test case,
            // room's invalidation flag has already been reset but observer notification is delayed.
            // This means the paging source is not being invalidated.
            itemStore.get(10)

            val expectError = assertFailsWith<AssertionError> {
                itemStore.awaitGeneration(2)
            }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            // and stale PagingSource would return item 70 instead of item 10
            assertThat(itemStore.awaitItem(10)).isEqualTo(items[70])
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

    private fun simple_emptyStart_thenAddAnItem(
        preOpenDb: Boolean
    ) {
        if (preOpenDb) {
            // trigger db open
            db.openHelper.writableDatabase
        }

        runTest {
            itemStore.awaitGeneration(1)
            itemStore.awaitInitialLoad()
            assertThat(itemStore.peekItems()).isEmpty()

            val entity = PagingEntity(id = 1, value = "foo")
            db.dao.insert(entity)
            itemStore.awaitGeneration(2)
            itemStore.awaitInitialLoad()
            assertThat(itemStore.peekItems()).containsExactly(entity)
        }
    }

    @Test
    fun appendWithDelayedInvalidation() {
        val items = createItems(startId = 0, count = 90)
        db.dao.insert(items)
        runTest {
            itemStore.awaitInitialLoad()
            assertThat(
                itemStore.peekItems()
            ).containsExactlyElementsIn(
                items.createExpected(
                    fromIndex = 0,
                    toIndex = CONFIG.initialLoadSize
                )
            )
            assertThat(db.invalidationTracker.pendingRefresh).isFalse()
            // now do some changes in the database but don't let change notifications go through
            // to the data source. it should not crash :)
            queryExecutor.filterFunction = { runnable ->
                runnable !== db.invalidationTracker.refreshRunnable
            }
            db.dao.deleteItems(
                items.subList(0, 80).map { it.id }
            )
            // make sure invalidation requests a refresh
            db.invalidationTracker.awaitPendingRefresh()
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
                (0 until 10).forEach {
                    itemStore.get(it)
                }
            }
            // now ensure all of them are loaded
            (0 until 10).forEach {
                assertThat(
                    itemStore.awaitItem(it)
                ).isEqualTo(
                    items[80 + it]
                )
            }

            // Runs the original invalidationTracker.refreshRunnable.
            // Note that the second initial load's call to mRefreshRunnable resets the flag to
            // false, so this mRefreshRunnable will not detect changes in the table anymore.
            assertThat(db.invalidationTracker.pendingRefresh).isFalse()
            queryExecutor.executeAll()

            itemStore.awaitInitialLoad()

            // make sure only two generations of paging sources have been created
            assertTrue(!pagingSources[1].invalid)

            // if a third generation is created, awaitGeneration(3) will return instead of timing
            // out.
            val expectError = assertFailsWith<AssertionError> {
                itemStore.awaitGeneration(3)
            }
            assertThat(expectError.message).isEqualTo("didn't complete in expected time")

            assertThat(itemStore.currentGenerationId).isEqualTo(2)
            assertThat(pagingSources.size).isEqualTo(2)
        }
    }

    private fun runTest(
        pager: Pager<Int, PagingEntity> =
            Pager(
                config = CONFIG,
                pagingSourceFactory = { db.dao.loadItems().also { pagingSources.add(it) } }
            ),
        block: suspend () -> Unit
    ) {
        val collection = coroutineScope.launch(Dispatchers.Main) {
            pager.flow.collectLatest {
                itemStore.collectFrom(it)
            }
        }
        runBlocking {
            try {
                block()
            } finally {
                collection.cancelAndJoin()
            }
        }
    }

    private fun createItems(
        startId: Int,
        count: Int
    ): List<PagingEntity> {
        return List(count) { pos ->
            PagingEntity(
                id = pos + startId
            )
        }
    }

    /**
     * Created an expected elements list from the current list.
     */
    private fun List<PagingEntity?>.createExpected(
        fromIndex: Int,
        toIndex: Int,
    ): List<PagingEntity?> {
        val result = mutableListOf<PagingEntity?>()
        (0 until fromIndex).forEach { _ -> result.add(null) }
        result.addAll(this.subList(fromIndex, toIndex))
        (toIndex until size).forEach { _ -> result.add(null) }
        return result
    }

    private fun List<PagingEntity?>.createBoundedExpected(
        fromIndex: Int,
        toIndex: Int,
        toPlaceholderIndex: Int,
    ): List<PagingEntity?> {
        val result = mutableListOf<PagingEntity?>()
        result.addAll(this.subList(fromIndex, toIndex))
        (toIndex until toPlaceholderIndex).forEach { _ -> result.add(null) }
        return result
    }

    @Database(
        version = 1,
        exportSchema = false,
        entities = [PagingEntity::class]
    )
    abstract class Paging3Db : RoomDatabase() {
        abstract val dao: Paging3Dao
    }

    @Entity
    data class PagingEntity(
        @PrimaryKey
        val id: Int,
        val value: String = "item_$id"
    ) {
        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PagingEntity>() {
                override fun areItemsTheSame(
                    oldItem: PagingEntity,
                    newItem: PagingEntity
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: PagingEntity,
                    newItem: PagingEntity
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    @Dao
    interface Paging3Dao {
        @Insert
        fun insert(items: List<PagingEntity>)

        @Insert
        fun insert(vararg items: PagingEntity)

        @Query("DELETE FROM PagingEntity WHERE id IN (:ids)")
        fun deleteItems(ids: List<Int>)

        @Query("SELECT * FROM PagingEntity ORDER BY id ASC")
        fun loadItems(): PagingSource<Int, PagingEntity>

        @RawQuery(observedEntities = [PagingEntity::class])
        fun loadItemsRaw(query: SupportSQLiteQuery): PagingSource<Int, PagingEntity>
    }

    /**
     * Our fake adapter that holds the items.
     */
    private class ItemStore(private val coroutineScope: CoroutineScope) {
        // We get a new generation each time list changes. This is used to await certain events
        // happening. Each generation have an id that maps to a paging generation.
        // This value is modified only on the main thread.
        private val generation = MutableStateFlow(Generation(0))

        val currentGenerationId
            get() = generation.value.id

        val asyncDiffer = AsyncPagingDataDiffer(
            diffCallback = PagingEntity.DIFF_CALLBACK,
            updateCallback = object : ListUpdateCallback {
                override fun onInserted(position: Int, count: Int) {
                    onDataSetChanged(generation.value.id)
                }

                override fun onRemoved(position: Int, count: Int) {
                    onDataSetChanged(generation.value.id)
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    onDataSetChanged(generation.value.id)
                }

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    onDataSetChanged(generation.value.id)
                }
            }
        )

        init {
            coroutineScope.launch {
                asyncDiffer.loadStateFlow
                    .drop(1) // Ignore initial state
                    .distinctUntilChangedBy { it.source.refresh }
                    .map { it.source.refresh }
                    .filter { it is LoadState.NotLoading }
                    .collect {
                        val current = generation.value
                        generation.value = current.copy(
                            initialLoadCompleted = true,
                        )
                    }
            }
        }

        private fun incrementGeneration() {
            val current = generation.value
            generation.value = current.copy(
                initialLoadCompleted = false,
                id = current.id + 1,
            )
        }

        fun peekItems() = (0 until asyncDiffer.itemCount).map {
            asyncDiffer.peek(it)
        }

        fun get(index: Int): PagingEntity? {
            return asyncDiffer.getItem(index)
        }

        suspend fun awaitItem(index: Int): PagingEntity = withTestTimeout {
            generation.mapLatest {
                asyncDiffer.peek(index)
            }.filterNotNull().first()
        }

        suspend fun collectFrom(data: PagingData<PagingEntity>) {
            incrementGeneration()
            asyncDiffer.submitData(data)
        }

        @MainThread
        private fun onDataSetChanged(id: Int) {
            coroutineScope.launch(Dispatchers.Main) {
                // deferring this
                yield()
                val curGen = generation.value
                if (curGen.id == id) {
                    generation.value = curGen.copy(
                        initialLoadCompleted = true,
                        changeCount = curGen.changeCount + 1
                    )
                }
            }
        }

        suspend fun awaitInitialLoad() = withTestTimeout {
            withContext(Dispatchers.Main) {
                generation.filter { it.initialLoadCompleted }.first()
            }
        }

        suspend fun awaitGeneration(id: Int) = withTestTimeout {
            withContext(Dispatchers.Main) {
                generation.filter { it.id == id }.first()
            }
        }
    }

    /**
     * Holds some metadata about the backing paging list
     */
    private data class Generation(
        /**
         * Generation id, incremented each time data source is invalidated
         */
        val id: Int,
        /**
         * True when the data source completes its initial load
         */
        val initialLoadCompleted: Boolean = false,
        /**
         * Incremented each time we receive some update events.
         */
        val changeCount: Int = 0
    )

    companion object {
        private val CONFIG = PagingConfig(
            pageSize = 3,
            initialLoadSize = 9,
            enablePlaceholders = true,
        )
    }
}