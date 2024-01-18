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

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.throwSQLiteException
import cnames.structs.sqlite3
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_OPEN_CREATE
import sqlite3.SQLITE_OPEN_READWRITE
import sqlite3.sqlite3_open_v2

/**
 * A [SQLiteDriver] that uses a version of SQLite included with the host operating system.
 *
 * Usage of this driver expects that `libsqlite` can be found in the shared library path.
 */
// TODO:
//    (b/307917398) more open flags
//    (b/304295573) busy handler registering
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class NativeSQLiteDriver(
    private val filename: String
) : SQLiteDriver {
    override fun open(): SQLiteConnection = memScoped {
        val dbPointer = allocPointerTo<sqlite3>()
        val resultCode = sqlite3_open_v2(
            filename = filename,
            ppDb = dbPointer.ptr,
            flags = SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE,
            zVfs = null
        )
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, null)
        }
        NativeSQLiteConnection(dbPointer.value!!)
    }
}
