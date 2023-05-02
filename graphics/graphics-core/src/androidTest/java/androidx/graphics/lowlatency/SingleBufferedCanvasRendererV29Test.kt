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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.drawSquares
import androidx.graphics.isAllColor
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_IDENTITY
import androidx.graphics.verifyQuadrants
import androidx.graphics.withEgl
import androidx.hardware.SyncFenceCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SingleBufferedCanvasRendererV29Test {

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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testClearRenderer() {
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val executor = Executors.newSingleThreadExecutor()
        val firstRenderLatch = CountDownLatch(1)
        val clearLatch = CountDownLatch(2)
        var buffer: HardwareBuffer? = null
        val renderer = SingleBufferedCanvasRendererV29(
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
            }).apply {
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                // attachments are not executed until a glReadPixels call is made
                forceFlush.set(true)
            }
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
                executor.shutdownNow()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testCancelPending() {
        val transformer = BufferTransformer().apply {
            computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        }
        val executor = Executors.newSingleThreadExecutor()
        var buffer: HardwareBuffer? = null
        val initialDrawLatch = CountDownLatch(1)
        val bufferReadyLatch = CountDownLatch(1)
        val waitForRequestLatch = CountDownLatch(1)

        var drawCancelledRequestLatch: CountDownLatch? = null
        val renderer = SingleBufferedCanvasRendererV29(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Int> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Int) {
                    canvas.drawColor(param)
                    initialDrawLatch.countDown()
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    syncFenceCompat?.awaitForever()
                    buffer = hardwareBuffer
                    bufferReadyLatch.countDown()
                    drawCancelledRequestLatch?.countDown()
                }
            }).apply {
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                // attachments are not executed until a glReadPixels call is made
                forceFlush.set(true)
            }
        try {
            renderer.render(Color.RED)
            assertTrue(initialDrawLatch.await(3000, TimeUnit.MILLISECONDS))

            drawCancelledRequestLatch = CountDownLatch(2)
            renderer.render(Color.GREEN)
            renderer.render(Color.YELLOW)
            renderer.cancelPending()
            waitForRequestLatch.countDown()

            assertTrue(bufferReadyLatch.await(3000, TimeUnit.MILLISECONDS))
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
                executor.shutdownNow()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiReleasesDoesNotCrash() {
        val transformer = BufferTransformer()
        val executor = Executors.newSingleThreadExecutor()
        val renderer = SingleBufferedCanvasRendererV29(
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
            }).apply {
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                // attachments are not executed until a glReadPixels call is made
                forceFlush.set(true)
            }
        try {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.shutdownNow()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
            renderer.release(true)
        } finally {
            if (!executor.isShutdown) {
                executor.shutdownNow()
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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
        val executor = Executors.newSingleThreadExecutor()
        var syncFenceNull = false
        var drawLatch: CountDownLatch? = null
        val renderer = SingleBufferedCanvasRendererV29(
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
            }).apply {
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                // attachments are not executed until a glReadPixels call is made
                forceFlush.set(true)
            }
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
            assertTrue(syncFenceNull)
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(true) {
                executor.shutdownNow()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testBatchedRenders() {
        val transformer = BufferTransformer()
        transformer.computeTransform(TEST_WIDTH, TEST_HEIGHT, BUFFER_TRANSFORM_IDENTITY)
        val executor = Executors.newSingleThreadExecutor()
        val renderCount = AtomicInteger(0)
        val renderer = SingleBufferedCanvasRendererV29(
            TEST_WIDTH,
            TEST_HEIGHT,
            transformer,
            executor,
            object : SingleBufferedCanvasRenderer.RenderCallbacks<Int> {
                override fun render(canvas: Canvas, width: Int, height: Int, param: Int) {
                    canvas.drawColor(param)
                    renderCount.incrementAndGet()
                }

                override fun onBufferReady(
                    hardwareBuffer: HardwareBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    // NO-OP
                }
            }).apply {
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                // attachments are not executed until a glReadPixels call is made
                forceFlush.set(true)
            }
        try {
            renderer.render(Color.RED)
            renderer.render(Color.BLUE)
            renderer.render(Color.YELLOW)
        } finally {
            val latch = CountDownLatch(1)
            renderer.release(false) {
                executor.shutdownNow()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
            assertEquals(3, renderCount.get())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun testRenderWithTransform(
        transform: Int,
        actualColors: RectColors,
        expectedColors: RectColors
    ) {
        val transformer = BufferTransformer()
        transformer.computeTransform(TEST_WIDTH, TEST_HEIGHT, transform)
        val executor = Executors.newSingleThreadExecutor()
        var buffer: HardwareBuffer? = null
        val renderLatch = CountDownLatch(1)
        val renderer = SingleBufferedCanvasRendererV29(
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
            }).apply {
                // See: b/236394768 Workaround for ANGLE issue where FBOs with HardwareBuffer
                // attachments are not executed until a glReadPixels call is made
                forceFlush.set(true)
            }
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
                executor.shutdownNow()
                latch.countDown()
            }
            assertTrue(latch.await(3000, TimeUnit.MILLISECONDS))
        }
    }
}