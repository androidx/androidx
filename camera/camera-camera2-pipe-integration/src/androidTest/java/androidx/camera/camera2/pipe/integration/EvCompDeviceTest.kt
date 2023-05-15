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

package androidx.camera.camera2.pipe.integration

import android.content.Context
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION
import android.os.Build
import android.util.Log
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.testing.VerifyResultListener
import androidx.camera.camera2.pipe.testing.toCameraControlAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class EvCompDeviceTest {
    private lateinit var cameraSelector: CameraSelector
    private lateinit var context: Context
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var cameraControl: CameraControlAdapter
    private lateinit var comboListener: ComboRequestListener

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    @Before
    fun setUp() {
        // TODO(b/162296654): Workaround the google_3a specific behavior.
        Assume.assumeFalse(
            "Cuttlefish uses google_3a v1 or v2 it might fail to set EV before first AE converge.",
            Build.MODEL.contains("Cuttlefish")
        )
        Assume.assumeFalse(
            "Pixel uses google_3a v1 or v2 it might fail to set EV before first AE converge.",
            Build.MODEL.contains("Pixel")
        )
        Assume.assumeFalse(
            "Disable Nexus 5 in postsubmit for b/173743705",
            Build.MODEL.contains("Nexus 5") && !Log.isLoggable("MH", Log.DEBUG)
        )

        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(
            context,
            CameraPipeConfig.defaultConfig()
        )
        cameraSelector = CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK
        ).build()
        camera = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraControl = camera.cameraControl.toCameraControlAdapter()

        @OptIn(ExperimentalCamera2Interop::class)
        comboListener = cameraControl.camera2cameraControl.requestListener
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::camera.isInitialized) {
            withContext(Dispatchers.Main) {
                camera.removeUseCases(camera.useCases)
            }
        }

        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun setExposure_futureResultTest() {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)

        // Arrange.
        bindUseCase()

        val upper = exposureState.exposureCompensationRange.upper

        // Act.
        val ret = cameraControl.setExposureCompensationIndex(upper).get(
            3000,
            TimeUnit.MILLISECONDS
        )

        // Assert.
        Truth.assertThat(ret).isEqualTo(upper)
    }

    @Test
    fun setExposureTest() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)
        val upper = exposureState.exposureCompensationRange.upper

        bindUseCase()

        // Act. Set the exposure compensation
        cameraControl.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS)

        // Assert.
        registerListener().verifyCaptureResultParameter(CONTROL_AE_EXPOSURE_COMPENSATION, upper)
    }

    @Test
    fun setExposureTest_runTwice() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        val upper = exposureState.exposureCompensationRange.upper

        // Set the EC value first time.
        cameraControl.setExposureCompensationIndex(upper - 1)

        // Act. Set the EC value again, and verify this task should complete successfully.
        cameraControl.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS)

        // Assert. Verify the exposure compensation target result is in the capture result.
        registerListener().verifyCaptureResultParameter(CONTROL_AE_EXPOSURE_COMPENSATION, upper)
    }

    @Test
    fun setExposureAndZoomRatio_theExposureSettingShouldApply() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        // Act. Set the exposure compensation, and then use the zoom API after the exposure is
        // changed.
        val upper = exposureState.exposureCompensationRange.upper
        cameraControl.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS)
        cameraControl.setZoomRatio(
            camera.cameraInfo.zoomState.value!!.maxZoomRatio
        ).get(
            3000,
            TimeUnit.MILLISECONDS
        )

        // Assert. Verify the exposure compensation target result is in the capture result.
        registerListener().verifyCaptureResultParameter(CONTROL_AE_EXPOSURE_COMPENSATION, upper)
    }

    @Test
    fun setExposureAndLinearZoom_theExposureSettingShouldApply() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        // Act. Set the exposure compensation, and then use the zoom API after the exposure is
        // changed.
        val upper = exposureState.exposureCompensationRange.upper
        cameraControl.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS)
        cameraControl.setLinearZoom(0.5f).get(3000, TimeUnit.MILLISECONDS)

        // Assert. Verify the exposure compensation target result is in the capture result.
        registerListener().verifyCaptureResultParameter(CONTROL_AE_EXPOSURE_COMPENSATION, upper)
    }

    @Test
    fun setExposureAndFlash_theExposureSettingShouldApply() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        // Act. Set the exposure compensation, and then use the flash API after the exposure is
        // changed.
        val upper = exposureState.exposureCompensationRange.upper
        cameraControl.setExposureCompensationIndex(upper).get(3000, TimeUnit.MILLISECONDS)
        cameraControl.setFlashMode(ImageCapture.FLASH_MODE_AUTO)

        // Assert. Verify the exposure compensation target result is in the capture result.
        registerListener().verifyCaptureResultParameter(CONTROL_AE_EXPOSURE_COMPENSATION, upper)
    }

    @Test
    fun setExposureTimeout_theNextCallShouldWork() = runBlocking {
        val exposureState = camera.cameraInfo.exposureState
        Assume.assumeTrue(exposureState.isExposureCompensationSupported)

        bindUseCase()

        assertThrows<TimeoutException> {
            // The set future will timeout in this test.
            cameraControl.setExposureCompensationIndex(1).get(0, TimeUnit.MILLISECONDS)
        }

        // Assert. Verify the second time call should set the new exposure value successfully.
        Truth.assertThat(
            cameraControl.setExposureCompensationIndex(2).get(
                3000,
                TimeUnit.MILLISECONDS
            )
        ).isEqualTo(2)
    }

    private suspend fun <T> VerifyResultListener.verifyCaptureResultParameter(
        key: CaptureResult.Key<T>,
        value: T,
        timeout: Long = TimeUnit.SECONDS.toMillis(5),
    ) = verify(
        { _, captureResult: FrameInfo -> captureResult.metadata[key] == value },
        timeout
    )

    private fun registerListener(capturesCount: Int = 1): VerifyResultListener =
        VerifyResultListener(capturesCount).also {
            comboListener.addListener(it, Dispatchers.Default.asExecutor())
        }

    private fun bindUseCase() {
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            cameraSelector,
            ImageAnalysis.Builder().build().apply {
                // set analyzer to make it active.
                setAnalyzer(Dispatchers.Default.asExecutor()) {
                    // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent the
                    // closing of the CameraDevice from being stuck.
                    it.close()
                }
            },
        )
        cameraControl = camera.cameraControl.toCameraControlAdapter()
    }
}