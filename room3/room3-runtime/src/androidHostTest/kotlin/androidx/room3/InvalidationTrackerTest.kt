/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.concurrent.AtomicBoolean
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.util.Locale
import kotlin.collections.removeFirst as removeFirstKt
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class InvalidationTrackerTest {

    private val testCoroutineScope = TestScope()

    private lateinit var tracker: InvalidationTracker
    private lateinit var sqliteDriver: FakeSQLiteDriver
    private lateinit var roomDatabase: FakeRoomDatabase

    @Before
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setup() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        val shadowTables = buildMap {
            put("C", "C_content")
            put("d", "a")
        }
        val viewTables = buildMap { put("e", setOf("a")) }
        val tableNames = arrayOf("a", "B", "i", "C", "d")
        sqliteDriver = FakeSQLiteDriver()
        roomDatabase = FakeRoomDatabase(shadowTables, viewTables, tableNames)
        roomDatabase.init(
            DatabaseConfiguration(
                    context = mock(),
                    name = null,
                    migrationContainer = RoomDatabase.MigrationContainer(),
                    callbacks = null,
                    allowMainThreadQueries = true,
                    journalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
                    multiInstanceInvalidationServiceIntent = null,
                    requireMigration = true,
                    allowDestructiveMigrationOnDowngrade = false,
                    migrationNotRequiredFrom = null,
                    prepackagedDatabaseCallback = null,
                    typeConverters = emptyList(),
                    autoMigrationSpecs = emptyList(),
                    allowDestructiveMigrationForAllTables = false,
                    sqliteDriver = sqliteDriver,
                    queryCoroutineContext = testCoroutineScope.coroutineContext,
                )
                .apply { this.preparedStatementCacheSize = 0 }
        )
        tracker = roomDatabase.invalidationTracker
    }

    @After
    fun after() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun observerWithNoExistingTable() = runTest {
        assertThrows<IllegalArgumentException> { tracker.createFlow("x").singleOrNull() }
            .hasMessageThat()
            .isEqualTo("There is no table with name x")
    }

    @Test
    fun ignoreCaseInTableNames() = runTest {
        val invalidations = tracker.createFlow("I").produceIn(this)
        assertThat(invalidations.receive()).containsExactly("I")
        invalidations.cancel()
    }

    @Test
    fun observeOneTable() = runTest {
        val invalidations = tracker.createFlow("a", emitInitialState = false).produceIn(this)

        // Mark 'a' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("a")

        // Mark 'B' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.tryReceive().isFailure).isTrue()

        // Mark 'a' as invalidated again and expect a new notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("a")

        invalidations.cancel()
    }

    @Test
    fun observeTwoTables() = runTest {
        val invalidations = tracker.createFlow("A", "B", emitInitialState = false).produceIn(this)

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("A", "B")

        // Mark 'B' and 'i' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(1, 2)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("B")

        // Mark 'a' and 'i' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 3)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("A")

        // Do a sync without any invalidation and expect no notification
        tracker.awaitRefreshAsync()
        assertThat(invalidations.tryReceive().isFailure).isTrue()

        invalidations.cancel()
    }

    @Test
    fun observeFtsTable() = runTest {
        val invalidations = tracker.createFlow("C", emitInitialState = false).produceIn(this)

        // Mark 'C' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(3)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).contains("C")

        // Mark 'a' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.tryReceive().isFailure).isTrue()

        // Mark 'a' and 'C' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 3)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).contains("C")

        invalidations.cancel()
    }

    @Test
    fun observeExternalContentFtsTable() = runTest {
        val invalidations = tracker.createFlow("d", emitInitialState = false).produceIn(this)

        // Mark 'a' as invalidated and expect a notification, 'a' is the content table of 'd'
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).contains("d")

        // Mark 'i' and 'C' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(2, 3)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.tryReceive().isFailure).isTrue()

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).contains("d")

        invalidations.cancel()
    }

    @Test
    fun observeExternalContentFtsTableAndContentTable() = runTest {
        val invalidations = tracker.createFlow("d", "a", emitInitialState = false).produceIn(this)

        // Mark 'a' as invalidated and expect a notification of both 'a' and 'd' since 'd' is
        // backed by 'a'
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("d", "a")

        // Mark 'B' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(2, 3)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.tryReceive().isFailure).isTrue()

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("d", "a")

        invalidations.cancel()
    }

    @Test
    fun observeExternalContentFatsTableAndContentTableSeparately() = runTest {
        val invalidationsForA = tracker.createFlow("a", emitInitialState = false).produceIn(this)
        val invalidationsForD = tracker.createFlow("d", emitInitialState = false).produceIn(this)

        // Mark 'a' as invalidated and expect a notification of both 'a' and 'd' since 'a' is
        // the content table for 'd'
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidationsForA.receive()).containsExactly("a")
        assertThat(invalidationsForD.receive()).containsExactly("d")

        // Remove observer 'd' which is backed by 'a', observers to 'a' should still work.
        invalidationsForD.cancel()
        testScheduler.advanceUntilIdle()

        // Mark 'a' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidationsForD.tryReceive().isClosed).isTrue()
        assertThat(invalidationsForA.receive()).containsExactly("a")

        invalidationsForA.cancel()
    }

    @Test
    fun observeView() = runTest {
        val invalidations = tracker.createFlow("E", emitInitialState = false).produceIn(this)

        // Mark 'a' and 'B' as invalidated and expect a notification, the view 'E' is backed by 'a'
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("a")

        // Mark 'B' and 'i' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(2, 3)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.tryReceive().isFailure).isTrue()

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("a")

        invalidations.cancel()
    }

    @Test
    fun multipleRefreshAsync() = runTest {
        // Validate that when multiple refresh are enqueued, that only one runs.
        tracker.refreshAsync()
        tracker.refreshAsync()
        tracker.refreshAsync()

        testScheduler.advanceUntilIdle()

        assertThat(sqliteDriver.preparedQueries.filter { it == SELECT_INVALIDATED_QUERY })
            .hasSize(1)
    }

    @Test
    fun refreshAndCloseDb() = runTest {
        // Validates that closing the database with a pending refresh is OK
        tracker.refreshAsync()
        roomDatabase.close()
    }

    @Test
    fun closeDbAndRefresh() = runTest {
        // Validates that closing the database and then somehow refreshing is OK
        roomDatabase.close()
        tracker.refreshAsync()
    }

    @Test
    fun refreshAndCloseDbWithSlowObserver() = runTest {
        // Validates that a slow observer will finish notification after database closing
        val invalidatedLatch = CompletableDeferred<Unit>()
        val invalidated = AtomicBoolean(false)
        val job =
            backgroundScope.launch {
                tracker.createFlow("a", emitInitialState = false).collect {
                    invalidatedLatch.complete(Unit)
                    assertThat(invalidated.compareAndSet(false, true)).isTrue()
                    delay(100)
                }
            }
        sqliteDriver.setInvalidatedTables(0)
        tracker.refreshAsync()
        testScheduler.advanceUntilIdle()
        invalidatedLatch.await()
        roomDatabase.close()
        assertThat(invalidated.get()).isTrue()
        job.cancel()
    }

    @Test
    fun createTriggerOnTable() = runTest {
        // Note: This tests validate triggers that are an impl (but important)
        // detail of the tracker, but in theory this is already covered by tests with observers
        val triggers = listOf("INSERT", "UPDATE", "DELETE")

        val invalidations = tracker.createFlow("a").produceIn(this)
        testScheduler.advanceUntilIdle()

        // Verifies the 'invalidated' column is reset when tracking starts
        assertThat(sqliteDriver.preparedQueries)
            .contains("INSERT OR IGNORE INTO room_table_modification_log VALUES(0, 0)")
        // Verifies triggers created for observed table
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries)
                .contains(
                    "CREATE TEMP TRIGGER IF NOT EXISTS " +
                        "`room_table_modification_trigger_a_$trigger` " +
                        "AFTER $trigger ON `a` BEGIN UPDATE " +
                        "room_table_modification_log SET invalidated = 1 WHERE table_id = 0 " +
                        "AND invalidated = 0; END"
                )
        }

        assertThat(invalidations.receive()).containsExactly("a")
        invalidations.cancel()
        testScheduler.advanceUntilIdle()
        // Sync triggers since Flow cancellation is quick and does not block to remove triggers
        tracker.awaitTriggerSync()
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries)
                .contains("DROP TRIGGER IF EXISTS `room_table_modification_trigger_a_$trigger`")
        }
    }

    @Test
    fun createTriggerOnShadowTable() = runTest {
        // Note: This tests validate triggers that are an impl (but important)
        // detail of the tracker, but in theory this is already covered by tests with observers
        val triggers = listOf("INSERT", "UPDATE", "DELETE")

        val invalidations = tracker.createFlow("C").produceIn(this)
        testScheduler.advanceUntilIdle()

        // Verifies the 'invalidated' column is reset when tracking starts
        assertThat(sqliteDriver.preparedQueries)
            .contains("INSERT OR IGNORE INTO room_table_modification_log VALUES(3, 0)")
        // Verifies that when tracking a table ('C') that has an external content table
        // that triggers are installed in the content table and not the virtual table
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries)
                .contains(
                    "CREATE TEMP TRIGGER IF NOT EXISTS " +
                        "`room_table_modification_trigger_c_content_$trigger` " +
                        "AFTER $trigger ON `c_content` BEGIN UPDATE " +
                        "room_table_modification_log SET invalidated = 1 WHERE table_id = 3 " +
                        "AND invalidated = 0; END"
                )
        }

        assertThat(invalidations.receive()).containsExactly("C")
        invalidations.cancel()
        testScheduler.advanceUntilIdle()
        // Sync triggers since Flow cancellation is quick and does not block to remove triggers
        tracker.awaitTriggerSync()
        // Validates trigger are removed when tracking stops
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries)
                .contains(
                    "DROP TRIGGER IF EXISTS `room_table_modification_trigger_c_content_$trigger`"
                )
        }
    }

    @Test
    fun createFlowWithNoExistingTable() = runTest {
        // Validate that sending a bad createFlow table name fails quickly
        assertThrows<IllegalArgumentException> {
                tracker.createFlow(tables = arrayOf("x")).singleOrNull()
            }
            .hasMessageThat()
            .isEqualTo("There is no table with name x")
    }

    @Test
    fun addAndRemoveObserver() = runTest {
        val invalidations = tracker.createFlow("a", emitInitialState = false).produceIn(this)

        // Mark 'a' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receive()).containsExactly("a")

        // Remove observer, validating tracking stops immediately
        invalidations.cancel()
        testScheduler.advanceUntilIdle()

        // Mark 'a' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidations.receiveCatching().isClosed).isTrue()
    }

    @Test
    fun flowObserver() = runTest {
        // Note: This tests validate triggers that are an impl (but important)
        // detail of the tracker, but in theory this is already covered by tests with observers
        val triggers = listOf("INSERT", "UPDATE", "DELETE")

        val flow = tracker.createFlow("a")
        testScheduler.advanceUntilIdle()

        // Validate just creating a flow will not install triggers (they are cold).
        assertThat(sqliteDriver.preparedQueries).isEmpty()

        val initialCollectLatch = Mutex(locked = true)
        val collectJob =
            backgroundScope.launch(Dispatchers.IO) {
                // Collect forever in the background, we'll cancel it soon and assert on cleanup
                flow.collect { initialCollectLatch.unlock() }
            }

        // Wait at least for one emission
        testScheduler.advanceUntilIdle()
        initialCollectLatch.withLock {}

        // Verifies triggers created for flow table
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries)
                .contains(
                    "CREATE TEMP TRIGGER IF NOT EXISTS " +
                        "`room_table_modification_trigger_a_$trigger` " +
                        "AFTER $trigger ON `a` BEGIN UPDATE " +
                        "room_table_modification_log SET invalidated = 1 WHERE table_id = 0 " +
                        "AND invalidated = 0; END"
                )
        }

        // Cancel flow collection
        collectJob.cancelAndJoin()
        // Due do quick cancellation, flows won't sync triggers immediately after marking tables
        // no longer needed to be observed, hence the need to sync() here manually. In practice
        // this is fine because new flows, observers or write operations sync triggers.
        tracker.sync()
        // Validates trigger are removed when observing stops and triggers are synced
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries)
                .contains("DROP TRIGGER IF EXISTS `room_table_modification_trigger_a_$trigger`")
        }
    }

    private fun runTest(testBody: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest {
            testBody.invoke(this)
            testScheduler.advanceUntilIdle()
            roomDatabase.close()
        }

    /**
     * Start invalidation async and await for it to be done.
     *
     * This is used as opposed so [InvalidationTracker.refresh] to validate the async things and
     * because only the sync versions expect at-least one call to the async one to flush
     * invalidation.
     */
    private fun InvalidationTracker.awaitRefreshAsync() {
        refreshAsync()
        testCoroutineScope.testScheduler.advanceUntilIdle()
    }

    private suspend fun InvalidationTracker.awaitTriggerSync() {
        sync()
        testCoroutineScope.testScheduler.advanceUntilIdle()
    }

    private inner class FakeRoomDatabase(
        private val shadowTablesMap: Map<String, String>,
        private val viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
        private val tableNames: Array<String>,
    ) : RoomDatabase() {

        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(this, shadowTablesMap, viewTables, *tableNames)
        }

        override fun createOpenDelegate(): RoomOpenDelegateMarker {
            return object : RoomOpenDelegate(0, "", "") {
                override suspend fun onCreate(connection: SQLiteConnection) {}

                override suspend fun onPreMigrate(connection: SQLiteConnection) {}

                override suspend fun onValidateSchema(connection: SQLiteConnection) =
                    ValidationResult(true, null)

                override suspend fun onPostMigrate(connection: SQLiteConnection) {}

                override suspend fun onOpen(connection: SQLiteConnection) {}

                override suspend fun createAllTables(connection: SQLiteConnection) {}

                override suspend fun dropAllTables(connection: SQLiteConnection) {}
            }
        }

        override fun clearAllTables() {}

        override fun createAutoMigrations(
            autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
        ): List<Migration> {
            return emptyList()
        }

        override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
            return emptySet()
        }

        override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
            return emptyMap()
        }
    }

    private class FakeSQLiteDriver : SQLiteDriver {

        private val invalidateTablesQueue = mutableListOf<IntArray>()

        val preparedQueries = mutableListOf<String>()

        override fun open(fileName: String): SQLiteConnection {
            return FakeSQLiteConnection()
        }

        fun setInvalidatedTables(vararg tableIds: Int) {
            invalidateTablesQueue.add(tableIds)
        }

        private inner class FakeSQLiteConnection : SQLiteConnection {

            override fun inTransaction() = false

            override fun prepare(sql: String): SQLiteStatement {
                preparedQueries.add(sql)
                val invalidatedTables =
                    if (sql == SELECT_INVALIDATED_QUERY && invalidateTablesQueue.isNotEmpty()) {
                        invalidateTablesQueue.removeFirstKt()
                    } else {
                        null
                    }
                return FakeSQLiteStatement(invalidatedTables)
            }

            override fun close() {}
        }

        private inner class FakeSQLiteStatement(private val invalidateTables: IntArray?) :
            SQLiteStatement {

            private var position = -1

            override fun bindBlob(index: Int, value: ByteArray) {}

            override fun bindDouble(index: Int, value: Double) {}

            override fun bindLong(index: Int, value: Long) {}

            override fun bindText(index: Int, value: String) {}

            override fun bindNull(index: Int) {}

            override fun getBlob(index: Int): ByteArray {
                error("Should not be called")
            }

            override fun getDouble(index: Int): Double {
                error("Should not be called")
            }

            override fun getLong(index: Int): Long {
                return if (invalidateTables != null) {
                    invalidateTables[position].toLong()
                } else {
                    0L
                }
            }

            override fun getText(index: Int): String {
                error("Should not be called")
            }

            override fun isNull(index: Int): Boolean {
                return false
            }

            override fun getColumnCount(): Int {
                return 0
            }

            override fun getColumnName(index: Int): String {
                error("Should not be called")
            }

            override fun getColumnType(index: Int): Int {
                error("Should not be called")
            }

            override fun step(): Boolean {
                if (invalidateTables != null) {
                    return ++position < invalidateTables.size
                } else {
                    return false
                }
            }

            override fun reset() {}

            override fun clearBindings() {}

            override fun close() {}
        }
    }

    companion object {
        private const val SELECT_INVALIDATED_QUERY =
            "SELECT * FROM room_table_modification_log WHERE invalidated = 1"
    }
}
