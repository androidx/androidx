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
import androidx.annotation.VisibleForTesting
import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.core.TextUnit
import androidx.ui.core.constrain
import androidx.ui.core.ipx
import androidx.ui.core.sp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.LinearGradientShader
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Shader
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirection
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextOverflow
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The default font size if none is specified. */
private val DefaultFontSize: TextUnit = 14.sp

/**
 * Resolve text style to be able to pass to underlying paragraphs.
 *
 * We need to pass non-null font size to underlying paragraph.
 */
private fun resolveTextStyle(style: TextStyle?, layoutDirection: LayoutDirection): TextStyle {
    val textDirectionAlgorithm = style?.textDirectionAlgorithm
        ?: resolveTextDirectionAlgorithm(layoutDirection, null)

    val fontSize = when (style?.fontSize) {
        null -> DefaultFontSize
        TextUnit.Inherit -> DefaultFontSize
        else -> style.fontSize
    }

    return when {
        style == null -> TextStyle(
            fontSize = fontSize,
            textDirectionAlgorithm = textDirectionAlgorithm
        )
        style.textDirectionAlgorithm != textDirectionAlgorithm ||
        style.fontSize != fontSize -> style.copy(
            fontSize = fontSize,
            textDirectionAlgorithm = textDirectionAlgorithm
        )
        else -> style
    }
}

