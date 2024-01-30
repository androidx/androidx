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

package androidx.sqliteMultiplatform

/**
 * SQLite statement definition.
 *
 * See also [Prepared Statement](https://www.sqlite.org/c3ref/stmt.html)
 */
interface SQLiteStatement {
    fun bindBlob(index: Int, value: ByteArray)
    fun bindDouble(index: Int, value: Double)
    fun bindLong(index: Int, value: Long)
    fun bindText(index: Int, value: String)
    fun bindNull(index: Int)

    fun getBlob(index: Int): ByteArray
    fun getDouble(index: Int): Double
    fun getLong(index: Int): Long
    fun getText(index: Int): String
    fun isNull(index: Int): Boolean

    fun getColumnCount(): Int
    fun getColumnName(index: Int): String

    fun step(): Boolean
    fun reset()
    fun close()
}
