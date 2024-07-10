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
    val index: Int = stmt.columnIndexOf(name)
    if (index >= 0) {
        return index
    }
    val availableColumns = List(stmt.getColumnCount()) { stmt.getColumnName(it) }.joinToString()
    throw IllegalArgumentException(
        "Column '$name' does not exist. Available columns: [$availableColumns]"
    )
}

/** Returns the zero-based index for the given column name, or -1 if the column doesn't exist. */
internal expect fun SQLiteStatement.columnIndexOf(name: String): Int

// TODO(b/322183292): Consider optimizing by creating a String->Int map, similar to Android
internal fun SQLiteStatement.columnIndexOfCommon(name: String): Int {
    if (this is MappedColumnsSQLiteStatementWrapper) {
        return getColumnIndex(name)
    }
    val columnCount = getColumnCount()
    for (i in 0 until columnCount) {
        if (name == getColumnName(i)) return i
    }
    return -1
}

/** Returns the zero-based index for the given column name, or -1 if the column doesn't exist. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun getColumnIndex(stmt: SQLiteStatement, name: String): Int {
    return stmt.columnIndexOf(name)
}

/**
 * Wraps the given statement such that `getColumnIndex()` will utilize the provided `mapping` when
 * getting the index of a column in `columnNames`.
 *
 * This is useful when the original statement contains duplicate columns. Instead of letting the
 * statement return the first matching column with a name, we can resolve the ambiguous column
 * indices and wrap the statement such that for a set of desired column indices, the returned value
 * will be that from the pre-computation.
 *
 * @param statement the statement to wrap.
 * @param columnNames the column names whose index are known. The result column index of the column
 *   name at i will be at `mapping[i]`.
 * @param mapping the cursor column indices of the columns at `columnNames`.
 * @return the wrapped Cursor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun wrapMappedColumns(
    statement: SQLiteStatement,
    columnNames: Array<String>,
    mapping: IntArray
): SQLiteStatement {
    return MappedColumnsSQLiteStatementWrapper(statement, columnNames, mapping)
}

internal class MappedColumnsSQLiteStatementWrapper(
    private val delegate: SQLiteStatement,
    private val columnNames: Array<String>,
    private val mapping: IntArray
) : SQLiteStatement by delegate {

    init {
        require(columnNames.size == mapping.size) { "Expected columnNames.size == mapping.size" }
    }

    private val columnNameToIndexMap = buildMap {
        columnNames.forEachIndexed { i, mappedColumnName -> put(mappedColumnName, mapping[i]) }
        for (i in 0 until getColumnCount()) {
            val name = getColumnName(i)
            if (!containsKey(name)) {
                put(getColumnName(i), i)
            }
        }
    }

    fun getColumnIndex(name: String): Int {
        return columnNameToIndexMap[name] ?: -1
    }
}
