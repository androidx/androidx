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
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.IntPxSize
import androidx.ui.core.PxPosition
import androidx.ui.core.Sp
import androidx.ui.core.constrain
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.sp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirection
import androidx.ui.graphics.Color
import androidx.ui.painting.BlendMode
import androidx.ui.painting.Canvas
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.Shader
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextOverflow
import java.util.Locale
import kotlin.math.ceil

private val DefaultTextAlign: TextAlign = TextAlign.Start
private val DefaultTextDirection: TextDirection = TextDirection.Ltr
/** The default font size if none is specified. */
private val DefaultFontSize: Sp = 14.sp

/**
 * Resolve text style to be able to pass to underlying paragraphs.
 *
 * We need to pass non-null font size to underlying paragraph.
 */
private fun resolveTextStyle(style: TextStyle?) =
    if (style == null) {
        TextStyle(fontSize = DefaultFontSize)
    } else if (style.fontSize == null) {
        style.copy(fontSize = DefaultFontSize)
    } else {
        style
    }

/**
 * Unfortunately, using full precision floating point here causes bad layouts because floating
 * point math isn't associative. If we add and subtract padding, for example, we'll get
 * different values when we estimate sizes and when we actually compute layout because the
 * operations will end up associated differently. To work around this problem for now, we round
 * fractional pixel values up to the nearest whole pixel value. The right long-term fix is to do
 * layout using fixed precision arithmetic.
 */
internal fun applyFloatingPointHack(layoutValue: Float): Float {
    return ceil(layoutValue)
}

