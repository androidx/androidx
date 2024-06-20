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

import androidx.room.coroutines.AndroidSQLiteDriverConnectionPool
import androidx.room.coroutines.ConnectionPool
import androidx.room.coroutines.newConnectionPool
import androidx.room.coroutines.newSingleConnectionPool
import androidx.room.driver.SupportSQLiteConnection
import androidx.room.driver.SupportSQLiteConnectionPool
import androidx.room.driver.SupportSQLiteDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.driver.AndroidSQLiteDriver

/**
 * An Android platform specific [RoomConnectionManager] with backwards compatibility with
 * [androidx.sqlite.db] APIs (SupportSQLite*).
 */
internal actual class RoomConnectionManager : BaseRoomConnectionManager {

    override val configuration: DatabaseConfiguration
    override val openDelegate: RoomOpenDelegate
    override val callbacks: List<RoomDatabase.Callback>

    private val connectionPool: ConnectionPool

    internal val supportOpenHelper: SupportSQLiteOpenHelper?
        get() = (connectionPool as? SupportSQLiteConnectionPool)?.supportDriver?.openHelper

    private var supportDatabase: SupportSQLiteDatabase? = null

    constructor(config: DatabaseConfiguration, openDelegate: RoomOpenDelegate) {
        this.configuration = config
        this.openDelegate = openDelegate
        this.callbacks = config.callbacks ?: emptyList()
        if (config.sqliteDriver == null) {
            // Compatibility mode due to no driver provided, instead a driver (SupportSQLiteDriver)
            // is created that wraps SupportSQLite* APIs. The underlying SupportSQLiteDatabase will
            // be migrated through the SupportOpenHelperCallback or through old gen code using
            // RoomOpenHelper. A ConnectionPool is also created that skips common opening
            // procedure and has no real connection management logic.
            requireNotNull(config.sqliteOpenHelperFactory) {
                "SQLiteManager was constructed with both null driver and open helper factory!"
            }
            val openHelperConfig =
                SupportSQLiteOpenHelper.Configuration.builder(config.context)
                    .name(config.name)
                    .callback(SupportOpenHelperCallback(openDelegate.version))
                    .build()
            this.connectionPool =
                SupportSQLiteConnectionPool(
                    SupportSQLiteDriver(config.sqliteOpenHelperFactory.create(openHelperConfig))
                )
        } else {
            this.connectionPool =
                if (config.sqliteDriver is AndroidSQLiteDriver) {
                    // Special-case the Android driver and use a pass-through pool since the Android
                    // bindings internally already have a thread-confined connection pool.
                    AndroidSQLiteDriverConnectionPool(
                        driver = DriverWrapper(config.sqliteDriver),
                        fileName = configuration.name ?: ":memory:"
                    )
                } else if (configuration.name == null) {
                    // An in-memory database must use a single connection pool.
                    newSingleConnectionPool(
                        driver = DriverWrapper(config.sqliteDriver),
                        fileName = ":memory:"
                    )
                } else {
                    newConnectionPool(
                        driver = DriverWrapper(config.sqliteDriver),
                        fileName = configuration.name,
                        maxNumOfReaders = configuration.journalMode.getMaxNumberOfReaders(),
                        maxNumOfWriters = configuration.journalMode.getMaxNumberOfWriters()
                    )
                }
        }
        init()
    }

    constructor(
        config: DatabaseConfiguration,
        supportOpenHelperFactory: (DatabaseConfiguration) -> SupportSQLiteOpenHelper
    ) {
        this.configuration = config
        this.openDelegate = NoOpOpenDelegate()
        this.callbacks = config.callbacks ?: emptyList()
        // Compatibility mode due to no driver provided, the SupportSQLiteDriver and
        // SupportConnectionPool are created. A Room onOpen callback is installed so that the
        // SupportSQLiteDatabase is extracted out of the RoomOpenHelper installed.
        val configWithCompatibilityCallback =
            config.installOnOpenCallback { db -> supportDatabase = db }
        this.connectionPool =
            SupportSQLiteConnectionPool(
                SupportSQLiteDriver(
                    supportOpenHelperFactory.invoke(configWithCompatibilityCallback)
                )
            )
        init()
    }

    private fun init() {
        val wal = configuration.journalMode == RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING
        supportOpenHelper?.setWriteAheadLoggingEnabled(wal)
    }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R = connectionPool.useConnection(isReadOnly, block)

    fun close() {
        connectionPool.close()
    }

    // TODO(b/316944352): Figure out auto-close with driver APIs
    fun isSupportDatabaseOpen() = supportDatabase?.isOpen ?: false

    /** An implementation of [SupportSQLiteOpenHelper.Callback] used in compatibility mode. */
    inner class SupportOpenHelperCallback(version: Int) :
        SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            this@RoomConnectionManager.onCreate(SupportSQLiteConnection(db))
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            this@RoomConnectionManager.onMigrate(
                SupportSQLiteConnection(db),
                oldVersion,
                newVersion
            )
        }

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            this.onUpgrade(db, oldVersion, newVersion)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            this@RoomConnectionManager.onOpen(SupportSQLiteConnection(db))
            supportDatabase = db
        }
    }

    /**
     * A no op implementation of [RoomOpenDelegate] used in compatibility mode with old gen code
     * that relies on [RoomOpenHelper].
     */
    private class NoOpOpenDelegate : RoomOpenDelegate(-1, "", "") {
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

    private fun DatabaseConfiguration.installOnOpenCallback(
        onOpen: (SupportSQLiteDatabase) -> Unit
    ): DatabaseConfiguration {
        val newCallbacks =
            (this.callbacks ?: emptyList()) +
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        onOpen.invoke(db)
                    }
                }
        return this.copy(callbacks = newCallbacks)
    }
}
