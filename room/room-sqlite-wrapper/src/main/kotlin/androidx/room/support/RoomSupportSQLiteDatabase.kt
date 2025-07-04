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
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.room.RoomDatabase
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.execSQL
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.Locale

/**
 * A [SupportSQLiteDatabase] implementation that is backed by a [RoomDatabase] for compatibility
 * with existing usages of [SupportSQLiteDatabase].
 *
 * This wrapper is useful when an `SQLiteDriver` has been configured with the [RoomDatabase] which
 * causes [RoomDatabase.openHelper] to throw an exception since the backing database is no longer a
 * real [SupportSQLiteDatabase].
 */
internal class RoomSupportSQLiteDatabase(roomDatabase: RoomDatabase) : SupportSQLiteDatabase {

    private val session = RoomSupportSQLiteSession(roomDatabase)

    override fun compileStatement(sql: String): SupportSQLiteStatement {
        return RoomSupportSQLiteStatement(session, sql)
    }

    override fun beginTransaction() {
        session.beginTransaction(SQLiteTransactionType.EXCLUSIVE)
    }

    override fun beginTransactionNonExclusive() {
        session.beginTransaction(SQLiteTransactionType.IMMEDIATE)
    }

    override fun beginTransactionReadOnly() {
        session.beginTransaction(SQLiteTransactionType.DEFERRED)
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {
        session.beginTransaction(SQLiteTransactionType.EXCLUSIVE, transactionListener)
    }

    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener
    ) {
        session.beginTransaction(SQLiteTransactionType.IMMEDIATE, transactionListener)
    }

    override fun beginTransactionWithListenerReadOnly(
        transactionListener: SQLiteTransactionListener
    ) {
        session.beginTransaction(SQLiteTransactionType.DEFERRED, transactionListener)
    }

    override fun endTransaction() {
        session.endTransaction()
    }

    override fun setTransactionSuccessful() {
        session.setTransactionSuccessful()
    }

    override fun inTransaction(): Boolean {
        return session.inTransaction()
    }

    override val isDbLockedByCurrentThread: Boolean
        get() = false

    override fun yieldIfContendedSafely(): Boolean {
        return session.yieldIfContended()
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        return session.yieldIfContended(sleepAfterYieldDelayMillis)
    }

    override var version: Int
        get() =
            session.useReaderBlocking { connection ->
                connection.usePrepared("PRAGMA user_version") { stmt ->
                    stmt.step()
                    stmt.getInt(0)
                }
            }
        set(value): Unit {
            check(value >= 1) { "Version must be >=1, was $value" }
            session.useWriterBlocking { connection ->
                connection.execSQL("PRAGMA user_version = $value")
            }
        }

    override val maximumSize: Long
        get() {
            val pageCount =
                session.useWriterBlocking { connection ->
                    connection.usePrepared("PRAGMA max_page_count") { stmt ->
                        stmt.step()
                        stmt.getLong(0)
                    }
                }
            return pageCount * pageSize
        }

    override fun setMaximumSize(numBytes: Long): Long {
        val currPageSize = pageSize
        var numPages = numBytes / currPageSize
        if (numBytes % currPageSize != 0L) {
            numPages++
        }
        val newPageCount =
            session.useWriterBlocking { connection ->
                connection.usePrepared("PRAGMA max_page_count = $numPages") { stmt ->
                    stmt.step()
                    stmt.getLong(0)
                }
            }
        return newPageCount * currPageSize
    }

    override var pageSize: Long
        get() =
            session.useWriterBlocking { connection ->
                connection.usePrepared("PRAGMA page_size") { stmt ->
                    stmt.step()
                    stmt.getLong(0)
                }
            }
        set(value) =
            session.useWriterBlocking { connection ->
                connection.execSQL("PRAGMA page_size = $value")
            }

    override fun query(query: String): Cursor =
        session.useReaderBlocking { connection ->
            connection.usePrepared(query) { stmt -> stmt.toCursor() }
        }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor =
        session.useReaderBlocking { connection ->
            connection.usePrepared(query) { stmt ->
                stmt.bindArgsArray(bindArgs)
                stmt.toCursor()
            }
        }

    override fun query(query: SupportSQLiteQuery): Cursor = query(query, null)

    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor {
        return session.useReaderBlocking { connection ->
            cancellationSignal?.throwIfCanceled()
            connection.usePrepared(query.sql) { stmt ->
                stmt.bindQuery(query)
                stmt.toCursor()
            }
        }
    }

    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
        val size = values.size()
        if (size == 0) {
            return -1
        }

        val sql = StringBuilder()
        sql.append("INSERT ")
        sql.append(getConflictAlgorithmClause(conflictAlgorithm))
        sql.append("INTO ")
        sql.append(table)
        sql.append("(")

        val bindArgs = arrayOfNulls<Any?>(size)
        var i = 0
        for (colName in values.keySet()) {
            sql.append(if (i > 0) "," else "")
            sql.append(colName)
            bindArgs[i++] = values[colName]
        }
        sql.append(")")
        sql.append(" VALUES (")
        for (j in 0 until size) {
            sql.append(if (j > 0) "," else "")
            sql.append("?")
        }
        sql.append(")")

