/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Logger
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val NUM_IMAGES = 10

/**
 * Profiles the ImageCapture performance for capturing images in different capture modes.
 *
 * Time measurement is taken from the time the capture request sent to when an ImageProxy is
 * returned. Saving an image to disk is not counted.
 *
 * Measurement is the total time for capturing NUM_IMAGES images.
 */
@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureLatencyTest(
    private val implName: String,
    private val cameraXConfig: CameraXConfig
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraXConfig)
    )

    @get:Rule
    val labTest = LabTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    companion object {
        private const val TAG = "ImageCaptureLatencyTest"
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    @Before
    fun setUp() = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()
            }
        }
    }

    @Test
    fun imageCaptureMeasurementZsl() {
        imageCaptureMeasurement(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
    }

    @Test
    fun imageCaptureMeasurementMinimizeLatency() {
        imageCaptureMeasurement(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun imageCaptureMeasurementMaximizeQuality() {
        imageCaptureMeasurement(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private fun imageCaptureMeasurement(captureMode: Int) {

        val imageCapture = ImageCapture.Builder().setCaptureMode(captureMode).build()

        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build(),
            imageCapture
        )

        // Skip if capture mode is ZSL and the device doesn't support ZSL
        if ((captureMode == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG) &&
            !camera.cameraInfo.isZslSupported) {
            Logger.d(TAG, "Skipping due to no ZSL support")
            return
        }

        val startTimeMillis = System.currentTimeMillis()

        val countDownLatch = CountDownLatch(NUM_IMAGES)

        for (i in 1..NUM_IMAGES) {
            imageCapture.takePicture(
                Dispatchers.Main.asExecutor(),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        image.close()
                        countDownLatch.countDown()
                    }
                })
        }

        assertTrue(countDownLatch.await(60, TimeUnit.SECONDS))

        val duration = System.currentTimeMillis() - startTimeMillis

        // This log is used to profile the ImageCapture performance. The log parser identifies the log
        // pattern "Image capture performance profiling" in the device output log.
        Logger.d(TAG,
            "Image capture performance profiling, duration: [$duration] capture mode: $captureMode")
    }
}
