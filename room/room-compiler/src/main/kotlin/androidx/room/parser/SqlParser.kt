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

package androidx.room.parser

import androidx.room.ColumnInfo
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.parser.expansion.isCoreSelect
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.Locale

@Suppress("FunctionName")
class QueryVisitor(
    private val original: String,
    private val syntaxErrors: List<String>,
    statement: ParseTree
) : SQLiteBaseVisitor<Void?>() {
    private val bindingExpressions = arrayListOf<BindParameterNode>()
    // table name alias mappings
    private val tableNames = mutableSetOf<Table>()
    private val withClauseNames = mutableSetOf<String>()
    private val queryType: QueryType
    private var foundTopLevelStarProjection: Boolean = false

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

    override fun visitExpr(ctx: SQLiteParser.ExprContext): Void? {
        val bindParameter = ctx.BIND_PARAMETER()
        if (bindParameter != null) {
            val parentContext = ctx.parent
            val isMultiple = parentContext is SQLiteParser.Comma_separated_exprContext &&
                !isFixedParamFunctionExpr(parentContext)
            bindingExpressions.add(
                BindParameterNode(
                    node = bindParameter,
                    isMultiple = isMultiple
                )
            )
        }
        return super.visitExpr(ctx)
    }

    override fun visitResult_column(ctx: SQLiteParser.Result_columnContext): Void? {
        if (ctx.parent.isCoreSelect && ctx.text == "*") {
            foundTopLevelStarProjection = true
        }
        return super.visitResult_column(ctx)
    }

    /**
     * Check if a comma separated expression (where multiple binding parameters are accepted) is
     * part of a function expression that receives a fixed number of parameters. This is
     * important for determining the priority of type converters used when binding a collection
     * into a binding parameters and specifically if the function takes a fixed number of
     * parameter, the collection should not be expanded.
     */
    private fun isFixedParamFunctionExpr(
        ctx: SQLiteParser.Comma_separated_exprContext
    ): Boolean {
        if (ctx.parent is SQLiteParser.ExprContext) {
            val parentExpr = ctx.parent as SQLiteParser.ExprContext
            val functionName = parentExpr.function_name() ?: return false
            return fixedParamFunctions.contains(functionName.text.lowercase(Locale.US))
        } else {
            return false
        }
    }

    fun createParsedQuery(): ParsedQuery {
        return ParsedQuery(
            original = original,
            type = queryType,
            inputs = bindingExpressions.sortedBy { it.sourceInterval.a },
            tables = tableNames,
            hasTopStarProjection =
                if (queryType == QueryType.SELECT) foundTopLevelStarProjection else null,
            syntaxErrors = syntaxErrors,
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

        // List of built-in SQLite functions that take a fixed non-zero number of parameters
        // See: https://sqlite.org/lang_corefunc.html
        val fixedParamFunctions = setOf(
            "abs",
            "glob",
            "hex",
            "ifnull",
            "iif",
            "instr",
            "length",
            "like",
            "likelihood",
            "likely",
            "load_extension",
            "lower",
            "ltrim",
            "nullif",
            "quote",
            "randomblob",
            "replace",
            "round",
            "rtrim",
            "soundex",
            "sqlite_compileoption_get",
            "sqlite_compileoption_used",
            "sqlite_offset",
            "substr",
            "trim",
            "typeof",
            "unicode",
            "unlikely",
            "upper",
            "zeroblob"
        )
    }
}

class SqlParser {
    companion object {
        private val INVALID_IDENTIFIER_CHARS = arrayOf('`', '\"')

        fun parse(input: String) = SingleQuerySqlParser.parse(
            input = input,
            visit = { statement, syntaxErrors ->
                QueryVisitor(
                    original = input,
                    syntaxErrors = syntaxErrors,
                    statement = statement
                ).createParsedQuery()
            },
            fallback = { syntaxErrors ->
                ParsedQuery(
                    original = input,
                    type = QueryType.UNKNOWN,
                    inputs = emptyList(),
                    tables = emptySet(),
                    hasTopStarProjection = null,
                    syntaxErrors = syntaxErrors,
                )
            }
        )

        fun isValidIdentifier(input: String): Boolean =
            input.isNotBlank() && INVALID_IDENTIFIER_CHARS.none { input.contains(it) }

        /**
         * creates a no-op select query for raw queries that queries the given list of tables.
         */
        fun rawQueryForTables(tableNames: Set<String>): ParsedQuery {
            return ParsedQuery(
                original = "raw query",
                type = QueryType.UNKNOWN,
                inputs = emptyList(),
                tables = tableNames.map { Table(name = it, alias = it) }.toSet(),
                hasTopStarProjection = null,
                syntaxErrors = emptyList(),
            )
        }
    }
}

data class BindParameterNode(
    private val node: TerminalNode,
    val isMultiple: Boolean // true if this is a multi-param node
) : TerminalNode by node

enum class QueryType {
    UNKNOWN,
    SELECT,
    DELETE,
    UPDATE,
    EXPLAIN,
    INSERT;

    companion object {
        // IF you change this, don't forget to update @Query documentation.
        val SUPPORTED = hashSetOf(SELECT, DELETE, UPDATE, INSERT)
    }
}

enum class SQLTypeAffinity {
    NULL,
    TEXT,
    INTEGER,
    REAL,
    BLOB;

    fun getTypeMirrors(env: XProcessingEnv): List<XType>? {
        return when (this) {
            TEXT -> withBoxedAndNullableTypes(env, CommonTypeNames.STRING)
            INTEGER -> withBoxedAndNullableTypes(
                env, TypeName.INT, TypeName.BYTE, TypeName.CHAR,
                TypeName.LONG, TypeName.SHORT
            )
            REAL -> withBoxedAndNullableTypes(env, TypeName.DOUBLE, TypeName.FLOAT)
            BLOB -> withBoxedAndNullableTypes(env, ArrayTypeName.of(TypeName.BYTE))
            else -> null
        }
    }

    /**
     * produce acceptable variations of the given type names.
     * For JAVAC:
     *  - If it is primitive, we'll add boxed version
     * For KSP:
     *  - We'll add a nullable version
     */
    private fun withBoxedAndNullableTypes(
        env: XProcessingEnv,
        vararg typeNames: TypeName
    ): List<XType> {
        return typeNames.flatMap { typeName ->
            sequence {
                val type = env.requireType(typeName)
                yield(type)
                if (env.backend == XProcessingEnv.Backend.KSP) {
                    yield(type.makeNullable())
                } else if (typeName.isPrimitive) {
                    yield(type.boxed())
                }
            }
        }.toList()
    }

    companion object {
        fun fromAnnotationValue(value: Int?): SQLTypeAffinity? {
            return when (value) {
                ColumnInfo.BLOB -> BLOB
                ColumnInfo.INTEGER -> INTEGER
                ColumnInfo.REAL -> REAL
                ColumnInfo.TEXT -> TEXT
                else -> null
            }
        }
    }
}

enum class Collate {
    BINARY,
    NOCASE,
    RTRIM,
    LOCALIZED,
    UNICODE;

    companion object {
        fun fromAnnotationValue(value: Int?): Collate? {
            return when (value) {
                ColumnInfo.BINARY -> BINARY
                ColumnInfo.NOCASE -> NOCASE
                ColumnInfo.RTRIM -> RTRIM
                ColumnInfo.LOCALIZED -> LOCALIZED
                ColumnInfo.UNICODE -> UNICODE
                else -> null
            }
        }
    }
}

enum class FtsVersion {
    FTS3,
    FTS4;
}