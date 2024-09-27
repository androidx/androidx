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
@file:Suppress("AcronymName") // SQL is a known term and should remain capitalized
@file:JvmName("SQLite")

package androidx.sqlite

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.jvm.JvmName

/** The data type for a 64-bit signed integer. */
public const val SQLITE_DATA_INTEGER: Int = 1

/** The data type for a 64-bit IEEE floating point number. */
public const val SQLITE_DATA_FLOAT: Int = 2

/** The data type for a [String]. */
public const val SQLITE_DATA_TEXT: Int = 3

/** The data type for a `BLOB` value, i.e. binary data. */
public const val SQLITE_DATA_BLOB: Int = 4

/** The data type for a `NULL` value. */
public const val SQLITE_DATA_NULL: Int = 5

/** The data type constants. */
@IntDef(
    value =
        [
            SQLITE_DATA_INTEGER,
            SQLITE_DATA_FLOAT,
            SQLITE_DATA_TEXT,
            SQLITE_DATA_BLOB,
            SQLITE_DATA_NULL,
        ]
)
@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class DataType

/** Executes a single SQL statement that returns no values. */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public fun SQLiteConnection.execSQL(sql: String) {
    prepare(sql).use { it.step() }
}

/** Use the receiver statement within the [block] and closes it once it is done. */
// TODO(b/315461431): Migrate to a Closeable interface in KMP
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public inline fun <R> SQLiteStatement.use(block: (SQLiteStatement) -> R): R {
    try {
        return block.invoke(this)
    } finally {
        close()
    }
}

/** Throws a [SQLiteException] with its message formed by the given [errorCode] amd [errorMsg]. */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public fun throwSQLiteException(errorCode: Int, errorMsg: String?): Nothing {
    val message = buildString {
        append("Error code: $errorCode")
        if (errorMsg != null) {
            append(", message: $errorMsg")
        }
    }
    throw SQLiteException(message)
}
