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
package androidx.sqlite.db

/**
 * A basic implementation of [SupportSQLiteQuery] which receives a query and its args and binds args
 * based on the passed in Object type.
 *
 * @param query The query string, can include bind arguments (.e.g ?).
 * @param bindArgs The bind argument value that will replace the placeholders in the query.
 * @constructor Creates an SQL query with the sql string and the bind arguments.
 */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public class SimpleSQLiteQuery(
    private val query: String,
    @Suppress("ArrayReturn") // Due to legacy API
    private val bindArgs: Array<out Any?>?
) : SupportSQLiteQuery {

    /**
     * Creates an SQL query without any bind arguments.
     *
     * @param query The SQL query to execute. Cannot include bind parameters.
     */
    public constructor(query: String) : this(query, null)

    override val sql: String
        get() = this.query

    /**
     * Creates an SQL query without any bind arguments.
     *
     * @param [statement] The SQL query to execute. Cannot include bind parameters.
     */
    @Suppress("AcronymName") // SQL is a known term and should remain capitalized
    override fun bindTo(statement: SupportSQLiteProgram) {
        bind(statement, bindArgs)
    }

    override val argCount: Int
        get() = bindArgs?.size ?: 0

    public companion object {
        /**
         * Binds the given arguments into the given sqlite statement.
         *
         * @param [statement] The sqlite statement
         * @param [bindArgs] The list of bind arguments
         */
        @JvmStatic
        public fun bind(
            @Suppress("AcronymName") // SQL is a known term and should remain capitalized
            statement: SupportSQLiteProgram,
            @Suppress("ArrayReturn") // Due to legacy API
            bindArgs: Array<out Any?>?
        ) {
            if (bindArgs == null) {
                return
            }

            val limit = bindArgs.size
            for (i in 0 until limit) {
                val arg = bindArgs[i]
                bind(statement, i + 1, arg)
            }
        }

        private fun bind(
            @Suppress("AcronymName") // SQL is a known term and should remain capitalized
            statement: SupportSQLiteProgram,
            index: Int,
            arg: Any?
        ) {
            // extracted from android.database.sqlite.SQLiteConnection
            if (arg == null) {
                statement.bindNull(index)
            } else if (arg is ByteArray) {
                statement.bindBlob(index, arg)
            } else if (arg is Float) {
                statement.bindDouble(index, arg.toDouble())
            } else if (arg is Double) {
                statement.bindDouble(index, arg)
            } else if (arg is Long) {
                statement.bindLong(index, arg)
            } else if (arg is Int) {
                statement.bindLong(index, arg.toLong())
            } else if (arg is Short) {
                statement.bindLong(index, arg.toLong())
            } else if (arg is Byte) {
                statement.bindLong(index, arg.toLong())
            } else if (arg is String) {
                statement.bindString(index, arg)
            } else if (arg is Boolean) {
                statement.bindLong(index, if (arg) 1 else 0)
            } else {
                throw IllegalArgumentException(
                    "Cannot bind $arg at index $index Supported types: Null, ByteArray, " +
                        "Float, Double, Long, Int, Short, Byte, String"
                )
            }
        }
    }
}
