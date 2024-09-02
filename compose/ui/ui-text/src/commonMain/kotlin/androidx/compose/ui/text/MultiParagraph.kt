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

package androidx.compose.ui.text

import androidx.annotation.IntRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.drawMultiParagraph
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * Lays out and renders multiple paragraphs at once. Unlike [Paragraph], supports multiple
 * [ParagraphStyle]s in a given text.
 *
 * @param intrinsics previously calculated text intrinsics
 * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth] will
 *   define the width of the MultiParagraph. [Constraints.maxHeight] helps defining the number of
 *   lines that fit with ellipsis is true. Minimum components of the [Constraints] object are no-op.
 * @param maxLines the maximum number of lines that the text can have
 * @param overflow configures how visual overflow is handled. Ellipsis is applied only when
 *   [maxLines] is set
 */
class MultiParagraph(
    val intrinsics: MultiParagraphIntrinsics,
    constraints: Constraints,
    val maxLines: Int = DefaultMaxLines,
    overflow: TextOverflow = TextOverflow.Clip,
) {

    /**
     * Lays out and renders multiple paragraphs at once. Unlike [Paragraph], supports multiple
     * [ParagraphStyle]s in a given text.
     *
     * @param intrinsics previously calculated text intrinsics
     * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth] will
     *   define the width of the MultiParagraph. [Constraints.maxHeight] helps defining the number
     *   of lines that fit with ellipsis is true. Minimum components of the [Constraints] object are
     *   no-op.
     * @param maxLines the maximum number of lines that the text can have
     * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
     */
    @Deprecated("Constructor with `ellipsis: Boolean` is deprecated, pass TextOverflow instead ")
    constructor(
        intrinsics: MultiParagraphIntrinsics,
        constraints: Constraints,
        maxLines: Int = DefaultMaxLines,
        ellipsis: Boolean,
    ) : this(
        intrinsics = intrinsics,
        constraints = constraints,
        maxLines = maxLines,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip
    )

    /**
     * Lays out and renders multiple paragraphs at once. Unlike [Paragraph], supports multiple
     * [ParagraphStyle]s in a given text.
     *
     * @param intrinsics previously calculated text intrinsics
     * @param maxLines the maximum number of lines that the text can have
     * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
     * @param width how wide the text is allowed to be
     */
    @Deprecated(
        "MultiParagraph that takes maximum allowed width is deprecated, pass constraints instead.",
        ReplaceWith(
            "MultiParagraph(intrinsics, Constraints(maxWidth = ceil(width).toInt()), " +
                "maxLines, ellipsis)",
            "kotlin.math.ceil",
            "androidx.compose.ui.unit.Constraints"
        )
    )
    constructor(
        intrinsics: MultiParagraphIntrinsics,
        maxLines: Int = DefaultMaxLines,
        ellipsis: Boolean = false,
        width: Float
    ) : this(
        intrinsics,
        Constraints(maxWidth = width.ceilToInt()),
        maxLines,
        if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip
    )

    /**
     * Lays out a given [annotatedString] with the given constraints. Unlike a [Paragraph],
     * [MultiParagraph] can handle a text what has multiple paragraph styles.
     *
     * @param annotatedString the text to be laid out
     * @param style the [TextStyle] to be applied to the whole text
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     *   skipped during layout and replaced with [Placeholder]. It's required that the range of each
     *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     *   thrown.
     * @param maxLines the maximum number of lines that the text can have
     * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
     * @param width how wide the text is allowed to be
     * @param density density of the device
     * @param resourceLoader [Font.ResourceLoader] to be used to load the font given in [SpanStyle]s
     * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set, or any of the
     *   [placeholders] crosses paragraph boundary.
     * @see Placeholder
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        "Font.ResourceLoader is deprecated, use fontFamilyResolver instead",
        replaceWith =
            ReplaceWith(
                "MultiParagraph(annotatedString, style, " +
                    "placeholders, maxLines, ellipsis, width, density, fontFamilyResolver)"
            )
    )
    constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        width: Float,
        density: Density,
        resourceLoader: Font.ResourceLoader
    ) : this(
        intrinsics =
            MultiParagraphIntrinsics(
                annotatedString = annotatedString,
                style = style,
                placeholders = placeholders,
                density = density,
                fontFamilyResolver = createFontFamilyResolver(resourceLoader)
            ),
        maxLines = maxLines,
        ellipsis = ellipsis,
        constraints = Constraints(maxWidth = width.ceilToInt())
    )

    /**
     * Lays out a given [annotatedString] with the given constraints. Unlike a [Paragraph],
     * [MultiParagraph] can handle a text what has multiple paragraph styles.
     *
     * @param annotatedString the text to be laid out
     * @param style the [TextStyle] to be applied to the whole text
     * @param width how wide the text is allowed to be
     * @param density density of the device
     * @param fontFamilyResolver to be used to load the font given in [SpanStyle]s
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     *   skipped during layout and replaced with [Placeholder]. It's required that the range of each
     *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     *   thrown.
     * @param maxLines the maximum number of lines that the text can have
     * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
     * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set, or any of the
     *   [placeholders] crosses paragraph boundary.
     * @see Placeholder
     */
    @Deprecated(
        "MultiParagraph that takes maximum allowed width is deprecated, pass constraints instead.",
        ReplaceWith(
            "MultiParagraph(annotatedString, style, Constraints(maxWidth = ceil(width).toInt()), " +
                "density, fontFamilyResolver, placeholders, maxLines, ellipsis)",
            "kotlin.math.ceil",
            "androidx.compose.ui.unit.Constraints"
        )
    )
    constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        width: Float,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false
    ) : this(
        intrinsics =
            MultiParagraphIntrinsics(
                annotatedString = annotatedString,
                style = style,
                placeholders = placeholders,
                density = density,
                fontFamilyResolver = fontFamilyResolver
            ),
        maxLines = maxLines,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        constraints = Constraints(maxWidth = width.ceilToInt())
    )

    /**
     * Lays out a given [annotatedString] with the given constraints. Unlike a [Paragraph],
     * [MultiParagraph] can handle a text what has multiple paragraph styles.
     *
     * @param annotatedString the text to be laid out
     * @param style the [TextStyle] to be applied to the whole text
     * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth] will
     *   define the width of the MultiParagraph. [Constraints.maxHeight] helps defining the number
     *   of lines that fit with ellipsis is true. Minimum components of the [Constraints] object are
     *   no-op.
     * @param density density of the device
     * @param fontFamilyResolver to be used to load the font given in [SpanStyle]s
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     *   skipped during layout and replaced with [Placeholder]. It's required that the range of each
     *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     *   thrown.
     * @param maxLines the maximum number of lines that the text can have
     * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
     * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set, or any of the
     *   [placeholders] crosses paragraph boundary.
     * @see Placeholder
     */
    @Deprecated("Constructor with `ellipsis: Boolean` is deprecated, pass TextOverflow instead")
    constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        constraints: Constraints,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean
    ) : this(
        intrinsics =
            MultiParagraphIntrinsics(
                annotatedString = annotatedString,
                style = style,
                placeholders = placeholders,
                density = density,
                fontFamilyResolver = fontFamilyResolver
            ),
        maxLines = maxLines,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        constraints = constraints
    )

    /**
     * Lays out a given [annotatedString] with the given constraints. Unlike a [Paragraph],
     * [MultiParagraph] can handle a text what has multiple paragraph styles.
     *
     * @param annotatedString the text to be laid out
     * @param style the [TextStyle] to be applied to the whole text
     * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth] will
     *   define the width of the MultiParagraph. [Constraints.maxHeight] helps defining the number
     *   of lines that fit with ellipsis is true. Minimum components of the [Constraints] object are
     *   no-op.
     * @param density density of the device
     * @param fontFamilyResolver to be used to load the font given in [SpanStyle]s
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     *   skipped during layout and replaced with [Placeholder]. It's required that the range of each
     *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     *   thrown.
     * @param maxLines the maximum number of lines that the text can have
     * @param overflow configures how visual overflow is handled. Ellipsis is applied only when
     *   [maxLines] is set
     * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set, or any of the
     *   [placeholders] crosses paragraph boundary.
     * @see Placeholder
     */
    constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        constraints: Constraints,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip
    ) : this(
        intrinsics =
            MultiParagraphIntrinsics(
                annotatedString = annotatedString,
                style = style,
                placeholders = placeholders,
                density = density,
                fontFamilyResolver = fontFamilyResolver
            ),
        maxLines = maxLines,
        overflow = overflow,
        constraints = constraints
    )

    private val annotatedString
        get() = intrinsics.annotatedString

    /** The width for text if all soft wrap opportunities were taken. */
    val minIntrinsicWidth: Float
        get() = intrinsics.minIntrinsicWidth

    /** Returns the smallest width beyond which increasing the width never decreases the height. */
    val maxIntrinsicWidth: Float
        get() = intrinsics.maxIntrinsicWidth

    /**
     * True if there is more vertical content, but the text was truncated, either because we reached
     * `maxLines` lines of text or because the `maxLines` was null, `ellipsis` was not null, and one
     * of the lines exceeded the width constraint.
     */
    val didExceedMaxLines: Boolean

    /** The amount of horizontal space this paragraph occupies. */
    val width: Float

    /**
     * The amount of vertical space this paragraph occupies.
     *
     * Valid only after layout has been called.
     */
    val height: Float

    /**
     * The distance from the top of the paragraph to the alphabetic baseline of the first line, in
     * logical pixels.
     */
    val firstBaseline: Float
        get() {
            return if (paragraphInfoList.isEmpty()) {
                0f
            } else {
                paragraphInfoList[0].paragraph.firstBaseline
            }
        }

    /**
     * The distance from the top of the paragraph to the alphabetic baseline of the first line, in
     * logical pixels.
     */
    val lastBaseline: Float
        get() {
            return if (paragraphInfoList.isEmpty()) {
                0f
            } else {
                with(paragraphInfoList.last()) { paragraph.lastBaseline.toGlobalYPosition() }
            }
        }

    /** The total number of lines in the text. */
    val lineCount: Int

    /**
     * The bounding boxes reserved for the input placeholders in this MultiParagraph. Their
     * locations are relative to this MultiParagraph's coordinate. The order of this list
     * corresponds to that of input placeholders. Notice that [Rect] in [placeholderRects] is
     * nullable. When [Rect] is null, it indicates that the corresponding [Placeholder] is
     * ellipsized.
     */
    val placeholderRects: List<Rect?>

    /* This is internal for testing purpose. */
    internal val paragraphInfoList: List<ParagraphInfo>

    init {
        require(constraints.minWidth == 0 && constraints.minHeight == 0) {
            "Setting Constraints.minWidth and Constraints.minHeight is not supported, " +
                "these should be the default zero values instead."
        }

        var currentHeight = 0f
        var currentLineCount = 0
        var didExceedMaxLines = false

        // create sub paragraphs and layouts
        val paragraphInfoList = mutableListOf<ParagraphInfo>()
        val infoList = intrinsics.infoList
        for (index in infoList.indices) {
            val paragraphInfo = infoList[index]
            val paragraph =
                Paragraph(
                    paragraphInfo.intrinsics,
                    Constraints(
                        maxWidth = constraints.maxWidth,
                        maxHeight =
                            if (constraints.hasBoundedHeight) {
                                (constraints.maxHeight - currentHeight.ceilToInt()).coerceAtLeast(0)
                            } else {
                                constraints.maxHeight
                            }
                    ),
                    maxLines - currentLineCount,
                    overflow,
                )

            val paragraphTop = currentHeight
            val paragraphBottom = currentHeight + paragraph.height
            currentHeight = paragraphBottom

            val startLineIndex = currentLineCount
            val endLineIndex = startLineIndex + paragraph.lineCount
            currentLineCount = endLineIndex

            paragraphInfoList.add(
                ParagraphInfo(
                    paragraph = paragraph,
                    startIndex = paragraphInfo.startIndex,
                    endIndex = paragraphInfo.endIndex,
                    startLineIndex = startLineIndex,
                    endLineIndex = endLineIndex,
                    top = paragraphTop,
                    bottom = paragraphBottom
                )
            )

            if (
                paragraph.didExceedMaxLines ||
                    (endLineIndex == maxLines && index != intrinsics.infoList.lastIndex)
            ) {
                didExceedMaxLines = true
                break
            }
        }

        this.height = currentHeight
        this.lineCount = currentLineCount
        this.didExceedMaxLines = didExceedMaxLines
        this.paragraphInfoList = paragraphInfoList
        this.width = constraints.maxWidth.toFloat()
        this.placeholderRects =
            paragraphInfoList
                .fastFlatMap { paragraphInfo ->
                    with(paragraphInfo) { paragraph.placeholderRects.fastMap { it?.toGlobal() } }
                }
                .let {
                    // When paragraphs get ellipsized, the size of this list will be smaller than
                    // the input placeholders. In this case, fill this list with null so that it has
                    // the
                    // same size as the input placeholders.
                    if (it.size < intrinsics.placeholders.size) {
                        it + List(intrinsics.placeholders.size - it.size) { null }
                    } else {
                        it
                    }
                }
    }

    /** Paint the paragraphs to canvas. */
    @Deprecated(
        "Use the new paint function that takes canvas as the only required parameter.",
        level = DeprecationLevel.HIDDEN
    )
    fun paint(
        canvas: Canvas,
        color: Color = Color.Unspecified,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null
    ) {
        canvas.save()
        paragraphInfoList.fastForEach {
            it.paragraph.paint(canvas, color, shadow, decoration)
            canvas.translate(0f, it.paragraph.height)
        }
        canvas.restore()
    }

    /** Paint the paragraphs to canvas. */
    fun paint(
        canvas: Canvas,
        color: Color = Color.Unspecified,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        canvas.save()
        paragraphInfoList.fastForEach {
            it.paragraph.paint(canvas, color, shadow, decoration, drawStyle, blendMode)
            canvas.translate(0f, it.paragraph.height)
        }
        canvas.restore()
    }

    /** Paint the paragraphs to canvas. */
    fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        decoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        drawMultiParagraph(canvas, brush, alpha, shadow, decoration, drawStyle, blendMode)
    }

    /** Returns path that enclose the given text range. */
    fun getPathForRange(start: Int, end: Int): Path {
        require(start in 0..end && end <= annotatedString.text.length) {
            "Start($start) or End($end) is out of range [0..${annotatedString.text.length})," +
                " or start > end!"
        }

        if (start == end) return Path()

        val path = Path()
        findParagraphsByRange(paragraphInfoList, TextRange(start, end)) { paragraphInfo ->
            with(paragraphInfo) {
                path.addPath(
                    path =
                        paragraph
                            .getPathForRange(start = start.toLocalIndex(), end = end.toLocalIndex())
                            .toGlobal()
                )
            }
        }

        return path
    }

    /**
     * Returns line number closest to the given graphical vertical position. If you ask for a
     * vertical position before 0, you get 0; if you ask for a vertical position beyond the last
     * line, you get the last line.
     */
    fun getLineForVerticalPosition(vertical: Float): Int {
        val paragraphIndex = findParagraphByY(paragraphInfoList, vertical)
        return with(paragraphInfoList[paragraphIndex]) {
            if (length == 0) {
                startLineIndex
            } else {
                paragraph
                    .getLineForVerticalPosition(vertical.toLocalYPosition())
                    .toGlobalLineIndex()
            }
        }
    }

    /** Returns the character offset closest to the given graphical position. */
    fun getOffsetForPosition(position: Offset): Int {
        val paragraphIndex = findParagraphByY(paragraphInfoList, position.y)
        return with(paragraphInfoList[paragraphIndex]) {
            if (length == 0) {
                startIndex
            } else {
                paragraph.getOffsetForPosition(position.toLocal()).toGlobalIndex()
            }
        }
    }

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
        inclusionStrategy: TextInclusionStrategy
    ): TextRange {
        var firstParagraph = findParagraphByY(paragraphInfoList, rect.top)
        // The first paragraph contains the entire rect, return early in this case.
        if (
            paragraphInfoList[firstParagraph].bottom >= rect.bottom ||
                firstParagraph == paragraphInfoList.lastIndex
        ) {
            return with(paragraphInfoList[firstParagraph]) {
                paragraph.getRangeForRect(rect.toLocal(), granularity, inclusionStrategy).toGlobal()
            }
        }

        var lastParagraph = findParagraphByY(paragraphInfoList, rect.bottom)

        var startRange: TextRange = TextRange.Zero
        while (startRange == TextRange.Zero && firstParagraph <= lastParagraph) {
            startRange =
                with(paragraphInfoList[firstParagraph]) {
                    paragraph
                        .getRangeForRect(rect.toLocal(), granularity, inclusionStrategy)
                        .toGlobal()
                }
            ++firstParagraph
        }

        if (startRange == TextRange.Zero) {
            return TextRange.Zero
        }

        var endRange: TextRange = TextRange.Zero
        while (endRange == TextRange.Zero && firstParagraph <= lastParagraph) {
            endRange =
                with(paragraphInfoList[lastParagraph]) {
                    paragraph
                        .getRangeForRect(rect.toLocal(), granularity, inclusionStrategy)
                        .toGlobal()
                }
            --lastParagraph
        }

        if (endRange == TextRange.Zero) return startRange
        return TextRange(startRange.start, endRange.end)
    }

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect includes
     * the top, bottom, left and right of a character.
     */
    fun getBoundingBox(offset: Int): Rect {
        requireIndexInRange(offset)

        val paragraphIndex = findParagraphByIndex(paragraphInfoList, offset)
        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getBoundingBox(offset.toLocalIndex()).toGlobal()
        }
    }

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
    fun fillBoundingBoxes(
        range: TextRange,
        array: FloatArray,
        @IntRange(from = 0) arrayStart: Int
    ): FloatArray {
        requireIndexInRange(range.min)
        requireIndexInRangeInclusiveEnd(range.max)

        var currentArrayStart = arrayStart
        var currentHeight = 0f
        findParagraphsByRange(paragraphInfoList, range) { paragraphInfo ->
            with(paragraphInfo) {
                val paragraphStart = if (startIndex > range.min) startIndex else range.min
                val paragraphEnd = if (endIndex < range.max) endIndex else range.max
                val finalRange =
                    TextRange(paragraphStart.toLocalIndex(), paragraphEnd.toLocalIndex())
                paragraph.fillBoundingBoxes(finalRange, array, currentArrayStart)
                val currentArrayEnd = currentArrayStart + finalRange.length * 4
                var arrayIndex = currentArrayStart
                while (arrayIndex < currentArrayEnd) {
                    // update top and bottom
                    array[arrayIndex + 1] += currentHeight
                    array[arrayIndex + 3] += currentHeight
                    arrayIndex += 4
                }
                currentArrayStart = currentArrayEnd
                currentHeight += paragraphInfo.paragraph.height
            }
        }

        return array
    }

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
    fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        requireIndexInRangeInclusiveEnd(offset)

        val paragraphIndex =
            if (offset == annotatedString.length) {
                paragraphInfoList.lastIndex
            } else {
                findParagraphByIndex(paragraphInfoList, offset)
            }

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getHorizontalPosition(offset.toLocalIndex(), usePrimaryDirection)
        }
    }

    /** Get the text direction of the paragraph containing the given offset. */
    fun getParagraphDirection(offset: Int): ResolvedTextDirection {
        requireIndexInRangeInclusiveEnd(offset)

        val paragraphIndex =
            if (offset == annotatedString.length) {
                paragraphInfoList.lastIndex
            } else {
                findParagraphByIndex(paragraphInfoList, offset)
            }

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getParagraphDirection(offset.toLocalIndex())
        }
    }

    /** Get the text direction of the character at the given offset. */
    fun getBidiRunDirection(offset: Int): ResolvedTextDirection {
        requireIndexInRangeInclusiveEnd(offset)

        val paragraphIndex =
            if (offset == annotatedString.length) {
                paragraphInfoList.lastIndex
            } else {
                findParagraphByIndex(paragraphInfoList, offset)
            }

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getBidiRunDirection(offset.toLocalIndex())
        }
    }

    /**
     * Returns the TextRange of the word at the given character offset. Characters not part of a
     * word, such as spaces, symbols, and punctuation, have word breaks on both sides. In such
     * cases, this method will return TextRange(offset, offset+1). Word boundaries are defined more
     * precisely in Unicode Standard Annex #29 http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): TextRange {
        requireIndexInRangeInclusiveEnd(offset)

        val paragraphIndex =
            if (offset == annotatedString.length) {
                paragraphInfoList.lastIndex
            } else {
                findParagraphByIndex(paragraphInfoList, offset)
            }

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getWordBoundary(offset.toLocalIndex()).toGlobal(treatZeroAsNull = false)
        }
    }

    /** Returns rectangle of the cursor area. */
    fun getCursorRect(offset: Int): Rect {
        requireIndexInRangeInclusiveEnd(offset)

        val paragraphIndex =
            if (offset == annotatedString.length) {
                paragraphInfoList.lastIndex
            } else {
                findParagraphByIndex(paragraphInfoList, offset)
            }

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getCursorRect(offset.toLocalIndex()).toGlobal()
        }
    }

    /**
     * Returns the line number on which the specified text offset appears. If you ask for a position
     * before 0, you get 0; if you ask for a position beyond the end of the text, you get the last
     * line.
     */
    fun getLineForOffset(offset: Int): Int {
        val paragraphIndex =
            if (offset >= annotatedString.length) {
                paragraphInfoList.lastIndex
            } else if (offset < 0) {
                0
            } else {
                findParagraphByIndex(paragraphInfoList, offset)
            }
        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineForOffset(offset.toLocalIndex()).toGlobalLineIndex()
        }
    }

    /** Returns the left x Coordinate of the given line. */
    fun getLineLeft(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineLeft(lineIndex.toLocalLineIndex())
        }
    }

    /** Returns the right x Coordinate of the given line. */
    fun getLineRight(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineRight(lineIndex.toLocalLineIndex())
        }
    }

    /** Returns the top y coordinate of the given line. */
    fun getLineTop(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineTop(lineIndex.toLocalLineIndex()).toGlobalYPosition()
        }
    }

    /**
     * Returns the distance from the top of the [MultiParagraph] to the alphabetic baseline of the
     * given line.
     */
    fun getLineBaseline(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineBaseline(lineIndex.toLocalLineIndex()).toGlobalYPosition()
        }
    }

    /** Returns the bottom y coordinate of the given line. */
    fun getLineBottom(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineBottom(lineIndex.toLocalLineIndex()).toGlobalYPosition()
        }
    }

    /** Returns the height of the given line. */
    fun getLineHeight(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineHeight(lineIndex.toLocalLineIndex())
        }
    }

    /** Returns the width of the given line. */
    fun getLineWidth(lineIndex: Int): Float {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineWidth(lineIndex.toLocalLineIndex())
        }
    }

    /** Returns the start offset of the given line, inclusive. */
    fun getLineStart(lineIndex: Int): Int {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineStart(lineIndex.toLocalLineIndex()).toGlobalIndex()
        }
    }

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
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int {
        requireLineIndexInRange(lineIndex)

        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)

        return with(paragraphInfoList[paragraphIndex]) {
            paragraph.getLineEnd(lineIndex.toLocalLineIndex(), visibleEnd).toGlobalIndex()
        }
    }

    /**
     * Returns true if the given line is ellipsized, otherwise returns false.
     *
     * @param lineIndex a 0 based line index
     * @return true if the given line is ellipsized, otherwise false
     */
    fun isLineEllipsized(lineIndex: Int): Boolean {
        requireLineIndexInRange(lineIndex)
        val paragraphIndex = findParagraphByLineIndex(paragraphInfoList, lineIndex)
        return with(paragraphInfoList[paragraphIndex]) { paragraph.isLineEllipsized(lineIndex) }
    }

    private fun requireIndexInRange(offset: Int) {
        require(offset in annotatedString.text.indices) {
            "offset($offset) is out of bounds [0, ${annotatedString.length})"
        }
    }

    private fun requireIndexInRangeInclusiveEnd(offset: Int) {
        require(offset in 0..annotatedString.text.length) {
            "offset($offset) is out of bounds [0, ${annotatedString.length}]"
        }
    }

    private fun requireLineIndexInRange(lineIndex: Int) {
        require(lineIndex in 0 until lineCount) {
            "lineIndex($lineIndex) is out of bounds [0, $lineCount)"
        }
    }
}

