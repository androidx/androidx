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

/**
 * A helper class responsible for measuring and drawing text with horizontal emphasis marks.
 *
 * This class handles the layout of the body text and the positioning of emphasis marks (e.g., dots)
 * on the side that [position] selects. It calculates the necessary font metrics adjustments to
 * accommodate the emphasis marks and draws them at the correct positions.
 *
 * @param text The source text containing the emphasis span.
 * @param start The start index of the emphasis span in the source text.
 * @param end The end index of the emphasis span in the source text.
 * @param emphasis The string to be used as the emphasis mark (e.g., "•").
 * @param position Where the emphasis mark sits relative to the base text.
 * @param paint The paint used for measuring and drawing the text.
 * @param relSize The relative size of the emphasis mark compared to the body text size.
 */
internal class HorizontalEmphasisSpanLayout(
    text: Spanned,
    start: Int,
    end: Int,
    private val emphasis: String,
    position: AnnotationPosition,
    paint: Paint,
    private val relSize: Float,
) : HorizontalSpanLayout {

    override val spanWidth: Int

    private val bodyLayout: Layout
    private val emphasisWidth: Int
    private val emphasisAscent: Int
    private val emphasisDescent: Int
    private val positions: FloatArray

    init {
        val copiedBodyText = cloneWithoutReplacementSpan(text, start, end)

        // TODO(b/561269843): The StaticLayout keeps a reference to this ThreadLocal TextPaint. If
        // draw() runs on another thread, mutating layout.paint mutates the building thread's cache.
        // Use a thread-local paint to avoid allocation overhead during measurement
        val workPaint = workingPaintCache.getOrSet { TextPaint() }
        workPaint.set(paint)

        // Measure Body Width
        spanWidth =
            ceil(Layout.getDesiredWidth(copiedBodyText, 0, copiedBodyText.length, workPaint))
                .toInt()

        // Create Body Layout
        bodyLayout =
            StaticLayout.Builder.obtain(
                    copiedBodyText,
                    0,
                    copiedBodyText.length,
                    workPaint,
                    spanWidth,
                )
                .build()

        // Create Emphasis Layout
        val emLayout =
            workPaint.withTextScale(relSize) {
                val width = ceil(Layout.getDesiredWidth(emphasis, 0, emphasis.length, this)).toInt()
                StaticLayout.Builder.obtain(emphasis, 0, emphasis.length, this, width).build()
            }
        emphasisWidth = emLayout.width
        emphasisAscent = emLayout.getLineAscent(0)
        emphasisDescent = emLayout.getLineDescent(0)

        // Calculate drawing position of emphasis letter.
        positions = FloatArray(copiedBodyText.length) { Float.NaN }
        copiedBodyText.forStyleRuns(0, end - start, workPaint) { ss, se, paint, _, _, _ ->
            copiedBodyText.forEachGrapheme(ss, se, paint.textLocale) { gs, ge ->
                if (isEmphasisTarget(Character.codePointAt(copiedBodyText, gs))) {
                    val width = paint.measureText(copiedBodyText, gs, ge)
                    val pos = bodyLayout.getPrimaryHorizontal(gs)
                    positions[gs] = pos + (width - emphasisWidth) / 2
                }
            }
        }
    }

    private val bodyAscent = bodyLayout.getLineAscent(0)
    private val bodyDescent = bodyLayout.getLineDescent(0)

    /** Height of the emphasis line box, i.e. the space the mark needs on its chosen side. */
    private val markLineHeight = emphasisDescent - emphasisAscent

    /**
     * True when the emphasis mark is placed over the base text line, which is how
     * [AnnotationPosition.Before] renders in horizontal writing mode. See
     * [`line-over` CSS](https://drafts.csswg.org/css-writing-modes-4/#line-over) for further
     * information. Any unrecognized position falls back to this, matching
     * [EmphasisSpan.DEFAULT_POSITION].
     */
    private val isMarkOver = position != AnnotationPosition.After

    /**
     * Reserves vertical space for the emphasis mark on the side its position selects.
     *
     * Reserving it on the wrong side would leave the mark to collide with the adjacent line, so the
     * position has to move the reservation and not just the drawing.
     *
     * @param fm the font metrics to overwrite in place
     */
    override fun fillFontMetrics(fm: Paint.FontMetricsInt) {
        if (isMarkOver) {
            fm.ascent = bodyAscent - markLineHeight
            fm.descent = bodyDescent
        } else {
            fm.ascent = bodyAscent
            fm.descent = bodyDescent + markLineHeight
        }
        fm.top = fm.ascent
        fm.bottom = fm.descent
    }

    /**
     * Draws the pre-calculated body layout and emphasis marks onto the canvas.
     *
     * @param canvas The target canvas.
     * @param x The horizontal start position.
     * @param y The baseline vertical position.
     * @param paint The paint from the draw call, used to update the layout paint and draw marks.
     */
    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        // Draw Body Text
        canvas.withSave {
            val bodyDrawY = y + bodyAscent
            translate(x, bodyDrawY)

            // The paint object stored in the layout is a shared cache, so reset it to the drawing
            // paint before calling draw ops.
            bodyLayout.paint.set(paint)
            bodyLayout.draw(this)
        }

        // Draw Emphasis Text
        // Canvas.drawText takes the glyph baseline. StaticLayout.draw takes the top of the line
        // box. Subtract the mark descent for Before, or the mark ascent for After. This makes the
        // mark line box touch the body line box.
        val emphasisDrawY =
            if (isMarkOver) y + bodyAscent - emphasisDescent else y + bodyDescent - emphasisAscent
        paint.withTextScale(relSize) {
            positions.forEach { pos ->
                if (pos.isNaN()) return@forEach
                canvas.drawText(emphasis, x + pos, emphasisDrawY, this)
            }
        }
    }
}
