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

package androidx.room3.util

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns the zero-based index for the given column name, or throws [IllegalArgumentException] if
 * the column doesn't exist.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun getColumnIndexOrThrow(stmt: SQLiteStatement, name: String): Int {
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun getColumnIndex(stmt: SQLiteStatement, name: String): Int {
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun wrapMappedColumns(
    statement: SQLiteStatement,
    columnNames: Array<String>,
    mapping: IntArray,
): SQLiteStatement {
    return MappedColumnsSQLiteStatementWrapper(statement, columnNames, mapping)
}

internal class MappedColumnsSQLiteStatementWrapper(
    private val delegate: SQLiteStatement,
    private val columnNames: Array<String>,
    private val mapping: IntArray,
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

/**
 * Wraps the given statement in an in-memory buffer such that the statement can be stepped and reset
 * multiple times without re-executing the underlying SQL query.
 *
 * @param statement the statement to buffer.
 * @return the buffered statement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public expect fun bufferStatement(statement: SQLiteStatement): SQLiteStatement

internal abstract class BaseBufferedSQLiteStatement(private val delegate: SQLiteStatement) :
    SQLiteStatement by delegate {
    // TODO: b/556739247 - Consider using primitive arrays for performance improvement
    protected val rows = mutableListOf<Array<Any?>>()
    protected var rowIndex = -1
    protected var fullyBuffered = false
    private val columnCount = delegate.getColumnCount()
    private val columnNames = delegate.getColumnNames()

    protected fun saveRow() {
        val row = arrayOfNulls<Any?>(columnCount)
        for (i in 0 until columnCount) {
            row[i] =
                if (delegate.isNull(i)) {
                    null
                } else {
                    when (delegate.getColumnType(i)) {
                        SQLITE_DATA_INTEGER -> delegate.getLong(i)
                        SQLITE_DATA_FLOAT -> delegate.getDouble(i)
                        SQLITE_DATA_TEXT -> delegate.getText(i)
                        SQLITE_DATA_BLOB -> delegate.getBlob(i)
                        else -> null
                    }
                }
        }
        rows.add(row)
    }

    override fun reset() {
        rowIndex = -1
    }

    override fun isNull(index: Int): Boolean = rows[rowIndex][index] == null

    override fun getLong(index: Int): Long {
        val value = rows[rowIndex][index] ?: return 0L
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> error("Cannot convert ${value::class} to Long")
        }
    }

    override fun getInt(index: Int): Int = getLong(index).toInt()

    override fun getDouble(index: Int): Double {
        val value = rows[rowIndex][index] ?: return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> error("Cannot convert ${value::class} to Double")
        }
    }

    override fun getFloat(index: Int): Float = getDouble(index).toFloat()

    override fun getText(index: Int): String {
        val value = rows[rowIndex][index] ?: return ""
        return when (value) {
            is ByteArray -> value.decodeToString()
            else -> value.toString()
        }
    }

    override fun getBlob(index: Int): ByteArray {
        val value = rows[rowIndex][index] ?: return ByteArray(0)
        return when (value) {
            is ByteArray -> value
            is String -> value.encodeToByteArray()
            else -> error("Cannot convert ${value::class} to ByteArray")
        }
    }

    override fun getBoolean(index: Int): Boolean = getLong(index) != 0L

    override fun getColumnCount(): Int = columnCount

    override fun getColumnNames(): List<String> = columnNames

    override fun getColumnName(index: Int): String = columnNames[index]

    override fun getColumnType(index: Int): Int {
        val value = rows[rowIndex][index] ?: return SQLITE_DATA_NULL
        return when (value) {
            is Long,
            is Int,
            is Short,
            is Byte -> SQLITE_DATA_INTEGER
            is Double,
            is Float -> SQLITE_DATA_FLOAT
            is String -> SQLITE_DATA_TEXT
            is ByteArray -> SQLITE_DATA_BLOB
            else -> SQLITE_DATA_NULL
        }
    }

    override fun close() {
        rows.clear()
        delegate.close()
    }
}
