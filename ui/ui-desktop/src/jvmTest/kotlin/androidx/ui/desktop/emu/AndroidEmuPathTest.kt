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

package androidx.ui.desktop.emu

import androidx.ui.geometry.Offset
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Path
import androidx.ui.graphics.PathFillType
import androidx.ui.graphics.PathOperation
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidEmuPathTest : AndroidEmuTest() {
    private val canvas: Canvas = initCanvas(width = 100, height = 100)

    @Test
    fun `draw complex path without crash`() {
        val path = Path().apply {
            moveTo(20f, 30f)
            reset()

            fillType = PathFillType.nonZero
            moveTo(250f, 100f)
            lineTo(300f, 200f)
            lineTo(200f, 200f)
            close()

            relativeMoveTo(10f, 20f)
            lineTo(20f, 40f)
            relativeLineTo(30f, 40f)
            quadraticBezierTo(x1 = 10f, x2 = 20f, y1 = 40f, y2 = 60f)
            relativeQuadraticBezierTo(dx1 = 10f, dx2 = 20f, dy1 = 40f, dy2 = 60f)
            cubicTo(x1 = 10f, y1 = 40f, x2 = 20f, y2 = 60f, x3 = 30f, y3 = 70f)
            relativeCubicTo(dx1 = 10f, dy1 = 40f, dx2 = 20f, dy2 = 60f, dx3 = 30f, dy3 = 70f)
            arcTo(
                rect = Rect(left = 0f, top = 0f, right = 20f, bottom = 20f),
                startAngleDegrees = 30f,
                sweepAngleDegrees = 10f,
                forceMoveTo = true
            )
            addRect(Rect(left = 0f, top = 0f, right = 20f, bottom = 20f))
            addOval(Rect(left = 0f, top = 0f, right = 20f, bottom = 20f))
            addArc(
                oval = Rect(left = 0f, top = 0f, right = 20f, bottom = 20f),
                startAngleDegrees = 30f,
                sweepAngleDegrees = 10f
            )
            addRRect(
                RRect(left = 0f, top = 0f, right = 20f, bottom = 20f, radiusX = 2f, radiusY = 2f)
            )
            addPath(
                Path().apply {
                    lineTo(20f, 40f)
                    lineTo(30f, 50f)
                },
                Offset(20f, 30f)
            )
            op(
                Path().apply {
                    lineTo(20f, 40f)
                    lineTo(30f, 50f)
                },
                Path().apply {
                    lineTo(30f, 40f)
                    lineTo(40f, 50f)
                },
                PathOperation.intersect
            )
            shift(Offset(10f, 20f))
        }

        assertEquals(Rect.fromLTRB(10f, 20f, 310f, 240f), path.getBounds())
        assertEquals(false, path.isConvex)
        assertEquals(false, path.isEmpty)

        val paint = Paint().apply {
            color = Color.Red
            strokeWidth = 2f
            strokeCap = StrokeCap.butt
            strokeJoin = StrokeJoin.round
        }

        canvas.drawPath(
            path = path,
            paint = paint
        )
    }

    @Test
    fun `check parameters of initial path`() {
        val path = Path()
        assertEquals(Rect.fromLTRB(0f, 0f, 0f, 0f), path.getBounds())
        assertEquals(true, path.isConvex)
        assertEquals(true, path.isEmpty)
    }
}
