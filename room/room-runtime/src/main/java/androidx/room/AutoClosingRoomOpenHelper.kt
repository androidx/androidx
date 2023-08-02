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
package androidx.room

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteTransactionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SupportSQLiteCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.io.IOException
import java.util.Locale

/**
 * A SupportSQLiteOpenHelper that has auto close enabled for database connections.
 */
internal class AutoClosingRoomOpenHelper(
    override val delegate: SupportSQLiteOpenHelper,
    @JvmField
    internal val autoCloser: AutoCloser
) : SupportSQLiteOpenHelper by delegate, DelegatingOpenHelper {
    private val autoClosingDb: AutoClosingSupportSQLiteDatabase

    init {
        autoCloser.init(delegate)
        autoClosingDb = AutoClosingSupportSQLiteDatabase(
            autoCloser
        )
    }

    @get:RequiresApi(api = Build.VERSION_CODES.N)
    override val writableDatabase: SupportSQLiteDatabase
        get() {
            autoClosingDb.pokeOpen()
            return autoClosingDb
        }

    @get:RequiresApi(api = Build.VERSION_CODES.N)
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

    /**
     * SupportSQLiteDatabase that also keeps refcounts and autocloses the database
     */
    internal class AutoClosingSupportSQLiteDatabase(
        private val autoCloser: AutoCloser
    ) : SupportSQLiteDatabase {
        fun pokeOpen() {
            autoCloser.executeRefCountingFunction<Any?> { null }
        }

        override fun compileStatement(sql: String): SupportSQLiteStatement {
            return AutoClosingSupportSqliteStatement(sql, autoCloser)
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
            checkNotNull(autoCloser.delegateDatabase) {
                "End transaction called but delegateDb is null"
            }
            try {
                autoCloser.delegateDatabase!!.endTransaction()
            } finally {
                autoCloser.decrementCountAndScheduleClose()
            }
        }

        override fun setTransactionSuccessful() {
            autoCloser.delegateDatabase?.setTransactionSuccessful() ?: error(
                "setTransactionSuccessful called but delegateDb is null"
            )
        }

        override fun inTransaction(): Boolean {
            return if (autoCloser.delegateDatabase == null) {
                false
            } else {
                autoCloser.executeRefCountingFunction(SupportSQLiteDatabase::inTransaction)
            }
        }

        override val isDbLockedByCurrentThread: Boolean
            get() = if (autoCloser.delegateDatabase == null) {
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
            get() = autoCloser.executeRefCountingFunction(
                SupportSQLiteDatabase::version
            )
            set(version) {
                autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                    db.version = version
                    null
                }
            }

        override val maximumSize: Long
            get() = autoCloser.executeRefCountingFunction(
                SupportSQLiteDatabase::maximumSize
            )

        override fun setMaximumSize(numBytes: Long): Long {
            return autoCloser.executeRefCountingFunction {
                    db: SupportSQLiteDatabase -> db.setMaximumSize(numBytes)
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
            val result = try {
                autoCloser.incrementCountAndEnsureDbIsOpen().query(query)
            } catch (throwable: Throwable) {
                autoCloser.decrementCountAndScheduleClose()
                throw throwable
            }
            return KeepAliveCursor(result, autoCloser)
        }

        override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
            val result = try {
                autoCloser.incrementCountAndEnsureDbIsOpen().query(query, bindArgs)
            } catch (throwable: Throwable) {
                autoCloser.decrementCountAndScheduleClose()
                throw throwable
            }
            return KeepAliveCursor(result, autoCloser)
        }

        override fun query(query: SupportSQLiteQuery): Cursor {
            val result = try {
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
            val result = try {
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
                db.insert(
                    table, conflictAlgorithm,
                    values
                )
            }
        }

        override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.delete(
                    table,
                    whereClause,
                    whereArgs
                )
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
                db.update(
                    table, conflictAlgorithm,
                    values, whereClause, whereArgs
                )
            }
        }

        @Throws(SQLException::class)
        override fun execSQL(sql: String) {
            autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                db.execSQL(sql)
                null
            }
        }

        @Throws(SQLException::class)
        override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
            autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                db.execSQL(sql, bindArgs)
                null
            }
        }

        override val isReadOnly: Boolean
            get() = autoCloser.executeRefCountingFunction { obj: SupportSQLiteDatabase ->
                obj.isReadOnly
            }

        override val isOpen: Boolean
            get() {
                // Get the db without incrementing the reference cause we don't want to open
                // the db for an isOpen call.
                val localDelegate = autoCloser.delegateDatabase ?: return false
                return localDelegate.isOpen
            }

        override fun needUpgrade(newVersion: Int): Boolean {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                db.needUpgrade(
                    newVersion
                )
            }
        }

        override val path: String?
            get() = autoCloser.executeRefCountingFunction { obj: SupportSQLiteDatabase ->
                obj.path
            }

        override fun setLocale(locale: Locale) {
            autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                db.setLocale(locale)
                null
            }
        }

        override fun setMaxSqlCacheSize(cacheSize: Int) {
            autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                db.setMaxSqlCacheSize(cacheSize)
                null
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
            autoCloser.executeRefCountingFunction<Any?> { db: SupportSQLiteDatabase ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    db.setForeignKeyConstraintsEnabled(enabled)
                }
                null
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

        @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        override val isWriteAheadLoggingEnabled: Boolean
            get() = autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    return@executeRefCountingFunction db.isWriteAheadLoggingEnabled
                }
                false
            }

        override val attachedDbs: List<Pair<String, String>>?
            get() = autoCloser.executeRefCountingFunction {
                    obj: SupportSQLiteDatabase -> obj.attachedDbs
            }

        override val isDatabaseIntegrityOk: Boolean
            get() = autoCloser.executeRefCountingFunction {
                    obj: SupportSQLiteDatabase -> obj.isDatabaseIntegrityOk
            }

        @Throws(IOException::class)
        override fun close() {
            autoCloser.closeDatabaseIfOpen()
        }
    }

    /**
     * We need to keep the db alive until the cursor is closed, so we can't decrement our
     * reference count until the cursor is closed. The underlying database will not close until
     * this cursor is closed.
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

        @RequiresApi(api = Build.VERSION_CODES.Q)
        override fun setNotificationUris(
            cr: ContentResolver,
            uris: List<Uri>
        ) {
            SupportSQLiteCompat.Api29Impl.setNotificationUris(delegate, cr, uris)
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        override fun getNotificationUri(): Uri {
            return SupportSQLiteCompat.Api19Impl.getNotificationUri(delegate)
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        override fun getNotificationUris(): List<Uri> {
            return SupportSQLiteCompat.Api29Impl.getNotificationUris(delegate)
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        override fun setExtras(extras: Bundle) {
            SupportSQLiteCompat.Api23Impl.setExtras(delegate, extras)
        }
    }

    /**
     * We can't close our db if the SupportSqliteStatement is open.
     *
     * Each of these that are created need to be registered with RefCounter.
     *
     * On auto-close, RefCounter needs to close each of these before closing the db that these
     * were constructed from.
     *
     * Each of the methods here need to get
     */
    // TODO(rohitsat) cache the prepared statement... I'm not sure what the performance implications
    // are for the way it's done here, but caching the prepared statement would definitely be more
    // complicated since we need to invalidate any of the PreparedStatements that were created
    // with this db
    private class AutoClosingSupportSqliteStatement(
        private val sql: String,
        private val autoCloser: AutoCloser
    ) : SupportSQLiteStatement {
        private val binds = ArrayList<Any?>()
        private fun <T> executeSqliteStatementWithRefCount(
            block: (SupportSQLiteStatement) -> T
        ): T {
            return autoCloser.executeRefCountingFunction { db: SupportSQLiteDatabase ->
                val statement: SupportSQLiteStatement = db.compileStatement(sql)
                doBinds(statement)
                block(statement)
            }
        }

        private fun doBinds(supportSQLiteStatement: SupportSQLiteStatement) {
            // Replay the binds
            binds.forEachIndexed { i, _ ->
                val bindIndex = i + 1 // Bind indices are 1 based so we start at 1 not 0
                when (val bind = binds[i]) {
                    null -> {
                        supportSQLiteStatement.bindNull(bindIndex)
                    }
                    is Long -> {
                        supportSQLiteStatement.bindLong(bindIndex, bind)
                    }
                    is Double -> {
                        supportSQLiteStatement.bindDouble(bindIndex, bind)
                    }
                    is String -> {
                        supportSQLiteStatement.bindString(bindIndex, bind)
                    }
                    is ByteArray -> {
                        supportSQLiteStatement.bindBlob(bindIndex, bind)
                    }
                }
            }
        }

        private fun saveBinds(bindIndex: Int, value: Any?) {
            val index = bindIndex - 1
            if (index >= binds.size) {
                // Add null entries to the list until we have the desired # of indices
                for (i in binds.size..index) {
                    binds.add(null)
                }
            }
            binds[index] = value
        }

        @Throws(IOException::class)
        override fun close() {
            // Nothing to do here since we re-compile the statement each time.
        }

        override fun execute() {
            executeSqliteStatementWithRefCount<Any?> { statement: SupportSQLiteStatement ->
                statement.execute()
                null
            }
        }

        override fun executeUpdateDelete(): Int {
            return executeSqliteStatementWithRefCount { obj: SupportSQLiteStatement ->
                obj.executeUpdateDelete() }
        }

        override fun executeInsert(): Long {
            return executeSqliteStatementWithRefCount { obj: SupportSQLiteStatement ->
                obj.executeInsert()
            }
        }

        override fun simpleQueryForLong(): Long {
            return executeSqliteStatementWithRefCount { obj: SupportSQLiteStatement ->
                obj.simpleQueryForLong()
            }
        }

        override fun simpleQueryForString(): String? {
            return executeSqliteStatementWithRefCount { obj: SupportSQLiteStatement ->
                obj.simpleQueryForString()
            }
        }

        override fun bindNull(index: Int) {
            saveBinds(index, null)
        }

        override fun bindLong(index: Int, value: Long) {
            saveBinds(index, value)
        }

        override fun bindDouble(index: Int, value: Double) {
            saveBinds(index, value)
        }

        override fun bindString(index: Int, value: String) {
            saveBinds(index, value)
        }

        override fun bindBlob(index: Int, value: ByteArray) {
            saveBinds(index, value)
        }

        override fun clearBindings() {
            binds.clear()
        }
    }
}
