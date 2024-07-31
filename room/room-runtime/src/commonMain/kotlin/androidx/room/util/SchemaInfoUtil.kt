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

import androidx.room.ColumnInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.use
import kotlin.collections.removeLast as removeLastKt

/**
 * Implements https://www.sqlite.org/datatype3.html section 3.1
 *
 * @param type The type that was given to the sqlite
 * @return The normalized type which is one of the 5 known affinities
 */
@ColumnInfo.SQLiteTypeAffinity
internal fun findAffinity(type: String?): Int {
    if (type == null) {
        return ColumnInfo.BLOB
    }
    val uppercaseType = type.uppercase()
    if (uppercaseType.contains("INT")) {
        return ColumnInfo.INTEGER
    }
    if (
        uppercaseType.contains("CHAR") ||
            uppercaseType.contains("CLOB") ||
            uppercaseType.contains("TEXT")
    ) {
        return ColumnInfo.TEXT
    }
    if (uppercaseType.contains("BLOB")) {
        return ColumnInfo.BLOB
    }
    if (
        uppercaseType.contains("REAL") ||
            uppercaseType.contains("FLOA") ||
            uppercaseType.contains("DOUB")
    ) {
        return ColumnInfo.REAL
    }
    // SQLite returns NUMERIC here but it is like a catch all. We already
    // have UNDEFINED so it is better to use UNDEFINED for consistency.
    return ColumnInfo.UNDEFINED
}

internal fun readTableInfo(connection: SQLiteConnection, tableName: String): TableInfo {
    val columns = readColumns(connection, tableName)
    val foreignKeys = readForeignKeys(connection, tableName)
    val indices = readIndices(connection, tableName)
    return TableInfo(tableName, columns, foreignKeys, indices)
}

private fun readForeignKeys(
    connection: SQLiteConnection,
    tableName: String
): Set<TableInfo.ForeignKey> {
    // this seems to return everything in order but it is not documented so better be safe
    connection.prepare("PRAGMA foreign_key_list(`$tableName`)").use { stmt ->
        val idColumnIndex = stmt.columnIndexOf("id")
        val seqColumnIndex = stmt.columnIndexOf("seq")
        val tableColumnIndex = stmt.columnIndexOf("table")
        val onDeleteColumnIndex = stmt.columnIndexOf("on_delete")
        val onUpdateColumnIndex = stmt.columnIndexOf("on_update")
        val ordered = readForeignKeyFieldMappings(stmt)

        // Reset cursor as readForeignKeyFieldMappings has moved it
        stmt.reset()
        return buildSet {
            while (stmt.step()) {
                val seq = stmt.getLong(seqColumnIndex)
                if (seq != 0L) {
                    continue
                }
                val id = stmt.getLong(idColumnIndex).toInt()
                val myColumns = mutableListOf<String>()
                val refColumns = mutableListOf<String>()

                ordered
                    .filter { it.id == id }
                    .forEach { key ->
                        myColumns.add(key.from)
                        refColumns.add(key.to)
                    }

                add(
                    TableInfo.ForeignKey(
                        referenceTable = stmt.getText(tableColumnIndex),
                        onDelete = stmt.getText(onDeleteColumnIndex),
                        onUpdate = stmt.getText(onUpdateColumnIndex),
                        columnNames = myColumns,
                        referenceColumnNames = refColumns
                    )
                )
            }
        }
    }
}

/**
 * Temporary data holder for a foreign key row in the pragma result. We need this to ensure sorting
 * in the generated foreign key object.
 */
