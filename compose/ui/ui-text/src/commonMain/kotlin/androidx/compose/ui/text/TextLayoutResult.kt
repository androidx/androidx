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

import androidx.collection.mutableScatterMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.platform.SynchronizedObject
import androidx.compose.ui.text.platform.makeSynchronizedObject
import androidx.compose.ui.text.platform.synchronized
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/** Holds parameters used to compute text layout. */
public class TextLayoutInput
private constructor(
    /** The text used for computing text layout. */
    public val text: AnnotatedString,

    /** The text layout used for computing this text layout. */
    public val style: TextStyle,

    /**
     * A list of [Placeholder]s inserted into text layout that reserves space to embed icons or
     * custom emojis. A list of bounding boxes will be returned in
     * [TextLayoutResult.placeholderRects] that corresponds to this input.
     *
     * @see TextLayoutResult.placeholderRects
     * @see MultiParagraph
     * @see MultiParagraphIntrinsics
     */
    public val placeholders: List<AnnotatedString.Range<Placeholder>>,

    /** The maxLines param used for computing this text layout. */
    public val maxLines: Int,

    /** The maxLines param used for computing this text layout. */
    public val softWrap: Boolean,

    /** The overflow param used for computing this text layout */
    public val overflow: TextOverflow,

    /** The density param used for computing this text layout. */
    public val density: Density,

    /** The layout direction used for computing this text layout. */
    public val layoutDirection: LayoutDirection,

    /**
     * The font resource loader used for computing this text layout.
     *
     * This is no longer used.
     *
     * @see fontFamilyResolver
     */
    @Suppress("DEPRECATION") resourceLoader: Font.ResourceLoader?,

    /** The font resolver used for computing this text layout. */
    public val fontFamilyResolver: FontFamily.Resolver,

    /** The minimum width provided while calculating this text layout. */
    public val constraints: Constraints,
) {

    private var _developerSuppliedResourceLoader = resourceLoader
    @Deprecated(
        "Replaced with FontFamily.Resolver",
        replaceWith = ReplaceWith("fontFamilyResolver"),
    )
    @Suppress("DEPRECATION")
    public val resourceLoader: Font.ResourceLoader
        get() {
            return _developerSuppliedResourceLoader
                ?: DeprecatedBridgeFontResourceLoader.from(fontFamilyResolver)
        }

    @Deprecated(
        "Font.ResourceLoader is replaced with FontFamily.Resolver",
        replaceWith =
            ReplaceWith(
                "TextLayoutInput(text, style, placeholders, " +
                    "maxLines, softWrap, overflow, density, layoutDirection, fontFamilyResolver, " +
                    "constraints"
            ),
    )
    @Suppress("DEPRECATION")
    public constructor(
        text: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        maxLines: Int,
        softWrap: Boolean,
        overflow: TextOverflow,
        density: Density,
        layoutDirection: LayoutDirection,
        resourceLoader: Font.ResourceLoader,
        constraints: Constraints,
    ) : this(
        text,
        style,
        placeholders,
        maxLines,
        softWrap,
        overflow,
        density,
        layoutDirection,
        resourceLoader,
        createFontFamilyResolver(resourceLoader),
        constraints,
    )

    public constructor(
        text: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        maxLines: Int,
        softWrap: Boolean,
        overflow: TextOverflow,
        density: Density,
        layoutDirection: LayoutDirection,
        fontFamilyResolver: FontFamily.Resolver,
        constraints: Constraints,
    ) : this(
        text,
        style,
        placeholders,
        maxLines,
        softWrap,
        overflow,
        density,
        layoutDirection,
        @Suppress("DEPRECATION") null,
        fontFamilyResolver,
        constraints,
    )

    @Deprecated(
        "Font.ResourceLoader is deprecated",
        replaceWith =
            ReplaceWith(
                "TextLayoutInput(text, style, placeholders," +
                    " maxLines, softWrap, overFlow, density, layoutDirection, fontFamilyResolver, " +
                    "constraints)"
            ),
    )
    // Unfortunately, there's no way to deprecate and add a parameter to a copy chain such that the
    // resolution is valid.
    //
    // However, as this was never intended to be a public function we will not replace it. There is
    // no use case for calling this method directly.
    public fun copy(
        text: AnnotatedString = this.text,
        style: TextStyle = this.style,
        placeholders: List<AnnotatedString.Range<Placeholder>> = this.placeholders,
        maxLines: Int = this.maxLines,
        softWrap: Boolean = this.softWrap,
        overflow: TextOverflow = this.overflow,
        density: Density = this.density,
        layoutDirection: LayoutDirection = this.layoutDirection,
        @Suppress("DEPRECATION") resourceLoader: Font.ResourceLoader = this.resourceLoader,
        constraints: Constraints = this.constraints,
    ): TextLayoutInput {
        return TextLayoutInput(
            text = text,
            style = style,
            placeholders = placeholders,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow,
            density = density,
            layoutDirection = layoutDirection,
            resourceLoader = resourceLoader,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints,
        )
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextLayoutInput) return false

        if (text != other.text) return false
        if (style != other.style) return false
        if (placeholders != other.placeholders) return false
        if (maxLines != other.maxLines) return false
        if (softWrap != other.softWrap) return false
        if (overflow != other.overflow) return false
        if (density != other.density) return false
        if (layoutDirection != other.layoutDirection) return false
        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (constraints != other.constraints) return false

        return true
    }

    public override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + placeholders.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + overflow.hashCode()
        result = 31 * result + density.hashCode()
        result = 31 * result + layoutDirection.hashCode()
        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + constraints.hashCode()
        return result
    }

    public override fun toString(): String {
        return "TextLayoutInput(" +
            "text=$text, " +
            "style=$style, " +
            "placeholders=$placeholders, " +
            "maxLines=$maxLines, " +
            "softWrap=$softWrap, " +
            "overflow=$overflow, " +
            "density=$density, " +
            "layoutDirection=$layoutDirection, " +
            "fontFamilyResolver=$fontFamilyResolver, " +
            "constraints=$constraints" +
            ")"
    }
}