/**
 * Given an character index of [MultiParagraph.annotatedString], find the corresponding
 * [ParagraphInfo] which covers the provided index.
 *
 * @param paragraphInfoList The list of [ParagraphInfo] containing the information of each paragraph
 *   in the [MultiParagraph].
 * @param index The target index in the [MultiParagraph]. It should be in the range of [0,
 *   text.length)
 * @return The index of the target [ParagraphInfo] in [paragraphInfoList].
 */
internal fun findParagraphByIndex(paragraphInfoList: List<ParagraphInfo>, index: Int): Int {
    return paragraphInfoList.fastBinarySearch { paragraphInfo ->
        when {
            paragraphInfo.startIndex > index -> 1
            paragraphInfo.endIndex <= index -> -1
            else -> 0
        }
    }
}

/**
 * Given the y graphical position relative to this [MultiParagraph], find the index of the
 * corresponding [ParagraphInfo] which occupies the provided position.
 *
 * @param paragraphInfoList The list of [ParagraphInfo] containing the information of each paragraph
 *   in the [MultiParagraph].
 * @param y The y coordinate position relative to the [MultiParagraph].
 * @return The index of the target [ParagraphInfo] in [paragraphInfoList].
 */
internal fun findParagraphByY(paragraphInfoList: List<ParagraphInfo>, y: Float): Int {
    if (y <= 0) return 0
    if (y >= paragraphInfoList.last().bottom) return paragraphInfoList.lastIndex
    return paragraphInfoList.fastBinarySearch { paragraphInfo ->
        when {
            paragraphInfo.top > y -> 1
            paragraphInfo.bottom <= y -> -1
            else -> 0
        }
    }
}

