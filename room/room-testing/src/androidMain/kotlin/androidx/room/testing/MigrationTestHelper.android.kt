/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.testing

import android.app.Instrumentation
import android.content.Context
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenDelegate
import androidx.room.driver.SupportSQLiteConnection
import androidx.room.driver.SupportSQLiteDriver
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.util.findAndInstantiateDatabaseImpl
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.reflect.KClass
import kotlin.reflect.cast
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A class that can help test and verify database creation and migration at different versions with
 * different schemas in Instrumentation tests.
 *
 * The helper relies on exported schemas so [androidx.room.Database.exportSchema] should be enabled.
 * Schema location should be configured via Room's Gradle Plugin (id 'androidx.room'):
 * ```
 * room {
 *   schemaDirectory("$projectDir/schemas")
 * }
 * ```
 *
 * The schema files must also be copied into the test assets in order for the helper to open them
 * during the instrumentation test:
 * ```
 * android {
 *     sourceSets {
 *         androidTest.assets.srcDirs += files("$projectDir/schemas".toString())
 *     }
 * }
 * ```
 *
 * See the various constructors documentation for possible configurations.
 *
 * See also
 * [Room's Test Migrations Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
 */
actual open class MigrationTestHelper : TestWatcher {
    private val delegate: AndroidMigrationTestHelper

    private val managedSupportDatabases = mutableListOf<WeakReference<SupportSQLiteDatabase>>()
    private val managedRoomDatabases = mutableListOf<WeakReference<RoomDatabase>>()

    private var testStarted = false

    /**
     * Creates a new migration helper. It uses the [instrumentation] context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     * When the [MigrationTestHelper] is created with this constructor configuration then only
     * [createDatabase] and [runMigrationsAndValidate] that return [SupportSQLiteDatabase] can be
     * used.
     *
     * @param instrumentation The instrumentation instance.
     * @param assetsFolder The asset folder in the assets directory.
     * @param openFactory factory for creating an [SupportSQLiteOpenHelper]
     */
    @Deprecated(
        """
        Cannot be used to run migration tests involving auto migrations.
        To test an auto migrations, you must use the constructors that receives the database
        class as parameter.
        """
    )
    @JvmOverloads
    constructor(
        instrumentation: Instrumentation,
        assetsFolder: String,
        openFactory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
    ) {
        this.delegate =
            SupportSQLiteMigrationTestHelper(
                instrumentation = instrumentation,
                assetsFolder = assetsFolder,
                databaseClass = null,
                openFactory = openFactory,
                autoMigrationSpecs = emptyList()
            )
    }

    /**
     * Creates a new migration helper. It uses the [instrumentation] context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     * When the [MigrationTestHelper] is created with this constructor configuration then only
     * [createDatabase] and [runMigrationsAndValidate] that return [SupportSQLiteDatabase] can be
     * used.
     *
     * @param instrumentation The instrumentation instance.
     * @param databaseClass The Database class to be tested.
     */
    constructor(
        instrumentation: Instrumentation,
        databaseClass: Class<out RoomDatabase>
    ) : this(
        instrumentation = instrumentation,
        databaseClass = databaseClass,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Creates a new migration helper. It uses the [instrumentation] context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     * Instances of classes annotated with [androidx.room.ProvidedAutoMigrationSpec] have provided
     * using this constructor. [MigrationTestHelper] will map auto migration spec classes to their
     * provided instances before running and validating the migrations.
     *
     * When the [MigrationTestHelper] is created with this constructor configuration then only
     * [createDatabase] and [runMigrationsAndValidate] that return [SupportSQLiteDatabase] can be
     * used.
     *
     * @param instrumentation The instrumentation instance.
     * @param databaseClass The Database class to be tested.
     * @param specs The list of available auto migration specs that will be provided to the
     *   RoomDatabase at runtime.
     * @param openFactory factory for creating an [SupportSQLiteOpenHelper]
     */
    @JvmOverloads
    constructor(
        instrumentation: Instrumentation,
        databaseClass: Class<out RoomDatabase>,
        specs: List<AutoMigrationSpec>,
        openFactory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
    ) {
        val assetsFolder =
            checkNotNull(databaseClass.canonicalName).let {
                if (it.endsWith("/")) {
                    it.substring(0, it.length - 1)
                } else {
                    it
                }
            }
        this.delegate =
            SupportSQLiteMigrationTestHelper(
                instrumentation = instrumentation,
                assetsFolder = assetsFolder,
                databaseClass = databaseClass,
                openFactory = openFactory,
                autoMigrationSpecs = specs
            )
    }

    /**
     * Creates a new migration helper. It uses the [instrumentation] context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     * When the [MigrationTestHelper] is created with this constructor configuration then only
     * [createDatabase] and [runMigrationsAndValidate] that return [SQLiteConnection] can be used.
     *
     * @param instrumentation The instrumentation instance.
     * @param file The database file.
     * @param driver A driver that opens connection to a file database. A driver that opens
     *   connections to an in-memory database would be meaningless.
     * @param databaseClass The [androidx.room.Database] annotated class.
     * @param databaseFactory An optional factory function to create an instance of the
     *   [databaseClass]. Should be the same factory used when building the database via
     *   [androidx.room.Room.databaseBuilder].
     * @param autoMigrationSpecs The list of [androidx.room.ProvidedAutoMigrationSpec] instances for
     *   [androidx.room.AutoMigration]s that require them.
     */
    @Suppress("StreamFiles")
    constructor(
        instrumentation: Instrumentation,
        file: File,
        driver: SQLiteDriver,
        databaseClass: KClass<out RoomDatabase>,
        databaseFactory: () -> RoomDatabase = {
            findAndInstantiateDatabaseImpl(databaseClass.java)
        },
        autoMigrationSpecs: List<AutoMigrationSpec> = emptyList()
    ) {
        val assetsFolder =
            checkNotNull(databaseClass.qualifiedName).let {
                if (it.endsWith("/")) {
                    it.substring(0, it.length - 1)
                } else {
                    it
                }
            }
        this.delegate =
            SQLiteDriverMigrationTestHelper(
                instrumentation = instrumentation,
                assetsFolder = assetsFolder,
                file = file,
                driver = driver,
                databaseClass = databaseClass,
                databaseFactory = databaseFactory,
                autoMigrationSpecs = autoMigrationSpecs
            )
    }

    override fun starting(description: Description?) {
        super.starting(description)
        testStarted = true
    }

    /**
     * Creates the database in the given version.
     *
     * If the database file already exists, it tries to delete it first. If delete fails, throws an
     * exception.
     *
     * @param name The name of the database.
     * @param version The version in which the database should be created.
     * @return A database connection which has the schema in the requested version.
     */
    @Throws(IOException::class)
    open fun createDatabase(name: String, version: Int): SupportSQLiteDatabase {
        check(delegate is SupportSQLiteMigrationTestHelper) {
            "MigrationTestHelper functionality returning a SupportSQLiteDatabase is not possible " +
                "because a SQLiteDriver was provided during configuration."
        }
        return delegate.createDatabase(name, version)
    }

    /**
     * Runs the given set of migrations on the provided database.
     *
     * It uses the same algorithm that Room uses to choose migrations so the migrations instances
     * that are provided to this method must be sufficient to bring the database from current
     * version to the desired version.
     *
     * After the migration, the method validates the database schema to ensure that migration result
     * matches the expected schema. Handling of dropped tables depends on the
     * `validateDroppedTables` argument. If set to true, the verification will fail if it finds a
     * table that is not registered in the Database. If set to false, extra tables in the database
     * will be ignored (this is the runtime library behavior).
     *
     * @param name The database name. You must first create this database via [createDatabase].
     * @param version The final version after applying the migrations.
     * @param validateDroppedTables If set to true, validation will fail if the database has unknown
     *   tables.
     * @param migrations The list of available migrations.
     * @throws IllegalStateException If the schema validation fails.
     */
    open fun runMigrationsAndValidate(
        name: String,
        version: Int,
        validateDroppedTables: Boolean,
        vararg migrations: Migration
    ): SupportSQLiteDatabase {
        check(delegate is SupportSQLiteMigrationTestHelper) {
            "MigrationTestHelper functionality returning a SupportSQLiteDatabase is not possible " +
                "because a SQLiteDriver was provided during configuration."
        }
        return delegate.runMigrationsAndValidate(name, version, validateDroppedTables, migrations)
    }

    /**
     * Creates the database at the given version.
     *
     * Once a database is created it can further validate with [runMigrationsAndValidate].
     *
     * @param version The version of the schema at which the database should be created.
     * @return A database connection of the newly created database.
     * @throws IllegalStateException If a new database was not created.
     */
    actual fun createDatabase(version: Int): SQLiteConnection {
        check(delegate is SQLiteDriverMigrationTestHelper) {
            "MigrationTestHelper functionality returning a SQLiteConnection is not possible " +
                "because a SupportSQLiteOpenHelper was provided during configuration (i.e. no " +
                "SQLiteDriver was provided)."
        }
        return delegate.createDatabase(version)
    }

    /**
     * Runs the given set of migrations on the existing database once created via [createDatabase].
     *
     * This function uses the same algorithm that Room performs to choose migrations such that the
     * [migrations] instances provided must be sufficient to bring the database from current version
     * to the desired version. If the database contains [androidx.room.AutoMigration]s, then those
     * are already included in the list of migrations to execute if necessary. Note that provided
     * manual migrations take precedence over auto migrations if they overlap in migration paths.
     *
     * Once migrations are done, this functions validates the database schema to ensure the
     * migration performed resulted in the expected schema.
     *
     * @param version The final version the database should migrate to.
     * @param migrations The list of migrations used to attempt the database migration.
     * @return A database connection of the migrated database.
     * @throws IllegalStateException If the schema validation fails.
     */
    actual fun runMigrationsAndValidate(
        version: Int,
        migrations: List<Migration>,
    ): SQLiteConnection {
        check(delegate is SQLiteDriverMigrationTestHelper) {
            "MigrationTestHelper functionality returning a SQLiteConnection is not possible " +
                "because a SupportSQLiteOpenHelper was provided during configuration (i.e. no " +
                "SQLiteDriver was provided)."
        }
        return delegate.runMigrationsAndValidate(version, migrations)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        delegate.finished()
        managedSupportDatabases.forEach { it.get()?.close() }
        managedRoomDatabases.forEach { it.get()?.close() }
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if [MigrationTestHelper] is registered as a Junit test rule via the
     * [org.junit.Rule] annotation.
     *
     * @param db The database connection that should be closed after the test finishes.
     */
    open fun closeWhenFinished(db: SupportSQLiteDatabase) {
        check(testStarted) {
            "You cannot register a database to be closed before" +
                " the test starts. Maybe you forgot to annotate MigrationTestHelper as a" +
                " test rule? (@Rule)"
        }
        managedSupportDatabases.add(WeakReference(db))
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if [MigrationTestHelper] is registered as a Junit test rule via the
     * [org.junit.Rule] annotation.
     *
     * @param db The RoomDatabase instance which holds the database.
     */
    open fun closeWhenFinished(db: RoomDatabase) {
        check(testStarted) {
            "You cannot register a database to be closed before" +
                " the test starts. Maybe you forgot to annotate MigrationTestHelper as a" +
                " test rule? (@Rule)"
        }
        managedRoomDatabases.add(WeakReference(db))
    }
}

/** Base implementation of Android's [MigrationTestHelper] */
private sealed class AndroidMigrationTestHelper(
    private val instrumentation: Instrumentation,
    private val assetsFolder: String
) {
    protected val managedConnections = mutableListOf<WeakReference<SQLiteConnection>>()

    fun finished() {
        managedConnections.forEach { it.get()?.close() }
    }

    protected fun loadSchema(version: Int): SchemaBundle {
        return try {
            loadSchema(instrumentation.context, version)
        } catch (testAssetsNotFoundEx: FileNotFoundException) {
            try {
                loadSchema(instrumentation.targetContext, version)
            } catch (appAssetsNotFoundEx: FileNotFoundException) {
                // throw the test assets exception instead
                throw FileNotFoundException(
                    "Cannot find the schema file in the assets folder. " +
                        "Make sure to include the exported json schemas in your test assert " +
                        "inputs. See " +
                        "https://developer.android.com/training/data-storage/room/" +
                        "migrating-db-versions#export-schema for details. Missing file: " +
                        testAssetsNotFoundEx.message
                )
            }
        }
    }

    protected fun loadSchema(context: Context, version: Int): SchemaBundle {
        val input = context.assets.open("$assetsFolder/$version.json")
        return SchemaBundle.deserialize(input)
    }

    protected fun createDatabaseConfiguration(
        container: RoomDatabase.MigrationContainer,
        openFactory: SupportSQLiteOpenHelper.Factory?,
        sqliteDriver: SQLiteDriver?,
        databaseFileName: String?
    ) =
        DatabaseConfiguration(
            context = instrumentation.targetContext,
            name = databaseFileName,
            sqliteOpenHelperFactory = openFactory,
            migrationContainer = container,
            callbacks = null,
            allowMainThreadQueries = true,
            journalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
            queryExecutor = ArchTaskExecutor.getIOThreadExecutor(),
            transactionExecutor = ArchTaskExecutor.getIOThreadExecutor(),
            multiInstanceInvalidationServiceIntent = null,
            requireMigration = true,
            allowDestructiveMigrationOnDowngrade = false,
            migrationNotRequiredFrom = emptySet(),
            copyFromAssetPath = null,
            copyFromFile = null,
            copyFromInputStream = null,
            prepackagedDatabaseCallback = null,
            typeConverters = emptyList(),
            autoMigrationSpecs = emptyList(),
            allowDestructiveMigrationForAllTables = false,
            sqliteDriver = sqliteDriver,
            queryCoroutineContext = null
        )
}

/**
 * Compatibility implementation of the [MigrationTestHelper] for [SupportSQLiteOpenHelper] and
 * [SupportSQLiteDatabase].
 */
private class SupportSQLiteMigrationTestHelper(
    instrumentation: Instrumentation,
    assetsFolder: String,
    databaseClass: Class<out RoomDatabase>?,
    private val openFactory: SupportSQLiteOpenHelper.Factory,
    private val autoMigrationSpecs: List<AutoMigrationSpec>,
) : AndroidMigrationTestHelper(instrumentation, assetsFolder) {

    private val context = instrumentation.targetContext
    private val databaseInstance: RoomDatabase =
        if (databaseClass == null) {
            object : RoomDatabase() {
                override fun createInvalidationTracker(): InvalidationTracker {
                    return InvalidationTracker(this, emptyMap(), emptyMap())
                }

                override fun clearAllTables() {
                    error("Function should never be called during tests.")
                }

                override fun createAutoMigrations(
                    autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
                ): List<Migration> {
                    return emptyList()
                }
            }
        } else {
            findAndInstantiateDatabaseImpl(databaseClass)
        }

    fun createDatabase(name: String, version: Int): SupportSQLiteDatabase {
        val dbPath = context.getDatabasePath(name)
        if (dbPath.exists()) {
            check(dbPath.delete()) { "There is a database file and I could not delete it." }
        }
        val schemaBundle = loadSchema(version)
        val connection =
            createDatabaseCommon(
                schema = schemaBundle.database,
                configurationFactory = ::createConfiguration,
                connectionManagerFactory = { config, openDelegate ->
                    SupportTestConnectionManager(config.copy(name = name), openDelegate)
                }
            )
        managedConnections.add(WeakReference(connection))
        check(connection is SupportSQLiteConnection) {
            "Expected connection to be a SupportSQLiteConnection but was ${connection::class}"
        }
        return connection.db
    }

    fun runMigrationsAndValidate(
        name: String,
        version: Int,
        validateDroppedTables: Boolean,
        migrations: Array<out Migration>
    ): SupportSQLiteDatabase {
        val dbPath = context.getDatabasePath(name)
        check(dbPath.exists()) {
            "Cannot find the database file for $name. " +
                "Before calling runMigrations, you must first create the database via " +
                "createDatabase()."
        }
        val schemaBundle = loadSchema(version)
        val connection =
            runMigrationsAndValidateCommon(
                databaseInstance = databaseInstance,
                schema = schemaBundle.database,
                migrations = migrations.toList(),
                autoMigrationSpecs = autoMigrationSpecs,
                validateUnknownTables = validateDroppedTables,
                configurationFactory = ::createConfiguration,
                connectionManagerFactory = { config, openDelegate ->
                    SupportTestConnectionManager(config.copy(name = name), openDelegate)
                }
            )
        managedConnections.add(WeakReference(connection))
        check(connection is SupportSQLiteConnection) {
            "Expected connection to be a SupportSQLiteConnection but was ${connection::class}"
        }
        return connection.db
    }

    private fun createConfiguration(container: RoomDatabase.MigrationContainer) =
        createDatabaseConfiguration(container, openFactory, null, null)

    private class SupportTestConnectionManager(
        override val configuration: DatabaseConfiguration,
        override val openDelegate: RoomOpenDelegate
    ) : TestConnectionManager() {

        private val driverWrapper: SQLiteDriver

        init {
            val openFactory = checkNotNull(configuration.sqliteOpenHelperFactory)
            val openHelperConfig =
                SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                    .name(configuration.name)
                    .callback(SupportOpenHelperCallback(openDelegate.version))
                    .build()
            val supportDriver = SupportSQLiteDriver(openFactory.create(openHelperConfig))
            this.driverWrapper = DriverWrapper(supportDriver)
        }

        override fun openConnection(): SQLiteConnection {
            val name = configuration.name
            val filename =
                if (configuration.name != null) {
                    configuration.context.getDatabasePath(name).absolutePath
                } else {
                    ":memory:"
                }
            return driverWrapper.open(filename)
        }

        inner class SupportOpenHelperCallback(version: Int) :
            SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                this@SupportTestConnectionManager.onCreate(SupportSQLiteConnection(db))
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                this@SupportTestConnectionManager.onMigrate(
                    SupportSQLiteConnection(db),
                    oldVersion,
                    newVersion
                )
            }

            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                this.onUpgrade(db, oldVersion, newVersion)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                this@SupportTestConnectionManager.onOpen(SupportSQLiteConnection(db))
            }
        }
    }
}

/** Implementation of the [MigrationTestHelper] for [SQLiteDriver] and [SQLiteConnection]. */
private class SQLiteDriverMigrationTestHelper(
    instrumentation: Instrumentation,
    assetsFolder: String,
    private val driver: SQLiteDriver,
    databaseClass: KClass<out RoomDatabase>,
    databaseFactory: () -> RoomDatabase,
    private val file: File,
    private val autoMigrationSpecs: List<AutoMigrationSpec>
) : AndroidMigrationTestHelper(instrumentation, assetsFolder) {

    private val databaseInstance = databaseClass.cast(databaseFactory.invoke())

    fun createDatabase(version: Int): SQLiteConnection {
        val schemaBundle = loadSchema(version)
        val connection =
            createDatabaseCommon(
                schema = schemaBundle.database,
                configurationFactory = ::createConfiguration
            )
        managedConnections.add(WeakReference(connection))
        return connection
    }

    fun runMigrationsAndValidate(
        version: Int,
        migrations: List<Migration>,
    ): SQLiteConnection {
        val schemaBundle = loadSchema(version)
        val connection =
            runMigrationsAndValidateCommon(
                databaseInstance = databaseInstance,
                schema = schemaBundle.database,
                migrations = migrations,
                autoMigrationSpecs = autoMigrationSpecs,
                validateUnknownTables = false,
                configurationFactory = ::createConfiguration
            )
        managedConnections.add(WeakReference(connection))
        return connection
    }

    private fun createConfiguration(container: RoomDatabase.MigrationContainer) =
        createDatabaseConfiguration(container, null, driver, file.path)
}
