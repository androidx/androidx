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

package androidx.sqlite.driver.bundled

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver

/**
 * A [SQLiteDriver] that uses a bundled version of SQLite included as a native component of this
 * library.
 *
 * The bundled SQLite used by this driver is compiled in
 * [multi-thread mode](https://www.sqlite.org/threadsafe.html) which means connections opened by the
 * driver are NOT thread-safe. If multiple connections are desired, then a connection pool is
 * required in order for the connections be used in a multi-thread and concurrent environment. If
 * only a single connection is needed then a thread-safe connection can be opened by using the
 * [SQLITE_OPEN_FULLMUTEX] flag. If the connection usage is exclusively single threaded, then no
 * additional configuration is required.
 */
public expect class BundledSQLiteDriver() : SQLiteDriver {

    /**
     * The thread safe mode SQLite was compiled with.
     *
     * See also [SQLite In Multi-Threaded Applications](https://www.sqlite.org/threadsafe.html)
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val threadingMode: Int

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
    public fun addExtension(fileName: String)

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
    public fun addExtension(fileName: String, entryPoint: String)

    override fun open(fileName: String): SQLiteConnection

    /**
     * Opens a new database connection.
     *
     * See also [Opening A New Database Connection](https://www.sqlite.org/c3ref/open.html)
     *
     * @param fileName Name of the database file.
     * @param flags Connection open flags.
     * @return the database connection.
     */
    public fun open(fileName: String, @OpenFlag flags: Int): SQLiteConnection
}
