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
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a LineLayout.
 *
 * @param text The text to be laid out.
 * @param start The inclusive starting index.
 * @param end The exclusive ending index.
 * @param paint The TextPaint object used to measure and draw the text.
 * @param textOrientation The orientation mode.
 */
internal fun createLineLayout(
    text: CharSequence,
    start: Int,
    end: Int,
    paint: TextPaint,
    @OrientationMode textOrientation: Int,
) =
    LineLayout(
        mutableListOf<LayoutRun>().apply {
            forEachOrientation(text, start, end, textOrientation) { runStart, runEnd, orientation ->
                add(createLayoutRun(text, runStart, runEnd, paint, orientation))
            }
        }
    )

/**
 * Represents a layout of multiple [LayoutRun]s arranged in a single line.
 *
 * @param runs The list of [LayoutRun]s composing this layout.
 */
internal class LineLayout(val runs: List<LayoutRun>) {
    val start: Int
        get() = runs.first().start

    val end: Int
        get() = runs.last().end

    /**
     * Distance from left most position from the baseline in pixels.
     *
     * This is usually negative value.
     *
     * To get the next drawing horizontal coordinate, add this amount to the baseline. To get the
     * width of this run, subtract this amount from the [rightSide] value, i.e. width = right -
     * left.
     */
    val leftSide: Float

    /**
     * Distance from right most position from the baseline in pixels.
     *
     * This is usually positive value.
     *
     * To get baseline of this run, subtract this amount from the drawing offset. To get the width
     * of this run, subtract [leftSide] from this amount, i.e. width = right - left.
     */
    val rightSide: Float

    /**
     * Distance from the top to bottom in pixels.
     *
     * This is always positive value.
     */
    val height: Float

    /**
     * Distance from the right to left in pixels.
     *
     * This is always positive value.
     */
    val width: Float
        get() = rightSide - leftSide

    init {
        val (l, r, h) =
            runs.fold(Triple(0f, 0f, 0f)) { acc, run ->
                Triple(
                    min(acc.first, run.leftSideOffset), // leftSide
                    max(acc.second, run.rightSideOffset), // rightSide
                    acc.third + run.height, // height
                )
            }
        leftSide = l
        rightSide = r
        height = h
    }

    /**
     * Draws the laid out text on the canvas as a single line.
     *
     * @param canvas The canvas to draw on.
     * @param originX The x-coordinate of the drawing origin.
     * @param originY The y-coordinate of the drawing origin.
     * @param paint The paint used for text rendering.
     */
    fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint) {
        var y = originY
        runs.forEach { run ->
            run.draw(canvas, originX, y, paint)
            y += run.height
        }
    }
}
