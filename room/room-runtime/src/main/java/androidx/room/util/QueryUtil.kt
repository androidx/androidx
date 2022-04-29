/*
 * Copyright 2022 The Android Open Source Project
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

/*
 * Copyright 2022 The Android Open Source Project
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
@file:JvmName("QueryUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.room.ParsedQuerySection
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Integer.min

/**
 * Prepares the query at runtime. Checks the number of query parameters and if it's larger than
 * [RoomDatabase.MAX_BIND_PARAMETER_CNT], then it uses a special logic to handle such big queries.
 *
 * @param db - a [RoomDatabase] instance
 * @param tempTable - the name of the temp table that will be created for large queries
 * @param sections - a list of [ParsedQuerySection]
 * @param throwForLargeQueries - if this parameter is true then the method will throw an exception
 * for large queries instead of preparing them
 *
 * @return a pair of [RoomSQLiteQuery] and a boolean flag where true means that this is a
 * large query (has more than [RoomDatabase.MAX_BIND_PARAMETER_CNT] parametrs, false otherwise.
 */
fun prepareStatement(
    db: RoomDatabase,
    tempTable: String,
    sections: List<ParsedQuerySection>,
    throwForLargeQueries: Boolean
): Pair<SupportSQLiteStatement, Boolean> {
    val bindVars = sections.filterIsInstance<ParsedQuerySection.BindVar>()

    val paramCount = bindVars.sumOf { it.parameterCount }

    val isLargeQuery = paramCount > RoomDatabase.MAX_BIND_PARAMETER_CNT

    if (isLargeQuery) {
        if (throwForLargeQueries) {
            throw java.lang.IllegalStateException(
                "Room doesn't support paging and Cursor returning queries with more " +
                    "than ${RoomDatabase.MAX_BIND_PARAMETER_CNT} parameters."
            )
        }
        return prepareLargeStatement(db, tempTable, sections)
    }

    val sql = newStringBuilder()
    sections.forEach { section ->
        when (section) {
            is ParsedQuerySection.Text -> sql.append(section.text)
            is ParsedQuerySection.BindVar -> appendPlaceholders(sql, section.parameterCount)
        }
    }

    val statement = db.compileStatement(sql.toString())
    var argIndex = 1

    bindVars.forEach { section ->
        val iterator = section.iterator
        if (iterator != null) {
            iterator.forEach { item ->
                SimpleSQLiteQuery.bind(statement, argIndex, item)
                argIndex++
            }
        } else {
            SimpleSQLiteQuery.bind(statement, argIndex, section.value)
            argIndex++
        }
    }

    return Pair(statement, false)
}

private fun prepareLargeStatement(
    db: RoomDatabase,
    tempTable: String,
    sections: List<ParsedQuerySection>
): Pair<SupportSQLiteStatement, Boolean> {
    val writableDB = db.openHelper.writableDatabase

    writableDB.execSQL("PRAGMA temp_store = MEMORY")
    writableDB.execSQL("CREATE TEMP TABLE $tempTable(value TEXT)")

    sections.filterIsInstance<ParsedQuerySection.BindVar>().forEach { section ->
        val iterator = section.iterator
        if (iterator != null) {
            insertIntoTempTable(writableDB, tempTable, iterator, section.parameterCount)
        } else {
            writableDB.execSQL("INSERT INTO $tempTable (value) VALUES(?)", arrayOf(section.value))
        }
    }

    val sql = newStringBuilder()
    var startRow = 1
    sections.forEach { section ->
        when (section) {
            is ParsedQuerySection.Text -> sql.append(section.text)
            is ParsedQuerySection.BindVar -> {
                if (section.isMultiple) {
                    sql.append("SELECT value FROM $tempTable WHERE _rowid_ >= ")
                    sql.append(startRow)
                    sql.append(" AND _rowid_ < ")
                    sql.append(startRow + section.parameterCount)
                    startRow += section.parameterCount
                } else {
                    sql.append("(SELECT value FROM $tempTable WHERE _rowid_ >= ")
                    sql.append(startRow)
                    sql.append(" AND _rowid_ < ")
                    sql.append(startRow + 1)
                    sql.append(")")
                    startRow += 1
                }
            }
        }
    }

    return Pair(db.compileStatement(sql.toString()), true)
}

/**
 * Prepares the query at runtime. Checks the number of query parameters and if it's larger than
 * [RoomDatabase.MAX_BIND_PARAMETER_CNT], then it uses a special logic to handle such big queries.
 *
 * @param db - a [RoomDatabase] instance
 * @param inTransaction - true if this method is being called within transaction, false otherwise
 * @param tempTable - the name of the temp table that will be created for large queries
 * @param sections - a list of [ParsedQuerySection]
 * @param throwForLargeQueries - if this parameter is true then the method will throw an exception
 * for large queries instead of preparing them
 *
 * @return a pair of [RoomSQLiteQuery] and a boolean flag where true means that this is a
 * large query (has more than [RoomDatabase.MAX_BIND_PARAMETER_CNT] parametrs, false otherwise.
 */
