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

import androidx.sqliteMultiplatform.SQLiteConnection
import androidx.sqliteMultiplatform.SQLiteStatement
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.utf8
import sqlite3.SQLITE_OK
import sqlite3.sqlite3_close_v2
import sqlite3.sqlite3_prepare_v2

internal class NativeSQLiteConnection(
    private val dbStruct: sqlite3
) : SQLiteConnection {
    override fun prepare(sql: String): SQLiteStatement = memScoped {
        val stmtPointer = allocPointerTo<sqlite3_stmt>()
        val sqlUtf8 = sql.utf8
        val resultCode = sqlite3_prepare_v2(
            db = dbStruct.ptr,
            zSql = sqlUtf8,
            nByte = sqlUtf8.size,
            ppStmt = stmtPointer.ptr,
            pzTail = null
        )
        if (resultCode != SQLITE_OK) {
            error("Error preparing statement - $resultCode")
        }
        NativeSQLiteStatement(dbStruct, stmtPointer.pointed!!)
    }

    override fun close() {
        sqlite3_close_v2(dbStruct.ptr)
    }
}
