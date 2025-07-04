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

package androidx.room.paging

import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_BLOB
import android.database.Cursor.FIELD_TYPE_FLOAT
import android.database.Cursor.FIELD_TYPE_INTEGER
import android.database.Cursor.FIELD_TYPE_NULL
import android.database.Cursor.FIELD_TYPE_STRING
import androidx.annotation.RestrictTo
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement

/** SQLiteStatement backed by a Cursor used for backwards compatibility of Paging2 APIs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CursorSQLiteStatement(private val cursor: Cursor) : SQLiteStatement {
    override fun getBlob(index: Int): ByteArray = cursor.getBlob(index)

    override fun getDouble(index: Int): Double = cursor.getDouble(index)

    override fun getLong(index: Int): Long = cursor.getLong(index)

    override fun getText(index: Int): String = cursor.getString(index)

    override fun isNull(index: Int): Boolean = cursor.isNull(index)

    override fun getColumnCount(): Int = cursor.columnCount

    override fun getColumnName(index: Int): String = cursor.getColumnName(index)

    override fun getColumnType(index: Int): Int = cursor.getDataType(index)

    override fun step(): Boolean = cursor.moveToNext()

    override fun reset() {
        cursor.moveToPosition(-1)
    }

    override fun close() {
        cursor.close()
    }

    override fun bindBlob(index: Int, value: ByteArray): Nothing =
        error("Only get*() calls are allowed on a Cursor backed SQLiteStatement")

    override fun bindDouble(index: Int, value: Double): Nothing =
        error("Only get*() calls are allowed on a Cursor backed SQLiteStatement")

    override fun bindLong(index: Int, value: Long): Nothing =
        error("Only get*() calls are allowed on a Cursor backed SQLiteStatement")

    override fun bindText(index: Int, value: String): Nothing =
        error("Only get*() calls are allowed on a Cursor backed SQLiteStatement")

    override fun bindNull(index: Int): Nothing =
        error("Only get*() calls are allowed on a Cursor backed SQLiteStatement")

    override fun clearBindings(): Nothing =
        error("Only get*() calls are allowed on a Cursor backed SQLiteStatement")

    public companion object {
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
