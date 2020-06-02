/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Rect
import org.jetbrains.skija.RoundedRect

public class Canvas(val skijaCanvas: org.jetbrains.skija.Canvas?) {

    constructor() : this(null)

    var skijaFont = org.jetbrains.skija.Font(Typeface.DEFAULT.skijaTypeface, 30f)

    fun translate(x: Float, y: Float) {
        skijaCanvas!!.translate(x, y)
    }

    fun rotate(degrees: Float) {
        skijaCanvas!!.rotate(degrees)
    }

    fun drawLine(
        startX: Float,
        startY: Float,
        stopX: Float,
        stopY: Float,
        paint: Paint
    ) {
        skijaCanvas!!.drawLine(
            startX,
            startY,
            stopX,
            stopY,
            paint.skijaPaint
        )
    }

    fun drawRect(rect: android.graphics.RectF, paint: android.graphics.Paint) {
        val skijaRect = Rect.makeLTRB(rect.left, rect.top, rect.right, rect.bottom)
        skijaCanvas!!.drawRect(skijaRect, paint.skijaPaint)
    }

    fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: android.graphics.Paint
    ) {
        val skijaRect = Rect.makeLTRB(left, top, right, bottom)
        skijaCanvas!!.drawRect(skijaRect, paint.skijaPaint)
    }

    fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        skijaCanvas!!.drawArc(
            left,
            top,
            right,
            bottom,
            startAngle,
            sweepAngle,
            !useCenter,
            paint.skijaPaint
        )
    }

    fun drawText(
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        val typeface = paint.getTypeface() ?: Typeface.DEFAULT
        val skijaFont = org.jetbrains.skija.Font(typeface.skijaTypeface, paint.textSize)
        val buffer = skijaFont.hbFont.shape(text.toString(), org.jetbrains.skija.FontFeature.EMPTY)
        skijaCanvas!!.drawTextBuffer(buffer, x, y, skijaFont.skFont, paint.skijaPaint)
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawText(text, 0, text.length, x, y, paint)
    }

    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        paint: android.graphics.Paint
    ) {
        val skijaRoundedRect = RoundedRect.makeLTRB(left, top, right, bottom, rx, ry)
        skijaCanvas!!.drawRoundedRect(skijaRoundedRect, paint.skijaPaint)
    }

    fun drawPath(path: Path, paint: Paint) {
        skijaCanvas!!.drawPath(path.skijaPath, paint.skijaPaint)
    }

    fun drawCircle(
        cx: Float,
        cy: Float,
        radius: Float,
        paint: Paint
    ) {
        skijaCanvas!!.drawCircle(cx, cy, radius, paint.skijaPaint)
    }

    fun clipRect(left: Float, top: Float, right: Float, bottom: Float, op: Region.Op): Boolean {
        val skijaRect = Rect.makeLTRB(left, top, right, bottom)
        skijaCanvas!!.clipRect(skijaRect, op.skija)
        return true
    }

    fun clipPath(path: Path, op: Region.Op): Boolean {
        skijaCanvas!!.clipPath(path.skijaPath, op.skija)
        return true
    }

    fun scale(sx: Float, sy: Float) {
        skijaCanvas!!.scale(sx, sy)
    }

    fun save(): Int {
        return skijaCanvas!!.save()
    }

    fun restore() {
        skijaCanvas!!.restore()
    }
}
