/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.sqlite.driver.web

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.throwSQLiteException

internal class WebWorkerSQLiteConnection(
    private val dbWorker: DatabaseWebWorker,
    private val databaseId: Int,
) : SQLiteConnection {

    private var inTransaction = false
    private var isClosed = false

    override fun inTransaction(): Boolean {
        throwIfClosed()
        return inTransaction
    }

    override suspend fun prepareAsync(sql: String): SQLiteStatement {
        throwIfClosed()
        return dbWorker.prepare(databaseId, sql, ::setInTransaction)
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            dbWorker.closeDatabase(databaseId)
        }
    }

    private fun setInTransaction(value: Boolean) {
        this.inTransaction = value
    }

    private fun throwIfClosed() {
        if (isClosed) {
            throwSQLiteException(21, "statement is closed")
        }
    }
}
