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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.modulate
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.ceil

public object TextPainter {

    // TODO(b/236964276): Deprecate when TextMeasurer and drawText are no longer Experimental
    /**
     * Paints the text onto the given canvas.
     *
     * @param canvas a canvas to be drawn
     * @param textLayoutResult a result of text layout
     */
    public fun paint(canvas: Canvas, textLayoutResult: TextLayoutResult) {
        val needClipping =
            textLayoutResult.hasVisualOverflow &&
                textLayoutResult.layoutInput.overflow != TextOverflow.Visible
        if (needClipping) {
            val width = textLayoutResult.size.width.toFloat()
            val height = textLayoutResult.size.height.toFloat()
            canvas.save()
            canvas.clipRect(0f, 0f, width, height)
        }

        /* inline resolveSpanStyleDefaults to avoid an allocation in draw */
        val style = textLayoutResult.layoutInput.style.spanStyle
        val textDecoration = style.textDecoration ?: TextDecoration.None
        val shadow = style.shadow ?: Shadow.None
        val drawStyle = style.drawStyle ?: Fill
        try {
            val legacySpanStyle = style.legacySpanStyle
            if (legacySpanStyle != null) {
                val brush = legacySpanStyle.brush
                if (brush != null) {
                    val alpha =
                        if (
                            legacySpanStyle.textForegroundStyle !== TextForegroundStyle.Unspecified
                        ) {
                            legacySpanStyle.textForegroundStyle.alpha
                        } else {
                            1.0f
                        }
                    textLayoutResult.multiParagraph.paint(
                        canvas = canvas,
                        brush = brush,
                        alpha = alpha,
                        shadow = shadow,
                        decoration = textDecoration,
                        drawStyle = drawStyle,
                    )
                } else {
                    val color =
                        if (
                            legacySpanStyle.textForegroundStyle !== TextForegroundStyle.Unspecified
                        ) {
                            legacySpanStyle.textForegroundStyle.color
                        } else {
                            Color.Black
                        }
                    textLayoutResult.multiParagraph.paint(
                        canvas = canvas,
                        color = color,
                        shadow = shadow,
                        decoration = textDecoration,
                        drawStyle = drawStyle,
                    )
                }
                return
            }
            val brush = style.brush
            if (brush != null) {
                // When style.brush is non-null (a ShaderBrush in style.extras), style.alpha reads
                // extras.alpha directly (Float.NaN if unspecified); see SpanStyle.alpha.
                val alpha = style.alpha
                textLayoutResult.multiParagraph.paint(
                    canvas = canvas,
                    brush = brush,
                    alpha = alpha,
                    shadow = shadow,
                    decoration = textDecoration,
                    drawStyle = drawStyle,
                )
            } else {
                // When style.brush is null, style.color decodes style.colorXor (returning
                // Color.Unspecified when 0L, or the alpha-modulated Color if constructed from a
                // SolidColor brush; see SpanStyle.color), so takeOrElse falls back to Color.Black
                // just like the former textForegroundStyle !== Unspecified check.
                val color = style.color.takeOrElse { Color.Black }
                textLayoutResult.multiParagraph.paint(
                    canvas = canvas,
                    color = color,
                    shadow = shadow,
                    decoration = textDecoration,
                    drawStyle = drawStyle,
                )
            }
        } finally {
            if (needClipping) {
                canvas.restore()
            }
        }
    }
}

/**
 * Draw styled text using a TextMeasurer.
 *
 * This draw function supports multi-styling and async font loading.
 *
 * TextMeasurer carries an internal cache to optimize text layout measurement for repeated calls in
 * draw phase. If layout affecting attributes like font size, font weight, overflow, softWrap, etc.
 * are changed in consecutive calls to this method, TextMeasurer and its internal cache that holds
 * layout results may not offer any benefits. Check out [TextMeasurer] and drawText overloads that
 * take [TextLayoutResult] to learn more about text layout and draw phase optimizations.
 *
 * @param textMeasurer Measures and lays out the text
 * @param text Text to be drawn
 * @param topLeft Offsets the text from top left point of the current coordinate system.
 * @param style the [TextStyle] to be applied to the text
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether to wrap text at soft line breaks. When true, wraps text to the next line
 *   when it exceeds layout bounds. When false, positions text as if there was unlimited horizontal
 *   space, which may cause it to clip or overflow according to [overflow].
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. If it is not null, then it must be greater than zero.
 * @param placeholders a list of [Placeholder]s that specify ranges of text which will be skipped
 *   during layout and replaced with [Placeholder]. It's required that the range of each
 *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is thrown.
 * @param size how wide and tall the text should be. If left [Size.Unspecified] as its default
 *   value, text will be forced to fit inside the total drawing area from where it's placed. If size
 *   is specified, [Size.width] will define the width of the text. [Size.height] helps defining the
 *   number of lines that fit if [softWrap] is enabled and [overflow] is [TextOverflow.Ellipsis].
 *   Otherwise, [Size.height] either defines where the text is clipped ([TextOverflow.Clip]) or
 *   becomes no-op.
 * @param blendMode Blending algorithm to be applied to the text
 * @sample androidx.compose.ui.text.samples.DrawTextAnnotatedStringSample
 */
