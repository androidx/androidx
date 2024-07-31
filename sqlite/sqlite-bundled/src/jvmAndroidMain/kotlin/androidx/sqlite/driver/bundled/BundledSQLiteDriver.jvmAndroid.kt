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
 */
// TODO(b/313895287): Explore usability of @FastNative and @CriticalNative for the external
// functions.
public actual class BundledSQLiteDriver : SQLiteDriver {

    /**
     * The thread safe mode SQLite was compiled with.
     *
     * See also [SQLite In Multi-Threaded Applications](https://www.sqlite.org/threadsafe.html)
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public actual val threadingMode: Int
        get() = nativeThreadSafeMode()

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
    public actual fun open(fileName: String, @OpenFlag flags: Int): SQLiteConnection {
        val address = nativeOpen(fileName, flags)
        return BundledSQLiteConnection(address)
    }

    private companion object {
        init {
            NativeLibraryLoader.loadLibrary("sqliteJni")
        }
    }
}

private external fun nativeThreadSafeMode(): Int

private external fun nativeOpen(name: String, openFlags: Int): Long
