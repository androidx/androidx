/*
 * Copyright (C) 2018 The Android Open Source Project
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
@file:JvmName("CursorUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.util

import android.database.Cursor
import android.database.CursorWrapper
import android.database.MatrixCursor
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Copies the given cursor into a in-memory cursor and then closes it.
 *
 *
 * This is useful for iterating over a cursor multiple times without the cost of JNI while
 * reading or IO while filling the window at the expense of memory consumption.
 *
 * @param c the cursor to copy.
 * @return a new cursor containing the same data as the given cursor.
 */
fun copyAndClose(c: Cursor): Cursor = c.useCursor { cursor ->
    val matrixCursor = MatrixCursor(cursor.columnNames, cursor.count)
    while (cursor.moveToNext()) {
        val row = arrayOfNulls<Any>(cursor.columnCount)
        for (i in 0 until c.columnCount) {
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> row[i] = null
                Cursor.FIELD_TYPE_INTEGER -> row[i] = cursor.getLong(i)
                Cursor.FIELD_TYPE_FLOAT -> row[i] = cursor.getDouble(i)
                Cursor.FIELD_TYPE_STRING -> row[i] = cursor.getString(i)
                Cursor.FIELD_TYPE_BLOB -> row[i] = cursor.getBlob(i)
                else -> throw IllegalStateException()
            }
        }
        matrixCursor.addRow(row)
    }
    matrixCursor
}

/**
 * Patches [Cursor.getColumnIndex] to work around issues on older devices.
 * If the column is not found, it retries with the specified name surrounded by backticks.
 *
 * @param c    The cursor.
 * @param name The name of the target column.
 * @return The index of the column, or -1 if not found.
 */
fun getColumnIndex(c: Cursor, name: String): Int {
    var index = c.getColumnIndex(name)
    if (index >= 0) {
        return index
    }
    index = c.getColumnIndex("`$name`")
    return if (index >= 0) {
        index
    } else {
        findColumnIndexBySuffix(c, name)
    }
}

/**
 * Patches [Cursor.getColumnIndexOrThrow] to work around issues on older devices.
 * If the column is not found, it retries with the specified name surrounded by backticks.
 *
 * @param c    The cursor.
 * @param name The name of the target column.
 * @return The index of the column.
 * @throws IllegalArgumentException if the column does not exist.
 */
fun getColumnIndexOrThrow(c: Cursor, name: String): Int {
    val index: Int = getColumnIndex(c, name)
    if (index >= 0) {
        return index
    }
    val availableColumns = try {
        c.columnNames.joinToString()
    } catch (e: Exception) {
        Log.d("RoomCursorUtil", "Cannot collect column names for debug purposes", e)
        "unknown"
    }
    throw IllegalArgumentException(
        "column '$name' does not exist. Available columns: $availableColumns"
    )
}

/**
 * Finds a column by name by appending `.` in front of it and checking by suffix match.
 * Also checks for the version wrapped with `` (backticks).
 * workaround for b/157261134 for API levels 25 and below
 *
 * e.g. "foo" will match "any.foo" and "`any.foo`"
 */
private fun findColumnIndexBySuffix(cursor: Cursor, name: String): Int {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        // we need this workaround only on APIs < 26. So just return not found on newer APIs
        return -1
    }
    if (name.isEmpty()) {
        return -1
    }
    val columnNames = cursor.columnNames
    return findColumnIndexBySuffix(columnNames, name)
}

@VisibleForTesting
fun findColumnIndexBySuffix(columnNames: Array<String>, name: String): Int {
    val dotSuffix = ".$name"
    val backtickSuffix = ".$name`"
    columnNames.forEachIndexed { index, columnName ->
        // do not check if column name is not long enough. 1 char for table name, 1 char for '.'
        if (columnName.length >= name.length + 2) {
            if (columnName.endsWith(dotSuffix)) {
                return index
            } else if (columnName[0] == '`' && columnName.endsWith(backtickSuffix)) {
                return index
            }
        }
    }
    return -1
}

/**
 * Backwards compatible function that executes the given block function on this Cursor and then
 * closes the Cursor.
 */
inline fun <R> Cursor.useCursor(block: (Cursor) -> R): R {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
        return this.use(block)
    } else {
        try {
            return block(this)
        } finally {
            this.close()
        }
    }
}

/**
 * Wraps the given cursor such that `getColumnIndex()` will utilize the provided
 * `mapping` when getting the index of a column in `columnNames`.
 *
 * This is useful when the original cursor contains duplicate columns. Instead of letting the
 * cursor return the first matching column with a name, we can resolve the ambiguous column
 * indices and wrap the cursor such that for a set of desired column indices, the returned
 * value will be that from the pre-computation.
 *
 * @param cursor the cursor to wrap.
 * @param columnNames the column names whose index are known. The result column index of the
 * column name at i will be at `mapping[i]`.
 * @param mapping the cursor column indices of the columns at `columnNames`.
 * @return the wrapped Cursor.
 */
fun wrapMappedColumns(cursor: Cursor, columnNames: Array<String>, mapping: IntArray): Cursor {
    check(columnNames.size == mapping.size) { "Expected columnNames.length == mapping.length" }
    return object : CursorWrapper(cursor) {
        override fun getColumnIndex(columnName: String): Int {
            columnNames.forEachIndexed { i, mappedColumnName ->
                if (mappedColumnName.equals(columnName, ignoreCase = true)) {
                    return mapping[i]
                }
            }
            return super.getColumnIndex(columnName)
        }
    }
}