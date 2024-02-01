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
package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection

/**
 * A data class that holds the information about an FTS table.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual class FtsTableInfo(
    /**
     * The table name
     */
    actual val name: String,

    /**
     * The column names
     */
    actual val columns: Set<String>,

    /**
     * The set of options. Each value in the set contains the option in the following format:
     * <key, value>.
     */
    actual val options: Set<String>
) {
    actual constructor(name: String, columns: Set<String>, createSql: String) :
        this(name, columns, parseFtsOptions(createSql))

    override fun equals(other: Any?) = equalsCommon(other)

    override fun hashCode() = hashCodeCommon()

    override fun toString() = toStringCommon()

    actual companion object {
        /**
         * Reads the table information from the given database.
         *
         * @param connection The database connection to read the information from.
         * @param tableName The table name.
         * @return A FtsTableInfo containing the columns and options for the provided table name.
         */
        actual fun read(connection: SQLiteConnection, tableName: String): FtsTableInfo {
            val columns = readFtsColumns(connection, tableName)
            val options = readFtsOptions(connection, tableName)
            return FtsTableInfo(tableName, columns, options)
        }
    }
}
