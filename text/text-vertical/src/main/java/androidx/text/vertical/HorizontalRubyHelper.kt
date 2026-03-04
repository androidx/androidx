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
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.concurrent.getOrSet
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A helper class that handles the layout logic, measurement, and drawing for [RubySpanCompat].
 *
 * @param text The original Spanned text.
 * @param start The start index of the span.
 * @param end The end index of the span.
 * @param rubyText The ruby text content.
 * @param paint The paint used for the initial measurement.
 * @param relSize The scaling factor for the ruby text.
 */
internal class HorizontalRubySpanLayout(
    text: Spanned,
    start: Int,
    end: Int,
    rubyText: CharSequence,
    paint: Paint,
    private val relSize: Float,
) : HorizontalSpanLayout {
    private val bodyLayout: Layout
    private val rubyLayout: Layout
    private val bodyXOffset: Float
    private val rubyXOffset: Float
    override val spanWidth: Int

    init {
        val copiedBodyText = cloneWithoutReplacementSpan(text, start, end)

        // Use a thread-local paint to avoid allocation overhead during measurement
        val workPaint = workingPaintCache.getOrSet { TextPaint() }
        workPaint.set(paint)

        // Measure Body Width
        val bodyWidth =
            ceil(Layout.getDesiredWidth(copiedBodyText, 0, copiedBodyText.length, workPaint))
                .toInt()

        // Measure Ruby Width
        workPaint.textSize *= relSize
        val rubyWidth =
            ceil(Layout.getDesiredWidth(rubyText, 0, rubyText.length, workPaint)).toInt()
        workPaint.textSize /= relSize // Restore size

        // Calculate total width and offsets to center content
        spanWidth = max(bodyWidth, rubyWidth)
        bodyXOffset = (spanWidth - bodyWidth) / 2f
        rubyXOffset = (spanWidth - rubyWidth) / 2f

        // Create Body Layout
        bodyLayout =
            StaticLayout.Builder.obtain(
                    copiedBodyText,
                    0,
                    copiedBodyText.length,
                    workPaint,
                    bodyWidth,
                )
                .build()

        // Create Ruby Layout
        workPaint.textSize *= relSize
        rubyLayout =
            StaticLayout.Builder.obtain(rubyText, 0, rubyText.length, workPaint, rubyWidth).build()
    }

    private val bodyAscent = bodyLayout.getLineAscent(0)
    private val bodyDescent = bodyLayout.getLineDescent(0)
    private val rubyAscent = rubyLayout.getLineAscent(0)
    private val rubyDescent = rubyLayout.getLineDescent(0)

    /**
     * Updates the provided FontMetrics to ensure there is enough vertical space for the ruby text
     * above the body text.
     *
     * @param fm The FontMetrics object to update.
     */
    override fun fillFontMetrics(fm: Paint.FontMetricsInt) {
        // Calculate the effective ascent required to fit the ruby text
        fm.ascent = bodyAscent - rubyDescent + rubyAscent
        fm.descent = bodyDescent
        fm.top = min(fm.ascent, fm.top)
        fm.bottom = max(fm.descent, fm.bottom)
    }

    /**
     * Draws the pre-calculated body and ruby layouts onto the canvas.
     *
     * @param canvas The target canvas.
     * @param x The horizontal start position.
     * @param y The baseline vertical position.
     * @param paint The paint from the draw call, used to update the layout paints.
     */
    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        // Draw Body Text
        canvas.save()
        try {
            val bodyDrawY = y + bodyAscent
            canvas.translate(x + bodyXOffset, bodyDrawY)

            // The paint object stored in the layout is a shared cache, so reset it to the drawing
            // paint before calling draw ops.
            bodyLayout.paint.set(paint)
            bodyLayout.draw(canvas)
        } finally {
            canvas.restore()
        }

        // Draw Ruby Text
        canvas.save()
        try {
            val rubyDrawY = y + bodyAscent + rubyAscent - rubyDescent
            canvas.translate(x + rubyXOffset, rubyDrawY)

            // The paint object stored in the layout is a shared cache, so reset it to the drawing
            // paint before calling draw ops.
            rubyLayout.paint.set(paint)
            rubyLayout.paint.textSize *= relSize
            rubyLayout.draw(canvas)
        } finally {
            canvas.restore()
        }
    }
}
