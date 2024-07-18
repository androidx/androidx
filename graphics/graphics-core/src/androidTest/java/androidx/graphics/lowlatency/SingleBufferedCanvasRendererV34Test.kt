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
package androidx.graphics.lowlatency

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.hardware.HardwareBuffer
import androidx.graphics.drawSquares
import androidx.graphics.isAllColor
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_IDENTITY
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.graphics.verifyQuadrants
import androidx.graphics.withEgl
import androidx.hardware.SyncFenceCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34)
@RunWith(AndroidJUnit4::class)
@SmallTest
class SingleBufferedCanvasRendererV34Test {

    companion object {
        const val TEST_WIDTH = 20
        const val TEST_HEIGHT = 20
    }

    data class RectColors(
        val topLeft: Int,
        val topRight: Int,
        val bottomLeft: Int,
        val bottomRight: Int
    )

    @Test
    fun testRenderFrameRotate0() {
        testRenderWithTransform(
            BUFFER_TRANSFORM_IDENTITY,
            RectColors(
                topLeft = Color.RED,
                topRight = Color.YELLOW,
                bottomRight = Color.BLUE,
                bottomLeft = Color.GREEN
            ),
            RectColors(
                topLeft = Color.RED,
                topRight = Color.YELLOW,
                bottomRight = Color.BLUE,
                bottomLeft = Color.GREEN
            )
        )
    }

    @Test
    fun testRenderFrameRotate90() {
        testRenderWithTransform(
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90,
            RectColors(
                topLeft = Color.RED,
                topRight = Color.YELLOW,
                bottomRight = Color.BLUE,
                bottomLeft = Color.GREEN
            ),
            RectColors(
                topLeft = Color.YELLOW,
                topRight = Color.BLUE,
                bottomRight = Color.GREEN,
                bottomLeft = Color.RED
            )
        )
    }

    @Test
    fun testRenderFrameRotate180() {
        testRenderWithTransform(
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180,
            RectColors(
                topLeft = Color.RED,
                topRight = Color.YELLOW,
                bottomRight = Color.BLUE,
                bottomLeft = Color.GREEN
            ),
            RectColors(
                topLeft = Color.BLUE,
                topRight = Color.GREEN,
                bottomRight = Color.RED,
                bottomLeft = Color.YELLOW
            )
        )
    }

    @Test
    fun testRenderFrameRotate270() {
        testRenderWithTransform(
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270,
            RectColors(
                topLeft = Color.RED,
                topRight = Color.YELLOW,
                bottomRight = Color.BLUE,
                bottomLeft = Color.GREEN
            ),
            RectColors(
                topLeft = Color.GREEN,
                topRight = Color.RED,
                bottomRight = Color.YELLOW,
                bottomLeft = Color.BLUE
            )
        )
    }

