/*
 * Copyright 2025 The Android Open Source Project
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

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult.SENSOR_TEST_PATTERN_MODE
import android.hardware.camera2.TotalCaptureResult
import android.util.Rational
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProcessor
import androidx.camera.core.ImageProcessor.Response
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.imagecapture.RgbaImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.core.util.SensorPatternUtil.ColorChannel
import androidx.camera.integration.core.util.SensorPatternUtil.assumeSolidColorPatternSupported
import androidx.camera.integration.core.util.SensorPatternUtil.getPrimaryColor
import androidx.camera.integration.core.util.SensorPatternUtil.setSolidColorPatternToCamera
import androidx.camera.integration.core.util.SensorPatternUtil.verifyColor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.hasCameraWithLensFacing
import androidx.camera.testing.impl.CountdownDeferred
import androidx.camera.testing.impl.SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.util.Camera2InteropUtil.setCameraCaptureSessionCallback
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val CAPTURE_TIMEOUT = 15.seconds

@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureEffectTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(hasCameraWithLensFacing(cameraSelector.lensFacing!!))
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun captureImageRed_withGreenImageEffect_producesGreenImage() =
        runSolidColorImageCaptureTest(ColorChannel.RED, ColorChannel.GREEN)

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun captureImageGreen_withBlueImageEffect_producesBlueImage() =
        runSolidColorImageCaptureTest(ColorChannel.GREEN, ColorChannel.BLUE)

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun captureImageBlue_withRedImageEffect_producesRedImage() =
        runSolidColorImageCaptureTest(ColorChannel.BLUE, ColorChannel.RED)

    @RequiresApi(29)
    private fun runSolidColorImageCaptureTest(
        colorChannelBeforeEffect: ColorChannel,
        colorChannelAfterEffect: ColorChannel,
    ) =
        runCameraTest(cameraConfig) { provider ->
            val camInfo = provider.getCameraInfo(cameraSelector)

            // Assume the device supports solid color test patterns
            assumeSolidColorPatternSupported(camInfo, implName)

            val testPatternModeFlow = MutableStateFlow<Int?>(null)
            val captureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        result.get(SENSOR_TEST_PATTERN_MODE)?.let { testPatternModeFlow.value = it }
                    }
                }

            // Create image capture
            val imageCapture = ImageCapture.Builder().build()

            // Add Preview to ensure the preview stream does not drop frames during/after recordings
            val preview =
                Preview.Builder()
                    .apply { setCameraCaptureSessionCallback(implName, this, captureCallback) }
                    .build()

            val effect = TestImageEffect(colorChannelAfterEffect)
            val useCaseGroup =
                UseCaseGroup.Builder()
                    .apply {
                        addUseCase(imageCapture)
                        addUseCase(preview)

                        setViewPort(ViewPort.Builder(Rational(9, 16), Surface.ROTATION_0).build())

                        addEffect(effect)
                    }
                    .build()

            withContext(Dispatchers.Main) {
                val lifecycleOwner = FakeLifecycleOwner()
                // Sets surface provider to preview
                preview.surfaceProvider = createAutoDrainingSurfaceTextureProvider()
                val camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
                setSolidColorPatternToCamera(camera, colorChannelBeforeEffect, implName)

                lifecycleOwner.startAndResume()

                // Act.
                val latch = CountdownDeferred(1)
                var outputPrimaryColor: Int? = null
                var captureError: Exception? = null
                val callback =
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toBitmap()
                            image.close()

                            // Get the primary color of the captured image.
                            outputPrimaryColor = bitmap.getPrimaryColor()

                            latch.countDown()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            captureError = exception
                            latch.countDown()
                        }
                    }
                imageCapture.takePicture(mainExecutor, callback)

                // Assert.
                // Wait for the signal that the image has been captured.
                assertThat(withTimeoutOrNull(CAPTURE_TIMEOUT) { latch.await() }).isNotNull()
                assertThat(captureError).isNull()
                assertThat(
                        verifyColor(effect.awaitInputPrimaryColor(5000), colorChannelBeforeEffect)
                    )
                    .isTrue()
                assertThat(verifyColor(outputPrimaryColor, colorChannelAfterEffect)).isTrue()

                lifecycleOwner.pauseAndStop()
                lifecycleOwner.destroy()
            }
        }

    private inline fun runCameraTest(
        cameraConfig: CameraXConfig,
        crossinline block: suspend CoroutineScope.(ProcessCameraProvider) -> Unit,
    ): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        val context: Context = ApplicationProvider.getApplicationContext()
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        try {
            block(cameraProvider)
        } finally {
            withContext(NonCancellable) { cameraProvider.shutdownAsync().await() }
        }
    }

    private class TestImageEffect(targetOutputColorChannel: ColorChannel) :
        CameraEffect(
            IMAGE_CAPTURE,
            CameraXExecutors.directExecutor(),
            TestImageProcessor(targetOutputColorChannel),
            Throwable::printStackTrace,
        ) {

        suspend fun awaitInputPrimaryColor(timeoutMillis: Long): Int? {
            return (imageProcessor as TestImageProcessor).awaitInputPrimaryColor(timeoutMillis)
        }

        private class TestImageProcessor(private val targetOutputColorChannel: ColorChannel) :
            ImageProcessor {
            private val inputPrimaryColorDeferred = CompletableDeferred<Int>()

            override fun process(request: ImageProcessor.Request): Response {
                val inputImage = request.inputImage
                val inputPrimaryColor =
                    (inputImage as RgbaImageProxy).createBitmap().getPrimaryColor()

                inputPrimaryColorDeferred.complete(inputPrimaryColor)

                return generateResponse(inputImage)
            }

            suspend fun awaitInputPrimaryColor(timeoutMillis: Long): Int? {
                return withTimeoutOrNull(timeoutMillis) { inputPrimaryColorDeferred.await() }
            }

            private fun generateResponse(inputImage: ImageProxy): Response {
                val outputBitmap =
                    Bitmap.createBitmap(
                        inputImage.width,
                        inputImage.height,
                        Bitmap.Config.ARGB_8888,
                    )
                val canvas = Canvas(outputBitmap)
                canvas.drawColor(targetOutputColorChannel.toColor())

                return Response {
                    RgbaImageProxy(
                        outputBitmap,
                        inputImage.cropRect,
                        inputImage.imageInfo.getRotationDegrees(),
                        inputImage.imageInfo.sensorToBufferTransformMatrix,
                        inputImage.imageInfo.getTimestamp(),
                    )
                }
            }
        }
    }
}
