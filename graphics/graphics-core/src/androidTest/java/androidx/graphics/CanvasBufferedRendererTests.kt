/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Environment.DIRECTORY_PICTURES
import android.util.Half
import androidx.annotation.RequiresApi
import androidx.graphics.CanvasBufferedRenderer.RenderResult.Companion.SUCCESS
import androidx.graphics.CanvasBufferedRendererTests.TestHelper.Companion.hardwareBufferRendererTest
import androidx.graphics.CanvasBufferedRendererTests.TestHelper.Companion.record
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.DefaultFlags
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CanvasBufferedRendererTests {

    private val mExecutor = Executors.newSingleThreadExecutor()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderAfterCloseReturnsError() = hardwareBufferRendererTest { renderer ->
        renderer.close()
        assertThrows(IllegalStateException::class.java) {
            renderer.obtainRenderRequest().drawAsync(mExecutor) { _ -> /* NO-OP */ }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testIsClosed() = hardwareBufferRendererTest { renderer ->
        assertFalse(renderer.isClosed)
        renderer.close()
        assertTrue(renderer.isClosed)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultipleClosesDoesNotCrash() = hardwareBufferRendererTest { renderer ->
        renderer.close()
        renderer.close()
        renderer.close()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testPreservationDisabledClearsContents() = hardwareBufferRendererTest { renderer ->
        val node =
            RenderNode("content").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                record { canvas -> canvas.drawColor(Color.BLUE) }
            }

        renderer.setContentRoot(node)
        var latch = CountDownLatch(1)
        var bitmap: Bitmap? = null
        renderer.obtainRenderRequest().preserveContents(true).drawAsync(mExecutor) { result ->
            assertEquals(SUCCESS, result.status)
            result.fence?.awaitForever()
            bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, null)
            latch.countDown()
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        assertNotNull(bitmap)
        assertTrue(bitmap!!.copy(Bitmap.Config.ARGB_8888, false).isAllColor(Color.BLUE))

        node.record { canvas -> canvas.drawColor(Color.RED, BlendMode.DST_OVER) }

        latch = CountDownLatch(1)
        renderer.obtainRenderRequest().preserveContents(false).drawAsync(mExecutor) { result ->
            assertEquals(SUCCESS, result.status)
            result.fence?.awaitForever()
            bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, null)
            latch.countDown()
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))

        assertNotNull(bitmap)
        assertTrue(bitmap!!.copy(Bitmap.Config.ARGB_8888, false).isAllColor(Color.RED))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testPreservationEnabledPreservesContents() =
        repeat(20) { verifyPreservedBuffer(CanvasBufferedRenderer.DEFAULT_IMPL) }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testPreservationEnabledPreservesContentsWithRedrawStrategy() =
        repeat(20) { verifyPreservedBuffer(CanvasBufferedRenderer.USE_V29_IMPL_WITH_REDRAW) }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun verifyPreservedBuffer(
        impl: Int,
        width: Int = TEST_WIDTH,
        height: Int = TEST_HEIGHT
    ) {
        val bitmap = bufferPreservationTestHelper(impl, width, height, mExecutor)
        assertNotNull(bitmap)
        assertTrue(bitmap!!.copy(Bitmap.Config.ARGB_8888, false).isAllColor(Color.BLUE))
    }

    /** Helper test method to save test bitmaps to disk to verify output for debugging purposes */
    private fun saveBitmap(bitmap: Bitmap, name: String) {
        val filename =
            InstrumentationRegistry.getInstrumentation()
                .context
                .getExternalFilesDir(DIRECTORY_PICTURES)
        val testFile = File(filename!!.path + "/" + name)
        try {
            FileOutputStream(testFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun bufferPreservationTestHelper(
        impl: Int,
        width: Int,
        height: Int,
        executor: Executor
    ): Bitmap? {
        val hardwareBufferRenderer =
            CanvasBufferedRenderer.Builder(width, height).setMaxBuffers(1).setImpl(impl).build()

        hardwareBufferRenderer.use { renderer ->
            val node =
                RenderNode("content").apply {
                    setPosition(0, 0, width, height)
                    record { canvas ->
                        canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
                        canvas.drawColor(Color.BLUE)
                    }
                }

            renderer.setContentRoot(node)
            val firstRenderLatch = CountDownLatch(1)
            var bitmap: Bitmap? = null
            renderer.obtainRenderRequest().preserveContents(true).drawAsync(executor) { result ->
                assertEquals(SUCCESS, result.status)
                result.fence?.awaitForever()
                bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, null)
                firstRenderLatch.countDown()
            }

            assertTrue(firstRenderLatch.await(3000, TimeUnit.MILLISECONDS))

            node.record { canvas -> canvas.drawColor(Color.RED, BlendMode.DST_OVER) }

            val secondRenderLatch = CountDownLatch(1)
            renderer.obtainRenderRequest().preserveContents(true).drawAsync(executor) { result ->
                assertEquals(SUCCESS, result.status)
                result.fence?.awaitForever()
                bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, null)
                secondRenderLatch.countDown()
            }

            assertTrue(secondRenderLatch.await(3000, TimeUnit.MILLISECONDS))

            return bitmap
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testHardwareBufferRender() = hardwareBufferRendererTest { renderer ->
        val contentRoot =
            RenderNode("content").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                record { canvas -> canvas.drawColor(Color.BLUE) }
            }
        renderer.setContentRoot(contentRoot)

        val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        val latch = CountDownLatch(1)
        var hardwareBuffer: HardwareBuffer? = null
        renderer.obtainRenderRequest().setColorSpace(colorSpace).drawAsync(mExecutor) { renderResult
            ->
            renderResult.fence?.awaitForever()
            hardwareBuffer = renderResult.hardwareBuffer
            latch.countDown()
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))

        val bitmap =
            Bitmap.wrapHardwareBuffer(hardwareBuffer!!, colorSpace)!!.copy(
                Bitmap.Config.ARGB_8888,
                false
            )

        assertEquals(TEST_WIDTH, bitmap.width)
        assertEquals(TEST_HEIGHT, bitmap.height)
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 0))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testDrawSync() = hardwareBufferRendererTest { renderer ->
        val contentRoot =
            RenderNode("content").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                record { canvas -> canvas.drawColor(Color.BLUE) }
            }
        renderer.setContentRoot(contentRoot)

        val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)

        var renderResult: CanvasBufferedRenderer.RenderResult?
        runBlocking {
            renderResult = renderer.obtainRenderRequest().setColorSpace(colorSpace).draw()
        }
        assertNotNull(renderResult)
        assertEquals(SUCCESS, renderResult!!.status)
        val fence = renderResult?.fence
        if (fence != null) {
            // by default drawSync will automatically wait on the fence and close it leaving
            // it in the invalid state
            assertFalse(fence.isValid())
        }

        val hardwareBuffer = renderResult!!.hardwareBuffer

        val bitmap =
            Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)!!.copy(
                Bitmap.Config.ARGB_8888,
                false
            )

        assertEquals(TEST_WIDTH, bitmap.width)
        assertEquals(TEST_HEIGHT, bitmap.height)
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 0))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testDrawSyncWithoutBlockingFence() = hardwareBufferRendererTest { renderer ->
        val contentRoot =
            RenderNode("content").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                record { canvas -> canvas.drawColor(Color.BLUE) }
            }
        renderer.setContentRoot(contentRoot)

        val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)

        var renderResult: CanvasBufferedRenderer.RenderResult?
        runBlocking {
            renderResult = renderer.obtainRenderRequest().setColorSpace(colorSpace).draw(false)
        }
        assertNotNull(renderResult)
        assertEquals(SUCCESS, renderResult!!.status)
        renderResult?.fence?.let { fence ->
            fence.awaitForever()
            fence.close()
        }

        val hardwareBuffer = renderResult!!.hardwareBuffer

        val bitmap =
            Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)!!.copy(
                Bitmap.Config.ARGB_8888,
                false
            )

        assertEquals(TEST_WIDTH, bitmap.width)
        assertEquals(TEST_HEIGHT, bitmap.height)
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 0))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testContentsPreservedSRGB() = preservedContentsTest { bitmap ->
        assertEquals(Color.RED, bitmap.getPixel(TEST_WIDTH / 2, TEST_HEIGHT / 4))
        assertEquals(Color.BLUE, bitmap.getPixel(TEST_WIDTH / 2, TEST_HEIGHT / 2 + TEST_HEIGHT / 4))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testContentsPreservedF16() =
        preservedContentsTest(
            format = PixelFormat.RGBA_F16,
            bitmapConfig = Bitmap.Config.RGBA_F16
        ) { bitmap ->
            val buffer =
                ByteBuffer.allocateDirect(bitmap.allocationByteCount).apply {
                    bitmap.copyPixelsToBuffer(this)
                    rewind()
                    order(ByteOrder.LITTLE_ENDIAN)
                }
            val srcColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            val srcToDst = ColorSpace.connect(srcColorSpace, ColorSpace.get(ColorSpace.Named.SRGB))

            val expectedRed = srcToDst.transform(1.0f, 0.0f, 0.0f)
            val expectedBlue = srcToDst.transform(0.0f, 0.0f, 1.0f)

            TestHelper.assertEqualsRgba16f(
                "TopMiddle",
                bitmap,
                TEST_WIDTH / 2,
                TEST_HEIGHT / 4,
                buffer,
                expectedRed[0],
                expectedRed[1],
                expectedRed[2],
                1.0f
            )
            TestHelper.assertEqualsRgba16f(
                "BottomMiddle",
                bitmap,
                TEST_WIDTH / 2,
                TEST_HEIGHT / 2 + TEST_HEIGHT / 4,
                buffer,
                expectedBlue[0],
                expectedBlue[1],
                expectedBlue[2],
                1.0f
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testContentsPreserved1010102() =
        preservedContentsTest(
            format = PixelFormat.RGBA_1010102,
            bitmapConfig = Bitmap.Config.RGBA_1010102
        ) { bitmap ->
            assertEquals(Color.RED, bitmap.getPixel(TEST_WIDTH / 2, TEST_HEIGHT / 4))
            assertEquals(
                Color.BLUE,
                bitmap.getPixel(TEST_WIDTH / 2, TEST_HEIGHT / 2 + TEST_HEIGHT / 4)
            )
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun preservedContentsTest(
        format: Int = PixelFormat.RGBA_8888,
        bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
        block: (Bitmap) -> Unit
    ) =
        hardwareBufferRendererTest(format = format) { renderer ->
            val contentRoot =
                RenderNode("content").apply {
                    setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                    record { canvas -> canvas.drawColor(Color.BLUE) }
                }
            renderer.setContentRoot(contentRoot)
            val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            val latch = CountDownLatch(1)
            var hardwareBuffer: HardwareBuffer?
            renderer
                .obtainRenderRequest()
                .setColorSpace(colorSpace)
                .preserveContents(true)
                .drawAsync(mExecutor) { renderResult ->
                    renderResult.fence?.awaitForever()
                    hardwareBuffer = renderResult.hardwareBuffer
                    latch.countDown()
                }

            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))

            val latch2 = CountDownLatch(1)
            contentRoot.record { canvas ->
                val paint = Paint().apply { color = Color.RED }
                canvas.drawRect(0f, 0f, TEST_WIDTH.toFloat(), TEST_HEIGHT / 2f, paint)
            }
            renderer.setContentRoot(contentRoot)

            hardwareBuffer = null
            renderer
                .obtainRenderRequest()
                .setColorSpace(colorSpace)
                .preserveContents(true)
                .drawAsync(mExecutor) { renderResult ->
                    renderResult.fence?.awaitForever()
                    hardwareBuffer = renderResult.hardwareBuffer
                    latch2.countDown()
                }

            assertTrue(latch2.await(3000, TimeUnit.MILLISECONDS))

            val bitmap =
                Bitmap.wrapHardwareBuffer(hardwareBuffer!!, colorSpace)!!.copy(bitmapConfig, false)

            assertEquals(TEST_WIDTH, bitmap.width)
            assertEquals(TEST_HEIGHT, bitmap.height)
            block(bitmap)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate0TallWide() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH * 2,
            height = TEST_HEIGHT,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.RED,
                topRight = Color.BLUE,
                bottomRight = Color.YELLOW,
                bottomLeft = Color.GREEN
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate0TallRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH,
            height = TEST_HEIGHT * 2,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.RED,
                topRight = Color.BLUE,
                bottomRight = Color.YELLOW,
                bottomLeft = Color.GREEN
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate90WideRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH * 2,
            height = TEST_HEIGHT,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.GREEN,
                topRight = Color.RED,
                bottomRight = Color.BLUE,
                bottomLeft = Color.YELLOW
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate90TallRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH,
            height = TEST_HEIGHT * 2,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.GREEN,
                topRight = Color.RED,
                bottomLeft = Color.YELLOW,
                bottomRight = Color.BLUE
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate180WideRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH * 2,
            height = TEST_HEIGHT,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.YELLOW,
                topRight = Color.GREEN,
                bottomLeft = Color.BLUE,
                bottomRight = Color.RED
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate180TallRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH,
            height = TEST_HEIGHT * 2,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.YELLOW,
                topRight = Color.GREEN,
                bottomLeft = Color.BLUE,
                bottomRight = Color.RED
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate270WideRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH * 2,
            height = TEST_HEIGHT,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.BLUE,
                topRight = Color.YELLOW,
                bottomRight = Color.GREEN,
                bottomLeft = Color.RED
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testTransformRotate270TallRect() =
        TestHelper.quadTest(
            mExecutor,
            width = TEST_WIDTH,
            height = TEST_HEIGHT * 2,
            transform = SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
        ) { bitmap ->
            TestHelper.assertBitmapQuadColors(
                bitmap,
                topLeft = Color.BLUE,
                topRight = Color.YELLOW,
                bottomRight = Color.GREEN,
                bottomLeft = Color.RED
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testUnknownTransformThrows() = hardwareBufferRendererTest { renderer ->
        val root =
            RenderNode("content").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                record { canvas ->
                    with(canvas) {
                        drawColor(Color.BLUE)
                        val paint = Paint().apply { color = Color.RED }
                        canvas.drawRect(0f, 0f, TEST_WIDTH / 2f, TEST_HEIGHT / 2f, paint)
                    }
                }
            }
        renderer.setContentRoot(root)

        val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        val latch = CountDownLatch(1)

        assertThrows(IllegalArgumentException::class.java) {
            renderer
                .obtainRenderRequest()
                .setColorSpace(colorSpace)
                .setBufferTransform(42)
                .drawAsync(mExecutor) { renderResult ->
                    renderResult.fence?.awaitForever()
                    latch.countDown()
                }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testColorSpaceDisplayP3() =
        TestHelper.colorSpaceTest(mExecutor, ColorSpace.get(ColorSpace.Named.DISPLAY_P3))

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testColorSpaceProPhotoRGB() =
        TestHelper.colorSpaceTest(mExecutor, ColorSpace.get(ColorSpace.Named.PRO_PHOTO_RGB))

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testColorSpaceAdobeRGB() =
        TestHelper.colorSpaceTest(mExecutor, ColorSpace.get(ColorSpace.Named.ADOBE_RGB))

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testColorSpaceDciP3() =
        TestHelper.colorSpaceTest(mExecutor, ColorSpace.get(ColorSpace.Named.DCI_P3))

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun spotShadowTest(
        transform: Int = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY,
    ) = hardwareBufferRendererTest { renderer ->
        val content = RenderNode("content")
        val colorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        renderer.apply {
            setLightSourceAlpha(0.0f, 1.0f)
            setLightSourceGeometry(TEST_WIDTH / 2f, 0f, 800.0f, 20.0f)
            setContentRoot(content)
        }
        val childRect = Rect(25, 25, 65, 65)
        content.setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
        with(TestHelper.Companion) {
            content.record { parentCanvas ->
                val childNode = RenderNode("shadowCaster")
                childNode.setPosition(childRect)
                val outline = Outline()
                outline.setRect(Rect(0, 0, childRect.width(), childRect.height()))
                outline.alpha = 1f
                childNode.setOutline(outline)
                val childCanvas = childNode.beginRecording()
                childCanvas.drawColor(Color.RED)
                childNode.endRecording()
                childNode.elevation = 20f

                parentCanvas.drawColor(Color.WHITE)
                parentCanvas.enableZ()
                parentCanvas.drawRenderNode(childNode)
                parentCanvas.disableZ()
            }
        }

        val latch = CountDownLatch(1)
        var renderStatus = CanvasBufferedRenderer.RenderResult.ERROR_UNKNOWN
        var hardwareBuffer: HardwareBuffer? = null

        renderer
            .obtainRenderRequest()
            .setColorSpace(colorSpace)
            .setBufferTransform(transform)
            .drawAsync(mExecutor) { renderResult ->
                renderStatus = renderResult.status
                renderResult.fence?.awaitForever()
                hardwareBuffer = renderResult.hardwareBuffer
                latch.countDown()
            }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        assertEquals(renderStatus, SUCCESS)
        val bitmap =
            Bitmap.wrapHardwareBuffer(hardwareBuffer!!, colorSpace)!!.copy(
                Bitmap.Config.ARGB_8888,
                false
            )

        val rect = Rect(childRect.left, childRect.bottom, childRect.right, childRect.bottom + 10)

        var result =
            bitmap.verify(rect) { actual, _, _ -> verifyPixelWithThreshold(actual, Color.RED, 10) }
        result =
            result ||
                bitmap.verify(rect.applyBufferTransform(bitmap.width, bitmap.height, transform)) {
                    actual,
                    _,
                    _ ->
                    verifyPixelGrayScale(actual, 1)
                }

        assertTrue(result)
    }

    private fun Bitmap.verify(rect: Rect, block: (Int, Int, Int) -> Boolean): Boolean {
        for (i in rect.left until rect.right) {
            for (j in rect.top until rect.bottom) {
                if (!block(getPixel(i, j), i, j)) {
                    return false
                }
            }
        }
        return true
    }

    /** @return True if close enough */
    private fun verifyPixelWithThreshold(color: Int, expectedColor: Int, threshold: Int): Boolean {
        val diff =
            (abs((Color.red(color) - Color.red(expectedColor)).toDouble()) +
                    abs((Color.green(color) - Color.green(expectedColor)).toDouble()) +
                    abs((Color.blue(color) - Color.blue(expectedColor)).toDouble()))
                .toInt()
        return diff <= threshold
    }

    /**
     * @param threshold Per channel differences for R / G / B channel against the average of these 3
     *   channels. Should be less than 2 normally.
     * @return True if the color is close enough to be a gray scale color.
     */
    private fun verifyPixelGrayScale(color: Int, threshold: Int): Boolean {
        var average = Color.red(color) + Color.green(color) + Color.blue(color)
        average /= 3
        return abs((Color.red(color) - average).toDouble()) <= threshold &&
            abs((Color.green(color) - average).toDouble()) <= threshold &&
            abs((Color.blue(color) - average).toDouble()) <= threshold
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSpotShadowSetup() = spotShadowTest()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSpotShadowRotate90() = spotShadowTest(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSpotShadowRotate180() = spotShadowTest(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testSpotShadowRotate270() = spotShadowTest(SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRendererBlocksOnBufferRelease() {
        val renderNode =
            RenderNode("node").apply {
                setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
                val canvas = beginRecording()
                canvas.drawColor(Color.RED)
                endRecording()
            }
        val renderer =
            CanvasBufferedRenderer.Builder(TEST_WIDTH, TEST_HEIGHT).setMaxBuffers(2).build().apply {
                setContentRoot(renderNode)
            }

        val executor = Executors.newSingleThreadExecutor()
        try {
            val latch1 = CountDownLatch(1)
            val latch2 = CountDownLatch(1)
            val latch3 = CountDownLatch(1)
            var hardwareBuffer: HardwareBuffer? = null
            renderer.obtainRenderRequest().drawAsync(executor) { result ->
                result.fence?.awaitForever()
                result.fence?.close()
                hardwareBuffer = result.hardwareBuffer
                latch1.countDown()
            }

            assertTrue(latch1.await(1000, TimeUnit.MILLISECONDS))

            var canvas = renderNode.beginRecording()
            canvas.drawColor(Color.BLUE)
            renderNode.endRecording()

            renderer.obtainRenderRequest().drawAsync(executor) { _ -> latch2.countDown() }

            assertTrue(latch2.await(1000, TimeUnit.MILLISECONDS))

            canvas = renderNode.beginRecording()
            canvas.drawColor(Color.GREEN)
            renderNode.endRecording()

            renderer.obtainRenderRequest().drawAsync(executor) { _ -> latch3.countDown() }

            // The 3rd render request should be blocked until the buffer is released
            assertFalse(latch3.await(1000, TimeUnit.MILLISECONDS))
            assertNotNull(hardwareBuffer)
            renderer.releaseBuffer(hardwareBuffer!!, null)
            assertTrue(latch3.await(1000, TimeUnit.MILLISECONDS))
        } finally {
            renderer.close()
            executor.shutdownNow()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun Rect.applyBufferTransform(width: Int, height: Int, transform: Int): Rect {
        val rectF = RectF(this)
        val matrix = Matrix()
        when (transform) {
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 -> {
                matrix.apply {
                    setRotate(90f)
                    postTranslate(width.toFloat(), 0f)
                }
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 -> {
                matrix.apply {
                    setRotate(180f)
                    postTranslate(width.toFloat(), height.toFloat())
                }
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 -> {
                matrix.apply {
                    setRotate(270f)
                    postTranslate(0f, width.toFloat())
                }
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY -> {
                matrix.reset()
            }
            else -> throw IllegalArgumentException("Invalid transform value")
        }
        matrix.mapRect(rectF)
        return Rect(
            rectF.left.toInt(),
            rectF.top.toInt(),
            rectF.right.toInt(),
            rectF.bottom.toInt()
        )
    }

    // See b/295332012
    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    )
    @Test
    fun testHardwareBufferRendererV34SharedFileDescriptorMonitoring() {
        fun createHardwareBufferRenderer(
            sharedFdMonitor: SharedFileDescriptorMonitor
        ): CanvasBufferedRendererV34 {
            return CanvasBufferedRendererV34(
                TEST_WIDTH,
                TEST_HEIGHT,
                HardwareBuffer.RGBA_8888,
                DefaultFlags,
                1,
                sharedFdMonitor
            )
        }

        val sharedFdMonitor = CanvasBufferedRendererV34.obtainSharedFdMonitor()
        // Monitor is only returned on devices running the vulkan hwui backend
        if (sharedFdMonitor != null) {
            val hbr1 = createHardwareBufferRenderer(sharedFdMonitor)
            val hbr2 = createHardwareBufferRenderer(sharedFdMonitor)
            val hbr3 = createHardwareBufferRenderer(sharedFdMonitor)

            hbr1.close()

            assertTrue(sharedFdMonitor.isMonitoring)

            hbr2.close()

            assertTrue(sharedFdMonitor.isMonitoring)

            hbr3.close()

            assertFalse(sharedFdMonitor.isMonitoring)

            val sharedFdMonitor2 = CanvasBufferedRendererV34.obtainSharedFdMonitor()!!
            val hbr4 = createHardwareBufferRenderer(sharedFdMonitor2)

            assertTrue(sharedFdMonitor2.isMonitoring)

            hbr4.close()

            assertFalse(sharedFdMonitor2.isMonitoring)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test
    fun testFdCleanupAfterSeveralRenders() {
        val hbr = CanvasBufferedRenderer.Builder(TEST_WIDTH, TEST_HEIGHT).setMaxBuffers(1).build()
        val renderNode = RenderNode("node").apply { setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT) }
        hbr.setContentRoot(renderNode)
        val executor = Executors.newSingleThreadExecutor()
        try {
            for (i in 0 until 100000) {
                val canvas = renderNode.beginRecording()
                canvas.drawColor(Color.RED)
                renderNode.endRecording()

                val latch = CountDownLatch(1)
                hbr.obtainRenderRequest().drawAsync(executor) { result ->
                    hbr.releaseBuffer(result.hardwareBuffer, result.fence)
                    latch.countDown()
                }
                latch.await()
            }
        } finally {
            executor.shutdownNow()
            hbr.close()
        }
    }

    companion object {
        const val TEST_WIDTH = 90
        const val TEST_HEIGHT = 90
    }

    /**
     * Helper class to move test methods that include APIs introduced in newer class levels. This is
     * done in order to avoid NoClassFoundExceptions being thrown when the test is loaded on lower
     * API levels even if there are corresponding @SdkSuppress annotations used in conjunction with
     * the corresponding API version code.
     */
    internal class TestHelper {
        companion object {

            @RequiresApi(Build.VERSION_CODES.Q)
            fun quadTest(
                executor: Executor,
                width: Int = TEST_WIDTH,
                height: Int = TEST_HEIGHT,
                transform: Int = SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY,
                colorSpace: ColorSpace = ColorSpace.get(ColorSpace.Named.SRGB),
                format: Int = PixelFormat.RGBA_8888,
                bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
                block: (Bitmap) -> Unit,
            ) {
                val bufferWidth: Int
                val bufferHeight: Int
                if (
                    transform == SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 ||
                        transform == SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
                ) {
                    bufferWidth = height
                    bufferHeight = width
                } else {
                    bufferWidth = width
                    bufferHeight = height
                }
                hardwareBufferRendererTest(
                    width = bufferWidth,
                    height = bufferHeight,
                    format = format
                ) { renderer ->
                    val root =
                        RenderNode("content").apply {
                            setPosition(0, 0, width, height)
                            record { canvas ->
                                val widthF = width.toFloat()
                                val heightF = height.toFloat()
                                val paint = Paint().apply { color = Color.RED }
                                canvas.drawRect(0f, 0f, widthF / 2f, heightF / 2f, paint)
                                paint.color = Color.BLUE
                                canvas.drawRect(widthF / 2f, 0f, widthF, heightF / 2f, paint)
                                paint.color = Color.GREEN
                                canvas.drawRect(0f, heightF / 2f, widthF / 2f, heightF, paint)
                                paint.color = Color.YELLOW
                                canvas.drawRect(widthF / 2f, heightF / 2f, widthF, heightF, paint)
                            }
                        }
                    renderer.setContentRoot(root)

                    val latch = CountDownLatch(1)
                    var hardwareBuffer: HardwareBuffer? = null
                    renderer
                        .obtainRenderRequest()
                        .setColorSpace(colorSpace)
                        .preserveContents(true)
                        .setBufferTransform(transform)
                        .drawAsync(executor) { renderResult ->
                            renderResult.fence?.awaitForever()
                            hardwareBuffer = renderResult.hardwareBuffer
                            latch.countDown()
                        }

                    assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))

                    val bitmap =
                        Bitmap.wrapHardwareBuffer(hardwareBuffer!!, colorSpace)!!.copy(
                            bitmapConfig,
                            false
                        )

                    assertEquals(bufferWidth, bitmap.width)
                    assertEquals(bufferHeight, bitmap.height)

                    block(bitmap)
                }
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            fun colorSpaceTest(executor: Executor, dstColorSpace: ColorSpace) =
                quadTest(
                    executor,
                    format = PixelFormat.RGBA_F16,
                    colorSpace = dstColorSpace,
                    bitmapConfig = Bitmap.Config.RGBA_F16
                ) { bitmap ->
                    val buffer =
                        ByteBuffer.allocateDirect(bitmap.allocationByteCount).apply {
                            bitmap.copyPixelsToBuffer(this)
                            rewind()
                            order(ByteOrder.LITTLE_ENDIAN)
                        }
                    val srcColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
                    val srcToDst = ColorSpace.connect(srcColorSpace, dstColorSpace)

                    val expectedRed = srcToDst.transform(1.0f, 0.0f, 0.0f)
                    val expectedBlue = srcToDst.transform(0.0f, 0.0f, 1.0f)
                    val expectedGreen = srcToDst.transform(0.0f, 1.0f, 0.0f)
                    val expectedYellow = srcToDst.transform(1.0f, 1.0f, 0.0f)

                    assertEqualsRgba16f(
                        "TopLeft",
                        bitmap,
                        TEST_WIDTH / 4,
                        TEST_HEIGHT / 4,
                        buffer,
                        expectedRed[0],
                        expectedRed[1],
                        expectedRed[2],
                        1.0f
                    )

                    assertEqualsRgba16f(
                        "TopRight",
                        bitmap,
                        (TEST_WIDTH * 3f / 4f).toInt(),
                        TEST_HEIGHT / 4,
                        buffer,
                        expectedBlue[0],
                        expectedBlue[1],
                        expectedBlue[2],
                        1.0f
                    )

                    assertEqualsRgba16f(
                        "BottomLeft",
                        bitmap,
                        TEST_WIDTH / 4,
                        (TEST_HEIGHT * 3f / 4f).toInt(),
                        buffer,
                        expectedGreen[0],
                        expectedGreen[1],
                        expectedGreen[2],
                        1.0f
                    )
                    assertEqualsRgba16f(
                        "BottomRight",
                        bitmap,
                        (TEST_WIDTH * 3f / 4f).toInt(),
                        (TEST_HEIGHT * 3f / 4f).toInt(),
                        buffer,
                        expectedYellow[0],
                        expectedYellow[1],
                        expectedYellow[2],
                        1.0f
                    )
                }

            @RequiresApi(Build.VERSION_CODES.Q)
            fun hardwareBufferRendererTest(
                width: Int = TEST_WIDTH,
                height: Int = TEST_HEIGHT,
                format: Int = HardwareBuffer.RGBA_8888,
                impl: Int = CanvasBufferedRenderer.DEFAULT_IMPL,
                block: (renderer: CanvasBufferedRenderer) -> Unit,
            ) {
                val usage =
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                if (
                    format != HardwareBuffer.RGBA_8888 &&
                        !HardwareBuffer.isSupported(width, height, format, 1, usage)
                ) {
                    // Early out if the hardware configuration is not supported.
                    // PixelFormat.RGBA_8888 should always be supported
                    return
                }
                val renderer =
                    CanvasBufferedRenderer.Builder(width, height)
                        .setMaxBuffers(1)
                        .setBufferFormat(format)
                        .setUsageFlags(usage)
                        .setImpl(impl)
                        .build()
                try {
                    block(renderer)
                } finally {
                    renderer.close()
                }
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            inline fun RenderNode.record(block: (canvas: Canvas) -> Unit): RenderNode {
                block(beginRecording())
                endRecording()
                return this
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            fun assertEqualsRgba16f(
                message: String,
                bitmap: Bitmap,
                x: Int,
                y: Int,
                dst: ByteBuffer,
                r: Float,
                g: Float,
                b: Float,
                a: Float,
            ) {
                val index = y * bitmap.rowBytes + (x shl 3)
                val cR = dst.getShort(index)
                val cG = dst.getShort(index + 2)
                val cB = dst.getShort(index + 4)
                val cA = dst.getShort(index + 6)
                assertEquals(message, r, Half.toFloat(cR), 0.01f)
                assertEquals(message, g, Half.toFloat(cG), 0.01f)
                assertEquals(message, b, Half.toFloat(cB), 0.01f)
                assertEquals(message, a, Half.toFloat(cA), 0.01f)
            }

            fun assertBitmapQuadColors(
                bitmap: Bitmap,
                topLeft: Int,
                topRight: Int,
                bottomLeft: Int,
                bottomRight: Int,
            ) {
                val width = bitmap.width
                val height = bitmap.height

                val topLeftStartX = 0
                val topLeftEndX = width / 2 - 2
                val topLeftStartY = 0
                val topLeftEndY = height / 2 - 2

                val topRightStartX = width / 2 + 2
                val topRightEndX = width - 1
                val topRightStartY = 0
                val topRightEndY = height / 2 - 2

                val bottomRightStartX = width / 2 + 2
                val bottomRightEndX = width - 1
                val bottomRightStartY = height / 2 + 2
                val bottomRightEndY = height - 1

                val bottomLeftStartX = 0
                val bottomLeftEndX = width / 2 - 2
                val bottomLeftStartY = height / 2 + 2
                val bottomLeftEndY = height - 1

                assertEquals(topLeft, bitmap.getPixel(topLeftStartX, topLeftStartY))
                assertEquals(topLeft, bitmap.getPixel(topLeftEndX, topLeftStartY))
                assertEquals(topLeft, bitmap.getPixel(topLeftEndX, topLeftEndY))
                assertEquals(topLeft, bitmap.getPixel(topLeftStartX, topLeftEndY))

                assertEquals(topRight, bitmap.getPixel(topRightStartX, topRightStartY))
                assertEquals(topRight, bitmap.getPixel(topRightEndX, topRightStartY))
                assertEquals(topRight, bitmap.getPixel(topRightEndX, topRightEndY))
                assertEquals(topRight, bitmap.getPixel(topRightStartX, topRightEndY))

                assertEquals(bottomRight, bitmap.getPixel(bottomRightStartX, bottomRightStartY))
                assertEquals(bottomRight, bitmap.getPixel(bottomRightEndX, bottomRightStartY))
                assertEquals(bottomRight, bitmap.getPixel(bottomRightEndX, bottomRightEndY))
                assertEquals(bottomRight, bitmap.getPixel(bottomRightStartX, bottomRightEndY))

                assertEquals(bottomLeft, bitmap.getPixel(bottomLeftStartX, bottomLeftStartY))
                assertEquals(bottomLeft, bitmap.getPixel(bottomLeftEndX, bottomLeftStartY))
                assertEquals(bottomLeft, bitmap.getPixel(bottomLeftEndX, bottomLeftEndY))
                assertEquals(bottomLeft, bitmap.getPixel(bottomLeftStartX, bottomLeftEndY))
            }
        }
    }
}
