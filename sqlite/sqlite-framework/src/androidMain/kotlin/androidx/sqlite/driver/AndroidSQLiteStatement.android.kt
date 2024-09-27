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

package androidx.sqlite.driver

import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_BLOB
import android.database.Cursor.FIELD_TYPE_FLOAT
import android.database.Cursor.FIELD_TYPE_INTEGER
import android.database.Cursor.FIELD_TYPE_NULL
import android.database.Cursor.FIELD_TYPE_STRING
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteProgram
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.ResultCode.SQLITE_MISUSE
import androidx.sqlite.driver.ResultCode.SQLITE_RANGE
import androidx.sqlite.throwSQLiteException

private typealias FrameworkStatement = android.database.sqlite.SQLiteStatement

internal sealed class AndroidSQLiteStatement(
    protected val db: SQLiteDatabase,
    protected val sql: String,
) : SQLiteStatement {

    protected var isClosed = false

    protected fun throwIfClosed() {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "statement is closed")
        }
    }

    companion object {
        fun create(db: SQLiteDatabase, sql: String): AndroidSQLiteStatement {
            return if (isRowStatement(sql)) {
                // Statements that return rows (SQLITE_ROW)
                SelectAndroidSQLiteStatement(db, sql)
            } else {
                // Statements that don't return row (SQLITE_DONE)
                OtherAndroidSQLiteStatement(db, sql)
            }
        }

        private fun isRowStatement(sql: String): Boolean {
            val prefix = sql.trim()
            if (prefix.length < 3) {
                return false
            }
            return when (prefix.substring(0, 3).uppercase()) {
                "SEL",
                "PRA",
                "WIT" -> true
                else -> false
            }
        }
    }

    // TODO(b/304298743): Use android.database.SQLiteRawStatement on Android V+
    private class SelectAndroidSQLiteStatement(db: SQLiteDatabase, sql: String) :
        AndroidSQLiteStatement(db, sql) {

        private var bindingTypes: IntArray = IntArray(0)
        private var longBindings: LongArray = LongArray(0)
        private var doubleBindings: DoubleArray = DoubleArray(0)
        private var stringBindings: Array<String?> = emptyArray()
        private var blobBindings: Array<ByteArray?> = emptyArray()

        // TODO(b/307918516): Synchronize
        private var cursor: Cursor? = null

        override fun bindBlob(index: Int, value: ByteArray) {
            throwIfClosed()
            ensureCapacity(SQLITE_DATA_BLOB, index)
            bindingTypes[index] = SQLITE_DATA_BLOB
            blobBindings[index] = value
        }

        override fun bindDouble(index: Int, value: Double) {
            throwIfClosed()
            ensureCapacity(SQLITE_DATA_FLOAT, index)
            bindingTypes[index] = SQLITE_DATA_FLOAT
            doubleBindings[index] = value
        }

        override fun bindLong(index: Int, value: Long) {
            throwIfClosed()
            ensureCapacity(SQLITE_DATA_INTEGER, index)
            bindingTypes[index] = SQLITE_DATA_INTEGER
            longBindings[index] = value
        }

        override fun bindText(index: Int, value: String) {
            throwIfClosed()
            ensureCapacity(SQLITE_DATA_TEXT, index)
            bindingTypes[index] = SQLITE_DATA_TEXT
            stringBindings[index] = value
        }

        override fun bindNull(index: Int) {
            throwIfClosed()
            ensureCapacity(SQLITE_DATA_NULL, index)
            bindingTypes[index] = SQLITE_DATA_NULL
        }

        override fun getBlob(index: Int): ByteArray {
            throwIfClosed()
            val c = throwIfNoRow()
            throwIfInvalidColumn(c, index)
            return c.getBlob(index)
        }

        override fun getDouble(index: Int): Double {
            throwIfClosed()
            val c = throwIfNoRow()
            throwIfInvalidColumn(c, index)
            return c.getDouble(index)
        }

        override fun getLong(index: Int): Long {
            throwIfClosed()
            val c = throwIfNoRow()
            throwIfInvalidColumn(c, index)
            return c.getLong(index)
        }

        override fun getText(index: Int): String {
            throwIfClosed()
            val c = throwIfNoRow()
            throwIfInvalidColumn(c, index)
            return c.getString(index)
        }

        override fun isNull(index: Int): Boolean {
            throwIfClosed()
            val c = throwIfNoRow()
            throwIfInvalidColumn(c, index)
            return c.isNull(index)
        }

        override fun getColumnCount(): Int {
            throwIfClosed()
            ensureCursor()
            return cursor?.columnCount ?: 0
        }

        override fun getColumnName(index: Int): String {
            throwIfClosed()
            ensureCursor()
            val c = checkNotNull(cursor)
            throwIfInvalidColumn(c, index)
            return c.getColumnName(index)
        }

        override fun getColumnType(index: Int): Int {
            throwIfClosed()
            ensureCursor()
            val c = checkNotNull(cursor)
            throwIfInvalidColumn(c, index)
            return c.getDataType(index)
        }

        override fun step(): Boolean {
            throwIfClosed()
            ensureCursor()
            return checkNotNull(cursor).moveToNext()
        }

        override fun reset() {
            throwIfClosed()
            cursor?.close()
            cursor = null
        }

        override fun clearBindings() {
            throwIfClosed()
            bindingTypes = IntArray(0)
            longBindings = LongArray(0)
            doubleBindings = DoubleArray(0)
            stringBindings = emptyArray()
            blobBindings = emptyArray()
        }

        override fun close() {
            if (!isClosed) {
                reset()
            }
            isClosed = true
        }

        private fun ensureCapacity(columnType: Int, index: Int) {
            val requiredSize = index + 1
            if (bindingTypes.size < requiredSize) {
                bindingTypes = bindingTypes.copyOf(requiredSize)
            }
            when (columnType) {
                SQLITE_DATA_INTEGER -> {
                    if (longBindings.size < requiredSize) {
                        longBindings = longBindings.copyOf(requiredSize)
                    }
                }
                SQLITE_DATA_FLOAT -> {
                    if (doubleBindings.size < requiredSize) {
                        doubleBindings = doubleBindings.copyOf(requiredSize)
                    }
                }
                SQLITE_DATA_TEXT -> {
                    if (stringBindings.size < requiredSize) {
                        stringBindings = stringBindings.copyOf(requiredSize)
                    }
                }
                SQLITE_DATA_BLOB -> {
                    if (blobBindings.size < requiredSize) {
                        blobBindings = blobBindings.copyOf(requiredSize)
                    }
                }
            }
        }

        private fun ensureCursor() {
            if (cursor == null) {
                cursor =
                    db.rawQueryWithFactory(
                        /* cursorFactory = */ { _, masterQuery, editTable, query ->
                            bindTo(query)
                            SQLiteCursor(masterQuery, editTable, query)
                        },
                        /* sql = */ sql,
                        /* selectionArgs = */ arrayOfNulls(0),
                        /* editTable = */ null
                    )
            }
        }

        private fun bindTo(query: SQLiteProgram) {
            for (index in 1 until bindingTypes.size) {
                when (bindingTypes[index]) {
                    SQLITE_DATA_INTEGER -> query.bindLong(index, longBindings[index])
                    SQLITE_DATA_FLOAT -> query.bindDouble(index, doubleBindings[index])
                    SQLITE_DATA_TEXT -> query.bindString(index, stringBindings[index])
                    SQLITE_DATA_BLOB -> query.bindBlob(index, blobBindings[index])
                    SQLITE_DATA_NULL -> query.bindNull(index)
                }
            }
        }

        private fun throwIfNoRow(): Cursor {
            return cursor ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        private fun throwIfInvalidColumn(c: Cursor, index: Int) {
            if (index < 0 || index >= c.columnCount) {
                throwSQLiteException(SQLITE_RANGE, "column index out of range")
            }
        }

        companion object {
            private fun Cursor.getDataType(index: Int): Int {
                val fieldType = this.getType(index)
                return when (this.getType(index)) {
                    FIELD_TYPE_NULL -> SQLITE_DATA_NULL
                    FIELD_TYPE_INTEGER -> SQLITE_DATA_INTEGER
                    FIELD_TYPE_FLOAT -> SQLITE_DATA_FLOAT
                    FIELD_TYPE_STRING -> SQLITE_DATA_TEXT
                    FIELD_TYPE_BLOB -> SQLITE_DATA_BLOB
                    else -> error("Unknown field type: $fieldType")
                }
            }
        }
    }

    private class OtherAndroidSQLiteStatement(db: SQLiteDatabase, sql: String) :
        AndroidSQLiteStatement(db, sql) {

        private val delegate: FrameworkStatement = db.compileStatement(sql)

        override fun bindBlob(index: Int, value: ByteArray) {
            throwIfClosed()
            delegate.bindBlob(index, value)
        }

        override fun bindDouble(index: Int, value: Double) {
            throwIfClosed()
            delegate.bindDouble(index, value)
        }

        override fun bindLong(index: Int, value: Long) {
            throwIfClosed()
            delegate.bindLong(index, value)
        }

        override fun bindText(index: Int, value: String) {
            throwIfClosed()
            delegate.bindString(index, value)
        }

        override fun bindNull(index: Int) {
            throwIfClosed()
            delegate.bindNull(index)
        }

        override fun getBlob(index: Int): ByteArray {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getDouble(index: Int): Double {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getLong(index: Int): Long {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getText(index: Int): String {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun isNull(index: Int): Boolean {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getColumnCount(): Int {
            throwIfClosed()
            return 0
        }

        override fun getColumnName(index: Int): String {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getColumnType(index: Int): Int {
            throwIfClosed()
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun step(): Boolean {
            throwIfClosed()
            delegate.execute()
            return false // Statement never returns a row.
        }

        override fun reset() {
            // Android executes and releases non-query statements, so there is nothing to 'reset'.
        }

        override fun clearBindings() {
            throwIfClosed()
            delegate.clearBindings()
        }

        override fun close() {
            delegate.close()
            isClosed = true
        }
    }
}
