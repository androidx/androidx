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
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.FilterQuality
import androidx.ui.graphics.ImageShader
import androidx.ui.graphics.LinearGradientShader
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.RadialGradientShader
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.graphics.TileMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidEmuPaintTest : AndroidEmuTest() {
    private val canvas: Canvas = initCanvas(width = 100, height = 100)

    @Test
    fun `call paint methods`() {
        val linearGradientShader = LinearGradientShader(
            from = Offset(0f, 0f),
            to = Offset(100f, 20f),
            colors = listOf(Color.Red, Color.Blue, Color.Green),
            colorStops = listOf(0f, 0.5f, 1f),
            tileMode = TileMode.Mirror
        )

        val paint = Paint().apply {
            color = Color.Red
            style = PaintingStyle.stroke
            strokeWidth = 2f
            strokeCap = StrokeCap.butt
            strokeMiterLimit = 3f
            strokeJoin = StrokeJoin.round
            isAntiAlias = true
            filterQuality = FilterQuality.low
            alpha = 0.3f
            blendMode = BlendMode.plus
            colorFilter = ColorFilter(Color.Red, BlendMode.multiply)
            shader = ImageShader(
                testImageAsset(),
                tileModeX = TileMode.Mirror
            )
            shader = RadialGradientShader(
                center = Offset(5f, 0f),
                radius = 10f,
                colors = listOf(Color.Red, Color.Blue, Color.Green),
                colorStops = listOf(0f, 0.5f, 1f),
                tileMode = TileMode.Mirror
            )
            shader = linearGradientShader
        }

        assertEquals(Color.Red, paint.color.copy(alpha = 1f))
        assertEquals(0.3f, paint.color.alpha, 0.1f)
        assertEquals(PaintingStyle.stroke, paint.style)
        assertEquals(2f, paint.strokeWidth)
        assertEquals(StrokeCap.butt, paint.strokeCap)
        assertEquals(3f, paint.strokeMiterLimit)
        assertEquals(StrokeJoin.round, paint.strokeJoin)
        assertEquals(true, paint.isAntiAlias)
        assertEquals(FilterQuality.low, paint.filterQuality)
        assertEquals(0.3f, paint.alpha, 0.1f)
        assertEquals(BlendMode.plus, paint.blendMode)
        assertEquals(ColorFilter(Color.Red, BlendMode.multiply), paint.colorFilter)
        assertEquals(linearGradientShader, paint.shader)

        canvas.drawLine(Offset(10f, 10f), Offset(40f, 50f), paint)
    }
}
