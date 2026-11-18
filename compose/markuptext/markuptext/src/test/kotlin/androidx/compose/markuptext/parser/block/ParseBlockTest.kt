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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class ParseBlockTest {

    /** Parses [markdown] split into lines. An empty string is a document with no lines. */
    private fun parseLines(
        markdown: String,
        context: ParseContext = ParseContext(),
    ): DocumentParseBlock =
        parseLines(if (markdown.isEmpty()) emptyList() else markdown.split("\n"), context)

    /** Parses [lines], where each element is one line, so `listOf("")` is one blank line. */
    private fun parseLines(
        lines: List<String>,
        context: ParseContext = ParseContext(),
    ): DocumentParseBlock {
        val doc = DocumentParseBlock()
        for (line in lines) {
            context.resetLine(LineScanner(line))
            doc.processLine(context)
        }
        return doc
    }

    // Assertion helpers: each checks the block's type (and content) and returns the block for
    // further checks.

    /** Asserts this block is a container with exactly one child, and returns the child. */
    private fun ParseBlock.assertSingleChild(): ParseBlock {
        val container = assertIs<ContainerParseBlock>(this, "Expected a container")
        assertEquals(1, container.children.size, "Children of ${this::class.simpleName}")
        return container.children[0]
    }

    private fun ParseBlock.assertParagraphText(expected: String): ParagraphParseBlock {
        val paragraph = assertIs<ParagraphParseBlock>(this, "Expected a paragraph \"$expected\"")
        assertEquals(expected, paragraph.content.toString(), "Paragraph text")
        assertEquals(0, paragraph.headingLevel, "Heading level of paragraph \"$expected\"")
        return paragraph
    }

    private fun ParseBlock.assertHeading(level: Int, text: String): ParagraphParseBlock {
        val heading = assertIs<ParagraphParseBlock>(this, "Expected a heading \"$text\"")
        assertEquals(level, heading.headingLevel, "Heading level of \"$text\"")
        assertEquals(text, heading.content.toString(), "Heading text")
        return heading
    }

    private fun ParseBlock.assertCodeContent(expected: String): ParseBlock {
        val content =
            when (this) {
                is IndentedCodeBlockParseBlock -> getCodeContent()
                is FencedCodeBlockParseBlock -> getCodeContent()
                else -> fail("Expected a code block, was ${this::class.simpleName}")
            }
        assertEquals(expected, content, "Code content")
        return this
    }

    private fun ParseBlock.assertBlockQuote(): BlockQuoteParseBlock =
        assertIs(this, "Expected a block quote")

    private fun ParseBlock.assertList(): ListParseBlock = assertIs(this, "Expected a list")

    private fun ParseBlock.assertListItem(): ListItemParseBlock =
        assertIs(this, "Expected a list item")

    /**
     * Parses [oldLines], replaces lines `[start, endExclusive)` with [replacement], re-parses
     * incrementally until convergence or end of input, and asserts the result matches a full parse
     * of the updated lines.
     */
    private fun assertIncrementalParseMatchesFullParse(
        oldLines: List<String>,
        start: Int,
        endExclusive: Int,
        replacement: List<String>,
    ) {
        val context = ParseContext()
        val doc = parseLines(oldLines, context)
        val newLines =
            oldLines.subList(0, start) + replacement + oldLines.subList(endExclusive, oldLines.size)

        doc.invalidateLines(start, endExclusive, replacement.size)
        var line = doc.lineCount
        while (line < newLines.size) {
            context.resetLine(LineScanner(newLines[line]))
            if (doc.processLine(context).isConverged) break
            line++
        }

        assertEquals(dumpParseTree(parseLines(newLines)), dumpParseTree(doc))
    }

    @Test
    fun testBlockIdGenerationFromParseContext() {
        val context1 = ParseContext()
        assertEquals(0L, context1.generateBlockId())
        assertEquals(1L, context1.generateBlockId())

        val context2 = ParseContext()
        assertEquals(0L, context2.generateBlockId())

        val doc =
            parseLines(
                """
                First paragraph

                Second paragraph
                """
                    .trimIndent()
            )
        assertEquals(-1L, doc.id)
        assertEquals(0L, doc.children[0].id)
        assertEquals(1L, doc.children[1].id)
        assertEquals(2L, doc.children[2].id)
    }

    @Test
    fun testEmptyDocument() {
        val doc = parseLines("")
        assertTrue(doc.children.isEmpty())
        assertEquals(0, doc.lineCount)
    }

    @Test
    fun testEmptyLinesGroupedIntoEmptyLineParseBlock() {
        val doc = parseLines("\n\n\n")
        assertEquals(1, doc.children.size)
        val empty = assertIs<EmptyLineParseBlock>(doc.children[0])
        assertEquals(4, empty.lineCount)
        assertEquals(4, doc.lineCount)
    }

    @Test
    fun testSingleLineParagraph() {
        val doc = parseLines("Hello world")
        assertEquals(1, doc.children.size)
        assertEquals(1, doc.lineCount)

        val p = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Hello world", p.content.toString())
        assertEquals(0, p.headingLevel)
        assertEquals(1, p.lineCount)
    }

    @Test
    fun testMultiLineParagraph() {
        val doc =
            parseLines(
                """
                Line 1
                Line 2
                Line 3
                """
                    .trimIndent()
            )
        assertEquals(1, doc.children.size)
        assertEquals(3, doc.lineCount)

        val p = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Line 1\nLine 2\nLine 3", p.content.toString())
        assertEquals(0, p.headingLevel)
        assertEquals(3, p.lineCount)
    }

    @Test
    fun testParagraphContinuationWithNonInterruptingLines() {
        val doc =
            parseLines(
                """
                ####### 7 hashes
                #NoSpace
                """
                    .trimIndent()
            )
        assertEquals(1, doc.children.size)
        val p = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("####### 7 hashes\n#NoSpace", p.content.toString())
        assertEquals(0, p.headingLevel)
        assertEquals(2, p.lineCount)
    }

    @Test
    fun testMultipleParagraphsSeparatedByEmptyLines() {
        val doc =
            parseLines(
                """
                Paragraph 1

                Paragraph 2


                Paragraph 3
                """
                    .trimIndent()
            )
        assertEquals(5, doc.children.size)
        assertEquals(6, doc.lineCount)

        val p1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Paragraph 1", p1.content.toString())
        assertEquals(1, p1.lineCount)

        val e1 = assertIs<EmptyLineParseBlock>(doc.children[1])
        assertEquals(1, e1.lineCount)

        val p2 = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals("Paragraph 2", p2.content.toString())
        assertEquals(1, p2.lineCount)

        val e2 = assertIs<EmptyLineParseBlock>(doc.children[3])
        assertEquals(2, e2.lineCount)

        val p3 = assertIs<ParagraphParseBlock>(doc.children[4])
        assertEquals("Paragraph 3", p3.content.toString())
        assertEquals(1, p3.lineCount)
    }

    @Test
    fun testAtxHeadings() {
        val doc =
            parseLines(
                """
                # H1
                ## H2
                ### H3 ###
                ###### H6
                """
                    .trimIndent()
            )
        assertEquals(4, doc.children.size)
        assertEquals(4, doc.lineCount)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("H1", h1.content.toString())
        assertEquals(1, h1.headingLevel)
        assertEquals(1, h1.lineCount)

        val h2 = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("H2", h2.content.toString())
        assertEquals(2, h2.headingLevel)
        assertEquals(1, h2.lineCount)

        val h3 = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals("H3", h3.content.toString())
        assertEquals(3, h3.headingLevel)
        assertEquals(1, h3.lineCount)

        val h6 = assertIs<ParagraphParseBlock>(doc.children[3])
        assertEquals("H6", h6.content.toString())
        assertEquals(6, h6.headingLevel)
        assertEquals(1, h6.lineCount)
    }

    @Test
    fun testAtxHeadingClosingHashes() {
        val doc =
            parseLines(
                """
                # Heading with # inside #

                # Heading with no space before closing#

                ### ###

                ####### Not a heading

                #NoSpace
                """
                    .trimIndent()
            )
        assertEquals(9, doc.children.size)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Heading with # inside", h1.content.toString())
        assertEquals(1, h1.headingLevel)

        val h2 = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals("Heading with no space before closing#", h2.content.toString())
        assertEquals(1, h2.headingLevel)

        val h3 = assertIs<ParagraphParseBlock>(doc.children[4])
        assertEquals("", h3.content.toString())
        assertEquals(3, h3.headingLevel)

        val p1 = assertIs<ParagraphParseBlock>(doc.children[6])
        assertEquals("####### Not a heading", p1.content.toString())
        assertEquals(0, p1.headingLevel)

        val p2 = assertIs<ParagraphParseBlock>(doc.children[8])
        assertEquals("#NoSpace", p2.content.toString())
        assertEquals(0, p2.headingLevel)
    }

    @Test
    fun testAtxHeadingDoesNotTakeContinuationLines() {
        val doc =
            parseLines(
                """
                # Heading
                Next line
                """
                    .trimIndent()
            )
        assertEquals(2, doc.children.size)
        assertEquals(2, doc.lineCount)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Heading", h1.content.toString())
        assertEquals(1, h1.headingLevel)
        assertEquals(1, h1.lineCount)

        val p = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("Next line", p.content.toString())
        assertEquals(0, p.headingLevel)
        assertEquals(1, p.lineCount)
    }

    @Test
    fun testSetextHeadings() {
        val doc =
            parseLines(
                """
                Level 1 Heading
                ===============
                Level 2 Heading
                ---------------
                """
                    .trimIndent()
            )
        assertEquals(2, doc.children.size)
        assertEquals(4, doc.lineCount)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Level 1 Heading", h1.content.toString())
        assertEquals(1, h1.headingLevel)
        assertEquals(2, h1.lineCount)

        val h2 = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("Level 2 Heading", h2.content.toString())
        assertEquals(2, h2.headingLevel)
        assertEquals(2, h2.lineCount)
    }

    @Test
    fun testSetextHeadingMultiLineParagraph() {
        val doc =
            parseLines(
                """
                Line 1
                Line 2
                ===
                """
                    .trimIndent()
            )
        assertEquals(1, doc.children.size)
        assertEquals(3, doc.lineCount)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Line 1\nLine 2", h1.content.toString())
        assertEquals(1, h1.headingLevel)
        assertEquals(3, h1.lineCount)
    }

    @Test
    fun testSetextHeadingFollowedByParagraph() {
        val doc =
            parseLines(
                """
                Heading
                ===
                Paragraph after heading
                """
                    .trimIndent()
            )
        assertEquals(2, doc.children.size)
        assertEquals(3, doc.lineCount)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Heading", h1.content.toString())
        assertEquals(1, h1.headingLevel)
        assertEquals(2, h1.lineCount)

        val p = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("Paragraph after heading", p.content.toString())
        assertEquals(0, p.headingLevel)
        assertEquals(1, p.lineCount)
    }

    @Test
    fun testSetextHeadingNotAllowedOnLazyContinuation() {
        val p = ParagraphParseBlock(id = 1L, initialContent = "Line 1", lineCount = 1)
        val context = ParseContext()
        context.resetLine(LineScanner("==="))
        context.startLazyContinuation()

        val result = p.parseLine(context)
        assertTrue(result.accepted)
        assertEquals(0, p.headingLevel) // Remained body text paragraph, not converted to H1
        assertEquals("Line 1\n===", p.content.toString())
    }

    @Test
    fun testIndentedParagraphAndHeading() {
        val doc =
            parseLines(
                """
                   # Indented H1

                   Indented paragraph

                Indented Setext
                   ===
                """
                    .trimIndent()
            )
        assertEquals(5, doc.children.size)

        val h1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Indented H1", h1.content.toString())
        assertEquals(1, h1.headingLevel)

        val p = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals("Indented paragraph", p.content.toString())
        assertEquals(0, p.headingLevel)

        val setext = assertIs<ParagraphParseBlock>(doc.children[4])
        assertEquals("Indented Setext", setext.content.toString())
        assertEquals(1, setext.headingLevel)
        assertEquals(2, setext.lineCount)
    }

    @Test
    fun testInvalidateLinesMidDocument() {
        val context = ParseContext()
        val doc = parseLines("P1\nP1b\n\n\n\n# H3", context)
        val (p1, e2, h3) = doc.children
        assertIs<EmptyLineParseBlock>(e2)
        assertEquals(listOf(2, 3, 1), doc.children.map { it.lineCount })

        // Invalidate lines 2..5 (affecting e2), replaced by 2 new lines
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 5, newLineCount = 2)
        assertEquals(2, doc.lineCount)
        assertEquals(listOf(p1), doc.children)

        // The 2 new lines continue p1; h3 now starts 2 lines later (new line 4), where the heading
        // interrupts p1, opens a new block and converges with h3.
        doc.feedLines(context, "new 1", "new 2")
        assertEquals(4, doc.lineCount)
        context.resetLine(LineScanner("# H3"))
        assertTrue(doc.processLine(context).isConverged)
        assertEquals(listOf(p1, h3), doc.children)
        assertEquals(5, doc.lineCount)
    }

    @Test
    fun testInvalidateLinesSpanningFromChildContainerAcrossSibling() {
        // doc > [childContainer > [c1 (2 lines), blank, C2], sibling1 (3 lines), sibling2]
        val context = ParseContext()
        val doc =
            context.documentStartingWith(
                IndentedContainer(context.generateBlockId()),
                firstLine = "C1",
            )
        doc.feedLines(context, "  C1b", "", "  C2", "S1", "S1b", "S1c", "# S2")
        val (childContainer, sibling1, sibling2) = doc.children
        assertIs<IndentedContainer>(childContainer)
        val c1 = childContainer.children[0]
        assertEquals(listOf(2, 1, 1), childContainer.children.map { it.lineCount })
        assertEquals(listOf(4, 3, 1), doc.children.map { it.lineCount })
        assertIs<ParagraphParseBlock>(sibling1)

        // Edit spans from line 2 (inside childContainer, after c1) across sibling1 (lines 4..6),
        // replaced by 1 new line
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 7, newLineCount = 1)

        // childContainer keeps its first child (2 lines); doc keeps childContainer open
        assertEquals(listOf(c1), childContainer.children)
        assertEquals(2, childContainer.lineCount)
        assertEquals(listOf(childContainer), doc.children)
        assertEquals(2, doc.lineCount)

        // The new line continues c1 inside childContainer. sibling2 now starts right after it
        // (new line 3): "# S2" closes childContainer, opens a heading in doc, and converges with
        // sibling2.
        doc.feedLines(context, "  new")
        assertEquals(3, childContainer.lineCount)
        context.resetLine(LineScanner("# S2"))
        assertTrue(doc.processLine(context).isConverged)
        assertEquals(listOf(childContainer, sibling2), doc.children)
        assertEquals(4, doc.lineCount)
    }

    @Test
    fun testDescendantConvergenceInLastChildContainer() {
        // doc > childContainer (a nested document, accepting every line) > [C1, blank, C2, blank,
        // C3]
        val context = ParseContext()
        val childContainer = DocumentParseBlock(id = context.generateBlockId())
        val doc = context.documentStartingWith(childContainer, firstLine = "C1")
        doc.feedLines(context, "", "C2", "", "C3")
        assertEquals(5, childContainer.children.size)
        assertEquals(listOf(childContainer), doc.children)

        // Edit line 2 ("C2") inside childContainer (which is the last/only child of doc)
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 3, newLineCount = 1)
        assertEquals(2, doc.lineCount)
        assertEquals(2, childContainer.lineCount)

        context.resetLine(LineScanner("C2 modified"))
        val result0 = doc.processLine(context)
        assertTrue(result0.accepted)
        assertFalse(result0.isConverged)

        // Line 3 ("") converges inside childContainer and bubbles up to doc without throwing
        context.resetLine(LineScanner(""))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertTrue(result1.isConverged)
        assertEquals(5, childContainer.lineCount)
        assertEquals(5, doc.lineCount)
    }

    @Test
    fun testIncrementalParseMismatchAtFirstUnverifiedThenConvergesAtNext() {
        val context = ParseContext()
        val doc =
            parseLines(
                """
                Paragraph 1

                Paragraph 2

                Paragraph 3
                """
                    .trimIndent(),
                context,
            )
        assertEquals(5, doc.children.size)
        assertEquals(5, doc.lineCount)
        val e2 = doc.children[3]
        val p3 = doc.children[4]

        // Replace line 1 ("") with "bridging text" so Paragraph 1 and Paragraph 2 merge
        doc.invalidateLines(oldLineStart = 1, oldLineEndExclusive = 2, newLineCount = 1)
        assertEquals(1, doc.lineCount)

        // Line 1 ("bridging text"): absorbed by Paragraph 1; delta reaches 0 for Paragraph 2
        context.resetLine(LineScanner("bridging text"))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertFalse(result1.isConverged)
        assertEquals(2, doc.lineCount)

        // Line 2 ("Paragraph 2"): reaches Paragraph 2's convergence point, but Paragraph 1 absorbs
        // it (mismatch!). Paragraph 2 is discarded and delta shifts to e2.
        context.resetLine(LineScanner("Paragraph 2"))
        val result2 = doc.processLine(context)
        assertTrue(result2.accepted)
        assertFalse(result2.isConverged)
        assertEquals(3, doc.lineCount)

        // Line 3 (""): opens a new EmptyLineParseBlock at delta == 0 and converges with e2!
        context.resetLine(LineScanner(""))
        val result3 = doc.processLine(context)
        assertTrue(result3.accepted)
        assertTrue(result3.isConverged)
        assertEquals(5, doc.lineCount)
        assertEquals(3, doc.children.size)

        val mergedP1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Paragraph 1\nbridging text\nParagraph 2", mergedP1.content.toString())
        assertEquals(3, mergedP1.lineCount)
        assertSame(e2, doc.children[1])
        assertSame(p3, doc.children[2])
    }

    @Test
    fun testConvergenceMatchingType() {
        val context = ParseContext()
        val doc = parseLines("a\n", context)
        val (p1, e2) = doc.children
        assertIs<ParagraphParseBlock>(p1)
        assertIs<EmptyLineParseBlock>(e2)

        // Delete line 0 (p1): e2 shifts from line 1 to line 0 (delta = 0)
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 0)
        assertEquals(0, doc.lineCount)
        assertTrue(doc.children.isEmpty())

        // Parsing line 0 opens a new EmptyLineParseBlock at delta == 0, converging with e2
        context.resetLine(LineScanner(""))
        val result = doc.processLine(context)
        assertTrue(result.accepted)
        assertTrue(result.isConverged)
        assertEquals(1, doc.lineCount)
        assertEquals(1, doc.children.size)
        assertEquals(e2, doc.children[0])
    }

    @Test
    fun testIncrementalParseEditMiddleParagraphWithConvergence() {
        val context = ParseContext()
        val doc =
            parseLines(
                """
                Paragraph 1

                Paragraph 2

                Paragraph 3
                """
                    .trimIndent(),
                context,
            )
        assertEquals(5, doc.children.size)
        assertEquals(5, doc.lineCount)
        val p3 = doc.children[4]

        // Invalidate line 2 ("Paragraph 2"): oldLineStart = 2, oldLineEndExclusive = 3,
        // newLineCount = 1
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 3, newLineCount = 1)
        assertEquals(2, doc.lineCount)
        assertEquals(2, doc.children.size) // activeChild is EmptyLine at index 1

        // Parse modified line 2 using the same ParseContext
        context.resetLine(LineScanner("Paragraph 2 modified"))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertFalse(result1.isConverged)
        assertEquals(3, doc.lineCount)
        assertEquals(3, doc.children.size)

        // Parse line 3 (""): aligns with e2 (EmptyLineParseBlock) and converges!
        context.resetLine(LineScanner(""))
        val result2 = doc.processLine(context)
        assertTrue(result2.accepted)
        assertTrue(result2.isConverged)
        assertEquals(5, doc.lineCount)
        assertEquals(5, doc.children.size)

        val p2 = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals("Paragraph 2 modified", p2.content.toString())
        assertEquals(5L, p2.id)
        assertSame(p3, doc.children[4])
    }

    @Test
    fun testIncrementalParseActiveChildAbsorbsLineDiscardsStaleUnverified() {
        val context = ParseContext()
        val doc = parseLines("P1\n\nP2 a\nP2 b\nP2 c\n\nP3", context)
        assertEquals(5, doc.children.size)
        val p1 = assertIs<ParagraphParseBlock>(doc.children[0])
        val e2 = doc.children[3]
        val p3 = doc.children[4]

        // Replace line 1 ("") with "P1 continuation": Paragraph 1 now absorbs the old Paragraph 2.
        doc.invalidateLines(oldLineStart = 1, oldLineEndExclusive = 2, newLineCount = 1)
        assertEquals(listOf<ParseBlock>(p1), doc.children)

        // Line 1 continues p1. Line 2 ("P2 a") is where the old Paragraph 2 started, but p1
        // absorbs it too, so the old Paragraph 2 (3 lines) is discarded instead of converging.
        doc.feedLines(context, "P1 continuation", "P2 a")
        assertEquals(listOf<ParseBlock>(p1), doc.children)
        assertEquals("P1\nP1 continuation\nP2 a", p1.content.toString())
        assertEquals(3, doc.lineCount)

        // The next old child (e2) starts 3 lines after the old Paragraph 2 did. Lines 3..4 are
        // still absorbed by p1; line 5 ("") opens a blank line block there and converges with e2.
        doc.feedLines(context, "P2 b", "P2 c")
        context.resetLine(LineScanner(""))
        assertTrue(doc.processLine(context).isConverged)
        assertEquals(listOf(p1, e2, p3), doc.children)
        assertEquals(7, doc.lineCount)
    }

    @Test
    fun testIncrementalParseConvergesWithUnchangedSetextHeading() {
        val context = ParseContext()
        val doc =
            parseLines(
                """
                # H0

                Title
                ===

                Paragraph 3
                """
                    .trimIndent(),
                context,
            )
        assertEquals(5, doc.children.size)
        val setextH1 = doc.children[2]
        val p3 = doc.children[4]

        // Invalidate line 0 ("# H0"): replace with "# H0 edited"
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 1)
        assertEquals(0, doc.lineCount)

        // Reparse line 0 using the same ParseContext
        context.resetLine(LineScanner("# H0 edited"))
        val result0 = doc.processLine(context)
        assertTrue(result0.accepted)
        assertFalse(result0.isConverged)

        // Reparse line 1 (""): converges with EmptyLineParseBlock at index 1
        context.resetLine(LineScanner(""))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertTrue(result1.isConverged)

        assertEquals(6, doc.lineCount)
        assertEquals(5, doc.children.size)

        // The unchanged Setext heading was preserved intact without re-parsing line 3 ("===")
        assertSame(setextH1, doc.children[2])
        assertSame(p3, doc.children[4])
        val h = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals(1, h.headingLevel)
        assertEquals(2, h.lineCount)
        assertEquals("Title", h.content.toString())
    }

    @Test
    fun testMultipleIncrementalEditsPreservesLineCount() {
        val context = ParseContext()
        val doc =
            parseLines(
                """
                Paragraph 1

                Paragraph 2

                Paragraph 3
                """
                    .trimIndent(),
                context,
            )
        assertEquals(5, doc.lineCount)

        // Edit 1: Edit line 0 from "Paragraph 1" to "Paragraph 1 edited"
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 1)
        context.resetLine(LineScanner("Paragraph 1 edited"))
        doc.processLine(context)
        context.resetLine(LineScanner("")) // line 1 converges with EmptyLineParseBlock
        val convResult1 = doc.processLine(context)
        assertTrue(convResult1.isConverged)
        assertEquals(5, doc.lineCount)

        val p1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Paragraph 1 edited", p1.content.toString())

        // Edit 2: Edit line 4 from "Paragraph 3" to "Paragraph 3 edited"
        doc.invalidateLines(oldLineStart = 4, oldLineEndExclusive = 5, newLineCount = 1)
        assertEquals(4, doc.lineCount)
        context.resetLine(LineScanner("Paragraph 3 edited"))
        val convResult2 = doc.processLine(context)
        assertTrue(convResult2.accepted)
        assertEquals(5, doc.lineCount)

        val p3 = assertIs<ParagraphParseBlock>(doc.children[4])
        assertEquals("Paragraph 3 edited", p3.content.toString())
    }

    /** Test container accepting blank lines and lines indented by at least 2 spaces. */
    private class IndentedContainer(
        id: Long,
        override val supportsLazyContinuation: Boolean = false,
    ) : BaseContainerParseBlock(id) {

        override fun matchLineStart(context: ParseContext): Boolean {
            val scanner = context.currentLine
            if (scanner.isEmptyLine()) return true
            if (scanner.leadingSpaceCount < 2) return false
            scanner.consume(2)
            return true
        }
    }

    /**
     * Creates a document as if [containers] (each nested in the previous one) started on its first
     * line, holding a paragraph with [firstLine]. Feed later lines to the document with
     * [feedLines], so the engine adds the following children and siblings.
     */
    private fun ParseContext.documentStartingWith(
        vararg containers: ContainerParseBlock,
        firstLine: String = "a",
    ): DocumentParseBlock {
        // Innermost first: initFirstChild takes the child's line count, so each child must already
        // hold its own first child.
        var child: ParseBlock =
            ParagraphParseBlock(id = generateBlockId(), initialContent = firstLine, lineCount = 1)
        for (container in containers.reversed()) {
            container.initFirstChild(child)
            child = container
        }
        return DocumentParseBlock().apply { initFirstChild(child) }
    }

    /** Creates a document whose only child is an [IndentedContainer] holding paragraph `a`. */
    private fun documentWithIndentedContainer(context: ParseContext): DocumentParseBlock =
        context.documentStartingWith(IndentedContainer(id = context.generateBlockId()))

    /** Test container whose lines start with `>`, supporting lazy continuation. */
    private class PrefixedContainer(id: Long) : BaseContainerParseBlock(id) {
        override val supportsLazyContinuation: Boolean = true

        override fun matchLineStart(context: ParseContext): Boolean {
            val scanner = context.currentLine
            if (scanner.leadingSpaceCount != 0 || scanner.firstNonSpaceChar != '>') return false
            scanner.consume(1)
            return true
        }
    }

    @Test
    fun testInitFirstChildSetsOnlyTheFirstChild() {
        val context = ParseContext()
        val quote = PrefixedContainer(id = context.generateBlockId())
        val heading =
            ParagraphParseBlock(
                id = context.generateBlockId(),
                headingLevel = 1,
                initialContent = "h",
                lineCount = 1,
            )
        quote.initFirstChild(heading)
        assertSame(heading, quote.children.single())
        assertSame(quote, heading.parent)
        assertEquals(1, quote.lineCount)

        // Later children come only from parsing lines.
        val second =
            ParagraphParseBlock(id = context.generateBlockId(), initialContent = "b", lineCount = 1)
        assertFailsWith<IllegalStateException> { quote.initFirstChild(second) }
        assertSame(heading, quote.children.single())
        assertEquals(1, quote.lineCount)

        // A container child must already hold its first line.
        val outer = PrefixedContainer(id = context.generateBlockId())
        assertFailsWith<IllegalStateException> {
            outer.initFirstChild(PrefixedContainer(id = context.generateBlockId()))
        }
        assertTrue(outer.children.isEmpty())
    }

    // ProposingLeaf, WrappingLeaf and RefusingContainer stay test-only: no built-in block wraps
    // itself for a container that refuses it, so only these blocks can show that a container checks
    // canAcceptChild before calling prepareForAttach, and that a refused block moves up unchanged.

    /** Test leaf that rejects every line, proposing a block made by [proposed]. */
    private class ProposingLeaf(id: Long, private val proposed: () -> ParseBlock) :
        LeafParseBlock(id, lineCount = 1) {
        override fun parseLine(context: ParseContext): ParseResult =
            ParseResult.rejectAndPropose(proposed())
    }

    /**
     * Test leaf that wraps itself in a [PrefixedContainer] unless its container already is one, and
     * records each container it is prepared for.
     */
    private class WrappingLeaf(
        id: Long,
        private val preparedFor: MutableList<ContainerParseBlock>,
    ) : LeafParseBlock(id, lineCount = 1) {
        override fun parseLine(context: ParseContext): ParseResult = ParseResult.reject()

        override fun prepareForAttach(
            container: ContainerParseBlock,
            context: ParseContext,
        ): ParseBlock {
            preparedFor.add(container)
            if (container is PrefixedContainer) return this
            return PrefixedContainer(id = context.generateBlockId()).also {
                it.initFirstChild(this)
            }
        }
    }

    /** Test container matching every line and refusing [WrappingLeaf] children. */
    private class RefusingContainer(id: Long) : BaseContainerParseBlock(id) {
        override val supportsLazyContinuation: Boolean = false

        override fun matchLineStart(context: ParseContext): Boolean = true

        override fun canAcceptChild(child: ParseBlock): Boolean = child !is WrappingLeaf
    }

    /**
     * Creates a document holding [container], which holds a [ProposingLeaf] that proposes a new
     * [WrappingLeaf] (recording into [preparedFor]) on every line.
     */
    private fun ParseContext.documentProposingWrappingLeaf(
        container: BaseContainerParseBlock,
        preparedFor: MutableList<ContainerParseBlock>,
    ): DocumentParseBlock {
        val proposing =
            ProposingLeaf(generateBlockId()) { WrappingLeaf(generateBlockId(), preparedFor) }
        container.initFirstChild(proposing)
        return DocumentParseBlock().apply { initFirstChild(container) }
    }

    @Test
    fun testPrepareForAttachRunsOnlyForTheContainerThatAccepts() {
        // The refusing container passes the raw WrappingLeaf up unchanged; only the document, which
        // accepts it, prepares it and attaches the wrapper it returns.
        val context = ParseContext()
        val preparedFor = mutableListOf<ContainerParseBlock>()
        val refusing = RefusingContainer(context.generateBlockId())
        val doc = context.documentProposingWrappingLeaf(refusing, preparedFor)

        doc.feedLines(context, "x")
        assertEquals(listOf<ContainerParseBlock>(doc), preparedFor, "Only the document prepares it")
        assertEquals(1, refusing.children.size, "The refusing container keeps only its first child")
        assertEquals(2, doc.children.size)
        val wrapper = assertIs<PrefixedContainer>(doc.children[1], "The wrapper the leaf returned")
        assertIs<WrappingLeaf>(wrapper.children.single(), "The leaf inside its wrapper")
    }

    @Test
    fun testPrepareForAttachCanKeepTheBlockUnwrapped() {
        // A PrefixedContainer accepts the proposed WrappingLeaf, which then needs no wrapper.
        val context = ParseContext()
        val preparedFor = mutableListOf<ContainerParseBlock>()
        val quote = PrefixedContainer(context.generateBlockId())
        val doc = context.documentProposingWrappingLeaf(quote, preparedFor)

        doc.feedLines(context, ">x")
        assertEquals(listOf<ContainerParseBlock>(quote), preparedFor, "Only the quote prepares it")
        assertIs<WrappingLeaf>(quote.children[1], "The leaf attached without a wrapper")
    }

    /** Test container consuming a `>` prefix and refusing [WrappingLeaf] children. */
    private class PrefixedRefusingContainer(id: Long) : BaseContainerParseBlock(id) {
        override val supportsLazyContinuation: Boolean = false

        override fun matchLineStart(context: ParseContext): Boolean {
            val scanner = context.currentLine
            if (scanner.firstNonSpaceChar != '>') return false
            scanner.consume(1)
            return true
        }

        override fun canAcceptChild(child: ParseBlock): Boolean = child !is WrappingLeaf
    }

    @Test
    fun testContainerConsumingAPrefixMustNotRefuseAChild() {
        // The document would attach the refused leaf, started after `>`, without the `>` container:
        // the engine fails instead of dropping the prefix.
        val context = ParseContext()
        val refusing = PrefixedRefusingContainer(context.generateBlockId())
        val doc = context.documentProposingWrappingLeaf(refusing, mutableListOf())
        assertFailsWith<IllegalStateException> { doc.feedLines(context, ">x") }
    }

    /** Test leaf that wraps itself in an empty [PrefixedContainer], forgetting to attach itself. */
    private class EmptyWrapperLeaf(id: Long) : LeafParseBlock(id, lineCount = 1) {
        override fun parseLine(context: ParseContext): ParseResult = ParseResult.reject()

        override fun prepareForAttach(
            container: ContainerParseBlock,
            context: ParseContext,
        ): ParseBlock = PrefixedContainer(id = context.generateBlockId())
    }

    @Test
    fun testPrepareForAttachResultMustHoldTheCurrentLine() {
        // An empty wrapper holds no line, but the document would count the line: the engine fails
        // instead of breaking its line counts.
        val context = ParseContext()
        val proposing =
            ProposingLeaf(context.generateBlockId()) {
                EmptyWrapperLeaf(context.generateBlockId())
            }
        val doc = DocumentParseBlock()
        doc.initFirstChild(proposing)
        assertFailsWith<IllegalStateException> { doc.feedLines(context, "x") }
    }

    @Test
    fun testLazyContinuationDoesNotMatchDescendantLineStarts() {
        // document > `>` container > 2-space container (lazy) > paragraph "a"
        fun buildDocument(context: ParseContext): DocumentParseBlock =
            context.documentStartingWith(
                PrefixedContainer(id = context.generateBlockId()),
                IndentedContainer(context.generateBlockId(), supportsLazyContinuation = true),
            )

        // "  # h" fails the outer `>` prefix, so the line is a lazy continuation. The inner
        // container must not match its 2-space indent: the heading interrupts the paragraph and
        // opens in the document, not inside the inner container.
        var context = ParseContext()
        var doc = buildDocument(context)
        val outer = doc.children[0] as PrefixedContainer
        val inner = outer.children[0] as IndentedContainer
        doc.feedLines(context, "  # h")
        assertEquals(2, doc.children.size)
        assertSame(outer, doc.children[0])
        assertEquals(1, assertIs<ParagraphParseBlock>(doc.children[1]).headingLevel)
        assertEquals(1, inner.children.size)

        // A plain lazy line still continues the paragraph through both containers.
        context = ParseContext()
        doc = buildDocument(context)
        doc.feedLines(context, "  more")
        assertEquals(1, doc.children.size)
        val paragraph =
            ((doc.children[0] as PrefixedContainer).children[0] as IndentedContainer).children[0]
        assertEquals("a\nmore", assertIs<ParagraphParseBlock>(paragraph).content.toString())
    }

    /**
     * Test leaf that tentatively accepts lines starting with `:` and keeps their text in [content].
     * On any other line it replaces itself with [replacementFor] holding the same lines, which then
     * parses the current line.
     */
    private class TentativeLeaf(
        id: Long,
        initialContent: String,
        private val replacementFor: (TentativeLeaf) -> ParseBlock,
    ) : LeafParseBlock(id, lineCount = 1) {
        val content: StringBuilder = StringBuilder(initialContent)

        override fun parseLine(context: ParseContext): ParseResult {
            val scanner = context.currentLine
            if (scanner.firstNonSpaceChar != ':') {
                return ParseResult.replaceSelf(replacementFor(this))
            }
            content.append('\n').append(scanner.getRawUnconsumedText())
            return ParseResult.accept()
        }
    }

    /** Replaces a [TentativeLeaf] with a paragraph holding its lines. */
    private fun ParseContext.paragraphFrom(leaf: TentativeLeaf) =
        ParagraphParseBlock(
            id = generateBlockId(),
            initialContent = leaf.content.toString(),
            lineCount = leaf.lineCount,
        )

    @Test
    fun testReplaceSelfReplacesChildAndReparsesLine() {
        val context = ParseContext()
        val doc = DocumentParseBlock()
        val tentative =
            TentativeLeaf(context.generateBlockId(), ": a") { context.paragraphFrom(it) }
        doc.initFirstChild(tentative)
        doc.feedLines(context, ": b")
        assertEquals(listOf<ParseBlock>(tentative), doc.children)
        assertEquals(2, tentative.lineCount)

        // "c" makes the tentative leaf give up: the paragraph takes its 2 lines, then accepts "c".
        doc.feedLines(context, "c")
        val paragraph = assertIs<ParagraphParseBlock>(doc.children.single())
        assertSame(doc, paragraph.parent)
        assertEquals(3, paragraph.lineCount)
        assertEquals(3, doc.lineCount)
        assertEquals(": a\n: b\nc", paragraph.content.toString())
    }

    @Test
    fun testReplaceSelfRejectsMismatchedLineCount() {
        val context = ParseContext()
        val doc = DocumentParseBlock()
        doc.initFirstChild(
            TentativeLeaf(context.generateBlockId(), ": a") {
                ParagraphParseBlock(
                    id = context.generateBlockId(),
                    initialContent = it.content.toString(),
                    lineCount = it.lineCount + 1,
                )
            }
        )
        assertFailsWith<IllegalStateException> { doc.feedLines(context, "c") }
    }

    @Test
    fun testReplaceSelfChainThrows() {
        // The replacement replaces itself again on the same line, which is not allowed.
        val context = ParseContext()
        val doc = DocumentParseBlock()
        doc.initFirstChild(
            TentativeLeaf(context.generateBlockId(), ": a") { first ->
                TentativeLeaf(context.generateBlockId(), first.content.toString()) {
                    context.paragraphFrom(it)
                }
            }
        )
        assertFailsWith<IllegalStateException> { doc.feedLines(context, "c") }
    }

    private fun DocumentParseBlock.feedLines(context: ParseContext, vararg lines: String) {
        for (line in lines) {
            context.resetLine(LineScanner(line))
            processLine(context)
        }
    }

    @Test
    fun testTrailingBlankLinesStayInContainerThatConsumedThem() {
        val context = ParseContext()
        val doc = documentWithIndentedContainer(context)
        val container = assertIs<IndentedContainer>(doc.children[0])

        doc.feedLines(context, "", "")
        assertEquals(3, container.lineCount)
        assertIs<EmptyLineParseBlock>(container.children[1])

        // "b" closes the container, which keeps its 2 trailing blank lines and reports them.
        doc.feedLines(context, "b")
        assertEquals(4, doc.lineCount)
        assertEquals(2, doc.children.size)
        assertSame(container, doc.children[0])
        assertEquals(3, container.lineCount)
        assertEquals(2, container.children[1].countTrailingEmptyLines())
        assertEquals("b", assertIs<ParagraphParseBlock>(doc.children[1]).content.toString())
        assertEquals(0, doc.children[1].countTrailingEmptyLines())
    }

    @Test
    fun testEditAfterTrailingBlankLinesIsOfferedToContainer() {
        val context = ParseContext()
        val doc = documentWithIndentedContainer(context)
        val container = assertIs<IndentedContainer>(doc.children[0])
        doc.feedLines(context, "", "", "b")
        assertEquals(2, doc.children.size)

        // Edit line 3 ("b" -> "  c"). The container ends right before the edit, so it stays
        // verified and active, and the edited line continues it.
        doc.invalidateLines(oldLineStart = 3, oldLineEndExclusive = 4, newLineCount = 1)
        assertEquals(3, doc.lineCount)
        assertEquals(listOf<ParseBlock>(container), doc.children)

        doc.feedLines(context, "  c")
        assertEquals(4, doc.lineCount)
        assertEquals(listOf<ParseBlock>(container), doc.children)
        assertEquals(4, container.lineCount)
        assertEquals(3, container.children.size)
        assertEquals(2, assertIs<EmptyLineParseBlock>(container.children[1]).lineCount)
        assertEquals("c", assertIs<ParagraphParseBlock>(container.children[2]).content.toString())
        assertEquals(0, container.children[2].countTrailingEmptyLines())
    }

    @Test
    fun testInvalidateLinesKeepsContainerEndingAtEditVerified() {
        // doc > [childContainer > [A, blank], B]
        val context = ParseContext()
        val doc =
            context.documentStartingWith(
                IndentedContainer(context.generateBlockId()),
                firstLine = "A",
            )
        doc.feedLines(context, "", "B")
        val childContainer = assertIs<IndentedContainer>(doc.children[0])
        assertIs<EmptyLineParseBlock>(childContainer.children[1])
        assertEquals("B", assertIs<ParagraphParseBlock>(doc.children[1]).content.toString())

        // Edit line 2 ("B"). childContainer ends right before the edit, so it stays verified
        // (including its trailing EmptyLineParseBlock) and becomes the active child.
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 3, newLineCount = 1)
        assertEquals(2, doc.lineCount)
        assertEquals(listOf<ParseBlock>(childContainer), doc.children)
        assertEquals(2, childContainer.lineCount)
        assertEquals(2, childContainer.children.size)
    }

    @Test
    fun testInvalidateLinesKeepsNonEmptyLeafEndingAtEditVerified() {
        val doc = parseLines("P1\nP1b\n# P2")
        val p1 = doc.children[0]
        assertEquals(listOf(2, 1), doc.children.map { it.lineCount })

        // Edit line 2 ("P2"). p1 ends right before the edit, so it stays verified and becomes the
        // active child.
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 3, newLineCount = 1)
        assertEquals(2, doc.lineCount)
        assertEquals(1, doc.children.size)
        assertEquals(listOf<ParseBlock>(p1), doc.children)
    }

    @Test
    fun testDividerVariantsAndNonMatchingLines() {
        val doc =
            parseLines(
                """
                ---
                ***
                ___
                   - - -
                *  *  *  *
                --
                -*-
                --- trailing
                """
                    .trimIndent()
            )
        // First 5 lines are DividerParseBlocks; last 3 lines merge into a single
        // ParagraphParseBlock
        assertEquals(6, doc.children.size)
        assertEquals(8, doc.lineCount)
        for (i in 0 until 5) {
            val divider = assertIs<DividerParseBlock>(doc.children[i])
            assertEquals(1, divider.lineCount)
        }
        val p = assertIs<ParagraphParseBlock>(doc.children[5])
        assertEquals("--\n-*-\n--- trailing", p.content.toString())
        assertEquals(3, p.lineCount)
    }

    @Test
    fun testSetextHeadingPrecedenceOverDividerAndParagraphInterruption() {
        val doc =
            parseLines(
                """
                Setext H2
                ---
                Interrupted by asterisks
                ***
                Interrupted by spaced dashes
                - - -
                After divider
                """
                    .trimIndent()
            )
        assertEquals(6, doc.children.size)
        assertEquals(7, doc.lineCount)

        val h2 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Setext H2", h2.content.toString())
        assertEquals(2, h2.headingLevel)
        assertEquals(2, h2.lineCount)

        val p1 = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("Interrupted by asterisks", p1.content.toString())
        assertEquals(0, p1.headingLevel)
        assertEquals(1, p1.lineCount)

        assertIs<DividerParseBlock>(doc.children[2])

        val p2 = assertIs<ParagraphParseBlock>(doc.children[3])
        assertEquals("Interrupted by spaced dashes", p2.content.toString())
        assertEquals(0, p2.headingLevel)
        assertEquals(1, p2.lineCount)

        assertIs<DividerParseBlock>(doc.children[4])

        val p3 = assertIs<ParagraphParseBlock>(doc.children[5])
        assertEquals("After divider", p3.content.toString())
        assertEquals(1, p3.lineCount)
    }

    @Test
    fun testFencedCodeBlockBackticksAndTildes() {
        val doc =
            parseLines(
                """
                ```kotlin
                fun main() {
                    println("Hello")
                }
                ```
                ~~~c\+\+ extra info
                int x = 0;
                ~~~~
                """
                    .trimIndent()
            )
        assertEquals(2, doc.children.size)
        assertEquals(8, doc.lineCount)

        val kotlinCode = assertIs<FencedCodeBlockParseBlock>(doc.children[0])
        assertTrue(kotlinCode.isClosed)
        assertEquals('`', kotlinCode.fenceChar)
        assertEquals(3, kotlinCode.fenceLength)
        assertEquals("kotlin", kotlinCode.language)
        assertEquals("fun main() {\n    println(\"Hello\")\n}", kotlinCode.getCodeContent())
        assertEquals(5, kotlinCode.lineCount)

        val cppCode = assertIs<FencedCodeBlockParseBlock>(doc.children[1])
        assertTrue(cppCode.isClosed)
        assertEquals('~', cppCode.fenceChar)
        assertEquals(3, cppCode.fenceLength)
        assertEquals("c++", cppCode.language)
        assertEquals("int x = 0;", cppCode.getCodeContent())
        assertEquals(3, cppCode.lineCount)
    }

    @Test
    fun testFencedCodeBlockIndentStrippingAndClosingRules() {
        val doc =
            parseLines(
                """
                  ````
                    line with 4 spaces (2 stripped)
                  ```
                  ```` trailing text is not a closing fence
                   ````
                After code block
                """
                    .trimIndent()
            )
        assertEquals(2, doc.children.size)
        assertEquals(6, doc.lineCount)

        val code = assertIs<FencedCodeBlockParseBlock>(doc.children[0])
        assertTrue(code.isClosed)
        assertEquals(4, code.fenceLength)
        assertEquals(2, code.fenceIndent)
        assertNull(code.language)
        assertEquals(
            "  line with 4 spaces (2 stripped)\n```\n```` trailing text is not a closing fence",
            code.getCodeContent(),
        )
        assertEquals(5, code.lineCount)

        val after = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("After code block", after.content.toString())
        assertEquals(1, after.lineCount)
    }

    @Test
    fun testBacktickFenceRejectsBacktickInInfoString() {
        val doc =
            parseLines(
                """
                ```foo`bar
                ~~~foo`bar
                inside tilde block
                ~~~
                """
                    .trimIndent()
            )
        assertEquals(2, doc.children.size)
        val p = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("```foo`bar", p.content.toString())

        val tildeCode = assertIs<FencedCodeBlockParseBlock>(doc.children[1])
        assertEquals("foo`bar", tildeCode.language)
        assertEquals("inside tilde block", tildeCode.getCodeContent())
    }

    @Test
    fun testUnclosedFencedCodeBlockRunsToEndOfDocument() {
        // Without a closing fence, the code block takes every remaining line: blank lines and lines
        // that would otherwise start other blocks are code, including blank lines at the end.
        val doc = parseLines("```kotlin\ncode\n\n# not a heading\n***\n    indented\n\n")
        val code = assertIs<FencedCodeBlockParseBlock>(doc.children.single())
        assertFalse(code.isClosed)
        assertEquals("kotlin", code.language)
        assertEquals(8, code.lineCount)
        assertEquals("code\n\n# not a heading\n***\n    indented\n\n", code.getCodeContent())
        // The block ends with 2 blank lines, but they are code, not a separator before the next
        // block.
        assertEquals(2, code.countTrailingEmptyLines())
        assertTrue(code.consumesTrailingEmptyLines)
    }

    @Test
    fun testIndentedCodeBlockAndTrailingBlankLines() {
        // A line with 4+ spaces and no non-whitespace characters is an empty line, not a code block
        assertFalse(LineScanner("    ").matchesIndentedCodeBlock())
        val leadingEmptyDoc = parseLines("      \n    val a = 1")
        assertEquals(2, leadingEmptyDoc.children.size)
        assertIs<EmptyLineParseBlock>(leadingEmptyDoc.children[0])
        assertIs<IndentedCodeBlockParseBlock>(leadingEmptyDoc.children[1])

        val doc =
            parseLines(
                "    val a = 1\n" +
                    "  \n" +
                    "      val b = 2\n" +
                    "\n" +
                    "   \n" +
                    "Paragraph after indented code"
            )
        assertEquals(2, doc.children.size)
        assertEquals(6, doc.lineCount)

        val code = assertIs<IndentedCodeBlockParseBlock>(doc.children[0])
        // The internal blank line is code. The trailing 2 blank lines stay in the block's lines
        // when the paragraph line closes it, but they are not code: they are reported as trailing
        // empty lines instead.
        assertEquals("val a = 1\n\n  val b = 2", code.getCodeContent())
        assertEquals(5, code.lineCount)
        assertEquals(2, code.countTrailingEmptyLines())

        val p = assertIs<ParagraphParseBlock>(doc.children[1])
        assertEquals("Paragraph after indented code", p.content.toString())
        assertEquals(1, p.lineCount)
    }

    @Test
    fun testIndentedCodeBlankLineKeepsSpacesBeyondIndentAndTrailingBlankLinesAtEnd() {
        // An interior blank line keeps the spaces beyond the 4-space indent. Blank lines at the end
        // of the document, with no closing line, stay in the block's lines but are not code.
        val doc = parseLines("    a\n      \n    b\n\n  ")
        val code = assertIs<IndentedCodeBlockParseBlock>(doc.children.single())
        assertEquals("a\n  \nb", code.getCodeContent())
        assertEquals(5, code.lineCount)
        assertEquals(2, code.countTrailingEmptyLines())
    }

    @Test
    fun testIndentedLineDoesNotInterruptParagraph() {
        val doc =
            parseLines(
                """
                Paragraph line 1
                    indented continuation line
                """
                    .trimIndent()
            )
        assertEquals(1, doc.children.size)
        val p = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Paragraph line 1\nindented continuation line", p.content.toString())
        assertEquals(2, p.lineCount)
    }

    @Test
    fun testIncrementalParseConvergesAcrossCodeBlockAndDivider() {
        val context = ParseContext()
        val doc =
            parseLines(
                """
                Intro paragraph
                ***
                ```kotlin
                val x = 1
                ```
                Trailing paragraph
                """
                    .trimIndent(),
                context,
            )
        assertEquals(4, doc.children.size)
        val originalDivider = doc.children[1]
        val originalCodeBlock = doc.children[2]
        val originalTrailing = doc.children[3]

        // Edit line 0 ("Intro paragraph" -> "Updated intro")
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 1)
        context.resetLine(LineScanner("Updated intro"))
        val r0 = doc.processLine(context)
        assertTrue(r0.accepted)
        assertFalse(r0.isConverged)

        // Line 1 ("***") opens DividerParseBlock at delta == 0 and converges!
        context.resetLine(LineScanner("***"))
        val r1 = doc.processLine(context)
        assertTrue(r1.accepted)
        assertTrue(r1.isConverged)

        assertEquals(6, doc.lineCount)
        assertSame(originalDivider, doc.children[1])
        assertSame(originalCodeBlock, doc.children[2])
        assertSame(originalTrailing, doc.children[3])
    }

    @Test
    fun testIncrementalParseConvergesAfterIndentedCodeTrailingBlankLines() {
        val context = ParseContext()
        val doc =
            parseLines(
                "    val a = 1\n" + "\n" + "Paragraph 1\n" + "\n" + "Paragraph 2",
                context,
            )
        // The blank line after the code stays in the code block (lines 0..1).
        assertEquals(4, doc.children.size)
        assertEquals(5, doc.lineCount)
        val originalP1 = doc.children[1]
        val originalP2 = doc.children[3]

        // Edit line 0 ("    val a = 1" -> "    val a = 2")
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 1)

        // Line 0: opens new IndentedCodeBlockParseBlock
        context.resetLine(LineScanner("    val a = 2"))
        assertFalse(doc.processLine(context).isConverged)

        // Line 1 (""): tentatively absorbed by IndentedCodeBlockParseBlock
        context.resetLine(LineScanner(""))
        assertFalse(doc.processLine(context).isConverged)

        // Line 2 ("Paragraph 1"): closes the code block, and Paragraph 1 converges with originalP1
        // without needing to re-parse lines 3..4!
        context.resetLine(LineScanner("Paragraph 1"))
        val r2 = doc.processLine(context)
        assertTrue(r2.accepted)
        assertTrue(r2.isConverged)

        assertEquals(5, doc.lineCount)
        assertEquals(4, doc.children.size)
        val code = assertIs<IndentedCodeBlockParseBlock>(doc.children[0])
        assertEquals("val a = 2", code.getCodeContent())
        assertEquals(1, code.countTrailingEmptyLines())
        assertSame(originalP1, doc.children[1])
        assertSame(originalP2, doc.children[3])
    }

    @Test
    fun testBlockQuoteNestedAndLazyContinuation() {
        val doc =
            parseLines(
                """
                > Quote line 1
                lazy continuation line 2
                >
                > > Nested quote
                nested lazy continuation

                Outside paragraph
                """
                    .trimIndent()
            )
        assertEquals(3, doc.children.size)
        assertEquals(7, doc.lineCount)

        val outerQuote = doc.children[0].assertBlockQuote()
        assertEquals(5, outerQuote.lineCount)
        assertEquals(3, outerQuote.children.size)
        val quoteParagraph =
            outerQuote.children[0].assertParagraphText("Quote line 1\nlazy continuation line 2")
        assertEquals(2, quoteParagraph.lineCount)
        assertIs<EmptyLineParseBlock>(
            outerQuote.children[1],
            "The \">\" line is blank in the quote",
        )
        val nestedQuote = outerQuote.children[2].assertBlockQuote()
        assertEquals(2, nestedQuote.lineCount)
        nestedQuote
            .assertSingleChild()
            .assertParagraphText("Nested quote\nnested lazy continuation")

        assertIs<EmptyLineParseBlock>(doc.children[1], "The blank line after the quote")
        doc.children[2].assertParagraphText("Outside paragraph")
    }

    @Test
    fun testBlockQuoteMarkerWithoutSpace() {
        val quote = parseLines(">foo").assertSingleChild().assertBlockQuote()
        quote.assertSingleChild().assertParagraphText("foo")

        val outer = parseLines(">>foo").assertSingleChild().assertBlockQuote()
        outer.assertSingleChild().assertBlockQuote().assertSingleChild().assertParagraphText("foo")

        val continued = parseLines(">bar\n> baz").assertSingleChild().assertBlockQuote()
        continued.assertSingleChild().assertParagraphText("bar\nbaz")

        val headingQuote = parseLines("># Foo").assertSingleChild().assertBlockQuote()
        headingQuote.assertSingleChild().assertHeading(level = 1, text = "Foo")
    }

    @Test
    fun testTabAfterQuoteMarkerLeavesIndentedCode() {
        // ">" takes one column of the first tab; the rest of the line is indented code.
        val quote = parseLines(">\t\tfoo").assertSingleChild().assertBlockQuote()
        quote.assertSingleChild().assertCodeContent("  foo")
    }

    @Test
    fun testQuoteNestsInQuoteOnSameLine() {
        val outer = parseLines("> > a").assertSingleChild().assertBlockQuote()
        val inner = outer.assertSingleChild().assertBlockQuote()
        inner.assertSingleChild().assertParagraphText("a")
        assertEquals(1, inner.lineCount)
    }

    @Test
    fun testBlankLineSeparatesBlockQuotes() {
        val doc = parseLines("> foo\n\n> bar")
        assertEquals(3, doc.children.size)
        doc.children[0].assertBlockQuote().assertSingleChild().assertParagraphText("foo")
        assertIs<EmptyLineParseBlock>(doc.children[1], "The blank line between the quotes")
        doc.children[2].assertBlockQuote().assertSingleChild().assertParagraphText("bar")
    }

    @Test
    fun testBlankLineWithoutMarkerClosesQuoteAfterBlankQuoteLine() {
        // Only paragraph text continues lazily: a blank line without ">" after a ">" line closes
        // the
        // quote, even though the quote's last block is a blank line too.
        var doc = parseLines("> foo\n>\n\n> bar")
        assertEquals(3, doc.children.size)
        val first = doc.children[0].assertBlockQuote()
        assertEquals(2, first.lineCount)
        first.children[0].assertParagraphText("foo")
        assertIs<EmptyLineParseBlock>(doc.children[1], "The blank line closes the first quote")
        doc.children[2].assertBlockQuote().assertSingleChild().assertParagraphText("bar")

        // The same one level down: a ">" line closes the inner quote.
        doc = parseLines("> > a\n> >\n>\n> > b")
        val outer = doc.assertSingleChild().assertBlockQuote()
        assertEquals(3, outer.children.size)
        outer.children[0].assertBlockQuote()
        assertIs<EmptyLineParseBlock>(outer.children[1], "The \">\" line closes the inner quote")
        outer.children[2].assertBlockQuote().assertSingleChild().assertParagraphText("b")
    }

    @Test
    fun testParagraphAfterBlankQuoteLineIsOutsideTheQuote() {
        val doc = parseLines("> bar\n>\nbaz")
        val quote = doc.children[0].assertBlockQuote()
        quote.children[0].assertParagraphText("bar")
        assertIs<EmptyLineParseBlock>(quote.children[1], "The blank \">\" line")
        doc.children[1].assertParagraphText("baz")
    }

    @Test
    fun testQuoteMarkerTakesOneSpaceBeforeIndentedCode() {
        val doc = parseLines(">     code\n\n>    not code")
        doc.children[0].assertBlockQuote().assertSingleChild().assertCodeContent("code")
        doc.children[2].assertBlockQuote().assertSingleChild().assertParagraphText("not code")
    }

    @Test
    fun testQuoteContinuationAllowsUpToThreeSpaces() {
        // With 4 spaces, "> c" is lazy paragraph text.
        val quote = parseLines("> a\n   > b\n    > c").assertSingleChild().assertBlockQuote()
        quote.assertSingleChild().assertParagraphText("a\nb\n> c")
    }

    @Test
    fun testBlockQuoteInterruptsParagraph() {
        val doc = parseLines("foo\n> bar")
        doc.children[0].assertParagraphText("foo")
        doc.children[1].assertBlockQuote().assertSingleChild().assertParagraphText("bar")

        val indentedQuote = parseLines("   > a").assertSingleChild().assertBlockQuote()
        indentedQuote.assertSingleChild().assertParagraphText("a")
    }

    @Test
    fun testInterruptingBlockOpensInOuterQuoteWhenInnerQuoteDoesNotMatch() {
        // Only the outer ">" matches "> # h": the heading closes the inner quote and opens in the
        // outer one.
        var outer = parseLines("> > a\n> # h").assertSingleChild().assertBlockQuote()
        assertEquals(2, outer.children.size)
        outer.children[0].assertBlockQuote().assertSingleChild().assertParagraphText("a")
        outer.children[1].assertHeading(level = 1, text = "h")

        // Plain text continues the inner paragraph lazily.
        outer = parseLines("> > a\n> b").assertSingleChild().assertBlockQuote()
        val inner = outer.assertSingleChild().assertBlockQuote()
        inner.assertSingleChild().assertParagraphText("a\nb")
    }

    @Test
    fun testBlockQuoteLazinessAtEachDepth() {
        // A quoted line after a lazy line continues the same paragraph.
        val quote = parseLines("> bar\nbaz\n> foo").assertSingleChild().assertBlockQuote()
        quote.assertSingleChild().assertParagraphText("bar\nbaz\nfoo")

        // Lazy lines at the middle and outer depth continue the innermost paragraph.
        val outer = parseLines(">>> foo\n> bar\n>>baz").assertSingleChild().assertBlockQuote()
        val middle = outer.assertSingleChild().assertBlockQuote()
        val inner = middle.assertSingleChild().assertBlockQuote()
        inner.assertSingleChild().assertParagraphText("foo\nbar\nbaz")
    }

    @Test
    fun testBlockQuoteWithBlankLines() {
        val blankQuote = parseLines(">\n>  \n> ").assertSingleChild().assertBlockQuote()
        val blank =
            assertIs<EmptyLineParseBlock>(blankQuote.assertSingleChild(), "Only blank lines")
        assertEquals(3, blank.lineCount)

        val quote = parseLines(">\n> foo\n>  ").assertSingleChild().assertBlockQuote()
        assertEquals(3, quote.children.size)
        assertIs<EmptyLineParseBlock>(quote.children[0], "The leading blank \">\" line")
        quote.children[1].assertParagraphText("foo")
        assertIs<EmptyLineParseBlock>(quote.children[2], "The trailing blank \">\" line")
    }

    @Test
    fun testBlockQuoteReportsNoTrailingEmptyLines() {
        // ">" lines are blank inside the quote but not for the quote's parent.
        val quote = parseLines("> a\n>\n>").assertSingleChild().assertBlockQuote()
        val blank = assertIs<EmptyLineParseBlock>(quote.children[1], "The blank \">\" lines")
        assertEquals(2, blank.countTrailingEmptyLines())
        assertEquals(
            0,
            quote.countTrailingEmptyLines(),
            "Blank lines the quote reports to its parent",
        )
    }

    @Test
    fun testIncrementalParseAroundBlockQuote() {
        val edits =
            listOf(
                // Deleting the blank line merges two quotes.
                Triple(listOf("> a", "", "> b"), 1, emptyList()),
                // A blank line splits a quote.
                Triple(listOf("> a", "> b", "> c"), 1, listOf("")),
                // A lazy line becomes a heading that closes the quote.
                Triple(listOf("> a", "b", "c"), 1, listOf("# b")),
                // A blank ">" line inside the quote becomes text that closes it.
                Triple(listOf("> a", ">", "> b", "x"), 1, listOf("y")),
                // Deleting the quote leaves a single blank line.
                Triple(listOf("", "> a"), 1, emptyList()),
            )
        for ((oldLines, line, replacement) in edits) {
            assertIncrementalParseMatchesFullParse(oldLines, line, line + 1, replacement)
        }
    }

    @Test
    fun testTryStartInterruptingBlockMatchesOnlyInterruptingBlocks() {
        // Each line either starts a block that can interrupt a paragraph, or consumes nothing.
        val context = ParseContext()
        for ((line, expected) in
            listOf(
                "" to true,
                "# h" to true,
                "```" to true,
                "***" to true,
                "> q" to true,
                "- a" to true,
                "1. a" to true,
                "2. a" to false,
                "-" to false,
                "#NotHeading" to false,
                "    code" to false,
                "    # indented" to false,
                "text" to false,
            )) {
            val scanner = LineScanner(line)
            val lineStartPosition = scanner.position
            context.resetLine(scanner)
            val block = context.tryStartInterruptingBlock()
            assertEquals(expected, block != null, "line \"$line\"")
            if (block == null) {
                assertEquals(
                    lineStartPosition,
                    scanner.position,
                    "line \"$line\" must not be consumed",
                )
            }
        }
    }

    @Test
    fun testLazyLineClosesCodeAndSetextInBlockQuote() {
        // "---" on a lazy line is a divider, not a setext underline.
        var doc = parseLines("> foo\n---")
        val quote = doc.children[0].assertBlockQuote()
        quote.assertSingleChild().assertParagraphText("foo")
        assertIs<DividerParseBlock>(doc.children[1], "A lazy \"---\" is a divider")

        // "===" is never a divider, so on a lazy line it stays paragraph text in the quote.
        doc = parseLines("> foo\n===")
        val quoteWithLazyLine = doc.assertSingleChild().assertBlockQuote()
        quoteWithLazyLine.assertSingleChild().assertParagraphText("foo\n===")

        // With the quote marker, "---" is a setext underline inside the quote.
        doc = parseLines("> foo\n> ---")
        val quoteWithHeading = doc.assertSingleChild().assertBlockQuote()
        quoteWithHeading.assertSingleChild().assertHeading(level = 2, text = "foo")

        // Only paragraphs continue lazily: indented and fenced code close with the quote.
        doc = parseLines(">     foo\n    bar")
        val quotedCode = doc.children[0].assertBlockQuote().assertSingleChild()
        quotedCode.assertCodeContent("foo")
        doc.children[1].assertCodeContent("bar")

        doc = parseLines("> ```\nfoo\n```")
        assertEquals(3, doc.children.size)
        val quotedFence = doc.children[0].assertBlockQuote().assertSingleChild()
        assertIs<FencedCodeBlockParseBlock>(quotedFence, "The fenced code block in the quote")
        doc.children[1].assertParagraphText("foo")
        assertIs<FencedCodeBlockParseBlock>(doc.children[2], "A lazy fence opens a new block")
    }

    @Test
    fun testBulletAndOrderedListGroupingAndTypeSplitting() {
        val doc =
            parseLines(
                """
                - dash 1
                - dash 2
                * star 1
                1. dot 1
                2. dot 2
                1) paren 1
                + plus 1
                """
                    .trimIndent()
            )
        val markers = doc.children.map { it.assertList().marker.bulletOrDelimiter }
        assertEquals(listOf('-', '*', '.', ')', '+'), markers)

        val dashList = doc.children[0].assertList()
        assertFalse(dashList.marker.isOrdered)
        assertEquals(2, dashList.children.size)
        dashList.children[0].assertListItem().assertSingleChild().assertParagraphText("dash 1")
        dashList.children[1].assertListItem().assertSingleChild().assertParagraphText("dash 2")

        val dotList = doc.children[2].assertList()
        assertTrue(dotList.marker.isOrdered)
        assertEquals(1, dotList.marker.startNumber)
        val secondDotItem = dotList.children[1].assertListItem()
        assertEquals(2, secondDotItem.marker.startNumber)
    }

    @Test
    fun testListMarkerStartsBareItem() {
        // A list marker starts a bare item; the container that accepts it decides whether it joins
        // a list or is wrapped in a new one.
        val context = ParseContext()
        context.resetLine(LineScanner("3. a"))
        val item = context.startBlock().assertListItem()
        assertEquals(3, item.marker.startNumber)
        item.assertSingleChild().assertParagraphText("a")
    }

    @Test
    fun testListItemJoinsListOfSameType() {
        val list = parseLines("- a\n- b").assertSingleChild().assertList()
        assertEquals(2, list.children.size)
    }

    @Test
    fun testDifferentBulletStartsNewList() {
        // The "-" list refuses the "*" item, and the document wraps it in a new list.
        val doc = parseLines("- a\n* b")
        val markers = doc.children.map { it.assertList().marker.bulletOrDelimiter }
        assertEquals(listOf('-', '*'), markers)
    }

    @Test
    fun testDifferentDelimiterOnLazyLineStartsNewList() {
        // The "." list refuses the ")" item, so it starts a new list, even though it does not start
        // at 1 (any list item may start on a lazy line).
        val doc = parseLines("1. foo\n2. bar\n3) baz")
        val dotList = doc.children[0].assertList()
        assertEquals(2, dotList.children.size)
        dotList.children[1].assertListItem().assertSingleChild().assertParagraphText("bar")
        val parenList = doc.children[1].assertList()
        assertEquals(')', parenList.marker.bulletOrDelimiter)
        assertEquals(3, parenList.marker.startNumber)
    }

    @Test
    fun testItemAsFirstChildOfNewContainerIsWrappedInList() {
        val outerList = parseLines("- - a").assertSingleChild().assertList()
        val outerItem = outerList.assertSingleChild().assertListItem()
        assertEquals(1, outerItem.lineCount)
        val innerItem = outerItem.assertSingleChild().assertList().assertSingleChild()
        innerItem.assertListItem().assertSingleChild().assertParagraphText("a")

        val quote = parseLines("> - a").assertSingleChild().assertBlockQuote()
        quote.assertSingleChild().assertList().assertSingleChild().assertListItem()
    }

    @Test
    fun testInterruptingBlockMovesUpToFirstContainerThatAccepts() {
        // Matched line: the heading opens in the item whose indentation matched.
        var list = parseLines("- a\n  # h").assertSingleChild().assertList()
        var item = list.assertSingleChild().assertListItem()
        assertEquals(2, item.children.size)
        item.children[1].assertHeading(level = 1, text = "h")

        // Lazy line: the item does not match, and the list refuses a heading, so it opens in the
        // document.
        var doc = parseLines("- a\n# h")
        doc.children[0].assertList()
        doc.children[1].assertHeading(level = 1, text = "h")

        // Lazy line out of a block quote: the new list opens in the document.
        doc = parseLines("> a\n- b")
        doc.children[0].assertBlockQuote()
        doc.children[1].assertList()

        // Sibling item inside a block quote: the list inside the quote takes the next item.
        val quote = parseLines("> - a\n> - b").assertSingleChild().assertBlockQuote()
        val quotedList = quote.assertSingleChild().assertList()
        assertEquals(2, quotedList.children.size)

        // Sibling item outside the block quote: a separate list in the document.
        doc = parseLines("> - a\n- b")
        doc.children[0].assertBlockQuote()
        val separateList = doc.children[1].assertList()
        assertEquals(1, separateList.children.size)

        // Nested lists: the item joins the list whose type matches, at the matched depth.
        list = parseLines("- a\n  * b\n  * c\n- d").assertSingleChild().assertList()
        assertEquals(2, list.children.size)
        item = list.children[0].assertListItem()
        val sublist = item.children[1].assertList()
        assertEquals(2, sublist.children.size)
    }

    @Test
    fun testRejectedBlockIsProposedToAncestorWithoutRebuilding() {
        // The list starts the heading, refuses it, and proposes it to the document, which reuses
        // it, so no block ID is spent on a second copy.
        val context = ParseContext()
        val doc = parseLines("- a\n\n# heading", context)
        val heading = doc.children[1].assertHeading(level = 1, text = "heading")
        // IDs: item 0, paragraph 1, list 2 (wrapping the item), blank 3, heading 4.
        assertEquals(4L, heading.id, "The document reuses the proposed heading")
        assertEquals(5L, context.generateBlockId(), "No second heading was built")
    }

    @Test
    fun testTrailingBlankLineAfterLastItemKeepsListTight() {
        // The blank line stays in the last item; no item follows it.
        val doc = parseLines("- item 1\n- item 2\n\nOutside paragraph")
        val list = doc.children[0].assertList()
        assertTrue(list.computeIsTight())
        assertEquals(3, list.lineCount)
        assertEquals(1, list.countTrailingEmptyLines())
        doc.children[1].assertParagraphText("Outside paragraph")
    }

    @Test
    fun testBlankLineBetweenItemsMakesListLoose() {
        val list = parseLines("- item 1\n\n- item 2").assertSingleChild().assertList()
        assertEquals(2, list.children.size)
        assertEquals(1, list.children[0].assertListItem().countTrailingEmptyLines())
        assertFalse(list.computeIsTight())

        // Any number of blank lines between items keeps one list.
        val threeItems = parseLines("- foo\n\n- bar\n\n\n- baz").assertSingleChild().assertList()
        assertEquals(3, threeItems.children.size)
        assertFalse(threeItems.computeIsTight())
    }

    @Test
    fun testBlankLineInsideItemMakesListLoose() {
        val list = parseLines("- para 1\n\n  para 2\n- item 2").assertSingleChild().assertList()
        assertEquals(2, list.children.size)
        assertFalse(list.computeIsTight())

        // Blank lines in the middle of indented code inside an item are code, but the blank line
        // between the paragraph and the code makes the list loose.
        val codeList =
            parseLines("- Foo\n\n      bar\n\n\n      baz").assertSingleChild().assertList()
        val item = codeList.assertSingleChild().assertListItem()
        item.children[0].assertParagraphText("Foo")
        assertIs<EmptyLineParseBlock>(item.children[1], "The blank line before the code")
        item.children[2].assertCodeContent("bar\n\n\nbaz")
        assertFalse(codeList.computeIsTight())
    }

    @Test
    fun testBlankLineAfterSublistSeparatesOuterItemBlocks() {
        // The blank line stays at the end of the sublist and separates it from "baz": the outer
        // list
        // is loose, the sublist stays tight.
        val outerList = parseLines("* foo\n  * bar\n\n  baz").assertSingleChild().assertList()
        assertFalse(outerList.computeIsTight())
        val outerItem = outerList.assertSingleChild().assertListItem()
        assertEquals(3, outerItem.children.size)
        val sublist = outerItem.children[1].assertList()
        assertTrue(sublist.computeIsTight())
        assertEquals(1, sublist.countTrailingEmptyLines())
        outerItem.children[2].assertParagraphText("baz")
    }

    @Test
    fun testBlankQuoteLineInsideItemKeepsListTight() {
        val list = parseLines("* a\n  > b\n  >\n* c").assertSingleChild().assertList()
        assertTrue(list.computeIsTight())
    }

    @Test
    fun testBlankLineAfterQuoteInItemSeparatesItems() {
        // The blank line without ">" closes the quote and stays in the item, so it separates the
        // two items.
        val list = parseLines("- > a\n  >\n\n- b").assertSingleChild().assertList()
        assertEquals(2, list.children.size)
        val firstItem = list.children[0].assertListItem()
        firstItem.children[0].assertBlockQuote()
        assertIs<EmptyLineParseBlock>(firstItem.children[1], "The blank line after the quote")
        assertFalse(list.computeIsTight())
    }

    @Test
    fun testNestedListEndingInsideUnclosedFenceKeepsOuterListTight() {
        // The blank line is content of the unclosed fence, so it does not separate the outer items.
        val outerList = parseLines("- a\n  - ```\n\n- b").assertSingleChild().assertList()
        assertEquals(2, outerList.children.size)
        assertTrue(outerList.computeIsTight())
    }

    @Test
    fun testEmptyItemFollowedByBlankLineEndsItem() {
        // The blank line stays in the item, and the indented line after it is not part of it.
        val doc = parseLines("-\n\n  not in list item")
        assertEquals(2, doc.children.size)
        val list = doc.children[0].assertList()
        assertEquals(2, list.lineCount)
        val emptyItem = list.assertSingleChild().assertListItem()
        assertEquals(2, emptyItem.lineCount)
        // The marker line is not a trailing blank line; only the blank line after it is.
        assertEquals(1, emptyItem.countTrailingEmptyLines())
        doc.children[1].assertParagraphText("not in list item")
    }

    @Test
    fun testItemStartingWithFiveSpacesOpensIndentedCode() {
        val list =
            parseLines("-     val x = 1\n\n  continuation para").assertSingleChild().assertList()
        val item = list.assertSingleChild().assertListItem()
        assertEquals(2, item.marker.contentIndentColumn)
        // The blank line stays at the end of the code block and separates it from the paragraph,
        // which makes the list loose.
        assertEquals(2, item.children.size)
        item.children[0].assertCodeContent("val x = 1")
        assertEquals(1, item.children[0].countTrailingEmptyLines())
        item.children[1].assertParagraphText("continuation para")
        assertFalse(list.computeIsTight())
    }

    @Test
    fun testListParagraphInterruptionRules() {
        val doc =
            parseLines(
                """
                Paragraph text
                2. ordered not starting at 1 does not interrupt
                *
                1.
                1. ordered starting at 1 interrupts
                ***
                2. ordered starting at 2 at block start
                """
                    .trimIndent()
            )
        assertEquals(4, doc.children.size)
        val paragraph =
            doc.children[0].assertParagraphText(
                "Paragraph text\n2. ordered not starting at 1 does not interrupt\n*\n1."
            )
        assertEquals(4, paragraph.lineCount)

        val interruptingList = doc.children[1].assertList()
        assertEquals(1, interruptingList.marker.startNumber)
        assertEquals(1, interruptingList.children.size)

        assertIs<DividerParseBlock>(doc.children[2], "The divider closes the list")

        val listStartingAt2 = doc.children[3].assertList()
        assertEquals(2, listStartingAt2.marker.startNumber)
    }

    @Test
    fun testOrderedItemStartingAtZeroDoesNotInterruptParagraph() {
        val paragraph = parseLines("a\n0. b").assertSingleChild()
        paragraph.assertParagraphText("a\n0. b")
    }

    @Test
    fun testIncrementalParseInsideListConvergesAtNextSiblingItem() {
        val context = ParseContext()
        val doc = parseLines("- item 1\n- item 2\n- item 3\n- item 4", context)
        val list = doc.assertSingleChild().assertList()
        assertEquals(4, list.children.size)
        val originalItem1 = list.children[0]
        val originalItem3 = list.children[2]
        val originalItem4 = list.children[3]

        // Edit line 1 ("- item 2" -> "- item 2 updated"); line 0 ("- item 1") stays verified.
        doc.invalidateLines(oldLineStart = 1, oldLineEndExclusive = 2, newLineCount = 1)
        assertEquals(1, doc.lineCount)
        assertEquals(1, list.lineCount)

        context.resetLine(LineScanner("- item 2 updated"))
        val editedLineResult = doc.processLine(context)
        assertTrue(editedLineResult.accepted)
        assertFalse(editedLineResult.isConverged)

        // Line 2 ("- item 3") opens a new item where the old one started and converges, without
        // re-parsing line 3.
        context.resetLine(LineScanner("- item 3"))
        val nextItemResult = doc.processLine(context)
        assertTrue(nextItemResult.accepted)
        assertTrue(nextItemResult.isConverged)

        assertEquals(4, doc.lineCount)
        assertEquals(4, list.children.size)
        assertSame(originalItem1, list.children[0])
        val updatedItem2 = list.children[1].assertListItem()
        updatedItem2.assertSingleChild().assertParagraphText("item 2 updated")
        assertSame(originalItem3, list.children[2])
        assertSame(originalItem4, list.children[3])
    }

    @Test
    fun testLazyContinuationDoesNotMatchListItemIndent() {
        // "    - m" fails the block quote prefix, so it can only be a lazy continuation. The list
        // item must not match its 2-space indent (which would expose "  - m" as a list marker); the
        // line continues the paragraph instead.
        val doc = parseLines("> - z\n    - m")
        val quote = doc.assertSingleChild().assertBlockQuote()
        val item = quote.assertSingleChild().assertList().assertSingleChild().assertListItem()
        item.assertSingleChild().assertParagraphText("z\n- m")
        assertEquals(2, doc.lineCount)
    }

    @Test
    fun testIncrementalParseWhenEditAfterBlankLineInListItem() {
        // The blank line stays in the item. Deleting "***" lets the end of the document follow the
        // item that holds it.
        assertIncrementalParseMatchesFullParse(
            oldLines = listOf("- a", "", "***"),
            start = 2,
            endExclusive = 3,
            replacement = emptyList(),
        )
        // Changing "- b" into an indented continuation makes the blank line separate two blocks
        // inside the first item instead of two items.
        assertIncrementalParseMatchesFullParse(
            oldLines = listOf("- a", "", "- b"),
            start = 2,
            endExclusive = 3,
            replacement = listOf("  c"),
        )
    }

    @Test
    fun testIncrementalParseWhenTheContainerAttachingAListLineChanges() {
        // "* b" was wrapped by the document; after the edit the new "*" list takes it instead, so
        // the old list starting at "* b" is discarded.
        assertIncrementalParseMatchesFullParse(
            oldLines = listOf("- a", "* b", "", "x"),
            start = 0,
            endExclusive = 1,
            replacement = listOf("* a"),
        )
        // The reverse: "* b" joined the "*" list, and after the edit the document wraps it.
        assertIncrementalParseMatchesFullParse(
            oldLines = listOf("* a", "* b", "", "x"),
            start = 0,
            endExclusive = 1,
            replacement = listOf("- a"),
        )
        // A different delimiter on a lazy line turns a sibling item into a new list.
        assertIncrementalParseMatchesFullParse(
            oldLines = listOf("1. foo", "2. bar", "3. baz", "", "x"),
            start = 2,
            endExclusive = 3,
            replacement = listOf("3) baz"),
        )
        // An item moving into and out of a block quote.
        assertIncrementalParseMatchesFullParse(
            oldLines = listOf("> - a", "> - b", "- c"),
            start = 1,
            endExclusive = 2,
            replacement = listOf("- b"),
        )
    }

    @Test
    fun testInterruptingBlockOnLazyLineOpensInOuterContainer() {
        // The block quote matches and the item does not: the heading and the divider open in the
        // quote, after the list, and the item's paragraph is not a setext heading.
        var quote = parseLines("> - a\n> # h").assertSingleChild().assertBlockQuote()
        quote.children[0].assertList()
        quote.children[1].assertHeading(level = 1, text = "h")

        quote = parseLines("> - a\n> ---").assertSingleChild().assertBlockQuote()
        val item = quote.children[0].assertList().assertSingleChild().assertListItem()
        val paragraph = item.assertSingleChild().assertParagraphText("a")
        assertEquals(0, paragraph.headingLevel, "Not a setext heading")
        assertIs<DividerParseBlock>(quote.children[1], "The lazy \"---\" is a divider")

        // On a lazy line out of a block quote, an ordered item not starting at 1 still starts a
        // list.
        val doc = parseLines("> a\n2. b")
        doc.children[0].assertBlockQuote()
        val list = doc.children[1].assertList()
        assertEquals(2, list.marker.startNumber)
    }

    @Test
    fun testLazyLineContinuesQuoteInsideListItemInsideQuote() {
        val outerQuote = parseLines("> 1. > Blockquote\ncontinued here.").assertSingleChild()
        val item =
            outerQuote.assertBlockQuote().assertSingleChild().assertList().assertSingleChild()
        val nestedQuote = item.assertListItem().assertSingleChild().assertBlockQuote()
        nestedQuote.assertSingleChild().assertParagraphText("Blockquote\ncontinued here.")
    }

    @Test
    fun testOrderedListStartNumberDigits() {
        val nineDigits = parseLines("123456789. ok").assertSingleChild().assertList()
        assertEquals(123456789, nineDigits.marker.startNumber)

        val tenDigits = parseLines("1234567890. not ok").assertSingleChild()
        tenDigits.assertParagraphText("1234567890. not ok")

        val zero = parseLines("0. ok").assertSingleChild().assertList()
        assertEquals(0, zero.marker.startNumber)

        val leadingZeros = parseLines("003. ok").assertSingleChild().assertList()
        assertEquals(3, leadingZeros.marker.startNumber)

        val negative = parseLines("-1. not ok").assertSingleChild()
        negative.assertParagraphText("-1. not ok")

        // Digits up to the end of the line, with no delimiter, are paragraph text.
        val digitsOnly = parseLines("123").assertSingleChild()
        digitsOnly.assertParagraphText("123")
    }

    @Test
    fun testListMarkerNeedsSpaceAfterIt() {
        val doc = parseLines("-one\n\n2.two")
        doc.children[0].assertParagraphText("-one")
        assertIs<EmptyLineParseBlock>(doc.children[1], "The blank line between the paragraphs")
        doc.children[2].assertParagraphText("2.two")
    }

    @Test
    fun testEmptyListItemsAsSiblings() {
        for (markdown in listOf("- foo\n-\n- bar", "1. foo\n2.\n3. bar")) {
            val list = parseLines(markdown).assertSingleChild().assertList()
            assertEquals(3, list.children.size, markdown)
            val emptyItem = list.children[1].assertListItem()
            assertIs<EmptyLineParseBlock>(
                emptyItem.assertSingleChild(),
                "The empty item's marker line",
            )
            assertTrue(list.computeIsTight(), markdown)
        }

        val singleItemList = parseLines("*").assertSingleChild().assertList()
        val singleEmptyItem = singleItemList.assertSingleChild().assertListItem()
        assertIs<EmptyLineParseBlock>(
            singleEmptyItem.assertSingleChild(),
            "The empty item's marker line",
        )

        // A blank line after an empty item separates items: the list is loose.
        val list = parseLines("* a\n*\n\n* c").assertSingleChild().assertList()
        assertEquals(3, list.children.size)
        assertFalse(list.computeIsTight())
    }

    @Test
    fun testListItemStartingWithBlankLine() {
        val doc = parseLines("-\n  foo\n-\n  ```\n  bar\n  ```\n-\n      baz")
        val list = doc.assertSingleChild().assertList()
        assertEquals(3, list.children.size)
        list.children[0].assertListItem().children[1].assertParagraphText("foo")
        list.children[1].assertListItem().children[1].assertCodeContent("bar")
        list.children[2].assertListItem().children[1].assertCodeContent("baz")
        assertTrue(list.computeIsTight())

        // Spaces after an empty marker do not count: the content indent is the marker plus one.
        val itemList = parseLines("-   \n  foo").assertSingleChild().assertList()
        val item = itemList.assertSingleChild().assertListItem()
        assertEquals(2, item.marker.contentIndentColumn)
        item.children[1].assertParagraphText("foo")
    }

    @Test
    fun testEmptyItemTakesContentOnNextLineButNotAfterBlankLine() {
        val list = parseLines("-\n  foo").assertSingleChild().assertList()
        val item = list.assertSingleChild().assertListItem()
        assertIs<EmptyLineParseBlock>(item.children[0], "The empty marker line")
        item.children[1].assertParagraphText("foo")
        assertTrue(list.computeIsTight())

        // An item can begin with at most one blank line: after a blank line, "foo" leaves the item,
        // and the blank lines stay in it.
        val doc = parseLines("-\n\n\n  foo")
        val emptyItem = doc.children[0].assertList().assertSingleChild().assertListItem()
        assertEquals(3, emptyItem.lineCount)
        doc.children[1].assertParagraphText("foo")

        // The marker line of an empty item is not a blank line between the item's children.
        val tightList = parseLines("-\n  foo\n- b").assertSingleChild().assertList()
        assertTrue(tightList.computeIsTight())
    }

    @Test
    fun testListItemTabs() {
        var list = parseLines("  - foo\n\n\tbar").assertSingleChild().assertList()
        var item = list.assertSingleChild().assertListItem()
        item.children[2].assertParagraphText("bar")

        list = parseLines("- foo\n\n\t\tbar").assertSingleChild().assertList()
        item = list.assertSingleChild().assertListItem()
        item.children[2].assertCodeContent("  bar")

        list = parseLines("-\t\tfoo").assertSingleChild().assertList()
        item = list.assertSingleChild().assertListItem()
        assertEquals(2, item.marker.contentIndentColumn)
        item.assertSingleChild().assertCodeContent("  foo")

        // Three nested levels, the last one indented with a tab.
        list = parseLines(" - foo\n   - bar\n\t - baz").assertSingleChild().assertList()
        val foo = list.assertSingleChild()
        val bar = foo.assertListItem().children[1].assertList().assertSingleChild()
        val baz = bar.assertListItem().children[1].assertList().assertSingleChild()
        baz.assertListItem().assertSingleChild().assertParagraphText("baz")
    }

    @Test
    fun testContentIndentIsRelativeToContainer() {
        var outer =
            parseLines("   > > 1.  one\n>>\n>>     two").assertSingleChild().assertBlockQuote()
        var inner = outer.assertSingleChild().assertBlockQuote()
        val item = inner.assertSingleChild().assertList().assertSingleChild().assertListItem()
        item.children[0].assertParagraphText("one")
        assertIs<EmptyLineParseBlock>(item.children[1], "The \">>\" line is blank inside the item")
        item.children[2].assertParagraphText("two")

        outer = parseLines(">>- one\n>>\n  >  > two").assertSingleChild().assertBlockQuote()
        inner = outer.assertSingleChild().assertBlockQuote()
        inner.children[0].assertList()
        inner.children[1].assertParagraphText("two")
    }

    @Test
    fun testListItemIndentationWidth() {
        // Not indented enough to continue the item.
        var doc = parseLines("- one\n\n two")
        doc.children[0].assertList()
        doc.children[1].assertParagraphText("two")

        // Wide padding: the content indent includes the padding.
        doc = parseLines(" -    one\n\n     two")
        doc.children[0].assertList()
        doc.children[1].assertCodeContent(" two")

        val wideList = parseLines(" -    one\n\n      two").assertSingleChild().assertList()
        var item = wideList.assertSingleChild().assertListItem()
        assertEquals(6, item.marker.contentIndentColumn)
        item.children[2].assertParagraphText("two")

        doc = parseLines("-    foo\n\n  bar")
        doc.children[0].assertList()
        doc.children[1].assertParagraphText("bar")

        // A multi-digit marker widens the content indent.
        val wideMarkerList = parseLines("10. a\n\n    b").assertSingleChild().assertList()
        item = wideMarkerList.assertSingleChild().assertListItem()
        item.children[2].assertParagraphText("b")

        doc = parseLines("10. a\n\n   b")
        doc.children[0].assertList()
        doc.children[1].assertParagraphText("b")

        // A blank line with more spaces than the indent is still a blank line inside the item.
        val looseList = parseLines("- a\n      \n  b").assertSingleChild().assertList()
        looseList.assertSingleChild().assertListItem().children[2].assertParagraphText("b")
        assertFalse(looseList.computeIsTight())
    }

    @Test
    fun testListSiblingIndentation() {
        // Up to 3 spaces of indent are still siblings; with 4, "- e" is paragraph text.
        val list = parseLines("- a\n - b\n  - c\n   - d\n    - e").assertSingleChild().assertList()
        assertEquals(4, list.children.size)
        list.children[3].assertListItem().assertSingleChild().assertParagraphText("d\n- e")

        // "3. c" is not indented enough for the item, and too indented to be a list marker.
        var doc = parseLines("1. a\n\n  2. b\n\n    3. c")
        val orderedList = doc.children[0].assertList()
        assertEquals(2, orderedList.children.size)
        doc.children[1].assertCodeContent("3. c")

        // A sublist needs the full width of "10) ".
        val outerItem =
            parseLines("10) foo\n    - bar").assertSingleChild().assertList().assertSingleChild()
        outerItem.assertListItem().children[1].assertList()

        doc = parseLines("10) foo\n   - bar")
        val markers = doc.children.map { it.assertList().marker.bulletOrDelimiter }
        assertEquals(listOf(')', '-'), markers)
    }

    @Test
    fun testListTightnessFromNestedBlocks() {
        // Blank lines inside a fenced code block are content, not separators.
        val fenceList =
            parseLines("- a\n- ```\n  b\n\n\n  ```\n- c").assertSingleChild().assertList()
        assertTrue(fenceList.computeIsTight())

        // A loose sublist does not make the outer list loose.
        var outer = parseLines("- a\n  - b\n\n    c\n- d").assertSingleChild().assertList()
        var sublist = outer.children[0].assertListItem().children[1].assertList()
        assertTrue(outer.computeIsTight())
        assertFalse(sublist.computeIsTight())

        val quoteAndFenceList =
            parseLines("- a\n  > b\n  ```\n  c\n  ```\n- d").assertSingleChild().assertList()
        assertTrue(quoteAndFenceList.computeIsTight())

        // A blank line between two blocks of an item makes the list loose.
        val looseList =
            parseLines("1. ```\n   foo\n   ```\n\n   bar").assertSingleChild().assertList()
        assertFalse(looseList.computeIsTight())

        // The blank line at the end of a nested list separates the outer items.
        outer =
            parseLines("- a\n  - b\n  - c\n\n- d\n  - e\n  - f").assertSingleChild().assertList()
        assertFalse(outer.computeIsTight())
        sublist = outer.children[0].assertListItem().children[1].assertList()
        assertTrue(sublist.computeIsTight())
        sublist = outer.children[1].assertListItem().children[1].assertList()
        assertTrue(sublist.computeIsTight())
    }

    @Test
    fun testListStaysTightWhenItemEndsInsideUnclosedFence() {
        val list = parseLines("- ```\n\n- b").assertSingleChild().assertList()
        val firstItem = list.children[0].assertListItem()
        val fence =
            assertIs<FencedCodeBlockParseBlock>(firstItem.assertSingleChild(), "The unclosed fence")
        assertEquals(1, fence.countTrailingEmptyLines())
        assertTrue(list.computeIsTight())
    }

    @Test
    fun testListsInsideListsAndHeadings() {
        // A list as the first block of an item, several levels deep on one line.
        val bulletItem =
            parseLines("1. - 2. foo").assertSingleChild().assertList().assertSingleChild()
        val bulletList = bulletItem.assertListItem().assertSingleChild().assertList()
        val orderedItem = bulletList.assertSingleChild().assertListItem()
        val innerList = orderedItem.assertSingleChild().assertList()
        assertEquals(2, innerList.marker.startNumber)
        assertEquals('.', innerList.marker.bulletOrDelimiter)

        // A setext heading inside an item.
        val headingList =
            parseLines("- # Foo\n- Bar\n  ---\n  baz").assertSingleChild().assertList()
        val secondItem = headingList.children[1].assertListItem()
        secondItem.children[0].assertHeading(level = 2, text = "Bar")
        secondItem.children[1].assertParagraphText("baz")

        // A bullet list interrupts a paragraph.
        val doc = parseLines("Foo\n- bar\n- baz")
        doc.children[0].assertParagraphText("Foo")
        val interruptingList = doc.children[1].assertList()
        assertEquals(2, interruptingList.children.size)

        // Two blank lines inside the innermost item keep the item open.
        val nestedList =
            parseLines("- foo\n  - bar\n    - baz\n\n\n      bim").assertSingleChild().assertList()
        val fooItem = nestedList.assertSingleChild()
        val barItem = fooItem.assertListItem().children[1].assertList().assertSingleChild()
        val bazItem = barItem.assertListItem().children[1].assertList().assertSingleChild()
        bazItem.assertListItem().children[2].assertParagraphText("bim")
    }

    @Test
    fun testWrappedListConvergesInsideRetainedBlockQuote() {
        val context = ParseContext()
        val doc = parseLines("> x\n>\n> # h\n> - b\n> - c", context)
        val quote = doc.assertSingleChild().assertBlockQuote()
        val oldList = quote.children[3].assertList()

        // Edit the heading. The quote is kept; at "> - b" it wraps the new item in a list again,
        // exactly where the old list started, so it converges and keeps the old list.
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 3, newLineCount = 1)
        context.resetLine(LineScanner("> # h2"))
        val headingResult = doc.processLine(context)
        assertFalse(headingResult.isConverged)
        context.resetLine(LineScanner("> - b"))
        val listResult = doc.processLine(context)
        assertTrue(listResult.isConverged)
        assertSame(oldList, quote.children[3])
        assertEquals(2, oldList.children.size)
    }

    @Test
    fun testConvergesOnListWrappedAfterRefusalByRecreatedList() {
        val context = ParseContext()
        val doc = parseLines("- a\n  more\n* b\n* c", context)
        val starList = doc.children[1].assertList()

        // The edit removes the "-" list (its paragraph covers lines 0 and 1), so re-parsing starts
        // at
        // line 0 and re-creates it. The document still wraps "* b" in a list of its own and
        // converges with the old one.
        doc.invalidateLines(oldLineStart = 1, oldLineEndExclusive = 2, newLineCount = 1)
        assertEquals(0, doc.lineCount)
        doc.feedLines(context, "- a", "  more2")
        context.resetLine(LineScanner("* b"))
        val result = doc.processLine(context)
        assertTrue(result.isConverged)
        assertSame(starList, doc.children[1])
    }

    @Test
    fun testIncrementalParseWhenListsSplitMergeOrMove() {
        val edits =
            listOf(
                // The item in a retained quote now joins the new list before it.
                Triple(listOf("> x", ">", "> a", "> - b"), 2, listOf("> - a")),
                // Splits a list into three, and merges them back.
                Triple(listOf("- a", "- b", "- c"), 1, listOf("* b")),
                Triple(listOf("- a", "* b", "- c"), 1, listOf("- b")),
                // "2. b" now interrupts nothing and starts a list, and the reverse.
                Triple(listOf("a", "2. b", "", "x"), 0, listOf("# h")),
                Triple(listOf("# h", "2. b"), 0, listOf("a")),
                // An item moves into a block quote.
                Triple(listOf("- a", "- b", "- c"), 1, listOf("> - b")),
                // The blank line between two items is deleted (loose to tight) or inserted.
                Triple(listOf("- a", "", "- b"), 1, emptyList()),
                Triple(listOf("- a", "- b"), 1, listOf("", "- b")),
                // An empty item's blank line becomes content the item cannot take.
                Triple(listOf("-", "", "  foo"), 1, listOf("  bar")),
            )
        for ((oldLines, line, replacement) in edits) {
            assertIncrementalParseMatchesFullParse(oldLines, line, line + 1, replacement)
        }
    }
}
