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
@file:JvmName("UnbundledSQLiteDriverKt")

package androidx.sqliteMultiplatform.unbundled

import androidx.sqliteMultiplatform.SQLiteConnection
import androidx.sqliteMultiplatform.SQLiteDriver

// TODO(b/313895287): Explore usability of @FastNative and @CriticalNative for the external functions.
actual class UnbundledSQLiteDriver actual constructor(
    private val filename: String
) : SQLiteDriver {
    override fun open(): SQLiteConnection {
        val address = nativeOpen(filename)
        return UnbundledSQLiteConnection(address)
    }

    companion object {
        init {
            NativeLibraryLoader.loadLibrary("jvmArtJniImplementation")
        }
    }
}

private external fun nativeOpen(name: String): Long
