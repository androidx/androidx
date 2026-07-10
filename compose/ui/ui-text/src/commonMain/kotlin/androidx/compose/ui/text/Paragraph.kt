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

@file:JvmMultifileClass
@file:JvmName("ParagraphKt")

package androidx.compose.ui.text

import androidx.annotation.IntRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

internal const val DefaultMaxLines = Int.MAX_VALUE

/**
 * Renders a single laid out paragraph of text.
 *
 * Draw the paragraph onto a [Canvas] using [paint].
 */
@JvmDefaultWithCompatibility
expect sealed interface Paragraph {
    /** The amount of horizontal space this paragraph occupies. */
    val width: Float

    /** The amount of vertical space this paragraph occupies. */
    val height: Float

    /** The width for text if all soft wrap opportunities were taken. */
    val minIntrinsicWidth: Float

    /** Returns the smallest width beyond which increasing the width never decreases the height. */
    val maxIntrinsicWidth: Float

    /**
     * The distance from the top of the paragraph to the alphabetic baseline of the first line, in
     * logical pixels.
     */
    val firstBaseline: Float

    /**
     * The distance from the top of the paragraph to the alphabetic baseline of the last line, in
     * logical pixels.
     */
    val lastBaseline: Float

    /**
     * True if there is more vertical content, but the text was truncated, either because we reached
     * `maxLines` lines of text or because the `maxLines` was null, `ellipsis` was not null, and one
     * of the lines exceeded the width constraint.
     *
     * See the discussion of the `maxLines` and `ellipsis` arguments at [ParagraphStyle].
     */
    val didExceedMaxLines: Boolean

    /** The total number of lines in the text. */
    val lineCount: Int

    /**
     * The bounding boxes reserved for the input placeholders in this Paragraphs. Their locations
     * are relative to this Paragraph's coordinate. The order of this list corresponds to that of
     * input placeholders. Notice that [Rect] in [placeholderRects] is nullable. When [Rect] is
     * null, it indicates that the corresponding [Placeholder] is ellipsized.
     */
    val placeholderRects: List<Rect?>

    /** Returns path that enclose the given text range. */
    fun getPathForRange(start: Int, end: Int): Path

    /** Returns rectangle of the cursor area. */
    fun getCursorRect(offset: Int): Rect

    /** Returns the left x Coordinate of the given line. */
    fun getLineLeft(lineIndex: Int): Float

    /** Returns the right x Coordinate of the given line. */
    fun getLineRight(lineIndex: Int): Float

    /** Returns the bottom y coordinate of the given line. */
    fun getLineTop(lineIndex: Int): Float

    /**
     * Returns the distance from the top of the paragraph to the alphabetic baseline of the given
     * line.
     */
    fun getLineBaseline(lineIndex: Int): Float

    /** Returns the bottom y coordinate of the given line. */
    fun getLineBottom(lineIndex: Int): Float

    /** Returns the height of the given line. */
    fun getLineHeight(lineIndex: Int): Float

    /** Returns the width of the given line. */
    fun getLineWidth(lineIndex: Int): Float

    /** Returns the start offset of the given line, inclusive. */
    fun getLineStart(lineIndex: Int): Int

    /**
     * Returns the end offset of the given line
     *
     * Characters being ellipsized are treated as invisible characters. So that if visibleEnd is
     * false, it will return line end including the ellipsized characters and vice verse.
     *
     * @param lineIndex the line number
     * @param visibleEnd if true, the returned line end will not count trailing whitespaces or
     *   linefeed characters. Otherwise, this function will return the logical line end. By default
     *   it's false.
     * @return an exclusive end offset of the line.
     */
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int

    /**
     * Returns true if the given line is ellipsized, otherwise returns false.
     *
     * @param lineIndex a 0 based line index
     * @return true if the given line is ellipsized, otherwise false
     */
    fun isLineEllipsized(lineIndex: Int): Boolean

    /**
     * Returns the line number on which the specified text offset appears. If you ask for a position
     * before 0, you get 0; if you ask for a position beyond the end of the text, you get the last
     * line.
     */
    fun getLineForOffset(offset: Int): Int

    /**
     * Compute the horizontal position where a newly inserted character at [offset] would be.
     *
     * If the inserted character at [offset] is within a LTR/RTL run, the returned position will be
     * the left(right) edge of the character.
     *
     * ```
     * For example:
     *     Paragraph's direction is LTR.
     *     Text in logic order:               L0 L1 L2 R3 R4 R5
     *     Text in visual order:              L0 L1 L2 R5 R4 R3
     *         position of the offset(2):          |
     *         position of the offset(4):                   |
     * ```
     *
     * However, when the [offset] is at the BiDi transition offset, there will be two possible
     * visual positions, which depends on the direction of the inserted character.
     *
     * ```
     * For example:
     *     Paragraph's direction is LTR.
     *     Text in logic order:               L0 L1 L2 R3 R4 R5
     *     Text in visual order:              L0 L1 L2 R5 R4 R3
     *         position of the offset(3):             |           (The inserted character is LTR)
     *                                                         |  (The inserted character is RTL)
     * ```
     *
     * In this case, [usePrimaryDirection] will be used to resolve the ambiguity. If true, the
     * inserted character's direction is assumed to be the same as Paragraph's direction. Otherwise,
     * the inserted character's direction is assumed to be the opposite of the Paragraph's
     * direction.
     *
     * ```
     * For example:
     *     Paragraph's direction is LTR.
     *     Text in logic order:               L0 L1 L2 R3 R4 R5
     *     Text in visual order:              L0 L1 L2 R5 R4 R3
     *         position of the offset(3):             |           (usePrimaryDirection is true)
     *                                                         |  (usePrimaryDirection is false)
     * ```
     *
     * This method is useful to compute cursor position.
     *
     * @param offset the offset of the character, in the range of [0, length].
     * @param usePrimaryDirection whether the paragraph direction is respected when [offset] points
     *   to a BiDi transition point.
     * @return a float number representing the horizontal position in the unit of pixel.
     */
    fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float

    /** Get the text direction of the paragraph containing the given offset. */
    fun getParagraphDirection(offset: Int): ResolvedTextDirection

    /** Get the text direction of the character at the given offset. */
    fun getBidiRunDirection(offset: Int): ResolvedTextDirection

    /**
     * Returns line number closest to the given graphical vertical position. If you ask for a
     * vertical position before 0, you get 0; if you ask for a vertical position beyond the last
     * line, you get the last line.
     */
    fun getLineForVerticalPosition(vertical: Float): Int

    /** Returns the character offset closest to the given graphical position. */
    fun getOffsetForPosition(position: Offset): Int

    /**
     * Find the range of text which is inside the specified [rect]. This method will break text into
     * small text segments based on the given [granularity] such as character or word. It also
     * support different [inclusionStrategy], which determines when a small text segments is
     * considered as inside the [rect]. Note that the word/character breaking is both operating
     * system and language dependent. In the certain cases, the text may be break into smaller
     * segments than the specified the [granularity]. If a text segment spans multiple lines or
     * multiple directional runs (e.g. a hyphenated word), the text segment is divided into pieces
     * at the line and run breaks, then the text segment is considered to be inside the area if any
     * of its pieces are inside the area.
     *
     * @param rect the rectangle area in which the text range will be found.
     * @param granularity the granularity of the text, it controls how text is segmented.
     * @param inclusionStrategy the strategy that determines whether a range of text's bounds is
     *   inside the given [rect] or not.
     * @return the [TextRange] that is inside the given [rect], or [TextRange.Zero] if no text is
     *   found.
     */
    fun getRangeForRect(
        rect: Rect,
        granularity: TextGranularity,
        inclusionStrategy: TextInclusionStrategy,
    ): TextRange

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect includes
     * the top, bottom, left and right of a character.
     */
    fun getBoundingBox(offset: Int): Rect

    /**
     * Fills the bounding boxes for characters provided in the [range] into [array]. The array is
     * filled starting from [arrayStart] (inclusive). The coordinates are in local text layout
     * coordinates.
     *
     * The returned information consists of left/right of a character; line top and bottom for the
     * same character.
     *
     * For the grapheme consists of multiple code points, e.g. ligatures, combining marks, the first
     * character has the total width and the remaining are returned as zero-width.
     *
     * The array divided into segments of four where each index in that segment represents left,
     * top, right, bottom of the character.
     *
     * The size of the provided [array] should be greater or equal than the four times * [TextRange]
     * length.
     *
     * The final order of characters in the [array] is from [TextRange.min] to [TextRange.max].
     *
     * @param range the [TextRange] representing the start and end indices in the [Paragraph].
     * @param array the array to fill in the values. The array divided into segments of four where
     *   each index in that segment represents left, top, right, bottom of the character.
     * @param arrayStart the inclusive start index in the array where the function will start
     *   filling in the values from
     */
    fun fillBoundingBoxes(range: TextRange, array: FloatArray, @IntRange(from = 0) arrayStart: Int)

    /**
     * Returns the TextRange of the word at the given character offset. Characters not part of a
     * word, such as spaces, symbols, and punctuation, have word breaks on both sides. In such
     * cases, this method will return TextRange(offset, offset). Word boundaries are defined more
     * precisely in Unicode Standard Annex #29 http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): TextRange

    @Deprecated(
        "Use the new paint function that takes canvas as the only required parameter.",
        level = DeprecationLevel.HIDDEN,
    )
    fun paint(
        canvas: Canvas,
        color: Color = Color.Unspecified,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
    )

    /**
     * Draws this paragraph onto [canvas] with optional style overrides.
     *
     * Overriding parameters permanently changes the paragraph style, affecting subsequent [paint]
     * calls.
     *
     * @param canvas canvas to draw on
     * @param color overrides the default text color but does not override [SpanStyle.color] or
     *   [SpanStyle.brush] spans. [Color.Unspecified] keeps the current color
     * @param shadow overrides the default text shadow but does not override [SpanStyle.shadow]
     *   spans. [Shadow.None] removes the shadow, while null keeps the current shadow
     * @param textDecoration overrides the default text decoration but does not override
     *   [SpanStyle.textDecoration] spans. [TextDecoration.None] removes the decoration, while null
     *   keeps the current decoration
     * @param drawStyle overrides the default draw style but does not override [SpanStyle.drawStyle]
     *   spans. null keeps the current draw style
     * @param blendMode blend mode to apply during painting
     */
    fun paint(
        canvas: Canvas,
        color: Color = Color.Unspecified,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    )

    /**
     * Draws this paragraph onto [canvas] with optional style overrides.
     *
     * Overriding parameters permanently changes the paragraph style, affecting subsequent [paint]
     * calls.
     *
     * @param canvas canvas to draw on
     * @param brush overrides the default text brush but does not override [SpanStyle.color] or
     *   [SpanStyle.brush] spans. [alpha] sets the opacity of [SolidColor]. Creates the shader using
     *   layout size for [ShaderBrush]
     * @param alpha opacity for [brush] (0.0 to 1.0), or [Float.NaN] to keep the current alpha
     * @param shadow overrides the default text shadow but does not override [SpanStyle.shadow]
     *   spans. [Shadow.None] removes the shadow, while null keeps the current shadow
     * @param textDecoration overrides the default text decoration but does not override
     *   [SpanStyle.textDecoration] spans. [TextDecoration.None] removes the decoration, while null
     *   keeps the current decoration
     * @param drawStyle overrides the default draw style but does not override [SpanStyle.drawStyle]
     *   spans. null keeps the current draw style
     * @param blendMode blend mode to apply during painting
     */
    fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead pass FontFamily.Resolver",
    replaceWith =
        ReplaceWith(
            "Paragraph(text, style, Constraints(maxWidth = ceil(width).toInt()), density, " +
                "createFontFamilyResolver(resourceLoader), spanStyles, placeholders, maxLines, " +
                "if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip)",
            "kotlin.math.ceil",
            "androidx.compose.ui.unit.Constraints",
            "androidx.compose.ui.text.style.TextOverflow",
            "androidx.compose.ui.text.font.createFontFamilyResolver",
        ),
)
expect fun Paragraph(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
    width: Float,
    density: Density,
    resourceLoader: Font.ResourceLoader,
): Paragraph

