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

import androidx.room.BaseRoomConnectionManager
import androidx.room.DatabaseConfiguration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomOpenDelegate
import androidx.room.Transactor
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.ViewInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * A class that can help test and verify database creation and migration at different versions with
 * different schemas.
 *
 * Common usage of this helper is to create a database at an older version first and then attempt a
 * migration and validation:
 * ```
 * @Test
 * fun migrationTest() {
 *   val migrationTestHelper = getMigrationTestHelper()
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
 * The helper is then instantiated to use the same schema location where they are exported to. See
 * platform-specific documentation for further configuration.
 */
expect class MigrationTestHelper {
    /**
     * Creates the database at the given version.
     *
     * Once a database is created it can further validate with [runMigrationsAndValidate].
     *
     * @param version The version of the schema at which the database should be created.
     * @return A database connection of the newly created database.
     * @throws IllegalStateException If a new database was not created.
     */
    fun createDatabase(version: Int): SQLiteConnection

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
    fun runMigrationsAndValidate(
        version: Int,
        migrations: List<Migration> = emptyList()
    ): SQLiteConnection
}

internal typealias ConnectionManagerFactory =
    (DatabaseConfiguration, RoomOpenDelegate) -> TestConnectionManager

internal typealias ConfigurationFactory = (RoomDatabase.MigrationContainer) -> DatabaseConfiguration

/** Common logic for [MigrationTestHelper.createDatabase] */
internal fun createDatabaseCommon(
    schema: DatabaseBundle,
    configurationFactory: ConfigurationFactory,
    connectionManagerFactory: ConnectionManagerFactory = { config, openDelegate ->
        DefaultTestConnectionManager(config, openDelegate)
    }
): SQLiteConnection {
    val emptyContainer = RoomDatabase.MigrationContainer()
    val configuration = configurationFactory.invoke(emptyContainer)
    val testConnectionManager =
        connectionManagerFactory.invoke(configuration, CreateOpenDelegate(schema))
    return testConnectionManager.openConnection()
}

/** Common logic for [MigrationTestHelper.runMigrationsAndValidate] */
internal fun runMigrationsAndValidateCommon(
    databaseInstance: RoomDatabase,
    schema: DatabaseBundle,
    migrations: List<Migration>,
    autoMigrationSpecs: List<AutoMigrationSpec>,
    validateUnknownTables: Boolean,
    configurationFactory: ConfigurationFactory,
    connectionManagerFactory: ConnectionManagerFactory = { config, openDelegate ->
        DefaultTestConnectionManager(config, openDelegate)
    }
): SQLiteConnection {
    val container = RoomDatabase.MigrationContainer()
    container.addMigrations(migrations)
    val autoMigrations = getAutoMigrations(databaseInstance, autoMigrationSpecs)
    autoMigrations.forEach { autoMigration ->
        val migrationExists =
            container.contains(autoMigration.startVersion, autoMigration.endVersion)
        if (!migrationExists) {
            container.addMigration(autoMigration)
        }
    }
    val configuration = configurationFactory.invoke(container)
    val testConnectionManager =
        connectionManagerFactory.invoke(
            configuration,
            MigrateOpenDelegate(schema, validateUnknownTables)
        )
    return testConnectionManager.openConnection()
}

private fun getAutoMigrations(
    databaseInstance: RoomDatabase,
    providedSpecs: List<AutoMigrationSpec>
): List<Migration> {
    val autoMigrationSpecMap =
        createAutoMigrationSpecMap(
            databaseInstance.getRequiredAutoMigrationSpecClasses(),
            providedSpecs
        )
    return databaseInstance.createAutoMigrations(autoMigrationSpecMap)
}

private fun createAutoMigrationSpecMap(
    requiredAutoMigrationSpecs: Set<KClass<out AutoMigrationSpec>>,
    providedSpecs: List<AutoMigrationSpec>
): Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec> {
    if (requiredAutoMigrationSpecs.isEmpty()) {
        return emptyMap()
    }
    return buildMap {
        requiredAutoMigrationSpecs.forEach { spec ->
            val match = providedSpecs.firstOrNull { provided -> spec.safeCast(provided) != null }
            requireNotNull(match) {
                "A required auto migration spec (${spec.qualifiedName}) has not been provided."
            }
            put(spec, match)
        }
    }
}

internal abstract class TestConnectionManager : BaseRoomConnectionManager() {
    override val callbacks: List<RoomDatabase.Callback> = emptyList()

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R {
        error("Function should never be invoked during tests.")
    }

    abstract fun openConnection(): SQLiteConnection
}

private class DefaultTestConnectionManager(
    override val configuration: DatabaseConfiguration,
    override val openDelegate: RoomOpenDelegate
) : TestConnectionManager() {

    private val driverWrapper = DriverWrapper(requireNotNull(configuration.sqliteDriver))

    override fun openConnection() = driverWrapper.open(configuration.name ?: ":memory:")
}

private sealed class TestOpenDelegate(databaseBundle: DatabaseBundle) :
    RoomOpenDelegate(
        version = databaseBundle.version,
        identityHash = databaseBundle.identityHash,
        legacyIdentityHash = databaseBundle.identityHash
    ) {
    override fun onCreate(connection: SQLiteConnection) {}

    override fun onPreMigrate(connection: SQLiteConnection) {}

    override fun onPostMigrate(connection: SQLiteConnection) {}

    override fun onOpen(connection: SQLiteConnection) {}

    override fun dropAllTables(connection: SQLiteConnection) {
        error("Can't drop all tables during a test.")
    }
}

private class CreateOpenDelegate(val databaseBundle: DatabaseBundle) :
    TestOpenDelegate(databaseBundle) {
    private var createAllTables = false

    override fun onOpen(connection: SQLiteConnection) {
        check(createAllTables) {
            "Creation of tables didn't occur while creating a new database. A database at the " +
                "driver configured path likely already exists. Did you forget to delete it?"
        }
    }

    override fun onValidateSchema(connection: SQLiteConnection): ValidationResult {
        error("Validation of schemas should never occur while creating a new database.")
    }

    override fun createAllTables(connection: SQLiteConnection) {
        databaseBundle.buildCreateQueries().forEach { createSql -> connection.execSQL(createSql) }
        createAllTables = true
    }
}

private class MigrateOpenDelegate(
    val databaseBundle: DatabaseBundle,
    val validateUnknownTables: Boolean
) : TestOpenDelegate(databaseBundle) {
    override fun onValidateSchema(connection: SQLiteConnection): ValidationResult {
        val tables = databaseBundle.entitiesByTableName
        tables.values.forEach { entity ->
            when (entity) {
                is EntityBundle -> {
                    val expected = entity.toTableInfo()
                    val found = TableInfo.read(connection, entity.tableName)
                    if (expected != found) {
                        return ValidationResult(
                            isValid = false,
                            expectedFoundMsg =
                                """ ${expected.name.trimEnd()}
                                |
                                |Expected:
                                |
                                |$expected
                                |
                                |Found:
                                |
                                |$found
                                """
                                    .trimMargin()
                        )
                    }
                }
                is FtsEntityBundle -> {
                    val expected = entity.toFtsTableInfo()
                    val found = FtsTableInfo.read(connection, entity.tableName)
                    if (expected != found) {
                        return ValidationResult(
                            isValid = false,
                            expectedFoundMsg =
                                """ ${expected.name.trimEnd()}
                                |
                                |Expected:
                                |
                                |$expected
                                |
                                |Found:
                                |
                                |$found
                                """
                                    .trimMargin()
                        )
                    }
                }
            }
        }
        databaseBundle.views.forEach { view ->
            val expected = view.toViewInfo()
            val found = ViewInfo.read(connection, view.viewName)
            if (expected != found) {
                return ValidationResult(
                    isValid = false,
                    expectedFoundMsg =
                        """ ${expected.name.trimEnd()}
                        |
                        |Expected: $expected
                        |
                        |Found: $found
                        """
                            .trimMargin()
                )
            }
        }
        if (validateUnknownTables) {
            val expectedTables = buildSet {
                tables.values.forEach { entity ->
                    add(entity.tableName)
                    if (entity is FtsEntityBundle) {
                        addAll(entity.shadowTableNames)
                    }
                }
            }
            connection
                .prepare(
                    """
                SELECT name FROM sqlite_master
                WHERE type = 'table' AND name NOT IN (?, ?, ?)
                """
                        .trimIndent()
                )
                .use { statement ->
                    statement.bindText(1, Room.MASTER_TABLE_NAME)
                    statement.bindText(2, "sqlite_sequence")
                    statement.bindText(3, "android_metadata")
                    while (statement.step()) {
                        val tableName = statement.getText(0)
                        if (!expectedTables.contains(tableName)) {
                            return ValidationResult(
                                isValid = false,
                                expectedFoundMsg = "Unexpected table $tableName"
                            )
                        }
                    }
                }
        }
        return ValidationResult(true, null)
    }

    override fun createAllTables(connection: SQLiteConnection) {
        error(
            "Creation of tables should never occur while validating migrations. Did you forget " +
                "to first create the database?"
        )
    }
}
