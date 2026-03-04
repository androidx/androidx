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
package androidx.room3.util

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.room3.ColumnInfo.SQLiteTypeAffinity
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public class TableInfo(
    /** The table name. */
    @JvmField public val name: String,
    @JvmField public val columns: Map<String, Column>,
    @JvmField public val foreignKeys: Set<ForeignKey>,
    @JvmField public val indices: Set<Index>? = null,
) {
    /** Identifies from where the info object was created. */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [CREATED_FROM_UNKNOWN, CREATED_FROM_ENTITY, CREATED_FROM_DATABASE])
    internal annotation class CreatedFrom

    override fun equals(other: Any?): Boolean = equalsCommon(other)

    override fun hashCode(): Int = hashCodeCommon()

    override fun toString(): String = toStringCommon()

    public companion object {
        /** Identifier for when the info is created from an unknown source. */
        public const val CREATED_FROM_UNKNOWN: Int = 0

        /**
         * Identifier for when the info is created from an entity definition, such as generated code
         * by the compiler or at runtime from a schema bundle, parsed from a schema JSON file.
         */
        public const val CREATED_FROM_ENTITY: Int = 1

        /**
         * Identifier for when the info is created from the database itself, reading information
         * from a PRAGMA, such as table_info.
         */
        public const val CREATED_FROM_DATABASE: Int = 2

        /**
         * Reads the table information from the given database.
         *
         * @param connection The database connection to read the information from.
         * @param tableName The table name.
         * @return A TableInfo containing the schema information for the provided table name.
         */
        @JvmStatic
        public suspend fun read(connection: SQLiteConnection, tableName: String): TableInfo {
            return readTableInfo(connection, tableName)
        }
    }

    /** Holds the information about a database column. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public class Column(
        /** The column name. */
        @JvmField public val name: String,
        /** The column type affinity. */
        @JvmField public val type: String,
        /** Whether or not the column can be NULL. */
        @JvmField public val notNull: Boolean,
        @JvmField public val primaryKeyPosition: Int,
        @JvmField public val defaultValue: String?,
        @param:CreatedFrom @JvmField public val createdFrom: Int,
    ) {
        /**
         * The column type after it is normalized to one of the basic types according to
         * https://www.sqlite.org/datatype3.html Section 3.1.
         *
         * This is the value Room uses for equality check.
         */
        @SQLiteTypeAffinity @JvmField public val affinity: Int = findAffinity(type)

        /**
         * Returns whether this column is part of the primary key or not.
         *
         * @return True if this column is part of the primary key, false otherwise.
         */
        public val isPrimaryKey: Boolean
            get() = primaryKeyPosition > 0

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Column) return false
            if (isPrimaryKey != other.isPrimaryKey) return false
            if (name != other.name) return false
            if (notNull != other.notNull) return false
            // Only validate default value if it was defined in an entity, i.e. if the info
            // from the compiler itself has it. b/136019383
            val defaultValue = this.defaultValue
            val otherDefaultValue = other.defaultValue
            if (
                createdFrom == CREATED_FROM_ENTITY &&
                    other.createdFrom == CREATED_FROM_DATABASE &&
                    defaultValue != null &&
                    !defaultValueEqualsCommon(defaultValue, other.defaultValue)
            ) {
                return false
            } else if (
                createdFrom == CREATED_FROM_DATABASE &&
                    other.createdFrom == CREATED_FROM_ENTITY &&
                    otherDefaultValue != null &&
                    !defaultValueEqualsCommon(otherDefaultValue, defaultValue)
            ) {
                return false
            } else if (
                createdFrom != CREATED_FROM_UNKNOWN &&
                    createdFrom == other.createdFrom &&
                    (if (defaultValue != null)
                        !defaultValueEqualsCommon(defaultValue, otherDefaultValue)
                    else otherDefaultValue != null)
            ) {
                return false
            }
            return affinity == other.affinity
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + affinity
            result = 31 * result + if (notNull) 1231 else 1237
            result = 31 * result + primaryKeyPosition
            return result
            // result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
            // equality which would break the equals + hashcode contract.
            // Default value is not part of the hashcode since we conditionally check it for
        }

        override fun toString(): String =
            ("""
                |Column {
                |   name = '${name}',
                |   type = '${type}',
                |   affinity = '${affinity}',
                |   notNull = '${notNull}',
                |   primaryKeyPosition = '${primaryKeyPosition}',
                |   defaultValue = '${defaultValue ?: "undefined"}'
                |}
            """
                .trimMargin()
                .prependIndent())

        internal companion object {
            fun defaultValueEquals(current: String, other: String?): Boolean =
                defaultValueEqualsCommon(current, other)
        }
    }

    /** Holds the information about an SQLite foreign key */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public class ForeignKey(
        @JvmField public val referenceTable: String,
        @JvmField public val onDelete: String,
        @JvmField public val onUpdate: String,
        @JvmField public val columnNames: List<String>,
        @JvmField public val referenceColumnNames: List<String>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ForeignKey) return false
            if (referenceTable != other.referenceTable) return false
            if (onDelete != other.onDelete) return false
            if (onUpdate != other.onUpdate) return false
            return if (columnNames != other.columnNames) false
            else referenceColumnNames == other.referenceColumnNames
        }

        override fun hashCode(): Int {
            var result = referenceTable.hashCode()
            result = 31 * result + onDelete.hashCode()
            result = 31 * result + onUpdate.hashCode()
            result = 31 * result + columnNames.hashCode()
            result = 31 * result + referenceColumnNames.hashCode()
            return result
        }

        override fun toString(): String =
            ("""
                |ForeignKey {
                |   referenceTable = '${referenceTable}',
                |   onDelete = '${onDelete}',
                |   onUpdate = '${onUpdate}',
                |   columnNames = {${columnNames.sorted().joinToStringMiddleWithIndent()}
                |   referenceColumnNames = {${
            referenceColumnNames.sorted().joinToStringEndWithIndent()
        }
                |}
            """
                .trimMargin()
                .prependIndent())
    }

    /** Holds the information about an SQLite index */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public class Index(
        @JvmField public val name: String,
        @JvmField public val unique: Boolean,
        @JvmField public val columns: List<String>,
        @JvmField public var orders: List<String>,
    ) {
        init {
            orders = orders.ifEmpty { List(columns.size) { androidx.room3.Index.Order.ASC.name } }
        }

        internal companion object {
            // should match the value in Index.kt
            const val DEFAULT_PREFIX: String = "index_"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Index) return false
            if (unique != other.unique) {
                return false
            }
            if (columns != other.columns) {
                return false
            }
            if (orders != other.orders) {
                return false
            }
            return if (name.startsWith(DEFAULT_PREFIX)) {
                other.name.startsWith(DEFAULT_PREFIX)
            } else {
                name == other.name
            }
        }

        override fun hashCode(): Int {
            var result =
                if (name.startsWith(DEFAULT_PREFIX)) {
                    DEFAULT_PREFIX.hashCode()
                } else {
                    name.hashCode()
                }
            result = 31 * result + if (unique) 1 else 0
            result = 31 * result + columns.hashCode()
            result = 31 * result + orders.hashCode()
            return result
        }

        override fun toString(): String =
            ("""
                |Index {
                |   name = '${name}',
                |   unique = '${unique}',
                |   columns = {${columns.joinToStringMiddleWithIndent()}
                |   orders = {${orders.joinToStringEndWithIndent()}
                |}
            """
                .trimMargin()
                .prependIndent())
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

internal fun formatString(collection: Collection<*>): String {
    return if (collection.isNotEmpty()) {
        collection.joinToString(separator = ",\n", prefix = "\n", postfix = "\n").prependIndent() +
            "},"
    } else {
        " }"
    }
}

private fun Collection<*>.joinToStringMiddleWithIndent(): String =
    this.joinToString(",").prependIndent() + "},".prependIndent()

private fun Collection<*>.joinToStringEndWithIndent(): String =
    this.joinToString(",").prependIndent() + " }".prependIndent()
