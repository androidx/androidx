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
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
        assertEquals(0, doc.verifiedLineCount)
    }

    @Test
    fun testEmptyLinesGroupedIntoEmptyLineParseBlock() {
        val doc = parseLines("\n\n\n")
        assertEquals(1, doc.children.size)
        val empty = assertIs<EmptyLineParseBlock>(doc.children[0])
        assertEquals(4, empty.lineCount)
        assertEquals(4, doc.lineCount)
        assertEquals(4, doc.verifiedLineCount)
    }

    @Test
    fun testSingleLineParagraph() {
        val doc = parseLines("Hello world")
        assertEquals(1, doc.children.size)
        assertEquals(1, doc.lineCount)
        assertEquals(1, doc.verifiedLineCount)

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
        assertEquals(3, doc.verifiedLineCount)

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
        assertEquals(6, doc.verifiedLineCount)

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
        assertEquals(4, doc.verifiedLineCount)

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
        val doc = DocumentParseBlock()
        val e1 = EmptyLineParseBlock(id = 1L, lineCount = 2)
        val e2 = EmptyLineParseBlock(id = 2L, lineCount = 3)
        val e3 = EmptyLineParseBlock(id = 3L, lineCount = 2)
        doc.addChild(e1)
        doc.addChild(e2)
        doc.addChild(e3)
        doc.verifiedLineCount = 7

        // Invalidate lines 2..5 (affecting e2)
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 5, newLineCount = 2)
        assertEquals(2, doc.verifiedLineCount)
        assertEquals(0, doc.activeChildIndex)
        assertEquals(e3, doc.unverifiedChild)
        assertEquals(2, doc.unverifiedChildLineDelta)
    }

    @Test
    fun testInvalidateLinesSpanningFromChildContainerAcrossSibling() {
        val doc = DocumentParseBlock()
        val childContainer = DocumentParseBlock(id = 1L)
        childContainer.addChild(ParagraphParseBlock(id = 2L, initialContent = "C1", lineCount = 2))
        childContainer.addChild(ParagraphParseBlock(id = 3L, initialContent = "C2", lineCount = 2))
        childContainer.verifiedLineCount = 4

        val sibling1 = ParagraphParseBlock(id = 4L, initialContent = "S1", lineCount = 3)
        val sibling2 = ParagraphParseBlock(id = 5L, initialContent = "S2", lineCount = 2)
        doc.addChild(childContainer)
        doc.addChild(sibling1)
        doc.addChild(sibling2)
        doc.verifiedLineCount = 9

        // Edit spans from line 2 (inside childContainer's second child) across sibling1 (lines
        // 4..7)
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 7, newLineCount = 1)

        // childContainer keeps its first child (2 lines) and has no trailing unverified children
        assertEquals(1, childContainer.children.size)
        assertEquals(2, childContainer.verifiedLineCount)
        assertEquals(-1, childContainer.unverifiedChildLineDelta)
        assertEquals(-1, childContainer.expectedConvergedLineCount)

        // doc retains childContainer as activeChild and sibling2 as unverifiedChild
        assertEquals(0, doc.activeChildIndex)
        assertEquals(2, doc.verifiedLineCount)
        assertSame(sibling2, doc.unverifiedChild)
        assertEquals(1, doc.unverifiedChildLineDelta)
        assertEquals(5, doc.expectedConvergedLineCount)
    }

    @Test
    fun testDescendantConvergenceInLastChildContainer() {
        val doc = DocumentParseBlock()
        val childContainer = DocumentParseBlock(id = 1L)
        childContainer.addChild(ParagraphParseBlock(id = 2L, initialContent = "C1", lineCount = 1))
        childContainer.addChild(EmptyLineParseBlock(id = 3L, lineCount = 1))
        childContainer.addChild(ParagraphParseBlock(id = 4L, initialContent = "C2", lineCount = 1))
        childContainer.addChild(EmptyLineParseBlock(id = 5L, lineCount = 1))
        childContainer.addChild(ParagraphParseBlock(id = 6L, initialContent = "C3", lineCount = 1))
        childContainer.verifiedLineCount = 5

        doc.addChild(childContainer)
        doc.verifiedLineCount = 5

        // Edit line 2 ("C2") inside childContainer (which is the last/only child of doc)
        doc.invalidateLines(oldLineStart = 2, oldLineEndExclusive = 3, newLineCount = 1)
        assertEquals(-1, doc.unverifiedChildLineDelta)
        assertEquals(5, doc.expectedConvergedLineCount)
        assertEquals(1, childContainer.unverifiedChildLineDelta)
        assertEquals(5, childContainer.expectedConvergedLineCount)

        val context = ParseContext()
        context.resetLine(LineScanner("C2 modified"))
        val result0 = doc.processLine(context)
        assertTrue(result0.accepted)
        assertFalse(result0.isConverged)

        // Line 3 ("") converges inside childContainer and bubbles up to doc without throwing
        context.resetLine(LineScanner(""))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertTrue(result1.isConverged)
        assertEquals(5, childContainer.verifiedLineCount)
        assertEquals(5, doc.verifiedLineCount)
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
        assertEquals(1, doc.verifiedLineCount)
        assertEquals(1, doc.unverifiedChildLineDelta)
        assertEquals(5, doc.expectedConvergedLineCount)

        // Line 1 ("bridging text"): absorbed by Paragraph 1; delta reaches 0 for Paragraph 2
        context.resetLine(LineScanner("bridging text"))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertFalse(result1.isConverged)
        assertEquals(2, doc.verifiedLineCount)
        assertEquals(0, doc.unverifiedChildLineDelta)

        // Line 2 ("Paragraph 2"): reaches Paragraph 2's convergence point, but Paragraph 1 absorbs
        // it (mismatch!). Paragraph 2 is discarded and delta shifts to e2.
        context.resetLine(LineScanner("Paragraph 2"))
        val result2 = doc.processLine(context)
        assertTrue(result2.accepted)
        assertFalse(result2.isConverged)
        assertEquals(3, doc.verifiedLineCount)
        assertEquals(0, doc.unverifiedChildLineDelta)
        assertEquals(5, doc.expectedConvergedLineCount)

        // Line 3 (""): opens a new EmptyLineParseBlock at delta == 0 and converges with e2!
        context.resetLine(LineScanner(""))
        val result3 = doc.processLine(context)
        assertTrue(result3.accepted)
        assertTrue(result3.isConverged)
        assertEquals(5, doc.verifiedLineCount)
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
        val doc = DocumentParseBlock()
        val e1 = EmptyLineParseBlock(id = 1L, lineCount = 1)
        val e2 = EmptyLineParseBlock(id = 2L, lineCount = 1)
        doc.addChild(e1)
        doc.addChild(e2)
        doc.verifiedLineCount = 2

        // Delete line 0 (e1): e2 shifts from line 1 to line 0 (delta = 0)
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 0)
        assertEquals(0, doc.verifiedLineCount)
        assertEquals(e2, doc.unverifiedChild)
        assertEquals(0, doc.unverifiedChildLineDelta)

        // Parsing line 0 opens a new EmptyLineParseBlock at delta == 0, converging with e2
        val context = ParseContext()
        context.resetLine(LineScanner(""))
        val result = doc.processLine(context)
        assertTrue(result.accepted)
        assertTrue(result.isConverged)
        assertEquals(1, doc.lineCount)
        assertEquals(0, doc.activeChildIndex)
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
        assertEquals(2, doc.verifiedLineCount)
        assertEquals(1, doc.activeChildIndex) // activeChild is EmptyLine at index 1

        // Parse modified line 2 using the same ParseContext
        context.resetLine(LineScanner("Paragraph 2 modified"))
        val result1 = doc.processLine(context)
        assertTrue(result1.accepted)
        assertFalse(result1.isConverged)
        assertEquals(3, doc.verifiedLineCount)
        assertEquals(2, doc.activeChildIndex)

        // Parse line 3 (""): aligns with e2 (EmptyLineParseBlock) and converges!
        context.resetLine(LineScanner(""))
        val result2 = doc.processLine(context)
        assertTrue(result2.accepted)
        assertTrue(result2.isConverged)
        assertEquals(5, doc.verifiedLineCount)
        assertEquals(5, doc.lineCount)
        assertEquals(5, doc.children.size)

        val p2 = assertIs<ParagraphParseBlock>(doc.children[2])
        assertEquals("Paragraph 2 modified", p2.content.toString())
        assertEquals(5L, p2.id)
        assertSame(p3, doc.children[4])
    }

    @Test
    fun testIncrementalParseActiveChildAbsorbsLineDiscardsStaleUnverified() {
        val doc = DocumentParseBlock()
        val p1 = ParagraphParseBlock(id = 1L, initialContent = "P1", lineCount = 1)
        val p2 = ParagraphParseBlock(id = 2L, initialContent = "P2", lineCount = 3)
        val p3 = ParagraphParseBlock(id = 3L, initialContent = "P3", lineCount = 2)
        doc.addChild(p1)
        doc.addChild(p2)
        doc.addChild(p3)
        doc.activeChildIndex = 0 // activeChild is p1, unverifiedChild is p2
        doc.verifiedLineCount = 1
        doc.unverifiedChildLineDelta = 0 // delta hits 0
        doc.expectedConvergedLineCount = 6

        // Parse a continuation line "P1 continuation" -> p1 absorbs it (childToAdd is null)
        val context = ParseContext()
        context.resetLine(LineScanner("P1 continuation"))

        val result = doc.processLine(context)
        assertTrue(result.accepted)
        assertFalse(result.isConverged)

        // p1 grew to lineCount 2; p2 is removed; p3 is now at index 1 (unverifiedChild)
        assertEquals(0, doc.activeChildIndex)
        assertEquals(2, doc.children.size)
        assertSame(p1, doc.children[0])
        assertSame(p3, doc.children[1])
        assertEquals("P1\nP1 continuation", p1.content.toString())
        assertEquals(2, p1.lineCount)
        // verifiedLineCount incremented for continuation line (1 + 1 = 2)
        assertEquals(2, doc.verifiedLineCount)
        // unverifiedChildLineDelta updated with p2.lineCount (3) and decremented for continuation
        // line (3 - 1 = 2)
        assertEquals(2, doc.unverifiedChildLineDelta)
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
        assertEquals(0, doc.verifiedLineCount)

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

        assertEquals(6, doc.verifiedLineCount)
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
        assertEquals(5, doc.verifiedLineCount)

        // Edit 1: Edit line 0 from "Paragraph 1" to "Paragraph 1 edited"
        doc.invalidateLines(oldLineStart = 0, oldLineEndExclusive = 1, newLineCount = 1)
        context.resetLine(LineScanner("Paragraph 1 edited"))
        doc.processLine(context)
        context.resetLine(LineScanner("")) // line 1 converges with EmptyLineParseBlock
        val convResult1 = doc.processLine(context)
        assertTrue(convResult1.isConverged)
        assertEquals(5, doc.verifiedLineCount)
        assertEquals(5, doc.lineCount)

        val p1 = assertIs<ParagraphParseBlock>(doc.children[0])
        assertEquals("Paragraph 1 edited", p1.content.toString())

        // Edit 2: Edit line 4 from "Paragraph 3" to "Paragraph 3 edited"
        doc.invalidateLines(oldLineStart = 4, oldLineEndExclusive = 5, newLineCount = 1)
        assertEquals(4, doc.verifiedLineCount)
        context.resetLine(LineScanner("Paragraph 3 edited"))
        val convResult2 = doc.processLine(context)
        assertTrue(convResult2.accepted)
        assertEquals(5, doc.verifiedLineCount)
        assertEquals(5, doc.lineCount)

        val p3 = assertIs<ParagraphParseBlock>(doc.children[4])
        assertEquals("Paragraph 3 edited", p3.content.toString())
    }
}
