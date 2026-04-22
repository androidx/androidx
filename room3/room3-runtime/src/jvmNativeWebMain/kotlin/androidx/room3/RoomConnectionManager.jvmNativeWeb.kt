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

package androidx.room3

import androidx.room3.coroutines.ConnectionPool
import androidx.room3.coroutines.PassthroughConnectionPool
import androidx.room3.coroutines.newConnectionPool
import androidx.room3.coroutines.newSingleConnectionPool

internal actual class RoomConnectionManager : BaseRoomConnectionManager {

    override val configuration: DatabaseConfiguration
    override val openDelegate: RoomOpenDelegate
    override val callbacks: List<RoomDatabase.Callback>

    internal val connectionPool: ConnectionPool

    constructor(config: DatabaseConfiguration, openDelegate: RoomOpenDelegate) {
        this.configuration = config
        this.openDelegate = openDelegate
        this.callbacks = configuration.callbacks
        this.connectionPool =
            if (configuration.sqliteDriver.hasConnectionPool) {
                // If the driver already has a connection pool then use a pass-through pool.
                PassthroughConnectionPool(
                    connectionFactory =
                        createConnectionFactory(
                            configuration.sqliteDriver,
                            configuration.name ?: ":memory:",
                        )
                )
            } else if (configuration.name == null) {
                // An in-memory database must use a single connection pool.
                newSingleConnectionPool(
                    connectionFactory =
                        createConnectionFactory(configuration.sqliteDriver, ":memory:")
                )
            } else {
                when (val poolConfig = configuration.connectionPoolConfiguration) {
                    is SingleConnection ->
                        newSingleConnectionPool(
                            connectionFactory =
                                createConnectionFactory(
                                    configuration.sqliteDriver,
                                    configuration.name,
                                )
                        )
                    is MultipleConnection ->
                        newConnectionPool(
                            connectionFactory =
                                createConnectionFactory(
                                    configuration.sqliteDriver,
                                    configuration.name,
                                ),
                            maxNumOfReaders = poolConfig.numOfReaders,
                            maxNumOfWriters = poolConfig.numOfWriters,
                        )
                }
            }
    }

    override suspend fun <R> useConnection(isReadOnly: Boolean, block: suspend (Transactor) -> R) =
        connectionPool.useConnection(isReadOnly, block)

    fun close() {
        connectionPool.close()
    }
}
