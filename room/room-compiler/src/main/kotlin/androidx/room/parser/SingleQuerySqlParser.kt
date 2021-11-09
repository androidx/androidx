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

package androidx.room.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

/**
 * Helper class to parse a single statement out of a query.
 */
object SingleQuerySqlParser {
    fun <T> parse(
        input: String,
        visit: (statement: SQLiteParser.Sql_stmtContext, syntaxErrors: MutableList<String>) -> T,
        fallback: (syntaxErrors: List<String>) -> T
    ): T {
        val inputStream = CharStreams.fromString(input)
        val lexer = SQLiteLexer(inputStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = SQLiteParser(tokenStream)
        val syntaxErrors = arrayListOf<String>()
        parser.addErrorListener(object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>,
                offendingSymbol: Any,
                line: Int,
                charPositionInLine: Int,
                msg: String,
                e: RecognitionException?
            ) {
                syntaxErrors.add(msg)
            }
        })
        try {
            val parsed = parser.parse()
            val statementList = parsed.sql_stmt_list()
            if (statementList.isEmpty()) {
                return fallback(listOf(ParserErrors.NOT_ONE_QUERY))
            }
            val statements = statementList.first().children
                .filterIsInstance<SQLiteParser.Sql_stmtContext>()
            if (statements.size != 1) {
                syntaxErrors.add(ParserErrors.NOT_ONE_QUERY)
            }
            val statement = statements.first()
            return visit(statement, syntaxErrors)
        } catch (antlrError: RuntimeException) {
            syntaxErrors.add("unknown error while parsing $input : ${antlrError.message}")
            return fallback(syntaxErrors)
        }
    }
}