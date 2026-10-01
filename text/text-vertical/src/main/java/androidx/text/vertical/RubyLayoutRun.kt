/*
 * Copyright 2025 The Android Open Source Project
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

import android.graphics.Canvas
import android.text.NoCopySpan
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import kotlin.math.max

/**
 * Iterates through each RubySpan within a specified range of a CharSequence.
 *
 * @param text The CharSequence
 * @param start The inclusive starting index
 * @param end The exclusive ending index
 * @param consumer A callback function that is called for each RubySpan transition. It receives
 *   three parameters:
 *     - The inclusive start index of the RubySpan.
 *     - The exclusive end index of the RubySpan.
 *     - The `RubySpan` object itself, or `null` if no RubySpan is found.
 */
internal inline fun forEachRubySpanTransition(
    text: CharSequence,
    start: Int,
    end: Int,
    crossinline consumer: (Int, Int, RubySpan?) -> Unit,
) =
    forEachSpan(text, start, end) { rStart, rEnd, rubySpans ->
        require(rubySpans.size <= 1) { "RubySpan cannot be overlapped" }
        consumer(rStart, rEnd, rubySpans.getOrNull(0))
    }

/**
 * Scales the text size change of a covering [MetricAffectingSpan] by [rubyScale].
 *
 * The wrapper divides the text size by [rubyScale] before it calls [delegate]. If [delegate]
 * changes the text size, the wrapper multiplies the new size by [rubyScale]. Otherwise, it restores
 * the original size to prevent a rounding error. Other [TextPaint] fields, such as `baselineShift`,
 * are not scaled. For example, if [rubyScale] is `0.5f`:
 *
 * - `AbsoluteSizeSpan(40)` sets the ruby text size to `20`.
 * - `RelativeSizeSpan(2f)` makes the ruby text two times larger, the same as the base text.
 */
private class ScaledCoveringCharacterStyle(
    private val delegate: MetricAffectingSpan,
    private val rubyScale: Float,
) : MetricAffectingSpan() {
    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.withUnscaledTextSize { delegate.updateMeasureState(it) }
    }

    override fun updateDrawState(tp: TextPaint) {
        tp.withUnscaledTextSize { delegate.updateDrawState(it) }
    }

    override fun getUnderlying(): MetricAffectingSpan = delegate.underlying

    private inline fun TextPaint.withUnscaledTextSize(crossinline block: (TextPaint) -> Unit) {
        val scaledSize = textSize
        val unscaledSize = if (rubyScale != 0f) scaledSize / rubyScale else scaledSize
        textSize = unscaledSize
        block(this)
        textSize = if (textSize != unscaledSize) textSize * rubyScale else scaledSize
    }
}

/**
 * Returns [rubyText] with [coveringSpans] from the base text attached.
 *
 * The spans of [rubyText] override [coveringSpans] regardless of their [Spanned.SPAN_PRIORITY].
 * This function wraps each [MetricAffectingSpan] in [coveringSpans] with
 * [ScaledCoveringCharacterStyle], except [FontShearSpan].
 *
 * @param rubyText the text of the ruby annotation
 * @param rubyScale the text scale of the ruby text, relative to the base text
 * @param coveringSpans the spans from [getCoveringStyles]
 * @return [rubyText] if [rubyText] or [coveringSpans] is empty, otherwise a new [SpannableString]
 */
private fun buildStyledRubyText(
    rubyText: CharSequence,
    rubyScale: Float,
    coveringSpans: List<CharacterStyle>,
): CharSequence {
    if (rubyText.isEmpty() || coveringSpans.isEmpty()) return rubyText

    // Strip spans with toString() so covering spans are attached before rubyText's own spans.
    return SpannableString(rubyText.toString()).apply {
        for (span in coveringSpans) {
            val rubyStyle =
                when (val underlying = span.underlying) {
                    // forStyleRuns finds FontShearSpan by its type, so do not wrap it.
                    is FontShearSpan -> FontShearSpan(underlying.fontShear)
                    is MetricAffectingSpan -> ScaledCoveringCharacterStyle(underlying, rubyScale)
                    else -> CharacterStyle.wrap(underlying)
                }
            setSpan(
                rubyStyle,
                0,
                length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY,
            )
        }
        // Re-attach the spans from rubyText after the covering spans. getSpans returns the
        // spans with a higher SPAN_PRIORITY first. For equal priorities, it keeps the insertion
        // order. The covering spans use the maximum priority, so the rubyText spans of all
        // priorities override them.
        if (rubyText is Spanned) {
            for (span in rubyText.getSpans(0, rubyText.length, Any::class.java)) {
                if (span is NoCopySpan) continue
                setSpan(
                    span,
                    rubyText.getSpanStart(span),
                    rubyText.getSpanEnd(span),
                    rubyText.getSpanFlags(span),
                )
            }
        }
    }
}

