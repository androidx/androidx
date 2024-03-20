/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmMultifileClass
@file:JvmName("SQLiteStatementUtil")

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteStatement
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns the zero-based index for the given column name, or throws [IllegalArgumentException] if
 * the column doesn't exist.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun getColumnIndexOrThrow(stmt: SQLiteStatement, name: String): Int {
    val index: Int = stmt.getColumnIndex(name)
    if (index >= 0) {
        return index
    }
    val availableColumns = List(stmt.getColumnCount()) { stmt.getColumnName(it) }.joinToString()
    throw IllegalArgumentException(
        "Column '$name' does not exist. Available columns: [$availableColumns]"
    )
}

/**
 * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
 */
internal expect fun SQLiteStatement.getColumnIndex(name: String): Int

// TODO(b/322183292): Consider optimizing by creating a String->Int map, similar to Android
internal fun SQLiteStatement.columnIndexOf(name: String): Int {
    val columnCount = getColumnCount()
    for (i in 0 until columnCount) {
        if (name == getColumnName(i)) return i
    }
    return -1
}
