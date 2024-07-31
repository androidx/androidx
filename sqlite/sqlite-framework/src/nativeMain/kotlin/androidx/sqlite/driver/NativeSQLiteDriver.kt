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
import sqlite3.sqlite3_threadsafe

/**
 * A [SQLiteDriver] that uses a version of SQLite included with the host operating system.
 *
 * Usage of this driver expects that `libsqlite` can be found in the shared library path.
 */
public class NativeSQLiteDriver : SQLiteDriver {

    /**
     * The thread safe mode SQLite was compiled with.
     *
     * See also [SQLite In Multi-Threaded Applications](https://www.sqlite.org/threadsafe.html)
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val threadingMode: Int
        get() = sqlite3_threadsafe()

    override fun open(fileName: String): SQLiteConnection {
        return open(fileName, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE)
    }

    /**
     * Opens a new database connection.
     *
     * See also [Opening A New Database Connection](https://www.sqlite.org/c3ref/open.html)
     *
     * @param fileName Name of the database file.
     * @param flags Connection open flags.
     * @return the database connection.
     */
    public fun open(fileName: String, @OpenFlag flags: Int): SQLiteConnection = memScoped {
        val dbPointer = allocPointerTo<sqlite3>()
        val resultCode =
            sqlite3_open_v2(filename = fileName, ppDb = dbPointer.ptr, flags = flags, zVfs = null)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, null)
        }
        NativeSQLiteConnection(dbPointer.value!!)
    }
}
