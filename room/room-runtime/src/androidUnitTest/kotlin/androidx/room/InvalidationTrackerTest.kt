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
package androidx.room

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class InvalidationTrackerTest {

    private lateinit var tracker: InvalidationTracker
    private lateinit var sqliteDriver: FakeSQLiteDriver
    private lateinit var roomDatabase: FakeRoomDatabase

    @get:Rule
    val taskExecutorRule = CountingTaskExecutorRule()

    @Before
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setup() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        val shadowTables = buildMap {
            put("C", "C_content")
            put("d", "a")
        }
        val viewTables = buildMap {
            put("e", setOf("a"))
        }
        val tableNames = arrayOf("a", "B", "i", "C", "d")
        sqliteDriver = FakeSQLiteDriver()
        roomDatabase = FakeRoomDatabase(
            shadowTables,
            viewTables,
            tableNames
        )
        roomDatabase.init(
            DatabaseConfiguration(
                context = mock(),
                name = null,
                sqliteOpenHelperFactory = null,
                migrationContainer = RoomDatabase.MigrationContainer(),
                callbacks = null,
                allowMainThreadQueries = true,
                journalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
                queryExecutor = ArchTaskExecutor.getIOThreadExecutor(),
                transactionExecutor = ArchTaskExecutor.getIOThreadExecutor(),
                multiInstanceInvalidationServiceIntent = null,
                requireMigration = true,
                allowDestructiveMigrationOnDowngrade = false,
                migrationNotRequiredFrom = null,
                copyFromAssetPath = null,
                copyFromFile = null,
                copyFromInputStream = null,
                prepackagedDatabaseCallback = null,
                typeConverters = emptyList(),
                autoMigrationSpecs = emptyList(),
                allowDestructiveMigrationForAllTables = false,
                sqliteDriver = sqliteDriver,
                queryCoroutineContext = null,
            )
        )
        tracker = roomDatabase.invalidationTracker
    }

    @After
    fun after() {
        Locale.setDefault(Locale.US)

        taskExecutorRule.drainTasks(1, TimeUnit.SECONDS)
        assertThat(taskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun observerWithNoExistingTable() = runTest {
        assertFailsWith<IllegalArgumentException>(message = "There is no table with name x") {
            val observer: InvalidationTracker.Observer = LatchObserver(1, "x")
            tracker.subscribe(observer)
        }
    }

    @Test
    fun ignoreCaseInTableNames() {
        val observer = LatchObserver(1, "I")
        tracker.addObserver(observer)
    }

    @Test
    fun observeOneTable() = runTest {
        val observer = LatchObserver(1, "a")
        tracker.subscribe(observer)

        // Mark 'a' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).containsExactly("a")

        // Mark 'B' as invalidated and expect no notification
        observer.reset(1)
        sqliteDriver.setInvalidatedTables(1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(false)

        // Mark 'a' as invalidated again and expect a new notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).containsExactly("a")
    }

    @Test
    fun observeTwoTables() = runTest {
        val observer = LatchObserver(1, "A", "B")
        tracker.subscribe(observer)

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(2)
        assertThat(observer.invalidatedTables).containsExactly("A", "B")

        // Mark 'B' and 'i' as invalidated and expect a notification
        observer.reset(1)
        sqliteDriver.setInvalidatedTables(1, 2)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).containsExactly("B")

        // Mark 'a' and 'i' as invalidated and expect a notification
        observer.reset(1)
        sqliteDriver.setInvalidatedTables(0, 3)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).containsExactly("A")

        // Do a sync without any invalidation and expect no notification
        observer.reset(1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(false)
    }

    @Test
    fun observeFtsTable() = runTest {
        val observer = LatchObserver(1, "C")
        tracker.subscribe(observer)

        // Mark 'C' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(3)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("C")

        // Mark 'a' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(1)
        observer.reset(1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(false)

        // Mark 'a' and 'C' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 3)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("C")
    }

    @Test
    fun observeExternalContentFtsTable() = runTest {
        val observer = LatchObserver(1, "d")
        tracker.subscribe(observer)

        // Mark 'a' as invalidated and expect a notification, 'a' is the content table of 'd'
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("d")

        // Mark 'i' and 'C' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(2, 3)
        observer.reset(1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(false)

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("d")
    }

    @Test
    fun observeExternalContentFtsTableAndContentTable() = runTest {
        val observer = LatchObserver(1, "d", "a")
        tracker.subscribe(observer)

        // Mark 'a' as invalidated and expect a notification of both 'a' and 'd' since 'd' is
        // backed by 'a'
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(2)
        assertThat(observer.invalidatedTables).containsAtLeast("d", "a")

        // Mark 'B' as invalidated and expect no notification
        observer.reset(1)
        sqliteDriver.setInvalidatedTables(2, 3)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(false)

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(2)
        assertThat(observer.invalidatedTables).containsAtLeast("d", "a")
    }

    @Test
    fun observeExternalContentFatsTableAndContentTableSeparately() = runTest {
        val observerA = LatchObserver(1, "a")
        val observerD = LatchObserver(1, "d")
        tracker.subscribe(observerA)
        tracker.subscribe(observerD)

        // Mark 'a' as invalidated and expect a notification of both 'a' and 'd' since 'a' is
        // the content table for 'd'
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observerA.await()).isEqualTo(true)
        assertThat(observerD.await()).isEqualTo(true)
        assertThat(observerA.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observerD.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observerA.invalidatedTables).contains("a")
        assertThat(observerD.invalidatedTables).contains("d")

        // Remove observer 'd' which is backed by 'a', observers to 'a' should still work.
        tracker.removeObserver(observerD)
        observerA.reset(1)
        observerD.reset(1)
        // Mark 'a' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observerA.await()).isEqualTo(true)
        assertThat(observerD.await()).isEqualTo(false)
        assertThat(observerA.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observerA.invalidatedTables).contains("a")
    }

    @Test
    fun observeView() = runTest {
        val observer = LatchObserver(1, "E")
        tracker.subscribe(observer)

        // Mark 'a' and 'B' as invalidated and expect a notification, the view 'E' is backed by 'a'
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("a")

        // Mark 'B' and 'i' as invalidated and expect no notification
        observer.reset(1)
        sqliteDriver.setInvalidatedTables(2, 3)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(false)

        // Mark 'a' and 'B' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0, 1)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("a")
    }

    @Test
    fun multipleRefreshAsync() = runTest {
        // Validate that when multiple refresh are enqueued, that only one runs.
        tracker.refreshAsync()
        tracker.refreshAsync()
        tracker.refreshAsync()

        taskExecutorRule.drainTasks(1, TimeUnit.SECONDS)

        assertThat(
            sqliteDriver.preparedQueries.filter {
                it == SELECT_INVALIDATED_QUERY
            }
        ).hasSize(1)
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
        val invalidatedLatch = CountDownLatch(1)
        val invalidated = atomic(false)
        tracker.addObserver(object : InvalidationTracker.Observer("a") {
            override fun onInvalidated(tables: Set<String>) {
                invalidatedLatch.countDown()
                assertThat(invalidated.compareAndSet(expect = false, update = true)).isTrue()
                runBlocking { delay(100) }
            }
        })
        sqliteDriver.setInvalidatedTables(0)
        tracker.refreshAsync()
        taskExecutorRule.drainTasks(200, TimeUnit.MILLISECONDS)
        invalidatedLatch.await()
        roomDatabase.close()
        assertThat(invalidated.value).isTrue()
    }

    @Test
    fun createTriggerOnTable() = runTest {
        // Note: This tests validate triggers that are an impl (but important)
        // detail of the tracker, but in theory this is already covered by tests with observers
        val triggers = listOf("INSERT", "UPDATE", "DELETE")

        val observer = LatchObserver(1, "a")
        tracker.subscribe(observer)
        tracker.sync()

        // Verifies the 'invalidated' column is reset when tracking starts
        assertThat(sqliteDriver.preparedQueries).contains(
            "INSERT OR IGNORE INTO room_table_modification_log VALUES(0, 0)"
        )
        // Verifies triggers created for observed table
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries).contains(
                "CREATE TEMP TRIGGER IF NOT EXISTS " +
                    "`room_table_modification_trigger_a_$trigger` " +
                    "AFTER $trigger ON `a` BEGIN UPDATE " +
                    "room_table_modification_log SET invalidated = 1 WHERE table_id = 0 " +
                    "AND invalidated = 0; END"
            )
        }

        tracker.unsubscribe(observer)
        tracker.sync()
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries).contains(
                "DROP TRIGGER IF EXISTS `room_table_modification_trigger_a_$trigger`"
            )
        }
    }

    @Test
    fun createTriggerOnShadowTable() = runTest {
        // Note: This tests validate triggers that are an impl (but important)
        // detail of the tracker, but in theory this is already covered by tests with observers
        val triggers = listOf("INSERT", "UPDATE", "DELETE")

        val observer = LatchObserver(1, "C")
        tracker.subscribe(observer)
        tracker.sync()

        // Verifies the 'invalidated' column is reset when tracking starts
        assertThat(sqliteDriver.preparedQueries).contains(
            "INSERT OR IGNORE INTO room_table_modification_log VALUES(3, 0)"
        )
        // Verifies that when tracking a table ('C') that has an external content table
        // that triggers are installed in the content table and not the virtual table
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries).contains(
                "CREATE TEMP TRIGGER IF NOT EXISTS " +
                    "`room_table_modification_trigger_c_content_$trigger` " +
                    "AFTER $trigger ON `c_content` BEGIN UPDATE " +
                    "room_table_modification_log SET invalidated = 1 WHERE table_id = 3 " +
                    "AND invalidated = 0; END"
            )
        }

        tracker.unsubscribe(observer)
        tracker.sync()
        // Validates trigger are removed when tracking stops
        triggers.forEach { trigger ->
            assertThat(sqliteDriver.preparedQueries).contains(
                "DROP TRIGGER IF EXISTS `room_table_modification_trigger_c_content_$trigger`"
            )
        }
    }

    @Test
    fun createLiveDataWithNoExistingTable() {
        // Validate that sending a bad createLiveData table name fails quickly
        assertFailsWith<IllegalArgumentException>(message = "There is no table with name x") {
            tracker.createLiveData(
                tableNames = arrayOf("x"),
                inTransaction = false
            ) {}
        }
    }

    @Test
    fun addAndRemoveObserver() = runTest {
        val observer = LatchObserver(1, "a")
        tracker.addObserver(observer)

        // Mark 'a' as invalidated and expect a notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).containsExactly("a")

        // Remove observer, validating tracking stops immediately
        tracker.removeObserver(observer)

        // Mark 'a' as invalidated and expect no notification
        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).containsExactly("a")
    }

    @Test
    fun weakObserver() {
        val invalidated = atomic(0)
        var observer: InvalidationTracker.Observer? = object : InvalidationTracker.Observer("a") {
            override fun onInvalidated(tables: Set<String>) {
                invalidated.incrementAndGet()
            }
        }
        tracker.addWeakObserver(observer!!)

        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidated.value).isEqualTo(1)

        // Attempt to perform garbage collection in a loop so that weak observer is discarded
        // and it stops receiving invalidation notifications. If GC fails to collect the observer
        // the test result is ignored.
        runBlocking {
            try {
                val weakRef = WeakReference(observer)
                observer = null
                withTimeout(TimeUnit.SECONDS.toMillis(2)) {
                    while (true) {
                        System.gc()
                        if (weakRef.get() == null) {
                            break
                        }
                        delay(10)
                    }
                }
            } catch (ex: TimeoutCancellationException) {
                throw AssumptionViolatedException(
                    "Test was flaky due to involving garbage collector loop."
                )
            }
        }

        sqliteDriver.setInvalidatedTables(0)
        tracker.awaitRefreshAsync()
        assertThat(invalidated.value).isEqualTo(1)
    }

    private fun InvalidationTracker.awaitRefreshAsync() {
        refreshAsync()
        taskExecutorRule.drainTasks(200, TimeUnit.MILLISECONDS)
    }

    private class LatchObserver(
        count: Int,
        vararg tableNames: String
    ) : InvalidationTracker.Observer(arrayOf(*tableNames)) {
        private var latch: CountDownLatch

        var invalidatedTables: Set<String>? = null
            private set

        init {
            latch = CountDownLatch(count)
        }

        fun await(): Boolean {
            return latch.await(200, TimeUnit.MILLISECONDS)
        }

        override fun onInvalidated(tables: Set<String>) {
            invalidatedTables = tables
            latch.countDown()
        }

        fun reset(count: Int) {
            invalidatedTables = null
            latch = CountDownLatch(count)
        }
    }

    private inner class FakeRoomDatabase(
        private val shadowTablesMap: Map<String, String>,
        private val viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
        private val tableNames: Array<String>
    ) : RoomDatabase() {

        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(
                this,
                shadowTablesMap,
                viewTables,
                *tableNames
            )
        }

        override fun createOpenDelegate(): RoomOpenDelegateMarker {
            return object : RoomOpenDelegate(0, "", "") {
                override fun onCreate(connection: SQLiteConnection) {}
                override fun onPreMigrate(connection: SQLiteConnection) {}
                override fun onValidateSchema(connection: SQLiteConnection) =
                    ValidationResult(true, null)
                override fun onPostMigrate(connection: SQLiteConnection) {}
                override fun onOpen(connection: SQLiteConnection) {}
                override fun createAllTables(connection: SQLiteConnection) {}
                override fun dropAllTables(connection: SQLiteConnection) {}
            }
        }

        override fun clearAllTables() {}
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

            override fun prepare(sql: String): SQLiteStatement {
                preparedQueries.add(sql)
                val invalidatedTables =
                    if (sql == SELECT_INVALIDATED_QUERY && invalidateTablesQueue.isNotEmpty()) {
                        invalidateTablesQueue.removeFirst()
                    } else {
                        null
                    }
                return FakeSQLiteStatement(invalidatedTables)
            }

            override fun close() {
            }
        }

        private inner class FakeSQLiteStatement(
            private val invalidateTables: IntArray?
        ) : SQLiteStatement {

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
