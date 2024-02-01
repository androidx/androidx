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
@file:JvmName("BundledSQLiteConnectionKt")

package androidx.sqlite.driver.bundled

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.ResultCode.SQLITE_MISUSE
import androidx.sqlite.throwSQLiteException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual class BundledSQLiteConnection(
    private val connectionPointer: Long
) : SQLiteConnection {

    private var isClosed = false

    override fun prepare(sql: String): SQLiteStatement {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
        val statementPointer = nativePrepare(connectionPointer, sql)
        return BundledSQLiteStatement(connectionPointer, statementPointer)
    }

    override fun close() {
        nativeClose(connectionPointer)
        isClosed = true
    }
}

private external fun nativePrepare(pointer: Long, sql: String): Long
private external fun nativeClose(pointer: Long)
