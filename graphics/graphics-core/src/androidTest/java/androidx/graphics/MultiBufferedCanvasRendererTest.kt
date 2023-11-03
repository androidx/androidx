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
import android.graphics.Color
import android.graphics.ColorSpace
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.BufferTransformHintResolver
import androidx.graphics.lowlatency.BufferTransformer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiBufferedCanvasRendererTest {

    companion object {
        const val TEST_WIDTH = 20
        const val TEST_HEIGHT = 20
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderFrameInvokesCallback() {
        val executor = Executors.newSingleThreadExecutor()
        val renderer = MultiBufferedCanvasRenderer(TEST_WIDTH, TEST_HEIGHT).apply {
            record { canvas ->
                canvas.drawColor(Color.RED)
            }
        }
        try {
            val renderLatch = CountDownLatch(1)
            renderer.renderFrame(executor) { _, _ ->
                renderLatch.countDown()
            }
            assertTrue(renderLatch.await(1000, TimeUnit.MILLISECONDS))
        } finally {
            renderer.release()
            executor.shutdownNow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderAfterReleaseDoesNotRender() {
        val executor = Executors.newSingleThreadExecutor()
        val renderer = MultiBufferedCanvasRenderer(TEST_WIDTH, TEST_HEIGHT).apply {
            record { canvas -> canvas.drawColor(Color.RED) }
        }
        try {
            val renderLatch = CountDownLatch(1)
            renderer.release()
            renderer.renderFrame(executor) { _, _ ->
                renderLatch.countDown()
            }
            assertFalse(renderLatch.await(1000, TimeUnit.MILLISECONDS))
        } finally {
            executor.shutdownNow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testMultiReleasesDoesNotCrash() {
        val renderer = MultiBufferedCanvasRenderer(TEST_WIDTH, TEST_HEIGHT).apply {
            record { canvas -> canvas.drawColor(Color.RED) }
        }
        renderer.release()
        renderer.release()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputUnknownTransformWide() {
        verifyRenderOutput(
            TEST_WIDTH * 2,
            TEST_HEIGHT,
            BufferTransformHintResolver.UNKNOWN_TRANSFORM,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.BLUE
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputUnknownTransformTall() {
        verifyRenderOutput(
            TEST_WIDTH,
            TEST_HEIGHT * 2,
            BufferTransformHintResolver.UNKNOWN_TRANSFORM,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.BLUE
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputIdentityTransformWide() {
        verifyRenderOutput(
            TEST_WIDTH * 2,
            TEST_HEIGHT,
            SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.BLUE
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputIdentityTransformTall() {
        verifyRenderOutput(
            TEST_WIDTH,
            TEST_HEIGHT * 2,
            SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.BLUE
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputRotate90Wide() {
        verifyRenderOutput(
            TEST_WIDTH * 2,
            TEST_HEIGHT,
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90,
            Color.YELLOW,
            Color.BLUE,
            Color.RED,
            Color.GREEN
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputRotate90tall() {
        verifyRenderOutput(
            TEST_WIDTH,
            TEST_HEIGHT * 2,
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90,
            Color.YELLOW,
            Color.BLUE,
            Color.RED,
            Color.GREEN
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputRotate180Wide() {
        verifyRenderOutput(
            TEST_WIDTH * 2,
            TEST_HEIGHT,
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180,
            Color.BLUE,
            Color.GREEN,
            Color.YELLOW,
            Color.RED
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputRotate180Tall() {
        verifyRenderOutput(
            TEST_WIDTH,
            TEST_HEIGHT * 2,
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180,
            Color.BLUE,
            Color.GREEN,
            Color.YELLOW,
            Color.RED
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputRotate270Wide() {
        verifyRenderOutput(
            TEST_WIDTH * 2,
            TEST_HEIGHT,
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270,
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            Color.YELLOW
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutputRotate270Tall() {
        verifyRenderOutput(
            TEST_WIDTH,
            TEST_HEIGHT * 2,
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270,
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            Color.YELLOW
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun verifyRenderOutput(
        width: Int,
        height: Int,
        transform: Int,
        topLeft: Int,
        topRight: Int,
        bottomLeft: Int,
        bottomRight: Int
    ) {
        val executor = Executors.newSingleThreadExecutor()
        val renderer = MultiBufferedCanvasRenderer(
            width,
            height,
            BufferTransformer().apply {
                computeTransform(width, height, transform)
            }
        ).apply {
            record { canvas ->
                drawSquares(
                    canvas,
                    width,
                    height,
                    Color.RED,
                    Color.YELLOW,
                    Color.GREEN,
                    Color.BLUE
                )
            }
        }
        try {
            val renderLatch = CountDownLatch(1)
            var bitmap: Bitmap? = null
            renderer.renderFrame(executor) { buffer, fence ->
                fence?.awaitForever()
                fence?.close()
                val colorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
                bitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                renderLatch.countDown()
            }
            assertTrue(renderLatch.await(1000, TimeUnit.MILLISECONDS))
            assertNotNull(bitmap)
            bitmap!!.verifyQuadrants(topLeft, topRight, bottomLeft, bottomRight)
        } finally {
            renderer.release()
            executor.shutdownNow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRendererBlocksOnBufferRelease() {
        val renderer = MultiBufferedCanvasRenderer(
            TEST_WIDTH,
            TEST_HEIGHT,
            maxImages = 2
        ).apply {
            record { canvas -> canvas.drawColor(Color.RED) }
        }
        val executor = Executors.newSingleThreadExecutor()
        try {
            val latch1 = CountDownLatch(1)
            val latch2 = CountDownLatch(1)
            val latch3 = CountDownLatch(1)
            var hardwareBuffer: HardwareBuffer? = null
            renderer.renderFrame(executor) { buffer, fence ->
                fence?.awaitForever()
                fence?.close()
                hardwareBuffer = buffer
                latch1.countDown()
            }
            assertTrue(latch1.await(1000, TimeUnit.MILLISECONDS))

            renderer.record { canvas -> canvas.drawColor(Color.BLUE) }

            renderer.renderFrame(executor) { _, _ -> latch2.countDown() }

            assertTrue(latch2.await(1000, TimeUnit.MILLISECONDS))

            renderer.record { canvas -> canvas.drawColor(Color.GREEN) }

            renderer.renderFrame(executor) { _, _ -> latch3.countDown() }

            // The 3rd render request should be blocked until the buffer is released
            assertFalse(latch3.await(1000, TimeUnit.MILLISECONDS))
            assertNotNull(hardwareBuffer)
            renderer.releaseBuffer(hardwareBuffer!!, null)
            assertTrue(latch3.await(1000, TimeUnit.MILLISECONDS))
        } finally {
            renderer.release()
            executor.shutdownNow()
        }
    }
}
