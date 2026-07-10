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

import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.web.worker.CoroutineWebWorker
import androidx.sqlite.driver.web.worker.Worker

/** A [CoroutineWebWorker] that communicates with a worker for database operations. */
internal abstract class DatabaseWebWorker(worker: Worker) : CoroutineWebWorker(worker) {

    /**
     * Handle worker errors.
     *
     * For SQLite error, it transforms SQLite WASM error message to C-style API message format to
     * match other drivers, the WASM format is:
     * ```
     * <error_code_name>: sqlite3 result code <error_code>: <error_msg>
     * ```
     *
     * See also
     * * https://sqlite.org/wasm/doc/trunk/api-oo1.md#exceptions
     * * https://sqlite.org/wasm/doc/trunk/api-c-style.md#exceptions
     */
    override fun handleResultError(error: String): Throwable {
        val relevantPart = error.substringAfter("sqlite3 result code", missingDelimiterValue = "")
        if (relevantPart.isEmpty()) {
            // Not an SQLite3Error
            return SQLiteException(error)
        }
        val indexOfColon = relevantPart.indexOf(":")
        val code = relevantPart.substring(0, indexOfColon).trim()
        val message =
            if (indexOfColon + 1 < relevantPart.length) {
                relevantPart.substring(indexOfColon + 1, relevantPart.length).trim()
            } else {
                ""
            }
        return SQLiteException(
            buildString {
                append("Error code: $code")
                if (message.isNotEmpty()) {
                    append(", message: $message")
                }
            }
        )
    }

    abstract suspend fun open(fileName: String): WebWorkerSQLiteConnection

    abstract suspend fun prepare(
        databaseId: Int,
        sql: String,
        inTransactionSetter: (Boolean) -> Unit,
    ): WebWorkerSQLiteStatement

    abstract suspend fun step(statementId: Int, bindings: StatementBindings): StatementResult

    abstract fun closeStatement(statementId: Int)

    abstract fun closeDatabase(databaseId: Int)
}
