/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.room.RoomDatabase.JournalMode.TRUNCATE
import androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING
import androidx.room.coroutines.ConnectionPool
import androidx.room.util.findMigrationPath
import androidx.room.util.isMigrationRequired
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import androidx.sqlite.use

/**
 * Room's database connection manager, responsible for opening and managing such connections,
 * including performing migrations if necessary and validating schema.
 */
internal abstract class RoomConnectionManager {

    protected abstract val configuration: DatabaseConfiguration
    protected abstract val connectionPool: ConnectionPool
    protected abstract val openDelegate: RoomOpenDelegate

    abstract suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R

    /* A driver wrapper that configures opened connections per the manager. */
    protected inner class DriverWrapper(
        private val actual: SQLiteDriver
    ) : SQLiteDriver {
        override fun open(): SQLiteConnection {
            // TODO(b/317973999): Try to validate connections point to the same filename as the
            //                    one in the database configuration provided in the builder.
            return configureConnection(actual.open())
        }
    }

    /**
     * Common database connection configuration and opening procedure, performs migrations if
     * necessary, validates schema and invokes configured callbacks if any.
     */
    // TODO(b/316945717): Thread safe and process safe opening and migration
    // TODO(b/316944352): Retry mechanism
    private fun configureConnection(connection: SQLiteConnection): SQLiteConnection {
        configureJournalMode(connection)
        val version = connection.prepare("PRAGMA user_version").use { statement ->
            statement.step()
            statement.getLong(0).toInt()
        }
        if (version != openDelegate.version) {
            connection.execSQL("BEGIN EXCLUSIVE TRANSACTION")
            runCatching {
                if (version == 0) {
                    onCreate(connection)
                } else {
                    onMigrate(connection, version, openDelegate.version)
                }
                connection.execSQL("PRAGMA user_version = ${openDelegate.version}")
            }.onSuccess {
                connection.execSQL("END TRANSACTION")
            }.onFailure {
                connection.execSQL("ROLLBACK TRANSACTION")
                throw it
            }
        }
        onOpen(connection)
        return connection
    }

    private fun configureJournalMode(connection: SQLiteConnection) {
        val wal = configuration.journalMode == WRITE_AHEAD_LOGGING
        if (wal) {
            connection.execSQL("PRAGMA journal_mode = WAL")
        } else {
            connection.execSQL("PRAGMA journal_mode = TRUNCATE")
        }
    }

    protected fun onCreate(connection: SQLiteConnection) {
        val isEmptyDatabase = hasEmptySchema(connection)
        openDelegate.createAllTables(connection)
        if (!isEmptyDatabase) {
            // A 0 version pre-populated database goes through the create path, Room only allows
            // for versions greater than 0, so if we find the database not to be empty, then it is
            // a pre-populated, we must validate it to see if its suitable for usage.
            val result = openDelegate.onValidateSchema(connection)
            if (!result.isValid) {
                error("Pre-packaged database has an invalid schema: ${result.expectedFoundMsg}")
            }
        }
        updateIdentity(connection)
        openDelegate.onCreate(connection)
        invokeCreateCallback(connection)
    }

    private fun hasEmptySchema(connection: SQLiteConnection): Boolean =
        connection.prepare(
            "SELECT count(*) FROM sqlite_master WHERE name != 'android_metadata'"
        ).use {
            it.step() && it.getLong(0) == 0L
        }

    private fun updateIdentity(connection: SQLiteConnection) {
        createMasterTableIfNotExists(connection)
        connection.execSQL(RoomMasterTable.createInsertQuery(openDelegate.identityHash))
    }

    private fun createMasterTableIfNotExists(connection: SQLiteConnection) {
        connection.execSQL(RoomMasterTable.CREATE_QUERY)
    }

