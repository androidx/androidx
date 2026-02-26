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
import android.hardware.camera2.CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExposureState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCamera2Interop::class)
class EvControlDeviceTest {
    private lateinit var cameraSelector: CameraSelector
    private lateinit var context: Context
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraControl: CameraControlInternal
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var captureCallback: Camera2InteropUtil.CaptureCallback

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule val useCamera = CameraUtil.grantCameraPermissionAndPreTestAndPostTest()

    @Before
    fun setUp() = runBlocking {
        // TODO(b/162296654): Workaround the google_3a specific behavior.
        Assume.assumeFalse(
            "Cuttlefish uses google_3a v1 or v2 it might fail to set EV before first AE converge.",
            Build.MODEL.contains("Cuttlefish"),
        )
        Assume.assumeFalse(
            "Pixel uses google_3a v1 or v2 it might fail to set EV before first AE converge.",
            Build.MODEL.contains("Pixel"),
        )
        Assume.assumeFalse(
            "Disable Nexus 5 in postsubmit for b/173743705",
            Build.MODEL.contains("Nexus 5") && !Log.isLoggable("MH", Log.DEBUG),
        )

        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        context = ApplicationProvider.getApplicationContext()
        captureCallback = Camera2InteropUtil.CaptureCallback()

        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
        }
        cameraControl = camera.cameraControl as CameraControlInternal
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun setExposure_futureResultTest() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)

        // Arrange.
        bindUseCase()

        val targetIndex = getSafeTargetExposureIndex(exposureState)

        // Act.
        val ret = cameraControl.setExposureCompensationIndex(targetIndex).awaitWithTimeout()

        // Assert.
        assertThat(ret).isEqualTo(targetIndex)
    }

    @Test
    fun setExposureTest() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)
        val targetIndex = getSafeTargetExposureIndex(exposureState)

        bindUseCase()

        // Act. Set the exposure compensation
        cameraControl.setExposureCompensationIndex(targetIndex).awaitWithTimeout()

        // Assert.
        captureCallback.verifyLastCaptureResult(
            keyValueMap = mapOf(CONTROL_AE_EXPOSURE_COMPENSATION to targetIndex),
            numOfCaptures = 5,
        )
    }

    @Test
    fun setExposureTest_runTwice() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)

        val targetIndex = getSafeTargetExposureIndex(exposureState)
        assumeTrue(exposureState.exposureCompensationRange.contains(targetIndex - 2))

        bindUseCase()

        // Set the EC value first time.
        cameraControl.setExposureCompensationIndex(targetIndex - 2)

        // Act. Set the EC value again, and verify this task should complete successfully.
        cameraControl.setExposureCompensationIndex(targetIndex).awaitWithTimeout()

        // Assert. Verify the exposure compensation target result is in the capture result.
        captureCallback.verifyLastCaptureResult(
            keyValueMap = mapOf(CONTROL_AE_EXPOSURE_COMPENSATION to targetIndex),
            numOfCaptures = 5,
        )
    }

    @Test
    fun setExposureAndZoomRatio_theExposureSettingShouldApply() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        // Act. Set the exposure compensation, and then use the zoom API after the exposure is
        // changed.
        val targetIndex = getSafeTargetExposureIndex(exposureState)
        cameraControl.setExposureCompensationIndex(targetIndex).awaitWithTimeout()
        cameraControl
            .setZoomRatio(camera.cameraInfo.zoomState.value!!.maxZoomRatio)
            .awaitWithTimeout()

        // Assert. Verify the exposure compensation target result is in the capture result.
        captureCallback.verifyLastCaptureResult(
            keyValueMap = mapOf(CONTROL_AE_EXPOSURE_COMPENSATION to targetIndex),
            numOfCaptures = 5,
        )
    }

    @Test
    fun setExposureAndLinearZoom_theExposureSettingShouldApply() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        // Act. Set the exposure compensation, and then use the zoom API after the exposure is
        // changed.
        val targetIndex = getSafeTargetExposureIndex(exposureState)
        cameraControl.setExposureCompensationIndex(targetIndex).awaitWithTimeout()
        cameraControl.setLinearZoom(0.5f).awaitWithTimeout()

        // Assert. Verify the exposure compensation target result is in the capture result.
        captureCallback.verifyLastCaptureResult(
            keyValueMap = mapOf(CONTROL_AE_EXPOSURE_COMPENSATION to targetIndex),
            numOfCaptures = 5,
        )
    }

    @Test
    fun setExposureAndFlash_theExposureSettingShouldApply() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        // Act. Set the exposure compensation, and then use the flash API after the exposure is
        // changed.
        val targetIndex = getSafeTargetExposureIndex(exposureState)
        cameraControl.setExposureCompensationIndex(targetIndex).awaitWithTimeout()
        cameraControl.setFlashMode(ImageCapture.FLASH_MODE_AUTO)

        // Assert. Verify the exposure compensation target result is in the capture result.
        captureCallback.verifyLastCaptureResult(
            keyValueMap = mapOf(CONTROL_AE_EXPOSURE_COMPENSATION to targetIndex),
            numOfCaptures = 5,
        )
    }

    @Test
    fun setExposureTimeout_theNextCallShouldWork() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        assumeTrue(exposureState.isExposureCompensationSupported)
        assumeTrue(exposureState.exposureCompensationRange.contains(2))

        bindUseCase()

        assertThrows<TimeoutException> {
            // The set future will time out in this test.
            cameraControl.setExposureCompensationIndex(1).get(0, TimeUnit.MILLISECONDS)
        }

        // Assert. Verify the second time call should set the new exposure value successfully.
        Truth.assertThat(cameraControl.setExposureCompensationIndex(2).awaitWithTimeout())
            .isEqualTo(2)
    }

    private fun bindUseCase() {
        instrumentation.runOnMainSync {
            val useCase =
                ImageAnalysis.Builder()
                    .also { imageAnalysisBuilder ->
                        Camera2InteropUtil.setCamera2InteropOptions(
                            implName = "camera2",
                            builder = imageAnalysisBuilder,
                            captureCallback = captureCallback,
                        )
                    }
                    .build()
                    .apply { setAnalyzer(CameraXExecutors.ioExecutor()) { it.close() } }
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            cameraControl = camera.cameraControl as CameraControlInternal
        }
    }

    private suspend fun <T> ListenableFuture<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) { await() }

    private fun getSafeTargetExposureIndex(exposureState: ExposureState): Int {
        val range = exposureState.exposureCompensationRange
        val current = exposureState.exposureCompensationIndex

        return when {
            range.contains(current + 1) -> current + 1
            range.contains(current - 1) -> current - 1
            else -> throw IllegalArgumentException("Cannot find safe target EV.")
        }
    }
}
