/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.text

import androidx.annotation.RestrictTo
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Path
import androidx.ui.text.font.Font
import androidx.ui.text.platform.AndroidParagraph
import androidx.ui.text.platform.AndroidParagraphIntrinsics
import androidx.ui.text.platform.TypefaceAdapter
import androidx.ui.text.style.TextDirection

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
     * Should be called after [layout] has been called.
     */
    val width: Float

    /**
     * The amount of vertical space this paragraph occupies.
     *
     * Should be called after [layout] has been called.
     */
    val height: Float

    /**
     * The width for text if all soft wrap opportunities were taken.
     */
    val minIntrinsicWidth: Float

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     */
    val maxIntrinsicWidth: Float

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the first line, in logical pixels.
     *
     * Should be called after [layout] has been called.
     */
    val firstBaseline: Float

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the last line, in logical pixels.
     *
     * Should be called after [layout] has been called.
     */
    val lastBaseline: Float

    /**
     * True if there is more vertical content, but the text was truncated, either
     * because we reached `maxLines` lines of text or because the `maxLines` was
     * null, `ellipsis` was not null, and one of the lines exceeded the width
     * constraint.
     *
     * See the discussion of the `maxLines` and `ellipsis` arguments at [ParagraphStyle].
     *
     * Should be called after [layout] has been called.
     */
    val didExceedMaxLines: Boolean

    /**
     * The total number of lines in the text.
     *
     * Should be called after [layout] has been called.
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

    /**
     * Returns the bottom y coordinate of the given line.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getLineBottom(lineIndex: Int): Float

    /** Returns the height of the given line. */
    fun getLineHeight(lineIndex: Int): Float

    /** Returns the width of the given line. */
    fun getLineWidth(lineIndex: Int): Float

    /**
     * Returns the line number on which the specified text offset appears.
     * If you ask for a position before 0, you get 0; if you ask for a position
     * beyond the end of the text, you get the last line.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getLineForOffset(offset: Int): Int

    /**
     * Get the primary horizontal position for the specified text offset.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getPrimaryHorizontal(offset: Int): Float

    /**
     * Get the secondary horizontal position for the specified text offset.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSecondaryHorizontal(offset: Int): Float

    /**
     * Get the text direction of the paragraph containing the given offset.
     */
    fun getParagraphDirection(offset: Int): TextDirection

    /**
     * Get the text direction of the character at the given offset.
     */
    fun getBidiRunDirection(offset: Int): TextDirection

    /** Returns the character offset closest to the given graphical position. */
    fun getOffsetForPosition(position: PxPosition): Int

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect
     * includes the top, bottom, left and right of a character.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getBoundingBox(offset: Int): Rect

    /**
     * Returns the TextRange of the word at the given character offset. Characters not
     * part of a word, such as spaces, symbols, and punctuation, have word breaks
     * on both sides. In such cases, this method will return TextRange(offset, offset+1).
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): TextRange

    /**
     * Paint the paragraph to canvas
     */
    fun paint(canvas: Canvas)
}

/**
 * @see Paragraph
 */
/* actual */ fun Paragraph(
    text: String,
    style: TextStyle,
    paragraphStyle: ParagraphStyle,
    textStyles: List<AnnotatedString.Item<TextStyle>>,
    maxLines: Int? = null,
    ellipsis: Boolean? = null,
    density: Density,
    layoutDirection: LayoutDirection,
    resourceLoader: Font.ResourceLoader
): Paragraph {
    return AndroidParagraph(
        text = text,
        style = style,
        paragraphStyle = paragraphStyle,
        textStyles = textStyles,
        maxLines = maxLines,
        ellipsis = ellipsis,
        typefaceAdapter = TypefaceAdapter(
            resourceLoader = resourceLoader
        ),
        density = density,
        layoutDirection = layoutDirection
    )
}

/* actual */ fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int? = null,
    ellipsis: Boolean? = null
): Paragraph {
    return AndroidParagraph(
        paragraphIntrinsics = paragraphIntrinsics as AndroidParagraphIntrinsics,
        maxLines = maxLines,
        ellipsis = ellipsis
    )
}