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

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.SQLiteTypeAffinity
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale
import java.util.TreeMap

/**
 * A data class that holds the information about a table.
 *
 * It directly maps to the result of `PRAGMA table_info(<table_name>)`. Check the
 * [PRAGMA table_info](http://www.sqlite.org/pragma.html#pragma_table_info)
 * documentation for more details.
 *
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
// if you change this class, you must change TableInfoValidationWriter.kt
class TableInfo(
    /**
     * The table name.
     */
    @JvmField
    val name: String,
    @JvmField
    val columns: Map<String, Column>,
    @JvmField
    val foreignKeys: Set<ForeignKey>,
    @JvmField
    val indices: Set<Index>? = null
) {
    /**
     * Identifies from where the info object was created.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [CREATED_FROM_UNKNOWN, CREATED_FROM_ENTITY, CREATED_FROM_DATABASE])
    internal annotation class CreatedFrom()

    /**
     * For backward compatibility with dbs created with older versions.
     */
    @SuppressWarnings("unused")
    constructor(
        name: String,
        columns: Map<String, Column>,
        foreignKeys: Set<ForeignKey>
    ) : this(name, columns, foreignKeys, emptySet<Index>())

    override fun equals(other: Any?): Boolean {
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

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + columns.hashCode()
        result = 31 * result + foreignKeys.hashCode()
        // skip index, it is not reliable for comparison.
        return result
    }

    override fun toString(): String {
        return ("TableInfo{name='$name', columns=$columns, foreignKeys=$foreignKeys, " +
            "indices=$indices}")
    }

    companion object {
        /**
         * Identifier for when the info is created from an unknown source.
         */
        const val CREATED_FROM_UNKNOWN = 0

        /**
         * Identifier for when the info is created from an entity definition, such as generated code
         * by the compiler or at runtime from a schema bundle, parsed from a schema JSON file.
         */
        const val CREATED_FROM_ENTITY = 1

        /**
         * Identifier for when the info is created from the database itself, reading information
         * from a PRAGMA, such as table_info.
         */
        const val CREATED_FROM_DATABASE = 2

        /**
         * Reads the table information from the given database.
         *
         * @param database  The database to read the information from.
         * @param tableName The table name.
         * @return A TableInfo containing the schema information for the provided table name.
         */
        @JvmStatic
        fun read(database: SupportSQLiteDatabase, tableName: String): TableInfo {
            return readTableInfo(
                database = database,
                tableName = tableName
            )
        }
    }

    /**
     * Holds the information about a database column.
     */
    class Column(
        /**
         * The column name.
         */
        @JvmField
        val name: String,
        /**
         * The column type affinity.
         */
        @JvmField
        val type: String,
        /**
         * Whether or not the column can be NULL.
         */
        @JvmField
        val notNull: Boolean,
        @JvmField
        val primaryKeyPosition: Int,
        @JvmField
        val defaultValue: String?,
        @CreatedFrom
        @JvmField
        val createdFrom: Int
    ) {
        /**
         * The column type after it is normalized to one of the basic types according to
         * https://www.sqlite.org/datatype3.html Section 3.1.
         *
         *
         * This is the value Room uses for equality check.
         */
        @SQLiteTypeAffinity
        @JvmField
        val affinity: Int = findAffinity(type)

        @Deprecated("Use {@link Column#Column(String, String, boolean, int, String, int)} instead.")
        constructor(name: String, type: String, notNull: Boolean, primaryKeyPosition: Int) : this(
            name,
            type,
            notNull,
            primaryKeyPosition,
            null,
            CREATED_FROM_UNKNOWN
        )

        /**
         * Implements https://www.sqlite.org/datatype3.html section 3.1
         *
         * @param type The type that was given to the sqlite
         * @return The normalized type which is one of the 5 known affinities
         */
        @SQLiteTypeAffinity
        private fun findAffinity(type: String?): Int {
            if (type == null) {
                return ColumnInfo.BLOB
            }
            val uppercaseType = type.uppercase(Locale.US)
            if (uppercaseType.contains("INT")) {
                return ColumnInfo.INTEGER
            }
            if (uppercaseType.contains("CHAR") ||
                uppercaseType.contains("CLOB") ||
                uppercaseType.contains("TEXT")
            ) {
                return ColumnInfo.TEXT
            }
            if (uppercaseType.contains("BLOB")) {
                return ColumnInfo.BLOB
            }
            if (uppercaseType.contains("REAL") ||
                uppercaseType.contains("FLOA") ||
                uppercaseType.contains("DOUB")
            ) {
                return ColumnInfo.REAL
            }
            // sqlite returns NUMERIC here but it is like a catch all. We already
            // have UNDEFINED so it is better to use UNDEFINED for consistency.
            return ColumnInfo.UNDEFINED
        }

        companion object {
            /**
             * Checks if the default values provided match. Handles the special case in which the
             * default value is surrounded by parenthesis (e.g. encountered in b/182284899).
             *
             * Surrounding parenthesis are removed by SQLite when reading from the database, hence
             * this function will check if they are present in the actual value, if so, it will
             * compare the two values by ignoring the surrounding parenthesis.
             *
             */
            @SuppressLint("SyntheticAccessor")
            @VisibleForTesting
            @JvmStatic
            fun defaultValueEquals(current: String, other: String?): Boolean {
                if (current == other) {
                    return true
                } else if (containsSurroundingParenthesis(current)) {
                    return current.substring(1, current.length - 1).trim() == other
                }
                return false
            }

            /**
             * Checks for potential surrounding parenthesis, if found, removes them and checks if
             * remaining paranthesis are balanced. If so, the surrounding parenthesis are redundant,
             * and returns true.
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
        }

        // TODO: problem probably here
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Column) return false
            if (Build.VERSION.SDK_INT >= 20) {
                if (primaryKeyPosition != other.primaryKeyPosition) return false
            } else {
                if (isPrimaryKey != other.isPrimaryKey) return false
            }
            if (name != other.name) return false
            if (notNull != other.notNull) return false
            // Only validate default value if it was defined in an entity, i.e. if the info
            // from the compiler itself has it. b/136019383
            if (
                createdFrom == CREATED_FROM_ENTITY &&
                    other.createdFrom == CREATED_FROM_DATABASE &&
                    defaultValue != null &&
                    !defaultValueEquals(defaultValue, other.defaultValue)
            ) {
                return false
            } else if (
                createdFrom == CREATED_FROM_DATABASE &&
                other.createdFrom == CREATED_FROM_ENTITY &&
                other.defaultValue != null &&
                !defaultValueEquals(other.defaultValue, defaultValue)
            ) {
                return false
            } else if (
                createdFrom != CREATED_FROM_UNKNOWN &&
                createdFrom == other.createdFrom &&
                (if (defaultValue != null)
                    !defaultValueEquals(defaultValue, other.defaultValue)
                else other.defaultValue != null)
            ) {
                return false
            }
            return affinity == other.affinity
        }

        /**
         * Returns whether this column is part of the primary key or not.
         *
         * @return True if this column is part of the primary key, false otherwise.
         */
        val isPrimaryKey: Boolean
            get() = primaryKeyPosition > 0

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + affinity
            result = 31 * result + if (notNull) 1231 else 1237
            result = 31 * result + primaryKeyPosition
            // Default value is not part of the hashcode since we conditionally check it for
            // equality which would break the equals + hashcode contract.
            // result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
            return result
        }

        override fun toString(): String {
            return ("Column{name='$name', type='$type', affinity='$affinity', " +
                "notNull=notNull, primaryKeyPosition=$primaryKeyPosition, " +
                "defaultValue='$defaultValue'}")
        }
    }

    /**
     * Holds the information about an SQLite foreign key
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    class ForeignKey(
        @JvmField
        val referenceTable: String,
        @JvmField
        val onDelete: String,
        @JvmField
        val onUpdate: String,
        @JvmField
        val columnNames: List<String>,
        @JvmField
        val referenceColumnNames: List<String>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ForeignKey) return false
            if (referenceTable != other.referenceTable) return false
            if (onDelete != other.onDelete) return false
            if (onUpdate != other.onUpdate) return false
            return if (columnNames != other.columnNames) false else referenceColumnNames ==
                other.referenceColumnNames
        }

        override fun hashCode(): Int {
            var result = referenceTable.hashCode()
            result = 31 * result + onDelete.hashCode()
            result = 31 * result + onUpdate.hashCode()
            result = 31 * result + columnNames.hashCode()
            result = 31 * result + referenceColumnNames.hashCode()
            return result
        }

        override fun toString(): String {
            return ("ForeignKey{referenceTable='$referenceTable', onDelete='$onDelete +', " +
                "onUpdate='$onUpdate', columnNames=$columnNames, " +
                "referenceColumnNames=$referenceColumnNames}")
        }
    }

    /**
     * Temporary data holder for a foreign key row in the pragma result. We need this to ensure
     * sorting in the generated foreign key object.
     */
    internal class ForeignKeyWithSequence(
        val id: Int,
        val sequence: Int,
        val from: String,
        val to: String
    ) : Comparable<ForeignKeyWithSequence> {
        override fun compareTo(other: ForeignKeyWithSequence): Int {
            val idCmp = id - other.id
            return if (idCmp == 0) {
                sequence - other.sequence
            } else {
                idCmp
            }
        }
    }

    /**
     * Holds the information about an SQLite index
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    class Index(
        @JvmField
        val name: String,
        @JvmField
        val unique: Boolean,
        @JvmField
        val columns: List<String>,
        @JvmField
        var orders: List<String>
    ) {
        init {
            orders = orders.ifEmpty {
                List(columns.size) { androidx.room.Index.Order.ASC.name }
            }
        }

        companion object {
            // should match the value in Index.kt
            const val DEFAULT_PREFIX = "index_"
        }

        @Deprecated("Use {@link #Index(String, boolean, List, List)}")
        constructor(name: String, unique: Boolean, columns: List<String>) : this(
            name,
            unique,
            columns,
            List<String>(columns.size) { androidx.room.Index.Order.ASC.name }
        )

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
            var result = if (name.startsWith(DEFAULT_PREFIX)) {
                DEFAULT_PREFIX.hashCode()
            } else {
                name.hashCode()
            }
            result = 31 * result + if (unique) 1 else 0
            result = 31 * result + columns.hashCode()
            result = 31 * result + orders.hashCode()
            return result
        }

        override fun toString(): String {
            return ("Index{name='$name', unique=$unique, columns=$columns, orders=$orders'}")
        }
    }
}

internal fun readTableInfo(database: SupportSQLiteDatabase, tableName: String): TableInfo {
    val columns = readColumns(database, tableName)
    val foreignKeys = readForeignKeys(database, tableName)
    val indices = readIndices(database, tableName)
    return TableInfo(tableName, columns, foreignKeys, indices)
}

private fun readForeignKeys(
    database: SupportSQLiteDatabase,
    tableName: String
): Set<TableInfo.ForeignKey> {
    // this seems to return everything in order but it is not documented so better be safe
    database.query("PRAGMA foreign_key_list(`$tableName`)").useCursor { cursor ->
        val idColumnIndex = cursor.getColumnIndex("id")
        val seqColumnIndex = cursor.getColumnIndex("seq")
        val tableColumnIndex = cursor.getColumnIndex("table")
        val onDeleteColumnIndex = cursor.getColumnIndex("on_delete")
        val onUpdateColumnIndex = cursor.getColumnIndex("on_update")
        val ordered = readForeignKeyFieldMappings(cursor)

        // Reset cursor as readForeignKeyFieldMappings has moved it
        cursor.moveToPosition(-1)
        return buildSet {
            while (cursor.moveToNext()) {
                val seq = cursor.getInt(seqColumnIndex)
                if (seq != 0) {
                    continue
                }
                val id = cursor.getInt(idColumnIndex)
                val myColumns = mutableListOf<String>()
                val refColumns = mutableListOf<String>()

                ordered.filter {
                    it.id == id
                }.forEach { key ->
                    myColumns.add(key.from)
                    refColumns.add(key.to)
                }

                add(
                    TableInfo.ForeignKey(
                        referenceTable = cursor.getString(tableColumnIndex),
                        onDelete = cursor.getString(onDeleteColumnIndex),
                        onUpdate = cursor.getString(onUpdateColumnIndex),
                        columnNames = myColumns,
                        referenceColumnNames = refColumns
                    )
                )
            }
        }
    }
}

private fun readForeignKeyFieldMappings(cursor: Cursor): List<TableInfo.ForeignKeyWithSequence> {
    val idColumnIndex = cursor.getColumnIndex("id")
    val seqColumnIndex = cursor.getColumnIndex("seq")
    val fromColumnIndex = cursor.getColumnIndex("from")
    val toColumnIndex = cursor.getColumnIndex("to")

    return buildList {
        while (cursor.moveToNext()) {
            add(
                TableInfo.ForeignKeyWithSequence(
                    id = cursor.getInt(idColumnIndex),
                    sequence = cursor.getInt(seqColumnIndex),
                    from = cursor.getString(fromColumnIndex),
                    to = cursor.getString(toColumnIndex)
                )
            )
        }
    }.sorted()
}

private fun readColumns(
    database: SupportSQLiteDatabase,
    tableName: String
): Map<String, TableInfo.Column> {
    database.query("PRAGMA table_info(`$tableName`)").useCursor { cursor ->
        if (cursor.columnCount <= 0) {
            return emptyMap()
        }

        val nameIndex = cursor.getColumnIndex("name")
        val typeIndex = cursor.getColumnIndex("type")
        val notNullIndex = cursor.getColumnIndex("notnull")
        val pkIndex = cursor.getColumnIndex("pk")
        val defaultValueIndex = cursor.getColumnIndex("dflt_value")

        return buildMap {
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val type = cursor.getString(typeIndex)
                val notNull = 0 != cursor.getInt(notNullIndex)
                val primaryKeyPosition = cursor.getInt(pkIndex)
                val defaultValue = cursor.getString(defaultValueIndex)
                put(
                    key = name,
                    value = TableInfo.Column(
                        name = name,
                        type = type,
                        notNull = notNull,
                        primaryKeyPosition = primaryKeyPosition,
                        defaultValue = defaultValue,
                        createdFrom = TableInfo.CREATED_FROM_DATABASE
                    )
                )
            }
        }
    }
}

/**
 * @return null if we cannot read the indices due to older sqlite implementations.
 */
private fun readIndices(database: SupportSQLiteDatabase, tableName: String): Set<TableInfo.Index>? {
    database.query("PRAGMA index_list(`$tableName`)").useCursor { cursor ->
        val nameColumnIndex = cursor.getColumnIndex("name")
        val originColumnIndex = cursor.getColumnIndex("origin")
        val uniqueIndex = cursor.getColumnIndex("unique")
        if (nameColumnIndex == -1 || originColumnIndex == -1 || uniqueIndex == -1) {
            // we cannot read them so better not validate any index.
            return null
        }
        return buildSet {
            while (cursor.moveToNext()) {
                val origin = cursor.getString(originColumnIndex)
                if ("c" != origin) {
                    // Ignore auto-created indices
                    continue
                }
                val name = cursor.getString(nameColumnIndex)
                val unique = cursor.getInt(uniqueIndex) == 1
                // Read index but if we cannot read it properly so better not read it
                val index = readIndex(database, name, unique) ?: return null
                add(index)
            }
        }
    }
}

/**
 * @return null if we cannot read the index due to older sqlite implementations.
 */
private fun readIndex(
    database: SupportSQLiteDatabase,
    name: String,
    unique: Boolean
): TableInfo.Index? {
    return database.query("PRAGMA index_xinfo(`$name`)").useCursor { cursor ->
        val seqnoColumnIndex = cursor.getColumnIndex("seqno")
        val cidColumnIndex = cursor.getColumnIndex("cid")
        val nameColumnIndex = cursor.getColumnIndex("name")
        val descColumnIndex = cursor.getColumnIndex("desc")
        if (
            seqnoColumnIndex == -1 ||
            cidColumnIndex == -1 ||
            nameColumnIndex == -1 ||
            descColumnIndex == -1
        ) {
            // we cannot read them so better not validate any index.
            return null
        }
        val columnsMap = TreeMap<Int, String>()
        val ordersMap = TreeMap<Int, String>()
        while (cursor.moveToNext()) {
            val cid = cursor.getInt(cidColumnIndex)
            if (cid < 0) {
                // Ignore SQLite row ID
                continue
            }
            val seq = cursor.getInt(seqnoColumnIndex)
            val columnName = cursor.getString(nameColumnIndex)
            val order = if (cursor.getInt(descColumnIndex) > 0) "DESC" else "ASC"
            columnsMap[seq] = columnName
            ordersMap[seq] = order
        }
        val columns = columnsMap.values.toList()
        val orders = ordersMap.values.toList()
        TableInfo.Index(name, unique, columns, orders)
    }
}