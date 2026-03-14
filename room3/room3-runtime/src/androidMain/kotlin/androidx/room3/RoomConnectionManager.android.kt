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

package androidx.room3

import androidx.room3.autoclose.AutoCloser
import androidx.room3.coroutines.ConnectionPool
import androidx.room3.coroutines.PassthroughConnectionPool
import androidx.room3.coroutines.TransactionWrapper
import androidx.room3.coroutines.newConnectionPool
import androidx.room3.coroutines.newSingleConnectionPool
import kotlinx.coroutines.runBlocking

/**
 * An Android platform specific [RoomConnectionManager] with backwards compatibility with
 * [androidx.sqlite.db] APIs (SupportSQLite*).
 */
internal actual class RoomConnectionManager : BaseRoomConnectionManager {

    override val configuration: DatabaseConfiguration
    override val openDelegate: RoomOpenDelegate
    override val callbacks: List<RoomDatabase.Callback>

    internal val connectionPool: ConnectionPool

    private var autoCloser: AutoCloser? = null

    constructor(
        config: DatabaseConfiguration,
        openDelegate: RoomOpenDelegate,
        transactionWrapper: TransactionWrapper<Any?>,
    ) {
        this.configuration = config
        this.openDelegate = openDelegate
        this.callbacks = config.callbacks
        this.connectionPool =
            if (config.sqliteDriver.hasConnectionPool) {
                // If the driver already has a connection pool then use a pass-through pool to
                // support drivers such as the Android since internally it already has a
                // thread-confined connection pool.
                PassthroughConnectionPool(
                    connectionFactory =
                        createConnectionFactory(config.sqliteDriver, config.name ?: ":memory:"),
                    transactionWrapper = transactionWrapper,
                )
            } else if (config.name == null) {
                // An in-memory database must use a single connection pool.
                newSingleConnectionPool(
                    connectionFactory = createConnectionFactory(config.sqliteDriver, ":memory:"),
                    statementCacheSize = config.preparedStatementCacheSize,
                )
            } else {
                newConnectionPool(
                    connectionFactory = createConnectionFactory(config.sqliteDriver, config.name),
                    maxNumOfReaders = config.journalMode.getMaxNumberOfReaders(),
                    maxNumOfWriters = config.journalMode.getMaxNumberOfWriters(),
                    statementCacheSize = config.preparedStatementCacheSize,
                )
            }
    }

    internal fun setAutoCloser(autoCloser: AutoCloser) {
        this.autoCloser = autoCloser
        autoCloser.setAutoOpenCallback {
            // TODO(b/316944816): Fix me! Can we avoid this runBlocking?
            runBlocking { configurationConnection(it) }
        }
    }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R,
    ): R {
        autoCloser?.incrementCount()
        try {
            return connectionPool.useConnection(isReadOnly, block)
        } finally {
            autoCloser?.decrementCount()
        }
    }

    override fun resolveFileName(fileName: String): String =
        if (fileName != ":memory:") {
            // Get database path from context, if the database name is not an absolute path, then
            // the app's database directory will be used, otherwise the given path is used.
            configuration.context.getDatabasePath(fileName).absolutePath
        } else {
            fileName
        }

    fun close() {
        autoCloser?.close()
        connectionPool.close()
    }
}
