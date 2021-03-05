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

import androidx.room.verifier.QueryResultInfo

sealed class Section {

    abstract val text: String

    data class Text(override val text: String) : Section()

    object NewLine : Section() {
        override val text: String
            get() = ""
    }

    data class BindVar(
        override val text: String,
        val isMultiple: Boolean
    ) : Section() {
        val varName by lazy {
            if (text.startsWith(":")) {
                text.substring(1)
            } else {
                null
            }
        }
    }

    companion object {
        fun text(text: String) = Text(text)
        fun newline() = NewLine
        fun bindVar(text: String, isMultiple: Boolean) = BindVar(text, isMultiple)
    }
}

data class Table(val name: String, val alias: String)
data class ParsedQuery(
    val original: String,
    val type: QueryType,
    val inputs: List<BindParameterNode>,
    val tables: Set<Table>, // pairs of table name and alias
    val syntaxErrors: List<String>
) {
    companion object {
        val STARTS_WITH_NUMBER = "^\\?[0-9]".toRegex()
        val MISSING = ParsedQuery(
            original = "missing query",
            type = QueryType.UNKNOWN,
            inputs = emptyList(),
            tables = emptySet(),
            syntaxErrors = emptyList()
        )
    }

    /**
     * Optional data that might be assigned when the query is parsed inside an annotation processor.
     * User may turn this off or it might be disabled for any reason so generated code should
     * always handle not having it.
     */
    var resultInfo: QueryResultInfo? = null
    val sections by lazy {
        val lines = original.lines()
        val inputsByLine = inputs.groupBy { it.symbol.line }
        val sections = arrayListOf<Section>()
        lines.forEachIndexed { index, line ->
            var charInLine = 0
            inputsByLine[index + 1]?.forEach { bindVar ->
                if (charInLine < bindVar.symbol.charPositionInLine) {
                    sections.add(
                        Section.text(
                            line.substring(
                                charInLine,
                                bindVar.symbol.charPositionInLine
                            )
                        )
                    )
                }
                sections.add(
                    Section.bindVar(
                        bindVar.text,
                        bindVar.isMultiple
                    )
                )
                charInLine = bindVar.symbol.charPositionInLine + bindVar.symbol.text.length
            }
            if (charInLine < line.length) {
                sections.add(Section.text(line.substring(charInLine)))
            }
            if (index + 1 < lines.size) {
                sections.add(Section.newline())
            }
        }
        sections
    }
    val bindSections by lazy { sections.filterIsInstance<Section.BindVar>() }
    private fun unnamedVariableErrors(): List<String> {
        val anonymousBindError = if (inputs.any { it.text == "?" }) {
            arrayListOf(ParserErrors.ANONYMOUS_BIND_ARGUMENT)
        } else {
            emptyList<String>()
        }
        return anonymousBindError + inputs.filter {
            it.text.matches(STARTS_WITH_NUMBER)
        }.map {
            ParserErrors.cannotUseVariableIndices(it.text, it.symbol.charPositionInLine)
        }
    }

    private fun unknownQueryTypeErrors(): List<String> {
        return if (QueryType.SUPPORTED.contains(type)) {
            emptyList()
        } else {
            listOf(ParserErrors.invalidQueryType(type))
        }
    }

    val errors by lazy {
        if (syntaxErrors.isNotEmpty()) {
            // if there is a syntax error, don't report others since they might be misleading.
            syntaxErrors
        } else {
            unnamedVariableErrors() + unknownQueryTypeErrors()
        }
    }
    val queryWithReplacedBindParams by lazy {
        sections.joinToString("") {
            when (it) {
                is Section.Text -> it.text
                is Section.BindVar -> "?"
                is Section.NewLine -> "\n"
            }
        }
    }
}