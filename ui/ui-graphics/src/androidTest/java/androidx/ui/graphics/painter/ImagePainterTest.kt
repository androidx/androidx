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

package androidx.ui.graphics.painter

import androidx.test.filters.SmallTest
import androidx.ui.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.compositeOver
import androidx.ui.graphics.toArgb
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ImagePainterTest {

    val white = Color.White.toArgb()
    private val srcSize = PxSize(Px(100.0f), Px(100.0f))

    private fun createTestSrcImage(): ImageAsset {
        val src = ImageAsset(100, 100)
        val canvas = Canvas(src)
        val paint = Paint().apply {
            this.color = Color.Red
        }

        canvas.drawRect(Rect.fromLTWH(25.0f, 25.0f, 75.0f, 75.0f), paint)
        return src
    }

    private fun createTestDstImage(): ImageAsset {
        val dst = ImageAsset(200, 200)
        val dstCanvas = Canvas(dst)
        val dstPaint = Paint().apply {
            this.color = Color.White
        }
        dstCanvas.drawRect(
            Rect.fromLTWH(0.0f, 0.0f, 200.0f, 200.0f),
            dstPaint
        )
        return dst
    }

    @Test
    fun testImagePainter() {
        val imagePainter = ImagePainter(createTestSrcImage())
        val dst = createTestDstImage()
        imagePainter.draw(Canvas(dst), srcSize)

        assertEquals(white, dst.nativeImage.getPixel(195, 5))
        assertEquals(white, dst.nativeImage.getPixel(195, 195))
        assertEquals(white, dst.nativeImage.getPixel(5, 195))
        assertEquals(Color.Red.toArgb(), dst.nativeImage.getPixel(30, 70))
    }

    @Test
    fun testImagePainterAppliedAlpha() {
        val imagePainter = ImagePainter(createTestSrcImage())
        val dst = createTestDstImage()

        val flagCanvas = LayerFlagCanvas(Canvas(dst))
        imagePainter.draw(flagCanvas, srcSize, alpha = 0.5f)

        // ImagePainter's optimized application of alpha should be applied here
        // instead of Painter's default implementation that invokes Canvas.saveLayer
        assertFalse(flagCanvas.saveLayerCalled)

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val result = Color(dst.nativeImage.getPixel(50, 50))
        assertEquals(expected.red, result.red, 0.01f)
        assertEquals(expected.green, result.green, 0.01f)
        assertEquals(expected.blue, result.blue, 0.01f)
        assertEquals(expected.alpha, result.alpha, 0.01f)
    }

    @Test
    fun testImagePainterTint() {
        val imagePainter = ImagePainter(createTestSrcImage())
        val dst = createTestDstImage()
        imagePainter.draw(
            Canvas(dst),
            srcSize,
            colorFilter = ColorFilter(Color.Cyan, BlendMode.srcIn)
        )

        val argbWhite = Color.White.toArgb()
        val argbCyan = Color.Cyan.toArgb()
        assertEquals(argbWhite, dst.nativeImage.getPixel(195, 5))
        assertEquals(argbWhite, dst.nativeImage.getPixel(195, 195))
        assertEquals(argbWhite, dst.nativeImage.getPixel(5, 195))
        assertEquals(argbCyan, dst.nativeImage.getPixel(30, 70))
    }

    class LayerFlagCanvas(private val canvas: Canvas) : Canvas by canvas {

        var saveLayerCalled: Boolean = false

        override fun saveLayer(bounds: Rect, paint: Paint) {
            saveLayerCalled = true
            canvas.saveLayer(bounds, paint)
        }
    }
}