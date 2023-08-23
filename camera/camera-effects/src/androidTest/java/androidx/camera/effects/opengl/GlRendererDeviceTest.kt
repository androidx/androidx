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

package androidx.camera.effects.opengl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [GlRenderer].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class GlRendererDeviceTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private const val TIMESTAMP_NS = 0L
    }

    private val input = createBitmap(WIDTH, HEIGHT)
    private val overlay = createOverlayBitmap()
    private val transparentOverlay = createTransparentOverlay()

    private val glRenderer = GlRenderer()
    private lateinit var inputSurface: Surface
    private lateinit var inputTexture: SurfaceTexture
    private lateinit var inputExecutor: ExecutorService

    private lateinit var outputSurface: Surface
    private lateinit var outputTexture: SurfaceTexture

    private val identityMatrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }

    @Before
    fun setUp() = runBlocking {
        inputExecutor = Executors.newSingleThreadExecutor()
        withContext(inputExecutor.asCoroutineDispatcher()) {
            glRenderer.init()
            inputTexture = SurfaceTexture(glRenderer.inputTextureId).apply {
                setDefaultBufferSize(WIDTH, HEIGHT)
            }
            inputSurface = Surface(inputTexture)
        }
        outputTexture = SurfaceTexture(0).apply {
            setDefaultBufferSize(WIDTH, HEIGHT)
        }
        outputSurface = Surface(outputTexture)
    }

    @After
    fun tearDown() {
        inputExecutor.execute {
            glRenderer.release()
            inputTexture.release()
            inputSurface.release()
        }
        outputTexture.release()
        outputSurface.release()
        inputExecutor.shutdown()
    }

    @Test(expected = IllegalStateException::class)
    fun renderInputWhenUninitialized_throwsException() {
        val glRenderer = GlRenderer()
        try {
            glRenderer.renderInputToSurface(TIMESTAMP_NS, identityMatrix, outputSurface)
        } finally {
            glRenderer.release()
        }
    }

    @Test
    fun drawInputToQueue_snapshot() = runBlocking(inputExecutor.asCoroutineDispatcher()) {
        // Arrange: upload a overlay and create a texture queue.
        glRenderer.uploadOverlay(overlay)
        drawInputSurface(input)
        val queue = glRenderer.createBufferTextureIds(1, Size(WIDTH, HEIGHT))
        // Act: draw input to the queue and then to the output.
        glRenderer.renderInputToQueueTexture(queue[0])
        val bitmap =
            glRenderer.renderQueueTextureToBitmap(queue[0], WIDTH, HEIGHT, identityMatrix)
        // Assert: the output is the input with overlay.
        assertOverlayColor(bitmap)
    }

    @Test
    fun drawInputWithoutOverlay_snapshot() = runBlocking(inputExecutor.asCoroutineDispatcher()) {
        // Arrange: upload a transparent overlay.
        glRenderer.uploadOverlay(transparentOverlay)
        drawInputSurface(input)
        // Act.
        val output = glRenderer.renderInputToBitmap(WIDTH, HEIGHT, identityMatrix)
        // Assert: the output is the same as the input.
        assertThat(getAverageDiff(output, input)).isEqualTo(0)
    }

    /**
     * Tests that the input is rendered to the output surface with the overlay.
     */
    private fun assertOverlayColor(bitmap: Bitmap) {
        // Top left quadrant is white.
        assertThat(
            getAverageDiff(
                bitmap,
                Rect(0, 0, WIDTH / 2, HEIGHT / 2),
                Color.WHITE
            )
        ).isEqualTo(0)
        assertThat(
            getAverageDiff(
                bitmap,
                Rect(WIDTH / 2, 0, WIDTH, HEIGHT / 2),
                Color.GREEN
            )
        ).isEqualTo(0)
        assertThat(
            getAverageDiff(
                bitmap,
                Rect(WIDTH / 2, HEIGHT / 2, WIDTH, HEIGHT),
                Color.YELLOW
            )
        ).isEqualTo(0)
        assertThat(
            getAverageDiff(
                bitmap,
                Rect(0, HEIGHT / 2, WIDTH / 2, HEIGHT),
                Color.BLUE
            )
        ).isEqualTo(0)
    }

    /**
     * Draws the bitmap to the input surface and waits for the frame to be available.
     */
    private suspend fun drawInputSurface(bitmap: Bitmap) {
        val deferredOnFrameAvailable = CompletableDeferred<Unit>()
        inputTexture.setOnFrameAvailableListener({
            deferredOnFrameAvailable.complete(Unit)
        }, Handler(Looper.getMainLooper()))

        // Draw bitmap to inputSurface.
        val canvas = inputSurface.lockCanvas(null)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        inputSurface.unlockCanvasAndPost(canvas)

        // Wait for frame available and update texture.
        withTimeoutOrNull(5_000) {
            deferredOnFrameAvailable.await()
        } ?: Assert.fail("Timed out waiting for SurfaceTexture frame available.")
        inputTexture.updateTexImage()
    }

    /**
     * Creates a bitmap with a white top-left quadrant.
     */
    private fun createOverlayBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val centerX = (WIDTH / 2).toFloat()
        val centerY = (HEIGHT / 2).toFloat()

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, centerX, centerY, paint)
        return bitmap
    }

    /**
     * Creates a transparent bitmap.
     */
    private fun createTransparentOverlay(): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        return bitmap
    }
}