internal fun findParagraphsByRange(
    paragraphInfoList: List<ParagraphInfo>,
    range: TextRange,
    action: (ParagraphInfo) -> Unit
) {
    val paragraphIndex = findParagraphByIndex(paragraphInfoList, range.min)
    for (i in paragraphIndex until paragraphInfoList.size) {
        val paragraph = paragraphInfoList[i]
        if (paragraph.startIndex >= range.max) break
        if (paragraph.startIndex == paragraph.endIndex) continue
        action(paragraph)
    }
}

/**
 * Given an line index in [MultiParagraph], find the corresponding [ParagraphInfo] which covers the
 * provided line index.
 *
 * @param paragraphInfoList The list of [ParagraphInfo] containing the information of each paragraph
 *   in the [MultiParagraph].
 * @param lineIndex The target line index in the [MultiParagraph], it should be in the range of
 *   [0, [MultiParagraph.lineCount])
 * @return The index of the target [ParagraphInfo] in [paragraphInfoList].
 */
internal fun findParagraphByLineIndex(paragraphInfoList: List<ParagraphInfo>, lineIndex: Int): Int {
    return paragraphInfoList.fastBinarySearch { paragraphInfo ->
        when {
            paragraphInfo.startLineIndex > lineIndex -> 1
            paragraphInfo.endLineIndex <= lineIndex -> -1
            else -> 0
        }
    }
}

