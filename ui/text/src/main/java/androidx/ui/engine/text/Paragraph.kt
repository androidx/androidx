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

import androidx.ui.core.Density
import androidx.ui.core.TextRange
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import androidx.ui.painting.TextStyle

/**
 * A paragraph of text.
 *
 * A paragraph retains the size and position of each glyph in the text and can
 * be efficiently resized and painted.
 *
 * Paragraphs can be displayed on a [Canvas] using the [paint] method.
 */
interface Paragraph {
    /**
     * The amount of horizontal space this paragraph occupies.
     *
     * Valid only after [layout] has been called.
     */
    val width: Float

    /**
     * The amount of vertical space this paragraph occupies.
     *
     * Valid only after [layout] has been called.
     */
    val height: Float

    /**
     * The minimum width that this paragraph could be without failing to paint
     * its contents within itself.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth: Float

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: Float

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the first line, in logical pixels.
     */
    val baseline: Float

    /**
     * True if there is more vertical content, but the text was truncated, either
     * because we reached `maxLines` lines of text or because the `maxLines` was
     * null, `ellipsis` was not null, and one of the lines exceeded the width
     * constraint.
     *
     * See the discussion of the `maxLines` and `ellipsis` arguments at [ParagraphStyle].
     */
    val didExceedMaxLines: Boolean

    /**
     * The total number of lines in the text.
     */
    val lineCount: Int

    /**
     * Computes the size and position of each glyph in the paragraph.
     *
     * The [ParagraphConstraints] control how wide the text is allowed to be.
     */
    fun layout(constraints: ParagraphConstraints)

    /** Returns path that enclose the given text range. */
    fun getPathForRange(start: Int, end: Int): Path

    /** Returns rectangle of the cursor area. */
    fun getCursorRect(offset: Int): Rect

    /** Returns the left x Coordinate of the given line. */
    fun getLineLeft(lineIndex: Int): Float

    /** Returns the right x Coordinate of the given line. */
    fun getLineRight(lineIndex: Int): Float

    /** Returns the height of the given line. */
    fun getLineHeight(lineIndex: Int): Float

    /** Returns the width of the given line. */
    fun getLineWidth(lineIndex: Int): Float

    /** Returns the text position closest to the given offset. */
    fun getPositionForOffset(offset: Offset): Int

    /**
     * Returns the bounding box as Rect of the character for given offset. Rect includes the
     * top, bottom, left and right of a character.
     */
    fun getBoundingBox(offset: Int): Rect

    /**
     * Returns the TextRange of the word at the given offset. Characters not
     * part of a word, such as spaces, symbols, and punctuation, have word breaks
     * on both sides. In such cases, this method will return TextRange(offset, offset+1).
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): TextRange

    /**
     * Paint the paragraph to canvas
     */
    fun paint(canvas: Canvas, x: Float, y: Float)
}

fun Paragraph(
    text: String,
    style: TextStyle,
    paragraphStyle: ParagraphStyle,
    textStyles: List<AnnotatedString.Item<TextStyle>>,
    density: Density
): Paragraph = ParagraphAndroid(
    text = text,
    style = style,
    paragraphStyle = paragraphStyle,
    textStyles = textStyles,
    density = density
)