@Deprecated(
    "Paragraph that takes maximum allowed width is deprecated, pass constraints instead.",
    ReplaceWith(
        "Paragraph(text, style, Constraints(maxWidth = ceil(width).toInt()), density, " +
            "fontFamilyResolver, spanStyles, placeholders, maxLines, " +
            "if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints",
        "androidx.compose.ui.text.style.TextOverflow",
    ),
)
expect fun Paragraph(
    text: String,
    style: TextStyle,
    width: Float,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
): Paragraph

@Deprecated(
    "Paragraph that takes `ellipsis: Boolean` is deprecated, pass TextOverflow instead.",
    level = DeprecationLevel.HIDDEN,
)
expect fun Paragraph(
    text: String,
    style: TextStyle,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
): Paragraph

/**
 * Lays out a given [text] with the given constraints. A paragraph is a text that has a single
 * [ParagraphStyle].
 *
 * If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @param text the text to be laid out
 * @param style the [TextStyle] to be applied to the whole text
 * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth] will
 *   define the width of the Paragraph. [Constraints.maxHeight] helps defining the number of lines
 *   that fit with ellipsis is true. Minimum components of the [Constraints] object are no-op.
 * @param density density of the device
 * @param fontFamilyResolver [FontFamily.Resolver] to be used to load the font given in [SpanStyle]s
 * @param spanStyles [SpanStyle]s to be applied to parts of text
 * @param placeholders a list of placeholder metrics which tells [Paragraph] where should be left
 *   blank to leave space for inline elements.
 * @param maxLines the maximum number of lines that the text can have
 * @param overflow specifies how visual overflow should be handled
 * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set
 */
