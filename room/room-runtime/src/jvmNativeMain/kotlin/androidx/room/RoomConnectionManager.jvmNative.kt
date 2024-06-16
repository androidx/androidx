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

package androidx.room

import androidx.room.coroutines.ConnectionPool
import androidx.room.coroutines.newConnectionPool
import androidx.room.coroutines.newSingleConnectionPool
import androidx.sqlite.SQLiteDriver

internal actual class RoomConnectionManager(
    override val configuration: DatabaseConfiguration,
    sqliteDriver: SQLiteDriver,
    override val openDelegate: RoomOpenDelegate,
    override val callbacks: List<RoomDatabase.Callback>,
) : BaseRoomConnectionManager() {

    private val connectionPool: ConnectionPool =
        if (configuration.name == null) {
            // An in-memory database must use a single connection pool.
            newSingleConnectionPool(driver = DriverWrapper(sqliteDriver), fileName = ":memory:")
        } else {
            newConnectionPool(
                driver = DriverWrapper(sqliteDriver),
                fileName = configuration.name,
                maxNumOfReaders = configuration.journalMode.getMaxNumberOfReaders(),
                maxNumOfWriters = configuration.journalMode.getMaxNumberOfWriters()
            )
        }

    override suspend fun <R> useConnection(isReadOnly: Boolean, block: suspend (Transactor) -> R) =
        connectionPool.useConnection(isReadOnly, block)

    fun close() {
        connectionPool.close()
    }
}
