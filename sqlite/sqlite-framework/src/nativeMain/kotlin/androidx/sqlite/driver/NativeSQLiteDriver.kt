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
import sqlite3.SQLITE_DBCONFIG_ENABLE_LOAD_EXTENSION
import sqlite3.SQLITE_ERROR
import sqlite3.SQLITE_OK
import sqlite3.SQLITE_OPEN_CREATE
import sqlite3.SQLITE_OPEN_READWRITE
import sqlite3.sqlite3_compileoption_used
import sqlite3.sqlite3_db_config
import sqlite3.sqlite3_extended_result_codes
import sqlite3.sqlite3_open_v2
import sqlite3.sqlite3_threadsafe

/**
 * A [SQLiteDriver] that uses a version of SQLite included with the host operating system.
 *
 * Usage of this driver expects that `libsqlite` can be found in the shared library path.
 *
 * The host's SQLite used by this driver might be compiled with different
 * [threading modes](https://www.sqlite.org/threadsafe.html) which can be checked with
 * [threadingMode]. Regardless of the mode compiled, this driver does not have an internal
 * connection pool and whether the driver connections are thread-safe or not will be determined by
 * the compiled threading mode of the host library.
 */
public class NativeSQLiteDriver : SQLiteDriver {
    private val extensions = mutableMapOf<String, String?>()

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
        var resultCode =
            sqlite3_open_v2(filename = fileName, ppDb = dbPointer.ptr, flags = flags, zVfs = null)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, null)
        }

        // Enable extended error codes
        resultCode = sqlite3_extended_result_codes(dbPointer.value!!, 1)
        if (resultCode != SQLITE_OK) {
            throwSQLiteException(resultCode, null)
        }

        // Enable the C function to load extensions but not the load_extension() SQL function.
        if (extensions.isNotEmpty()) {
            if (sqlite3_compileoption_used("OMIT_LOAD_EXTENSION") == 1) {
                throwSQLiteException(
                    SQLITE_ERROR,
                    "Found extensions but sqlite3_load_extension() is omitted.",
                )
            }
            resultCode =
                sqlite3_db_config(dbPointer.value!!, SQLITE_DBCONFIG_ENABLE_LOAD_EXTENSION, 1, 0)
            if (resultCode != SQLITE_OK) {
                throwSQLiteException(resultCode, null)
            }
        }

        val connection = NativeSQLiteConnection(dbPointer.value!!)
        try {
            extensions.forEach { (file, entrypoint) -> connection.loadExtension(file, entrypoint) }
        } catch (th: Throwable) {
            connection.close()
            throw th
        }
        return connection
    }

    /**
     * Registers a dynamically-linked SQLite extension to load for every subsequent connection
     * opened with this driver.
     *
     * The extension is loaded by SQLite in a platform-specific way. SQLite will attempt to open the
     * file using (e.g. dlopen on POSIX) and look up a native function responsible for initializing
     * the extension. SQLite will derive the entry function from the file name.
     *
     * It is the developer's responsibility to ensure that the library is actually available with
     * the app. If the file is not available when a connection from this driver is opened, then an
     * [androidx.sqlite.SQLiteException] will be thrown during [open].
     *
     * See also: [Load an extension](https://www.sqlite.org/c3ref/load_extension.html)
     *
     * @param fileName The path to the extension to load. A given file can only be added as an
     *   extension once.
     */
    public fun addExtension(fileName: String) {
        check(fileName !in extensions) { "Extension '$fileName' is already added." }
        extensions[fileName] = null
    }

    /**
     * Registers a dynamically-linked SQLite extension to load for every subsequent connection
     * opened with this driver.
     *
     * The extension is loaded by SQLite in a platform-specific way. SQLite will attempt to open the
     * file using (e.g. dlopen on POSIX) and look up a native function responsible for initializing
     * the extension. The [entryPoint] defines the function name to be invoke to initialize the
     * extension.
     *
     * It is the developer's responsibility to ensure that the library is actually available with
     * the app. If the file is not available when a connection from this driver is opened, then an
     * [androidx.sqlite.SQLiteException] will be thrown during [open].
     *
     * See also: [Load an extension](https://www.sqlite.org/c3ref/load_extension.html)
     *
     * @param fileName The path to the extension to load. A given file can only be added as an
     *   extension once.
     * @param entryPoint The function name to serve as entry point in the loaded extension library.
     */
    public fun addExtension(fileName: String, entryPoint: String) {
        check(fileName !in extensions) { "Extension '$fileName' is already added." }
        extensions[fileName] = entryPoint
    }
}
