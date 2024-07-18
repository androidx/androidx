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
import androidx.sqliteMultiplatform.SQLiteDriver
import cnames.structs.sqlite3
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_OPEN_CREATE
import sqlite3.SQLITE_OPEN_READWRITE
import sqlite3.sqlite3_open_v2

/**
 * TODO:
 *  * more open flags
 *  * busy handler registering
 */
class NativeSQLiteDriver(
    val filename: String
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
            error("Error opening database - $resultCode")
        }
        NativeSQLiteConnection(dbPointer.pointed!!)
    }
}
