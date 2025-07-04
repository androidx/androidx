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

package androidx.sqlite.driver.bundled

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteStatement

// Restricted instead of internal due to KT-37316
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public expect class BundledSQLiteStatement : SQLiteStatement {
    override fun bindBlob(index: Int, value: ByteArray)

    override fun bindDouble(index: Int, value: Double)

    override fun bindLong(index: Int, value: Long)

    override fun bindText(index: Int, value: String)

    override fun bindNull(index: Int)

    override fun getBlob(index: Int): ByteArray

    override fun getDouble(index: Int): Double

    override fun getLong(index: Int): Long

    override fun getText(index: Int): String

    override fun isNull(index: Int): Boolean

    override fun getColumnCount(): Int

    override fun getColumnName(index: Int): String

    override fun getColumnType(index: Int): Int

    override fun step(): Boolean

    override fun reset()

    override fun clearBindings()

    override fun close()
}
