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
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.room.migration.AutoMigrationSpec
import androidx.room.util.isMigrationRequired as isMigrationRequiredExt
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

/** Configuration class for a [RoomDatabase]. */
@Suppress("UNUSED_PARAMETER")
actual open class DatabaseConfiguration
@SuppressLint("LambdaLast")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor(
    /* The context to use while connecting to the database. */
    @JvmField val context: Context,

    /* The name of the database file or null if it is an in-memory database. */
    @JvmField actual val name: String?,

    /* The factory to use to access the database. */
    @JvmField val sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory?,

    /* Collection of available migrations. */
    @JvmField actual val migrationContainer: RoomDatabase.MigrationContainer,

    /* Database callbacks. */
    @JvmField actual val callbacks: List<RoomDatabase.Callback>?,

    /* Whether Room should throw an exception for queries run on the main thread. */
    @JvmField val allowMainThreadQueries: Boolean,

    /* The journal mode for this database. */
    @JvmField actual val journalMode: RoomDatabase.JournalMode,

    /* The Executor used to execute asynchronous queries. */
    @JvmField val queryExecutor: Executor,

    /* The Executor used to execute asynchronous transactions. */
    @JvmField val transactionExecutor: Executor,

    /**
     * Intent that should be bound to acquire the invalidation service or `null` if not used.
     *
     * @see [multiInstanceInvalidation]
     */
    @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @JvmField
    val multiInstanceInvalidationServiceIntent: Intent?,

    /* Whether Room should throw an exception for missing migrations. */
    @JvmField actual val requireMigration: Boolean,

    /* Whether Room will fallback to destructive migrations on downgrades only .*/
    @JvmField actual val allowDestructiveMigrationOnDowngrade: Boolean,
    internal actual val migrationNotRequiredFrom: Set<Int>?,

    /* Asset path of pre-package database or null if not used. */
    @JvmField val copyFromAssetPath: String?,

    /* File of pre-package database or null if not used. */
    @JvmField val copyFromFile: File?,

    /* Input stream of pre-package database or null if not used. */
    @JvmField val copyFromInputStream: Callable<InputStream>?,

    /* Callback when Room uses a pre-packaged database. */
    @JvmField val prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?,

    /* List of provided type converters. */
    @JvmField actual val typeConverters: List<Any>,

    /* List of provided auto migration specs. */
    @JvmField actual val autoMigrationSpecs: List<AutoMigrationSpec>,

    /* Whether Room will delete all tables or only known tables during destructive migrations. */
    @JvmField actual val allowDestructiveMigrationForAllTables: Boolean,

    /* The SQLite Driver for the database. */
    @JvmField actual val sqliteDriver: SQLiteDriver?,

    /* The Coroutine context for the database. */
    @JvmField actual val queryCoroutineContext: CoroutineContext?,
) {
    /**
     * If true, table invalidation in an instance of [RoomDatabase] is broadcast and synchronized
     * with other instances of the same [RoomDatabase] file, including those in a separate process.
     */
    @JvmField
    val multiInstanceInvalidation: Boolean = multiInstanceInvalidationServiceIntent != null

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param requireMigration True if Room should require a valid migration if version changes,
     *   instead of recreating the tables.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        requireMigration: Boolean,
        migrationNotRequiredFrom: Set<Int>?
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = queryExecutor,
        multiInstanceInvalidationServiceIntent = null,
        allowDestructiveMigrationOnDowngrade = false,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = null,
        copyFromFile = null,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = null,
        typeConverters = emptyList(),
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Room should perform multi-instance invalidation.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     */
    @OptIn(ExperimentalRoomApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidation: Boolean,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent =
            if (multiInstanceInvalidation)
                Intent(context, MultiInstanceInvalidationService::class.java)
            else null,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = null,
        copyFromFile = null,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = null,
        typeConverters = emptyList(),
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Room should perform multi-instance invalidation.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     */
    @OptIn(ExperimentalRoomApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidation: Boolean,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent =
            if (multiInstanceInvalidation)
                Intent(context, MultiInstanceInvalidationService::class.java)
            else null,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = null,
        typeConverters = emptyList(),
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Room should perform multi-instance invalidation.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a pre-package
     *   database file will be copied from.
     */
    @OptIn(ExperimentalRoomApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidation: Boolean,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?,
        copyFromInputStream: Callable<InputStream>?
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent =
            if (multiInstanceInvalidation)
                Intent(context, MultiInstanceInvalidationService::class.java)
            else null,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = copyFromInputStream,
        typeConverters = emptyList(),
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Room should perform multi-instance invalidation.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a pre-package
     *   database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     */
    @OptIn(ExperimentalRoomApi::class)
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidation: Boolean,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?,
        copyFromInputStream: Callable<InputStream>?,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent =
            if (multiInstanceInvalidation)
                Intent(context, MultiInstanceInvalidationService::class.java)
            else null,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = prepackagedDatabaseCallback,
        copyFromInputStream = copyFromInputStream,
        typeConverters = emptyList(),
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Room should perform multi-instance invalidation.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a pre-package
     *   database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     * @param typeConverters The type converters.
     */
    @OptIn(ExperimentalRoomApi::class)
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidation: Boolean,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?,
        copyFromInputStream: Callable<InputStream>?,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?,
        typeConverters: List<Any>
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent =
            if (multiInstanceInvalidation)
                Intent(context, MultiInstanceInvalidationService::class.java)
            else null,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = prepackagedDatabaseCallback,
        copyFromInputStream = copyFromInputStream,
        typeConverters = typeConverters,
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Room should perform multi-instance invalidation.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a pre-package
     *   database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     * @param typeConverters The type converters.
     * @param autoMigrationSpecs The auto migration specs.
     */
    @OptIn(ExperimentalRoomApi::class)
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidation: Boolean,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?,
        copyFromInputStream: Callable<InputStream>?,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?,
        typeConverters: List<Any>,
        autoMigrationSpecs: List<AutoMigrationSpec>
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent =
            if (multiInstanceInvalidation)
                Intent(context, MultiInstanceInvalidationService::class.java)
            else null,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = copyFromInputStream,
        typeConverters = typeConverters,
        autoMigrationSpecs = autoMigrationSpecs,
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidationServiceIntent Intent that should be bound to acquire the
     *   invalidation service or `null` if not used.
     * @param requireMigration True if Room should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Room should recreate tables if no
     *   migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *   aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a pre-package
     *   database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     * @param typeConverters The type converters.
     * @param autoMigrationSpecs The auto migration specs.
     */
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidationServiceIntent: Intent?,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?,
        copyFromInputStream: Callable<InputStream>?,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?,
        typeConverters: List<Any>,
        autoMigrationSpecs: List<AutoMigrationSpec>
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent = multiInstanceInvalidationServiceIntent,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = copyFromInputStream,
        typeConverters = typeConverters,
        autoMigrationSpecs = autoMigrationSpecs,
        allowDestructiveMigrationForAllTables = false,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("This constructor is deprecated.")
    constructor(
        context: Context,
        name: String?,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory,
        migrationContainer: RoomDatabase.MigrationContainer,
        callbacks: List<RoomDatabase.Callback>?,
        allowMainThreadQueries: Boolean,
        journalMode: RoomDatabase.JournalMode,
        queryExecutor: Executor,
        transactionExecutor: Executor,
        multiInstanceInvalidationServiceIntent: Intent?,
        requireMigration: Boolean,
        allowDestructiveMigrationOnDowngrade: Boolean,
        migrationNotRequiredFrom: Set<Int>?,
        copyFromAssetPath: String?,
        copyFromFile: File?,
        copyFromInputStream: Callable<InputStream>?,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?,
        typeConverters: List<Any>,
        autoMigrationSpecs: List<AutoMigrationSpec>,
        allowDestructiveMigrationForAllTables: Boolean,
    ) : this(
        context = context,
        name = name,
        sqliteOpenHelperFactory = sqliteOpenHelperFactory,
        migrationContainer = migrationContainer,
        callbacks = callbacks,
        allowMainThreadQueries = allowMainThreadQueries,
        journalMode = journalMode,
        queryExecutor = queryExecutor,
        transactionExecutor = transactionExecutor,
        multiInstanceInvalidationServiceIntent = multiInstanceInvalidationServiceIntent,
        allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade,
        requireMigration = requireMigration,
        migrationNotRequiredFrom = migrationNotRequiredFrom,
        copyFromAssetPath = copyFromAssetPath,
        copyFromFile = copyFromFile,
        prepackagedDatabaseCallback = null,
        copyFromInputStream = copyFromInputStream,
        typeConverters = typeConverters,
        autoMigrationSpecs = autoMigrationSpecs,
        allowDestructiveMigrationForAllTables = allowDestructiveMigrationForAllTables,
        sqliteDriver = null,
        queryCoroutineContext = null
    )

    /**
     * Returns whether a migration is required from the specified version.
     *
     * @param version The schema version.
     * @return True if a valid migration is required, false otherwise.
     */
    @Deprecated(
        """Use [isMigrationRequired(int, int)] which takes
      [allowDestructiveMigrationOnDowngrade] into account.""",
        ReplaceWith("isMigrationRequired(version, version + 1)")
    )
    open fun isMigrationRequiredFrom(version: Int): Boolean {
        return isMigrationRequired(version, version + 1)
    }

    /**
     * Returns whether a migration is required between two versions.
     *
     * @param fromVersion The old schema version.
     * @param toVersion The new schema version.
     * @return True if a valid migration is required, false otherwise.
     */
    open fun isMigrationRequired(fromVersion: Int, toVersion: Int): Boolean {
        return isMigrationRequiredExt(fromVersion, toVersion)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun copy(
        context: Context = this.context,
        name: String? = this.name,
        sqliteOpenHelperFactory: SupportSQLiteOpenHelper.Factory? = this.sqliteOpenHelperFactory,
        migrationContainer: RoomDatabase.MigrationContainer = this.migrationContainer,
        callbacks: List<RoomDatabase.Callback>? = this.callbacks,
        allowMainThreadQueries: Boolean = this.allowMainThreadQueries,
        journalMode: RoomDatabase.JournalMode = this.journalMode,
        queryExecutor: Executor = this.queryExecutor,
        transactionExecutor: Executor = this.transactionExecutor,
        multiInstanceInvalidationServiceIntent: Intent? =
            this.multiInstanceInvalidationServiceIntent,
        requireMigration: Boolean = this.requireMigration,
        allowDestructiveMigrationOnDowngrade: Boolean = this.allowDestructiveMigrationOnDowngrade,
        migrationNotRequiredFrom: Set<Int>? = this.migrationNotRequiredFrom,
        copyFromAssetPath: String? = this.copyFromAssetPath,
        copyFromFile: File? = this.copyFromFile,
        copyFromInputStream: Callable<InputStream>? = this.copyFromInputStream,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback? =
            this.prepackagedDatabaseCallback,
        typeConverters: List<Any> = this.typeConverters,
        autoMigrationSpecs: List<AutoMigrationSpec> = this.autoMigrationSpecs,
        allowDestructiveMigrationForAllTables: Boolean = this.allowDestructiveMigrationForAllTables,
        sqliteDriver: SQLiteDriver? = this.sqliteDriver,
        queryCoroutineContext: CoroutineContext? = this.queryCoroutineContext
    ) =
        DatabaseConfiguration(
            context,
            name,
            sqliteOpenHelperFactory,
            migrationContainer,
            callbacks,
            allowMainThreadQueries,
            journalMode,
            queryExecutor,
            transactionExecutor,
            multiInstanceInvalidationServiceIntent,
            requireMigration,
            allowDestructiveMigrationOnDowngrade,
            migrationNotRequiredFrom,
            copyFromAssetPath,
            copyFromFile,
            copyFromInputStream,
            prepackagedDatabaseCallback,
            typeConverters,
            autoMigrationSpecs,
            allowDestructiveMigrationForAllTables,
            sqliteDriver,
            queryCoroutineContext
        )
}