public fun DrawScope.drawText(
    textMeasurer: TextMeasurer,
    text: AnnotatedString,
    topLeft: Offset = Offset.Zero,
    style: TextStyle = TextStyle.Default,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    size: Size = Size.Unspecified,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    val textLayoutResult =
        textMeasurer.measure(
            text = text,
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            placeholders = placeholders,
            constraints = textLayoutConstraints(size, topLeft),
            layoutDirection = layoutDirection,
            density = this,
        )

    withTransform({
        translate(topLeft.x, topLeft.y)
        clip(textLayoutResult)
    }) {
        drawAnnotationsBehind(text, textLayoutResult)
        paint(textLayoutResult, blendMode = blendMode)
        drawAnnotationsOver(text, textLayoutResult)
    }
}

/**
 * Draw text using a TextMeasurer.
 *
 * This draw function supports only one text style, and async font loading.
 *
 * TextMeasurer carries an internal cache to optimize text layout measurement for repeated calls in
 * draw phase. If layout affecting attributes like font size, font weight, overflow, softWrap, etc.
 * are changed in consecutive calls to this method, TextMeasurer and its internal cache that holds
 * layout results may not offer any benefits. Check out [TextMeasurer] and drawText overloads that
 * take [TextLayoutResult] to learn more about text layout and draw phase optimizations.
 *
 * @param textMeasurer Measures and lays out the text
 * @param text Text to be drawn
 * @param topLeft Offsets the text from top left point of the current coordinate system.
 * @param style the [TextStyle] to be applied to the text
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether to wrap text at soft line breaks. When true, wraps text to the next line
 *   when it exceeds layout bounds. When false, positions text as if there was unlimited horizontal
 *   space, which may cause it to clip or overflow according to [overflow].
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. If it is not null, then it must be greater than zero.
 * @param size how wide and tall the text should be. If left [Size.Unspecified] as its default
 *   value, text will be forced to fit inside the total drawing area from where it's placed. If size
 *   is specified, [Size.width] will define the width of the text. [Size.height] helps defining the
 *   number of lines that fit if [softWrap] is enabled and [overflow] is [TextOverflow.Ellipsis].
 *   Otherwise, [Size.height] either defines where the text is clipped ([TextOverflow.Clip]) or
 *   becomes no-op.
 * @param blendMode Blending algorithm to be applied to the text
 * @sample androidx.compose.ui.text.samples.DrawTextSample
 * @sample androidx.compose.ui.text.samples.DrawTextStyledSample
 */
public fun DrawScope.drawText(
    textMeasurer: TextMeasurer,
    text: String,
    topLeft: Offset = Offset.Zero,
    style: TextStyle = TextStyle.Default,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    size: Size = Size.Unspecified,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    val textLayoutResult =
        textMeasurer.measure(
            text = AnnotatedString(text),
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            constraints = textLayoutConstraints(size, topLeft),
            layoutDirection = layoutDirection,
            density = this,
        )

    withTransform({
        translate(topLeft.x, topLeft.y)
        clip(textLayoutResult)
    }) {
        paint(textLayoutResult, blendMode = blendMode)
    }
}

/**
 * Draw an existing text layout as produced by [TextMeasurer].
 *
 * This draw function cannot relayout when async font loading resolves. If using async fonts or
 * other dynamic text layout, you are responsible for invalidating layout on changes.
 *
 * @param textLayoutResult Text Layout to be drawn
 * @param color Text color to use
 * @param topLeft Offsets the text from top left point of the current coordinate system.
 * @param alpha opacity to be applied to the [color] from 0.0f to 1.0f representing fully
 *   transparent to fully opaque respectively
 * @param shadow The shadow effect applied on the text.
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * @param drawStyle Whether the text is stroked or filled in.
 * @param blendMode Blending algorithm to be applied to the text
 * @sample androidx.compose.ui.text.samples.DrawTextMeasureInLayoutSample
 * @sample androidx.compose.ui.text.samples.DrawTextDrawWithCacheSample
 */
public fun DrawScope.drawText(
    textLayoutResult: TextLayoutResult,
    color: Color = Color.Unspecified,
    topLeft: Offset = Offset.Zero,
    alpha: Float = Float.NaN,
    shadow: Shadow? = null,
    textDecoration: TextDecoration? = null,
    drawStyle: DrawStyle? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    withTransform({
        translate(topLeft.x, topLeft.y)
        clip(textLayoutResult)
    }) {
        drawAnnotationsBehind(textLayoutResult.layoutInput.text, textLayoutResult)
        paint(
            textLayoutResult = textLayoutResult,
            color = color,
            alpha = alpha,
            shadow = shadow,
            textDecoration = textDecoration,
            drawStyle = drawStyle,
            blendMode = blendMode,
        )
        drawAnnotationsOver(textLayoutResult.layoutInput.text, textLayoutResult)
    }
}

