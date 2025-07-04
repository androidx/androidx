/*
 * Copyright 2025 The Android Open Source Project
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

@file:JvmName("RoomSupportSQLite")

package androidx.room.support

import android.database.Cursor
import android.database.MatrixCursor
import androidx.room.PooledConnection
import androidx.room.RoomDatabase
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.WeakHashMap

private val wrapperCache = WeakHashMap<RoomDatabase, RoomSupportSQLiteDatabase>()

/**
 * Gets a new [SupportSQLiteDatabase] implementation that is backed by a [RoomDatabase] for
 * compatibility with existing usages of [SupportSQLiteDatabase].
 */
public fun RoomDatabase.getSupportWrapper(): SupportSQLiteDatabase {
    // If Room is in compatibility mode, it has no driver configured and SupportSQLite APIs
    // are available.
    if (inCompatibilityMode()) {
        return openHelper.writableDatabase
    }
    return synchronized(wrapperCache) {
        wrapperCache.getOrPut(this) { RoomSupportSQLiteDatabase(this) }
    }
}

internal fun SQLiteStatement.bindArgsArray(bindArgs: Array<out Any?>) {
    bindArgs.forEachIndexed { index, arg ->
        val bindIndex = index + 1
        when (arg) {
            null -> bindNull(bindIndex)
            is Boolean -> bindBoolean(bindIndex, arg)
            is Int -> bindInt(bindIndex, arg)
            is Long -> bindLong(bindIndex, arg)
            is Float -> bindFloat(bindIndex, arg)
            is Double -> bindDouble(bindIndex, arg)
            is String -> bindText(bindIndex, arg)
            is ByteArray -> bindBlob(bindIndex, arg)
        }
    }
}

internal fun SupportSQLiteStatement.bindArgsArray(bindArgs: Array<out Any?>) {
    bindArgs.forEachIndexed { index, arg ->
        val bindIndex = index + 1
        when (arg) {
            null -> bindNull(bindIndex)
            is Boolean -> bindLong(bindIndex, if (arg) 1 else 0)
            is Int -> bindLong(bindIndex, arg.toLong())
            is Long -> bindLong(bindIndex, arg)
            is Float -> bindDouble(bindIndex, arg.toDouble())
            is Double -> bindDouble(bindIndex, arg)
            is String -> bindString(bindIndex, arg)
            is ByteArray -> bindBlob(bindIndex, arg)
        }
    }
}

internal fun SQLiteStatement.bindQuery(query: SupportSQLiteQuery) {
    query.bindTo(
        object : SupportSQLiteProgram {
            override fun bindNull(index: Int) {
                this@bindQuery.bindNull(index)
            }

            override fun bindLong(index: Int, value: Long) {
                this@bindQuery.bindLong(index, value)
            }

            override fun bindDouble(index: Int, value: Double) {
                this@bindQuery.bindDouble(index, value)
            }

            override fun bindString(index: Int, value: String) {
                this@bindQuery.bindText(index, value)
            }

            override fun bindBlob(index: Int, value: ByteArray) {
                this@bindQuery.bindBlob(index, value)
            }

            override fun clearBindings() {
                this@bindQuery.clearBindings()
            }

            override fun close() {}
        }
    )
}

internal fun SQLiteStatement.toCursor(): Cursor {
    val columnNames = getColumnNames().toTypedArray()
    val cursor = MatrixCursor(columnNames)
    while (step()) {
        val row =
            Array<Any?>(columnNames.size) { i ->
                val columnType = getColumnType(i)
                when (columnType) {
                    SQLITE_DATA_INTEGER -> getLong(i)
                    SQLITE_DATA_FLOAT -> getDouble(i)
                    SQLITE_DATA_TEXT -> getText(i)
                    SQLITE_DATA_BLOB -> getBlob(i)
                    SQLITE_DATA_NULL -> null
                    else -> error("Unknown column type: $columnType")
                }
            }
        cursor.addRow(row)
    }
    return cursor
}

internal suspend fun PooledConnection.getLastInsertedRowId(): Long {
    if (getTotalChangedRows() == 0) {
        return -1
    }
    return usePrepared("SELECT last_insert_rowid()") {
        it.step()
        it.getLong(0)
    }
}

internal suspend fun PooledConnection.getTotalChangedRows(): Int {
    return usePrepared("SELECT changes()") {
        it.step()
        it.getLong(0).toInt()
    }
}
