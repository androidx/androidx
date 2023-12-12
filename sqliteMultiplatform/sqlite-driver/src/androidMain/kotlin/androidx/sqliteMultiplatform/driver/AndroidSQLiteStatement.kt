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

package androidx.sqliteMultiplatform.driver

import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteProgram
import androidx.sqliteMultiplatform.SQLiteException
import androidx.sqliteMultiplatform.SQLiteStatement

private typealias FrameworkStatement = android.database.sqlite.SQLiteStatement
private typealias FrameworkSQLiteException = android.database.sqlite.SQLiteException

internal sealed class AndroidSQLiteStatement(
    protected val db: SQLiteDatabase,
    protected val sql: String,
) : SQLiteStatement {

    companion object {
        fun create(db: SQLiteDatabase, sql: String): AndroidSQLiteStatement {
            return when (DatabaseUtils.getSqlStatementType(sql)) {
                DatabaseUtils.STATEMENT_SELECT,
                DatabaseUtils.STATEMENT_PRAGMA ->
                    // Statements that return rows (SQLITE_ROW)
                    SelectAndroidSQLiteStatement(db, sql)
                else ->
                    // Statements that don't return row (SQLITE_DONE)
                    OtherAndroidSQLiteStatement(db, sql)
            }
        }
    }

    // TODO(b/304298743): Use android.database.SQLiteRawStatement on Android V+
    private class SelectAndroidSQLiteStatement(
        db: SQLiteDatabase,
        sql: String
    ) : AndroidSQLiteStatement(db, sql) {

        private var bindingTypes: IntArray = IntArray(0)
        private var longBindings: LongArray = LongArray(0)
        private var doubleBindings: DoubleArray = DoubleArray(0)
        private var stringBindings: Array<String?> = emptyArray()
        private var blobBindings: Array<ByteArray?> = emptyArray()

        // TODO(b/307918516): Synchronize
        private var cursor: Cursor? = null

        override fun bindBlob(index: Int, value: ByteArray) {
            ensureCapacity(COLUMN_TYPE_BLOB, index)
            bindingTypes[index] = COLUMN_TYPE_BLOB
            blobBindings[index] = value
        }

        override fun bindDouble(index: Int, value: Double) {
            ensureCapacity(COLUMN_TYPE_DOUBLE, index)
            bindingTypes[index] = COLUMN_TYPE_DOUBLE
            doubleBindings[index] = value
        }

        override fun bindLong(index: Int, value: Long) {
            ensureCapacity(COLUMN_TYPE_LONG, index)
            bindingTypes[index] = COLUMN_TYPE_LONG
            longBindings[index] = value
        }

        override fun bindText(index: Int, value: String) {
            ensureCapacity(COLUMN_TYPE_STRING, index)
            bindingTypes[index] = COLUMN_TYPE_STRING
            stringBindings[index] = value
        }

        override fun bindNull(index: Int) {
            ensureCapacity(COLUMN_TYPE_NULL, index)
            bindingTypes[index] = COLUMN_TYPE_NULL
        }

        override fun getBlob(index: Int): ByteArray = withExceptionCatch {
            return cursor?.getBlob(index) ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getDouble(index: Int): Double = withExceptionCatch {
            return cursor?.getDouble(index) ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getLong(index: Int): Long = withExceptionCatch {
            return cursor?.getLong(index) ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getText(index: Int): String = withExceptionCatch {
            return cursor?.getString(index) ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun isNull(index: Int): Boolean = withExceptionCatch {
            return cursor?.isNull(index) ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getColumnCount(): Int = withExceptionCatch {
            return cursor?.columnCount ?: 0
        }

        override fun getColumnName(index: Int): String = withExceptionCatch {
            return cursor?.getColumnName(index) ?: throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun step(): Boolean = withExceptionCatch {
            if (cursor == null) {
                cursor = db.rawQueryWithFactory(
                    /* cursorFactory = */ { _, masterQuery, editTable, query ->
                        bindTo(query)
                        SQLiteCursor(masterQuery, editTable, query)
                    },
                    /* sql = */ sql,
                    /* selectionArgs = */ arrayOfNulls(0),
                    /* editTable = */ null
                )
            }
            return requireNotNull(cursor).moveToNext()
        }

        override fun reset(): Unit = withExceptionCatch {
            bindingTypes = IntArray(0)
            longBindings = LongArray(0)
            doubleBindings = DoubleArray(0)
            stringBindings = emptyArray()
            blobBindings = emptyArray()
            cursor?.close()
            cursor = null
        }

        override fun close(): Unit = withExceptionCatch {
            // TODO(b/307918516): Also flip a finalized flag to avoid further usage
            reset()
        }

        private fun ensureCapacity(columnType: Int, index: Int) {
            val requiredSize = index + 1
            if (bindingTypes.size < requiredSize) {
                bindingTypes = bindingTypes.copyOf(requiredSize)
            }
            when (columnType) {
                COLUMN_TYPE_LONG -> {
                    if (longBindings.size < requiredSize) {
                        longBindings = longBindings.copyOf(requiredSize)
                    }
                }
                COLUMN_TYPE_DOUBLE -> {
                    if (doubleBindings.size < requiredSize) {
                        doubleBindings = doubleBindings.copyOf(requiredSize)
                    }
                }
                COLUMN_TYPE_STRING -> {
                    if (stringBindings.size < requiredSize) {
                        stringBindings = stringBindings.copyOf(requiredSize)
                    }
                }
                COLUMN_TYPE_BLOB -> {
                    if (blobBindings.size < requiredSize) {
                        blobBindings = blobBindings.copyOf(requiredSize)
                    }
                }
            }
        }

        private fun bindTo(query: SQLiteProgram) {
            for (index in 1 until bindingTypes.size) {
                when (bindingTypes[index]) {
                    COLUMN_TYPE_LONG -> query.bindLong(index, longBindings[index])
                    COLUMN_TYPE_DOUBLE -> query.bindDouble(index, doubleBindings[index])
                    COLUMN_TYPE_STRING -> query.bindString(index, stringBindings[index])
                    COLUMN_TYPE_BLOB -> query.bindBlob(index, blobBindings[index])
                    COLUMN_TYPE_NULL -> query.bindNull(index)
                }
            }
        }

        companion object {
            private const val COLUMN_TYPE_LONG = 1
            private const val COLUMN_TYPE_DOUBLE = 2
            private const val COLUMN_TYPE_STRING = 3
            private const val COLUMN_TYPE_BLOB = 4
            private const val COLUMN_TYPE_NULL = 5
        }
    }

    private class OtherAndroidSQLiteStatement(
        db: SQLiteDatabase,
        sql: String
    ) : AndroidSQLiteStatement(db, sql) {

        private val delegate: FrameworkStatement = withExceptionCatch {
            db.compileStatement(sql)
        }

        override fun bindBlob(index: Int, value: ByteArray) = withExceptionCatch {
            delegate.bindBlob(index, value)
        }

        override fun bindDouble(index: Int, value: Double) = withExceptionCatch {
            delegate.bindDouble(index, value)
        }

        override fun bindLong(index: Int, value: Long) = withExceptionCatch {
            delegate.bindLong(index, value)
        }

        override fun bindText(index: Int, value: String) = withExceptionCatch {
            delegate.bindString(index, value)
        }

        override fun bindNull(index: Int) = withExceptionCatch {
            delegate.bindNull(index)
        }

        override fun getBlob(index: Int): ByteArray {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getDouble(index: Int): Double {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getLong(index: Int): Long {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getText(index: Int): String {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun isNull(index: Int): Boolean {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun getColumnCount(): Int {
            return 0
        }

        override fun getColumnName(index: Int): String {
            throwSQLiteException(SQLITE_MISUSE, "no row")
        }

        override fun step(): Boolean = withExceptionCatch {
            delegate.execute()
            return false // Statement never returns a row.
        }

        override fun reset(): Unit = withExceptionCatch {
            delegate.clearBindings()
        }

        override fun close(): Unit = withExceptionCatch {
            delegate.close()
        }
    }
}

private inline fun <T> withExceptionCatch(block: () -> T): T {
    try {
       return block.invoke()
    } catch (ex: FrameworkSQLiteException) {
        // TODO(b/304297717): Parse error code from exception.
       throw SQLiteException(ex.message ?: "")
    }
}

private fun throwSQLiteException(errorCode: Int, errorMsg: String?): Nothing {
    val message = buildString {
        append("Error code: $errorCode")
        if (errorMsg != null) {
            append(", message: $errorMsg")
        }
    }
    throw SQLiteException(message)
}

private const val SQLITE_MISUSE = 21
