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
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A data class that holds the information about an FTS table.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect class FtsTableInfo {
    /**
     * The table name
     */
    @JvmField
    val name: String

    /**
     * The column names
     */
    @JvmField
    val columns: Set<String>

    /**
     * The set of options. Each value in the set contains the option in the following format:
     * <key, value>.
     */
    @JvmField
    val options: Set<String>

    constructor(name: String, columns: Set<String>, createSql: String)

    companion object {
        /**
         * Reads the table information from the given database.
         *
         * @param connection The database connection to read the information from.
         * @param tableName The table name.
         * @return A FtsTableInfo containing the columns and options for the provided table name.
         */
        @JvmStatic
        fun read(connection: SQLiteConnection, tableName: String): FtsTableInfo
    }
}

internal fun FtsTableInfo.equalsCommon(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FtsTableInfo) return false
    val that = other
    if (name != that.name) return false
    if (columns != that.columns) return false
    return options == that.options
}

internal fun FtsTableInfo.hashCodeCommon(): Int {
    var result = name.hashCode()
    result = 31 * result + (columns.hashCode())
    result = 31 * result + (options.hashCode())
    return result
}

internal fun FtsTableInfo.toStringCommon(): String {
    return ("FtsTableInfo{name='$name', columns=$columns, options=$options'}")
}
