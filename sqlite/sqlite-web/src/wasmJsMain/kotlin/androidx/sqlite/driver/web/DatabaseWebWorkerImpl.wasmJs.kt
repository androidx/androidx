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

import androidx.sqlite.driver.web.util.initExternal
import androidx.sqlite.driver.web.util.toIntArray
import androidx.sqlite.driver.web.util.toStringArray
import org.w3c.dom.Worker

/** A [DatabaseWebWorker] implementation for wasmJs. */
internal class DatabaseWebWorkerImpl(worker: Worker) : DatabaseWebWorker(worker) {
    override suspend fun open(fileName: String): WebWorkerSQLiteConnection {
        val request = OpenDbRequest(fileName)
        val result =
            sendRequest(request) {
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                it.data as OpenDbResult
            }
        return WebWorkerSQLiteConnection(this, databaseId = result.databaseId)
    }

    override suspend fun prepare(
        databaseId: Int,
        sql: String,
        inTransactionSetter: (Boolean) -> Unit,
    ): WebWorkerSQLiteStatement {
        val request = PrepareStmtRequest(databaseId, sql)
        val result =
            sendRequest(request) {
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                it.data as PrepareStmtResult
            }
        return WebWorkerSQLiteStatement.create(
            dbWorker = this,
            databaseId = databaseId,
            statementId = result.statementId,
            sql = sql,
            parameterCount = result.parameterCount,
            columnNames = result.columnNames.toStringArray(),
            inTransactionSetter = inTransactionSetter,
        )
    }

    override suspend fun step(statementId: Int, bindings: StatementBindings): StatementResult {
        val request = StepStmtRequest(statementId, bindings.array)
        val result =
            sendRequest(request) {
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                it.data as StepStmtResult
            }
        return StatementResult(rows = result.rows, columnTypes = result.columnTypes.toIntArray())
    }

    override fun closeStatement(statementId: Int) {
        sendRequest(CloseRequest(statementId, null))
    }

    override fun closeDatabase(databaseId: Int) {
        sendRequest(CloseRequest(null, databaseId))
    }
}

private external interface CommandRequest {
    var cmd: String
}

private external interface OpenDbRequest : CommandRequest, JsAny {
    var fileName: String
}

private fun OpenDbRequest(fileName: String): OpenDbRequest =
    initExternal<OpenDbRequest>().apply {
        this.cmd = "open"
        this.fileName = fileName
    }

private external interface OpenDbResult : JsAny {
    val databaseId: Int
}

private external interface PrepareStmtRequest : CommandRequest, JsAny {
    var databaseId: Int
    var sql: String
}

private fun PrepareStmtRequest(databaseId: Int, sql: String): PrepareStmtRequest =
    initExternal<PrepareStmtRequest>().apply {
        this.cmd = "prepare"
        this.databaseId = databaseId
        this.sql = sql
    }

private external interface PrepareStmtResult : JsAny {
    val statementId: Int
    val parameterCount: Int
    val columnNames: JsArray<JsString>
}

private external interface StepStmtRequest : CommandRequest, JsAny {
    var statementId: Int
    var bindings: JsArray<JsAny?>
}

private fun StepStmtRequest(statementId: Int, bindings: JsArray<JsAny?>): StepStmtRequest =
    initExternal<StepStmtRequest>().apply {
        this.cmd = "step"
        this.statementId = statementId
        this.bindings = bindings
    }

private external interface StepStmtResult : JsAny {
    val rows: JsArray<JsArray<JsAny?>>
    val columnTypes: JsArray<JsNumber>
}

private external interface CloseRequest : CommandRequest, JsAny {
    var statementId: Int?
    var databaseId: Int?
}

private fun CloseRequest(statementId: Int?, databaseId: Int?): CloseRequest =
    initExternal<CloseRequest>().apply {
        this.cmd = "close"
        this.statementId = statementId
        this.databaseId = databaseId
    }
