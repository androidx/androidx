/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.parser.expansion

import androidx.room.parser.QueryType
import androidx.room.parser.SQLiteBaseVisitor
import androidx.room.parser.SQLiteParser
import androidx.room.parser.SingleQuerySqlParser
import androidx.room.parser.Table
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode

// see documentatation in ExpandableParsedQuery file.

@Suppress("FunctionName")
class ExpandableQueryVisitor(
    private val original: String,
    private val syntaxErrors: List<String>,
    statement: ParseTree,
    private val forRuntimeQuery: Boolean
) : SQLiteBaseVisitor<Void?>() {
    private val resultColumns = arrayListOf<SectionInfo>()
    private val explicitColumns = arrayListOf<String>()
    private val bindingExpressions = arrayListOf<SectionInfo>()
    // table name alias mappings
    private val tableNames = mutableSetOf<Table>()
    private val withClauseNames = mutableSetOf<String>()
    private val queryType: QueryType

    init {
        queryType = (0 until statement.childCount).map {
            findQueryType(statement.getChild(it))
        }.filterNot { it == QueryType.UNKNOWN }.firstOrNull() ?: QueryType.UNKNOWN

        statement.accept(this)
    }

    private fun findQueryType(statement: ParseTree): QueryType {
        return when (statement) {
            is SQLiteParser.Select_stmtContext ->
                QueryType.SELECT
            is SQLiteParser.Delete_stmt_limitedContext,
            is SQLiteParser.Delete_stmtContext ->
                QueryType.DELETE
            is SQLiteParser.Insert_stmtContext ->
                QueryType.INSERT
            is SQLiteParser.Update_stmtContext,
            is SQLiteParser.Update_stmt_limitedContext ->
                QueryType.UPDATE
            is TerminalNode -> when (statement.text) {
                "EXPLAIN" -> QueryType.EXPLAIN
                else -> QueryType.UNKNOWN
            }
            else -> QueryType.UNKNOWN
        }
    }

    override fun visitResult_column(ctx: SQLiteParser.Result_columnContext?): Void? {
        fun addProjectionSection(
            c: SQLiteParser.Result_columnContext,
            p: ExpandableSection.Projection
        ) {
            resultColumns.add(
                SectionInfo(
                    Position(c.start.line - 1, c.start.charPositionInLine),
                    Position(c.stop.line - 1, c.stop.charPositionInLine + c.stop.text.length),
                    p
                )
            )
        }
        ctx?.let { c ->
            // Result columns (only in top-level SELECT)
            if (c.parent.isCoreSelect) {
                when {
                    c.text == "*" -> {
                        addProjectionSection(c, ExpandableSection.Projection.All)
                    }
                    c.table_name() != null -> {
                        addProjectionSection(
                            c,
                            ExpandableSection.Projection.Table(
                                c.table_name().text.trim('`'),
                                original.substring(c.start.startIndex, c.stop.stopIndex + 1)
                            )
                        )
                    }
                    c.column_alias() != null -> {
                        explicitColumns.add(c.column_alias().text.trim('`'))
                    }
                    else -> {
                        explicitColumns.add(c.text.trim('`'))
                    }
                }
            }
        }
        return super.visitResult_column(ctx)
    }

    override fun visitExpr(ctx: SQLiteParser.ExprContext): Void? {
        val bindParameter = ctx.BIND_PARAMETER()
        if (bindParameter != null) {
            bindingExpressions.add(
                SectionInfo(
                    Position(
                        bindParameter.symbol.line - 1,
                        bindParameter.symbol.charPositionInLine
                    ),
                    Position(
                        bindParameter.symbol.line - 1,
                        bindParameter.symbol.charPositionInLine + bindParameter.text.length
                    ),
                    ExpandableSection.BindVar(bindParameter.text)
                )
            )
        }
        return super.visitExpr(ctx)
    }

    fun createParsedQuery(): ExpandableParsedQuery {
        return ExpandableParsedQuery(
            original = original,
            type = queryType,
            projections = resultColumns.toList(),
            explicitColumns = explicitColumns.toList(),
            inputs = bindingExpressions.toList(),
            tables = tableNames,
            syntaxErrors = syntaxErrors,
            runtimeQueryPlaceholder = forRuntimeQuery
        )
    }

    override fun visitCommon_table_expression(
        ctx: SQLiteParser.Common_table_expressionContext
    ): Void? {
        val tableName = ctx.table_name()?.text
        if (tableName != null) {
            withClauseNames.add(unescapeIdentifier(tableName))
        }
        return super.visitCommon_table_expression(ctx)
    }

    override fun visitTable_or_subquery(ctx: SQLiteParser.Table_or_subqueryContext): Void? {
        val tableName = ctx.table_name()?.text
        if (tableName != null) {
            val tableAlias = ctx.table_alias()?.text
            if (tableName !in withClauseNames) {
                tableNames.add(
                    Table(
                        unescapeIdentifier(tableName),
                        unescapeIdentifier(tableAlias ?: tableName)
                    )
                )
            }
        }
        return super.visitTable_or_subquery(ctx)
    }

    private fun unescapeIdentifier(text: String): String {
        val trimmed = text.trim()
        ESCAPE_LITERALS.forEach {
            if (trimmed.startsWith(it) && trimmed.endsWith(it)) {
                return unescapeIdentifier(trimmed.substring(1, trimmed.length - 1))
            }
        }
        return trimmed
    }

    companion object {
        private val ESCAPE_LITERALS = listOf("\"", "'", "`")
    }
}

/**
 * Returns the parent of this [RuleContext] recursively as a [Sequence].
 */
private fun RuleContext.ancestors(): Sequence<RuleContext> = generateSequence(parent) { c ->
    c.parent
}

/**
 * Whether this [RuleContext] is the top SELECT statement.
 */
private val RuleContext.isCoreSelect: Boolean
    get() {
        return this is SQLiteParser.Select_or_valuesContext &&
            ancestors().none { it is SQLiteParser.Select_or_valuesContext }
    }

class ExpandableSqlParser {
    companion object {
        fun parse(input: String) = SingleQuerySqlParser.parse(
            input = input,
            visit = { statement, syntaxErrors ->
                ExpandableQueryVisitor(
                    original = input,
                    syntaxErrors = syntaxErrors,
                    statement = statement,
                    forRuntimeQuery = false
                ).createParsedQuery()
            },
            fallback = { syntaxErrors ->
                ExpandableParsedQuery(
                    original = input,
                    type = QueryType.UNKNOWN,
                    projections = emptyList(),
                    explicitColumns = emptyList(),
                    inputs = emptyList(),
                    tables = emptySet(),
                    syntaxErrors = syntaxErrors,
                    runtimeQueryPlaceholder = false
                )
            }
        )
    }
}