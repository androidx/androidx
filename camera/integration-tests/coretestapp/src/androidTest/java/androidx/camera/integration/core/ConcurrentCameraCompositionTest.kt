/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.processing.util.GLUtils
import androidx.camera.core.processing.util.GLUtils.InputFormat
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "ConcurrentCameraCompositionTest"

@LargeTest
@RunWith(AndroidJUnit4::class)
class ConcurrentCameraCompositionTest {
    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule val labTest: LabTestRule = LabTestRule()
    private lateinit var previewSize: Size
    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var glThread: HandlerThread
    private lateinit var glHandler: Handler

    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null
    private var textureId = -1
    private var shaderProgram: GLUtils.SamplerShaderProgram? = null

    @Before
    fun setUp(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        assumeFalse(
            "Test fails on cuttlefish b/467708340",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
        assumeFalse(
            "Test fails on FTL emulator",
            Build.MODEL.contains("sdk_gphone64_arm64", ignoreCase = true),
        )
        assumeTrue(
            "No concurrent cameras available",
            cameraProvider.availableConcurrentCameraInfos.isNotEmpty(),
        )

        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()

        glThread = HandlerThread("GLThread")
        glThread.start()
        glHandler = Handler(glThread.looper)
        initEGL()
    }

    private fun initEGL() {
        val semaphore = Semaphore(0)
        glHandler.post {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(eglDisplay, intArrayOf(0), 0, intArrayOf(0), 0)
            val configAttribs =
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE,
                    8,
                    EGL14.EGL_GREEN_SIZE,
                    8,
                    EGL14.EGL_BLUE_SIZE,
                    8,
                    EGL14.EGL_ALPHA_SIZE,
                    8,
                    EGL14.EGL_NONE,
                )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = intArrayOf(0)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            eglConfig = configs[0]

            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext =
                EGL14.eglCreateContext(
                    eglDisplay,
                    eglConfig!!,
                    EGL14.EGL_NO_CONTEXT,
                    contextAttribs,
                    0,
                )
            textureId = GLUtils.createTexture()
            semaphore.release()
        }
        semaphore.acquire()
    }

