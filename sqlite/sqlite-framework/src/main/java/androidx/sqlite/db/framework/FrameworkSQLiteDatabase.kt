/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.sqlite.db.framework

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteCursorDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQuery
import android.database.sqlite.SQLiteTransactionListener
import android.os.Build
import android.os.CancellationSignal
import android.text.TextUtils
import android.util.Pair
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.io.IOException
import java.util.Locale

/**
 * Delegates all calls to an implementation of [SQLiteDatabase].
 *
 * @constructor Creates a wrapper around [SQLiteDatabase].
 *
 * @param delegate The delegate to receive all calls.
 */
internal class FrameworkSQLiteDatabase(
    private val delegate: SQLiteDatabase
) : SupportSQLiteDatabase {
    override fun compileStatement(sql: String): SupportSQLiteStatement {
        return FrameworkSQLiteStatement(delegate.compileStatement(sql))
    }

    override fun beginTransaction() {
        delegate.beginTransaction()
    }

    override fun beginTransactionNonExclusive() {
        delegate.beginTransactionNonExclusive()
    }

    override fun beginTransactionWithListener(
        transactionListener: SQLiteTransactionListener
    ) {
        delegate.beginTransactionWithListener(transactionListener)
    }

    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener
    ) {
        delegate.beginTransactionWithListenerNonExclusive(transactionListener)
    }

    override fun endTransaction() {
        delegate.endTransaction()
    }

    override fun setTransactionSuccessful() {
        delegate.setTransactionSuccessful()
    }

    override fun inTransaction(): Boolean {
        return delegate.inTransaction()
    }

    override val isDbLockedByCurrentThread: Boolean
        get() = delegate.isDbLockedByCurrentThread

    override fun yieldIfContendedSafely(): Boolean {
        return delegate.yieldIfContendedSafely()
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        return delegate.yieldIfContendedSafely(sleepAfterYieldDelayMillis)
    }

    override var version: Int
        get() = delegate.version
        set(value) {
            delegate.version = value
        }

    override var maximumSize: Long
        get() = delegate.maximumSize
        set(numBytes) {
            delegate.maximumSize = numBytes
        }

    override fun setMaximumSize(numBytes: Long): Long {
        delegate.maximumSize = numBytes
        return delegate.maximumSize
    }

    override val isExecPerConnectionSQLSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override fun execPerConnectionSQL(sql: String, bindArgs: Array<out Any?>?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.execPerConnectionSQL(delegate, sql, bindArgs)
        } else {
            throw UnsupportedOperationException(
                "execPerConnectionSQL is not supported on a " +
                    "SDK version lower than 30, current version is: " + Build.VERSION.SDK_INT
            )
        }
    }

    override var pageSize: Long
        get() = delegate.pageSize
        set(numBytes) {
            delegate.pageSize = numBytes
        }

    override fun query(query: String): Cursor {
        return query(SimpleSQLiteQuery(query))
    }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
        return query(SimpleSQLiteQuery(query, bindArgs))
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        val cursorFactory = {
                _: SQLiteDatabase?,
                masterQuery: SQLiteCursorDriver?,
                editTable: String?,
                sqLiteQuery: SQLiteQuery? ->
            query.bindTo(
                FrameworkSQLiteProgram(
                    sqLiteQuery!!
                )
            )
            SQLiteCursor(masterQuery, editTable, sqLiteQuery)
        }

        return delegate.rawQueryWithFactory(
            cursorFactory, query.sql, EMPTY_STRING_ARRAY, null)
    }

    @RequiresApi(16)
    override fun query(
        query: SupportSQLiteQuery,
        cancellationSignal: CancellationSignal?
    ): Cursor {
        return SupportSQLiteCompat.Api16Impl.rawQueryWithFactory(delegate, query.sql,
            EMPTY_STRING_ARRAY, null, cancellationSignal!!
        ) { _: SQLiteDatabase?,
            masterQuery: SQLiteCursorDriver?,
            editTable: String?,
            sqLiteQuery: SQLiteQuery? ->
            query.bindTo(
                FrameworkSQLiteProgram(
                    sqLiteQuery!!
                )
            )
            SQLiteCursor(masterQuery, editTable, sqLiteQuery)
        }
    }

    @Throws(SQLException::class)
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
        return delegate.insertWithOnConflict(table, null, values, conflictAlgorithm)
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        val query = buildString {
            append("DELETE FROM ")
            append(table)
            if (!whereClause.isNullOrEmpty()) {
                append(" WHERE ")
                append(whereClause)
            }
        }
        val statement = compileStatement(query)
        SimpleSQLiteQuery.bind(statement, whereArgs)
        return statement.executeUpdateDelete()
    }

    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?
    ): Int {
        // taken from SQLiteDatabase class.
        require(values.size() != 0) { "Empty values" }

        // move all bind args to one array
        val setValuesSize = values.size()
        val bindArgsSize =
            if (whereArgs == null) setValuesSize else setValuesSize + whereArgs.size
        val bindArgs = arrayOfNulls<Any>(bindArgsSize)
        val sql = buildString {
            append("UPDATE ")
            append(CONFLICT_VALUES[conflictAlgorithm])
            append(table)
            append(" SET ")

            var i = 0
            for (colName in values.keySet()) {
                append(if (i > 0) "," else "")
                append(colName)
                bindArgs[i++] = values[colName]
                append("=?")
            }
            if (whereArgs != null) {
                i = setValuesSize
                while (i < bindArgsSize) {
                    bindArgs[i] = whereArgs[i - setValuesSize]
                    i++
                }
            }
            if (!TextUtils.isEmpty(whereClause)) {
                append(" WHERE ")
                append(whereClause)
            }
        }
        val stmt = compileStatement(sql)
        SimpleSQLiteQuery.bind(stmt, bindArgs)
        return stmt.executeUpdateDelete()
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String) {
        delegate.execSQL(sql)
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        delegate.execSQL(sql, bindArgs)
    }

    override val isReadOnly: Boolean
        get() = delegate.isReadOnly

    override val isOpen: Boolean
        get() = delegate.isOpen

    override fun needUpgrade(newVersion: Int): Boolean {
        return delegate.needUpgrade(newVersion)
    }

    override val path: String?
        get() = delegate.path

    override fun setLocale(locale: Locale) {
        delegate.setLocale(locale)
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        delegate.setMaxSqlCacheSize(cacheSize)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
        SupportSQLiteCompat.Api16Impl.setForeignKeyConstraintsEnabled(delegate, enabled)
    }

    override fun enableWriteAheadLogging(): Boolean {
        return delegate.enableWriteAheadLogging()
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun disableWriteAheadLogging() {
        SupportSQLiteCompat.Api16Impl.disableWriteAheadLogging(delegate)
    }

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override val isWriteAheadLoggingEnabled: Boolean
        get() = SupportSQLiteCompat.Api16Impl.isWriteAheadLoggingEnabled(delegate)

    override val attachedDbs: List<Pair<String, String>>?
        get() = delegate.attachedDbs

    override val isDatabaseIntegrityOk: Boolean
        get() = delegate.isDatabaseIntegrityOk

    @Throws(IOException::class)
    override fun close() {
        delegate.close()
    }

    /**
     * Checks if this object delegates to the same given database reference.
     */
    fun isDelegate(sqLiteDatabase: SQLiteDatabase): Boolean {
        return delegate == sqLiteDatabase
    }

    @RequiresApi(30)
    internal object Api30Impl {
        @DoNotInline
        fun execPerConnectionSQL(
            sQLiteDatabase: SQLiteDatabase,
            sql: String,
            bindArgs: Array<out Any?>?
        ) {
            sQLiteDatabase.execPerConnectionSQL(sql, bindArgs)
        }
    }

    companion object {
        private val CONFLICT_VALUES =
            arrayOf(
                "",
                " OR ROLLBACK ",
                " OR ABORT ",
                " OR FAIL ",
                " OR IGNORE ",
                " OR REPLACE "
            )
        private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)
    }
}
