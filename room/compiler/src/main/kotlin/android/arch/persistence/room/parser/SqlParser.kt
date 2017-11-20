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

package android.arch.persistence.room.parser

import android.arch.persistence.room.ColumnInfo
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class QueryVisitor(val original: String, val syntaxErrors: ArrayList<String>,
                   statement: ParseTree) : SQLiteBaseVisitor<Void?>() {
    val bindingExpressions = arrayListOf<TerminalNode>()
    // table name alias mappings
    val tableNames = mutableSetOf<Table>()
    val withClauseNames = mutableSetOf<String>()
    val queryType: QueryType

    init {
        queryType = (0..statement.childCount - 1).map {
            findQueryType(statement.getChild(it))
        }.filterNot { it == QueryType.UNKNOWN }.firstOrNull() ?: QueryType.UNKNOWN

        statement.accept(this)
    }

    private fun findQueryType(statement: ParseTree): QueryType {
        return when (statement) {
            is SQLiteParser.Factored_select_stmtContext,
            is SQLiteParser.Compound_select_stmtContext,
            is SQLiteParser.Select_stmtContext,
            is SQLiteParser.Simple_select_stmtContext ->
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
            bindingExpressions.add(bindParameter)
        }
        return super.visitExpr(ctx)
    }

    fun createParsedQuery(): ParsedQuery {
        return ParsedQuery(original,
                queryType,
                bindingExpressions.sortedBy { it.sourceInterval.a },
                tableNames,
                syntaxErrors)
    }

    override fun visitCommon_table_expression(
            ctx: SQLiteParser.Common_table_expressionContext): Void? {
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
                tableNames.add(Table(
                        unescapeIdentifier(tableName),
                        unescapeIdentifier(tableAlias ?: tableName)))
            }
        }
        return super.visitTable_or_subquery(ctx)
    }

    private fun unescapeIdentifier(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("`") && trimmed.endsWith('`')) {
            return unescapeIdentifier(trimmed.substring(1, trimmed.length - 1))
        }
        if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
            return unescapeIdentifier(trimmed.substring(1, trimmed.length - 1))
        }
        return trimmed
    }
}

class SqlParser {
    companion object {
        private val INVALID_IDENTIFIER_CHARS = arrayOf('`', '\"')
        fun parse(input: String): ParsedQuery {
            val inputStream = ANTLRInputStream(input)
            val lexer = SQLiteLexer(inputStream)
            val tokenStream = CommonTokenStream(lexer)
            val parser = SQLiteParser(tokenStream)
            val syntaxErrors = arrayListOf<String>()
            parser.addErrorListener(object : BaseErrorListener() {
                override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any,
                                         line: Int, charPositionInLine: Int, msg: String,
                                         e: RecognitionException?) {
                    syntaxErrors.add(msg)
                }
            })
            try {
                val parsed = parser.parse()
                val statementList = parsed.sql_stmt_list()
                if (statementList.isEmpty()) {
                    syntaxErrors.add(ParserErrors.NOT_ONE_QUERY)
                    return ParsedQuery(input, QueryType.UNKNOWN, emptyList(), emptySet(),
                            listOf(ParserErrors.NOT_ONE_QUERY))
                }
                val statements = statementList.first().children
                        .filter { it is SQLiteParser.Sql_stmtContext }
                if (statements.size != 1) {
                    syntaxErrors.add(ParserErrors.NOT_ONE_QUERY)
                }
                val statement = statements.first()
                return QueryVisitor(input, syntaxErrors, statement).createParsedQuery()
            } catch (antlrError: RuntimeException) {
                return ParsedQuery(input, QueryType.UNKNOWN, emptyList(), emptySet(),
                        listOf("unknown error while parsing $input : ${antlrError.message}"))
            }
        }

        fun isValidIdentifier(input: String): Boolean =
                input.isNotBlank() && INVALID_IDENTIFIER_CHARS.none { input.contains(it) }
    }
}

enum class QueryType {
    UNKNOWN,
    SELECT,
    DELETE,
    UPDATE,
    EXPLAIN,
    INSERT;

    companion object {
        // IF you change this, don't forget to update @Query documentation.
        val SUPPORTED = hashSetOf(SELECT, DELETE, UPDATE)
    }
}

enum class SQLTypeAffinity {
    NULL,
    TEXT,
    INTEGER,
    REAL,
    BLOB;
    fun getTypeMirrors(env: ProcessingEnvironment): List<TypeMirror>? {
        val typeUtils = env.typeUtils
        return when (this) {
            TEXT -> listOf(env.elementUtils.getTypeElement("java.lang.String").asType())
            INTEGER -> withBoxedTypes(env, TypeKind.INT, TypeKind.BYTE, TypeKind.CHAR,
                    TypeKind.BOOLEAN, TypeKind.LONG, TypeKind.SHORT)
            REAL -> withBoxedTypes(env, TypeKind.DOUBLE, TypeKind.FLOAT)
            BLOB -> listOf(typeUtils.getArrayType(
                    typeUtils.getPrimitiveType(TypeKind.BYTE)))
            else -> emptyList()
        }
    }

    private fun withBoxedTypes(env: ProcessingEnvironment, vararg primitives: TypeKind):
            List<TypeMirror> {
        return primitives.flatMap {
            val primitiveType = env.typeUtils.getPrimitiveType(it)
            listOf(primitiveType, env.typeUtils.boxedClass(primitiveType).asType())
        }
    }

    companion object {
        // converts from ColumnInfo#SQLiteTypeAffinity
        fun fromAnnotationValue(value: Int): SQLTypeAffinity? {
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
    RTRIM;

    companion object {
        fun fromAnnotationValue(value: Int): Collate? {
            return when (value) {
                ColumnInfo.BINARY -> BINARY
                ColumnInfo.NOCASE -> NOCASE
                ColumnInfo.RTRIM -> RTRIM
                else -> null
            }
        }
    }
}
