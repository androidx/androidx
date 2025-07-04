/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room.support

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteStatement

internal class RoomSupportSQLiteStatement(
    private val session: RoomSupportSQLiteSession,
    private val sql: String,
) : SupportSQLiteStatement {

    private var isClosed = false

    private var bindingTypes: IntArray = IntArray(1)
    private var longBindings: LongArray = LongArray(1)
    private var doubleBindings: DoubleArray = DoubleArray(1)
    private var stringBindings: Array<String?> = arrayOfNulls(1)
    private var blobBindings: Array<ByteArray?> = arrayOfNulls(1)

    override fun execute() {
        throwIfClosed()
        session.useWriterBlocking { connection ->
            connection.usePrepared(sql) { stmt ->
                stmt.bindArgs()
                stmt.step()
            }
        }
    }

    override fun executeUpdateDelete(): Int {
        throwIfClosed()
        return session.useWriterBlocking { connection ->
            connection.usePrepared(sql) { stmt ->
                stmt.bindArgs()
                stmt.step()
            }
            connection.getTotalChangedRows()
        }
    }

    override fun executeInsert(): Long {
        throwIfClosed()
        return session.useWriterBlocking { connection ->
            connection.usePrepared(sql) { stmt ->
                stmt.bindArgs()
                stmt.step()
            }
            connection.getLastInsertedRowId()
        }
    }

    override fun simpleQueryForLong(): Long {
        throwIfClosed()
        return session.useWriterBlocking { connection ->
            connection.usePrepared(sql) { stmt ->
                stmt.bindArgs()
                stmt.step()
                stmt.getLong(0)
            }
        }
    }

    override fun simpleQueryForString(): String? {
        throwIfClosed()
        return session.useWriterBlocking { connection ->
            connection.usePrepared(sql) { stmt ->
                stmt.bindArgs()
                stmt.step()
                if (stmt.isNull(0)) {
                    null
                } else {
                    stmt.getText(0)
                }
            }
        }
    }

    override fun bindNull(index: Int) {
        throwIfClosed()
        ensureCapacity(SQLITE_DATA_NULL, index)
        bindingTypes[index] = SQLITE_DATA_NULL
    }

    override fun bindLong(index: Int, value: Long) {
        throwIfClosed()
        ensureCapacity(SQLITE_DATA_INTEGER, index)
        bindingTypes[index] = SQLITE_DATA_INTEGER
        longBindings[index] = value
    }

    override fun bindDouble(index: Int, value: Double) {
        throwIfClosed()
        ensureCapacity(SQLITE_DATA_FLOAT, index)
        bindingTypes[index] = SQLITE_DATA_FLOAT
        doubleBindings[index] = value
    }

    override fun bindString(index: Int, value: String) {
        throwIfClosed()
        ensureCapacity(SQLITE_DATA_TEXT, index)
        bindingTypes[index] = SQLITE_DATA_TEXT
        stringBindings[index] = value
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        throwIfClosed()
        ensureCapacity(SQLITE_DATA_BLOB, index)
        bindingTypes[index] = SQLITE_DATA_BLOB
        blobBindings[index] = value
    }

    override fun clearBindings() {
        throwIfClosed()
        bindingTypes = IntArray(1)
        longBindings = LongArray(1)
        doubleBindings = DoubleArray(1)
        stringBindings = arrayOfNulls(1)
        blobBindings = arrayOfNulls(1)
    }

    override fun close() {
        if (!isClosed) {
            clearBindings()
        }
        isClosed = true
    }

    private fun ensureCapacity(columnType: Int, index: Int) {
        val requiredSize = index + 1
        if (bindingTypes.size < requiredSize) {
            bindingTypes = bindingTypes.copyOf(requiredSize)
        }
        when (columnType) {
            SQLITE_DATA_INTEGER -> {
                if (longBindings.size < requiredSize) {
                    longBindings = longBindings.copyOf(requiredSize)
                }
            }
            SQLITE_DATA_FLOAT -> {
                if (doubleBindings.size < requiredSize) {
                    doubleBindings = doubleBindings.copyOf(requiredSize)
                }
            }
            SQLITE_DATA_TEXT -> {
                if (stringBindings.size < requiredSize) {
                    stringBindings = stringBindings.copyOf(requiredSize)
                }
            }
            SQLITE_DATA_BLOB -> {
                if (blobBindings.size < requiredSize) {
                    blobBindings = blobBindings.copyOf(requiredSize)
                }
            }
        }
    }

    private fun SQLiteStatement.bindArgs() {
        for (index in 1 until bindingTypes.size) {
            when (bindingTypes[index]) {
                SQLITE_DATA_NULL -> bindNull(index)
                SQLITE_DATA_INTEGER -> bindLong(index, longBindings[index])
                SQLITE_DATA_FLOAT -> bindDouble(index, doubleBindings[index])
                SQLITE_DATA_TEXT -> bindText(index, requireNotNull(stringBindings[index]))
                SQLITE_DATA_BLOB -> bindBlob(index, requireNotNull(blobBindings[index]))
            }
        }
    }

    private fun throwIfClosed() {
        check(!isClosed) { "Statement is closed" }
    }
}
