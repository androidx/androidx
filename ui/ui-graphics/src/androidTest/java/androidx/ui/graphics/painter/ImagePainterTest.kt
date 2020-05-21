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
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.geometry.Size
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.compositeOver
import androidx.ui.graphics.drawscope.drawPainter
import androidx.ui.graphics.toPixelMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ImagePainterTest {

    val white = Color.White
    private val srcSize = Size(100.0f, 100.0f)

    private fun createTestSrcImage(): ImageAsset {
        val src = ImageAsset(100, 100)
        val canvas = Canvas(src)
        val paint = Paint().apply {
            this.color = Color.Blue
        }

        canvas.drawRect(Rect.fromLTWH(0.0f, 0.0f, 100.0f, 100.0f), paint)
        paint.color = Color.Red
        canvas.drawRect(Rect.fromLTWH(25.0f, 25.0f, 50.0f, 50.0f), paint)
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
        drawPainter(imagePainter, Canvas(dst), srcSize)

        val pixelmap = dst.toPixelMap()
        assertEquals(white, pixelmap[195, 5])
        assertEquals(white, pixelmap[195, 195])
        assertEquals(white, pixelmap[5, 195])
        assertEquals(Color.Red, pixelmap[30, 70])
    }

    @Test
    fun testImagePainterAppliedAlpha() {
        val imagePainter = ImagePainter(createTestSrcImage())
        val dst = createTestDstImage()

        val flagCanvas = LayerFlagCanvas(Canvas(dst))
        drawPainter(imagePainter, flagCanvas, srcSize, alpha = 0.5f)

        // ImagePainter's optimized application of alpha should be applied here
        // instead of Painter's default implementation that invokes Canvas.saveLayer
        assertFalse(flagCanvas.saveLayerCalled)

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val result = dst.toPixelMap()[50, 50]
        assertEquals(expected.red, result.red, 0.01f)
        assertEquals(expected.green, result.green, 0.01f)
        assertEquals(expected.blue, result.blue, 0.01f)
        assertEquals(expected.alpha, result.alpha, 0.01f)
    }

    @Test
    fun testImagePainterTint() {
        val imagePainter = ImagePainter(createTestSrcImage())
        val dst = createTestDstImage()

        drawPainter(
            imagePainter,
            Canvas(dst),
            srcSize,
            colorFilter = ColorFilter(Color.Cyan, BlendMode.srcIn)
        )

        val pixelmap = dst.toPixelMap()
        assertEquals(Color.White, pixelmap[195, 5])
        assertEquals(Color.White, pixelmap[195, 195])
        assertEquals(Color.White, pixelmap[5, 195])
        assertEquals(Color.Cyan, pixelmap[30, 70])
    }

    @Test
    fun testImageSrcBounds() {
        val srcImage = createTestSrcImage()
        val dst = createTestDstImage()
        val canvas = Canvas(dst)

        val topLeftPainter = ImagePainter(srcImage,
            srcOffset = Offset.zero,
            srcSize = Size(50.0f, 50.0f)
        )

        val intrinsicSize = topLeftPainter.intrinsicSize
        assertEquals(50.0f, intrinsicSize.width)
        assertEquals(50.0f, intrinsicSize.height)
        drawPainter(topLeftPainter, Canvas(dst), intrinsicSize)

        val topLeftMap = dst.toPixelMap()
        assertEquals(Color.Blue, topLeftMap[0, 0])
        assertEquals(Color.Blue, topLeftMap[0, 49])
        assertEquals(Color.Blue, topLeftMap[49, 0])
        assertEquals(Color.Red, topLeftMap[49, 49])

        val topRightPainter = ImagePainter(srcImage,
            srcOffset = Offset(50.0f, 0.0f),
            srcSize = Size(50.0f, 50.0f)
        )

        val topRightDst = createTestDstImage()
        drawPainter(topRightPainter, Canvas(topRightDst), topRightPainter.intrinsicSize)

        val topRightMap = topRightDst.toPixelMap()
        assertEquals(Color.Blue, topRightMap[0, 0])
        assertEquals(Color.Red, topRightMap[0, 49])
        assertEquals(Color.Blue, topRightMap[49, 0])
        assertEquals(Color.Blue, topRightMap[49, 49])

        val bottomLeftPainter = ImagePainter(srcImage,
            srcOffset = Offset(0.0f, 50.0f),
            srcSize = Size(50.0f, 50.0f)
        )

        drawPainter(bottomLeftPainter, canvas, bottomLeftPainter.intrinsicSize)

        val bottomLeftMap = dst.toPixelMap()
        assertEquals(Color.Blue, bottomLeftMap[0, 0])
        assertEquals(Color.Red, bottomLeftMap[49, 0])
        assertEquals(Color.Blue, bottomLeftMap[0, 49])
        assertEquals(Color.Blue, bottomLeftMap[49, 49])

        val bottomRightPainter = ImagePainter(srcImage,
            srcOffset = Offset(50.0f, 50.0f),
            srcSize = Size(50.0f, 50.0f)
        )

        drawPainter(bottomRightPainter, canvas, bottomRightPainter.intrinsicSize)

        val bottomRightMap = dst.toPixelMap()
        assertEquals(Color.Red, bottomRightMap[0, 0])
        assertEquals(Color.Blue, bottomRightMap[49, 0])
        assertEquals(Color.Blue, bottomRightMap[0, 49])
        assertEquals(Color.Blue, bottomRightMap[49, 49])
    }

    @Test
    fun testInvalidLeftBoundThrows() {
        try {
            ImagePainter(createTestSrcImage(),
                Offset(-1.0f, 1.0f),
                Size(10.0f, 10.0f)
            )
            fail("Left bound must be greater than or equal to zero")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testInvalidTopBoundThrows() {
        try {
            ImagePainter(createTestSrcImage(),
                Offset(0.0f, -1.0f),
                Size(10.0f, 10.0f)
            )
            fail("Top bound must be greater than or equal to zero")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testInvalidRightBoundThrows() {
        try {
            val image = createTestSrcImage()
            ImagePainter(image,
                Offset(0.0f, 0.0f),
                Size(image.width + 1.0f, 10.0f)
            )
            fail("Right bound must be less than ImageAsset width")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testInvalidBottomBoundThrows() {
        try {
            val image = createTestSrcImage()
            ImagePainter(image,
                Offset(0.0f, 0.0f),
                Size(10.0f, image.height + 1.0f)
            )
            fail("Bottom bound must be less than ImageAsset height")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testRightLessThanLeftThrows() {
        try {
            ImagePainter(createTestSrcImage(),
                Offset(50.0f, 0.0f),
                Size(-40.0f, 10.0f)
            )
            fail("Right bound must be greater than left bound")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testTopLessThanBottomThrows() {
        try {
            ImagePainter(createTestSrcImage(),
                Offset(0.0f, 100.0f),
                Size(-90.0f, -90.0f)
            )
            fail("Bottom bound must be larger than top bound")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    class LayerFlagCanvas(private val canvas: Canvas) : Canvas by canvas {

        var saveLayerCalled: Boolean = false

        override fun saveLayer(bounds: Rect, paint: Paint) {
            saveLayerCalled = true
            canvas.saveLayer(bounds, paint)
        }
    }
}