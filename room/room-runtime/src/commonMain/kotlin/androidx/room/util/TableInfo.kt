/*
 * Copyright (C) 2017 The Android Open Source Project
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
import androidx.room.ColumnInfo.SQLiteTypeAffinity
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

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
expect class TableInfo(
    name: String,
    columns: Map<String, Column>,
    foreignKeys: Set<ForeignKey>,
    indices: Set<Index>? = null
) {
    /** The table name. */
    @JvmField val name: String
    @JvmField val columns: Map<String, Column>
    @JvmField val foreignKeys: Set<ForeignKey>
    @JvmField val indices: Set<Index>?

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String

    companion object {
        /** Identifier for when the info is created from an unknown source. */
        val CREATED_FROM_UNKNOWN: Int

        /**
         * Identifier for when the info is created from an entity definition, such as generated code
         * by the compiler or at runtime from a schema bundle, parsed from a schema JSON file.
         */
        val CREATED_FROM_ENTITY: Int

        /**
         * Identifier for when the info is created from the database itself, reading information
         * from a PRAGMA, such as table_info.
         */
        val CREATED_FROM_DATABASE: Int

        /**
         * Reads the table information from the given database.
         *
         * @param connection The database connection to read the information from.
         * @param tableName The table name.
         * @return A TableInfo containing the schema information for the provided table name.
         */
        @JvmStatic fun read(connection: SQLiteConnection, tableName: String): TableInfo
    }

    /** Holds the information about a database column. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    class Column(
        name: String,
        type: String,
        notNull: Boolean,
        primaryKeyPosition: Int,
        defaultValue: String?,
        createdFrom: Int
    ) {
        /** The column name. */
        @JvmField val name: String
        /** The column type affinity. */
        @JvmField val type: String
        /** Whether or not the column can be NULL. */
        @JvmField val notNull: Boolean
        @JvmField val primaryKeyPosition: Int
        @JvmField val defaultValue: String?
        @JvmField val createdFrom: Int

        /**
         * The column type after it is normalized to one of the basic types according to
         * https://www.sqlite.org/datatype3.html Section 3.1.
         *
         * This is the value Room uses for equality check.
         */
        @SQLiteTypeAffinity @JvmField val affinity: Int

        /**
         * Returns whether this column is part of the primary key or not.
         *
         * @return True if this column is part of the primary key, false otherwise.
         */
        val isPrimaryKey: Boolean

        override fun equals(other: Any?): Boolean

        override fun hashCode(): Int

        override fun toString(): String
    }

    /** Holds the information about an SQLite foreign key */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    class ForeignKey(
        referenceTable: String,
        onDelete: String,
        onUpdate: String,
        columnNames: List<String>,
        referenceColumnNames: List<String>
    ) {
        @JvmField val referenceTable: String
        @JvmField val onDelete: String
        @JvmField val onUpdate: String
        @JvmField val columnNames: List<String>
        @JvmField val referenceColumnNames: List<String>

        override fun equals(other: Any?): Boolean

        override fun hashCode(): Int

        override fun toString(): String
    }

    /** Holds the information about an SQLite index */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    class Index(name: String, unique: Boolean, columns: List<String>, orders: List<String>) {

        @JvmField val name: String
        @JvmField val unique: Boolean
        @JvmField val columns: List<String>
        @JvmField var orders: List<String>

        companion object {
            // should match the value in Index.kt
            val DEFAULT_PREFIX: String
        }

        override fun equals(other: Any?): Boolean

        override fun hashCode(): Int

        override fun toString(): String
    }
}

internal fun TableInfo.equalsCommon(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TableInfo) return false
    if (name != other.name) return false
    if (columns != other.columns) {
        return false
    }
    if (foreignKeys != other.foreignKeys) {
        return false
    }
    return if (indices == null || other.indices == null) {
        // if one us is missing index information, seems like we couldn't acquire the
        // information so we better skip.
        true
    } else indices == other.indices
}

internal fun TableInfo.hashCodeCommon(): Int {
    var result = name.hashCode()
    result = 31 * result + columns.hashCode()
    result = 31 * result + foreignKeys.hashCode()
    // skip index, it is not reliable for comparison.
    return result
}

internal fun TableInfo.toStringCommon(): String {
    return ("""
            |TableInfo {
            |    name = '$name',
            |    columns = {${formatString(columns.values.sortedBy { it.name })}
            |    foreignKeys = {${formatString(foreignKeys)}
            |    indices = {${formatString(indices?.sortedBy { it.name } ?: emptyList<String>())}
            |}
        """
        .trimMargin())
}

internal fun TableInfo.Column.equalsCommon(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TableInfo.Column) return false
    if (isPrimaryKey != other.isPrimaryKey) return false
    if (name != other.name) return false
    if (notNull != other.notNull) return false
    // Only validate default value if it was defined in an entity, i.e. if the info
    // from the compiler itself has it. b/136019383
    if (
        createdFrom == TableInfo.CREATED_FROM_ENTITY &&
            other.createdFrom == TableInfo.CREATED_FROM_DATABASE &&
            defaultValue != null &&
            !defaultValueEqualsCommon(defaultValue, other.defaultValue)
    ) {
        return false
    } else if (
        createdFrom == TableInfo.CREATED_FROM_DATABASE &&
            other.createdFrom == TableInfo.CREATED_FROM_ENTITY &&
            other.defaultValue != null &&
            !defaultValueEqualsCommon(other.defaultValue, defaultValue)
    ) {
        return false
    } else if (
        createdFrom != TableInfo.CREATED_FROM_UNKNOWN &&
            createdFrom == other.createdFrom &&
            (if (defaultValue != null) !defaultValueEqualsCommon(defaultValue, other.defaultValue)
            else other.defaultValue != null)
    ) {
        return false
    }
    return affinity == other.affinity
}