/**
 * A special LayoutRun specialized for a Ruby text.
 *
 * @param text The text this layout represents.
 * @param start The starting inclusive index of the text.
 * @param end The ending exclusive index of the text.
 * @param textOrientation The text orientation mode.
 * @param paint The paint used for text rendering.
 * @param rubySpan The rubySpan attached to the range.
 */
internal class RubyLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    textOrientation: TextOrientation,
    paint: TextPaint,
    rubySpan: RubySpan,
) : LayoutRun(text, start, end) {

    init {
        val rubyText = rubySpan.text
        if (rubyText is Spanned) {
            require(rubyText.getSpans(0, rubyText.length, RubySpan::class.java).isEmpty()) {
                "Ruby text cannot have RubySpan. (Ruby cannot be nested.)"
            }
        }
    }

    override val height: Float
        get() = max(bodyLayoutRuns.height, rubyLayoutRuns.height)

    /**
     * True when the ruby is placed on the right side of the base text, which is the vertical
     * rendering of [AnnotationPosition.Before]. Any unrecognized position falls back to this, which
     * matches [RubySpan.DEFAULT_POSITION].
     */
    private val isRubyOnRight = rubySpan.position != AnnotationPosition.After

    override val leftSideOffset: Float
        get() =
            if (isRubyOnRight) bodyLayoutRuns.leftSide
            else bodyLayoutRuns.leftSide - rubyLayoutRuns.width

    override val rightSideOffset: Float
        get() =
            if (isRubyOnRight) bodyLayoutRuns.rightSide + rubyLayoutRuns.width
            else bodyLayoutRuns.rightSide

    private val rubyScale = rubySpan.textScale
    private val coveringSpans = (text as? Spanned)?.getCoveringStyles(start, end).orEmpty()
    private val styledRubyText: CharSequence =
        buildStyledRubyText(rubySpan.text, rubyScale, coveringSpans)
    private val rubyLayoutRuns: LineLayout =
        paint.withTextScale(rubyScale) {
            createLineLayout(
                styledRubyText,
                0,
                styledRubyText.length,
                this,
                rubySpan.orientation,
            )
        }
    private val bodyLayoutRuns: LineLayout =
        createLineLayout(text, start, end, paint, textOrientation)

    override fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint) {
        val bodyHeight = bodyLayoutRuns.height
        val rubyHeight = rubyLayoutRuns.height

        // Vertical centering the body text and ruby text.
        var bodyY = originY
        var rubyY = originY
        val heightDiffHalf = (bodyHeight - rubyHeight) / 2
        if (heightDiffHalf > 0) {
            // The body text is taller than the ruby text. Push the ruby text for centering.
            rubyY += heightDiffHalf
        } else {
            // The body text is shorter than the ruby text. Push the body text for centering.
            bodyY -= heightDiffHalf
        }

        // `originX` is the baseline of the body text. Butt the ruby box against the body box on
        // whichever side `AnnotationPosition` selects, then convert back to a baseline.
        val rubyX =
            if (isRubyOnRight) originX + bodyLayoutRuns.rightSide - rubyLayoutRuns.leftSide
            else originX + bodyLayoutRuns.leftSide - rubyLayoutRuns.rightSide

        // Fill only the top and bottom centering slivers on the shorter column with the covering
        // background color so per-run backgrounds inside bodyLayoutRuns or rubyLayoutRuns are not
        // overdrawn.
        if (heightDiffHalf != 0f) {
            val baseBgColor = resolveBackgroundColor(paint, coveringSpans)
            if (baseBgColor != 0) {
                val shorterLayout = if (heightDiffHalf > 0f) rubyLayoutRuns else bodyLayoutRuns
                val shorterX = if (heightDiffHalf > 0f) rubyX else originX
                val shorterY = if (heightDiffHalf > 0f) rubyY else bodyY
                val shorterLeft = shorterX + shorterLayout.leftSide
                val shorterWidth = shorterLayout.width
                canvas.drawBackground(
                    shorterLeft,
                    originY,
                    shorterWidth,
                    shorterY - originY,
                    baseBgColor,
                )
                canvas.drawBackground(
                    shorterLeft,
                    shorterY + shorterLayout.height,
                    shorterWidth,
                    (originY + height) - (shorterY + shorterLayout.height),
                    baseBgColor,
                )
            }
        }

        bodyLayoutRuns.draw(canvas, originX, bodyY, paint)
        paint.withTextScale(rubyScale) { rubyLayoutRuns.draw(canvas, rubyX, rubyY, this) }
    }

    override fun getCharAdvances(out: FloatArray, paint: TextPaint) {
        // We don't support line break inside Ruby. Just assigning all height into the first
        // character for preventing line break.
        out[0] = height
        if (out.size > 1) {
            out.fill(0f, 1, out.size)
        }
    }
}
