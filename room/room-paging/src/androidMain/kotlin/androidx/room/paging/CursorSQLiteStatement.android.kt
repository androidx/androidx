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

import android.database.AbstractCursor
import androidx.sqlite.SQLiteStatement

/** Wrapper class for backwards compatibility in room-paging. */
internal class SQLiteStatementCursor(
    private val statement: SQLiteStatement,
    private val rowCount: Int
) : AbstractCursor() {
    override fun getCount(): Int = rowCount

    override fun getColumnNames(): Array<String> = statement.getColumnNames().toTypedArray()

    override fun getString(column: Int): String = statement.getText(column)

    override fun getShort(column: Int): Short = statement.getLong(column).toShort()

    override fun getInt(column: Int): Int = statement.getInt(column)

    override fun getLong(column: Int): Long = statement.getLong(column)

    override fun getFloat(column: Int): Float = statement.getFloat(column)

    override fun getDouble(column: Int): Double = statement.getDouble(column)

    override fun isNull(column: Int): Boolean = statement.isNull(column)

    override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
        check(oldPosition + 1 == newPosition) {
            "Compat cursor can only move forward one position at a time."
        }
        return statement.step()
    }
}
