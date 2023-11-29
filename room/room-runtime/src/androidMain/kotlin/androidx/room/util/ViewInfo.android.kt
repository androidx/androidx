/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A data class that holds the information about a view.
 *
 *
 * This derives information from sqlite_master.
 *
 *
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class ViewInfo(
    /**
     * The view name
     */
    @JvmField
    val name: String,
    /**
     * The SQL of CREATE VIEW.
     */
    @JvmField
    val sql: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewInfo) return false
        return ((name == other.name) && if (sql != null) sql == other.sql else other.sql == null)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (sql?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return ("ViewInfo{" + "name='" + name + '\'' + ", sql='" + sql + '\'' + '}')
    }

    companion object {
        /**
         * Reads the view information from the given database.
         *
         * @param database The database to read the information from.
         * @param viewName The view name.
         * @return A ViewInfo containing the schema information for the provided view name.
         */
        @JvmStatic
        fun read(database: SupportSQLiteDatabase, viewName: String): ViewInfo {
            return database.query(
                "SELECT name, sql FROM sqlite_master " +
                    "WHERE type = 'view' AND name = '$viewName'"
            ).useCursor { cursor ->
                if (cursor.moveToFirst()) {
                    ViewInfo(cursor.getString(0), cursor.getString(1))
                } else {
                    ViewInfo(viewName, null)
                }
            }
        }
    }
}
