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

    data class Text(val content: String) : Section() {
        override val text: String
            get() = content
    }

    object Newline : Section() {
        override val text: String
            get() = "\n"
    }

    data class BindVar(val symbol: String) : Section() {
        override val text: String
            get() = symbol
    }

    sealed class Projection : Section() {

        object All : Projection() {
            override val text: String
                get() = "*"
        }

        data class Table(
            val tableAlias: String,
            override val text: String
        ) : Projection()
    }
}

data class Table(val name: String, val alias: String)

data class Position(val line: Int, val charInLine: Int) : Comparable<Position> {
    override fun compareTo(other: Position): Int {
        return if (line == other.line) {
            charInLine - other.charInLine
        } else {
            line - other.line
        }
    }
}

data class SectionInfo(
    val start: Position,
    val end: Position,
    val section: Section
)

data class ParsedQuery(
    val original: String,
    val type: QueryType,
    val inputs: List<SectionInfo>,
    val projections: List<SectionInfo>,
    val explicitColumns: List<String>,
    // pairs of table name and alias,
    val tables: Set<Table>,
    val syntaxErrors: List<String>,
    val runtimeQueryPlaceholder: Boolean
) {
    companion object {
        val STARTS_WITH_NUMBER = "^\\?[0-9]".toRegex()
        val MISSING = ParsedQuery(
            original = "missing query",
            type = QueryType.UNKNOWN,
            projections = emptyList(),
            explicitColumns = emptyList(),
            inputs = emptyList(),
            tables = emptySet(),
            syntaxErrors = emptyList(),
            runtimeQueryPlaceholder = false
        )
    }

    /**
     * Optional data that might be assigned when the query is parsed inside an annotation processor.
     * User may turn this off or it might be disabled for any reason so generated code should
     * always handle not having it.
     */
    var resultInfo: QueryResultInfo? = null

    /**
     * Rewritten when the query is interpreted and rewritten by QueryInterpreter.
     */
    var interpreted = original

    val sections by lazy {
        val specialSections: List<SectionInfo> = (inputs + projections).sortedBy { it.start }

        val lines = original.lines()
        val sections = arrayListOf<Section>()
        var index = 0
        var charInLine = 0
        while (index < lines.size) {
            val line = lines[index]
            var multipleLineSection = false

            specialSections
                .filter { it.start.line == index }
                .forEach { (start, end, section) ->
                    if (charInLine < start.charInLine) {
                        sections.add(Section.Text(line.substring(charInLine, start.charInLine)))
                    }
                    sections.add(section)
                    charInLine = end.charInLine
                    if (index < end.line) {
                        index = end.line
                        multipleLineSection = true
                    }
                }

            if (!multipleLineSection) {
                if (charInLine < line.length) {
                    sections.add(Section.Text(line.substring(charInLine)))
                }
                if (index + 1 < lines.size) {
                    sections.add(Section.Newline)
                }
                index++
                charInLine = 0
            }
        }
        sections
    }

    val bindSections by lazy { sections.filterIsInstance<Section.BindVar>() }

    private fun unnamedVariableErrors(): List<String> {
        val anonymousBindError = if (inputs.any { it.section.text == "?" }) {
            arrayListOf(ParserErrors.ANONYMOUS_BIND_ARGUMENT)
        } else {
            emptyList<String>()
        }
        return anonymousBindError + inputs.filter {
            it.section.text.matches(STARTS_WITH_NUMBER)
        }.map {
            ParserErrors.cannotUseVariableIndices(it.section.text, it.start.charInLine)
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
}