private inline fun <T> List<T>.fastBinarySearch(comparison: (T) -> Int): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0) low = mid + 1 else if (cmp > 0) high = mid - 1 else return mid // key found
    }
    return -(low + 1) // key not found
}

/**
 * This is a helper data structure to store the information of a single [Paragraph] in an
 * [MultiParagraph]. It's mainly used to convert a global index, lineNumber and [Offset] to the
 * local ones inside the [paragraph], and vice versa.
 *
 * @param paragraph The [Paragraph] object corresponding to this [ParagraphInfo].
 * @param startIndex The start index of this paragraph in the parent [MultiParagraph], inclusive.
 * @param endIndex The end index of this paragraph in the parent [MultiParagraph], exclusive.
 * @param startLineIndex The start line index of this paragraph in the parent [MultiParagraph],
 *   inclusive.
 * @param endLineIndex The end line index of this paragraph in the parent [MultiParagraph],
 *   exclusive.
 * @param top The top position of the [paragraph] relative to the parent [MultiParagraph].
 * @param bottom The bottom position of the [paragraph] relative to the parent [MultiParagraph].
 */
internal data class ParagraphInfo(
    val paragraph: Paragraph,
    val startIndex: Int,
    val endIndex: Int,
    var startLineIndex: Int = -1,
    var endLineIndex: Int = -1,
    var top: Float = -1.0f,
    var bottom: Float = -1.0f
) {

    /** The length of the text in the covered by this paragraph. */
    val length
        get() = endIndex - startIndex

    /** Convert an index in the parent [MultiParagraph] to the local index in the [paragraph]. */
    fun Int.toLocalIndex(): Int {
        return this.coerceIn(startIndex, endIndex) - startIndex
    }

    /**
     * Convert a local index in the [paragraph] to the global index in the parent [MultiParagraph].
     */
    fun Int.toGlobalIndex(): Int {
        return this + startIndex
    }

    /**
     * Convert a line index in the parent [MultiParagraph] to the local line index in the
     * [paragraph].
     */
    fun Int.toLocalLineIndex(): Int {
        return this - startLineIndex
    }

    /**
     * Convert a local line index in the [paragraph] to the global line index in the parent
     * [MultiParagraph].
     */
    fun Int.toGlobalLineIndex(): Int {
        return this + startLineIndex
    }

    /**
     * Convert a local y position relative to [paragraph] to the global y position relative to the
     * parent [MultiParagraph].
     */
    fun Float.toGlobalYPosition(): Float {
        return this + top
    }

    /**
     * Convert a global y position relative to the parent [MultiParagraph] to a local y position
     * relative to [paragraph].
     */
    fun Float.toLocalYPosition(): Float {
        return this - top
    }

    /**
     * Convert a [Offset] relative to the parent [MultiParagraph] to the local [Offset] relative to
     * the [paragraph].
     */
    fun Offset.toLocal(): Offset {
        return Offset(x, y - top)
    }

    /**
     * Convert a [Rect] relative to the [paragraph] to the [Rect] relative to the parent
     * [MultiParagraph].
     */
    fun Rect.toGlobal(): Rect {
        return translate(Offset(0f, this@ParagraphInfo.top))
    }

    /**
     * Convert a [Rect] relative to the parent [MultiParagraph] to the local [Rect] relative to this
     * [paragraph].
     */
    fun Rect.toLocal(): Rect {
        return translate(Offset(0f, -this@ParagraphInfo.top))
    }

    /**
     * Convert a [Path] relative to the [paragraph] to the [Path] relative to the parent
     * [MultiParagraph].
     *
     * Notice that this function changes the input value.
     */
    fun Path.toGlobal(): Path {
        translate(Offset(0f, top))
        return this
    }

    /**
     * Convert a [TextRange] in to the [paragraph] to the [TextRange] in the parent
     * [MultiParagraph].
     *
     * @param treatZeroAsNull whether [TextRange.Zero] is used represents `null`. When it's true,
     *   [TextRange.Zero] is not mapped to global index and is returned directly.
     */
    fun TextRange.toGlobal(treatZeroAsNull: Boolean = true): TextRange {
        if (treatZeroAsNull && this == TextRange.Zero) {
            return TextRange.Zero
        }
        return TextRange(start = start.toGlobalIndex(), end = end.toGlobalIndex())
    }
}
