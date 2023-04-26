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
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES
import android.hardware.camera2.CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AE
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AF
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AWB
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
import android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO
import android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
import android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
import android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF
import android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AE_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AWB_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_ZOOM_RATIO
import android.hardware.camera2.CaptureRequest.FLASH_MODE
import android.hardware.camera2.CaptureRequest.FLASH_MODE_TORCH
import android.hardware.camera2.CaptureRequest.SCALER_CROP_REGION
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.compat.quirk.CrashWhenTakingPhotoWithAutoFlashAEModeQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureFailWithAutoFlashQuirk
import androidx.camera.camera2.pipe.integration.compat.workaround.AutoFlashAEModeDisablerImpl
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.testing.VerifyResultListener
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.After
import org.junit.Assert
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
    private var testEffectMode: Int? = null

    private val imageCapture = ImageCapture.Builder().build()
    private val imageAnalysis = ImageAnalysis.Builder().build().apply {
        // set analyzer to make it active.
        setAnalyzer(Dispatchers.Default.asExecutor()) {
            // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent the closing
            // of the CameraDevice from being stuck.
            it.close()
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

        hasFlashUnit = camera.cameraInfo.hasFlashUnit()
        testEffectMode = camera.getTestEffectMode()
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
                requestMeta.isAeMode(CONTROL_AE_MODE_ON_AUTO_FLASH)
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
                requestMeta.isAeMode(CONTROL_AE_MODE_ON)
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
                requestMeta.isAeMode(CONTROL_AE_MODE_ON_ALWAYS_FLASH)
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
                    requestMeta.isAeMode(CONTROL_AE_MODE_ON)
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
                    requestMeta.isAeMode(CONTROL_AE_MODE_ON_AUTO_FLASH)
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
                // some devices may have a 0 weight region added by default, so weightedRegionCount

                val afRegionMatched = requestMeta.getOrDefault(
                    CONTROL_AF_REGIONS,
                    emptyArray()
                ).weightedRegionCount == expectedAfCount

                val aeRegionMatched = requestMeta.getOrDefault(
                    CONTROL_AE_REGIONS,
                    emptyArray()
                ).weightedRegionCount == expectedAeCount

                val awbRegionMatched = requestMeta.getOrDefault(
                    CONTROL_AWB_REGIONS,
                    emptyArray()
                ).weightedRegionCount == expectedAwbCount

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

    @Test
    fun setTemplatePreview_afModeToContinuousPicture() = runBlocking {
        bindUseCase(createPreview())

        // Assert. Verify the afMode.
        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->
                requestMeta.isAfMode(CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            },
            TIMEOUT
        )
    }

    @Test
    fun setTemplateRecord_afModeToContinuousVideo() = runBlocking {
        bindUseCase(createFakeRecordingUseCase())

        // Assert. Verify the afMode.
        waitForResult(captureCount = 60).verify(
            { requestMeta: RequestMetadata, _ ->
                requestMeta.isAfMode(CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            },
            TIMEOUT
        )
    }

    @Test
    fun setZoomRatio_operationCanceledExceptionIfNoUseCase() {
        val ratio = camera.getMaxSupportedZoomRatio()
        assertFutureFailedWithOperationCancellation(cameraControl.setZoomRatio(ratio))
    }

    private fun <T> assertFutureFailedWithOperationCancellation(future: ListenableFuture<T>) {
        Assert.assertThrows(ExecutionException::class.java) {
            future[3, TimeUnit.SECONDS]
        }.apply {
            Truth.assertThat(cause)
                .isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
    }

    private fun CameraCharacteristics.getMaxRegionCount(
        optionMaxRegions: CameraCharacteristics.Key<Int>
    ) = get(optionMaxRegions) ?: 0

    private suspend fun arrangeRequestOptions() {
        cameraControl.setExposureCompensationIndex(1)
        cameraControl.setZoomRatio(camera.getMaxSupportedZoomRatio())
        testEffectMode?.let { effectMode ->
            cameraControl.camera2cameraControl.setCaptureRequestOptions(
                CaptureRequestOptions.Builder().setCaptureRequestOption(
                    CONTROL_EFFECT_MODE,
                    effectMode
                ).build()
            ).await()
        }

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
                if (testEffectMode != null) {
                    assumeThat(
                        "Camera2Interop Request doesn't set to CaptureRequest, ignore the test",
                        captureRequest.request[CONTROL_EFFECT_MODE],
                        equalTo(testEffectMode)
                    )
                }

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

    private fun Camera.getTestEffectMode(): Int? {
        return Camera2CameraInfo.from(cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)?.getOrNull(0)
    }

    private fun Camera.getMaxSupportedZoomRatio(): Float {
        return cameraInfo.zoomState.value!!.maxZoomRatio
    }

    private suspend fun verifyRequestOptions() {
        waitForResult(captureCount = 30).verify(
            { metadata: RequestMetadata, _ ->
                val checkEV = metadata.request[CONTROL_AE_EXPOSURE_COMPENSATION] == 1
                val checkEffectMode = testEffectMode?.let {
                    metadata.request[CONTROL_EFFECT_MODE] == it
                } ?: true
                val checkZoom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    metadata.request[CONTROL_ZOOM_RATIO] != null
                } else {
                    metadata.request[SCALER_CROP_REGION] != null
                }

                checkEV && checkEffectMode && checkZoom
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

    private fun createFakeRecordingUseCase(): FakeUseCase {
        return FakeTestUseCase(
            FakeUseCaseConfig.Builder().setTargetName("FakeRecordingUseCase").useCaseConfig
        ).apply {
            initAndActive()
        }
    }

    private class FakeTestUseCase(config: FakeUseCaseConfig) : FakeUseCase(config) {

        val deferrableSurface = object : DeferrableSurface() {
            init {
                terminationFuture.addListener(
                    { cleanUp() }, Dispatchers.IO.asExecutor()
                )
            }

            private val surfaceTexture = SurfaceTexture(0).also {
                it.setDefaultBufferSize(640, 480)
            }
            val testSurface = Surface(surfaceTexture)

            override fun provideSurface(): ListenableFuture<Surface> {
                return Futures.immediateFuture(testSurface)
            }

            fun cleanUp() {
                testSurface.release()
                surfaceTexture.release()
            }
        }

        fun initAndActive() {
            val sessionConfigBuilder = SessionConfig.Builder().apply {
                setTemplateType(CameraDevice.TEMPLATE_RECORD)
                addSurface(deferrableSurface)
            }

            updateSessionConfig(sessionConfigBuilder.build())
            notifyActive()
        }

        override fun onUnbind() {
            super.onUnbind()
            deferrableSurface.close()
        }
    }

    private suspend fun createPreview(): Preview =
        Preview.Builder().build().also { preview ->
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(getSurfaceProvider())
            }
        }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // No-op
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                }
            }
        )
    }

    private fun RequestMetadata.isAfMode(afMode: Int): Boolean {
        return if (characteristics.isAfModeSupported(afMode)) {
            getOrDefault(CONTROL_AF_MODE, null) == afMode
        } else {
            val fallbackMode =
                if (characteristics.isAfModeSupported(CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                    CONTROL_AF_MODE_CONTINUOUS_PICTURE
                } else if (characteristics.isAfModeSupported(CONTROL_AF_MODE_AUTO)) {
                    CONTROL_AF_MODE_AUTO
                } else {
                    CONTROL_AF_MODE_OFF
                }
            getOrDefault(CONTROL_AF_MODE, null) == fallbackMode
        }
    }

    private fun RequestMetadata.isAeMode(aeMode: Int): Boolean {
        val aeQuirkEnabled =
            camera.getCameraQuirks().contains(ImageCaptureFailWithAutoFlashQuirk::class.java) ||
                DeviceQuirks[CrashWhenTakingPhotoWithAutoFlashAEModeQuirk::class.java] != null
        val aeModeCorrected =
            if (aeQuirkEnabled) AutoFlashAEModeDisablerImpl.getCorrectedAeMode(aeMode) else aeMode

        return if (characteristics.isAeModeSupported(aeModeCorrected)) {
            getOrDefault(CONTROL_AE_MODE, null) == aeModeCorrected
        } else {
            val fallbackMode =
                if (characteristics.isAeModeSupported(CONTROL_AE_MODE_ON)) {
                    CONTROL_AE_MODE_ON
                } else {
                    CONTROL_AE_MODE_OFF
                }
            getOrDefault(CONTROL_AE_MODE, null) == fallbackMode
        }
    }

    private fun Camera.getCameraQuirks(): Quirks {
        return (cameraInfo as? CameraInfoInternal)?.cameraQuirks!!
    }

    private fun CameraCharacteristics.isAfModeSupported(
        afMode: Int
    ) = (get(CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf(-1)).contains(afMode)

    private fun CameraCharacteristics.isAeModeSupported(
        aeMode: Int
    ) = (get(CONTROL_AE_AVAILABLE_MODES) ?: intArrayOf(-1)).contains(aeMode)

    /**
     * Returns the number of metering regions whose weight is greater than 0.
     */
    private val Array<MeteringRectangle>.weightedRegionCount: Int
        get() {
            var count = 0
            forEach {
                count += if (it.meteringWeight != 0) 1 else 0
            }
            return count
        }
}
