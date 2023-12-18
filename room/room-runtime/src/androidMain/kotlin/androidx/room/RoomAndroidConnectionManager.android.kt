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

import androidx.room.driver.SupportSQLiteConnection
import androidx.room.driver.SupportSQLiteDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * An Android platform specific [RoomConnectionManager] with backwards compatibility with
 * [androidx.sqlite.db] APIs (SupportSQLite*).
 */
internal class RoomAndroidConnectionManager : RoomConnectionManager {

    override val configuration: DatabaseConfiguration
    override val sqliteDriver: SQLiteDriver
    override val openDelegate: RoomOpenDelegate

    private val callbacks: List<RoomDatabase.Callback>

    internal val supportOpenHelper: SupportSQLiteOpenHelper?
        get() = (sqliteDriver as? SupportSQLiteDriver)?.openHelper

    private var supportDatabase: SupportSQLiteDatabase? = null

    constructor(
        config: DatabaseConfiguration,
        openDelegate: RoomOpenDelegate
    ) {
        this.configuration = config
        if (config.sqliteDriver == null) {
            // Compatibility mode due to no driver provided, instead a driver
            // (SupportSQLiteDriver) is created that wraps SupportSQLite* APIs.
            requireNotNull(config.sqliteOpenHelperFactory) {
                "SQLiteManager was constructed with both null driver and open helper factory!"
            }
            val openHelperConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context)
                .name(config.name)
                .callback(SupportOpenHelperCallback(openDelegate.version))
                .build()
            this.sqliteDriver = SupportSQLiteDriver(
                config.sqliteOpenHelperFactory.create(openHelperConfig)
            )
            this.supportOpenHelper
        } else {
            this.sqliteDriver = config.sqliteDriver
        }
        this.openDelegate = openDelegate
        this.callbacks = config.callbacks ?: emptyList()
    }

    constructor(
        config: DatabaseConfiguration,
        supportOpenHelper: SupportSQLiteOpenHelper
    ) {
        this.configuration = config
        this.openDelegate = NoOpOpenDelegate()
        // Compatibility mode, a driver (SupportSQLiteDriver) is created that wraps
        // the provided SupportSQLiteOpenHelper.
        this.sqliteDriver = SupportSQLiteDriver(supportOpenHelper)
        this.callbacks = config.callbacks ?: emptyList()
    }

    override fun getConnection(): SQLiteConnection {
        // In compatibility mode the driver is a SupportSQLiteDriver, the underlying
        // SupportSQLiteDatabase will have already been migrated through SupportOpenHelperCallback
        // or through old gen code using RoomOpenHelper, therefore skip opening procedure.
        if (sqliteDriver is SupportSQLiteDriver) {
            return sqliteDriver.open()
        }
        return openConnection()
    }

    override fun dropAllTables(connection: SQLiteConnection) {
        if (configuration.allowDestructiveMigrationForAllTables) {
            // Drops all tables (excluding special ones)
            super.dropAllTables(connection)
        } else {
            // Drops known tables (Room entity tables)
            openDelegate.dropAllTables(connection)
        }
    }

    fun close() {
        supportOpenHelper?.close()
    }

    override fun invokeCreateCallback(connection: SQLiteConnection) {
        // TODO(b/316944352): Add callback mirror of SQLiteConnection
        callbacks.forEach {
            if (connection is SupportSQLiteConnection) {
                it.onCreate(connection.db)
            }
        }
    }

    override fun invokeDestructiveMigrationCallback(connection: SQLiteConnection) {
        // TODO(b/316944352): Add callback mirror of SQLiteConnection
        callbacks.forEach {
            if (connection is SupportSQLiteConnection) {
                it.onDestructiveMigration(connection.db)
            }
        }
    }

    override fun invokeOpenCallback(connection: SQLiteConnection) {
        // TODO(b/316944352): Add callback mirror of SQLiteConnection
        callbacks.forEach {
            if (connection is SupportSQLiteConnection) {
                it.onOpen(connection.db)
            }
        }
    }

    // TODO(b/316944352): Figure out auto-close with driver APIs
    fun isSupportDatabaseOpen() = supportDatabase?.isOpen ?: false

    /**
     * An implementation of [SupportSQLiteOpenHelper.Callback] used in compatibility mode.
     */
    inner class SupportOpenHelperCallback(
        version: Int
    ) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            this@RoomAndroidConnectionManager.onCreate(
                SupportSQLiteConnection(db)
            )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            this@RoomAndroidConnectionManager.onMigrate(
                SupportSQLiteConnection(db), oldVersion, newVersion
            )
        }

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            this.onUpgrade(db, oldVersion, newVersion)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            this@RoomAndroidConnectionManager.onOpen(SupportSQLiteConnection(db))
            supportDatabase = db
        }
    }

    /**
     * A no op implementation of [RoomOpenDelegate] used in compatibility mode with old gen code.
     */
    private class NoOpOpenDelegate : RoomOpenDelegate(-1, "") {
        override fun onCreate(connection: SQLiteConnection) {
            error("NOP delegate should never be called")
        }

        override fun onPreMigrate(connection: SQLiteConnection) {
            error("NOP delegate should never be called")
        }

        override fun onValidateSchema(connection: SQLiteConnection): ValidationResult {
            error("NOP delegate should never be called")
        }

        override fun onPostMigrate(connection: SQLiteConnection) {
            error("NOP delegate should never be called")
        }

        override fun onOpen(connection: SQLiteConnection) {
            error("NOP delegate should never be called")
        }

        override fun createAllTables(connection: SQLiteConnection) {
            error("NOP delegate should never be called")
        }

        override fun dropAllTables(connection: SQLiteConnection) {
            error("NOP delegate should never be called")
        }
    }
}
