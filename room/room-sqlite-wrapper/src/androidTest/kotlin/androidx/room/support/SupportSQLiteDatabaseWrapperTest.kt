/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room.support

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteTransactionListener
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteException
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.use
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class SupportSQLiteDatabaseWrapperTest(private val driver: Driver) {

    companion object {
        @JvmStatic @Parameters(name = "driver = {0}") fun drivers() = Driver.entries.toTypedArray()
    }

    enum class Driver {
        BUNDLED,
        ANDROID,
        NONE,
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var database: TestDatabase
    private lateinit var wrapper: SupportSQLiteDatabase

    @Before
    fun setup() {
        context.deleteDatabase("test.db")

        database =
            Room.databaseBuilder(context, TestDatabase::class.java, "test.db")
                .setQueryCoroutineContext(Dispatchers.IO)
                .apply {
                    when (driver) {
                        Driver.BUNDLED -> BundledSQLiteDriver()
                        Driver.ANDROID -> AndroidSQLiteDriver()
                        Driver.NONE -> null
                    }?.let { setDriver(it) }
                }
                .build()
        wrapper = database.getSupportWrapper()
    }

    @After
    fun tearDown() {
        database.close()
        wrapper.close()
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun dao(): TestDao
    }

    @Dao
    interface TestDao {

        @Query("SELECT * FROM TestEntity") fun getEntities(): List<TestEntity>

        @Insert fun insert(entity: TestEntity)

        @Transaction
        fun insertAndReturn(entity: TestEntity): List<TestEntity> {
            insert(entity)
            return getEntities()
        }
    }

    @Entity data class TestEntity(@PrimaryKey val id: Long)

    @Test
    fun query_string() {
        val testEntities = List(10) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        val resultEntities =
            wrapper.query("SELECT * FROM TestEntity").use {
                buildList {
                    while (it.moveToNext()) {
                        add(TestEntity(it.getLong(0)))
                    }
                }
            }

        assertThat(resultEntities).containsExactlyElementsIn(testEntities)
    }

    @Test
    fun query_cursor() {
        wrapper.execSQL(
            """
            CREATE TABLE Test(
                integerCol_long INTEGER,
                realCol_double REAL,
                textCol TEXT,
                blobCol BLOB,
                nullCol TEXT
            )
            """
                .trimIndent()
        )

        wrapper
            .compileStatement(
                """
                INSERT INTO Test (
                    integerCol_long,
                    realCol_double,
                    textCol,
                    blobCol,
                    nullCol
                ) VALUES (?, ?, ?, ?, ?)
            """
                    .trimIndent()
            )
            .use {
                it.bindLong(1, 3L)
                it.bindDouble(2, 7.87)
                it.bindString(3, "PR")
                it.bindBlob(4, byteArrayOf(0x0F, 0x12, 0x1B))
                it.bindNull(5)
                it.execute()
            }

        wrapper.query("SELECT * FROM Test").use {
            assertThat(it.moveToNext()).isTrue()
            assertThat(it.columnCount).isEqualTo(5)
            assertThat(it.columnNames)
                .asList()
                .containsExactly(
                    "integerCol_long",
                    "realCol_double",
                    "textCol",
                    "blobCol",
                    "nullCol",
                )
            assertThat(it.getColumnName(0)).isEqualTo("integerCol_long")
            assertThat(it.getColumnName(1)).isEqualTo("realCol_double")
            assertThat(it.getColumnName(2)).isEqualTo("textCol")
            assertThat(it.getColumnName(3)).isEqualTo("blobCol")
            assertThat(it.getColumnName(4)).isEqualTo("nullCol")
            assertThat(it.getType(0)).isEqualTo(SQLITE_DATA_INTEGER)
            assertThat(it.getType(1)).isEqualTo(SQLITE_DATA_FLOAT)
            assertThat(it.getType(2)).isEqualTo(SQLITE_DATA_TEXT)
            assertThat(it.getType(3)).isEqualTo(SQLITE_DATA_BLOB)
            assertThat(it.getLong(0)).isEqualTo(3L)
            assertThat(it.getDouble(1)).isEqualTo(7.87)
            assertThat(it.getString(2)).isEqualTo("PR")
            assertThat(it.getBlob(3)).isEqualTo(byteArrayOf(0x0F, 0x12, 0x1B))
            assertThat(it.isNull(4)).isTrue()
        }
    }

    @Test
    fun query_cursor_compositeType() {
        wrapper.query("SELECT 1 || 'abc'").use {
            assertThat(it.moveToNext()).isTrue()
            assertThat(it.getType(0)).isEqualTo(SQLITE_DATA_TEXT)
            assertThat(it.getString(0)).isEqualTo("1abc")
        }
    }

    @Test
    fun query_stringWithArgs() {
        database.dao().insert(TestEntity(2))

        val resultEntities =
            wrapper.query("SELECT * FROM TestEntity WHERE id = ?", arrayOf(2)).use {
                buildList {
                    while (it.moveToNext()) {
                        add(TestEntity(it.getLong(0)))
                    }
                }
            }

        assertThat(resultEntities).containsExactly(TestEntity(2))
    }

    @Test
    fun query_simpleSQLiteQuery() {
        val testEntities = List(10) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        val query = SimpleSQLiteQuery("SELECT * FROM TestEntity")
        val resultEntities =
            wrapper.query(query).use {
                buildList {
                    while (it.moveToNext()) {
                        add(TestEntity(it.getLong(0)))
                    }
                }
            }

        assertThat(resultEntities).containsExactlyElementsIn(testEntities)
    }

    @Test
    fun query_simpleSQLiteQueryWithArgs() {
        database.dao().insert(TestEntity(2))

        val query = SimpleSQLiteQuery("SELECT * FROM TestEntity WHERE id = ?", arrayOf(2))
        val resultEntities =
            wrapper.query(query).use {
                buildList {
                    while (it.moveToNext()) {
                        add(TestEntity(it.getLong(0)))
                    }
                }
            }

        assertThat(resultEntities).containsExactly(TestEntity(2))
    }

    @Test
    fun insert() {
        val values = ContentValues().apply { put("id", 1) }
        val resultOne =
            wrapper.insert(
                table = "TestEntity",
                conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE,
                values = values,
            )
        assertThat(resultOne).isEqualTo(1)
        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))

        val resultTwo =
            wrapper.insert(
                table = "TestEntity",
                conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
                values = values,
            )
        assertThat(resultTwo).isEqualTo(-1)
        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))
    }

    @Test
    fun delete() {
        val testEntities = List(5) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        val resultOne =
            wrapper.delete(table = "TestEntity", whereClause = "id = ?", whereArgs = arrayOf("0"))
        assertThat(resultOne).isEqualTo(1)
        assertThat(database.dao().getEntities()).containsExactlyElementsIn(testEntities.drop(1))

        val resultTwo = wrapper.delete(table = "TestEntity", whereClause = null, whereArgs = null)
        assertThat(resultTwo).isEqualTo(4)
        assertThat(database.dao().getEntities()).isEmpty()
    }

    @Test
    fun update() {
        val testEntities = List(5) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        val values = ContentValues().apply { put("id", 10) }
        val resultOne =
            wrapper.update(
                table = "TestEntity",
                conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE,
                values = values,
                whereClause = "id = ?",
                whereArgs = arrayOf("1"),
            )
        assertThat(resultOne).isEqualTo(1)
        assertThat(database.dao().getEntities()).contains(TestEntity(10))

        val resultTwo =
            wrapper.update(
                table = "TestEntity",
                conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
                values = values,
                whereClause = "id = ?",
                whereArgs = arrayOf("2"),
            )
        assertThat(resultTwo).isEqualTo(0)
    }

    @Test
    fun statement_execute() {
        wrapper.compileStatement("INSERT INTO TestEntity VALUES (1)").use { it.execute() }
        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))

        wrapper.compileStatement("UPDATE TestEntity SET id = 2").use { it.execute() }
        assertThat(database.dao().getEntities()).containsExactly(TestEntity(2))

        wrapper.compileStatement("DELETE FROM TestEntity").use { it.execute() }
        assertThat(database.dao().getEntities()).isEmpty()
    }

    @Test
    fun statement_executeUpdateDelete() {
        val testEntities = List(10) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        val updateResult =
            wrapper.compileStatement("UPDATE TestEntity SET id = 20 WHERE id = 1").use {
                it.executeUpdateDelete()
            }
        assertThat(updateResult).isEqualTo(1)

        val noUpdateResult =
            wrapper.compileStatement("UPDATE TestEntity SET id = 20 WHERE id = 1").use {
                it.executeUpdateDelete()
            }
        assertThat(noUpdateResult).isEqualTo(0)

        val deleteResult =
            wrapper.compileStatement("DELETE FROM TestEntity").use { it.executeUpdateDelete() }
        assertThat(deleteResult).isEqualTo(10)

        val noDeleteResult =
            wrapper.compileStatement("DELETE FROM TestEntity").use { it.executeUpdateDelete() }
        assertThat(noDeleteResult).isEqualTo(0)
    }

    @Test
    fun statement_executeInsert() {
        val rowId =
            wrapper.compileStatement("INSERT INTO TestEntity (id) VALUES (1)").use {
                it.executeInsert()
            }
        assertThat(rowId).isEqualTo(1)

        val noRowId =
            wrapper.compileStatement("INSERT OR IGNORE INTO TestEntity (id) VALUES (1)").use {
                it.executeInsert()
            }
        assertThat(noRowId).isEqualTo(-1)
    }

    @Test
    fun statement_reused() {
        wrapper.execSQL("CREATE TABLE TestTable (id INTEGER, data TEXT)")

        val stmt = wrapper.compileStatement("INSERT INTO TestTable VALUES (?, ?)")
        stmt.bindLong(1, 1)
        stmt.bindString(2, "Tom")
        stmt.execute()

        stmt.clearBindings()
        stmt.bindLong(1, 2)
        stmt.bindString(2, "Pelusa")
        stmt.execute()

        stmt.close()

        val names =
            wrapper.query("SELECT * FROM TestTable").use {
                buildList {
                    while (it.moveToNext()) {
                        add(it.getString(1))
                    }
                }
            }
        assertThat(names).containsExactly("Tom", "Pelusa")
    }

    @Test
    fun statement_closed() {
        val stmt = wrapper.compileStatement("INSERT INTO TestEntity (id) VALUES (?)")
        stmt.close()

        assertThrows<IllegalStateException> { stmt.execute() }
        assertThrows<IllegalStateException> { stmt.executeInsert() }
    }

    @Test
    fun statement_simpleQueryForLong() {
        val result = wrapper.compileStatement("SELECT 20 AS result").use { it.simpleQueryForLong() }
        assertThat(result).isEqualTo(20)
    }

    @Test
    fun statement_simpleQueryForString() {
        val result =
            wrapper.compileStatement("SELECT 'Tom' as result").use { it.simpleQueryForString() }
        assertThat(result).isEqualTo("Tom")
    }

    @Test
    fun commitTransaction() {
        wrapper.beginTransaction()
        wrapper.execSQL("INSERT INTO TestEntity VALUES (1)")
        wrapper.setTransactionSuccessful()
        wrapper.endTransaction()

        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))
    }

    @Test
    fun commitTransaction_compileStatement() {
        wrapper.beginTransaction()
        wrapper.compileStatement("INSERT INTO TestEntity VALUES (1)").use { it.execute() }
        wrapper.setTransactionSuccessful()
        wrapper.endTransaction()

        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))
    }

    @Test
    fun commitTransaction_withListener() {
        val listener = TestTransactionListener()
        wrapper.beginTransactionWithListener(listener)

        assertThat(listener.onBeginCalled).isTrue()
        assertThat(listener.onCommitCalled).isFalse()
        assertThat(listener.onRollbackCalled).isFalse()

        wrapper.setTransactionSuccessful()
        wrapper.endTransaction()

        assertThat(listener.onBeginCalled).isTrue()
        assertThat(listener.onCommitCalled).isTrue()
        assertThat(listener.onRollbackCalled).isFalse()
    }

    @Test
    fun commitNestedTransaction() {
        wrapper.beginTransaction()
        try {
            wrapper.execSQL("INSERT INTO TestEntity VALUES (1)")
            wrapper.beginTransaction()
            try {
                wrapper.execSQL("INSERT INTO TestEntity VALUES (2)")
                wrapper.setTransactionSuccessful()
            } finally {
                wrapper.endTransaction()
            }
            wrapper.setTransactionSuccessful()
        } finally {
            wrapper.endTransaction()
        }

        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1), TestEntity(2))
    }

    @Test
    fun rollbackTransaction() {
        wrapper.beginTransaction()
        wrapper.execSQL("INSERT INTO TestEntity VALUES (1)")
        wrapper.endTransaction()

        assertThat(database.dao().getEntities()).isEmpty()
    }

    @Test
    fun rollbackNestedTransaction() {
        wrapper.beginTransaction()
        try {
            wrapper.execSQL("INSERT INTO TestEntity VALUES (1)")
            wrapper.beginTransaction()
            try {
                wrapper.execSQL("INSERT INTO TestEntity VALUES (2)")
            } finally {
                wrapper.endTransaction()
            }
            wrapper.setTransactionSuccessful()
        } finally {
            wrapper.endTransaction()
        }

        assertThat(database.dao().getEntities()).isEmpty()
    }

    @Test
    fun rollbackTransaction_error() {
        assertThrows<SQLiteException> {
                wrapper.beginTransaction()
                try {
                    wrapper.execSQL("INSERT INTO TestEntity VALUES (1)")
                    wrapper.execSQL("INSERT INTO TestEntity VALUES (1)") // primary key violation
                    wrapper.setTransactionSuccessful()
                } finally {
                    wrapper.endTransaction()
                }
            }
            .hasMessageThat()
            .contains("UNIQUE constraint failed")

        assertThat(database.dao().getEntities()).isEmpty()
    }

    @Test
    fun rollbackTransaction_withListener() {
        val listener = TestTransactionListener()
        wrapper.beginTransactionWithListener(listener)

        assertThat(listener.onBeginCalled).isTrue()
        assertThat(listener.onCommitCalled).isFalse()
        assertThat(listener.onRollbackCalled).isFalse()

        wrapper.endTransaction()

        assertThat(listener.onBeginCalled).isTrue()
        assertThat(listener.onCommitCalled).isFalse()
        assertThat(listener.onRollbackCalled).isTrue()
    }

    @Test
    fun inTransaction() {
        assertThat(wrapper.inTransaction()).isFalse()
        wrapper.beginTransaction()
        assertThat(wrapper.inTransaction()).isTrue()
        wrapper.endTransaction()
        assertThat(wrapper.inTransaction()).isFalse()
    }

    @Test
    @LargeTest
    fun yieldIfContendedSafely() {
        wrapper.beginTransaction()
        // yield but since there is no contention, expect a false result
        assertThat(wrapper.yieldIfContendedSafely()).isFalse()

        // insert some initial data, it should be visible to other if the transaction yields
        wrapper.execSQL("INSERT INTO TestEntity VALUES (1)")
        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))

        // On another thread, begin a transaction causing a contention. When the test thread
        // yields, the second thread begin its own transaction and any changes done by the test
        // thread should be visible, then this second thread finishes once test thread should
        // continue.
        val barrier = CountDownLatch(1)
        val waiter = thread {
            barrier.countDown()
            wrapper.beginTransaction()
            assertThat(database.dao().getEntities()).containsExactly(TestEntity(1))
            wrapper.execSQL("INSERT INTO TestEntity VALUES (2)")
            assertThat(database.dao().getEntities()).containsExactly(TestEntity(1), TestEntity(2))
            wrapper.setTransactionSuccessful()
            wrapper.endTransaction()
        }

        // wait for waiter thread to start
        barrier.await()
        // the barrier can only get us so far, just before we actually invoke begin(), so we sleep a
        // bit such that begin() actually gets blocked attempting to start a transaction, otherwise
        // we'll yield too early when there is no contention
        @Suppress("BanThreadSleep") // got no other choice
        Thread.sleep(200)

        // yield and since there is a contention, expect a true result
        assertThat(wrapper.yieldIfContendedSafely()).isTrue()
        // The second thread finished, assert we can see its changes
        assertThat(database.dao().getEntities()).containsExactly(TestEntity(1), TestEntity(2))

        // finish the transaction that yielded
        wrapper.execSQL("INSERT INTO TestEntity VALUES (3)")
        wrapper.setTransactionSuccessful()
        wrapper.endTransaction()

        assertThat(database.dao().getEntities())
            .containsExactly(TestEntity(1), TestEntity(2), TestEntity(3))

        waiter.join()
    }

    @Test
    fun version() {
        assertThat(wrapper.version).isEqualTo(1)

        wrapper.version = 5
        assertThat(wrapper.version).isEqualTo(5)

        assertThat(wrapper.needUpgrade(10)).isTrue()
    }

    @Test
    fun databaseSize() {
        // create a new wrapper whose database journal is TRUNCATE since in WAL mode the size
        // control is not as precise
        val wrapper =
            RoomSupportSQLiteDatabase(
                Room.databaseBuilder(context, TestDatabase::class.java, "size_test.db")
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .setDriver(BundledSQLiteDriver())
                    .build()
            )

        wrapper.pageSize = 1024L
        wrapper.execSQL("VACUUM") // need to vacuum for new page size to take effect
        assertThat(wrapper.pageSize).isEqualTo(1024L)

        val newMaxSize = wrapper.setMaximumSize(4000)
        assertThat(newMaxSize).isEqualTo(4096L)
        assertThat(wrapper.maximumSize).isEqualTo(4096L)

        wrapper.close()
        context.deleteDatabase("size_test.db")
    }

    @Test
    fun isReadOnly() {
        // a Room database is never really read only
        assertThat(wrapper.isReadOnly).isFalse()
    }

    @Test
    fun path() {
        val expected = context.getDatabasePath("test.db").path
        assertThat(wrapper.path).isEqualTo(expected)
    }

    @Test
    fun writeAheadLogging() {
        if (driver != Driver.BUNDLED) {
            throw AssumptionViolatedException(
                "Only testing bundled where journal mode change is disabled."
            )
        }

        assertThat(wrapper.isWriteAheadLoggingEnabled).isTrue()

        assertThrows<UnsupportedOperationException> { wrapper.enableWriteAheadLogging() }
            .hasMessageThat()
            .isEqualTo(
                "Modifying journal mode is not supported by the wrapper, please configure journal " +
                    "via Room's database builder."
            )

        assertThrows<UnsupportedOperationException> { wrapper.disableWriteAheadLogging() }
            .hasMessageThat()
            .isEqualTo(
                "Modifying journal mode is not supported by the wrapper, please configure journal " +
                    "via Room's database builder."
            )
    }

    @Test
    fun attachedDatabases() {
        val attachedDbs = wrapper.attachedDbs
        assertThat(attachedDbs).isNotNull()
        val (name, path) = attachedDbs!!.first().let { it.first to it.second }
        assertThat(name).isEqualTo("main")
        assertThat(path).endsWith("databases/test.db")
    }

    @Test
    fun databaseIntegrityOk() {
        assertThat(wrapper.isDatabaseIntegrityOk).isTrue()
    }

    /** Test that a read transaction is not blocked by a write transaction. */
    @Test
    fun readTransactionNotBlockedByWrite() {
        if (driver == Driver.ANDROID) {
            // TODO(b/288918056): Use Android V API for DEFERRED once it is available
            throw AssumptionViolatedException("Android driver does not support read transactions")
        }
        val testEntities = List(10) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        wrapper.beginTransactionNonExclusive()
        val t = thread {
            wrapper.beginTransactionReadOnly()
            val resultEntities =
                wrapper.query("SELECT * FROM TestEntity").use {
                    buildList {
                        while (it.moveToNext()) {
                            add(TestEntity(it.getLong(0)))
                        }
                    }
                }

            assertThat(resultEntities).containsAtLeastElementsIn(testEntities)
            wrapper.endTransaction()
        }
        t.join()
        wrapper.endTransaction()
    }

    /** Test that a wrapper transaction can include blocking DAO write operations. */
    @Test
    fun combineTransactionWithDao_write() {
        wrapper.beginTransaction()
        try {
            database.dao().insert(TestEntity(1))
            wrapper.setTransactionSuccessful()
        } finally {
            wrapper.endTransaction()
        }

        val resultEntities = database.dao().getEntities()
        assertThat(resultEntities).containsExactly(TestEntity(1))
    }

    /** Test that a wrapper transaction can include blocking DAO read operations. */
    @Test
    fun combineTransactionWithDao_read() {
        val testEntities = List(10) { TestEntity(it.toLong()) }
        testEntities.forEach { database.dao().insert(it) }

        wrapper.beginTransaction()
        try {
            val resultEntities = database.dao().getEntities()
            assertThat(resultEntities).containsAtLeastElementsIn(testEntities)
            wrapper.setTransactionSuccessful()
        } finally {
            wrapper.endTransaction()
        }
    }

    @Test
    fun combineTransactionWithDao_transaction() {
        wrapper.beginTransaction()
        try {
            assertThat(database.dao().insertAndReturn(TestEntity(1))).containsExactly(TestEntity(1))
            wrapper.setTransactionSuccessful()
        } finally {
            wrapper.endTransaction()
        }

        val resultEntities = database.dao().getEntities()
        assertThat(resultEntities).containsExactly(TestEntity(1))
    }

    @Test
    fun cachedWrapper() {
        if (driver != Driver.BUNDLED) {
            throw AssumptionViolatedException("Only testing bundled where wrapper is cached")
        }
        val wrapper1 = database.getSupportWrapper()
        val wrapper2 = database.getSupportWrapper()
        assertThat(wrapper1).isSameInstanceAs(wrapper2)
    }

    private class TestTransactionListener : SQLiteTransactionListener {
        var onBeginCalled = false
        var onCommitCalled = false
        var onRollbackCalled = false

        override fun onBegin() {
            onBeginCalled = true
        }

        override fun onCommit() {
            onCommitCalled = true
        }

        override fun onRollback() {
            onRollbackCalled = true
        }
    }
}
