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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.utils.TransformUtils.sizeToRect
import androidx.camera.effects.Frame
import androidx.camera.effects.OverlayEffect
import androidx.camera.effects.internal.Utils.lockCanvas
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.core.util.Consumer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumentation tests for [SurfaceProcessorImpl]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class SurfaceProcessorImplDeviceTest {

    companion object {
        private const val ROTATION_DEGREES = 90
        private val TRANSFORM = Matrix().apply { postRotate(90F) }
        private const val INPUT_COLOR = Color.GREEN
        private const val OVERLAY_COLOR = Color.RED

        // The timeout is set to 200ms to qualify for @SmallTest.
        private const val TIMEOUT_MILLIS = 200L
        private const val THREAD_NAME = "GL_THREAD"
    }

    private val size = Size(640, 480)
    private val cropRect = sizeToRect(size)
    private lateinit var surfaceRequest: SurfaceRequest
    private lateinit var outputTexture: SurfaceTexture
    private lateinit var outputSurface: Surface
    private lateinit var outputTexture2: SurfaceTexture
    private lateinit var outputSurface2: Surface
    private lateinit var surfaceOutput: SurfaceOutput
    private lateinit var surfaceOutput2: SurfaceOutput
    private lateinit var processor: SurfaceProcessorImpl
    private lateinit var transformationInfo: TransformationInfo
    private lateinit var glThread: HandlerThread
    private lateinit var glHandler: Handler

    @Before
    fun setUp() {
        glThread = HandlerThread(THREAD_NAME)
        glThread.start()
        glHandler = Handler(glThread.looper)

        transformationInfo =
            TransformationInfo.of(
                cropRect,
                ROTATION_DEGREES,
                Surface.ROTATION_90,
                true,
                TRANSFORM,
                true,
            )
        surfaceRequest = SurfaceRequest(size, FakeCamera()) {}
        surfaceRequest.updateTransformationInfo(transformationInfo)
        outputTexture = SurfaceTexture(0)
        outputTexture.detachFromGLContext()
        outputSurface = Surface(outputTexture)
        outputTexture2 = SurfaceTexture(1)
        outputTexture2.detachFromGLContext()
        outputSurface2 = Surface(outputTexture2)
        surfaceOutput = SurfaceOutputImpl(outputSurface, size)
        surfaceOutput2 = SurfaceOutputImpl(outputSurface2, size)
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
        glThread.quitSafely()
    }

    @Test
    fun onDrawListenerReturnsFalse_notDrawnToOutput() = runBlocking {
        // Act: return false in the on draw listener.
        val latch = fillFramesAndWaitForOutput(0, 1) { it.setOnDrawListener { false } }
        // Assert: output is not drawn.
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun onDrawListener_receivesTransformationInfo() = runBlocking {
        // Arrange.
        var frameReceived: Frame? = null
        // Act: fill frames and wait draw frame listener.
        val latch =
            fillFramesAndWaitForOutput(0, 1) { processor ->
                processor.setOnDrawListener { frame ->
                    frameReceived = frame
                    true
                }
            }
        // Assert: draw frame listener receives correct transformation info.
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(frameReceived!!.size).isEqualTo(size)
        assertThat(frameReceived!!.cropRect).isEqualTo(transformationInfo.cropRect)
        assertThat(frameReceived!!.isMirroring).isEqualTo(transformationInfo.isMirroring)
        assertThat(frameReceived!!.sensorToBufferTransform)
            .isEqualTo(transformationInfo.sensorToBufferTransform)
        assertThat(frameReceived!!.rotationDegrees).isEqualTo(ROTATION_DEGREES)
    }

    @Test
    fun getOverlayCanvas_overlayDrawnToOutput(): Unit = runBlocking {
        val latch =
            fillFramesAndWaitForOutput(0, 1) { processor ->
                processor.setOnDrawListener { frame ->
                    // Act: get the overlay canvas and draw color.
                    frame.getOverlayCanvas().drawColor(OVERLAY_COLOR)
                    true
                }
            }
        // Assert: output receives frame with overlay color.
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertOutputColor(OVERLAY_COLOR)
    }

    @Test
    fun doNotGetOverlayCanvas_overlayNotDrawnToOutput() = runBlocking {
        var canvas: Canvas? = null
        val latch =
            fillFramesAndWaitForOutput(0, 1) { processor ->
                processor.setOnDrawListener { _ ->
                    // Act: draw color on overlay canvas without getting it.
                    canvas = lockCanvas(processor.overlaySurface)
                    canvas!!.drawColor(OVERLAY_COLOR)
                    true
                }
            }
        // Assert: output receives frame with input color
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertOutputColor(INPUT_COLOR)
        // Cleanup: unlock Canvas
        processor.overlaySurface.unlockCanvasAndPost(canvas!!)
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
        processor = SurfaceProcessorImpl(1, glHandler)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onInputSurface(surfaceRequest)
            processor.onOutputSurface(surfaceOutput)
        }
        val inputSurface = surfaceRequest.deferrableSurface.surface.get()
        drawSurface(inputSurface)

        // Act: replace output surface so the cached frame is no longer valid. The cached frame
        // should be marked empty and not blocking the pipeline.
        val countDownLatch = getTextureUpdateLatch(outputTexture2)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onOutputSurface(surfaceOutput2)
        }

        // Assert: draw the input surface twice and the output surface should receive a frame. It
        // confirms that there is no frame from the previous cycle blocking the pipeline.
        drawSurface(inputSurface)
        drawSurface(inputSurface)
        assertThat(countDownLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun drawCachedFrame_frameDrawnToOutput() = runBlocking {
        // Arrange: draw the input and get the cached frame.
        val latch = fillFramesAndWaitForOutput(1, 1)
        val cachedFrame = processor.buffer.frames.single()

        // Act: draw the cached frame.
        val drawFuture = processor.drawFrameAsync(cachedFrame.timestampNanos)

        // Assert: the future completes with RESULT_SUCCESS and the output receives the frame.
        assertThat(drawFuture.get()).isEqualTo(OverlayEffect.RESULT_SUCCESS)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun drawMissingFrame_futureCompletesWithNotFound() = runBlocking {
        // Arrange: draw the input and get the cached frame.
        val latch = fillFramesAndWaitForOutput(1, 1)
        val frame = processor.buffer.frames.single()

        // Act: draw the frame with a wrong timestamp.
        val drawFuture = processor.drawFrameAsync(frame.timestampNanos - 1)

        // Assert: the future completes with RESULT_FRAME_NOT_FOUND and the output does not receive
        // the frame.
        assertThat(drawFuture.get()).isEqualTo(OverlayEffect.RESULT_FRAME_NOT_FOUND)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun drawFrameAndCancel_futureCompletesWithCanceled() = runBlocking {
        // Arrange: draw the input and drop the incoming frames.
        val latch = fillFramesAndWaitForOutput(1, 1) { it.setOnDrawListener { false } }
        val frame = processor.buffer.frames.single()

        // Act: draw the frame.
        val drawFuture = processor.drawFrameAsync(frame.timestampNanos)

        // Assert: the future completes with RESULT_CANCELLED_BY_CALLER and the output does not
        // receive the frame.
        assertThat(drawFuture.get()).isEqualTo(OverlayEffect.RESULT_CANCELLED_BY_CALLER)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun drawFrameAfterReplacingOutput_futureCompletesWithInvalidSurface() = runBlocking {
        // Arrange: setup processor with buffer depth == 1 and fill it full.
        processor = SurfaceProcessorImpl(1, glHandler)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onInputSurface(surfaceRequest)
            processor.onOutputSurface(surfaceOutput)
        }
        val inputSurface = surfaceRequest.deferrableSurface.surface.get()
        drawSurface(inputSurface)
        val frame = processor.buffer.frames.single()

        // Arrange: replace the output Surface so the buffered frame is associated with an invalid
        // surface.
        val latch = getTextureUpdateLatch(outputTexture2)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onOutputSurface(surfaceOutput2)
        }

        // Act: draw the buffered frame.
        val drawFuture = processor.drawFrameAsync(frame.timestampNanos)

        // Assert: the future completes with RESULT_INVALID_SURFACE and the output does not
        // receive the frame.
        assertThat(drawFuture.get()).isEqualTo(OverlayEffect.RESULT_INVALID_SURFACE)
        assertThat(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun drawFrameAfterRelease_futureCompletesWithException(): Unit = runBlocking {
        // Arrange: draw the input and get the cached frame.
        fillFramesAndWaitForOutput(1, 1)
        processor.release()

        // Act: release the processor and draw a frame.
        val drawFuture = processor.drawFrameAsync(0)

        // Assert: the future completes with an exception.
        try {
            drawFuture.get()
        } catch (e: ExecutionException) {
            assertThat(e.cause).isInstanceOf(IllegalStateException::class.java)
        }
    }

    /** Renders the input surface to a bitmap and asserts that the color of the bitmap. */
    private suspend fun assertOutputColor(color: Int) {
        val matrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(matrix, 0)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            val bitmap =
                processor.glRendererForTesting.renderInputToBitmap(size.width, size.height, matrix)
            assertThat(getAverageDiff(bitmap, Rect(0, 0, size.width, size.height), color))
                .isEqualTo(0)
        }
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
        frameCount: Int,
        configureProcessor: (SurfaceProcessorImpl) -> Unit = {},
    ): CountDownLatch {
        // Arrange: Create a processor.
        processor = SurfaceProcessorImpl(queueDepth, glHandler)
        configureProcessor(processor)
        withContext(processor.glExecutor.asCoroutineDispatcher()) {
            processor.onInputSurface(surfaceRequest)
            processor.onOutputSurface(surfaceOutput)
        }
        val countDownLatch = getTextureUpdateLatch(outputTexture)

        // Act: Draw frames to the input surface.
        val inputSurface = surfaceRequest.deferrableSurface.surface.get()

        repeat(frameCount) { drawSurface(inputSurface) }

        return countDownLatch
    }

    /**
     * Draws a frame to the surface and block the thread until the gl thread finishes processing.
     */
    private suspend fun drawSurface(surface: Surface) {
        val canvas = surface.lockCanvas(null)
        canvas.drawColor(INPUT_COLOR)
        surface.unlockCanvasAndPost(canvas)
        // Drain the GL thread to ensure the processor caches or draws the frame. Otherwise, the
        // input SurfaceTexture's onSurfaceAvailable callback may only get called once for
        // multiple drawings.
        withContext(processor.glExecutor.asCoroutineDispatcher()) {}
    }

    private fun getTextureUpdateLatch(surfaceTexture: SurfaceTexture): CountDownLatch {
        val countDownLatch = CountDownLatch(1)
        surfaceTexture.setOnFrameAvailableListener { countDownLatch.countDown() }
        return countDownLatch
    }

    private class SurfaceOutputImpl(private val surface: Surface, val surfaceSize: Size) :
        SurfaceOutput {

        override fun close() {}

        override fun getSurface(
            executor: Executor,
            listener: Consumer<SurfaceOutput.Event>,
        ): Surface {
            return surface
        }

        override fun getTargets(): Int {
            return PREVIEW or VIDEO_CAPTURE
        }

        override fun getSize(): Size {
            return surfaceSize
        }

        override fun updateTransformMatrix(updated: FloatArray, original: FloatArray) {}
    }
}
