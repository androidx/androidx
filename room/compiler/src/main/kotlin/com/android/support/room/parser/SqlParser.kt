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

package com.android.support.room.parser

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

class BindingExtractor(val original: String) : SQLiteBaseVisitor<Void?>() {
    val bindingExpressions = arrayListOf<TerminalNode>()
    override fun visitExpr(ctx: SQLiteParser.ExprContext?): Void? {
        val bindParameter = ctx!!.BIND_PARAMETER()
        if (bindParameter != null) {
            bindingExpressions.add(bindParameter)
        }
        return super.visitExpr(ctx)
    }

    fun createParsedQuery(syntaxErrors: ArrayList<String>): ParsedQuery {
        return ParsedQuery(original,
                bindingExpressions.sortedBy { it.sourceInterval.a }, syntaxErrors)
    }
}

class SqlParser {
    companion object {
        val BINDING_REGEX = "\\:[a-zA-Z_-][a-zA-Z0-9_-]+".toRegex()
        fun parse(input: String): ParsedQuery {
            val inputStream = ANTLRInputStream(input)
            val lexer = SQLiteLexer(inputStream)
            val tokenStream = CommonTokenStream(lexer)
            val parser = SQLiteParser(tokenStream)
            val syntaxErrors = arrayListOf<String>()
            parser.addErrorListener(object : BaseErrorListener() {
                override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any,
                                         line: Int, charPositionInLine: Int, msg: String,
                                         e: RecognitionException) {
                    syntaxErrors.add(msg)
                }
            })
            val extractor = BindingExtractor(input)
            parser.select_stmt().accept(extractor)
            return extractor.createParsedQuery(syntaxErrors)
        }
    }
}