/**
 * An object that paints text onto a [Canvas].
 *
 * To use a [TextDelegate], follow these steps:
 *
 * 1. Create an [AnnotatedString] and pass it to the [TextDelegate] constructor.
 *
 * 2. Call [layout] to prepare the paragraph.
 *
 * 3. Call [paint] as often as desired to paint the paragraph.
 *
 *  If the width of the area into which the text is being painted changes, return to step 2. If the
 *  text to be painted changes, return to step 1.
 *
 * @param text the text to paint.
 *
 * @param style The text style specified to render the text. Notice that you can also set text
 * style on the given [AnnotatedString], and the style set on [text] always has higher priority
 * than this setting. But if only one global text style is needed, passing it to [TextDelegate]
 * is always preferred.
 *
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it is truncated such that subsequent
 * lines are dropped.
 *
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and [TextAlign] may have unexpected effects.
 *
 * @param overflow How visual overflow should be handled. Specifically, the ellipsis is applied
 * to the last line before the line truncated by [maxLines], if [maxLines] is non-null and that
 * line overflows the width constraint.
 *
 * @param layoutDirection The composable layout direction.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class TextDelegate(
    val text: AnnotatedString,
    style: TextStyle? = null,
    val maxLines: Int = Int.MAX_VALUE,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val density: Density,
    val layoutDirection: LayoutDirection,
    val resourceLoader: Font.ResourceLoader
) {
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
         * The amount of space required to paint this text in IntPx.
         */
        val size: IntPxSize,

        /**
         * The minimum width provided while calculating this result.
         */
        val minWidth: IntPx,

        /**
         * The maximum width provided while calculating this result.
         */
        val maxWidth: IntPx
    ) {
        private val didOverflowHeight: Boolean get() = multiParagraph.didExceedMaxLines
        val didOverflowWidth: Boolean get() = size.width.value < multiParagraph.width
        val hasVisualOverflow: Boolean get() = didOverflowWidth || didOverflowHeight
    }

    val style: TextStyle = resolveTextStyle(style, layoutDirection)

    /**
     * The text layout result. null if text layout is not computed.
     */
    @VisibleForTesting
    internal var layoutResult: LayoutResult? = null

    private var overflowShader: Shader? = null

    @VisibleForTesting
    internal var paragraphIntrinsics: MultiParagraphIntrinsics? = null

    private inline fun <T> assumeLayout(block: (LayoutResult) -> T) =
        block(layoutResult ?: throw AssertionError("layout must be called first"))

    private inline fun <T> assumeIntrinsics(block: (MultiParagraphIntrinsics) -> T) =
        block(paragraphIntrinsics
            ?: throw AssertionError("layoutForIntrinsics must be called first")
        )

    /**
     * The width for text if all soft wrap opportunities were taken.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth: IntPx get() = assumeIntrinsics { it.minIntrinsicWidth.toIntPx() }

    /**
     * The width at which increasing the width of the text no longer decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: IntPx get() = assumeIntrinsics { it.maxIntrinsicWidth.toIntPx() }

    /**
     * The horizontal space required to paint this text.
     *
     * Valid only after [layout] has been called.
     */
    val width: IntPx get() = assumeLayout { it.size.width }

    /**
     * The vertical space required to paint this text.
     *
     * Valid only after [layout] has been called.
     */
    val height: IntPx get() = assumeLayout { it.size.height }

    /**
     * The vertical space from the top of text box to the baseline of the first line.
     *
     * Valid only after [layout] has been called.
     */
    val firstBaseline: IntPx get() = assumeLayout { it.multiParagraph.firstBaseline.toIntPx() }

    /**
     * The vertical space from the top of text box to the baseline of the last line.
     *
     * Valid only after [layout] has been called.
     */
    val lastBaseline: IntPx get() = assumeLayout { it.multiParagraph.lastBaseline.toIntPx() }

    init {
        assert(maxLines > 0)
    }

    fun layoutIntrinsics() {
        var intrinsics = paragraphIntrinsics ?: MultiParagraphIntrinsics(
            annotatedString = text,
            style = style,
            density = density,
            resourceLoader = resourceLoader
        )

        paragraphIntrinsics = intrinsics
    }

    /**
     * Computes the visual position of the glyphs for painting the text.
     *
     * The text will layout with a width that's as close to its max intrinsic width as possible
     * while still being greater than or equal to `minWidth` and less than or equal to `maxWidth`.
     */
    private fun layoutText(minWidth: Float, maxWidth: Float): MultiParagraph {
        layoutIntrinsics()
        assumeIntrinsics { paragraphIntrinsics ->
            // if minWidth == maxWidth the width is fixed.
            //    therefore we can pass that value to our paragraph and use it
            // if minWidth != maxWidth there is a range
            //    then we should check if the max intrinsic width is in this range to decide the
            //    width to be passed to Paragraph
            //        if max intrinsic width is between minWidth and maxWidth
            //           we can use it to layout
            //        else if max intrinsic width is greater than maxWidth, we can only use maxWidth
            //        else if max intrinsic width is less than minWidth, we should use minWidth
            val width = if (minWidth == maxWidth) {
                maxWidth
            } else {
                paragraphIntrinsics.maxIntrinsicWidth.coerceIn(minWidth, maxWidth)
            }

            return MultiParagraph(
                intrinsics = paragraphIntrinsics,
                maxLines = maxLines,
                ellipsis = overflow == TextOverflow.Ellipsis,
                constraints = ParagraphConstraints(width = width)
            )
        }
    }

    fun layout(constraints: Constraints) {
        val minWidth = constraints.minWidth
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
        val maxWidth = if (widthMatters) constraints.maxWidth else IntPx.Infinity

        // If the layout result is the same one we computed before, just return the previous
        // result.
        layoutResult?.let {
            if (it.minWidth == minWidth && it.maxWidth == maxWidth) return@layout
        }

        val multiParagraph = layoutText(minWidth.value.toFloat(), maxWidth.value.toFloat())

        val size = constraints.constrain(
            IntPxSize(multiParagraph.width.toIntPx(), multiParagraph.height.toIntPx())
        )

        layoutResult = LayoutResult(multiParagraph, size, minWidth, maxWidth).also {
            overflowShader = createOverflowShader(it)
        }
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
     * To set the text style, specify a [SpanStyle] when creating the [TextSpan] that you pass to
     * the [TextDelegate] constructor or to the [text] property.
     */
    fun paint(canvas: Canvas) = assumeLayout { layoutResult ->
        val width = layoutResult.size.width.value.toFloat()
        val height = layoutResult.size.height.value.toFloat()

        if (layoutResult.hasVisualOverflow) {
            val bounds = Rect.fromLTWH(0f, 0f, width, height)
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

        if (layoutResult.hasVisualOverflow) {
            if (overflowShader != null) {
                val bounds = Rect.fromLTWH(0f, 0f, width, height)
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
        canvas.drawPath(selectionPath, Paint().apply { this.color = color })
    }

    /**
     * Draws the cursor at the given character offset.
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
     * Get the horizontal position for the specified text [offset].
     * @see MultiParagraph.getHorizontalPosition
     */
    fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float =
        assumeLayout { layoutResult ->
            layoutResult.multiParagraph.getHorizontalPosition(offset, usePrimaryDirection)
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

private fun TextDelegate.createOverflowShader(
    layoutResult: TextDelegate.LayoutResult
): Shader? {
    return if (layoutResult.hasVisualOverflow && overflow == TextOverflow.Fade) {
        val paragraph = Paragraph(
            text = "\u2026", // horizontal ellipsis
            spanStyles = listOf(),
            style = style,
            density = density,
            resourceLoader = resourceLoader,
            constraints = ParagraphConstraints(Float.POSITIVE_INFINITY)
        )

        val fadeWidth = paragraph.maxIntrinsicWidth
        val fadeHeight = paragraph.height
        val width = layoutResult.size.width.value.toFloat()

        if (layoutResult.didOverflowWidth) {
            // FIXME: Should only fade the last line, i.e., should use last line's direction.
            // (b/139496055)
            val (fadeStart, fadeEnd) = if (layoutDirection == LayoutDirection.Rtl) {
                Pair(fadeWidth, 0.0f)
            } else {
                Pair(width - fadeWidth, width)
            }
            LinearGradientShader(
                Offset(fadeStart, 0.0f),
                Offset(fadeEnd, 0.0f),
                listOf(Color(0xFFFFFFFF), Color(0x00FFFFFF))
            )
        } else {
            val fadeEnd = layoutResult.size.height.value.toFloat()
            val fadeStart = fadeEnd - fadeHeight
            LinearGradientShader(
                Offset(0.0f, fadeStart),
                Offset(0.0f, fadeEnd),
                listOf(Color(0xFFFFFFFF), Color(0x00FFFFFF))
            )
        }
    } else {
        null
    }
}

/**
 * If [textDirectionAlgorithm] is null returns a [TextDirectionAlgorithm] based on
 * [layoutDirection].
 */
@VisibleForTesting
internal fun resolveTextDirectionAlgorithm(
    layoutDirection: LayoutDirection,
    textDirectionAlgorithm: TextDirectionAlgorithm?
): TextDirectionAlgorithm {
    return textDirectionAlgorithm
        ?: when (layoutDirection) {
            LayoutDirection.Ltr -> TextDirectionAlgorithm.ContentOrLtr
            LayoutDirection.Rtl -> TextDirectionAlgorithm.ContentOrRtl
        }
}

private fun Float.toIntPx(): IntPx = ceil(this).roundToInt().ipx