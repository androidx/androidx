/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.text.vertical

import android.graphics.Canvas
import android.text.TextPaint
import androidx.annotation.Px
import androidx.text.vertical.LineBreaker.Result
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// Illustrate meaning of baseline, left, right and drawing offset.
// (Vertical LR case, line grows from right to left.)
//
//         2nd line            1st line
//
//           width               width
//    <---------------->   <---------------->
//    leftSide  rightSide   leftSide rightSide
//    <-------- ------->   <-------- ------->
//       2nd baseline        1st baseline
//   |         |         |         |         +Drawing offset (top-right corner)
//   |  +------+------+  |  +------+------+  |
//   |  |      |      |  |  |      |      |  |
//   |  |  n-th char  |  |  |   1st char  |  |
//   |  |      |      |  |  |      |      |  |
//   |  |      |      |  |  |      |      |  |
//   |  +------+------+  |  +------+------+  |
//   |  |      |      |  |  |      |      |  |
//   |  | n+1-th char |  |  |   2nd char  |  |
//   |  |      |      |  |  |      |      |  |
//   |  |      |      |  |  |      |      |  |
//   |  +------+------+  |  +------+------+  |
//   |  |      |      |  |  |      |      |  |
//   |  |      |      |  |  |      |      |  |
//   |  |      |      |  |  |      |      |  |
//                          ^1st line left   ^1st line right
//   ^2nd line left   ^2nd line right
//
// The rightSide is a positive distance from the baseline to the line right.
//
// The leftSide is a negative distance from the baseline to the line left.
//
// The width is a positive distance from the line left to line right.
// The width is calculated with right - left.

internal object LineBreaker {
    /**
     * Represents lines of text that has been laid out.
     *
     * @property width The total width of the line.
     * @property lineLeftSide The distance from each line left to each line baseline.
     * @property lineRightSide The distance from each line baseline to each line right.
     */
    class Result(
        private val lineLayouts: List<LineLayout>,
        val width: Float,
        val lineLeftSide: Float,
        val lineRightSide: Float,
    ) {
        /** The number of lines in the text layout. */
        val lineCount: Int
            get() = lineLayouts.size

        /**
         * Retrieves the inclusive starting character index of a specified line.
         *
         * @param lineNo The line number.
         * @return The inclusive starting character index of the specified line.
         */
        fun getLineStart(lineNo: Int) = lineLayouts[lineNo].start

        /**
         * Retrieves the exclusive ending character index of a specified line.
         *
         * @param lineNo The line number.
         * @return The exclusive ending character index of the specified line.
         */
        fun getLineEnd(lineNo: Int) = lineLayouts[lineNo].end

        /**
         * Draws the text lines onto the given canvas.
         *
         * @param canvas The Canvas to draw on.
         * @param x The starting X position for drawing. The right-top is the drawing origin.
         * @param y The starting Y position for drawing. The right-top is the drawing origin.
         * @param paint The TextPaint object to use for text rendering.
         */
        fun draw(canvas: Canvas, x: Float, y: Float, paint: TextPaint) {
            val lineWidth = lineRightSide - lineLeftSide
            lineLayouts.forEachIndexed { i, line ->
                // Baseline offset of the i-th line.
                val baselineOffset = -lineWidth * i - lineRightSide
                line.draw(canvas, x + baselineOffset, y, paint)
            }
        }
    }

    /**
     * Performs a line break and layouts each lines.
     *
     * @param text The text to be processed.
     * @param start The inclusive starting index of the range.
     * @param end The exclusive ending index of the range.
     * @param paint The TextPaint used for measuring and drawing text.
     * @param heightConstraint The height constraint in pixels.
     * @param textOrientation The desired orientation for the text (MIXED, HORIZONTAL, VERTICAL).
     *   Defaults to MIXED.
     * @return A Result object containing the broken text lines.
     */
    fun breakTextIntoLines(
        text: CharSequence,
        start: Int,
        end: Int,
        paint: TextPaint,
        @Px heightConstraint: Float,
        textOrientation: Int = TextOrientation.MIXED,
    ): Result {
        val ctx = Context(text, paint, heightConstraint)
        text.forEachParagraph(start, end) { paraStart, paraEnd ->
            forEachRubySpanTransition(text, paraStart, paraEnd) { runStart, runEnd, rubySpan ->
                if (rubySpan == null) {
                    forEachOrientation(text, runStart, runEnd, textOrientation) { oStart, oEnd, o ->
                        if (o == ResolvedOrientation.TateChuYoko) {
                            ctx.processTateChuYoko(oStart, oEnd)
                        } else {
                            ctx.processRun(oStart, oEnd, o)
                        }
                    }
                } else {
                    ctx.processRubyRun(runStart, runEnd, rubySpan, textOrientation)
                }
            }

            // Add a line break after each paragraph except the last one.
            if (paraEnd != end) {
                ctx.breakLine()
            }
        }
        return ctx.finish()
    }
}

