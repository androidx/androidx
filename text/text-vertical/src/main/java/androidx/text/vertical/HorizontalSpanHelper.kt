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
import android.text.style.ReplacementSpan
import java.lang.ref.WeakReference
import java.util.Objects

/**
 * A thread-local cache for TextPaint to reuse instances during measurement, reducing object
 * allocation.
 */
internal val workingPaintCache = ThreadLocal<TextPaint>()

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
 * Creates a copy of the specified range of the Spanned text, excluding [NoCopySpan] and
 * [ReplacementSpan].
 *
 * Excluding [ReplacementSpan] is crucial to prevent infinite recursion, as this class itself is a
 * [ReplacementSpan] and measuring it would trigger this logic again.
 *
 * @param src The source Spanned text.
 * @param start The start index.
 * @param end The end index.
 * @return A new SpannableString containing the text and relevant spans.
 */
internal fun cloneWithoutReplacementSpan(src: Spanned, start: Int, end: Int): Spanned {
    val textContent = src.subSequence(start, end).toString()
    val spannable = SpannableString(textContent)

    val spans = src.getSpans(start, end, Any::class.java)
    for (span in spans) {
        if (span is NoCopySpan || span is ReplacementSpan) continue

        val spanStart = src.getSpanStart(span).coerceIn(start, end)
        val spanEnd = src.getSpanEnd(span).coerceIn(start, end)
        val spanFlags = src.getSpanFlags(span)

        spannable.setSpan(span, spanStart - start, spanEnd - start, spanFlags)
    }
    return spannable
}
