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

import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.throwSQLiteException
import androidx.sqlite.util.getStatementPrefix

internal class WebWorkerSQLiteStatement
private constructor(
    private val dbWorker: DatabaseWebWorker,
    private val databaseId: Int,
    private val statementId: Int,
    private val sql: String,
    private val parameterCount: Int,
    private val columnNames: Array<String>,
    private val transactionOperation: TransactionOperation?,
    private val inTransactionSetter: ((Boolean) -> Unit)?,
) : SQLiteStatement {

    companion object {
        fun create(
            dbWorker: DatabaseWebWorker,
            databaseId: Int,
            statementId: Int,
            sql: String,
            parameterCount: Int,
            columnNames: Array<String>,
            inTransactionSetter: (Boolean) -> Unit,
        ): WebWorkerSQLiteStatement {
            val sqlString = sql.trim().uppercase()
            val sqlPrefix = getStatementPrefix(sqlString)
            if (sqlPrefix == null) {
                return WebWorkerSQLiteStatement(
                    dbWorker,
                    databaseId,
                    statementId,
                    sql,
                    parameterCount,
                    columnNames,
                    null,
                    null,
                )
            }
            // Special-case transaction statements to reflect inTransaction state in connection.
            val transactionOp = getTransactionOperation(sqlPrefix, sqlString)
            return if (transactionOp != null) {
                WebWorkerSQLiteStatement(
                    dbWorker,
                    databaseId,
                    statementId,
                    sql,
                    parameterCount,
                    columnNames,
                    transactionOp,
                    inTransactionSetter,
                )
            } else {
                WebWorkerSQLiteStatement(
                    dbWorker,
                    databaseId,
                    statementId,
                    sql,
                    parameterCount,
                    columnNames,
                    null,
                    null,
                )
            }
        }

        private fun getTransactionOperation(prefix: String, sql: String): TransactionOperation? =
            when (prefix) {
                "END",
                "COM" -> TransactionOperation.END
                "ROL" ->
                    if (sql.contains(" TO ")) {
                        null
                    } else {
                        TransactionOperation.ROLLBACK
                    }
                "BEG" -> {
                    if (sql.contains("EXCLUSIVE")) {
                        TransactionOperation.BEGIN_EXCLUSIVE
                    } else if (sql.contains("IMMEDIATE")) {
                        TransactionOperation.BEGIN_IMMEDIATE
                    } else {
                        TransactionOperation.BEGIN_DEFERRED
                    }
                }
                else -> null
            }

        private enum class TransactionOperation {
            END,
            ROLLBACK,
            BEGIN_EXCLUSIVE,
            BEGIN_IMMEDIATE,
            BEGIN_DEFERRED,
        }
    }

    private var isClosed = false

    private val bindings = StatementBindings()

    private var rowIndex = -1
    private lateinit var result: StatementResult

    override fun bindBlob(index: Int, value: ByteArray) {
        throwIfClosed()
        throwIfInvalidParameter(index)
        bindings.setBlob(index - 1, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        throwIfClosed()
        throwIfInvalidParameter(index)
        bindings.setDouble(index - 1, value)
    }

    override fun bindLong(index: Int, value: Long) {
        throwIfClosed()
        throwIfInvalidParameter(index)
        bindings.setLong(index - 1, value)
    }

    override fun bindText(index: Int, value: String) {
        throwIfClosed()
        throwIfInvalidParameter(index)
        bindings.setText(index - 1, value)
    }

    override fun bindNull(index: Int) {
        throwIfClosed()
        throwIfInvalidParameter(index)
        bindings.setNull(index - 1)
    }

    override fun getBlob(index: Int): ByteArray {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return result.getByteArray(rowIndex, index)
    }

    override fun getDouble(index: Int): Double {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return result.getDouble(rowIndex, index)
    }

    override fun getLong(index: Int): Long {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return result.getLong(rowIndex, index)
    }

    override fun getText(index: Int): String {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return result.getString(rowIndex, index)
    }

    override fun isNull(index: Int): Boolean {
        throwIfClosed()
        throwIfInvalidColumn(index)
        return getColumnType(index) == SQLITE_DATA_NULL
    }

    override fun getColumnCount(): Int {
        throwIfClosed()
        return columnNames.size
    }

    override fun getColumnName(index: Int): String {
        throwIfClosed()
        throwIfInvalidColumn(index)
        return columnNames[index]
    }

    override fun getColumnType(index: Int): Int {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return result.columnTypes[index]
    }

    override suspend fun stepAsync(): Boolean {
        throwIfClosed()
        if (rowIndex == -1) {
            this.result = dbWorker.step(statementId, bindings)
            // If this is a transaction statement, set the connection's inTransaction state.
            if (transactionOperation != null) {
                checkNotNull(inTransactionSetter)
                when (transactionOperation) {
                    TransactionOperation.END,
                    TransactionOperation.ROLLBACK -> inTransactionSetter.invoke(false)
                    TransactionOperation.BEGIN_EXCLUSIVE,
                    TransactionOperation.BEGIN_IMMEDIATE,
                    TransactionOperation.BEGIN_DEFERRED -> inTransactionSetter.invoke(true)
                }
            }
        }
        rowIndex++
        return rowIndex < result.size
    }

    override fun reset() {
        throwIfClosed()
        rowIndex = -1
    }

    override fun clearBindings() {
        throwIfClosed()
        bindings.clear()
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            dbWorker.closeStatement(statementId)
        }
    }

    private fun throwIfNoRow() {
        if (rowIndex == -1) {
            throwSQLiteException(21, "no row")
        }
    }

    private fun throwIfInvalidParameter(index: Int) {
        if (index < 1 || index > parameterCount) {
            throwSQLiteException(25, "column index out of range")
        }
    }

    private fun throwIfInvalidColumn(index: Int) {
        if (index < 0 || index >= columnNames.size) {
            throwSQLiteException(25, "column index out of range")
        }
    }

    private fun throwIfClosed() {
        if (isClosed) {
            throwSQLiteException(21, "statement is closed")
        }
    }
}
