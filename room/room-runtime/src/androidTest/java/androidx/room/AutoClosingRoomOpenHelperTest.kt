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

package androidx.room

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.FlakyTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

public class AutoClosingRoomOpenHelperTest {

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
    public fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase("name")
    }

    private fun getAutoClosingRoomOpenHelper(
        timeoutMillis: Long = 10,
        callback: SupportSQLiteOpenHelper.Callback = Callback()
    ): AutoClosingRoomOpenHelper {

        val delegateOpenHelper = FrameworkSQLiteOpenHelperFactory()
            .create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(ApplicationProvider.getApplicationContext())
                    .callback(callback)
                    .name("name")
                    .build()
            )

        val autoCloseExecutor = Executors.newSingleThreadExecutor()

        return AutoClosingRoomOpenHelper(
            delegateOpenHelper,
            AutoCloser(timeoutMillis, TimeUnit.MILLISECONDS, autoCloseExecutor).apply {
                init(delegateOpenHelper)
                setAutoCloseCallback { }
            }
        )
    }

    @Test
    public fun testQueryFailureDecrementsRefCount() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()

        assertThrows<SQLiteException> {
            autoClosingRoomOpenHelper
                .writableDatabase.query("select * from nonexistanttable")
        }

        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    public fun testCursorKeepsDbAlive() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()
        autoClosingRoomOpenHelper.writableDatabase.execSQL("create table user (idk int)")

        val cursor =
            autoClosingRoomOpenHelper.writableDatabase.query("select * from user")
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(1)
        cursor.close()
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    public fun testTransactionKeepsDbAlive() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()
        autoClosingRoomOpenHelper.writableDatabase.beginTransaction()
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(1)
        autoClosingRoomOpenHelper.writableDatabase.endTransaction()
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    public fun enableWriteAheadLogging_onOpenHelper() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()

        autoClosingRoomOpenHelper.setWriteAheadLoggingEnabled(true)
        assertThat(autoClosingRoomOpenHelper.writableDatabase.isWriteAheadLoggingEnabled).isTrue()

        Thread.sleep(100) // Let the db auto close...

        assertThat(autoClosingRoomOpenHelper.writableDatabase.isWriteAheadLoggingEnabled).isTrue()
    }

    @Test
    public fun testEnableWriteAheadLogging_onSupportSqliteDatabase_throwsUnsupportedOperation() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()

        assertThrows<UnsupportedOperationException> {
            autoClosingRoomOpenHelper.writableDatabase.enableWriteAheadLogging()
        }

        assertThrows<UnsupportedOperationException> {
            autoClosingRoomOpenHelper.writableDatabase.disableWriteAheadLogging()
        }
    }

    @FlakyTest(bugId = 190607416)
    @Test
    public fun testOnOpenCalledOnEachOpen() {
        val countingCallback = object : Callback() {
            var onCreateCalls = 0
            var onOpenCalls = 0

            override fun onCreate(db: SupportSQLiteDatabase) {
                onCreateCalls++
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                onOpenCalls++
            }
        }

        val autoClosingRoomOpenHelper =
            getAutoClosingRoomOpenHelper(callback = countingCallback)

        autoClosingRoomOpenHelper.writableDatabase
        assertThat(countingCallback.onOpenCalls).isEqualTo(1)
        assertThat(countingCallback.onCreateCalls).isEqualTo(1)

        Thread.sleep(20) // Database should auto-close here
        autoClosingRoomOpenHelper.writableDatabase
        assertThat(countingCallback.onOpenCalls).isEqualTo(2)
        assertThat(countingCallback.onCreateCalls).isEqualTo(1)
    }

    @Test
    public fun testStatementReturnedByCompileStatement_doesntKeepDatabaseOpen() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()

        val db = autoClosingRoomOpenHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        db.compileStatement("insert into users (idk) values (1)")

        Thread.sleep(20)
        assertThat(db.isOpen).isFalse() // db should close
        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    public fun testStatementReturnedByCompileStatement_reOpensDatabase() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()

        val db = autoClosingRoomOpenHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        val statement = db
            .compileStatement("insert into user (idk) values (1)")

        Thread.sleep(20)

        statement.executeInsert() // This should succeed

        db.query("select * from user").useCursor {
            assertThat(it.count).isEqualTo(1)
        }

        assertThat(autoClosingRoomOpenHelper.autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    public fun testStatementReturnedByCompileStatement_worksWithBinds() {
        val autoClosingRoomOpenHelper = getAutoClosingRoomOpenHelper()
        val db = autoClosingRoomOpenHelper.writableDatabase

        db.execSQL("create table users (i int, d double, b blob, n int, s string)")

        val statement = db.compileStatement(
            "insert into users (i, d, b, n, s) values (?,?,?,?,?)"
        )

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
    public fun testGetDelegate() {
        val delegateOpenHelper = FrameworkSQLiteOpenHelperFactory()
            .create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(ApplicationProvider.getApplicationContext())
                    .callback(Callback())
                    .name("name")
                    .build()
            )

        val autoCloseExecutor = Executors.newSingleThreadExecutor()

        val autoClosing = AutoClosingRoomOpenHelper(
            delegateOpenHelper,
            AutoCloser(0, TimeUnit.MILLISECONDS, autoCloseExecutor)
        )

        assertThat(autoClosing.getDelegate()).isSameInstanceAs(delegateOpenHelper)
    }

    // Older API versions didn't have Cursor implement Closeable
    private inline fun Cursor.useCursor(block: (Cursor) -> Unit) {
        try {
            block(this)
        } finally {
            this.close()
        }
    }
}