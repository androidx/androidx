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

package androidx.ui.graphics.canvas

import androidx.compose.ui.geometry.Offset
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.DesktopGraphicsTest
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PointMode
import androidx.ui.graphics.StrokeCap
import org.junit.Test

class DesktopCanvasPointsTest : DesktopGraphicsTest() {
    private val canvas: Canvas = initCanvas(widthPx = 16, heightPx = 16)

    @Test
    fun drawLines() {
        canvas.drawPoints(
            pointMode = PointMode.lines,
            points = listOf(
                Offset(0f, 8f),
                Offset(8f, 0f),
                Offset(8f, 8f),
                Offset(0f, 8f)
            ),
            paint = Paint().apply {
                color = Color.Red
                strokeWidth = 2f
            }
        )

        canvas.translate(6f, 6f)
        canvas.drawPoints(
            pointMode = PointMode.lines,
            points = listOf(
                Offset(0f, 8f),
                Offset(8f, 0f),
                Offset(8f, 8f),
                Offset(0f, 8f)
            ),
            paint = Paint().apply {
                color = Color.Green
                strokeWidth = 2f
                strokeCap = StrokeCap.round
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun drawPoints() {
        canvas.drawPoints(
            pointMode = PointMode.points,
            points = listOf(
                Offset(0f, 2f),
                Offset(2f, 0f)
            ),
            paint = Paint().apply {
                color = Color.Red
                strokeWidth = 2f
                strokeCap = StrokeCap.butt
            }
        )
        canvas.drawRawPoints(
            pointMode = PointMode.points,
            points = floatArrayOf(
                4f, 4f,
                8f, 8f
            ),
            paint = Paint().apply {
                color = Color.Blue
                strokeWidth = 4f
                strokeCap = StrokeCap.round
            }
        )
        canvas.drawPoints(
            pointMode = PointMode.points,
            points = listOf(
                Offset(4f, 0f)
            ),
            paint = Paint().apply {
                color = Color.Green
                strokeWidth = 2f
                strokeCap = StrokeCap.square
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun drawPolygons() {
        canvas.drawPoints(
            pointMode = PointMode.polygon,
            points = listOf(
                Offset(0f, 8f),
                Offset(8f, 0f),
                Offset(8f, 8f),
                Offset(0f, 8f)
            ),
            paint = Paint().apply {
                color = Color.Red
                strokeWidth = 2f
            }
        )

        canvas.translate(6f, 6f)
        canvas.drawPoints(
            pointMode = PointMode.polygon,
            points = listOf(
                Offset(0f, 8f),
                Offset(8f, 0f),
                Offset(8f, 8f),
                Offset(0f, 8f)
            ),
            paint = Paint().apply {
                color = Color.Green
                strokeWidth = 2f
                strokeCap = StrokeCap.round
            }
        )

        screenshotRule.snap(surface)
    }
}
