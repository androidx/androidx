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

package androidx.room3

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.room3.Room.LOG_TAG
import androidx.room3.autoclose.AutoCloser
import androidx.room3.autoclose.AutoCloserConfig
import androidx.room3.autoclose.AutoClosingSQLiteDriver
import androidx.room3.concurrent.CloseBarrier
import androidx.room3.coroutines.TransactionElement
import androidx.room3.coroutines.runBlockingUninterruptible
import androidx.room3.coroutines.withTransactionContext
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.room3.prepackage.CopyFromAssetPath
import androidx.room3.prepackage.CopyFromFile
import androidx.room3.prepackage.CopyFromInputStream
import androidx.room3.prepackage.PrePackagedCopySQLiteDriver
import androidx.room3.util.contains as containsCommon
import androidx.room3.util.findAndInstantiateDatabaseImpl
import androidx.room3.util.findMigrationPath as findMigrationPathExt
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import java.io.File
import java.io.InputStream
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel

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
public actual abstract class RoomDatabase
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
actual constructor() {

    private lateinit var configuration: DatabaseConfiguration
    private lateinit var coroutineScope: CoroutineScope

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val path: String?
        get() = configuration.name?.let { configuration.context.getDatabasePath(it).path }

    /**
     * The executor for thread-confined transactions, such as those from the [AndroidSQLiteDriver]
     * or any driver that report to have an internal pool via [SQLiteDriver.hasConnectionPool].
     *
     * @see androidx.room3.coroutines.startTransactionCoroutine
     */
    internal val transactionLimitedExecutor: Executor
        get() = internalTransactionLimitedExecutor

    private lateinit var internalTransactionLimitedExecutor: Executor

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

    private val typeConverters: MutableMap<KClass<*>, Any> = mutableMapOf()

    internal var useTempTrackingTable: Boolean = true

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
    @Suppress("KmpVisibilityMismatch") // expect is internal
    public actual open fun init(configuration: DatabaseConfiguration) {
        this.configuration = configuration
        useTempTrackingTable = configuration.useTempTrackingTable

        this.autoCloser = configuration.autoCloseConfig?.let { AutoCloser(it) }
        val openDelegate = createOpenDelegate() as RoomOpenDelegate
        val configuration = wrapDriverConfiguration(configuration, openDelegate.version)
        connectionManager = createConnectionManager(configuration, openDelegate)
        internalTracker = createInvalidationTracker()
        autoCloser?.let {
            connectionManager.setAutoCloser(it)
            invalidationTracker.setAutoCloser(it)
        }
        validateAutoMigrations(configuration)
        validateTypeConverters(configuration)

        // For Room's coroutine scope, we use the provided context but add a SupervisorJob that
        // is tied to the given Job (if any).
        val parentJob = configuration.queryCoroutineContext[Job]
        coroutineScope =
            CoroutineScope(configuration.queryCoroutineContext + SupervisorJob(parentJob))
        val dispatcher =
            configuration.queryCoroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        internalTransactionLimitedExecutor = dispatcher.asExecutor()

        allowMainThreadQueries = configuration.allowMainThreadQueries
        autoCloser?.initCoroutineScope(coroutineScope)

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

    /** Wraps the configured [SQLiteDriver] based on various builder set functionalities. */
    private fun wrapDriverConfiguration(
        configuration: DatabaseConfiguration,
        databaseVersion: Int,
    ): DatabaseConfiguration {
        // The order of wrapping is significant, the last wrap being the outer-most and first to be
        // invoked by the connection manager, while the first one being the inner-most, being the
        // last to be invoked.
        var newConfiguration = configuration
        if (configuration.autoCloseConfig != null) {
            newConfiguration =
                configuration.copy(
                    sqliteDriver =
                        AutoClosingSQLiteDriver(
                            autoCloser = checkNotNull(autoCloser),
                            delegateDriver = configuration.sqliteDriver,
                        )
                )
        }
        if (configuration.copyFromConfig != null) {
            newConfiguration =
                configuration.copy(
                    sqliteDriver =
                        PrePackagedCopySQLiteDriver(
                            configuration.sqliteDriver,
                            configuration,
                            databaseVersion,
                        )
                )
        }
        return newConfiguration
    }

    /**
     * Creates a connection manager to manage database connection. Note that this method is called
     * when the [RoomDatabase] is initialized.
     *
     * @return A new connection manager.
     */
    internal actual fun createConnectionManager(
        configuration: DatabaseConfiguration,
        openDelegate: RoomOpenDelegate,
    ): RoomConnectionManager {
        return RoomConnectionManager(
            config = configuration,
            openDelegate = openDelegate,
            transactionWrapper = ::withTransactionContext,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public actual abstract fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration>

    /**
     * Creates a delegate to configure and initialize the database when it is being opened. An
     * implementation of this function is generated by the Room processor. Note that this method is
     * called when the [RoomDatabase] is initialized.
     *
     * @return A new delegate to be used while opening the database
     * @throws NotImplementedError by default
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected actual abstract fun createOpenDelegate(): RoomOpenDelegateMarker

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
        return coroutineScope.coroutineContext.minusKey(Job)
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
    protected actual abstract fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>>

    /** Property delegate of [getRequiredTypeConverterClasses] for common ext functionality. */
    internal actual val requiredTypeConverterClassesMap: Map<KClass<*>, List<KClass<*>>>
        get() = getRequiredTypeConverterClasses()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public actual abstract fun getRequiredAutoMigrationSpecClasses():
        Set<KClass<out AutoMigrationSpec>>

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
        runBlockingUninterruptible {
            connectionManager.useConnection(isReadOnly = false) { connection ->
                if (!connection.inTransaction()) {
                    invalidationTracker.sync()
                }
                connection.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                    if (hasForeignKeys) {
                        executeSQL("PRAGMA defer_foreign_keys = TRUE")
                    }
                    tableNames.forEach { tableName -> executeSQL("DELETE FROM `$tableName`") }
                }
                if (!connection.inTransaction()) {
                    connection.executeSQL("PRAGMA wal_checkpoint(FULL)")
                    connection.executeSQL("VACUUM")
                    invalidationTracker.refreshAsync()
                }
            }
        }
    }

    /** True if the actual database connection is open, regardless of auto-close. */
    internal val isOpenInternal: Boolean
        get() = autoCloser?.isOpen() ?: true

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
     * Initialize invalidation tracker. Note that this method is called when the [RoomDatabase] is
     * initialized and opens a database connection.
     *
     * @param connection The database connection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    protected actual suspend fun internalInitInvalidationTracker(connection: SQLiteConnection) {
        invalidationTracker.internalInit(connection)
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
    // GetterOnBuilder: To keep ABI compatibility from Java
    // KmpModifierMismatch: expect is not open
    @Suppress("GetterOnBuilder", "KmpModifierMismatch")
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
        private val typeConverters: MutableList<Any> = mutableListOf()

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
         *   database. The callable will be invoked in a thread from the dispatcher set via
         *   [setQueryCoroutineContext]. The callable is only invoked if Room needs to create and
         *   open the database from the pre-package database, usually the first time it is created
         *   or during a destructive migration.
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
         *   database. The callable will be invoked in a thread from the dispatcher set via
         *   [setQueryCoroutineContext]. The callable is only invoked if Room needs to create and
         *   open the database from the pre-package database, usually the first time it is created
         *   or during a destructive migration.
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
         * The database will not be re-opened if the RoomDatabase is closed manually (by calling
         * [RoomDatabase.close]). If the database is closed manually, you must create a new database
         * using [RoomDatabase.Builder.build].
         *
         * @param autoCloseTimeout the amount of time after the last usage before closing the
         *   database. Must greater or equal to zero.
         * @param autoCloseTimeUnit the timeunit for autoCloseTimeout.
         * @return This builder instance.
         */
        @ExperimentalRoomApi
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
         * If no context is provided, then Room will default to `Dispatchers.IO`.
         *
         * @param context The context
         * @return This [Builder] instance
         * @throws IllegalArgumentException if no [CoroutineDispatcher] is found in the given
         *   [context] or if this builder was already configured with an [Executor].
         */
        @Suppress("MissingGetterMatchingBuilder")
        public actual fun setQueryCoroutineContext(context: CoroutineContext): Builder<T> = apply {
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
            validateMigrationsNotRequired(migrationStartAndEndVersions, migrationsNotRequiredFrom)

            if (driver == null) {
                // No driver, use default one for Android
                driver = AndroidSQLiteDriver()
            }
            val autoCloseConfig =
                if (autoCloseTimeout > 0) {
                    AutoCloserConfig(autoCloseTimeout, requireNotNull(autoCloseTimeUnit))
                } else {
                    null
                }
            val copyFromConfig =
                if (
                    copyFromAssetPath != null || copyFromFile != null || copyFromInputStream != null
                ) {
                    requireNotNull(name) {
                        "Cannot create from asset or file for an in-memory database."
                    }
                    val copyFromAssetPathConfig = if (copyFromAssetPath == null) 0 else 1
                    val copyFromFileConfig = if (copyFromFile == null) 0 else 1
                    val copyFromInputStreamConfig = if (copyFromInputStream == null) 0 else 1
                    val copyConfigurations =
                        copyFromAssetPathConfig + copyFromFileConfig + copyFromInputStreamConfig
                    require(copyConfigurations == 1) {
                        "More than one of createFromAsset(), createFromInputStream() and " +
                            "createFromFile() were called on this Builder, but the database can " +
                            "only be created using one of the " +
                            "three configurations."
                    }
                    copyFromAssetPath?.let { CopyFromAssetPath(context, it) }
                        ?: copyFromFile?.let { CopyFromFile(it) }
                        ?: copyFromInputStream?.let { CopyFromInputStream(it) }
                } else {
                    null
                }
            val configuration =
                DatabaseConfiguration(
                        context = context,
                        name = name,
                        migrationContainer = migrationContainer,
                        callbacks = callbacks,
                        allowMainThreadQueries = allowMainThreadQueries,
                        journalMode = journalMode.resolve(context),
                        multiInstanceInvalidationServiceIntent = multiInstanceInvalidationIntent,
                        requireMigration = requireMigration,
                        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
                        migrationNotRequiredFrom = migrationsNotRequiredFrom,
                        prepackagedDatabaseCallback = prepackagedDatabaseCallback,
                        typeConverters = typeConverters,
                        autoMigrationSpecs = autoMigrationSpecs,
                        allowDestructiveMigrationForAllTables =
                            allowDestructiveMigrationForAllTables,
                        sqliteDriver = requireNotNull(driver),
                        queryCoroutineContext = queryCoroutineContext ?: Dispatchers.IO,
                    )
                    .apply {
                        this.useTempTrackingTable = inMemoryTrackingTableMode
                        this.copyFromConfig = copyFromConfig
                        this.autoCloseConfig = autoCloseConfig
                    }
            val db = factory?.invoke() ?: findAndInstantiateDatabaseImpl(klass.java)
            db.init(configuration)
            return db
        }
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    @Suppress("KmpModifierMismatch") // expect is not open
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
         * Called when the database is created for the first time.
         *
         * This function called after all the tables are created.
         *
         * @param connection The database connection.
         */
        public actual open suspend fun onCreate(connection: SQLiteConnection) {}

        /**
         * Called after the database was destructively migrated.
         *
         * @param connection The database connection.
         */
        public actual open suspend fun onDestructiveMigration(connection: SQLiteConnection) {}

        /**
         * Called when the database has been opened.
         *
         * @param connection The database connection.
         */
        public actual open suspend fun onOpen(connection: SQLiteConnection) {}
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
    // TODO(b/339934813): Move pre-package out of Room and into a set of utility drivers.
    public abstract class PrepackagedDatabaseCallback {

        /**
         * Called when the pre-packaged database has been copied.
         *
         * @param connection The database connection.
         */
        public abstract fun onOpenPrepackagedDatabase(connection: SQLiteConnection)
    }

    public companion object {
        /**
         * Unfortunately, we cannot read this value so we are only setting it to the SQLite default.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
        public const val MAX_BIND_PARAMETER_CNT: Int = 999
    }
}
