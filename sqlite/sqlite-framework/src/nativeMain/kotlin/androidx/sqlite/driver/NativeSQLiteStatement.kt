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

package androidx.sqlite.driver

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.throwSQLiteException
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlin.concurrent.Volatile
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
import sqlite3.sqlite3_clear_bindings
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
import sqlite3.sqlite3_stmt_busy

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For actual typealias in unbundled
public class NativeSQLiteStatement(
    private val dbPointer: CPointer<sqlite3>,
    private val stmtPointer: CPointer<sqlite3_stmt>
) : SQLiteStatement {

    @OptIn(ExperimentalStdlibApi::class) @Volatile private var isClosed = false

    override fun bindBlob(index: Int, value: ByteArray) {
        throwIfClosed()
        val resultCode =
            sqlite3_bind_blob(stmtPointer, index, value.toCValues(), value.size, SQLITE_TRANSIENT)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindLong(index: Int, value: Long) {
        throwIfClosed()
        val resultCode = sqlite3_bind_int64(stmtPointer, index, value)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindDouble(index: Int, value: Double) {
        throwIfClosed()
        val resultCode = sqlite3_bind_double(stmtPointer, index, value)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindText(index: Int, value: String) {
        throwIfClosed()
        // The third parameter to sqlite3_bind_text16() should be a pointer to well-formed
        // UTF16 text. If a non-negative fourth parameter is provided to sqlite3_bind_text16() then
        // that parameter must be the byte offset where the NUL terminator would occur. Hence due to
        // value.utf16 returning a C string that is zero-terminated, we use 'valueUtf16.size - 1' as
        // the fourth parameter.
        val valueUtf16 = value.utf16
        val resultCode =
            sqlite3_bind_text16(
                stmtPointer,
                index,
                valueUtf16,
                valueUtf16.size - 1,
                SQLITE_TRANSIENT
            )
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun bindNull(index: Int) {
        throwIfClosed()
        val resultCode = sqlite3_bind_null(stmtPointer, index)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun getText(index: Int): String {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        // Kotlin/Native uses UTF-16 character encoding by default and strings returned by
        // sqlite3_column_text16(), even empty strings, are always zero-terminated. Thus we use
        // toKStringFromUtf16() that returns a kotlin.String from a zero-terminated C string.
        val value = sqlite3_column_text16(stmtPointer, index)
        if (value == null) throwIfOutOfMemory()
        return value!!.reinterpret<UShortVar>().toKStringFromUtf16()
    }

    override fun getLong(index: Int): Long {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return sqlite3_column_int64(stmtPointer, index)
    }

    override fun getBlob(index: Int): ByteArray {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        val blob = sqlite3_column_blob(stmtPointer, index)
        if (blob == null) throwIfOutOfMemory()
        val size = sqlite3_column_bytes(stmtPointer, index)
        if (size == 0) throwIfOutOfMemory()
        return if (blob != null && size > 0) {
            blob.readBytes(size)
        } else {
            ByteArray(0)
        }
    }

    override fun getDouble(index: Int): Double {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return sqlite3_column_double(stmtPointer, index)
    }

    override fun isNull(index: Int): Boolean {
        throwIfClosed()
        return getColumnType(index) == SQLITE_NULL
    }

    override fun getColumnType(index: Int): Int {
        throwIfClosed()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return sqlite3_column_type(stmtPointer, index)
    }

    override fun getColumnCount(): Int {
        throwIfClosed()
        return sqlite3_column_count(stmtPointer)
    }

    override fun getColumnName(index: Int): String {
        throwIfClosed()
        throwIfInvalidColumn(index)
        val namePointer = sqlite3_column_name16(stmtPointer, index) ?: throw OutOfMemoryError()
        return namePointer.reinterpret<UShortVar>().toKStringFromUtf16()
    }

    override fun step(): Boolean {
        throwIfClosed()
        val resultCode = sqlite3_step(stmtPointer)
        return when (resultCode) {
            SQLITE_ROW -> true
            SQLITE_DONE -> false
            else -> throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun reset() {
        throwIfClosed()
        val resultCode = sqlite3_reset(stmtPointer)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun clearBindings() {
        throwIfClosed()
        val resultCode = sqlite3_clear_bindings(stmtPointer)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
    }

    override fun close() {
        if (!isClosed) {
            sqlite3_finalize(stmtPointer)
        }
        isClosed = true
    }

    private fun throwIfClosed() {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "statement is closed")
        }
    }

    private fun throwIfNoRow() {
        if (sqlite3_stmt_busy(stmtPointer) == 0) {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }
    }

    private fun throwIfInvalidColumn(index: Int) {
        if (index < 0 || index >= getColumnCount()) {
            throwSQLiteException(SQLITE_RANGE, "column index out of range")
        }
    }

    private fun throwIfOutOfMemory() {
        val lastResultCode = sqlite3_errcode(dbPointer)
        if (lastResultCode == SQLITE_NOMEM) {
            throw OutOfMemoryError()
        }
    }
}
