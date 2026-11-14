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

class ParseBlockTest {

    private fun parseLines(
        markdown: String,
        context: ParseContext = ParseContext(),
    ): DocumentParseBlock {
        val doc = DocumentParseBlock()
        if (markdown.isEmpty()) return doc
        val lines = markdown.split("\n")
        for (line in lines) {
            context.resetLine(LineScanner(line))
            doc.processLine(context)
        }
        return doc
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

        override fun canAcceptChild(child: ParseBlock): Boolean = true
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

        override fun canAcceptChild(child: ParseBlock): Boolean = true
    }

    @Test
    fun testPrefixedContainerReportsNoTrailingEmptyLines() {
        // ">" lines are blank inside the container but not for its parent, so the container
        // reports no trailing blank lines even though it ends with an EmptyLineParseBlock.
        val context = ParseContext()
        val quote = PrefixedContainer(id = context.generateBlockId())
        val doc = context.documentStartingWith(quote)
        doc.feedLines(context, ">", ">")
        assertEquals(listOf<ParseBlock>(quote), doc.children)
        assertEquals(2, assertIs<EmptyLineParseBlock>(quote.children[1]).countTrailingEmptyLines())
        assertEquals(0, quote.countTrailingEmptyLines())
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
}
