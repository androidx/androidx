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

import android.database.Cursor
import android.database.sqlite.SQLiteException
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.JunitTaskExecutorRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.assertFailsWith
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer

@RunWith(JUnit4::class)
class InvalidationTrackerTest {
    private lateinit var mTracker: InvalidationTracker

    private val mRoomDatabase: RoomDatabase = mock()

    private val mSqliteDb: SupportSQLiteDatabase = mock()

    private val mOpenHelper: SupportSQLiteOpenHelper = mock()

    @get:Rule
    var mTaskExecutorRule = JunitTaskExecutorRule(1, true)

    @Before
    fun setup() {
        val statement: SupportSQLiteStatement = mock()
        doReturn(statement).whenever(mSqliteDb)
            .compileStatement(eq(InvalidationTracker.RESET_UPDATED_TABLES_SQL))
        doReturn(mSqliteDb).whenever(mOpenHelper).writableDatabase
        doReturn(true).whenever(mRoomDatabase).isOpenInternal
        doReturn(ArchTaskExecutor.getIOThreadExecutor()).whenever(mRoomDatabase).queryExecutor
        val closeLock = ReentrantLock()
        doReturn(closeLock).whenever(mRoomDatabase).getCloseLock()
        doReturn(mOpenHelper).whenever(mRoomDatabase).openHelper
        val shadowTables = HashMap<String, String>()
        shadowTables["C"] = "C_content"
        shadowTables["d"] = "a"
        val viewTables = HashMap<String, Set<String>>()
        val tableSet = HashSet<String>()
        tableSet.add("a")
        viewTables["e"] = tableSet
        mTracker = InvalidationTracker(
            mRoomDatabase, shadowTables, viewTables,
            "a", "B", "i", "C", "d"
        )
        mTracker.internalInit(mSqliteDb)
        reset(mSqliteDb)
    }

