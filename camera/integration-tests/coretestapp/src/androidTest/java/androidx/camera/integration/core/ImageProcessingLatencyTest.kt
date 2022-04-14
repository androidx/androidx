/*
 * Copyright 2021 The Android Open Source Project
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
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Logger
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.fakes.FakeLifecycleOwner
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

// Profile the ImageProcessing performance to convert the input image from CameraX.
@LargeTest
@RunWith(Parameterized::class)
class ImageProcessingLatencyTest(
    private val targetResolution: Size
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    companion object {
        private const val TAG = "ImageProcessingLatencyTest"
        private val size480p = Size(480, 640)
        private val size1080p = Size(1080, 1920)
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(size480p, size1080p)
    }

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }
    }

    @LabTestRule.LabTestRearCamera
    @Test
    fun imageProcessingMeasurementViaRearCamera() {
        measureImageProcessing(CameraSelector.LENS_FACING_BACK)
    }

    @LabTestRule.LabTestFrontCamera
    @Test
    fun imageProcessingMeasurementViaFrontCamera() {
        measureImageProcessing(CameraSelector.LENS_FACING_FRONT)
    }

    private fun measureImageProcessing(lensFacing: Int) {
        // The log is used to profile the ImageProcessing performance. The log parser identifies
        // the log pattern "Image processing performance profiling" in the device output log.
        Logger.d(
            TAG,
            "Image processing performance profiling, resolution: $targetResolution, " +
                "lensFacing: $lensFacing"
        )
        // Profile the YubToRgbConverter performance with the first 200 frames.
        val countDownLatch = CountDownLatch(200)
        val imageAnalyzer = ImageAnalysis.Builder()
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(targetResolution)
            .build().also {
                it.setAnalyzer(
                    Dispatchers.Main.asExecutor()
                ) { image ->
                    countDownLatch.countDown()
                    image.close()
                }
            }

        camera =
            CameraUtil.createCameraAndAttachUseCase(
                context,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                imageAnalyzer
            )

        assertTrue(countDownLatch.await(60, TimeUnit.SECONDS))
    }
}