/**
 * A custom word breaker for the line breaking specialized for vertical text.
 *
 * @property text The text to break into words.
 * @property locale The locale for language-specific word breaking rules.
 */
private data class WordBreaker(val text: CharSequence, val locale: Locale) {
    private var currentRunStart = -1 // unused init value
    private var currentRunEnd = -1 // unused init value
    private var currentRunOrientation = ResolvedOrientation.Upright // unused init value
    private var currentHead = -1 // unused init value

    val current: Int
        get() = currentHead

    private val br =
        BreakIterator.getLineInstance(locale).apply {
            // TODO: Introduce CharacterIterator
            setText(this@WordBreaker.text.toString())
        }

    fun updateForRun(start: Int, end: Int, orientation: ResolvedOrientation) {
        currentRunStart = start
        currentRunEnd = end
        currentRunOrientation = orientation
        currentHead = currentRunStart
        advanceBreakOffset()
    }

    /**
     * Advances the word break offset within the current run.
     *
     * @return The new break offset.
     */
    fun advanceBreakOffset(): Int {
        currentHead =
            when (currentRunOrientation) {
                ResolvedOrientation.Rotate -> br.following(currentHead)
                ResolvedOrientation.Upright -> br.following(currentHead)
                else -> throw RuntimeException("TateChuYoko and Ruby should not be broken.")
            }
        if (currentHead == BreakIterator.DONE || currentHead > currentRunEnd) {
            currentHead = currentRunEnd
        }
        return currentHead
    }
}

/**
 * Manages the state and logic for breaking text into lines.
 *
 * @property text The input text.
 * @property paint The paint used for measuring text.
 * @property heightConstraint The height constraint.
 */