    protected fun onMigrate(connection: SQLiteConnection, oldVersion: Int, newVersion: Int) {
        var migrated = false
        val migrations =
            configuration.migrationContainer.findMigrationPath(oldVersion, newVersion)
        if (migrations != null) {
            openDelegate.onPreMigrate(connection)
            migrations.forEach { it.migrate(connection) }
            val result = openDelegate.onValidateSchema(connection)
            if (!result.isValid) {
                error("Migration didn't properly handle: ${result.expectedFoundMsg}")
            }
            openDelegate.onPostMigrate(connection)
            updateIdentity(connection)
            migrated = true
        }
        if (!migrated) {
            if (configuration.isMigrationRequired(oldVersion, newVersion)) {
                error(
                    "A migration from $oldVersion to $newVersion was required but not found. " +
                        "Please provide the necessary Migration path via " +
                        "RoomDatabase.Builder.addMigration(...) or allow for " +
                        "destructive migrations via one of the " +
                        "RoomDatabase.Builder.fallbackToDestructiveMigration* methods."
                )
            }
            dropAllTables(connection)
            invokeDestructiveMigrationCallback(connection)
            openDelegate.createAllTables(connection)
        }
    }

    protected open fun dropAllTables(connection: SQLiteConnection) {
        connection.prepare(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ).use { statement ->
            buildList {
                while (statement.step()) {
                    val name = statement.getText(0)
                    if (name.startsWith("sqlite_") || name == "android_metadata") {
                        continue
                    }
                    add(name)
                }
            }
        }.forEach { table ->
            connection.execSQL("DROP TABLE IF EXISTS $table")
        }
    }

    protected fun onOpen(connection: SQLiteConnection) {
        checkIdentity(connection)
        openDelegate.onOpen(connection)
        invokeOpenCallback(connection)
    }

    private fun checkIdentity(connection: SQLiteConnection) {
        if (hasRoomMasterTable(connection)) {
            val identityHash: String? = connection.prepare(RoomMasterTable.READ_QUERY).use {
                if (it.step()) {
                    it.getText(0)
                } else {
                    null
                }
            }

            if (openDelegate.identityHash != identityHash) {
                error(
                    "Room cannot verify the data integrity. Looks like" +
                        " you've changed schema but forgot to update the version number. You can" +
                        " simply fix this by increasing the version number. Expected identity" +
                        " hash: ${openDelegate.identityHash}, found: $identityHash"
                )
            }
        } else {
            // No room_master_table, this might an a pre-populated DB, we must validate to see if
            // its suitable for usage.
            val result = openDelegate.onValidateSchema(connection)
            if (!result.isValid) {
                error("Pre-packaged database has an invalid schema: ${result.expectedFoundMsg}")
            }
            openDelegate.onPostMigrate(connection)
            updateIdentity(connection)
        }
    }

    private fun hasRoomMasterTable(connection: SQLiteConnection): Boolean =
        connection.prepare(
            "SELECT 1 FROM sqlite_master " +
                "WHERE type = 'table' AND name = '${RoomMasterTable.TABLE_NAME}'"
        ).use {
            it.step() && it.getLong(0) != 0L
        }

    @Suppress("REDUNDANT_ELSE_IN_WHEN") // Redundant in common but not in Android
    protected fun RoomDatabase.JournalMode.getMaxNumberOfReaders() = when (this) {
        TRUNCATE -> 1
        WRITE_AHEAD_LOGGING -> 4
        else -> error("Can't get max number of reader for journal mode '$this'")
    }

    @Suppress("REDUNDANT_ELSE_IN_WHEN") // Redundant in common but not in Android
    protected fun RoomDatabase.JournalMode.getMaxNumberOfWriters() = when (this) {
        TRUNCATE -> 1
        WRITE_AHEAD_LOGGING -> 1
        else -> error("Can't get max number of writers for journal mode '$this'")
    }

    protected abstract fun invokeCreateCallback(connection: SQLiteConnection)
    protected abstract fun invokeDestructiveMigrationCallback(connection: SQLiteConnection)
    protected abstract fun invokeOpenCallback(connection: SQLiteConnection)
}
