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
@file:JvmName("BundledSQLiteDriverKt")

package androidx.sqlite.driver.bundled

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver

/**
 * A [SQLiteDriver] that uses a bundled version of SQLite included as a native component of the
 * library.
 *
 * The bundled SQLite used by this driver is compiled in
 * [multi-thread mode](https://www.sqlite.org/threadsafe.html) which means connections opened by the
 * driver are NOT thread-safe. If multiple connections are desired, then a connection pool is
 * required in order for the connections be used in a multi-thread and concurrent environment. If
 * only a single connection is needed then a thread-safe connection can be opened by using the
 * [SQLITE_OPEN_FULLMUTEX] flag. If the application is single threaded, then no additional
 * configuration is required.
 */
// TODO(b/313895287): Explore usability of @FastNative and @CriticalNative for the external
// functions.
public actual class BundledSQLiteDriver : SQLiteDriver {
    private val extensions = mutableMapOf<String, String?>()

    @Suppress("INAPPLICABLE_JVM_NAME") // Due to KT-31420
    @get:JvmName("hasConnectionPool")
    override val hasConnectionPool: Boolean
        get() = false

    /**
     * The thread safe mode SQLite was compiled with.
     *
     * See also [SQLite In Multi-Threaded Applications](https://www.sqlite.org/threadsafe.html)
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public actual val threadingMode: Int
        get() = nativeThreadSafeMode()

    actual override fun open(fileName: String): SQLiteConnection {
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
    public actual fun open(fileName: String, @OpenFlag flags: Int): SQLiteConnection {
        NativeLibraryObject // loads native library
        val address = nativeOpen(fileName, flags)
        val connection = BundledSQLiteConnection(address)
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
    public actual fun addExtension(fileName: String) {
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
    public actual fun addExtension(fileName: String, entryPoint: String) {
        check(fileName !in extensions) { "Extension '$fileName' is already added." }
        extensions[fileName] = entryPoint
    }

    private object NativeLibraryObject {
        init {
            NativeLibraryLoader.loadLibrary("sqliteJni")
        }
    }
}

private external fun nativeThreadSafeMode(): Int

private external fun nativeOpen(name: String, openFlags: Int): Long
