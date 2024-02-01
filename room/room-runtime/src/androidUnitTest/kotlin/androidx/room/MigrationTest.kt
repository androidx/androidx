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

package androidx.room

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.kruth.assertThat
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MigrationTest {

    @Test
    fun testMigrationExtension() {
        var calledWithDb: SupportSQLiteDatabase? = null
        val migration = Migration(10, 20) {
            calledWithDb = it
        }
        val db = FakeDB()
        migration.migrate(db)
        assertThat(migration.startVersion).isEqualTo(10)
        assertThat(migration.endVersion).isEqualTo(20)
        assertThat(calledWithDb).isEqualTo(calledWithDb)
    }
}

private class FakeDB : SupportSQLiteDatabase {
    override fun close() {
        throw UnsupportedOperationException()
    }

    override fun compileStatement(sql: String): SupportSQLiteStatement {
        throw UnsupportedOperationException()
    }

    override fun beginTransaction() {
        throw UnsupportedOperationException()
    }

    override fun beginTransactionNonExclusive() {
        throw UnsupportedOperationException()
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {
        throw UnsupportedOperationException()
    }

    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener
    ) {
        throw UnsupportedOperationException()
    }

    override fun endTransaction() {
        throw UnsupportedOperationException()
    }

    override fun setTransactionSuccessful() {
        throw UnsupportedOperationException()
    }

    override fun inTransaction(): Boolean {
        throw UnsupportedOperationException()
    }

    override val isDbLockedByCurrentThread: Boolean
        get() { throw UnsupportedOperationException() }

    override fun yieldIfContendedSafely(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        throw UnsupportedOperationException()
    }

    override var version: Int
        get() { throw UnsupportedOperationException() }
        set(_) { throw UnsupportedOperationException() }

    override val maximumSize: Long
        get() { throw UnsupportedOperationException() }
    override fun setMaximumSize(numBytes: Long): Long {
        throw UnsupportedOperationException()
    }

    override var pageSize: Long
        get() { throw UnsupportedOperationException() }
        set(_) { throw UnsupportedOperationException() }

    override fun query(query: String): Cursor {
        throw UnsupportedOperationException()
    }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
        throw UnsupportedOperationException()
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        throw UnsupportedOperationException()
    }

    override fun query(
        query: SupportSQLiteQuery,
        cancellationSignal: CancellationSignal?
    ): Cursor {
        throw UnsupportedOperationException()
    }

    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
        throw UnsupportedOperationException()
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?
    ): Int {
        throw UnsupportedOperationException()
    }

    override fun execSQL(sql: String) {
        throw UnsupportedOperationException()
    }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        throw UnsupportedOperationException()
    }

    override val isReadOnly: Boolean
        get() { throw UnsupportedOperationException() }

    override val isOpen: Boolean
        get() { throw UnsupportedOperationException() }

    override fun needUpgrade(newVersion: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override val path: String
        get() { throw UnsupportedOperationException() }

    override fun setLocale(locale: Locale) {
        throw UnsupportedOperationException()
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        throw UnsupportedOperationException()
    }

    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
        throw UnsupportedOperationException()
    }

    override fun enableWriteAheadLogging(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun disableWriteAheadLogging() {
        throw UnsupportedOperationException()
    }

    override val isWriteAheadLoggingEnabled: Boolean
        get() { throw UnsupportedOperationException() }

    override val attachedDbs: MutableList<Pair<String, String>>
        get() { throw UnsupportedOperationException() }

    override val isDatabaseIntegrityOk: Boolean
        get() { throw UnsupportedOperationException() }
}
