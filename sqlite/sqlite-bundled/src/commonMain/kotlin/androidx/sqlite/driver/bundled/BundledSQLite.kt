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

@file:JvmName("BundledSQLite")

package androidx.sqlite.driver.bundled

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.jvm.JvmName

/** Opens the database in read-only mode. */
public const val SQLITE_OPEN_READONLY: Int = 0x00000001

/** Opens the database for reading and writing. */
public const val SQLITE_OPEN_READWRITE: Int = 0x00000002

/** Create the database if it does not already exist. */
public const val SQLITE_OPEN_CREATE: Int = 0x00000004

/** Interpret the filename as a URI. */
public const val SQLITE_OPEN_URI: Int = 0x00000040

/** Opens the database as a in-memory database. */
public const val SQLITE_OPEN_MEMORY: Int = 0x00000080

/**
 * The database connection will use the "multi-thread" threading mode.
 *
 * See also [SQLite In Multi-Threaded Applications](https://www.sqlite.org/threadsafe.html)
 */
public const val SQLITE_OPEN_NOMUTEX: Int = 0x00008000

/**
 * The database connection will use the "serialized" threading mode.
 *
 * See also [SQLite In Multi-Threaded Applications](https://www.sqlite.org/threadsafe.html)
 */
public const val SQLITE_OPEN_FULLMUTEX: Int = 0x00010000

/** The filename is not allowed to contain a symbolic link. */
public const val SQLITE_OPEN_NOFOLLOW: Int = 0x01000000

/** The database connection will use extended result codes. */
public const val SQLITE_OPEN_EXRESCODE: Int = 0x02000000

/** The flags constant that can be used with [BundledSQLiteDriver.open]. */
@IntDef(
    flag = true,
    value =
        [
            SQLITE_OPEN_READONLY,
            SQLITE_OPEN_READWRITE,
            SQLITE_OPEN_CREATE,
            SQLITE_OPEN_URI,
            SQLITE_OPEN_MEMORY,
            SQLITE_OPEN_NOMUTEX,
            SQLITE_OPEN_FULLMUTEX,
            SQLITE_OPEN_NOFOLLOW,
            SQLITE_OPEN_EXRESCODE,
        ]
)
@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public expect annotation class OpenFlag()