    private fun initPBuffer() {
        val semaphore = Semaphore(0)
        glHandler.post {
            // Make pbuffer large enough for forced resolution
            val pbufferAttribs =
                intArrayOf(
                    EGL14.EGL_WIDTH,
                    previewSize.width,
                    EGL14.EGL_HEIGHT,
                    previewSize.height,
                    EGL14.EGL_NONE,
                )
            eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig!!, pbufferAttribs, 0)

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            shaderProgram =
                GLUtils.SamplerShaderProgram(DynamicRange.SDR, InputFormat.DEFAULT, false)
            semaphore.release()
        }
        semaphore.acquire()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync().get(20, TimeUnit.SECONDS)
        }
        if (::glHandler.isInitialized) {
            val semaphore = Semaphore(0)
            glHandler.post {
                shaderProgram?.delete()
                if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT,
                    )
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    EGL14.eglTerminate(eglDisplay)
                }
                glThread.quitSafely()
                semaphore.release()
            }
            semaphore.acquire()
        }
    }

    private suspend fun getPreviewBitmapFromConcurrentCameraComposition(
        compositionBackCamera: CompositionSettings,
        compositionFrontCamera: CompositionSettings,
    ): Bitmap {
        val frontCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        val backCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        assumeTrue(CameraUtil.hasCameraWithLensFacing(frontCameraSelector.lensFacing!!))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(backCameraSelector.lensFacing!!))

        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val useCaseGroup =
            UseCaseGroup.Builder().addUseCase(preview).addUseCase(videoCapture).build()

        val primary =
            SingleCameraConfig(
                backCameraSelector,
                useCaseGroup,
                compositionBackCamera,
                fakeLifecycleOwner,
            )

        val secondary =
            SingleCameraConfig(
                frontCameraSelector,
                useCaseGroup,
                compositionFrontCamera,
                fakeLifecycleOwner,
            )

        val frameSemaphore = Semaphore(0)
        var surfaceTexture: SurfaceTexture? = null
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(CameraXExecutors.directExecutor()) { request ->
                surfaceTexture = SurfaceTexture(textureId)
                surfaceTexture!!.setDefaultBufferSize(
                    request.resolution.width,
                    request.resolution.height,
                )
                surfaceTexture!!.setOnFrameAvailableListener(
                    {
                        it.updateTexImage()
                        frameSemaphore.release()
                    },
                    glHandler,
                )
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, CameraXExecutors.directExecutor()) {
                    surface.release()
                    surfaceTexture!!.release()
                }
            }
            cameraProvider.bindToLifecycle(listOf(primary, secondary))
            previewSize = preview.attachedSurfaceResolution!!
            initPBuffer()
        }

        // Wait for enough frames to settle AE/AWB and composition.
        Log.d(TAG, "Waiting for frames...")
        val success = frameSemaphore.tryAcquire(15, 60, TimeUnit.SECONDS)
        if (!success) {
            Log.e(
                TAG,
                "Timeout waiting for frames. Received only ${frameSemaphore.availablePermits()} frames",
            )
        }
        assertThat(success).isTrue()
        // Capture pixels.
        return renderAndCapturePreviewBitmap(
            surfaceTexture!!,
            previewSize.width,
            previewSize.height,
        )
    }

    @Test
    fun composition_border_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                // Hide the first camera
                CompositionSettings.Builder().setAlpha(0f).build(),
                // Bottom-Right
                CompositionSettings.Builder()
                    .setAlpha(1.0f)
                    .setBorderColor(Color.GREEN)
                    .setBorderWidthRatio(0.1f)
                    .setOffset(0.5f, -0.5f)
                    .setScale(0.5f, 0.5f)
                    .build(),
            )

        // Assert.
        // Verify the four corners are border color(green)
        verifyPixelColor(bitmap, 0.01f, -0.01f, Color.GREEN)
        verifyPixelColor(bitmap, 0.99f, -0.01f, Color.GREEN)
        verifyPixelColor(bitmap, 0.99f, -0.99f, Color.GREEN)
        verifyPixelColor(bitmap, 0.01f, -0.99f, Color.GREEN)
        // verify the center is not border color
        verifyPixelColorNotMatched(bitmap, 0.75f, -0.75f, Color.GREEN)
    }

    @Test
    fun composition_roundedCorner_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                // Make the first camera a solid RED color
                CompositionSettings.Builder()
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1.0f) // make it a solid color
                    .build(),
                // Bottom-Right
                CompositionSettings.Builder()
                    .setAlpha(1.0f)
                    .setOffset(0.5f, -0.5f)
                    .setScale(0.5f, 0.5f)
                    .setRoundedCornerRatio(0.4f)
                    .build(),
            )

        // Assert.
        // Verify the 4 corners are clipped.
        verifyPixelColor(bitmap, 0.01f, -0.01f, Color.RED)
        verifyPixelColor(bitmap, 0.99f, -0.01f, Color.RED)
        verifyPixelColor(bitmap, 0.99f, -0.99f, Color.RED)
        verifyPixelColor(bitmap, 0.01f, -0.99f, Color.RED)
        // Verify the center is not clipped
        verifyPixelColorNotMatched(bitmap, 0.75f, -0.75f, Color.RED)
    }

    @Test
    fun composition_borderAndRoundedCorner_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                // Make the first camera a solid RED color
                CompositionSettings.Builder()
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1.0f) // make it a solid color
                    .build(),
                // Bottom-Right
                CompositionSettings.Builder()
                    .setAlpha(1.0f)
                    .setOffset(0.5f, -0.5f)
                    .setScale(0.5f, 0.5f)
                    .setBorderColor(Color.GREEN)
                    .setBorderWidthRatio(0.1f)
                    .setRoundedCornerRatio(0.5f)
                    .build(),
            )

        // Assert.
        // Verify Border in top / down / left / right
        verifyPixelColor(bitmap, 0.5f, -0.01f, Color.GREEN)
        verifyPixelColor(bitmap, 0.5f, -0.99f, Color.GREEN)
        verifyPixelColor(bitmap, 0.01f, -0.5f, Color.GREEN)
        verifyPixelColor(bitmap, 1.0f, -0.5f, Color.GREEN)

        // Verify the 4 corners are clipped.
        verifyPixelColor(bitmap, 0.01f, -0.01f, Color.RED)
        verifyPixelColor(bitmap, 0.99f, -0.01f, Color.RED)
        verifyPixelColor(bitmap, 0.99f, -0.99f, Color.RED)
        verifyPixelColor(bitmap, 0.01f, -0.99f, Color.RED)

        // Verify the center is not clipped, non-borderColor
        verifyPixelColorNotMatched(bitmap, 0.75f, -0.75f, Color.RED)
        verifyPixelColorNotMatched(bitmap, 0.75f, -0.75f, Color.GREEN)
    }

    @Test
    fun composition_borderRatioLargerThanRoundedCorner_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                // Make the first camera a solid RED color
                CompositionSettings.Builder()
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1.0f) // make it a solid color
                    .build(),
                // Bottom-Right
                CompositionSettings.Builder()
                    .setAlpha(1.0f)
                    .setOffset(0.5f, -0.5f)
                    .setScale(0.5f, 0.5f)
                    .setBorderColor(Color.GREEN)
                    .setBorderWidthRatio(0.4f)
                    .setRoundedCornerRatio(0.2f)
                    .build(),
            )

        // Verify Border in top / down / left / right
        verifyPixelColor(bitmap, 0.5f, -0.01f, Color.GREEN)
        verifyPixelColor(bitmap, 0.5f, -0.99f, Color.GREEN)
        verifyPixelColor(bitmap, 0.01f, -0.5f, Color.GREEN)
        verifyPixelColor(bitmap, 1.0f, -0.5f, Color.GREEN)

        // Verify the 4 corners are clipped.
        verifyPixelColor(bitmap, 0.01f, -0.01f, Color.RED)
        verifyPixelColor(bitmap, 0.99f, -0.01f, Color.RED)
        verifyPixelColor(bitmap, 0.99f, -0.99f, Color.RED)
        verifyPixelColor(bitmap, 0.01f, -0.99f, Color.RED)

        // Verify the center is not clipped, non-borderColor
        verifyPixelColorNotMatched(bitmap, 0.75f, -0.75f, Color.RED)
        verifyPixelColorNotMatched(bitmap, 0.75f, -0.75f, Color.GREEN)
    }

    @Test
    fun composition_zOrder_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                CompositionSettings.Builder()
                    .setScale(0.5f, 0.5f)
                    .setOffset(0.0f, 0.0f)
                    .setBorderColor(Color.GREEN)
                    .setBorderWidthRatio(1.0f) // make it solid color
                    .setZOrder(1)
                    .build(),
                CompositionSettings.Builder()
                    .setScale(1.0f, 1.0f)
                    .setOffset(0.0f, 0.0f)
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1.0f) // make it solid color
                    .setZOrder(0)
                    .build(),
            )

        // The 1st camera(green) will be on top of 2nd camera (red), so the center will be green
        verifyPixelColor(bitmap, 0.0f, 0.0f, Color.GREEN)
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun composition_alpha_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                CompositionSettings.Builder()
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1.0f) // make it solid color
                    .build(),
                CompositionSettings.Builder()
                    .setBorderColor(Color.WHITE)
                    .setBorderWidthRatio(0.1f)
                    .setAlpha(0.0f)
                    .build(),
            )
        // The border color is not impacted by the alpha
        verifyPixelColor(bitmap, 1.0f, 0.0f, Color.WHITE)
        // The 2nd camera is invisible so the RED color will be shown
        verifyPixelColor(bitmap, 0.0f, 0.0f, Color.RED)
    }

    @Test
    fun composition_scaleAndOffset_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                CompositionSettings.Builder()
                    .setScale(0.2f, 0.2f)
                    .setOffset(-0.8f, 0.8f)
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1f) // make it solid color
                    .build(),
                CompositionSettings.Builder()
                    .setScale(0.2f, 0.2f)
                    .setOffset(0.8f, -0.8f)
                    .setBorderColor(Color.GREEN)
                    .setBorderWidthRatio(1f) // make it solid color
                    .build(),
            )

        // Verify RED at top-left
        verifyPixelColor(bitmap, -0.9f, 0.9f, Color.RED)
        // Verify GREEN at bottom-right
        verifyPixelColor(bitmap, 0.9f, -0.9f, Color.GREEN)
        // Verify BLACK at center (background)
        verifyPixelColor(bitmap, 0.0f, 0.0f, Color.BLACK)
    }

    @Test
    fun composition_aspectRatio_isCorrect() = runBlocking {
        val bitmap =
            getPreviewBitmapFromConcurrentCameraComposition(
                // Make the first camera a solid RED color
                CompositionSettings.Builder()
                    .setBorderColor(Color.RED)
                    .setBorderWidthRatio(1.0f) // make it a solid color
                    .build(),
                // Full screen
                CompositionSettings.Builder()
                    .setAlpha(1.0f)
                    .setScale(1.0f, 1.0f)
                    .setRoundedCornerRatio(1.0f)
                    .build(),
            )

        val surfaceAR = previewSize.width.toFloat() / previewSize.height.toFloat()

        // Point at the center should not be clipped (NOT RED)
        verifyPixelColorNotMatched(bitmap, 0.0f, 0.0f, Color.RED)

        // Point at extreme corner should be clipped (RED)
        verifyPixelColor(bitmap, 0.99f, 0.99f, Color.RED)

        if (surfaceAR > 1.1f) {
            // Width > Height. Pill shape horizontal.
            // Point at far left/right middle should not be clipped.
            verifyPixelColorNotMatched(bitmap, 0.95f, 0.0f, Color.RED)
            verifyPixelColorNotMatched(bitmap, -0.95f, 0.0f, Color.RED)

            // Point that distinguishes AR=1 from AR=surfaceAR.
            // (0.5, 0.9) would be clipped if AR=1, but not if AR is correctly calculated.
            verifyPixelColorNotMatched(bitmap, 0.5f, 0.9f, Color.RED)
        } else if (surfaceAR < 0.9f) {
            // Height > Width. Pill shape vertical.
            // Point at far top/bottom middle should not be clipped.
            verifyPixelColorNotMatched(bitmap, 0.0f, 0.95f, Color.RED)
            verifyPixelColorNotMatched(bitmap, 0.0f, -0.95f, Color.RED)

            // Point that distinguishes AR=1 from AR=surfaceAR.
            // (0.9, 0.5) would be clipped if AR=1, but not if AR is correctly calculated.
            verifyPixelColorNotMatched(bitmap, 0.9f, 0.5f, Color.RED)
        }
    }

    private suspend fun renderAndCapturePreviewBitmap(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmapDeferred = kotlinx.coroutines.CompletableDeferred<Bitmap>()
        glHandler.post {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            surfaceTexture.updateTexImage()
            val texMatrix = FloatArray(16)
            surfaceTexture.getTransformMatrix(texMatrix)

            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            shaderProgram!!.use()
            shaderProgram!!.updateTextureMatrix(texMatrix)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glFinish()
            GLUtils.checkGlErrorOrThrow("glDrawArrays")

            val buffer = ByteBuffer.allocateDirect(width * height * 4)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            GLES20.glReadPixels(
                0,
                0,
                width,
                height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                buffer,
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)

            val matrix = android.graphics.Matrix()
            matrix.postScale(1f, -1f, width / 2f, height / 2f)
            val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
            bitmapDeferred.complete(flippedBitmap)
        }
        return bitmapDeferred.await()
    }

    private fun verifyPixelColor(bitmap: Bitmap, ndcX: Float, ndcY: Float, expectedColor: Int) {
        val x = ((ndcX + 1f) / 2f * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = ((1f - ndcY) / 2f * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val actualColor = bitmap.getPixel(x, y)

        assertColorMatch(actualColor, expectedColor, ndcX, ndcY)
    }

    private fun verifyPixelColorNotMatched(
        bitmap: Bitmap,
        ndcX: Float,
        ndcY: Float,
        compareColor: Int,
    ) {
        val x = ((ndcX + 1f) / 2f * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = ((1f - ndcY) / 2f * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val actualColor = bitmap.getPixel(x, y)

        assertColorNotMatch(actualColor, compareColor, ndcX, ndcY)
    }

    private fun assertColorMatch(actual: Int, expected: Int, ndcX: Float, ndcY: Float) {
        val r1 = Color.red(actual)
        val g1 = Color.green(actual)
        val b1 = Color.blue(actual)
        val r2 = Color.red(expected)
        val g2 = Color.green(expected)
        val b2 = Color.blue(expected)

        val threshold = 50
        val match =
            Math.abs(r1 - r2) < threshold &&
                Math.abs(g1 - g2) < threshold &&
                Math.abs(b1 - b2) < threshold

        assertWithMessage(
                "Color mismatch at NDC ($ndcX, $ndcY): expected ${Integer.toHexString(expected)}, " +
                    "actual ${Integer.toHexString(actual)}"
            )
            .that(match)
            .isTrue()
    }

    private fun assertColorNotMatch(actual: Int, notExpected: Int, ndcX: Float, ndcY: Float) {
        val r1 = Color.red(actual)
        val g1 = Color.green(actual)
        val b1 = Color.blue(actual)
        val r2 = Color.red(notExpected)
        val g2 = Color.green(notExpected)
        val b2 = Color.blue(notExpected)

        val threshold = 50
        val match =
            Math.abs(r1 - r2) < threshold &&
                Math.abs(g1 - g2) < threshold &&
                Math.abs(b1 - b2) < threshold

        assertWithMessage(
                "Color should NOT match at NDC ($ndcX, $ndcY): not expected ${Integer.toHexString(notExpected)}, " +
                    "actual ${Integer.toHexString(actual)}"
            )
            .that(match)
            .isFalse()
    }
}
