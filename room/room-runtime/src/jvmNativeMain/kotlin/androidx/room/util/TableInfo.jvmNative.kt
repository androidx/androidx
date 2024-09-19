/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo.SQLiteTypeAffinity
import androidx.sqlite.SQLiteConnection

/**
 * A data class that holds the information about a table.
 *
 * It directly maps to the result of `PRAGMA table_info(<table_name>)`. Check the
 * [PRAGMA table_info](http://www.sqlite.org/pragma.html#pragma_table_info) documentation for more
 * details.
 *
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual class TableInfo
actual constructor(
    /** The table name. */
    actual val name: String,
    actual val columns: Map<String, Column>,
    actual val foreignKeys: Set<ForeignKey>,
    actual val indices: Set<Index>?
) {
    /** Identifies from where the info object was created. */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [CREATED_FROM_UNKNOWN, CREATED_FROM_ENTITY, CREATED_FROM_DATABASE])
    internal annotation class CreatedFrom()

    actual override fun equals(other: Any?) = equalsCommon(other)

    actual override fun hashCode() = hashCodeCommon()

    actual override fun toString() = toStringCommon()

    actual companion object {
        /** Identifier for when the info is created from an unknown source. */
        actual const val CREATED_FROM_UNKNOWN = 0

        /**
         * Identifier for when the info is created from an entity definition, such as generated code
         * by the compiler or at runtime from a schema bundle, parsed from a schema JSON file.
         */
        actual const val CREATED_FROM_ENTITY = 1

        /**
         * Identifier for when the info is created from the database itself, reading information
         * from a PRAGMA, such as table_info.
         */
        actual const val CREATED_FROM_DATABASE = 2

        /**
         * Reads the table information from the given database.
         *
         * @param connection The database connection to read the information from.
         * @param tableName The table name.
         * @return A TableInfo containing the schema information for the provided table name.
         */
        actual fun read(connection: SQLiteConnection, tableName: String): TableInfo {
            return readTableInfo(connection, tableName)
        }
    }

    /** Holds the information about a database column. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    actual class Column
    actual constructor(
        /** The column name. */
        actual val name: String,
        /** The column type affinity. */
        actual val type: String,
        /** Whether or not the column can be NULL. */
        actual val notNull: Boolean,
        actual val primaryKeyPosition: Int,
        actual val defaultValue: String?,
        @CreatedFrom actual val createdFrom: Int
    ) {
        /**
         * The column type after it is normalized to one of the basic types according to
         * https://www.sqlite.org/datatype3.html Section 3.1.
         *
         * This is the value Room uses for equality check.
         */
        @SQLiteTypeAffinity actual val affinity: Int = findAffinity(type)

        /**
         * Returns whether this column is part of the primary key or not.
         *
         * @return True if this column is part of the primary key, false otherwise.
         */
        actual val isPrimaryKey: Boolean
            get() = primaryKeyPosition > 0

        actual override fun equals(other: Any?) = equalsCommon(other)

        actual override fun hashCode() = hashCodeCommon()

        actual override fun toString() = toStringCommon()
    }

    /** Holds the information about an SQLite foreign key */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    actual class ForeignKey
    actual constructor(
        actual val referenceTable: String,
        actual val onDelete: String,
        actual val onUpdate: String,
        actual val columnNames: List<String>,
        actual val referenceColumnNames: List<String>
    ) {
        actual override fun equals(other: Any?) = equalsCommon(other)

        actual override fun hashCode() = hashCodeCommon()

        actual override fun toString() = toStringCommon()
    }

    /** Holds the information about an SQLite index */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    actual class Index
    actual constructor(
        actual val name: String,
        actual val unique: Boolean,
        actual val columns: List<String>,
        actual var orders: List<String>
    ) {
        init {
            orders = orders.ifEmpty { List(columns.size) { androidx.room.Index.Order.ASC.name } }
        }

        actual companion object {
            // should match the value in Index.kt
            actual const val DEFAULT_PREFIX = "index_"
        }

        actual override fun equals(other: Any?) = equalsCommon(other)

        actual override fun hashCode() = hashCodeCommon()

        actual override fun toString() = toStringCommon()
    }
}
