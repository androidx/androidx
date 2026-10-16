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

package androidx.text.vertical

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.NoCopySpan
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan

/**
 * Clears [TextPaint.bgColor] on plain text runs.
 *
 * Stops the platform from filling plain text from line top to line bottom. Horizontal text layout
 * draws plain text background boxes from font ascent to font descent instead so the fill does not
 * bleed into the [RubySpan] or [EmphasisSpan] annotation band.
 *
 * Implements [NoCopySpan] so inner layouts cloned by [cloneWithoutReplacementSpan] and
 * [getCoveringStyles] background lookups omit this span. The platform also skips
 * non-[MetricAffectingSpan]s on [ReplacementSpan] paints. Attach with a non-zero
 * [Spanned.SPAN_PRIORITY] so inline [android.text.style.BackgroundColorSpan] instances override it.
 */
internal object NoTextBackgroundSpan : CharacterStyle(), NoCopySpan {
    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = 0
    }
}

/**
 * Copies [text] into a [SpannableString] with [NoTextBackgroundSpan] attached.
 *
 * @param text source text to copy
 * @return a [Spanned] copy of [text] with [NoTextBackgroundSpan] attached when non-empty
 */
internal fun prepareHorizontalText(text: CharSequence): Spanned {
    val spannable = SpannableString(text)
    val length = spannable.length
    if (length == 0) return spannable
    spannable.setSpan(
        NoTextBackgroundSpan,
        0,
        length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or (1 shl Spanned.SPAN_PRIORITY_SHIFT),
    )
    return spannable
}

/**
 * Computes plain text background rectangles for runs outside [ReplacementSpan]s across LTR, RTL,
 * and mixed BiDi lines.
 *
 * Each rectangle spans from the font ascent to the font descent of its run so the fill meets
 * adjacent [RubySpan] and [EmphasisSpan] base-text boxes without bleeding into the annotation band.
 *
 * @param layout measured [Layout] of [text]
 * @param text the [Spanned] that [layout] was built from
 * @param paint base [TextPaint] used to build [layout]
 * @return plain text background rectangles in layout coordinates
 */
internal fun plainTextBackgroundRects(
    layout: Layout,
    text: Spanned,
    paint: TextPaint,
): List<RectF> {
    val rects = mutableListOf<RectF>()
    val workPaint = TextPaint()
    val fm = Paint.FontMetricsInt()
    val tmpPath = Path()
    val lineClipPath = Path()
    val tmpRect = RectF()
    for (line in 0..<layout.lineCount) {
        val lineVisibleEnd = layout.getLineVisibleEnd(line)
        val lineEnd = layout.getLineEnd(line)
        val baseline = layout.getLineBaseline(line).toFloat()
        var start = layout.getLineStart(line)
        while (start < lineVisibleEnd) {
            val end =
                text.nextSpanTransition(start, lineVisibleEnd, MetricAffectingSpan::class.java)
            val spans = text.getSpans(start, end, MetricAffectingSpan::class.java)
            val hasReplacementSpan = spans.any {
                it is ReplacementSpan && text.getSpanStart(it) < text.getSpanEnd(it)
            }
            if (!hasReplacementSpan) {
                workPaint.set(paint)
                workPaint.baselineShift = 0
                for (span in spans) {
                    if (text.getSpanStart(span) < text.getSpanEnd(span)) {
                        span.updateMeasureState(workPaint)
                    }
                }
                // RubySpan and EmphasisSpan lay out their base text with includePad = false, so
                // their base text runs from font ascent to font descent (expanded by
                // baselineShift). Match those vertical edges so adjacent background fills meet.
                workPaint.getFontMetricsInt(fm)
                if (workPaint.baselineShift < 0) {
                    fm.ascent += workPaint.baselineShift
                } else {
                    fm.descent += workPaint.baselineShift
                }
                var s = start
                while (s < end) {
                    val isRtl = layout.isRtlCharAt(s)
                    var e = s + 1
                    while (
                        e < end &&
                            layout.isRtlCharAt(e) == isRtl &&
                            layout.getPrimaryHorizontal(e) == layout.getSecondaryHorizontal(e)
                    ) {
                        e++
                    }
                    tmpPath.reset()
                    layout.getSelectionPath(s, e, tmpPath)
                    if (e == lineEnd && line < layout.lineCount - 1) {
                        // On a soft-wrapped non-last line where getLineVisibleEnd(line) ==
                        // getLineEnd(line), getLineForOffset(e) returns line + 1 and
                        // getSelectionPath appends trailing-margin and next-line contours. Clip
                        // vertically to [lineTop, baseline] (strictly above line + 1) and
                        // horizontally to [lineLeft, lineRight] before reading horizontal bounds.
                        lineClipPath.reset()
                        lineClipPath.addRect(
                            layout.getLineLeft(line),
                            layout.getLineTop(line).toFloat(),
                            layout.getLineRight(line),
                            baseline,
                            Path.Direction.CW,
                        )
                        tmpPath.op(lineClipPath, Path.Op.INTERSECT)
                    }
                    tmpPath.computeBounds(tmpRect, false)
                    if (tmpRect.left < tmpRect.right) {
                        rects +=
                            RectF(
                                tmpRect.left,
                                baseline + fm.ascent,
                                tmpRect.right,
                                baseline + fm.descent,
                            )
                    }
                    s = e
                }
            }
            start = end
        }
    }
    return rects
}
