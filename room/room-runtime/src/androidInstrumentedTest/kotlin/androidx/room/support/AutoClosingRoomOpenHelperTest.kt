/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.util.useCursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AutoClosingRoomOpenHelperTest {

    companion object {
        private const val DB_NAME = "test.db"
        private const val TIMEOUT_AMOUNT = 10L
    }

    private val testCoroutineScope = TestScope()

    private lateinit var autoCloser: AutoCloser
    private lateinit var testWatch: AutoCloserTestWatch
    private lateinit var callback: Callback
    private lateinit var autoClosingRoomOpenHelper: AutoClosingRoomOpenHelper

    private open class Callback(var throwOnOpen: Boolean = false) :
        SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) {}

        override fun onOpen(db: SupportSQLiteDatabase) {
            if (throwOnOpen) {
                throw IOException()
            }
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    @Before
    fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DB_NAME)

        testWatch = AutoCloserTestWatch(TIMEOUT_AMOUNT, testCoroutineScope.testScheduler)
        callback = Callback()
        val delegateOpenHelper =
            FrameworkSQLiteOpenHelperFactory()
                .create(
                    SupportSQLiteOpenHelper.Configuration.builder(
                            ApplicationProvider.getApplicationContext()
                        )
                        .callback(callback)
                        .name(DB_NAME)
                        .build()
                )
        autoCloser =
            AutoCloser(TIMEOUT_AMOUNT, TimeUnit.MILLISECONDS, testWatch).apply {
                initOpenHelper(delegateOpenHelper)
                initCoroutineScope(testCoroutineScope)
                setAutoCloseCallback {}
            }
        autoClosingRoomOpenHelper =
            AutoClosingRoomOpenHelper(delegate = delegateOpenHelper, autoCloser = autoCloser)
    }

    @After
    fun cleanUp() {
        // At the end of all tests we always expect to auto-close the database
        assertWithMessage("Database was not closed").that(autoCloser.delegateDatabase).isNull()
    }

    @Test
    fun testQueryFailureDecrementsRefCount() = runTest {
        assertThrows<SQLiteException> {
            autoClosingRoomOpenHelper.writableDatabase.query("select * from nonexistanttable")
        }

        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun testCursorKeepsDbAlive() = runTest {
        autoClosingRoomOpenHelper.writableDatabase.execSQL("create table user (idk int)")

        val cursor = autoClosingRoomOpenHelper.writableDatabase.query("select * from user")
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(1)
        cursor.close()
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun testTransactionKeepsDbAlive() = runTest {
        autoClosingRoomOpenHelper.writableDatabase.beginTransaction()
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(1)
        autoClosingRoomOpenHelper.writableDatabase.endTransaction()
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun enableWriteAheadLogging_onOpenHelper() = runTest {
        autoClosingRoomOpenHelper.setWriteAheadLoggingEnabled(true)
        assertThat(autoClosingRoomOpenHelper.writableDatabase.isWriteAheadLoggingEnabled).isTrue()

        testWatch.step()

        assertThat(autoClosingRoomOpenHelper.writableDatabase.isWriteAheadLoggingEnabled).isTrue()
    }

    @Test
    fun testEnableWriteAheadLogging_onSupportSqliteDatabase_throwsUnsupportedOperation() = runTest {
        assertThrows<UnsupportedOperationException> {
            autoClosingRoomOpenHelper.writableDatabase.enableWriteAheadLogging()
        }

        assertThrows<UnsupportedOperationException> {
            autoClosingRoomOpenHelper.writableDatabase.disableWriteAheadLogging()
        }
    }

    @Test
    fun testStatementReturnedByCompileStatement_doesNotKeepDatabaseOpen() = runTest {
        val db = autoClosingRoomOpenHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        db.compileStatement("insert into users (idk) values (1)")

        testWatch.step()

        assertThat(db.isOpen).isFalse() // db should close
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun testStatementReturnedByCompileStatement_reOpensDatabase() = runTest {
        val db = autoClosingRoomOpenHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        val statement = db.compileStatement("insert into user (idk) values (1)")

        testWatch.step()

        statement.executeInsert() // This should succeed

        db.query("select * from user").useCursor { assertThat(it.count).isEqualTo(1) }

        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun testStatementReturnedByCompileStatement_worksWithBinds() = runTest {
        val db = autoClosingRoomOpenHelper.writableDatabase

        db.execSQL("create table users (i int, d double, b blob, n int, s string)")

        val statement = db.compileStatement("insert into users (i, d, b, n, s) values (?,?,?,?,?)")

        statement.bindString(5, "123")
        statement.bindLong(1, 123)
        statement.bindDouble(2, 1.23)
        statement.bindBlob(3, byteArrayOf(1, 2, 3))
        statement.bindNull(4)

        statement.executeInsert()

        db.query("select * from users").useCursor {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getInt(0)).isEqualTo(123)
            assertThat(it.getDouble(1)).isWithin(.01).of(1.23)

            assertThat(it.getBlob(2)).isEqualTo(byteArrayOf(1, 2, 3))
            assertThat(it.isNull(3)).isTrue()
            assertThat(it.getString(4)).isEqualTo("123")
        }

        statement.clearBindings()
        statement.executeInsert() // should insert with nulls

        db.query("select * from users").useCursor {
            assertThat(it.moveToFirst()).isTrue()
            it.moveToNext()
            assertThat(it.isNull(0)).isTrue()
            assertThat(it.isNull(1)).isTrue()
            assertThat(it.isNull(2)).isTrue()
            assertThat(it.isNull(3)).isTrue()
            assertThat(it.isNull(4)).isTrue()
        }
    }

    @Test
    fun testGetDelegate() = runTest {
        val delegateOpenHelper =
            FrameworkSQLiteOpenHelperFactory()
                .create(
                    SupportSQLiteOpenHelper.Configuration.builder(
                            ApplicationProvider.getApplicationContext()
                        )
                        .callback(Callback())
                        .name(DB_NAME)
                        .build()
                )

        val autoClosing =
            AutoClosingRoomOpenHelper(
                delegateOpenHelper,
                AutoCloser(0, TimeUnit.MILLISECONDS).apply {
                    initCoroutineScope(testCoroutineScope)
                    setAutoCloseCallback {}
                }
            )

        assertThat(autoClosing.delegate).isSameInstanceAs(delegateOpenHelper)
    }

    private fun runTest(testBody: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest {
            testBody.invoke(this)
            testWatch.step()
        }
}
