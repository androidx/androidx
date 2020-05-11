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

public class Canvas(private val skijaCanvas: org.jetbrains.skija.Canvas) {
    var skijaFont = org.jetbrains.skija.Font(Typeface.DEFAULT.skijaTypeface, 30f)

    fun translate(x: Float, y: Float) {
        skijaCanvas.translate(x, y)
    }

    fun drawLine(
        startX: Float,
        startY: Float,
        stopX: Float,
        stopY: Float,
        paint: Paint
    ) {
        skijaCanvas.drawLine(
            startX,
            startY,
            stopX,
            stopY,
            paint.skijaPaint
        )
    }

    fun drawRect(rect: android.graphics.RectF, paint: android.graphics.Paint) {
        val skijaRect = Rect.makeLTRB(rect.left, rect.top, rect.right, rect.bottom)
        skijaCanvas.drawRect(skijaRect, paint.skijaPaint)
    }

    fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: android.graphics.Paint
    ) {
        val skijaRect = Rect.makeLTRB(left, top, right, bottom)
        skijaCanvas.drawRect(skijaRect, paint.skijaPaint)
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
        skijaCanvas.drawArc(
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
        val buffer = skijaFont.hbFont.shape(text.toString(), org.jetbrains.skija.FontFeature.EMPTY)
        skijaCanvas.drawTextBuffer(buffer, x, y, skijaFont.skFont, paint.skijaPaint)
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawText(text, 0, text.length, x, y, paint)
    }

    fun save(): Int {
        return skijaCanvas.save()
    }

    fun restore() {
        skijaCanvas.restore()
    }
}
