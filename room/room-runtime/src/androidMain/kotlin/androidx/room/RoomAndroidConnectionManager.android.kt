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

import androidx.room.coroutines.ConnectionPool
import androidx.room.coroutines.newConnectionPool
import androidx.room.coroutines.newSingleConnectionPool
import androidx.room.driver.SupportSQLiteConnection
import androidx.room.driver.SupportSQLiteDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.use

/**
 * An Android platform specific [RoomConnectionManager] with backwards compatibility with
 * [androidx.sqlite.db] APIs (SupportSQLite*).
 */
internal class RoomAndroidConnectionManager : RoomConnectionManager {

    override val configuration: DatabaseConfiguration
    override val connectionPool: ConnectionPool
    override val openDelegate: RoomOpenDelegate

    private val callbacks: List<RoomDatabase.Callback>

    internal val supportOpenHelper: SupportSQLiteOpenHelper?
        get() = (connectionPool as? SupportConnectionPool)?.supportDriver?.openHelper

    private var supportDatabase: SupportSQLiteDatabase? = null

    constructor(
        config: DatabaseConfiguration,
        openDelegate: RoomOpenDelegate
    ) {
        this.configuration = config
        if (config.sqliteDriver == null) {
            // Compatibility mode due to no driver provided, instead a driver (SupportSQLiteDriver)
            // is created that wraps SupportSQLite* APIs. The underlying SupportSQLiteDatabase will
            // be migrated through the SupportOpenHelperCallback or through old gen code using
            // RoomOpenHelper. A ConnectionPool is also created that skips common opening
            // procedure and has no real connection management logic.
            requireNotNull(config.sqliteOpenHelperFactory) {
                "SQLiteManager was constructed with both null driver and open helper factory!"
            }
            val openHelperConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context)
                .name(config.name)
                .callback(SupportOpenHelperCallback(openDelegate.version))
                .build()
            this.connectionPool = SupportConnectionPool(
                SupportSQLiteDriver(config.sqliteOpenHelperFactory.create(openHelperConfig))
            )
        } else {
            this.connectionPool = if (configuration.name == null) {
                // An in-memory database must use a single connection pool.
                newSingleConnectionPool(
                    driver = DriverWrapper(config.sqliteDriver)
                )
            } else {
                newConnectionPool(
                    driver = DriverWrapper(config.sqliteDriver),
                    maxNumOfReaders = configuration.journalMode.getMaxNumberOfReaders(),
                    maxNumOfWriters = configuration.journalMode.getMaxNumberOfWriters()
                )
            }
        }
        this.openDelegate = openDelegate
        this.callbacks = config.callbacks ?: emptyList()
        init()
    }

    constructor(
        config: DatabaseConfiguration,
        supportOpenHelperFactory: (DatabaseConfiguration) -> SupportSQLiteOpenHelper
    ) {
        this.configuration = config
        this.openDelegate = NoOpOpenDelegate()
        // Compatibility mode due to no driver provided, the SupportSQLiteDriver and
        // SupportConnectionPool are created. A Room onOpen callback is installed so that the
        // SupportSQLiteDatabase is extracted out of the RoomOpenHelper installed.
        val configWithCompatibilityCallback =
            config.installOnOpenCallback { db -> supportDatabase = db }
        this.connectionPool = SupportConnectionPool(
            SupportSQLiteDriver(supportOpenHelperFactory.invoke(configWithCompatibilityCallback))
        )
        this.callbacks = config.callbacks ?: emptyList()
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
        connectionPool.close()
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
     * A no op implementation of [RoomOpenDelegate] used in compatibility mode with old gen code
     * that relies on [RoomOpenHelper].
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

    private class SupportConnectionPool(
        val supportDriver: SupportSQLiteDriver
    ) : ConnectionPool {
        override suspend fun <R> useConnection(
            isReadOnly: Boolean,
            block: suspend (Transactor) -> R
        ): R {
            return block.invoke(SupportPooledConnection(supportDriver.open()))
        }

        override fun close() {
            supportDriver.openHelper.close()
        }
    }

    /**
     * An implementation of a connection pool used in compatibility mode. This impl doesn't do
     * any connection management since the SupportSQLite* APIs already internally do.
     */
    private class SupportPooledConnection(
        val delegate: SupportSQLiteConnection
    ) : Transactor {

        private var currentTransactionType: Transactor.SQLiteTransactionType? = null

        override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
            return delegate.prepare(sql).use { block.invoke(it) }
        }

        // TODO(b/318767291): Add coroutine confinement like RoomDatabase.withTransaction
        override suspend fun <R> withTransaction(
            type: Transactor.SQLiteTransactionType,
            block: suspend TransactionScope<R>.() -> R
        ): R {
            return transaction(type, block)
        }

        private suspend fun <R> transaction(
            type: Transactor.SQLiteTransactionType,
            block: suspend TransactionScope<R>.() -> R
        ): R {
            val db = delegate.db
            if (!db.inTransaction()) {
                currentTransactionType = type
            }
            when (type) {
                Transactor.SQLiteTransactionType.DEFERRED -> db.beginTransactionReadOnly()
                Transactor.SQLiteTransactionType.IMMEDIATE -> db.beginTransactionNonExclusive()
                Transactor.SQLiteTransactionType.EXCLUSIVE -> db.beginTransaction()
            }
            try {
                val result = SupportTransactor<R>().block()
                db.setTransactionSuccessful()
                return result
            } catch (rollback: RollbackException) {
                @Suppress("UNCHECKED_CAST")
                return rollback.result as R
            } finally {
                db.endTransaction()
                if (!db.inTransaction()) {
                    currentTransactionType = null
                }
            }
        }

        override suspend fun inTransaction(): Boolean {
            return delegate.db.inTransaction()
        }

        private class RollbackException(val result: Any?) : Throwable()

        private inner class SupportTransactor<T> : TransactionScope<T> {

            override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
                return this@SupportPooledConnection.usePrepared(sql, block)
            }

            override suspend fun <R> withNestedTransaction(
                block: suspend (TransactionScope<R>) -> R
            ): R {
                return transaction(checkNotNull(currentTransactionType), block)
            }

            override suspend fun rollback(result: T): Nothing {
                throw RollbackException(result)
            }
        }
    }

    private fun DatabaseConfiguration.installOnOpenCallback(
        onOpen: (SupportSQLiteDatabase) -> Unit
    ): DatabaseConfiguration {
        val newCallbacks = (this.callbacks ?: emptyList()) + object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                onOpen.invoke(db)
            }
        }
        return this.copy(callbacks = newCallbacks)
    }
}
