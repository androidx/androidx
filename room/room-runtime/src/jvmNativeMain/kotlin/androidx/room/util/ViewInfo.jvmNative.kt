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
import kotlin.jvm.JvmStatic

/**
 * A data class that holds the information about a view.
 *
 * This derives information from sqlite_master.
 *
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public actual class ViewInfo
actual constructor(
    /** The view name */
    public actual val name: String,
    /** The SQL of CREATE VIEW. */
    public actual val sql: String?,
) {
    public actual override fun equals(other: Any?): Boolean = equalsCommon(other)

    public actual override fun hashCode(): Int = hashCodeCommon()

    public actual override fun toString(): String = toStringCommon()

    public actual companion object {
        /**
         * Reads the view information from the given database.
         *
         * @param connection The database connection to read the information from.
         * @param viewName The view name.
         * @return A ViewInfo containing the schema information for the provided view name.
         */
        @JvmStatic
        public actual fun read(connection: SQLiteConnection, viewName: String): ViewInfo {
            return readViewInfo(connection, viewName)
        }
    }
}
