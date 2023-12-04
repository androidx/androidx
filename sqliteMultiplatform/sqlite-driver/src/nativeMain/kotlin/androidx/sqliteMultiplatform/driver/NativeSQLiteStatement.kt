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

package androidx.sqliteMultiplatform.driver

import androidx.annotation.RestrictTo
import androidx.sqliteMultiplatform.SQLiteStatement
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.utf16
import sqlite3.SQLITE_DONE
import sqlite3.SQLITE_MISUSE
import sqlite3.SQLITE_NOMEM
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_RANGE
import sqlite3.SQLITE_ROW
import sqlite3.SQLITE_TRANSIENT
import sqlite3.sqlite3_bind_blob
import sqlite3.sqlite3_bind_double
import sqlite3.sqlite3_bind_int64
import sqlite3.sqlite3_bind_null
import sqlite3.sqlite3_bind_text16
import sqlite3.sqlite3_column_blob
import sqlite3.sqlite3_column_bytes
import sqlite3.sqlite3_column_count
import sqlite3.sqlite3_column_double
import sqlite3.sqlite3_column_int64
import sqlite3.sqlite3_column_name16
import sqlite3.sqlite3_column_text16
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_errcode
import sqlite3.sqlite3_finalize
import sqlite3.sqlite3_reset
import sqlite3.sqlite3_step

/**
 * TODO:
 *  * (b/304297717) step non-OK code handling
 *  * (b/307917224) index out of bounds handling
 *  * (b/304295573) busy / locked handling
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For actual typealias in unbundled
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class NativeSQLiteStatement(
    private val dbPointer: CPointer<sqlite3>,
    private val stmtPointer: CPointer<sqlite3_stmt>
) : SQLiteStatement {
    override fun bindBlob(index: Int, value: ByteArray) {
        val resultCode = sqlite3_bind_blob(
            stmtPointer,
            index,
            value.toCValues(),
            value.size,
            SQLITE_TRANSIENT
        )
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindLong(index: Int, value: Long) {
        val resultCode = sqlite3_bind_int64(stmtPointer, index, value)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindDouble(index: Int, value: Double) {
        val resultCode = sqlite3_bind_double(stmtPointer, index, value)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindText(index: Int, value: String) {
        val valueUtf16 = value.utf16
        val resultCode = sqlite3_bind_text16(
            stmtPointer, index, valueUtf16, valueUtf16.size, SQLITE_TRANSIENT
        )
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindNull(index: Int) {
        val resultCode = sqlite3_bind_null(stmtPointer, index)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun getText(index: Int): String {
        throwIfNoRow()
        throwIfInvalidColumn(index)
        // Kotlin/Native uses UTF-16 character encoding by default.
        val value = sqlite3_column_text16(stmtPointer, index)
        if (sqlite3_errcode(dbPointer) == SQLITE_NOMEM) {
            throw OutOfMemoryError()
        }
        return value!!.reinterpret<UShortVar>().toKStringFromUtf16()
    }

    override fun getLong(index: Int): Long {
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return sqlite3_column_int64(stmtPointer, index)
    }

    override fun getBlob(index: Int): ByteArray {
        throwIfNoRow()
        throwIfInvalidColumn(index)
        val blob = sqlite3_column_blob(stmtPointer, index)
        if (sqlite3_errcode(dbPointer) == SQLITE_NOMEM) {
            throw OutOfMemoryError()
        }
        val size = sqlite3_column_bytes(stmtPointer, index)
        return blob!!.readBytes(size)
    }

    override fun getDouble(index: Int): Double {
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return sqlite3_column_double(stmtPointer, index)
    }

    override fun isNull(index: Int): Boolean {
        return getColumnType(index) == SQLITE_NULL
    }

    private fun getColumnType(index: Int): Int {
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return sqlite3_column_type(stmtPointer, index)
    }

    override fun getColumnCount(): Int {
        return sqlite3_column_count(stmtPointer)
    }

    override fun getColumnName(index: Int): String {
        throwIfInvalidColumn(index)
        val namePointer = sqlite3_column_name16(stmtPointer, index) ?: throw OutOfMemoryError()
        return namePointer.reinterpret<UShortVar>().toKStringFromUtf16()
    }

    override fun step(): Boolean {
        val resultCode = sqlite3_step(stmtPointer)
        return when (resultCode) {
            SQLITE_ROW -> true
            SQLITE_DONE -> false
            else -> throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun reset() {
        val resultCode = sqlite3_reset(stmtPointer)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun close() {
        sqlite3_finalize(stmtPointer)
    }

    private fun throwIfNoRow() {
        val lastResultCode = sqlite3_errcode(dbPointer)
        if (lastResultCode != SQLITE_ROW) {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }
    }

    private fun throwIfInvalidColumn(index: Int) {
        if (index < 0 || index >= getColumnCount()) {
            throwSQLiteException(SQLITE_RANGE, "column index out of range")
        }
    }
}
