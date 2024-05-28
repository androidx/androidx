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
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.RenderNode
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.CanvasBufferedRenderer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * For some devices, setting [HardwareRenderer.isOpaque] to true with [ImageReader.getMaxImages] set
 * to 1 will end up preserving the contents of the buffers across renders. For low latency rendering
 * this is ideal in order to only draw the deltas of content across render requests. However, not
 * all devices support this optimization and will clear the contents regardless before each render
 * request. This class is used to verify if the device does support preservation of contents across
 * renders and can be used to signal for a fallback solution that will re-render the scene before
 * proceeding for consistency.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class PreservedBufferContentsVerifier {

    private val executor = Executors.newSingleThreadExecutor()
    private val paint = Paint()

    private val renderNode =
        RenderNode("testNode").apply { setPosition(0, 0, TEST_WIDTH, TEST_HEIGHT) }

    private val multiBufferedRenderer =
        CanvasBufferedRenderer.Builder(TEST_WIDTH, TEST_HEIGHT)
            .setMaxBuffers(1)
            .setImpl(CanvasBufferedRenderer.USE_V29_IMPL_WITH_SINGLE_BUFFER)
            .build()
            .apply { setContentRoot(renderNode) }

    /**
     * Executes a test rendering to verify if contents are preserved across renders. This is
     * accomplished by the following steps:
     * 1) Issue an initial render that clears the contents and draws green on the left
     * 2) Issue an additional render that draws blue on the right side
     * 3) Draw red using the dst over blend mode to render contents underneath
     *
     * If this device does support preserving content the result will have green pixels on the left
     * hand side and blue on the right.
     *
     * If this device **does not** support preserving content, then the left hand side will be red
     * as the red pixels would be rendered underneath transparent content if the buffer was cleared
     * in advance.
     */
    fun supportsPreservedRenderedContent(): Boolean {
        var canvas = renderNode.beginRecording()
        // Ensure clear pixels before proceeding
        canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
        canvas.drawRect(
            0f,
            0f,
            TEST_WIDTH / 2f,
            TEST_HEIGHT.toFloat(),
            paint.apply { color = Color.GREEN }
        )
        renderNode.endRecording()

        val firstRenderLatch = CountDownLatch(1)
        multiBufferedRenderer.obtainRenderRequest().preserveContents(true).drawAsync(executor) { _
            ->
            firstRenderLatch.countDown()
        }

        firstRenderLatch.await()

        canvas = renderNode.beginRecording()
        canvas.drawRect(
            TEST_WIDTH / 2f,
            0f,
            TEST_WIDTH.toFloat(),
            TEST_HEIGHT.toFloat(),
            paint.apply { color = Color.BLUE }
        )
        // Draw red underneath the existing content
        canvas.drawColor(Color.RED, BlendMode.DST_OVER)
        renderNode.endRecording()

        var bitmap: Bitmap? = null
        val secondRenderLatch = CountDownLatch(1)
        multiBufferedRenderer.obtainRenderRequest().preserveContents(true).drawAsync(executor) {
            result ->
            result.fence?.awaitForever()
            bitmap =
                Bitmap.wrapHardwareBuffer(
                    result.hardwareBuffer,
                    CanvasBufferedRenderer.DefaultColorSpace
                )
            secondRenderLatch.countDown()
        }
        secondRenderLatch.await()

        val hardwareBitmap = bitmap
        return if (hardwareBitmap != null) {
            val copyBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
            val result =
                copyBitmap.getPixel(0, 0) == Color.GREEN &&
                    copyBitmap.getPixel(TEST_WIDTH - 1, TEST_HEIGHT - 1) == Color.BLUE
            result
        } else {
            false
        }
    }

    fun release() {
        executor.shutdownNow()
        multiBufferedRenderer.close()
        renderNode.discardDisplayList()
    }

    companion object {
        const val TEST_WIDTH = 2
        const val TEST_HEIGHT = 2
    }
}