/**
 * An object that paints a [TextSpan] tree into a [Canvas].
 *
 * To use a [TextDelegate], follow these steps:
 *
 * 1. Create a [TextSpan] tree and pass it to the [TextDelegate] constructor.
 *
 * 2. Call [layout] to prepare the paragraph.
 *
 * 3. Call [paint] as often as desired to paint the paragraph.
 *
 * If the width of the area into which the text is being painted changes, return to step 2. If the
 *  text to be painted changes, return to step 1.
 *
 * @param text The (potentially styled) text to paint. After this is set, you must call [layout]
 *   before the next call to [paint]. This and [textDirection] must be non-null before you call
 *   [layout].
 *
 * @param style The text style specified to render the text. Notice that you can also set text style
 *  on the given [AnnotatedString], and the style set on [text] always has higher priority than this
 *  setting. But if only one gobal text style is needed, passing it to [TextDelegate] is always
 *  preferred.
 *
 * @param paragraphStyle Style configuration that applies only to paragraphs such as text alignment,
 * or text direction.
 *
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it is truncated such that subsequent lines are
 *   dropped.
 *   After this is set, you must call [layout] before the next call to [paint].
 *
 * @param softWrap Whether the text should break at soft line breaks.
 *   If false, the glyphs in the text will be positioned as if there was unlimited horizontal space.
 *   If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
 *
 * @param overflow How visual overflow should be handled.
 *   Specifically, the ellipsis is applied to the last line before the line truncated by [maxLines],
 *   if [maxLines] is non-null and that line overflows the width constraint.
 *   After this is set, you must call [layout] before the next call to [paint].
 *
 * @param locale The locale used to select region-specific glyphs.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class TextDelegate(
    val text: AnnotatedString,
    style: TextStyle? = null,
    paragraphStyle: ParagraphStyle? = null,
    val maxLines: Int? = null,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val locale: Locale? = null,
    val density: Density,
    val resourceLoader: Font.ResourceLoader
) {
    init {
        assert(maxLines == null || maxLines > 0)
    }

    /**
     * The resolved text style.
     */
    val textStyle: TextStyle = resolveTextStyle(style)

    /**
     * The paragraph style.
     *
     * If null is passed to constructor, use default paragraph style.
     */
    val paragraphStyle: ParagraphStyle = paragraphStyle ?: ParagraphStyle()

    /**
     * The data class which holds text layout result.
     */
    @VisibleForTesting
    internal data class LayoutResult(
        /**
         * The multi paragraph object.
         *
         * The text layout is already computed.
         */
        val multiParagraph: MultiParagraph,
        /**
         * The amount of space required to paint this text.
         */
        val size: Size
    )

    /**
     * The text layout result. null if text layout is not computed.
     */
    @VisibleForTesting
    internal var layoutResult: LayoutResult? = null

    private var overflowShader: Shader? = null

    @VisibleForTesting
    internal var hasVisualOverflow = false

    private var lastMinWidth: Float = 0.0f
    private var lastMaxWidth: Float = 0.0f

    @VisibleForTesting
    internal val textDirection: TextDirection? =
        paragraphStyle?.textDirection ?: DefaultTextDirection

    private inline fun <T> assumeLayout(block: (LayoutResult) -> T) =
        block(layoutResult ?: throw AssertionError("layout must be called first"))

    /**
     * The width for text if all soft wrap opportunities were taken.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth: Float
        get() = assumeLayout { applyFloatingPointHack(it.multiParagraph.minIntrinsicWidth) }

    /**
     * The width at which increasing the width of the text no longer decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: Float
        get() = assumeLayout { applyFloatingPointHack(it.multiParagraph.maxIntrinsicWidth) }

    /**
     * The horizontal space required to paint this text.
     *
     * Valid only after [layout] has been called.
     */
    val width: Float
        get() = assumeLayout { applyFloatingPointHack(it.size.width) }

    /**
     * The vertical space required to paint this text.
     *
     * Valid only after [layout] has been called.
     */
    val height: Float
        get() = assumeLayout { applyFloatingPointHack(it.size.height) }

    /**
     * Computes the visual position of the glyphs for painting the text.
     *
     * The text will layout with a width that's as close to its max intrinsic width as possible
     * while still being greater than or equal to `minWidth` and less than or equal to `maxWidth`.
     *
     * The [text] and [textDirection] properties must be non-null before this is called.
     */
    private fun layoutText(minWidth: Float, maxWidth: Float): MultiParagraph {
        assert(textDirection != null) {
            "TextDelegate.textDirection must be set to a non-null value before using the" +
                    " TextDelegate."
        }

        // TODO(haoyuchang): fix that when softWarp is false and overflow is Ellipsis, ellipsis
        //  doesn't work.
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
        val finalMaxWidth = if (widthMatters) maxWidth else Float.POSITIVE_INFINITY

        // If the layout result is the same one we computed before, just return the previous
        // result.
        val prevResult = layoutResult
        if (prevResult != null && minWidth == lastMinWidth && finalMaxWidth == lastMaxWidth) {
            return prevResult.multiParagraph
        }

        lastMinWidth = minWidth
        lastMaxWidth = finalMaxWidth
        val multiParagraph = MultiParagraph(
            annotatedString = text,
            textStyle = textStyle,
            paragraphStyle = paragraphStyle,
            maxLines = maxLines,
            ellipsis = overflow == TextOverflow.Ellipsis,
            density = density,
            resourceLoader = resourceLoader
        ).apply { layout(ParagraphConstraints(width = finalMaxWidth)) }

        if (minWidth != finalMaxWidth) {
            val newWidth = multiParagraph.maxIntrinsicWidth.coerceIn(minWidth, finalMaxWidth)
            if (newWidth != multiParagraph.width) {
                multiParagraph.layout(ParagraphConstraints(width = newWidth))
            }
        }

        return multiParagraph
    }

    fun layout(constraints: Constraints) {
        val multiParagraph = layoutText(
            constraints.minWidth.value.toFloat(),
            constraints.maxWidth.value.toFloat()
        )

        val didOverflowHeight = multiParagraph.didExceedMaxLines
        val size = constraints.constrain(
            IntPxSize(multiParagraph.width.px.round(), multiParagraph.height.px.round())
        ).let {
            Size(it.width.value.toFloat(), it.height.value.toFloat())
        }
        val didOverflowWidth = size.width < multiParagraph.width
        // TODO(abarth): We're only measuring the sizes of the line boxes here. If
        // the glyphs draw outside the line boxes, we might think that there isn't
        // visual overflow when there actually is visual overflow. This can become
        // a problem if we start having horizontal overflow and introduce a clip
        // that affects the actual (but undetected) vertical overflow.
        hasVisualOverflow = didOverflowWidth || didOverflowHeight
        overflowShader = if (hasVisualOverflow && overflow == TextOverflow.Fade) {
            val fadeSizeDelegate = TextDelegate(
                text = AnnotatedString(text = "\u2026", textStyles = listOf()),
                style = textStyle,
                paragraphStyle = paragraphStyle,
                density = density,
                resourceLoader = resourceLoader
            )
            val paragraphForFadeSizeDelegate = fadeSizeDelegate.layoutText(
                minWidth = 1.0f,
                maxWidth = Float.POSITIVE_INFINITY
            )
            val fadeWidth = paragraphForFadeSizeDelegate.width
            val fadeHeight = paragraphForFadeSizeDelegate.height
            if (didOverflowWidth) {
                val (fadeStart, fadeEnd) = if (textDirection == TextDirection.Rtl) {
                    Pair(fadeWidth, 0.0f)
                } else {
                    Pair(size.width - fadeWidth, size.width)
                }
                Gradient.linear(
                    Offset(fadeStart, 0.0f),
                    Offset(fadeEnd, 0.0f),
                    listOf(Color(0xFFFFFFFF), Color(0x00FFFFFF))
                )
            } else {
                val fadeEnd = size.height
                val fadeStart = fadeEnd - fadeHeight
                Gradient.linear(
                    Offset(0.0f, fadeStart),
                    Offset(0.0f, fadeEnd),
                    listOf(Color(0xFFFFFFFF), Color(0x00FFFFFF))
                )
            }
        } else {
            null
        }

        layoutResult = LayoutResult(multiParagraph, size)
    }

    /**
     * Paints the text onto the given canvas.
     *
     * Valid only after [layout] has been called.
     *
     * If you cannot see the text being painted, check that your text color does not conflict with
     * the background on which you are drawing. The default text color is white (to contrast with
     * the default black background color), so if you are writing an application with a white
     * background, the text will not be visible by default.
     *
     * To set the text style, specify a [TextStyle] when creating the [TextSpan] that you pass to
     * the [TextDelegate] constructor or to the [text] property.
     */
    fun paint(canvas: Canvas) = assumeLayout { layoutResult ->
        // Ideally we could compute the min/max intrinsic width/height with a
        // non-destructive operation. However, currently, computing these values
        // will destroy state inside the painter. If that happens, we need to
        // get back the correct state by calling layout again.
        //
        // TODO(abarth): Make computing the min/max intrinsic width/height
        // a non-destructive operation.
        //
        // If you remove this call, make sure that changing the textAlign still
        // works properly.
        // TODO(Migration/qqd): Need to figure out where this constraints come from and how to make
        // it non-null. For now Crane Text version does not need to layout text again. Comment it.
        // layoutTextWithConstraints(constraints!!)

        if (hasVisualOverflow) {
            val bounds = Rect.fromLTWH(0f, 0f, layoutResult.size.width, layoutResult.size.height)
            if (overflowShader != null) {
                // This layer limits what the shader below blends with to be just the text
                // (as opposed to the text and its background).
                canvas.saveLayer(bounds, Paint())
            } else {
                canvas.save()
            }
            canvas.clipRect(bounds)
        }

        layoutResult.multiParagraph.paint(canvas)
        val size = layoutResult.size
        if (hasVisualOverflow) {
            if (overflowShader != null) {
                val bounds = Rect.fromLTWH(0f, 0f, size.width, size.height)
                val paint = Paint()
                paint.blendMode = BlendMode.multiply
                paint.shader = overflowShader
                canvas.drawRect(bounds, paint)
            }
            canvas.restore()
        }
    }

    /**
     * Draws text background of the given range.
     *
     * If the given range is empty, do nothing.
     *
     * @param start inclusive start character offset of the drawing range.
     * @param end exclusive end character offset of the drawing range.
     * @param color a color to be used for drawing background.
     * @param canvas the target canvas.
     */
    fun paintBackground(
        start: Int,
        end: Int,
        color: Color,
        canvas: Canvas
    ) = assumeLayout { layoutResult ->
        if (start == end) return
        val selectionPath = layoutResult.multiParagraph.getPathForRange(start, end)
        // TODO(haoyuchang): check if move this paint to parameter is better
        canvas.drawPath(selectionPath, Paint().apply { this.color = color })
    }

    /**
     * Draws the cursor at the given character offset.
     *
     * TODO(nona): Make cursor customizable.
     *
     * @param offset the cursor offset in the text.
     * @param canvas the target canvas.
     */
    fun paintCursor(offset: Int, canvas: Canvas) = assumeLayout { layoutResult ->
        val cursorRect = layoutResult.multiParagraph.getCursorRect(offset)
        canvas.drawRect(cursorRect, Paint().apply { this.color = Color.Black })
    }

    /**
     * Returns the bottom y coordinate of the given line.
     */
    fun getLineBottom(lineIndex: Int): Float = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getLineBottom(lineIndex)
    }

    /**
     * Returns the line number on which the specified text offset appears.
     * If you ask for a position before 0, you get 0; if you ask for a position
     * beyond the end of the text, you get the last line.
     */
    fun getLineForOffset(offset: Int): Int = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getLineForOffset(offset)
    }

    /**
     * Get the primary horizontal position for the specified text offset.
     */
    fun getPrimaryHorizontal(offset: Int): Float = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getPrimaryHorizontal(offset)
    }

    /**
     * Get the secondary horizontal position for the specified text offset.
     */
    fun getSecondaryHorizontal(offset: Int): Float = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getSecondaryHorizontal(offset)
    }

    /**
     * Get the text direction of the paragraph containing the given offset.
     */
    fun getParagraphDirection(offset: Int): TextDirection = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getParagraphDirection(offset)
    }

    /**
     * Get the text direction of the character at the given offset.
     */
    fun getBidiRunDirection(offset: Int): TextDirection = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getBidiRunDirection(offset)
    }

    /** Returns the character offset closest to the given graphical position. */
    fun getOffsetForPosition(position: PxPosition): Int = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getOffsetForPosition(position)
    }

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect includes
     * the top, bottom, left and right of a character.
     *
     * Valid only after [layout] has been called.
     */
    fun getBoundingBox(offset: Int): Rect = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getBoundingBox(offset)
    }

    /**
     * Returns the text range of the word at the given character offset. Characters not part of a
     * word, such as spaces, symbols, and punctuation, have word breaks on both sides. In such
     * cases, this method will return a text range that contains the given character offset.
     *
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * <http://www.unicode.org/reports/tr29/#Word_Boundaries>.
     */
    fun getWordBoundary(offset: Int): TextRange = assumeLayout { layoutResult ->
        layoutResult.multiParagraph.getWordBoundary(offset)
    }
}
