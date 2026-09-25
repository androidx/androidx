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

/**
 * A helper class that handles the layout logic, measurement, and drawing for [RubySpan].
 *
 * @param text the original [Spanned] text
 * @param start the start index of the span
 * @param end the end index of the span
 * @param rubyText the ruby text content
 * @param position where the ruby text sits relative to the base text
 * @param paint the paint used for the initial measurement
 * @param rubyScale the scaling factor for the ruby text
 */
internal class HorizontalRubySpanLayout(
    text: Spanned,
    start: Int,
    end: Int,
    rubyText: CharSequence,
    position: AnnotationPosition,
    paint: Paint,
    private val rubyScale: Float,
) : HorizontalSpanLayout {

    override val spanWidth: Int

    private val bodyLayout: Layout
    private val rubyLayout: Layout
    private val bodyXOffset: Float
    private val rubyXOffset: Float

    init {
        val copiedBodyText = cloneWithoutReplacementSpan(text, start, end)

        // TODO(b/561269843): The StaticLayouts keep a reference to this ThreadLocal TextPaint. If
        // draw() runs on another thread, mutating layout.paint mutates the building thread's cache.
        // Use a thread-local paint to avoid allocation overhead during measurement
        val workPaint = workingPaintCache.getOrSet { TextPaint() }
        workPaint.setFrom(paint)

        // Measure Body Width
        val bodyWidth =
            ceil(Layout.getDesiredWidth(copiedBodyText, 0, copiedBodyText.length, workPaint))
                .toInt()

        // Measure Ruby Width
        val rubyWidth =
            workPaint.withTextScale(rubyScale) {
                ceil(Layout.getDesiredWidth(rubyText, 0, rubyText.length, this)).toInt()
            }

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
        // The scale is dropped after the build. draw() re-applies it.
        rubyLayout =
            workPaint.withTextScale(rubyScale) {
                StaticLayout.Builder.obtain(rubyText, 0, rubyText.length, this, rubyWidth).build()
            }
    }

    private val bodyAscent = bodyLayout.getLineAscent(0)
    private val bodyDescent = bodyLayout.getLineDescent(0)
    private val rubyAscent = rubyLayout.getLineAscent(0)
    private val rubyDescent = rubyLayout.getLineDescent(0)

    /** Height of the ruby line box, i.e. the space the annotation needs on its chosen side. */
    private val rubyLineHeight = rubyDescent - rubyAscent

    /**
     * True when the ruby text is placed over the base text line, which is how
     * [AnnotationPosition.Before] renders in horizontal writing mode. See
     * [`line-over` CSS](https://drafts.csswg.org/css-writing-modes-4/#line-over) for further
     * information. Any unrecognized position falls back to this, matching
     * [RubySpan.DEFAULT_POSITION].
     */
    private val isRubyOver = position != AnnotationPosition.After

    /**
     * Reserves vertical space for the ruby text on the side its position selects.
     *
     * Reserving it on the wrong side would leave the annotation to collide with the adjacent line,
     * so the position has to move the reservation and not just the drawing.
     *
     * @param fm the font metrics to overwrite in place
     */
    override fun fillFontMetrics(fm: Paint.FontMetricsInt) {
        if (isRubyOver) {
            fm.ascent = bodyAscent - rubyLineHeight
            fm.descent = bodyDescent
        } else {
            fm.ascent = bodyAscent
            fm.descent = bodyDescent + rubyLineHeight
        }
        fm.top = fm.ascent
        fm.bottom = fm.descent
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
        canvas.withSave {
            val bodyDrawY = y + bodyAscent
            translate(x + bodyXOffset, bodyDrawY)

            // The paint object stored in the layout is a shared cache, so reset it to the drawing
            // paint before calling draw ops.
            bodyLayout.paint.setFrom(paint)
            // The body text keeps the spans that set baselineShift on paint, for example
            // SuperscriptSpan. Set baselineShift to 0 so that the body text does not move twice.
            bodyLayout.paint.baselineShift = 0
            bodyLayout.draw(this)
        }

        // Draw Ruby Text
        canvas.withSave {
            // `y` is the baseline and StaticLayout draws from the top of its line box, so butt the
            // ruby box against either the top or the bottom of the body's box.
            val rubyDrawY = if (isRubyOver) y + bodyAscent - rubyLineHeight else y + bodyDescent
            translate(x + rubyXOffset, rubyDrawY)

            // The paint object stored in the layout is a shared cache, so reset it to the drawing
            // paint before calling draw ops.
            rubyLayout.paint.setFrom(paint)
            // The body layout paint does not use baselineShift, so the ruby layout paint does not
            // use it either.
            rubyLayout.paint.baselineShift = 0
            rubyLayout.paint.withTextScale(rubyScale) { rubyLayout.draw(this@withSave) }
        }
    }
}
