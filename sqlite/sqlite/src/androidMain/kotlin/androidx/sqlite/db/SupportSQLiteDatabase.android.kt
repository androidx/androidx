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
package androidx.sqlite.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import java.io.Closeable
import java.util.Locale

/**
 * A database abstraction which removes the framework dependency and allows swapping underlying sql
 * versions. It mimics the behavior of [android.database.sqlite.SQLiteDatabase]
 */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public interface SupportSQLiteDatabase : Closeable {
    /**
     * Compiles the given SQL statement.
     *
     * @param sql The sql query.
     * @return Compiled statement.
     */
    public fun compileStatement(sql: String): SupportSQLiteStatement

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     * Transactions can be nested. When the outer transaction is ended all of the work done in that
     * transaction and all of the nested transactions will be committed or rolled back. The changes
     * will be rolled back if any transaction is ended without being marked as clean (by calling
     * setTransactionSuccessful). Otherwise they will be committed.
     *
     * Here is the standard idiom for transactions:
     * ```
     *  db.beginTransaction()
     *  try {
     *      ...
     *      db.setTransactionSuccessful()
     *  } finally {
     *      db.endTransaction()
     *  }
     * ```
     */
    public fun beginTransaction()

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When the outer
     * transaction is ended all of the work done in that transaction and all of the nested
     * transactions will be committed or rolled back. The changes will be rolled back if any
     * transaction is ended without being marked as clean (by calling setTransactionSuccessful).
     * Otherwise they will be committed.
     *
     * Here is the standard idiom for transactions:
     * ```
     *  db.beginTransactionNonExclusive()
     *  try {
     *      ...
     *      db.setTransactionSuccessful()
     *  } finally {
     *      db.endTransaction()
     *  }
     *  ```
     */
    public fun beginTransactionNonExclusive()

    /**
     * Begins a transaction in DEFERRED mode, with the android-specific constraint that the
     * transaction is read-only. The database may not be modified inside a read-only transaction
     * otherwise a [android.database.sqlite.SQLiteDatabaseLockedException] might be thrown.
     *
     * Read-only transactions may run concurrently with other read-only transactions, and if they
     * database is in WAL mode, they may also run concurrently with IMMEDIATE or EXCLUSIVE
     * transactions.
     *
     * Transactions can be nested. However, the behavior of the transaction is not altered by nested
     * transactions. A nested transaction may be any of the three transaction types but if the
     * outermost type is read-only then nested transactions remain read-only, regardless of how they
     * are started.
     *
     * Here is the standard idiom for read-only transactions:
     * ```
     *   db.beginTransactionReadOnly();
     *   try {
     *     ...
     *   } finally {
     *     db.endTransaction();
     *   }
     * ```
     *
     * If the implementation does not support read-only transactions then the default implementation
     * delegates to [beginTransaction].
     */
    public fun beginTransactionReadOnly() {
        beginTransaction()
    }

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     * Transactions can be nested. When the outer transaction is ended all of the work done in that
     * transaction and all of the nested transactions will be committed or rolled back. The changes
     * will be rolled back if any transaction is ended without being marked as clean (by calling
     * setTransactionSuccessful). Otherwise they will be committed.
     *
     * Here is the standard idiom for transactions:
     * ```
     *  db.beginTransactionWithListener(listener)
     *  try {
     *      ...
     *      db.setTransactionSuccessful()
     *  } finally {
     *      db.endTransaction()
     *  }
     * ```
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     *   commits, or is rolled back, either explicitly or by a call to [yieldIfContendedSafely].
     */
    public fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener)

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When the outer
     * transaction is ended all of the work done in that transaction and all of the nested
     * transactions will be committed or rolled back. The changes will be rolled back if any
     * transaction is ended without being marked as clean (by calling setTransactionSuccessful).
     * Otherwise they will be committed.
     *
     * Here is the standard idiom for transactions:
     * ```
     *  db.beginTransactionWithListenerNonExclusive(listener)
     *  try {
     *      ...
     *      db.setTransactionSuccessful()
     *  } finally {
     *      db.endTransaction()
     *  }
     * ```
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     *   commits, or is rolled back, either explicitly or by a call to [yieldIfContendedSafely].
     */
    public fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener
    )

    /**
     * Begins a transaction in read-only mode with a {@link SQLiteTransactionListener} listener. The
     * database may not be modified inside a read-only transaction otherwise a
     * [android.database.sqlite.SQLiteDatabaseLockedException] might be thrown.
     *
     * Transactions can be nested. However, the behavior of the transaction is not altered by nested
     * transactions. A nested transaction may be any of the three transaction types but if the
     * outermost type is read-only then nested transactions remain read-only, regardless of how they
     * are started.
     *
     * Here is the standard idiom for read-only transactions:
     * ```
     *   db.beginTransactionWightListenerReadOnly(listener);
     *   try {
     *     ...
     *   } finally {
     *     db.endTransaction();
     *   }
     * ```
     *
     * If the implementation does not support read-only transactions then the default implementation
     * delegates to [beginTransactionWithListener].
     */
    @Suppress("ExecutorRegistration")
    public fun beginTransactionWithListenerReadOnly(
        transactionListener: SQLiteTransactionListener
    ) {
        beginTransactionWithListener(transactionListener)
    }

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    public fun endTransaction()

    /**
     * Marks the current transaction as successful. Do not do any more database work between calling
     * this and calling endTransaction. Do as little non-database work as possible in that situation
     * too. If any errors are encountered between this and endTransaction the transaction will still
     * be committed.
     *
     * @throws IllegalStateException if the current thread is not in a transaction or the
     *   transaction is already marked as successful.
     */
    public fun setTransactionSuccessful()

    /**
     * Returns true if the current thread has a transaction pending.
     *
     * @return True if the current thread is in a transaction.
     */
    public fun inTransaction(): Boolean

    /**
     * True if the current thread is holding an active connection to the database.
     *
     * The name of this method comes from a time when having an active connection to the database
     * meant that the thread was holding an actual lock on the database. Nowadays, there is no
     * longer a true "database lock" although threads may block if they cannot acquire a database
     * connection to perform a particular operation.
     */
    public val isDbLockedByCurrentThread: Boolean

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     *
     * @return true if the transaction was yielded
     */
    public fun yieldIfContendedSafely(): Boolean

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     *
     * @param sleepAfterYieldDelayMillis if > 0, sleep this long before starting a new transaction
     *   if the lock was actually yielded. This will allow other background threads to make some
     *   more progress than they would if we started the transaction immediately.
     * @return true if the transaction was yielded
     */
    public fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean

    /** Is true if [execPerConnectionSQL] is supported by the implementation. */
    @get:Suppress("AcronymName") // To keep consistency with framework method name.
    public val isExecPerConnectionSQLSupported: Boolean
        get() = false

    /**
     * Execute the given SQL statement on all connections to this database.
     *
     * This statement will be immediately executed on all existing connections, and will be
     * automatically executed on all future connections.
     *
     * Some example usages are changes like `PRAGMA trusted_schema=OFF` or functions like `SELECT
     * icu_load_collation()`. If you execute these statements using [execSQL] then they will only
     * apply to a single database connection; using this method will ensure that they are uniformly
     * applied to all current and future connections.
     *
     * An implementation of [SupportSQLiteDatabase] might not support this operation. Use
     * [isExecPerConnectionSQLSupported] to check if this operation is supported before calling this
     * method.
     *
     * @param sql The SQL statement to be executed. Multiple statements separated by semicolons are
     *   not supported.
     * @param bindArgs The arguments that should be bound to the SQL statement.
     * @throws UnsupportedOperationException if this operation is not supported. To check if it
     *   supported use [isExecPerConnectionSQLSupported]
     */
    @Suppress("AcronymName") // To keep consistency with framework method name.
    public fun execPerConnectionSQL(
        sql: String,
        @SuppressLint("ArrayReturn") bindArgs: Array<out Any?>?
    ) {
        throw UnsupportedOperationException()
    }

    /** The database version. */
    public var version: Int

    /** The maximum size the database may grow to. */
    public val maximumSize: Long

    /**
     * Sets the maximum size the database will grow to. The maximum size cannot be set below the
     * current size.
     *
     * @param numBytes the maximum database size, in bytes
     * @return the new maximum database size
     */
    public fun setMaximumSize(numBytes: Long): Long

    /**
     * The current database page size, in bytes.
     *
     * The page size must be a power of two. This method does not work if any data has been written
     * to the database file, and must be called right after the database has been created.
     */
    public var pageSize: Long

    /**
     * Runs the given query on the database. If you would like to have typed bind arguments, use
     * [query].
     *
     * @param query The SQL query that includes the query and can bind into a given compiled
     *   program.
     * @return A [Cursor] object, which is positioned before the first entry. Note that [Cursor]s
     *   are not synchronized, see the documentation for more details.
     */
    public fun query(query: String): Cursor

    /**
     * Runs the given query on the database. If you would like to have bind arguments, use [query].
     *
     * @param query The SQL query that includes the query and can bind into a given compiled
     *   program.
     * @param bindArgs The query arguments to bind.
     * @return A [Cursor] object, which is positioned before the first entry. Note that [Cursor]s
     *   are not synchronized, see the documentation for more details.
     */
    public fun query(query: String, bindArgs: Array<out Any?>): Cursor

    /**
     * Runs the given query on the database.
     *
     * This class allows using type safe sql program bindings while running queries.
     *
     * @param query The [SimpleSQLiteQuery] query that includes the query and can bind into a given
     *   compiled program.
     * @return A [Cursor] object, which is positioned before the first entry. Note that [Cursor]s
     *   are not synchronized, see the documentation for more details.
     */
    public fun query(query: SupportSQLiteQuery): Cursor

    /**
     * Runs the given query on the database.
     *
     * This class allows using type safe sql program bindings while running queries.
     *
     * @param query The SQL query that includes the query and can bind into a given compiled
     *   program.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none. If
     *   the operation is canceled, then [androidx.core.os.OperationCanceledException] will be
     *   thrown when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that [Cursor]s
     *   are not synchronized, see the documentation for more details.
     */
    public fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor

    /**
     * Convenience method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param values this map contains the initial column values for the row. The keys should be the
     *   column names and the values the column values
     * @param conflictAlgorithm for insert conflict resolver. One of
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_NONE],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_ROLLBACK],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE].
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     * @throws SQLException If the insert fails
     */
    @Throws(SQLException::class)
    public fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting. Passing null will delete
     *   all rows.
     * @param whereArgs You may include ?s in the where clause, which will be replaced by the values
     *   from whereArgs. The values will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all
     *   rows and get a count pass "1" as the whereClause.
     */
    public fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param conflictAlgorithm for update conflict resolver. One of
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_NONE],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_ROLLBACK],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE],
     *   [android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE].
     * @param values a map from column names to new column values. null is a valid value that will
     *   be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating. Passing null will update
     *   all rows.
     * @param whereArgs You may include ?s in the where clause, which will be replaced by the values
     *   from whereArgs. The values will be bound as Strings.
     * @return the number of rows affected
     */
    public fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?
    ): Int

    /**
     * Execute a single SQL statement that does not return any data.
     *
     * When using [enableWriteAheadLogging], journal_mode is automatically managed by this class.
     * So, do not set journal_mode using "PRAGMA journal_mode" statement if your app is using
     * [enableWriteAheadLogging]
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     *   not supported.
     * @throws SQLException if the SQL string is invalid
     */
    @Suppress("AcronymName") // SQL is a known term and should remain capitalized
    @Throws(SQLException::class)
    public fun execSQL(sql: String)

    /**
     * Execute a single SQL statement that does not return any data.
     *
     * When using [enableWriteAheadLogging], journal_mode is automatically managed by this class.
     * So, do not set journal_mode using "PRAGMA journal_mode" statement if your app is using
     * [enableWriteAheadLogging]
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     *   not supported.
     * @param bindArgs only byte[], String, Long and Double are supported in selectionArgs.
     * @throws SQLException if the SQL string is invalid
     */
    @Suppress("AcronymName") // SQL is a known term and should remain capitalized
    @Throws(SQLException::class)
    public fun execSQL(sql: String, bindArgs: Array<out Any?>)

    /** Is true if the database is opened as read only. */
    public val isReadOnly: Boolean

    /** Is true if the database is currently open. */
    public val isOpen: Boolean

    /**
     * Returns true if the new version code is greater than the current database version.
     *
     * @param newVersion The new version code.
     * @return True if the new version code is greater than the current database version.
     */
    public fun needUpgrade(newVersion: Int): Boolean

    /** The path to the database file. */
    public val path: String?

    /**
     * Sets the locale for this database. Does nothing if this database has the
     * [android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS] flag set or was opened read
     * only.
     *
     * @param locale The new locale.
     * @throws SQLException if the locale could not be set. The most common reason for this is that
     *   there is no collator available for the locale you requested. In this case the database
     *   remains unchanged.
     */
    public fun setLocale(locale: Locale)

    /**
     * Sets the maximum size of the prepared-statement cache for this database. (size of the cache =
     * number of compiled-sql-statements stored in the cache).
     *
     * Maximum cache size can ONLY be increased from its current size (default = 10). If this method
     * is called with smaller size than the current maximum value, then IllegalStateException is
     * thrown.
     *
     * This method is thread-safe.
     *
     * @param cacheSize the size of the cache. can be (0 to
     *   [android.database.sqlite.SQLiteDatabase.MAX_SQL_CACHE_SIZE])
     * @throws IllegalStateException if input cacheSize is over the max.
     *   [android.database.sqlite.SQLiteDatabase.MAX_SQL_CACHE_SIZE].
     */
    public fun setMaxSqlCacheSize(cacheSize: Int)

    /**
     * Sets whether foreign key constraints are enabled for the database.
     *
     * By default, foreign key constraints are not enforced by the database. This method allows an
     * application to enable foreign key constraints. It must be called each time the database is
     * opened to ensure that foreign key constraints are enabled for the session.
     *
     * A good time to call this method is right after calling `#openOrCreateDatabase` or in the
     * [SupportSQLiteOpenHelper.Callback.onConfigure] callback.
     *
     * When foreign key constraints are disabled, the database does not check whether changes to the
     * database will violate foreign key constraints. Likewise, when foreign key constraints are
     * disabled, the database will not execute cascade delete or update triggers. As a result, it is
     * possible for the database state to become inconsistent. To perform a database integrity
     * check, call [isDatabaseIntegrityOk].
     *
     * This method must not be called while a transaction is in progress.
     *
     * See also [SQLite Foreign Key Constraints](http://sqlite.org/foreignkeys.html) for more
     * details about foreign key constraint support.
     *
     * @param enabled True to enable foreign key constraints, false to disable them.
     * @throws IllegalStateException if the are transactions is in progress when this method is
     *   called.
     */
    public fun setForeignKeyConstraintsEnabled(enabled: Boolean)

    /**
     * This method enables parallel execution of queries from multiple threads on the same database.
     * It does this by opening multiple connections to the database and using a different database
     * connection for each query. The database journal mode is also changed to enable writes to
     * proceed concurrently with reads.
     *
     * When write-ahead logging is not enabled (the default), it is not possible for reads and
     * writes to occur on the database at the same time. Before modifying the database, the writer
     * implicitly acquires an exclusive lock on the database which prevents readers from accessing
     * the database until the write is completed.
     *
     * In contrast, when write-ahead logging is enabled (by calling this method), write operations
     * occur in a separate log file which allows reads to proceed concurrently. While a write is in
     * progress, readers on other threads will perceive the state of the database as it was before
     * the write began. When the write completes, readers on other threads will then perceive the
     * new state of the database.
     *
     * It is a good idea to enable write-ahead logging whenever a database will be concurrently
     * accessed and modified by multiple threads at the same time. However, write-ahead logging uses
     * significantly more memory than ordinary journaling because there are multiple connections to
     * the same database. So if a database will only be used by a single thread, or if optimizing
     * concurrency is not very important, then write-ahead logging should be disabled.
     *
     * After calling this method, execution of queries in parallel is enabled as long as the
     * database remains open. To disable execution of queries in parallel, either call
     * [disableWriteAheadLogging] or close the database and reopen it.
     *
     * The maximum number of connections used to execute queries in parallel is dependent upon the
     * device memory and possibly other properties.
     *
     * If a query is part of a transaction, then it is executed on the same database handle the
     * transaction was begun.
     *
     * Writers should use [beginTransactionNonExclusive] or
     * [beginTransactionWithListenerNonExclusive] to start a transaction. Non-exclusive mode allows
     * database file to be in readable by other threads executing queries.
     *
     * If the database has any attached databases, then execution of queries in parallel is NOT
     * possible. Likewise, write-ahead logging is not supported for read-only databases or memory
     * databases. In such cases, `enableWriteAheadLogging` returns false.
     *
     * The best way to enable write-ahead logging is to pass the
     * [android.database.sqlite.SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING] flag to
     * [android.database.sqlite.SQLiteDatabase.openDatabase]. This is more efficient than calling
     *
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
     * myDatabaseErrorHandler) db.enableWriteAheadLogging()
     *
     * Another way to enable write-ahead logging is to call `enableWriteAheadLogging` after opening
     * the database.
     *
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY, myDatabaseErrorHandler) db.enableWriteAheadLogging()
     *
     * See also [SQLite Write-Ahead Logging](http://sqlite.org/wal.html) for more details about how
     * write-ahead logging works.
     *
     * @return True if write-ahead logging is enabled.
     * @throws IllegalStateException if there are transactions in progress at the time this method
     *   is called. WAL mode can only be changed when there are no transactions in progress.
     */
    public fun enableWriteAheadLogging(): Boolean

    /**
     * This method disables the features enabled by [enableWriteAheadLogging].
     *
     * @throws IllegalStateException if there are transactions in progress at the time this method
     *   is called. WAL mode can only be changed when there are no transactions in progress.
     */
    public fun disableWriteAheadLogging()

    /** Is true if write-ahead logging has been enabled for this database. */
    public val isWriteAheadLoggingEnabled: Boolean

    /**
     * The list of full path names of all attached databases including the main database by
     * executing 'pragma database_list' on the database.
     */
    @get:Suppress("NullableCollection") public val attachedDbs: List<Pair<String, String>>?

    /**
     * Is true if the given database (and all its attached databases) pass integrity_check, false
     * otherwise.
     */
    public val isDatabaseIntegrityOk: Boolean
}