private class ForeignKeyWithSequence(
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

private fun readForeignKeyFieldMappings(stmt: SQLiteStatement): List<ForeignKeyWithSequence> {
    val idColumnIndex = stmt.columnIndexOf("id")
    val seqColumnIndex = stmt.columnIndexOf("seq")
    val fromColumnIndex = stmt.columnIndexOf("from")
    val toColumnIndex = stmt.columnIndexOf("to")

    return buildList {
            while (stmt.step()) {
                add(
                    ForeignKeyWithSequence(
                        id = stmt.getLong(idColumnIndex).toInt(),
                        sequence = stmt.getLong(seqColumnIndex).toInt(),
                        from = stmt.getText(fromColumnIndex),
                        to = stmt.getText(toColumnIndex)
                    )
                )
            }
        }
        .sorted()
}

private fun readColumns(
    connection: SQLiteConnection,
    tableName: String
): Map<String, TableInfo.Column> {
    connection.prepare("PRAGMA table_info(`$tableName`)").use { stmt ->
        if (!stmt.step()) {
            return emptyMap()
        }

        val nameIndex = stmt.columnIndexOf("name")
        val typeIndex = stmt.columnIndexOf("type")
        val notNullIndex = stmt.columnIndexOf("notnull")
        val pkIndex = stmt.columnIndexOf("pk")
        val defaultValueIndex = stmt.columnIndexOf("dflt_value")

        return buildMap {
            do {
                val name = stmt.getText(nameIndex)
                val type = stmt.getText(typeIndex)
                val notNull = stmt.getLong(notNullIndex) != 0L
                val primaryKeyPosition = stmt.getLong(pkIndex).toInt()
                val defaultValue =
                    if (stmt.isNull(defaultValueIndex)) null else stmt.getText(defaultValueIndex)
                put(
                    key = name,
                    value =
                        TableInfo.Column(
                            name = name,
                            type = type,
                            notNull = notNull,
                            primaryKeyPosition = primaryKeyPosition,
                            defaultValue = defaultValue,
                            createdFrom = TableInfo.CREATED_FROM_DATABASE
                        )
                )
            } while (stmt.step())
        }
    }
}

/** @return null if we cannot read the indices due to older sqlite implementations. */
private fun readIndices(connection: SQLiteConnection, tableName: String): Set<TableInfo.Index>? {
    connection.prepare("PRAGMA index_list(`$tableName`)").use { stmt ->
        val nameColumnIndex = stmt.columnIndexOf("name")
        val originColumnIndex = stmt.columnIndexOf("origin")
        val uniqueIndex = stmt.columnIndexOf("unique")
        if (nameColumnIndex == -1 || originColumnIndex == -1 || uniqueIndex == -1) {
            // we cannot read them so better not validate any index.
            return null
        }
        return buildSet {
            while (stmt.step()) {
                val origin = stmt.getText(originColumnIndex)
                if ("c" != origin) {
                    // Ignore auto-created indices
                    continue
                }
                val name = stmt.getText(nameColumnIndex)
                val unique = stmt.getLong(uniqueIndex) == 1L
                // Read index but if we cannot read it properly so better not read it
                val index = readIndex(connection, name, unique) ?: return null
                add(index)
            }
        }
    }
}

/** @return null if we cannot read the index due to older sqlite implementations. */
private fun readIndex(
    connection: SQLiteConnection,
    name: String,
    unique: Boolean
): TableInfo.Index? {
    return connection.prepare("PRAGMA index_xinfo(`$name`)").use { stmt ->
        val seqnoColumnIndex = stmt.columnIndexOf("seqno")
        val cidColumnIndex = stmt.columnIndexOf("cid")
        val nameColumnIndex = stmt.columnIndexOf("name")
        val descColumnIndex = stmt.columnIndexOf("desc")
        if (
            seqnoColumnIndex == -1 ||
                cidColumnIndex == -1 ||
                nameColumnIndex == -1 ||
                descColumnIndex == -1
        ) {
            // we cannot read them so better not validate any index.
            return null
        }
        val columnsMap = mutableMapOf<Int, String>()
        val ordersMap = mutableMapOf<Int, String>()
        while (stmt.step()) {
            val cid = stmt.getLong(cidColumnIndex).toInt()
            if (cid < 0) {
                // Ignore SQLite row ID
                continue
            }
            val seq = stmt.getLong(seqnoColumnIndex).toInt()
            val columnName = stmt.getText(nameColumnIndex)
            val order = if (stmt.getLong(descColumnIndex) > 0) "DESC" else "ASC"
            columnsMap[seq] = columnName
            ordersMap[seq] = order
        }
        val columns = columnsMap.entries.sortedBy { it.key }.map { it.value }.toList()
        val orders = ordersMap.entries.sortedBy { it.key }.map { it.value }.toList()
        TableInfo.Index(name, unique, columns, orders)
    }
}

internal fun readFtsColumns(connection: SQLiteConnection, tableName: String): Set<String> {
    return buildSet {
        connection.prepare("PRAGMA table_info(`$tableName`)").use { stmt ->
            if (!stmt.step()) return@use
            val nameIndex = stmt.columnIndexOf("name")
            do {
                add(stmt.getText(nameIndex))
            } while (stmt.step())
        }
    }
}

internal fun readFtsOptions(connection: SQLiteConnection, tableName: String): Set<String> {
    val sql =
        connection.prepare("SELECT * FROM sqlite_master WHERE `name` = '$tableName'").use { stmt ->
            if (stmt.step()) {
                stmt.getText(stmt.columnIndexOf("sql"))
            } else {
                ""
            }
        }
    return parseFtsOptions(sql)
}

// A set of valid FTS Options
private val FTS_OPTIONS =
    arrayOf(
        "tokenize=",
        "compress=",
        "content=",
        "languageid=",
        "matchinfo=",
        "notindexed=",
        "order=",
        "prefix=",
        "uncompress="
    )

/**
 * Parses FTS options from the create statement of an FTS table.
 *
 * This method assumes the given create statement is a valid well-formed SQLite statement as defined
 * in the [CREATE VIRTUAL TABLE syntax diagram](https://www.sqlite.org/lang_createvtab.html).
 *
 * @param createStatement the "CREATE VIRTUAL TABLE" statement.
 * @return the set of FTS option key and values in the create statement.
 */
internal fun parseFtsOptions(createStatement: String): Set<String> {
    if (createStatement.isEmpty()) {
        return emptySet()
    }

    // Module arguments are within the parenthesis followed by the module name.
    val argsString =
        createStatement.substring(
            createStatement.indexOf('(') + 1,
            createStatement.lastIndexOf(')')
        )

    // Split the module argument string by the comma delimiter, keeping track of quotation
    // so that if the delimiter is found within a string literal we don't substring at the
    // wrong index. SQLite supports four ways of quoting keywords, see:
    // https://www.sqlite.org/lang_keywords.html
    val args = mutableListOf<String>()
    val quoteStack = ArrayDeque<Char>()
    var lastDelimiterIndex = -1
    argsString.forEachIndexed { i, value ->
        when (value) {
            '\'',
            '"',
            '`' ->
                if (quoteStack.isEmpty()) {
                    quoteStack.addFirst(value)
                } else if (quoteStack.firstOrNull() == value) {
                    quoteStack.removeLastKt()
                }
            '[' ->
                if (quoteStack.isEmpty()) {
                    quoteStack.addFirst(value)
                }
            ']' ->
                if (!quoteStack.isEmpty() && quoteStack.firstOrNull() == '[') {
                    quoteStack.removeLastKt()
                }
            ',' ->
                if (quoteStack.isEmpty()) {
                    args.add(argsString.substring(lastDelimiterIndex + 1, i).trim { it <= ' ' })
                    lastDelimiterIndex = i
                }
        }
    }

    // Add final argument.
    args.add(argsString.substring(lastDelimiterIndex + 1).trim())

    // Match args against valid options, otherwise they are column definitions.
    val options =
        args
            .filter { arg -> FTS_OPTIONS.any { validOption -> arg.startsWith(validOption) } }
            .toSet()
    return options
}

internal fun readViewInfo(connection: SQLiteConnection, viewName: String): ViewInfo {
    return connection
        .prepare(
            "SELECT name, sql FROM sqlite_master " + "WHERE type = 'view' AND name = '$viewName'"
        )
        .use { stmt ->
            if (stmt.step()) {
                ViewInfo(stmt.getText(0), stmt.getText(1))
            } else {
                ViewInfo(viewName, null)
            }
        }
}
