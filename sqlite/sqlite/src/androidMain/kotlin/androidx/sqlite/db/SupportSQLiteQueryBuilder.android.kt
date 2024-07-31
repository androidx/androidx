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

import java.util.regex.Pattern

/** A simple query builder to create SQL SELECT queries. */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public class SupportSQLiteQueryBuilder private constructor(private val table: String) {
    private var distinct = false
    private var columns: Array<out String>? = null
    private var selection: String? = null
    private var bindArgs: Array<out Any?>? = null
    private var groupBy: String? = null
    private var having: String? = null
    private var orderBy: String? = null
    private var limit: String? = null

    /**
     * Adds DISTINCT keyword to the query.
     *
     * @return this
     */
    public fun distinct(): SupportSQLiteQueryBuilder = apply { this.distinct = true }

    /**
     * Sets the given list of columns as the columns that will be returned.
     *
     * @param columns The list of column names that should be returned.
     * @return this
     */
    public fun columns(columns: Array<out String>?): SupportSQLiteQueryBuilder = apply {
        this.columns = columns
    }

    /**
     * Sets the arguments for the WHERE clause.
     *
     * @param selection The list of selection columns
     * @param bindArgs The list of bind arguments to match against these columns
     * @return this
     */
    public fun selection(
        selection: String?,
        bindArgs: Array<out Any?>?
    ): SupportSQLiteQueryBuilder = apply {
        this.selection = selection
        this.bindArgs = bindArgs
    }

    /**
     * Adds a GROUP BY statement.
     *
     * @param groupBy The value of the GROUP BY statement.
     * @return this
     */
    public fun groupBy(groupBy: String?): SupportSQLiteQueryBuilder = apply {
        this.groupBy = groupBy
    }

    /**
     * Adds a HAVING statement. You must also provide [groupBy] for this to work.
     *
     * @param having The having clause.
     * @return this
     */
    public fun having(having: String?): SupportSQLiteQueryBuilder = apply { this.having = having }

    /**
     * Adds an ORDER BY statement.
     *
     * @param orderBy The order clause.
     * @return this
     */
    public fun orderBy(orderBy: String?): SupportSQLiteQueryBuilder = apply {
        this.orderBy = orderBy
    }

    /**
     * Adds a LIMIT statement.
     *
     * @param limit The limit value.
     * @return this
     */
    public fun limit(limit: String): SupportSQLiteQueryBuilder = apply {
        val patternMatches = limitPattern.matcher(limit).matches()
        require(limit.isEmpty() || patternMatches) { "invalid LIMIT clauses:$limit" }
        this.limit = limit
    }

    /**
     * Creates the [SupportSQLiteQuery] that can be passed into [SupportSQLiteDatabase.query].
     *
     * @return a new query
     */
    public fun create(): SupportSQLiteQuery {
        require(!groupBy.isNullOrEmpty() || having.isNullOrEmpty()) {
            "HAVING clauses are only permitted when using a groupBy clause"
        }
        val query =
            buildString(120) {
                append("SELECT ")
                if (distinct) {
                    append("DISTINCT ")
                }
                if (!columns.isNullOrEmpty()) {
                    appendColumns(columns!!)
                } else {
                    append("* ")
                }
                append("FROM ")
                append(table)
                appendClause(" WHERE ", selection)
                appendClause(" GROUP BY ", groupBy)
                appendClause(" HAVING ", having)
                appendClause(" ORDER BY ", orderBy)
                appendClause(" LIMIT ", limit)
            }
        return SimpleSQLiteQuery(query, bindArgs)
    }

    private fun StringBuilder.appendClause(name: String, clause: String?) {
        if (!clause.isNullOrEmpty()) {
            append(name)
            append(clause)
        }
    }

    /** Add the names that are non-null in columns to string, separating them with commas. */
    private fun StringBuilder.appendColumns(columns: Array<out String>) {
        val n = columns.size
        for (i in 0 until n) {
            val column = columns[i]
            if (i > 0) {
                append(", ")
            }
            append(column)
        }
        append(' ')
    }

    public companion object {
        private val limitPattern = Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?")

        /**
         * Creates a query for the given table name.
         *
         * @param tableName The table name(s) to query.
         * @return A builder to create a query.
         */
        @JvmStatic
        public fun builder(tableName: String): SupportSQLiteQueryBuilder {
            return SupportSQLiteQueryBuilder(tableName)
        }
    }
}