/**
 * Draw an existing text layout as produced by [TextMeasurer].
 *
 * This draw function cannot relayout when async font loading resolves. If using async fonts or
 * other dynamic text layout, you are responsible for invalidating layout on changes.
 *
 * @param textLayoutResult Text Layout to be drawn
 * @param brush The brush to use when drawing the text.
 * @param topLeft Offsets the text from top left point of the current coordinate system.
 * @param alpha Opacity to be applied to [brush] from 0.0f to 1.0f representing fully transparent to
 *   fully opaque respectively.
 * @param shadow The shadow effect applied on the text.
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * @param drawStyle Whether the text is stroked or filled in.
 * @param blendMode Blending algorithm to be applied to the text
 */
public fun DrawScope.drawText(
    textLayoutResult: TextLayoutResult,
    brush: Brush,
    topLeft: Offset = Offset.Zero,
    alpha: Float = Float.NaN,
    shadow: Shadow? = null,
    textDecoration: TextDecoration? = null,
    drawStyle: DrawStyle? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    withTransform({
        translate(topLeft.x, topLeft.y)
        clip(textLayoutResult)
    }) {
        drawAnnotationsBehind(textLayoutResult.layoutInput.text, textLayoutResult)
        paint(
            textLayoutResult = textLayoutResult,
            alpha = alpha,
            shadow = shadow,
            textDecoration = textDecoration,
            drawStyle = drawStyle,
            blendMode = blendMode,
            brush = brush,
        )
        drawAnnotationsOver(textLayoutResult.layoutInput.text, textLayoutResult)
    }
}

private fun DrawScope.paint(
    textLayoutResult: TextLayoutResult,
    color: Color = Color.Unspecified,
    alpha: Float = Float.NaN,
    shadow: Shadow? = null,
    textDecoration: TextDecoration? = null,
    drawStyle: DrawStyle? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    brush: Brush? = null,
) {
    val spanStyle = textLayoutResult.layoutInput.style.spanStyle
    val newShadow = shadow ?: spanStyle.shadow ?: Shadow.None
    val newTextDecoration = textDecoration ?: spanStyle.textDecoration ?: TextDecoration.None
    val newDrawStyle = drawStyle ?: spanStyle.drawStyle ?: Fill

    // if [brush] is unspecified, but text layout was created using brush, and [color] is
    // unspecified, we should treat this like drawText(brush) call
    val newBrush = brush ?: if (color.isUnspecified) spanStyle.brush else null
    if (newBrush != null) {
        val newAlpha =
            when {
                !alpha.isNaN() -> alpha
                !spanStyle.alpha.isNaN() -> spanStyle.alpha
                else -> 1.0f
            }
        textLayoutResult.multiParagraph.paint(
            canvas = drawContext.canvas,
            brush = newBrush,
            alpha = newAlpha,
            shadow = newShadow,
            decoration = newTextDecoration,
            drawStyle = newDrawStyle,
            blendMode = blendMode,
        )
    } else {
        textLayoutResult.multiParagraph.paint(
            canvas = drawContext.canvas,
            color = color.takeOrElse { spanStyle.color }.takeOrElse { Color.Black }.modulate(alpha),
            shadow = newShadow,
            decoration = newTextDecoration,
            drawStyle = newDrawStyle,
            blendMode = blendMode,
        )
    }
}

private fun DrawTransform.clip(textLayoutResult: TextLayoutResult) {
    if (
        textLayoutResult.hasVisualOverflow &&
            textLayoutResult.layoutInput.overflow != TextOverflow.Visible
    ) {
        clipRect(
            left = 0f,
            top = 0f,
            right = textLayoutResult.size.width.toFloat(),
            bottom = textLayoutResult.size.height.toFloat(),
        )
    }
}

/** Converts given size and placement preferences to Constraints for measuring text layout. */
private fun DrawScope.textLayoutConstraints(size: Size, topLeft: Offset): Constraints {
    val minWidth: Int
    val maxWidth: Int
    val isWidthNaN = size.isUnspecified || size.width.isNaN()
    if (isWidthNaN) {
        minWidth = 0
        maxWidth = ceil(this.size.width - topLeft.x).fastRoundToInt().coerceAtLeast(minWidth)
    } else {
        val fixedWidth = ceil(size.width).fastRoundToInt()
        minWidth = fixedWidth
        maxWidth = fixedWidth
    }

    val minHeight: Int
    val maxHeight: Int
    val isHeightNaN = size.isUnspecified || size.height.isNaN()
    if (isHeightNaN) {
        minHeight = 0
        maxHeight = ceil(this.size.height - topLeft.y).fastRoundToInt().coerceAtLeast(minHeight)
    } else {
        val fixedHeight = ceil(size.height).fastRoundToInt()
        minHeight = fixedHeight
        maxHeight = fixedHeight
    }

    return Constraints(minWidth, maxWidth, minHeight, maxHeight)
}
