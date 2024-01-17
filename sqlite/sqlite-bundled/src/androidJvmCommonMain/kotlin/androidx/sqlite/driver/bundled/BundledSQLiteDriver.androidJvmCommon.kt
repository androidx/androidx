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

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver

/**
 * A [SQLiteDriver] that uses a bundled version of SQLite included as a native component of the
 * library.
 */
// TODO(b/313895287): Explore usability of @FastNative and @CriticalNative for the external functions.
actual class BundledSQLiteDriver actual constructor(
    private val filename: String
) : SQLiteDriver {
    override fun open(): SQLiteConnection {
        val address = nativeOpen(filename)
        return BundledSQLiteConnection(address)
    }

    private companion object {
        init {
            NativeLibraryLoader.loadLibrary("jvmArtJniImplementation")
        }
    }
}

private external fun nativeOpen(name: String): Long
