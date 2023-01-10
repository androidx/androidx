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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AE
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AF
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AWB
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
import android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AE_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AWB_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM
import android.hardware.camera2.CaptureRequest.CONTROL_ZOOM_RATIO
import android.hardware.camera2.CaptureRequest.FLASH_MODE
import android.hardware.camera2.CaptureRequest.FLASH_MODE_TORCH
import android.hardware.camera2.CaptureRequest.SCALER_CROP_REGION
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.testing.VerifyResultListener
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val TIMEOUT = TimeUnit.SECONDS.toMillis(10)

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCamera2Interop::class)
@SdkSuppress(minSdkVersion = 21)
class CameraControlAdapterDeviceTest {
    private lateinit var cameraSelector: CameraSelector
    private lateinit var context: Context
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var cameraControl: CameraControlAdapter
    private lateinit var comboListener: ComboRequestListener
    private lateinit var characteristics: CameraCharacteristics
    private var hasFlashUnit: Boolean = false

    private val imageCapture = ImageCapture.Builder().build()
    private val imageAnalysis = ImageAnalysis.Builder().build().apply {
        // set analyzer to make it active.
        setAnalyzer(Dispatchers.Default.asExecutor()) {
            // Fake analyzer, do nothing.
        }
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    @Before
    fun setUp() {
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
        cameraControl = camera.cameraControl as CameraControlAdapter
        comboListener = cameraControl.camera2cameraControl.requestListener

        characteristics = CameraUtil.getCameraCharacteristics(
            CameraSelector.LENS_FACING_BACK
        )!!

        hasFlashUnit = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE).let {
            it != null && it
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::camera.isInitialized) {
            withContext(Dispatchers.Main) {
                camera.detachUseCases()
            }
        }

        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    // TODO: test all public API of the CameraControl to ensure the RequestOptions still exist
    //  after adding/removing the UseCase.
    @Test
    fun addUseCase_requestOptionsShouldSetToCamera(): Unit = runBlocking {
        // Arrange.
        bindUseCase(imageAnalysis)
        arrangeRequestOptions()

        // Act.
        withContext(Dispatchers.Main) {
            camera.addUseCases(listOf(imageCapture))
        }

        // Assert. Attaching a new UseCase should not change the RequestOptions that we set the
        // UseCaseCamera, The CaptureRequest after the new UseCase is attached should have the
        // same RequestOptions as before. The verify block will verify the CaptureRequest has the
        // same RequestOptions as we arranged.
        verifyRequestOptions()
    }

    // TODO: test all public API of the CameraControl to ensure the RequestOptions still exist
    //  after adding/removing the UseCase.
    @Test
    fun removeUseCase_requestOptionsShouldSetToCamera(): Unit = runBlocking {
        // Arrange.
        bindUseCase(imageAnalysis, imageCapture)
        arrangeRequestOptions()

        // Act.
        withContext(Dispatchers.Main) {
            camera.removeUseCases(listOf(imageCapture))
        }

        // Assert. Removing one of the UseCases (not all) should not change the
        // RequestOptions that we set the UseCaseCamera, the CaptureRequest after the UseCase
        // removal should have the same RequestOptions as before. The verify block will verify
        // the CaptureRequest has the same RequestOptions as we arranged.
        verifyRequestOptions()
    }

    @Test
    fun setFlashModeAuto_aeModeSetAndRequestUpdated(): Unit = runBlocking {
        Assume.assumeTrue(hasFlashUnit)
        bindUseCase(imageAnalysis)
        cameraControl.flashMode = ImageCapture.FLASH_MODE_AUTO

        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->
                requestMeta.request[CONTROL_AE_MODE] == CONTROL_AE_MODE_ON_AUTO_FLASH
            },
            TIMEOUT
        )
        Truth.assertThat(cameraControl.flashMode).isEqualTo(ImageCapture.FLASH_MODE_AUTO)
    }

    @Test
    fun setFlashModeOff_aeModeSetAndRequestUpdated(): Unit = runBlocking {
        Assume.assumeTrue(hasFlashUnit)
        bindUseCase(imageAnalysis)
        cameraControl.flashMode = ImageCapture.FLASH_MODE_OFF

        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->
                requestMeta[CONTROL_AE_MODE] == CONTROL_AE_MODE_ON
            },
            TIMEOUT
        )
        Truth.assertThat(cameraControl.flashMode).isEqualTo(ImageCapture.FLASH_MODE_OFF)
    }

    @Test
    fun setFlashModeOn_aeModeSetAndRequestUpdated(): Unit = runBlocking {
        Assume.assumeTrue(hasFlashUnit)
        bindUseCase(imageAnalysis)
        cameraControl.flashMode = ImageCapture.FLASH_MODE_ON

        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->
                requestMeta[CONTROL_AE_MODE] == CONTROL_AE_MODE_ON_ALWAYS_FLASH
            },
            TIMEOUT
        )
        Truth.assertThat(cameraControl.flashMode).isEqualTo(ImageCapture.FLASH_MODE_ON)
    }

    @Test
    fun enableTorch_aeModeSetAndRequestUpdated(): Unit = runBlocking {
        Assume.assumeTrue(hasFlashUnit)
        bindUseCase(imageAnalysis)
        cameraControl.enableTorch(true).await()

        waitForResult(captureCount = 30).verify(
            { requestMeta: RequestMetadata, frameInfo: FrameInfo ->
                frameInfo.requestMetadata[FLASH_MODE] == FLASH_MODE_TORCH &&
                    requestMeta[CONTROL_AE_MODE] == CONTROL_AE_MODE_ON
            },
            TIMEOUT
        )
    }

    @Test
    fun disableTorchFlashModeAuto_aeModeSetAndRequestUpdated(): Unit = runBlocking {
        Assume.assumeTrue(hasFlashUnit)
        bindUseCase(imageAnalysis)
        cameraControl.flashMode = ImageCapture.FLASH_MODE_AUTO
        cameraControl.enableTorch(false).await()

        waitForResult(captureCount = 30).verify(
            { requestMeta: RequestMetadata, frameInfo: FrameInfo ->
                frameInfo.requestMetadata[FLASH_MODE] != FLASH_MODE_TORCH &&
                    requestMeta[CONTROL_AE_MODE] == CONTROL_AE_MODE_ON_AUTO_FLASH
            },
            TIMEOUT
        )
    }

    @Test
    fun startFocusAndMetering_3ARegionsUpdated() = runBlocking {
        Assume.assumeTrue(
            characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AF) > 0 ||
                characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AE) > 0 ||
                characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AWB) > 0
        )
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val action = FocusMeteringAction.Builder(factory.createPoint(0f, 0f)).build()
        bindUseCase(imageAnalysis)

        // Act.
        cameraControl.startFocusAndMetering(action).await()

        // Assert. Here we verify only 3A region count is correct.
        val expectedAfCount =
            characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AF).coerceAtMost(1)
        val expectedAeCount =
            characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AE).coerceAtMost(1)
        val expectedAwbCount =
            characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AWB).coerceAtMost(1)
        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->
                val afRegionMatched = requestMeta.getOrDefault(
                    CONTROL_AF_REGIONS,
                    emptyArray()
                ).size == expectedAfCount

                val aeRegionMatched = requestMeta.getOrDefault(
                    CONTROL_AE_REGIONS,
                    emptyArray()
                ).size == expectedAeCount

                val awbRegionMatched = requestMeta.getOrDefault(
                    CONTROL_AWB_REGIONS,
                    emptyArray()
                ).size == expectedAwbCount

                afRegionMatched && aeRegionMatched && awbRegionMatched
            },
            TIMEOUT
        )
    }

    @Test
    fun cancelFocusAndMetering_3ARegionsReset() = runBlocking {
        Assume.assumeTrue(
            characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AF) > 0 ||
                characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AE) > 0 ||
                characteristics.getMaxRegionCount(CONTROL_MAX_REGIONS_AWB) > 0
        )
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val action = FocusMeteringAction.Builder(factory.createPoint(0f, 0f)).build()
        bindUseCase(imageAnalysis)

        // Act.
        cameraControl.startFocusAndMetering(action).await()
        cameraControl.cancelFocusAndMetering().await()

        // Assert. The regions are reset to the default.
        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->

                val isDefaultAfRegion = requestMeta.getOrDefault(
                    CONTROL_AF_REGIONS,
                    CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
                ).contentEquals(CameraGraph.Constants3A.METERING_REGIONS_DEFAULT)

                val isDefaultAeRegion = requestMeta.getOrDefault(
                    CONTROL_AE_REGIONS,
                    CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
                ).contentEquals(CameraGraph.Constants3A.METERING_REGIONS_DEFAULT)

                val isDefaultAwbRegion = requestMeta.getOrDefault(
                    CONTROL_AWB_REGIONS,
                    CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
                ).contentEquals(CameraGraph.Constants3A.METERING_REGIONS_DEFAULT)

                isDefaultAfRegion && isDefaultAeRegion && isDefaultAwbRegion
            },
            TIMEOUT
        )
    }

    private fun CameraCharacteristics.getMaxRegionCount(
        option_max_regions: CameraCharacteristics.Key<Int>
    ) = get(option_max_regions) ?: 0

    private suspend fun arrangeRequestOptions() {
        cameraControl.setExposureCompensationIndex(1)
        cameraControl.setZoomRatio(1.0f)
        cameraControl.camera2cameraControl.setCaptureRequestOptions(
            CaptureRequestOptions.Builder().setCaptureRequestOption(
                CONTROL_CAPTURE_INTENT,
                CONTROL_CAPTURE_INTENT_CUSTOM
            ).build()
        ).await()

        // Ensure the requests are already set to the CaptureRequest.
        waitForResult().verify(
            { captureRequest: RequestMetadata, _ ->
                // Ensure the EV working before testing
                assumeThat(
                    "EV Request doesn't set to CaptureRequest, ignore the test",
                    captureRequest.request[CONTROL_AE_EXPOSURE_COMPENSATION],
                    equalTo(1)
                )

                // Ensure the Camera2Interop working before testing
                assumeThat(
                    "Camera2Interop Request doesn't set to CaptureRequest, ignore the test",
                    captureRequest.request[CONTROL_CAPTURE_INTENT],
                    equalTo(CONTROL_CAPTURE_INTENT_CUSTOM)
                )

                // Ensure the Zoom working before testing
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    assumeThat(
                        "Zoom Request doesn't set to CaptureRequest, ignore the test",
                        captureRequest.request[CONTROL_ZOOM_RATIO],
                        notNullValue()
                    )
                } else {
                    assumeThat(
                        "Zoom Request doesn't set to CaptureRequest, ignore the test",
                        captureRequest.request[SCALER_CROP_REGION],
                        notNullValue()
                    )
                }
                return@verify true
            },
            TIMEOUT
        )
    }

    private suspend fun verifyRequestOptions() {
        waitForResult(captureCount = 30).verify(
            { metadata: RequestMetadata, _ ->
                val checkEV = metadata.request[CONTROL_AE_EXPOSURE_COMPENSATION] == 1
                val checkCaptureIntent =
                    metadata.request[CONTROL_CAPTURE_INTENT] == CONTROL_CAPTURE_INTENT_CUSTOM
                val checkZoom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    metadata.request[CONTROL_ZOOM_RATIO] != null
                } else {
                    metadata.request[SCALER_CROP_REGION] != null
                }

                checkEV && checkCaptureIntent && checkZoom
            },
            TIMEOUT
        )
    }

    private fun waitForResult(captureCount: Int = 1): VerifyResultListener =
        VerifyResultListener(captureCount).also {
            comboListener.addListener(it, Dispatchers.Default.asExecutor())
        }

    private fun bindUseCase(vararg useCases: UseCase) {
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            cameraSelector,
            *useCases,
        )
        cameraControl = camera.cameraControl as CameraControlAdapter
    }
}