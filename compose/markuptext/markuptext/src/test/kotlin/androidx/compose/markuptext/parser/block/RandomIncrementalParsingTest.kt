/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.markuptext.parser.block

import androidx.compose.markuptext.parser.LineScanner
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Randomized differential test: applies random line edits to random documents, re-parses each edit
 * incrementally, and checks that the result is structurally identical to a full parse of the edited
 * document.
 */
class RandomIncrementalParsingTest {

    @Test
    fun incrementalParseMatchesFullParse() {
        val random = Random(SEED)
        repeat(DOCUMENT_COUNT) { documentIndex ->
            var lines = List(random.nextInt(MAX_LINES + 1)) { randomLine(random) }
            val context = ParseContext()
            val doc = parseFully(lines, context)

            repeat(EDITS_PER_DOCUMENT) { editIndex ->
                val start = random.nextInt(lines.size + 1)
                val endExclusive = random.nextInt(start, lines.size + 1)
                val replacement =
                    List(random.nextInt(MAX_REPLACEMENT_LINES + 1)) {
                        randomLine(random)
                    }
                val newLines =
                    lines.subList(0, start) + replacement + lines.subList(endExclusive, lines.size)

                reparseIncrementally(doc, context, newLines, start, endExclusive, replacement.size)

                assertEquals(
                    dumpStructure(parseFully(newLines, ParseContext())),
                    dumpStructure(doc),
                    "Incremental parse mismatch (document $documentIndex, edit $editIndex): " +
                        "lines=$lines, replaced [$start, $endExclusive) with $replacement",
                )
                lines = newLines
            }
        }
    }

    private fun randomLine(random: Random): String = LINES[random.nextInt(LINES.size)]

    private fun parseFully(lines: List<String>, context: ParseContext): DocumentParseBlock {
        val doc = DocumentParseBlock()
        for (line in lines) {
            context.resetLine(LineScanner(line))
            doc.processLine(context)
        }
        return doc
    }

    /**
     * Invalidates lines `[start, endExclusive)` replaced by [newLineCount] lines, then re-parses
     * [newLines] from the first unverified line until convergence or end of input.
     */
    private fun reparseIncrementally(
        doc: DocumentParseBlock,
        context: ParseContext,
        newLines: List<String>,
        start: Int,
        endExclusive: Int,
        newLineCount: Int,
    ) {
        doc.invalidateLines(start, endExclusive, newLineCount)
        for (index in doc.lineCount until newLines.size) {
            context.resetLine(LineScanner(newLines[index]))
            if (doc.processLine(context).isConverged) break
        }
    }

    /** Renders the structure of [block] and its descendants for comparison. */
    private fun dumpStructure(block: ParseBlock, indent: String = ""): String = buildString {
        append(indent).append(block::class.simpleName).append(" lineCount=").append(block.lineCount)
        when (block) {
            is ParagraphParseBlock ->
                append(" heading=")
                    .append(block.headingLevel)
                    .append(" '")
                    .append(block.content)
                    .append("'")
            else -> {}
        }
        append('\n')
        if (block is ContainerParseBlock) {
            var childLineCount = 0
            for (child in block.children) {
                childLineCount += child.lineCount
                append(dumpStructure(child, "$indent  "))
            }
            if (childLineCount != block.lineCount) {
                append(indent).append("!! children lineCount sum=").append(childLineCount)
            }
        }
    }

    private companion object {
        const val SEED = 20261028
        const val DOCUMENT_COUNT = 3000
        const val EDITS_PER_DOCUMENT = 5
        const val MAX_LINES = 10
        const val MAX_REPLACEMENT_LINES = 3

        /** Line vocabulary covering every block type supported by [ParseContext.startBlock]. */
        val LINES =
            listOf(
                "",
                "",
                "   ",
                "text",
                "more text",
                "  indented text",
                "# Heading",
                "## Heading #",
                "#NotHeading",
                "===",
                "---",
                "***",
                "_ _ _",
                "    code",
                "      deeper code",
                "```",
                "```kotlin",
                "~~~",
                "> quote",
                ">",
                "> - quoted item",
                "- item",
                "* item",
                "-",
                "  continuation",
                "  - nested item",
                "    - deeper item",
                "1. first",
                "2. second",
                "  1. nested first",
            )
    }
}
