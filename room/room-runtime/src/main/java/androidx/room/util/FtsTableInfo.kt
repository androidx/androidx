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
import androidx.annotation.VisibleForTesting
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.ArrayDeque

/**
 * A data class that holds the information about an FTS table.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class FtsTableInfo(
    /**
     * The table name
     */
    @JvmField
    val name: String,

    /**
     * The column names
     */
    @JvmField
    val columns: Set<String>,

    /**
     * The set of options. Each value in the set contains the option in the following format:
     * <key, value>.
     */
    @JvmField
    val options: Set<String>
) {
    constructor(name: String, columns: Set<String>, createSql: String) :
        this(name, columns, parseOptions(createSql))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FtsTableInfo) return false
        val that = other
        if (name != that.name) return false
        if (columns != that.columns) return false
        return options == that.options
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (columns.hashCode())
        result = 31 * result + (options.hashCode())
        return result
    }

    override fun toString(): String {
        return ("FtsTableInfo{name='$name', columns=$columns, options=$options'}")
    }

    companion object {
        // A set of valid FTS Options
        private val FTS_OPTIONS = arrayOf(
            "tokenize=", "compress=", "content=", "languageid=", "matchinfo=", "notindexed=",
            "order=", "prefix=", "uncompress="
        )

        /**
         * Reads the table information from the given database.
         *
         * @param database  The database to read the information from.
         * @param tableName The table name.
         * @return A FtsTableInfo containing the columns and options for the provided table name.
         */
        @JvmStatic
        fun read(database: SupportSQLiteDatabase, tableName: String): FtsTableInfo {
            val columns = readColumns(database, tableName)
            val options = readOptions(database, tableName)
            return FtsTableInfo(tableName, columns, options)
        }

        private fun readColumns(database: SupportSQLiteDatabase, tableName: String): Set<String> {
            return buildSet {
                database.query("PRAGMA table_info(`$tableName`)").useCursor { cursor ->
                    if (cursor.columnCount > 0) {
                        val nameIndex = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            add(cursor.getString(nameIndex))
                        }
                    }
                }
            }
        }

        private fun readOptions(database: SupportSQLiteDatabase, tableName: String): Set<String> {
            val sql = database.query(
                "SELECT * FROM sqlite_master WHERE `name` = '$tableName'"
            ).useCursor { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow("sql"))
                } else {
                    ""
                }
            }
            return parseOptions(sql)
        }

        /**
         * Parses FTS options from the create statement of an FTS table.
         *
         * This method assumes the given create statement is a valid well-formed SQLite statement as
         * defined in the [CREATE VIRTUAL TABLE
         * syntax diagram](https://www.sqlite.org/lang_createvtab.html).
         *
         * @param createStatement the "CREATE VIRTUAL TABLE" statement.
         * @return the set of FTS option key and values in the create statement.
         */
        @VisibleForTesting
        @JvmStatic
        fun parseOptions(createStatement: String): Set<String> {
            if (createStatement.isEmpty()) {
                return emptySet()
            }

            // Module arguments are within the parenthesis followed by the module name.
            val argsString = createStatement.substring(
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
                    '\'', '"', '`' ->
                        if (quoteStack.isEmpty()) {
                            quoteStack.push(value)
                        } else if (quoteStack.peek() == value) {
                            quoteStack.pop()
                        }
                    '[' -> if (quoteStack.isEmpty()) {
                        quoteStack.push(value)
                    }
                    ']' -> if (!quoteStack.isEmpty() && quoteStack.peek() == '[') {
                        quoteStack.pop()
                    }
                    ',' -> if (quoteStack.isEmpty()) {
                        args.add(argsString.substring(lastDelimiterIndex + 1, i).trim { it <= ' ' })
                        lastDelimiterIndex = i
                    }
                }
            }

            // Add final argument.
            args.add(argsString.substring(lastDelimiterIndex + 1).trim())

            // Match args against valid options, otherwise they are column definitions.
            val options = args.filter { arg ->
                FTS_OPTIONS.any { validOption ->
                    arg.startsWith(validOption)
                }
            }.toSet()
            return options
        }
    }
}