@Suppress("DEPRECATION")
private class DeprecatedBridgeFontResourceLoader
private constructor(private val fontFamilyResolver: FontFamily.Resolver) : Font.ResourceLoader {
    @Deprecated(
        "Replaced by FontFamily.Resolver, this method should not be called",
        ReplaceWith("FontFamily.Resolver.resolve(font, )"),
    )
    override fun load(font: Font): Any {
        return fontFamilyResolver.resolve(font.toFontFamily(), font.weight, font.style).value
    }

    companion object {
        // In normal usage will  be a map of size 1.
        //
        // To fill this map with a large number of entries an app must:
        //
        // 1. Repeatedly change FontFamily.Resolver
        // 2. Call the deprecated method getFontResourceLoader on TextLayoutInput
        //
        // If this map is found to be large in profiling of an app, please modify your code to not
        // call getFontResourceLoader, and evaluate if FontFamily.Resolver is being correctly cached
        // (via e.g. remember)
        var cache = mutableScatterMapOf<FontFamily.Resolver, Font.ResourceLoader>()
        val lock: SynchronizedObject = makeSynchronizedObject()

        fun from(fontFamilyResolver: FontFamily.Resolver): Font.ResourceLoader {
            synchronized(lock) {
                // the same resolver to return the same ResourceLoader
                cache[fontFamilyResolver]?.let {
                    return it
                }

                val deprecatedBridgeFontResourceLoader =
                    DeprecatedBridgeFontResourceLoader(fontFamilyResolver)
                cache[fontFamilyResolver] = deprecatedBridgeFontResourceLoader
                return deprecatedBridgeFontResourceLoader
            }
        }
    }
}

