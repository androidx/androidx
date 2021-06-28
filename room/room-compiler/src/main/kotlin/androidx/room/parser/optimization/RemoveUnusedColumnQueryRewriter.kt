/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.parser.optimization

import androidx.room.parser.ParsedQuery
import androidx.room.parser.SqlParser
import androidx.room.processor.QueryRewriter
import androidx.room.solver.query.result.QueryResultAdapter

/**
 * If the query response has unused columns, this rewrites the query to only fetch those columns.
 *
 * e.g. if it is a query like `SELECT * FROM User` where only `name` and `lastName` columns are
 * accessed in the generated code, this re-writer will change it to
 * `SELECT name, lastName FROM (SELECT * FROM User)`. Sqlite takes care of the rest where it
 * flattens the query to avoid fetching unused columns in intermediate steps.
 */
object RemoveUnusedColumnQueryRewriter : QueryRewriter {
    override fun rewrite(query: ParsedQuery, resultAdapter: QueryResultAdapter): ParsedQuery {
        // cannot do anything w/o a result info
        val resultInfo = query.resultInfo ?: return query
        if (resultAdapter.mappings.isEmpty()) {
            return query
        }
        val usedColumns = resultAdapter.mappings.flatMap { mapping ->
            mapping.matchedFields.map { it.columnName }
        }
        val columnNames = resultInfo.columns.map { it.name }
        val unusedColumns = columnNames - usedColumns
        if (unusedColumns.isEmpty()) {
            return query // nothing to optimize here
        }
        if (columnNames.size != columnNames.distinct().size) {
            // if result has duplicate columns, ignore for now
            return query
        }
        val usedColumnNames = columnNames - unusedColumns
        val updated = SqlParser.parse(
            "SELECT ${usedColumnNames.joinToString(", ") { "`$it`" }} FROM (${query.original})"
        )
        if (updated.errors.isNotEmpty()) {
            // we somehow messed up, return original
            return query
        }
        return updated
    }
}
