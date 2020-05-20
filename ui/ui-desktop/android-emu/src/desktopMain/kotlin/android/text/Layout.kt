/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.compat.annotation.UnsupportedAppUsage

open class Layout(
    var mText: CharSequence,
    paint: TextPaint,
    width: Int,
    align: Alignment,
    spacingmult: Float,
    spacingadd: Float
) {
    enum class Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        /** @hide */
        @UnsupportedAppUsage
        ALIGN_LEFT,
        /** @hide */
        @UnsupportedAppUsage
        ALIGN_RIGHT,
    }

    class Directions(val mDirections: IntArray)

    class TabStops(val increment: Float, val spans: Array<Any>)

    companion object {
        @JvmStatic
        fun getDesiredWidth(source: CharSequence, start: Int, end: Int, paint: TextPaint): Float {
            return minOf(end - start + 1, source.length) * 1f
        }

        @JvmField
        val DIR_LEFT_TO_RIGHT = 1

        @JvmField
        val DIR_RIGHT_TO_LEFT = -1

        @JvmField
        val RUN_LENGTH_MASK = 0x03ffffff

        @JvmField
        val RUN_LEVEL_SHIFT = 26

        @JvmField
        val RUN_RTL_FLAG = 1 shl RUN_LEVEL_SHIFT

        @JvmField
        val DIRS_ALL_LEFT_TO_RIGHT = Directions(intArrayOf(0, RUN_LENGTH_MASK))

        @JvmField
        val DIRS_ALL_RIGHT_TO_LEFT = Directions(intArrayOf(0, RUN_LENGTH_MASK or RUN_RTL_FLAG))
    }

    fun replaceWith(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        align: Alignment,
        spacingmult: Float,
        spacingadd: Float
    ) {
        println("Layout.replaceWith")
        mText = text
    }

    fun getText(): CharSequence = mText

    open fun getHeight(): Int {
        return 0
    }

    open fun getLineCount(): Int {
        return 1
    }

    open fun getLineTop(line: Int): Int {
        return 0
    }

    open fun getLineBottom(line: Int): Int {
        return 0
    }
    open fun getLineDescent(line: Int): Int {
        return 0
    }

    open fun getLineStart(line: Int): Int {
        return 0
    }

    open fun getLineEnd(line: Int): Int {
        return mText.length
    }

    open fun getParagraphDirection(line: Int): Int {
        return DIR_LEFT_TO_RIGHT
    }

    open fun getLineContainsTab(line: Int): Boolean {
        return false
    }

    open fun getLineMax(line: Int): Float {
        return 0f
    }

    open fun getLineForOffset(offset: Int): Int {
        return 0
    }

    open fun getLineWidth(line: Int): Float {
        return 0f
    }

    open fun getLineDirections(line: Int): Directions {
        return Layout.DIRS_ALL_LEFT_TO_RIGHT
    }

    open fun getTopPadding(): Int {
        return 0
    }

    open fun getBottomPadding(): Int {
        return 0
    }

    open fun getEllipsisCount(line: Int): Int {
        return 0
    }

    open fun getEllipsisStart(line: Int): Int {
        return 0
    }

    open fun getEllipsizedWidth(): Int {
        return 0
    }

    open fun getPrimaryHorizontal(offset: Int): Float {
        return 0f
    }

    open fun draw(c: Canvas) {
        // println("Layout.draw1: $mText")
        draw(c, null, null, 0)
    }

    open fun draw(canvas: Canvas, highlight: Path?, highlightpaint: Paint?, cursorOffset: Int) {
        // println("Layout.draw2")
        drawText(canvas, 0, 1)
    }

    fun drawText(canvas: Canvas, firstLine: Int, lastLine: Int) {
        // println("Layout.drawText: $firstLine .. $lastLine")
        canvas.drawText(mText, 0, mText.length, 0.5f, 0.5f, mWorkPaint)
    }

    fun getLineBaseline(line: Int): Int {
        return getLineTop(line + 1) - getLineDescent(line)
    }

    val mWorkPaint = TextPaint(0)
}