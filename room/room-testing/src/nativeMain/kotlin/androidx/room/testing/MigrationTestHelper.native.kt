/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.room.DatabaseConfiguration
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.util.findDatabaseConstructorAndInitDatabaseImpl
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import kotlin.reflect.KClass
import kotlin.reflect.cast
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * A class that can help test and verify database creation and migration at different versions with
 * different schemas.
 *
 * Common usage of this helper is to create a database at an older version first and then attempt a
 * migration and validation:
 * ```
 * private val migrationTestHelper = MigrationTestHelper(
 *    schemaDirectoryPath = Path("schemas")
 *    driver = sqliteDriver,
 *    databaseClass = PetDatabase::class,
 *    databaseFactory = { PetDatabase::class.instantiateImpl() }
 * )
 *
 * @AfterTest
 * fun after() {
 *   migrationTestHelper.finished()
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
 * The helper relies on exported schemas so [androidx.room.Database.exportSchema] should be enabled.
 * Schema location should be configured via Room's Gradle Plugin (id 'androidx.room'):
 * ```
 * room {
 *   schemaDirectory("$projectDir/schemas")
 * }
 * ```
 *
 * The [schemaDirectoryPath] must match the exported schema location for this helper to properly
 * create and validate schemas.
 *
 * @param schemaDirectoryPath The schema directory where schema files are exported.
 * @param fileName Name of the database.
 * @param driver A driver that opens connection to a file database. A driver that opens connections
 *   to an in-memory database would be meaningless.
 * @param databaseClass The [androidx.room.Database] annotated class.
 * @param databaseFactory The factory function to create an instance of the [databaseClass]. Should
 *   be the same factory used when building the database via [androidx.room.Room.databaseBuilder].
 * @param autoMigrationSpecs The list of [androidx.room.ProvidedAutoMigrationSpec] instances for
 *   [androidx.room.AutoMigration]s that require them.
 */
actual class MigrationTestHelper(
    private val schemaDirectoryPath: String,
    private val fileName: String,
    private val driver: SQLiteDriver,
    private val databaseClass: KClass<out RoomDatabase>,
    databaseFactory: () -> RoomDatabase = {
        findDatabaseConstructorAndInitDatabaseImpl(databaseClass)
    },
    private val autoMigrationSpecs: List<AutoMigrationSpec> = emptyList()
) {
    private val databaseInstance = databaseClass.cast(databaseFactory.invoke())
    private val managedConnections = mutableListOf<SQLiteConnection>()

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
        val schemaBundle = loadSchema(version)
        val connection =
            createDatabaseCommon(
                schema = schemaBundle.database,
                configurationFactory = ::createDatabaseConfiguration
            )
        managedConnections.add(connection)
        return connection
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
     * @throws IllegalStateException If the schema validation fails.
     */
    actual fun runMigrationsAndValidate(
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
                configurationFactory = ::createDatabaseConfiguration
            )
        managedConnections.add(connection)
        return connection
    }

    fun finished() {
        managedConnections.forEach(SQLiteConnection::close)
    }

    private fun loadSchema(version: Int): SchemaBundle {
        val databaseFQN = checkNotNull(databaseClass.qualifiedName)
        val schemaPath = schemaDirectoryPath.toPath().resolve(databaseFQN).resolve("$version.json")
        return FileSystem.SYSTEM.read(schemaPath) { SchemaBundle.deserialize(this) }
    }

    private fun createDatabaseConfiguration(
        container: RoomDatabase.MigrationContainer,
    ) =
        DatabaseConfiguration(
            name = fileName,
            migrationContainer = container,
            callbacks = null,
            journalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
            requireMigration = true,
            allowDestructiveMigrationOnDowngrade = false,
            migrationNotRequiredFrom = null,
            typeConverters = emptyList(),
            autoMigrationSpecs = emptyList(),
            allowDestructiveMigrationForAllTables = false,
            sqliteDriver = driver,
            queryCoroutineContext = null
        )
}
