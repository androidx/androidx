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
import androidx.sqlite.driver.bundled.jni.FastNative
import androidx.sqlite.throwSQLiteException
import kotlin.concurrent.Volatile

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public actual class BundledSQLiteConnection(private val connectionPointer: Long) :
    SQLiteConnection {

    @Volatile private var isClosed = false

    actual override fun inTransaction(): Boolean {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
        return nativeInTransaction(connectionPointer)
    }

    actual override fun prepare(sql: String): SQLiteStatement {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
        val statementPointer = nativePrepare(connectionPointer, sql)
        return BundledSQLiteStatement(connectionPointer, statementPointer)
    }

    internal fun loadExtension(fileName: String, entryPoint: String?) {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }

        nativeLoadExtension(connectionPointer, fileName, entryPoint)
    }

    actual override fun close() {
        if (!isClosed) {
            isClosed = true
            nativeClose(connectionPointer)
        }
    }
}

@FastNative private external fun nativeInTransaction(pointer: Long): Boolean

private external fun nativePrepare(pointer: Long, sql: String): Long

private external fun nativeLoadExtension(pointer: Long, fileName: String, entryPoint: String?)

@FastNative private external fun nativeClose(pointer: Long)