    @Before
    fun setLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
    }

    @After
    fun unsetLocale() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun tableIds() {
        assertThat(mTracker.tableIdLookup.size).isEqualTo(5)
        assertThat(mTracker.tableIdLookup["a"]).isEqualTo(0)
        assertThat(mTracker.tableIdLookup["b"]).isEqualTo(1)
        assertThat(mTracker.tableIdLookup["i"]).isEqualTo(2)
        assertThat(mTracker.tableIdLookup["c"]).isEqualTo(3) // fts
        assertThat(mTracker.tableIdLookup["d"]).isEqualTo(0) // external content fts
    }

    @Test
    fun tableNames() {
        assertThat(mTracker.tablesNames.size).isEqualTo(5)
        assertThat(mTracker.tablesNames[0]).isEqualTo("a")
        assertThat(mTracker.tablesNames[1]).isEqualTo("b")
        assertThat(mTracker.tablesNames[2]).isEqualTo("i")
        assertThat(mTracker.tablesNames[3]).isEqualTo("c_content") // fts
        assertThat(mTracker.tablesNames[4]).isEqualTo("a") // external content fts
    }

    @Test
    @org.junit.Ignore // TODO(b/233855234) - disabled until test is moved to Kotlin
    fun testWeak() {
        val data = AtomicInteger(0)
        var observer: InvalidationTracker.Observer? = object : InvalidationTracker.Observer("a") {
            override fun onInvalidated(tables: Set<String>) {
                data.incrementAndGet()
            }
        }
        val queue = ReferenceQueue<Any?>()
        WeakReference(observer, queue)
        mTracker.addWeakObserver(observer!!)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(data.get()).isEqualTo(1)
        @Suppress("UNUSED_VALUE") // On purpose, to dereference the observer and GC it
        observer = null
        forceGc(queue)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(data.get()).isEqualTo(1)
    }

    @Test
    fun addRemoveObserver() {
        val observer: InvalidationTracker.Observer = LatchObserver(1, "a")
        mTracker.addObserver(observer)
        assertThat(mTracker.observerMap.size()).isEqualTo(1)
        mTracker.removeObserver(LatchObserver(1, "a"))
        assertThat(mTracker.observerMap.size()).isEqualTo(1)
        mTracker.removeObserver(observer)
        assertThat(mTracker.observerMap.size()).isEqualTo(0)
    }

    private fun drainTasks() {
        mTaskExecutorRule.drainTasks(200)
    }

    @Test
    fun badObserver() {
        assertFailsWith<IllegalArgumentException>(message = "There is no table with name x") {
            val observer: InvalidationTracker.Observer = LatchObserver(1, "x")
            mTracker.addObserver(observer)
        }
    }

    private fun refreshSync() {
        mTracker.refreshVersionsAsync()
        drainTasks()
    }

    @Ignore // b/253058904
    @Test
    fun refreshCheckTasks() {
        whenever(mRoomDatabase.query(any<SimpleSQLiteQuery>(), isNull())).thenReturn(mock<Cursor>())
        mTracker.refreshVersionsAsync()
        mTracker.refreshVersionsAsync()
        verify(mTaskExecutorRule.taskExecutor).executeOnDiskIO(mTracker.refreshRunnable)
        drainTasks()
        reset(mTaskExecutorRule.taskExecutor)
        mTracker.refreshVersionsAsync()
        verify(mTaskExecutorRule.taskExecutor).executeOnDiskIO(mTracker.refreshRunnable)
    }

    @Test
    @Throws(Exception::class)
    fun observe1Table() {
        val observer = LatchObserver(1, "a")
        mTracker.addObserver(observer)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("a")
        setInvalidatedTables(1)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(false)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("a")
    }

    @Test
    @Throws(Exception::class)
    fun observe2Tables() {
        val observer = LatchObserver(1, "A", "B")
        mTracker.addObserver(observer)
        setInvalidatedTables(0, 1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(2)
        assertThat(observer.invalidatedTables).containsAtLeast("A", "B")
        setInvalidatedTables(1, 2)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("B")
        setInvalidatedTables(0, 3)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("A")
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(false)
    }

    @Test
    fun locale() {
        val observer = LatchObserver(1, "I")
        mTracker.addObserver(observer)
    }

    @Test
    fun closedDb() {
        doReturn(false).whenever(mRoomDatabase).isOpenInternal
        doThrow(IllegalStateException("foo")).whenever(mOpenHelper).writableDatabase
        mTracker.addObserver(LatchObserver(1, "a", "b"))
        mTracker.refreshRunnable.run()
    }

    @Test
    fun createTriggerOnShadowTable() {
        val observer = LatchObserver(1, "C")
        val triggers = arrayOf("UPDATE", "DELETE", "INSERT")
        var sqlCaptorValues: List<String>
        mTracker.addObserver(observer)
        var sqlArgCaptor: KArgumentCaptor<String> = argumentCaptor()
        verify(mSqliteDb, times(4)).execSQL(sqlArgCaptor.capture())
        sqlCaptorValues = sqlArgCaptor.allValues
        assertThat(sqlCaptorValues[0])
            .isEqualTo("INSERT OR IGNORE INTO room_table_modification_log VALUES(3, 0)")
        for (i in triggers.indices) {
            assertThat(sqlCaptorValues[i + 1])
                .isEqualTo(
                    "CREATE TEMP TRIGGER IF NOT EXISTS " +
                        "`room_table_modification_trigger_c_content_" + triggers[i] +
                        "` AFTER " + triggers[i] + " ON `c_content` BEGIN UPDATE " +
                        "room_table_modification_log SET invalidated = 1 WHERE table_id = 3 " +
                        "AND invalidated = 0; END"
                )
        }
        reset(mSqliteDb)
        mTracker.removeObserver(observer)
        sqlArgCaptor = argumentCaptor()
        verify(mSqliteDb, times(3)).execSQL(sqlArgCaptor.capture())
        sqlCaptorValues = sqlArgCaptor.allValues
        for (i in triggers.indices) {
            assertThat(sqlCaptorValues[i])
                .isEqualTo(
                    "DROP TRIGGER IF EXISTS `room_table_modification_trigger_c_content_" +
                        triggers[i] + "`"
                )
        }
    }

    @Test
    fun observeFtsTable() {
        val observer = LatchObserver(1, "C")
        mTracker.addObserver(observer)
        setInvalidatedTables(3)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("C")
        setInvalidatedTables(1)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(false)
        setInvalidatedTables(0, 3)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("C")
    }

    @Test
    fun observeExternalContentFtsTable() {
        val observer = LatchObserver(1, "d")
        mTracker.addObserver(observer)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("d")
        setInvalidatedTables(2, 3)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(false)
        setInvalidatedTables(0, 1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("d")
    }

    @Test
    fun observeExternalContentFtsTableAndContentTable() {
        val observer = LatchObserver(1, "d", "a")
        mTracker.addObserver(observer)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(2)
        assertThat(observer.invalidatedTables).containsAtLeast("d", "a")
        setInvalidatedTables(2, 3)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(false)
        setInvalidatedTables(0, 1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(2)
        assertThat(observer.invalidatedTables).containsAtLeast("d", "a")
    }

    @Test
    fun observeExternalContentFatsTableAndContentTableSeparately() {
        val observerA = LatchObserver(1, "a")
        val observerD = LatchObserver(1, "d")
        mTracker.addObserver(observerA)
        mTracker.addObserver(observerD)
        setInvalidatedTables(0)
        refreshSync()
        assertThat(observerA.await()).isEqualTo(true)
        assertThat(observerD.await()).isEqualTo(true)
        assertThat(observerA.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observerD.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observerA.invalidatedTables).contains("a")
        assertThat(observerD.invalidatedTables).contains("d")

        // Remove observer 'd' which is backed by 'a', observers to 'a' should still work.
        mTracker.removeObserver(observerD)
        setInvalidatedTables(0)
        observerA.reset(1)
        observerD.reset(1)
        refreshSync()
        assertThat(observerA.await()).isEqualTo(true)
        assertThat(observerD.await()).isEqualTo(false)
        assertThat(observerA.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observerA.invalidatedTables).contains("a")
    }

    @Test
    fun observeView() {
        val observer = LatchObserver(1, "E")
        mTracker.addObserver(observer)
        setInvalidatedTables(0, 1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("a")
        setInvalidatedTables(2, 3)
        observer.reset(1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(false)
        setInvalidatedTables(0, 1)
        refreshSync()
        assertThat(observer.await()).isEqualTo(true)
        assertThat(observer.invalidatedTables!!.size).isEqualTo(1)
        assertThat(observer.invalidatedTables).contains("a")
    }

    @Test
    fun failFastCreateLiveData() {
        // assert that sending a bad createLiveData table name fails instantly
        try {
            mTracker.createLiveData<Unit>(
                tableNames = arrayOf("invalid table name"),
                inTransaction = false
            ) {}
            fail("should've throw an exception for invalid table name")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun closedDbAfterOpen() {
        setInvalidatedTables(3, 1)
        mTracker.addObserver(LatchObserver(1, "a", "b"))
        mTracker.syncTriggers()
        mTracker.refreshRunnable.run()
        doThrow(SQLiteException("foo")).whenever(mRoomDatabase)?.query(
            query = InvalidationTracker.SELECT_UPDATED_TABLES_SQL,
            args = arrayOf(Array<Any>::class.java)
        )
        mTracker.pendingRefresh.set(true)
        mTracker.refreshRunnable.run()
    }

    /**
     * Setup Cursor result to return INVALIDATED for given tableIds
     */
    private fun setInvalidatedTables(vararg tableIds: Int) {
        // mockito does not like multi-threaded access so before setting versions, make sure we
        // sync background tasks.
        drainTasks()
        val cursor = createCursorWithValues(*tableIds)
        doReturn(cursor).whenever(mRoomDatabase)?.query(
            query = argThat<SimpleSQLiteQuery> { argument ->
                argument.sql == InvalidationTracker.SELECT_UPDATED_TABLES_SQL
            },
            signal = isNull(),
        )
    }

    private fun createCursorWithValues(vararg tableIds: Int): Cursor {
        val cursor: Cursor = mock()
        val index = AtomicInteger(-1)
        whenever(cursor.moveToNext()).thenAnswer { index.addAndGet(1) < tableIds.size }
        val intAnswer = Answer { invocation ->
            // checkUpdatedTable only checks for column 0 (invalidated table id)
            assert(invocation.arguments[0] as Int == 0)
            tableIds[index.toInt()]
        }
        whenever(cursor.getInt(anyInt())).thenAnswer(intAnswer)
        return cursor
    }

    internal class LatchObserver(
        count: Int,
        vararg tableNames: String
    ) : InvalidationTracker.Observer(arrayOf(*tableNames)) {
        private var mLatch: CountDownLatch

        var invalidatedTables: Set<String>? = null
            private set

        init {
            mLatch = CountDownLatch(count)
        }

        fun await(): Boolean {
            return mLatch.await(3, TimeUnit.SECONDS)
        }

        override fun onInvalidated(tables: Set<String>) {
            invalidatedTables = tables
            mLatch.countDown()
        }

        fun reset(count: Int) {
            invalidatedTables = null
            mLatch = CountDownLatch(count)
        }
    }

    companion object {
        /**
         * Tries to trigger garbage collection by allocating in the heap until an element is
         * available in the given reference queue.
         */
        private fun forceGc(queue: ReferenceQueue<Any?>) {
            val continueTriggeringGc = AtomicBoolean(true)
            val t = Thread {
                var byteCount = 0
                try {
                    val leak = ArrayList<ByteArray>()
                    do {
                        val arraySize = (Math.random() * 1000).toInt()
                        byteCount += arraySize
                        leak.add(ByteArray(arraySize))
                        System.gc() // Not guaranteed to trigger GC, hence the leak and the timeout
                        Thread.sleep(10)
                    } while (continueTriggeringGc.get())
                } catch (e: InterruptedException) {
                    // Ignored
                }
                println("Allocated $byteCount bytes trying to force a GC.")
            }
            t.start()
            val result = queue.remove(TimeUnit.SECONDS.toMillis(10))
            continueTriggeringGc.set(false)
            t.interrupt()
            assertWithMessage("Couldn't trigger garbage collection, test flake")
                .that(result)
                .isNotNull()
            result.clear()
        }
    }
}
