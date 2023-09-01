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

package androidx.camera.effects.internal

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.fakes.FakeCamera
import androidx.core.util.Consumer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [SurfaceProcessorImpl].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class SurfaceProcessorImplDeviceTest {

    companion object {
        private val SIZE = Size(640, 480)

        // The timeout is set to 200ms to qualify for @SmallTest.
        private const val TIMEOUT_MILLIS = 200L
    }

    private lateinit var surfaceRequest: SurfaceRequest
    private lateinit var outputTexture: SurfaceTexture
    private lateinit var outputSurface: Surface
    private lateinit var outputTexture2: SurfaceTexture
    private lateinit var outputSurface2: Surface
    private lateinit var surfaceOutput: SurfaceOutput
    private lateinit var surfaceOutput2: SurfaceOutput
    private lateinit var processor: SurfaceProcessorImpl

    @Before
    fun setUp() {
        surfaceRequest = SurfaceRequest(SIZE, FakeCamera()) {}
        outputTexture = SurfaceTexture(0)
        outputTexture.detachFromGLContext()
        outputSurface = Surface(outputTexture)
        outputTexture2 = SurfaceTexture(1)
        outputTexture2.detachFromGLContext()
        outputSurface2 = Surface(outputTexture2)
        surfaceOutput = SurfaceOutputImpl(outputSurface)
        surfaceOutput2 = SurfaceOutputImpl(outputSurface2)
    }

    @After
    fun tearDown() {
        outputTexture.release()
        outputSurface.release()
        outputTexture2.release()
        outputSurface2.release()
        if (::processor.isInitialized) {
            processor.release()
        }
    }

    @Test
    fun zeroQueueDepth_inputDrawnToOutput() = runBlocking {
        // Assert: output receives frame when queue depth == 0.
        val latch = fillFramesAndWaitForOutput(0, 1)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun nonZeroQueueDepth_inputNotDrawnToOutputBeforeFilledUp() = runBlocking {
        // Assert: output does not receive frame when frame count = queue depth.
        val latch = fillFramesAndWaitForOutput(3, 3)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun nonZeroQueueDepth_inputDrawnToOutputAfterFilledUp() = runBlocking {
        // Assert: output receives frame when frame count > queue depth.
        val latch = fillFramesAndWaitForOutput(3, 4)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun replaceOutputSurface_noFrameFromPreviousCycle() = runBlocking {
        // Arrange: setup processor with buffer depth == 1 and fill it full.
        processor = SurfaceProcessorImpl(1)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onInputSurface(surfaceRequest)
            processor.onOutputSurface(surfaceOutput)
        }
        val inputSurface = surfaceRequest.deferrableSurface.surface.get()
        drawSurface(inputSurface)

        // Act: replace output surface so the cached frame is no longer valid. The cached frame
        // should be marked empty and not blocking the pipeline.
        val countDownLatch = CountDownLatch(1)
        outputTexture2.setOnFrameAvailableListener {
            countDownLatch.countDown()
        }
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onOutputSurface(surfaceOutput2)
        }

        // Assert: draw the input surface twice and the output surface should receive a frame. It
        // confirms that there is no frame from the previous cycle blocking the pipeline.
        drawSurface(inputSurface)
        drawSurface(inputSurface)
        assertThat(countDownLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    /**
     * Creates a processor and draws frames to the input surface.
     *
     * @param queueDepth The queue depth of the processor.
     * @param frameCount The number of frames to draw.
     * @return True if the output surface receives a frame.
     */
    private suspend fun fillFramesAndWaitForOutput(
        queueDepth: Int,
        frameCount: Int
    ): CountDownLatch {
        // Arrange: Create a processor.
        processor = SurfaceProcessorImpl(queueDepth)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onInputSurface(surfaceRequest)
            processor.onOutputSurface(surfaceOutput)
        }
        val countDownLatch = CountDownLatch(1)
        outputTexture.setOnFrameAvailableListener {
            countDownLatch.countDown()
        }

        // Act: Draw frames to the input surface.
        val inputSurface = surfaceRequest.deferrableSurface.surface.get()

        repeat(frameCount) {
            drawSurface(inputSurface)
        }

        return countDownLatch
    }

    /**
     * Draws a frame to the surface and block the thread until the gl thread finishes processing.
     */
    private suspend fun drawSurface(surface: Surface) {
        val canvas = surface.lockCanvas(null)
        surface.unlockCanvasAndPost(canvas)
        // Drain the GL thread to ensure the processor caches or draws the frame. Otherwise, the
        // input SurfaceTexture's onSurfaceAvailable callback may only get called once for
        // multiple drawings.
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
        }
    }

    private class SurfaceOutputImpl(private val surface: Surface) : SurfaceOutput {

        override fun close() {
        }

        override fun getSurface(
            executor: Executor,
            listener: Consumer<SurfaceOutput.Event>
        ): Surface {
            return surface
        }

        override fun getTargets(): Int {
            return PREVIEW or VIDEO_CAPTURE
        }

        override fun getSize(): Size {
            return SIZE
        }

        override fun updateTransformMatrix(updated: FloatArray, original: FloatArray) {
        }
    }
}
