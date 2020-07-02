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

import androidx.ui.desktop.TestResources.testImageAsset
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ClipOp
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Path
import androidx.ui.graphics.PathFillType
import androidx.ui.graphics.PointMode
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.graphics.VertexMode
import androidx.ui.graphics.Vertices
import androidx.ui.graphics.vectormath.Matrix4
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import org.junit.Test

class AndroidEmuCanvasTest : AndroidEmuTest() {
    private val canvas: Canvas = initCanvas(width = 100, height = 100)

    @Test
    fun `save, restore without crash`() {
        canvas.save()
        canvas.restore()
        canvas.save()
        canvas.save()
        canvas.restore()
        canvas.restore()
    }

    @Test
    fun `save, restore layer without crash`() {
        canvas.saveLayer(
            Rect(
                left = 0f,
                top = 0f,
                right = 50f,
                bottom = 50f
            ),
            Paint()
        )
        canvas.restore()
    }

    @Test
    fun `transform operations without crash`() {
        canvas.translate(10f, 20f)
        canvas.scale(1.1f, 1.2f)
        canvas.rotate(30f)
        canvas.skew(1f, 2f)
        canvas.concat(
            Matrix4.identity().apply {
                translate(0.9f, 0.5f)
            }
        )
    }

    @Test
    fun `clipRect without crash`() {
        canvas.clipRect(
            left = 0f,
            top = 10f,
            right = 100f,
            bottom = 70f,
            clipOp = ClipOp.intersect
        )
        canvas.clipRect(
            left = 50f,
            top = 30f,
            right = 70f,
            bottom = 100f,
            clipOp = ClipOp.difference
        )
    }

    @Test
    fun `clipPath without crash`() {
        val path = Path().apply {
            fillType = PathFillType.nonZero
            lineTo(20f, 40f)
            addArc(
                oval = Rect(left = 0f, top = 0f, right = 20f, bottom = 20f),
                startAngleDegrees = 30f,
                sweepAngleDegrees = 10f
            )
        }
        canvas.clipPath(path, ClipOp.intersect)
    }

    @Test
    fun `drawLine without crash`() {
        val paint = Paint().apply {
            color = Color.Red
            strokeWidth = 2f
            strokeCap = StrokeCap.butt
        }
        canvas.drawLine(Offset(10f, 10f), Offset(40f, 50f), paint)
    }

    @Test
    fun `drawRect without crash`() {
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawRect(left = 0f, top = 10f, right = 10f, bottom = 20f, paint = paint)
    }

    @Test
    fun `drawRoundRect without crash`() {
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawRoundRect(
            left = 0f,
            top = 10f,
            right = 10f,
            bottom = 20f,
            radiusX = 4f,
            radiusY = 2f,
            paint = paint
        )
    }

    @Test
    fun `drawOval without crash`() {
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawOval(
            left = 0f,
            top = 10f,
            right = 10f,
            bottom = 20f,
            paint = paint
        )
    }

    @Test
    fun `drawCircle without crash`() {
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawCircle(
            Offset(20f, 30f),
            radius = 25f,
            paint = paint
        )
    }

    @Test
    fun `drawArc without crash`() {
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawArc(
            left = 0f,
            top = 0f,
            right = 20f,
            bottom = 20f,
            startAngle = 30f,
            sweepAngle = 10f,
            useCenter = true,
            paint = paint
        )
    }

    @Test
    fun `drawPath without crash`() {
        val path = Path().apply {
            fillType = PathFillType.nonZero
            moveTo(25f, 10f)
            lineTo(30f, 20f)
            lineTo(20f, 20f)
        }

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
    fun `drawImage without crash`() {
        val image = testImageAsset()
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawImage(image, Offset(30f, 40f), paint)
    }

    @Test
    fun `drawImageRect without crash`() {
        val image = testImageAsset()
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawImageRect(
            image,
            srcOffset = IntOffset(30, 40),
            srcSize = IntSize(20, 30),
            dstOffset = IntOffset(30, 40),
            dstSize = IntSize(20, 30),
            paint = paint
        )
    }

    @Test
    fun `drawPoints without crash`() {
        val paint = Paint().apply {
            color = Color.Red
            strokeWidth = 2f
            strokeCap = StrokeCap.butt
            strokeJoin = StrokeJoin.round
        }
        canvas.drawPoints(
            pointMode = PointMode.points,
            points = listOf(
                Offset(20f, 30f),
                Offset(50f, 30f),
                Offset(30f, 50f)
            ),
            paint = paint
        )
        canvas.drawPoints(
            pointMode = PointMode.lines,
            points = listOf(
                Offset(20f, 30f),
                Offset(50f, 30f),
                Offset(30f, 50f)
            ),
            paint = paint
        )
        canvas.drawPoints(
            pointMode = PointMode.polygon,
            points = listOf(
                Offset(20f, 30f),
                Offset(50f, 30f),
                Offset(30f, 50f)
            ),
            paint = paint
        )
    }

    @Test
    fun `drawRawPoints without crash`() {
        val paint = Paint().apply {
            color = Color.Red
            strokeWidth = 2f
            strokeCap = StrokeCap.butt
            strokeJoin = StrokeJoin.round
        }
        canvas.drawRawPoints(
            pointMode = PointMode.points,
            points = floatArrayOf(
                20f, 30f,
                50f, 30f,
                30f, 50f
            ),
            paint = paint
        )
        canvas.drawRawPoints(
            pointMode = PointMode.lines,
            points = floatArrayOf(
                20f, 30f,
                50f, 30f,
                30f, 50f
            ),
            paint = paint
        )
        canvas.drawRawPoints(
            pointMode = PointMode.polygon,
            points = floatArrayOf(
                20f, 30f,
                50f, 30f,
                30f, 50f
            ),
            paint = paint
        )
    }

    @Test
    fun `drawVertices without crash`() {
        val paint = Paint().apply {
            color = Color.Red
        }
        canvas.drawVertices(
            Vertices(
                vertexMode = VertexMode.triangleStrip,
                positions = listOf(
                    Offset(20f, 10f),
                    Offset(25f, 30f),
                    Offset(40f, 15f)
                ),
                textureCoordinates = listOf(
                    Offset(0f, 0f),
                    Offset(0.5f, 1f),
                    Offset(1f, 0.5f)
                ),
                colors = listOf(
                    Color.Red,
                    Color.Green,
                    Color.Blue
                ),
                indices = listOf(
                    0, 1, 2
                )
            ),
            blendMode = BlendMode.plus,
            paint = paint
        )
    }

    @Test
    fun `enableZ, disableZ without crash`() {
        canvas.enableZ()
        canvas.disableZ()
    }
}
