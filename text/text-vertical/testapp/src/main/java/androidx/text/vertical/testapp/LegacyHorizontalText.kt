/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.NoCopySpan
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.text.vertical.compose.VerticalTextStyle

/**
 * Sets [TextPaint.bgColor] to 0 for the text that it covers.
 *
 * The platform fills the background of plain text from the line top to the line bottom. This span
 * stops that fill. [LegacyHorizontalText] fills each plain text run from the font ascent to the
 * font descent instead, so the background does not go into the ruby or emphasis band. The platform
 * applies only [MetricAffectingSpan]s to the paint of a [ReplacementSpan]. Thus,
 * [androidx.text.vertical.RubySpan] and [androidx.text.vertical.EmphasisSpan] still get the
 * background color from the paint.
 *
 * This span implements [NoCopySpan] so that inner layouts cloned by [ReplacementSpan] helpers do
 * not copy it. Attach this span with a priority above 0 in the [Spanned.SPAN_PRIORITY] bits. The
 * platform then applies it before inline background spans, so these spans can still set the color.
 */
private object NoTextBackgroundSpan : CharacterStyle(), NoCopySpan {
    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = 0
    }
}

/**
 * Returns the background boxes of the text runs that are not in a [ReplacementSpan].
 *
 * Each box goes from the start to the end of the run, and from the font ascent to the font descent
 * of the run. The base text of [androidx.text.vertical.RubySpan] and
 * [androidx.text.vertical.EmphasisSpan] uses the same edges, so the fills meet. This function
 * supports only left-to-right text. [LegacyHorizontalText] uses it to fill the style-level
 * [TextPaint.bgColor].
 *
 * @param layout the layout of [text].
 * @param text the text to find the runs in.
 * @param paint the paint that [layout] uses.
 */
@VisibleForTesting
internal fun plainTextBackgroundRects(
    layout: Layout,
    text: Spanned,
    paint: TextPaint,
): List<RectF> {
    val rects = mutableListOf<RectF>()
    val workPaint = TextPaint()
    val fm = Paint.FontMetricsInt()
    for (line in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineVisibleEnd(line)
        val baseline = layout.getLineBaseline(line).toFloat()
        var start = lineStart
        while (start < lineEnd) {
            val end = text.nextSpanTransition(start, lineEnd, MetricAffectingSpan::class.java)
            val spans = text.getSpans(start, end, MetricAffectingSpan::class.java)
            if (spans.none { it is ReplacementSpan }) {
                workPaint.set(paint)
                spans.forEach { it.updateMeasureState(workPaint) }
                // RubySpan and EmphasisSpan lay out their base text with includePad = false. Thus,
                // their base text goes from the font ascent to the font descent. Use the same edges
                // so that the fills meet.
                workPaint.getFontMetricsInt(fm)
                val left = layout.getPrimaryHorizontal(start)
                val right =
                    if (end < layout.getLineEnd(line)) {
                        layout.getPrimaryHorizontal(end)
                    } else {
                        layout.getLineRight(line)
                    }
                rects += RectF(left, baseline + fm.ascent, right, baseline + fm.descent)
            }
            start = end
        }
    }
    return rects
}

@Composable
fun LegacyHorizontalText(text: Spanned, style: VerticalTextStyle, modifier: Modifier = Modifier) {
    var hTextLayout by remember { mutableStateOf<Layout?>(null) }
    var plainBackgroundRects by remember { mutableStateOf(emptyList<RectF>()) }
    val drawText =
        remember(text) {
            SpannableString(text).apply {
                setSpan(
                    NoTextBackgroundSpan,
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or (1 shl Spanned.SPAN_PRIORITY_SHIFT),
                )
            }
        }
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val paint = remember(density, resolver) { TextPaint() }
    val backgroundPaint = remember { Paint() }
    val typeface =
        remember(resolver, style) {
            resolver
                .resolve(
                    fontFamily = style.fontFamily,
                    fontWeight = style.fontWeight ?: FontWeight.Normal,
                    fontStyle = style.fontStyle ?: FontStyle.Normal,
                    fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
                )
                .value as Typeface
        }
    Layout(
        modifier =
            modifier.drawWithContent {
                drawIntoCanvas { c ->
                    val canvas = c.nativeCanvas
                    if (plainBackgroundRects.isNotEmpty() && style.background.isSpecified) {
                        backgroundPaint.color = style.background.toArgb()
                        plainBackgroundRects.forEach { canvas.drawRect(it, backgroundPaint) }
                    }
                    hTextLayout?.draw(canvas)
                }
            },
        content = {},
    ) { _, constraints ->
        // Set up the paint here and not in composition. This block captures the style, so a
        // change of the style gives a new measure policy, a new measure pass, and a new
        // StaticLayout to draw.
        paint.reset()
        setStyleToPaint(style, typeface, density, paint)
        val bgColor = paint.bgColor
        val layout =
            StaticLayout.Builder.obtain(drawText, 0, drawText.length, paint, constraints.maxWidth)
                .build()
        // The Layout constructor sets paint.bgColor to 0. Set it again, so that the RubySpan and
        // the EmphasisSpan get the background color.
        paint.bgColor = bgColor
        hTextLayout = layout
        plainBackgroundRects =
            if (bgColor != 0) plainTextBackgroundRects(layout, drawText, paint) else emptyList()
        layout(constraints.maxWidth, layout.height) {}
    }
}

private fun setStyleToPaint(
    style: VerticalTextStyle,
    typeface: Typeface,
    density: Density,
    out: TextPaint,
) {
    with(density) {
        out.textSize =
            if (style.fontSize.isSpecified) style.fontSize.toPx() else DefaultFontSize.toPx()
        out.typeface = typeface
        out.fontFeatureSettings = style.fontFeatureSettings
        if (style.color.isSpecified) {
            out.color = style.color.toArgb()
        }
        // The caller reuses the TextPaint, and Paint.reset() does not reset the fields that
        // TextPaint adds, such as bgColor. Assign each TextPaint field that this function sets
        // every time, also when the style does not specify a value.
        out.bgColor =
            if (style.background.isSpecified) {
                style.background.toArgb()
            } else {
                android.graphics.Color.TRANSPARENT
            }
        if (Build.VERSION.SDK_INT >= 25) {
            style.localeList
                ?.map { it.platformLocale }
                ?.toTypedArray()
                ?.let { out.textLocales = android.os.LocaleList(*it) }
        }
    }
}

private val DefaultFontSize = 16.sp