/** Holds the result of a text layout computation. */
public class TextLayoutResult
public constructor(
    /** The input parameters used for this layout. */
    public val layoutInput: TextLayoutInput,

    /** The computed [MultiParagraph] layout. */
    public val multiParagraph: MultiParagraph,

    /**
     * The width and height of this text layout.
     *
     * Unlike [multiParagraph] dimensions, this size respects the input constraints.
     */
    public val size: IntSize,
) {
    /** The distance from the top to the alphabetic baseline of the first line. */
    public val firstBaseline: Float = multiParagraph.firstBaseline

    /** The distance from the top to the alphabetic baseline of the last line. */
    public val lastBaseline: Float = multiParagraph.lastBaseline

    /** True if the text height exceeds the layout boundaries. */
    public val didOverflowHeight: Boolean
        get() = multiParagraph.didExceedMaxLines || size.height < multiParagraph.height

    /** True if the text width exceeds the layout boundaries. */
    public val didOverflowWidth: Boolean
        get() = size.width < multiParagraph.width

    /** True if the text overflows vertically or horizontally. */
    public val hasVisualOverflow: Boolean
        get() = didOverflowWidth || didOverflowHeight

    /**
     * Returns a list of bounding boxes that is reserved for [TextLayoutInput.placeholders]. Each
     * [Rect] in this list corresponds to the [Placeholder] passed to [TextLayoutInput.placeholders]
     * and it will have the height and width specified in the [Placeholder]. It's guaranteed that
     * [TextLayoutInput.placeholders] and [TextLayoutResult.placeholderRects] will have same length
     * and order.
     *
     * @see TextLayoutInput.placeholders
     * @see Placeholder
     */
    public val placeholderRects: List<Rect?> = multiParagraph.placeholderRects

    /** Returns a number of lines of this text layout */
    public val lineCount: Int
        get() = multiParagraph.lineCount

    /**
     * Returns the start offset of the given line, inclusive.
     *
     * The start offset represents a position in text before the first character in the given line.
     * For example, `getLineStart(1)` will return 4 for the text below
     * <pre>
     * ┌────┐
     * │abcd│
     * │efg │
     * └────┘
     * </pre>
     *
     * @param lineIndex the line number
     * @return the start offset of the line
     */
    public fun getLineStart(lineIndex: Int): Int = multiParagraph.getLineStart(lineIndex)

    /**
     * Returns the end offset of the given line.
     *
     * The end offset represents a position in text after the last character in the given line. For
     * example, `getLineEnd(0)` will return 4 for the text below
     * <pre>
     * ┌────┐
     * │abcd│
     * │efg │
     * └────┘
     * </pre>
     *
     * Characters being ellipsized are treated as invisible characters. So that if visibleEnd is
     * false, it will return line end including the ellipsized characters and vice versa.
     *
     * @param lineIndex the line number
     * @param visibleEnd if true, the returned line end will not count trailing whitespaces or
     *   linefeed characters. Otherwise, this function will return the logical line end. By default
     *   it's false.
     * @return an exclusive end offset of the line.
     */
    public fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int =
        multiParagraph.getLineEnd(lineIndex, visibleEnd)

    /**
     * Returns true if the given line is ellipsized, otherwise returns false.
     *
     * @param lineIndex a 0 based line index
     * @return true if the given line is ellipsized, otherwise false
     */
    public fun isLineEllipsized(lineIndex: Int): Boolean =
        multiParagraph.isLineEllipsized(lineIndex)

    /**
     * Returns the top y coordinate of the given line.
     *
     * @param lineIndex the line number
     * @return the line top y coordinate
     */
    public fun getLineTop(lineIndex: Int): Float = multiParagraph.getLineTop(lineIndex)

    /**
     * Returns the distance in pixels from the top of the text layout to the alphabetic baseline of
     * the line at index [lineIndex].
     */
    public fun getLineBaseline(lineIndex: Int): Float = multiParagraph.getLineBaseline(lineIndex)

    /**
     * Returns the bottom y coordinate of the given line.
     *
     * @param lineIndex the line number
     * @return the line bottom y coordinate
     */
    public fun getLineBottom(lineIndex: Int): Float = multiParagraph.getLineBottom(lineIndex)

    /**
     * Returns the left x coordinate of the given line.
     *
     * @param lineIndex the line number
     * @return the line left x coordinate
     */
    public fun getLineLeft(lineIndex: Int): Float = multiParagraph.getLineLeft(lineIndex)

    /**
     * Returns the right x coordinate of the given line.
     *
     * @param lineIndex the line number
     * @return the line right x coordinate
     */
    public fun getLineRight(lineIndex: Int): Float = multiParagraph.getLineRight(lineIndex)

    /**
     * Returns the line number on which the specified text offset appears.
     *
     * If you ask for a position before 0, you get 0; if you ask for a position beyond the end of
     * the text, you get the last line.
     *
     * @param offset a character offset
     * @return the 0 origin line number.
     */
    public fun getLineForOffset(offset: Int): Int = multiParagraph.getLineForOffset(offset)

    /**
     * Returns line number closest to the given graphical vertical position.
     *
     * If you ask for a vertical position before 0, you get 0; if you ask for a vertical position
     * beyond the last line, you get the last line.
     *
     * @param vertical the vertical position
     * @return the 0 origin line number.
     */
    public fun getLineForVerticalPosition(vertical: Float): Int =
        multiParagraph.getLineForVerticalPosition(vertical)

    /**
     * Get the horizontal position for the specified text [offset].
     *
     * Returns the relative distance from the text starting offset. For example, if the paragraph
     * direction is Left-to-Right, this function returns positive value as a distance from the
     * left-most edge. If the paragraph direction is Right-to-Left, this function returns negative
     * value as a distance from the right-most edge.
     *
     * [usePrimaryDirection] argument is taken into account only when the offset is in the BiDi
     * directional transition point. [usePrimaryDirection] is true means use the primary direction
     * run's coordinate, and use the secondary direction's run's coordinate if false.
     *
     * @param offset a character offset
     * @param usePrimaryDirection true for using the primary run's coordinate if the given offset is
     *   in the BiDi directional transition point.
     * @return the relative distance from the text starting edge.
     * @see MultiParagraph.getHorizontalPosition
     */
    public fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float =
        multiParagraph.getHorizontalPosition(offset, usePrimaryDirection)

    /**
     * Get the text direction of the paragraph containing the given offset.
     *
     * @param offset a character offset
     * @return the paragraph direction
     */
    public fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        multiParagraph.getParagraphDirection(offset)

    /**
     * Get the text direction of the resolved BiDi run that the character at the given offset
     * associated with.
     *
     * @param offset a character offset
     * @return the direction of the BiDi run of the given character offset.
     */
    public fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        multiParagraph.getBidiRunDirection(offset)

    /**
     * Returns the character offset closest to the given graphical position.
     *
     * @param position a graphical position in this text layout
     * @return a character offset that is closest to the given graphical position.
     */
    public fun getOffsetForPosition(position: Offset): Int =
        multiParagraph.getOffsetForPosition(position)

    /**
     * Returns the bounding box of the character for given character offset.
     *
     * @param offset a character offset
     * @return a bounding box for the character in pixels.
     */
    public fun getBoundingBox(offset: Int): Rect = multiParagraph.getBoundingBox(offset)

    /**
     * Returns the text range of the word at the given character offset.
     *
     * Characters not part of a word, such as spaces, symbols, and punctuation, have word breaks on
     * both sides. In such cases, this method will return a text range that contains the given
     * character offset.
     *
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * <http://www.unicode.org/reports/tr29/#Word_Boundaries>.
     */
    public fun getWordBoundary(offset: Int): TextRange = multiParagraph.getWordBoundary(offset)

    /**
     * Returns the rectangle of the cursor area
     *
     * @param offset An character offset of the cursor
     * @return a rectangle of cursor region
     */
    public fun getCursorRect(offset: Int): Rect = multiParagraph.getCursorRect(offset)

    /**
     * Returns path that enclose the given text range.
     *
     * @param start an inclusive start character offset
     * @param end an exclusive end character offset
     * @return a drawing path
     */
    public fun getPathForRange(start: Int, end: Int): Path =
        multiParagraph.getPathForRange(start, end)

    public fun copy(
        layoutInput: TextLayoutInput = this.layoutInput,
        size: IntSize = this.size,
    ): TextLayoutResult {
        return TextLayoutResult(
            layoutInput = layoutInput,
            multiParagraph = multiParagraph,
            size = size,
        )
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextLayoutResult) return false

        if (layoutInput != other.layoutInput) return false
        if (multiParagraph != other.multiParagraph) return false
        if (size != other.size) return false
        if (firstBaseline != other.firstBaseline) return false
        if (lastBaseline != other.lastBaseline) return false
        if (placeholderRects != other.placeholderRects) return false

        return true
    }

    public override fun hashCode(): Int {
        var result = layoutInput.hashCode()
        result = 31 * result + multiParagraph.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + firstBaseline.hashCode()
        result = 31 * result + lastBaseline.hashCode()
        result = 31 * result + placeholderRects.hashCode()
        return result
    }

    public override fun toString(): String {
        return "TextLayoutResult(" +
            "layoutInput=$layoutInput, " +
            "multiParagraph=$multiParagraph, " +
            "size=$size, " +
            "firstBaseline=$firstBaseline, " +
            "lastBaseline=$lastBaseline, " +
            "placeholderRects=$placeholderRects" +
            ")"
    }
}
