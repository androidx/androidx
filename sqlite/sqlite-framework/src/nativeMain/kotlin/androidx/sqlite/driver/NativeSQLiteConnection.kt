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
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.throwSQLiteException
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlin.concurrent.Volatile
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.utf16
import kotlinx.cinterop.value
import sqlite3.SQLITE_MISUSE
import sqlite3.SQLITE_OK
import sqlite3.sqlite3_close_v2
import sqlite3.sqlite3_free
import sqlite3.sqlite3_get_autocommit
import sqlite3.sqlite3_prepare16_v2

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For actual typealias in unbundled
public class NativeSQLiteConnection(private val dbPointer: CPointer<sqlite3>) : SQLiteConnection {

    @Volatile private var isClosed = false

    override fun inTransaction(): Boolean {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
        return sqlite3_get_autocommit(dbPointer) == 0
    }

    override fun prepare(sql: String): SQLiteStatement = memScoped {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
        val stmtPointer = allocPointerTo<sqlite3_stmt>()
        // Kotlin/Native uses UTF-16 character encoding by default.
        val sqlUtf16 = sql.utf16
        val resultCode =
            sqlite3_prepare16_v2(
                db = dbPointer,
                zSql = sqlUtf16,
                nByte = sqlUtf16.size,
                ppStmt = stmtPointer.ptr,
                pzTail = null,
            )
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, dbPointer.getErrorMsg())
        }
        NativeSQLiteStatement(dbPointer, stmtPointer.value!!)
    }

    internal fun loadExtension(fileName: String, entryPoint: String?): Unit = memScoped {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
        val errorMessagePointer = alloc<CPointerVar<ByteVar>>()
        val resultCode =
            tryLoadExtension(
                db = dbPointer,
                zFile = fileName.cstr.ptr,
                zProc = entryPoint?.cstr?.ptr,
                pzErrMsg = errorMessagePointer.ptr,
            )

        if (resultCode != SQLITE_OK) {
            val errorMessage = errorMessagePointer.value?.toKStringFromUtf8()
            if (errorMessage != null) {
                sqlite3_free(errorMessagePointer.value)
            }

            throwSQLiteException(resultCode, errorMessage)
        }
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            sqlite3_close_v2(dbPointer)
        }
    }
}
