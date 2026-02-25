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

package androidx.room3.testing

import android.app.Instrumentation
import android.content.Context
import androidx.room3.DatabaseConfiguration
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.room3.migration.bundle.SchemaBundle
import androidx.room3.util.findAndInstantiateDatabaseImpl
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlinx.coroutines.Dispatchers
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A class that can help test and verify database creation and migration at different versions with
 * different schemas in Instrumentation tests.
 *
 * Common usage of this helper is to create a database at an older version first and then attempt a
 * migration and validation:
 * ```
 * @get:Rule
 * val migrationTestHelper = MigrationTestHelper(
 *    instrumentation = InstrumentationRegistry.getInstrumentation(),
 *    file = instrumentation.targetContext.getDatabasePath("test.db")
 *    driver = sqliteDriver,
 *    databaseClass = PetDatabase::class
 * )
 *
 * @BeforeTest
 * fun before() {
 *   instrumentation.targetContext.deleteDatabase("test.db")
 * }
 *
 * @Test
 * fun migrationTest() {
 *   // Create the database at version 1
 *   val newConnection = migrationTestHelper.createDatabase(1)
 *   // Insert some data that should be preserved
 *   newConnection.execSQL("INSERT INTO Pet (id, name) VALUES (1, 'Tom')")
 *   newConnection.close()
 *
 *   // Migrate the database to version 2
 *   val migratedConnection =
 *       migrationTestHelper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)))
 *   migratedConnection.prepare("SELECT * FROM Pet).use { stmt ->
 *     // Validates data is preserved between migrations.
 *     assertThat(stmt.step()).isTrue()
 *     assertThat(stmt.getText(1)).isEqualTo("Tom")
 *   }
 *   migratedConnection.close()
 * }
 * ```
 *
 * The helper relies on exported schemas so [androidx.room3.Database.exportSchema] should be
 * enabled. Schema location should be configured via Room's Gradle Plugin (id 'androidx.room'):
 * ```
 * room {
 *   schemaDirectory("$projectDir/schemas")
 * }
 * ```
 *
 * The schema files must also be copied into the test assets in order for the helper to open them
 * during the instrumentation test, this is also configured by Room's Gradle Plugin but can also be
 * configured manually:
 * ```
 * android {
 *     sourceSets {
 *         androidTest.assets.srcDirs += files("$projectDir/schemas".toString())
 *     }
 * }
 * ```
 *
 * See also
 * [Room's Test Migrations Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
 */
@Suppress("KmpModifierMismatch") // the expect is not open
public actual open class MigrationTestHelper : TestWatcher {
    private val delegate: AndroidMigrationTestHelper

    private val managedRoomDatabases = mutableListOf<WeakReference<RoomDatabase>>()

    private var testStarted = false

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
     * @param databaseClass The [androidx.room3.Database] annotated class.
     * @param databaseFactory An optional factory function to create an instance of the
     *   [databaseClass]. Should be the same factory used when building the database via
     *   [androidx.room3.Room.databaseBuilder].
     * @param autoMigrationSpecs The list of [androidx.room3.ProvidedAutoMigrationSpec] instances
     *   for [androidx.room3.AutoMigration]s that require them.
     */
    @Suppress("StreamFiles")
    public constructor(
        instrumentation: Instrumentation,
        file: File,
        driver: SQLiteDriver,
        databaseClass: KClass<out RoomDatabase>,
        databaseFactory: () -> RoomDatabase = {
            findAndInstantiateDatabaseImpl(databaseClass.java)
        },
        autoMigrationSpecs: List<AutoMigrationSpec> = emptyList(),
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
                autoMigrationSpecs = autoMigrationSpecs,
            )
    }

    override fun starting(description: Description?) {
        super.starting(description)
        testStarted = true
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
    public actual suspend fun createDatabase(version: Int): SQLiteConnection {
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
     * to the desired version. If the database contains [androidx.room3.AutoMigration]s, then those
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
    public actual suspend fun runMigrationsAndValidate(
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
        managedRoomDatabases.forEach { it.get()?.close() }
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if [MigrationTestHelper] is registered as a Junit test rule via the
     * [org.junit.Rule] annotation.
     *
     * @param db The RoomDatabase instance which holds the database.
     */
    public open fun closeWhenFinished(db: RoomDatabase) {
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
    private val assetsFolder: String,
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
        sqliteDriver: SQLiteDriver,
        databaseFileName: String?,
    ) =
        DatabaseConfiguration(
            context = instrumentation.targetContext,
            name = databaseFileName,
            migrationContainer = container,
            callbacks = null,
            allowMainThreadQueries = true,
            journalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
            multiInstanceInvalidationServiceIntent = null,
            requireMigration = true,
            allowDestructiveMigrationOnDowngrade = false,
            migrationNotRequiredFrom = emptySet(),
            prepackagedDatabaseCallback = null,
            typeConverters = emptyList(),
            autoMigrationSpecs = emptyList(),
            allowDestructiveMigrationForAllTables = false,
            sqliteDriver = sqliteDriver,
            queryCoroutineContext = Dispatchers.IO,
        )
}

/** Implementation of the [MigrationTestHelper] for [SQLiteDriver] and [SQLiteConnection]. */
private class SQLiteDriverMigrationTestHelper(
    instrumentation: Instrumentation,
    assetsFolder: String,
    private val driver: SQLiteDriver,
    databaseClass: KClass<out RoomDatabase>,
    databaseFactory: () -> RoomDatabase,
    private val file: File,
    private val autoMigrationSpecs: List<AutoMigrationSpec>,
) : AndroidMigrationTestHelper(instrumentation, assetsFolder) {

    private val databaseInstance = databaseClass.cast(databaseFactory.invoke())

    suspend fun createDatabase(version: Int): SQLiteConnection {
        val schemaBundle = loadSchema(version)
        val connection =
            createDatabaseCommon(
                schema = schemaBundle.database,
                configurationFactory = ::createConfiguration,
            )
        managedConnections.add(WeakReference(connection))
        return connection
    }

    suspend fun runMigrationsAndValidate(
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
                configurationFactory = ::createConfiguration,
            )
        managedConnections.add(WeakReference(connection))
        return connection
    }

    private fun createConfiguration(container: RoomDatabase.MigrationContainer) =
        createDatabaseConfiguration(container, driver, file.path)
}
