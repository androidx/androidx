/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.database.SQLException
import android.database.sqlite.SQLiteTransactionListener
import android.os.Build
import android.os.CancellationSignal
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.room.DelegatingOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.io.IOException
import java.util.Locale

/** A SupportSQLiteOpenHelper that has auto close enabled for database connections. */
internal class AutoClosingRoomOpenHelper(
    override val delegate: SupportSQLiteOpenHelper,
    internal val autoCloser: AutoCloser
) : SupportSQLiteOpenHelper by delegate, DelegatingOpenHelper {

    private val autoClosingDb = AutoClosingSupportSQLiteDatabase(autoCloser)

    init {
        autoCloser.initOpenHelper(delegate)
    }

    override val writableDatabase: SupportSQLiteDatabase
        get() {
            autoClosingDb.pokeOpen()
            return autoClosingDb
        }

    override val readableDatabase: SupportSQLiteDatabase
        get() {
            // Note we don't differentiate between writable db and readable db
            // We try to open the db so the open callbacks run
            autoClosingDb.pokeOpen()
            return autoClosingDb
        }

    override fun close() {
        autoClosingDb.close()
    }

    /** SupportSQLiteDatabase that also keeps refcounts and autocloses the database */
    internal class AutoClosingSupportSQLiteDatabase(private val autoCloser: AutoCloser) :
        SupportSQLiteDatabase {
        fun pokeOpen() {
            autoCloser.executeRefCountingFunction<Any?> { null }
        }

        override fun compileStatement(sql: String): SupportSQLiteStatement {
            return AutoClosingSupportSQLiteStatement(sql, autoCloser)
        }

        override fun beginTransaction() {
            // We assume that after every successful beginTransaction() call there *must* be a
            // endTransaction() call.
            val db = autoCloser.incrementCountAndEnsureDbIsOpen()
            try {
                db.beginTransaction()
            } catch (t: Throwable) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                autoCloser.decrementCountAndScheduleClose()
                throw t
            }
        }

        override fun beginTransactionNonExclusive() {
            // We assume that after every successful beginTransaction() call there *must* be a
            // endTransaction() call.
            val db = autoCloser.incrementCountAndEnsureDbIsOpen()
            try {
                db.beginTransactionNonExclusive()
            } catch (t: Throwable) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                autoCloser.decrementCountAndScheduleClose()
                throw t
            }
        }

        override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {
            // We assume that after every successful beginTransaction() call there *must* be a
            // endTransaction() call.
            val db = autoCloser.incrementCountAndEnsureDbIsOpen()
            try {
                db.beginTransactionWithListener(transactionListener)
            } catch (t: Throwable) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                autoCloser.decrementCountAndScheduleClose()
                throw t
            }
        }

        override fun beginTransactionWithListenerNonExclusive(
            transactionListener: SQLiteTransactionListener
        ) {
            // We assume that after every successful beginTransaction() call there *will* always
            // be a corresponding endTransaction() call. Without a corresponding
            // endTransactionCall we will never close the db.
            val db = autoCloser.incrementCountAndEnsureDbIsOpen()
            try {
                db.beginTransactionWithListenerNonExclusive(transactionListener)
            } catch (t: Throwable) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                autoCloser.decrementCountAndScheduleClose()
                throw t
            }
        }

        override fun endTransaction() {
            try {
                autoCloser.delegateDatabase!!.endTransaction()
            } finally {
                autoCloser.decrementCountAndScheduleClose()
            }
        }

        override fun setTransactionSuccessful() {
            autoCloser.delegateDatabase!!.setTransactionSuccessful()
        }

        override fun inTransaction(): Boolean {
            return if (autoCloser.delegateDatabase == null) {
                false
            } else {
                autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::inTransaction)
            }
        }

        override val isDbLockedByCurrentThread: Boolean
            get() =
                if (autoCloser.delegateDatabase == null) {
                    false
                } else {
                    autoCloser.executeRefCountingFunction(
                        SupportSQLiteDatabase::isDbLockedByCurrentThread
                    )
                }

        override fun yieldIfContendedSafely(): Boolean {
            return autoCloser.executeRefCountingFunction(
                SupportSQLiteDatabase::yieldIfContendedSafely
            )
        }

        override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
            return autoCloser.executeRefCountingFunction(
                SupportSQLiteDatabase::yieldIfContendedSafely
            )
        }

        override var version: Int
            get() = autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::version)
            set(version) {
                autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                    db.version = version
                }
            }

        override val maximumSize: Long
            get() = autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::maximumSize)

        override fun setMaximumSize(numBytes: Long): Long {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.setMaximumSize(numBytes)
            }
        }

        override var pageSize: Long
            get() = autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::pageSize)
            set(numBytes) {
                autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                    db.pageSize = numBytes
                    null
                }
            }

        override fun query(query: String): Cursor {
            val result =
                try {
                    autoCloser.incrementCountAndEnsureDbIsOpen().query(query)
                } catch (throwable: Throwable) {
                    autoCloser.decrementCountAndScheduleClose()
                    throw throwable
                }
            return KeepAliveCursor(result, autoCloser)
        }

        override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
            val result =
                try {
                    autoCloser.incrementCountAndEnsureDbIsOpen().query(query, bindArgs)
                } catch (throwable: Throwable) {
                    autoCloser.decrementCountAndScheduleClose()
                    throw throwable
                }
            return KeepAliveCursor(result, autoCloser)
        }

        override fun query(query: SupportSQLiteQuery): Cursor {
            val result =
                try {
                    autoCloser.incrementCountAndEnsureDbIsOpen().query(query)
                } catch (throwable: Throwable) {
                    autoCloser.decrementCountAndScheduleClose()
                    throw throwable
                }
            return KeepAliveCursor(result, autoCloser)
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        override fun query(
            query: SupportSQLiteQuery,
            cancellationSignal: CancellationSignal?
        ): Cursor {
            val result =
                try {
                    autoCloser.incrementCountAndEnsureDbIsOpen().query(query, cancellationSignal)
                } catch (throwable: Throwable) {
                    autoCloser.decrementCountAndScheduleClose()
                    throw throwable
                }
            return KeepAliveCursor(result, autoCloser)
        }

        @Throws(SQLException::class)
        override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.insert(table, conflictAlgorithm, values)
            }
        }

        override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.delete(table, whereClause, whereArgs)
            }
        }

        override fun update(
            table: String,
            conflictAlgorithm: Int,
            values: ContentValues,
            whereClause: String?,
            whereArgs: Array<out Any?>?
        ): Int {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.update(table, conflictAlgorithm, values, whereClause, whereArgs)
            }
        }

        @Throws(SQLException::class)
        override fun execSQL(sql: String) {
            autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase -> db.execSQL(sql) }
        }

        @Throws(SQLException::class)
        override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
            autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.execSQL(sql, bindArgs)
            }
        }

        override val isReadOnly: Boolean
            get() = autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::isReadOnly)

        override val isOpen: Boolean
            get() {
                // Get the db without incrementing the reference cause we don't want to open
                // the db for an isOpen call.
                return autoCloser.delegateDatabase?.isOpen ?: return false
            }

        override fun needUpgrade(newVersion: Int): Boolean {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.needUpgrade(newVersion)
            }
        }

        override val path: String?
            get() = autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::path)

        override fun setLocale(locale: Locale) {
            autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.setLocale(locale)
            }
        }

        override fun setMaxSqlCacheSize(cacheSize: Int) {
            autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.setMaxSqlCacheSize(cacheSize)
            }
        }

        override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
            autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.setForeignKeyConstraintsEnabled(enabled)
            }
        }

        override fun enableWriteAheadLogging(): Boolean {
            throw UnsupportedOperationException(
                "Enable/disable write ahead logging on the " +
                    "OpenHelper instead of on the database directly."
            )
        }

        override fun disableWriteAheadLogging() {
            throw UnsupportedOperationException(
                "Enable/disable write ahead logging on the " +
                    "OpenHelper instead of on the database directly."
            )
        }

        override val isWriteAheadLoggingEnabled: Boolean
            get() =
                autoCloser.executeRefCountingFunction(
                    SupportSQLiteDatabase::isWriteAheadLoggingEnabled
                )

        override val attachedDbs: List<Pair<String, String>>?
            get() = autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::attachedDbs)

        override val isDatabaseIntegrityOk: Boolean
            get() =
                autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::isDatabaseIntegrityOk)

        @Throws(IOException::class)
        override fun close() {
            autoCloser.closeDatabaseIfOpen()
        }
    }

    /**
     * We need to keep the db alive until the cursor is closed, so we can't decrement our reference
     * count until the cursor is closed. The underlying database will not close until this cursor is
     * closed.
     */
    private class KeepAliveCursor(
        private val delegate: Cursor,
        private val autoCloser: AutoCloser
    ) : Cursor by delegate {
        // close is the only important/changed method here:
        override fun close() {
            delegate.close()
            autoCloser.decrementCountAndScheduleClose()
        }
    }

    /**
     * Since long-living statements are a normal use-case, auto-close does not have a keep-alive
     * statement, instead records SQL query and binding args and replicates on execution, opening
     * the database is necessary but not helding a ref count on it.
     */
    private class AutoClosingSupportSQLiteStatement(
        private val sql: String,
        private val autoCloser: AutoCloser
    ) : SupportSQLiteStatement {

        private var bindingTypes: IntArray = IntArray(0)
        private var longBindings: LongArray = LongArray(0)
        private var doubleBindings: DoubleArray = DoubleArray(0)
        private var stringBindings: Array<String?> = emptyArray()
        private var blobBindings: Array<ByteArray?> = emptyArray()

        override fun close() {
            // Not much to do here since we re-compile the statement each time.
            clearBindings()
        }

        override fun execute() {
            executeWithRefCount { statement: SupportSQLiteStatement -> statement.execute() }
        }

        override fun executeUpdateDelete(): Int {
            return executeWithRefCount { obj: SupportSQLiteStatement -> obj.executeUpdateDelete() }
        }

        override fun executeInsert(): Long {
            return executeWithRefCount { obj: SupportSQLiteStatement -> obj.executeInsert() }
        }

        override fun simpleQueryForLong(): Long {
            return executeWithRefCount { obj: SupportSQLiteStatement -> obj.simpleQueryForLong() }
        }

        override fun simpleQueryForString(): String? {
            return executeWithRefCount { obj: SupportSQLiteStatement -> obj.simpleQueryForString() }
        }

        private fun <T> executeWithRefCount(block: (SupportSQLiteStatement) -> T): T {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                val actualStatement = db.compileStatement(sql)
                bindTo(actualStatement)
                block(actualStatement)
            }
        }

        override fun bindNull(index: Int) {
            ensureCapacity(COLUMN_TYPE_NULL, index)
            bindingTypes[index] = COLUMN_TYPE_NULL
        }

        override fun bindLong(index: Int, value: Long) {
            ensureCapacity(COLUMN_TYPE_LONG, index)
            bindingTypes[index] = COLUMN_TYPE_LONG
            longBindings[index] = value
        }

        override fun bindDouble(index: Int, value: Double) {
            ensureCapacity(COLUMN_TYPE_DOUBLE, index)
            bindingTypes[index] = COLUMN_TYPE_DOUBLE
            doubleBindings[index] = value
        }

        override fun bindString(index: Int, value: String) {
            ensureCapacity(COLUMN_TYPE_STRING, index)
            bindingTypes[index] = COLUMN_TYPE_STRING
            stringBindings[index] = value
        }

        override fun bindBlob(index: Int, value: ByteArray) {
            ensureCapacity(COLUMN_TYPE_BLOB, index)
            bindingTypes[index] = COLUMN_TYPE_BLOB
            blobBindings[index] = value
        }

        override fun clearBindings() {
            bindingTypes = IntArray(0)
            longBindings = LongArray(0)
            doubleBindings = DoubleArray(0)
            stringBindings = emptyArray()
            blobBindings = emptyArray()
        }

        private fun ensureCapacity(columnType: Int, index: Int) {
            val requiredSize = index + 1
            if (bindingTypes.size < requiredSize) {
                bindingTypes = bindingTypes.copyOf(requiredSize)
            }
            when (columnType) {
                COLUMN_TYPE_LONG -> {
                    if (longBindings.size < requiredSize) {
                        longBindings = longBindings.copyOf(requiredSize)
                    }
                }
                COLUMN_TYPE_DOUBLE -> {
                    if (doubleBindings.size < requiredSize) {
                        doubleBindings = doubleBindings.copyOf(requiredSize)
                    }
                }
                COLUMN_TYPE_STRING -> {
                    if (stringBindings.size < requiredSize) {
                        stringBindings = stringBindings.copyOf(requiredSize)
                    }
                }
                COLUMN_TYPE_BLOB -> {
                    if (blobBindings.size < requiredSize) {
                        blobBindings = blobBindings.copyOf(requiredSize)
                    }
                }
            }
        }

        private fun bindTo(query: SupportSQLiteProgram) {
            for (index in 1 until bindingTypes.size) {
                when (bindingTypes[index]) {
                    COLUMN_TYPE_LONG -> query.bindLong(index, longBindings[index])
                    COLUMN_TYPE_DOUBLE -> query.bindDouble(index, doubleBindings[index])
                    COLUMN_TYPE_STRING -> query.bindString(index, stringBindings[index]!!)
                    COLUMN_TYPE_BLOB -> query.bindBlob(index, blobBindings[index]!!)
                    COLUMN_TYPE_NULL -> query.bindNull(index)
                }
            }
        }

        companion object {
            private const val COLUMN_TYPE_LONG = 1
            private const val COLUMN_TYPE_DOUBLE = 2
            private const val COLUMN_TYPE_STRING = 3
            private const val COLUMN_TYPE_BLOB = 4
            private const val COLUMN_TYPE_NULL = 5
        }
    }
}
