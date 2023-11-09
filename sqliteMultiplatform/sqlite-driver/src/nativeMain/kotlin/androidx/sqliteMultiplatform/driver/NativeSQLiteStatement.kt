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

import androidx.sqliteMultiplatform.SQLiteStatement
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import sqlite3.SQLITE_NOMEM
import sqlite3.SQLITE_NULL
import sqlite3.SQLITE_ROW
import sqlite3.SQLITE_TRANSIENT
import sqlite3.sqlite3_bind_blob
import sqlite3.sqlite3_bind_double
import sqlite3.sqlite3_bind_int64
import sqlite3.sqlite3_bind_null
import sqlite3.sqlite3_bind_text
import sqlite3.sqlite3_column_blob
import sqlite3.sqlite3_column_bytes
import sqlite3.sqlite3_column_count
import sqlite3.sqlite3_column_double
import sqlite3.sqlite3_column_int64
import sqlite3.sqlite3_column_name
import sqlite3.sqlite3_column_text
import sqlite3.sqlite3_column_type
import sqlite3.sqlite3_errcode
import sqlite3.sqlite3_finalize
import sqlite3.sqlite3_reset
import sqlite3.sqlite3_step

/**
 * TODO:
 *  * step non-OK code handling
 *  * index out of bounds handling
 *  * busy / locked handling
 */
internal class NativeSQLiteStatement(
    private val dbPointer: CPointer<sqlite3>,
    private val stmtPointer: CPointer<sqlite3_stmt>
) : SQLiteStatement {
    override fun bindBlob(index: Int, value: ByteArray) {
        sqlite3_bind_blob(
            stmtPointer,
            index + 1,
            value.toCValues(),
            value.size,
            SQLITE_TRANSIENT
        )
    }

    override fun bindLong(index: Int, value: Long) {
        sqlite3_bind_int64(stmtPointer, index + 1, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        sqlite3_bind_double(stmtPointer, index + 1, value)
    }

    override fun bindText(index: Int, value: String) {
        sqlite3_bind_text(stmtPointer, index + 1, value, value.length, SQLITE_TRANSIENT)
    }

    override fun bindNull(index: Int) {
        sqlite3_bind_null(stmtPointer, index + 1)
    }

    override fun getText(index: Int): String {
        val value = sqlite3_column_text(stmtPointer, index)
        if (sqlite3_errcode(dbPointer) == SQLITE_NOMEM) {
            throw OutOfMemoryError()
        }
        return value?.reinterpret<ByteVar>()?.toKString() ?: ""
    }

    override fun getLong(index: Int): Long {
        return sqlite3_column_int64(stmtPointer, index)
    }

    override fun getBlob(index: Int): ByteArray {
        val blob = sqlite3_column_blob(stmtPointer, index)
        val size = sqlite3_column_bytes(stmtPointer, index)
        if (sqlite3_errcode(dbPointer) == SQLITE_NOMEM) {
            throw OutOfMemoryError()
        }
        return blob?.readBytes(size) ?: ByteArray(0)
    }

    override fun getDouble(index: Int): Double {
        return sqlite3_column_double(stmtPointer, index)
    }

    override fun isNull(index: Int) = sqlite3_column_type(stmtPointer, index) == SQLITE_NULL

    override fun getColumnCount(): Int {
        return sqlite3_column_count(stmtPointer)
    }

    override fun getColumnName(index: Int): String {
        return sqlite3_column_name(stmtPointer, index)?.toKString() ?: throw OutOfMemoryError()
    }

    override fun step(): Boolean {
        val resultCode = sqlite3_step(stmtPointer)
        return resultCode == SQLITE_ROW
    }

    override fun reset() {
        sqlite3_reset(stmtPointer)
    }

    override fun close() {
        sqlite3_finalize(stmtPointer)
    }
}