    @Test
    fun testClearRenderer() {
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val executor = HandlerThreadExecutor("thread")
        val firstRenderLatch = CountDownLatch(1)
        val clearLatch = CountDownLatch(2)
        var buffer: HardwareBuffer? = null
        val renderer = SingleBufferedCanvasRendererV34(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Unit> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Unit) {
                    canvas.drawColor(Color.RED)
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    syncFenceCompat?.awaitForever()
                    buffer = hardwareBuffer
                    firstRenderLatch.countDown()
                    clearLatch.countDown()
                }
            })
        try {
            renderer.render(Unit)
            firstRenderLatch.await(3000, TimeUnit.MILLISECONDS)
            renderer.clear()
            assertTrue(clearLatch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(buffer)
            val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
            val bitmap = Bitmap.wrapHardwareBuffer(buffer!!, colorSpace)
                ?.copy(Bitmap.Config.ARGB_8888, false)
            assertNotNull(bitmap)
            assertTrue(bitmap!!.isAllColor(Color.TRANSPARENT))
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.quit()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testCancelPending() {
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val executor = HandlerThreadExecutor("thread")
        var buffer: HardwareBuffer? = null
        val initialDrawLatch = CountDownLatch(1)

        var drawCancelledRequestLatch: CountDownLatch? = null
        val renderer = SingleBufferedCanvasRendererV34(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Int> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Int) {
                    canvas.drawColor(param)
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    syncFenceCompat?.awaitForever()
                    buffer = hardwareBuffer
                    initialDrawLatch.countDown()
                    drawCancelledRequestLatch?.countDown()
                }
            })
        try {
            renderer.render(Color.RED)
            assertTrue(initialDrawLatch.await(3000, TimeUnit.MILLISECONDS))

            drawCancelledRequestLatch = CountDownLatch(2)
            renderer.render(Color.BLUE)
            renderer.render(Color.BLACK)
            renderer.cancelPending()

            // Because the requests were cancelled this latch should not be signalled
            assertFalse(drawCancelledRequestLatch.await(1000, TimeUnit.MILLISECONDS))
            assertNotNull(buffer)
            val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
            val bitmap = Bitmap.wrapHardwareBuffer(buffer!!, colorSpace)
                ?.copy(Bitmap.Config.ARGB_8888, false)
            assertNotNull(bitmap)
            assertTrue(bitmap!!.isAllColor(Color.RED))
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.quit()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testMultiReleasesDoesNotCrash() {
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val executor = HandlerThreadExecutor("thread")
        val renderer = SingleBufferedCanvasRendererV34(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Void> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Void) {
                    // NO-OP
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    // NO-OP
                }
            })
        try {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.quit()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
            renderer.release(true)
        } finally {
            if (!executor.isRunning) {
                executor.quit()
            }
        }
    }

    @Test
    fun testRendererVisibleFlag() {
        var supportsNativeAndroidFence = false
        withEgl { eglManager ->
            supportsNativeAndroidFence = eglManager.supportsNativeAndroidFence()
        }
        if (!supportsNativeAndroidFence) {
            return
        }
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val executor = HandlerThreadExecutor("thread")
        var syncFenceNull = false
        var drawLatch: CountDownLatch? = null
        val renderer = SingleBufferedCanvasRendererV34(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Int> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Int) {
                    canvas.drawColor(param)
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    syncFenceNull = syncFenceCompat == null
                    syncFenceCompat?.awaitForever()
                    drawLatch?.countDown()
                }
            })
        try {
            renderer.isVisible = false
            drawLatch = CountDownLatch(1)
            renderer.render(Color.RED)
            assertTrue(drawLatch.await(3000, TimeUnit.MILLISECONDS))
            assertFalse(syncFenceNull)

            renderer.isVisible = true
            drawLatch = CountDownLatch(1)
            renderer.render(Color.BLUE)
            assertTrue(drawLatch.await(3000, TimeUnit.MILLISECONDS))
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.quit()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun testCancelMidRender() {
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val cancelLatch = CountDownLatch(1)
        val renderStartLatch = CountDownLatch(1)
        val bufferLatch = CountDownLatch(1)
        var bufferRenderCancelled = false
        val executor = HandlerThreadExecutor("thread")
        val renderer = SingleBufferedCanvasRendererV34(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Int> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Int) {
                    renderStartLatch.countDown()
                    cancelLatch.await(3000, TimeUnit.MILLISECONDS)
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    // NO-OP
                }

                override fun onBufferCancelled(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    bufferRenderCancelled = true
                    bufferLatch.countDown()
                }
            })
        try {
            renderer.render(Color.RED)
            renderStartLatch.await(3000, TimeUnit.MILLISECONDS)
            renderer.cancelPending()
            cancelLatch.countDown()
            bufferLatch.await(3000, TimeUnit.MILLISECONDS)
            assertTrue(bufferRenderCancelled)
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(false) {
                executor.quit()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    private fun testRenderWithTransform(
        transform: Int,
        actualColors: RectColors,
        expectedColors: RectColors
    ) {
        val transformer = BufferTransformer()
        transformer.computeTransform(TEST_WIDTH, TEST_HEIGHT, transform)
        val executor = HandlerThreadExecutor("thread")
        var buffer: HardwareBuffer? = null
        val renderLatch = CountDownLatch(1)
        val renderer = SingleBufferedCanvasRendererV34(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Int> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Int) {
                    drawSquares(
                        canvas,
                        width,
                        height,
                        actualColors.topLeft,
                        actualColors.topRight,
                        actualColors.bottomLeft,
                        actualColors.bottomRight
                    )
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    syncFenceCompat?.awaitForever()
                    buffer = hardwareBuffer
                    renderLatch.countDown()
                }
            })
        try {
            renderer.render(0)
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))
            assertNotNull(buffer)
            val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
            val bitmap = Bitmap.wrapHardwareBuffer(buffer!!, colorSpace)
                ?.copy(Bitmap.Config.ARGB_8888, false)
            assertNotNull(bitmap)
            bitmap!!.verifyQuadrants(
                expectedColors.topLeft,
                expectedColors.topRight,
                expectedColors.bottomLeft,
                expectedColors.bottomRight
            )
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.quit()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }
}
