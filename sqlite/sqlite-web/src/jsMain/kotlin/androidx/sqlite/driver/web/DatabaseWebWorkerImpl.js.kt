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

import org.w3c.dom.Worker

/** A [DatabaseWebWorker] implementation for Js. */
internal class DatabaseWebWorkerImpl(worker: Worker) : DatabaseWebWorker(worker) {
    override suspend fun open(fileName: String): WebWorkerSQLiteConnection {
        val request = OpenDbRequest(fileName)
        val result = sendRequest(request) { OpenDbResult(it.data) }
        return WebWorkerSQLiteConnection(this, databaseId = result.databaseId)
    }

    override suspend fun prepare(
        databaseId: Int,
        sql: String,
        inTransactionSetter: (Boolean) -> Unit,
    ): WebWorkerSQLiteStatement {
        val request = PrepareStmtRequest(databaseId, sql)
        val result = sendRequest(request) { PrepareStmtResult(it.data) }
        return WebWorkerSQLiteStatement.create(
            dbWorker = this,
            databaseId = databaseId,
            statementId = result.statementId,
            sql = sql,
            parameterCount = result.parameterCount,
            columnNames = result.columnNames,
            inTransactionSetter = inTransactionSetter,
        )
    }

    override suspend fun step(statementId: Int, bindings: StatementBindings): StatementResult {
        val request = StepStmtRequest(statementId, bindings.array)
        val result = sendRequest(request) { StepStmtResult(it.data) }
        return StatementResult(result.rows, result.columnTypes)
    }

    override fun closeStatement(statementId: Int) {
        sendRequest(CloseRequest(statementId, null))
    }

    override fun closeDatabase(databaseId: Int) {
        sendRequest(CloseRequest(null, databaseId))
    }
}

private abstract class CommandRequest(@JsName("cmd") val cmd: String)

private class OpenDbRequest(@JsName("fileName") val fileName: String) : CommandRequest("open")

private class OpenDbResult private constructor(val databaseId: Int) {
    constructor(raw: dynamic) : this(raw["databaseId"])
}

private class PrepareStmtRequest(
    @JsName("databaseId") val databaseId: Int,
    @JsName("sql") val sql: String,
) : CommandRequest("prepare")

private class PrepareStmtResult
private constructor(val statementId: Int, val parameterCount: Int, val columnNames: Array<String>) {
    constructor(raw: dynamic) : this(raw["statementId"], raw["parameterCount"], raw["columnNames"])
}

private class StepStmtRequest(
    @JsName("statementId") val statementId: Int,
    @JsName("bindings") val bindings: Array<Any?>,
) : CommandRequest("step")

private class StepStmtResult
private constructor(val rows: Array<Array<dynamic>>, val columnTypes: IntArray) {
    constructor(raw: dynamic) : this(raw["rows"], raw["columnTypes"])
}

private class CloseRequest(
    @JsName("statementId") val statementId: Int?,
    @JsName("databaseId") val databaseId: Int?,
) : CommandRequest("close")
