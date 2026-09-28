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

import android.graphics.Canvas
import android.graphics.Paint
import android.text.NoCopySpan
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import java.lang.ref.WeakReference
import java.util.Objects

/**
 * A thread-local cache for TextPaint to reuse instances during measurement, reducing object
 * allocation.
 */
internal val workingPaintCache = ThreadLocal<TextPaint>()

private val defaultTextPaint = TextPaint()

/**
 * Copies all fields from [src] into this [TextPaint].
 *
 * Because [ReplacementSpan] callbacks receive a parameter with static type [Paint], a direct
 * `set(paint)` call resolves to [Paint.set] and skips [TextPaint] fields (`bgColor`, `linkColor`,
 * `baselineShift`, `drawableState`, `density`, `underlineColor`, and `underlineThickness`). When
 * [src] is a plain [Paint], this resets the [TextPaint] fields from [defaultTextPaint] before
 * copying [src]. Do not replace this copy with direct field assignments: `underlineColor` and
 * `underlineThickness` are public only on API 29 and higher, and this copy clears them on all API
 * levels.
 */
internal fun TextPaint.setFrom(src: Paint) {
    if (src is TextPaint) {
        set(src)
    } else {
        set(defaultTextPaint)
        set(src)
    }
}

/**
 * Returns the [CharacterStyle] spans in this text that cover all of `[start, end)`, in the order
 * that [Spanned.getSpans] returns them.
 *
 * The result does not include [NoCopySpan] spans or [ReplacementSpan] spans, such as [RubySpan] and
 * [EmphasisSpan].
 */
internal fun Spanned.getCoveringStyles(start: Int, end: Int): List<CharacterStyle> =
    getSpans(start, end, CharacterStyle::class.java).filter { span ->
        val underlying = span.underlying
        getSpanStart(span) <= start &&
            getSpanEnd(span) >= end &&
            underlying !is NoCopySpan &&
            underlying !is ReplacementSpan
    }

/**
 * Returns the background color of a [ReplacementSpan].
 *
 * The platform applies only the covering [MetricAffectingSpan]s to the paint that it passes to
 * [ReplacementSpan.draw]. It does not apply other [CharacterStyle]s, such as
 * [android.text.style.BackgroundColorSpan]. This function applies all of [coveringStyles] in order,
 * so the last style that sets [TextPaint.bgColor] gives the color, the same as in plain text.
 *
 * @param paint the paint that the platform passes to [ReplacementSpan.draw]
 * @param coveringStyles the styles that [getCoveringStyles] returns for the span
 */
@ColorInt
internal fun resolveBackgroundColor(paint: Paint, coveringStyles: List<CharacterStyle>): Int {
    var bgColor = (paint as? TextPaint)?.bgColor ?: 0
    if (coveringStyles.isEmpty()) return bgColor
    tempPaint { workPaint ->
        // Only workPaint.bgColor is read after updateDrawState.
        workPaint.bgColor = bgColor
        for (style in coveringStyles) {
            style.updateDrawState(workPaint)
        }
        bgColor = workPaint.bgColor
    }
    return bgColor
}

/**
 * Fills a box with [bgColor].
 *
 * The platform does not fill the background of a [ReplacementSpan], so each span fills the box that
 * [HorizontalSpanLayout.fillFontMetrics] reports with this function. This function does nothing if
 * [bgColor] is 0 or if the box is empty.
 */
internal fun Canvas.drawSpanBackground(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    @ColorInt bgColor: Int,
) {
    // Canvas.drawRect sorts the edges of a box, so it also fills a box with inverted edges.
    if (bgColor == 0 || left >= right || top >= bottom) return
    tempPaint { bgPaint ->
        // The pool can return a paint that has the state of an earlier user, for example
        // Paint.Style.STROKE. Reset the paint, so that it fills the box.
        bgPaint.reset()
        bgPaint.color = bgColor
        drawRect(left, top, right, bottom, bgPaint)
    }
}

/** Interface for managing the layout and rendering of horizontal text spans. */
internal interface HorizontalSpanLayout {
    /**
     * Populates the provided [Paint.FontMetricsInt] with the metrics of this span.
     *
     * @param fm The font metrics object to fill.
     */
    fun fillFontMetrics(fm: Paint.FontMetricsInt)

    /**
     * Draws the span onto the given [Canvas].
     *
     * @param canvas The canvas to draw on.
     * @param x The x-coordinate for the drawing.
     * @param y The y-coordinate for the drawing.
     * @param paint The paint to use for drawing.
     */
    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint)

    /** The measured width of the span. */
    val spanWidth: Int
}

/**
 * A key used to cache layout calculations.
 *
 * It uses a WeakReference for the text to prevent memory leaks if the CharSequence holds a
 * reference to a Context.
 */