fun prepareQuery(
    db: RoomDatabase,
    inTransaction: Boolean,
    tempTable: String,
    sections: List<ParsedQuerySection>,
    throwForLargeQueries: Boolean
): Pair<RoomSQLiteQuery, Boolean> {
    val bindVars = sections.filterIsInstance<ParsedQuerySection.BindVar>()

    val paramCount = bindVars.sumOf { it.parameterCount }

    val isLargeQuery = paramCount > RoomDatabase.MAX_BIND_PARAMETER_CNT

    if (isLargeQuery) {
        if (throwForLargeQueries) {
            throw java.lang.IllegalStateException(
                "Room doesn't support paging and Cursor returning queries with more " +
                    "than ${RoomDatabase.MAX_BIND_PARAMETER_CNT} parameters."
            )
        }
        return prepareLargeQuery(db, inTransaction, tempTable, sections)
    }

    val sql = newStringBuilder()
    sections.forEach { section ->
        when (section) {
            is ParsedQuerySection.Text -> sql.append(section.text)
            is ParsedQuerySection.BindVar -> appendPlaceholders(sql, section.parameterCount)
        }
    }

    val statement = RoomSQLiteQuery.acquire(sql.toString(), paramCount)
    var argIndex = 1

    bindVars.forEach { section ->
        val iterator = section.iterator
        if (iterator != null) {
            iterator.forEach { item ->
                RoomSQLiteQuery.bind(statement, argIndex, item)
                argIndex++
            }
        } else {
            RoomSQLiteQuery.bind(statement, argIndex, section.value)
            argIndex++
        }
    }

    return Pair(statement, false)
}

private fun prepareLargeQuery(
    db: RoomDatabase,
    inTransaction: Boolean,
    tempTable: String,
    sections: List<ParsedQuerySection>
): Pair<RoomSQLiteQuery, Boolean> {
    val writableDB = db.openHelper.writableDatabase

    // Operations on temp table have to be performed inside a transaction so we have to begin a
    // transaction if we're not inside one already.
    if (!inTransaction) {
        db.beginTransaction()
    }

    writableDB.execSQL("PRAGMA temp_store = MEMORY")
    writableDB.execSQL("CREATE TEMP TABLE $tempTable(value TEXT)")

    sections.filterIsInstance<ParsedQuerySection.BindVar>().forEach { section ->
        val iterator = section.iterator
        if (iterator != null) {
            insertIntoTempTable(writableDB, tempTable, iterator, section.parameterCount)
        } else {
            writableDB.execSQL("INSERT INTO $tempTable (value) VALUES(?)", arrayOf(section.value))
        }
    }

    val sql = newStringBuilder()
    var startRow = 1
    sections.forEach { section ->
        when (section) {
            is ParsedQuerySection.Text -> sql.append(section.text)
            is ParsedQuerySection.BindVar -> {
                if (section.isMultiple) {
                    sql.append("SELECT value FROM $tempTable WHERE _rowid_ >= ")
                    sql.append(startRow)
                    sql.append(" AND _rowid_ < ")
                    sql.append(startRow + section.parameterCount)
                    startRow += section.parameterCount
                } else {
                    sql.append("(SELECT value FROM $tempTable WHERE _rowid_ >= ")
                    sql.append(startRow)
                    sql.append(" AND _rowid_ < ")
                    sql.append(startRow + 1)
                    sql.append(")")
                    startRow += 1
                }
            }
        }
    }

    return Pair(RoomSQLiteQuery.acquire(sql.toString(), 0), true)
}

/**
 * Safely inserts data into the temp table. Inserts the values in chunks if there are
 * more than [RoomDatabase.MAX_BIND_PARAMETER_CNT] parameters.
 */
private fun insertIntoTempTable(
    db: SupportSQLiteDatabase,
    tempTable: String,
    iterator: Iterator<*>,
    parameterCount: Int
) {
    val parts: MutableList<Any?> = ArrayList()

    var start = 0
    while (start < parameterCount) {
        val end = min(start + RoomDatabase.MAX_BIND_PARAMETER_CNT, parameterCount)
        val stringBuilder = newStringBuilder()
        stringBuilder.append("INSERT INTO $tempTable (value) VALUES ")
        val inputSize = end - start
        appendParenthesizedPlaceholders(stringBuilder, inputSize)
        while (iterator.hasNext() && parts.size < inputSize) {
            parts.add(iterator.next())
        }
        db.execSQL(stringBuilder.toString(), parts.toTypedArray())
        parts.clear()
        start += RoomDatabase.MAX_BIND_PARAMETER_CNT
    }
}