        return compileStatement(sql.toString()).use {
            it.bindArgsArray(bindArgs)
            it.executeInsert()
        }
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        val sql = buildString {
            append("DELETE FROM ")
            append(table)
            if (!whereClause.isNullOrEmpty()) {
                append(" WHERE ")
                append(whereClause)
            }
        }
        return compileStatement(sql).use {
            if (whereArgs != null) {
                it.bindArgsArray(whereArgs)
            }
            it.executeUpdateDelete()
        }
    }

    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?,
    ): Int {
        val valuesSize = values.size()
        require(valuesSize > 0) { "Empty values" }

        val sql = StringBuilder()
        sql.append("UPDATE ")
        sql.append(getConflictAlgorithmClause(conflictAlgorithm))
        sql.append(table)
        sql.append(" SET ")

        val bindsArgsSize = if (whereArgs == null) valuesSize else valuesSize + whereArgs.size
        val bindArgs = arrayOfNulls<Any?>(bindsArgsSize)
        var i = 0
        for (colName in values.keySet()) {
            sql.append(if (i > 0) "," else "")
            sql.append(colName)
            sql.append(" = ?")
            bindArgs[i++] = values[colName]
        }
        if (whereArgs != null) {
            for (i in valuesSize until bindsArgsSize) {
                bindArgs[i] = whereArgs[i - valuesSize]
            }
        }
        if (!whereClause.isNullOrEmpty()) {
            sql.append(" WHERE ")
            sql.append(whereClause)
        }
        return compileStatement(sql.toString()).use {
            it.bindArgsArray(bindArgs)
            it.executeUpdateDelete()
        }
    }

    override fun execSQL(sql: String): Unit =
        session.useWriterBlocking { connection -> connection.execSQL(sql) }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>): Unit =
        session.useWriterBlocking { connection ->
            connection.usePrepared(sql) { stmt ->
                stmt.bindArgsArray(bindArgs)
                stmt.step()
            }
        }

    override val isReadOnly: Boolean
        get() = false

    override val isOpen: Boolean
        get() = session.isClosed()

    override fun needUpgrade(newVersion: Int): Boolean {
        return newVersion > version
    }

    override val path: String?
        get() = session.path

    override fun setLocale(locale: Locale) {
        // nothing to do, no built-in locale support
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        // nothing to do, statements are not cached
    }

    override fun setForeignKeyConstraintsEnabled(enabled: Boolean): Unit =
        session.useWriterBlocking { connection ->
            val onOffString = if (enabled) "ON" else "OFF"
            connection.execSQL("PRAGMA foreign_keys = $onOffString")
        }

    override fun enableWriteAheadLogging(): Boolean {
        throw UnsupportedOperationException(
            "Modifying journal mode is not supported by the wrapper, please configure journal " +
                "via Room's database builder."
        )
    }

    override fun disableWriteAheadLogging(): Unit {
        throw UnsupportedOperationException(
            "Modifying journal mode is not supported by the wrapper, please configure journal " +
                "via Room's database builder."
        )
    }

    override val isWriteAheadLoggingEnabled: Boolean
        get() =
            session.useReaderBlocking { connection ->
                connection.usePrepared("PRAGMA journal_mode") { stmt ->
                    stmt.step()
                    stmt.getText(0).equals("wal", ignoreCase = true)
                }
            }

    override val attachedDbs: List<Pair<String, String>>?
        get() =
            session.useWriterBlocking { connection ->
                connection.usePrepared("PRAGMA database_list") { stmt ->
                    buildList {
                        while (stmt.step()) {
                            val name = stmt.getText(1)
                            val path = stmt.getText(2)
                            add(Pair(name, path))
                        }
                    }
                }
            }

    override val isDatabaseIntegrityOk: Boolean
        get() =
            session.useReaderBlocking { connection ->
                connection.usePrepared("PRAGMA integrity_check") { stmt ->
                    if (stmt.step()) {
                        stmt.getText(0).equals("ok", ignoreCase = true)
                    } else {
                        false
                    }
                }
            }

    override fun close() {
        session.close()
    }

    private fun getConflictAlgorithmClause(conflictAlgorithm: Int) =
        when (conflictAlgorithm) {
            SQLiteDatabase.CONFLICT_NONE -> ""
            SQLiteDatabase.CONFLICT_ROLLBACK -> "OR ROLLBACK "
            SQLiteDatabase.CONFLICT_ABORT -> "OR ABORT "
            SQLiteDatabase.CONFLICT_FAIL -> "OR FAIL "
            SQLiteDatabase.CONFLICT_IGNORE -> "OR IGNORE "
            SQLiteDatabase.CONFLICT_REPLACE -> "OR REPLACE "
            else -> error("Unknown conflictAlgorithm: $conflictAlgorithm")
        }
}
