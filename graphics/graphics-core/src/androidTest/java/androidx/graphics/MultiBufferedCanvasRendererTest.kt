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
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
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
        val renderNode = RenderNode("node").apply {
            setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
            val canvas = beginRecording()
            canvas.drawColor(Color.RED)
            endRecording()
        }
        val executor = Executors.newSingleThreadExecutor()
        val renderer = MultiBufferedCanvasRenderer(renderNode, TEST_WIDTH, TEST_HEIGHT)
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
        val renderNode = RenderNode("node").apply {
            setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
            val canvas = beginRecording()
            canvas.drawColor(Color.RED)
            endRecording()
        }
        val executor = Executors.newSingleThreadExecutor()
        val renderer = MultiBufferedCanvasRenderer(renderNode, TEST_WIDTH, TEST_HEIGHT)
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
        val renderNode = RenderNode("node").apply {
            setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
            val canvas = beginRecording()
            canvas.drawColor(Color.RED)
            endRecording()
        }
        val renderer = MultiBufferedCanvasRenderer(renderNode, TEST_WIDTH, TEST_HEIGHT)
        renderer.release()
        renderer.release()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderOutput() {
        val renderNode = RenderNode("node").apply {
            setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
            val canvas = beginRecording()
            drawSquares(
                canvas,
                TEST_WIDTH,
                TEST_HEIGHT,
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.BLUE
            )
            endRecording()
        }
        val executor = Executors.newSingleThreadExecutor()
        val renderer = MultiBufferedCanvasRenderer(renderNode, TEST_WIDTH, TEST_HEIGHT)
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
            bitmap!!.verifyQuadrants(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE)
        } finally {
            renderer.release()
            executor.shutdownNow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRendererBlocksOnBufferRelease() {
        val renderNode = RenderNode("node").apply {
            setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT)
            val canvas = beginRecording()
            canvas.drawColor(Color.RED)
            endRecording()
        }
        val renderer = MultiBufferedCanvasRenderer(
            renderNode,
            TEST_WIDTH,
            TEST_HEIGHT,
            maxImages = 2
        )
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

            var canvas = renderNode.beginRecording()
            canvas.drawColor(Color.BLUE)
            renderNode.endRecording()

            renderer.renderFrame(executor) { _, _ -> latch2.countDown() }

            assertTrue(latch2.await(1000, TimeUnit.MILLISECONDS))

            canvas = renderNode.beginRecording()
            canvas.drawColor(Color.GREEN)
            renderNode.endRecording()

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
