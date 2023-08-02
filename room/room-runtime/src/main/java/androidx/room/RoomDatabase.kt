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
package androidx.room

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.Room.LOG_TAG
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import java.io.InputStream
import java.util.BitSet
import java.util.Collections
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Base class for all Room databases. All classes that are annotated with [Database] must
 * extend this class.
 *
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using [Dao] classes.
 *
 * @constructor You cannot create an instance of a database, instead, you should acquire it via
 * [#Room.databaseBuilder] or
 * [#Room.inMemoryDatabaseBuilder].
 *
 * @see Database
 */
abstract class RoomDatabase {
    /**
     * Set by the generated open helper.
     *
     */
    @Volatile
    @Deprecated("Will be hidden in the next release.")
    @JvmField
    protected var mDatabase: SupportSQLiteDatabase? = null

    /**
     * The Executor in use by this database for async queries.
     */
    open val queryExecutor: Executor
        get() = internalQueryExecutor

    private lateinit var internalQueryExecutor: Executor

    /**
     * The Executor in use by this database for async transactions.
     */
    open val transactionExecutor: Executor
        get() = internalTransactionExecutor

    private lateinit var internalTransactionExecutor: Executor

    /**
     * The SQLite open helper used by this database.
     */
    open val openHelper: SupportSQLiteOpenHelper
        get() = internalOpenHelper

    private lateinit var internalOpenHelper: SupportSQLiteOpenHelper

    /**
     * The invalidation tracker for this database.
     *
     * You can use the invalidation tracker to get notified when certain tables in the database
     * are modified.
     *
     * @return The invalidation tracker for the database.
     */
    open val invalidationTracker: InvalidationTracker = createInvalidationTracker()
    private var allowMainThreadQueries = false
    private var writeAheadLoggingEnabled = false

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("Will be hidden in a future release.")
    @JvmField
    protected var mCallbacks: List<Callback>? = null

    /**
     * A map of auto migration spec classes to their provided instance.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected var autoMigrationSpecs: MutableMap<Class<out AutoMigrationSpec>, AutoMigrationSpec> =
        mutableMapOf()
    private val readWriteLock = ReentrantReadWriteLock()
    private var autoCloser: AutoCloser? = null

    /**
     * [InvalidationTracker] uses this lock to prevent the database from closing while it is
     * querying database updates.
     *
     * The returned lock is reentrant and will allow multiple threads to acquire the lock
     * simultaneously until [close] is invoked in which the lock becomes exclusive as
     * a way to let the InvalidationTracker finish its work before closing the database.
     *
     * @return The lock for [close].
     */
    internal fun getCloseLock(): Lock {
        return readWriteLock.readLock()
    }

    /**
     * Suspending transaction id of the current thread.
     *
     * This id is only set on threads that are used to dispatch coroutines within a suspending
     * database transaction.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val suspendingTransactionId = ThreadLocal<Int>()

    /**
     * Gets the map for storing extension properties of Kotlin type.
     *
     * @hide
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val backingFieldMap: MutableMap<String, Any> = Collections.synchronizedMap(mutableMapOf())

    private val typeConverters: MutableMap<Class<*>, Any> = mutableMapOf()

    /**
     * Gets the instance of the given Type Converter.
     *
     * @param klass The Type Converter class.
     * @param T The type of the expected Type Converter subclass.
     * @return An instance of T if it is provided in the builder.
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T> getTypeConverter(klass: Class<T>): T? {
        return typeConverters[klass] as T?
    }

    /**
     * Called by Room when it is initialized.
     *
     * @throws IllegalArgumentException if initialization fails.
     *
     * @param configuration The database configuration.
     */
    @CallSuper
    @Suppress("DEPRECATION")
    open fun init(configuration: DatabaseConfiguration) {
        internalOpenHelper = createOpenHelper(configuration)
        val requiredAutoMigrationSpecs = getRequiredAutoMigrationSpecs()
        val usedSpecs = BitSet()
        for (spec in requiredAutoMigrationSpecs) {
            var foundIndex = -1
            for (providedIndex in configuration.autoMigrationSpecs.indices.reversed()) {
                val provided: Any = configuration.autoMigrationSpecs[providedIndex]
                if (spec.isAssignableFrom(provided.javaClass)) {
                    foundIndex = providedIndex
                    usedSpecs.set(foundIndex)
                    break
                }
            }
            require(foundIndex >= 0) {
                "A required auto migration spec (${spec.canonicalName}) is missing in the " +
                    "database configuration."
            }
            autoMigrationSpecs[spec] = configuration.autoMigrationSpecs[foundIndex]
        }
        for (providedIndex in configuration.autoMigrationSpecs.indices.reversed()) {
            require(usedSpecs[providedIndex]) {
                "Unexpected auto migration specs found. " +
                    "Annotate AutoMigrationSpec implementation with " +
                    "@ProvidedAutoMigrationSpec annotation or remove this spec from the " +
                    "builder."
            }
        }
        val autoMigrations = getAutoMigrations(autoMigrationSpecs)
        for (autoMigration in autoMigrations) {
            val migrationExists = configuration.migrationContainer.contains(
                autoMigration.startVersion,
                autoMigration.endVersion
            )
            if (!migrationExists) {
                configuration.migrationContainer.addMigrations(autoMigration)
            }
        }

        // Configure SqliteCopyOpenHelper if it is available:
        unwrapOpenHelper(
                clazz = SQLiteCopyOpenHelper::class.java,
                openHelper = openHelper
            )?.setDatabaseConfiguration(configuration)

        unwrapOpenHelper(
            clazz = AutoClosingRoomOpenHelper::class.java,
            openHelper = openHelper
        )?.let {
            autoCloser = it.autoCloser
            invalidationTracker.setAutoCloser(it.autoCloser)
        }

        val wal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val enabled = configuration.journalMode == JournalMode.WRITE_AHEAD_LOGGING
            openHelper.setWriteAheadLoggingEnabled(enabled)
            enabled
        } else {
            false
        }
        mCallbacks = configuration.callbacks
        internalQueryExecutor = configuration.queryExecutor
        internalTransactionExecutor = TransactionExecutor(configuration.transactionExecutor)
        allowMainThreadQueries = configuration.allowMainThreadQueries
        writeAheadLoggingEnabled = wal
        if (configuration.multiInstanceInvalidationServiceIntent != null) {
            requireNotNull(configuration.name)
            invalidationTracker.startMultiInstanceInvalidation(
                configuration.context,
                configuration.name,
                configuration.multiInstanceInvalidationServiceIntent
            )
        }
        val requiredFactories = getRequiredTypeConverters()
        // indices for each converter on whether it is used or not so that we can throw an exception
        // if developer provides an unused converter. It is not necessarily an error but likely
        // to be because why would developer add a converter if it won't be used?
        val used = BitSet()
        requiredFactories.forEach { (daoName, converters) ->
            for (converter in converters) {
                var foundIndex = -1
                // traverse provided converters in reverse so that newer one overrides
                for (providedIndex in configuration.typeConverters.indices.reversed()) {
                    val provided = configuration.typeConverters[providedIndex]
                    if (converter.isAssignableFrom(provided.javaClass)) {
                        foundIndex = providedIndex
                        used.set(foundIndex)
                        break
                    }
                }
                require(foundIndex >= 0) {
                    "A required type converter ($converter) for" +
                        " ${daoName.canonicalName} is missing in the database configuration."
                }
                typeConverters[converter] = configuration.typeConverters[foundIndex]
            }
        }
        // now, make sure all provided factories are used
        for (providedIndex in configuration.typeConverters.indices.reversed()) {
            if (!used[providedIndex]) {
                val converter = configuration.typeConverters[providedIndex]
                throw IllegalArgumentException(
                    "Unexpected type converter $converter. " +
                        "Annotate TypeConverter class with @ProvidedTypeConverter annotation " +
                        "or remove this converter from the builder."
                )
            }
        }
    }

    /**
     * Returns a list of [Migration] of a database that have been automatically generated.
     *
     * @return A list of migration instances each of which is a generated autoMigration
     * @param autoMigrationSpecs
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmSuppressWildcards // Suppress wildcards due to generated Java code
    open fun getAutoMigrations(
        autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return emptyList()
    }

    /**
     * Unwraps (delegating) open helpers until it finds clazz, otherwise returns null.
     *
     * @param clazz the open helper type to search for
     * @param openHelper the open helper to search through
     * @param T the type of clazz
     * @return the instance of clazz, otherwise null
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> unwrapOpenHelper(clazz: Class<T>, openHelper: SupportSQLiteOpenHelper): T? {
        if (clazz.isInstance(openHelper)) {
            return openHelper as T
        }
        return if (openHelper is DelegatingOpenHelper) {
            unwrapOpenHelper(
                clazz = clazz,
                openHelper = openHelper.delegate
            )
        } else null
    }

    /**
     * Creates the open helper to access the database. Generated class already implements this
     * method.
     * Note that this method is called when the RoomDatabase is initialized.
     *
     * @param config The configuration of the Room database.
     * @return A new SupportSQLiteOpenHelper to be used while connecting to the database.
     */
    protected abstract fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper

    /**
     * Called when the RoomDatabase is created.
     *
     * This is already implemented by the generated code.
     *
     * @return Creates a new InvalidationTracker.
     */
    protected abstract fun createInvalidationTracker(): InvalidationTracker

    /**
     * Returns a Map of String -> List&lt;Class&gt; where each entry has the `key` as the DAO name
     * and `value` as the list of type converter classes that are necessary for the database to
     * function.
     *
     * This is implemented by the generated code.
     *
     * @return Creates a map that will include all required type converters for this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected open fun getRequiredTypeConverters(): Map<Class<*>, List<Class<*>>> {
        return emptyMap()
    }

    /**
     * Returns a Set of required AutoMigrationSpec classes.
     *
     * This is implemented by the generated code.
     *
     * @return Creates a set that will include all required auto migration specs for this database.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
        return emptySet()
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
    @WorkerThread
    abstract fun clearAllTables()

    /**
     * True if database connection is open and initialized.
     *
     * @return true if the database connection is open, false otherwise.
     */
    @Suppress("Deprecation")
    open val isOpen: Boolean
        get() {
            return (autoCloser?.isActive ?: mDatabase?.isOpen) == true
        }

    /**
     * Closes the database if it is already open.
     */
    open fun close() {
        if (isOpen) {
            val closeLock: Lock = readWriteLock.writeLock()
            closeLock.lock()
            try {
                invalidationTracker.stopMultiInstanceInvalidation()
                openHelper.close()
            } finally {
                closeLock.unlock()
            }
        }
    }

    /** True if the calling thread is the main thread.  */
    internal val isMainThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    /**
     * Asserts that we are not on the main thread.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    open fun assertNotMainThread() {
        if (allowMainThreadQueries) {
            return
        }
        check(!isMainThread) {
            "Cannot access database on the main thread since" +
                " it may potentially lock the UI for a long period of time."
        }
    }

    /**
     * Asserts that we are not on a suspending transaction.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // used in generated code
    open fun assertNotSuspendingTransaction() {
        check(inTransaction() || suspendingTransactionId.get() == null) {
            "Cannot access database on a different coroutine" +
                " context inherited from a suspending transaction."
        }
    }
    // Below, there are wrapper methods for SupportSQLiteDatabase. This helps us track which
    // methods we are using and also helps unit tests to mock this class without mocking
    // all SQLite database methods.
    /**
     * Convenience method to query the database with arguments.
     *
     * @param query The sql query
     * @param args  The bind arguments for the placeholders in the query
     * @return A Cursor obtained by running the given query in the Room database.
     */
    open fun query(query: String, args: Array<Any?>?): Cursor {
        return openHelper.writableDatabase.query(SimpleSQLiteQuery(query, args))
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.query].
     *
     * @param query The Query which includes the SQL and a bind callback for bind arguments.
     * @param signal The cancellation signal to be attached to the query.
     * @return Result of the query.
     */
    @JvmOverloads
    open fun query(query: SupportSQLiteQuery, signal: CancellationSignal? = null): Cursor {
        assertNotMainThread()
        assertNotSuspendingTransaction()
        return if (signal != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
     */
    open fun compileStatement(sql: String): SupportSQLiteStatement {
        assertNotMainThread()
        assertNotSuspendingTransaction()
        return openHelper.writableDatabase.compileStatement(sql)
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.beginTransaction].
     */
    @Deprecated(
        "beginTransaction() is deprecated",
        ReplaceWith("runInTransaction(Runnable)")
    )
    open fun beginTransaction() {
        assertNotMainThread()
        val autoCloser = autoCloser
        if (autoCloser == null) {
            internalBeginTransaction()
        } else {
            autoCloser.executeRefCountingFunction<Any?> {
                internalBeginTransaction()
                null
            }
        }
    }

    private fun internalBeginTransaction() {
        assertNotMainThread()
        val database = openHelper.writableDatabase
        invalidationTracker.syncTriggers(database)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
            database.isWriteAheadLoggingEnabled
        ) {
            database.beginTransactionNonExclusive()
        } else {
            database.beginTransaction()
        }
    }

    /**
     * Wrapper for [SupportSQLiteDatabase.endTransaction].
     */
    @Deprecated(
        "endTransaction() is deprecated",
        ReplaceWith("runInTransaction(Runnable)")
    )
    open fun endTransaction() {
        val autoCloser = autoCloser
        if (autoCloser == null) {
            internalEndTransaction()
        } else {
            autoCloser.executeRefCountingFunction<Any?> {
                internalEndTransaction()
                null
            }
        }
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
     */
    @Deprecated(
        "setTransactionSuccessful() is deprecated",
        ReplaceWith("runInTransaction(Runnable)")
    )
    open fun setTransactionSuccessful() {
        openHelper.writableDatabase.setTransactionSuccessful()
    }

    /**
     * Executes the specified [Runnable] in a database transaction. The transaction will be
     * marked as successful unless an exception is thrown in the [Runnable].
     *
     * Room will only perform at most one transaction at a time.
     *
     * @param body The piece of code to execute.
     */
    @Suppress("DEPRECATION")
    open fun runInTransaction(body: Runnable) {
        beginTransaction()
        try {
            body.run()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    /**
     * Executes the specified [Callable] in a database transaction. The transaction will be
     * marked as successful unless an exception is thrown in the [Callable].
     *
     * Room will only perform at most one transaction at a time.
     *
     * @param body The piece of code to execute.
     * @param V  The type of the return value.
     * @return The value returned from the [Callable].
     */
    @Suppress("DEPRECATION")
    open fun <V> runInTransaction(body: Callable<V>): V {
        beginTransaction()
        return try {
            val result = body.call()
            setTransactionSuccessful()
            result
        } finally {
            endTransaction()
        }
    }

    /**
     * Called by the generated code when database is open.
     *
     * You should never call this method manually.
     *
     * @param db The database instance.
     */
    protected open fun internalInitInvalidationTracker(db: SupportSQLiteDatabase) {
        invalidationTracker.internalInit(db)
    }

    /**
     * Returns true if current thread is in a transaction.
     *
     * @return True if there is an active transaction in current thread, false otherwise.
     * @see SupportSQLiteDatabase.inTransaction
     */
    open fun inTransaction(): Boolean {
        return openHelper.writableDatabase.inTransaction()
    }

    /**
     * Journal modes for SQLite database.
     *
     * @see Builder.setJournalMode
     */
    enum class JournalMode {
        /**
         * Let Room choose the journal mode. This is the default value when no explicit value is
         * specified.
         *
         * The actual value will be [TRUNCATE] when the device runs API Level lower than 16
         * or it is a low-RAM device. Otherwise, [WRITE_AHEAD_LOGGING] will be used.
         */
        AUTOMATIC,

        /**
         * Truncate journal mode.
         */
        TRUNCATE,

        /**
         * Write-Ahead Logging mode.
         */
        WRITE_AHEAD_LOGGING;

        /**
         * Resolves [AUTOMATIC] to either [TRUNCATE] or [WRITE_AHEAD_LOGGING].
         */
        internal fun resolve(context: Context): JournalMode {
            if (this != AUTOMATIC) {
                return this
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                if (!isLowRamDevice(manager)) {
                    return WRITE_AHEAD_LOGGING
                }
            }
            return TRUNCATE
        }

        private fun isLowRamDevice(activityManager: ActivityManager) =
            if (Build.VERSION.SDK_INT >= 19) {
                SupportSQLiteCompat.Api19Impl.isLowRamDevice(activityManager)
            } else {
                false
            }
    }

    /**
     * Builder for RoomDatabase.
     *
     * @param T The type of the abstract database class.
     */
    @Suppress("GetterOnBuilder") // To keep ABI compatibility from Java
    open class Builder<T : RoomDatabase> internal constructor(
        private val context: Context,
        private val klass: Class<T>,
        private val name: String?
    ) {
        private val callbacks: MutableList<Callback> = mutableListOf()
        private var prepackagedDatabaseCallback: PrepackagedDatabaseCallback? = null
        private var queryCallback: QueryCallback? = null
        private var queryCallbackExecutor: Executor? = null
        private val typeConverters: MutableList<Any> = mutableListOf()
        private var autoMigrationSpecs: MutableList<AutoMigrationSpec> = mutableListOf()

        private var queryExecutor: Executor? = null

        private var transactionExecutor: Executor? = null
        private var factory: SupportSQLiteOpenHelper.Factory? = null
        private var allowMainThreadQueries = false
        private var journalMode: JournalMode = JournalMode.AUTOMATIC
        private var multiInstanceInvalidationIntent: Intent? = null
        private var requireMigration: Boolean = true
        private var allowDestructiveMigrationOnDowngrade = false
        private var autoCloseTimeout = -1L
        private var autoCloseTimeUnit: TimeUnit? = null

        /**
         * Migrations, mapped by from-to pairs.
         */
        private val migrationContainer: MigrationContainer = MigrationContainer()
        private var migrationsNotRequiredFrom: MutableSet<Int> = mutableSetOf()

        /**
         * Keeps track of [Migration.startVersion]s and [Migration.endVersion]s added in
         * [addMigrations] for later validation that makes those versions don't
         * match any versions passed to [fallbackToDestructiveMigrationFrom].
         */
        private var migrationStartAndEndVersions: MutableSet<Int>? = null
        private var copyFromAssetPath: String? = null
        private var copyFromFile: File? = null
        private var copyFromInputStream: Callable<InputStream>? = null

        /**
         * Configures Room to create and open the database using a pre-packaged database located in
         * the application 'assets/' folder.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The pre-packaged database file must be located in
         * the "assets/" folder of your application. For example, the path for a file located in
         * "assets/databases/products.db" would be "databases/products.db".
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFilePath The file path within the 'assets/' directory of where the
         * database file is located.
         *
         * @return This builder instance.
         */
        open fun createFromAsset(databaseFilePath: String) = apply {
            this.copyFromAssetPath = databaseFilePath
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database located in
         * the application 'assets/' folder.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The pre-packaged database file must be located in
         * the "assets/" folder of your application. For example, the path for a file located in
         * "assets/databases/products.db" would be "databases/products.db".
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFilePath The file path within the 'assets/' directory of where the
         * database file is located.
         * @param callback The pre-packaged callback.
         *
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle") // To keep naming consistency.
        open fun createFromAsset(
            databaseFilePath: String,
            callback: PrepackagedDatabaseCallback
        ) = apply {
            this.prepackagedDatabaseCallback = callback
            this.copyFromAssetPath = databaseFilePath
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database file.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The given file must be accessible and the right
         * permissions must be granted for Room to copy the file.
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * The [Callback.onOpen] method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFile The database file.
         *
         * @return This builder instance.
         */
        open fun createFromFile(databaseFile: File) = apply {
            this.copyFromFile = databaseFile
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database file.
         *
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The given file must be accessible and the right
         * permissions must be granted for Room to copy the file.
         *
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * [Database.exportSchema] is enabled.
         *
         * The [Callback.onOpen] method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param databaseFile The database file.
         * @param callback The pre-packaged callback.
         *
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle", "StreamFiles") // To keep naming consistency.
        open fun createFromFile(
            databaseFile: File,
            callback: PrepackagedDatabaseCallback
        ) = apply {
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
         * The [Callback.onOpen] method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param inputStreamCallable A callable that returns an InputStream from which to copy
         * the database. The callable will be invoked in a thread from
         * the Executor set via [setQueryExecutor]. The
         * callable is only invoked if Room needs to create and open the
         * database from the pre-package database, usually the first time
         * it is created or during a destructive migration.
         *
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle") // To keep naming consistency.
        open fun createFromInputStream(
            inputStreamCallable: Callable<InputStream>
        ) = apply {
            this.copyFromInputStream = inputStreamCallable
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
         * The [Callback.onOpen] method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         *
         * This method is not supported for an in memory database [Builder].
         *
         * @param inputStreamCallable A callable that returns an InputStream from which to copy
         * the database. The callable will be invoked in a thread from
         * the Executor set via [setQueryExecutor]. The
         * callable is only invoked if Room needs to create and open the
         * database from the pre-package database, usually the first time
         * it is created or during a destructive migration.
         * @param callback The pre-packaged callback.
         *
         * @return This builder instance.
         */
        @SuppressLint("BuilderSetStyle", "LambdaLast") // To keep naming consistency.
        open fun createFromInputStream(
            inputStreamCallable: Callable<InputStream>,
            callback: PrepackagedDatabaseCallback
        ) = apply {
            this.prepackagedDatabaseCallback = callback
            this.copyFromInputStream = inputStreamCallable
        }

        /**
         * Sets the database factory. If not set, it defaults to
         * [FrameworkSQLiteOpenHelperFactory].
         *
         * @param factory The factory to use to access the database.
         * @return This builder instance.
         */
        open fun openHelperFactory(factory: SupportSQLiteOpenHelper.Factory?) = apply {
            this.factory = factory
        }

        /**
         * Adds a migration to the builder.
         *
         * Each Migration has a start and end versions and Room runs these migrations to bring the
         * database to the latest version.
         *
         * If a migration item is missing between current version and the latest version, Room
         * will clear the database and recreate so even if you have no changes between 2 versions,
         * you should still provide a Migration object to the builder.
         *
         * A migration can handle more than 1 version (e.g. if you have a faster path to choose when
         * going version 3 to 5 without going to version 4). If Room opens a database at version
         * 3 and latest version is &gt;= 5, Room will use the migration object that can migrate from
         * 3 to 5 instead of 3 to 4 and 4 to 5.
         *
         * @param migrations The migration object that can modify the database and to the necessary
         * changes.
         * @return This builder instance.
         */
        open fun addMigrations(vararg migrations: Migration) = apply {
            if (migrationStartAndEndVersions == null) {
                migrationStartAndEndVersions = HashSet()
            }
            for (migration in migrations) {
                migrationStartAndEndVersions!!.add(migration.startVersion)
                migrationStartAndEndVersions!!.add(migration.endVersion)
            }
            migrationContainer.addMigrations(*migrations)
        }

        /**
         * Adds an auto migration spec to the builder.
         *
         * @param autoMigrationSpec The auto migration object that is annotated with
         * [AutoMigrationSpec] and is declared in an [AutoMigration] annotation.
         * @return This builder instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        open fun addAutoMigrationSpec(autoMigrationSpec: AutoMigrationSpec) = apply {
            this.autoMigrationSpecs.add(autoMigrationSpec)
        }

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
        open fun allowMainThreadQueries() = apply {
            this.allowMainThreadQueries = true
        }

        /**
         * Sets the journal mode for this database.
         *
         * This value is ignored if the builder is initialized with
         * [Room.inMemoryDatabaseBuilder].
         *
         * The journal mode should be consistent across multiple instances of
         * [RoomDatabase] for a single SQLite database file.
         *
         * The default value is [JournalMode.AUTOMATIC].
         *
         * @param journalMode The journal mode.
         * @return This builder instance.
         */
        open fun setJournalMode(journalMode: JournalMode) = apply {
            this.journalMode = journalMode
        }

        /**
         * Sets the [Executor] that will be used to execute all non-blocking asynchronous
         * queries and tasks, including `LiveData` invalidation, `Flowable` scheduling
         * and `ListenableFuture` tasks.
         *
         * When both the query executor and transaction executor are unset, then a default
         * `Executor` will be used. The default `Executor` allocates and shares threads
         * amongst Architecture Components libraries. If the query executor is unset but a
         * transaction executor was set [setTransactionExecutor], then the same `Executor` will be
         * used for queries.
         *
         * For best performance the given `Executor` should be bounded (max number of threads
         * is limited).
         *
         * The input `Executor` cannot run tasks on the UI thread.
         *
         * @return This builder instance.
         */
        open fun setQueryExecutor(executor: Executor) = apply {
            this.queryExecutor = executor
        }

        /**
         * Sets the [Executor] that will be used to execute all non-blocking asynchronous
         * transaction queries and tasks, including `LiveData` invalidation, `Flowable`
         * scheduling and `ListenableFuture` tasks.
         *
         * When both the transaction executor and query executor are unset, then a default
         * `Executor` will be used. The default `Executor` allocates and shares threads
         * amongst Architecture Components libraries. If the transaction executor is unset but a
         * query executor was set using [setQueryExecutor], then the same `Executor` will be used
         * for transactions.
         *
         * If the given `Executor` is shared then it should be unbounded to avoid the
         * possibility of a deadlock. Room will not use more than one thread at a time from this
         * executor since only one transaction at a time can be executed, other transactions will
         * be queued on a first come, first serve order.
         *
         * The input `Executor` cannot run tasks on the UI thread.
         *
         * @return This builder instance.
         */
        open fun setTransactionExecutor(executor: Executor) = apply {
            this.transactionExecutor = executor
        }

        /**
         * Sets whether table invalidation in this instance of [RoomDatabase] should be
         * broadcast and synchronized with other instances of the same [RoomDatabase],
         * including those in a separate process. In order to enable multi-instance invalidation,
         * this has to be turned on both ends.
         *
         * This is not enabled by default.
         *
         * This does not work for in-memory databases. This does not work between database instances
         * targeting different database files.
         *
         * @return This builder instance.
         */
        @Suppress("UnsafeOptInUsageError")
        open fun enableMultiInstanceInvalidation() = apply {
            this.multiInstanceInvalidationIntent = if (name != null) {
                Intent(context, MultiInstanceInvalidationService::class.java)
            } else {
                null
            }
        }

        /**
         * Sets whether table invalidation in this instance of [RoomDatabase] should be
         * broadcast and synchronized with other instances of the same [RoomDatabase],
         * including those in a separate process. In order to enable multi-instance invalidation,
         * this has to be turned on both ends and need to point to the same
         * [MultiInstanceInvalidationService].
         *
         * This is not enabled by default.
         *
         * This does not work for in-memory databases. This does not work between database instances
         * targeting different database files.
         *
         * @param invalidationServiceIntent Intent to bind to the
         * [MultiInstanceInvalidationService].
         * @return This builder instance.
         */
        @ExperimentalRoomApi
        @Suppress("MissingGetterMatchingBuilder")
        open fun setMultiInstanceInvalidationServiceIntent(
            invalidationServiceIntent: Intent
        ) = apply {
            this.multiInstanceInvalidationIntent =
                if (name != null) invalidationServiceIntent else null
        }

        /**
         * Allows Room to destructively recreate database tables if [Migration]s that would
         * migrate old database schemas to the latest schema version are not found.
         *
         * When the database version on the device does not match the latest schema version, Room
         * runs necessary [Migration]s on the database.
         *
         * If it cannot find the set of [Migration]s that will bring the database to the
         * current version, it will throw an [IllegalStateException].
         *
         * You can call this method to change this behavior to re-create the database instead of
         * crashing.
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
        open fun fallbackToDestructiveMigration() = apply {
            this.requireMigration = false
            this.allowDestructiveMigrationOnDowngrade = true
        }

        /**
         * Allows Room to destructively recreate database tables if [Migration]s are not
         * available when downgrading to old schema versions.
         *
         * For details, see [Builder.fallbackToDestructiveMigration].
         *
         * @return This builder instance.
         */
        open fun fallbackToDestructiveMigrationOnDowngrade() = apply {
            this.requireMigration = true
            this.allowDestructiveMigrationOnDowngrade = true
        }

        /**
         * Informs Room that it is allowed to destructively recreate database tables from specific
         * starting schema versions.
         *
         * This functionality is the same as that provided by
         * [fallbackToDestructiveMigration], except that this method allows the
         * specification of a set of schema versions for which destructive recreation is allowed.
         *
         * Using this method is preferable to [fallbackToDestructiveMigration] if you want
         * to allow destructive migrations from some schema versions while still taking advantage
         * of exceptions being thrown due to unintentionally missing migrations.
         *
         * Note: No versions passed to this method may also exist as either starting or ending
         * versions in the [Migration]s provided to [addMigrations]. If a
         * version passed to this method is found as a starting or ending version in a Migration, an
         * exception will be thrown.
         *
         * @param startVersions The set of schema versions from which Room should use a destructive
         * migration.
         * @return This builder instance.
         */
        open fun fallbackToDestructiveMigrationFrom(vararg startVersions: Int) = apply {
            for (startVersion in startVersions) {
                this.migrationsNotRequiredFrom.add(startVersion)
            }
        }

        /**
         * Adds a [Callback] to this database.
         *
         * @param callback The callback.
         * @return This builder instance.
         */
        open fun addCallback(callback: Callback) = apply {
            this.callbacks.add(callback)
        }

        /**
         * Sets a [QueryCallback] to be invoked when queries are executed.
         *
         * The callback is invoked whenever a query is executed, note that adding this callback
         * has a small cost and should be avoided in production builds unless needed.
         *
         * A use case for providing a callback is to allow logging executed queries. When the
         * callback implementation logs then it is recommended to use an immediate executor.
         *
         * @param queryCallback The query callback.
         * @param executor The executor on which the query callback will be invoked.
         * @return This builder instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        open fun setQueryCallback(
            queryCallback: QueryCallback,
            executor: Executor
        ) = apply {
            this.queryCallback = queryCallback
            this.queryCallbackExecutor = executor
        }

        /**
         * Adds a type converter instance to this database.
         *
         * @param typeConverter The converter. It must be an instance of a class annotated with
         * [ProvidedTypeConverter] otherwise Room will throw an exception.
         * @return This builder instance.
         */
        open fun addTypeConverter(typeConverter: Any) = apply {
            this.typeConverters.add(typeConverter)
        }

        /**
         * Enables auto-closing for the database to free up unused resources. The underlying
         * database will be closed after it's last use after the specified `autoCloseTimeout` has
         * elapsed since its last usage. The database will be automatically
         * re-opened the next time it is accessed.
         *
         * Auto-closing is not compatible with in-memory databases since the data will be lost
         * when the database is auto-closed.
         *
         * Also, temp tables and temp triggers will be cleared each time the database is
         * auto-closed. If you need to use them, please include them in your
         * callback [RoomDatabase.Callback.onOpen].
         *
         * All configuration should happen in your [RoomDatabase.Callback.onOpen]
         * callback so it is re-applied every time the database is re-opened. Note that the
         * [RoomDatabase.Callback.onOpen] will be called every time the database is re-opened.
         *
         * The auto-closing database operation runs on the query executor.
         *
         * The database will not be reopened if the RoomDatabase or the
         * SupportSqliteOpenHelper is closed manually (by calling
         * [RoomDatabase.close] or [SupportSQLiteOpenHelper.close]. If the
         * database is closed manually, you must create a new database using
         * [RoomDatabase.Builder.build].
         *
         * @param autoCloseTimeout the amount of time after the last usage before closing the
         * database. Must greater or equal to zero.
         * @param autoCloseTimeUnit the timeunit for autoCloseTimeout.
         * @return This builder instance.
         */
        @ExperimentalRoomApi // When experimental is removed, add these parameters to
        // DatabaseConfiguration
        @Suppress("MissingGetterMatchingBuilder")
        open fun setAutoCloseTimeout(
            @IntRange(from = 0) autoCloseTimeout: Long,
            autoCloseTimeUnit: TimeUnit
        ) = apply {
            require(autoCloseTimeout >= 0) { "autoCloseTimeout must be >= 0" }
            this.autoCloseTimeout = autoCloseTimeout
            this.autoCloseTimeUnit = autoCloseTimeUnit
        }

        /**
         * Creates the databases and initializes it.
         *
         * By default, all RoomDatabases use in memory storage for TEMP tables and enables recursive
         * triggers.
         *
         * @return A new database instance.
         */
        open fun build(): T {
            if (queryExecutor == null && transactionExecutor == null) {
                transactionExecutor = ArchTaskExecutor.getIOThreadExecutor()
                queryExecutor = transactionExecutor
            } else if (queryExecutor != null && transactionExecutor == null) {
                transactionExecutor = queryExecutor
            } else if (queryExecutor == null) {
                queryExecutor = transactionExecutor
            }
            if (migrationStartAndEndVersions != null) {
                for (version in migrationStartAndEndVersions!!) {
                    require(!migrationsNotRequiredFrom.contains(version)) {
                        "Inconsistency detected. A Migration was supplied to " +
                            "addMigration(Migration... migrations) that has a start " +
                            "or end version equal to a start version supplied to " +
                            "fallbackToDestructiveMigrationFrom(int... " +
                            "startVersions). Start version: $version"
                    }
                }
            }

            val factory: SupportSQLiteOpenHelper.Factory = if (factory == null) {
                FrameworkSQLiteOpenHelperFactory()
            } else {
                factory
            }?.let {
                if (autoCloseTimeout > 0) {
                    requireNotNull(name) {
                        "Cannot create auto-closing database for an in-memory database."
                    }
                    val autoCloser = AutoCloser(
                        autoCloseTimeout,
                        requireNotNull(autoCloseTimeUnit),
                        requireNotNull(queryExecutor)
                    )
                    AutoClosingRoomOpenHelperFactory(it, autoCloser)
                } else {
                    it
                }
            }?.let {
                if (
                    copyFromAssetPath != null ||
                    copyFromFile != null ||
                    copyFromInputStream != null
                ) {
                    requireNotNull(name) {
                        "Cannot create from asset or file for an in-memory database."
                    }

                    val copyFromAssetPathConfig = if (copyFromAssetPath == null) 0 else 1
                    val copyFromFileConfig = if (copyFromFile == null) 0 else 1
                    val copyFromInputStreamConfig = if (copyFromInputStream == null) 0 else 1
                    val copyConfigurations = copyFromAssetPathConfig + copyFromFileConfig +
                        copyFromInputStreamConfig

                    require(copyConfigurations == 1) {
                        "More than one of createFromAsset(), " +
                            "createFromInputStream(), and createFromFile() were called on this " +
                            "Builder, but the database can only be created using one of the " +
                            "three configurations."
                    }
                    SQLiteCopyOpenHelperFactory(
                        copyFromAssetPath,
                        copyFromFile,
                        copyFromInputStream,
                        it
                    )
                } else {
                    it
                }
            }.let {
                requireNotNull(it)
                if (queryCallback != null) {
                    QueryInterceptorOpenHelperFactory(
                        it,
                        requireNotNull(queryCallbackExecutor),
                        requireNotNull(queryCallback)
                    )
                } else {
                    it
                }
            }
            val configuration = DatabaseConfiguration(
                context,
                name,
                factory,
                migrationContainer,
                callbacks,
                allowMainThreadQueries,
                journalMode.resolve(context),
                requireNotNull(queryExecutor),
                requireNotNull(transactionExecutor),
                multiInstanceInvalidationIntent,
                requireMigration,
                allowDestructiveMigrationOnDowngrade,
                migrationsNotRequiredFrom,
                copyFromAssetPath,
                copyFromFile,
                copyFromInputStream,
                prepackagedDatabaseCallback,
                typeConverters,
                autoMigrationSpecs
            )
            val db = Room.getGeneratedImplementation<T, T>(klass, "_Impl")
            db.init(configuration)
            return db
        }
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    open class MigrationContainer {
        private val migrations = mutableMapOf<Int, TreeMap<Int, Migration>>()

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        open fun addMigrations(vararg migrations: Migration) {
            migrations.forEach(::addMigration)
        }

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        open fun addMigrations(migrations: List<Migration>) {
            migrations.forEach(::addMigration)
        }

        private fun addMigration(migration: Migration) {
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
        open fun getMigrations(): Map<Int, Map<Int, Migration>> {
            return migrations
        }

        /**
         * Finds the list of migrations that should be run to move from `start` version to
         * `end` version.
         *
         * @param start The current database version
         * @param end   The target database version
         * @return An ordered list of [Migration] objects that should be run to migrate
         * between the given versions. If a migration path cannot be found, returns `null`.
         */
        open fun findMigrationPath(start: Int, end: Int): List<Migration>? {
            if (start == end) {
                return emptyList()
            }
            val migrateUp = end > start
            val result = mutableListOf<Migration>()
            return findUpMigrationPath(result, migrateUp, start, end)
        }

        private fun findUpMigrationPath(
            result: MutableList<Migration>,
            upgrade: Boolean,
            start: Int,
            end: Int
        ): List<Migration>? {
            var migrationStart = start
            while (if (upgrade) migrationStart < end else migrationStart > end) {
                val targetNodes = migrations[migrationStart] ?: return null
                // keys are ordered so we can start searching from one end of them.
                val keySet = if (upgrade) {
                    targetNodes.descendingKeySet()
                } else {
                    targetNodes.keys
                }
                var found = false
                for (targetVersion in keySet) {
                    val shouldAddToPath = if (upgrade) {
                        targetVersion in (migrationStart + 1)..end
                    } else {
                        targetVersion in end until migrationStart
                    }
                    if (shouldAddToPath) {
                        // We are iterating over the key set of targetNodes, so we can assume it
                        // won't return a null value.
                        result.add(targetNodes[targetVersion]!!)
                        migrationStart = targetVersion
                        found = true
                        break
                    }
                }
                if (!found) {
                    return null
                }
            }
            return result
        }

        /**
         * Indicates if the given migration is contained within the [MigrationContainer] based
         * on its start-end versions.
         *
         * @param startVersion Start version of the migration.
         * @param endVersion End version of the migration
         * @return True if it contains a migration with the same start-end version, false otherwise.
         */
        fun contains(startVersion: Int, endVersion: Int): Boolean {
            val migrations = getMigrations()
            if (migrations.containsKey(startVersion)) {
                val startVersionMatches = migrations[startVersion] ?: emptyMap()
                return startVersionMatches.containsKey(endVersion)
            }
            return false
        }
    }

    /**
     * Callback for [RoomDatabase].
     */
    abstract class Callback {
        /**
         * Called when the database is created for the first time. This is called after all the
         * tables are created.
         *
         * @param db The database.
         */
        open fun onCreate(db: SupportSQLiteDatabase) {}

        /**
         * Called when the database has been opened.
         *
         * @param db The database.
         */
        open fun onOpen(db: SupportSQLiteDatabase) {}

        /**
         * Called after the database was destructively migrated
         *
         * @param db The database.
         */
        open fun onDestructiveMigration(db: SupportSQLiteDatabase) {}
    }

    /**
     * Callback for [Builder.createFromAsset], [Builder.createFromFile]
     * and [Builder.createFromInputStream]
     *
     * This callback will be invoked after the pre-package DB is copied but before Room had
     * a chance to open it and therefore before the [RoomDatabase.Callback] methods are
     * invoked. This callback can be useful for updating the pre-package DB schema to satisfy
     * Room's schema validation.
     */
    abstract class PrepackagedDatabaseCallback {
        /**
         * Called when the pre-packaged database has been copied.
         *
         * @param db The database.
         */
        open fun onOpenPrepackagedDatabase(db: SupportSQLiteDatabase) {}
    }

    /**
     * Callback interface for when SQLite queries are executed.
     *
     * Can be set using [RoomDatabase.Builder.setQueryCallback].
     */
    interface QueryCallback {
        /**
         * Called when a SQL query is executed.
         *
         * @param sqlQuery The SQLite query statement.
         * @param bindArgs Arguments of the query if available, empty list otherwise.
         */
        fun onQuery(sqlQuery: String, bindArgs: List<Any?>)
    }

    companion object {
        /**
         * Unfortunately, we cannot read this value so we are only setting it to the SQLite default.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        const val MAX_BIND_PARAMETER_CNT = 999
    }
}
