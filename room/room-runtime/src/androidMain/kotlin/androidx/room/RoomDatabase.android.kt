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
@file:JvmMultifileClass
@file:JvmName("RoomDatabaseKt")

package androidx.room

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.CancellationSignal
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.Room.LOG_TAG
import androidx.room.concurrent.CloseBarrier
import androidx.room.coroutines.runBlockingUninterruptible
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.support.AutoCloser
import androidx.room.support.AutoClosingRoomOpenHelper
import androidx.room.support.AutoClosingRoomOpenHelperFactory
import androidx.room.support.PrePackagedCopyOpenHelper
import androidx.room.support.PrePackagedCopyOpenHelperFactory
import androidx.room.support.QueryInterceptorOpenHelperFactory
import androidx.room.util.contains as containsCommon
import androidx.room.util.findAndInstantiateDatabaseImpl
import androidx.room.util.findMigrationPath as findMigrationPathExt
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.driver.SupportSQLiteConnection
import java.io.File
import java.io.InputStream
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Base class for all Room databases. All classes that are annotated with [Database] must extend
 * this class.
 *
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using [Dao] classes.
 *
 * @constructor You cannot create an instance of a database, instead, you should acquire it via
 *   [#Room.databaseBuilder] or [#Room.inMemoryDatabaseBuilder].
 * @see Database
 */
public actual abstract class RoomDatabase {
    @Volatile
    @JvmField
    @Deprecated(
        message = "This property is always null and will be removed in a future version.",
        level = DeprecationLevel.ERROR,
    )
    protected var mDatabase: SupportSQLiteDatabase? = null

    private lateinit var configuration: DatabaseConfiguration
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var transactionContext: CoroutineContext

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val path: String?
        get() = configuration.name?.let { configuration.context.getDatabasePath(it).path }

    /** The Executor in use by this database for async queries. */
    public open val queryExecutor: Executor
        get() = internalQueryExecutor

    private lateinit var internalQueryExecutor: Executor

    /** The Executor in use by this database for async transactions. */
    public open val transactionExecutor: Executor
        get() = internalTransactionExecutor

    private lateinit var internalTransactionExecutor: Executor

    /**
     * The SQLite open helper used by this database.
     *
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    // TODO(b/408062492): @Deprecate with replace to wrapper
    public open val openHelper: SupportSQLiteOpenHelper
        get() =
            connectionManager.supportOpenHelper
                ?: error(
                    "Cannot return a SupportSQLiteOpenHelper since no " +
                        "SupportSQLiteOpenHelper.Factory was configured with Room."
                )

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val driver: SQLiteDriver
        get() = configuration.sqliteDriver ?: error("No SQLiteDriver was configured with Room.")

    private lateinit var connectionManager: RoomConnectionManager

    /**
     * The invalidation tracker for this database.
     *
     * You can use the invalidation tracker to get notified when certain tables in the database are
     * modified.
     *
     * @return The invalidation tracker for the database.
     */
    public actual open val invalidationTracker: InvalidationTracker
        get() = internalTracker

    private lateinit var internalTracker: InvalidationTracker

    /**
     * A barrier that prevents the database from closing while the [InvalidationTracker] is using
     * the database asynchronously.
     *
     * @return The barrier for [close].
     */
    internal actual val closeBarrier = CloseBarrier(::onClosed)

    private var allowMainThreadQueries = false

    @JvmField
    @Deprecated(
        message = "This property is always null and will be removed in a future version.",
        level = DeprecationLevel.ERROR,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected var mCallbacks: List<Callback>? = null

    private var autoCloser: AutoCloser? = null

    /**
     * Suspending transaction context of the current thread containing a [TransactionElement].
     *
     * This is set on threads that are used to dispatch coroutines within a suspending database
     * transaction. It can also be set by the SupportSQLite wrapper when there is an active
     * compatibility transaction so DAO functions can interop with the active transaction.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val suspendingTransactionContext: ThreadLocal<CoroutineContext> =
        ThreadLocal<CoroutineContext>()

    private val isThreadInSuspendingTransaction: Boolean
        get() = suspendingTransactionContext.get()?.get(TransactionElement) != null

    private val typeConverters: MutableMap<KClass<*>, Any> = mutableMapOf()

    internal var useTempTrackingTable: Boolean = true

    /**
     * Gets the instance of the given Type Converter.
     *
     * @param klass The Type Converter class.
     * @param T The type of the expected Type Converter subclass.
     * @return An instance of T if it is provided in the builder.
     */
    @Deprecated("No longer called by generated implementation")
    @Suppress("UNCHECKED_CAST")
    public open fun <T : Any> getTypeConverter(klass: Class<T>): T? {
        return typeConverters[klass.kotlin] as T?
    }

    /**
     * Gets the instance of the given type converter class.
     *
     * This method should only be called by the generated DAO implementations.
     *
     * @param klass The Type Converter class.
     * @param T The type of the expected Type Converter subclass.
     * @return An instance of T.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    @Suppress("UNCHECKED_CAST")
    public actual fun <T : Any> getTypeConverter(klass: KClass<T>): T {
        return typeConverters[klass] as T
    }

    /**
     * Adds a provided type converter to be used in the database DAOs.
     *
     * @param kclass the class of the type converter
     * @param converter an instance of the converter
     */
    internal actual fun addTypeConverter(kclass: KClass<*>, converter: Any) {
        typeConverters[kclass] = converter
    }

    /**
     * Called by Room when it is initialized.
     *
     * @param configuration The database configuration.
     * @throws IllegalArgumentException if initialization fails.
     */
    @CallSuper
    public actual open fun init(configuration: DatabaseConfiguration) {
        this.configuration = configuration
        useTempTrackingTable = configuration.useTempTrackingTable

        connectionManager = createConnectionManager(configuration)
        internalTracker = createInvalidationTracker()
        validateAutoMigrations(configuration)
        validateTypeConverters(configuration)

        if (configuration.queryCoroutineContext != null) {
            // For backwards compatibility with internals not converted to Coroutines, use the
            // provided dispatcher as executor.
            val dispatcher =
                configuration.queryCoroutineContext[ContinuationInterceptor] as CoroutineDispatcher
            internalQueryExecutor = dispatcher.asExecutor()
            internalTransactionExecutor = TransactionExecutor(internalQueryExecutor)
            // For Room's coroutine scope, we use the provided context but add a SupervisorJob that
            // is tied to the given Job (if any).
            val parentJob = configuration.queryCoroutineContext[Job]
            coroutineScope =
                CoroutineScope(configuration.queryCoroutineContext + SupervisorJob(parentJob))
            transactionContext =
                if (inCompatibilityMode()) {
                    // To prevent starvation due to primary connection blocking in
                    // SupportSQLiteDatabase a limited dispatcher is used for transactions.
                    @OptIn(ExperimentalCoroutinesApi::class) // For limitedParallelism(1)
                    coroutineScope.coroutineContext + dispatcher.limitedParallelism(1)
                } else {
                    // When a SQLiteDriver is provided a suspending connection pool is used and
                    // there is no reason to limit parallelism.
                    coroutineScope.coroutineContext
                }
        } else {
            internalQueryExecutor = configuration.queryExecutor
            internalTransactionExecutor = TransactionExecutor(configuration.transactionExecutor)
            // For Room's coroutine scope, we use the provided executor as dispatcher along with a
            // SupervisorJob.
            coroutineScope =
                CoroutineScope(internalQueryExecutor.asCoroutineDispatcher() + SupervisorJob())
            transactionContext =
                coroutineScope.coroutineContext +
                    internalTransactionExecutor.asCoroutineDispatcher()
        }

        allowMainThreadQueries = configuration.allowMainThreadQueries

        // Configure PrePackagedCopyOpenHelper if it is available
        unwrapOpenHelper<PrePackagedCopyOpenHelper>(connectionManager.supportOpenHelper)
            ?.setDatabaseConfiguration(configuration)

        // Configure AutoClosingRoomOpenHelper if it is available
        unwrapOpenHelper<AutoClosingRoomOpenHelper>(connectionManager.supportOpenHelper)?.let {
            autoCloser = it.autoCloser
            it.autoCloser.initCoroutineScope(coroutineScope)
            invalidationTracker.setAutoCloser(it.autoCloser)
        }

        // Configure multi-instance invalidation, if enabled
        if (configuration.multiInstanceInvalidationServiceIntent != null) {
            requireNotNull(configuration.name)
            invalidationTracker.initMultiInstanceInvalidation(
                configuration.context,
                configuration.name,
                configuration.multiInstanceInvalidationServiceIntent,
            )
        }
    }

    /**
     * Creates a connection manager to manage database connection. Note that this method is called
     * when the [RoomDatabase] is initialized.
     *
     * @return A new connection manager.
     */
    internal actual fun createConnectionManager(
        configuration: DatabaseConfiguration
    ): RoomConnectionManager {
        val openDelegate =
            try {
                createOpenDelegate() as RoomOpenDelegate
            } catch (ex: NotImplementedError) {
                null
            }
        // If createOpenDelegate() is not implemented then the database implementation was
        // generated with an older compiler, we are force to create a connection manager
        // using the SupportSQLiteOpenHelper returned from createOpenHelper() with the
        // deprecated RoomOpenHelper installed.
        return if (openDelegate == null) {
            @Suppress("DEPRECATION")
            RoomConnectionManager(
                config = configuration,
                supportOpenHelperFactory = { config -> createOpenHelper(config) },
                transactionWrapper = ::compatTransactionCoroutineExecute,
            )
        } else {
            RoomConnectionManager(
                config = configuration,
                openDelegate = openDelegate,
                transactionWrapper = ::compatTransactionCoroutineExecute,
            )
        }
    }

    /**
     * Returns a list of [Migration] of a database that have been automatically generated.
     *
     * @param autoMigrationSpecs
     * @return A list of migration instances each of which is a generated autoMigration
     */
    @Deprecated("No longer implemented by generated")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    @JvmSuppressWildcards // Suppress wildcards due to generated Java code
    public open fun getAutoMigrations(
        autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return emptyList()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public actual open fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        // For backwards compatibility when newer runtime is used with older generated code,
        // call the Java version of getAutoMigrations()
        val javaClassesMap = autoMigrationSpecs.mapKeys { it.key.java }
        @Suppress("DEPRECATION")
        return getAutoMigrations(javaClassesMap)
    }

    /**
     * Unwraps (delegating) open helpers until it finds [T], otherwise returns null.
     *
     * @param openHelper the open helper to search through
     * @param T the type of open helper type to search for
     * @return the instance of [T], otherwise null
     */
    private inline fun <reified T : SupportSQLiteOpenHelper> unwrapOpenHelper(
        openHelper: SupportSQLiteOpenHelper?
    ): T? {
        if (openHelper == null) {
            return null
        }
        var current: SupportSQLiteOpenHelper = openHelper
        while (true) {
            if (current is T) {
                return current
            }
            if (current is DelegatingOpenHelper) {
                current = current.delegate
            } else {
                break
            }
        }
        return null
    }

    /**
     * Creates the open helper to access the database. Generated class already implements this
     * method. Note that this method is called when the RoomDatabase is initialized.
     *
     * @param config The configuration of the Room database.
     * @return A new SupportSQLiteOpenHelper to be used while connecting to the database.
     * @throws NotImplementedError by default
     */
    @Deprecated("No longer implemented by generated")
    protected open fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        throw NotImplementedError()
    }

    /**
     * Creates a delegate to configure and initialize the database when it is being opened. An
     * implementation of this function is generated by the Room processor. Note that this method is
     * called when the [RoomDatabase] is initialized.
     *
     * @return A new delegate to be used while opening the database
     * @throws NotImplementedError by default
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected actual open fun createOpenDelegate(): RoomOpenDelegateMarker {
        throw NotImplementedError()
    }

    /**
     * Creates the invalidation tracker
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A new invalidation tracker.
     */
    protected actual abstract fun createInvalidationTracker(): InvalidationTracker

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun getCoroutineScope(): CoroutineScope {
        return coroutineScope
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getQueryContext(): CoroutineContext {
        return coroutineScope.coroutineContext
    }

    internal fun getTransactionContext(): CoroutineContext {
        return transactionContext
    }

    /**
     * Returns a Map of String -> List&lt;Class&gt; where each entry has the `key` as the DAO name
     * and `value` as the list of type converter classes that are necessary for the database to
     * function.
     *
     * This is implemented by the generated code.
     *
     * @return Creates a map that will include all required type converters for this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected open fun getRequiredTypeConverters(): Map<Class<*>, List<Class<*>>> {
        return emptyMap()
    }

    /**
     * Returns a Map of String -> List&lt;KClass&gt; where each entry has the `key` as the DAO name
     * and `value` as the list of type converter classes that are necessary for the database to
     * function.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A map that will include all required type converters for this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected actual open fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        // For backwards compatibility when newer runtime is used with older generated code,
        // call the Java version this function.
        return getRequiredTypeConverters().entries.associate { (key, value) ->
            key.kotlin to value.map { it.kotlin }
        }
    }

    /** Property delegate of [getRequiredTypeConverterClasses] for common ext functionality. */
    internal actual val requiredTypeConverterClassesMap: Map<KClass<*>, List<KClass<*>>>
        get() = getRequiredTypeConverterClasses()

    /**
     * Returns a Set of required AutoMigrationSpec classes.
     *
     * This is implemented by the generated code.
     *
     * @return Creates a set that will include all required auto migration specs for this database.
     */
    @Deprecated("No longer implemented by generated")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public open fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
        return emptySet()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public actual open fun getRequiredAutoMigrationSpecClasses():
        Set<KClass<out AutoMigrationSpec>> {
        // For backwards compatibility when newer runtime is used with older generated code,
        // call the Java version of this function.
        @Suppress("DEPRECATION")
        return getRequiredAutoMigrationSpecs().map { it.kotlin }.toSet()
    }

    /**
     * Deletes all rows from all the tables that are registered to this database as
     * [Database.entities].
     *
     * This does NOT reset the auto-increment value generated by [PrimaryKey.autoGenerate].
     *
     * After deleting the rows, Room will set a WAL checkpoint and run VACUUM. This means that the
     * data is completely erased. The space will be reclaimed by the system if the amount surpasses
     * the threshold of database file size.
     *
     * See SQLite documentation for details. [FileFormat](https://www.sqlite.org/fileformat.html)
     */
    @WorkerThread public abstract fun clearAllTables()

    /**
     * Performs a 'clear all tables' operation.
     *
     * This should only be invoked from generated code.
     *
     * @see [RoomDatabase.clearAllTables]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected fun performClear(hasForeignKeys: Boolean, vararg tableNames: String) {
        assertNotMainThread()
        assertNotSuspendingTransaction()
        runBlockingUninterruptible {
            connectionManager.useConnection(isReadOnly = false) { connection ->
                if (!connection.inTransaction()) {
                    invalidationTracker.sync()
                }
                connection.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                    if (hasForeignKeys) {
                        execSQL("PRAGMA defer_foreign_keys = TRUE")
                    }
                    tableNames.forEach { tableName -> execSQL("DELETE FROM `$tableName`") }
                }
                if (!connection.inTransaction()) {
                    connection.execSQL("PRAGMA wal_checkpoint(FULL)")
                    connection.execSQL("VACUUM")
                    invalidationTracker.refreshAsync()
                }
            }
        }
    }

    /**
     * True if database connection is open and initialized.
     *
     * When Room is configured with [RoomDatabase.Builder.setAutoCloseTimeout] the database is
     * considered open even if internally the connection has been closed, unless manually closed.
     *
     * @return true if the database connection is open, false otherwise.
     */
    public open val isOpen: Boolean
        get() = autoCloser?.isActive ?: connectionManager.isSupportDatabaseOpen()

    /** True if the actual database connection is open, regardless of auto-close. */
    internal val isOpenInternal: Boolean
        get() =
            autoCloser?.let { it.delegateDatabase?.isOpen ?: false }
                ?: connectionManager.isSupportDatabaseOpen()

    /**
     * Closes the database.
     *
     * Once a [RoomDatabase] is closed it should no longer be used.
     */
    public actual open fun close() {
        closeBarrier.close()
    }

    private fun onClosed() {
        coroutineScope.cancel()
        invalidationTracker.stop()
        connectionManager.close()
    }

    /** True if the calling thread is the main thread. */
    internal val isMainThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    /** Asserts that we are not on the main thread. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public open fun assertNotMainThread() {
        if (allowMainThreadQueries) {
            return
        }
        check(!isMainThread) {
            "Cannot access database on the main thread since" +
                " it may potentially lock the UI for a long period of time."
        }
    }

    /** Asserts that we are not on a suspending transaction. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public open fun assertNotSuspendingTransaction() {
        check(!inCompatibilityMode() || inTransaction() || !isThreadInSuspendingTransaction) {
            "Cannot access database on a different coroutine" +
                " context inherited from a suspending transaction."
        }
    }

    /**
     * Use a connection to perform database operations.
     *
     * This function is for internal access to the pool, it is an unconfined coroutine function to
     * be used by Room generated code paths. For the public version see [useReaderConnection] and
     * [useWriterConnection].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R,
    ): R {
        return connectionManager.useConnection(isReadOnly, block)
    }

    /**
     * Return true if this database is operating in compatibility mode, otherwise false.
     *
     * Room is considered in compatibility mode in Android when no [SQLiteDriver] was provided and
     * [androidx.sqlite.db] APIs are used instead (SupportSQLite*).
     *
     * @see RoomConnectionManager
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun inCompatibilityMode(): Boolean = connectionManager.supportOpenHelper != null

    // Below, there are wrapper methods for SupportSQLiteDatabase. This helps us track which
    // methods we are using and also helps unit tests to mock this class without mocking
    // all SQLite database methods.
    /**
     * Convenience method to query the database with arguments.
     *
     * @param query The sql query
     * @param args The bind arguments for the placeholders in the query
     * @return A Cursor obtained by running the given query in the Room database.
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    public open fun query(query: String, args: Array<out Any?>?): Cursor {
        assertNotMainThread()
        assertNotSuspendingTransaction()
        return openHelper.writableDatabase.query(SimpleSQLiteQuery(query, args))
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.query].
     *
     * @param query The Query which includes the SQL and a bind callback for bind arguments.
     * @param signal The cancellation signal to be attached to the query.
     * @return Result of the query.
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    @JvmOverloads
    public open fun query(query: SupportSQLiteQuery, signal: CancellationSignal? = null): Cursor {
        assertNotMainThread()
        assertNotSuspendingTransaction()
        return if (signal != null) {
            openHelper.writableDatabase.query(query, signal)
        } else {
            openHelper.writableDatabase.query(query)
        }
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.compileStatement].
     *
     * @param sql The query to compile.
     * @return The compiled query.
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    public open fun compileStatement(sql: String): SupportSQLiteStatement {
        assertNotMainThread()
        assertNotSuspendingTransaction()
        return openHelper.writableDatabase.compileStatement(sql)
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.beginTransaction].
     *
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    @Deprecated("beginTransaction() is deprecated", ReplaceWith("runInTransaction(Runnable)"))
    public open fun beginTransaction() {
        assertNotMainThread()
        internalBeginTransaction()
    }

    private fun internalBeginTransaction() {
        assertNotMainThread()
        val database = openHelper.writableDatabase
        if (!database.inTransaction()) {
            invalidationTracker.syncBlocking()
        }
        if (database.isWriteAheadLoggingEnabled) {
            database.beginTransactionNonExclusive()
        } else {
            database.beginTransaction()
        }
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.endTransaction].
     *
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    @Deprecated("endTransaction() is deprecated", ReplaceWith("runInTransaction(Runnable)"))
    public open fun endTransaction() {
        internalEndTransaction()
    }

    private fun internalEndTransaction() {
        openHelper.writableDatabase.endTransaction()
        if (!inTransaction()) {
            // enqueue refresh only if we are NOT in a transaction. Otherwise, wait for the last
            // endTransaction call to do it.
            invalidationTracker.refreshVersionsAsync()
        }
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.setTransactionSuccessful].
     *
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     */
    @Deprecated(
        "setTransactionSuccessful() is deprecated",
        ReplaceWith("runInTransaction(Runnable)"),
    )
    public open fun setTransactionSuccessful() {
        openHelper.writableDatabase.setTransactionSuccessful()
    }

    /**
     * Executes the specified [Runnable] in a database transaction. The transaction will be marked
     * as successful unless an exception is thrown in the [Runnable].
     *
     * Room will only perform at most one transaction at a time.
     *
     * If a [SQLiteDriver] is configured with this database, then it is best to use
     * [useWriterConnection] along with [immediateTransaction] to perform transactional operations.
     *
     * @param body The piece of code to execute.
     */
    public open fun runInTransaction(body: Runnable) {
        runInTransaction { body.run() }
    }

    /**
     * Executes the specified [Callable] in a database transaction. The transaction will be marked
     * as successful unless an exception is thrown in the [Callable].
     *
     * Room will only perform at most one transaction at a time.
     *
     * If a [SQLiteDriver] is configured with this database, then it is best to use
     * [useWriterConnection] along with [immediateTransaction] to perform transactional operations.
     *
     * @param body The piece of code to execute.
     * @param V The type of the return value.
     * @return The value returned from the [Callable].
     */
    public open fun <V> runInTransaction(body: Callable<V>): V {
        return runInTransaction { body.call() }
    }

    @Suppress("DEPRECATION") // Usage of try-finally transaction idiom APIs
    private fun <T> runInTransaction(body: () -> T): T {
        if (inCompatibilityMode()) {
            beginTransaction()
            try {
                val result = body.invoke()
                setTransactionSuccessful()
                return result
            } finally {
                endTransaction()
            }
        } else {
            return performBlocking(db = this, isReadOnly = false, inTransaction = true) {
                body.invoke()
            }
        }
    }

    /**
     * Initialize invalidation tracker. Note that this method is called when the [RoomDatabase] is
     * initialized and opens a database connection.
     *
     * @param db The database instance.
     */
    @Deprecated("No longer called by generated")
    protected open fun internalInitInvalidationTracker(db: SupportSQLiteDatabase) {
        internalInitInvalidationTracker(SupportSQLiteConnection(db))
    }

    /**
     * Initialize invalidation tracker. Note that this method is called when the [RoomDatabase] is
     * initialized and opens a database connection.
     *
     * @param connection The database connection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected actual fun internalInitInvalidationTracker(connection: SQLiteConnection) {
        invalidationTracker.internalInit(connection)
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.inTransaction]. Returns true if current thread is in a
     * transaction.
     *
     * @return True if there is an active transaction in current thread, false otherwise.
     * @throws IllegalStateException If a [SQLiteDriver] is configured with this database.
     * @see SupportSQLiteDatabase.inTransaction
     */
    public open fun inTransaction(): Boolean {
        return isOpenInternal && openHelper.writableDatabase.inTransaction()
    }

    /**
     * Journal modes for SQLite database.
     *
     * @see Builder.setJournalMode
     */
    public actual enum class JournalMode {
        /**
         * Let Room choose the journal mode. This is the default value when no explicit value is
         * specified.
         *
         * The actual value will be [TRUNCATE] when the device runs API Level lower than 16 or it is
         * a low-RAM device. Otherwise, [WRITE_AHEAD_LOGGING] will be used.
         */
        AUTOMATIC,

        /** Truncate journal mode. */
        TRUNCATE,

        /** Write-Ahead Logging mode. */
        WRITE_AHEAD_LOGGING;

        /** Resolves [AUTOMATIC] to either [TRUNCATE] or [WRITE_AHEAD_LOGGING]. */
        internal fun resolve(context: Context): JournalMode {
            if (this != AUTOMATIC) {
                return this
            }
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (manager != null && !manager.isLowRamDevice) {
                return WRITE_AHEAD_LOGGING
            }
            return TRUNCATE
        }
    }

    /**
     * Builder for [RoomDatabase].
     *
     * @param T The type of the abstract database class.
     */
    @Suppress("GetterOnBuilder") // To keep ABI compatibility from Java
    public actual open class Builder<T : RoomDatabase> {
        private val klass: KClass<T>
        private val context: Context
        private val name: String?
        private val factory: (() -> T)?

        /**
         * Constructor for [RoomDatabase.Builder].
         *
         * @param klass The abstract database class.
         * @param name The name of the database or NULL for an in-memory database.
         * @param factory The lambda calling `initializeImpl()` on the abstract database class which
         *   returns the generated database implementation.
         * @param context The context for the database, this is usually the Application context.
         */
        @PublishedApi
        internal constructor(
            klass: KClass<T>,
            name: String?,
            factory: (() -> T)?,
            context: Context,
        ) {
            this.klass = klass
            this.context = context
            this.name = name
            this.factory = factory
        }

        /**
         * Constructor for [RoomDatabase.Builder].
         *
         * @param context The context for the database, this is usually the Application context.
         * @param klass The abstract database class.
         * @param name The name of the database or NULL for an in-memory database.
         */
        internal constructor(context: Context, klass: Class<T>, name: String?) {
            this.klass = klass.kotlin
            this.context = context
            this.name = name
            this.factory = null
        }

        private val callbacks: MutableList<Callback> = mutableListOf()
        private var prepackagedDatabaseCallback: PrepackagedDatabaseCallback? = null
        private var queryCallback: QueryCallback? = null
        private var queryCallbackExecutor: Executor? = null
        private var queryCallbackCoroutineContext: CoroutineContext? = null
        private val typeConverters: MutableList<Any> = mutableListOf()
        private var queryExecutor: Executor? = null
        private var transactionExecutor: Executor? = null

        private var supportOpenHelperFactory: SupportSQLiteOpenHelper.Factory? = null
        private var allowMainThreadQueries = false
        private var journalMode: JournalMode = JournalMode.AUTOMATIC
        private var multiInstanceInvalidationIntent: Intent? = null

        private var autoCloseTimeout = -1L
        private var autoCloseTimeUnit: TimeUnit? = null

        /** Migrations, mapped by from-to pairs. */
        private val migrationContainer: MigrationContainer = MigrationContainer()

        /**
         * Versions that don't require migrations, configured via
         * [fallbackToDestructiveMigrationFrom].
         */
        private var migrationsNotRequiredFrom: MutableSet<Int> = mutableSetOf()

        /**
         * Keeps track of [Migration.startVersion]s and [Migration.endVersion]s added in
         * [addMigrations] for later validation that makes those versions don't match any versions
         * passed to [fallbackToDestructiveMigrationFrom].
         */
        private val migrationStartAndEndVersions = mutableSetOf<Int>()

        private val autoMigrationSpecs: MutableList<AutoMigrationSpec> = mutableListOf()

        private var requireMigration: Boolean = true
        private var allowDestructiveMigrationOnDowngrade = false
        private var allowDestructiveMigrationForAllTables = false

        private var copyFromAssetPath: String? = null
        private var copyFromFile: File? = null
        private var copyFromInputStream: Callable<InputStream>? = null

        private var driver: SQLiteDriver? = null
        private var queryCoroutineContext: CoroutineContext? = null

        private var inMemoryTrackingTableMode = true

        /**
         * Configures Room to create and open the database using a pre-packaged database located in
         * the application 'assets/' folder.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal app
         * database folder and then opens it. The pre-packaged database file must be located in the
         * "assets/" folder of your application. For example, the path for a file located in
         * "assets/databases/products.db" would be "databases/products.db".
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFilePath The file path within the 'assets/' directory of where the
         *   database file is located.
         * @return This builder instance.
         */
        public open fun createFromAsset(databaseFilePath: String): Builder<T> = apply {
            this.copyFromAssetPath = databaseFilePath
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database located in
         * the application 'assets/' folder.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal app
         * database folder and then opens it. The pre-packaged database file must be located in the
         * "assets/" folder of your application. For example, the path for a file located in
         * "assets/databases/products.db" would be "databases/products.db".
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFilePath The file path within the 'assets/' directory of where the
         *   database file is located.
         * @param callback The pre-packaged callback.
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle") // To keep naming consistency.
        public open fun createFromAsset(
            databaseFilePath: String,
            callback: PrepackagedDatabaseCallback,
        ): Builder<T> = apply {
            this.prepackagedDatabaseCallback = callback
            this.copyFromAssetPath = databaseFilePath
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database file.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal app
         * database folder and then opens it. The given file must be accessible and the right
         * permissions must be granted for Room to copy the file.
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * The [Callback.onOpen] method can be used as an indicator that the pre-packaged database
         * was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFile The database file.
         * @return This builder instance.
         */
        public open fun createFromFile(databaseFile: File): Builder<T> = apply {
            this.copyFromFile = databaseFile
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database file.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal app
         * database folder and then opens it. The given file must be accessible and the right
         * permissions must be granted for Room to copy the file.
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * The [Callback.onOpen] method can be used as an indicator that the pre-packaged database
         * was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFile The database file.
         * @param callback The pre-packaged callback.
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle", "StreamFiles") // To keep naming consistency.
        public open fun createFromFile(
            databaseFile: File,
            callback: PrepackagedDatabaseCallback,
        ): Builder<T> = apply {
            this.prepackagedDatabaseCallback = callback
            this.copyFromFile = databaseFile
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database via an
         * [InputStream].
         *
         * This is useful for processing compressed database files. Room does not open the
         * pre-packaged database, instead it copies it into the internal app database folder, and
         * then open it. The [InputStream] will be closed once Room is done consuming it.
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * The [Callback.onOpen] method can be used as an indicator that the pre-packaged database
         * was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param inputStreamCallable A callable that returns an InputStream from which to copy the
         *   database. The callable will be invoked in a thread from the Executor set via
         *   [setQueryExecutor]. The callable is only invoked if Room needs to create and open the
         *   database from the pre-package database, usually the first time it is created or during
         *   a destructive migration.
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle") // To keep naming consistency.
        public open fun createFromInputStream(
            inputStreamCallable: Callable<InputStream>
        ): Builder<T> = apply { this.copyFromInputStream = inputStreamCallable }

        /**
         * Configures Room to create and open the database using a pre-packaged database via an
         * [InputStream].
         *
         * This is useful for processing compressed database files. Room does not open the
         * pre-packaged database, instead it copies it into the internal app database folder, and
         * then open it. The [InputStream] will be closed once Room is done consuming it.
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * The [Callback.onOpen] method can be used as an indicator that the pre-packaged database
         * was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param inputStreamCallable A callable that returns an InputStream from which to copy the
         *   database. The callable will be invoked in a thread from the Executor set via
         *   [setQueryExecutor]. The callable is only invoked if Room needs to create and open the
         *   database from the pre-package database, usually the first time it is created or during
         *   a destructive migration.
         * @param callback The pre-packaged callback.
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle", "LambdaLast") // To keep naming consistency.
        public open fun createFromInputStream(
            inputStreamCallable: Callable<InputStream>,
            callback: PrepackagedDatabaseCallback,
        ): Builder<T> = apply {
            this.prepackagedDatabaseCallback = callback
            this.copyFromInputStream = inputStreamCallable
        }

        /**
         * Sets the database factory. If not set, it defaults to [FrameworkSQLiteOpenHelperFactory].
         *
         * @param factory The factory to use to access the database.
         * @return This builder instance.
         */
        public open fun openHelperFactory(factory: SupportSQLiteOpenHelper.Factory?): Builder<T> =
            apply {
                this.supportOpenHelperFactory = factory
            }

        /**
         * Adds a migration to the builder.
         *
         * Each [Migration] has a start and end versions and Room runs these migrations to bring the
         * database to the latest version.
         *
         * A migration can handle more than 1 version (e.g. if you have a faster path to choose when
         * going from version 3 to 5 without going to version 4). If Room opens a database at
         * version 3 and latest version is >= 5, Room will use the migration object that can migrate
         * from 3 to 5 instead of 3 to 4 and 4 to 5.
         *
         * @param migrations The migration objects that modify the database schema with the
         *   necessary changes for a version change.
         * @return This builder instance.
         */
        public actual open fun addMigrations(vararg migrations: Migration): Builder<T> = apply {
            for (migration in migrations) {
                migrationStartAndEndVersions.add(migration.startVersion)
                migrationStartAndEndVersions.add(migration.endVersion)
            }
            migrationContainer.addMigrations(*migrations)
        }

        /**
         * Adds an auto migration spec instance to the builder.
         *
         * @param autoMigrationSpec The auto migration object that is annotated with
         *   [ProvidedAutoMigrationSpec] and is declared in an [AutoMigration] annotation.
         * @return This builder instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public actual open fun addAutoMigrationSpec(
            autoMigrationSpec: AutoMigrationSpec
        ): Builder<T> = apply { this.autoMigrationSpecs.add(autoMigrationSpec) }

        /**
         * Disables the main thread query check for Room.
         *
         * Room ensures that Database is never accessed on the main thread because it may lock the
         * main thread and trigger an ANR. If you need to access the database from the main thread,
         * you should always use async alternatives or manually move the call to a background
         * thread.
         *
         * You may want to turn this check off for testing.
         *
         * @return This builder instance.
         */
        public open fun allowMainThreadQueries(): Builder<T> = apply {
            this.allowMainThreadQueries = true
        }

        /**
         * Sets the journal mode for this database.
         *
         * The value is ignored if the builder is for an 'in-memory database'. The journal mode
         * should be consistent across multiple instances of [RoomDatabase] for a single SQLite
         * database file.
         *
         * The default value is [JournalMode.AUTOMATIC].
         *
         * @param journalMode The journal mode.
         * @return This builder instance.
         */
        public actual open fun setJournalMode(journalMode: JournalMode): Builder<T> = apply {
            this.journalMode = journalMode
        }

        /**
         * Sets the [Executor] that will be used to execute all non-blocking asynchronous queries
         * and tasks, including `LiveData` invalidation, `Flowable` scheduling and
         * `ListenableFuture` tasks.
         *
         * When both the query executor and transaction executor are unset, then a default
         * `Executor` will be used. The default `Executor` allocates and shares threads amongst
         * Architecture Components libraries. If the query executor is unset but a transaction
         * executor was set via [setTransactionExecutor], then the same `Executor` will be used for
         * queries.
         *
         * For best performance the given `Executor` should be bounded (max number of threads is
         * limited).
         *
         * The input `Executor` cannot run tasks on the UI thread.
         *
         * If either [setQueryCoroutineContext] has been called, then this function will throw an
         * [IllegalArgumentException].
         *
         * @return This builder instance.
         * @throws IllegalArgumentException if this builder was already configured with a
         *   [CoroutineContext].
         */
        public open fun setQueryExecutor(executor: Executor): Builder<T> = apply {
            require(queryCoroutineContext == null) {
                "This builder has already been configured with a CoroutineContext. A RoomDatabase" +
                    "can only be configured with either an Executor or a CoroutineContext."
            }
            this.queryExecutor = executor
        }

        /**
         * Sets the [Executor] that will be used to execute all non-blocking asynchronous
         * transaction queries and tasks, including `LiveData` invalidation, `Flowable` scheduling
         * and `ListenableFuture` tasks.
         *
         * When both the transaction executor and query executor are unset, then a default
         * `Executor` will be used. The default `Executor` allocates and shares threads amongst
         * Architecture Components libraries. If the transaction executor is unset but a query
         * executor was set using [setQueryExecutor], then the same `Executor` will be used for
         * transactions.
         *
         * If the given `Executor` is shared then it should be unbounded to avoid the possibility of
         * a deadlock. Room will not use more than one thread at a time from this executor since
         * only one transaction at a time can be executed, other transactions will be queued on a
         * first come, first serve order.
         *
         * The input `Executor` cannot run tasks on the UI thread.
         *
         * If either [setQueryCoroutineContext] has been called, then this function will throw an
         * [IllegalArgumentException].
         *
         * @return This builder instance.
         * @throws IllegalArgumentException if this builder was already configured with a
         *   [CoroutineContext].
         */
        public open fun setTransactionExecutor(executor: Executor): Builder<T> = apply {
            require(queryCoroutineContext == null) {
                "This builder has already been configured with a CoroutineContext. A RoomDatabase" +
                    "can only be configured with either an Executor or a CoroutineContext."
            }
            this.transactionExecutor = executor
        }

        /**
         * Sets whether table invalidation in this instance of [RoomDatabase] should be broadcast
         * and synchronized with other instances of the same [RoomDatabase], including those in a
         * separate process. In order to enable multi-instance invalidation, this has to be turned
         * on both ends.
         *
         * This is not enabled by default.
         *
         * This does not work for in-memory databases. This does not work between database instances
         * targeting different database files.
         *
         * @return This builder instance.
         */
        @OptIn(ExperimentalRoomApi::class)
        @Suppress("UnsafeOptInUsageError")
        public open fun enableMultiInstanceInvalidation(): Builder<T> = apply {
            this.multiInstanceInvalidationIntent =
                if (name != null) {
                    Intent(context, MultiInstanceInvalidationService::class.java)
                } else {
                    null
                }
        }

        /**
         * Sets whether table invalidation in this instance of [RoomDatabase] should be broadcast
         * and synchronized with other instances of the same [RoomDatabase], including those in a
         * separate process. In order to enable multi-instance invalidation, this has to be turned
         * on both ends and need to point to the same [MultiInstanceInvalidationService].
         *
         * This is not enabled by default.
         *
         * This does not work for in-memory databases. This does not work between database instances
         * targeting different database files.
         *
         * @param invalidationServiceIntent Intent to bind to the
         *   [MultiInstanceInvalidationService].
         * @return This builder instance.
         */
        @ExperimentalRoomApi
        @Suppress("MissingGetterMatchingBuilder")
        public open fun setMultiInstanceInvalidationServiceIntent(
            invalidationServiceIntent: Intent
        ): Builder<T> = apply {
            this.multiInstanceInvalidationIntent =
                if (name != null) invalidationServiceIntent else null
        }

        /**
         * Allows Room to destructively recreate database tables if [Migration]s that would migrate
         * old database schemas to the latest schema version are not found.
         *
         * When the database version on the device does not match the latest schema version, Room
         * runs necessary [Migration]s on the database.
         *
         * If it cannot find the set of [Migration]s that will bring the database to the current
         * version, it will throw an [IllegalStateException].
         *
         * You can call this method to change this behavior to re-create the database tables instead
         * of crashing.
         *
         * If the database was create from an asset or a file then Room will try to use the same
         * file to re-create the database, otherwise this will delete all of the data in the
         * database tables managed by Room.
         *
         * To let Room fallback to destructive migration only during a schema downgrade then use
         * [fallbackToDestructiveMigrationOnDowngrade].
         *
         * @return This builder instance.
         */
        @Deprecated(
            message =
                "Replace by overloaded version with parameter to indicate if all tables " +
                    "should be dropped or not.",
            replaceWith = ReplaceWith("fallbackToDestructiveMigration(false)"),
        )
        @Suppress("BuilderSetStyle") // Overload of exsisting API
        public open fun fallbackToDestructiveMigration(): Builder<T> = apply {
            this.requireMigration = false
            this.allowDestructiveMigrationOnDowngrade = true
        }

        /**
         * Allows Room to destructively recreate database tables if [Migration]s that would migrate
         * old database schemas to the latest schema version are not found.
         *
         * When the database version on the device does not match the latest schema version, Room
         * runs necessary [Migration]s on the database. If it cannot find the set of [Migration]s
         * that will bring the database to the current version, it will throw an
         * [IllegalStateException]. You can call this method to change this behavior to re-create
         * the database tables instead of crashing.
         *
         * If the database was create from an asset or a file then Room will try to use the same
         * file to re-create the database, otherwise this will delete all of the data in the
         * database tables managed by Room.
         *
         * To let Room fallback to destructive migration only during a schema downgrade then use
         * [fallbackToDestructiveMigrationOnDowngrade].
         *
         * @param dropAllTables Set to `true` if all tables should be dropped during destructive
         *   migration including those not managed by Room. Recommended value is `true` as otherwise
         *   Room could leave obsolete data when table names or existence changes between versions.
         * @return This builder instance.
         */
        @Suppress("BuilderSetStyle") // Overload of existing API
        public actual fun fallbackToDestructiveMigration(dropAllTables: Boolean): Builder<T> =
            apply {
                this.requireMigration = false
                this.allowDestructiveMigrationOnDowngrade = true
                this.allowDestructiveMigrationForAllTables = dropAllTables
            }

        /**
         * Allows Room to destructively recreate database tables if [Migration]s are not available
         * when downgrading to old schema versions.
         *
         * For details, see [Builder.fallbackToDestructiveMigration].
         *
         * @return This builder instance.
         */
        @Deprecated(
            message =
                "Replace by overloaded version with parameter to indicate if all tables " +
                    "should be dropped or not.",
            replaceWith = ReplaceWith("fallbackToDestructiveMigrationOnDowngrade(false)"),
        )
        public open fun fallbackToDestructiveMigrationOnDowngrade(): Builder<T> = apply {
            this.requireMigration = true
            this.allowDestructiveMigrationOnDowngrade = true
        }

        /**
         * Allows Room to destructively recreate database tables if [Migration]s are not available
         * when downgrading to old schema versions.
         *
         * For details, see [Builder.fallbackToDestructiveMigration].
         *
         * @param dropAllTables Set to `true` if all tables should be dropped during destructive
         *   migration including those not managed by Room. Recommended value is `true` as otherwise
         *   Room could leave obsolete data when table names or existence changes between versions.
         * @return This builder instance.
         */
        @Suppress("BuilderSetStyle") // Overload of existing API
        public actual fun fallbackToDestructiveMigrationOnDowngrade(
            dropAllTables: Boolean
        ): Builder<T> = apply {
            this.requireMigration = true
            this.allowDestructiveMigrationOnDowngrade = true
            this.allowDestructiveMigrationForAllTables = dropAllTables
        }

        /**
         * Informs Room that it is allowed to destructively recreate database tables from specific
         * starting schema versions.
         *
         * This functionality is the same as that provided by [fallbackToDestructiveMigration],
         * except that this method allows the specification of a set of schema versions for which
         * destructive recreation is allowed.
         *
         * Using this method is preferable to [fallbackToDestructiveMigration] if you want to allow
         * destructive migrations from some schema versions while still taking advantage of
         * exceptions being thrown due to unintentionally missing migrations.
         *
         * Note: No versions passed to this method may also exist as either starting or ending
         * versions in the [Migration]s provided to [addMigrations]. If a version passed to this
         * method is found as a starting or ending version in a Migration, an exception will be
         * thrown.
         *
         * @param startVersions The set of schema versions from which Room should use a destructive
         *   migration.
         * @return This builder instance.
         */
        @Deprecated(
            message =
                "Replace by overloaded version with parameter to indicate if all tables " +
                    "should be dropped or not.",
            replaceWith = ReplaceWith("fallbackToDestructiveMigrationFrom(false, startVersions)"),
        )
        public open fun fallbackToDestructiveMigrationFrom(vararg startVersions: Int): Builder<T> =
            apply {
                for (startVersion in startVersions) {
                    this.migrationsNotRequiredFrom.add(startVersion)
                }
            }

        /**
         * Informs Room that it is allowed to destructively recreate database tables from specific
         * starting schema versions.
         *
         * This functionality is the same [fallbackToDestructiveMigration], except that this method
         * allows the specification of a set of schema versions for which destructive recreation is
         * allowed.
         *
         * Using this method is preferable to [fallbackToDestructiveMigration] if you want to allow
         * destructive migrations from some schema versions while still taking advantage of
         * exceptions being thrown due to unintentionally missing migrations.
         *
         * Note: No versions passed to this method may also exist as either starting or ending
         * versions in the [Migration]s provided via [addMigrations]. If a version passed to this
         * method is found as a starting or ending version in a Migration, an exception will be
         * thrown.
         *
         * @param dropAllTables Set to `true` if all tables should be dropped during destructive
         *   migration including those not managed by Room.
         * @param startVersions The set of schema versions from which Room should use a destructive
         *   migration.
         * @return This builder instance.
         */
        @Suppress(
            "BuilderSetStyle", // Overload of existing API
            "MissingJvmstatic", // No need for @JvmOverloads due to an overload already existing
        )
        public actual open fun fallbackToDestructiveMigrationFrom(
            @Suppress("KotlinDefaultParameterOrder") // There is a vararg that must be last
            dropAllTables: Boolean,
            vararg startVersions: Int,
        ): Builder<T> = apply {
            for (startVersion in startVersions) {
                this.migrationsNotRequiredFrom.add(startVersion)
            }
            this.allowDestructiveMigrationForAllTables = dropAllTables
        }

        /**
         * Adds a [Callback] to this database.
         *
         * @param callback The callback.
         * @return This builder instance.
         */
        public actual open fun addCallback(callback: Callback): Builder<T> = apply {
            this.callbacks.add(callback)
        }

        /**
         * Sets a [QueryCallback] to be invoked when queries are executed.
         *
         * The callback is invoked whenever a query is executed, note that adding this callback has
         * a small cost and should be avoided in production builds unless needed.
         *
         * A use case for providing a callback is to allow logging executed queries. When the
         * callback implementation logs then it is recommended to use an immediate executor.
         *
         * If a previous callback was set with [setQueryCallback] then this call will override it,
         * including removing the Coroutine context previously set, if any.
         *
         * @param queryCallback The query callback.
         * @param executor The executor on which the query callback will be invoked.
         * @return This builder instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public open fun setQueryCallback(
            queryCallback: QueryCallback,
            executor: Executor,
        ): Builder<T> = apply {
            this.queryCallback = queryCallback
            this.queryCallbackExecutor = executor
            this.queryCallbackCoroutineContext = null
        }

        /**
         * Sets a [QueryCallback] to be invoked when queries are executed.
         *
         * The callback is invoked whenever a query is executed, note that adding this callback has
         * a small cost and should be avoided in production builds unless needed.
         *
         * A use case for providing a callback is to allow logging executed queries. When the
         * callback implementation simply logs then it is recommended to use
         * [kotlinx.coroutines.Dispatchers.Unconfined].
         *
         * If a previous callback was set with [setQueryCallback] then this call will override it,
         * including removing the executor previously set, if any.
         *
         * @param context The coroutine context on which the query callback will be invoked.
         * @param queryCallback The query callback.
         * @return This builder instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setQueryCallback(
            context: CoroutineContext,
            queryCallback: QueryCallback,
        ): Builder<T> = apply {
            this.queryCallback = queryCallback
            this.queryCallbackExecutor = null
            this.queryCallbackCoroutineContext = context
        }

        /**
         * Adds a type converter instance to the builder.
         *
         * @param typeConverter The converter instance that is annotated with
         *   [ProvidedTypeConverter].
         * @return This builder instance.
         */
        public actual open fun addTypeConverter(typeConverter: Any): Builder<T> = apply {
            this.typeConverters.add(typeConverter)
        }

        /**
         * Enables auto-closing for the database to free up unused resources. The underlying
         * database will be closed after it's last use after the specified [autoCloseTimeout] has
         * elapsed since its last usage. The database will be automatically re-opened the next time
         * it is accessed.
         *
         * Auto-closing is not compatible with in-memory databases since the data will be lost when
         * the database is auto-closed.
         *
         * Also, temp tables and temp triggers will be cleared each time the database is
         * auto-closed. If you need to use them, please include them in your callback
         * [RoomDatabase.Callback.onOpen].
         *
         * All configuration should happen in your [RoomDatabase.Callback.onOpen] callback so it is
         * re-applied every time the database is re-opened. Note that the
         * [RoomDatabase.Callback.onOpen] will be called every time the database is re-opened.
         *
         * The auto-closing database operation runs on the query executor.
         *
         * The database will not be re-opened if the RoomDatabase or the SupportSqliteOpenHelper is
         * closed manually (by calling [RoomDatabase.close] or [SupportSQLiteOpenHelper.close]. If
         * the database is closed manually, you must create a new database using
         * [RoomDatabase.Builder.build].
         *
         * @param autoCloseTimeout the amount of time after the last usage before closing the
         *   database. Must greater or equal to zero.
         * @param autoCloseTimeUnit the timeunit for autoCloseTimeout.
         * @return This builder instance.
         */
        @ExperimentalRoomApi // When experimental is removed, add these parameters to
        // DatabaseConfiguration
        @Suppress("MissingGetterMatchingBuilder")
        public open fun setAutoCloseTimeout(
            @IntRange(from = 0) autoCloseTimeout: Long,
            autoCloseTimeUnit: TimeUnit,
        ): Builder<T> = apply {
            require(autoCloseTimeout >= 0) { "autoCloseTimeout must be >= 0" }
            this.autoCloseTimeout = autoCloseTimeout
            this.autoCloseTimeUnit = autoCloseTimeUnit
        }

        /**
         * Sets the [SQLiteDriver] implementation to be used by Room to open database connections.
         * For example, an instance of [androidx.sqlite.driver.AndroidSQLiteDriver] or
         * [androidx.sqlite.driver.bundled.BundledSQLiteDriver].
         *
         * Once a driver is configured using this function, various callbacks that receive a
         * [SupportSQLiteDatabase] will not be invoked, such as [RoomDatabase.Callback.onCreate].
         * Moreover, APIs that use SupportSQLite will also throw an exception, such as
         * [RoomDatabase.openHelper].
         *
         * See the documentation on
         * [Migrating to SQLite Driver](https://d.android.com/training/data-storage/room/room-kmp-migration#migrate_from_support_sqlite_to_sqlite_driver)
         * for more information.
         *
         * @param driver The driver
         * @return This builder instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        public actual fun setDriver(driver: SQLiteDriver): Builder<T> = apply {
            this.driver = driver
        }

        /**
         * Sets the [CoroutineContext] that will be used to execute all asynchronous queries and
         * tasks, such as `Flow` emissions and [InvalidationTracker] notifications.
         *
         * If no [CoroutineDispatcher] is present in the [context] then this function will throw an
         * [IllegalArgumentException]
         *
         * If no context is provided, then Room wil default to using the [Executor] set via
         * [setQueryExecutor] as the context via the conversion function [asCoroutineDispatcher].
         *
         * If either [setQueryExecutor] or [setTransactionExecutor] has been called, then this
         * function will throw an [IllegalArgumentException].
         *
         * @param context The context
         * @return This [Builder] instance
         * @throws IllegalArgumentException if no [CoroutineDispatcher] is found in the given
         *   [context] or if this builder was already configured with an [Executor].
         */
        @Suppress("MissingGetterMatchingBuilder")
        public actual fun setQueryCoroutineContext(context: CoroutineContext): Builder<T> = apply {
            require(queryExecutor == null && transactionExecutor == null) {
                "This builder has already been configured with an Executor. A RoomDatabase can" +
                    "only be configured with either an Executor or a CoroutineContext."
            }
            require(context[ContinuationInterceptor] != null) {
                "It is required that the coroutine context contain a dispatcher."
            }
            this.queryCoroutineContext = context
        }

        /**
         * Sets whether Room will use an in-memory table or a persisted table to track invalidation.
         *
         * An in-memory table is used by default. Using an in-memory tables is more performant,
         * reduces the journal file size but has an increased memory footprint, where as using a
         * real table has the opposite effect.
         *
         * @param inMemory True if in-memory tables should be used, false otherwise.
         * @return This [Builder] instance
         */
        @ExperimentalRoomApi
        @Suppress("MissingGetterMatchingBuilder")
        public fun setInMemoryTrackingMode(inMemory: Boolean): Builder<T> = apply {
            this.inMemoryTrackingTableMode = inMemory
        }

        /**
         * Creates the databases and initializes it.
         *
         * By default, all RoomDatabases use in memory storage for TEMP tables and enables recursive
         * triggers.
         *
         * @return A new database instance.
         * @throws IllegalArgumentException if the builder was misconfigured.
         */
        public actual open fun build(): T {
            if (queryExecutor == null && transactionExecutor == null) {
                transactionExecutor = ArchTaskExecutor.getIOThreadExecutor()
                queryExecutor = transactionExecutor
            } else if (queryExecutor != null && transactionExecutor == null) {
                transactionExecutor = queryExecutor
            } else if (queryExecutor == null) {
                queryExecutor = transactionExecutor
            }

            validateMigrationsNotRequired(migrationStartAndEndVersions, migrationsNotRequiredFrom)

            val initialFactory: SupportSQLiteOpenHelper.Factory? =
                if (driver == null && supportOpenHelperFactory == null) {
                    // No driver and no factory, compatibility mode, create the default factory
                    FrameworkSQLiteOpenHelperFactory()
                } else if (driver == null) {
                    // No driver but a factory was provided, use it in compatibility mode
                    supportOpenHelperFactory
                } else if (supportOpenHelperFactory == null) {
                    // A driver was provided, no need to create the default factory
                    null
                } else {
                    // Both driver and factory provided, invalid configuration.
                    throw IllegalArgumentException(
                        "A RoomDatabase cannot be configured with both a SQLiteDriver and a " +
                            "SupportOpenHelper.Factory."
                    )
                }
            val autoCloseEnabled = autoCloseTimeout > 0
            val prePackagedCopyEnabled =
                copyFromAssetPath != null || copyFromFile != null || copyFromInputStream != null
            val queryCallbackEnabled = queryCallback != null
            val supportOpenHelperFactory =
                initialFactory
                    ?.let {
                        if (autoCloseEnabled) {
                            requireNotNull(name) {
                                "Cannot create auto-closing database for an in-memory database."
                            }
                            val autoCloser =
                                AutoCloser(
                                    timeoutAmount = autoCloseTimeout,
                                    timeUnit = requireNotNull(autoCloseTimeUnit),
                                )
                            AutoClosingRoomOpenHelperFactory(delegate = it, autoCloser = autoCloser)
                        } else {
                            it
                        }
                    }
                    ?.let {
                        if (prePackagedCopyEnabled) {
                            requireNotNull(name) {
                                "Cannot create from asset or file for an in-memory database."
                            }

                            val copyFromAssetPathConfig = if (copyFromAssetPath == null) 0 else 1
                            val copyFromFileConfig = if (copyFromFile == null) 0 else 1
                            val copyFromInputStreamConfig =
                                if (copyFromInputStream == null) 0 else 1
                            val copyConfigurations =
                                copyFromAssetPathConfig +
                                    copyFromFileConfig +
                                    copyFromInputStreamConfig

                            require(copyConfigurations == 1) {
                                "More than one of createFromAsset(), " +
                                    "createFromInputStream(), and createFromFile() were called on this " +
                                    "Builder, but the database can only be created using one of the " +
                                    "three configurations."
                            }
                            PrePackagedCopyOpenHelperFactory(
                                copyFromAssetPath = copyFromAssetPath,
                                copyFromFile = copyFromFile,
                                copyFromInputStream = copyFromInputStream,
                                delegate = it,
                            )
                        } else {
                            it
                        }
                    }
                    ?.let {
                        if (queryCallbackEnabled) {
                            val queryCallbackContext =
                                queryCallbackExecutor?.asCoroutineDispatcher()
                                    ?: requireNotNull(queryCallbackCoroutineContext)
                            QueryInterceptorOpenHelperFactory(
                                delegate = it,
                                queryCallbackScope = CoroutineScope(queryCallbackContext),
                                queryCallback = requireNotNull(queryCallback),
                            )
                        } else {
                            it
                        }
                    }
            // No open helper means a driver is to be used.
            if (supportOpenHelperFactory == null) {
                require(!autoCloseEnabled) {
                    "Auto Closing Database is not supported when an SQLiteDriver is configured."
                }
                require(!prePackagedCopyEnabled) {
                    "Pre-Package Database is not supported when an SQLiteDriver is configured."
                }
                require(!queryCallbackEnabled) {
                    "Query Callback is not supported when an SQLiteDriver is configured."
                }
            }
            val configuration =
                DatabaseConfiguration(
                        context = context,
                        name = name,
                        sqliteOpenHelperFactory = supportOpenHelperFactory,
                        migrationContainer = migrationContainer,
                        callbacks = callbacks,
                        allowMainThreadQueries = allowMainThreadQueries,
                        journalMode = journalMode.resolve(context),
                        queryExecutor = requireNotNull(queryExecutor),
                        transactionExecutor = requireNotNull(transactionExecutor),
                        multiInstanceInvalidationServiceIntent = multiInstanceInvalidationIntent,
                        requireMigration = requireMigration,
                        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
                        migrationNotRequiredFrom = migrationsNotRequiredFrom,
                        copyFromAssetPath = copyFromAssetPath,
                        copyFromFile = copyFromFile,
                        copyFromInputStream = copyFromInputStream,
                        prepackagedDatabaseCallback = prepackagedDatabaseCallback,
                        typeConverters = typeConverters,
                        autoMigrationSpecs = autoMigrationSpecs,
                        allowDestructiveMigrationForAllTables =
                            allowDestructiveMigrationForAllTables,
                        sqliteDriver = driver,
                        queryCoroutineContext = queryCoroutineContext,
                    )
                    .apply { this.useTempTrackingTable = inMemoryTrackingTableMode }
            val db = factory?.invoke() ?: findAndInstantiateDatabaseImpl(klass.java)
            db.init(configuration)
            return db
        }
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    public actual open class MigrationContainer {
        private val migrations = mutableMapOf<Int, TreeMap<Int, Migration>>()

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        public open fun addMigrations(vararg migrations: Migration) {
            migrations.forEach(::addMigration)
        }

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        public actual open fun addMigrations(migrations: List<Migration>) {
            migrations.forEach(::addMigration)
        }

        /**
         * Add a [Migration] to the container. If the container already has a migration with the
         * same start-end versions then it will be overwritten.
         *
         * @param migration the migration to add.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun addMigration(migration: Migration) {
            val start = migration.startVersion
            val end = migration.endVersion
            val targetMap = migrations.getOrPut(start) { TreeMap<Int, Migration>() }

            if (targetMap.contains(end)) {
                Log.w(LOG_TAG, "Overriding migration ${targetMap[end]} with $migration")
            }
            targetMap[end] = migration
        }

        /**
         * Returns the map of available migrations where the key is the start version of the
         * migration, and the value is a map of (end version -> Migration).
         *
         * @return Map of migrations keyed by the start version
         */
        public actual open fun getMigrations(): Map<Int, Map<Int, Migration>> {
            return migrations
        }

        /**
         * Finds the list of migrations that should be run to move from `start` version to `end`
         * version.
         *
         * @param start The current database version
         * @param end The target database version
         * @return An ordered list of [Migration] objects that should be run to migrate between the
         *   given versions. If a migration path cannot be found, returns `null`.
         */
        public open fun findMigrationPath(start: Int, end: Int): List<Migration>? {
            return this.findMigrationPathExt(start, end)
        }

        /**
         * Indicates if the given migration is contained within the [MigrationContainer] based on
         * its start-end versions.
         *
         * @param startVersion Start version of the migration.
         * @param endVersion End version of the migration
         * @return True if it contains a migration with the same start-end version, false otherwise.
         */
        public actual fun contains(startVersion: Int, endVersion: Int): Boolean {
            return this.containsCommon(startVersion, endVersion)
        }

        internal actual fun getSortedNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>? {
            val targetNodes = migrations[migrationStart] ?: return null
            return targetNodes to targetNodes.keys
        }

        internal actual fun getSortedDescendingNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>? {
            val targetNodes = migrations[migrationStart] ?: return null
            return targetNodes to targetNodes.descendingKeySet()
        }
    }

    /** Callback for [RoomDatabase]. */
    public actual abstract class Callback {
        /**
         * Called when the database is created for the first time. This is called after all the
         * tables are created.
         *
         * This function is only called when Room is configured without a driver. If a driver is set
         * using [Builder.setDriver], then only the version that receives a [SQLiteConnection] is
         * called.
         *
         * @param db The database.
         */
        public open fun onCreate(db: SupportSQLiteDatabase) {}

        /**
         * Called when the database is created for the first time.
         *
         * This function called after all the tables are created.
         *
         * @param connection The database connection.
         */
        public actual open fun onCreate(connection: SQLiteConnection) {
            if (connection is SupportSQLiteConnection) {
                onCreate(connection.db)
            }
        }

        /**
         * Called after the database was destructively migrated
         *
         * This function is only called when Room is configured without a driver. If a driver is set
         * using [Builder.setDriver], then only the version that receives a [SQLiteConnection] is
         * called.
         *
         * @param db The database.
         */
        public open fun onDestructiveMigration(db: SupportSQLiteDatabase) {}

        /**
         * Called after the database was destructively migrated.
         *
         * @param connection The database connection.
         */
        public actual open fun onDestructiveMigration(connection: SQLiteConnection) {
            if (connection is SupportSQLiteConnection) {
                onDestructiveMigration(connection.db)
            }
        }

        /**
         * Called when the database has been opened.
         *
         * This function is only called when Room is configured without a driver. If a driver is set
         * using [Builder.setDriver], then only the version that receives a [SQLiteConnection] is
         * called.
         *
         * @param db The database.
         */
        public open fun onOpen(db: SupportSQLiteDatabase) {}

        /**
         * Called when the database has been opened.
         *
         * @param connection The database connection.
         */
        public actual open fun onOpen(connection: SQLiteConnection) {
            if (connection is SupportSQLiteConnection) {
                onOpen(connection.db)
            }
        }
    }

    /**
     * Callback for [Builder.createFromAsset], [Builder.createFromFile] and
     * [Builder.createFromInputStream]
     *
     * This callback will be invoked after the pre-package DB is copied but before Room had a chance
     * to open it and therefore before the [RoomDatabase.Callback] methods are invoked. This
     * callback can be useful for updating the pre-package DB schema to satisfy Room's schema
     * validation.
     */
    public abstract class PrepackagedDatabaseCallback {
        /**
         * Called when the pre-packaged database has been copied.
         *
         * @param db The database.
         */
        public open fun onOpenPrepackagedDatabase(db: SupportSQLiteDatabase) {}
    }

    /**
     * Callback interface for when SQLite queries are executed.
     *
     * Can be set using [RoomDatabase.Builder.setQueryCallback].
     */
    public fun interface QueryCallback {
        /**
         * Called when a SQL query is executed.
         *
         * @param sqlQuery The SQLite query statement.
         * @param bindArgs Arguments of the query if available, empty list otherwise.
         */
        public fun onQuery(sqlQuery: String, bindArgs: List<Any?>)
    }

    public companion object {
        /**
         * Unfortunately, we cannot read this value so we are only setting it to the SQLite default.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
        public const val MAX_BIND_PARAMETER_CNT: Int = 999
    }
}

/**
 * Calls the specified suspending [block] in a database transaction. The transaction will be marked
 * as successful unless an exception is thrown in the suspending [block] or the coroutine is
 * cancelled.
 *
 * Room will only perform at most one transaction at a time, additional transactions are queued and
 * executed on a first come, first serve order.
 *
 * Performing blocking database operations is not permitted in a coroutine scope other than the one
 * received by the suspending block. It is recommended that all [Dao] function invoked within the
 * [block] be suspending functions.
 *
 * The internal dispatcher used to execute the given [block] will block an utilize a thread from
 * Room's transaction executor until the [block] is complete.
 */
public suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R =
    withTransactionContext {
        @Suppress("DEPRECATION") beginTransaction()
        try {
            val result = block.invoke()
            @Suppress("DEPRECATION") setTransactionSuccessful()
            result
        } finally {
            @Suppress("DEPRECATION") endTransaction()
        }
    }

/** Calls the specified suspending [block] with Room's transaction context. */
internal suspend fun <R> RoomDatabase.withTransactionContext(block: suspend () -> R): R {
    val transactionBlock: suspend CoroutineScope.() -> R = transaction@{
        checkNotNull(coroutineContext[TransactionElement]) {
            "Expected a TransactionElement in the CoroutineContext but none was found."
        }
        return@transaction block.invoke()
    }
    // Use inherited transaction context if available, this allows nested suspending transactions.
    val transactionDispatcher = coroutineContext[TransactionElement]?.transactionDispatcher
    return if (transactionDispatcher != null) {
        withContext(transactionDispatcher, transactionBlock)
    } else {
        startTransactionCoroutine(transactionBlock)
    }
}

/**
 * Suspend caller coroutine and start the transaction coroutine in a thread from the
 * [RoomDatabase.transactionExecutor], resuming the caller coroutine with the result once done. The
 * caller's `context` will be a parent of the started coroutine to propagating cancellation and
 * release the thread when cancelled.
 */
private suspend fun <R> RoomDatabase.startTransactionCoroutine(
    transactionBlock: suspend CoroutineScope.() -> R
): R = suspendCancellableCoroutine { continuation ->
    try {
        transactionExecutor.execute {
            try {
                // Thread acquired, start the transaction coroutine using the parent context.
                // The started coroutine will have an event loop dispatcher that we'll use for the
                // transaction context.
                runBlocking(continuation.context.minusKey(ContinuationInterceptor)) {
                    val dispatcher = coroutineContext[ContinuationInterceptor]!!
                    val transactionContext = createTransactionContext(dispatcher)
                    continuation.resume(withContext(transactionContext, transactionBlock))
                }
            } catch (ex: Throwable) {
                // If anything goes wrong, propagate exception to the calling coroutine.
                continuation.cancel(ex)
            }
        }
    } catch (ex: RejectedExecutionException) {
        // Couldn't acquire a thread, cancel coroutine.
        continuation.cancel(
            IllegalStateException(
                "Unable to acquire a thread to perform the database transaction.",
                ex,
            )
        )
    }
}

/**
 * Creates a [CoroutineContext] for performing database operations within a coroutine transaction.
 *
 * The context is a combination of a dispatcher, a [TransactionElement] and a thread local element.
 * * The dispatcher will dispatch coroutines to a single thread that is taken over from the Room
 *   transaction executor. If the coroutine context is switched, suspending DAO functions will be
 *   able to dispatch to the transaction thread. In reality the dispatcher is the event loop of a
 *   [runBlocking] started on the dedicated thread.
 * * The [TransactionElement] serves as an indicator for inherited context, meaning, if there is a
 *   switch of context, suspending DAO methods will be able to use the indicator to dispatch the
 *   database operation to the transaction thread.
 * * The thread local element serves as a second indicator and marks threads that are used to
 *   execute coroutines within the coroutine transaction, more specifically it allows us to identify
 *   if a blocking DAO method is invoked within the transaction coroutine.
 */
private fun RoomDatabase.createTransactionContext(
    dispatcher: ContinuationInterceptor
): CoroutineContext {
    val baseContext = dispatcher + TransactionElement(dispatcher)
    val threadLocalElement = suspendingTransactionContext.asContextElement(baseContext)
    return baseContext + threadLocalElement
}

/**
 * A [CoroutineContext.Element] that indicates there is an on-going database transaction.
 *
 * Even though all this element contains is a [ContinuationInterceptor], it is required since its
 * key will be unique which prevents the interceptor to be overridden during a context folding.
 */
internal class TransactionElement(internal val transactionDispatcher: ContinuationInterceptor) :
    CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<TransactionElement>

    override val key: CoroutineContext.Key<TransactionElement>
        get() = TransactionElement
}

/**
 * Creates a [Flow] that listens for changes in the database via the [InvalidationTracker] and emits
 * sets of the tables that were invalidated.
 *
 * The Flow will emit at least one value, a set of all the tables registered for observation to
 * kick-start the stream unless [emitInitialState] is set to `false`.
 *
 * If one of the tables to observe does not exist in the database, this functions throws an
 * [IllegalArgumentException].
 *
 * The returned Flow can be used to create a stream that reacts to changes in the database:
 * ```
 * fun getArtistTours(from: Date, to: Date): Flow<Map<Artist, TourState>> {
 *   return db.invalidationTrackerFlow("Artist").map { _ ->
 *     val artists = artistsDao.getAllArtists()
 *     val tours = tourService.fetchStates(artists.map { it.id })
 *     associateTours(artists, tours, from, to)
 *   }
 * }
 * ```
 *
 * @param tables The name of the tables or views to observe.
 * @param emitInitialState Set to `false` if no initial emission is desired. Default value is
 *   `true`.
 */
@Deprecated(
    message = "Replaced by equivalent API in InvalidationTracker.",
    replaceWith = ReplaceWith("this.invalidationTracker.createFlow(*tables)"),
)
public fun RoomDatabase.invalidationTrackerFlow(
    vararg tables: String,
    emitInitialState: Boolean = true,
): Flow<Set<String>> = invalidationTracker.createFlow(*tables, emitInitialState = emitInitialState)

/**
 * Compatibility suspend transaction execution with driver usage. This will maintain the dispatcher
 * behaviour in [withTransaction] when Room is in compatibility mode executing driver transactions
 * and maintains compatibility with suspend DAO usages.
 */
internal suspend fun <R> RoomDatabase.compatTransactionCoroutineExecute(block: suspend () -> R): R {
    if (inCompatibilityMode() && isOpenInternal && inTransaction()) {
        return block.invoke()
    }
    if (coroutineContext[RoomExternalOperationElement] == null) {
        return block.invoke()
    }
    return withTransactionContext(block)
}
