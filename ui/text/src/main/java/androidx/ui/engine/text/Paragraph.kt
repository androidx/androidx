/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.engine.text

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import androidx.ui.services.text_editing.TextRange

/**
 * A paragraph of text.
 *
 * A paragraph retains the size and position of each glyph in the text and can
 * be efficiently resized and painted.
 *
 * To create a [Paragraph] object, use a [ParagraphBuilder].
 *
 * Paragraphs can be displayed on a [Canvas] using the [Canvas.drawParagraph]
 * method.
 *
 * This class is created by the engine, and should not be instantiated
 * or extended directly.
 *
 * To create a [Paragraph] object, use a [ParagraphBuilder].
 */
//
class Paragraph internal constructor(
    val text: StringBuilder,
    val paragraphStyle: ParagraphStyle,
    internal val textStyles: List<ParagraphBuilder.TextStyleIndex>
) {
    private var needsLayout = true
    /** increased visibility for testing **/
    internal val paragraphImpl: ParagraphAndroid

    /**
     * The amount of horizontal space this paragraph occupies.
     *
     * Valid only after [layout] has been called.
     */
    val width: Float
        get() = paragraphImpl.width

    /**
     * The amount of vertical space this paragraph occupies.
     *
     * Valid only after [layout] has been called.
     */
    val height: Float
        get() = paragraphImpl.height

    /**
     * The minimum width that this paragraph could be without failing to paint
     * its contents within itself.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth
        get() = paragraphImpl.minIntrinsicWidth

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: Float
        get() = paragraphImpl.maxIntrinsicWidth

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the first line, in logical pixels.
     */
    val baseline: Float
        get() = paragraphImpl.baseline

    /**
     * True if there is more vertical content, but the text was truncated, either
     * because we reached `maxLines` lines of text or because the `maxLines` was
     * null, `ellipsis` was not null, and one of the lines exceeded the width
     * constraint.
     *
     * See the discussion of the `maxLines` and `ellipsis` arguments at [ParagraphStyle].
     */
    val didExceedMaxLines: Boolean
        get() = paragraphImpl.didExceedMaxLines

    init {
        if (paragraphStyle.lineHeight != null && paragraphStyle.lineHeight < 0.0f) {
            throw IllegalArgumentException("lineHeight can't be negative")
        }
        paragraphImpl = ParagraphAndroid(text, paragraphStyle, textStyles)
    }

    // void Paragraph::SetFontCollection(
    // std::shared_ptr<FontCollection> font_collection) {
    //    font_collection_ = std::move(font_collection);
    // }

    // void Paragraph::SetParagraphStyle(const ParagraphStyle& style) {
    //    needs_layout_ = true;
    //    paragraph_style_ = style;
    // }

    /**
     * Computes the size and position of each glyph in the paragraph.
     *
     * The [ParagraphConstraints] control how wide the text is allowed to be.
     */
    fun layout(constraints: ParagraphConstraints) {
        _layout(constraints.width)
    }

    private fun _layout(width: Float, force: Boolean = false) {
        // TODO(migration/siyamed) the comparison should be floor(width) since it is
        // floored in paragraphImpl, or the comparison should be moved to there.
        if (!needsLayout && this.width == width && !force) return
        needsLayout = false
        paragraphImpl.layout(width)
    }

    /** Returns path that enclose the given text range. */
    fun getPathForRange(start: Int, end: Int): Path {
        assert(start <= end && start >= 0 && end <= text.length) {
            "Start($start) or End($end) is out of Range(0..${text.length}), or start > end!"
        }
        return paragraphImpl.getPathForRange(start, end)
    }

    /** Returns the left x Coordinate of the given line. */
    fun getLineLeft(lineIndex: Int): Float = paragraphImpl.getLineLeft(lineIndex)

    /** Returns the right x Coordinate of the given line. */
    fun getLineRight(lineIndex: Int): Float = paragraphImpl.getLineRight(lineIndex)

    /** Returns the height of the given line. */
    fun getLineHeight(lineIndex: Int): Float = paragraphImpl.getLineHeight(lineIndex)

    /** Returns the width of the given line. */
    fun getLineWidth(lineIndex: Int): Float = paragraphImpl.getLineWidth(lineIndex)

    /** Returns the text position closest to the given offset. */
    fun getPositionForOffset(offset: Offset): TextPosition {
        return paragraphImpl.getPositionForOffset(offset)
    }

    /**
     * Returns the coordinates of top and bottom end of the Caret for given TextPosition. The Caret
     * is on the left side of the character at the given TextPosition for LTR text, and on the right
     * side of the character at the given TextPosition for RTL text.
     */
    fun getCaretForTextPosition(textPosition: TextPosition): Pair<Offset, Offset> {
        return paragraphImpl.getCaretForTextPosition(textPosition)
    }

    /**
     * Returns the TextRange of the word at the given offset. Characters not
     * part of a word, such as spaces, symbols, and punctuation, have word breaks
     * on both sides. In such cases, this method will return TextRange(offset, offset+1).
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): TextRange {
        val (start, end) = paragraphImpl.getWordBoundary(offset)
        return TextRange(start, end)
    }

    // Redirecting the paint function in this way solves some dependency problems
    // in the C++ code. If we straighten out the C++ dependencies, we can remove
    // this indirection.
    fun paint(canvas: Canvas, x: Float, y: Float) {
        paragraphImpl.paint(canvas, x, y)
    }
}
