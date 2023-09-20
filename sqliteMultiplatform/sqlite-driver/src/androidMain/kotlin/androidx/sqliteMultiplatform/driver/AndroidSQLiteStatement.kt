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
import androidx.sqliteMultiplatform.SQLiteStatement

private typealias FrameworkStatement = android.database.sqlite.SQLiteStatement

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

        // TODO(b/304299101): Optimize (avoid boxing)
        private val arguments = mutableListOf<Any?>()
        private val argumentTypes = mutableListOf<ColumnType>()

        // TODO(b/307918516): Synchronize
        private var cursor: Cursor? = null

        override fun bindBlob(index: Int, value: ByteArray) {
            resizeForIndex(index)
            argumentTypes[index] = ColumnType.BLOB
            arguments[index] = value
        }

        override fun bindDouble(index: Int, value: Double) {
            resizeForIndex(index)
            argumentTypes[index] = ColumnType.DOUBLE
            arguments[index] = value
        }

        override fun bindLong(index: Int, value: Long) {
            resizeForIndex(index)
            argumentTypes[index] = ColumnType.LONG
            arguments[index] = value
        }

        override fun bindText(index: Int, value: String) {
            resizeForIndex(index)
            argumentTypes[index] = ColumnType.STRING
            arguments[index] = value
        }

        override fun bindNull(index: Int) {
            resizeForIndex(index)
            argumentTypes[index] = ColumnType.NULL
            arguments[index] = null
        }

        override fun getBlob(index: Int): ByteArray {
            return requireNotNull(cursor).getBlob(index)
        }

        override fun getDouble(index: Int): Double {
            return requireNotNull(cursor).getDouble(index)
        }

        override fun getLong(index: Int): Long {
            return requireNotNull(cursor).getLong(index)
        }

        override fun getText(index: Int): String {
            return requireNotNull(cursor).getString(index)
        }

        override fun isNull(index: Int): Boolean {
            return requireNotNull(cursor).isNull(index)
        }

        override fun getColumnCount(): Int {
            return requireNotNull(cursor).columnCount
        }

        override fun getColumnName(index: Int): String {
            return requireNotNull(cursor).getColumnName(index)
        }

        override fun step(): Boolean {
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

        override fun reset() {
            arguments.clear()
            argumentTypes.clear()
            cursor?.close()
            cursor = null
        }

        override fun close() {
            // TODO(b/307918516): Also flip a finalized flag to avoid further usage
            reset()
        }

        private fun resizeForIndex(index: Int) {
            if (argumentTypes.size > index) return
            for (i in argumentTypes.size..index) {
                argumentTypes.add(ColumnType.NULL)
                arguments.add(null)
            }
        }

        private fun bindTo(query: SQLiteProgram) {
            argumentTypes.forEachIndexed { index, type ->
                val bindIndex = index + 1
                when (type) {
                    ColumnType.LONG -> query.bindLong(bindIndex, arguments[index] as Long)
                    ColumnType.DOUBLE -> query.bindDouble(bindIndex, arguments[index] as Double)
                    ColumnType.STRING -> query.bindString(bindIndex, arguments[index] as String)
                    ColumnType.BLOB -> query.bindBlob(bindIndex, arguments[index] as ByteArray)
                    ColumnType.NULL -> query.bindNull(bindIndex)
                }
            }
        }

        companion object {
            private enum class ColumnType {
                LONG,
                DOUBLE,
                STRING,
                BLOB,
                NULL,
            }
        }
    }

    private class OtherAndroidSQLiteStatement(
        db: SQLiteDatabase,
        sql: String
    ) : AndroidSQLiteStatement(db, sql) {

        private val delegate: FrameworkStatement = db.compileStatement(sql)

        override fun bindBlob(index: Int, value: ByteArray) {
            delegate.bindBlob(index, value)
        }

        override fun bindDouble(index: Int, value: Double) {
            delegate.bindDouble(index, value)
        }

        override fun bindLong(index: Int, value: Long) {
            delegate.bindLong(index, value)
        }

        override fun bindText(index: Int, value: String) {
            delegate.bindString(index, value)
        }

        override fun bindNull(index: Int) {
            delegate.bindNull(index)
        }

        override fun getBlob(index: Int): ByteArray {
            error("No result columns")
        }

        override fun getDouble(index: Int): Double {
            error("No result columns")
        }

        override fun getLong(index: Int): Long {
            error("No result columns")
        }

        override fun getText(index: Int): String {
            error("No result columns")
        }

        override fun isNull(index: Int): Boolean {
            error("No result columns")
        }

        override fun getColumnCount(): Int {
            return 0
        }

        override fun getColumnName(index: Int): String {
            error("No result columns")
        }

        override fun step(): Boolean {
            delegate.execute()
            return false
        }

        override fun reset() {
            delegate.clearBindings()
        }

        override fun close() {
            delegate.close()
        }
    }
}