// TODO(b/561269847): LayoutKey must include Paint characteristics and span-specific properties
// like Position, otherwise layouts are erroneously reused across different text sizes/positions.
internal class LayoutKey(private val start: Int, private val end: Int, text: CharSequence) {
    private val textRef = WeakReference(text)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayoutKey) return false

        if (start != other.start) return false
        if (end != other.end) return false
        if (textRef.get() != other.textRef.get()) return false

        return true
    }

    override fun hashCode(): Int = Objects.hash(start, end, textRef.get())
}

internal class HorizontalSpanImpl(
    private val key: (paint: Paint, bodyText: Spanned, start: Int, end: Int) -> LayoutKey,
    private val build:
        (paint: Paint, bodyText: Spanned, start: Int, end: Int) -> HorizontalSpanLayout,
) {
    private var lastKey: LayoutKey? = null
    private var lastLayout: HorizontalSpanLayout? = null

    private fun getLayout(
        paint: Paint,
        bodyText: Spanned,
        start: Int,
        end: Int,
    ): HorizontalSpanLayout {
        val key = key(paint, bodyText, start, end)
        // Return cached layout if available and valid
        if (lastKey == key && lastLayout != null) {
            return lastLayout!!
        }

        // Create a new layout object which handles the measurement logic
        return build(paint, bodyText, start, end).also {
            lastLayout = it
            lastKey = key
        }
    }

    fun getSize(
        paint: Paint,
        bodyText: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (bodyText == null) return 0
        require(bodyText is Spanned) { "Text must be Spanned" }

        val layout = getLayout(paint, bodyText, start, end)

        if (fm != null) {
            layout.fillFontMetrics(fm)
        }
        return layout.spanWidth
    }

    fun draw(
        canvas: Canvas,
        bodyText: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        if (bodyText == null) return
        require(bodyText is Spanned) { "Text must be Spanned" }

        val layout = getLayout(paint, bodyText, start, end)
        layout.draw(canvas, x, y.toFloat(), paint)
    }
}

/**
 * Clears [TextPaint.bgColor] in the body layout.
 *
 * [drawSpanBackground] fills the covering background color across the span box.
 * [cloneWithoutReplacementSpan] puts this span right after the last covering span that sets
 * `bgColor`, so the body layout does not fill that color again. A partial span that comes after
 * this span still fills its characters.
 */
private object NoBgColorSpan : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = 0
    }
}

/**
 * Returns the last style in this list that sets [TextPaint.bgColor] to a color other than 0, or
 * `null` if no style sets it.
 */
private fun List<CharacterStyle>.findLastBgColorStyle(): CharacterStyle? {
    if (isEmpty()) return null
    var lastStyle: CharacterStyle? = null
    tempPaint { probe ->
        for (style in this) {
            probe.bgColor = 0
            style.updateDrawState(probe)
            if (probe.bgColor != 0) lastStyle = style
        }
    }
    return lastStyle
}

/**
 * Creates a copy of the specified range of the Spanned text, excluding [NoCopySpan] and
 * [ReplacementSpan].
 *
 * Excluding [ReplacementSpan] is crucial to prevent infinite recursion, as this class itself is a
 * [ReplacementSpan] and measuring it would trigger this logic again.
 *
 * The copy keeps the order of the spans. It also gets [NoBgColorSpan] right after the last covering
 * span that sets [TextPaint.bgColor].
 *
 * @param src The source Spanned text.
 * @param start The start index.
 * @param end The end index.
 * @return A new SpannableString containing the text and relevant spans.
 */
internal fun cloneWithoutReplacementSpan(src: Spanned, start: Int, end: Int): Spanned {
    val textContent = src.subSequence(start, end).toString()
    val spannable = SpannableString(textContent)
    val lastBgColorStyle = src.getCoveringStyles(start, end).findLastBgColorStyle()

    val spans = src.getSpans(start, end, Any::class.java)
    for (span in spans) {
        if (span is NoCopySpan || span is ReplacementSpan) continue

        val spanStart = src.getSpanStart(span).coerceIn(start, end)
        val spanEnd = src.getSpanEnd(span).coerceIn(start, end)
        val spanFlags = src.getSpanFlags(span)

        spannable.setSpan(span, spanStart - start, spanEnd - start, spanFlags)
        if (span === lastBgColorStyle) {
            // Spanned.getSpans sorts the spans by priority. Use the same priority, so that
            // NoBgColorSpan stays right after this span.
            spannable.setSpan(
                NoBgColorSpan,
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or (spanFlags and Spanned.SPAN_PRIORITY),
            )
        }
    }
    return spannable
}