/**
 * Checks if the default values provided match. Handles the special case in which the default value
 * is surrounded by parenthesis (e.g. encountered in b/182284899).
 *
 * Surrounding parenthesis are removed by SQLite when reading from the database, hence this function
 * will check if they are present in the actual value, if so, it will compare the two values by
 * ignoring the surrounding parenthesis.
 */
internal fun defaultValueEqualsCommon(current: String, other: String?): Boolean {
    if (current == other) {
        return true
    } else if (containsSurroundingParenthesis(current)) {
        return current.substring(1, current.length - 1).trim() == other
    }
    return false
}

/**
 * Checks for potential surrounding parenthesis, if found, removes them and checks if remaining
 * parenthesis are balanced. If so, the surrounding parenthesis are redundant, and returns true.
 */
private fun containsSurroundingParenthesis(current: String): Boolean {
    if (current.isEmpty()) {
        return false
    }
    var surroundingParenthesis = 0
    current.forEachIndexed { i, c ->
        if (i == 0 && c != '(') {
            return false
        }
        if (c == '(') {
            surroundingParenthesis++
        } else if (c == ')') {
            surroundingParenthesis--
            if (surroundingParenthesis == 0 && i != current.length - 1) {
                return false
            }
        }
    }
    return surroundingParenthesis == 0
}

internal fun TableInfo.Column.hashCodeCommon(): Int {
    var result = name.hashCode()
    result = 31 * result + affinity
    result = 31 * result + if (notNull) 1231 else 1237
    result = 31 * result + primaryKeyPosition
    // Default value is not part of the hashcode since we conditionally check it for
    // equality which would break the equals + hashcode contract.
    // result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
    return result
}

internal fun TableInfo.Column.toStringCommon(): String {
    return ("""
            |Column {
            |   name = '$name',
            |   type = '$type',
            |   affinity = '$affinity',
            |   notNull = '$notNull',
            |   primaryKeyPosition = '$primaryKeyPosition',
            |   defaultValue = '${defaultValue ?: "undefined"}'
            |}
        """
        .trimMargin()
        .prependIndent())
}

internal fun TableInfo.ForeignKey.equalsCommon(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TableInfo.ForeignKey) return false
    if (referenceTable != other.referenceTable) return false
    if (onDelete != other.onDelete) return false
    if (onUpdate != other.onUpdate) return false
    return if (columnNames != other.columnNames) false
    else referenceColumnNames == other.referenceColumnNames
}

internal fun TableInfo.ForeignKey.hashCodeCommon(): Int {
    var result = referenceTable.hashCode()
    result = 31 * result + onDelete.hashCode()
    result = 31 * result + onUpdate.hashCode()
    result = 31 * result + columnNames.hashCode()
    result = 31 * result + referenceColumnNames.hashCode()
    return result
}

internal fun TableInfo.ForeignKey.toStringCommon(): String {
    return ("""
            |ForeignKey {
            |   referenceTable = '$referenceTable',
            |   onDelete = '$onDelete',
            |   onUpdate = '$onUpdate',
            |   columnNames = {${columnNames.sorted().joinToStringMiddleWithIndent()}
            |   referenceColumnNames = {${referenceColumnNames.sorted().joinToStringEndWithIndent()}
            |}
        """
        .trimMargin()
        .prependIndent())
}

internal fun TableInfo.Index.equalsCommon(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TableInfo.Index) return false
    if (unique != other.unique) {
        return false
    }
    if (columns != other.columns) {
        return false
    }
    if (orders != other.orders) {
        return false
    }
    return if (name.startsWith(TableInfo.Index.DEFAULT_PREFIX)) {
        other.name.startsWith(TableInfo.Index.DEFAULT_PREFIX)
    } else {
        name == other.name
    }
}

internal fun TableInfo.Index.hashCodeCommon(): Int {
    var result =
        if (name.startsWith(TableInfo.Index.DEFAULT_PREFIX)) {
            TableInfo.Index.DEFAULT_PREFIX.hashCode()
        } else {
            name.hashCode()
        }
    result = 31 * result + if (unique) 1 else 0
    result = 31 * result + columns.hashCode()
    result = 31 * result + orders.hashCode()
    return result
}

internal fun TableInfo.Index.toStringCommon(): String {
    return ("""
            |Index {
            |   name = '$name',
            |   unique = '$unique',
            |   columns = {${columns.joinToStringMiddleWithIndent()}
            |   orders = {${orders.joinToStringEndWithIndent()}
            |}
        """
        .trimMargin()
        .prependIndent())
}

internal fun formatString(collection: Collection<*>): String {
    return if (collection.isNotEmpty()) {
        collection.joinToString(separator = ",\n", prefix = "\n", postfix = "\n").prependIndent() +
            "},"
    } else {
        " }"
    }
}

private fun Collection<*>.joinToStringMiddleWithIndent() {
    this.joinToString(",").prependIndent() + "},".prependIndent()
}

private fun Collection<*>.joinToStringEndWithIndent() {
    this.joinToString(",").prependIndent() + " }".prependIndent()
}