private data class Context(
    val text: CharSequence,
    val paint: TextPaint,
    @Px val heightConstraint: Float,
) {
    val breaker = WordBreaker(text, paint.textLocale)
    var currentLineHeight: Float = 0f
    val currentLineRuns: MutableList<LayoutRun> = mutableListOf()
    val brokenLines: MutableList<LineLayout> = mutableListOf()

    /**
     * Breaks the current line and adds it to the list of broken lines.
     *
     * This method also resets the current line context.
     */
    fun breakLine() {
        require(currentLineRuns.isNotEmpty()) { "Cannot break with empty runs." }
        brokenLines.add(LineLayout(currentLineRuns.toList()))
        currentLineRuns.clear()
        currentLineHeight = 0f
    }

    private fun addRun(start: Int, end: Int, orientation: ResolvedOrientation) =
        currentLineRuns.add(createLayoutRun(text, start, end, paint, orientation))

    /**
     * Iterate through each words within a specified range of a CharSequence.
     *
     * @param start The inclusive starting index of the range.
     * @param end The exclusive ending index of the range.
     * @param advances An array advances.
     * @param consumer A callback function that is called for each words. It receives three
     *   parameters:
     *     - The inclusive start index of the word.
     *     - The exclusive end index of the word.
     *     - The height of the word.
     */
    private inline fun forEachWord(
        start: Int,
        end: Int,
        orientation: ResolvedOrientation,
        advances: FloatArray,
        crossinline consumer: (Int, Int, Float) -> Unit,
    ) {
        breaker.updateForRun(start, end, orientation)

        var wordStart = start
        var wordHeight = 0f
        for (i in start until end) {
            wordHeight += advances[i - start]
            if (i + 1 == breaker.current) {
                consumer(wordStart, i + 1, wordHeight)

                wordStart = i + 1
                wordHeight = 0f
                breaker.advanceBreakOffset()
            }
        }
    }

    private inline fun forEachGrapheme(
        wStart: Int,
        wEnd: Int,
        advances: FloatArray,
        advancesOffset: Int,
        crossinline consumer: (Int, Int, Float) -> Unit,
    ) {
        var gStart = wStart
        for (i in wStart + 1 until wEnd) {
            if (advances[i - advancesOffset] == 0.0f) {
                // If multiple characters are assigned to the single grapheme, the advance is
                // assigned to the first character and remaining are zeros. Therefore, call the
                // consumer with the first character offset and last character offset that has zero
                // advance.
                continue
            }

            consumer(gStart, i, advances[gStart - advancesOffset])
            gStart = i
        }
        consumer(gStart, wEnd, advances[gStart - advancesOffset])
    }

    /**
     * Processes breakable continued run.
     *
     * For TateChuYoko, use processTateChuYokoRun, for Ruby, use processRubyRun
     *
     * @param start The inclusive starting index of the run.
     * @param end The exclusive ending index of the run.
     * @param orientation The text orientation of the run (e.g., Horizontal, Vertical, TateChuYoko).
     */
    fun processRun(start: Int, end: Int, orientation: ResolvedOrientation) {
        require(
            orientation == ResolvedOrientation.Upright || orientation == ResolvedOrientation.Rotate
        )
        breaker.updateForRun(start, end, orientation)

        val advances = FloatArray(end - start)
        val layout = createLayoutRun(text, start, end, paint, orientation)
        layout.getCharAdvances(advances, paint)

        var lineStartOffset = start
        var lastKnownGoodBreakOffset = -1
        forEachWord(start, end, orientation, advances) { wStart, wEnd, wHeight ->
            if (currentLineHeight + wHeight <= heightConstraint) {
                // We still have space. Just update the line height.
                currentLineHeight += wHeight
                lastKnownGoodBreakOffset = wEnd
                return@forEachWord
            }

            // Okay, the current word cannot fit the line. If we know the previous offset, break
            // with it.
            if (lastKnownGoodBreakOffset != -1) {
                // If there is a good break offset before, break with it.
                addRun(lineStartOffset, lastKnownGoodBreakOffset, orientation)
                breakLine()

                lineStartOffset = wStart
                lastKnownGoodBreakOffset = -1
            } else if (currentLineRuns.isNotEmpty()) {
                // If this is the first break offset, and if some runs are already in the current
                // line, break here (the beginning of the run) first.
                breakLine()

                lineStartOffset = wStart
                lastKnownGoodBreakOffset = -1
            }

            // As the result of line break, if the current word can fit in the line,
            // keep continue to the next word.
            if (wHeight <= heightConstraint) {
                currentLineHeight = wHeight
                lastKnownGoodBreakOffset = wEnd
                return@forEachWord
            }

            // Oh no, we don't have any break offset before this offset.
            // Try to break text with desperate break.

            // First, give up for TateChuYoko orientations cannot be broken further.
            if (orientation == ResolvedOrientation.TateChuYoko) {
                addRun(wStart, wEnd, orientation)
                breakLine()
                lineStartOffset = wEnd
                return@forEachWord
            }

            forEachGrapheme(lineStartOffset, wEnd, advances, start) { gStart, gEnd, gHeight ->
                if (gStart == lineStartOffset) {
                    // Ensure there's at least one grapheme per line during breaking.
                    currentLineHeight = gHeight
                    lastKnownGoodBreakOffset = gEnd
                    return@forEachGrapheme
                } else if (currentLineHeight + gHeight <= heightConstraint) {
                    // We still have space. Extend the current line and continues
                    currentLineHeight += gHeight
                    lastKnownGoodBreakOffset = gEnd
                    return@forEachGrapheme
                }

                // We don't have space, so break line with the previously known good grapheme break
                // offset.
                addRun(lineStartOffset, lastKnownGoodBreakOffset, orientation)
                breakLine()

                // Updating the current line but ensure there's at least one grapheme per line.
                currentLineHeight = gHeight
                lineStartOffset = lastKnownGoodBreakOffset
                lastKnownGoodBreakOffset = gEnd
            }
        }

        // Add the remaining part of the run to the current line.
        addRun(lineStartOffset, end, orientation)
    }

    /**
     * Processes a continued text run within a layout for RubySpan.
     *
     * @param start The inclusive starting index of the run.
     * @param end The exclusive ending index of the run.
     * @param ruby The ruby span.
     * @param orientation The text orientation mode.
     */
    fun processRubyRun(start: Int, end: Int, ruby: RubySpan, @OrientationMode orientation: Int) {
        val rubyLayout = RubyLayoutRun(text, start, end, orientation, paint, ruby)
        processNonBreakableLayout(rubyLayout)
    }

    fun processTateChuYoko(start: Int, end: Int) {
        val layout = createLayoutRun(text, start, end, paint, ResolvedOrientation.TateChuYoko)
        processNonBreakableLayout(layout)
    }

    private fun processNonBreakableLayout(layout: LayoutRun) {
        val height = layout.height
        if (currentLineHeight + height <= heightConstraint) {
            currentLineRuns.add(layout)
            currentLineHeight += height
            return
        }

        if (currentLineRuns.isNotEmpty()) {
            // The line goes over the height limit by appending this Layout.
            // Break it before adding Ruby.
            breakLine()
        }

        if (height <= heightConstraint) {
            currentLineRuns.add(layout)
            currentLineHeight = height
            return
        }

        currentLineRuns.add(layout)
        breakLine()
    }

    /**
     * Finishes the line breaking process and returns the result.
     *
     * @return The result of the line breaking process.
     */
    fun finish(): Result {
        breakLine()

        // To use the same line width, iterate over all lines and use the maximum of it.
        var leftSide = 0f
        var rightSide = 0f

        brokenLines.forEach {
            leftSide = min(leftSide, it.leftSide)
            rightSide = max(rightSide, it.rightSide)
        }

        return Result(brokenLines, (rightSide - leftSide) * brokenLines.size, leftSide, rightSide)
    }
}