expect fun Paragraph(
    text: String,
    style: TextStyle,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    overflow: TextOverflow = TextOverflow.Clip,
): Paragraph

@Deprecated(
    "Paragraph that takes maximum allowed width is deprecated, pass constraints instead.",
    ReplaceWith(
        "Paragraph(paragraphIntrinsics, Constraints(maxWidth = ceil(width).toInt()), maxLines, " +
            "if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints",
        "androidx.compose.ui.text.style.TextOverflow",
    ),
)
expect fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
    width: Float,
): Paragraph

@Deprecated(
    "Paragraph that takes ellipsis: Boolean is deprecated, pass TextOverflow instead.",
    level = DeprecationLevel.HIDDEN,
)
expect fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
): Paragraph

/**
 * Lays out the text in [ParagraphIntrinsics] with the given constraints. A paragraph is a text that
 * has a single [ParagraphStyle].
 *
 * @param paragraphIntrinsics [ParagraphIntrinsics] instance
 * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth] will
 *   define the width of the Paragraph. [Constraints.maxHeight] helps defining the number of lines
 *   that fit with ellipsis is true. Minimum components of the [Constraints] object are no-op.
 * @param maxLines the maximum number of lines that the text can have
 * @param overflow specifies how visual overflow should be handled
 */
expect fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int = DefaultMaxLines,
    overflow: TextOverflow = TextOverflow.Clip,
): Paragraph
